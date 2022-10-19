package com.example.upbittrade.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TradeInfoData(
    @PrimaryKey var sequentialId: Long,
    var marketId: String,
    var tradePrice: Number,
    var tradeVolume: Number,
    var prevClosingPrice: Number,
    var changePrice: Number,
    var askBid: String,
    var timestamp: Long,
    var tradeTimeUtc: String,
    ) {


}