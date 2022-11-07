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

class TradeListAdapter() : RecyclerView.Adapter<TradeListAdapter.TradeListViewHolder>() {

    var tradeMap = HashMap<String, TradeItem>()
    var tradeList = ArrayList<String>()
    var marketsMapInfo = HashMap<String, MarketInfo>()


    @SuppressLint("NotifyDataSetChanged")
    fun setItem(tradeItem: TradeItem) {
        val marketId = tradeItem.marketId

        if (!tradeMap.containsKey(marketId)) {
            tradeList.add(marketId!!)
        }
        tradeMap[marketId!!] = tradeItem

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(tradeInfoData: TradeInfoData) {
        val marketId = tradeInfoData.marketId
        if (tradeMap.containsKey(marketId)) {
            if (tradeInfoData is TradeItem) {
                tradeMap[marketId]?.state = tradeInfoData.state
            }
            tradeMap[marketId]?.tradePrice = tradeInfoData.tradePrice
            tradeMap[marketId]?.timestamp = tradeInfoData.timestamp
            tradeMap[marketId]?.prevClosingPrice = tradeInfoData.prevClosingPrice
            tradeMap[marketId]?.changePrice = tradeInfoData.changePrice
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(minCandleInfoData: MinCandleInfoData) {
        val marketId = minCandleInfoData.marketId
        if (tradeMap.containsKey(marketId)) {
            tradeMap[marketId]?.tradePrice = minCandleInfoData.tradePrice
            tradeMap[marketId]?.timestamp = minCandleInfoData.timestamp
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(marketId: String) {
        tradeList.remove(marketId)
        tradeMap.remove(marketId)
        notifyDataSetChanged()
    }

    inner class TradeListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var marketId: TextView
        var tradePriceView: TextView

        var tradeStatus: TextView
        var tradeProfit: TextView
        var tradeProfitRate: TextView
        var tradeBidPrice: TextView
        var tradeBidTime: TextView
        var tradeAskTime: TextView

        init {
            marketId = itemView.findViewById(R.id.market_id)
            tradeStatus = itemView.findViewById(R.id.trade_status)
            tradeProfit = itemView.findViewById(R.id.trade_profit)
            tradeProfitRate = itemView.findViewById(R.id.trade_profit_rate)
            tradePriceView = itemView.findViewById(R.id.trade_price)
            tradeBidPrice = itemView.findViewById(R.id.trade_bid_price)
            tradeBidTime = itemView.findViewById(R.id.trade_bid_time)
            tradeAskTime = itemView.findViewById(R.id.trade_ask_time)
        }

        fun onBind(position: Int) {
            val key = tradeList[position]
            val tradeItem = tradeMap[key]

            marketId.text = marketsMapInfo[key]?.koreanName

            with(tradeItem) {
                tradeStatus.text = this?.state?.name
                tradeProfit.text = Utils.getZeroFormatString((this?.tradePrice!! - buyPrice!!))
                tradeProfitRate.text = Utils.Format.percentFormat.format((this.tradePrice!! - this.buyPrice!!).div( this.buyPrice!!))
                tradePriceView.text = getZeroFormatString(this.tradePrice)
                tradeBidPrice.text = getZeroFormatString(this.buyPrice!!)
                buyTime?.let { tradeBidTime.text = Utils.Format.timeFormat.format(it)}
                sellTime?.let { tradeAskTime.text = Utils.Format.timeFormat.format(it)}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.coin_trade_item, parent, false)
        return TradeListViewHolder(view)
    }

    override fun onBindViewHolder(holder: TradeListViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        return tradeList.size
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