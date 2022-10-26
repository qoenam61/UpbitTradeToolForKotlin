package com.example.upbittrade.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TradeInfoDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tradeInfoData: TradeInfoData)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(minCandleInfoData: MinCandleInfoData)

    @Update
    fun update(tradeInfoData: TradeInfoData)

    @Update
    fun update(minCandleInfoData: MinCandleInfoData)

    @Delete
    fun delete(tradeInfoData: TradeInfoData)

    @Delete
    fun delete(minCandleInfoData: MinCandleInfoData)

    @Query("SELECT * FROM TradeInfoData")
    fun getAllForTradeInfoData() : LiveData<List<TradeInfoData>>

    @Query("SELECT * FROM MinCandleInfoData")
    fun getAllForMinCandleInfoData() : LiveData<List<MinCandleInfoData>>

    @Query("SELECT * FROM MinCandleInfoData WHERE _marketId is :marketId ORDER BY _timestamp DESC LIMIT 1")
    fun getCurrentDataForMinCandle(marketId: String) : List<MinCandleInfoData>

    @Query("SELECT * FROM MinCandleInfoData WHERE _marketId is :marketId AND _timestamp BETWEEN :end AND :start ORDER BY _timestamp DESC")
    fun getMatchFilterForMinCandle(marketId: String, start: Long, end: Long) : List<MinCandleInfoData>

    @Query("SELECT * FROM TradeInfoData WHERE _marketId is :marketId AND _timestamp BETWEEN :start AND (:start - :duration) ORDER BY _timestamp DESC")
    fun getMatchFilterForTradeInfo(marketId: String, start: Long, duration: Long) : List<TradeInfoData>

}