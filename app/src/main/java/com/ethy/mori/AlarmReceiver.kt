package com.ethy.mori

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // 我們定義一個 key，用來在 Intent 中傳遞和讀取訊息
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_ID = "EXTRA_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 從傳入的 Intent 中讀取提醒的 ID 和自訂訊息
        val reminderId = intent.getIntExtra(EXTRA_ID, 0)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "嘿，今天記帳了嗎？"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, reminderId, tapIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, MoriApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mori)
            .setContentTitle("Mori 記帳提醒")
            .setContentText(message) // <<< 使用我們傳入的自訂訊息
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // 使用 reminderId 作為 notificationId，確保每個提醒的通知都是獨立的
            notificationManager.notify(reminderId, builder.build())
        }
    }
}