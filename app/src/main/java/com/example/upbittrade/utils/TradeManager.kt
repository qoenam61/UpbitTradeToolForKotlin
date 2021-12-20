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

class TradeManager(private val listener: TradeChangedListener) {
    companion object {
        private const val TAG = "TradeManager"
    }

    interface TradeChangedListener {
        fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo)
        fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo)
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
                orderCoinInfo.state = OrderCoinInfo.State.READY
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
            && tradeCoinInfo.getPriceRangeRate() > TradeFragment.UserParam.thresholdRate
            && tradeCoinInfo.getAvgAccVolumeRate() > TradeFragment.UserParam.thresholdAccPriceVolumeRate
            && tradeCoinInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
            && tradeCoinInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceRate
        ) {
            return true
        }
        return false
    }

    fun updateTickerInfoToBuyList(ticker: List<Ticker>, postInfo: OrderCoinInfo, responseOrder: ResponseOrder, processor: BackgroundProcessor): OrderCoinInfo {
        val marketId = ticker.first().marketId
        val time: Long = SystemClock.uptimeMillis()
        val currentPrice = ticker.first().tradePrice?.toDouble()
        val side = responseOrder.side
        val state = postInfo.state

        if (postInfo.state == OrderCoinInfo.State.WAIT) {
            if (postInfo.getRegisterDuration() != null && postInfo.getRegisterDuration()!! > TradeFragment.UserParam.monitorTime) {
                Log.d(TAG, "[DEBUG] updateTickerInfoToBuyList delete marketId: $marketId ")
                processor.registerProcess(TaskItem(TradePagerActivity.PostType.DELETE_ORDER_INFO, responseOrder.uuid))
                return postInfo
            }
        }

        if (side.equals("bid") || side.equals("BID")) {
            val profitRate = postInfo.getProfitRate()
            val maxProfitRate = postInfo.maxProfitRate
            val sellPrice = Utils().convertPrice ((postInfo.maxPrice + postInfo.currentPrice!!) / 2)
            val volume = responseOrder.remainingVolume

            if (state == OrderCoinInfo.State.BUY) {
                if (profitRate!! > postInfo.maxProfitRate) {
                    postInfo.maxPrice = postInfo.currentPrice!!
                }
                postInfo.maxProfitRate = max(profitRate, maxProfitRate)

                // Take a profit
                if (postInfo.maxProfitRate - profitRate > TradeFragment.UserParam.thresholdRate * 0.66) {
                    postInfo.state = OrderCoinInfo.State.WAIT

                    processor.registerProcess(
                        PostOrderItem(
                            TradePagerActivity.PostType.POST_ORDER_INFO, marketId,
                        "ask", volume.toString(), sellPrice.toString(), "limit", UUID.randomUUID())
                    )
                }


                // Stop a loss
            }
        }

        return postInfo
    }
}