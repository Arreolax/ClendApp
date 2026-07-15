package com.example.clendapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ranks")
data class Ranks(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)