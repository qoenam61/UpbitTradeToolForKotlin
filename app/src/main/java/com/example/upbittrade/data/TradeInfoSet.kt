package com.example.upbittrade.data

import kotlinx.coroutines.sync.Mutex

class TradeInfoSet(private val listener: OnChangedListener): HashSet<String>() {

    interface OnChangedListener {
        fun onSetChanged(tradeInfoSet: HashSet<String>, mutex: Mutex)
    }

    val mutex = Mutex()

    override fun add(marketId: String): Boolean {
        val result = super.add(marketId)
        if (!contains(marketId)) {
            listener.onSetChanged(this, mutex)
        }
        return result
    }

    override fun remove(element: String): Boolean {
        val result = super.remove(element)
        listener.onSetChanged(this, mutex)
        return result
    }
}