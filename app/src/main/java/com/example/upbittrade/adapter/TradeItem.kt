package com.example.upbittrade.adapter

import com.example.upbittrade.database.TradeInfoData
import com.example.upbittrade.service.TradeService

class TradeItem: TradeInfoData {

    var state: TradeService.State = TradeService.State.READY
    var buyPrice: Double? = null
    var sellPrice: Double? = null
    var remainingVolume: Double? = null

    constructor(
        tradeInfoData: TradeInfoData
    ) : super(
        tradeInfoData.sequentialId,
        tradeInfoData.marketId,
        tradeInfoData.tradePrice,
        tradeInfoData.tradeVolume,
        tradeInfoData.prevClosingPrice,
        tradeInfoData.changePrice,
        tradeInfoData.askBid,
        tradeInfoData.timestamp,
        tradeInfoData.tradeTimeUtc)
}