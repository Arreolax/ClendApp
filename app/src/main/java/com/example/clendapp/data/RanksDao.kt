package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface RanksDao {

    @Insert
    suspend fun insert(rank: Ranks)

    @Insert
    suspend fun insertAll(ranks: List<Ranks>)

    @Query("SELECT * FROM ranks")
    fun getAll(): List<Ranks>
}