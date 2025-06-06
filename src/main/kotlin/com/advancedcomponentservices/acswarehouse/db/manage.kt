package com.advancedcomponentservices.acswarehouse.db

import com.advancedcomponentservices.acswarehouse.db.models.AppState
import com.advancedcomponentservices.acswarehouse.db.models.BPItem
import com.advancedcomponentservices.acswarehouse.db.models.Item
import com.advancedcomponentservices.acswarehouse.db.models.Order
import com.advancedcomponentservices.acswarehouse.db.models.SkuCrossIndex
import java.io.File
import java.sql.DriverManager
import java.sql.Connection
import java.time.LocalDate
import java.time.ZoneId

fun ensureDirectoryExists(path: String) {
    val dir = File(path)
    if (!dir.exists()) {
        dir.mkdirs()  // Creates the directory and any non-existent parent directories
    }
}

fun connectToDatabase(): Connection? {
    val userHome = System.getProperty("user.home")
    ensureDirectoryExists("$userHome${File.separator}.acs-warehouse")
    val dbPath = "$userHome/.acs-warehouse/warehouse.db"
    val url = "jdbc:sqlite:$dbPath"
    return try {
        DriverManager.getConnection(url).also { conn ->
            println("Connected to SQLite DB. $conn")
            createOrdersIfNotExists(conn)
            createSkuCrossIndexIfNotExists(conn)
            createItemsIfNotExists(conn)
            createBPItemsIfNotExists(conn)
        }
    } catch (e: Exception) {
        println("Connection failed: ${e.message}")
        null
    }
}

fun createOrdersIfNotExists(connection: Connection) {
    val sql = """
        CREATE TABLE IF NOT EXISTS orders (
            lineItemId TEXT PRIMARY KEY,
            id TEXT NOT NULL,
            po TEXT NOT NULL,
            date TEXT NOT NULL,
            nameZip TEXT NOT NULL,
            namePhone TEXT NOT NULL,
            nameEmail TEXT NOT NULL,
            shipDate TEXT,
            via TEXT NOT NULL,
            terms TEXT NOT NULL,
            num INTEGER NOT NULL,
            name TEXT NOT NULL,
            qty INTEGER,
            upsAccount TEXT NOT NULL,
            item TEXT NOT NULL,
            itemDescription TEXT NOT NULL,
            backOrdered INTEGER NOT NULL,
            shipToCity TEXT NOT NULL,
            shipToAddress TEXT NOT NULL,
            shipToAddress2 TEXT NOT NULL,
            shipToState TEXT NOT NULL,
            shipToZip TEXT NOT NULL,
            binLocation TEXT,
            shipToDateIs7DaysFromToday INTEGER NOT NULL,
            onHand INTEGER NOT NULL,
            onPurchaseOrder INTEGER,
            purchaseOrder INTEGER,
            matchedPo INTEGER,
            expectedDate TEXT,
            noteCode TEXT,
            reviewDate TEXT,
            note TEXT,
            sendToWarehouse INTEGER NOT NULL,
            partial INTEGER NOT NULL,
            hasException INTEGER NOT NULL,
            orderCt TEXT NOT NULL,
            matchDataForLabelCheck TEXT NOT NULL,
            baseSku TEXT,
            packQty INTEGER NOT NULL,
            requiredQty INTEGER NOT NULL,
            invoiced INTEGER NOT NULL,
            isKit INTEGER NOT NULL,
            shippedQty INTEGER NOT NULL,
            bulkBinLocation TEXT
        );
    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        stmt.execute()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}

fun createSkuCrossIndexIfNotExists(connection: Connection) {
    val sql = """
        CREATE TABLE IF NOT EXISTS skuCrossIndex (
            amzOcsPartsSku TEXT PRIMARY KEY,
            qbSku TEXT,
            packSize INTEGER
        );
    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        stmt.execute()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}

fun createItemsIfNotExists(connection: Connection) {
    val sql = """
        CREATE TABLE IF NOT EXISTS items (
            item TEXT PRIMARY KEY,
            purchaseDescription TEXT,
            mpn TEXT,
            type TEXT NOT NULL,
            cost NUMERIC,
            price NUMERIC,
            quantityOnHand NUMERIC,
            quantityOnSalesOrder INTEGER,
            quantityOnPurchaseOrder INTEGER,
            preferredVendor TEXT,
            costValue NUMERIC NOT NULL,
            onHandValue NUMERIC NOT NULL,
            salesOrderValue INTEGER NOT NULL,
            purchaseOrderValue INTEGER NOT NULL,
            priceValue NUMERIC NOT NULL,
            isKit INTEGER NOT NULL
        );
    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        stmt.execute()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}

fun createBPItemsIfNotExists(connection: Connection) {
    val sql = """
        CREATE TABLE IF NOT EXISTS bpItems (
            item TEXT PRIMARY KEY,
            description TEXT,
            bulkBinLocation TEXT,
            palletNumbers TEXT,
            palletQuantity TEXT
        );
    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        stmt.execute()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}


fun getAppState(connection: Connection): AppState {
    val query = "SELECT * FROM app_state limit 1"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
    return AppState(
        id = resultSet.getInt("id"),
        version = resultSet.getString("version"),
        lastRefreshed = resultSet.getLong("lastRefreshed"),
    )

}

fun getBPItems(connection: Connection): List<BPItem> {
    val results = mutableListOf<BPItem>()
    val query = "SELECT * FROM bpItems"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
    while (resultSet.next()) {
        results.add(
            BPItem(
                item = resultSet.getString("item"),
                description = resultSet.getString("description"),
                bulkBinLocation =  resultSet.getString("bulkBinLocation"),
                palletNumbers =  resultSet.getString("palletNumbers"),
                palletQuantity =   resultSet.getString("palletQuantity"),
            )
        )
    }
    return results
}


fun insertOrUpdateOrdersInQueue(orders: List<Order>, connection: Connection) {
    val sql = """
        INSERT INTO orders (
            lineItemId, id, po, date, nameZip, namePhone, nameEmail, shipDate,
            via, terms, num, name, qty, upsAccount, item, itemDescription,
            backOrdered, shipToCity, shipToAddress, shipToAddress2, shipToState, shipToZip,
            binLocation, shipToDateIs7DaysFromToday, onHand, onPurchaseOrder,
            purchaseOrder, matchedPo, expectedDate, noteCode, reviewDate, note,
            sendToWarehouse, partial, hasException, orderCt, matchDataForLabelCheck,
            baseSku, packQty, requiredQty, invoiced, isKit, shippedQty, bulkBinLocation
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(lineItemId) DO UPDATE SET
            id=excluded.id,
            po=excluded.po,
            date=excluded.date,
            nameZip=excluded.nameZip,
            namePhone=excluded.namePhone,
            nameEmail=excluded.nameEmail,
            shipDate=excluded.shipDate,
            via=excluded.via,
            terms=excluded.terms,
            num=excluded.num,
            name=excluded.name,
            qty=excluded.qty,
            upsAccount=excluded.upsAccount,
            item=excluded.item,
            itemDescription=excluded.itemDescription,
            backOrdered=excluded.backOrdered,
            shipToCity=excluded.shipToCity,
            shipToAddress=excluded.shipToAddress,
            shipToAddress2=excluded.shipToAddress2,
            shipToState=excluded.shipToState,
            shipToZip=excluded.shipToZip,
            binLocation=excluded.binLocation,
            shipToDateIs7DaysFromToday=excluded.shipToDateIs7DaysFromToday,
            onHand=excluded.onHand,
            onPurchaseOrder=excluded.onPurchaseOrder,
            purchaseOrder=excluded.purchaseOrder,
            matchedPo=excluded.matchedPo,
            expectedDate=excluded.expectedDate,
            noteCode=excluded.noteCode,
            reviewDate=excluded.reviewDate,
            note=excluded.note,
            sendToWarehouse=excluded.sendToWarehouse,
            partial=excluded.partial,
            hasException=excluded.hasException,
            orderCt=excluded.orderCt,
            matchDataForLabelCheck=excluded.matchDataForLabelCheck,
            baseSku=excluded.baseSku,
            packQty=excluded.packQty,
            requiredQty=excluded.requiredQty,
            invoiced=excluded.invoiced,
            isKit=excluded.isKit,
            shippedQty=excluded.shippedQty,
            bulkBinLocation=excluded.bulkBinLocation
    """.trimIndent()

    val stmt = connection.prepareStatement(sql)
    try {
        for (order in orders) {
            var i = 1
            fun set(value: Any?) {
                when (value) {
                    null -> stmt.setObject(i++, null)
                    is String -> stmt.setString(i++, value)
                    is Int -> stmt.setInt(i++, value)
                    is Boolean -> stmt.setInt(i++, if (value) 1 else 0)
                    is LocalDate -> stmt.setString(i++, value.toString())
                    else -> stmt.setObject(i++, value)
                }
            }

            with(order) {
                listOf(
                    lineItemId, id, po, date, nameZip, namePhone, nameEmail, shipDate,
                    via, terms, num, name, qty, upsAccount, item, itemDescription,
                    backOrdered, shipToCity, shipToAddress, shipToAddress2, shipToState, shipToZip,
                    binLocation, shipToDateIs7DaysFromToday, onHand, onPurchaseOrder,
                    purchaseOrder, matchedPo, expectedDate, noteCode, reviewDate, note,
                    sendToWarehouse, partial, hasException, orderCt, matchDataForLabelCheck,
                    baseSku, packQty, requiredQty, invoiced, isKit, shippedQty, bulkBinLocation
                ).forEach { set(it) }
            }

            stmt.executeUpdate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }

}

fun insertOrUpdateSkuCrossItems(skuCrossIndex: List<SkuCrossIndex>, connection: Connection) {
    val sql = """
        INSERT INTO skuCrossIndex (
            amzOcsPartsSku, qbSku, packSize
        )
        VALUES (?, ?, ?)
        ON CONFLICT(amzOcsPartsSku) DO UPDATE SET
            qbSku=excluded.qbSku,
            packSize=excluded.packSize
    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        for (skuItem in skuCrossIndex) {
            var i = 1
            fun set(value: Any?) {
                when (value) {
                    null -> stmt.setObject(i++, null)
                    is String -> stmt.setString(i++, value)
                    is Int -> stmt.setInt(i++, value)
                    is Boolean -> stmt.setInt(i++, if (value) 1 else 0)
                    is LocalDate -> stmt.setString(i++, value.toString())
                    else -> stmt.setObject(i++, value)
                }
            }

            with(skuItem) {
                listOf(
                    amzOcsPartsSku, qbSku, packSize
                ).forEach { set(it) }
            }

            stmt.executeUpdate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}

fun insertOrUpdateItems(items: List<Item>, connection: Connection) {
    val sql = """
        INSERT INTO items (
            item, purchaseDescription, mpn, type, cost, price, quantityOnHand, quantityOnSalesOrder,
            quantityOnPurchaseOrder, preferredVendor, costValue, onHandValue, salesOrderValue, 
            purchaseOrderValue, priceValue, isKit
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(item) DO UPDATE SET
            purchaseDescription=excluded.purchaseDescription,
            mpn=excluded.mpn,
            type=excluded.type,
            cost=excluded.cost,
            price=excluded.price,
            quantityOnHand=excluded.quantityOnHand,
            quantityOnSalesOrder=excluded.quantityOnSalesOrder,
            quantityOnPurchaseOrder=excluded.quantityOnPurchaseOrder,
            preferredVendor=excluded.preferredVendor,
            costValue=excluded.costValue,
            onHandValue=excluded.onHandValue,
            salesOrderValue=excluded.salesOrderValue,
            purchaseOrderValue=excluded.purchaseOrderValue,
            priceValue=excluded.priceValue,
            isKit=excluded.isKit

    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        for (itemData in items) {
            var i = 1
            fun set(value: Any?) {
                when (value) {
                    null -> stmt.setObject(i++, null)
                    is String -> stmt.setString(i++, value)
                    is Int -> stmt.setInt(i++, value)
                    is Boolean -> stmt.setInt(i++, if (value) 1 else 0)
                    is LocalDate -> stmt.setString(i++, value.toString())
                    else -> stmt.setObject(i++, value)
                }
            }

            with(itemData) {
                listOf(
                    item, purchaseDescription, mpn, type, cost, price, quantityOnHand, quantityOnSalesOrder,
                    quantityOnPurchaseOrder, preferredVendor, costValue, onHandValue, salesOrderValue,
                    purchaseOrderValue, priceValue, isKit
                ).forEach { set(it) }
            }

            stmt.executeUpdate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}

fun insertOrUpdateBPItems(bpItems: List<BPItem>, connection: Connection) {
    val sql = """
        INSERT INTO bpItems (
            item, description, bulkBinLocation, palletNumbers, palletQuantity
        )
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(item) DO UPDATE SET
            description=excluded.description,
            bulkBinLocation=excluded.bulkBinLocation,
            palletNumbers=excluded.palletNumbers,
            palletQuantity=excluded.palletQuantity

    """.trimIndent()
    val stmt = connection.prepareStatement(sql)
    try {
        for (bpItem in bpItems) {
            var i = 1
            fun set(value: Any?) {
                when (value) {
                    null -> stmt.setObject(i++, null)
                    is String -> stmt.setString(i++, value)
                    is Int -> stmt.setInt(i++, value)
                    is Boolean -> stmt.setInt(i++, if (value) 1 else 0)
                    is LocalDate -> stmt.setString(i++, value.toString())
                    else -> stmt.setObject(i++, value)
                }
            }

            with(bpItem) {
                listOf(
                    item, description, bulkBinLocation, palletNumbers, palletQuantity
                ).forEach { set(it) }
            }

            stmt.executeUpdate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stmt.close()
    }
}


// TODO delete partials like in old app when generating the new queue