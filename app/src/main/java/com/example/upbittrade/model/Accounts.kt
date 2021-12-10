package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class Accounts {
    @SerializedName("currency")
    val currency: String? = null

    @SerializedName("balance")
    val balance: Number? = null

    @SerializedName("locked")
    val locked: Number? = null

    @SerializedName("avg_buy_price")
    val avgBuyPrice: Number? = null

    @SerializedName("avg_buy_price_modified")
    val avgBuyPriceModified: Boolean? = null

    @SerializedName("unit_currency")
    val unitCurrency: Number? = null

    override fun toString(): String {
        return "Accounts{" +
                "currency='" + currency + '\'' +
                ", balance='" + balance + '\'' +
                ", locked='" + locked + '\'' +
                ", avgBuyPrice='" + avgBuyPrice + '\'' +
                ", avgBuyPriceModified=" + avgBuyPriceModified +
                ", unitCurrency='" + unitCurrency + '\'' +
                '}'
    }
}