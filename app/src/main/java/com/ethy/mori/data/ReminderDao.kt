package com.ethy.mori.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// @Dao 告訴 Room，這是我們的資料存取物件介面
@Dao
interface ReminderDao {

    // @Query 用來執行查詢，Flow 則可以讓我們響應式地觀察資料變化
    @Query("SELECT * FROM reminders ORDER BY hour, minute ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    // @Insert 表示這是一個插入新資料的函式
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    // @Update 表示更新資料
    @Update
    suspend fun update(reminder: Reminder)

    // @Delete 表示刪除資料
    @Delete
    suspend fun delete(reminder: Reminder)
}