package com.example.upbittrade.data

import com.example.upbittrade.adapter.TradeItem

class ReportArrayList(val listener: OnReportListener): ArrayList<TradeItem>() {

    interface OnReportListener {
        fun onChangedItem(item: TradeItem, state: State)
    }

    enum class State {
        Add,
        Remove
    }

    override fun add(element: TradeItem): Boolean {
        val result = super.add(element)
        listener.onChangedItem(element, State.Add)
        return result
    }

    override fun remove(element: TradeItem): Boolean {
        val result = super.remove(element)
        listener.onChangedItem(element, State.Remove)
        return result
    }
}