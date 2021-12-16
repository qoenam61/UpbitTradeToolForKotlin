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
import com.example.upbittrade.utils.TradeAdapter.Companion.Type
import com.example.upbittrade.utils.TradeAdapter.Companion.Type.MONITOR_LIST

class TradeAdapter(private val context: Context, val type: Type): RecyclerView.Adapter<CoinHolder>() {
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
        Log.d(TAG, "[DEBUG] onCreateViewHolder itemCount : $itemCount")
        return CoinHolder(view, type)
    }

    override fun onBindViewHolder(holder: CoinHolder, position: Int) {
        Log.d(TAG, "[DEBUG] onBindViewHolder itemCount : $itemCount")
        when (type) {
            MONITOR_LIST -> {
                val marketId = monitorMap?.get(position)
                val tradeInfo = TradeFragment.tradeInfo[marketId]
                if (tradeInfo != null) {
                    holder.marketId?.text = tradeInfo.marketId
                    holder.trade_price?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.closePrice!!.toDouble())
                    holder.trade_price_rate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.changeRate)
                    holder.min_price_rate?.text =
                        TradeFragment.Format.percentFormat.format(tradeInfo.getMinPriceRate())
                    holder.trade_count?.text =
                        TradeFragment.Format.nonZeroFormat.format(tradeInfo.tickCount)
                    holder.min_price_per_avg_price?.text =
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
        Log.d(TAG, "[DEBUG] getItemCount - type: $type")
        return when (type) {
            MONITOR_LIST -> {
                if (monitorMap == null) {
                    0
                } else {
                    monitorMap!!.size
                }
                Log.d(TAG, "[DEBUG] getItemCount - size: ${monitorMap?.size}")
            }
            Type.TRADE_LIST -> {
                if (tradeSet == null) {
                    0
                } else {
                    tradeSet!!.size
                }
            }
            Type.RESULT_LIST -> {
                if (resultSet == null) {
                    0
                } else {
                    resultSet!!.size
                }
            }
        }
    }
}

class CoinHolder : RecyclerView.ViewHolder {
    var marketId: TextView? = null
    var trade_price: TextView? = null
    var trade_price_rate: TextView? = null
    var min_price_rate: TextView? = null
    var trade_count: TextView? = null
    var min_price_per_avg_price: TextView? = null

    constructor(itemView: View, type: Type) : super(itemView) {
        when(type) {
            MONITOR_LIST -> {
                marketId = itemView.findViewById(R.id.market_id)
                trade_price = itemView.findViewById(R.id.trade_price)
                trade_price_rate = itemView.findViewById(R.id.trade_price_rate)
                min_price_rate = itemView.findViewById(R.id.min_price_rate)
                trade_count = itemView.findViewById(R.id.trade_count)
                min_price_per_avg_price = itemView.findViewById(R.id.min_price_per_avg_price)
            }
        }
    }
}