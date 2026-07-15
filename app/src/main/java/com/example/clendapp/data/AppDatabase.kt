package com.example.clendapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Database(entities = [User::class, Tasks::class, Ranks::class], version = 6, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tasksDao(): TasksDao
    abstract fun ranksDao(): RanksDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clend_app_database"
                )
                .addCallback(AppDatabaseCallback())
                .fallbackToDestructiveMigration() // Para desarrollo, borra la DB si cambia el esquema
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insertamos datos usando SQL directo para evitar problemas de inicialización de la instancia
                db.execSQL("INSERT INTO ranks (name) VALUES ('Papita')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Cerebrito')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Nerd')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Master')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Dios')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Diavlo')")
                db.execSQL("INSERT INTO ranks (name) VALUES ('Top Global')")
            }
        }
    }
}
