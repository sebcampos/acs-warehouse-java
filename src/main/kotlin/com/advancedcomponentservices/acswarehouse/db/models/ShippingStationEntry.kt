package com.advancedcomponentservices.acswarehouse.db.models

import java.time.LocalDate

data class ShippingStationEntry(
    val lineItemId: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val orderCt: String,
    val po: String,
    val terms: String,
    val customerName: String,
    val customerZip: String,
    val customerEmail: String,
    val customerPhone: String,
    val customerNote: String,
    val via: String,
    val shipAccount: String,
    val orderedQuantity: Int,
    val shipQuantity: Int,
    val item: String,
    val itemDescription: String,
    val shipToCity: String,
    val shipToAddress: String,
    val shipToAddress2: String?,
    val shipToState: String,
    val shipToZip: String,
    val remainingStock: Int? = null,
    val toBuyerNote: String
)
