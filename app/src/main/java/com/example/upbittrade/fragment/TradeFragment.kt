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
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.fragment.TradeFragment.UserParam.baseTime
import com.example.upbittrade.fragment.TradeFragment.UserParam.limitAmount
import com.example.upbittrade.fragment.TradeFragment.UserParam.thresholdRate
import com.example.upbittrade.fragment.TradeFragment.UserParam.thresholdTick
import com.example.upbittrade.model.Candle
import com.example.upbittrade.model.DayCandle
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.model.TradeViewModel
import com.example.upbittrade.utils.BackgroundProcessor
import java.text.DecimalFormat
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
        private var processor: BackgroundProcessor? = null
        private val marketMapInfo = HashMap<String, MarketInfo>()
        private val dayCandleListInfo = ArrayList<DayCandle>()
        private val dayCandleMapInfo = HashMap<String, DayCandle>()
    }

    object Format {
        var nonZeroFormat = DecimalFormat("###,###,###,###")
        var zeroFormat = DecimalFormat("###,###,###,###.#")
        var percentFormat = DecimalFormat("###.##" + "%")
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
            inputMethodManager!!.hideSoftInputFromWindow(buyingPriceEditText.windowToken, 0)
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
                    processor?.registerProcess(ExtendCandleItem(MIN_CANDLE_INFO, "60", name, 24 * 7))
                }
            }
        }

        viewModel.resultMinCandleInfo?.observe(viewCycleOwner) {
            minCandlesInfo ->
            val iterator = minCandlesInfo.iterator()
            while (iterator.hasNext()) {
                val minCandle: Candle = iterator.next()
                val name = minCandle.market.toString()

                Log.d(TAG, "[DEBUG] resultMinCandleInfo: $name rate: ${minCandle.getTradePrice().div(1000000).toInt()}")
            }
        }


        viewModel.resultDayCandleInfo?.observe(viewCycleOwner) {
            dayCandleInfo ->
            val dayCandle: DayCandle = dayCandleInfo[0]
            val name = dayCandle.market.toString()
            dayCandleMapInfo[name] = dayCandle

            val tempDayCandleList = ArrayList<DayCandle>(dayCandleMapInfo.values)
            tempDayCandleList.sortedByDescending {
                it.getCenterPrice()
            }

            val iterator = dayCandleInfo.iterator()
            while (iterator.hasNext()) {
                val dayCandle: DayCandle = iterator.next()
                val name = dayCandle.market.toString()
                dayCandleMapInfo[name] = dayCandle
                Log.d(TAG, "[DEBUG] resultDayCandleInfo: $name rate: ${dayCandle.candleAccTradePrice?.toDouble()?.div(1000000)?.toInt()}")
            }
            Log.d(TAG, "[DEBUG] resultDayCandleInfo end:")

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
        processor?.interrupt()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "[DEBUG] onStop: ")
    }
}
