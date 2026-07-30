package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoriesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Categories)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Categories>)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAll(): List<Categories>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Categories?
}
