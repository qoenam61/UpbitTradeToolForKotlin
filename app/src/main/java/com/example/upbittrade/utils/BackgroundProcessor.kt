package com.example.upbittrade.utils

import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.data.CandleItem
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.TradeViewModel
import okhttp3.internal.notify
import okhttp3.internal.wait
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class BackgroundProcessor : Thread {
    companion object {
        const val TAG = "BackgroundProcessor"
        var viewModel: AndroidViewModel? = null
        val pingDelay: Long = 100
    }

    constructor(model: AndroidViewModel) {
        viewModel = model
    }

    var isRunning = false

    object ThreadProcess {
        val executor = ThreadPoolExecutor(1, 4, pingDelay, TimeUnit.SECONDS, LinkedBlockingDeque())
    }

    override fun run() {
        isRunning = false
        while (true) {
            isRunning = true
            var taskList: TaskList? = null
            if (!TaskList.isNullOrEmpty()) {
                taskList = TaskList
                ThreadProcess.executor.execute(taskList)
            }

            try {
                Log.i(
                    TAG,
                    "[DEBUG] run: " + SystemClock.uptimeMillis() + " size: " + taskList?.size
                )
                sleep(pingDelay * (taskList?.size?.toLong() ?: 10) + 1)
            } catch (e: InterruptedException) {
                isRunning = false
                Log.w(TAG, "exception Thread sleep")
                break
            }
        }
    }

    object MyHandler: android.os.Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val item: TaskItem = msg.data.getSerializable("TaskItem") as TaskItem
            when(item.type) {
                MARKETS_INFO -> {
                    if (viewModel is TradeViewModel) {
                        (viewModel as TradeViewModel).searchMarketsInfo.value = true
                    }
                }
                POST_ORDER_INFO -> {}
                DELETE_ORDER_INFO -> {}
                MIN_CANDLE_INFO -> {
                    if (viewModel is TradeViewModel) {
                        (viewModel as TradeViewModel).searchMinCandleInfo.value = (item as ExtendCandleItem)
                    }
                }
                DAY_CANDLE_INFO -> {
                    if (viewModel is TradeViewModel) {
                        (viewModel as TradeViewModel).searchDayCandleInfo.value = (item as ExtendCandleItem)
                    }
                }
                WEEK_CANDLE_INFO -> {}
                MONTH_CANDLE_INFO -> {}
                ACCOUNTS_INFO -> {}
                CHANCE_INFO -> {}
                TICKER_INFO -> {
                    if (viewModel is TradeViewModel) {
                        (viewModel as TradeViewModel).searchTickerInfo.value = item.marketId
                    }
                }
                TRADE_INFO -> {
                    if (viewModel is TradeViewModel) {
                        (viewModel as TradeViewModel).searchTradeInfo.value = (item as CandleItem)
                    }
                }
                SEARCH_ORDER_INFO -> {}
            }
        }
    }

    object TaskList: ConcurrentLinkedDeque<TaskItem>(), Runnable {
        override fun run() {
            val iterator = iterator()
            while (iterator.hasNext()) {
                val taskItem = iterator.next()
                when(taskItem.type) {
                    MARKETS_INFO, MIN_CANDLE_INFO  -> {
                        sendMessage(taskItem)
                        poll()
                    } else -> {
                        sendMessage(taskItem)
                    }
                }
                try {
                    sleep(pingDelay)
                } catch (e : InterruptedException) {
                    Log.e(TAG, "InterruptedException: exit sleep")
                }
            }
        }

        private fun sendMessage(item: TaskItem) {
            val bundle = Bundle()
            bundle.putSerializable("TaskItem", item)
            val message = Message()
            message.what = item.type.ordinal
            message.data = bundle
            MyHandler.sendMessage(message)
        }
    }

    fun registerProcess(list: List<TaskItem>) {
        TaskList.addAll(list)
    }

    fun registerProcess(item: TaskItem) {
        val iterator = TaskList.iterator()
        while (iterator.hasNext()) {
            val list = iterator.next()
            if (list.type == item.type && list.marketId.equals(item.marketId)) {
                Log.d(TAG, "registerProcess type: ${item.type} duplicated id: " + item.marketId)
                return
            }
        }

        Log.d(TAG, "registerProcess type: ${item.type} offer id: " + item.marketId)
        TaskList.offer(item)
    }

    fun unregisterProcess(item: TaskItem) {
        TaskList.remove(item)
    }

    fun release() {
        interrupt()
        TaskList.clear()
    }
}