package com.example.upbittrade.model

import kotlin.math.abs

class CoinInfo {
    var marketId: String? = null
    var firstPriceVolume: Double = 0.0
    var secondPriceVolume: Double = 0.0
    var currentPriceVolume: Double = 0.0

    fun getPriceVolumeRate(): Double {
        return (secondPriceVolume - firstPriceVolume) / abs(firstPriceVolume)
    }
}