package com.ethy.mori.network

// 一個非常簡單的資料類別，對應我們要傳送的欄位
data class GoogleSheetEntry(
    val item: String,
    val category: String,
    val amount: Double
)