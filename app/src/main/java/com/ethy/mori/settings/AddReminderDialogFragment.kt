package com.ethy.mori.settings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ethy.mori.databinding.DialogAddReminderBinding

class AddReminderDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 使用 AlertDialog.Builder 來建立對話框
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // 取得 LayoutInflater 來載入我們的自訂佈局
            val inflater = requireActivity().layoutInflater
            val binding = DialogAddReminderBinding.inflate(inflater)

            // 將我們的自訂佈局設定給對話框
            builder.setView(binding.root)
                // 設定標題
                .setTitle("新增提醒")
                // 設定「儲存」按鈕和它的點擊事件
                .setPositiveButton("儲存") { dialog, id ->
                    // 1. 從 TimePicker 取得使用者選擇的小時和分鐘
                    val hour = binding.timePicker.hour
                    val minute = binding.timePicker.minute
                    // 2. 從 EditText 取得使用者輸入的訊息
                    val message = binding.etReminderMessage.text.toString()

                    // 3. 將資料打包成一個 Bundle
                    val result = bundleOf(
                        "hour" to hour,
                        "minute" to minute,
                        "message" to message
                    )
                    // 4. 使用 setFragmentResult 將結果傳送回去
                    setFragmentResult("add_reminder_request", result)
                }
                // 設定「取消」按鈕，它不需要做任何事，點擊後對話框會自動關閉
                .setNegativeButton("取消") { dialog, id ->
                    getDialog()?.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}