package com.example.clendapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FoldersDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: Folders): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(folders: List<Folders>)

    @Query("SELECT * FROM folders ORDER BY id ASC")
    suspend fun getAll(): List<Folders>

    @Query("SELECT * FROM folders WHERE id_user = 0 OR id_user = :userId ORDER BY id ASC")
    suspend fun getFoldersForUser(userId: Int): List<Folders>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Int): Folders?

    @Query("SELECT * FROM folders WHERE name = :name AND (id_user = 0 OR id_user = :userId) LIMIT 1")
    suspend fun getFolderByName(name: String, userId: Int): Folders?
}