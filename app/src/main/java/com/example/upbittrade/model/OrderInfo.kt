package com.example.upbittrade.model

import java.text.DecimalFormat

@Suppress("LocalVariableName")
class OrderInfo {
    val uuid: String? = null
    val identifier: String? = null

    var openPrice = 0.0
    var closePrice = 0.0
    var highPrice = 0.0
    var lowPrice = 0.0

    var buyTime: Long = 0
    var sellTime: Long = 0

    var maxProfitRate = 0.0
    var buyingAmount = 0.0
    var sellingAmount = 0.0

    var currentStatus = STATUS.NONE

    enum class STATUS {
        NONE,
        READY,
        WAIT,
        BUY,
        SELL
    }
}