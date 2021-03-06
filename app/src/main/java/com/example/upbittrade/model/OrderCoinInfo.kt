package com.example.upbittrade.model

import com.example.upbittrade.utils.BidPrice

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
        this.closePrice = orderCoinInfo.closePrice
        this.bidPrice = orderCoinInfo.bidPrice
        this.askPrice = orderCoinInfo.askPrice
        this.paidFee = orderCoinInfo.paidFee
        this.reservedFee = orderCoinInfo.reservedFee
    }

    var state: State = State.READY

    var orderTime: Long? = null
    var registerTime: Long? = null
    var tradeBidTime: Long? = null
    var tradeAskTime: Long? = null
    var currentTime: Long? = null

    var maxPrice: Double? = null
    var minPrice: Double? = null
    var maxProfitRate: Double? = null

    var volume: Double? = null

    var askPrice: Double? = null
    var type: Int? = null

    var bidPrice: BidPrice? = null

    var reservedFee: Double? = null
    var paidFee: Double? = null

    fun getProfit(): Double {
        if (volume == null) {
            return 0.0
        }
        askPrice?.let {
            return getProfit(it * volume!!)
        }
        closePrice?.let {
            return getProfit(it.toDouble() * volume!!)
        }
        return 0.0
    }

    fun getProfit(priceVolume: Double): Double {
        if (volume == null) {
            return 0.0
        }
        val bidPriceVolume = bidPrice?.price!! * volume!!

        var result = priceVolume - bidPriceVolume

        reservedFee?.let {
            result -= it
        }
        paidFee?.let {
            result -= it
        }
        return result
    }

    fun getProfitRate(): Double {
        askPrice?.let {
            return getProfitRate(it * volume!!)
        }
        closePrice?.let {
            return getProfitRate(it.toDouble() * volume!!)
        }
        return 0.0
    }

    fun getProfitRate(priceVolume: Double): Double {
        if (volume == null) {
            return 0.0
        }
        val profit = getProfit(priceVolume)
        val bidPriceVolume = bidPrice?.price!! * volume!!
        return profit / bidPriceVolume
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