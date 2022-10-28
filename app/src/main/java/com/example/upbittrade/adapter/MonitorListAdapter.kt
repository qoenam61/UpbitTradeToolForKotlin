package com.example.upbittrade.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.database.MinCandleInfoData
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.utils.Utils
import kotlin.math.abs

class MonitorListAdapter: RecyclerView.Adapter<MonitorListAdapter.MonitorViewHolder>() {

    private val monitorMap = HashMap<String, MinCandleInfoData>()
    private val monitorList = ArrayList<String>()
    var marketsMapInfo = HashMap<String, MarketInfo>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItem(candleInfoData: MinCandleInfoData) {
        val marketId = candleInfoData.marketId
        monitorMap[marketId!!] = candleInfoData
        monitorList.add(marketId)
        notifyDataSetChanged()
    }

    inner class MonitorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val marketId: TextView
        private val tradePrice: TextView

        private val tradePriceRate: TextView
        private val minPriceRate: TextView
        private val tradeCount: TextView
        private val minPricePerAvgPrice: TextView
        private val bidAskRate: TextView
        private val bidAskPriceRate: TextView

        init {
            marketId = itemView.findViewById(R.id.market_id)
            tradePrice = itemView.findViewById(R.id.trade_price)
            tradePriceRate = itemView.findViewById(R.id.trade_price_rate)
            minPriceRate = itemView.findViewById(R.id.min_price_rate)
            tradeCount = itemView.findViewById(R.id.trade_count)
            minPricePerAvgPrice = itemView.findViewById(R.id.min_price_per_avg_price)
            bidAskRate = itemView.findViewById(R.id.bid_ask_rate)
            bidAskPriceRate = itemView.findViewById(R.id.bid_ask_rate_price_volume)
        }


        fun bind(position: Int) {
            val key = monitorList[position]
            val candleInfoData = monitorMap[key]

            marketId.text = marketsMapInfo[key]?.koreanName
            with(candleInfoData) {
                tradePrice.text = getZeroFormatString(this?.tradePrice)
                tradePriceRate.text = Utils.Format.timeFormat.format(this?.timestamp)
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