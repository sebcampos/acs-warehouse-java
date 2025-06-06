package com.advancedcomponentservices.acswarehouse.db.models

data class SkuCrossIndex(
    val amzOcsPartsSku: String,
    val qbSku: String?,
    val packSize: Int?
)
