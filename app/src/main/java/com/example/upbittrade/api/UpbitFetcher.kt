package com.example.upbittrade.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.model.Accounts
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpbitFetcher(val listener: ConnectionState) {
    companion object {
        const val TAG = "UpbitFetcher"
    }

    interface ConnectionState {
        fun onConnection(isConnect: Boolean)
        fun deleteError(uuid: String?)
        fun shortMoney(uuid: String?, type: String?)
    }

    var accountRetrofit: AccountRetrofit? = null

    fun makeRetrofit(accessKey: String, secretKey: String) {
        accountRetrofit = AccountRetrofit(accessKey, secretKey)
        accountRetrofit?.makeUpBitApi()
    }

    fun getAccounts(isLogIn: Boolean): LiveData<List<Accounts>> {
        val result = MutableLiveData<List<Accounts>>()
        val call: Call<List<Accounts?>?>? = accountRetrofit?.getUpBitApi()?.getAccounts()
        call!!.enqueue(object : Callback<List<Accounts?>?> {
            override fun onResponse(call: Call<List<Accounts?>?>, response: Response<List<Accounts?>?>) {
                if (response.body() != null) {
                    if (isLogIn) {
                        if (listener != null) {
                            listener.onConnection(true)
                        }
                    }
                    result.setValue(response.body() as List<Accounts>?)
                } else {
                    if (isLogIn) {
                        if (listener != null) {
                            listener.onConnection(false)
                        }
                    }
                }
            }

            override fun onFailure(p0: Call<List<Accounts?>?>, p1: Throwable) {
                Log.w(TAG, "onFailure - isLogIn: $isLogIn")
                if (isLogIn) {
                    if (listener != null) {
                        listener.onConnection(false)
                    }
                }
            }
        })
        return result
    }
}