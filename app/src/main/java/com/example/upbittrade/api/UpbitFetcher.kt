package com.example.upbittrade.api

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.model.APIKey
import com.example.upbittrade.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpbitFetcher(val context: Context) {
    companion object {
        const val TAG = "UpbitFetcher"
    }

    interface ConnectionState {
        fun onConnection(isConnect: Boolean)
        fun deleteError(uuid: String?)
        fun shortMoney(uuid: String?, type: String?)
    }

    var accountRetrofit: AppKeyAccountRetrofit? = null

    fun makeRetrofit(accessKey: String, secretKey: String) {
        accountRetrofit = AppKeyAccountRetrofit(accessKey, secretKey)
        accountRetrofit?.makeUpBitApi()
    }

    fun getAPIKeyList(keys: Array<String>): LiveData<List<APIKey>> {
        makeRetrofit(keys[0], keys[1])
        val result = MutableLiveData<List<APIKey>>()
        val call: Call<List<APIKey?>?>? = accountRetrofit?.getUpBitApi()?.checkAPIKey()

        Log.d(TAG, "getAPIKeyList: $keys request: ${call.toString()}")
        call!!.enqueue(object : Callback<List<APIKey?>?> {
            override fun onResponse(call: Call<List<APIKey?>?>, response: Response<List<APIKey?>?>) {
                Log.d(TAG, "onResponse: ${response.isSuccessful} body: ${response.body()}")

                if (response.isSuccessful) {
                    result.value = response.body() as List<APIKey>
                } else {
                    Utils.showToast(context, "Fail to login")
                }
            }

            override fun onFailure(p0: Call<List<APIKey?>?>, p1: Throwable) {
                Log.e(TAG, "onFailure: ", p1)
                Utils.showToast(context, "onFailure to login")
            }
        })
        return result
    }
}