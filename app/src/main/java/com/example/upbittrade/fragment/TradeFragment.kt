package com.example.upbittrade.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.fragment.TradeFragment.UserParam.baseTime
import com.example.upbittrade.fragment.TradeFragment.UserParam.limitAmount
import com.example.upbittrade.fragment.TradeFragment.UserParam.thresholdRate
import com.example.upbittrade.fragment.TradeFragment.UserParam.thresholdTick
import com.example.upbittrade.model.*
import com.example.upbittrade.utils.BackgroundProcessor
import com.example.upbittrade.utils.TradeFragmentView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        private const val LIMIT_AMOUNT = 10000.0
        private const val BASE_TIME = 3.0
        private const val THRESHOLD_RATE = 0.03
        private const val THRESHOLD_TICK = 1500

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
        var limitAmount: Double = 0.0
        var baseTime: Double = 0.0
        var thresholdRate: Double = 0.0
        var thresholdTick: Double = 0.0
    }

    lateinit var mainActivity: TradePagerActivity
    private var viewModel: TradeViewModel? = null
    private var processor: BackgroundProcessor? = null
    private val marketMapInfo = HashMap<String, MarketInfo>()
    private val minCandleMapInfo = HashMap<String, ResultTradeInfo>()
    private val tradeMapInfo = HashMap<String, List<TradeInfo>>()
    private var tradeView: TradeFragmentView? = null
    private var monitorAdapter: TradeFragmentView.TradeAdapter? = null
//    private var tradeAdapter: TradeFragmentView.TradeAdapter? = null
//    private var resultAdapter: TradeFragmentView.TradeAdapter? = null

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
        tradeView = TradeFragmentView(view)
        monitorAdapter = TradeFragmentView.TradeAdapter(TradeFragmentView.Companion.Type.MONITOR_LIST)
//        tradeAdapter = TradeFragmentView.TradeAdapter(TradeFragmentView.Companion.Type.TRADE_LIST)
//        resultAdapter = TradeFragmentView.TradeAdapter(TradeFragmentView.Companion.Type.RESULT_LIST)

        val monitorListView = view.findViewById<RecyclerView>(R.id.monitor_list_view)
        val tradeListView = view.findViewById<RecyclerView>(R.id.monitor_list_view)
        val resultListView = view.findViewById<RecyclerView>(R.id.monitor_list_view)

        val monitorLm = LinearLayoutManager(context)
        monitorListView.layoutManager = monitorLm
//        val tradeLm = LinearLayoutManager(context)
//        tradeListView.layoutManager = tradeLm
//        val resultLm = LinearLayoutManager(context)
//        resultListView.layoutManager = resultLm


        monitorListView.adapter = monitorAdapter
//        tradeListView.adapter = tradeAdapter
//        resultListView.adapter = resultAdapter

        val buyingPriceText = view.findViewById<TextView>(R.id.trade_buying_price)
        val monitorTimeText = view.findViewById<TextView>(R.id.trade_monitor_time)
        val monitorRateText = view.findViewById<TextView>(R.id.trade_monitor_rate)
        val monitorTickText = view.findViewById<TextView>(R.id.trade_monitor_tick)
        val buyingPriceEditText = view.findViewById<EditText>(R.id.trade_input_buying_price)
        val monitorTimeEditText = view.findViewById<EditText>(R.id.trade_input_monitor_time)
        val monitorRateEditText = view.findViewById<EditText>(R.id.trade_input_monitor_rate)
        val monitorTickEditText = view.findViewById<EditText>(R.id.trade_input_monitor_tick)

        buyingPriceText.text = Format.nonZeroFormat.format(LIMIT_AMOUNT)
        monitorTimeText.text = Format.zeroFormat.format(BASE_TIME)
        monitorRateText.text = Format.percentFormat.format(THRESHOLD_RATE)
        monitorTickText.text = Format.nonZeroFormat.format(THRESHOLD_TICK)
        buyingPriceEditText.setText(Format.nonZeroFormat.format(LIMIT_AMOUNT))
        monitorTimeEditText.setText(Format.zeroFormat.format(BASE_TIME))
        monitorRateEditText.setText((THRESHOLD_RATE * 100).toString())
        monitorTickEditText.setText(Format.nonZeroFormat.format(THRESHOLD_TICK))

        val applyButton = view.findViewById<Button>(R.id.trade_input_button)
        applyButton?.setOnClickListener {
            val buyingPrice = buyingPriceEditText.text.toString()
            val monitorTime = monitorTimeEditText.text.toString()
            val monitorRate = monitorRateEditText.text.toString()
            val monitorTick = monitorTickEditText.text.toString()
            try {
                limitAmount =
                    if (buyingPrice.isNotBlank()) buyingPrice.replace(",", "").toDouble()
                    else LIMIT_AMOUNT

                baseTime =
                    if (monitorTime.isNotBlank()) monitorTime.toDouble() * 60 * 1000
                    else BASE_TIME * 60 * 1000

                thresholdRate =
                    if (monitorRate.isNotBlank()) monitorRate.replace("%", "").toDouble() / 100
                    else THRESHOLD_RATE

                thresholdTick =
                    if (monitorTick.isNotBlank()) monitorTick.replace(",", "").toDouble()
                    else THRESHOLD_TICK.toDouble()

            } catch (e: NumberFormatException) {
                Log.e(
                    TAG,
                    "Error NumberFormatException"
                )
            }

            buyingPriceText.text = Format.nonZeroFormat.format(limitAmount)
            monitorTimeText.text = Format.zeroFormat.format(baseTime / (60 * 1000))
            monitorRateText.text = Format.percentFormat.format(thresholdRate)
            monitorTickText.text = Format.nonZeroFormat.format(thresholdTick)
            buyingPriceEditText.setText(Format.nonZeroFormat.format(limitAmount))
            monitorTimeEditText.setText(Format.zeroFormat.format(baseTime / (60 * 1000)))
            monitorRateEditText.setText((thresholdRate * 100).toString())
            monitorTickEditText.setText(Format.nonZeroFormat.format(thresholdTick))

            buyingPriceEditText.clearFocus()
            monitorTimeEditText.clearFocus()
            monitorRateEditText.clearFocus()
            monitorTickEditText.clearFocus()

            val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(buyingPriceEditText.windowToken, 0)
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
        tradeInfo[marketId] = ResultTradeInfo(marketId, tempInfo.size, tempInfo.last().timestamp, highPrice, lowPrice, openPrice, closePrice, accPriceVolume, avgPriceVolumePerMin)

        monitorAdapter!!.monitorMap = tradeInfo.filter { (it.value.tickCount!! > 1 && it.value.getPriceVolumeRate() > 0.1)} as HashMap<String, ResultTradeInfo>
        monitorAdapter!!.notifyDataSetChanged()
        Log.d(TAG, "[DEBUG] makeTradeMapInfo - size: ${monitorAdapter!!.monitorMap.size}")

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
}
