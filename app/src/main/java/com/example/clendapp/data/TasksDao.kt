package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: Tasks): Long

    @Update
    suspend fun update(task: Tasks)

    @Delete
    suspend fun delete(task: Tasks)

    @Query("SELECT * from tasks WHERE id = :id")
    fun getTask(id: Int): Flow<Tasks?>

    @Query("SELECT * from tasks WHERE id_user = :userId ORDER BY date ASC, dueDate ASC")
    fun getAllTasks(userId: Int): Flow<List<Tasks>>

    @Query("SELECT * from tasks WHERE id_user = :userId")
    suspend fun getAllTasksList(userId: Int): List<Tasks>

    @Query("SELECT * FROM tasks WHERE id_user = :userId AND date >= :startOfDay AND date <= :endOfDay ORDER BY dueDate ASC")
    fun getTasksForDate(userId: Int, startOfDay: Long, endOfDay: Long): Flow<List<Tasks>>

    @Query("UPDATE tasks SET late = (CASE WHEN dueDate < :currentTime AND isCompleted = 0 THEN 1 ELSE 0 END)")
    suspend fun updateLateTasks(currentTime: Long)
}