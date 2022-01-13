package com.example.upbittrade.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.fragment.TradeFragment.Companion.marketMapInfo
import com.example.upbittrade.fragment.TradeFragment.Companion.tradeMonitorMapInfo
import com.example.upbittrade.model.OrderCoinInfo
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.*
import com.example.upbittrade.utils.Utils.Companion.getTextColor
import com.example.upbittrade.utils.Utils.Companion.getZeroFormatString
import java.util.*

class TradeAdapter(private val context: Context, val type: Type): RecyclerView.Adapter<TradeAdapter.CoinHolder>() {
    companion object {
        const val TAG = "TradeFragmentView"
        enum class Type {
            MONITOR_LIST,
            TRADE_LIST,
            REPORT_LIST
        }

        private const val TYPE_A = 0
        private const val TYPE_B = 1
    }

    var monitorKeyList: List<String>? = null
    var tradeKeyList: List<String>? = null
    var reportList: List<OrderCoinInfo>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinHolder {
        val view = when (type) {
            MONITOR_LIST -> {
                LayoutInflater.from(context).inflate(
                    if (viewType == TYPE_A) R.layout.coin_monitor_item else R.layout.coin_monitor_item_selected,
                    parent, false)
            }
            TRADE_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_trade_item, parent, false)
            }
            REPORT_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_report_item, parent, false)
            }
        }
        return CoinHolder(view, type)
    }

    override fun onBindViewHolder(holder: CoinHolder, position: Int) {
        when (type) {
            MONITOR_LIST -> {
                monitorList(holder, position)
            }
            TRADE_LIST -> {
                tradeList(holder, position)
            }
            REPORT_LIST -> {
                reportList(holder, position)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (tradeKeyList != null && tradeKeyList!!.contains(monitorKeyList?.get(position)!!)) {
            return TYPE_B
        }
        return TYPE_A
    }

    override fun getItemCount(): Int {
        var count = 0
        when (type) {
            MONITOR_LIST -> {
                count = if (monitorKeyList == null) {
                    0
                } else {
                    monitorKeyList!!.size
                }
            }
            TRADE_LIST -> {
                count = if (tradeKeyList == null) {
                    0
                } else {
                    tradeKeyList!!.size
                }
            }
            REPORT_LIST -> {
                count = if (reportList == null) {
                    0
                } else {
                    reportList!!.size
                }
            }
        }
        return count
    }

    inner class CoinHolder(itemView: View, type: Type) : RecyclerView.ViewHolder(itemView) {
        var marketId: TextView? = null
        var tradePrice: TextView? = null

        var tradePriceRate: TextView? = null
        var minPriceRate: TextView? = null
        var tradeCount: TextView? = null
        var minPricePerAvgPrice: TextView? = null
        var bidAskRate: TextView? = null
        var bidAskPriceRate: TextView? = null

        var tradeStatus: TextView? = null
        var tradeProfit: TextView? = null
        var tradeProfitRate: TextView? = null
        var tradeBidPrice: TextView? = null
        var tradeBidTime: TextView? = null
        var tradeBuyDuration: TextView? = null

        var tradeAskPrice: TextView? = null
        var tradeAskTime: TextView? = null


        init {
            when(type) {
                MONITOR_LIST -> {
                    marketId = itemView.findViewById(R.id.market_id)
                    tradePrice = itemView.findViewById(R.id.trade_price)
                    tradePriceRate = itemView.findViewById(R.id.trade_price_rate)
                    minPriceRate = itemView.findViewById(R.id.min_price_rate)
                    tradeCount = itemView.findViewById(R.id.trade_count)
                    minPricePerAvgPrice = itemView.findViewById(R.id.min_price_per_avg_price)
                    bidAskRate = itemView.findViewById(R.id.bid_ask_rate)
                    bidAskPriceRate = itemView.findViewById(R.id.bid_ask_rate_price_volume)
                }
                TRADE_LIST -> {
                    marketId = itemView.findViewById(R.id.market_id)
                    tradeStatus = itemView.findViewById(R.id.trade_status)
                    tradeProfit = itemView.findViewById(R.id.trade_profit)
                    tradeProfitRate = itemView.findViewById(R.id.trade_profit_rate)
                    tradePrice = itemView.findViewById(R.id.trade_price)
                    tradeBidPrice = itemView.findViewById(R.id.trade_bid_price)
                    tradeBidTime = itemView.findViewById(R.id.trade_bid_time)
                    tradeBuyDuration = itemView.findViewById(R.id.trade_buy_duration)
                }
                REPORT_LIST -> {
                    marketId = itemView.findViewById(R.id.market_id)
                    tradeStatus = itemView.findViewById(R.id.trade_status)
                    tradeProfit = itemView.findViewById(R.id.trade_profit)
                    tradeProfitRate = itemView.findViewById(R.id.trade_profit_rate)
                    tradeAskPrice = itemView.findViewById(R.id.trade_ask_price)
                    tradeBidPrice = itemView.findViewById(R.id.trade_bid_price)
                    tradeAskTime = itemView.findViewById(R.id.trade_ask_time)
                    tradeBidTime = itemView.findViewById(R.id.trade_bid_time)
                }
            }
        }
    }


    private fun monitorList(holder: CoinHolder, position: Int) {
        val marketId = monitorKeyList?.get(position)
        val tradeInfo = tradeMonitorMapInfo[marketId]
        if (tradeInfo != null) {
            holder.marketId?.text = marketMapInfo[marketId]!!.koreanName

            with(tradeInfo) {
                val price = closePrice
                if (price != null) {
                    holder.tradePrice?.text = getZeroFormatString(price.toDouble())
                }
                holder.tradePriceRate?.text =
                    TradeFragment.Format.percentFormat.format(dayChangeRate)

                holder.tradePrice?.setTextColor(getTextColor(dayChangeRate))

                holder.tradePriceRate?.setTextColor(getTextColor(dayChangeRate))

                holder.minPriceRate?.text =
                    TradeFragment.Format.percentFormat.format(getPriceRate())
                holder.minPriceRate?.setTextColor(getTextColor(getPriceRate()))

                holder.tradeCount?.text =
                    TradeFragment.Format.nonZeroFormat.format(tickCount)

                holder.minPricePerAvgPrice?.text =
                    TradeFragment.Format.percentFormat.format(getAvgAccVolumeRate())
                holder.minPricePerAvgPrice?.setTextColor(getTextColor(getAvgAccVolumeRate(), 1.0))

                holder.bidAskRate?.text =
                    TradeFragment.Format.percentFormat.format(getBidAskRate())
                holder.bidAskRate?.setTextColor(getTextColor(getBidAskRate(), 0.5))

                holder.bidAskPriceRate?.text =
                    TradeFragment.Format.percentFormat.format(getBidAskPriceRate())
                holder.bidAskPriceRate?.setTextColor(getTextColor(getBidAskPriceRate(), 0.5))

            }
        }
    }

    private fun tradeList(holder: CoinHolder, position: Int) {
        val marketId = tradeKeyList?.get(position)
        val tradeInfo = TradeFragment.tradePostMapInfo[marketId]
        if (tradeInfo != null) {
            holder.marketId?.text = marketMapInfo[marketId]!!.koreanName
            with(tradeInfo) {
                holder.tradeStatus?.text = state.name
                val timeZoneFormat = TradeFragment.Format.timeFormat
                timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                holder.tradeStatus?.setTextColor(getStatusColor(state))

                if (volume != null) {
                    holder.tradeProfit?.text = getZeroFormatString(getProfitPrice()* volume!!)
                }

                holder.tradeProfitRate?.text =
                    TradeFragment.Format.percentFormat.format(getProfitRate())
                holder.tradeProfitRate?.setTextColor(getTextColor(getProfitRate()))

                val price = closePrice?.toDouble()
                if (price != null) {
                    holder.tradePrice?.text = getZeroFormatString(price)
                }

                val bidPrice = bidPrice?.price
                if (bidPrice != null) {
                    holder.tradeBidPrice?.text = getZeroFormatString(bidPrice)
                }

                if (tradeBidTime != null) {
                    holder.tradeBidTime?.text =
                        timeZoneFormat.format(tradeBidTime)
                }

                if (getBuyDuration() != null) {
                    holder.tradeBuyDuration?.text =
                        TradeFragment.Format.durationFormat.format(getBuyDuration())
                }
            }

        }
    }

    private fun reportList(holder: CoinHolder, position: Int) {
        val tradeInfo = reportList?.get(position)
        val marketId = tradeInfo?.marketId

        if (tradeInfo != null) {
            holder.marketId?.text = marketMapInfo[marketId]!!.koreanName

            with(tradeInfo) {
                holder.tradeStatus?.text = state.name

                val timeZoneFormat = TradeFragment.Format.timeFormat
                timeZoneFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                holder.tradeStatus?.setTextColor(getStatusColor(state))

                if (volume != null) {
                    holder.tradeProfit?.text = getZeroFormatString(getProfitPrice() * volume!!)
                }

                holder.tradeProfitRate?.text =
                    TradeFragment.Format.percentFormat.format(getProfitRate())
                holder.tradeProfitRate?.setTextColor(getTextColor(getProfitRate()))

                val askPrice = askPrice
                if (askPrice != null) {
                    holder.tradeAskPrice?.text = getZeroFormatString(askPrice)
                }

                val bidPrice = bidPrice?.price
                if (bidPrice != null) {
                    holder.tradeBidPrice?.text = getZeroFormatString(bidPrice)
                }

                if (tradeAskTime != null) {
                    holder.tradeAskTime?.text =
                        timeZoneFormat.format(tradeAskTime)
                }

                if (tradeBidTime != null) {
                    holder.tradeBidTime?.text =
                        timeZoneFormat.format(tradeBidTime)
                }
            }
        }
    }

    private fun getStatusColor(state: OrderCoinInfo.State?): Int {
        state ?: return Color.DKGRAY
        return when (state) {
            OrderCoinInfo.State.READY -> {
                Color.DKGRAY
            }
            OrderCoinInfo.State.BUYING -> {
                Color.GREEN
            }
            OrderCoinInfo.State.SELLING -> {
                Color.CYAN
            }
            OrderCoinInfo.State.BUY -> {
                Color.RED
            }
            OrderCoinInfo.State.SELL -> {
                Color.BLUE
            }
            else -> Color.DKGRAY
        }
    }
}