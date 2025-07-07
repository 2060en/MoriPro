package com.ethy.mori

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ethy.mori.databinding.FragmentAddEntryBinding
import com.ethy.mori.network.ApiClient
import com.ethy.mori.network.GoogleSheetEntry
import com.ethy.mori.network.NotionPageRequest
import com.ethy.mori.network.NotionProperties
import com.ethy.mori.network.NumberProperty
import com.ethy.mori.network.Parent
import com.ethy.mori.network.SelectOption
import com.ethy.mori.network.SelectProperty
import com.ethy.mori.network.TextContent
import com.ethy.mori.network.TitleContent
import com.ethy.mori.network.TitleProperty
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback

class AddEntryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddEntryBinding? = null
    private val binding get() = _binding!!

    private var amount: String = ""
    private var itemDescription: String = ""

    // ↓↓↓ 請將 "YOUR_NOTION_TOKEN" 和 "YOUR_DATABASE_ID" 替換成您自己的金鑰 ↓↓↓
    private val NOTION_API_TOKEN = "Bearer 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"
    private val NOTION_DATABASE_ID = "228da545b9708010bcf8dcf2619220cf"
    private val GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzJJ3lwEZ0nGBRa2ULQpyQf1lDXghHjBKfUo1BN8YCNOLB9pSzXQ_XTRaTCWERNIIH9zw/exec"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val windowHeight = resources.displayMetrics.heightPixels
            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = (windowHeight * 0.40).toInt()
            bottomSheet.layoutParams = layoutParams
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    // 一個新的函式，專門用來切換步驟，讓邏輯更清晰
    private fun showStep(step: Int) {
        // 先隱藏所有內容和按鈕
        binding.step1Container.visibility = View.GONE
        binding.step2Container.visibility = View.GONE
        binding.step3Container.visibility = View.GONE
        binding.btnNextToStep2.visibility = View.GONE
        binding.buttonsContainerStep2.visibility = View.GONE
        binding.buttonsContainerStep3.visibility = View.GONE

        // 根據傳入的步驟號碼，顯示對應的內容和按鈕
        when (step) {
            1 -> {
                binding.step1Container.visibility = View.VISIBLE
                binding.btnNextToStep2.visibility = View.VISIBLE
            }
            2 -> {
                binding.step2Container.visibility = View.VISIBLE
                binding.buttonsContainerStep2.visibility = View.VISIBLE
            }
            3 -> {
                binding.step3Container.visibility = View.VISIBLE
                binding.buttonsContainerStep3.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        // --- 步驟一 ---
        binding.btnNextToStep2.setOnClickListener {
            amount = binding.etAmount.text.toString()
            if (amount.isBlank()) {
                Toast.makeText(requireContext(), "請先輸入金額", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.tvAmountContext.text = "$ $amount"
            showStep(2) // 呼叫新函式來切換到步驟二
        }

        // --- 步驟二 ---
        binding.btnBackToStep1.setOnClickListener {
            showStep(1) // 切換回步驟一
        }

        binding.btnNextToStep3.setOnClickListener {
            itemDescription = binding.etItemDescription.text.toString()
            if (itemDescription.isBlank()) {
                Toast.makeText(requireContext(), "請輸入項目說明", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.tvContextStep3.text = "$ $amount - $itemDescription"
            showStep(3) // 切換到步驟三
        }

        // --- 步驟三 ---
        binding.btnBackToStep2.setOnClickListener {
            showStep(2) // 切換回步驟二
        }

        binding.btnSave.setOnClickListener {
            val checkedChipId = binding.chipGroupCategory.checkedChipId
            if (checkedChipId == View.NO_ID) {
                Toast.makeText(requireContext(), "請選擇一個分類", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategory = binding.root.findViewById<Chip>(checkedChipId).text.toString()

            val finalRecord = "金額: $amount, 項目: $itemDescription, 分類: $selectedCategory"
            Toast.makeText(requireContext(), "儲存成功！$finalRecord", Toast.LENGTH_LONG).show()
            // 呼叫我們的新函式來發送資料
            sendDataToNotion(amount, itemDescription, selectedCategory)
            dismiss()
        }
    }
    private fun sendDataToNotion(amountStr: String, description: String, category: String) {
        // 1. 建立要發送的資料包裹 (這部分不變)
        val requestBody = NotionPageRequest(
            parent = Parent(databaseId = NOTION_DATABASE_ID),
            properties = NotionProperties(
                item = TitleProperty(listOf(TitleContent(TextContent(description)))),
                amount = NumberProperty(amountStr.toDouble()),
                category = SelectProperty(SelectOption(category))
            )
        )

        // 2. 取得 API 呼叫的 Call 物件
        val call = ApiClient.notionApiService.createPage(
            token = NOTION_API_TOKEN,
            requestBody = requestBody
        )

        // 3. 使用 enqueue 異步執行，並傳入一個回呼物件來處理結果
        call.enqueue(object : retrofit2.Callback<Unit> {

            // 當收到伺服器回應時 (不論成功或失敗) 會被呼叫
            override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    // Notion 寫入成功！
                    Log.d(TAG, "Notion API call successful.")

                    // ↓↓↓ 在這裡，接著呼叫 Google Sheets 的函式！↓↓↓
                    sendDataToGoogleSheets(amountStr, description, category)

                    // 為了更好的使用者體驗，我們先不等 Google Sheets 回應，直接提示成功並關閉視窗
                    context?.let { Toast.makeText(it, "🎉 記錄成功！", Toast.LENGTH_SHORT).show() }
                    Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 800)
                } else {
                    // 伺服器回傳錯誤 (例如 400, 401, 500)
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(requireContext(), "寫入 Notion 失敗: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Notion API Error: ${response.code()} - $errorBody")
                }
            }

            // 當網路發生問題 (例如沒網路) 或其他例外時會被呼叫
            override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
                Toast.makeText(requireContext(), "發生網路錯誤: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Network Failure: ", t)
            }
        })
    }
    private fun sendDataToGoogleSheets(amountStr: String, description: String, category: String) {
        val entry = GoogleSheetEntry(
            item = description,
            category = category,
            amount = amountStr.toDouble()
        )

        val call = ApiClient.googleSheetsApiService.addEntry(
            url = GOOGLE_SCRIPT_URL,
            entry = entry
        )

        call.enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    // Google Sheets 寫入成功，我們在背景記錄日誌即可
                    Log.d(TAG, "Google Sheets API call successful.")
                } else {
                    // Google Sheets 寫入失敗，同樣記錄日誌
                    Log.e(TAG, "Google Sheets API Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
                // Google Sheets 網路錯誤，同樣記錄日誌
                Log.e(TAG, "Google Sheets Network Failure: ", t)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEntryBottomSheetFragment"
    }
}