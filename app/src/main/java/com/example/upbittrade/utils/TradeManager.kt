package com.example.upbittrade.utils

import android.util.Log
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.Ticker
import com.example.upbittrade.model.TradeCoinInfo
import java.lang.Double.max
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TradeManager(private val listener: TradeChangedListener) {
    companion object {
        private const val TAG = "TradeManager"
    }

    interface TradeChangedListener {
        fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo)
        fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo, orderType: String, sellPrice: Double?, volume: Double)
    }

    enum class Type {
        POST_BID,
        POST_ASK
    }

    private var type: Type? = null
    private var marketIdList: List<String>? = null

    fun setList(type: Type, list: List<String>?) {
        this.type = type
        marketIdList = list
        FilterList(list).start()
    }

    inner class FilterList(val marketIdList: List<String>?): Thread() {
        var filteredList: List<String>? = null
        override fun run() {
            super.run()

            filteredList = when(type) {
                Type.POST_BID -> {
                    marketIdList?.filter { filterBuyingList(TradeFragment.tradeMonitorMapInfo[it])}
                }

                Type.POST_ASK -> {
                    null
                }
                else -> {
                    null
                }
            }

            filteredList!!.forEach {
                val orderCoinInfo = OrderCoinInfo(TradeFragment.tradeMonitorMapInfo[it]!!)
                if (orderCoinInfo.getBidPrice() != null) {
                    listener.onPostBid(it, orderCoinInfo)
                }
            }
        }
    }

    private fun filterBuyingList(tradeCoinInfo: TradeCoinInfo?): Boolean {
        if (tradeCoinInfo == null) {
            return false
        }

        if (TradeFragment.tradePostMapInfo.containsKey(tradeCoinInfo.marketId)) {
            return false
        }

        // tick count > thresholdTick
        // getTradeInfoPriceRate() > thresholdRate or getTradeInfoPriceRangeRate() > thresholdRangeRate
        // getPriceVolumeRate() > thresholdAvgMinPerAvgDayPriceVolumeRate
        // getBidAskRate() > thresholdBidAskRate
        // getBidAskPriceRate() > thresholdBidAskPriceRate
        if (tradeCoinInfo.tickCount!! > TradeFragment.UserParam.thresholdTick
            && (tradeCoinInfo.getPriceRate() > TradeFragment.UserParam.thresholdRate
                    || tradeCoinInfo.getPriceRangeRate() > TradeFragment.UserParam.thresholdRangeRate)
            && tradeCoinInfo.getAvgAccVolumeRate() > TradeFragment.UserParam.thresholdAccPriceVolumeRate
            && tradeCoinInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
            && tradeCoinInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceVolumeRate
        ) {
            return true
        }
        return false
    }

    fun tacticalToSell(postInfo: OrderCoinInfo, responseOrder: ResponseOrder): OrderCoinInfo {
        val marketId = postInfo.marketId
        val currentPrice = postInfo.currentPrice
        val bidPrice = postInfo.getBidPrice()
        val profitRate = postInfo.getProfitRate()!!
        val maxProfitRate = postInfo.maxProfitRate
        val tickGap = abs(bidPrice!! - currentPrice!!) / postInfo.getTickPrice()!!
        val volume = responseOrder.volume?.toDouble()

        // Take a profit
        if (profitRate >= 0 && maxProfitRate - profitRate > TradeFragment.UserParam.thresholdRate * 0.66
            && tickGap >  getTickThreshold(currentPrice)) {
            val askPrice = (postInfo.highPrice!!.toDouble() + currentPrice.toDouble()) / 2.0

            Log.d(TAG, "[DEBUG] tacticalToSell - Take a profit marketId: $marketId " +
                    "currentPrice: ${TradeFragment.Format.zeroFormat.format(currentPrice)} " +
                    "askPrice: ${TradeFragment.Format.zeroFormat.format(askPrice)} " +
                    "volume: ${TradeFragment.Format.zeroFormat.format(volume)} " +
                    "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                    "maxProfitRate: ${TradeFragment.Format.percentFormat.format(maxProfitRate)} " +
                    "tickGap: ${TradeFragment.Format.nonZeroFormat.format(tickGap)} "
            )

            listener.onPostAsk(marketId!!, postInfo, "limit", Utils().convertPrice(askPrice), volume!!)
        } else {
            // Stop a loss
            val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice
            val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice
            val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice
            val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice
            val sign: Boolean = closePrice!!.toDouble() - openPrice!!.toDouble() >= 0.0

            if (profitRate < TradeFragment.UserParam.thresholdRate * -0.66
                && tickGap > getTickThreshold(currentPrice)
            ) {

                val highTail: Double = (highPrice!!.toDouble() - closePrice.toDouble()
                    .coerceAtLeast(openPrice.toDouble()))

                val lowTail: Double = (openPrice.toDouble()
                    .coerceAtMost(closePrice.toDouble()) - lowPrice!!.toDouble())

                val body: Double = abs(closePrice.toDouble() - openPrice.toDouble())

                val length: Double = highTail + lowTail + body
                var askPrice = 0.0

                when {
                    //Market
                    !sign && (body + highTail) / length > 0.8 -> {
                        listener.onPostAsk(marketId!!, postInfo, "market", null, volume!!)
                    }

                    // HHCO
                    !sign && lowTail / length > 0.5 -> {
                        askPrice = Utils().convertPrice(
                            sqrt(
                                (highPrice.toDouble().pow(2.0) + highPrice.toDouble().pow(2.0)
                                        + closePrice.toDouble().pow(2.0) + openPrice.toDouble()
                                    .pow(2.0)) / 4
                            )
                        )!!.toDouble()
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }

                    else -> {
                        //HCO
                        askPrice = Utils().convertPrice(
                            sqrt(
                                (highPrice.toDouble().pow(2.0) + closePrice.toDouble().pow(2.0)
                                        + openPrice.toDouble().pow(2.0)) / 3
                            )
                        )!!.toDouble()
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }
                }
                Log.d(TAG, "[DEBUG] tacticalToSell Stop a loss - marketId: $marketId " +
                            "currentPrice: ${TradeFragment.Format.zeroFormat.format(currentPrice)} " +
                            "sellPrice: ${TradeFragment.Format.zeroFormat.format(askPrice)} " +
                            "volume: ${TradeFragment.Format.zeroFormat.format(volume)} " +
                            "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                            "maxProfitRate: ${
                                TradeFragment.Format.percentFormat.format(
                                    maxProfitRate
                                )
                            } " +
                            "tickGap: ${TradeFragment.Format.nonZeroFormat.format(tickGap)} ")
            }
        }
        return postInfo
    }

    private fun getTickThreshold(price: Double): Double {
        val baseTick = TradeFragment.UserParam.thresholdTickGap
        return when {
            price  < 1000 -> {
                baseTick
            }
            price  < 10000 -> {
                // 5
                baseTick * 1.5
            }
            price  < 100000 -> {
                // 10
                baseTick * 2
            }
            price  < 1000000 -> {
                // 50, 100
                if (price  < 500000) {
                    baseTick * 2.5
                } else {
                    baseTick * 3
                }
            }
            price  < 10000000 -> {
                // 1000
                baseTick * 4
            }
            price  < 100000000 -> {
                // 1000
                baseTick * 5
            }
            else -> TradeFragment.UserParam.thresholdTickGap
        }
    }
}