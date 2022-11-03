package com.example.upbittrade.adapter

import com.example.upbittrade.database.TradeInfoData
import com.example.upbittrade.service.TradeService
import java.util.UUID

class TradeItem: TradeInfoData {

    var state: TradeService.State = TradeService.State.READY
    var buyPrice: Double? = null
    var sellPrice: Double? = null
    var volume: Double? = null
    var uuid: UUID? = null

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