package com.advancedcomponentservices.acswarehouse.etl

import com.advancedcomponentservices.acswarehouse.db.models.Orders

fun parseOpenOrderStatus(values: ArrayList<ArrayList<String>>): ArrayList<Orders> {
    val ordersList = ArrayList<Orders>()
    val columnNames = values[0]
    val columnMap = HashMap<String, Int>()
    for (i in columnNames.indices) {
        columnMap[columnNames[i]] = i
    }
    for (value in values.subList(1, values.size)) {
        println(value)
    }
    return ordersList
}