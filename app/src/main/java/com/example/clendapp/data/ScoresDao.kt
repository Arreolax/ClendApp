package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ScoresDao {
    @Query("SELECT * FROM scores WHERE id_user = :userId LIMIT 1")
    suspend fun getScoresByUserId(userId: Int): Scores?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: Scores): Long

    @Update
    suspend fun updateScores(scores: Scores)
}
