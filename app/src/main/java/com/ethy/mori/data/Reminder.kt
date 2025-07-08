package com.ethy.mori.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity 告訴 Room，這個類別對應到資料庫中的一個資料表
@Entity(tableName = "reminders")
data class Reminder(
    // @PrimaryKey 表示這是主鍵，autoGenerate = true 會讓 id 自動遞增
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // @ColumnInfo 用來定義欄位的名稱
    @ColumnInfo(name = "hour")
    val hour: Int,

    @ColumnInfo(name = "minute")
    val minute: Int,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true
)