package com.ethy.mori

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    // 我們定義一個專屬的日誌標籤，方便在 Logcat 中過濾
    private val TAG = "MoriAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive 方法被觸發了！")

        // 1. 檢查通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "錯誤：沒有發送通知的權限。")
                // 如果沒有權限，就直接結束，不往下執行
                return
            }
        }
        Log.d(TAG, "權限檢查通過。")

        // 2. 建立點擊通知後的行為
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, tapIntent, PendingIntent.FLAG_IMMUTABLE)
        Log.d(TAG, "PendingIntent 建立成功。")

        // 3. 建立通知內容
        val builder = NotificationCompat.Builder(context, MoriApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_mori)
            .setContentTitle("Mori 記帳提醒")
            .setContentText("嘿，今天記帳了嗎？點我開始記錄一筆吧！")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        Log.d(TAG, "Notification Builder 建立成功。")

        // 4. 取得通知管理器並發送通知
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            Log.d(TAG, "準備呼叫 notify()...")
            notificationManager.notify(101, builder.build())
            Log.d(TAG, "notify() 呼叫成功！通知應該已經發送。")
        } catch (e: Exception) {
            Log.e(TAG, "呼叫 notify() 時發生例外錯誤", e)
        }
    }
}