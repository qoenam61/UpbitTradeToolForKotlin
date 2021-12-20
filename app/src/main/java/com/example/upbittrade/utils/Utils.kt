package com.example.upbittrade.utils

import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.roundToInt

class Utils {
    fun convertPrice(price: Double): Double? {
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
                priceResult = floor(price * 100) / 100
                result = mFormatUnder10.format(priceResult)
            }
            price < 100 -> {
                priceResult = floor(price * 10) / 10
                result = mFormatUnder100.format(priceResult)
            }
            price < 1000 -> {
                priceResult = floor(price)
                result = mFormatUnder1_000.format(priceResult)
            }
            price < 10000 -> {
                // 5
                val extra = ((price % 10 * 2 / 10).roundToInt() * 5).toDouble()
                priceResult = floor(price / 10) * 10 + extra
                result = mFormatUnder10_000.format(priceResult)
            }
            price < 100000 -> {
                // 10
                val extra = ((price % 100 / 100).roundToInt() * 100).toDouble()
                priceResult = floor(price / 100) * 100 + extra
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

                priceResult = floor(price / 100) * 100 + extra
                result = mFormatUnder1_000_000.format(priceResult)
            }
            price < 10000000 -> {
                // 1000
                val extra = ((price % 1000 / 1000).roundToInt() * 1000).toDouble()
                priceResult = floor(price / 1000) * 1000 + extra
                result = mFormatUnder10_000_000.format(priceResult)
            }
            price < 100000000 -> {
                // 1000
                val extra = ((price % 1000 / 1000).roundToInt() * 1000).toDouble()
                priceResult = floor(price / 1000) * 1000 + extra
                result = mFormatUnder100_000_000.format(priceResult)
            }
        }
        return result?.toDouble()
    }

}