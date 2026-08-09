package com.koftamainee.glucolog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM product WHERE source = :source AND hidden = 0 " +
        "ORDER BY (lastUsed IS NULL), lastUsed DESC, name")
    fun observeBySource(source: String): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM product WHERE hidden = 0 AND nameLower LIKE '%' || :q || '%' " +
            "ORDER BY (lastUsed IS NULL), lastUsed DESC, name LIMIT :limit"
    )
    suspend fun searchProducts(q: String, limit: Int): List<ProductEntity>

    @Query(
        "SELECT * FROM product WHERE source = :source AND hidden = 0 " +
            "AND nameLower LIKE '%' || :q || '%' " +
            "ORDER BY (lastUsed IS NULL), lastUsed DESC, name LIMIT :limit"
    )
    suspend fun searchProductsBySource(q: String, source: String, limit: Int): List<ProductEntity>

    @Query("SELECT name FROM product")
    suspend fun getAllNames(): List<String>

    @Query("SELECT COUNT(*) FROM product WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getProduct(id: Long): ProductEntity?

    @Query("SELECT * FROM product ORDER BY name")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM product WHERE name = :name LIMIT 1")
    suspend fun getProductByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Query("UPDATE product SET hidden = 1 WHERE id = :id")
    suspend fun hideProduct(id: Long)

    @Query("UPDATE product SET hidden = 0")
    suspend fun unhideAll()

    @Query("UPDATE product SET lastUsed = :now WHERE id IN (:ids)")
    suspend fun markProductsUsed(now: Long, ids: List<Long>)

    @Query("DELETE FROM product WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Query("DELETE FROM product")
    suspend fun deleteAllProducts()
}
