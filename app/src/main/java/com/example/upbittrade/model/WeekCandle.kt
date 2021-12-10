package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class WeekCandle: Candle() {
    @SerializedName("first_day_of_period")
    var firstDayOfPeriod: String? = null
}