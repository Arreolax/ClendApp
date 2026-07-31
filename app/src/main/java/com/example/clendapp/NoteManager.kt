package com.example.clendapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object NoteManager {
    private const val FILE_NAME = "notes.json"
    private const val FOLDER_FILE = "folders.json"
    private var notes = mutableListOf<Note>()
    private var defaultFolders = mutableListOf("Personal", "Work", "Ideas")
    private var customFolders = mutableListOf<String>()

    fun loadNotes(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            try {
                val jsonString = file.readText()
                val jsonArray = JSONArray(jsonString)
                notes.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    notes.add(Note(
                        obj.getLong("id"),
                        obj.getString("title"),
                        obj.getString("content"),
                        obj.getString("folder"),
                        obj.getBoolean("isList"),
                        obj.getLong("lastOpened")
                    ))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val folderFile = File(context.filesDir, FOLDER_FILE)
        if (folderFile.exists()) {
            try {
                val jsonArray = JSONArray(folderFile.readText())
                customFolders.clear()
                for (i in 0 until jsonArray.length()) {
                    customFolders.add(jsonArray.getString(i))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        cleanupFolders(context)
    }

    fun saveNotes(context: Context) {
        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("folder", note.folder)
            obj.put("isList", note.isList)
            obj.put("lastOpened", note.lastOpened)
            jsonArray.put(obj)
        }
        File(context.filesDir, FILE_NAME).writeText(jsonArray.toString())
        
        val folderArray = JSONArray()
        customFolders.forEach { folderArray.put(it) }
        File(context.filesDir, FOLDER_FILE).writeText(folderArray.toString())
    }

    private fun cleanupFolders(context: Context) {
        val foldersInUse = notes.map { it.folder }.toSet()
        customFolders.removeAll { it !in foldersInUse }
        saveNotes(context)
    }

    fun getNotes(): List<Note> = notes.sortedByDescending { it.lastOpened }
    
    fun getNotesByFolder(folder: String): List<Note> = 
        if (folder == "All") getNotes() 
        else notes.filter { it.folder == folder }.sortedByDescending { it.lastOpened }

    fun addNote(context: Context, note: Note) {
        notes.add(note)
        saveNotes(context)
    }

    fun updateNote(context: Context, note: Note) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index != -1) {
            notes[index] = note
            saveNotes(context)
        }
    }

    fun deleteNote(context: Context, noteId: Long) {
        notes.removeAll { it.id == noteId }
        cleanupFolders(context)
    }

    fun getFolders(): List<String> {
        return (defaultFolders + customFolders).distinct()
    }

    fun addFolder(context: Context, folder: String) {
        if (folder !in defaultFolders && folder !in customFolders) {
            customFolders.add(folder)
            saveNotes(context)
        }
    }
}
