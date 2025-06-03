package com.advancedcomponentservices.acswarehouse.db

import com.advancedcomponentservices.acswarehouse.db.models.AppState
import com.advancedcomponentservices.acswarehouse.db.models.BinAndWarehouseLocations
import com.advancedcomponentservices.acswarehouse.db.models.Settings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.sql.DriverManager
import java.sql.Connection



fun connectToDatabase(): Connection? {
    val userHome = System.getProperty("user.home")
    val dbPath = "$userHome/.acs-warehouse/warehouse.db"
    val url = "jdbc:sqlite:$dbPath"
    return try {
        DriverManager.getConnection(url).also {
            println("Connected to SQLite DB.")
        }
    } catch (e: Exception) {
        println("Connection failed: ${e.message}")
        null
    }
}

fun getSettings(connection: Connection): Settings {
    val query = "SELECT * FROM settings limit 1"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
    val jsonString = resultSet.getString("service_account")
    val type = object : TypeToken<HashMap<String, String>>() {}.type
    val serviceAccount: HashMap<String, String> = Gson().fromJson(jsonString, type)
    return Settings(
        spreadSheetName = resultSet.getString("spreadsheet_name"),
        openOrderStatusReportName =  resultSet.getString("open_order_status_name"),
        crossIndexName =  resultSet.getString("cross_index_name"),
        pickListLogName =  resultSet.getString("pick_list_log_name"),
        itemsName = resultSet.getString("items_name"),
        serviceAccount = serviceAccount,
        pdfDirectory =  resultSet.getString("pdf_directory"),
        shippingStationDirectory =   resultSet.getString("shipping_stations_directory"),
    )
}

fun getAppState(connection: Connection): AppState {
    val query = "SELECT * FROM app_state limit 1"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
    return AppState(
        id = resultSet.getInt("id"),
        version = resultSet.getString("version"),
        lastRefreshed = resultSet.getLong("last_refreshed"),
    )

}

fun getBinAndWarehouseLocations(connection: Connection): List<BinAndWarehouseLocations> {
    val results = mutableListOf<BinAndWarehouseLocations>()
    val query = "SELECT * FROM bin_and_warehouse_locations"
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
    while (resultSet.next()) {
        results.add(
            BinAndWarehouseLocations(
                item = resultSet.getString("item"),
                name = resultSet.getString("name"),
                bulkBinLocation =  resultSet.getString("bulk_bin_location"),
                standardPackMethod =  resultSet.getString("standard_pack_method"),
                baseSkuCt =   resultSet.getString("base_sku_ct"),
                manualBaseSku =   resultSet.getString("manual_base_sku"),
                packQty =  resultSet.getInt("pack_qty"),
                onHandQty =    resultSet.getInt("on_hand_qty"),
                qtySold =   resultSet.getInt("qty_sold"),
                salesRankQty =    resultSet.getInt("sales_rank_qty")
            )
        )
    }
    return results
}