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
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.TRADE_LIST

class TradeAdapter(private val context: Context, val type: Type): RecyclerView.Adapter<TradeAdapter.CoinHolder>() {
    companion object {
        const val TAG = "TradeFragmentView"
        enum class Type {
            MONITOR_LIST,
            TRADE_LIST,
            RESULT_LIST
        }
    }

    var monitorKeyList: List<String>? = null
    var tradeKeyList: List<String>? = null
    var resultKeyList: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinHolder {
        var view = when (type) {
            MONITOR_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_monitor_item, parent, false)
            }
            TRADE_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_trade_item, parent, false)
            }
            Type.RESULT_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_result_item, parent, false)
            }
        }
        return CoinHolder(view, type)
    }

    override fun onBindViewHolder(holder: CoinHolder, position: Int) {
        when (type) {
            MONITOR_LIST -> {
                val marketId = monitorKeyList?.get(position)
                val tradeInfo = TradeFragment.tradeMonitorMapInfo[marketId]
                if (tradeInfo != null) {
                    holder.marketId?.text = TradeFragment.marketMapInfo[marketId]!!.koreanName
                    holder.tradePrice?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.closePrice!!.toDouble())
                    holder.tradePriceRate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.dayChangeRate)
                    when {
                        tradeInfo.dayChangeRate!!.compareTo(0.0) > 0 -> {
                            holder.tradePrice?.setTextColor(Color.RED)
                            holder.tradePriceRate?.setTextColor(Color.RED)
                        }
                        tradeInfo.dayChangeRate!!.compareTo(0.0) < 0 -> {
                            holder.tradePrice?.setTextColor(Color.BLUE)
                            holder.tradePriceRate?.setTextColor(Color.BLUE)
                        }
                        else -> {
                            holder.tradePrice?.setTextColor(Color.BLACK)
                            holder.tradePriceRate?.setTextColor(Color.BLACK)
                        }
                    }

                    holder.minPriceRate?.text =
                            TradeFragment.Format.percentFormat.format(tradeInfo.getPriceRangeRate())
                    when {
                        tradeInfo.getPriceRangeRate().compareTo(0.0) > 0 -> {
                            holder.minPriceRate?.setTextColor(Color.RED)
                        }
                        tradeInfo.getPriceRangeRate().compareTo(0.0) < 0 -> {
                            holder.minPriceRate?.setTextColor(Color.BLUE)
                        }
                        else -> {
                            holder.minPriceRate?.setTextColor(Color.BLACK)
                        }
                    }

                    holder.tradeCount?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.tickCount)

                    holder.minPricePerAvgPrice?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getAvgAccVolumeRate())
                    when {
                        tradeInfo.getAvgAccVolumeRate().compareTo(1.0) > 0 -> {
                            holder.minPricePerAvgPrice?.setTextColor(Color.RED)
                        }
                        tradeInfo.getAvgAccVolumeRate().compareTo(1.0) < 0 -> {
                            holder.minPricePerAvgPrice?.setTextColor(Color.BLUE)
                        }
                        else -> {
                            holder.minPricePerAvgPrice?.setTextColor(Color.BLACK)
                        }
                    }
                    holder.bidAskRate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getBidAskRate())
                    when {
                        tradeInfo.getBidAskRate().compareTo(0.5) > 0 -> {
                            holder.bidAskRate?.setTextColor(Color.RED)
                        }
                        tradeInfo.getBidAskRate().compareTo(0.5) < 0 -> {
                            holder.bidAskRate?.setTextColor(Color.BLUE)
                        }
                        else -> {
                            holder.bidAskRate?.setTextColor(Color.BLACK)
                        }
                    }
                    holder.bidAskPriceRate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getBidAskPriceRate())
                    when {
                        tradeInfo.getBidAskPriceRate().compareTo(0.5) > 0 -> {
                            holder.bidAskPriceRate?.setTextColor(Color.RED)
                        }
                        tradeInfo.getBidAskRate().compareTo(0.5) < 0 -> {
                            holder.bidAskPriceRate?.setTextColor(Color.BLUE)
                        }
                        else -> {
                            holder.bidAskPriceRate?.setTextColor(Color.BLACK)
                        }
                    }
                }
            }
            TRADE_LIST -> {

                val marketId = tradeKeyList?.get(position)
                val tradeInfo = TradeFragment.tradePostMapInfo[marketId]
                if (tradeInfo != null) {
                    holder.marketId?.text = TradeFragment.marketMapInfo[marketId]!!.koreanName
                    holder.tradeStatus?.text = tradeInfo.status.name

                    if (tradeInfo.getProfit() != null) {
                        holder.tradeProfit?.text =
                            TradeFragment.Format.nonZeroFormat.format(tradeInfo.getProfit())
                    }

                    if (tradeInfo.getProfitRate() != null) {
                        holder.tradeProfitRate?.text =
                            TradeFragment.Format.percentFormat.format(tradeInfo.getProfitRate())

                        when {
                            tradeInfo.getProfitRate()!!.compareTo(0.0) > 0 -> {
                                holder.tradeProfitRate?.setTextColor(Color.RED)
                            }
                            tradeInfo.getProfitRate()!!.compareTo(0.0) < 0 -> {
                                holder.tradeProfitRate?.setTextColor(Color.BLUE)
                            }
                            else -> {
                                holder.tradeProfitRate?.setTextColor(Color.BLACK)
                            }
                        }
                    }
                    if (tradeInfo.currentPrice != null) {
                        holder.tradePrice?.text =
                            TradeFragment.Format.nonZeroFormat.format(tradeInfo.currentPrice)
                    }

                    if (tradeInfo.getBidPrice() != null) {
                        holder.tradeBidPrice?.text =
                            TradeFragment.Format.nonZeroFormat.format(tradeInfo.getBidPrice())
                    }
                    if (tradeInfo.tradeBuyTime != null) {
                        holder.tradeBuyTime?.text =
                            TradeFragment.Format.timeFormat.format(tradeInfo.tradeBuyTime)
                    }

                    if (tradeInfo.getBuyDuration() != null) {
                        holder.tradeBuyDuration?.text =
                            TradeFragment.Format.timeFormat.format(tradeInfo.getBuyDuration())
                    }
                }
            }
            Type.RESULT_LIST -> {
                resultKeyList?.size
            }
        }
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
            Type.RESULT_LIST -> {
                count = if (resultKeyList == null) {
                    0
                } else {
                    resultKeyList!!.size
                }
            }
        }
        return count
    }

    inner class CoinHolder : RecyclerView.ViewHolder {
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
        var tradeBuyTime: TextView? = null
        var tradeBuyDuration: TextView? = null


        constructor(itemView: View, type: Type) : super(itemView) {
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
                    tradeBuyTime = itemView.findViewById(R.id.trade_buy_time)
                    tradeBuyDuration = itemView.findViewById(R.id.trade_buy_duration)
                }
            }
        }
    }
}