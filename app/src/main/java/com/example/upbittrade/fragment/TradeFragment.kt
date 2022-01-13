package com.example.upbittrade.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import com.example.upbittrade.fragment.TradeFragment.UserParam.thresholdMarketTrend
import com.example.upbittrade.model.*
import com.example.upbittrade.utils.*
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.TRADE_LIST
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

class TradeFragment: Fragment() {
    companion object {
        const val TAG = "TradeFragment"
        const val LIMIT_AMOUNT = 100000.0
        const val BID_AMOUNT = 10000.0
        const val BASE_TIME: Long = 6 * 60 * 1000
        const val THRESHOLD_RATE = 0.02
        const val THRESHOLD_TICK = 300
        const val THRESHOLD_ACC_PRICE_VOLUME_RATE = 1.0f
        const val THRESHOLD_BID_ASK_RATE = 0.5f
        const val THRESHOLD_BID_ASK_PRICE_VOLUME_RATE = 0.5f

        private const val UNIT_REPEAT_MARKET_INFO = 30 * 60 * 1000
        private const val UNIT_REPEAT_MARKET_INFO_SHORT = 10 * 60 * 1000
        private const val UNIT_MIN_CANDLE = 60
        private const val UNIT_MIN_CANDLE_COUNT = 24
        private const val UNIT_MONITOR_TIME: Long = 60 * 1000
        private const val UNIT_TRADE_COUNT = 3000

        val marketMapInfo = HashMap<String, MarketInfo>()

        val tradeMapInfo = HashMap<String, List<TradeInfo>>()
        val tradeMonitorMapInfo = HashMap<String, TradeCoinInfo>()
        var tradePostMapInfo = HashMap<String, OrderCoinInfo>()
        var tradeReportListInfo = ArrayList<OrderCoinInfo>()

        var tradeResponseMapInfo = HashMap<String, ResponseOrder>()

        var marketTrend: Double? = null
        var bidAskTotalAvgRate: Double? = null
        var avgTradeCount: Double? = null
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

    object UserParam {
        var completed = false
        var totalPriceToBuy: Double = LIMIT_AMOUNT
        var priceToBuy: Double = BID_AMOUNT
        var monitorTime: Long = BASE_TIME
        var thresholdRate: Double = THRESHOLD_RATE
        var thresholdRangeRate: Double = thresholdRate
        var thresholdTick: Int = UNIT_TRADE_COUNT
        var thresholdAccPriceVolumeRate: Float = THRESHOLD_ACC_PRICE_VOLUME_RATE
        var thresholdBidAskRate: Float = THRESHOLD_BID_ASK_RATE
        var thresholdBidAskPriceVolumeRate: Float = THRESHOLD_BID_ASK_PRICE_VOLUME_RATE
        var thresholdTickGap: Double = 5.0 * 1.5
        var thresholdMarketTrend: Double = 0.03
    }

    private lateinit var mainActivity: TradePagerActivity
    private lateinit var tradeManager: TradeManager

    private var viewModel: TradeViewModel? = null
    private var processor: BackgroundProcessor? = null

    private val minCandleMapInfo = HashMap<String, TradeCoinInfo>()

    private var monitorKeyList: List<String>? = null
    private var monitorAdapter: TradeAdapter? = null
    private var tradeAdapter: TradeAdapter? = null
    private var reportPopup: TotalResultDialog? = null

    private var monitorListView: RecyclerView? = null
    private var tradeListView: RecyclerView? = null
    private var totalResultCount: TextView? = null
    private var circleBar: CircleProgressBar? = null
    private var trendRate: TextView? = null
    private var totalBidPrice = HashMap<String, Double>()


    var isRunning = false

    var isInSufficientFunds = false

    private val debug = false

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mainActivity = activity as TradePagerActivity
        viewModel = TradeViewModel(application = activity.application, object : TradeFetcher.PostOrderListener {
            override fun onInSufficientFunds(marketId: String, side: String, errorCode:Int, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onInSufficientFunds marketId: $marketId side: $side errorCode: $errorCode uuid: $uuid")

                val postInfo = tradePostMapInfo[marketId] ?: return
                /*if (side == "ask" && errorCode == 400) {
                    if (postInfo.state == OrderCoinInfo.State.SELLING) {
                        val time: Long = System.currentTimeMillis()
                        val tradePostInfo = tradePostMapInfo[marketId]
                        val responseOrder = tradeResponseMapInfo[marketId]
                        if (tradePostInfo != null && responseOrder != null) {
                            postOrderAskDone(marketId, time, responseOrder)
                        }
                    }
                }*/

                if (side == "bid" && errorCode == 400) {
                    if (postInfo.state == OrderCoinInfo.State.BUYING) {
                        isInSufficientFunds = true
                        setBreakIconVisibility(isInSufficientFunds)

                        processor?.unregisterProcess(CHECK_ORDER_INFO, marketId)
                        tradePostMapInfo.remove(marketId)
                        activity.runOnUiThread {
                            tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                            tradeAdapter?.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onError(marketId: String, side: String?, errorCode: Int, uuid: UUID) {
                Log.d(TAG, "[DEBUG] onError marketId: $marketId side: $side errorCode: $errorCode uuid: $uuid")
                val postInfo = tradePostMapInfo[marketId] ?: return

                if (side == "bid") {
                    if (postInfo.state == OrderCoinInfo.State.BUYING) {
                        processor?.unregisterProcess(CHECK_ORDER_INFO, marketId)
                        tradePostMapInfo.remove(marketId)
                        totalBidPrice.remove(marketId)
                        activity.runOnUiThread {
                            tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                            tradeAdapter?.notifyDataSetChanged()
                        }
                    }
                }

                if (side == "ask") {
                    if (postInfo.state == OrderCoinInfo.State.SELLING) {
                        processor?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
                        tradePostMapInfo[marketId] = postInfo.apply {
                            state = OrderCoinInfo.State.BUY
                            registerTime = null
                        }
                        tradeResponseMapInfo[marketId]?.side = "bid"
                        tradeResponseMapInfo[marketId]?.state = "done"
                    }
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

        trendRate = view.findViewById(R.id.trend_rate)

        reportPopup = TotalResultDialog(requireContext())
        val totalResultButton = view.findViewById<Button>(R.id.total_result)
        totalResultButton.setOnClickListener {
            reportPopup?.show()
        }
        totalResultCount = view.findViewById(R.id.total_result_count)

        tradeManager = TradeManager(object : TradeManager.TradeChangedListener {
            override fun onPostBid(marketId: String, orderCoinInfo: OrderCoinInfo) {
                if (isInSufficientFunds
                    || marketTrend == null || marketTrend!! < thresholdMarketTrend * -1
                    || bidAskTotalAvgRate == null || bidAskTotalAvgRate!! < UserParam.thresholdBidAskPriceVolumeRate * 0.9) {
                    return
                }

                if (!tradePostMapInfo.containsKey(marketId)) {
                    val bidPriceObject = orderCoinInfo.bidPrice
                    if (bidPriceObject != null) {
                        if (bidPriceObject.price != null) {
                            val volume = (UserParam.priceToBuy / bidPriceObject.price)
                            val uuid = UUID.randomUUID()
                            val time = System.currentTimeMillis()
                            val timeZoneFormat = Format.timeFormat
                            timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                            Log.d(
                                TAG, "[DEBUG] onPostBid - key: $marketId " +
                                        "bidPrice: ${Format.nonZeroFormat.format(bidPriceObject.price)} " +
                                        "volume: ${Format.zeroFormat.format(volume)} " +
                                        "bidType: ${bidPriceObject.type} " +
                                        "PostState: ${orderCoinInfo.state} " +
                                        "uuid: $uuid" +
                                        "time: ${timeZoneFormat.format(time)} "
                            )

                            tradePostMapInfo[marketId] = orderCoinInfo.apply {
                                this.state = OrderCoinInfo.State.BUYING
                                this.orderTime = time
                                this.volume = volume
                            }

                            with(processor) {
                                this?.registerProcess(
                                    PostOrderItem(
                                        POST_ORDER_INFO,
                                        marketId,
                                        "bid",
                                        volume,
                                        bidPriceObject.price,
                                        "limit",
                                        uuid
                                    )
                                )
                                this?.registerProcess(
                                    PostOrderItem(CHECK_ORDER_INFO, marketId, "wait", 1, "asc")
                                )
                                this?.registerProcess(
                                    PostOrderItem(CHECK_ORDER_INFO, marketId, "done", 1, "asc")
                                )
                            }
                        }
                    }
                }

                tradeAdapter?.tradeKeyList = tradePostMapInfo.keys.toList()
                activity?.runOnUiThread {
                    tradeAdapter?.notifyDataSetChanged()
                }
            }

            override fun onPostAsk(marketId: String, orderCoinInfo: OrderCoinInfo, orderType: String, askPrice: Double?, volume: Double) {
                if (orderCoinInfo.state == OrderCoinInfo.State.BUY) {
                    val uuid = UUID.randomUUID()
                    val time = System.currentTimeMillis()

                    val timeZoneFormat = Format.timeFormat
                    timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    Log.d(TAG, "[DEBUG] onPostAsk - key: $marketId " +
                            "sellPrice: ${
                                if (askPrice == null)
                                    null
                                else
                                    Format.zeroFormat.format(askPrice.toDouble())
                            } " +
                            "volume: ${Format.zeroFormat.format(volume)} " +
                            "PostState: ${orderCoinInfo.state} " +
                            "uuid: $uuid " +
                            "time: ${timeZoneFormat.format(time)}"
                    )

                    tradePostMapInfo[marketId] = orderCoinInfo.apply {
                        this.state = OrderCoinInfo.State.SELLING
                        this.orderTime = time
                        this.askPrice = askPrice
                        this.volume = volume
                    }

                    with(processor) {
                        this?.registerProcess(
                            PostOrderItem(POST_ORDER_INFO, marketId, "ask", volume, askPrice, orderType, uuid)
                        )
                        this?.registerProcess(
                            PostOrderItem(CHECK_ORDER_INFO, marketId, "wait", 1, "asc")
                        )
                        this?.registerProcess(
                            PostOrderItem(CHECK_ORDER_INFO, marketId, "done", 1, "asc")
                        )
                    }
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
//        Log.i(TAG, "onStart: ")
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
        val postInfo = tradePostMapInfo[marketId] ?: return
        val createdAt = responseOrder.createdAt ?: return
        val createdTime: Long?
        val price = responseOrder.price?.toDouble()
        val volume = responseOrder.volume?.toDouble()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
        val date = sdf.parse(createdAt)
        createdTime = date?.time

        if (createdTime != null) {
            val orderTime = postInfo.orderTime
            val thresholdTime: Long = when {
                responseOrder.state.equals("wait") -> {
                    UserParam.monitorTime
                }
                responseOrder.state.equals("done") -> {
                    UserParam.monitorTime * 5
                }
                else -> {
                    0
                }
            }

            if (orderTime != null && createdTime - orderTime < thresholdTime) {
                val timeZoneFormat = Format.timeFormat
                timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                if ((postInfo.state == OrderCoinInfo.State.BUYING && responseOrder.side.equals("bid"))
                            || (postInfo.state == OrderCoinInfo.State.SELLING && responseOrder.side.equals("ask"))
                ) {
                    if (debug) {
                        Log.i(TAG,"checkOrderInfo marketId: $marketId " +
                                "state: ${responseOrder.state} " +
                                "side: ${responseOrder.side} " +
                                "price: ${
                                    if (price == null) null else Format.zeroFormat.format(
                                        price
                                    )
                                } " +
                                "volume: ${
                                    if (volume == null) null else Format.zeroFormat.format(
                                        volume
                                    )
                                } " +
                                "time: ${timeZoneFormat.format(createdTime)}")
                    }
                    makeResponseMapInfo(responseOrder)
                }
            }
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
//                Log.i(TAG, "resultMarketsInfo - marketId: $marketId")
                extTaskItemList.add(ExtendCandleItem(MIN_CANDLE_INFO, UNIT_MIN_CANDLE.toString(), marketId, UNIT_MIN_CANDLE_COUNT))
                taskItemList.add(CandleItem(TRADE_INFO, marketId, UNIT_TRADE_COUNT))
            }
        }

        circleBar?.max = marketMapInfo.size
        circleBar?.progress = 0
        with(processor) {
            this?.registerProcess(extTaskItemList)
            this?.registerProcess(taskItemList)
        }
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

        val tempInfo: List<TradeInfo> = if (!tradeMapInfo.containsKey(marketId)) {
            tradesInfoList.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UserParam.monitorTime
            }.reversed()
        } else {
            val addList = tradesInfoList.asReversed().filter { tradeInfo ->
                tradeMapInfo[marketId]!!.last().sequentialId < tradeInfo.sequentialId
            }
            val combineInfo = tradeMapInfo[marketId]!! + addList
            combineInfo.filter { tradeInfo ->
                tradesInfoList.first().timestamp - tradeInfo.timestamp < UserParam.monitorTime
            }
        }

        val accPriceVolume = tempInfo.fold(0.0) { acc: Double, tradeInfo: TradeInfo ->
            acc + tradeInfo.getPriceVolume()
        }
        val highPrice = tempInfo.maxOf { it.tradePrice!!.toDouble() }
        val lowPrice = tempInfo.minOf { it.tradePrice!!.toDouble() }
        val openPrice = tempInfo.first().tradePrice!!.toDouble()
        val closePrice = tempInfo.last().tradePrice!!.toDouble()
        val avgAccPriceVolume =
            minCandleMapInfo[marketId]?.accPriceVolume?.div(UNIT_MIN_CANDLE * UNIT_MIN_CANDLE_COUNT)
                ?.times((UserParam.monitorTime / UNIT_MONITOR_TIME))

        val bid = tempInfo.fold(0) { acc: Int, tradeInfo: TradeInfo ->
            acc + addCount(tradeInfo, "BID")
        }

        val ask = tempInfo.fold(0) { acc: Int, tradeInfo: TradeInfo ->
            acc + addCount(tradeInfo, "ASK")
        }

        val bidPriceVolume = tempInfo.fold(0.0) { acc: Double, tradeInfo: TradeInfo ->
            acc + addPriceVolume(tradeInfo, "BID")
        }

        val askPriceVolume = tempInfo.fold(0.0) { acc: Double, tradeInfo: TradeInfo ->
            acc + addPriceVolume(tradeInfo, "ASK")
        }

        tradeMapInfo[marketId] = tempInfo
        tradeMonitorMapInfo[marketId] = TradeCoinInfo(
            marketId,
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

        if (progress % circleBar!!.max == 0) {
            val bidTotal = tradeMonitorMapInfo.values.fold(0.0) { acc: Double, value: TradeCoinInfo ->
                acc + value.bidPriceVolume!!.toDouble()
            }
            val askTotal = tradeMonitorMapInfo.values.fold(0.0) { acc: Double, value: TradeCoinInfo ->
                acc + value.askPriceVolume!!.toDouble()
            }
            bidAskTotalAvgRate = bidTotal / (bidTotal + askTotal)

            trendRate?.text = Format.percentFormat.format(bidAskTotalAvgRate)
            if (bidAskTotalAvgRate != null) {
                trendRate?.setTextColor(Utils.getTextColor(bidAskTotalAvgRate, 0.5))
            }

            marketTrend = tradeMapInfo.values.fold(0.0) { acc: Double, value: List<TradeInfo> ->
                acc + value.last().getDayChangeRate()
            }
            marketTrend = marketTrend!! / tradeMapInfo.size


            circleBar?.setProgressBackgroundColor(
                when {
                    marketTrend!! > thresholdMarketTrend -> {
                        Color.RED
                    }
                    marketTrend!! > thresholdMarketTrend * -1 && marketTrend!! <= thresholdMarketTrend -> {
                        Color.YELLOW
                    }
                    else -> {
                        Color.BLUE
                    }
                }
            )

            circleBar?.setProgressStartColor(
                when {
                    marketTrend!! > thresholdMarketTrend -> {
                        Color.GREEN
                    }
                    marketTrend!! > thresholdMarketTrend * -1 && marketTrend!! <= thresholdMarketTrend -> {
                        Color.GREEN
                    }
                    else -> {
                        Color.GREEN
                    }
                }
            )

            circleBar?.setProgressEndColor(
                when {
                    marketTrend!! > thresholdMarketTrend -> {
                        Color.CYAN
                    }
                    marketTrend!! > thresholdMarketTrend * -1 && marketTrend!! <= thresholdMarketTrend -> {
                        Color.CYAN
                    }
                    else -> {
                        Color.CYAN
                    }
                }
            )

            val totalTradeCount = tradeMapInfo.values.fold(0.0) {
                acc: Double, value: List<TradeInfo> -> acc + value.size
            }
            avgTradeCount = totalTradeCount / tradeMapInfo.size

            Log.i(TAG,"makeTradeMapInfo - marketTrend: ${Format.percentFormat.format(marketTrend)} " +
                    "bidAskTotalRate: ${Format.percentFormat.format(bidAskTotalAvgRate)} " +
                    "avgTradeCount: ${Format.zeroFormat.format(avgTradeCount)}")

        }

        // thresholdTick , thresholdAccPriceVolumeRate
        monitorKeyList = (tradeMonitorMapInfo.filter {
            avgTradeCount != null
                    && (it.value.tickCount!! > avgTradeCount!! + UserParam.thresholdTick
                        || it.value.tickCount!! > avgTradeCount!! * 2)
                    && it.value.getAvgAccVolumeRate() > UserParam.thresholdAccPriceVolumeRate
        } as HashMap<String, TradeCoinInfo>)
            .toSortedMap(compareByDescending { sortedMapList(it) }).keys.toList()

        tradeManager.setList(TradeManager.Type.POST_BID, monitorKeyList)

        updateView()

        /*if (tradeMonitorMapInfo[marketId] != null && minCandleMapInfo[marketId] != null) {
            val priceVolume = tradeMonitorMapInfo[marketId]!!.accPriceVolume?.div(UNIT_PRICE)
            val rate = tradeMonitorMapInfo[marketId]!!.getAvgAccVolumeRate()
            val timeZoneFormat = Format.timeFormat
            timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            Log.i(TAG, "makeTradeMapInfo marketId: $marketId " +
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
        }*/
    }

    private fun monitorTickerInfo(tickersInfo: List<Ticker>) {
        val marketId = tickersInfo.first().marketId!!
        var postInfo: OrderCoinInfo? = tradePostMapInfo[marketId]
        if (postInfo != null) {
           Format.timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val responseOrder: ResponseOrder? = tradeResponseMapInfo[marketId]
            val time: Long = System.currentTimeMillis()
            val currentPrice = tickersInfo.first().tradePrice?.toDouble()!!

            val currentTradeMonitorInfo = tradeMonitorMapInfo[marketId]!!
            postInfo = postInfo.apply {
                currentTime = time
                highPrice = max(currentPrice, currentTradeMonitorInfo.highPrice!!.toDouble())
                lowPrice = min(currentPrice, currentTradeMonitorInfo.lowPrice!!.toDouble())
                openPrice = currentTradeMonitorInfo.openPrice!!
                closePrice = currentPrice
            }

            tradePostMapInfo[marketId] = postInfo.apply {
                maxProfitRate = if (maxProfitRate == null) {
                    getProfitRate(highPrice!!.toDouble())
                } else {
                    max(getProfitRate(highPrice!!.toDouble()), maxProfitRate!!)
                }

                maxPrice = if (maxPrice == null) {
                    highPrice!!.toDouble()
                } else {
                    max(highPrice!!.toDouble(), maxPrice!!)
                }

                minPrice = if (minPrice == null) {
                    lowPrice!!.toDouble()
                } else {
                    min(lowPrice!!.toDouble(), minPrice!!)
                }
            }

            if (responseOrder != null) {
//                val bidPrice = postInfo.bidPrice?.price
//                val tickGap = abs(bidPrice!! - currentPrice) / postInfo.getTickPrice()!!
//                val volume = responseOrder.volume?.toDouble()
//                Log.i(TAG, "monitorTickerInfo marketId: $marketId  " +
//                        "currentPrice: ${Format.zeroFormat.format(currentPrice)} " +
//                        "volume: ${if (volume == null) null else Format.zeroFormat.format(volume)} " +
//                        "PostState: ${postInfo.state} " +
//                        "side: ${responseOrder.side} " +
//                        "state: ${responseOrder.state} " +
//                        "profitRate: ${Format.percentFormat.format(profitRate)} " +
//                        "maxProfitRate: ${Format.percentFormat.format(maxProfitRate)} " +
//                        "tickGap: $tickGap " +
//                        "time: ${Format.timeFormat.format(time)}")

                if (postInfo.state == OrderCoinInfo.State.BUY
                    && (responseOrder.side.equals("bid") || responseOrder.side.equals("Bid"))
                    && responseOrder.state.equals("done")) {

                    Log.d(TAG, "[DEBUG] monitorTickerInfo - marketId: ${postInfo.marketId} " +
                            "highPrice: ${Utils.getZeroFormatString(postInfo.highPrice!!.toDouble())} " +
                            "lowPrice: ${Utils.getZeroFormatString(postInfo.lowPrice!!.toDouble())} " +
                            "openPrice: ${Utils.getZeroFormatString(postInfo.openPrice!!.toDouble())} " +
                            "closePrice: ${Utils.getZeroFormatString(postInfo.closePrice!!.toDouble())} "
                    )

                    tradePostMapInfo[marketId] = tradeManager.tacticalToSell(tradePostMapInfo[marketId]!!, responseOrder)

                }
            }
            updateView()
        }
    }

    private fun makeResponseMapInfo(responseOrder: ResponseOrder) {
        val marketId: String = responseOrder.marketId ?: return
        tradePostMapInfo[marketId] ?: return
        val time: Long = System.currentTimeMillis()
        val price = responseOrder.price?.toDouble()
        val volume = responseOrder.volume?.toDouble()
        val timeZoneFormat = Format.timeFormat
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        tradePostMapInfo[marketId]!!.currentTime = time
        if (debug) {
            Log.i(TAG, "makeResponseMapInfo marketId: $marketId state: ${responseOrder.state} " +
                        "side: ${responseOrder.side} " +
                        "price: ${if (price == null) null else Format.zeroFormat.format(price)} " +
                        "volume: ${if (volume == null) null else Format.zeroFormat.format(volume)} " +
                        "time: ${timeZoneFormat.format(time)}"
            )
        }

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
        val marketId: String = responseOrder.marketId ?: return
        tradePostMapInfo[marketId] ?: return
        val time: Long = System.currentTimeMillis()
        val timeZoneFormat = Format.timeFormat
        val price = responseOrder.price?.toDouble()
        val volume = responseOrder.volume?.toDouble()
        timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        tradePostMapInfo[marketId]!!.currentTime = time
        if (debug) {
            Log.i(TAG, "updateResponseMapInfo marketId: $marketId state: ${responseOrder.state} " +
                        "side: ${responseOrder.side} " +
                        "price: ${if (price == null) null else Format.zeroFormat.format(price)} " +
                        "volume: ${if (volume == null) null else Format.zeroFormat.format(volume)} " +
                        "time: ${timeZoneFormat.format(time)}"
            )
        }

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
                with(processor) {
                    this?.unregisterProcess(TICKER_INFO, marketId)
                    this?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
                }
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
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId] ?: return false
        if (tradePostInfo.state != OrderCoinInfo.State.DELETE
            && tradePostInfo.getRegisterDuration() != null
            && tradePostInfo.getRegisterDuration()!! > UserParam.monitorTime * 1.5) {
            Log.d(TAG, "[DEBUG] postOrderDeleteWait - DELETE_ORDER_INFO marketId: $marketId uuid: ${responseOrder.uuid}")
            tradePostInfo.state = OrderCoinInfo.State.DELETE
            tradePostMapInfo[marketId] = tradePostInfo

            processor?.registerProcess(
                TaskItem(
                    DELETE_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
            updateView()


            return true
        }
        return false
    }

    private fun resultDeleteOrderInfo(responseOrder: ResponseOrder) {
        val marketId = responseOrder.marketId ?: return
        Log.d(TAG, "[DEBUG] resultDeleteOrderInfo marketId : $marketId " +
                "side: ${responseOrder.side} " +
                "state: ${responseOrder.state} " +
                "uuid: ${responseOrder.uuid} "
        )
        if (!responseOrder.state.equals("wait")) {
            return
        }

        val postInfo = tradePostMapInfo[marketId] ?: return

        if (postInfo.state == OrderCoinInfo.State.DELETE
            && responseOrder.side.equals("bid") || responseOrder.side.equals("BID")) {
            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            totalBidPrice.remove(marketId)
            with(processor) {
                this?.unregisterProcess(TICKER_INFO, marketId)
                this?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
                this?.unregisterProcess(CHECK_ORDER_INFO, marketId)
            }
        }

        if (postInfo.state == OrderCoinInfo.State.DELETE
            && responseOrder.side.equals("ask") || responseOrder.side.equals("ASK")) {
            with(processor) {
                this?.unregisterProcess(SEARCH_ORDER_INFO, marketId)
                this?.unregisterProcess(CHECK_ORDER_INFO, marketId)
            }

            tradePostMapInfo[marketId] = postInfo.apply {
                state = OrderCoinInfo.State.BUY
                orderTime = null
                registerTime = null
            }

            tradeResponseMapInfo[marketId] = responseOrder.apply {
                side = "bid"
                state = "done"
            }

        }
        updateView()
    }

    private fun postOrderBidWait(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId] ?: return
        val registerTime: Long? = tradePostInfo.registerTime
        val bidType: Int = tradePostInfo.bidPrice?.type!!
        if (tradePostInfo.state == OrderCoinInfo.State.BUYING && registerTime == null) {
            Log.d(TAG, "[DEBUG] postOrderBidWait marketId: $marketId " +
                    "state: READY -> ${tradePostInfo.state} " +
                    "uuid: ${responseOrder.uuid}")

            tradePostMapInfo[marketId] = tradePostInfo.apply {
                this.state = OrderCoinInfo.State.BUYING
                this.bidPrice = BidPrice(responseOrder.price?.toDouble(), bidType)
                this.volume = responseOrder.volume?.toDouble()
                this.registerTime = time
            }

            processor?.registerProcess(
                TaskItem(
                    SEARCH_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
            tradeResponseMapInfo[marketId] = responseOrder

            checkTotalBidPriceAmount(marketId, responseOrder)
        } else {
            Log.w(TAG, "postOrderBidWait marketId: $marketId state: ${tradePostInfo.state} uuid: ${responseOrder.uuid}")
        }
    }

    private fun postOrderBidDone(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId] ?: return
        val bidType: Int = tradePostInfo.bidPrice?.type!!
        if (tradePostInfo.state == OrderCoinInfo.State.BUYING) {
            Log.d(TAG, "[DEBUG] postOrderBidDone marketId: $marketId " +
                    "state: ${tradePostInfo.state} -> BUY " +
                    "uuid: ${responseOrder.uuid}")

            processor = processor?.apply {
                unregisterProcess(SEARCH_ORDER_INFO, marketId)
                registerProcess(TaskItem(TICKER_INFO, marketId))
                unregisterProcess(CHECK_ORDER_INFO, marketId)
            }

            tradePostMapInfo[marketId] = tradePostInfo.apply {
                state = OrderCoinInfo.State.BUY
                bidPrice = BidPrice(responseOrder.price?.toDouble(), bidType)
                volume = responseOrder.volume?.toDouble()
                orderTime = null
                registerTime = null
                tradeBidTime = time
            }

            tradeResponseMapInfo[marketId] = responseOrder

            checkTotalBidPriceAmount(marketId, responseOrder)
        }
    }

    private fun postOrderAskWait(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId] ?: return

        if (tradePostInfo.state == OrderCoinInfo.State.SELLING && tradePostInfo.registerTime == null) {
            Log.d(TAG, "[DEBUG] postOrderAskWait marketId: $marketId " +
                    "state: BUY -> ${tradePostInfo.state} " +
                    "uuid: ${responseOrder.uuid}")

            tradePostMapInfo[marketId] = tradePostInfo.apply {
                registerTime = time
                askPrice = responseOrder.price?.toDouble()
                volume = responseOrder.volume?.toDouble()
            }

            tradeResponseMapInfo[marketId] = responseOrder

            processor?.registerProcess(
                TaskItem(
                    SEARCH_ORDER_INFO,
                    marketId,
                    UUID.fromString(responseOrder.uuid)
                )
            )
        } else {
            Log.w(TAG, "postOrderAskWait marketId: $marketId state: ${tradePostInfo.state} uuid: ${responseOrder.uuid}")
        }
    }

    private fun postOrderAskDone(marketId: String, time: Long, responseOrder: ResponseOrder) {
        val tradePostInfo: OrderCoinInfo = tradePostMapInfo[marketId] ?: return
        if (tradePostInfo.state == OrderCoinInfo.State.SELLING) {
            Log.d(TAG, "[DEBUG] postOrderAskDone marketId: $marketId " +
                    "state: ${tradePostInfo.state} -> SELL " +
                    "uuid: ${responseOrder.uuid}")

            processor = processor?.apply {
                unregisterProcess(SEARCH_ORDER_INFO, marketId)
                unregisterProcess(TICKER_INFO, marketId)
                unregisterProcess(CHECK_ORDER_INFO, marketId)
            }

            isInSufficientFunds = false
            setBreakIconVisibility(isInSufficientFunds)

            tradePostMapInfo[marketId] = tradePostInfo.apply {
                state = OrderCoinInfo.State.SELL
                askPrice = responseOrder.price?.toDouble()
                volume = responseOrder.volume?.toDouble()
                orderTime = null
                registerTime = null
                tradeAskTime = time
            }

            tradeResponseMapInfo[marketId] = responseOrder

            // Total Result
            makeTotalResult(OrderCoinInfo(tradePostInfo))

            tradePostMapInfo.remove(marketId)
            tradeResponseMapInfo.remove(marketId)
            totalBidPrice.remove(marketId)
            updateView()
        }
    }

    private fun makeTotalResult(tradePostInfo: OrderCoinInfo) {
        val askPrice = tradePostInfo.askPrice
//        Log.i(TAG, "makeTotalResult marketId: ${tradePostInfo.marketId} " +
//                "state: ${tradePostInfo.state} " +
//                "bidPrice: ${tradePostInfo.getBidPrice()} " +
//                "askPrice: $askPrice")

        if (askPrice == null) {
            tradePostInfo.askPrice = tradePostInfo.closePrice?.toDouble()
        }
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
        processor?.release()
        isRunning = false
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

    private fun checkTotalBidPriceAmount(marketId: String, responseOrder: ResponseOrder) {
        if (!totalBidPrice.containsKey(marketId)) {
            totalBidPrice[marketId] = responseOrder.price!!.toDouble() * responseOrder.volume!!.toDouble()
            val totalBidPrice = totalBidPrice.values.fold(0.0) {
                    acc:Double, value: Double -> acc + value
            }
            if (totalBidPrice >= UserParam.totalPriceToBuy) {
                isInSufficientFunds = true
                setBreakIconVisibility(isInSufficientFunds)
                Log.d(TAG, "[DEBUG] checkTotalBidPriceAmount - isInSufficientFunds: $isInSufficientFunds")
            }
        }
    }

    private fun setBreakIconVisibility(visible: Boolean) {
        view?.findViewById<ImageView>(R.id.insufficient_funds)?.visibility =
            if(visible) View.VISIBLE else View.INVISIBLE
    }
}
