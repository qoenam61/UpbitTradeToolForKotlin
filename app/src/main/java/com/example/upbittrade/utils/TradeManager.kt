package com.example.upbittrade.utils

import android.util.Log
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.fragment.TradeFragment.Companion.marketTrend
import com.example.upbittrade.fragment.TradeFragment.Companion.tradeMapInfo
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.TradeCoinInfo
import java.lang.Double.max
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class TradeManager(private val listener: TradeChangedListener) {
    companion object {
        private const val TAG = "TradeManager"
    }

    interface TradeChangedListener {
        fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo)
        fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo, orderType: String, askPrice: Double?, volume: Double)
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
        override fun run() {
            super.run()
            val filteredList = when(type) {
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
                if (!TradeFragment.tradePostMapInfo.containsKey(it)) {
                    val orderCoinInfo = OrderCoinInfo(TradeFragment.tradeMonitorMapInfo[it]!!)
                    if (orderCoinInfo.getBidPrice() != null) {
                        listener.onPostBid(it, orderCoinInfo)
                    }
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

        val marketId = tradeCoinInfo.marketId
        val highPrice = tradeCoinInfo.highPrice!!.toDouble()
        val lowPrice = tradeCoinInfo.lowPrice!!.toDouble()
        val tickGap = abs(highPrice - lowPrice) / tradeCoinInfo.getTickPrice()!!
        val dayChangeRate = tradeMapInfo[marketId]!!.last().getDayChangeRate()

        // tick count > thresholdTick
        // getTradeInfoPriceRate() > thresholdRate or getTradeInfoPriceRangeRate() > thresholdRangeRate
        // getPriceVolumeRate() > thresholdAvgMinPerAvgDayPriceVolumeRate
        // getBidAskRate() > thresholdBidAskRate
        // getBidAskPriceRate() > thresholdBidAskPriceRate
        if (tradeCoinInfo.tickCount!! > TradeFragment.UserParam.thresholdTick
            && (tradeCoinInfo.getPriceRate() > TradeFragment.UserParam.thresholdRate
                    || tradeCoinInfo.getPriceRangeRate() > TradeFragment.UserParam.thresholdRangeRate)
            && tickGap > getTickThreshold(tradeCoinInfo.closePrice!!.toDouble())
            && marketTrend != null && (dayChangeRate - marketTrend!!) > TradeFragment.UserParam.thresholdRate * 0.66
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
        val maxPrice = postInfo.maxPrice!!
        val tickGap = abs(bidPrice!! - currentPrice!!) / postInfo.getTickPrice()!!
        val volume = responseOrder.volume?.toDouble()

        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!
        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!
        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!
        val sign: Boolean = closePrice.toDouble() - openPrice.toDouble() >= 0.0

        val bidAskRate = TradeFragment.tradeMonitorMapInfo[marketId]?.getBidAskRate()!!
        val bidAskPriceRate = TradeFragment.tradeMonitorMapInfo[marketId]?.getBidAskPriceRate()!!

        when {
            // Take a profit
            profitRate >= 0 && maxProfitRate - profitRate > TradeFragment.UserParam.thresholdRate * 0.66
                    && tickGap > getTickThreshold(currentPrice)
    //            && bidAskRate <= TradeFragment.UserParam.thresholdBidAskRate
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate -> {
                val askPrice = Utils().convertPrice(
                    sqrt(
                        (maxPrice.pow(2.0)
                                + maxPrice.pow(2.0)
                                + highPrice.toDouble().pow(2.0)
                                + max(closePrice.toDouble().pow(2.0), openPrice.toDouble().pow(2.0))
                                ) / 4
                    )
                )

                Log.d(TAG, "[DEBUG] tacticalToSell - Take a profit marketId: $marketId " +
                        "currentPrice: ${TradeFragment.Format.zeroFormat.format(currentPrice)} " +
                        "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                        "askPrice: ${TradeFragment.Format.zeroFormat.format(askPrice)} " +
                        "volume: ${if (volume == null) null else TradeFragment.Format.zeroFormat.format(volume)} " +
                        "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                        "maxProfitRate: ${TradeFragment.Format.percentFormat.format(maxProfitRate)} " +
                        "tickGap: ${TradeFragment.Format.nonZeroFormat.format(tickGap)} "
                )

                listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
            }

            // Stop a loss
            (profitRate < TradeFragment.UserParam.thresholdRate * -0.66
                    && tickGap > getTickThreshold(currentPrice)
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate)
            || (profitRate >= 0
                    && bidAskRate <= TradeFragment.UserParam.thresholdBidAskRate
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate) -> {
                val highTail: Double = (highPrice.toDouble() - closePrice.toDouble()
                    .coerceAtLeast(openPrice.toDouble()))

                val lowTail: Double = (openPrice.toDouble()
                    .coerceAtMost(closePrice.toDouble()) - lowPrice.toDouble())

                val body: Double = abs(closePrice.toDouble() - openPrice.toDouble())

                val length: Double = highTail + lowTail + body
                var askPrice: Double? = null
                val askType: Int?

                when {
                    //Market
                    body / length == 1.0 -> {
                        askType = 0
                        if (sign) {
                            askPrice = Utils().convertPrice(
                                sqrt(
                                    (maxPrice.pow(2.0)
                                            + highPrice.toDouble().pow(2.0)
                                            ) / 2
                                )
                            )
                            listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                        } else {
                            listener.onPostAsk(marketId!!, postInfo, "market", null, volume!!)
                        }
                    }

                    body / length > 0.5 && lowTail > highTail-> {
                        if (sign) {
                            askType = 1
                            askPrice = Utils().convertPrice(
                                sqrt(
                                    (maxPrice.pow(2.0)
                                            + highPrice.toDouble().pow(2.0)
                                            + closePrice.toDouble().pow(2.0)
                                            ) / 3
                                )
                            )
                        } else {
                            askType = 2
                            askPrice = Utils().convertPrice(
                                sqrt(
                                    (highPrice.toDouble().pow(2.0)
                                            + closePrice.toDouble().pow(2.0)
                                            + openPrice.toDouble().pow(2.0)
                                            ) / 3
                                )
                            )
                        }
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }

                    body / length > 0.5 && lowTail <= highTail-> {
                        if (sign) {
                            askType = 3
                            askPrice = Utils().convertPrice(
                                sqrt(
                                    (maxPrice.pow(2.0)
                                            + highPrice.toDouble().pow(2.0)
                                            + closePrice.toDouble().pow(2.0)
                                            + openPrice.toDouble().pow(2.0)
                                            ) / 4
                                )
                            )
                        } else {
                            askType = 4
                            askPrice = Utils().convertPrice(
                                sqrt(
                                    (highPrice.toDouble().pow(2.0)
                                            + closePrice.toDouble().pow(2.0)
                                            + openPrice.toDouble().pow(2.0)
                                            + lowPrice.toDouble().pow(2.0)
                                            ) / 4
                                )
                            )
                        }
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }

                    body / length <= 0.5 && lowTail > highTail-> {
                        askType = 5
                        askPrice = Utils().convertPrice(
                            sqrt(
                                (maxPrice.pow(2.0)
                                        + highPrice.toDouble().pow(2.0)
                                        + closePrice.toDouble().pow(2.0)
                                        + openPrice.toDouble().pow(2.0)
                                        + lowPrice.toDouble().pow(2.0)
                                        ) / 5
                            )
                        )
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }

                    body / length <= 0.5 && lowTail <= highTail-> {
                        askType = 6
                        askPrice = Utils().convertPrice(
                            sqrt(
                                (highPrice.toDouble().pow(2.0)
                                        + closePrice.toDouble().pow(2.0)
                                        ) / 2
                            )
                        )
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }

                    else -> {
                        askPrice = if (closePrice.toDouble() >= openPrice.toDouble()) {
                            askType = 7
                            Utils().convertPrice(
                                sqrt(
                                    (maxPrice.pow(2.0)
                                            +highPrice.toDouble().pow(2.0)
                                            +closePrice.toDouble().pow(2.0)
                                            ) / 3
                                )
                            )
                        } else {
                            askType = 8
                            Utils().convertPrice(
                                sqrt(
                                    (maxPrice.pow(2.0)
                                            +highPrice.toDouble().pow(2.0)
                                            +closePrice.toDouble().pow(2.0)
                                            +openPrice.toDouble().pow(2.0)
                                            ) / 4
                                )
                            )
                        }
                        listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
                    }
                }
                Log.d(TAG, "[DEBUG] tacticalToSell Stop a loss $askType - marketId: $marketId " +
                        "currentPrice: ${TradeFragment.Format.zeroFormat.format(currentPrice)} " +
                        "askPrice: ${
                            if (askPrice == null)
                                null
                            else
                                TradeFragment.Format.zeroFormat.format(askPrice)} " +
                        "volume: ${TradeFragment.Format.zeroFormat.format(volume)} " +
                        "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                        "maxProfitRate: ${
                            TradeFragment.Format.percentFormat.format(
                                maxProfitRate
                            )
                        } " +
                        "tickGap: ${TradeFragment.Format.nonZeroFormat.format(tickGap)} ")

            }

            //Expired
            postInfo.getBuyDuration() != null
                    && postInfo.getBuyDuration()!! > TradeFragment.UserParam.monitorTime * 5
                    && tickGap <= getTickThreshold(currentPrice)
                    && postInfo.tickCount!! <= TradeFragment.UserParam.thresholdTick * 1.5
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate -> {

                //HCO
                val askPrice = Utils().convertPrice(
                    sqrt(
                        (highPrice.toDouble().pow(2.0)
                                + max(closePrice.toDouble().pow(2.0), openPrice.toDouble().pow(2.0))) / 2
                    )
                )
                listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)

                Log.d(TAG, "[DEBUG] tacticalToSell time expired $type - marketId: $marketId " +
                        "currentPrice: ${TradeFragment.Format.zeroFormat.format(currentPrice)} " +
                        "askPrice: $askPrice " +
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
        val rate = TradeFragment.UserParam.thresholdRate * 100 * 0.66
        val result: Double
         when {
             price < 0.1 -> {
                 //0.0001
                 result = baseTick * 2 * rate * price
             }
             price < 1 -> {
                 //0.001
                 result = baseTick * 2 * rate * price
             }
            price < 10 -> {
                //0.01
                result = baseTick * 2 * rate * (price / 10)
            }
            price  < 100 -> {
                //0.1
                result = baseTick * 2 * rate * (price / 100)
            }
            price  < 1000 -> {
                //1
                result = baseTick * 2 * rate * (price / 1000)
            }
            price  < 10000 -> {
                // 5
                result = baseTick * 4 * rate * (price / 10000)
            }
            price  < 100000 -> {
                // 10
                result = baseTick * 20 * rate * (price / 100000)
            }
            price  < 1000000 -> {
                // 50, 100
                result = if (price  < 500000) {
                    baseTick * 20 * rate * (price / 500000)
                } else {
                    baseTick * 20 * rate * (price / 1000000)
                }
            }
            price  < 10000000 -> {
                // 1000
                result = baseTick * 20 * rate * (price / 10000000)
            }
            price  < 100000000 -> {
                // 1000
                result = baseTick * 200 * rate * (price / 10000000)
            }
            else ->
                result = TradeFragment.UserParam.thresholdTickGap
        }
        return result
    }
}