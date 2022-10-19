package com.example.upbittrade.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.MarketInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TradeService : LifecycleService() {

    private val SC = "MyService"
    private val binder = TradeServiceBinder()
    private lateinit var bindService: TradePagerActivity.BindServiceCallBack

    private val UNIT_PERIODIC_GAP = 60L
    private val UNIT_MIN_CANDLE = 60
    private val UNIT_MIN_CANDLE_COUNT = 1

    companion object {
        const val TAG = "TradeService"
        var ACCESS_KEY : String? = null
        var SECRET_KEY : String? = null
    }

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

        Log.d(TAG, "setRegisterCallBack: ")
        observeLiveData()

        bindService.tradeViewModel.searchMarketsInfo.value = true
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
        val viewModel = bindService.tradeViewModel

        val mutexMinCandle = Mutex()
        val mutexDayCandle = Mutex()

        viewModel.resultMarketsInfo.observe(this) {
            makeMarketMapInfo(it)

            val marketMapInfo = viewModel.repository.marketMapInfo

            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    var time = SystemClock.uptimeMillis()
                    for (marketId in marketMapInfo.keys) {
//                        Log.d(TAG, "resultMarketsInfo - marketId: $marketId")
                        mutexMinCandle.withLock {
                            viewModel.searchMinCandleInfo.postValue(ExtendCandleItem(
                                TradePagerActivity.PostType.MIN_CANDLE_INFO,
                                UNIT_MIN_CANDLE.toString(),
                                marketId,
                                UNIT_MIN_CANDLE_COUNT
                            ))
                        }
                        mutexMinCandle.lock()
                    }
//                    Log.d(TAG, "resultMarketsInfo - duration: ${(SystemClock.uptimeMillis() - time)}")
                }
            }

            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    var time = SystemClock.uptimeMillis()
                    for (marketId in marketMapInfo.keys) {
//                        Log.d(TAG, "resultMarketsInfo - marketId: $marketId")
                        mutexDayCandle.withLock {
                            viewModel.searchDayCandleInfo.postValue(ExtendCandleItem(
                                TradePagerActivity.PostType.DAY_CANDLE_INFO,
                                marketId,
                                UNIT_MIN_CANDLE_COUNT,
                                "KRW"
                            ))
                        }
                        mutexDayCandle.lock()
                    }
//                    Log.d(TAG, "resultMarketsInfo - duration: ${(SystemClock.uptimeMillis() - time)}")
                }
            }

            CoroutineScope(Dispatchers.Default).launch {
                viewModel.searchTradeInfo.postValue(CandleItem(
                    TradePagerActivity.PostType.TRADE_INFO,
                    "KRW-BTC",
                    30
                ))

                delay(100)

                viewModel.searchTradeInfo.postValue(CandleItem(
                    TradePagerActivity.PostType.TRADE_INFO,
                    "KRW-ETH",
                    30
                ))

                delay(100)
                viewModel.searchTradeInfo.postValue(CandleItem(
                    TradePagerActivity.PostType.TRADE_INFO,
                    "KRW-BTC",
                    30
                ))

                delay(100)

                viewModel.searchTradeInfo.postValue(CandleItem(
                    TradePagerActivity.PostType.TRADE_INFO,
                    "KRW-ETH",
                    30
                ))

                delay(100)

                viewModel.searchTradeInfo.postValue(CandleItem(
                    TradePagerActivity.PostType.TRADE_INFO,
                    "KRW-ETC",
                    30
                ))
            }
        }

        var minCandleCount = 0
        var minCandleSendTime = 0L
        viewModel.resultMinCandleInfo.observe(this) {
            for (candle in it) {
                Log.d(TAG, "resultMinCandleInfo: $candle")
            }

            if (minCandleCount == 0) {
                minCandleSendTime = SystemClock.uptimeMillis()
            }
            minCandleCount++

            var delayTime = 0L
            if (minCandleCount % 10 == 0) {
                delayTime =  1000 - (SystemClock.uptimeMillis() - minCandleSendTime)
                minCandleCount = 0
            }

            CoroutineScope(Dispatchers.Default).launch {
                Log.d(TAG, "observeLiveData(min) - unlock : $delayTime")
                if (delayTime > 0) {
                    delay(delayTime)
                }
                mutexMinCandle.unlock()
            }
        }

        var dayCandleCount = 0
        var dayCandleSendTime = 0L
        viewModel.resultDayCandleInfo.observe(this) {
            for (candle in it) {
                Log.d(TAG, "resultDayCandleInfo: $candle")
            }

            if (dayCandleCount == 0) {
                dayCandleSendTime = SystemClock.uptimeMillis()
            }
            dayCandleCount++

            var delayTime = 0L
            if (dayCandleCount % 10 == 0) {
                delayTime =  1000 - (SystemClock.uptimeMillis() - dayCandleSendTime)
                dayCandleCount = 0
            }

            CoroutineScope(Dispatchers.Default).launch {
                Log.d(TAG, "observeLiveData(day) - unlock : $delayTime")
                if (delayTime > 0) {
                    delay(delayTime)
                }
                mutexDayCandle.unlock()
            }
        }

        viewModel.resultTradeInfo.observe(this) {
            for (candle in it) {
                Log.d(TAG, "resultTradeInfo: $candle")
            }
        }
    }

    private fun makeMarketMapInfo(marketsInfo: List<MarketInfo>) {
        Log.d(TAG, "makeMarketMapInfo: ")
        val marketMapInfo = bindService.tradeViewModel.repository.marketMapInfo
        marketMapInfo.clear()
        val extTaskItemList = ArrayList<TaskItem>()
        val taskItemList = ArrayList<TaskItem>()
        val iterator = marketsInfo.iterator()
        while (iterator.hasNext()) {
            val marketInfo: MarketInfo = iterator.next()
            val marketId = marketInfo.market
            if (marketId?.contains("KRW-") == true
                && marketInfo.marketWarning?.contains("CAUTION") == false) {
                marketMapInfo[marketId] = marketInfo
                Log.i(TradeFragment.TAG, "resultMarketsInfo - marketId: $marketId")
            }
        }
    }
}