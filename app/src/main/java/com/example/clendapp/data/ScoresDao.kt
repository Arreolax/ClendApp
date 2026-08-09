package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoresDao {
    @Query("SELECT * FROM scores WHERE id_user = :userId LIMIT 1")
    suspend fun getScoresByUserId(userId: Int): Scores?

    @Query("SELECT * FROM scores WHERE id_user = :userId LIMIT 1")
    fun getScoresFlowByUserId(userId: Int): Flow<Scores?>

    @Query("SELECT * FROM scores ORDER BY calculator_score DESC")
    fun getAllScoresByCalculator(): Flow<List<Scores>>

    @Query("SELECT * FROM scores ORDER BY study_score DESC")
    fun getAllScoresByStudy(): Flow<List<Scores>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: Scores): Long

    @Update
    suspend fun updateScores(scores: Scores)
}
