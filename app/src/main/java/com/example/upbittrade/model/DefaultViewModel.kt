package com.example.upbittrade.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.example.upbittrade.api.UpbitFetcher

open class DefaultViewModel: ViewModel() {
    companion object {
        const val TAG = "DefaultViewModel"
    }

    private val upbitFetcher: UpbitFetcher = UpbitFetcher()

    val searchAppKeyListInfo = MutableLiveData<Array<String>>()
    val resultAppKeyListInfo: LiveData<List<APIKey>>? =
        Transformations.switchMap(searchAppKeyListInfo) {
            upbitFetcher.getAPIKeyList(it)
        }

    fun setKey(accessKey: String, secretKey: String) {
        upbitFetcher.makeRetrofit(accessKey, secretKey)
    }
}