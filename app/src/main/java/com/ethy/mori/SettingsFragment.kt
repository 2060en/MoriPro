package com.ethy.mori

import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import android.os.Bundle

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // --- 監聽「管理消費分類」的點擊 ---
        findPreference<Preference>("key_category_settings")?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CategoryListActivity::class.java)
            startActivity(intent)
            true
        }
    }
}