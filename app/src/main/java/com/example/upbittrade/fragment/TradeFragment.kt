package com.example.upbittrade.fragment

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
import com.example.upbittrade.R
import com.example.upbittrade.activity.TradePagerActivity
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.api.TradeFetcher
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.PostOrderItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.*
import com.example.upbittrade.utils.*
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.*
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
        const val THRESHOLD_RATE = 0.01
        const val THRESHOLD_TICK = 100
        const val THRESHOLD_ACC_PRICE_VOLUME_RATE = 1f
        const val THRESHOLD_BID_ASK_RATE = 0.5f
        const val THRESHOLD_BID_ASK_PRICE_VOLUME_RATE = 0.5f

        private const val UNIT_REPEAT_MARKET_INFO = 30 * 60 * 1000
        private const val UNIT_REPEAT_MARKET_INFO_SHORT = 10 * 60 * 1000
        private const val UNIT_MIN_CANDLE = 60
        private const val UNIT_MIN_CANDLE_COUNT = 24
        private const val UNIT_MONITOR_TIME: Long = 60 * 1000
        private const val UNIT_TRADE_COUNT = 3000
        private const val UNIT_PRICE = 1000000

        val marketMapInfo = HashMap<String, MarketInfo>()

        val tradeMonitorMapInfo = HashMap<String, TradeCoinInfo>()
        var tradePostMapInfo = HashMap<String, OrderCoinInfo>()
        var tradeReportListInfo = ArrayList<OrderCoinInfo>()

        var tradeResponseMapInfo = HashMap<String, ResponseOrder>()
    }

    object Format {
        var nonZeroFormat = DecimalFormat("###,###,###,###")
        var zeroFormat = DecimalFormat("###,###,###,###.#")
        var percentFormat = DecimalFormat("###.##" + "%")
        var timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        var durationFormat = SimpleDateFormat("HH:mm:ss")
    }

    object UserParam {
        var completed = false
        var priceToBuy: Double = LIMIT_AMOUNT
        var monitorTime: Long = BASE_TIME
        var thresholdRate: Double = THRESHOLD_RATE
        var thresholdTick: Int = UNIT_TRADE_COUNT
        var thresholdAccPriceVolumeRate: Float = THRESHOLD_ACC_PRICE_VOLUME_RATE
        var thresholdBidAskRate: Float = THRESHOLD_BID_ASK_RATE
        var thresholdBidAskPriceVolumeRate: Float = THRESHOLD_BID_ASK_PRICE_VOLUME_RATE
        var thresholdBidTickGap: Double = 7.0
        var thresholdAskTickGap: Double = 7.0
    }

    private lateinit var mainActivity: TradePagerActivity
    private lateinit var tradeManager: TradeManager

    private var viewModel: TradeViewModel? = null
    private var processor: BackgroundProcessor? = null

    private val minCandleMapInfo = HashMap<String, TradeCoinInfo>()
    private val tradeMapInfo = HashMap<String, List<TradeInfo>>()

    private var monitorKeyList: List<String>? = null
    private var monitorAdapter: TradeAdapter? = null
    private var tradeAdapter: TradeAdapter? = null
    private var reportPopup: TotalResultDialog? = null

    private var monitorListView: RecyclerView? = null
    private var tradeListView: RecyclerView? = null



    var isRunning = false

    var isInSufficientFunds = false

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = TradeViewModel(application = activity.application, object : TradeFetcher.PostOrderListener {
            override fun onInSufficientFunds(type: String, uuid: UUID) {
                Log.i(TAG, "onInSufficientFunds type: $type uuid: $uuid")
                isInSufficientFunds = true
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        reportPopup = TotalResultDialog(requireContext())
        val totalResultButton = view.findViewById<Button>(R.id.total_result)
        totalResultButton.setOnClickListener {
            reportPopup?.show()
        }

        tradeManager = TradeManager(object : TradeManager.TradeChangedListener {
            override fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo) {
                if (isInSufficientFunds) {
                    return
                }

                tradePostMapInfo[marketId] = orderCoinInfo

                val bidPrice = orderCoinInfo.getBidPrice()
                val volume = (UserParam.priceToBuy / bidPrice!!).toString()
                Log.d(TAG, "[DEBUG] onPostBid - key: $marketId bidPrice: $bidPrice volume: $volume")

                processor?.registerProcess(
                    PostOrderItem(
                        POST_ORDER_INFO,
                        marketId,
                        "bid",
                        volume,
                        bidPrice.toString(),
                        "limit",
                        UUID.randomUUID()
                    )
                )

                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }

                processor?.registerProcess(TaskItem(TICKER_INFO, marketId))
            }

            override fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo, orderType: String, sellPrice: Double?, volume: Double) {
                Log.d(TAG, "[DEBUG] onPostAsk - key: $marketId " +
                        "sellPrice: ${
                            if (sellPrice == null) 
                                null 
                            else 
                                Format.zeroFormat.format(sellPrice.toDouble())
                        } volume: ${Format.zeroFormat.format(volume)}"
                )

                tradePostMapInfo[marketId] = orderCoinInfo
                processor?.registerProcess(
                    PostOrderItem(
                        POST_ORDER_INFO,
                        marketId,
                        "ask",
                        volume.toString(),
                        sellPrice.toString(),
                        orderType,
                        UUID.randomUUID()
                    )
                )

                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }
                processor?.registerProcess(TaskItem(TICKER_INFO, marketId))
            }

            override fun onDelete(marketId: String, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onDelete - key: $marketId uuid: $uuid")
                processor?.registerProcess(
                    TaskItem(
                        DELETE_ORDER_INFO,
                        marketId,
                        uuid
                    )
                )

                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }
            }
        })

        return view
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart: ")
        val viewCycleOwner = viewLifecycleOwner
        viewModel?.resultMarketsInfo?.observe(viewCycleOwner) {
                marketsInfo ->
            makeMarketMapInfo(marketsInfo)
        }

        viewModel?.resultMinCandleInfo?.observe(viewCycleOwner) {
                minCandlesInfo ->
            makeCandleMapInfo(minCandlesInfo)
        }

        viewModel?.resultTradeInfo?.observe(viewCycleOwner) {
                tradesInfo ->
            makeTradeMapInfo(tradesInfo)
        }

        viewModel?.resultTickerInfo?.observe(viewCycleOwner) {
                tickersInfo ->
            monitorTickerInfo(tickersInfo)
        }

        viewModel?.resultPostOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
            makeResponseMapInfo(responseOrder)
        }

        viewModel?.resultSearchOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
            updateResponseMapInfo(responseOrder)
        }

        viewModel?.resultDeleteOrderInfo?.observe(viewCycleOwner) {
                responseOrder ->
            makeDeleteOrderInfo(responseOrder)
        }
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
                extTaskItemList.add(ExtendCandleItem(MIN_CANDLE_INFO, UNIT_MIN_CANDLE.toString(), marketId, UNIT_MIN_CANDLE_COUNT))
                taskItemList.add(CandleItem(TRADE_INFO, marketId, UNIT_TRADE_COUNT))
            }
        }
        processor?.registerProcess(extTaskItemList)
        processor?.registerProcess(taskItemList)
    }

    private fun makeCandleMapInfo(minCandlesInfo: List<Candle>) {
        val marketId: String = minCandlesInfo.first().marketId.toString()
        val accPriceVolume = minCandlesInfo.fold(0.0) {
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

    private fun makeTradeMapInfo(tradesInfoList: List<TradeInfo>) {
        if (tradesInfoList.isNullOrEmpty()) {
            return
        }

        val marketId: String = tradesInfoList.first().marketId.toString()

        val tempInfo: List<TradeInfo> = if (tradeMapInfo[marketId] == null) {
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

        // thresholdTick , thresholdAccPriceVolumeRate
        monitorKeyList = (tradeMonitorMapInfo.filter {
            (it.value.tickCount!! > UserParam.thresholdTick
                    && it.value.getAvgAccVolumeRate() > UserParam.thresholdAccPriceVolumeRate)
        } as HashMap<String, TradeCoinInfo>)
            .toSortedMap(compareByDescending { sortedMapList(it) }).keys.toList()

        tradeManager.setList(TradeManager.Type.POST_BID, monitorKeyList)

        updateView()

        if (tradeMonitorMapInfo[marketId] != null && minCandleMapInfo[marketId] != null) {
            val priceVolume = tradeMonitorMapInfo[marketId]!!.accPriceVolume?.div(UNIT_PRICE)
            val rate = tradeMonitorMapInfo[marketId]!!.getAvgAccVolumeRate()
            val timeZoneFormat = Format.timeFormat
            timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            Log.i(
                TAG, "makeTradeMapInfo marketId: $marketId " +
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
                        "time: ${timeZoneFormat.format(tradeMonitorMapInfo[marketId]!!.timestamp)} "
            )
        }
    }

    private fun monitorTickerInfo(tickersInfo: List<Ticker>) {
        val marketId = tickersInfo.first().marketId
        val time: Long = System.currentTimeMillis()
        val currentPrice = tickersInfo.first().tradePrice?.toDouble()

        if (tradePostMapInfo[marketId] != null) {
            val postInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
            val responseOrder: ResponseOrder? = tradeResponseMapInfo[marketId]

            postInfo.currentPrice = currentPrice
            postInfo.currentTime = time
            if (responseOrder != null) {
                val tickerInfo = tradeManager.updateTickerInfoToTrade(
                    tickersInfo,
                    postInfo,
                    responseOrder)
                if (tickerInfo != null) {
                    tradePostMapInfo[marketId!!] = tickerInfo
                } else {
                    Log.i(TAG, "updateTickerInfoToTrade - marketId: $marketId Not Sell ")
                }
            }
            updateView()
        }
    }

    private fun makeResponseMapInfo(responseOrder: ResponseOrder) {
        val marketId = responseOrder.marketId
        val time: Long = System.currentTimeMillis()
        val tradePostInfo = tradePostMapInfo[marketId]!!
        val registerTime: Long? = tradePostInfo.registerTime

        tradeResponseMapInfo[marketId!!] = responseOrder

        val timeZoneFormat = Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        Log.d(TAG, "[DEBUG] resultPostOrderInfo marketId: $marketId state: ${responseOrder.state} " +
                "side: ${responseOrder.side} " +
                "price: ${Format.zeroFormat.format(responseOrder.price)} " +
                "volume: ${Format.zeroFormat.format(responseOrder.volume)} " +
                "time: ${timeZoneFormat.format(time)}"
        )

        if (registerTime == null) {
            tradePostInfo.registerTime = time
        }
        tradePostInfo.currentTime = time

        if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            if (responseOrder.state.equals("wait")) {

                postOrderBidWait(marketId, time, tradePostInfo, responseOrder)

            } else if (responseOrder.state.equals("done")
                && responseOrder.remainingVolume?.toDouble() == 0.0
                && tradePostInfo.tradeBuyTime == null) {

                postOrderBidDone(marketId, time, tradePostInfo)
            }
        }
        if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            if (responseOrder.state.equals("wait")) {

                postOrderAskWait(marketId, time, tradePostInfo, responseOrder)

            } else if (responseOrder.state.equals("done") && responseOrder.remainingVolume?.toDouble() == 0.0) {

                postOrderAskDone(marketId, time, tradePostInfo, responseOrder)

            }
        }

        if (!responseOrder.state.equals("cancel")) {
            tradePostMapInfo[marketId] = tradePostInfo
        }
        updateView()
    }

    private fun updateResponseMapInfo(responseOrder: ResponseOrder) {
        val marketId: String = responseOrder.marketId!!
        val time: Long = System.currentTimeMillis()
        val tradePostInfo = tradePostMapInfo[marketId]!!

        if (!responseOrder.state.equals("cancel")) {
            tradeResponseMapInfo[marketId] = responseOrder
        }

        val timeZoneFormat = Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        Log.d(TAG, "[DEBUG] resultSearchOrderInfo marketId: $marketId state: ${responseOrder.state} " +
                "side: ${responseOrder.side} " +
                "price: ${Format.zeroFormat.format(responseOrder.price)} " +
                "volume: ${Format.zeroFormat.format(responseOrder.volume)} " +
                "time: ${timeZoneFormat.format(time)}"
        )

        tradePostInfo.currentTime = time

        if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            if (responseOrder.state.equals("done") && responseOrder.remainingVolume?.toDouble() == 0.0) {

                postOrderBidDone(marketId, time, tradePostInfo)

            } else if (responseOrder.state.equals("cancel")) {
                tradePostMapInfo.remove(marketId)
                tradeResponseMapInfo.remove(marketId)
                processor?.unregisterProcess(TICKER_INFO, marketId)
                processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            }
        }

        if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            if (responseOrder.state.equals("done") && responseOrder.remainingVolume?.toDouble() == 0.0) {
                postOrderAskDone(marketId, time, tradePostInfo, responseOrder)
            } else if (responseOrder.state.equals("cancel")) {
                processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            }
        }

        if (!responseOrder.state.equals("cancel")) {
            tradePostMapInfo[marketId] = tradePostInfo
        }
        updateView()
    }

    private fun makeDeleteOrderInfo(responseOrder: ResponseOrder) {
        val marketId = responseOrder.marketId
        Log.d(TAG, "[DEBUG] resultDeleteOrderInfo marketId : $marketId side: ${responseOrder.side} state: ${responseOrder.state}")

        if (responseOrder.state.equals("bid") || responseOrder.state.equals("BID")) {
            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            processor?.unregisterProcess(TICKER_INFO, marketId!!)
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId!!)
        }

        if (responseOrder.state.equals("ask") || responseOrder.state.equals("ASK")) {
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId!!)
        }
        updateView()
    }

    private fun postOrderBidWait(marketId: String, time: Long, tradePostInfo: OrderCoinInfo, responseOrder: ResponseOrder) {
        tradePostInfo.state = OrderCoinInfo.State.BUYING
        processor?.registerProcess(
            TaskItem(
                SEARCH_ORDER_INFO,
                marketId,
                UUID.fromString(responseOrder.uuid)
            )
        )
    }

    private fun postOrderAskWait(marketId: String, time: Long, tradePostInfo: OrderCoinInfo, responseOrder: ResponseOrder) {
        tradePostInfo.state = OrderCoinInfo.State.SELLING
        tradePostInfo.sellPrice = responseOrder.price?.toDouble()
        processor?.registerProcess(
            TaskItem(
                SEARCH_ORDER_INFO,
                marketId,
                UUID.fromString(responseOrder.uuid)
            )
        )
    }

    private fun postOrderBidDone(marketId: String, time: Long, tradePostInfo: OrderCoinInfo) {
        tradePostInfo.state = OrderCoinInfo.State.BUY
        tradePostInfo.registerTime = null
        tradePostInfo.tradeBuyTime = time
        processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
    }

    private fun postOrderAskDone(marketId: String, time: Long, tradePostInfo: OrderCoinInfo, responseOrder: ResponseOrder) {
        isInSufficientFunds = false
        tradePostInfo.state = OrderCoinInfo.State.SELL
        tradePostInfo.sellPrice = responseOrder.price?.toDouble()
        tradePostInfo.volume = responseOrder.volume?.toDouble()
        tradePostInfo.registerTime = null
        tradePostInfo.tradeSellTime = time

        // Total Result
        makeTotalResult(tradePostInfo)

        tradePostMapInfo.remove(marketId)
        tradeResponseMapInfo.remove(marketId)
        processor?.unregisterProcess(TICKER_INFO, marketId)
        processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
    }

    private fun makeTotalResult(tradePostInfo: OrderCoinInfo) {
        tradeReportListInfo.add(tradePostInfo)
        reportPopup?.setList(tradeReportListInfo.toList())
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        val repeatThread = object : Thread() {
            override fun run() {
                while (isRunning) {
                    if (!tradePostMapInfo.isNullOrEmpty()) {
                        sleep(UNIT_REPEAT_MARKET_INFO_SHORT.toLong())
                        Log.i(TAG, "delay resetBackgroundProcessor")
                        continue
                    }
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
        Log.i(TAG, "onPause: ")
        processor?.release()
        isRunning = false
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop: ")
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

    private fun updateView() {
        monitorAdapter!!.monitorKeyList = monitorKeyList
        monitorAdapter!!.notifyDataSetChanged()
        tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
        tradeAdapter?.notifyDataSetChanged()
    }
}
