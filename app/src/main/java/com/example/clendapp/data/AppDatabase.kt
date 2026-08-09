package com.example.clendapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, Tasks::class, Ranks::class, Categories::class, Scores::class],
    version = 21,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun tasksDao(): TasksDao
    abstract fun ranksDao(): RanksDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun scoresDao(): ScoresDao

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
                Log.d("DB", "onCreate executed")
                insertDefaultRanks(db)
                insertDefaultCategories(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d("DB", "onOpen executed")

                // Adding ranks if empty
                val ranksTableExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='ranks'").use { it.moveToFirst() }
                if (ranksTableExists) {
                    val cursor = db.query("SELECT COUNT(*) FROM ranks")
                    if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                        Log.d("DB", "Adding ranks in onOpen")
                        insertDefaultRanks(db)
                    }
                    cursor.close()
                }

                // Adding categories if empty
                val categoriesTableExists = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'").use { it.moveToFirst() }
                if (categoriesTableExists) {
                    val cursor = db.query("SELECT COUNT(*) FROM categories")
                    if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                        Log.d("DB", "Adding categories in onOpen")
                        insertDefaultCategories(db)
                    }
                    cursor.close()
                }
            }

            private fun insertDefaultRanks(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ranks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, score INTEGER NOT NULL)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Potato', 0)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Brainy', 100)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Nerd', 250)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Master', 500)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('God', 1000)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Devil', 2500)")
                db.execSQL("INSERT INTO ranks (name, score) VALUES ('Top Global', 5000)")
            }

            private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, id_user INTEGER NOT NULL)")
                db.execSQL("INSERT INTO categories (name, id_user) VALUES ('Personal', 0)")
                db.execSQL("INSERT INTO categories (name, id_user) VALUES ('Study', 0)")
            }
        }
    }
}
