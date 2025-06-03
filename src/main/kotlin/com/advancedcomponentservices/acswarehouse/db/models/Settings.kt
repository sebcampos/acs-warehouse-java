package com.advancedcomponentservices.acswarehouse.db.models


data class Settings(
    val spreadSheetName: String,
    val openOrderStatusReportName: String,
    val crossIndexName: String,
    val pickListLogName: String,
    val itemsName: String,
    val serviceAccount: HashMap<String, String>,
    val pdfDirectory: String,
    val shippingStationDirectory: String
)

