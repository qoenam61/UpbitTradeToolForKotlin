package com.example.upbittrade.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.TradeViewModel
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
    }

    private lateinit var mainActivity: TradePagerActivity

    lateinit var viewModel: TradeViewModel

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

        return view
    }

    override fun onStart() {
        super.onStart()
//        Log.i(TAG, "onStart: ")
        val viewCycleOwner = viewLifecycleOwner
        viewModel.resultMarketsInfo.observe(viewCycleOwner) {
                marketsInfo ->
        }

        viewModel.resultMinCandleInfo.observe(viewCycleOwner) {
                minCandlesInfo ->
        }

        viewModel.resultTradeInfo.observe(viewCycleOwner) {
                tradesInfo ->
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
