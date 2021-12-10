package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class Candle: Serializable, Comparable<Candle> {
    @SerializedName("market")
    var market: String? = null

    @SerializedName("candle_date_time_utc")
    var candleDateTimeUtc: String? = null

    @SerializedName("candle_date_time_kst")
    var candleDateTimeKst: String? = null

    @SerializedName("opening_price")
    var openingPrice: Number? = null

    @SerializedName("high_price")
    var highPrice: Number? = null

    @SerializedName("low_price")
    var lowPrice: Number? = null

    @SerializedName("trade_price")
    var tradePrice: Number? = null

    @SerializedName("timestamp")
    var timestamp: Long? = null

    @SerializedName("candle_acc_trade_price")
    var candleAccTradePrice: Number? = null

    @SerializedName("candle_acc_trade_volume")
    var candleAccTradeVolume: Number? = null

    @SerializedName("unit")
    var unit: Int? = null

    var changedPrice = 0.0

    var changedRate = 0.0

    override fun compareTo(other: Candle): Int {
        val originalData: Double = candleAccTradePrice as Double
        val compareData: Double = other.candleAccTradePrice as Double

        return when {
            originalData < compareData -> {
                1
            }
            originalData > compareData -> {
                -1
            }
            else -> {
                0
            }
        }
    }
}