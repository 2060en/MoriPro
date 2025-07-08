package com.ethy.mori.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// @Database 註解，列出所有的 Entity 和資料庫版本號
@Database(entities = [Reminder::class, Category::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 讓資料庫知道它管理的 DAO
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    // 使用 companion object 來建立一個單例 (Singleton)，確保 App 中只有一個資料庫實例
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mori_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}