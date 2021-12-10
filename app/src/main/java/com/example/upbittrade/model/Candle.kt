package com.example.upbittrade.model

import android.util.Log
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

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

//    open fun getCenterPrice(): Double? {
//        var result = tradePrice?.toDouble()
//        highPrice?.toDouble()?.let { result?.plus(it) }
//        lowPrice?.toDouble()?.let { result?.plus(it) }
//        openingPrice?.toDouble()?.let { result?.plus(it) }
//
//        return result?.div(4)
//    }

    fun getCenterPrice(): Double {
        val high = highPrice?.toDouble()
        val close = tradePrice?.toDouble()
        val open = openingPrice?.toDouble()
        val low = lowPrice?.toDouble()
        return (high!! + close!! + open!! + low!!) / 4
    }

/*    fun getTradePrice() {
        var total: Double? = lowPrice?.toDouble()?.let { highPrice?.toDouble()?.minus(it) }

        var maxValue: Double? = tradePrice?.toDouble()?.let { openingPrice?.toDouble()?.let { it1 ->
            max(it,it1)
        } }

        var minValue: Double? = tradePrice?.toDouble()?.let { openingPrice?.toDouble()?.let { it1 ->
            min(it,it1)
        } }

        var upper: Double? = maxValue?.let { highPrice?.toDouble()?.minus(it) }
        var lower: Double? = lowPrice?.toDouble()?.let { minValue?.minus(it) }

        Log.d("TAG",
            "[DEBUG] getTradePrice - highPrice: $highPrice lowPrice: $lowPrice tradePrice: $tradePrice openingPrice: $openingPrice"
        )
        Log.d("TAG",
            "[DEBUG] getTradePrice - total: $total maxValue: $maxValue minValue: $minValue upper: $upper lower: $lower"
        )

    }*/

    fun getTradePrice(): Double {
        var total: Double = highPrice!!.toDouble() - lowPrice!!.toDouble()
        var upper: Double = highPrice!!.toDouble() - max(tradePrice!!.toDouble(), openingPrice!!.toDouble())
        var lower: Double = min(tradePrice!!.toDouble(), openingPrice!!.toDouble()) - lowPrice!!.toDouble()

        var result: Double = 0.0
        result += candleAccTradePrice!!.toDouble() * (lower / total)
        result -= candleAccTradePrice!!.toDouble() * (upper / total)
        result += candleAccTradePrice!!.toDouble() * ((tradePrice!!.toDouble() - openingPrice!!.toDouble()) / total)

        Log.d("TAG",
            "[DEBUG] getTradePrice - highPrice: $highPrice lowPrice: $lowPrice tradePrice: $tradePrice openingPrice: $openingPrice"
        )
        Log.d("TAG",
            "[DEBUG] getTradePrice - total: $total upper: $upper lower: $lower result: $result"
        )
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
}