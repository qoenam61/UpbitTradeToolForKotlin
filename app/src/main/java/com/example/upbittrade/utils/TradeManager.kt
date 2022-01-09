package com.example.upbittrade.utils

import android.util.Log
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.fragment.TradeFragment.Companion.bidAskTotalAvgRate
import com.example.upbittrade.fragment.TradeFragment.Companion.marketTrend
import com.example.upbittrade.fragment.TradeFragment.Companion.tradeMapInfo
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.TradeCoinInfo
import kotlin.math.abs
import kotlin.math.max
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
                    orderCoinInfo.bidPrice = Utils.getBidPriceCalculate(
                        orderCoinInfo.highPrice!!.toDouble(),
                        orderCoinInfo.lowPrice!!.toDouble(),
                        orderCoinInfo.openPrice!!.toDouble(),
                        orderCoinInfo.closePrice!!.toDouble())
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
                    || tradeCoinInfo.getPriceRangeRate() > TradeFragment.UserParam.thresholdRangeRate
                    || tickGap > getTickThreshold(tradeCoinInfo.closePrice!!.toDouble()))
            && tradeCoinInfo.getAvgAccVolumeRate() > TradeFragment.UserParam.thresholdAccPriceVolumeRate
            && bidAskTotalAvgRate != null && bidAskTotalAvgRate!! > TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9
            && marketTrend != null && (dayChangeRate - marketTrend!!) > TradeFragment.UserParam.thresholdRate * 0.66
            && ((tradeCoinInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
                    && tradeCoinInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceVolumeRate)
                || (tradeCoinInfo.getBidAskPriceRate() - bidAskTotalAvgRate!! > TradeFragment.UserParam.thresholdRate * 0.66))
        ) {
            return true
        }
        return false
    }

    fun tacticalToSell(postInfo: OrderCoinInfo, responseOrder: ResponseOrder): OrderCoinInfo {
        val marketId = postInfo.marketId
        val volume = responseOrder.volume?.toDouble()
        val profitRate = postInfo.getProfitRate()
        val maxProfitRate = postInfo.maxProfitRate

        if (profitRate == null) {
            return postInfo
        }

        val currentPrice = postInfo.closePrice?.toDouble()!!
        val bidPrice = postInfo.bidPrice?.price!!
        val tickGap = abs(bidPrice - currentPrice) / postInfo.getTickPrice()!!

        val bidAskRate = TradeFragment.tradeMonitorMapInfo[marketId]?.getBidAskRate()!!
        val bidAskPriceRate = TradeFragment.tradeMonitorMapInfo[marketId]?.getBidAskPriceRate()!!
        var askPrice: Double? = null

        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!
        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!
        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!
        val sign: Boolean = closePrice.toDouble() - openPrice.toDouble() >= 0.0

        val highTail: Double = (highPrice.toDouble() - closePrice.toDouble()
            .coerceAtLeast(openPrice.toDouble()))

        val lowTail: Double = (openPrice.toDouble()
            .coerceAtMost(closePrice.toDouble()) - lowPrice.toDouble())

        val body: Double = abs(closePrice.toDouble() - openPrice.toDouble())

        val length: Double = highTail + lowTail + body


        when {
            // Take a profit
            (profitRate >= TradeFragment.UserParam.thresholdRate * 0.66
                    && maxProfitRate - profitRate > TradeFragment.UserParam.thresholdRate * 0.66
                    && tickGap > getTickThreshold(currentPrice)
                    && (bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9
                        || (bidAskTotalAvgRate != null && bidAskPriceRate - bidAskTotalAvgRate!! <= TradeFragment.UserParam.thresholdRate * -0.66)))
            || (profitRate >= 0
                    && bidAskRate <= TradeFragment.UserParam.thresholdBidAskRate * 0.9
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9)
            || (profitRate >= 0
                    && bidAskTotalAvgRate != null
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9
                    && bidAskPriceRate - bidAskTotalAvgRate!! <= TradeFragment.UserParam.thresholdRate * -0.66) -> {

                askPrice = getTakeProfitPrice(marketId, postInfo)
            }

            // Stop a loss
            (profitRate < TradeFragment.UserParam.thresholdRate * -0.66
                    && tickGap > getTickThreshold(currentPrice)
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9)
             -> {

                when {
                    body / length == 1.0 -> {
                        askPrice = if (sign) {
                            getStopLossLong(marketId, postInfo)
                        } else {
                            getStopLossMarket(marketId, postInfo, volume!!)
                        }
                    }

                    body / length > 0.5 -> {
                        askPrice = if (sign) {
                            getStopLossLong(marketId, postInfo)
                        } else {
                            getStopLossMinusLong(marketId, postInfo)
                        }
                    }

                    body / length <= 0.5 -> {
                        askPrice = getStopLossShort(marketId, postInfo)
                    }

                    else -> {
                        askPrice = null
                    }
                }
            }

            //Expired
            postInfo.getBuyDuration() != null
                    && postInfo.getBuyDuration()!! > TradeFragment.UserParam.monitorTime * 5
                    && tickGap <= getTickThreshold(currentPrice)
                    && bidAskTotalAvgRate != null
                    && bidAskPriceRate <= TradeFragment.UserParam.thresholdBidAskPriceVolumeRate * 0.9
                    && bidAskPriceRate - bidAskTotalAvgRate!! <= TradeFragment.UserParam.thresholdRate * -0.66 -> {

                askPrice = getExpiredTimePrice(marketId, postInfo)
            }
        }

        if (askPrice != null) {
            Log.d(TAG, "[DEBUG] tacticalToSell - marketId $marketId " +
                    "volume: ${TradeFragment.Format.zeroFormat2.format(volume)} " +
                    "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                    "maxProfitRate: ${TradeFragment.Format.percentFormat.format(maxProfitRate)} " +
                    "tickGap: ${TradeFragment.Format.nonZeroFormat.format(tickGap)} " +
                    "bidAskRate: ${TradeFragment.Format.percentFormat.format(bidAskRate)} " +
                    "bidAskPriceRate: ${TradeFragment.Format.percentFormat.format(bidAskPriceRate)} "
            )
            listener.onPostAsk(marketId!!, postInfo, "limit", askPrice, volume!!)
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

    private fun getTakeProfitPrice(marketId: String?, postInfo: OrderCoinInfo): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
//        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
//        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        var result = Utils.convertPrice(
            sqrt(
                (maxPrice.pow(2.0)
                        + highPrice.pow(2.0)
                        + closePrice.pow(2.0)
                        ) / 3
            )
        )!!

        result = max(highPrice, result)

        Log.d(TAG, "[DEBUG] getTakeProfitPrice - Take a profit marketId: $marketId " +
                "askPrice: ${TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )

        return result
    }

    private fun getStopLossMarket(marketId: String?, postInfo: OrderCoinInfo, volume: Double): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val bidPrice = postInfo.bidPrice?.price!!
//        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
//        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        var result: Double? = null

        if (minPrice == closePrice) {
            listener.onPostAsk(marketId, postInfo, "market", null, volume)
        } else {
            result = Utils.convertPrice(
                sqrt(
                    (bidPrice.pow(2.0)
                            + closePrice.pow(2.0)
                            + lowPrice.pow(2.0)
                            ) / 3
                )
            )!!
        }
        Log.d(TAG, "[DEBUG] getStopLossMarket - Stop a loss marketId: $marketId " +
                "askPrice: ${if (result == null) null else TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )
        return result
    }

    private fun getStopLossLong(marketId: String?, postInfo: OrderCoinInfo): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val bidPrice = postInfo.bidPrice?.price!!
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
//        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
//        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        val result = Utils.convertPrice(
            sqrt(
                (maxPrice.pow(2.0)
                        + bidPrice.pow(2.0)
                        + highPrice.pow(2.0)
                        + closePrice.pow(2.0)
                        ) / 4
            )
        )!!

        Log.d(TAG, "[DEBUG] getStopLossLong - Stop a loss marketId: $marketId " +
                "askPrice: ${TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )

        return result
    }

    private fun getStopLossMinusLong(marketId: String?, postInfo: OrderCoinInfo): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val bidPrice = postInfo.bidPrice?.price!!
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
//        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        val result = Utils.convertPrice(
            sqrt(
                (bidPrice.pow(2.0)
                        + highPrice.pow(2.0)
                        + openPrice.pow(2.0)
                        + closePrice.pow(2.0)
                        ) / 4
            )
        )!!

        Log.d(TAG, "[DEBUG] getStopLossMinusLong - Stop a loss marketId: $marketId " +
                "askPrice: ${TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )

        return result
    }

    private fun getStopLossShort(marketId: String?, postInfo: OrderCoinInfo): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val bidPrice = postInfo.bidPrice?.price!!
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
//        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        val result = Utils.convertPrice(
            sqrt(
                (bidPrice.pow(2.0)
                        + highPrice.pow(2.0)
                        + openPrice.pow(2.0)
                        + closePrice.pow(2.0)
                        ) / 4
            )
        )!!

        Log.d(TAG, "[DEBUG] getStopLossShort - Stop a loss marketId: $marketId " +
                "askPrice: ${TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )

        return result
    }

    private fun getExpiredTimePrice(marketId: String?, postInfo: OrderCoinInfo): Double? {
        if (marketId == null) {
            return null
        }
        val maxPrice = postInfo.maxPrice!!
        val minPrice = postInfo.minPrice!!
        val bidPrice = postInfo.bidPrice?.price!!
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice!!.toDouble()
//        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice!!.toDouble()
//        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice!!.toDouble()
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice!!.toDouble()

        var result = Utils.convertPrice(
            sqrt(
                (maxPrice.pow(2.0)
                        + bidPrice.pow(2.0)
                        + highPrice.pow(2.0)
                        + closePrice.pow(2.0)
                        ) / 4
            )
        )!!

        result = max(highPrice, result)

        Log.d(TAG, "[DEBUG] getTakeProfitPrice - Take a profit marketId: $marketId " +
                "askPrice: ${TradeFragment.Format.zeroFormat.format(result)} " +
                "currentPrice: ${TradeFragment.Format.zeroFormat.format(closePrice)} " +
                "maxPrice: ${TradeFragment.Format.zeroFormat.format(maxPrice)} " +
                "minPrice: ${TradeFragment.Format.zeroFormat.format(minPrice)} "
        )

        return result
    }
}