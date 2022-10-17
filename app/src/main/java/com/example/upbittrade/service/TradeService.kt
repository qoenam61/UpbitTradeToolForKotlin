package com.example.upbittrade.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.model.MarketInfo

class TradeService : LifecycleService() {

    private val SC = "MyService"
    private val binder = TradeServiceBinder()
    private lateinit var bindService: TradePagerActivity.BindServiceCallBack

    private lateinit var marketListInfo : List<MarketInfo>

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun notification() {
        val nc = NotificationChannel(SC, "Trade Service", NotificationManager.IMPORTANCE_DEFAULT)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(nc)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notification()

        val nc: Notification = NotificationCompat.Builder(this, SC).setContentTitle("Running...").setSmallIcon(
            R.mipmap.ic_launcher_round).build()
        startForeground(1, nc)
        return super.onStartCommand(intent, flags, startId)
    }

    fun setRegisterCallBack(bindService: TradePagerActivity.BindServiceCallBack) {
        this.bindService = bindService

        bindService.tradeViewModel.searchMarketsInfo.value = false
    }

    inner class TradeServiceBinder : Binder() {
        fun getService() : TradeService {
            return this@TradeService
        }
    }

    override fun onCreate() {
        super.onCreate()

    }


    private fun observeLiveData() {
//        bindService.tradeViewModel.resultMarketsInfo?.observe(this) {
//            marketsInfo -> marketListInfo = marketsInfo
//        }
    }
}