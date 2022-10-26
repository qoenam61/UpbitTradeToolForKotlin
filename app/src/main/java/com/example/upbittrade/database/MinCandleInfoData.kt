package com.example.upbittrade.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.upbittrade.model.Candle
import com.google.gson.Gson

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

    @PrimaryKey var id: String = "$marketId-$candleDateTimeKst"

    companion object {
        fun mapping(candle: Candle): MinCandleInfoData {
            return MinCandleInfoData(
                candle.marketId.toString(),
                candle.openingPrice?.toDouble(),
                candle.highPrice?.toDouble(),
                candle.lowPrice?.toDouble(),
                candle.tradePrice?.toDouble(),
                candle.candleAccTradePrice?.toDouble(),
                candle.candleAccTradeVolume?.toDouble(),
                candle.unit,
                candle.timestamp,
                candle.candleDateTimeUtc,
                candle.candleDateTimeKst
            )
        }
    }

    override fun toString(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
}