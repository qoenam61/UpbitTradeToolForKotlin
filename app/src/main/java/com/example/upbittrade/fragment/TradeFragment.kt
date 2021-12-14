package com.example.upbittrade.fragment

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
import com.example.upbittrade.R
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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TradeFragment(private val viewModel: TradeViewModel): Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        private const val LIMIT_AMOUNT = 10000.0
        private const val BASE_TIME = 3.0
        private const val THRESHOLD_RATE = 0.03
        private const val THRESHOLD_TICK = 1500
        private const val UNIT_MIN_CANDLE = "60"
        private const val UNIT_MONITOR_TIME = 60 * 1000

        private var processor: BackgroundProcessor? = null
        private val marketMapInfo = HashMap<String, MarketInfo>()
        private val minCandleMapInfo = HashMap<String, Candle>()
        private val tradeMapInfo = HashMap<String, List<TradeInfo>>()

        private val dayCandleListInfo = ArrayList<DayCandle>()
        private val dayCandleMapInfo = HashMap<String, DayCandle>()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processor = BackgroundProcessor(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trade, container, false)

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
        viewModel.resultMarketsInfo?.observe(viewCycleOwner) {
            marketsInfo ->
            marketMapInfo.clear()
            val iterator = marketsInfo.iterator()
            while (iterator.hasNext()) {
                val marketInfo: MarketInfo = iterator.next()
                val name = marketInfo.market
                if (name?.contains("KRW-") == true
                    && marketInfo.marketWarning?.contains("CAUTION") == false) {
                    marketMapInfo[name] = marketInfo
                    Log.d(TAG, "[DEBUG] resultMarketsInfo: $name")
                    processor?.registerProcess(ExtendCandleItem(MIN_CANDLE_INFO, UNIT_MIN_CANDLE, name, 1))
                }
            }
        }

        viewModel.resultMinCandleInfo?.observe(viewCycleOwner) {
            minCandlesInfo ->
            val iterator = minCandlesInfo.reversed().iterator()
            Format.timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            var marketId: String? = null
            while (iterator.hasNext()) {
                val minCandle: Candle = iterator.next()
                marketId = minCandle.marketId.toString()
                minCandleMapInfo[marketId] = minCandle
                Log.d(TAG, "[DEBUG] resultMinCandleInfo: $marketId " +
                        "time: ${Format.timeFormat.format(minCandle.timestamp)} " +
                        "get1minAverageTradePrice: ${minCandle.get1minAverageTradePrice(UNIT_MIN_CANDLE.toInt()).div(1000000).toInt()}")
            }
            registerTradeInfo(minCandleMapInfo)
        }

        viewModel.resultDayCandleInfo?.observe(viewCycleOwner) {
            dayCandlesInfo ->
            val dayCandle: DayCandle = dayCandlesInfo[0]
            val name = dayCandle.marketId.toString()
            dayCandleMapInfo[name] = dayCandle

            val tempDayCandleList = ArrayList<DayCandle>(dayCandleMapInfo.values)
            tempDayCandleList.sortedByDescending {
                it.getCenterPrice()
            }

            val iterator = dayCandlesInfo.iterator()
            while (iterator.hasNext()) {
                val dayCandle: DayCandle = iterator.next()
                val name = dayCandle.marketId.toString()
                dayCandleMapInfo[name] = dayCandle
                Log.d(TAG, "[DEBUG] resultDayCandleInfo: $name " +
                        "time: ${Format.timeFormat.format(dayCandle.timestamp)} " +
                        "rate: ${dayCandle.candleAccTradePrice?.toDouble()?.div(1000000)?.toInt()}")
            }
        }

        viewModel.resultTradeInfo?.observe(viewCycleOwner) {
            tradesInfo ->
            makeTradeMapInfo(tradesInfo)
        }

    }

    override fun onResume() {
        super.onResume()
        processor?.registerProcess(TaskItem(MARKETS_INFO))
        processor?.start()

    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[DEBUG] onPause: ")
        processor?.release()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "[DEBUG] onStop: ")
    }

    private fun registerTradeInfo(maps: HashMap<String, Candle>) {
        val iterator = maps.values.iterator()
        while(iterator.hasNext()) {
            val tradeInfo = iterator.next()
            processor?.registerProcess(CandleItem(TRADE_INFO, tradeInfo.marketId, 3000))
        }
    }

    private fun makeTradeMapInfo(tradesInfoList: List<TradeInfo>) {
        var marketId: String = tradesInfoList.first().marketId.toString()


        var tempInfo: List<TradeInfo>? = null

        if (tradeMapInfo[marketId] == null) {
            Log.d(TAG, "[DEBUG] makeTradeMapInfo: null")
            tempInfo = tradesInfoList.filter { tradeInfo ->
                tradesInfoList.first().timestamp!!.toLong() - tradeInfo.timestamp!!.toLong() < UNIT_MONITOR_TIME
            }.reversed()
        } else {
            Log.d(TAG, "[DEBUG] makeTradeMapInfo: else")
            val addList = tradesInfoList.filter {tradeInfo ->
                tradeMapInfo[marketId]!!.last().sequentialId < tradeInfo.sequentialId}.reversed()
            val combineInfo = tradeMapInfo[marketId]!! + addList

            val iterator: Iterator<TradeInfo> = combineInfo.listIterator()
            while (iterator.hasNext()) {
                val tradeInfo: TradeInfo = iterator.next()
                marketId = tradeInfo.marketId.toString()
                Log.d(TAG, "[DEBUG] makeTradeMapInfo marketId: $marketId " +
                        "time: ${Format.timeFormat.format(tradeInfo.timestamp)}")
            }

            tempInfo = combineInfo.filter { tradeInfo ->
                tradesInfoList.first().timestamp!!.toLong() - tradeInfo.timestamp!!.toLong() < UNIT_MONITOR_TIME }
        }
        tradeMapInfo[marketId] = tempInfo
    }
}
