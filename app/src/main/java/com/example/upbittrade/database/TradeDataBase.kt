package com.example.upbittrade.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TradeInfoData::class, MinCandleInfoData::class], version = 1)
abstract class TradeDataBase: RoomDatabase() {

    abstract fun tradeInfoDao() : TradeInfoDAO

    companion object {
        private var instance: TradeDataBase? = null

        @Synchronized
        fun getInstance(context: Context): TradeDataBase? {
            if (instance == null) {
                synchronized(TradeDataBase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TradeDataBase::class.java,
                        "tradeInfo-database"
                    ).build()
                }
            }
            return instance
        }
    }


}