package com.example.upbittrade.fragment

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.PostOrderItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.*
import com.example.upbittrade.utils.BackgroundProcessor
import com.example.upbittrade.utils.InitPopupDialog
import com.example.upbittrade.utils.TradeAdapter
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.TRADE_LIST
import com.example.upbittrade.utils.TradeManager
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        const val LIMIT_AMOUNT = 6000.0
        const val BASE_TIME: Long = 3 * 60 * 1000
        const val THRESHOLD_RATE = 0.005
        const val THRESHOLD_TICK = 50
        const val THRESHOLD_AVG_MIN_AVG_DAY_PRICE_VOLUME = 0.1f

        private const val UNIT_REPEAT_MARKET_INFO = 30 * 60 * 1000
        private const val UNIT_MIN_CANDLE = 60
        private const val UNIT_MIN_CANDLE_COUNT = 24
        private const val UNIT_MONITOR_TIME: Long = 60 * 1000
        private const val UNIT_TRADE_COUNT = 3000
        private const val UNIT_PRICE = 1000000

        val marketMapInfo = HashMap<String, MarketInfo>()
        val tradeMonitorMapInfo = HashMap<String, TradeCoinInfo>()
        var tradePostMapInfo = HashMap<String, OrderCoinInfo>()
        var tradeResponseMapInfo = HashMap<String, ResponseOrder>()
    }

    object Format {
        var nonZeroFormat = DecimalFormat("###,###,###,###")
        var zeroFormat = DecimalFormat("###,###,###,###.#")
        var percentFormat = DecimalFormat("###.##" + "%")
        var timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    }

    object UserParam {
        var completed = false
        var priceToBuy: Double = LIMIT_AMOUNT
        var monitorTime: Long = BASE_TIME
        var thresholdRate: Double = THRESHOLD_RATE
        var thresholdTick: Int = UNIT_TRADE_COUNT
        var thresholdAccPriceVolumeRate: Float = THRESHOLD_AVG_MIN_AVG_DAY_PRICE_VOLUME
        var thresholdBidAskRate: Float = 0.1f
        var thresholdBidAskPriceRate: Float = 0.1f
    }

    lateinit var mainActivity: TradePagerActivity
    lateinit var tradeManager: TradeManager

    private var viewModel: TradeViewModel? = null
    private var processor: BackgroundProcessor? = null

    private val minCandleMapInfo = HashMap<String, TradeCoinInfo>()
    private val tradeMapInfo = HashMap<String, List<TradeInfo>>()

    private var monitorAdapter: TradeAdapter? = null
    private var tradeAdapter: TradeAdapter? = null

    private var monitorKeyList: List<String>? = null

    private var monitorListView: RecyclerView? = null
    private var tradeListView: RecyclerView? = null


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
        Format.timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val view = inflater.inflate(R.layout.fragment_trade, container, false)
        monitorAdapter = TradeAdapter(requireContext(), MONITOR_LIST)
        monitorListView = view.findViewById(R.id.monitor_list_view)
        monitorListView!!.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        monitorListView!!.adapter = monitorAdapter

        tradeAdapter = TradeAdapter(requireContext(), TRADE_LIST)
        tradeListView = view.findViewById(R.id.trade_list_view)
        tradeListView!!.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        tradeListView!!.adapter = tradeAdapter


        val initDialog = InitPopupDialog(requireContext())
        initDialog.show()

        val changeParamButton = view.findViewById<Button>(R.id.change_parameter)
        changeParamButton.setOnClickListener {
            initDialog.show()
        }

        tradeManager = TradeManager(object : TradeManager.TradeChangedListener {
            override fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo) {
                Log.d(TAG, "[DEBUG] onPostBid - key: $marketId")

                val bidPrice = orderCoinInfo.getBidPrice()
                val volume = (UserParam.priceToBuy / bidPrice!!).toString()
                processor?.registerProcess(PostOrderItem(POST_ORDER_INFO, marketId,
                    "bid", volume, bidPrice.toString(), "limit", UUID.randomUUID()))

                tradePostMapInfo[marketId] = orderCoinInfo
                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }
                processor?.registerProcess(TaskItem(TICKER_INFO, marketId))
            }

            override fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo) {
                Log.d(TAG, "[DEBUG] onPostAsk - key: $marketId")
            }
        })

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

            var marketId: String = minCandlesInfo.first().marketId.toString()
            var accPriceVolume = minCandlesInfo.fold(0.0) {
                acc: Double, minCandleInfo: Candle -> acc + minCandleInfo.candleAccTradePrice!!.toDouble()
            }
            val highPrice = minCandlesInfo.maxOf { it.tradePrice!!.toDouble() }
            val lowPrice = minCandlesInfo.minOf { it.tradePrice!!.toDouble() }
            val openPrice = minCandlesInfo.first().tradePrice!!.toDouble()
            val closePrice = minCandlesInfo.last().tradePrice!!.toDouble()
            minCandleMapInfo[marketId] = TradeCoinInfo(marketId,
                minCandlesInfo.size, minCandlesInfo.last().timestamp,
                highPrice, lowPrice, openPrice, closePrice, accPriceVolume)
        }

        viewModel?.resultTradeInfo?.observe(viewCycleOwner) {
                tradesInfo ->
            makeTradeMapInfo(tradesInfo)
        }

        viewModel?.resultTickerInfo?.observe(viewCycleOwner) {
                tickersInfo ->

            val marketId = tickersInfo.first().marketId
            val time: Long = SystemClock.uptimeMillis()
            val currentPrice = tickersInfo.first().tradePrice?.toDouble()
            Log.d(TAG, "[DEBUG] resultTickerInfo - marketId: $marketId currentPrice: $currentPrice time: $time")

            val postInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
            val responseOrder: ResponseOrder = tradeResponseMapInfo[marketId]!!

            postInfo.currentPrice = currentPrice
            postInfo.currentTime = time

            tradePostMapInfo[marketId!!] = tradeManager.updateTickerInfoToBuyList(tickersInfo,
                postInfo,
                responseOrder,
                processor!!
            )

            tradeAdapter?.notifyDataSetChanged()
        }

        viewModel?.resultPostOrderInfo?.observe(viewCycleOwner) {
            responseOrder ->
            val marketId = responseOrder.marketId
            val time: Long = SystemClock.uptimeMillis()
            val registerTime: Long? = tradePostMapInfo[marketId]?.registerTime

            tradeResponseMapInfo[marketId!!] = responseOrder

            Log.d(TAG, "[DEBUG] resultPostOrderInfo marketId: $marketId state: ${responseOrder.state} side: ${responseOrder.side} time: ${Format.timeFormat.format(time)}")

            if (registerTime == null) {
                tradePostMapInfo[marketId]?.registerTime = time
            }
            tradePostMapInfo[marketId]?.currentTime = time

            if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
                if (responseOrder.state.equals("wait")) {
                    tradePostMapInfo[marketId]?.state = OrderCoinInfo.State.WAIT
                    processor?.registerProcess(
                        TaskItem(
                            SEARCH_ORDER_INFO,
                            marketId,
                            UUID.fromString(responseOrder.uuid)
                        )
                    )
                } else if (responseOrder.state.equals("done") && responseOrder.remainingVolume?.toDouble() == 0.0
                    && tradePostMapInfo[marketId]?.tradeBuyTime == null
                ) {
                    tradePostMapInfo[marketId]?.registerTime = null
                    tradePostMapInfo[marketId]?.tradeBuyTime = time
                    tradePostMapInfo[marketId]?.state = OrderCoinInfo.State.BUY
                }

            }
            if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
                if (responseOrder.state.equals("wait")) {
                    tradePostMapInfo[marketId]?.state = OrderCoinInfo.State.WAIT
                } else if (responseOrder.state.equals("done") && responseOrder.remainingVolume?.toDouble() == 0.0) {

                }
            }


        }

        viewModel?.resultSearchOrderInfo?.observe(viewCycleOwner) {
            responseOrder ->

            val marketId = responseOrder.marketId
            val time: Long = SystemClock.uptimeMillis()
            tradeResponseMapInfo[marketId!!] = responseOrder

            Log.d(TAG, "[DEBUG] resultSearchOrderInfo marketId: $marketId state: ${responseOrder.state} side: ${responseOrder.side} time: ${Format.timeFormat.format(time)}")

            if (responseOrder.state.equals("done")  && responseOrder.remainingVolume?.toDouble() == 0.0) {
                tradePostMapInfo[marketId]?.tradeBuyTime = time
                tradePostMapInfo[marketId]?.state = OrderCoinInfo.State.BUY
                processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            }
            tradePostMapInfo[marketId]?.currentTime = time
        }

        viewModel?.resultDeleteOrderInfo?.observe(viewCycleOwner) {
            responseOrder ->
            val marketId = responseOrder.marketId

            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            processor?.unregisterProcess(TICKER_INFO, marketId!!)


            Log.d(TAG, "[DEBUG] resultDeleteOrderInfo marketId : $marketId")
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
        if (tradesInfoList.isNullOrEmpty()) {
            return
        }

        var marketId: String = tradesInfoList.first().marketId.toString()

        var tempInfo: List<TradeInfo> = if (tradeMapInfo[marketId] == null) {
            tradesInfoList.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UserParam.monitorTime
            }.reversed()
        } else {
            val addList = tradesInfoList.asReversed().filter {tradeInfo ->
                tradeMapInfo[marketId]!!.last().sequentialId < tradeInfo.sequentialId}
            val combineInfo = tradeMapInfo[marketId]!! + addList
            combineInfo.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UserParam.monitorTime }
        }

        val accPriceVolume = tempInfo.fold(0.0) {
            acc: Double, tradeInfo: TradeInfo -> acc + tradeInfo.getPriceVolume()
        }
        val highPrice = tempInfo.maxOf { it.tradePrice!!.toDouble() }
        val lowPrice = tempInfo.minOf { it.tradePrice!!.toDouble() }
        val openPrice = tempInfo.first().tradePrice!!.toDouble()
        val closePrice = tempInfo.last().tradePrice!!.toDouble()
        val avgAccPriceVolume =
            minCandleMapInfo[marketId]?.accPriceVolume?.div(UNIT_MIN_CANDLE * UNIT_MIN_CANDLE_COUNT)
                ?.times((UserParam.monitorTime / UNIT_MONITOR_TIME))

        val bid = tempInfo.fold(0) {
                acc: Int, tradeInfo: TradeInfo -> acc + addCount(tradeInfo, "BID")
        }

        val ask = tempInfo.fold(0) {
                acc: Int, tradeInfo: TradeInfo -> acc + addCount(tradeInfo, "ASK")
        }

        val bidPriceVolume = tempInfo.fold(0.0) {
                acc: Double, tradeInfo: TradeInfo -> acc + addPriceVolume(tradeInfo, "BID")
        }

        val askPriceVolume = tempInfo.fold(0.0) {
                acc: Double, tradeInfo: TradeInfo -> acc + addPriceVolume(tradeInfo, "ASK")
        }

        tradeMapInfo[marketId] = tempInfo
        tradeMonitorMapInfo[marketId] = TradeCoinInfo(marketId,
            tempInfo.size,
            tempInfo.last().timestamp,
            highPrice,
            lowPrice,
            openPrice,
            closePrice,
            accPriceVolume,
            avgAccPriceVolume,
            tempInfo.last().getDayChangeRate(),
            bid,
            ask,
            bidPriceVolume,
            askPriceVolume
        )

        monitorKeyList = (tradeMonitorMapInfo.filter {
            (it.value.tickCount!! > UserParam.thresholdTick
                && it.value.getAvgAccVolumeRate() > UserParam.thresholdAccPriceVolumeRate)
        } as HashMap<String, TradeCoinInfo>)
            .toSortedMap(compareByDescending { sortedMapList(it) }).keys.toList()

        tradeManager.setList(TradeManager.Type.POST_BID, monitorKeyList)

        monitorAdapter!!.monitorKeyList = monitorKeyList
        monitorAdapter!!.notifyDataSetChanged()

        if (tradeMonitorMapInfo[marketId] != null && minCandleMapInfo[marketId] != null) {
            val priceVolume = tradeMonitorMapInfo[marketId]!!.accPriceVolume?.div(UNIT_PRICE)
            val rate = tradeMonitorMapInfo[marketId]!!.getAvgAccVolumeRate()
            Log.d(
                TAG, "[DEBUG] makeTradeMapInfo marketId: $marketId " +
                        "count: ${tradeMonitorMapInfo[marketId]!!.tickCount} " +
                        "rate: ${Format.percentFormat.format(rate)} " +
                        "highPrice: ${Format.nonZeroFormat.format(tradeMonitorMapInfo[marketId]!!.highPrice)} " +
                        "lowPrice: ${Format.nonZeroFormat.format(tradeMonitorMapInfo[marketId]!!.lowPrice)} " +
                        "openPrice: ${Format.nonZeroFormat.format(tradeMonitorMapInfo[marketId]!!.openPrice)} " +
                        "closePrice: ${Format.nonZeroFormat.format(tradeMonitorMapInfo[marketId]!!.closePrice)} " +
                        "priceVolume: ${Format.nonZeroFormat.format(priceVolume)} " +
                        "bid: ${Format.nonZeroFormat.format(bid)} " +
                        "ask: ${Format.nonZeroFormat.format(ask)} " +
                        "bid/ask: ${Format.percentFormat.format(tradeMonitorMapInfo[marketId]!!.getBidAskRate())} " +
                        "bidPrice: ${Format.nonZeroFormat.format(bidPriceVolume)} " +
                        "askPrice: ${Format.nonZeroFormat.format(askPriceVolume)} " +
                        "bidPrice/askPrice: ${Format.percentFormat.format(tradeMonitorMapInfo[marketId]!!.getBidAskRate())} " +
                        "avg1MinPriceVolume: ${Format.nonZeroFormat.format(avgAccPriceVolume?.div(UNIT_PRICE))} " +
                        "time: ${Format.timeFormat.format(tradeMonitorMapInfo[marketId]!!.timestamp)} "
            )
        }
    }

    private fun addCount(it: TradeInfo, string: String): Int {
        if (it.askBid.equals(string)) {
            return 1
        }
        return 0
    }

    private fun addPriceVolume(it: TradeInfo, string: String): Double {
        if (it.askBid.equals(string)) {
            return it.tradeVolume!!.toDouble() * it.tradePrice!!.toDouble()
        }
        return 0.0
    }

    private fun sortedMapList(it: String): Int? {
        return tradeMonitorMapInfo[it]?.tickCount
    }
}
