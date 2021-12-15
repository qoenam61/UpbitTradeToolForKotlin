package com.example.upbittrade.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.model.ResultTradeInfo
import com.example.upbittrade.model.TradeInfo

class TradeFragmentView(view: View) {
    companion object {
        enum class Type {
            MONITOR_LIST,
            TRADE_LIST,
            RESULT_LIST
        }
    }

    class CoinHolder : RecyclerView.ViewHolder {
        constructor(itemView: View, type: Type) : super(itemView) {

        }
    }

    class TradeAdapter(val type: Type): RecyclerView.Adapter<CoinHolder>() {
        var monitorSet = HashMap<String, ResultTradeInfo>()
        var tradeSet = HashMap<String, ResultTradeInfo>()
        var resultSet = HashMap<String, ResultTradeInfo>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinHolder {
            var view = when (type) {
                Type.MONITOR_LIST -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.coin_monitor_item, parent, false)
                }
                Type.TRADE_LIST -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.coin_trade_item, parent, false)
                }
                Type.RESULT_LIST -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.coin_result_item, parent, false)
                }
            }
            return CoinHolder(view!!, type)
        }

        override fun onBindViewHolder(holder: CoinHolder, position: Int) {
        }

        override fun getItemCount(): Int {
            return when (type) {
                Type.MONITOR_LIST -> {
                    monitorSet.size
                }
                Type.TRADE_LIST -> {
                    tradeSet.size
                }
                Type.RESULT_LIST -> {
                    resultSet.size
                }
            }
        }
    }
}