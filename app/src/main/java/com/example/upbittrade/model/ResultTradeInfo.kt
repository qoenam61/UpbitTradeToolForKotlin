package com.example.upbittrade.model

class ResultTradeInfo {
    var marketId: String? = null
    var tickCount: Int? = null
    var timestamp: Long? = null
    var highPrice: Number? = null
    var lowPrice: Number? = null
    var openPrice: Number? = null
    var closePrice: Number? = null
    var accPriceVolume: Double? = 0.0
    var avgPriceVolumePerDayMin: Double? = 0.0
    var changeRate: Double? = 0.0
    var bid: Int? = 0
    var ask: Int? = 0
    var bidPriceVolume: Double? = 0.0
    var askPriceVolume: Double? = 0.0

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
        avgPriceVolumePerDayMin: Double?,
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
        this.avgPriceVolumePerDayMin = avgPriceVolumePerDayMin
        this.changeRate = changeRate
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
        avgPriceVolumePerDayMin: Double?,
        changeRate: Double?,
        bid: Int,
        ask: Int,
        bidPriceVolume: Double,
        askPriceVolume: Double
    ) {
        this.marketId = marketId
        this.tickCount = tickCount
        this.timestamp = timestamp
        this.highPrice = highPrice
        this.lowPrice = lowPrice
        this.openPrice = openPrice
        this.closePrice = closePrice
        this.accPriceVolume = accPriceVolume
        this.avgPriceVolumePerDayMin = avgPriceVolumePerDayMin
        this.changeRate = changeRate
        this.bid = bid
        this.ask = ask
        this.bidPriceVolume = bidPriceVolume
        this.askPriceVolume = askPriceVolume
    }

    fun getCenterPrice(): Double {
        val high = highPrice?.toDouble()
        val close = closePrice?.toDouble()
        val open = openPrice?.toDouble()
        val low = lowPrice?.toDouble()
        return (high!! + close!! + open!! + low!!) / 4
    }

    fun getPriceVolumeRate(): Double {
        if (accPriceVolume == null || avgPriceVolumePerDayMin == null) {
            return 0.0
        }
        return accPriceVolume!! / avgPriceVolumePerDayMin!!
    }

    fun getMinPriceRate(): Double {
        if (openPrice == null || closePrice == null) {
            return 0.0
        }
        val diff = closePrice!!.toDouble().minus(openPrice!!.toDouble())
        return diff.div(openPrice!!.toDouble())
    }

    fun getBidAskRate(): Double {
        if (bid == null || ask == null || (bid == 0 && ask ==0)) {
            return 0.0
        }
        return bid!!.div(bid!! + ask!!) .toDouble()
    }

    fun getBidAskPriceRate(): Double {
        if (bid == null || ask == null) {
            return 0.0
        }
        return bidPriceVolume!!.div(bidPriceVolume!! + askPriceVolume!!)
    }
}