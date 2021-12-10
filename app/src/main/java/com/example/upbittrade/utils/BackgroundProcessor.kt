package com.example.upbittrade.utils

import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.upbittrade.activity.TradePagerActivity.PostType.*
import com.example.upbittrade.data.ExtendCandleItem
import com.example.upbittrade.data.TaskItem
import com.example.upbittrade.model.TradeViewModel
import okhttp3.internal.notify
import okhttp3.internal.wait
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BackgroundProcessor : Thread {
    companion object {
        const val TAG = "BackgroundProcessor"
        var viewModel: AndroidViewModel? = null
        val pingDelay: Long = 100
    }

    constructor(model: AndroidViewModel) {
        viewModel = model
    }

    object ThreadProcess {
        val executor = ThreadPoolExecutor(1, 4, pingDelay, TimeUnit.SECONDS, LinkedBlockingDeque())
    }

    override fun run() {
        while (true) {
            var taskList: TaskList? = null
            if (!TaskList.isNullOrEmpty()) {
                taskList = TaskList
                ThreadProcess.executor.execute(taskList)

                Log.w(TAG, "[DEBUG] sendMessage end")
            }

            try {
                Log.i(
                    TAG,
                    "[DEBUG] run: " + SystemClock.uptimeMillis() + " size: " + taskList?.size
                )
                sleep(pingDelay * (taskList?.size?.toLong() ?: 10) + 1)
            } catch (e: InterruptedException) {
                Log.w(TAG, "exception Thread sleep")
                break
            }
        }
    }

    object MyHandler: android.os.Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val item: TaskItem = msg.data.getSerializable("TaskItem") as TaskItem
            Log.d(TAG, "[DEBUG] MyHandler - $item")
            when(item.type) {
                MARKETS_INFO -> {
                    if (viewModel is TradeViewModel) {
                        Log.d(TAG, "[DEBUG] handleMessage: MARKETS_INFO")
                        (viewModel as TradeViewModel).searchMarketsInfo.value = true
                    }
                }
                POST_ORDER_INFO -> {}
                DELETE_ORDER_INFO -> {}
                MIN_CANDLE_INFO -> {
                    if (viewModel is TradeViewModel) {
                        Log.d(TAG, "[DEBUG] handleMessage: MIN_CANDLE_INFO")
                        (viewModel as TradeViewModel).searchMinCandleInfo.value = (item as ExtendCandleItem)
                    }
                }
                DAY_CANDLE_INFO -> {
                    if (viewModel is TradeViewModel) {
                        Log.d(TAG, "[DEBUG] handleMessage: DAY_CANDLE_INFO")
                        (viewModel as TradeViewModel).searchDayCandleInfo.value = (item as ExtendCandleItem)
                    }
                }
                WEEK_CANDLE_INFO -> {}
                MONTH_CANDLE_INFO -> {}
                ACCOUNTS_INFO -> {}
                CHANCE_INFO -> {}
                TICKER_INFO -> {}
                TRADE_INFO -> {}
                SEARCH_ORDER_INFO -> {}
            }
        }
    }

    object TaskList: ConcurrentLinkedDeque<TaskItem>(), Runnable {
        override fun run() {
            val iterator = iterator()
            while (iterator.hasNext()) {
                val taskItem = iterator.next()
                Log.d(TAG, "[DEBUG] MyHandler - $taskItem")
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
                    Log.e(TAG, "run: ", e)
                }
            }
        }

        private fun sendMessage(item: TaskItem) {
            Log.d(TAG, "[DEBUG] sendMessage - $item")

            val bundle = Bundle()
            bundle.putSerializable("TaskItem", item)
            val message = Message()
            message.what = item.type.ordinal
            message.data = bundle
            MyHandler.sendMessage(message)
        }
    }

    fun registerProcess(item: TaskItem) {
        TaskList.offer(item)
    }
}