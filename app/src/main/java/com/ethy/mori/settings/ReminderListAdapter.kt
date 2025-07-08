package com.ethy.mori.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ethy.mori.data.Reminder
import com.ethy.mori.databinding.ListItemReminderBinding
import java.util.Locale

// 在建構子中，加入一個 onToggle Lambda 函式，用來處理點擊事件
class ReminderListAdapter(private val onToggle: (Reminder) -> Unit) :
    ListAdapter<Reminder, ReminderListAdapter.ReminderViewHolder>(RemindersComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ListItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // 將 onToggle 函式傳遞給 ViewHolder
        return ReminderViewHolder(binding, onToggle)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    // ViewHolder 的建構子也需要接收 onToggle 函式
    class ReminderViewHolder(private val binding: ListItemReminderBinding, private val onToggle: (Reminder) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentReminder: Reminder? = null

        init {
            binding.switchReminderEnabled.setOnClickListener {
                // 當開關被點擊時，呼叫傳入的 onToggle 函式，並把當前的 reminder 物件傳出去
                currentReminder?.let { onToggle(it) }
            }
        }

        fun bind(reminder: Reminder) {
            currentReminder = reminder
            binding.tvReminderTime.text = String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute)
            binding.tvReminderMessage.text = reminder.message
            binding.switchReminderEnabled.isChecked = reminder.isEnabled
        }
    }

    class RemindersComparator : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}