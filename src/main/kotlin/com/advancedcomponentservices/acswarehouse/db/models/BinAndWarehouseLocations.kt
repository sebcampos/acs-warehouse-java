package com.advancedcomponentservices.acswarehouse.db.models

data class BinAndWarehouseLocations(
    val item: String,
    val name: String,
    val bulkBinLocation: String,
    val standardPackMethod: String,
    val baseSkuCt: String,
    val manualBaseSku: String,
    val packQty: Int,
    val onHandQty: Int,
    val qtySold: Int,
    val salesRankQty: Int
)
