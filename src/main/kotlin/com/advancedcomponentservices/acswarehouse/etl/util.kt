package com.advancedcomponentservices.acswarehouse.etl

import com.advancedcomponentservices.acswarehouse.db.models.BPItem
import com.advancedcomponentservices.acswarehouse.db.models.Item
import com.advancedcomponentservices.acswarehouse.db.models.Order
import com.advancedcomponentservices.acswarehouse.db.models.SkuCrossIndex
import com.advancedcomponentservices.acswarehouse.etl.models.OpenOrderStatusReportOrder
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy")

fun inputStringIsNull(inputString: String): Boolean {
    return inputString.isBlank() || inputString.trim() == "#N/A"
}

fun inputStringOrNull(inputString: String): String? {
    return if (inputStringIsNull(inputString)) {
        null
    }
    else {
        inputString
    }
}

fun inputDateOrNull (inputDate: String): LocalDate?  {
    return if (inputStringIsNull(inputDate)) {
        null
    }
    else {
        LocalDate.parse(inputDate, formatter)
    }
}

fun inputIntOrNull(inputInt: String): Int? {
    val cleanedInput = inputInt.replace(",", "")
    return cleanedInput.toIntOrNull()
}

fun inputBigDecimalOrNull(inputBigDecimal: String): BigDecimal? {
    val cleanedInput = inputBigDecimal.replace(",", "")
    return cleanedInput.toBigDecimalOrNull()
}

fun orderHasException(name: String, note: String?, shipToDateIsSevenDaysFromToday: Boolean): Boolean {
    return name.trim() == "Hexagon Parts" || note != null || shipToDateIsSevenDaysFromToday
}

fun calculateSerialNumberForToday(num: Int): String {
    val origin = LocalDate.of(1899, 12, 30)
    val today = LocalDate.now()
    val serial = ChronoUnit.DAYS.between(origin, today)
    return "$num-$serial"
}


fun buildQueue(
    openOrders: ArrayList<OpenOrderStatusReportOrder>,
    items: ArrayList<Item>,
    skuIndex: ArrayList<SkuCrossIndex>,
    bpItems: ArrayList<BPItem>
): ArrayList<Order> {
    val orders = ArrayList<Order>()

    val inventoryMap = HashMap<String, Int>()
    for (order in openOrders) {
        if (!inventoryMap.containsKey(order.item)) {
            inventoryMap[order.item] = order.onHand
        }
    }

    for (openOrder in openOrders) {
        val matchingItem = items.find { item -> item.item == openOrder.item }
        val matchingSku = skuIndex.find {sku -> sku.amzOcsPartsSku == openOrder.item }
        val matchingBPItem =  bpItems.find { bp -> bp.item == openOrder.item }
        val baseSku = matchingSku?.qbSku
        val packQty: Int = if (matchingSku?.packSize != null) {
            matchingSku.packSize
        } else {
            0
        }
        val requiredQty: Int = if (packQty > 0) {
            openOrder.backOrdered * packQty
        } else {
            openOrder.backOrdered
        }
        val isKit: Boolean  = if (matchingItem?.isKit == null) {
            false
        } else {
            matchingItem.isKit
        }
        val onHand = inventoryMap[openOrder.item]!!
        val orderCt = calculateSerialNumberForToday(openOrder.num)
        val matchDataForLabelCheck = "SO: $orderCt ----- ${openOrder.name} ----- PO: ${openOrder.po}"

        // Determine if order should be sent to warehouse and if order is a partial
        val sendToWarehouse: Boolean
        val partial: Boolean

        // check if order has an exception
        if (openOrder.hasException)
        {
            sendToWarehouse = false
            partial = false
        }

        // check if there is any on-hand for this order
        else if (onHand == 0 && openOrder.backOrdered > 0)
        {
            sendToWarehouse = false
            partial = false
        }

        // if on-hand is greater mark as send to warehouse and decrement on-hand value
        else if (openOrder.backOrdered <= onHand)
        {
            sendToWarehouse = true
            partial = false
            inventoryMap[openOrder.item] = (onHand - openOrder.backOrdered)
        }

        // if on-hand is less than ordered but greater than 0 mark as send to warehouse
        // and as partial. update on-hand value to 0
        else if (onHand > 0)
        {
            sendToWarehouse = true
            partial = true
            inventoryMap[openOrder.item] = 0
        }

        else {
            sendToWarehouse = false
            partial = false
        }


        val order = Order(
            lineItemId =  openOrder.lineItemId,
            id = openOrder.id,
            po =  openOrder.po,
            date = openOrder.date,
            nameZip =  openOrder.nameZip,
            namePhone = openOrder.namePhone,
            nameEmail = openOrder.nameEmail,
            shipDate =  openOrder.shipDate,
            via = openOrder.via,
            terms =  openOrder.terms,
            num =  openOrder.num,
            name = openOrder.name,
            qty =  openOrder.qty,
            upsAccount =  openOrder.upsAccount,
            item = openOrder.item,
            itemDescription = openOrder.itemDescription,
            backOrdered = openOrder.backOrdered,
            shipToCity =  openOrder.shipToCity,
            shipToAddress =  openOrder.shipToAddress,
            shipToAddress2 =   openOrder.shipToAddress2,
            shipToState =  openOrder.shipToState,
            shipToZip =  openOrder.shipToZip,
            binLocation =  openOrder.binLocation,
            shipToDateIs7DaysFromToday =  openOrder.shipToDateIs7DaysFromToday,
            onHand =  onHand,
            onPurchaseOrder =  openOrder.onPurchaseOrder,
            purchaseOrder =  openOrder.purchaseOrder,
            matchedPo = openOrder.matchedPo,
            expectedDate =  openOrder.expectedDate,
            noteCode = openOrder.noteCode,
            reviewDate =  openOrder.reviewDate,
            note =  openOrder.note,
            sendToWarehouse = sendToWarehouse,
            partial = partial,
            hasException = openOrder.hasException,
            orderCt = orderCt,
            matchDataForLabelCheck = matchDataForLabelCheck,
            baseSku = baseSku,
            packQty = packQty,
            requiredQty = requiredQty,
            invoiced = false,  // always false when first created
            isKit = isKit,
            shippedQty = 0,
            bulkBinLocation = matchingBPItem?.bulkBinLocation
        )
        orders.add(order)

    }


    return orders
}