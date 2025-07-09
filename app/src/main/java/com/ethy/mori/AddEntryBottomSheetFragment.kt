package com.ethy.mori

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.ethy.mori.addentry.AddEntryViewModel
import com.ethy.mori.addentry.AddEntryViewModelFactory
import com.ethy.mori.data.AppDatabase
import com.ethy.mori.databinding.FragmentAddEntryBinding
import com.ethy.mori.network.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 請注意：我將 AddEntryBottomSheetFragment 直接放在 com.ethy.mori package 下，
// 如果您之前將它放在了 addentry package，請將第一行的 package 路徑改回 package com.ethy.mori.addentry
class AddEntryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddEntryBinding? = null
    private val binding get() = _binding!!

    // 用來在步驟間傳遞資料的變數
    private var amount: String = ""
    private var itemDescription: String = ""

    // 讀取使用者在設定頁面中輸入的金鑰
    private val prefs by lazy {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
    }
    private val notionToken by lazy { prefs.getString("key_notion_token", "") }
    private val notionDbId by lazy { prefs.getString("key_notion_db_id", "") }
    private val googleScriptUrl by lazy { prefs.getString("key_google_script_url", "") }


    private val addEntryViewModel: AddEntryViewModel by viewModels {
        AddEntryViewModelFactory(AppDatabase.getInstance(requireContext()).categoryDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setupClickListeners()
        observeCategories()
        // 初始化為第一步的狀態
        showStep(1)
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
                        binding.etMainInput.post {
                            binding.etMainInput.requestFocus()
                        }
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

    private fun showStep(step: Int) {
        // 先隱藏所有按鈕容器
        binding.buttonsContainerStep1.visibility = View.GONE
        binding.buttonsContainerStep2.visibility = View.GONE
        binding.buttonsContainerStep3.visibility = View.GONE

        when (step) {
            1 -> {
                binding.tvContext.text = "新增"
                binding.tvMainTitle.text = "花了多少錢？"
                binding.etMainInput.visibility = View.VISIBLE
                binding.etMainInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                binding.etMainInput.hint = "輸入金額"
                binding.etMainInput.setText(amount)
                binding.chipGroupCategory.visibility = View.GONE
                binding.buttonsContainerStep1.visibility = View.VISIBLE
            }
            2 -> {
                binding.tvContext.text = "$ $amount"
                binding.tvMainTitle.text = "用在哪裡呢？"
                binding.etMainInput.visibility = View.VISIBLE
                binding.etMainInput.inputType = InputType.TYPE_CLASS_TEXT
                binding.etMainInput.hint = "輸入說明 (例如：晚餐)"
                binding.etMainInput.setText(itemDescription)
                binding.chipGroupCategory.visibility = View.GONE
                binding.buttonsContainerStep2.visibility = View.VISIBLE
            }
            3 -> {
                binding.tvContext.text = "$ $amount - $itemDescription"
                binding.tvMainTitle.text = "這是什麼類別？"
                binding.etMainInput.visibility = View.GONE
                binding.chipGroupCategory.visibility = View.VISIBLE
                binding.buttonsContainerStep3.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnNextToStep2.setOnClickListener {
            val currentAmount = binding.etMainInput.text.toString()
            if (currentAmount.toDoubleOrNull() == null) {
                Toast.makeText(requireContext(), "請輸入有效的金額", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            this.amount = currentAmount
            showStep(2)
        }
        binding.btnBackToStep1.setOnClickListener {
            this.itemDescription = binding.etMainInput.text.toString()
            showStep(1)
        }
        binding.btnNextToStep3.setOnClickListener {
            val currentDescription = binding.etMainInput.text.toString()
            if (currentDescription.isBlank()) {
                Toast.makeText(requireContext(), "請輸入項目說明", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            this.itemDescription = currentDescription
            showStep(3)
        }
        binding.btnBackToStep2.setOnClickListener {
            showStep(2)
        }
        binding.btnSave.setOnClickListener {
            val checkedChipId = binding.chipGroupCategory.checkedChipId
            if (checkedChipId == View.NO_ID) {
                Toast.makeText(requireContext(), "請選擇一個分類", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedCategory = binding.root.findViewById<Chip>(checkedChipId).text.toString()
            val amountDouble = amount.toDoubleOrNull()
            if (amountDouble != null) {
                sendDataToNotion(amountDouble, itemDescription, selectedCategory)
            } else {
                Toast.makeText(requireContext(), "金額格式錯誤，無法儲存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeCategories() {
        addEntryViewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            binding.chipGroupCategory.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(context)
                chip.text = category.name
                chip.isClickable = true
                chip.isCheckable = true
                binding.chipGroupCategory.addView(chip)
            }
        }
    }

    private fun sendDataToNotion(amount: Double, description: String, category: String) {
        if (notionToken.isNullOrBlank() || notionDbId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "請先到設定頁面填寫 Notion API 金鑰", Toast.LENGTH_LONG).show()
            return
        }
        val requestBody = NotionPageRequest(
            parent = Parent(databaseId = notionDbId!!),
            properties = NotionProperties(
                item = TitleProperty(listOf(TitleContent(TextContent(description)))),
                amount = NumberProperty(amount),
                category = SelectProperty(SelectOption(category))
            )
        )
        val call = ApiClient.notionApiService.createPage(
            token = "Bearer $notionToken",
            requestBody = requestBody
        )
        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Notion API call successful.")
                    sendDataToGoogleSheets(amount, description, category)
                    context?.let { Toast.makeText(it, "🎉 記錄成功！", Toast.LENGTH_SHORT).show() }
                    Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 800)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(requireContext(), "寫入 Notion 失敗: $errorBody", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Notion API Error: ${response.code()} - $errorBody")
                }
            }
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Toast.makeText(requireContext(), "發生網路錯誤: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Network Failure: ", t)
            }
        })
    }

    private fun sendDataToGoogleSheets(amount: Double, description: String, category: String) {
        if (googleScriptUrl.isNullOrBlank()) {
            Log.e(TAG, "Google Script URL is not set in preferences.")
            return
        }
        val entry = GoogleSheetEntry(item = description, category = category, amount = amount)
        val call = ApiClient.googleSheetsApiService.addEntry(url = googleScriptUrl!!, entry = entry)
        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Google Sheets API call successful.")
                } else {
                    Log.e(TAG, "Google Sheets API Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: Call<Unit>, t: Throwable) {
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