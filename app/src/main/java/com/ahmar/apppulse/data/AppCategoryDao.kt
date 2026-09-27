package com.ahmar.apppulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(category: AppCategory)

    @Query("SELECT * FROM app_categories")
    fun getAll(): Flow<List<AppCategory>>

    @Query("SELECT DISTINCT category FROM app_categories ORDER BY category ASC")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM app_categories WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppCategory?
}
