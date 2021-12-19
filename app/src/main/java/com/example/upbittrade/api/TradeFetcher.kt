package com.example.upbittrade.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TradeFetcher {
    companion object {
        const val TAG = "TradeFetcher"
    }

    var tradeInfoRetrofit: TradeInfoRetrofit? = null


    fun makeRetrofit(accessKey: String, secretKey: String) {
        Log.d(TAG, "makeRetrofit - accessKey: $accessKey secretKey: $secretKey")
        tradeInfoRetrofit = TradeInfoRetrofit(accessKey, secretKey)
        tradeInfoRetrofit?.makeUpBitApi()
    }

    fun getMarketInfo(isDetails: Boolean): LiveData<List<MarketInfo>> {
        val result = MutableLiveData<List<MarketInfo>>()
        val call: Call<List<MarketInfo?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getMarketInfo(isDetails)
        call!!.enqueue(object : Callback<List<MarketInfo?>?> {
            override fun onResponse(
                call: Call<List<MarketInfo?>?>,
                response: Response<List<MarketInfo?>?>
            ) {
                if (response.body() != null) {
                    result.value = response.body() as List<MarketInfo>
                }
            }

            override fun onFailure(call: Call<List<MarketInfo?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun getMinCandleInfo(item: ExtendCandleItem): LiveData<List<Candle>> {
        val unit = item.unit
        val marketId = item.marketId
        val to = item.to
        val count = item.count
        val convertingPriceUnit = item.convertingPriceUnit

        val result = MutableLiveData<List<Candle>>()
        val call: Call<List<Candle?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getMinCandleInfo(unit, marketId, to, count)

        call!!.enqueue(object : Callback<List<Candle?>?> {
            override fun onResponse(
                call: Call<List<Candle?>?>,
                response: Response<List<Candle?>?>
            ) {
                if (response.body() != null) {
                    result.value = response.body() as List<Candle>
                }
            }

            override fun onFailure(call: Call<List<Candle?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun getDayCandleInfo(item: ExtendCandleItem): LiveData<List<DayCandle>> {
        val marketId = item.marketId
        val to = item.to
        val count = item.count
        val convertingPriceUnit = item.convertingPriceUnit

        val result = MutableLiveData<List<DayCandle>>()
        val call: Call<List<DayCandle?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getDayCandleInfo(marketId, to, count, convertingPriceUnit)

        call!!.enqueue(object : Callback<List<DayCandle?>?> {
            override fun onResponse(
                call: Call<List<DayCandle?>?>,
                response: Response<List<DayCandle?>?>
            ) {
                if (response.body() != null) {
                    result.value = response.body() as List<DayCandle>
                }

            }

            override fun onFailure(call: Call<List<DayCandle?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun getTradeInfo(item: CandleItem): LiveData<List<TradeInfo>> {
        val marketId = item.marketId
        val count = item.count

        val result = MutableLiveData<List<TradeInfo>>()
        val call: Call<List<TradeInfo?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getTradeInfo(marketId, count)

        call!!.enqueue(object : Callback<List<TradeInfo?>?> {
            override fun onResponse(
                call: Call<List<TradeInfo?>?>,
                response: Response<List<TradeInfo?>?>
            ) {
                if (response.body() != null) {
                    result.value = response.body() as List<TradeInfo>
                }
            }

            override fun onFailure(call: Call<List<TradeInfo?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun getTickerInfo(marketId: String): LiveData<List<Ticker>> {
        val result = MutableLiveData<List<Ticker>>()
        val call: Call<List<Ticker?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getTicker(marketId)

        call!!.enqueue(object : Callback<List<Ticker?>?> {
            override fun onResponse(
                call: Call<List<Ticker?>?>,
                response: Response<List<Ticker?>?>
            ) {
                if (response.body() != null) {
                    result.value = response.body() as List<Ticker>
                }
            }

            override fun onFailure(call: Call<List<Ticker?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }
}