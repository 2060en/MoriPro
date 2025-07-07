package com.ethy.mori

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ethy.mori.databinding.ActivitySettingsBinding // 新增這個 import

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding // 宣告 ViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 改用 ViewBinding 來載入畫面
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 將我們在 XML 中定義的 Toolbar 設定為官方的 ActionBar
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        // 現在 supportActionBar 不再是 null，這行程式碼會生效
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // 處理返回箭頭的點擊事件 (維持不變)
    override fun onSupportNavigateUp(): Boolean {
        // is a better way to handle back button than onBackPressed()
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}