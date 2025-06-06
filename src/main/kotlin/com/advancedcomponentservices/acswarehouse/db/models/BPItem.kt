package com.advancedcomponentservices.acswarehouse.db.models

data class BPItem(
    val item: String,
    val description: String?,
    val bulkBinLocation: String?,
    val palletNumbers: String?,
    val palletQuantity: String?
)
