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
    suspend fun insert(task: Tasks)

    @Update
    suspend fun update(task: Tasks)

    @Delete
    suspend fun delete(task: Tasks)

    @Query("SELECT * from tasks WHERE id = :id")
    fun getTask(id: Int): Flow<Tasks?>

    @Query("SELECT * from tasks WHERE id_user = :userId ORDER BY date ASC, dueDate ASC")
    fun getAllTasks(userId: Int): Flow<List<Tasks>>

    @Query("SELECT * FROM tasks WHERE id_user = :userId AND date >= :startOfDay AND date <= :endOfDay ORDER BY dueDate ASC")
    fun getTasksForDate(userId: Int, startOfDay: Long, endOfDay: Long): Flow<List<Tasks>>
}