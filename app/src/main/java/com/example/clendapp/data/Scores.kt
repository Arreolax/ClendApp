package com.example.clendapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Scores(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_user: Int,
    val calculator_score: Int = 0,
    val study_score: Int = 0,
    val id_rank: Int = 1
)
