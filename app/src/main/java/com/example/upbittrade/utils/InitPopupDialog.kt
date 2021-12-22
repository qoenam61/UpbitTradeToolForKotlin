package com.example.upbittrade.utils

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment

class InitPopupDialog: Dialog {

    constructor(context: Context): super(context) {
        setContentView(R.layout.init_parameter_popup)
        val buyingPriceText = findViewById<TextView>(R.id.trade_buying_price)
        val monitorTimeText = findViewById<TextView>(R.id.trade_monitor_time)
        val monitorRateText = findViewById<TextView>(R.id.trade_monitor_rate)
        val monitorTickText = findViewById<TextView>(R.id.trade_monitor_tick)
        val avgMinVsAvgDayText = findViewById<TextView>(R.id.min_price_per_avg_price)
        val buyingPriceEditText = findViewById<EditText>(R.id.trade_input_buying_price)
        val monitorTimeEditText = findViewById<EditText>(R.id.trade_input_monitor_time)
        val monitorRateEditText = findViewById<EditText>(R.id.trade_input_monitor_rate)
        val monitorTickEditText = findViewById<EditText>(R.id.trade_input_monitor_tick)
        val avgMinVsAvgDayEditText = findViewById<EditText>(R.id.input_min_price_per_avg_price)

        buyingPriceText.text = TradeFragment.Format.nonZeroFormat.format(TradeFragment.LIMIT_AMOUNT)
        monitorTimeText.text = TradeFragment.Format.zeroFormat.format(TradeFragment.BASE_TIME / (60 * 1000))
        monitorRateText.text = TradeFragment.Format.percentFormat.format(TradeFragment.THRESHOLD_RATE)
        monitorTickText.text = TradeFragment.Format.nonZeroFormat.format(TradeFragment.THRESHOLD_TICK)
        avgMinVsAvgDayText.text = TradeFragment.Format.percentFormat.format(TradeFragment.THRESHOLD_ACC_PRICE_VOLUME_RATE)

        buyingPriceEditText.setText(TradeFragment.Format.nonZeroFormat.format(TradeFragment.LIMIT_AMOUNT))
        monitorTimeEditText.setText(TradeFragment.Format.zeroFormat.format(TradeFragment.BASE_TIME /  (60 * 1000)))
        monitorRateEditText.setText((TradeFragment.THRESHOLD_RATE * 100).toString())
        monitorTickEditText.setText(TradeFragment.Format.nonZeroFormat.format(TradeFragment.THRESHOLD_TICK))
        avgMinVsAvgDayEditText.setText(TradeFragment.Format.percentFormat.format(TradeFragment.THRESHOLD_ACC_PRICE_VOLUME_RATE))

        val applyButton = findViewById<Button>(R.id.trade_input_button)
        applyButton?.setOnClickListener {
            val buyingPrice = buyingPriceEditText.text.toString()
            val monitorTime = monitorTimeEditText.text.toString()
            val monitorRate = monitorRateEditText.text.toString()
            val monitorTick = monitorTickEditText.text.toString()
            val avgMinVsAvgDay = avgMinVsAvgDayEditText.text.toString()
            try {
                TradeFragment.UserParam.priceToBuy =
                    if (buyingPrice.isNotBlank()) buyingPrice.replace(",", "").toDouble()
                    else TradeFragment.LIMIT_AMOUNT

                TradeFragment.UserParam.monitorTime =
                    if (monitorTime.isNotBlank()) monitorTime.toLong() * 60 * 1000
                    else TradeFragment.BASE_TIME

                TradeFragment.UserParam.thresholdRate =
                    if (monitorRate.isNotBlank()) monitorRate.replace("%", "").toDouble() / 100
                    else TradeFragment.THRESHOLD_RATE

                TradeFragment.UserParam.thresholdTick =
                    if (monitorTick.isNotBlank()) monitorTick.replace(",", "").toInt()
                    else TradeFragment.THRESHOLD_TICK

                TradeFragment.UserParam.thresholdAccPriceVolumeRate =
                    if (avgMinVsAvgDay.isNotBlank()) avgMinVsAvgDay.replace("%", "").toFloat() / 100
                    else TradeFragment.THRESHOLD_ACC_PRICE_VOLUME_RATE

            } catch (e: NumberFormatException) {
                Log.e(
                    TradeFragment.TAG,
                    "Error NumberFormatException"
                )
            }

            buyingPriceText.text =
                TradeFragment.Format.nonZeroFormat.format(TradeFragment.UserParam.priceToBuy)
            monitorTimeText.text =
                TradeFragment.Format.zeroFormat.format(TradeFragment.UserParam.monitorTime / (60 * 1000))
            monitorRateText.text =
                TradeFragment.Format.percentFormat.format(TradeFragment.UserParam.thresholdRate)
            monitorTickText.text =
                TradeFragment.Format.nonZeroFormat.format(TradeFragment.UserParam.thresholdTick)
            avgMinVsAvgDayText.text =
                TradeFragment.Format.percentFormat.format(TradeFragment.UserParam.thresholdAccPriceVolumeRate)

            buyingPriceEditText.setText(TradeFragment.Format.nonZeroFormat.format(TradeFragment.UserParam.priceToBuy))
            monitorTimeEditText.setText(TradeFragment.Format.zeroFormat.format(TradeFragment.UserParam.monitorTime / (60 * 1000)))
            monitorRateEditText.setText((TradeFragment.UserParam.thresholdRate * 100).toString())
            monitorTickEditText.setText(TradeFragment.Format.nonZeroFormat.format(TradeFragment.UserParam.thresholdTick))
            avgMinVsAvgDayEditText.setText((TradeFragment.UserParam.thresholdAccPriceVolumeRate * 100).toString())

            buyingPriceEditText.clearFocus()
            monitorTimeEditText.clearFocus()
            monitorRateEditText.clearFocus()
            monitorTickEditText.clearFocus()
            avgMinVsAvgDayEditText.clearFocus()

            val inputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(buyingPriceEditText.windowToken, 0)

            dismiss()
            TradeFragment.UserParam.completed = true
        }
    }
}