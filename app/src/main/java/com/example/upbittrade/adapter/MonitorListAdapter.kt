package com.example.upbittrade.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.database.MinCandleInfoData
import com.example.upbittrade.database.TradeInfoData
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.model.TradeViewModel
import com.example.upbittrade.utils.Utils
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.abs

class MonitorListAdapter() : RecyclerView.Adapter<MonitorListAdapter.MonitorViewHolder>() {

    var monitorMap = HashMap<String, MonitorItem>()
    var monitorList = ArrayList<String>()
    var marketsMapInfo = HashMap<String, MarketInfo>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItem(monitorItem: MonitorItem) {
        val marketId = monitorItem.marketId

        if (!monitorMap.containsKey(marketId)) {
            monitorList.add(marketId!!)
        }
        monitorMap[marketId!!] = monitorItem

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(tradeInfoData: TradeInfoData) {
        val marketId = tradeInfoData.marketId
        if (monitorMap.containsKey(marketId)) {
            monitorMap[marketId]?.tradePrice = tradeInfoData.tradePrice
            monitorMap[marketId]?.timestamp = tradeInfoData.timestamp
            monitorMap[marketId]?.prevClosingPrice = tradeInfoData.prevClosingPrice
            monitorMap[marketId]?.changePrice = tradeInfoData.changePrice
            monitorMap[marketId]?.askBidRate = 0f
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(minCandleInfoData: MinCandleInfoData) {
        val marketId = minCandleInfoData.marketId
        if (monitorMap.containsKey(marketId)) {
            monitorMap[marketId]?.tradePrice = minCandleInfoData.tradePrice
            monitorMap[marketId]?.timestamp = minCandleInfoData.timestamp
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(marketId: String) {
        monitorList.remove(marketId)
        monitorMap.remove(marketId)
        notifyDataSetChanged()
    }

    inner class MonitorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val marketId: TextView
        private val tradePrice: TextView

        private val tradePriceRate: TextView
        private val timeStamp: TextView
        private val avgPrice: TextView
        private val devPrice: TextView
        private val bidAskRate: TextView
        private val bidAskPriceRate: TextView

        init {
            marketId = itemView.findViewById(R.id.market_id)
            tradePrice = itemView.findViewById(R.id.trade_price)
            tradePriceRate = itemView.findViewById(R.id.trade_price_rate)
            timeStamp = itemView.findViewById(R.id.timestamp)
            avgPrice = itemView.findViewById(R.id.avgPrice)
            devPrice = itemView.findViewById(R.id.devPrice)
            bidAskRate = itemView.findViewById(R.id.bid_ask_rate)
            bidAskPriceRate = itemView.findViewById(R.id.bid_ask_rate_price_volume)
        }


        fun bind(position: Int) {
            val key = monitorList[position]
            val monitorItem = monitorMap[key]

            marketId.text = marketsMapInfo[key]?.koreanName
            with(monitorItem) {
                tradePrice.text = getZeroFormatString(this?.tradePrice)
                this?.prevClosingPrice?.let {
                    tradePriceRate.text = Utils.Format.percentFormat.format((this.tradePrice!! - it).div( this.tradePrice!!))
                }
                timeStamp.text = Utils.Format.timeFormat.format(this?.timestamp)
                avgPrice.text = getZeroFormatString(this?.avgPrice)
                devPrice.text = Utils.Format.zeroFormat.format(this?.devPrice)

                this?.askBidRate?.let {
                    bidAskRate.text = Utils.Format.percentFormat.format(it)
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.coin_monitor_item, parent, false)
        return MonitorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonitorViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return monitorList.size
    }

    fun getZeroFormatString(value: Double?): String {
        value ?: return ""
        return when {
            abs(value) < 100.0 && abs(value) >= 1.0-> {
                Utils.Format.zeroFormat.format(value)
            }
            abs(value) < 1.0 -> {
                Utils.Format.zeroFormat2.format(value)
            }
            else -> {
                Utils.Format.nonZeroFormat.format(value)
            }
        }
    }
}