package com.ethy.mori

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import java.util.*
import android.Manifest
import androidx.preference.Preference
import com.ethy.mori.CategoryListActivity

class SettingsFragment : PreferenceFragmentCompat() {

    // 1. 宣告一個權限請求的發射器
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 如果使用者允許了權限，我們就設定鬧鐘
                scheduleAlarm()
                Toast.makeText(requireContext(), "權限已授予，已設定每日提醒", Toast.LENGTH_SHORT).show()
            } else {
                // 如果使用者拒絕了權限，我們提示他，並將開關恢復為關閉狀態
                Toast.makeText(requireContext(), "未授予通知權限，無法設定提醒", Toast.LENGTH_LONG).show()
                findPreference<SwitchPreferenceCompat>("notifications_enabled")?.isChecked = false
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // 找到「管理每日提醒」這個設定項
        val reminderSettingsPreference: Preference? = findPreference("key_reminder_settings")
        // 為它設定點擊事件
        reminderSettingsPreference?.setOnPreferenceClickListener {
            // 建立一個 Intent 來啟動 ReminderListActivity
            val intent = Intent(requireContext(), ReminderListActivity::class.java)
            startActivity(intent)
            true // 回傳 true 表示事件已被處理
        }

        val categorySettingsPreference: Preference? = findPreference("key_category_settings")
        categorySettingsPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CategoryListActivity::class.java)
            startActivity(intent)
            true
        }
    }

    // 2. 新增一個檢查並請求權限的函式
    private fun checkAndRequestNotificationPermission() {
        // 只在 Android 13 (API 33) 以上需要請求這個權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 檢查是否已經有權限了
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已經有權限，直接設定鬧鐘
                    scheduleAlarm()
                    Toast.makeText(requireContext(), "已設定每日記帳提醒", Toast.LENGTH_SHORT).show()
                    findPreference<SwitchPreferenceCompat>("notifications_enabled")?.isChecked = true
                }
                else -> {
                    // 沒有權限，發射權限請求對話框
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // 較舊的 Android 版本不需要動態請求權限，直接設定鬧鐘
            scheduleAlarm()
            Toast.makeText(requireContext(), "已設定每日記帳提醒", Toast.LENGTH_SHORT).show()
            findPreference<SwitchPreferenceCompat>("notifications_enabled")?.isChecked = true
        }
    }


    private fun scheduleAlarm() {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 建立一個指向 AlarmReceiver 的 Intent
        val intent = Intent(context, AlarmReceiver::class.java)
        // 建立 PendingIntent，它是一個「信物」，讓 AlarmManager 可以在未來代表我們執行這個 Intent
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 設定鬧鐘時間為晚上 10 點
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // 如果設定的時間已經過去了，就設定為明天的同一時間
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 設定一個每日重複的、不精確的鬧鐘（這樣比較省電）
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 使用同一個 PendingIntent 來取消之前設定的鬧鐘
        alarmManager.cancel(pendingIntent)
    }
}