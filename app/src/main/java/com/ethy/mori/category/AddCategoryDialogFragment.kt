package com.ethy.mori.category

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.ethy.mori.databinding.DialogAddCategoryBinding

class AddCategoryDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val binding = DialogAddCategoryBinding.inflate(inflater)

            builder.setView(binding.root)
                .setTitle("新增消費分類")
                .setPositiveButton("儲存") { _, _ ->
                    val categoryName = binding.etCategoryName.text.toString()
                    if (categoryName.isNotBlank()) {
                        // 將資料打包並回傳
                        setFragmentResult("add_category_request", bundleOf("name" to categoryName))
                    } else {
                        Toast.makeText(requireContext(), "分類名稱不可為空", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消") { _, _ ->
                    dialog?.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}