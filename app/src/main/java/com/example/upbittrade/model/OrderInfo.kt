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

    fun convertPrice(price: Double): Double {
        val mFormatUnder10 = DecimalFormat("#.##")
        val mFormatUnder100 = DecimalFormat("##.#")
        val mFormatUnder1_000 = DecimalFormat("###")
        val mFormatUnder10_000 = DecimalFormat("####")
        val mFormatUnder100_000 = DecimalFormat("#####")
        val mFormatUnder1_000_000 = DecimalFormat("######")
        val mFormatUnder10_000_000 = DecimalFormat("#######")
        val mFormatUnder100_000_000 = DecimalFormat("########")
        var result: String? = null
        var priceResult = 0.0

        when {
            price < 10 -> {
                priceResult = Math.floor(price * 100) / 100
                result = mFormatUnder10.format(priceResult)
            }
            price < 100 -> {
                priceResult = Math.floor(price * 10) / 10
                result = mFormatUnder100.format(priceResult)
            }
            price < 1000 -> {
                priceResult = Math.floor(price)
                result = mFormatUnder1_000.format(priceResult)
            }
            price < 10000 -> {
                // 5
                val extra = (Math.round(price % 10 * 2 / 10) * 5).toDouble()
                priceResult = Math.floor(price / 10) * 10 + extra
                result = mFormatUnder10_000.format(priceResult)
            }
            price < 100000 -> {
                // 10
                val extra = (Math.round(price % 100 / 100) * 100).toDouble()
                priceResult = Math.floor(price / 100) * 100 + extra
                result = mFormatUnder100_000.format(priceResult)
            }
            price < 1000000 -> {
                // 50, 100
                var extra = 0.0
                extra = if (price < 500000) {
                    (Math.round(price % 100 * 2 / 100) * 50).toDouble()
                } else {
                    (Math.round(price % 100 / 100) * 100).toDouble()
                }
                priceResult = Math.floor(price / 100) * 100 + extra
                result = mFormatUnder1_000_000.format(priceResult)
            }
            price < 10000000 -> {
                // 1000
                val extra = (Math.round(price % 1000 / 1000) * 1000).toDouble()
                priceResult = Math.floor(price / 1000) * 1000 + extra
                result = mFormatUnder10_000_000.format(priceResult)
            }
            price < 100000000 -> {
                // 1000
                val extra = (Math.round(price % 1000 / 1000) * 1000).toDouble()
                priceResult = Math.floor(price / 1000) * 1000 + extra
                result = mFormatUnder100_000_000.format(priceResult)
            }
        }
        return result as Double
    }
}