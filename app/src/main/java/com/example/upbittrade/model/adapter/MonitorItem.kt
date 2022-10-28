package com.example.upbittrade.model.adapter

import com.example.upbittrade.database.MinCandleInfoData

class MonitorItem: MinCandleInfoData {

    var prevClosingPrice: Double? = null
    var changePrice: Double? = null
    var askBidRate: Float? = null

    constructor(
        candleInfoData: MinCandleInfoData
    ) : super(
        candleInfoData.marketId,
        candleInfoData.openingPrice,
        candleInfoData.highPrice,
        candleInfoData.lowPrice,
        candleInfoData.tradePrice,
        candleInfoData.candleAccTradePrice,
        candleInfoData.candleAccTradeVolume,
        candleInfoData.unit,
        candleInfoData.timestamp,
        candleInfoData.candleDateTimeUtc,
        candleInfoData.candleDateTimeKst)

}