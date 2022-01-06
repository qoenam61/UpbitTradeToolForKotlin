package com.example.upbittrade.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.model.OrderCoinInfo


private var reportListView: RecyclerView? = null
private var reportAdapter: TradeAdapter? = null

private var profitPrice: TextView? = null
private var profitPriceRate: TextView? = null

class TotalResultDialog: Dialog {
    constructor(context: Context): super(context) {
        setContentView(R.layout.report_list_layout)

        reportAdapter = TradeAdapter(context, TradeAdapter.Companion.Type.REPORT_LIST)
        reportListView = findViewById(R.id.trade_report_list)
        reportListView!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        reportListView!!.adapter = reportAdapter

        profitPriceRate = findViewById(R.id.profit_price_rate)
        profitPrice = findViewById(R.id.profit_price)
    }

    fun setList(list: List<OrderCoinInfo>) {
        var askPriceAmount = 0.0
        var bidPriceAmount = 0.0
        list.forEach {
            if (it.askPrice != null && it.volume != null) {
                askPriceAmount += it.askPrice!! * it.volume!!
                bidPriceAmount += it.getBidPrice()!! * it.volume!!
            }
        }

        val profit = (askPriceAmount - bidPriceAmount)
        profitPrice?.text = getZeroFormatString(profit)
        profitPrice?.setTextColor(getColor(profit))


        val priceRate = profit / TradeFragment.UserParam.priceToBuy
        profitPriceRate!!.text = TradeFragment.Format.percentFormat.format(priceRate)
        profitPriceRate?.setTextColor(getColor(priceRate))

        reportAdapter?.reportList = list
        reportAdapter?.notifyDataSetChanged()
    }

    private fun getZeroFormatString(value: Double?): String {
        value ?: return ""
        return when {
            value < 100.0 -> {
                TradeFragment.Format.zeroFormat.format(value)
            }
            value < 1.0 -> {
                TradeFragment.Format.zeroFormat2.format(value)
            }
            else -> {
                TradeFragment.Format.nonZeroFormat.format(value)
            }
        }
    }

    private fun getColor(value: Double?): Int {
        return getColor(value, 0.0)
    }

    private fun getColor(value: Double?, threshold: Double): Int {
        value ?: return Color.DKGRAY
        return when {
            value.compareTo(threshold) > 0 -> {
                Color.RED
            }
            value.compareTo(threshold) < 0 -> {
                Color.BLUE
            }
            else -> {
                Color.BLACK
            }
        }
    }
}