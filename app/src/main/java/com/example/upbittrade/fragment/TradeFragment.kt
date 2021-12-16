package com.example.upbittrade.fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.*
import com.example.upbittrade.utils.BackgroundProcessor
import com.example.upbittrade.utils.InitPopupDialog
import com.example.upbittrade.utils.TradeAdapter
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST
import okhttp3.internal.notifyAll
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        const val LIMIT_AMOUNT = 10000.0
        const val BASE_TIME = 3.0
        const val THRESHOLD_RATE = 0.03
        const val THRESHOLD_TICK = 1500

        private const val UNIT_REPEAT_MARKET_INFO = 30 * 60 * 1000
        private const val UNIT_MIN_CANDLE = 60
        private const val UNIT_MIN_CANDLE_COUNT = 24
        private const val UNIT_MONITOR_TIME = 60 * 1000
        private const val UNIT_TRADE_COUNT = 3000
        private const val UNIT_PRICE = 1000000

        val tradeInfo = HashMap<String, ResultTradeInfo>()
    }

    object Format {
        var nonZeroFormat = DecimalFormat("###,###,###,###")
        var zeroFormat = DecimalFormat("###,###,###,###.#")
        var percentFormat = DecimalFormat("###.##" + "%")
        var timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    }

    object UserParam {
        var completed = false
        var limitAmount: Double = 0.0
        var baseTime: Double = 0.0
        var thresholdRate: Double = 0.0
        var thresholdTick: Double = 0.0
        var thresholdPriceVolumeRate: Float = 1.0f
    }

    lateinit var mainActivity: TradePagerActivity
    private var viewModel: TradeViewModel? = null
    private var processor: BackgroundProcessor? = null
    private val marketMapInfo = HashMap<String, MarketInfo>()
    private val minCandleMapInfo = HashMap<String, ResultTradeInfo>()
    private val tradeMapInfo = HashMap<String, List<TradeInfo>>()
    private var tradeView: TradeAdapter? = null
    private var monitorAdapter: TradeAdapter? = null
//    private var tradeAdapter: TradeFragmentView.TradeAdapter? = null
//    private var resultAdapter: TradeFragmentView.TradeAdapter? = null

    private var monitorListView: RecyclerView? = null


    var isRunning = false

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = TradeViewModel(application = activity.application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trade, container, false)
        monitorAdapter = TradeAdapter(requireContext(), MONITOR_LIST)
        monitorListView = view.findViewById(R.id.monitor_list_view)
        monitorListView!!.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        monitorListView!!.adapter = monitorAdapter

        val initDialog = InitPopupDialog(requireContext())
        initDialog.show()

        val changeParamButton = view.findViewById<Button>(R.id.change_parameter)
        changeParamButton.setOnClickListener {
            initDialog.show()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "[DEBUG] onStart: ")
        val viewCycleOwner = viewLifecycleOwner
        viewModel?.resultMarketsInfo?.observe(viewCycleOwner) {
            marketsInfo ->
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
                    Log.d(TAG, "[DEBUG] resultMarketsInfo - marketId: $marketId")
                    extTaskItemList.add(ExtendCandleItem(MIN_CANDLE_INFO, UNIT_MIN_CANDLE.toString(), marketId, UNIT_MIN_CANDLE_COUNT))
                    taskItemList.add(CandleItem(TRADE_INFO, marketId, UNIT_TRADE_COUNT))
                }
            }
            processor?.registerProcess(extTaskItemList)
            processor?.registerProcess(taskItemList)
        }

        viewModel?.resultMinCandleInfo?.observe(viewCycleOwner) {
            minCandlesInfo ->
            val iterator = minCandlesInfo.reversed().iterator()
            Format.timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            var marketId: String = minCandlesInfo.first().marketId.toString()
            var accPriceVolume = minCandlesInfo.fold(0.0) {
                acc: Double, minCandleInfo: Candle -> acc + minCandleInfo.candleAccTradePrice!!.toDouble()
            }
            val highPrice = minCandlesInfo.maxOf { it.tradePrice!!.toDouble() }
            val lowPrice = minCandlesInfo.minOf { it.tradePrice!!.toDouble() }
            val openPrice = minCandlesInfo.first().tradePrice!!.toDouble()
            val closePrice = minCandlesInfo.last().tradePrice!!.toDouble()
            minCandleMapInfo[marketId] = ResultTradeInfo(marketId,
                minCandlesInfo.size, minCandlesInfo.last().timestamp,
                highPrice, lowPrice, openPrice, closePrice, accPriceVolume)
        }

        viewModel?.resultTradeInfo?.observe(viewCycleOwner) {
            tradesInfo ->
            makeTradeMapInfo(tradesInfo)
        }

    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        val repeatThread = object : Thread() {
            override fun run() {
                while (isRunning) {
                    Log.i(TAG, "resetBackgroundProcessor")
                    processor?.release()
                    if (processor == null || processor?.isRunning == false) {
                        processor = BackgroundProcessor(viewModel!!)
                        processor?.start()
                    }
                    processor?.registerProcess(TaskItem(MARKETS_INFO))
                    sleep(UNIT_REPEAT_MARKET_INFO.toLong())
                }
            }
        }
        repeatThread.start()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[DEBUG] onPause: ")
        processor?.release()
        isRunning = false
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "[DEBUG] onStop: ")
    }

    private fun makeTradeMapInfo(tradesInfoList: List<TradeInfo>) {
        if (tradesInfoList.isNullOrEmpty()) {
            return
        }

        var marketId: String = tradesInfoList.first().marketId.toString()

        var tempInfo: List<TradeInfo> = if (tradeMapInfo[marketId] == null) {
            tradesInfoList.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UNIT_MONITOR_TIME
            }.reversed()
        } else {
            val addList = tradesInfoList.asReversed().filter {tradeInfo ->
                tradeMapInfo[marketId]!!.last().sequentialId < tradeInfo.sequentialId}
            val combineInfo = tradeMapInfo[marketId]!! + addList
            combineInfo.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UNIT_MONITOR_TIME }
        }

        val accPriceVolume = tempInfo.fold(0.0) {
            acc: Double, tradeInfo: TradeInfo -> acc + tradeInfo.getPriceVolume()
        }
        val highPrice = tempInfo.maxOf { it.tradePrice!!.toDouble() }
        val lowPrice = tempInfo.minOf { it.tradePrice!!.toDouble() }
        val openPrice = tempInfo.first().tradePrice!!.toDouble()
        val closePrice = tempInfo.last().tradePrice!!.toDouble()
        val avgPriceVolumePerMin =
            minCandleMapInfo[marketId]?.accPriceVolume?.div(UNIT_MIN_CANDLE * UNIT_MIN_CANDLE_COUNT)

        tradeMapInfo[marketId] = tempInfo
        tradeInfo[marketId] = ResultTradeInfo(marketId,
            tempInfo.size,
            tempInfo.last().timestamp,
            highPrice,
            lowPrice,
            openPrice,
            closePrice,
            accPriceVolume,
            avgPriceVolumePerMin,
            tempInfo.last().getRate()
        )

        Log.d(TAG, "[DEBUG] makeTradeMapInfo - size: ${UserParam.thresholdTick} priceVolumeRate: ${UserParam.thresholdPriceVolumeRate}")


        val inputList = (tradeInfo.filter {
            (it.value.tickCount!! > UserParam.thresholdTick
                && it.value.getPriceVolumeRate() > UserParam.thresholdPriceVolumeRate)
        } as HashMap<String, ResultTradeInfo>)
            .toSortedMap(compareByDescending { sortedMapList(it) }).keys.toList()

        monitorAdapter!!.monitorMap = inputList
        monitorAdapter!!.notifyDataSetChanged()

        if (tradeInfo[marketId] != null && minCandleMapInfo[marketId] != null) {
            val priceVolume = tradeInfo[marketId]!!.accPriceVolume?.div(UNIT_PRICE)
            val rate = tradeInfo[marketId]!!.getPriceVolumeRate()
            Log.d(
                TAG, "[DEBUG] makeTradeMapInfo marketId: $marketId " +
                        "count: ${tradeInfo[marketId]!!.tickCount} " +
                        "rate: ${Format.percentFormat.format(rate)} " +
                        "highPrice: ${Format.nonZeroFormat.format(tradeInfo[marketId]!!.tickCount)} " +
                        "lowPrice: ${Format.nonZeroFormat.format(tradeInfo[marketId]!!.tickCount)} " +
                        "openPrice: ${Format.nonZeroFormat.format(tradeInfo[marketId]!!.tickCount)} " +
                        "closePrice: ${Format.nonZeroFormat.format(tradeInfo[marketId]!!.tickCount)} " +
                        "priceVolume: ${Format.nonZeroFormat.format(priceVolume)} " +
                        "avg1MinPriceVolume: ${Format.nonZeroFormat.format(avgPriceVolumePerMin?.div(UNIT_PRICE))} " +
                        "time: ${Format.timeFormat.format(tradeInfo[marketId]!!.timestamp)} "
            )
        }
    }

    private fun sortedMapList(it: String): Int? {
        return tradeInfo[it]?.tickCount
    }
}
