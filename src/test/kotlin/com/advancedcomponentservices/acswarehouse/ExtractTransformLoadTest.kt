package com.advancedcomponentservices.acswarehouse

import com.advancedcomponentservices.acswarehouse.db.connectToDatabase
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateBPItems
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateItems
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateOrdersInQueue
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateSkuCrossItems
import com.advancedcomponentservices.acswarehouse.etl.buildQueue
import com.advancedcomponentservices.acswarehouse.etl.parseItems
import com.advancedcomponentservices.acswarehouse.etl.parseOpenOrderStatus
import com.advancedcomponentservices.acswarehouse.etl.parseSkuCrossIndex
import com.advancedcomponentservices.acswarehouse.etl.parseBPItems
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.cdimascio.dotenv.dotenv
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.util.Base64


import com.advancedcomponentservices.acswarehouse.google.Client as GoogleClient


class ExtractTransformLoadTest {
    private val dotenv = dotenv()
    private val encoded = dotenv["GOOGLE_SERVICE_ACCOUNT"] ?: error("GOOGLE_SERVICE_ACCOUNT not found")
    private val json = String(Base64.getDecoder().decode(encoded))
    private val gson = Gson()
    private val mapType: Type? = object : TypeToken<HashMap<String, String>>() {}.type
    private val serviceAccount: HashMap<String, String> = gson.fromJson(json, mapType)

    val gc =  GoogleClient(serviceAccount)
    val commercialSheetName = "Commercial Order Process v2"
    val commercialSheetsList = setOf("Open Order Status Report", "SKU Cross Index", "Items")
    val commercialV2 = gc.getSpreadSheet(commercialSheetName)!!
    val commercialResponse = gc.readSpreadSheets(commercialV2.id, commercialSheetsList)

    val warehouseSheet = gc.getSpreadSheet("Bin and Warehouse Locations")!!
    val bpItemSheet = gc.readSpreadSheet(warehouseSheet.id, "Warehouse BP Item Loc")

    @Test
    fun `test Parse Orders Sent to Warehouse` ()
    {
        // sheet response range sometimes start with " ' "
        val openOrdersSheetRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'Open Order Status Report") || it.range.startsWith("'SOpen Order Status Report")}
        val openOrders = parseOpenOrderStatus(openOrdersSheetRange!!.values)
        println(openOrders);
    }

    @Test
    fun `test Parse Sku Cross Index` ()
    {
        val skuIndexRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'SKU Cross Index") || it.range.startsWith("SKU Cross Index")}
        val skuIndex = parseSkuCrossIndex(skuIndexRange!!.values)
        println(skuIndex)
    }

    @Test
    fun `test Parse Items Sheet` ()
    {
        val itemsRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'Items") || it.range.startsWith("Items")}
        val items = parseItems(itemsRange!!.values)
        println(items)
    }

    @Test
    fun `test Parse Warehouse BP Item` ()
    {
        val bpItems = parseBPItems(bpItemSheet.values)
        println(bpItems)
    }

}