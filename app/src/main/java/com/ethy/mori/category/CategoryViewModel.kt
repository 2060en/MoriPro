package com.ethy.mori.category

import androidx.lifecycle.*
import com.ethy.mori.data.Category
import com.ethy.mori.data.CategoryDao
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryDao: CategoryDao) : ViewModel() {

    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories().asLiveData()

    fun insert(category: Category) = viewModelScope.launch {
        categoryDao.insert(category)
    }

    fun delete(category: Category) = viewModelScope.launch {
        categoryDao.delete(category)
    }
}

class CategoryViewModelFactory(private val categoryDao: CategoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}