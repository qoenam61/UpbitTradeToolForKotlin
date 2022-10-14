package com.example.upbittrade.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.api.TradeFetcher
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.model.ResponseOrder
import com.example.upbittrade.model.TradeViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"

        val marketMapInfo = HashMap<String, MarketInfo>()
    }

    object Format {
        var nonZeroFormat = DecimalFormat("###,###,###,###")
        var zeroFormat = DecimalFormat("###,###,###,###.##")
        var zeroFormat2 = DecimalFormat("###,###,###,###.####")
        var percentFormat = DecimalFormat("###.##" + "%")
        var timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        @SuppressLint("SimpleDateFormat")
        var durationFormat = SimpleDateFormat("HH:mm:ss")
    }

    private lateinit var mainActivity: TradePagerActivity

    private var viewModel: TradeViewModel? = null

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = TradeViewModel(application = activity.application, object : TradeFetcher.PostOrderListener {
            override fun onInSufficientFunds(marketId: String, side: String, errorCode:Int, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onInSufficientFunds marketId: $marketId side: $side errorCode: $errorCode uuid: $uuid")
                if (side == "ask" && errorCode == 400) {

                }

                if (side == "bid" && errorCode == 400) {

                }
            }

            override fun onError(marketId: String, side: String?, errorCode: Int, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onError marketId: $marketId side: $side errorCode: $errorCode uuid: $uuid")
                if (side == "bid") {

                }

                if (side == "ask") {

                }
            }
        })
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
        viewModel?.resultMarketsInfo?.observe(viewCycleOwner) {
                marketsInfo ->
            makeMarketMapInfo(marketsInfo)
        }

        viewModel?.resultMinCandleInfo?.observe(viewCycleOwner) {
                minCandlesInfo ->
        }

        viewModel?.resultTradeInfo?.observe(viewCycleOwner) {
                tradesInfo ->
        }

        viewModel?.resultTickerInfo?.observe(viewCycleOwner) {
                tickersInfo ->
        }

        viewModel?.resultPostOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
        }

        viewModel?.resultSearchOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
        }

        viewModel?.resultDeleteOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
        }
        viewModel?.resultCheckOrderInfo?.observe(viewCycleOwner) {
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

    private fun makeMarketMapInfo(marketsInfo: List<MarketInfo>) {
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
                Log.i(TAG, "resultMarketsInfo - marketId: $marketId")
            }
        }
    }

}
