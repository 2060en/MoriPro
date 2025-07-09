package com.ethy.mori

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ethy.mori.data.AppDatabase
import com.ethy.mori.data.Reminder
import com.ethy.mori.databinding.ActivityReminderListBinding
import com.ethy.mori.settings.AddReminderDialogFragment
import com.ethy.mori.settings.ReminderListAdapter
import com.ethy.mori.settings.ReminderViewModel
import com.ethy.mori.settings.ReminderViewModelFactory
import java.util.*

class ReminderListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderListBinding

    private val reminderViewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(AppDatabase.getInstance(this).reminderDao())
    }

    // 將 AlarmManager 宣告為成員變數，方便使用
    private val alarmManager by lazy {
        getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // --- 修改 Adapter 的建立，將鬧鐘操作加入到開關的點擊事件中 ---
        val adapter = ReminderListAdapter { reminder ->
            val updatedReminder = reminder.copy(isEnabled = !reminder.isEnabled)
            reminderViewModel.update(updatedReminder)

            // 根據新的開關狀態，設定或取消鬧鐘
            if (updatedReminder.isEnabled) {
                scheduleAlarm(updatedReminder)
            } else {
                cancelAlarm(updatedReminder)
            }
        }
        binding.remindersRecyclerView.adapter = adapter
        binding.remindersRecyclerView.layoutManager = LinearLayoutManager(this)

        reminderViewModel.allReminders.observe(this) { reminders ->
            reminders?.let { adapter.submitList(it) }
        }

        binding.fabAddReminder.setOnClickListener {
            AddReminderDialogFragment().show(supportFragmentManager, "AddReminderDialog")
        }

        // --- 修改 FragmentResultListener，在新增提醒後也設定鬧鐘 ---
        supportFragmentManager.setFragmentResultListener("add_reminder_request", this) { _, bundle ->
            val hour = bundle.getInt("hour")
            val minute = bundle.getInt("minute")
            val message = bundle.getString("message", "")

            val newReminder = Reminder(hour = hour, minute = minute, message = message, isEnabled = true)

            // 由於 id 是自動產生的，我們需要在插入後重新排程所有鬧鐘
            // 這裡我們先插入，之後再處理排程
            reminderViewModel.insert(newReminder)
            // 之後我們會在這裡加入一個更好的排程方法
        }

        // --- 修改滑動刪除的邏輯，在刪除時也取消鬧鐘 ---
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val reminderToDelete = adapter.currentList[position]
                // 在刪除資料庫紀錄前，先取消它對應的鬧鐘
                cancelAlarm(reminderToDelete)
                reminderViewModel.delete(reminderToDelete)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.remindersRecyclerView)
    }

    // ↓↓↓ 加入這兩個全新的函式 ↓↓↓

    private fun scheduleAlarm(reminder: Reminder) {
        // 呼叫我們的鑰匙產生器
        val pendingIntent = createReminderPendingIntent(reminder)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // 處理 Android 14 以上可能出現的精確鬧鐘權限問題
            Log.e("ReminderListActivity", "無法設定鬧鐘，請檢查權限", e)
            Toast.makeText(this, "無法設定鬧鐘，請檢查 App 權限", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelAlarm(reminder: Reminder) {
        // 呼叫我們的鑰匙產生器，以確保鑰匙完全相同
        val pendingIntent = createReminderPendingIntent(reminder)
        alarmManager.cancel(pendingIntent)
    }
    private fun createReminderPendingIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            // 確保 Intent 中包含所有必要的資訊
            putExtra(AlarmReceiver.EXTRA_MESSAGE, reminder.message)
            putExtra(AlarmReceiver.EXTRA_ID, reminder.id)
        }

        // 使用 reminder.id 作為 requestCode，確保每個 PendingIntent 都是唯一的
        return PendingIntent.getBroadcast(
            applicationContext,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // <<< 修正後的正確 Flag
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}