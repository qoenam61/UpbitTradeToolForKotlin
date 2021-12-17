package com.example.upbittrade.utils

import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.model.TradeCoinInfo

class TradeManager(private val listener: TradeChangedListener) {
    companion object {
        private const val TAG = "TradeManager"
    }

    interface TradeChangedListener {
        fun onPostBid(postBidMap: HashMap<String, OrderCoinInfo>)
        fun onPostAsk(postAskMap: HashMap<String, OrderCoinInfo>)
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
                    marketIdList?.filter { filterBuyList(TradeFragment.tradeInfo[it])}
                }

                Type.POST_ASK -> {
                    null
                }
                else -> {
                    null
                }
            }

            filteredList!!.forEach() {
//                Log.d(TAG, "FilterList filterBuyList: $it ")
                val orderCoinInfo = TradeFragment.tradeInfo[it]!! as OrderCoinInfo
                orderCoinInfo.status = OrderCoinInfo.Status.READY
                postBid[it] = orderCoinInfo
            }

            if (postBid.isNotEmpty()) {
                listener.onPostBid(postBid)
            }
        }
    }

    private fun filterBuyList(tradeCoinInfo: TradeCoinInfo?): Boolean {
        if (tradeCoinInfo == null) {
            return false
        }

        if (postBid.containsKey(tradeCoinInfo.marketId)) {
            return false
        }

        // tick count > thresholdTick
        // getTradeInfoPriceRate() > thresholdRate
        // getPriceVolumeRate() > thresholdAvgMinPerAvgDayPriceVolumeRate
        // getBidAskRate() > thresholdBidAskRate
        // getBidAskPriceRate() > thresholdBidAskPriceRate
        if (tradeCoinInfo.tickCount!! > TradeFragment.UserParam.thresholdTick
            && tradeCoinInfo.getTradeInfoPriceRate() > TradeFragment.UserParam.thresholdRate
            && tradeCoinInfo.getAvgMinVsAvgDayPriceVolumeRate() > TradeFragment.UserParam.thresholdAvgMinPerAvgDayPriceVolumeRate
            && tradeCoinInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
            && tradeCoinInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceRate
        ) {
            return true
        }
        return false
    }
}