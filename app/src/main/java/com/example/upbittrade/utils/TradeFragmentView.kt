package com.example.upbittrade.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R

class TradeFragmentView {
    companion object {
        enum class Type {
            MONITOR,
            TRADE_LIST,
            RESULT_LIST
        }
    }

    fun newInstance(view: View): TradeFragmentView {
        return TradeFragmentView()
    }

    class CoinHolder : RecyclerView.ViewHolder {
        constructor(itemView: View, type: Type) : super(itemView) {

        }
    }

    class TradeAdapter(val type: Type): RecyclerView.Adapter<CoinHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinHolder {
            var view = when (type) {
                Type.MONITOR -> {
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
            TODO("Not yet implemented")
        }

        override fun getItemCount(): Int {
            TODO("Not yet implemented")
        }

    }
}