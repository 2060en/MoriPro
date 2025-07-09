package com.ethy.mori

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.ethy.mori.addentry.AddEntryViewModel
import com.ethy.mori.addentry.AddEntryViewModelFactory
import com.ethy.mori.data.AppDatabase
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

    private val addEntryViewModel: AddEntryViewModel by viewModels {
        AddEntryViewModelFactory(AppDatabase.getInstance(requireContext()).categoryDao())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✨ --- 新增這一行 --- ✨
        // 這會告訴系統，當鍵盤出現時，自動調整視窗大小，而不是把整個畫面往上推
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        showStep(0)
        setupClickListeners()
        // 開始觀察分類資料的變化
        observeCategories()
    }
    private fun observeCategories() {
        addEntryViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            // 先清除所有舊的 Chip，避免重複增加
            binding.chipGroupCategory.removeAllViews()

            // 遍歷從資料庫拿到的所有分類
            categories.forEach { category ->
                // 為每一個分類，都建立一個新的 Chip 元件
                val chip = Chip(context)
                chip.text = category.name // 設定 Chip 的文字
                chip.isClickable = true
                chip.isCheckable = true
                // 將 Chip 加入到 ChipGroup 容器中
                binding.chipGroupCategory.addView(chip)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val windowHeight = resources.displayMetrics.heightPixels
            // ✨ --- 修改這裡的邏輯 --- ✨
            // 我們現在讓它預設佔 40%，當鍵盤彈出時，高度會由系統自動調整
            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = (windowHeight * 0.40).toInt() // 維持原本的 40%
            bottomSheet.layoutParams = layoutParams
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // ✨ --- 新增鍵盤監聽 --- ✨
            // 這是你的「暴力解方」的優化版，我們監聽佈局變化來判斷鍵盤狀態
            val rootView = requireActivity().window.decorView.rootView
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                val r = android.graphics.Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - r.bottom

                if (keypadHeight > screenHeight * 0.15) { // 鍵盤彈出
                    // 當鍵盤彈出時，將 BottomSheet 的高度設定為 80%
                    if (layoutParams.height != (windowHeight * 0.80).toInt()) {
                        layoutParams.height = (windowHeight * 0.80).toInt()
                        bottomSheet.layoutParams = layoutParams
                    }
                } else { // 鍵盤收起
                    // 當鍵盤收起時，恢復為原本的 40%
                    if (layoutParams.height != (windowHeight * 0.40).toInt()) {
                        layoutParams.height = (windowHeight * 0.40).toInt()
                        bottomSheet.layoutParams = layoutParams
                    }
                }
            }
        }
    }

    // 一個新的函式，專門用來切換步驟，讓邏輯更清晰
    private fun showStep(targetStepIndex: Int) { // 我們將參數改為 0, 1, 2
        // 取得目前顯示的是第幾頁
        val currentStepIndex = binding.stepsViewFlipper.displayedChild

        // 如果目標頁面和當前頁面相同，則不做任何事
        if (targetStepIndex == currentStepIndex) return

        // 判斷是前進還是後退，並設定對應的動畫
        if (targetStepIndex > currentStepIndex) {
            // 前進：下一頁從右邊滑入，當前頁從左邊滑出
            binding.stepsViewFlipper.setInAnimation(requireContext(), R.anim.slide_in_right)
            binding.stepsViewFlipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
        } else {
            // 後退：上一頁從左邊滑入，當前頁從右邊滑出
            binding.stepsViewFlipper.setInAnimation(requireContext(), R.anim.slide_in_left)
            binding.stepsViewFlipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
        }

        // 命令 ViewFlipper 切換到目標頁面
        binding.stepsViewFlipper.displayedChild = targetStepIndex

        // 同時，切換底部的按鈕
        binding.buttonsContainerStep1.visibility = if (targetStepIndex == 0) View.VISIBLE else View.GONE
        binding.buttonsContainerStep2.visibility = if (targetStepIndex == 1) View.VISIBLE else View.GONE
        binding.buttonsContainerStep3.visibility = if (targetStepIndex == 2) View.VISIBLE else View.GONE
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
            showStep(1) // 呼叫新函式來切換到步驟二
        }

        // --- 步驟二 ---
        binding.btnBackToStep1.setOnClickListener {
            showStep(0) // 切換回步驟一
        }

        binding.btnNextToStep3.setOnClickListener {
            itemDescription = binding.etItemDescription.text.toString()
            if (itemDescription.isBlank()) {
                Toast.makeText(requireContext(), "請輸入項目說明", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.tvContextStep3.text = "$ $amount - $itemDescription"
            showStep(2) // 切換到步驟三
        }

        // --- 步驟三 ---
        binding.btnBackToStep2.setOnClickListener {
            showStep(1) // 切換回步驟二
        }

        // --- 步驟三的儲存按鈕，加入更安全的驗證邏輯 ---
        binding.btnSave.setOnClickListener {
            // --- 驗證邏輯區塊 ---
            val checkedChipId = binding.chipGroupCategory.checkedChipId
            if (checkedChipId == View.NO_ID) {
                Toast.makeText(requireContext(), "請選擇一個分類", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 在第一時間就驗證並轉換金額
            val amountDouble = amount.toDoubleOrNull()
            if (amountDouble == null) {
                Toast.makeText(requireContext(), "金額格式不正確，請重新輸入", Toast.LENGTH_SHORT).show()
                showStep(0) // 跳回第一步讓使用者修改
                return@setOnClickListener
            }
            // --- 驗證結束 ---

            val selectedCategory = binding.root.findViewById<Chip>(checkedChipId).text.toString()

            // 將驗證過的資料傳遞下去
            sendDataToNotion(amountDouble, itemDescription, selectedCategory)
        }
    }
    private fun sendDataToNotion(amount: Double, description: String, category: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val notionToken = prefs.getString("key_notion_token", "")
        val notionDbId = prefs.getString("key_notion_db_id", "")

        if (notionToken.isNullOrBlank() || notionDbId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "請先到設定頁面填寫 Notion API 金鑰", Toast.LENGTH_LONG).show()
            return
        }

        val requestBody = NotionPageRequest(
            parent = Parent(databaseId = notionDbId),
            properties = NotionProperties(
                item = TitleProperty(listOf(TitleContent(TextContent(description)))),
                amount = NumberProperty(amount), // 直接使用傳入的 Double
                category = SelectProperty(SelectOption(category))
            )
        )

        val call = ApiClient.notionApiService.createPage(
            token = "Bearer $notionToken",
            requestBody = requestBody
        )

        call.enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Notion API call successful.")
                    sendDataToGoogleSheets(amount, description, category) // 接續呼叫，並傳遞 Double
                    context?.let { Toast.makeText(it, "🎉 記錄成功！", Toast.LENGTH_SHORT).show() }
                    Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 800)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(requireContext(), "寫入 Notion 失敗: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Notion API Error: ${response.code()} - $errorBody")
                }
            }

            override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
                Toast.makeText(requireContext(), "發生網路錯誤: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Network Failure: ", t)
            }
        })
    }
    private fun sendDataToGoogleSheets(amount: Double, description: String, category: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val scriptUrl = prefs.getString("key_google_script_url", "")

        // 2. 檢查網址是否存在
        if (scriptUrl.isNullOrBlank()) {
            Log.e(TAG, "Google Script URL is not set in preferences.")
            return // 如果沒設定，就默默地不執行，只記錄錯誤
        }

        // 3. 建立請求 (內容不變)
        val entry = GoogleSheetEntry(
            item = description,
            category = category,
            amount = amount
        )
        val call = ApiClient.googleSheetsApiService.addEntry(
            url = scriptUrl,
            entry = entry
        )

        // 4. 發送請求 (Callback 內容不變)
        call.enqueue(object : retrofit2.Callback<Unit> {
            override fun onResponse(call: retrofit2.Call<Unit>, response: retrofit2.Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Google Sheets API call successful.")
                } else {
                    Log.e(TAG, "Google Sheets API Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: retrofit2.Call<Unit>, t: Throwable) {
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