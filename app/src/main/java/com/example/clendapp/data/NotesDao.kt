package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notes: Notes): Long

    @Update
    suspend fun update(notes: Notes)

    @Delete
    suspend fun delete(notes: Notes)

    @Query("SELECT * from notes WHERE id = :id")
    fun getNote(id: Int): Flow<Notes?>

    @Query("SELECT * from notes WHERE id_user = :userId ORDER BY date DESC")
    fun getAllNotes(userId: Int): Flow<List<Notes>>

    @Query("SELECT * from notes WHERE id_user = :userId AND id_folder = :folderId ORDER BY date DESC")
    fun getNotesByFolder(userId: Int, folderId: Int): Flow<List<Notes>>

    @Query("SELECT * from notes WHERE id_user = :userId")
    suspend fun getAllNotesList(userId: Int): List<Notes>

}