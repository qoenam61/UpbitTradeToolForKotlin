package com.example.upbittrade.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.upbittrade.model.TradeInfo
import com.google.gson.Gson

@Entity
open class TradeInfoData(
    @PrimaryKey var sequentialId: Long,
    @ColumnInfo(name = "_marketId") var marketId: String?,
    @ColumnInfo(name = "_tradePrice") var tradePrice: Double?,
    @ColumnInfo(name = "_tradeVolume") var tradeVolume: Double?,
    @ColumnInfo(name = "_prevClosingPrice") var prevClosingPrice: Double?,
    @ColumnInfo(name = "_changePrice") var changePrice: Double?,
    @ColumnInfo(name = "_askBid") open var askBid: String?,
    @ColumnInfo(name = "_timestamp") var timestamp: Long?,
    @ColumnInfo(name = "_tradeTimeUtc") var tradeTimeUtc: String?
    ) {

    companion object {
        fun mapping(tradeInfo: TradeInfo): TradeInfoData {
            return TradeInfoData(
                tradeInfo.sequentialId,
                tradeInfo.marketId.toString(),
                tradeInfo.tradePrice?.toDouble(),
                tradeInfo.tradeVolume?.toDouble(),
                tradeInfo.prevClosingPrice?.toDouble(),
                tradeInfo.changePrice?.toDouble(),
                tradeInfo.askBid,
                tradeInfo.timestamp,
                tradeInfo.tradeTimeUtc
            )
        }
    }

    override fun toString(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
}