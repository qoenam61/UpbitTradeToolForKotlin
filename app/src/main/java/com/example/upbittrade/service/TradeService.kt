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
import com.example.upbittrade.data.MonitorItemSet
import com.example.upbittrade.database.MinCandleInfoData
import com.example.upbittrade.database.TradeInfoData
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.adapter.MonitorItem
import com.example.upbittrade.adapter.TradeItem
import com.example.upbittrade.utils.PreferenceUtil
import com.example.upbittrade.utils.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.sql.Timestamp
import java.util.ArrayList
import java.util.Calendar
import java.util.HashMap
import kotlin.math.pow
import kotlin.math.sqrt

class TradeService : LifecycleService() {

    private val SC = "MyService"
    private val binder = TradeServiceBinder()
    private lateinit var bindService: TradePagerActivity.BindServiceCallBack

    private val UNIT_REMAINING_TIME_OFFSET = 60L

    private val UNIT_MIN_CANDLE = 3
    private val UNIT_MIN_CANDLE_COUNT = 200
    private val UNIT_MIN_CANDLE_PERIOD = UNIT_MIN_CANDLE * UNIT_MIN_CANDLE_COUNT
    private val UNIT_MONITORING_BUY_DEVIATION = 2
    private val UNIT_MONITORING_SELL_DEVIATION = 1

    private val UNIT_TRADE_INFO_COUNT = 200
    private val UNIT_TRADE_PERIOD = UNIT_MIN_CANDLE * 5
    private val UNIT_TRADE_BUY_DEVIATION = 2
    private val UNIT_TRADE_CANCEL_DEVIATION = 1

    val tradeMapInfo = HashMap<String, TradeItem>()
    val tradeListInfo = ArrayList<String>()
    val monitorMapInfo = HashMap<String, MonitorItem>()
    val monitorListInfo = ArrayList<String>()

    private lateinit var monitorItemSet: MonitorItemSet
    private var mutexTradeInfoCandle: Mutex? = null

    enum class State {
        READY,
        BUY,
        SELL,
        CANCEL
    }

    companion object {
        const val TAG = "TradeService"
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

        monitorItemSet = MonitorItemSet(object : MonitorItemSet.OnChangedListener {
            val viewModel = bindService.tradeViewModel
            var job: Job? = null

            override fun onSetDataChanged(tradeInfoSet: HashSet<String>, mutex: Mutex) {
                mutexTradeInfoCandle = mutex
                job?.cancel()
                job = CoroutineScope(Dispatchers.Default).launch {
                    while (true) {
                        var time = SystemClock.uptimeMillis()
                        val iterator = tradeInfoSet.iterator()
                        while (iterator.hasNext()) {
                            val marketId = iterator.next()
                            mutex.withLock {
                                viewModel.searchTradeInfo.postValue(
                                    CandleItem(
                                        TradePagerActivity.PostType.TRADE_INFO,
                                        marketId,
                                        UNIT_TRADE_INFO_COUNT
                                    )
                                )
                            }
                            mutex.lock()
    //                      Log.d(TAG, "resultMarketsInfo - duration: ${(SystemClock.uptimeMillis() - time)}")
                        }
                    }
                }
            }
        })

        observeLiveData()
        bindService.tradeViewModel.searchMarketsInfo.value = true
    }

    inner class TradeServiceBinder : Binder() {
        fun getService() : TradeService {
            return this@TradeService
        }
    }

    private fun observeLiveData() {
        val viewModel = bindService.tradeViewModel

        val mutexMinCandle = Mutex()

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
        }

        var minCandleCount = 0
        var minCandleSendTime = 0L
        viewModel.resultMinCandleInfo.observe(this) {
            if (it.isNotEmpty()) {
                val marketId = it[0].marketId
                CoroutineScope(Dispatchers.Default).launch {
                    val job1 = launch {
                        for (candle in it) {
//                            Log.d(TAG, "resultMinCandleInfo: $candle")
                            val can = MinCandleInfoData.mapping(candle)
                            viewModel.repository.database?.tradeInfoDao()?.insert(can)
                        }
                    }
                    job1.join()

                    val job2 = launch {
                        val start = Timestamp(System.currentTimeMillis())
                        val cal = Calendar.getInstance()
                        cal.time = start
                        cal.add(Calendar.MINUTE, UNIT_MIN_CANDLE_PERIOD * -1)
                        val end = cal.time.time

//                        Log.d(TAG, "resultMinCandleInfo - getMatchFilterForMinCandle - marketId: $marketId" +
//                                "start: ${Utils.Format.timeFormat.format(start.time)} " +
//                                "end: ${Utils.Format.timeFormat.format(end)}")

                        val subJob = launch {
                            val data = viewModel.repository.database?.tradeInfoDao()?.getMatchFilterForMinCandle(
                                marketId!!, start.time, end)
                            if (!data.isNullOrEmpty()) {
                                analysisMinCandleInfoData(data)
                            }
                        }
                        subJob.join()
                    }
                    job2.join()

                    val job3 = launch {
                        if (minCandleCount == 0) {
                            minCandleSendTime = SystemClock.uptimeMillis()
                        }
                        minCandleCount++

                        var delayTime = 0L
                        if (minCandleCount % 10 == 0) {
                            delayTime =  1000 - (SystemClock.uptimeMillis() - minCandleSendTime)
                            minCandleCount = 0
                        }
//                        Log.d(TAG, "observeLiveData(min) - unlock : $delayTime")
                        if (delayTime > 0) {
                            delay(delayTime + UNIT_REMAINING_TIME_OFFSET)
                        }
                        mutexMinCandle.unlock()
                    }
                    job3.join()
                }
            } else {
                mutexMinCandle.unlock()
            }
        }

        var tradeInfoCount = 0
        var tradeInfoSendTime = 0L
        viewModel.resultTradeInfo.observe(this) {
            if (it.isNotEmpty()) {
                val marketId = it[0].marketId
                CoroutineScope(Dispatchers.Default).launch {
                    val job1 = launch {
                        for (tradeInfo in it) {
//                            Log.d(TAG, "resultTradeInfo: $tradeInfo")
                            val trade = TradeInfoData.mapping(tradeInfo)
                            viewModel.repository.database?.tradeInfoDao()?.insert(trade)
                        }
                    }
                    job1.join()

                    val job2 = launch {
                        val start = Timestamp(System.currentTimeMillis())
                        val cal = Calendar.getInstance()
                        cal.time = start
                        cal.add(Calendar.MINUTE, UNIT_TRADE_PERIOD * -1)
                        val end = cal.time.time

//                        Log.d(TAG, "resultTradeInfo - getMatchFilterForTradeInfo - marketId: $marketId" +
//                                "start: ${Utils.Format.timeFormat.format(start.time)} " +
//                                "end: ${Utils.Format.timeFormat.format(end)}")

                        val subJob = launch {
                            val data = viewModel.repository.database?.tradeInfoDao()?.getMatchFilterForTradeInfo(
                                marketId!!, start.time, end)
                            if (!data.isNullOrEmpty()) {
                                analysisTradeInfoData(data)
                            }
                        }
                        subJob.join()
                    }
                    job2.join()

                    val job3 = launch {
                        if (tradeInfoCount == 0) {
                            tradeInfoSendTime = SystemClock.uptimeMillis()
                        }
                        tradeInfoCount++

                        var delayTime = 0L
                        if (tradeInfoCount % 10 == 0) {
                            delayTime =  1000 - (SystemClock.uptimeMillis() - tradeInfoSendTime)
                            tradeInfoCount = 0
                        }

//                        Log.d(TAG, "observeLiveData(tradeInfo) - unlock : $delayTime")
                        if (delayTime > 0) {
                            delay(delayTime + UNIT_REMAINING_TIME_OFFSET)
                        }
                        mutexTradeInfoCandle?.unlock()
                    }
                    job3.join()
                }
            } else {
                mutexTradeInfoCandle?.unlock()
            }
        }

        viewModel.addMonitorItem.observe(this) {
            monitorItem ->
        }

        viewModel.removeMonitorItem.observe(this) {
            marketId ->

        }

        viewModel.addTradeInfo.observe(this) {
                tradeInfo ->
            buyOrder(tradeInfo)
        }

        viewModel.removeTradeInfo.observe(this) {
                marketId ->
            cancelOrder(marketId)
        }
    }

    private fun makeMarketMapInfo(marketsInfo: List<MarketInfo>) {
        Log.d(TAG, "makeMarketMapInfo: ")
        val marketMapInfo = bindService.tradeViewModel.repository.marketMapInfo
        marketMapInfo.clear()
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
        val viewModel = bindService.tradeViewModel
        viewModel.searchMarketsMapInfo.value = marketMapInfo
    }

    private fun analysisMinCandleInfoData(candleData: List<MinCandleInfoData>): Float {
//        for (candle in candleData) {
//            Log.d(TAG, "analysisMinCandleInfoData: $candle")
//        }

        val totalPrice = candleData.sumOf { it.highPrice!! + it.lowPrice!! + it.openingPrice!! + it.tradePrice!!}
        val avgPrice = totalPrice / (4 * candleData.size)
        val totalPricePow = candleData.sumOf { (it.highPrice!!).pow(2) + (it.lowPrice!!).pow(2) + (it.openingPrice!!).pow(2) + (it.tradePrice!!).pow(2) }
        val priceDeviation = sqrt((totalPricePow / (4 * candleData.size)) - avgPrice.pow(2))

        val totalVolume = candleData.sumOf { it.candleAccTradeVolume!! }
        val avgVolume = totalVolume / (candleData.size)
        val totalVolumePow = candleData.sumOf { it.candleAccTradeVolume!!.pow(2) }
        val volumeDeviation = sqrt((totalVolumePow / (candleData.size)) - avgVolume.pow(2))


        //gaussian_pdf = (1/sqrt(2*pi*sigma^2))*exp(-0.5.*((x-mu)/sigma).^2);
//        val prob =  (1/sqrt(2 * Math.PI * deviation.pow(2)))*exp(-0.5 * ((candleData[0].tradePrice!!.toDouble() - avg)/deviation).pow(2))
        val candleInfoData = candleData[0]
        val currentPrice = candleInfoData.tradePrice!!
        val currentVolume = candleInfoData.candleAccTradeVolume!!
        val avgRate = (currentPrice - avgPrice) / avgPrice
        val viewModel = bindService.tradeViewModel

        if (!monitorItemSet.contains(candleInfoData.marketId!!) &&
            currentPrice > Utils.convertPrice(avgPrice + (UNIT_MONITORING_BUY_DEVIATION * priceDeviation))
            && currentVolume > avgVolume + (UNIT_MONITORING_BUY_DEVIATION * priceDeviation)) {
            Log.d(
                TAG, "analysisMinCandleInfoData(add) - marketId: ${candleInfoData.marketId} " +
//                        "prob: ${Utils.Format.percentFormat.format(prob)} " +
                        "avg_rate: ${Utils.Format.percentFormat.format(avgRate)} " +
                        "avg: ${Utils.Format.zeroFormat2.format(avgPrice)} " +
                        "price: ${Utils.Format.zeroFormat2.format(currentPrice)} " +
                        "deviation: ${Utils.Format.zeroFormat2.format(priceDeviation)} " +
                        "total: ${Utils.Format.zeroFormat2.format(totalPrice)} " +
                        "count: ${candleData.size} "
            )
            val monitorItem = MonitorItem(candleInfoData)
            monitorItemSet.add(candleInfoData.marketId!!)
            monitorListInfo.add(candleInfoData.marketId!!)
            monitorMapInfo[candleInfoData.marketId!!] = monitorItem
            viewModel.addMonitorItem.postValue(monitorItem)
        } else if (monitorItemSet.contains(candleInfoData.marketId!!)
            && currentPrice < Utils.convertPrice(avgPrice - (UNIT_MONITORING_SELL_DEVIATION * priceDeviation))) {
            Log.d(
                TAG, "analysisMinCandleInfoData(remove) - marketId: ${candleInfoData.marketId} " +
//                        "prob: ${Utils.Format.percentFormat.format(prob)} " +
                        "avg_rate: ${Utils.Format.percentFormat.format(avgRate)} " +
                        "avg: ${Utils.Format.zeroFormat2.format(avgPrice)} " +
                        "price: ${Utils.Format.zeroFormat2.format(currentPrice)} " +
                        "deviation: ${Utils.Format.zeroFormat2.format(priceDeviation)} " +
                        "total: ${Utils.Format.zeroFormat2.format(totalPrice)} " +
                        "count: ${candleData.size} "
            )
            monitorItemSet.remove(candleInfoData.marketId!!)
            monitorListInfo.remove(candleInfoData.marketId!!)
            monitorMapInfo.remove(candleInfoData.marketId!!)
            viewModel.removeMonitorItem.postValue(candleInfoData.marketId)
        }
        viewModel.updateMonitorItem.postValue(candleInfoData)
        return 0f
    }

    private fun analysisTradeInfoData(tradeData: List<TradeInfoData>): TradeInfoData {
        val sumTradePrice = tradeData.sumOf { it.tradePrice!! }
        val sumTradePricePow = tradeData.sumOf { it.tradePrice!!.pow(2) }
        val avgTradePrice = sumTradePrice / tradeData.size
        val deviationPrice = sqrt((sumTradePricePow / (tradeData.size)) - avgTradePrice.pow(2))

        val sumTradeVolume = tradeData.sumOf { it.tradeVolume!! }
        val sumTradeVolumePow = tradeData.sumOf { it.tradeVolume!!.pow(2) }
        val avgTradeVolume = sumTradeVolume / tradeData.size
        val deviationVolume = sqrt((sumTradeVolumePow / (tradeData.size)) - avgTradeVolume.pow(2))

        var askCount = 0f
        var bidCount = 0f
        tradeData.forEach {
            if ("ask".equals(it.askBid, ignoreCase = true)) {
                askCount += 1f
            } else {
                bidCount += 1f
            }
        }

        if (askCount == 0f) askCount = Float.MAX_VALUE

        val tradeInfoData = tradeData[0]

        tradeInfoData.tradeVolume = avgTradePrice
        tradeInfoData.askBid = Utils.Format.percentFormat.format(bidCount.div(askCount))

        val viewModel = bindService.tradeViewModel
        val currentPrice = tradeInfoData.tradePrice!!
        val currentVolume = tradeInfoData.tradeVolume!!

        val tradeItem = TradeItem(tradeInfoData)

        if (!tradeMapInfo.contains(tradeInfoData.marketId) &&
            currentPrice > Utils.convertPrice(avgTradePrice + (UNIT_TRADE_BUY_DEVIATION * deviationPrice))
            && currentVolume > (avgTradeVolume + (UNIT_TRADE_BUY_DEVIATION * deviationVolume))
        ) {
            Log.d(TAG, "analysisTradeInfoData(add) - " +
                    "marektId: ${tradeInfoData.marketId} " +
                    "askBidRate: ${Utils.Format.percentFormat.format(bidCount.div(askCount))} " +
                    "count: ${tradeData.size}")

            tradeItem.state = State.READY
            tradeItem.buyPrice = avgTradePrice
            tradeItem.remainingVolume = 0.0
            tradeListInfo.add(tradeInfoData.marketId!!)
            tradeMapInfo[tradeInfoData.marketId!!] = tradeItem
            viewModel.addTradeInfo.postValue(tradeItem)
        } else if (tradeItem.state == State.READY && tradeMapInfo.contains(tradeInfoData.marketId) &&
            currentPrice < Utils.convertPrice(avgTradePrice - (UNIT_TRADE_CANCEL_DEVIATION * deviationPrice))) {
            Log.d(TAG, "analysisTradeInfoData(remove) - " +
                    "marektId: ${tradeInfoData.marketId} " +
                    "askBidRate: ${Utils.Format.percentFormat.format(bidCount.div(askCount))} " +
                    "count: ${tradeData.size}")

            tradeItem.state = State.CANCEL
            tradeListInfo.remove(tradeInfoData.marketId!!)
            tradeMapInfo.remove(tradeInfoData.marketId!!)
            viewModel.removeTradeInfo.postValue(tradeInfoData.marketId)
        }
        viewModel.updateTradeInfoData.postValue(tradeInfoData)
        return tradeInfoData
    }

    override fun onDestroy() {
        super.onDestroy()

        val pref = PreferenceUtil(this)
        pref.setBoolean(PreferenceUtil.SUCCESS_LOGIN, false)
    }

    private fun buyOrder(tradeInfo: TradeItem?) {
//        TODO("Not yet implemented")
        Log.d(TAG, "buyOrder - marketId: ${tradeInfo?.marketId}")
    }

    private fun cancelOrder(marketId: String?) {
//        TODO("Not yet implemented")
        Log.d(TAG, "cancelOrder - marketId: $marketId")
    }
}