package com.example.upbittrade.utils

import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.ResultTradeInfo

class TradeManager: Thread {
    companion object {
        enum class Type {
            POST_BID,
            POST_ASK
        }
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
        when(type) {
            Type.POST_BID -> {
                marketIdList!!.filter { filterBuyList(TradeFragment.tradeInfo[it])}
            }

            Type.POST_ASK -> {

            }
        }
    }

    private fun filterBuyList(tradeInfo: ResultTradeInfo?): Boolean {
        if (tradeInfo == null) {
            return false
        }

        if (tradeInfo.)


        return false
    }
}