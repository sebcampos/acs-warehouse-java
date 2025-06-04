package com.advancedcomponentservices.acswarehouse.google.models

data class ReadSheetResponse(val range: String, val majorDimension: String, val values: ArrayList<ArrayList<String>>)
