package com.example.upbittrade.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface TradeInfoDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tradeInfoData: TradeInfoData)

    @Update
    fun update(tradeInfoData: TradeInfoData)

    @Delete
    fun delete(tradeInfoData: TradeInfoData)
}