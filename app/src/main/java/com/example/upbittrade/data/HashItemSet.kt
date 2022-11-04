package com.example.upbittrade.data

import kotlinx.coroutines.sync.Mutex

class HashItemSet(private val listener: OnChangedListener): HashSet<String>() {

    interface OnChangedListener {
        fun onSetDataChanged(itemSet: HashSet<String>, mutex: Mutex)
    }

    private val mutex = Mutex()

    override fun add(marketId: String): Boolean {
        val result: Boolean
        if (!contains(marketId)) {
            result = super.add(marketId)
            listener.onSetDataChanged(this, mutex)
        } else {
            result = super.add(marketId)
        }
        return result
    }

    override fun remove(element: String): Boolean {
        val result = super.remove(element)
        listener.onSetDataChanged(this, mutex)
        return result
    }
}