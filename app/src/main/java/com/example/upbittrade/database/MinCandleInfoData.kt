package com.example.upbittrade.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MinCandleInfoData(
    @ColumnInfo(name = "_marketId") var marketId: String?,
    @ColumnInfo(name = "_openingPrice") var openingPrice: Double?,
    @ColumnInfo(name = "_highPrice") var highPrice: Double?,
    @ColumnInfo(name = "_lowPrice") var lowPrice: Double?,
    @ColumnInfo(name = "_tradePrice") var tradePrice: Double?,
    @ColumnInfo(name = "_candleAccTradePrice") var candleAccTradePrice: Double?,
    @ColumnInfo(name = "_candleAccTradeVolume") var candleAccTradeVolume: Double?,
    @ColumnInfo(name = "_unit") var unit: Int?,
    @ColumnInfo(name = "_timestamp") var timestamp: Long?,
    @ColumnInfo(name = "_candleDateTimeUtc") var candleDateTimeUtc: String?,
    @ColumnInfo(name = "_candleDateTimeKst") var candleDateTimeKst: String?
    ) {

    @PrimaryKey var id: String = marketId + timestamp

    companion object {
        fun mapping(minCandleInfoData: MinCandleInfoData): MinCandleInfoData {
            return MinCandleInfoData(
                minCandleInfoData.marketId.toString(),
                minCandleInfoData.openingPrice,
                minCandleInfoData.highPrice,
                minCandleInfoData.lowPrice,
                minCandleInfoData.tradePrice,
                minCandleInfoData.candleAccTradePrice,
                minCandleInfoData.candleAccTradeVolume,
                minCandleInfoData.unit,
                minCandleInfoData.timestamp,
                minCandleInfoData.candleDateTimeUtc,
                minCandleInfoData.candleDateTimeKst
            )
        }
    }
}