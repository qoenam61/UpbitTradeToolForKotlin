package com.example.upbittrade.utils

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
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
            if (it.sellPrice != null && it.volume != null) {
                askPriceAmount += it.sellPrice!! * it.volume!!
                bidPriceAmount += it.getBidPrice()!! * it.volume!!
            }
        }

        profitPrice!!.text = (askPriceAmount - bidPriceAmount).toString()
        profitPriceRate!!.text = ((askPriceAmount - bidPriceAmount) / bidPriceAmount).toString()

        reportAdapter?.reportList = list
        reportAdapter?.notifyDataSetChanged()
    }
}