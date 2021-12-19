package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Ticker: Serializable {

    @SerializedName("market")
    var marketId: String? = null

    @SerializedName("trade_date")
    var tradeDate: String? = null

    @SerializedName("trade_time")
    var tradeTime: String? = null

    @SerializedName("trade_date_kst")
    var tradeDateKst: String? = null

    @SerializedName("trade_time_kst")
    var tradeTimeKst: String? = null

    @SerializedName("trade_timestamp")
    var tradeTimestamp: Long = 0

    @SerializedName("opening_price")
    var openingPrice: Number? = null

    @SerializedName("high_price")
    var highPrice: Number? = null

    @SerializedName("low_price")
    var lowPrice: Number? = null

    @SerializedName("trade_price")
    var tradePrice: Number? = null

    @SerializedName("prev_closing_price")
    var prevClosingPrice: Number? = null

    @SerializedName("change")
    var change: String? = null

    @SerializedName("change_price")
    var changePrice: Number? = null

    @SerializedName("changeRate")
    var changeRate: Number? = null

    @SerializedName("signed_change_price")
    var signedChangePrice: Number? = null

    @SerializedName("signed_change_rate")
    var signedChangeRate: Number? = null

    @SerializedName("trade_volume")
    var tradeVolume: Number? = null

    @SerializedName("acc_trade_price")
    var accTradePrice: Number? = null

    @SerializedName("acc_trade_price_24h")
    var accTradePrice24h: Number? = null

    @SerializedName("acc_trade_volume")
    var accTradeVolume: Number? = null

    @SerializedName("acc_trade_volume_24h")
    var accTradeVolume24h: Number? = null

    @SerializedName("highest_52_week_price")
    var highest52WeekPrice: Number? = null

    @SerializedName("highest_52_week_date")
    var highest52WeekDate: String? = null

    @SerializedName("lowest_52_week_price")
    var lowest52WeekPrice: Number? = null

    @SerializedName("lowest_52_week_date")
    var lowest52WeekDate: String? = null

    @SerializedName("timestamp")
    var timestamp: Long = 0

    var sumTradePrice: Number? = null
}