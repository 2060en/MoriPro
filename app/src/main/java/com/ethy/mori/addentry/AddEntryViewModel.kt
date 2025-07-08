package com.ethy.mori.addentry

import androidx.lifecycle.*
import com.ethy.mori.data.Category
import com.ethy.mori.data.CategoryDao

class AddEntryViewModel(categoryDao: CategoryDao) : ViewModel() {
    // 從 DAO 取得所有分類，並提供給 UI 觀察
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories().asLiveData()
}

class AddEntryViewModelFactory(private val categoryDao: CategoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEntryViewModel(categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}