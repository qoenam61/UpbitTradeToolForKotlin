package com.example.upbittrade.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.upbittrade.R
import com.example.upbittrade.model.MarketInfo
import com.example.upbittrade.utils.Utils
import java.util.HashMap

class TradeReportListAdapter: RecyclerView.Adapter<TradeReportListAdapter.ReportViewHolder>() {

    val reportList = ArrayList<TradeItem>()
    var marketsMapInfo = HashMap<String, MarketInfo>()

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var marketId: TextView
        var tradeStatus: TextView
        var tradeProfit: TextView
        var tradeProfitRate: TextView
        var tradeBidPrice: TextView
        var tradeBidTime: TextView
        var tradeAskPrice: TextView
        var tradeAskTime: TextView

        init {
            marketId = itemView.findViewById(R.id.market_id)
            tradeStatus = itemView.findViewById(R.id.trade_status)
            tradeProfit = itemView.findViewById(R.id.trade_profit)
            tradeProfitRate = itemView.findViewById(R.id.trade_profit_rate)
            tradeAskPrice = itemView.findViewById(R.id.trade_ask_price)
            tradeBidPrice = itemView.findViewById(R.id.trade_bid_price)
            tradeAskTime = itemView.findViewById(R.id.trade_ask_time)
            tradeBidTime = itemView.findViewById(R.id.trade_bid_time)
        }
        fun bind(position: Int) {
            val marketId = reportList[position].marketId
            val reportItem = reportList[position]

            this.marketId.text = marketsMapInfo[marketId]!!.koreanName
            with(reportItem) {
                tradeStatus.text = state.name
                tradeProfit.text = Utils.getZeroFormatString((sellPrice!! - buyPrice!!))
                tradeProfitRate.text = Utils.Format.percentFormat.format(Utils.getTextColor((sellPrice!! - buyPrice!!) / buyPrice!!))
                tradeAskPrice.text = Utils.getZeroFormatString(sellPrice)
                tradeBidPrice.text = Utils.getZeroFormatString(buyPrice)
                tradeAskTime.text = Utils.Format.timeFormat.format(sellTime)
                tradeBidTime.text = Utils.Format.timeFormat.format(buyTime)
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.coin_report_item, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return reportList.size
    }
}