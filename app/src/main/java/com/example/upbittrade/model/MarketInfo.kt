package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class MarketInfo {
    @SerializedName("market")
    val market: String? = null

    @SerializedName("korean_name")
    val koreanName: String? = null

    @SerializedName("english_name")
    val englishName: String? = null

    @SerializedName("market_warning")
    val marketWarning: String? = null
}