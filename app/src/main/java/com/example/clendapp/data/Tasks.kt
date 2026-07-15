package com.example.clendapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Tasks(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val category: Int,
    val date: Long,
    val startDate: Long,
    val dueDate: Long,
    val repeat: Boolean = false
)