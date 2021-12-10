package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class ResponseOrder {
    @SerializedName("uuid")
    var uuid: String? = null

    @SerializedName("side")
    var side: String? = null

    @SerializedName("ord_type")
    var orderType: String? = null

    @SerializedName("price")
    var price: Number? = null

    @SerializedName("avg_price")
    var avgPrice: Number? = null

    @SerializedName("state")
    var state: String? = null

    @SerializedName("market")
    var market: String? = null

    @SerializedName("created_at")
    var created_at: String? = null

    @SerializedName("volume")
    var volume: Number? = null

    @SerializedName("remaining_volume")
    var remainingVolume: Number? = null

    @SerializedName("reserved_fee")
    var reservedFee: Number? = null

    @SerializedName("remaining_fee")
    var remainingFee: Number? = null

    @SerializedName("paid_fee")
    var paid_fee: Number? = null

    @SerializedName("locked")
    var locked: Number? = null

    @SerializedName("executed_volume")
    var executedVolume: Number? = null

    @SerializedName("trades_count")
    var tradesCount: Int? = null
}