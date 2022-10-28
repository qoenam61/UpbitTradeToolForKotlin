package com.example.upbittrade.adapter

import com.example.upbittrade.database.TradeInfoData

class TradeItem: TradeInfoData {

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