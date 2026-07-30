package com.example.clendapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Categories(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)