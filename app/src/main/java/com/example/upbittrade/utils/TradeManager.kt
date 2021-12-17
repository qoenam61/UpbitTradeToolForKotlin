package com.example.upbittrade.utils

import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.ResultTradeInfo

class TradeManager: Thread {
    companion object {
        enum class Type {
            POST_BID,
            POST_ASK
        }
        var postBidKeyList: List<String>? = null
    }

    private var marketIdList: List<String>? = null
    private var type: Type? = null


    

    constructor(type: Type, list: List<String>) {
        this.type = type
        marketIdList = list
    }

    fun newInstance(type: Type, list: List<String>): TradeManager {
        val manager = TradeManager(type, list)
        manager.start()
        return manager
    }

    override fun run() {
        super.run()
        postBidKeyList = when(type) {
            Type.POST_BID -> {
                 marketIdList!!.filter { filterBuyList(TradeFragment.tradeInfo[it])}
            }

            Type.POST_ASK -> {
                null
            }
            else -> {
                null
            }
        }

        // registerBackgroundProcess!!
    }

    private fun filterBuyList(tradeInfo: ResultTradeInfo?): Boolean {
        if (tradeInfo == null) {
            return false
        }

        // tick count > thresholdTick
        // getTradeInfoPriceRate() > thresholdRate
        // getPriceVolumeRate() > thresholdPriceVolumeRate
        // getBidAskRate() > 0.5
        // getBidAskPriceRate() > 0.5
        if (tradeInfo.tickCount!! > TradeFragment.UserParam.thresholdTick
            && tradeInfo.getTradeInfoPriceRate() > TradeFragment.UserParam.thresholdRate
            && tradeInfo.getPriceVolumeRate() > TradeFragment.UserParam.thresholdPriceVolumeRate
            && tradeInfo.getBidAskRate() > TradeFragment.UserParam.thresholdBidAskRate
            && tradeInfo.getBidAskPriceRate() > TradeFragment.UserParam.thresholdBidAskPriceRate
        ) {
            return true
        }
        return false
    }
}