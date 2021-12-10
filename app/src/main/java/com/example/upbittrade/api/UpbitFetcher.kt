package com.example.upbittrade.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.upbittrade.model.Accounts
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class UpbitFetcher(val listener: ConnectionState) {
    object TAG {
        const val name = "UpbitFetcher"
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
//                Log.d(TAG.toString(), "[DEBUG] onResponse getAccounts - isLogIn: $isLogIn" + " response: " + response.body())
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
                if (!response.isSuccessful) {
                    try {
                        val jObjError = JSONObject(response.errorBody().toString())
                        Log.w(
                            TAG.toString(),
                            "onResponse getAccounts -toString: " + call.toString()
                                    + " code: " + response.code()
                                    + " headers: " + response.headers()
                                    + " raw: " + response.raw()
                                    + " jObjError: " + (jObjError ?: "NULL")
                        )
//                        if (mActivity != null) {
//                            mActivity.runOnUiThread(Runnable {
//                                Toast.makeText(
//                                    this,
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
                } else {
                    Log.d(TAG.toString(), "onResponse successful - isLogIn: $isLogIn")
                }
            }

            override fun onFailure(p0: Call<List<Accounts?>?>, p1: Throwable) {
                Log.d(TAG.toString(), "onFailure - isLogIn: $isLogIn")
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