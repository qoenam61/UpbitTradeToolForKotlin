package com.example.upbittrade.model

class OrderCoinInfo: TradeCoinInfo() {
    enum class Status {
        READY,
        WAIT,
        BUY,
        SELL
    }

    var status: Status = Status.READY
}