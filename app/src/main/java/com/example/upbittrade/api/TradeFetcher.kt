package com.example.upbittrade.api

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.Candle
import com.example.upbittrade.model.DayCandle
import com.example.upbittrade.model.MarketInfo
import com.google.gson.JsonParser
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

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
                    Log.d(TAG, "tMarketInfo onResponse: ${response.body()}")
                }
                if (!response.isSuccessful) {
                    try {
                        val jObjError = JSONObject(response.errorBody()!!.string())
                        Log.w(
                            TAG,
                            "getMarketInfo onResponse - toString: " + call.toString()
                                    + " code: " + response.code()
                                    + " headers: " + response.headers()
                                    + " raw: " + response.raw()
                                    + " jObjError: " + (jObjError ?: "NULL")
                        )
//                        if (mActivity != null) {
//                            mActivity.runOnUiThread(Runnable {
//                                Toast.makeText(
//                                    mActivity,
//                                    jObjError.toString(),
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            })
//                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }            }

            override fun onFailure(call: Call<List<MarketInfo?>?>, t: Throwable) {
                Log.d(TAG, "onFailure: $t")
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
                    Log.d(TAG, "[DEBUG] getMinCandleInfo onResponse: ${response.body()}")
                }
                if (!response.isSuccessful) {
                    try {
                        Log.d(TAG, "[DEBUG] getMinCandleInfo errorBody: ${response.errorBody()}")
//                        val jObjError = JSONObject(response.errorBody()!!.string())
                        Log.w(
                            TAG,
                            "[DEBUG] getMinCandleInfo onResponse - toString: " + call.toString()
                                    + " code: " + response.code()
                                    + " headers: " + response.headers()
                                    + " raw: " + response.raw()
//                                    + " jObjError: " + (jObjError.names() ?: "NULL")
                        )
//                        if (mActivity != null) {
//                            mActivity.runOnUiThread(Runnable {
//                                Toast.makeText(
//                                    mActivity,
//                                    jObjError.toString(),
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            })
//                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(call: Call<List<Candle?>?>, t: Throwable) {
                Log.d(TAG, "onFailure: $t")
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
                    Log.d(TAG, "[DEBUG] getDayCandleInfo onResponse: ${response.body()}")
                }
                if (!response.isSuccessful) {
                    try {
                        Log.d(TAG, "[DEBUG] getDayCandleInfo errorBody: ${response.errorBody()}")
//                        val jObjError = JSONObject(response.errorBody()!!.string())
                        Log.w(
                            TAG,
                            "[DEBUG] getDayCandleInfo onResponse - toString: " + call.toString()
                                    + " code: " + response.code()
                                    + " headers: " + response.headers()
                                    + " raw: " + response.raw()
//                                    + " jObjError: " + (jObjError.names() ?: "NULL")
                        )
//                        if (mActivity != null) {
//                            mActivity.runOnUiThread(Runnable {
//                                Toast.makeText(
//                                    mActivity,
//                                    jObjError.toString(),
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            })
//                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }            }

            override fun onFailure(call: Call<List<DayCandle?>?>, t: Throwable) {
                Log.d(TAG, "onFailure: $t")
            }
        })
        return result
    }
}