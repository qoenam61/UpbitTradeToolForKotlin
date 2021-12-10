package com.example.upbittrade.model

import android.app.Application
import android.content.Intent
import android.content.Intent.*
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.api.UpbitFetcher
import com.example.upbittrade.fragment.LoginFragment
import com.example.upbittrade.utils.PreferenceUtil

open class DefaultViewModel(application: Application): AndroidViewModel(application) {
    object TAG {
        const val name = "DefaultViewModel"
    }

    private val searchAccountsInfo = MutableLiveData<Boolean>()
    val resultAccountsInfo: LiveData<List<Accounts>>? =
        Transformations.switchMap(searchAccountsInfo) {
                input -> upbitFetcher.getAccounts(input)
        }


    private val upbitFetcher: UpbitFetcher = UpbitFetcher(object : UpbitFetcher.ConnectionState {
        override fun onConnection(isConnect: Boolean) {
            Log.d(TAG.toString(), "[DEBUG] onConnection: $isConnect")
            if (isConnect) {
                val preferenceUtil = application?.let { PreferenceUtil(it) }
                preferenceUtil?.setString(preferenceUtil.ACCESS_KEY, LoginFragment.KeyObject.accessKey!!)
                preferenceUtil?.setString(preferenceUtil.SECRET_KEY, LoginFragment.KeyObject.secretKey!!)

                val context = application.baseContext
                val intent = Intent(context, TradePagerActivity::class.java)
                context.startActivity(intent.addFlags(FLAG_ACTIVITY_NEW_TASK))
            }
        }

        override fun deleteError(uuid: String?) {
            TODO("Not yet implemented")
        }

        override fun shortMoney(uuid: String?, type: String?) {
            TODO("Not yet implemented")
        }

    })

    fun setSearchAccountInfo(input: Boolean) {
        Log.d(TAG.toString(), "[DEBUG] setSearchAccountInfo: ")
        searchAccountsInfo.value = input
    }

    fun setKey(accessKey: String, secretKey: String) {
        upbitFetcher.makeRetrofit(accessKey, secretKey)
    }
}