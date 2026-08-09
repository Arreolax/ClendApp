package com.example.clendapp.data

import android.content.Context
import com.example.clendapp.data.AppDatabase
import com.example.clendapp.data.Scores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object StreakManager {

    /**
     * Completa una actividad, actualiza la racha y los puntos.
     * Retorna la cantidad de puntos ganados en esta actividad.
     * @param pointsOverride Si se proporciona, usa este valor en lugar de los 15 por defecto.
     */
    suspend fun completeActivity(context: Context, pointsOverride: Int? = null): Int = withContext(Dispatchers.IO) {
        val sharedPref = context.getSharedPreferences("clend_app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return@withContext 0

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

        // Lógica de bits semanales (Lunes=0, Martes=1, ..., Domingo=6)
        // Calendar.MONDAY = 2, ..., Calendar.SUNDAY = 1
        val dayIndex = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
        
        // Si han pasado más de 7 días o es una nueva semana lógica, podríamos resetear bits.
        // Pero para simplificar, si la racha se reinicia (else -> 1), reseteamos bits,
        // O si el día actual ya estaba marcado pero es una fecha distinta (nueva semana).
        var currentBits = currentScores.weekly_bits
        if (newStreak <= 1 && !isSameDay) {
            currentBits = 0
        }
        
        val updatedBits = currentBits or (1 shl dayIndex)

        // Puntos a sumar
        val pointsToAdd = pointsOverride ?: 15

        var updatedScores = currentScores.copy(
            study_score = currentScores.study_score + pointsToAdd,
            streak_count = newStreak,
            last_activity_date = System.currentTimeMillis(),
            weekly_bits = updatedBits
        )

        // Verificar desbloqueo de música (30 puntos totales)
        val totalPoints = updatedScores.study_score + updatedScores.calculator_score
        if (totalPoints >= 30 && !updatedScores.music_unlocked) {
            updatedScores = updatedScores.copy(music_unlocked = true)
        }

        db.scoresDao().updateScores(updatedScores)
        checkRankUpgrade(db, updatedScores)
        
        return@withContext pointsToAdd
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
