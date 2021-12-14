package com.example.upbittrade.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.api.TradeFetcher
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem

class TradeViewModel: AndroidViewModel {

    private var upbitFetcher: TradeFetcher? = null

    constructor(application: Application) : super(application) {
        upbitFetcher = TradeFetcher()
        upbitFetcher?.makeRetrofit(
            TradePagerActivity.ACCESS_KEY.toString(),
            TradePagerActivity.SECRET_KEY.toString())
    }

    val searchMarketsInfo = MutableLiveData<Boolean>()
    private val searchTickerInfo = MutableLiveData<Boolean>()
    val searchMinCandleInfo = MutableLiveData<ExtendCandleItem>()
    val searchDayCandleInfo = MutableLiveData<ExtendCandleItem>()
    val searchWeekCandleInfo = MutableLiveData<CandleItem>()
    val searchMonthCandleInfo = MutableLiveData<CandleItem>()
    val searchTradeInfo = MutableLiveData<CandleItem>()


    var resultMarketsInfo: LiveData<List<MarketInfo>>? = Transformations.switchMap(searchMarketsInfo) {
        input -> upbitFetcher?.getMarketInfo(input)
    }

    var resultDayCandleInfo: LiveData<List<DayCandle>>? =  Transformations.switchMap(searchDayCandleInfo) {
        input -> upbitFetcher?.getDayCandleInfo(input)
    }

    var resultMinCandleInfo: LiveData<List<Candle>>? = Transformations.switchMap(searchMinCandleInfo) {
        input -> upbitFetcher?.getMinCandleInfo(input)
    }

    val resultTradeInfo: LiveData<List<TradeInfo>>? = Transformations.switchMap(searchTradeInfo) {
            input -> upbitFetcher?.getTradeInfo(input)
    }


    private class CandleInput {
        var unit = 0
        var marketId: String
        var to: String
        var count: Int
        var convertingPriceUnit: String? = null

        constructor(marketId: String, to: String, count: Int) {
            this.marketId = marketId
            this.to = to
            this.count = count
        }

        constructor(unit: Int, marketId: String, to: String, count: Int) {
            this.unit = unit
            this.marketId = marketId
            this.to = to
            this.count = count
        }

        constructor(marketId: String, to: String, count: Int, convertingPriceUnit: String?) {
            unit = unit
            this.marketId = marketId
            this.to = to
            this.count = count
            this.convertingPriceUnit = convertingPriceUnit
        }
    }

    private class TradeInput {
        var marketId: String
        var to: String? = null
        var count: Int
        var cursor: String
        var daysAgo = 0

        constructor(marketId: String, to: String?, count: Int, cursor: String, daysAgo: Int) {
            this.marketId = marketId
            this.to = to
            this.count = count
            this.cursor = cursor
            this.daysAgo = daysAgo
        }

        constructor(marketId: String, count: Int, cursor: String, daysAgo: Int) {
            this.marketId = marketId
            this.count = count
            this.cursor = cursor
            this.daysAgo = daysAgo
        }

        constructor(marketId: String, to: String?, count: Int, cursor: String) {
            this.marketId = marketId
            this.to = to
            this.count = count
            this.cursor = cursor
        }

        constructor(marketId: String, count: Int, cursor: String) {
            this.marketId = marketId
            this.count = count
            this.cursor = cursor
        }
    }
}