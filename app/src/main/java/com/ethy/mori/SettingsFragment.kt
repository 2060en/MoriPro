package com.ethy.mori

import android.Manifest
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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    // 宣告一個權限請求的發射器
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val notificationSwitch = findPreference<SwitchPreferenceCompat>("notifications_enabled")
            if (isGranted) {
                // 如果使用者允許了權限，我們就設定鬧鐘
                scheduleAlarm()
                Toast.makeText(requireContext(), "權限已授予，已設定每日提醒", Toast.LENGTH_SHORT).show()
                notificationSwitch?.isChecked = true
            } else {
                // 如果使用者拒絕了權限，我們提示他，並將開關恢復為關閉狀態
                Toast.makeText(requireContext(), "未授予通知權限，無法設定提醒", Toast.LENGTH_LONG).show()
                notificationSwitch?.isChecked = false
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // --- 監聽「每日提醒開關」 ---
        val notificationSwitch = findPreference<SwitchPreferenceCompat>("notifications_enabled")
        notificationSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                checkAndRequestNotificationPermission()
            } else {
                cancelAlarm()
                Toast.makeText(requireContext(), "已取消每日記帳提醒", Toast.LENGTH_SHORT).show()
            }
            // 我們自己手動處理開關狀態，所以這裡永遠回傳 false
            false
        }

        // --- 監聽「管理每日提醒」的點擊 ---
        findPreference<Preference>("key_reminder_settings")?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), ReminderListActivity::class.java)
            startActivity(intent)
            true
        }

        // --- 監聽「管理消費分類」的點擊 ---
        findPreference<Preference>("key_category_settings")?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CategoryListActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private fun checkAndRequestNotificationPermission() {
        val notificationSwitch = findPreference<SwitchPreferenceCompat>("notifications_enabled")

        // 只在 Android 13 (API 33) 以上需要請求這個權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 情況一：權限已經被授予
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleAlarm()
                    Toast.makeText(requireContext(), "已設定每日記帳提醒", Toast.LENGTH_SHORT).show()
                    notificationSwitch?.isChecked = true
                }

                // 情況二：使用者之前拒絕過，我們應該向他解釋為什麼需要這個權限
                // 在我們的案例中，直接再次請求即可
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                // 情況三：第一次請求，或使用者點了「不再詢問」後的永久拒絕
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // 較舊的 Android 版本不需要動態請求權限
            scheduleAlarm()
            Toast.makeText(requireContext(), "已設定每日記帳提醒", Toast.LENGTH_SHORT).show()
            notificationSwitch?.isChecked = true
        }
    }

    // scheduleAlarm() 和 cancelAlarm() 函式，和我們之前寫的一樣
    private fun scheduleAlarm() {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

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
        alarmManager.cancel(pendingIntent)
    }
}