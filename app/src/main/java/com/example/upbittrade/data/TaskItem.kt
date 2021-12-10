package com.example.upbittrade.data

import com.example.upbittrade.activity.TradePagerActivity.PostType
import java.io.Serializable

open class TaskItem: Serializable {
    var type: PostType
    var marketId: String? = null

    constructor(type: PostType) {
        this.type = type
    }

    constructor(type: PostType, marketId: String?) {
        this.type = type
        this.marketId = marketId
    }
}

open class CandleItem: TaskItem {
    var to: String? = null
    var count = 0

    constructor(type: PostType, marketId: String?, count: Int) : super(type, marketId) {
        this.count = count
    }

    constructor(type: PostType, marketId: String?, to: String?, count: Int) : super(type, marketId) {
        this.to = to
        this.count = count
    }
}

open class ExtendCandleItem: CandleItem {
    var unit: String? = null
    var convertingPriceUnit: String? = null

    constructor(type: PostType, marketId: String?, count: Int) : super(type, marketId, count)

    constructor(type: PostType, unit: String, marketId: String?, count: Int) : super(type, marketId, count) {
        this.unit = unit
    }

    constructor(
        type: PostType,
        marketId: String?,
        count: Int,
        convertingPriceUnit: String?
    ) : super(type, marketId, count){
        this.convertingPriceUnit = convertingPriceUnit
    }

    constructor(
        type: PostType,
        marketId: String?,
        to: String?,
        count: Int,
        convertingPriceUnit: String?
    ) : super(type, marketId, to, count){
        this.convertingPriceUnit = convertingPriceUnit
    }
}