package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class TradeInfo {
    @SerializedName("market")
    var marketId: String? = null

    @SerializedName("trade_date_utc")
    var tradeDateUtc: String? = null

    @SerializedName("trade_time_utc")
    var tradeTimeUtc: String? = null

    @SerializedName("timestamp")
    var timestamp: Long = 0

    @SerializedName("trade_price")
    var tradePrice: Number? = null

    @SerializedName("trade_volume")
    var tradeVolume: Number? = null

    @SerializedName("prev_closing_price")
    var prevClosingPrice: Number? = null

    @SerializedName("change_price")
    var changePrice: Number? = null

    @SerializedName("ask_bid")
    var askBid: String? = null

    @SerializedName("sequential_id")
    var sequentialId: Long = 0

    fun getPriceVolume(): Double {
        return tradePrice!!.toDouble() * tradeVolume!!.toDouble()
    }

    fun getDayChangeRate(): Double {
        if (tradePrice == null || prevClosingPrice == null) {
            return 0.0
        }
        val diff = tradePrice!!.toDouble() - prevClosingPrice!!.toDouble()
        return diff / prevClosingPrice!!.toDouble()
    }
}