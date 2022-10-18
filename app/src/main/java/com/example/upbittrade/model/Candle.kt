package com.example.upbittrade.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

open class Candle: Serializable, Comparable<Candle> {
    @SerializedName("market")
    var marketId: String? = null

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

    var accPriceVolume = 0.0

    fun get1minAverageTradePrice(totalTime: Int): Double {
        return candleAccTradePrice!!.toDouble() / totalTime
    }

    fun getCenterPrice(): Double {
        val high = highPrice?.toDouble()
        val close = tradePrice?.toDouble()
        val open = openingPrice?.toDouble()
        val low = lowPrice?.toDouble()
        return (high!! + close!! + open!! + low!!) / 4
    }

    fun getTradeVolumePrice(): Double {
        var total: Double = highPrice!!.toDouble() - lowPrice!!.toDouble()
        var upper: Double = highPrice!!.toDouble() - max(tradePrice!!.toDouble(), openingPrice!!.toDouble())
        var lower: Double = min(tradePrice!!.toDouble(), openingPrice!!.toDouble()) - lowPrice!!.toDouble()

        var result: Double = 0.0
        result += candleAccTradePrice!!.toDouble() * (lower / total)
        result -= candleAccTradePrice!!.toDouble() * (upper / total)
        result += candleAccTradePrice!!.toDouble() * ((tradePrice!!.toDouble() - openingPrice!!.toDouble()) / total)

        return result
    }

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

    override fun toString(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
}