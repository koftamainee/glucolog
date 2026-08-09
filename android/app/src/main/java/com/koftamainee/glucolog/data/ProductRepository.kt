package com.koftamainee.glucolog.data

import android.content.Context
import androidx.room.withTransaction
import com.koftamainee.glucolog.data.db.AppDatabase
import com.koftamainee.glucolog.data.db.ProductEntity
import com.koftamainee.glucolog.data.importexport.FoodCsvCodec
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val db: AppDatabase) {

    private val foodDao = db.foodDao()

    fun observeMine(): Flow<List<ProductEntity>> = foodDao.observeBySource(SOURCE_MANUAL)

    suspend fun searchProducts(q: String, source: String?, limit: Int): List<ProductEntity> =
        if (source == null) foodDao.searchProducts(q, limit)
        else foodDao.searchProductsBySource(q, source, limit)

    suspend fun getProduct(id: Long): ProductEntity? = foodDao.getProduct(id)

    suspend fun getAllProducts(): List<ProductEntity> = foodDao.getAllProducts()

    suspend fun addProduct(
        name: String,
        kcal: Float,
        proteins: Float,
        fats: Float,
        carbs: Float,
        portionMass: Int,
        note: String?,
        source: String = SOURCE_MANUAL,
    ): Long = foodDao.insertProduct(
        ProductEntity(
            name = name,
            kcal = kcal,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
            portionMass = portionMass,
            note = note,
            source = source,
            nameLower = normalizeName(name),
        )
    )

    suspend fun updateProduct(product: ProductEntity) = foodDao.updateProduct(
        product.copy(nameLower = normalizeName(product.name))
    )

    suspend fun deleteProduct(id: Long) = foodDao.deleteProduct(id)

    suspend fun hideProduct(id: Long) = foodDao.hideProduct(id)

    suspend fun importFood(products: List<ProductEntity>, replace: Boolean) {
        db.withTransaction {
            if (replace) {
                foodDao.deleteAllProducts()
            }
            products.forEach { product ->
                val normalized = product.copy(nameLower = normalizeName(product.name))
                val existing = foodDao.getProductByName(product.name)
                if (existing != null) {
                    foodDao.updateProduct(
                        existing.copy(
                            kcal = normalized.kcal,
                            proteins = normalized.proteins,
                            fats = normalized.fats,
                            carbs = normalized.carbs,
                            portionMass = normalized.portionMass,
                            note = normalized.note,
                            source = normalized.source,
                            nameLower = normalized.nameLower,
                        )
                    )
                } else {
                    foodDao.insertProduct(normalized)
                }
            }
        }
    }

    suspend fun seedBuiltin(context: Context) {
        if (foodDao.countBySource(SOURCE_BUILTIN) > 0) {
            foodDao.unhideAll()
            return
        }
        val text = context.assets.open("food.csv").bufferedReader().use { it.readText() }
        val parsed = FoodCsvCodec.parse(text)
        db.withTransaction {
            val existing = foodDao.getAllNames().map { normalizeName(it) }.toMutableSet()
            val toInsert = mutableListOf<ProductEntity>()
            parsed.products.forEach { product ->
                val lower = normalizeName(product.name)
                if (existing.add(lower)) {
                    toInsert.add(
                        product.copy(
                            source = SOURCE_BUILTIN,
                            nameLower = lower,
                        )
                    )
                }
            }
            toInsert.chunked(500).forEach { chunk -> foodDao.insertAll(chunk) }
        }
    }

    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_REMOTE = "remote"
        const val SOURCE_BUILTIN = "builtin"

        fun normalizeName(name: String): String =
            name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    }
}
