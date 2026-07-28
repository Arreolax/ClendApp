package com.example.clendapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Tasks::class, Ranks::class],
    version = 11,
    exportSchema = true
)
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
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d("DB", "onCreate ejecutado")
                insertDefaultRanks(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d("DB", "onOpen ejecutado")
                val tableExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='ranks'").use { 
                    it.moveToFirst() 
                }
                
                if (tableExists) {
                    val cursor = db.query("SELECT COUNT(*) FROM ranks")
                    if (cursor.moveToFirst()) {
                        val count = cursor.getInt(0)
                        if (count == 0) {
                            Log.d("DB", "Poblando ranks en onOpen porque estaba vacía")
                            insertDefaultRanks(db)
                        }
                    }
                    cursor.close()
                }
            }

            private fun insertDefaultRanks(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ranks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")

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