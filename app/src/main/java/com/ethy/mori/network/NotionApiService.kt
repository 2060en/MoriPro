package com.ethy.mori.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotionApiService {

    // 定義一個名為 createPageInDatabase 的函式
    // @POST("v1/pages") -> 告訴 Retrofit 這是一個 POST 請求，目標路徑是 "v1/pages"
    @POST("v1/pages")
    fun createPage( // <--- 移除 suspend
        @Header("Authorization") token: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28",
        @Body requestBody: NotionPageRequest
    ): Call<Unit> // <--- 回傳類型改為 Call<Unit>
}