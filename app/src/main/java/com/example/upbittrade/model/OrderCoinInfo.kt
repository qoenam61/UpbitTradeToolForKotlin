package com.example.upbittrade.model

class OrderCoinInfo: TradeCoinInfo() {
    enum class Status {
        READY,
        WAIT,
        BUY,
        SELL
    }

    var status: Status = Status.READY

    val bidPrice: Double? = null

    val askPrice: Double? = null

    val tickPrice: Double? = null


}