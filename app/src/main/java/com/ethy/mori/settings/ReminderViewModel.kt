package com.ethy.mori.settings

import androidx.lifecycle.*
import com.ethy.mori.data.Reminder
import com.ethy.mori.data.ReminderDao
import kotlinx.coroutines.launch

// ViewModel 需要一個 DAO 來操作資料庫
class ReminderViewModel(private val reminderDao: ReminderDao) : ViewModel() {

    // 從 DAO 取得所有的提醒事項，並將其作為一個可被觀察的 Flow
    val allReminders: LiveData<List<Reminder>> = reminderDao.getAllReminders().asLiveData()

    // 插入新的提醒事項
    fun insert(reminder: Reminder) = viewModelScope.launch {
        reminderDao.insert(reminder)
    }
    fun update(reminder: Reminder) = viewModelScope.launch {
        reminderDao.update(reminder)
    }
    fun delete(reminder: Reminder) = viewModelScope.launch {
        reminderDao.delete(reminder)
    }
}

// 我們需要一個工廠 (Factory) 來告訴系統如何建立我們的 ViewModel (因為它需要傳入一個 DAO)
class ReminderViewModelFactory(private val reminderDao: ReminderDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(reminderDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}