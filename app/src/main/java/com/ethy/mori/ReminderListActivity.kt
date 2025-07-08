package com.ethy.mori

import android.os.Bundle
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

class ReminderListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderListBinding

    private val reminderViewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(AppDatabase.getInstance(this).reminderDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = ReminderListAdapter { reminder ->
            val updatedReminder = reminder.copy(isEnabled = !reminder.isEnabled)
            reminderViewModel.update(updatedReminder)
        }
        binding.remindersRecyclerView.adapter = adapter
        binding.remindersRecyclerView.layoutManager = LinearLayoutManager(this)

        reminderViewModel.allReminders.observe(this) { reminders ->
            reminders?.let { adapter.submitList(it) }
        }

        // --- 修改 FAB 的點擊事件 ---
        binding.fabAddReminder.setOnClickListener {
            // 建立並顯示我們的新增對話框
            AddReminderDialogFragment().show(supportFragmentManager, "AddReminderDialog")
        }

        // --- 新增 FragmentResultListener 來接收回傳資料 ---
        supportFragmentManager.setFragmentResultListener("add_reminder_request", this) { requestKey, bundle ->
            // 從 bundle 中取出資料
            val hour = bundle.getInt("hour")
            val minute = bundle.getInt("minute")
            val message = bundle.getString("message", "")

            // 建立新的 Reminder 物件
            val newReminder = Reminder(
                hour = hour,
                minute = minute,
                message = message,
                isEnabled = true
            )
            // 透過 ViewModel 將它插入資料庫
            reminderViewModel.insert(newReminder)
        }
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, // 我們不需要拖曳排序功能，所以設為 0
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // 我們允許向左和向右滑動
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // 因為不需要拖曳排序，所以直接回傳 false
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 當一個項目被滑動時，這個函式會被呼叫
                val position = viewHolder.adapterPosition
                // 從 adapter 取得被滑動的那個 reminder 物件
                val reminderToDelete = adapter.currentList[position]
                // 透過 ViewModel 將它從資料庫中刪除
                reminderViewModel.delete(reminderToDelete)
            }
        }

        // 將我們建立的 aitemTouchHelperCallback 附加到 RecyclerView 上
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.remindersRecyclerView)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}