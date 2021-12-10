package com.example.upbittrade.api

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException

abstract class DefaultRetrofit(val accessKey: String, val secretKey: String) {
    object TAG {
      const val name = "DefaultRetrofit"
    }

    var upbitApi: UpbitApi? = null

    fun makeUpBitApi() {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
            .run {
                addInterceptor(AppInterceptor())
                build()
            }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.upbit.com")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        upbitApi = retrofit.create(UpbitApi::class.java)
    }

    inner class AppInterceptor() : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain) = with(chain) {
            val origin: Request = chain.request()
            val newRequest: Request = changedRequest(origin)
            Log.d(TAG.toString(), "intercept: $newRequest")
            proceed(newRequest)
        }


    }

    open fun changedRequest(origin: Request): Request {
        return origin.newBuilder()
            .header("Content-Type", "application/json")
            .addHeader("Authorization", getAuthToken())
            .build()
    }

    abstract fun getAuthToken(): String

    open fun getUpBitApi(): UpbitApi? {
        return upbitApi
    }
}