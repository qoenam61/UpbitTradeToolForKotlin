package com.example.upbittrade.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.adapter.TradeItem
import com.example.upbittrade.adapter.TradeReportListAdapter
import com.example.upbittrade.model.MarketInfo
import java.util.HashMap

class ReportDialog(context: Context): Dialog(context) {

    private var reportAdapter: TradeReportListAdapter? = null
    private var reportListView: RecyclerView? = null

    init {
        setContentView(R.layout.report_list_layout)

        reportAdapter = TradeReportListAdapter()
        reportListView = findViewById(R.id.trade_report_list)
        reportListView!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        reportListView!!.adapter = reportAdapter

    }

    fun setMarketMap(marketsInfo: HashMap<String, MarketInfo>) {
        reportAdapter?.marketsMapInfo?.putAll(marketsInfo)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItem(tradeItem: TradeItem) {
        reportAdapter?.reportList?.add(tradeItem)
        reportAdapter?.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAllItem(tradeList: List<TradeItem>) {
        reportAdapter?.reportList?.addAll(tradeList)
        reportAdapter?.notifyDataSetChanged()
    }
}