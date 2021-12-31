package com.example.upbittrade.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.PostOrderItem
import com.example.upbittrade.model.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class TradeFetcher(val listener: PostOrderListener) {
    companion object {
        const val TAG = "TradeFetcher"
    }

    interface PostOrderListener {
        fun onInSufficientFunds(marketId: String, side: String, errorCode: Int, uuid:UUID)
        fun onError(marketId: String, side: String?, errorCode: Int, uuid:UUID)
    }

    var tradeInfoRetrofit: TradeInfoRetrofit? = null
    var postOrderRetrofit: PostOrderRetrofit? = null


    fun makeRetrofit(accessKey: String, secretKey: String) {
        Log.d(TAG, "makeRetrofit - accessKey: $accessKey secretKey: $secretKey")
        tradeInfoRetrofit = TradeInfoRetrofit(accessKey, secretKey)
        tradeInfoRetrofit?.makeUpBitApi()
        postOrderRetrofit = PostOrderRetrofit(accessKey, secretKey)
        postOrderRetrofit?.makeUpBitApi()
    }

    fun getMarketInfo(isDetails: Boolean): LiveData<List<MarketInfo>> {
        val result = MutableLiveData<List<MarketInfo>>()
        val call: Call<List<MarketInfo?>?>? = tradeInfoRetrofit?.getUpBitApi()?.getMarketInfo(isDetails)
        call!!.enqueue(object : Callback<List<MarketInfo?>?> {
            override fun onResponse(
                call: Call<List<MarketInfo?>?>,
                response: Response<List<MarketInfo?>?>
            ) {
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<MarketInfo>
                } else {
                    Log.w(TAG, "getMarketInfo"
                                + " call: " + call.request()
                                + " code: " + response.code()
                                + " headers: " + response.headers()
                                + " raw: " + response.raw()
                    )
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
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<Candle>
                } else {
                    Log.w(TAG, "getMinCandleInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                    )
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
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<DayCandle>
                } else {
                    Log.w(TAG, "getDayCandleInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                    )
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
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<TradeInfo>
                } else {
                    Log.w(TAG, "getTradeInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                    )
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
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<Ticker>
                } else {
                    Log.w(TAG, "getTickerInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                    )
                }
            }

            override fun onFailure(call: Call<List<Ticker?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun postOrderInfo(postOrderItem: PostOrderItem): LiveData<ResponseOrder> {
        val marketId: String = postOrderItem.marketId!!
        val side: String? = postOrderItem.side
        val volume: String = postOrderItem.volume.toString()
        val price: String = postOrderItem.price.toString()
        val ordType: String? = postOrderItem.ordType
        val identifier: UUID? = postOrderItem.identifier

        val params = HashMap<String?, String?>()
        params["market"] = marketId

        // Order
        if (side != null && side != "null") {
            params["side"] = side
        }
        if (volume != null && volume != "null") {
            params["volume"] = volume
        }
        if (price != null && price != "null") {
            params["price"] = price
        }
        if (ordType != null && ordType != "null") {
            params["ord_type"] = ordType
        }
        if (identifier != null && identifier.toString() != "null") {
            params["identifier"] = identifier.toString()
        }

        postOrderRetrofit?.params = params

        val result = MutableLiveData<ResponseOrder>()
        val call: Call<ResponseOrder?>? = postOrderRetrofit?.getUpBitApi()?.postOrderInfo(params)
        call!!.enqueue(object : Callback<ResponseOrder?> {
            override fun onResponse(
                call: Call<ResponseOrder?>,
                response: Response<ResponseOrder?>
            ) {
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as ResponseOrder
//                    Log.i(TAG, "postOrderInfo:  " +
//                            "raw: ${response.raw()} " +
//                            "body: ${(response.body() as ResponseOrder)}"
//                    )
                } else {
                    val jObjError = JSONObject(
                        response.errorBody()!!.string()
                    )
                    Log.w(
                        TAG,
                        "postOrderInfo"
                                + " call: " + call.request()
                                + " code: " + response.code()
                                + " headers: " + response.headers()
                                + " raw: " + response.raw()
                                + " jObjError: " + (jObjError ?: "NULL")
                    )

                    val errorObj = jObjError["error"] as JSONObject
                    if (response.code() == 400 && errorObj != null
                        && errorObj["name"] != null
                        && errorObj["name"] == "insufficient_funds_ask") {
                        Log.w(TAG, "postOrderInfo: insufficient_funds_ask")
                        listener.onInSufficientFunds(marketId, "ask", response.code(),  identifier!!)

                    } else if (response.code() == 400 && errorObj != null
                        && errorObj["name"] != null
                        && errorObj["name"] == "insufficient_funds_bid") {
                        Log.w(TAG, "postOrderInfo: insufficient_funds_bid")
                        listener.onInSufficientFunds(marketId,"bid", response.code(), identifier!!)
                    } else if (response.code() == 500 && errorObj != null
                        && errorObj["name"] != null
                        && errorObj["name"] == "server_error") {
                        listener.onError(marketId, side, response.code(), identifier!!)
                    }
                }
            }

            override fun onFailure(call: Call<ResponseOrder?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun searchOrderInfo(uuid: UUID): LiveData<ResponseOrder> {
        val params = HashMap<String?, String?>()
        params["uuid"] = uuid.toString()
        postOrderRetrofit?.params = params

        val result = MutableLiveData<ResponseOrder>()
        val call: Call<ResponseOrder?>? = postOrderRetrofit?.getUpBitApi()?.searchOrderInfo(uuid.toString())
        call!!.enqueue(object : Callback<ResponseOrder?> {
            override fun onResponse(
                call: Call<ResponseOrder?>,
                response: Response<ResponseOrder?>
            ) {
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as ResponseOrder
//                    Log.i(TAG, "searchOrderInfo :  " +
//                            "raw: ${response.raw()} " +
//                            "body: ${(response.body() as ResponseOrder)}"
//                    )
                } else {
                    val jObjError = JSONObject(
                        response.errorBody()!!.string()
                    )
                    Log.w(TAG, "searchOrderInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                            + " jObjError: " + (jObjError ?: "NULL")
                    )
                }
            }

            override fun onFailure(call: Call<ResponseOrder?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun deleteOrderInfo(uuid: UUID): LiveData<ResponseOrder> {
        val params = HashMap<String?, String?>()
        params["uuid"] = uuid.toString()
        postOrderRetrofit?.params = params

        val result = MutableLiveData<ResponseOrder>()
        val call: Call<ResponseOrder?>? = postOrderRetrofit?.getUpBitApi()?.deleteOrderInfo(uuid.toString())
        call!!.enqueue(object : Callback<ResponseOrder?> {
            override fun onResponse(
                call: Call<ResponseOrder?>,
                response: Response<ResponseOrder?>
            ) {
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as ResponseOrder
//                    Log.i(TAG, "deleteOrderInfo:  " +
//                            "raw: ${response.raw()} " +
//                            "body: ${(response.body() as ResponseOrder)}"
//                    )
                } else {
                    val jObjError = JSONObject(
                        response.errorBody()!!.string()
                    )
                    Log.w(TAG, "deleteOrderInfo"
                            + " call: " + call.request()
                            + " code: " + response.code()
                            + " headers: " + response.headers()
                            + " raw: " + response.raw()
                            + " jObjError: " + (jObjError ?: "NULL")
                    )
                }
            }

            override fun onFailure(call: Call<ResponseOrder?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }

    fun checkOrderInfo(postOrderItem: PostOrderItem): LiveData<List<ResponseOrder>> {
        val marketId: String = postOrderItem.marketId!!
        val state: String? = postOrderItem.state
        val limit: Number? = postOrderItem.limit
        val orderBy: String? = postOrderItem.orderBy

        val params = HashMap<String?, String?>()
        params["market"] = marketId
        if (state != null && state != "null") {
            params["state"] = state
        }
        if (limit != null) {
            params["limit"] = limit.toString()
        }
        if (orderBy != null && orderBy != "null") {
            params["order_by"] = orderBy
        }

        postOrderRetrofit?.params = params

        val result = MutableLiveData<List<ResponseOrder>>()
        val call: Call<List<ResponseOrder?>?>? = postOrderRetrofit?.getUpBitApi()?.checkOrderInfo(params)
        call!!.enqueue(object : Callback<List<ResponseOrder?>?> {
            override fun onResponse(
                call: Call<List<ResponseOrder?>?>,
                response: Response<List<ResponseOrder?>?>
            ) {
                if (response.body() != null && response.isSuccessful) {
                    result.value = response.body() as List<ResponseOrder>
                    Log.i(TAG, "checkOrderInfo:  " +
                            "raw: ${response.raw()} " +
                            "body: ${(response.body() as List<ResponseOrder>)}"
                    )
                } else {
                    val jObjError = JSONObject(
                        response.errorBody()!!.string()
                    )
                    Log.w(
                        TAG,
                        "checkOrderInfo"
                                + " call: " + call.request()
                                + " code: " + response.code()
                                + " headers: " + response.headers()
                                + " raw: " + response.raw()
                                + " jObjError: " + (jObjError ?: "NULL")
                    )
                }
            }

            override fun onFailure(call: Call<List<ResponseOrder?>?>, t: Throwable) {
                Log.w(TAG, "onFailure: $t")
            }
        })
        return result
    }
}