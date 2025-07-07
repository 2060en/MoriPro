package com.ethy.mori

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ethy.mori.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // 宣告一個 binding 變數，它會幫我們掌管所有 activity_main.xml 裡的元件
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 使用 View Binding 初始化畫面的標準流程 ---
        // 1. 將 XML 佈局檔案實例化成 binding 物件
        binding = ActivityMainBinding.inflate(layoutInflater)
        // 2. 將 binding 的根視圖 (也就是我們的 CoordinatorLayout) 設為 Activity 的內容視圖
        setContentView(binding.root)
        // --- 流程結束 ---

        // 設定我們自訂的 Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 呼叫一個專門設定點擊事件的函式，讓 onCreate 保持乾淨
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 為右下角的「+」按鈕設定點擊事件
        binding.fabAddEntry.setOnClickListener {
            val addEntrySheet = AddEntryBottomSheetFragment()
            addEntrySheet.show(supportFragmentManager, AddEntryBottomSheetFragment.TAG)
        }

        // 為右上角的使用者圖示設定點擊事件
        binding.ivUserSetting.setOnClickListener {
            Toast.makeText(this, "設定功能開發中！", Toast.LENGTH_SHORT).show()
        }

        // 為 Logo 圖示設定點擊事件（可選，這裡也加上測試）
        binding.ivLogo.setOnClickListener {
            Toast.makeText(this, "Logo 被點擊了！", Toast.LENGTH_SHORT).show()
        }
    }
}