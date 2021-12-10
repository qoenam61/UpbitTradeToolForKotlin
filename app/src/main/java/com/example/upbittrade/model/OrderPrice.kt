package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class OrderPrice {
    @SerializedName("currency")
    var currency: String? = null

    @SerializedName("balance")
    var balance: Number? = null

    @SerializedName("locked")
    var locked: Number? = null

    @SerializedName("avg_buy_price")
    var avgBuyPrice: Number? = null

    @SerializedName("avg_buy_price_modified")
    var avgBuyPriceModified = false

    @SerializedName("unit_currency")
    var unitCurrency: String? = null
}