package com.example.upbittrade.api

import com.example.upbittrade.model.*
import retrofit2.Call
import retrofit2.http.*

interface UpbitApi {
    @GET("/v1/api_keys")
    fun checkAPIKey(): Call<List<APIKey?>?>?

    @GET("/v1/accounts")
    fun getAccounts(): Call<List<Accounts?>?>?

    @GET("/v1/market/all")
    fun getMarketInfo(@Query("isDetails") isDetails: Boolean): Call<List<MarketInfo?>?>?

    @GET("/v1/candles/minutes/{unit}")
    fun getMinCandleInfo(
        @Path("unit") unit: String?,
        @Query("market") marketId: String?,
        @Query("to") to: String?,
        @Query("count") count: Int
    ): Call<List<Candle?>?>?

    @GET("/v1/candles/days")
    fun getDayCandleInfo(
        @Query("market") marketId: String?,
        @Query("to") to: String?,
        @Query("count") count: Int,
        @Query("convertingPriceUnit") convertingPriceUnit: String?
    ): Call<List<DayCandle?>?>?

    @GET("/v1/candles/weeks")
    fun getWeekCandleInfo(
        @Query("market") marketId: String?,
        @Query("to") to: String?,
        @Query("count") count: Int
    ): Call<List<WeekCandle?>?>?

    @GET("/v1/candles/months")
    fun getMonthsCandleInfo(
        @Query("market") marketId: String?,
        @Query("to") to: String?,
        @Query("count") count: Int
    ): Call<List<MonthCandle?>?>?

    @GET("/v1/trades/ticks")
    fun getTradeInfo(
        @Query("market") marketId: String?,
        @Query("count") count: Int
    ): Call<List<TradeInfo?>?>?

    @GET("/v1/ticker")
    fun getTicker(@Query("markets") marketId: String?): Call<List<Ticker?>?>?

    @FormUrlEncoded
    @POST("/v1/orders")
    fun postOrderInfo(@FieldMap fields: Map<String?, String?>?): Call<ResponseOrder?>?

    @GET("/v1/order")
    fun searchOrderInfo(@Query("uuid") uuid: String?): Call<ResponseOrder?>?

    @DELETE("/v1/order")
    fun deleteOrderInfo(@Query("uuid") uuid: String?): Call<ResponseOrder?>?

    @GET("/v1/orders")
    fun checkOrderInfo(@QueryMap fields: Map<String?, String?>?): Call<List<ResponseOrder?>?>?

}