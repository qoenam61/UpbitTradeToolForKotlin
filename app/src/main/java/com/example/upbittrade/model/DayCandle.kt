package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class DayCandle: Candle() {

    @SerializedName("prev_closing_price")
    var prevClosingPrice: Number? = null

    @SerializedName("change_price")
    var changePrice: Number? = null

    @SerializedName("change_rate")
    var changeRate: Number? = null

    @SerializedName("converted_trade_price")
    var convertedTradePrice: Number? = null


//    override fun getCenterPrice(): Double {
//        val high = highPrice?.toDouble()
//        val close = tradePrice?.toDouble()
//        val open = openingPrice?.toDouble()
//        val low = lowPrice?.toDouble()
//        return (high!! + close!! + open!! + low!!) / 4
//    }

    private fun getChangedVolumeRate(): Double {
        val prevRate: Double =
            (tradePrice as Double - lowPrice as Double) / lowPrice as Double
        return Math.round(prevRate * 1000).toDouble() / 1000
    }

    override fun compareTo(o: Candle): Int {
        val originalData: Double =
            candleAccTradePrice as Double * this.getChangedVolumeRate()
        val compareData: Double =
            o.candleAccTradePrice as Double * (o as DayCandle).getChangedVolumeRate()

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