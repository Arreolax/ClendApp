package com.example.clendapp

import java.io.Serializable

data class Note(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var content: String,
    var folder: String = "Personal",
    var isList: Boolean = false,
    var lastOpened: Long = System.currentTimeMillis()
) : Serializable
