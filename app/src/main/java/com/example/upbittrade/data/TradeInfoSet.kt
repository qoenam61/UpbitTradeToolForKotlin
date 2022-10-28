package com.example.upbittrade.data

import kotlinx.coroutines.sync.Mutex

class TradeInfoSet(private val listener: OnChangedListener): HashSet<String>() {

    interface OnChangedListener {
        fun onSetChanged(tradeInfoSet: HashSet<String>, mutex: Mutex)
    }

    val mutex = Mutex()

    override fun add(element: String): Boolean {
        val result = super.add(element)
        listener.onSetChanged(this, mutex)
        return result
    }
}