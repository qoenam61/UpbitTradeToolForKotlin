package com.example.upbittrade.model

import com.google.gson.annotations.SerializedName

class ResponseOrder {
    @SerializedName("uuid")
    var uuid: String? = null

    @SerializedName("side")
    var side: String? = null

    @SerializedName("ord_type")
    var orderType: String? = null

    @SerializedName("price")
    var price: Number? = null

    @SerializedName("avg_price")
    var avgPrice: Number? = null

    @SerializedName("state")
    var state: String? = null

    @SerializedName("market")
    var marketId: String? = null

    @SerializedName("created_at")
    var created_at: String? = null

    @SerializedName("volume")
    var volume: Number? = null

    @SerializedName("remaining_volume")
    var remainingVolume: Number? = null

    @SerializedName("reserved_fee")
    var reservedFee: Number? = null

    @SerializedName("remaining_fee")
    var remainingFee: Number? = null

    @SerializedName("paid_fee")
    var paid_fee: Number? = null

    @SerializedName("locked")
    var locked: Number? = null

    @SerializedName("executed_volume")
    var executedVolume: Number? = null

    @SerializedName("trades_count")
    var tradesCount: Int? = null


    override fun toString(): String {
        val result = StringBuilder()
        val newLine = System.getProperty("line.separator")
        result.append(this.javaClass.name)
        result.append("{")
//        result.append(newLine)
        // 클래스의 필드 리스트를 가져옴
        val fields = this.javaClass.declaredFields
        // 필드 리스트에서 필드를 하나하나 꺼내옴
        for (field in fields) {
            result.append(" ")
            try {
                result.append(field.name)
                result.append(": ")
                result.append(field[this])
            } catch (ex: IllegalAccessException) {
                println(ex)
            }
//            result.append(newLine)
        }
        result.append("}")
        return result.toString()
    }

}