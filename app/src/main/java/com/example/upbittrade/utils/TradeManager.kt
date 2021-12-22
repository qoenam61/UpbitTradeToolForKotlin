package com.example.upbittrade.utils

import android.os.SystemClock
import android.util.Log
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.data.PostOrderItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.Ticker
import com.example.upbittrade.model.TradeCoinInfo
import java.lang.Double.max
import java.util.*
import kotlin.collections.HashMap
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
        fun onDelete(marketId: String, uuid: UUID)
    }

    enum class Type {
        POST_BID,
        POST_ASK
    }

    private val postBid = HashMap<String, OrderCoinInfo>()

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
        // getTradeInfoPriceRate() > thresholdRate
        // getPriceVolumeRate() > thresholdAvgMinPerAvgDayPriceVolumeRate
        // getBidAskRate() > thresholdBidAskRate
        // getBidAskPriceRate() > thresholdBidAskPriceRate
        if (tradeCoinInfo.tickCount!! > TradeFragment.UserParam.thresholdTick
            && tradeCoinInfo.getPriceRate() >= 0
            && tradeCoinInfo.getPriceRangeRate() > TradeFragment.UserParam.thresholdRate
            && tradeCoinInfo.getAvgAccVolumeRate() > TradeFragment.UserParam.thresholdAccPriceVolumeRate
            && tradeCoinInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
            && tradeCoinInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceRate
        ) {
            return true
        }
        return false
    }

    fun updateTickerInfoToTrade(ticker: List<Ticker>, postInfo: OrderCoinInfo, responseOrder: ResponseOrder): OrderCoinInfo? {
        val marketId = ticker.first().marketId
        val time: Long = System.currentTimeMillis()
        val currentPrice = ticker.first().tradePrice?.toDouble()
        val side = responseOrder?.side
        val state = postInfo.state
        val timeZoneFormat = TradeFragment.Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        Log.i(TAG, "[DEBUG] updateTickerInfoToBuyList marketId: $marketId  " +
                "currentPrice: $currentPrice " +
                "side: $side " +
                "state: $state " +
                "time: ${timeZoneFormat.format(time)}")

        if (postInfo.state == OrderCoinInfo.State.WAIT && responseOrder != null) {
            if (postInfo.getRegisterDuration() != null && postInfo.getRegisterDuration()!! > TradeFragment.UserParam.monitorTime) {
                Log.d(TAG, "[DEBUG] updateTickerInfoToTrade: DELETE_ORDER_INFO")
                postInfo.registerTime = null
                postInfo.state = OrderCoinInfo.State.READY
//                processor.registerProcess(TaskItem(TradePagerActivity.PostType.DELETE_ORDER_INFO, marketId, UUID.fromString(responseOrder.uuid)))
                listener.onDelete(marketId!!, UUID.fromString(responseOrder.uuid))
                return postInfo
            }
            return null
        }

        if ((side.equals("ask") || side.equals("ASK")) && responseOrder.state.equals("done")) {
            return postInfo
        }

        return tacticalToSell(ticker, postInfo, responseOrder)
    }

    private fun tacticalToSell(ticker: List<Ticker>, postInfo: OrderCoinInfo, responseOrder: ResponseOrder?): OrderCoinInfo {
        val marketId = ticker.first().marketId
        val currentPrice = ticker.first().tradePrice?.toDouble()
        val profitRate = postInfo.getProfitRate()
        var maxProfitRate = postInfo.maxProfitRate
        val volume = responseOrder?.remainingVolume?.toDouble()

        if (profitRate!! > postInfo.maxProfitRate) {
            postInfo.maxPrice = postInfo.currentPrice!!
        }
        maxProfitRate = max(profitRate, maxProfitRate)
        postInfo.maxProfitRate = maxProfitRate

        // Take a profit
        if (maxProfitRate - profitRate > TradeFragment.UserParam.thresholdRate * 0.66) {
            val sellPrice = (postInfo.maxPrice + currentPrice!!.toDouble()) / 2.0

            Log.d(
                TAG,
                "[DEBUG] tacticalToSell Take a profit marketId: $marketId " +
                        "currentPrice: ${TradeFragment.Format.nonZeroFormat.format(currentPrice)} " +
                        "sellPrice: ${TradeFragment.Format.nonZeroFormat.format(sellPrice)} " +
                        "profitRate: ${TradeFragment.Format.percentFormat.format(profitRate)} " +
                        "maxProfitRate: ${TradeFragment.Format.percentFormat.format(maxProfitRate)} " +
                        "volume: ${TradeFragment.Format.zeroFormat.format(volume)} "
            )

/*            processor.registerProcess(
                PostOrderItem(
                    TradePagerActivity.PostType.POST_ORDER_INFO,
                    marketId,
                    "ask",
                    volume.toString(),
                    Utils().convertPrice(sellPrice).toString(),
                    "limit",
                    UUID.randomUUID()
                )
            )*/
            listener.onPostAsk(marketId!!, postInfo, "limit", Utils().convertPrice(sellPrice), volume!!)
        }

        // Stop a loss
        val highPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.highPrice
        val lowPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.lowPrice
        val openPrice = TradeFragment.tradeMonitorMapInfo[marketId]?.openPrice
        val closePrice = TradeFragment.tradeMonitorMapInfo[marketId]?.closePrice
        val rageRate =
            (highPrice!!.toDouble() - lowPrice!!.toDouble()) / lowPrice.toDouble()
        val sign: Boolean = closePrice!!.toDouble() - openPrice!!.toDouble() >= 0.0

        if (profitRate < TradeFragment.UserParam.thresholdRate * -0.66) {

            val highTail: Double = (highPrice.toDouble() - closePrice.toDouble()
                .coerceAtLeast(openPrice.toDouble()))

            val lowTail: Double = (openPrice.toDouble()
                .coerceAtMost(closePrice.toDouble()) - lowPrice.toDouble())

            val body: Double = abs(closePrice.toDouble() - openPrice.toDouble())

            val length: Double = highTail + lowTail + body


            when {
                //Market
                !sign && body / length > 0.8 -> {

/*                    processor.registerProcess(
                        PostOrderItem(
                            TradePagerActivity.PostType.POST_ORDER_INFO, marketId,
                            "ask", volume.toString(), null, "market", UUID.randomUUID()
                        )
                    )*/
                    listener.onPostAsk(marketId!!, postInfo, "market", null, volume!!)

                }


                // HHCO
                !sign && (body + lowTail) / length > 0.8 -> {
                    val sellPrice = Utils().convertPrice(
                        sqrt(
                            (highPrice.toDouble().pow(2.0) + highPrice.toDouble()
                                .pow(2.0)
                                    + closePrice.toDouble()
                                .pow(2.0) + openPrice.toDouble().pow(2.0)) / 4
                        )
                    )!!.toDouble()

/*                    processor.registerProcess(
                        PostOrderItem(
                            TradePagerActivity.PostType.POST_ORDER_INFO,
                            marketId,
                            "ask",
                            volume.toString(),
                            Utils().convertPrice(sellPrice).toString(),
                            "limit",
                            UUID.randomUUID()
                        )
                    )*/
                    listener.onPostAsk(marketId!!, postInfo, "limit", sellPrice, volume!!)

                }

                //HCOL
                else -> {
                    val sellPrice = Utils().convertPrice(
                        sqrt(
                            (highPrice.toDouble().pow(2.0) + closePrice.toDouble()
                                .pow(2.0)
                                    + openPrice.toDouble()
                                .pow(2.0) + lowPrice.toDouble().pow(2.0)) / 4
                        )
                    )!!.toDouble()

/*                    processor.registerProcess(
                        PostOrderItem(
                            TradePagerActivity.PostType.POST_ORDER_INFO,
                            marketId,
                            "ask",
                            volume.toString(),
                            Utils().convertPrice(sellPrice).toString(),
                            "limit",
                            UUID.randomUUID()
                        )
                    )*/
                    listener.onPostAsk(marketId!!, postInfo, "limit", sellPrice, volume!!)

                }
            }
        }

        return postInfo
    }
}