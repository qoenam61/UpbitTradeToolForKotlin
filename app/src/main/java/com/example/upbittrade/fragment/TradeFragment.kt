package com.example.upbittrade.fragment

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dinuscxj.progressbar.CircleProgressBar
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
import kotlin.math.abs
import kotlin.math.max

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        const val LIMIT_AMOUNT = 7000.0
        const val BASE_TIME: Long = 3 * 60 * 1000
        const val THRESHOLD_RATE = 0.015
        const val THRESHOLD_TICK = 300
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
        var thresholdRangeRate: Double = thresholdRate * 1.5
        var thresholdTick: Int = UNIT_TRADE_COUNT
        var thresholdAccPriceVolumeRate: Float = THRESHOLD_ACC_PRICE_VOLUME_RATE
        var thresholdBidAskRate: Float = THRESHOLD_BID_ASK_RATE
        var thresholdBidAskPriceVolumeRate: Float = THRESHOLD_BID_ASK_PRICE_VOLUME_RATE
        var thresholdTickGap: Double = 5.0
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
    private var totalResultCount: TextView? = null
    private var circleBar: CircleProgressBar? = null


    var isRunning = false

    var isInSufficientFunds = false

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = TradeViewModel(application = activity.application, object : TradeFetcher.PostOrderListener {
            override fun onInSufficientFunds(marketId: String, side: String, errorCode:Int, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onInSufficientFunds marketId: $marketId side: $side errorCode: $errorCode uuid: $uuid")

                val postInfo = tradePostMapInfo[marketId]
                if (side == "ask" && errorCode == 400) {
                    if (postInfo?.state != OrderCoinInfo.State.SELLING) {
                        val time: Long = System.currentTimeMillis()
                        val tradePostInfo = tradePostMapInfo[marketId]
                        val responseOrder = tradeResponseMapInfo[marketId]
                        if (tradePostInfo != null && responseOrder != null) {
                            postOrderAskDone(marketId, time, responseOrder)
                        }
                    }
                }

                if (side == "bid" && errorCode == 400) {
                    isInSufficientFunds = true
                    /*if (postInfo?.state != OrderCoinInfo.State.BUYING) {
                        tradePostMapInfo.remove(marketId)
                        activity.runOnUiThread {
                            tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                            tradeAdapter?.notifyDataSetChanged()
                        }
                    }*/
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

        circleBar = view.findViewById(R.id.circle_bar)
        circleBar?.progress = 0

        reportPopup = TotalResultDialog(requireContext())
        val totalResultButton = view.findViewById<Button>(R.id.total_result)
        totalResultButton.setOnClickListener {
            reportPopup?.show()
        }
        totalResultCount = view.findViewById(R.id.total_result_count)

        tradeManager = TradeManager(object : TradeManager.TradeChangedListener {
            override fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo) {
                if (isInSufficientFunds) {
                    return
                }

                val postInfo = tradePostMapInfo[marketId]
                if (postInfo == null) {
                    orderCoinInfo.state = OrderCoinInfo.State.BUYING
                    tradePostMapInfo[marketId] = orderCoinInfo

                    val bidPrice = orderCoinInfo.getBidPrice()
                    val volume = (UserParam.priceToBuy / bidPrice!!).toString()
                    Log.d(TAG, "[DEBUG] onPostBid - key: $marketId bidPrice: $bidPrice volume: $volume PostState: ${orderCoinInfo.state}")

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
                }

                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }
            }

            override fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo, orderType: String, askPrice: Double?, volume: Double) {
                Log.d(TAG, "[DEBUG] onPostAsk - key: $marketId " +
                        "sellPrice: ${
                            if (askPrice == null) 
                                null 
                            else 
                                Format.zeroFormat.format(askPrice.toDouble())
                        } " +
                        "volume: ${Format.zeroFormat.format(volume)} " +
                        "PostState: ${orderCoinInfo.state} "
                )

                if (orderCoinInfo.state == OrderCoinInfo.State.BUY) {
                    orderCoinInfo.state = OrderCoinInfo.State.SELLING
                    tradePostMapInfo[marketId] = orderCoinInfo
                    processor?.registerProcess(
                        PostOrderItem(
                            POST_ORDER_INFO,
                            marketId,
                            "ask",
                            volume.toString(),
                            askPrice.toString(),
                            orderType,
                            UUID.randomUUID()
                        )
                    )
                }
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
            resultDeleteOrderInfo(responseOrder)
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

        circleBar?.max = marketMapInfo.size
        circleBar?.progress = 0
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

        var progress = circleBar?.progress
        progress = progress!! + 1
        circleBar?.progress = progress % circleBar!!.max

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
            /*Log.i(
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
            )*/
        }
    }

    private fun monitorTickerInfo(tickersInfo: List<Ticker>) {
        val marketId = tickersInfo.first().marketId
        val postInfo: OrderCoinInfo? = tradePostMapInfo[marketId]
        if (postInfo != null) {
           Format.timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val responseOrder: ResponseOrder? = tradeResponseMapInfo[marketId]
            val time: Long = System.currentTimeMillis()
            val currentPrice = tickersInfo.first().tradePrice?.toDouble()
            postInfo.currentTime = time
            postInfo.currentPrice = currentPrice

            val profitRate = postInfo.getProfitRate()!!
            var maxProfitRate = postInfo.maxProfitRate
            val bidPrice = postInfo.getBidPrice()
            val tickGap = abs(bidPrice!! - currentPrice!!) / postInfo.getTickPrice()!!

            maxProfitRate = max(profitRate, maxProfitRate)
            postInfo.maxProfitRate = maxProfitRate
            tradePostMapInfo[marketId!!] = postInfo

            if (responseOrder != null) {
                Log.i(TAG, "monitorTickerInfo marketId: $marketId  " +
                        "currentPrice: ${Format.zeroFormat.format(currentPrice)} " +
                        "volume: ${Format.zeroFormat.format(responseOrder.volume)} " +
                        "PostState: ${postInfo.state} " +
                        "side: ${responseOrder.side} " +
                        "state: ${responseOrder.state} " +
                        "profitRate: ${Format.percentFormat.format(profitRate)} " +
                        "maxProfitRate: ${Format.percentFormat.format(maxProfitRate)} " +
                        "tickGap: $tickGap " +
                        "time: ${Format.timeFormat.format(time)}")

                if (postInfo.state == OrderCoinInfo.State.BUY
                    && (responseOrder.side.equals("bid") || responseOrder.side.equals("Bid"))
                    && responseOrder.state.equals("done")) {

                    tradePostMapInfo[marketId] = tradeManager.tacticalToSell(postInfo, responseOrder)

                }
            }
            updateView()
        }
    }

    private fun makeResponseMapInfo(responseOrder: ResponseOrder) {
        val marketId = responseOrder.marketId!!
        val time: Long = System.currentTimeMillis()
        val timeZoneFormat = Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        tradePostMapInfo[marketId]!!.currentTime = time
        Log.i(TAG, "makeResponseMapInfo marketId: $marketId state: ${responseOrder.state} " +
                "side: ${responseOrder.side} " +
                "price: ${Format.zeroFormat.format(responseOrder.price)} " +
                "volume: ${Format.zeroFormat.format(responseOrder.volume)} " +
                "time: ${timeZoneFormat.format(time)}"
        )

        if (responseOrder.state.equals("wait")) {
            if (postOrderDeleteWait(marketId, responseOrder)) {
                return
            }
        }

        if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            if (responseOrder.state.equals("wait")) {

                postOrderBidWait(marketId, time, responseOrder)

            } else if (responseOrder.state.equals("done")
                && responseOrder.remainingVolume?.toDouble() == 0.0) {

                postOrderBidDone(marketId, time, responseOrder)
            }
        }

        if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            if (responseOrder.state.equals("wait")) {

                postOrderAskWait(marketId, time, responseOrder)

            } else if (responseOrder.state.equals("done")
                && responseOrder.remainingVolume?.toDouble() == 0.0) {

                postOrderAskDone(marketId, time, responseOrder)

            }
        }

        if (!responseOrder.state.equals("cancel")) {
            tradeResponseMapInfo[marketId] = responseOrder
        }
        updateView()
    }

    private fun updateResponseMapInfo(responseOrder: ResponseOrder) {
        val marketId: String = responseOrder.marketId!!
        val time: Long = System.currentTimeMillis()
        val timeZoneFormat = Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        tradePostMapInfo[marketId]!!.currentTime = time
        Log.i(TAG, "updateResponseMapInfo marketId: $marketId state: ${responseOrder.state} " +
                "side: ${responseOrder.side} " +
                "price: ${Format.zeroFormat.format(responseOrder.price)} " +
                "volume: ${Format.zeroFormat.format(responseOrder.volume)} " +
                "time: ${timeZoneFormat.format(time)}"
        )

        if (responseOrder.state.equals("wait")) {
            if (postOrderDeleteWait(marketId, responseOrder)) {
                return
            }
        }

        if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            if (responseOrder.state.equals("done")
                && responseOrder.remainingVolume?.toDouble() == 0.0) {

                postOrderBidDone(marketId, time, responseOrder)

            } else if (responseOrder.state.equals("cancel")) {
                tradePostMapInfo.remove(marketId)
                tradeResponseMapInfo.remove(marketId)
                processor?.unregisterProcess(TICKER_INFO, marketId)
                processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            }
        }

        if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            if (responseOrder.state.equals("done")
                && responseOrder.remainingVolume?.toDouble() == 0.0) {
                postOrderAskDone(marketId, time, responseOrder)
            } else if (responseOrder.state.equals("cancel")) {
                processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            }
        }

        if (!responseOrder.state.equals("cancel")) {
            tradeResponseMapInfo[marketId] = responseOrder
        }
        updateView()
    }

    private fun postOrderDeleteWait(marketId: String, responseOrder: ResponseOrder): Boolean {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
        if (tradePostInfo.getRegisterDuration() != null
            && tradePostInfo.getRegisterDuration()!! > UserParam.monitorTime) {
            Log.d(TAG, "[DEBUG] postOrderDeleteWait - DELETE_ORDER_INFO marketId: $marketId uuid: ${responseOrder.uuid}")
            processor?.registerProcess(
                TaskItem(
                    DELETE_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
            return true
        }
        return false
    }

    private fun resultDeleteOrderInfo(responseOrder: ResponseOrder) {
        val marketId = responseOrder.marketId
        Log.d(TAG, "[DEBUG] resultDeleteOrderInfo marketId : $marketId side: ${responseOrder.side} state: ${responseOrder.state}")
        if (!responseOrder.state.equals("wait")) {
            return
        }
        if (responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            processor?.unregisterProcess(TICKER_INFO, marketId!!)
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId!!)
        }

        if (responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId!!)
            tradePostMapInfo[marketId]?.state = OrderCoinInfo.State.BUY
            tradePostMapInfo[marketId]?.registerTime = null
            tradeResponseMapInfo[marketId]?.side = "bid"
            tradeResponseMapInfo[marketId]?.state = "done"
        }
        updateView()
    }

    private fun postOrderBidWait(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
        Log.d(TAG, "[DEBUG] postOrderBidWait marketId: $marketId state: ${tradePostInfo.state} -> BUYING uuid: ${responseOrder.uuid}")
        if (tradePostInfo.state == OrderCoinInfo.State.BUYING) {
            val registerTime: Long? = tradePostInfo.registerTime
            if (registerTime == null) {
                tradePostInfo.registerTime = time
            }
            tradePostMapInfo[marketId] = tradePostInfo

            processor?.registerProcess(
                TaskItem(
                    SEARCH_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
        } else {
            Log.i(TAG, "postOrderBidWait marketId: $marketId state: ${tradePostInfo.state} -> BUYING uuid: ${responseOrder.uuid}")
        }
    }

    private fun postOrderBidDone(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
        Log.d(TAG, "[DEBUG] postOrderBidDone marketId: $marketId state: ${tradePostInfo.state} -> BUY uuid: ${responseOrder.uuid}")
        if (tradePostInfo.state == OrderCoinInfo.State.BUYING) {
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            processor?.registerProcess(TaskItem(TICKER_INFO, marketId))

            tradePostInfo.state = OrderCoinInfo.State.BUY
            tradePostInfo.registerTime = null
            tradePostInfo.tradeBidTime = time
            tradePostMapInfo[marketId] = tradePostInfo
        }
    }

    private fun postOrderAskWait(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
        Log.d(TAG, "[DEBUG] postOrderAskWait marketId: $marketId state: ${tradePostInfo.state} -> SELLING uuid: ${responseOrder.uuid}")
        if (tradePostInfo.state == OrderCoinInfo.State.SELLING) {
            val registerTime: Long? = tradePostInfo.registerTime
            if (registerTime == null) {
                tradePostInfo.registerTime = time
            }
            tradePostInfo.askPrice = responseOrder.price?.toDouble()
            tradePostMapInfo[marketId] = tradePostInfo

            processor?.registerProcess(
                TaskItem(
                    SEARCH_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
        } else {
            Log.i(TAG, "postOrderAskWait marketId: $marketId state: ${tradePostInfo.state} -> SELLING uuid: ${responseOrder.uuid}")
        }
    }

    private fun postOrderAskDone(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId]!!
        Log.d(TAG, "[DEBUG] postOrderAskDone marketId: $marketId state: ${tradePostInfo.state} -> SELL uuid: ${responseOrder.uuid}")
        if (tradePostInfo.state == OrderCoinInfo.State.SELLING) {
            processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
            processor?.unregisterProcess(TICKER_INFO, marketId)

            isInSufficientFunds = false
            tradePostInfo.state = OrderCoinInfo.State.SELL
            tradePostInfo.askPrice = responseOrder.price?.toDouble()
            tradePostInfo.volume = responseOrder.volume?.toDouble()
            tradePostInfo.registerTime = null
            tradePostInfo.tradeAskTime = time
            tradePostMapInfo[marketId] = tradePostInfo

            // Total Result
            makeTotalResult(OrderCoinInfo(tradePostInfo))

            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            updateView()
        }
    }

    private fun makeTotalResult(tradePostInfo: OrderCoinInfo) {
        Log.i(TAG, "makeTotalResult marketId: ${tradePostInfo.marketId} " +
                "state: ${tradePostInfo.state} " +
                "bidPrice: ${tradePostInfo.getBidPrice()} " +
                "askPrice: ${tradePostInfo.askPrice}")
        tradeReportListInfo.add(tradePostInfo)
        reportPopup?.setList(tradeReportListInfo.toList())
        val size = tradeReportListInfo.size
        totalResultCount?.text = size.toString()
        if (size > 0) {
            totalResultCount?.setTextColor(Color.RED)
        } else {
            totalResultCount?.setTextColor(Color.BLACK)
        }
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
