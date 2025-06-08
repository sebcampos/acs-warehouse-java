package com.advancedcomponentservices.acswarehouse

import com.advancedcomponentservices.acswarehouse.db.connectToDatabase
import com.advancedcomponentservices.acswarehouse.db.deleteRelatedPartialsFromQueue
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateBPItems
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateItems
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateOrdersInQueue
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateShippingStationEntries
import com.advancedcomponentservices.acswarehouse.db.insertOrUpdateSkuCrossItems
import com.advancedcomponentservices.acswarehouse.db.models.Order
import com.advancedcomponentservices.acswarehouse.db.models.ShippingStationEntry
import com.advancedcomponentservices.acswarehouse.etl.buildQueue
import com.advancedcomponentservices.acswarehouse.etl.buildShippingStationList
import com.advancedcomponentservices.acswarehouse.etl.generatePickList
import com.advancedcomponentservices.acswarehouse.etl.parseBPItems
import com.advancedcomponentservices.acswarehouse.etl.parseItems
import com.advancedcomponentservices.acswarehouse.etl.parseOpenOrderStatus
import com.advancedcomponentservices.acswarehouse.etl.parseSkuCrossIndex
import com.advancedcomponentservices.acswarehouse.google.Client as GoogleClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.cdimascio.dotenv.dotenv
import java.lang.reflect.Type
import java.sql.Connection
import java.time.LocalDate
import java.util.Base64
import kotlin.text.startsWith

class Manager {

    private val connection: Connection = connectToDatabase()!!
    private lateinit var gc: GoogleClient

    fun refreshDatabase() {
        val dotenv = dotenv()
        val encoded = dotenv["GOOGLE_SERVICE_ACCOUNT"] ?: error("GOOGLE_SERVICE_ACCOUNT not found")
        val json = String(Base64.getDecoder().decode(encoded))

        val gson = Gson()
        val mapType: Type? = object : TypeToken<HashMap<String, String>>() {}.type
        val serviceAccount: HashMap<String, String> = gson.fromJson(json, mapType)
        gc =  GoogleClient(serviceAccount)
        val commercialSheetName = "Commercial Order Process v2"
        val commercialSheetsList = setOf("Open Order Status Report", "SKU Cross Index", "Items")
        val commercialV2 = gc.getSpreadSheet(commercialSheetName)!!
        val commercialResponse = gc.readSpreadSheets(commercialV2.id, commercialSheetsList)

        val warehouseSheet = gc.getSpreadSheet("Bin and Warehouse Locations")!!
        val bpItemSheet = gc.readSpreadSheet(warehouseSheet.id, "Warehouse BP Item Loc")

        val itemsRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'Items") || it.range.startsWith("Items")}
        val skuIndexRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'SKU Cross Index") || it.range.startsWith("SKU Cross Index")}
        val openOrdersSheetRange = commercialResponse.valueRanges.firstOrNull {it.range.startsWith("'Open Order Status Report") || it.range.startsWith("'SOpen Order Status Report")}

        val openOrders = parseOpenOrderStatus(openOrdersSheetRange!!.values)
        val skuIndex = parseSkuCrossIndex(skuIndexRange!!.values)
        val items = parseItems(itemsRange!!.values)
        val bpItems = parseBPItems(bpItemSheet.values)

        val queueOrders = buildQueue(openOrders, items, skuIndex, bpItems)
        deleteRelatedPartialsFromQueue(queueOrders, connection)
        insertOrUpdateOrdersInQueue(queueOrders, connection)
        insertOrUpdateSkuCrossItems(skuIndex, connection)
        insertOrUpdateItems(items, connection)
        insertOrUpdateBPItems(bpItems, connection)
    }


    fun getTodaysShippingStationEntries(connection: Connection): List<ShippingStationEntry> {
        val results = mutableListOf<ShippingStationEntry>()
        val query = "SELECT * FROM shippingStationLog WHERE dueDate = '${LocalDate.now()}'"
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(query)
        while (resultSet.next()) {
            results.add(
                ShippingStationEntry(
                    lineItemId = resultSet.getString("lineItemId"),
                    date = LocalDate.parse(resultSet.getString("date")),
                    dueDate = LocalDate.parse(resultSet.getString("dueDate")),
                    orderCt = resultSet.getString("orderCt"),
                    po = resultSet.getString("po"),
                    terms = resultSet.getString("terms"),
                    customerName = resultSet.getString("customerName"),
                    customerZip = resultSet.getString("customerZip"),
                    customerEmail = resultSet.getString("customerEmail"),
                    customerPhone = resultSet.getString("customerPhone"),
                    customerNote = resultSet.getString("customerNote"),
                    via = resultSet.getString("via"),
                    shipAccount =  resultSet.getString("shipAccount"),
                    orderedQuantity =  resultSet.getInt("orderedQuantity"),
                    shipQuantity = resultSet.getInt("shipQuantity"),
                    item = resultSet.getString("item"),
                    itemDescription = resultSet.getString("itemDescription"),
                    shipToCity =  resultSet.getString("shipToCity"),
                    shipToAddress = resultSet.getString("shipToAddress"),
                    shipToAddress2 =   resultSet.getString("shipToAddress2"),
                    shipToState =  resultSet.getString("shipToState"),
                    shipToZip = resultSet.getString("shipToZip"),
                    remainingStock =  resultSet.getInt("remainingStock"),
                    toBuyerNote =   resultSet.getString("toBuyerNote"),
                )
            )
        }
        return results
    }


    fun getSendToWarehouse(): List<Order> {
        val orders = mutableListOf<Order>()
        val query = "SELECT * FROM orders where sendToWarehouse = true and invoiced = false"
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(query)
        val shipDate = if (resultSet.getString("shipDate") != null) {
            LocalDate.parse(resultSet.getString("shipDate"))
        } else {
            null
        }
        val expectedDate = if (resultSet.getString("expectedDate") != null) {
            LocalDate.parse(resultSet.getString("expectedDate"))
        }
        else {
            null
        }
        val reviewDate = if (resultSet.getString("reviewDate") != null) {
            LocalDate.parse(resultSet.getString("reviewDate"))
        }
        else {
            null
        }
        while (resultSet.next()) {
            orders.add(
                Order(
                    lineItemId = resultSet.getString("lineItemId"),
                    id = resultSet.getString("id"),
                    po =  resultSet.getString("po"),
                    date = LocalDate.parse(resultSet.getString("date")),
                    nameZip = resultSet.getString("nameZip"),
                    namePhone =   resultSet.getString("namePhone"),
                    nameEmail =   resultSet.getString("nameEmail"),
                    shipDate = shipDate,
                    via = resultSet.getString("via"),
                    terms = resultSet.getString("terms"),
                    num = resultSet.getInt("num"),
                    name = resultSet.getString("name"),
                    qty = resultSet.getInt("qty"),
                    upsAccount = resultSet.getString("upsAccount"),
                    item = resultSet.getString("item"),
                    itemDescription = resultSet.getString("itemDescription"),
                    backOrdered  = resultSet.getInt("backOrdered"),
                    shipToCity =  resultSet.getString("shipToCity"),
                    shipToAddress = resultSet.getString("shipToAddress"),
                    shipToAddress2 =   resultSet.getString("shipToAddress2"),
                    shipToState =  resultSet.getString("shipToState"),
                    shipToZip =   resultSet.getString("shipToZip"),
                    binLocation = resultSet.getString("binLocation"),
                    shipToDateIs7DaysFromToday = resultSet.getBoolean("shipToDateIs7DaysFromToday"),
                    onHand = resultSet.getInt("onHand"),
                    onPurchaseOrder =  resultSet.getInt("onPurchaseOrder"),
                    purchaseOrder =  resultSet.getInt("purchaseOrder"),
                    matchedPo =  resultSet.getInt("matchedPo"),
                    expectedDate = expectedDate,
                    noteCode =   resultSet.getString("noteCode"),
                    reviewDate = reviewDate,
                    note = resultSet.getString("note"),
                    sendToWarehouse =   resultSet.getBoolean("sendToWarehouse"),
                    partial = resultSet.getBoolean("partial"),
                    hasException = resultSet.getBoolean("hasException"),
                    orderCt = resultSet.getString("orderCt"),
                    matchDataForLabelCheck =  resultSet.getString("matchDataForLabelCheck"),
                    baseSku =  resultSet.getString("baseSku"),
                    packQty =  resultSet.getInt("packQty"),
                    requiredQty =   resultSet.getInt("requiredQty"),
                    invoiced = resultSet.getBoolean("invoiced"),
                    isKit = resultSet.getBoolean("isKit"),
                    shippedQty =  resultSet.getInt("shippedQty"),
                    bulkBinLocation =  resultSet.getString("bulkBinLocation")
                )
            )
        }
        return orders
    }

    fun createPickList() {
        val dotenv = dotenv()
        val outputPath = dotenv["OUTPUT_PATH"] ?: error("OUTPUT_PATH not found")
        val sendToWarehouse = getSendToWarehouse()
        if (!sendToWarehouse.isEmpty())
        {
            // create the pdfs
            generatePickList(outputPath, sendToWarehouse)


            // creating shipping station entries and add to database
            val shippingStationList = buildShippingStationList(sendToWarehouse)
            insertOrUpdateShippingStationEntries(shippingStationList, connection)

            // mark all as invoiced then update in database
            sendToWarehouse.forEach { it.invoiced = true }
            insertOrUpdateOrdersInQueue(sendToWarehouse, connection)
        }
        val todaysEntries = getTodaysShippingStationEntries(connection)
        println(todaysEntries)

        // TODO create log df by merging shipping station list with sent to warehouse
        //    then upload to google sheet


    }
}