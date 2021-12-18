package com.example.upbittrade.model

import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.roundToInt

open class TradeCoinInfo {
    var marketId: String? = null
    var tickCount: Int? = null
    var timestamp: Long? = null
    var highPrice: Number? = null
    var lowPrice: Number? = null
    var openPrice: Number? = null
    var closePrice: Number? = null
    var accPriceVolume: Double? = 0.0
    var avgAccPriceVolume: Double? = 0.0
    var dayChangeRate: Double? = 0.0
    var bid: Int? = 0
    var ask: Int? = 0
    var bidPriceVolume: Double? = 0.0
    var askPriceVolume: Double? = 0.0

    constructor()

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
        avgAccPriceVolume: Double?,
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
        this.avgAccPriceVolume = avgAccPriceVolume
        this.dayChangeRate = changeRate
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
        dayChangeRate: Double?,
        bid: Int?,
        ask: Int?,
        bidPriceVolume: Double?,
        askPriceVolume: Double?
    ) {
        this.marketId = marketId
        this.tickCount = tickCount
        this.timestamp = timestamp
        this.highPrice = highPrice
        this.lowPrice = lowPrice
        this.openPrice = openPrice
        this.closePrice = closePrice
        this.accPriceVolume = accPriceVolume
        this.avgAccPriceVolume = avgPriceVolumePerDayMin
        this.dayChangeRate = dayChangeRate
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

    fun getAvgAccVolumeRate(): Double {
        if (accPriceVolume == null || avgAccPriceVolume == null) {
            return 0.0
        }
        return accPriceVolume!! / avgAccPriceVolume!!
    }

    fun getPriceRangeRate(): Double {
        if (lowPrice == null || highPrice == null) {
            return 0.0
        }
        val diff = highPrice!!.toDouble().minus(lowPrice!!.toDouble())
        return diff.div(lowPrice!!.toDouble())
    }

    fun getBidAskRate(): Double {
        if (bid == null || ask == null || (bid == 0 && ask ==0)) {
            return 0.0
        }
        return (bid!!.toDouble() / (bid!!.toDouble() + ask!!.toDouble()))
    }

    fun getBidAskPriceRate(): Double {
        if (bidPriceVolume == null || askPriceVolume == null || (bidPriceVolume == 0.0 && askPriceVolume == 0.0)) {
            return 0.0
        }
        return (bidPriceVolume!!.toDouble() / (bidPriceVolume!!.toDouble() + askPriceVolume!!.toDouble()))
    }
/*

    var tickPrice: Double = when {
        closePrice!!.toDouble() < 10 -> {
            0.01
        }
        closePrice!!.toDouble()  < 100 -> {
            0.1
        }
        closePrice!!.toDouble()  < 1000 -> {
            1.0
        }
        closePrice!!.toDouble()  < 10000 -> {
            // 5
            5.0
        }
        closePrice!!.toDouble()  < 100000 -> {
            // 10
            10.0
        }
        closePrice!!.toDouble()  < 1000000 -> {
            // 50, 100
            if (closePrice!!.toDouble()  < 500000) {
                50.0
            } else {
                100.0
            }
        }
        closePrice!!.toDouble()  < 10000000 -> {
            // 1000
            1000.0
        }
        closePrice!!.toDouble()  < 100000000 -> {
            // 1000
            1000.0
        }
        else -> 0.0
    }
*/

    fun convertPrice(price: Double): Double? {
        val mFormatUnder10 = DecimalFormat("#.##")
        val mFormatUnder100 = DecimalFormat("##.#")
        val mFormatUnder1_000 = DecimalFormat("###")
        val mFormatUnder10_000 = DecimalFormat("####")
        val mFormatUnder100_000 = DecimalFormat("#####")
        val mFormatUnder1_000_000 = DecimalFormat("######")
        val mFormatUnder10_000_000 = DecimalFormat("#######")
        val mFormatUnder100_000_000 = DecimalFormat("########")
        var result: String? = null
        var priceResult = 0.0
        when {
            price < 10 -> {
                priceResult = floor(price * 100) / 100
                result = mFormatUnder10.format(priceResult)
            }
            price < 100 -> {
                priceResult = floor(price * 10) / 10
                result = mFormatUnder100.format(priceResult)
            }
            price < 1000 -> {
                priceResult = floor(price)
                result = mFormatUnder1_000.format(priceResult)
            }
            price < 10000 -> {
                // 5
                val extra = ((price % 10 * 2 / 10).roundToInt() * 5).toDouble()
                priceResult = floor(price / 10) * 10 + extra
                result = mFormatUnder10_000.format(priceResult)
            }
            price < 100000 -> {
                // 10
                val extra = ((price % 100 / 100).roundToInt() * 100).toDouble()
                priceResult = floor(price / 100) * 100 + extra
                result = mFormatUnder100_000.format(priceResult)
            }
            price < 1000000 -> {
                // 50, 100
                var extra = 0.0
                extra = if (price < 500000) {
                    (Math.round(price % 100 * 2 / 100) * 50).toDouble()
                } else {
                    (Math.round(price % 100 / 100) * 100).toDouble()
                }

                priceResult = floor(price / 100) * 100 + extra
                result = mFormatUnder1_000_000.format(priceResult)
            }
            price < 10000000 -> {
                // 1000
                val extra = ((price % 1000 / 1000).roundToInt() * 1000).toDouble()
                priceResult = floor(price / 1000) * 1000 + extra
                result = mFormatUnder10_000_000.format(priceResult)
            }
            price < 100000000 -> {
                // 1000
                val extra = ((price % 1000 / 1000).roundToInt() * 1000).toDouble()
                priceResult = floor(price / 1000) * 1000 + extra
                result = mFormatUnder100_000_000.format(priceResult)
            }
        }
        return result?.toDouble()
    }
}