package com.ethy.mori.network

import com.google.gson.annotations.SerializedName

// 這是最外層的請求包裹
data class NotionPageRequest(
    val parent: Parent,
    val properties: NotionProperties
)

// 定義要寫入哪個資料庫
data class Parent(
    @SerializedName("database_id")
    val databaseId: String
)

// 所有屬性的集合
data class NotionProperties(
    // @SerializedName 確保轉換成 JSON 時，欄位名稱是 Notion API 要求的格式
    @SerializedName("項目")
    val item: TitleProperty,

    @SerializedName("金額")
    val amount: NumberProperty,

    @SerializedName("分類")
    val category: SelectProperty
)

// --- 各種不同屬性的詳細定義 ---

// "標題" 類型的屬性
data class TitleProperty(
    val title: List<TitleContent>
)

data class TitleContent(
    val text: TextContent
)

data class TextContent(
    val content: String
)

// "數字" 類型的屬性
data class NumberProperty(
    val number: Double
)

// "選取" 類型的屬性
data class SelectProperty(
    val select: SelectOption
)

data class SelectOption(
    val name: String
)

