package com.advancedcomponentservices.acswarehouse.db.models

import java.math.BigDecimal

data class Item(
    val item: String,
    val purchaseDescription: String?,
    val mpn: String?,
    val type: String,
    val cost: BigDecimal?,
    val price: BigDecimal?,
    val quantityOnHand: BigDecimal?,
    val quantityOnSalesOrder: Int?,
    val quantityOnPurchaseOrder: Int?,
    val preferredVendor: String?,
    val costValue: BigDecimal,
    val onHandValue: BigDecimal,
    val salesOrderValue: Int,
    val purchaseOrderValue: Int,
    val priceValue: BigDecimal,
    val isKit: Boolean
)
