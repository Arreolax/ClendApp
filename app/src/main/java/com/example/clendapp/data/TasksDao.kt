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
    fun getTask(id: Int): Flow<Tasks>

    @Query("SELECT * from tasks ORDER BY date ASC, startDate ASC")
    fun getAllTasks(): Flow<List<Tasks>>
}