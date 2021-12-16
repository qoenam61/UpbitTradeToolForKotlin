package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class ResultTradeInfo {
    var marketId: String? = null
    var tickCount: Int? = null
    var timestamp: Long? = null
    var highPrice: Number? = null
    var lowPrice: Number? = null
    var openPrice: Number? = null
    var closePrice: Number? = null
    var accPriceVolume: Double? = 0.0
    var avgMinPriceVolume: Double? = 0.0
    var changeRate: Double? = 0.0

    constructor(
        marketId: String?,
        tickCount: Int?,
        timestamp: Long?,
        highPrice: Number?,
        lowPrice: Number?,
        openPrice: Number?,
        closePrice: Number?,
        accPriceVolume: Double?,
    ) {
        this.marketId = marketId
        this.tickCount = tickCount
        this.timestamp = timestamp
        this.highPrice = highPrice
        this.lowPrice = lowPrice
        this.openPrice = openPrice
        this.closePrice = closePrice
        this.accPriceVolume = accPriceVolume
    }

    constructor(
        marketId: String?,
        tickCount: Int?,
        timestamp: Long?,
        highPrice: Number?,
        lowPrice: Number?,
        openPrice: Number?,
        closePrice: Number?,
        accPriceVolume: Double?,
        avgMinPriceVolume: Double?,
        changeRate: Double?
    ) {
        this.marketId = marketId
        this.tickCount = tickCount
        this.timestamp = timestamp
        this.highPrice = highPrice
        this.lowPrice = lowPrice
        this.openPrice = openPrice
        this.closePrice = closePrice
        this.accPriceVolume = accPriceVolume
        this.avgMinPriceVolume = avgMinPriceVolume
        this.changeRate = changeRate
    }

    fun getCenterPrice(): Double {
        val high = highPrice?.toDouble()
        val close = closePrice?.toDouble()
        val open = openPrice?.toDouble()
        val low = lowPrice?.toDouble()
        return (high!! + close!! + open!! + low!!) / 4
    }

    fun getPriceVolumeRate(): Double {
        if (accPriceVolume == null || avgMinPriceVolume == null) {
            return 0.0
        }
        return accPriceVolume!! / avgMinPriceVolume!!
    }

    fun getMinPriceRate(): Double {
        if (openPrice == null || closePrice == null) {
            return 0.0
        }
        val diff = closePrice!!.toDouble().minus(openPrice!!.toDouble())
        return diff.div(openPrice!!.toDouble())
    }
}