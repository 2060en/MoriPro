package com.ethy.mori.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Notion API 的基礎網址
    private const val NOTION_BASE_URL = "https://api.notion.com/"

    // 建立一個日誌攔截器，可以在 Logcat 中印出詳細的網路請求資訊，方便偵錯
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // BODY 等級會印出最詳細的內容
    }

    // 建立 OkHttpClient，並加入我們的日誌攔截器
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 建立 Retrofit 實例
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(NOTION_BASE_URL) // 設定基礎網址
        .client(httpClient) // 設定我們自訂的 httpClient (為了印日誌)
        .addConverterFactory(GsonConverterFactory.create()) // 設定 JSON 轉換器為 Gson
        .build()

    // 透過 lazy 的方式，在第一次被使用時，才去建立 NotionApiService 的實作
    val notionApiService: NotionApiService by lazy {
        retrofit.create(NotionApiService::class.java)
    }
    // ↓↓↓ 我們為 Google Sheets 建立一個獨立的 Retrofit 實例 ↓↓↓
    // 因為它的 BaseURL 和 Notion 不同 (雖然我們用 @Url 覆蓋了)
    private val googleSheetsRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/") // 這裡的 BaseURL 只是象徵性的
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ↓↓↓ 加入這個新的 lazy 屬性 ↓↓↓
    val googleSheetsApiService: GoogleSheetsApiService by lazy {
        googleSheetsRetrofit.create(GoogleSheetsApiService::class.java)
    }
}