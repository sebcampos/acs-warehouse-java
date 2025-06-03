package com.advancedcomponentservices.acswarehouse.db.models

data class Items(
    val item: String,
    val purchaseDescription: String,
    val mpn: String,
    val type: String,
    val cost: Number,
    val price: Number,
    val quantityOnHand: Int,
    val quantityOnSalesOrder: Int,
    val quantityOnPurchaseOrder: Int,
    val preferredVendor: String,
    val costValue: Number,
    val onHandValue: Number,
    val salesOrderValue: Number,
    val purchaseOrderValue: Number,
    val priceValue: Number,
    val isKit: Boolean
)
