package com.example.upbittrade.data

import com.example.upbittrade.activity.TradePagerActivity.PostType
import java.io.Serializable
import java.util.*

open class TaskItem: Serializable {
    var type: PostType
    var marketId: String? = null
    var uuid: UUID? = null

    constructor(type: PostType) {
        this.type = type
    }

    constructor(type: PostType, marketId: String?) {
        this.type = type
        this.marketId = marketId
    }

    constructor(type: PostType, marketId: String?, uuid: UUID) {
        this.type = type
        this.marketId = marketId
        this.uuid = uuid
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

open class PostOrderItem: TaskItem {
    var side: String? = null
    var volume: Double? = null
    var price: Double? = null
    var ordType: String? = null
    var identifier: UUID? = null

    constructor(
        type: PostType,
        marketId: String?,
        side: String?,
        volume: Double?,
        price: Double?,
        ordType: String?,
        identifier: UUID?
    ) : super(type, marketId) {
        this.side = side
        this.volume = volume
        this.price = price
        this.ordType = ordType
        this.identifier = identifier
    }
}