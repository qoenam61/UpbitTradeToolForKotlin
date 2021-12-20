package com.example.upbittrade.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OrderCoinInfo: TradeCoinInfo {
    enum class State {
        READY,
        WAIT,
        BUY,
        SELL
    }

    constructor(tradeCoinInfo: TradeCoinInfo):
            super(
                tradeCoinInfo.marketId,
                tradeCoinInfo.tickCount,
                tradeCoinInfo.timestamp,
                tradeCoinInfo.highPrice,
                tradeCoinInfo.lowPrice,
                tradeCoinInfo.openPrice,
                tradeCoinInfo.closePrice,
                tradeCoinInfo.accPriceVolume,
                tradeCoinInfo.avgAccPriceVolume,
                tradeCoinInfo.dayChangeRate,
                tradeCoinInfo.bid,
                tradeCoinInfo.ask,
                tradeCoinInfo.bidPriceVolume,
                tradeCoinInfo.askPriceVolume)

    var state: State = State.READY

    var currentPrice: Double? = null

    var registerTime: Long? = null
    var tradeBuyTime: Long? = null
    var currentTime: Long? = null

    var maxProfitRate: Double = 0.0
    var maxPrice: Double = 0.0

    fun getBidPrice(): Double? {
        val highTail: Double = (highPrice!!.toDouble() - closePrice!!.toDouble()
            .coerceAtLeast(openPrice!!.toDouble()))

        val lowTail: Double = (openPrice!!.toDouble().coerceAtMost(closePrice!!.toDouble()) - lowPrice!!.toDouble())

        val body: Double = abs(closePrice!!.toDouble() - openPrice!!.toDouble())

        val length: Double = highTail + lowTail + body

        return when {
            body / length == 1.0 -> {
                // type0 : HHCO
                convertPrice(sqrt(
                    (highPrice!!.toDouble().pow(2.0) + highPrice!!.toDouble().pow(2.0)
                        + closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)) / 4)
                )!!.toDouble()
            }

            body / length > 0.9 -> {
                // type1 : HCO
                convertPrice(
                    sqrt(
                        (highPrice!!.toDouble().pow(2.0) + closePrice!!.toDouble().pow(2.0)
                        + openPrice!!.toDouble().pow(2.0)) / 3)
                )!!.toDouble()
            }

            body / length > 0.8 -> {
                // type2 : HCOL
                convertPrice(sqrt(
                    (highPrice!!.toDouble().pow(2.0) + closePrice!!.toDouble().pow(2.0)
                        + openPrice!!.toDouble().pow(2.0) + lowPrice!!.toDouble().pow(2.0)) / 4)
                )!!.toDouble()
            }

            (body + lowTail) / length == 1.0 -> {
                // type3 : COL
                convertPrice(sqrt(
                    (closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)
                        + lowPrice!!.toDouble().pow(2.0)) / 3)
                )!!.toDouble()
            }

            (body + lowTail) / length > 0.8 -> {
                // type4 : COLL
                convertPrice(sqrt(
                    (closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)
                        + lowPrice!!.toDouble().pow(2.0) + lowPrice!!.toDouble().pow(2.0)) / 4)
                )!!.toDouble()
            }

            else -> null
        }
    }

    fun getProfit(): Double? {
        val bidPrice = getBidPrice()
        if (currentPrice == null || bidPrice == null) {
            return null
        }
        return currentPrice!! - bidPrice
    }

    fun getProfitRate(): Double? {
        val profit = getProfit()
        val bidPrice = getBidPrice()
        if (profit == null || bidPrice == null) {
            return null
        }
        return profit / bidPrice
    }

    fun getRegisterDuration(): Long? {
        if (registerTime == null || currentTime == null) {
            return null
        }
        return currentTime!!.minus(registerTime!!)
    }

    fun getBuyDuration(): Long? {
        if (tradeBuyTime == null || currentTime == null) {
            return null
        }
        return currentTime!!.minus(tradeBuyTime!!)
    }

    fun getAskPrice(): Double {
        return 0.0
    }
}