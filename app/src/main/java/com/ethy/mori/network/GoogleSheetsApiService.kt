package com.ethy.mori.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface GoogleSheetsApiService {

    // @POST 代表這是一個 POST 請求
    // 因為 Apps Script 的網址是完整的，我們用 @Url 來直接傳入完整網址，
    // 這會覆蓋掉 Retrofit Client 中設定的 Base URL。
    @POST
    fun addEntry(
        @Url url: String,
        @Body entry: GoogleSheetEntry
    ): Call<Unit> // 同樣，我們不關心回傳的內容
}