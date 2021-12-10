package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class Price {
    @SerializedName("currency")
    private val currency: String? = null

    @SerializedName("price_unit")
    private val priceUnit: String? = null

    @SerializedName("min_total")
    private val minTotal: Number? = null
}