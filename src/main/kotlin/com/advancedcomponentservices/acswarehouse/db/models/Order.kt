package com.advancedcomponentservices.acswarehouse.db.models

import java.time.LocalDate


data class Order(
    val lineItemId: String,
    val id: String,
    val po: String,
    val date: LocalDate,
    val nameZip: String,
    val namePhone: String,
    val nameEmail: String,
    val shipDate: LocalDate?,
    val via: String,
    val terms: String,
    val num: Int,
    val name: String,
    val qty: Int?,
    val upsAccount: String,
    val item: String,
    val itemDescription: String,
    val backOrdered: Int,
    val shipToCity: String,
    val shipToAddress: String,
    val shipToAddress2: String?,
    val shipToState: String,
    val shipToZip: String,
    val binLocation: String?,
    val shipToDateIs7DaysFromToday: Boolean,
    val onHand: Int,
    val onPurchaseOrder: Int?,
    val purchaseOrder: Int?,
    val matchedPo: Int?,
    val expectedDate: LocalDate?,
    val noteCode: String?,
    val reviewDate: LocalDate?,
    val note: String?,
    val sendToWarehouse: Boolean,
    val partial: Boolean,
    val hasException: Boolean,
    val orderCt: String,
    val matchDataForLabelCheck: String,
    val baseSku: String?,
    val packQty: Int,
    val requiredQty: Int,
    var invoiced: Boolean,
    val isKit: Boolean,
    val shippedQty: Int,
    val bulkBinLocation: String?
)
