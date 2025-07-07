package com.ethy.mori

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MoriApplication : Application() {

    companion object {
        const val CHANNEL_ID = "MORI_REMINDER_CHANNEL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // NotificationChannel 只在 API 26 (Android 8.0) 以上才需要建立
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "每日記帳提醒"
            val descriptionText = "定時提醒使用者進行記帳"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // 向系統註冊這個頻道
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}