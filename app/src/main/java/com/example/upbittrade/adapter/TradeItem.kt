package com.example.upbittrade.adapter

import com.example.upbittrade.database.TradeInfoData
import com.example.upbittrade.service.TradeService
import java.util.UUID

class TradeItem: TradeInfoData {

    var state: TradeService.State = TradeService.State.READY
    var buyPrice: Double? = null
    var buyTime: Long? = null
    var sellPrice: Double? = null
    var sellTime: Long? = null
    var volume: Double? = null
    var uuid: UUID? = null

    var devVolume: Double? = null
    var devPrice: Double? = null
    var avgVolume: Double? = null
    var avgPrice: Double? = null

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