package com.example.upbittrade.api

import okhttp3.Request

class TradeInfoRetrofit(accessKey: String, secretKey: String)
    : DefaultRetrofit(accessKey, secretKey) {
    override fun getAuthToken(): String {
        return ""
    }

    override fun changedRequest(origin: Request): Request {
        return origin.newBuilder()
            .header("Accept", "application/json")
            .build()
    }
}