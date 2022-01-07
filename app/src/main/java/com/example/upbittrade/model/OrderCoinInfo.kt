package com.example.upbittrade.model

import android.util.Log
import com.example.upbittrade.utils.Utils
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OrderCoinInfo: TradeCoinInfo {
    companion object {
        const val TAG = "OrderCoinInfo"
    }
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

    var orderTime: Long? = null
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

    var bidType: Int? = null

    fun getBidPrice(): Double? {
        val highTail: Double = (highPrice!!.toDouble() - closePrice!!.toDouble()
            .coerceAtLeast(openPrice!!.toDouble()))

        val lowTail: Double =
            (openPrice!!.toDouble().coerceAtMost(closePrice!!.toDouble()) - lowPrice!!.toDouble())

        val body: Double = abs(closePrice!!.toDouble() - openPrice!!.toDouble())

        val length: Double = highTail + lowTail + body

        val sign = closePrice!!.toDouble() - openPrice!!.toDouble() >= 0.0

        val bidPrice: Double?
        when {
            body / length == 1.0 -> {
                bidPrice = if (sign) {
                    bidType = 0
                    Utils().convertPrice(
                        sqrt(
                            (highPrice!!.toDouble().pow(2.0)
                                    + closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    ) / 3
                        )
                    )
                } else {
                    bidType = 1
                    null
                }
            }

            body / length > 0.5 && lowTail > highTail-> {
                bidPrice = if (sign) {
                    bidType = 2
                    Utils().convertPrice(
                        sqrt(
                            (highPrice!!.toDouble().pow(2.0)
                                    + closePrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 3
                        )
                    )
                } else {
                    bidType = 3
                    null
                }
            }

            body / length > 0.8 && lowTail <= highTail-> {
                bidPrice = if (sign) {
                    bidType = 4
                    Utils().convertPrice(
                        sqrt(
                            (highPrice!!.toDouble().pow(2.0)
                                    + closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 4
                        )
                    )
                } else {
                    bidType = 5
                    null
                }
            }

            body / length <= 0.5 && lowTail > highTail-> {
                bidPrice = if (sign) {
                    bidType = 6
                    Utils().convertPrice(
                        sqrt(
                            (highPrice!!.toDouble().pow(2.0)
                                    + closePrice!!.toDouble().pow(2.0)
                                    + openPrice!!.toDouble().pow(2.0)
                                    + lowPrice!!.toDouble().pow(2.0)
                                    ) / 4
                        )
                    )
                } else {
                    bidType = 7
                    null
                }
            }

            else -> {
                bidType = 9
                bidPrice = null
            }
        }

        return bidPrice
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