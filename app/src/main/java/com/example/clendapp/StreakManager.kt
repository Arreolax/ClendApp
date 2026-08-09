package com.example.clendapp

import android.content.Context
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Scores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object StreakManager {

    suspend fun completeActivity(context: Context) = withContext(Dispatchers.IO) {
        val sharedPref = context.getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return@withContext

        val db = AppDatabase.getDatabase(context)
        val currentScores = db.scoresDao().getScoresByUserId(userId) ?: Scores(id_user = userId)

        val now = Calendar.getInstance()
        val lastActivity = Calendar.getInstance().apply { timeInMillis = currentScores.last_activity_date }

        val isSameDay = now.get(Calendar.YEAR) == lastActivity.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastActivity.get(Calendar.DAY_OF_YEAR)

        val isNextDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.let {
            it.get(Calendar.YEAR) == lastActivity.get(Calendar.YEAR) &&
                    it.get(Calendar.DAY_OF_YEAR) == lastActivity.get(Calendar.DAY_OF_YEAR)
        }

        val newStreak = when {
            isSameDay -> currentScores.streak_count
            isNextDay -> currentScores.streak_count + 1
            else -> 1 // Racha reiniciada o primera vez
        }

        // Sumar 15 puntos por completar actividad
        val updatedScores = currentScores.copy(
            study_score = currentScores.study_score + 15,
            streak_count = newStreak,
            last_activity_date = System.currentTimeMillis()
        )

        db.scoresDao().updateScores(updatedScores)
        checkRankUpgrade(db, updatedScores)
    }

    private suspend fun checkRankUpgrade(db: AppDatabase, scores: Scores) {
        val totalPoints = scores.study_score + scores.calculator_score
        val newRankId = when {
            totalPoints >= 5000 -> 7 // Top Global
            totalPoints >= 2500 -> 6 // Diavlo
            totalPoints >= 1000 -> 5 // Dios
            totalPoints >= 500 -> 4  // Master
            totalPoints >= 250 -> 3  // Nerd
            totalPoints >= 100 -> 2  // Cerebrito
            else -> 1 // Papita
        }

        if (newRankId != scores.id_rank) {
            db.scoresDao().updateScores(scores.copy(id_rank = newRankId))
        }
    }
}
