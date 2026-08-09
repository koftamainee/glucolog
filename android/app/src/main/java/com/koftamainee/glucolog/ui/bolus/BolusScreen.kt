package com.koftamainee.glucolog.ui.bolus

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.koftamainee.glucolog.data.ProductRepository
import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.domain.BolusCalculator
import com.koftamainee.glucolog.domain.BolusResult
import com.koftamainee.glucolog.domain.FoodPortion
import com.koftamainee.glucolog.ui.components.PrimaryAddButton
import com.koftamainee.glucolog.ui.components.SectionCard
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun BolusScreen(viewModel: BolusViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showAddFood by remember { mutableStateOf(false) }
    var showProductEditor by remember { mutableStateOf(false) }
    var editProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var foodQuery by remember { mutableStateOf("") }
    var foodFilter by remember { mutableStateOf(FoodFilter.ALL) }

    LaunchedEffect(foodFilter) {
        viewModel.searchFood(foodQuery, searchSourceFor(foodFilter))
    }

    val foodExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) viewModel.exportFoodCsv(uri) }
    val foodImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importFoodCsv(uri) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            val pagerState = rememberPagerState(pageCount = { 2 })
            LaunchedEffect(state.tab) {
                if (state.tab != pagerState.currentPage) {
                    pagerState.animateScrollToPage(state.tab)
                }
            }
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf("Расчёт", "Еда").forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> CalculationTab(
                    state = state,
                    onAddFood = { showAddFood = true },
                    onTargetGlucoseChange = viewModel::onTargetGlucoseChange,
                    onActualGlucoseChange = viewModel::onActualGlucoseChange,
                    onToggleActualAuto = viewModel::toggleActualGlucoseAuto,
                    onActiveInsulinChange = viewModel::onActiveInsulinChange,
                    onToggleActiveAuto = viewModel::toggleActiveInsulinAuto,
                    onUgChange = viewModel::onUgChange,
                    onToggleUgAuto = viewModel::toggleUgAuto,
                    onFchiChange = viewModel::onFchiChange,
                    onToggleFchiAuto = viewModel::toggleFchiAuto,
                    onMassChange = viewModel::updateMass,
                    onRemove = viewModel::removeItem,
                    onWriteMeal = viewModel::writeMeal,
                    onWriteBolus = viewModel::writeBolus,
                    onClear = viewModel::clearFood,
                )
                else -> FoodTab(
                    userProducts = state.userProducts,
                    searchResults = state.searchResults,
                    searchBusy = state.searchBusy,
                    query = foodQuery,
                    onQueryChange = { newQuery ->
                        foodQuery = newQuery
                        viewModel.searchFood(newQuery, searchSourceFor(foodFilter))
                    },
                    filter = foodFilter,
                    onFilterChange = { newFilter ->
                        foodFilter = newFilter
                        viewModel.searchFood(foodQuery, searchSourceFor(newFilter))
                    },
                    onAdd = {
                        editProduct = null
                        showProductEditor = true
                    },
                    onEdit = { product ->
                        editProduct = product
                        showProductEditor = true
                    },
                    onDelete = viewModel::deleteProduct,
                    onExport = { foodExportLauncher.launch("glucolog-food.csv") },
                    onImport = { foodImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                    onRestore = viewModel::restoreBuiltinLibrary,
                )
            }
        }
        }
    }

    if (showAddFood) {
        AddFoodDialog(
            searchResults = state.searchResults,
            searchBusy = state.searchBusy,
            onQueryChange = { viewModel.searchFood(it, null) },
            onAddProduct = viewModel::addProductFromLibrary,
            onDismiss = { showAddFood = false },
        )
    }

    if (showProductEditor) {
        ProductEditorDialog(
            initial = editProduct,
            onSave = { name, proteins, fats, carbs, mass, note ->
                val product = editProduct
                if (product == null) {
                    viewModel.addManualProduct(name, proteins, fats, carbs, mass, note)
                } else {
                    viewModel.updateLocalProduct(product.id, name, proteins, fats, carbs, mass, note)
                }
                editProduct = null
            },
            onDismiss = { showProductEditor = false },
        )
    }
}

enum class FoodFilter(val title: String) {
    MINE("Мои"),
    BUILTIN("Встроенные"),
    ALL("Все"),
}

private fun searchSourceFor(filter: FoodFilter): String? = when (filter) {
    FoodFilter.MINE -> ProductRepository.SOURCE_MANUAL
    FoodFilter.BUILTIN -> ProductRepository.SOURCE_BUILTIN
    FoodFilter.ALL -> null
}

@Composable
private fun SourceBadge(product: ProductEntity) {
    if (product.source == ProductRepository.SOURCE_BUILTIN) {
        Text(
            text = "Встроенный",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun CalculationTab(
    state: BolusUiState,
    onAddFood: () -> Unit,
    onTargetGlucoseChange: (String) -> Unit,
    onActualGlucoseChange: (String) -> Unit,
    onToggleActualAuto: () -> Unit,
    onActiveInsulinChange: (String) -> Unit,
    onToggleActiveAuto: () -> Unit,
    onUgChange: (String) -> Unit,
    onToggleUgAuto: () -> Unit,
    onFchiChange: (String) -> Unit,
    onToggleFchiAuto: () -> Unit,
    onMassChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onWriteMeal: () -> Unit,
    onWriteBolus: (Float) -> Unit,
    onClear: () -> Unit,
) {
    val factorsBlocked = state.ugAuto && state.tdd == null
    val sugarBlocked = state.actualGlucose.trim().isEmpty()
    val result = remember(state) {
        computeResult(state)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionCard(title = "Еда на приём пищи", trailing = {
            PrimaryAddButton(text = "Добавить еду", onClick = onAddFood)
        }) {
            if (state.foodItems.isEmpty()) {
                Text("Список пуст. Добавьте еду из библиотеки.", style = MaterialTheme.typography.bodyMedium)
            }
            state.foodItems.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = "на 100 г: ${nutrientsText(item)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedTextField(
                        value = item.mass,
                        onValueChange = { onMassChange(index, it) },
                        label = { Text("г") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(84.dp),
                    )
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Убрать")
                    }
                }
                if (index != state.foodItems.lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        SectionCard(title = "Параметры") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AutoFieldRow(
                        label = "АГ",
                        value = state.actualGlucose,
                        onValueChange = onActualGlucoseChange,
                        auto = state.actualGlucoseAuto,
                        onToggleAuto = onToggleActualAuto,
                        modifier = Modifier.weight(1f),
                    )
                    AutoFieldRow(
                        label = "АИ",
                        value = state.activeInsulin,
                        onValueChange = onActiveInsulinChange,
                        auto = state.activeInsulinAuto,
                        onToggleAuto = onToggleActiveAuto,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AutoFieldRow(
                        label = "УГ",
                        value = state.ug,
                        onValueChange = onUgChange,
                        auto = state.ugAuto,
                        onToggleAuto = onToggleUgAuto,
                        modifier = Modifier.weight(1f),
                    )
                    AutoFieldRow(
                        label = "ФЧИ",
                        value = state.fchi,
                        onValueChange = onFchiChange,
                        auto = state.fchiAuto,
                        onToggleAuto = onToggleFchiAuto,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                ParameterRow(
                    label = "Целевой сахар (ЦГ)",
                    value = state.targetGlucose,
                    onValueChange = onTargetGlucoseChange,
                )
                state.tdd?.let { tdd ->
                    Text(
                        text = "Суточная доза из журнала: ${fmt(tdd)} ЕД (среднее за неделю)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                when {
                    factorsBlocked -> Text(
                        text = "Нет данных за неделю — укажите УГ/ФЧИ вручную",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    sugarBlocked -> Text(
                        text = "Нет данных о сахаре — укажите текущий сахар (АГ)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        SectionCard(title = "Итог") {
            if (result == null) {
                Text(
                    text = when {
                        factorsBlocked -> "Нет данных за неделю — укажите УГ/ФЧИ вручную."
                        sugarBlocked -> "Нет данных о сахаре — укажите текущий сахар (АГ)."
                        else -> "Добавьте еду и введите массу, чтобы рассчитать инсулин."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                ResultRow("БЖЕ", fmt(result.bje))
                ResultRow("ХЕ", fmt(result.breadUnits))
                ResultRow("Углеводы, г", fmt(result.totalCarbs))
                ResultRow("Инсулин на БЖЕ", fmt(result.insulinOnBje))
                ResultRow("Инсулин на ХЕ", fmt(result.insulinOnBreadUnits))
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Итоговый болюс (ХЕ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        text = fmt(result.total) + " ЕД",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onWriteMeal, modifier = Modifier.weight(1f)) {
                        Text("Записать еду")
                    }
                    Button(onClick = { onWriteBolus(result.total) }, modifier = Modifier.weight(1f)) {
                        Text("Записать болюс")
                    }
                }
                TextButton(onClick = onClear) { Text("Очистить список") }
            }
        }
    }
}

@Composable
private fun FoodTab(
    userProducts: List<ProductEntity>,
    searchResults: List<ProductEntity>,
    searchBusy: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    filter: FoodFilter,
    onFilterChange: (FoodFilter) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProductEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestore: () -> Unit,
) {
    val (manual, builtin) = remember(searchResults) {
        searchResults.partition { it.source == ProductRepository.SOURCE_MANUAL }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Еда (на 100 г)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryAddButton(text = "Добавить", onClick = onAdd)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                        Text("Экспорт CSV")
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                        Text("Импорт CSV")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { onFilterChange(option) },
                            label = { Text(option.title) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Поиск по названию") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (searchBusy) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (searchResults.isEmpty() && !searchBusy) {
                item {
                    Text(
                        text = if (filter == FoodFilter.MINE && userProducts.isEmpty()) {
                            "Список пуст. Добавьте продукты вручную."
                        } else {
                            "Ничего не найдено."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(manual.size) { index ->
                FoodItemCard(
                    product = manual[index],
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
            if (manual.isNotEmpty() && builtin.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            items(builtin.size) { index ->
                FoodItemCard(
                    product = builtin[index],
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
            if (builtin.size >= 50) {
                item {
                    Text("Показаны первые 50 встроенных — уточните запрос.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (filter != FoodFilter.MINE) {
                item {
                    TextButton(onClick = onRestore) { Text("Восстановить встроенную библиотеку") }
                }
            }
        }

        if (listState.firstVisibleItemIndex > 0) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Наверх")
            }
        }
    }
}

@Composable
private fun FoodItemCard(
    product: ProductEntity,
    onEdit: (ProductEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    SectionCard(
        title = product.name,
        modifier = Modifier.clickable { onEdit(product) },
        trailing = {
            IconButton(onClick = { onDelete(product.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        },
    ) {
        SourceBadge(product)
        Text(
            text = "Б: ${fmt(product.proteins)} · Ж: ${fmt(product.fats)} · У: ${fmt(product.carbs)} · ${fmt(product.kcal)} ккал",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (product.portionMass > 0) {
            Text("Порция ${product.portionMass} г", style = MaterialTheme.typography.bodySmall)
        }
        product.note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddFoodDialog(
    searchResults: List<ProductEntity>,
    searchBusy: Boolean,
    onQueryChange: (String) -> Unit,
    onAddProduct: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { onQueryChange(query) }

    val (manual, builtin) = remember(searchResults) {
        searchResults.partition { it.source == ProductRepository.SOURCE_MANUAL }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить еду") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    searchBusy -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    searchResults.isEmpty() ->
                        Text("Ничего не найдено. Добавьте продукт во вкладке «Еда».", style = MaterialTheme.typography.bodySmall)
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(manual) { product ->
                            DialogItemRow(
                                title = product.name,
                                subtitle = "Б ${fmt(product.proteins)} · Ж ${fmt(product.fats)} · У ${fmt(product.carbs)}",
                                action = { onAddProduct(product.id) },
                                actionText = "Добавить",
                            )
                        }
                        if (manual.isNotEmpty() && builtin.isNotEmpty()) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        items(builtin) { product ->
                            DialogItemRow(
                                title = product.name,
                                subtitle = "Б ${fmt(product.proteins)} · Ж ${fmt(product.fats)} · У ${fmt(product.carbs)}",
                                action = { onAddProduct(product.id) },
                                actionText = "Добавить",
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}

@Composable
private fun DialogItemRow(
    title: String,
    subtitle: String,
    action: () -> Unit,
    actionText: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = action) { Text(actionText) }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    onSave: (String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var proteins by remember(initial) { mutableStateOf(initial?.proteins?.let { fmt(it) } ?: "") }
    var fats by remember(initial) { mutableStateOf(initial?.fats?.let { fmt(it) } ?: "") }
    var carbs by remember(initial) { mutableStateOf(initial?.carbs?.let { fmt(it) } ?: "") }
    var portionMass by remember(initial) {
        mutableStateOf(initial?.portionMass?.takeIf { it > 0 }?.toString() ?: "")
    }
    var note by remember(initial) { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый продукт" else "Редактировать продукт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = proteins, onValueChange = { proteins = it }, label = { Text("Белки, г") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fats, onValueChange = { fats = it }, label = { Text("Жиры, г") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Углеводы, г") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = portionMass, onValueChange = { portionMass = it }, label = { Text("Порция, г (необязательно)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Примечание (необязательно)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Text("Ккал считаются сами: Б×4 + Ж×9 + У×4", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, proteins, fats, carbs, portionMass, note)
                onDismiss()
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun ParameterRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AutoFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    auto: Boolean,
    onToggleAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = !auto,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            Checkbox(
                checked = auto,
                onCheckedChange = { onToggleAuto() },
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun nutrientsText(item: FoodItem): String =
    "Б ${fmt(item.nutrients.proteins)} · Ж ${fmt(item.nutrients.fats)} · У ${fmt(item.nutrients.carbs)} · ${fmt(item.nutrients.kcal)} ккал"

private fun computeResult(state: BolusUiState): BolusResult? {
    if (state.ugAuto && state.tdd == null) return null
    if (state.actualGlucose.trim().isEmpty()) return null
    val items = state.foodItems.mapNotNull { item ->
        val mass = item.mass.toFloatOrNull() ?: return@mapNotNull null
        if (mass <= 0f) null else FoodPortion(item.nutrients, mass)
    }
    if (items.isEmpty()) return null
    return BolusCalculator.calculate(
        items = items,
        ug = state.ug.toFloatOrNull() ?: 0f,
        fchi = state.fchi.toFloatOrNull() ?: 0f,
        targetGlucose = state.targetGlucose.toFloatOrNull() ?: 0f,
        actualGlucose = state.actualGlucose.toFloatOrNull() ?: 0f,
        activeInsulin = state.activeInsulin.toFloatOrNull() ?: 0f,
    )
}

private fun fmt(value: Float): String = String.format(Locale.ROOT, "%.1f", value)
