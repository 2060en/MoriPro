package com.ethy.mori.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ethy.mori.data.Category
import com.ethy.mori.databinding.ListItemCategoryBinding

// 在建構子中，加入一個 onDelete Lambda 函式，用來處理刪除按鈕的點擊事件
class CategoryListAdapter(private val onDelete: (Category) -> Unit) :
    ListAdapter<Category, CategoryListAdapter.CategoryViewHolder>(CategoriesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ListItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class CategoryViewHolder(private val binding: ListItemCategoryBinding, private val onDelete: (Category) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentCategory: Category? = null

        init {
            binding.btnDeleteCategory.setOnClickListener {
                currentCategory?.let { onDelete(it) }
            }
        }

        fun bind(category: Category) {
            currentCategory = category
            binding.tvCategoryName.text = category.name
        }
    }

    class CategoriesComparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}