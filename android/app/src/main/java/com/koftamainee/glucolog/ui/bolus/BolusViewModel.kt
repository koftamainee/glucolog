package com.koftamainee.glucolog.ui.bolus

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koftamainee.glucolog.data.DayRepository
import com.koftamainee.glucolog.data.ProductRepository
import com.koftamainee.glucolog.data.SettingsDataStore
import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.data.importexport.FileOps
import com.koftamainee.glucolog.data.importexport.FoodCsvCodec
import com.koftamainee.glucolog.data.xdrip.XdripStatusProvider
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.domain.BolusCalculator
import com.koftamainee.glucolog.domain.FoodNutrients
import com.koftamainee.glucolog.domain.MealField
import com.koftamainee.glucolog.domain.currentTimeString
import com.koftamainee.glucolog.domain.timeToFloat
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FoodItem(
    val id: Long,
    val name: String,
    val nutrients: FoodNutrients,
    val mass: String,
    val note: String? = null,
)

data class BolusUiState(
    val foodItems: List<FoodItem> = emptyList(),
    val tdd: Float? = null,
    val ug: String = "0.48",
    val fchi: String = "5",
    val ugAuto: Boolean = true,
    val fchiAuto: Boolean = true,
    val targetGlucose: String = "5",
    val actualGlucose: String = "",
    val actualGlucoseAuto: Boolean = true,
    val activeInsulin: String = "",
    val activeInsulinAuto: Boolean = true,
    val userProducts: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<ProductEntity> = emptyList(),
    val searchBusy: Boolean = false,
    val searchSource: String? = null,
    val message: String? = null,
    val tab: Int = 0,
)

class BolusViewModel(
    private val productRepo: ProductRepository,
    private val settings: SettingsDataStore,
    private val dayRepo: DayRepository,
    private val xdrip: XdripStatusProvider,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BolusUiState())
    val state: StateFlow<BolusUiState> = _state

    private var xdripLastValue: Float? = null
    private var xdripLastDate: String? = null

    init {
        viewModelScope.launch {
            settings.targetGlucose.collect { target ->
                _state.update { it.copy(targetGlucose = format(target)) }
            }
        }
        viewModelScope.launch {
            xdrip.status.collect { status ->
                xdripLastValue = status.lastValue
                xdripLastDate = status.lastDate
                refreshActualGlucose()
            }
        }
        viewModelScope.launch {
            productRepo.observeMine().collect { products ->
                _state.update { it.copy(userProducts = products) }
            }
        }
        viewModelScope.launch {
            dayRepo.observeDay(LocalDate.now()).collect {
                refreshFactors()
                refreshActualGlucose()
                refreshActiveInsulin()
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(activeInsulin = format(computeActiveInsulin())) }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshFactors()
                refreshActiveInsulin()
            }
        }
    }

    fun selectTab(index: Int) = _state.update { it.copy(tab = index) }

    private var searchJob: Job? = null

    fun searchFood(query: String, source: String?) {
        _state.update {
            it.copy(searchQuery = query, searchSource = source, searchBusy = true)
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val q = query.trim().lowercase()
            val results = withContext(Dispatchers.IO) {
                when (source) {
                    null -> {
                        val manual = productRepo.searchProducts(q, ProductRepository.SOURCE_MANUAL, 50)
                        val builtin = productRepo.searchProducts(q, ProductRepository.SOURCE_BUILTIN, 50)
                        manual + builtin
                    }
                    else -> productRepo.searchProducts(q, source, 50)
                }
            }
            _state.update { it.copy(searchResults = results, searchBusy = false) }
        }
    }

    fun restoreBuiltinLibrary() {
        viewModelScope.launch {
            productRepo.seedBuiltin(appContext)
            _state.update { it.copy(message = "Встроенная библиотека восстановлена") }
        }
    }

    fun onTargetGlucoseChange(value: String) {
        _state.update { it.copy(targetGlucose = value) }
        viewModelScope.launch {
            settings.setTargetGlucose(value.toFloatOrNull() ?: return@launch)
        }
    }

    fun onActualGlucoseChange(value: String) {
        _state.update { it.copy(actualGlucose = value, actualGlucoseAuto = false) }
    }

    fun toggleActualGlucoseAuto() {
        _state.update { it.copy(actualGlucoseAuto = !it.actualGlucoseAuto) }
        if (!_state.value.actualGlucoseAuto.not()) viewModelScope.launch { refreshActualGlucose() }
    }

    fun onActiveInsulinChange(value: String) {
        _state.update { it.copy(activeInsulin = value, activeInsulinAuto = false) }
    }

    fun toggleActiveInsulinAuto() {
        _state.update { it.copy(activeInsulinAuto = !it.activeInsulinAuto) }
        if (!_state.value.activeInsulinAuto.not()) refreshActiveInsulin()
    }

    fun onUgChange(value: String) {
        _state.update { it.copy(ug = value, ugAuto = false) }
    }

    fun toggleUgAuto() {
        _state.update { it.copy(ugAuto = !it.ugAuto) }
        if (!_state.value.ugAuto.not()) viewModelScope.launch { refreshFactors() }
    }

    fun onFchiChange(value: String) {
        _state.update { it.copy(fchi = value, fchiAuto = false) }
    }

    fun toggleFchiAuto() {
        _state.update { it.copy(fchiAuto = !it.fchiAuto) }
        if (!_state.value.fchiAuto.not()) viewModelScope.launch { refreshFactors() }
    }

    fun addLocalProduct(product: ProductEntity) {
        _state.update { state ->
            state.copy(foodItems = state.foodItems + FoodItem(
                id = product.id,
                name = product.name,
                nutrients = FoodNutrients(product.kcal, product.proteins, product.fats, product.carbs),
                mass = product.portionMass.takeIf { it > 0 }?.toString() ?: "100",
                note = product.note,
            ))
        }
        viewModelScope.launch {
            productRepo.markProductsUsed(listOf(product.id))
        }
    }

    fun updateMass(index: Int, value: String) {
        _state.update { state ->
            state.copy(
                foodItems = state.foodItems.mapIndexed { i, item ->
                    if (i == index) item.copy(mass = value) else item
                }
            )
        }
    }

    fun removeItem(index: Int) {
        _state.update { state ->
            state.copy(foodItems = state.foodItems.filterIndexed { i, _ -> i != index })
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun writeMeal() {
        val items = _state.value.foodItems
        if (items.isEmpty()) {
            _state.update { it.copy(message = "Список еды пуст") }
            return
        }
        viewModelScope.launch {
            val mealKey = "bolus_${System.nanoTime()}"
            val date = LocalDate.now()
            val text = items.joinToString("; ") { item ->
                val mass = item.mass.toIntOrNull() ?: 0
                "${item.name} ($mass г)"
            }
            val carbs = items.fold(0f) { acc, item ->
                val mass = item.mass.toFloatOrNull() ?: 0f
                acc + item.nutrients.carbs * mass / 100f
            }.roundToLong().toInt()
            dayRepo.setMealField(date, mealKey, MealField.TIME, currentTimeString())
            dayRepo.setMealField(date, mealKey, MealField.FOOD, text)
            dayRepo.setMealField(date, mealKey, MealField.CARBS, carbs.toString())
            _state.update { it.copy(message = "Еда вставлена в день") }
        }
    }

    fun writeBolus(total: Float) {
        if (total <= 0f) {
            _state.update { it.copy(message = "Болюс не записан: итог неположительный") }
            return
        }
        viewModelScope.launch {
            val now = timeToFloat(currentTimeString()) ?: 0f
            dayRepo.setBolus(LocalDate.now(), now, total)
            refreshActiveInsulin()
            _state.update { it.copy(message = "Болюс записан: ${format(total)} ЕД") }
        }
    }

    fun clearFood() = _state.update { it.copy(foodItems = emptyList()) }

    fun addManualProduct(
        name: String,
        proteins: String,
        fats: String,
        carbs: String,
        portionMass: String,
        note: String,
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(message = "Укажите название продукта") }
            return
        }
        val p = proteins.toFloatOrNull() ?: 0f
        val f = fats.toFloatOrNull() ?: 0f
        val c = carbs.toFloatOrNull() ?: 0f
        viewModelScope.launch {
            productRepo.addProduct(
                name = name.trim(),
                kcal = p * 4f + f * 9f + c * 4f,
                proteins = p,
                fats = f,
                carbs = c,
                portionMass = portionMass.toIntOrNull() ?: 0,
                note = note.trim().ifEmpty { null },
                source = ProductRepository.SOURCE_MANUAL,
            )
        }
    }

    fun updateLocalProduct(
        id: Long,
        name: String,
        proteins: String,
        fats: String,
        carbs: String,
        portionMass: String,
        note: String,
    ) {
        val current = findProduct(id) ?: return
        val p = proteins.toFloatOrNull() ?: 0f
        val f = fats.toFloatOrNull() ?: 0f
        val c = carbs.toFloatOrNull() ?: 0f
        viewModelScope.launch {
            productRepo.updateProduct(
                current.copy(
                    name = name.trim(),
                    kcal = p * 4f + f * 9f + c * 4f,
                    proteins = p,
                    fats = f,
                    carbs = c,
                    portionMass = portionMass.toIntOrNull() ?: 0,
                    note = note.trim().ifEmpty { null },
                    source = ProductRepository.SOURCE_MANUAL,
                )
            )
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            val product = findProduct(id) ?: return@launch
            if (product.source == ProductRepository.SOURCE_BUILTIN) {
                productRepo.hideProduct(id)
            } else {
                productRepo.deleteProduct(id)
            }
            _state.update { state ->
                state.copy(
                    foodItems = state.foodItems.filterNot { it.id == id },
                    searchResults = state.searchResults.filterNot { it.id == id },
                )
            }
        }
    }

    fun addProductFromLibrary(id: Long) {
        findProduct(id)?.let(::addLocalProduct)
    }

    private fun findProduct(id: Long): ProductEntity? =
        _state.value.userProducts.firstOrNull { it.id == id }
            ?: _state.value.searchResults.firstOrNull { it.id == id }

    fun exportFoodCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val products = productRepo.getAllProducts()
                val text = FoodCsvCodec.export(products)
                FileOps.writeText(appContext, uri, text)
                _state.update {
                    it.copy(message = "Экспортировано продуктов: ${products.size}")
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Ошибка экспорта") }
            }
        }
    }

    fun importFoodCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = FileOps.readText(appContext, uri)
                val parsed = FoodCsvCodec.parse(text)
                productRepo.importFood(parsed.products, replace = false)
                _state.update {
                    it.copy(message = "Импортировано продуктов: ${parsed.products.size}")
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Не удалось импортировать") }
            }
        }
    }

    private suspend fun refreshFactors() {
        val tdd = computeTdd()
        _state.update { state ->
            state.copy(
                tdd = tdd,
                ug = if (state.ugAuto) autoUg(tdd) else state.ug,
                fchi = if (state.fchiAuto) autoFchi(tdd) else state.fchi,
            )
        }
    }

    private suspend fun refreshActualGlucose() {
        if (!_state.value.actualGlucoseAuto) return
        val today = LocalDate.now()
        val todayLatest = dayRepo.getDay(today).glucose.maxByOrNull { it.h }?.g
        val xdripToday = if (xdripLastDate == today.toString()) xdripLastValue else null
        val value = todayLatest ?: xdripToday
        _state.update { it.copy(actualGlucose = if (value != null) format(value) else "") }
    }

    private fun autoUg(tdd: Float?): String =
        if (tdd != null) format(BolusCalculator.carbohydrateCoefficient(tdd)) else "0"

    private fun autoFchi(tdd: Float?): String =
        if (tdd != null) format(BolusCalculator.insulinSensitivityFactor(tdd)) else "0"

    private suspend fun computeTdd(): Float? {
        val today = LocalDate.now()
        val doses = (0 until 7).map { i ->
            dayRepo.getDay(today.minusDays(i.toLong()))
                .insulin.fold(0f) { acc, point ->
                    acc + (point.bolus ?: 0f) + (point.basal ?: 0f)
                }
        }
        return BolusCalculator.averageDailyDose(doses)
    }

    private fun refreshActiveInsulin() {
        viewModelScope.launch {
            if (_state.value.activeInsulinAuto) {
                _state.update { it.copy(activeInsulin = format(computeActiveInsulin())) }
            }
        }
    }

    private suspend fun computeActiveInsulin(): Float {
        val now = timeToFloat(currentTimeString()) ?: return 0f
        val day = dayRepo.getDay(LocalDate.now())
        val total = day.insulin.fold(0f) { acc, point ->
            val bolus = point.bolus ?: 0f
            val hours = now - point.h
            if (bolus <= 0f || hours <= 0f) acc
            else acc + max(0f, bolus * (1f - 0.2f * hours))
        }
        return BolusCalculator.round2(total)
    }

    private fun format(value: Float): String {
        val r = BolusCalculator.round2(value)
        return if (r == r.toInt().toFloat()) r.toInt().toString() else r.toString()
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                BolusViewModel(
                    container.productRepository,
                    container.settingsDataStore,
                    container.dayRepository,
                    container.xdripStatusProvider,
                    container.appContext,
                )
            }
        }
    }
}
