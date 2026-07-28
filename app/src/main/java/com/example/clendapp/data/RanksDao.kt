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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rank: Ranks)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(ranks: List<Ranks>)

    @Query("SELECT * FROM ranks ORDER BY id ASC")
    suspend fun getAll(): List<Ranks>

    @Query("SELECT * FROM ranks WHERE id = :id")
    suspend fun getRankById(id: Int): Ranks?
}