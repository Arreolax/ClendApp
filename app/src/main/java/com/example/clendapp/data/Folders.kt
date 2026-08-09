package com.example.clendapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folders(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val id_user: Int
)