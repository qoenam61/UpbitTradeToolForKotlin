package com.example.upbittrade.model

import android.app.Application
import com.example.upbittrade.database.TradeDataBase

class Repository(application: Application) {

    val marketMapInfo : HashMap<String, MarketInfo> = HashMap()
    val database = TradeDataBase.getInstance(application.applicationContext)

}