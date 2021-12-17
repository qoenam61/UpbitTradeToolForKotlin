package com.example.upbittrade.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.fragment.TradeFragment
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST

class TradeAdapter(private val context: Context, val type: Type): RecyclerView.Adapter<TradeAdapter.CoinHolder>() {
    companion object {
        const val TAG = "TradeFragmentView"
        enum class Type {
            MONITOR_LIST,
            TRADE_LIST,
            RESULT_LIST
        }
    }

    var monitorMap: List<String>? = null
    var tradeSet: List<String>? = null
    var resultSet: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinHolder {
        var view = when (type) {
            MONITOR_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_monitor_item, parent, false)
            }
            Type.TRADE_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_trade_item, parent, false)
            }
            Type.RESULT_LIST -> {
                LayoutInflater.from(context).inflate(R.layout.coin_result_item, parent, false)
            }
        }
        Log.d(TAG, "[DEBUG] onCreateViewHolder itemCount : $itemCount viewType $viewType")
        return CoinHolder(view, type)
    }

    override fun onBindViewHolder(holder: CoinHolder, position: Int) {
        Log.d(TAG, "[DEBUG] onBindViewHolder itemCount : $itemCount ")
        when (type) {
            MONITOR_LIST -> {
                val marketId = monitorMap?.get(position)
                val tradeInfo = TradeFragment.tradeInfo[marketId]
                if (tradeInfo != null) {
                    holder.marketId?.text = TradeFragment.marketMapInfo[marketId]!!.koreanName
                    holder.tradePrice?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.closePrice!!.toDouble())
                    holder.tradePriceRate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.changeRate)
                    holder.minPriceRate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getMinPriceRate())
                    holder.tradeCount?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.tickCount)
                    holder.minPricePerAvgPrice?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getPriceVolumeRate())
                }
            }
            Type.TRADE_LIST -> {
                tradeSet?.size
            }
            Type.RESULT_LIST -> {
                resultSet?.size
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        when (type) {
            MONITOR_LIST -> {
                count = if (monitorMap == null) {
                    0
                } else {
                    monitorMap!!.size
                }
                Log.d(TAG, "[DEBUG] getItemCount - size: ${monitorMap?.size}")
            }
            Type.TRADE_LIST -> {
                count = if (tradeSet == null) {
                    0
                } else {
                    tradeSet!!.size
                }
            }
            Type.RESULT_LIST -> {
                count = if (resultSet == null) {
                    0
                } else {
                    resultSet!!.size
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

        constructor(itemView: View, type: Type) : super(itemView) {
            when(type) {
                MONITOR_LIST -> {
                    marketId = itemView.findViewById(R.id.market_id)
                    tradePrice = itemView.findViewById(R.id.trade_price)
                    tradePriceRate = itemView.findViewById(R.id.trade_price_rate)
                    minPriceRate = itemView.findViewById(R.id.min_price_rate)
                    tradeCount = itemView.findViewById(R.id.trade_count)
                    minPricePerAvgPrice = itemView.findViewById(R.id.min_price_per_avg_price)
                }
            }
        }
    }
}