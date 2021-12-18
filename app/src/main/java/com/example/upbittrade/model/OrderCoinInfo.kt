package com.example.upbittrade.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OrderCoinInfo: TradeCoinInfo() {
    enum class Status {
        READY,
        WAIT,
        BUY,
        SELL
    }

    // type0 : HHCO
    private var type0 =
        (highPrice!!.toDouble().pow(2.0) + highPrice!!.toDouble().pow(2.0)
                + closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)) / 4
    private var priceTypeHHCO: Double? = convertPrice(sqrt(type0))

    var status: Status = Status.READY

    val askPrice: Double? = null

    fun getBidPrice(): Double? {
        val highTail: Double = (highPrice!!.toDouble() - closePrice!!.toDouble()
            .coerceAtLeast(openPrice!!.toDouble()))

        val lowTail: Double = (openPrice!!.toDouble().coerceAtMost(closePrice!!.toDouble()) - lowPrice!!.toDouble())

        val body: Double = abs(closePrice!!.toDouble() - openPrice!!.toDouble())

        val length: Double = highTail + lowTail + body

        return when {
            body / length == 1.0 -> {
                // type0 : HHCO
                convertPrice(sqrt((highPrice!!.toDouble().pow(2.0) + highPrice!!.toDouble().pow(2.0)
                        + closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)) / 4))
            }

            body / length == 0.9 -> {
                // type1 : HCO
                convertPrice(
                    sqrt((highPrice!!.toDouble().pow(2.0) + closePrice!!.toDouble().pow(2.0)
                        + openPrice!!.toDouble().pow(2.0)) / 3)
                )
            }

            body / length == 0.8 -> {
                // type2 : HCOL
                convertPrice(sqrt((highPrice!!.toDouble().pow(2.0) + closePrice!!.toDouble().pow(2.0)
                        + openPrice!!.toDouble().pow(2.0) + lowPrice!!.toDouble().pow(2.0)) / 4))
            }

            (body + lowTail) / length == 1.0 -> {
                // type3 : COL
                convertPrice(sqrt((closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)
                        + lowPrice!!.toDouble().pow(2.0)) / 3))
            }

            (body + lowTail) / length > 0.8 -> {
                // type4 : COLL
                convertPrice(sqrt((closePrice!!.toDouble().pow(2.0) + openPrice!!.toDouble().pow(2.0)
                        + lowPrice!!.toDouble().pow(2.0) + lowPrice!!.toDouble().pow(2.0)) / 4))
            }

            else -> null
        }
    }


}