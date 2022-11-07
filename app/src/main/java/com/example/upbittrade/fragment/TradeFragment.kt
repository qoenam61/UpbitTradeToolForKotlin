package com.example.upbittrade.fragment

import android.annotation.SuppressLint
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
import com.dinuscxj.progressbar.CircleProgressBar
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.adapter.MonitorListAdapter
import com.example.upbittrade.adapter.TradeListAdapter
import com.example.upbittrade.adapter.TradeReportListAdapter
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.TradeViewModel
import com.example.upbittrade.utils.ReportDialog
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
    }

    private lateinit var mainActivity: TradePagerActivity

    lateinit var viewModel: TradeViewModel
    lateinit var monitorListAdapter: MonitorListAdapter
    lateinit var tradeListAdapter: TradeListAdapter
    lateinit var reportPopup: ReportDialog
    lateinit var circleBar: CircleProgressBar

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = mainActivity.viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trade, container, false)

        Log.d(TAG, "onCreateView: ")
        monitorListAdapter = MonitorListAdapter()
        val monitorList = view.findViewById<RecyclerView>(R.id.monitor_list_view)
        monitorList!!.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        monitorList.adapter = monitorListAdapter

        tradeListAdapter = TradeListAdapter()
        val tradeList = view.findViewById<RecyclerView>(R.id.trade_list_view)
        tradeList!!.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        tradeList.adapter = tradeListAdapter

        reportPopup = ReportDialog(requireContext())
        val totalResultButton = view.findViewById<Button>(R.id.total_result)
        totalResultButton.setOnClickListener {
            reportPopup.show()
        }

        circleBar = view.findViewById(R.id.circle_bar)
        circleBar.max = monitorListAdapter.monitorList.size
        circleBar.progress = 0

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
//        Log.i(TAG, "onStart: ")
        val viewCycleOwner = viewLifecycleOwner
        viewModel.searchMarketsMapInfo.observe(viewCycleOwner) {
                marketsInfo ->
            monitorListAdapter.marketsMapInfo = marketsInfo
            tradeListAdapter.marketsMapInfo = marketsInfo
            reportPopup.setMarketMap(marketsInfo)

            mainActivity.viewModel.monitorMap?.let { monitorListAdapter.monitorMap.putAll(it) }
            mainActivity.viewModel.monitorList?.let {
                monitorListAdapter.monitorList.clear()
                monitorListAdapter.monitorList.addAll(it)
            }
            monitorListAdapter.notifyDataSetChanged()

            mainActivity.viewModel.tradeMap?.let { tradeListAdapter.tradeMap.putAll(it) }
            mainActivity.viewModel.tradeList?.let {
                tradeListAdapter.tradeList.clear()
                tradeListAdapter.tradeList.addAll(it)
            }
            tradeListAdapter.notifyDataSetChanged()

            mainActivity.viewModel.reportList?.let { reportPopup.setAllItem(it)}
        }

        viewModel.addMonitorItem.observe(viewCycleOwner) {
                monitorItem -> monitorListAdapter.setItem(monitorItem)
        }

        viewModel.removeMonitorItem.observe(viewCycleOwner) {
                marketId -> monitorListAdapter.removeItem(marketId)
        }

        var circleProgress = 0
        viewModel.updateMonitorItem.observe(viewCycleOwner) {
                minCandlesInfo ->
            monitorListAdapter.updateItem(minCandlesInfo)
            tradeListAdapter.updateItem(minCandlesInfo)

            val monitorSize = monitorListAdapter.monitorList.size
            if (monitorSize == 0) {
                circleBar.max = monitorListAdapter.marketsMapInfo.size
            } else {
                circleBar.max = monitorSize
            }
            if (circleBar.max != 0) {
                circleBar.progress = (circleProgress++) % circleBar.max
                if (circleProgress == circleBar.max) {
                    circleProgress = 0
                }
            }
        }

        viewModel.addTradeInfo.observe(viewCycleOwner) {
            tradeInfo ->
            tradeListAdapter.setItem(tradeInfo)
        }

        viewModel.removeTradeInfo.observe(viewCycleOwner) {
                marketId ->
//            tradeListAdapter.removeItem(marketId)
        }

        viewModel.updateTradeInfoData.observe(viewCycleOwner) {
                tradeInfoData ->
            monitorListAdapter.updateItem(tradeInfoData)
            tradeListAdapter.updateItem(tradeInfoData)
        }

        viewModel.addReportItem.observe(viewCycleOwner) {
            reportPopup.setItem(it)
        }

        viewModel.resultTickerInfo.observe(viewCycleOwner) {
                tickersInfo ->
        }

        viewModel.resultPostOrderInfo.observe(viewCycleOwner) {
                responseOrder ->
        }

        viewModel.resultSearchOrderInfo.observe(viewCycleOwner) {
                responseOrder ->
        }

        viewModel.resultDeleteOrderInfo.observe(viewCycleOwner) {
                responseOrder ->
        }
        viewModel.resultCheckOrderInfo.observe(viewCycleOwner) {
                responseOrder ->
            checkOrderInfo(responseOrder)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun checkOrderInfo(responseOrderList: List<ResponseOrder>?) {
        if (responseOrderList.isNullOrEmpty()) {
            return
        }
        val responseOrder = responseOrderList.first()
        val marketId = responseOrder.marketId!!
        val createdAt = responseOrder.createdAt ?: return
        val createdTime: Long?
        val price = responseOrder.price?.toDouble()
        val volume = responseOrder.volume?.toDouble()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
        val date = sdf.parse(createdAt)
        createdTime = date?.time
    }

}
