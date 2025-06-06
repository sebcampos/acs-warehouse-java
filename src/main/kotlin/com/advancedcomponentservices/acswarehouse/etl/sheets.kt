package com.advancedcomponentservices.acswarehouse.etl
import com.advancedcomponentservices.acswarehouse.db.models.BPItem
import com.advancedcomponentservices.acswarehouse.db.models.SkuCrossIndex
import com.advancedcomponentservices.acswarehouse.db.models.Item
import com.advancedcomponentservices.acswarehouse.etl.models.OpenOrderStatusReportOrder




fun parseOpenOrderStatus(rows: ArrayList<ArrayList<String>>): ArrayList<OpenOrderStatusReportOrder> {
    val openOrdersList = ArrayList<OpenOrderStatusReportOrder>()
    val columnNames = rows[0]
    val columnMap = HashMap<String, Int>()
    for (i in columnNames.indices) {
        columnMap[columnNames[i]] = i
    }
    val numCounters = mutableMapOf<Int, Int>()
    for (row in rows.subList(1, rows.size)) {
        // Google sheets api does not include values of cells at the end if they are blank
        while (row.size < columnNames.size) {
            row.add("")
        }

        // skip any values that do not have Num column
        if (row[columnMap["Num"]!!].trim() == "")
        {
            break
        }

        val noteCode = inputStringOrNull(row[columnMap["Note Code"]!!])

        // dates
        val date = inputDateOrNull(row[columnMap["Date"]!!])
        val shipDate =  inputDateOrNull(row[columnMap["Ship Date"]!!])
        val expectedDate = inputDateOrNull(row[columnMap["Expected Date"]!!])
        val reviewDate = inputDateOrNull(row[columnMap["Review Date"]!!])


        val shipToDateIs7DaysFromTodayVal = row[columnMap["Ship date is 7 days from today"]!!]
        val shipToDateIs7DaysFromToday = shipToDateIs7DaysFromTodayVal.trim() == "YES"

        val num = row[columnMap["Num"]!!].toInt()
        val qty = inputIntOrNull(row[columnMap["Qty"]!!])
        val backOrdered = inputIntOrNull(row[columnMap["Backordered"]!!])
        val onHand = row[columnMap["On-Hand"]!!].toInt()
        val onPurchaseOrder = inputIntOrNull(row[columnMap["On-Purchase Order"]!!])
        val purchaseOrder = inputIntOrNull(row[columnMap["Purchase Order"]!!])
        val matchedPo = inputIntOrNull(row[columnMap["Matched PO"]!!])
        val poNumber = row[columnMap["P. O. #"]!!]


        val nameZip = row[columnMap["Name Zip"]!!]
        val namePhone = row[columnMap["Name Phone #"]!!]
        val nameEmail = row[columnMap["Name E-Mail"]!!]
        val via = row[columnMap["Via"]!!]
        val terms = row[columnMap["Terms"]!!]
        val name = row[columnMap["Name"]!!]
        val upsAccount = row[columnMap["UPS Account"]!!]
        val itemDescription = row[columnMap["Item Description"]!!]
        val shipToCity = row[columnMap["Ship To City"]!!]
        val shipToAddress = row[columnMap["Ship To Address 1"]!!]
        val shipToAddress2 = row[columnMap["Ship To Address 2"]!!]
        val shipToState = row[columnMap["Ship To State"]!!]
        val shipToZip = row[columnMap["Ship Zip"]!!]
        val binLocation = inputStringOrNull(row[columnMap["Bin Location"]!!])
        var note = inputStringOrNull(row[columnMap["Note"]!!])

        val hasException = orderHasException(name, note, shipToDateIs7DaysFromToday)
        if (hasException && name.trim() == "Hexagon Parts") {
            note = "'Hexagon Parts Exception"
        }
        else if (hasException && shipToDateIs7DaysFromToday) {
            note = "Order Ship Date is more than 7 days from now"
        }
        val sendToWarehouse = !hasException

        val item = row[columnMap["Item"]!!]
        val count = numCounters.getOrDefault(num, 0) + 1
        numCounters[num] = count
        val id = "$num-$item"
        val lineItemId = "$num-$item-$count"
        val order = OpenOrderStatusReportOrder(
            lineItemId = lineItemId,
            id = id,
            po = poNumber,
            date = date!!,
            nameZip = nameZip,
            namePhone = namePhone,
            nameEmail = nameEmail,
            shipDate = shipDate,
            via = via,
            terms = terms,
            num = num,
            name = name,
            qty = qty,
            upsAccount = upsAccount,
            item = item,
            itemDescription = itemDescription,
            backOrdered = backOrdered!!,
            shipToCity = shipToCity,
            shipToAddress = shipToAddress,
            shipToAddress2 = shipToAddress2,
            shipToState = shipToState,
            shipToZip = shipToZip,
            binLocation = binLocation,
            shipToDateIs7DaysFromToday = shipToDateIs7DaysFromToday,
            onHand = onHand,
            onPurchaseOrder = onPurchaseOrder,
            purchaseOrder = purchaseOrder,
            matchedPo = matchedPo,
            expectedDate = expectedDate,
            noteCode = noteCode,
            reviewDate = reviewDate,
            note = note,
            hasException = hasException,
            sendToWarehouse = sendToWarehouse
        )
        openOrdersList.add(order)
    }
    return openOrdersList
}

fun parseSkuCrossIndex(rows: ArrayList<ArrayList<String>>): ArrayList<SkuCrossIndex> {
    val skuCrossItems = ArrayList<SkuCrossIndex>()
    val columnNames = rows[1]
    val columnMap = HashMap<String, Int>()
    for (i in columnNames.indices) {
        columnMap[columnNames[i]] = i
    }
    for (row in rows.subList(2, rows.size)) {
        // Google sheets api does not include values of cells at the end if they are blank
        while (row.size < columnNames.size) {
            row.add("")
        }

        val amzOcsPartsSku = inputStringOrNull(row[columnMap["Amz (OCSParts) SKU"]!!])
        if (amzOcsPartsSku == null) {
            break
        }
        val qbSku = inputStringOrNull(row[columnMap["QB SKU"]!!])
        val packSize = inputIntOrNull(row[columnMap["Pack Size"]!!])

        val item = SkuCrossIndex(
            amzOcsPartsSku = amzOcsPartsSku,
            qbSku = qbSku,
            packSize = packSize
        )
        skuCrossItems.add(item)

    }
    return skuCrossItems
}

fun parseItems(rows: ArrayList<ArrayList<String>>): ArrayList<Item> {
    val items = ArrayList<Item>()
    val columnNames = rows[1]
    val columnMap = HashMap<String, Int>()
    for (i in columnNames.indices) {
        columnMap[columnNames[i]] = i
    }
    for (row in rows.subList(2, rows.size)) {
        // Google sheets api does not include values of cells at the end if they are blank
        while (row.size < columnNames.size) {
            row.add("")
        }

        // item is required primary key
        val itemId = inputStringOrNull(row[columnMap["Item"]!!])
        if (itemId == null) {
            break
        }

        val purchaseDescription = inputStringOrNull(row[columnMap["Purchase Description"]!!])
        val mpn = inputStringOrNull(row[columnMap["MPN"]!!])
        val type = row[columnMap["Type"]!!]
        val cost = inputBigDecimalOrNull(row[columnMap["Cost"]!!])
        val price = inputBigDecimalOrNull(row[columnMap["Price"]!!])
        val quantityOnHand = inputBigDecimalOrNull(row[columnMap["Quantity On Hand"]!!])
        val quantityOnSalesOrder = inputIntOrNull(row[columnMap["Quantity On Sales Order"]!!])
        val quantityOnPurchaseOrder = inputIntOrNull(row[columnMap["Quantity On Purchase Order"]!!])
        val preferredVendor = inputStringOrNull(row[columnMap["Preferred Vendor"]!!])
        val costValue = row[columnMap["Cost (Value)"]!!].toBigDecimal()
        val onHandValue = row[columnMap["On Hand (Value)"]!!].toBigDecimal()
        val salesOrderValue = row[columnMap["Sales Order (Value)"]!!].toInt()
        val purchaseOrderValue = row[columnMap["Purchase Order (Value)"]!!].toInt()
        val priceValue = row[columnMap["Price (Value)"]!!].toBigDecimal()
        val isKit = type.trim().lowercase() == "inventory assembly"

        val item = Item(
            item=itemId,
            purchaseDescription=purchaseDescription,
            mpn=mpn,
            type=type,
            cost=cost,
            price=price,
            quantityOnHand=quantityOnHand,
            quantityOnSalesOrder=quantityOnSalesOrder,
            quantityOnPurchaseOrder=quantityOnPurchaseOrder,
            preferredVendor=preferredVendor,
            costValue=costValue,
            onHandValue=onHandValue,
            salesOrderValue=salesOrderValue,
            purchaseOrderValue=purchaseOrderValue,
            priceValue=priceValue,
            isKit=isKit
        )
        items.add(item)
    }
    return items
}

fun parseBPItems(rows: ArrayList<ArrayList<String>>): ArrayList<BPItem> {
    val bpItems = ArrayList<BPItem>()
    val columnNames = rows[0]
    columnNames[0] = "Item"
    val columnMap = HashMap<String, Int>()
    for (i in columnNames.indices) {
        columnMap[columnNames[i]] = i
    }
    for (row in rows.subList(1, rows.size)) {
        // Google sheets api does not include values of cells at the end if they are blank
        while (row.size < columnNames.size) {
            row.add("")
        }
        val itemId = inputStringOrNull(row[columnMap["Item"]!!])
        if (itemId == null) {
            break
        }

        val description = inputStringOrNull(row[columnMap["Description"]!!])
        val bulkBinLocation = inputStringOrNull(row[columnMap["Location"]!!])
        val palletNumbers = inputStringOrNull(row[columnMap["Pallet Numbers"]!!])
        val palletQuantity = inputStringOrNull(row[columnMap["Pallet Qty"]!!])

        val item = BPItem(
            item = itemId,
            description = description,
            bulkBinLocation = bulkBinLocation,
            palletNumbers = palletNumbers,
            palletQuantity = palletQuantity
        )
        bpItems.add(item)
    }
    return bpItems
}