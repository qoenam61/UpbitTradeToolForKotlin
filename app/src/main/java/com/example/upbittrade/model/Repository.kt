package com.example.upbittrade.model

import android.app.Application
import android.content.Intent
import com.example.upbittrade.service.TradeService

class Repository {

    var marketMapInfo : HashMap<String, MarketInfo>? = null

    constructor(application: Application) {
    }


}