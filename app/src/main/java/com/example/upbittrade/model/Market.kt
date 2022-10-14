package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class Market {
    @SerializedName("id")
    private val id: String? = null

    @SerializedName("name")
    private val name: String? = null

    @SerializedName("order_types")
    private val order_types: ArrayList<String>? = null

    @SerializedName("order_sides")
    private val order_sides: ArrayList<String>? = null

    @SerializedName("bid")
    private val bid: Price? = null

    @SerializedName("ask")
    private val ask: Price? = null

    @SerializedName("max_total")
    private val max_total: Number? = null

    @SerializedName("state")
    private val state: String? = null
}