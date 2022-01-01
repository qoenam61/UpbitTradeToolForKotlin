package com.example.upbittrade.model

import com.example.upbittrade.utils.Utils
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OrderCoinInfo: TradeCoinInfo {
    enum class State {
        READY,
        BUYING,
        BUY,
        SELLING,
        SELL,
        DELETE
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

    constructor(orderCoinInfo: OrderCoinInfo): this(orderCoinInfo as TradeCoinInfo) {
        this.state = orderCoinInfo.state
        this.registerTime = orderCoinInfo.registerTime
        this.tradeBidTime = orderCoinInfo.tradeBidTime
        this.tradeAskTime = orderCoinInfo.tradeAskTime
        this.currentTime = orderCoinInfo.currentTime
        this.maxProfitRate = orderCoinInfo.maxProfitRate
        this.volume = orderCoinInfo.volume
        this.currentPrice = orderCoinInfo.currentPrice
        this.askPrice = orderCoinInfo.askPrice
    }

    var state: State = State.READY

    var registerTime: Long? = null
    var tradeBidTime: Long? = null
    var tradeAskTime: Long? = null
    var currentTime: Long? = null

    var maxPrice: Double? = null
    var maxProfitRate: Double = 0.0

    var volume: Double? = null

    var currentPrice: Double? = null
    var askPrice: Double? = null
    var type: Int? = null

    fun getBidPrice(): Double? {
        val highTail: Double = (highPrice!!.toDouble() - closePrice!!.toDouble()
            .coerceAtLeast(openPrice!!.toDouble()))

        val lowTail: Double =
            (openPrice!!.toDouble().coerceAtMost(closePrice!!.toDouble()) - lowPrice!!.toDouble())

        val body: Double = abs(closePrice!!.toDouble() - openPrice!!.toDouble())

        val length: Double = highTail + lowTail + body

        val sign = closePrice!!.toDouble() - openPrice!!.toDouble() >= 0.0

        return when {
            body / length == 1.0 -> {
                if (sign) {
                    Utils().convertPrice(
                        sqrt(
                            (closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    ) / 2
                        )
                    )!!.toDouble()
                } else {
                    null
                }
            }

            body / length > 0.5 && lowTail > highTail-> {
                if (sign) {
                    Utils().convertPrice(
                        sqrt(
                            (highPrice!!.toDouble().pow(2.0)
                                    + closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 4
                        )
                    )!!.toDouble()
                } else {
                    null
                }
            }

            body / length > 0.5 && lowTail <= highTail-> {
                if (sign) {
                    Utils().convertPrice(
                        sqrt(
                            (closePrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 2
                        )
                    )!!.toDouble()
                } else {
                    null
                }
            }

            body / length <= 0.5 && lowTail > highTail-> {
                if (sign) {
                    Utils().convertPrice(
                        sqrt(
                            (closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 3
                        )
                    )
                } else {
                    null
                }
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
        if (tradeBidTime == null || currentTime == null) {
            return null
        }
        return currentTime!!.minus(tradeBidTime!!)
    }
}