package com.example.upbittrade.adapter

import com.example.upbittrade.database.MinCandleInfoData

class MonitorItem: MinCandleInfoData {

    var prevClosingPrice: Double? = null
    var changePrice: Double? = null
    var askBidRate: Float? = null

    var avgPrice: Double? = null
    var avgVolme: Double? = null
    var devPrice: Double? = null
    var devVolme: Double? = null

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