package com.ethy.mori

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethy.mori.category.AddCategoryDialogFragment
import com.ethy.mori.category.CategoryListAdapter
import com.ethy.mori.category.CategoryViewModel
import com.ethy.mori.category.CategoryViewModelFactory
import com.ethy.mori.data.AppDatabase
import com.ethy.mori.data.Category
import com.ethy.mori.databinding.ActivityCategoryListBinding

class CategoryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryListBinding

    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory(AppDatabase.getInstance(this).categoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 建立 Adapter，並傳入刪除按鈕的點擊邏輯
        val adapter = CategoryListAdapter { category ->
            categoryViewModel.delete(category)
        }

        binding.categoryRecyclerView.adapter = adapter
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(this)

        // 觀察資料變化，並自動更新列表
        categoryViewModel.allCategories.observe(this) { categories ->
            categories?.let { adapter.submitList(it) }
        }

        // --- 修改 FAB 的點擊事件 ---
        binding.fabAddCategory.setOnClickListener {
            // 不再新增假資料，而是打開對話框
            AddCategoryDialogFragment().show(supportFragmentManager, "AddCategoryDialog")
        }

        // --- 新增 FragmentResultListener 來接收回傳資料 ---
        supportFragmentManager.setFragmentResultListener("add_category_request", this) { _, bundle ->
            val name = bundle.getString("name", "")
            if (name.isNotBlank()) {
                val newCategory = Category(name = name)
                categoryViewModel.insert(newCategory)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}