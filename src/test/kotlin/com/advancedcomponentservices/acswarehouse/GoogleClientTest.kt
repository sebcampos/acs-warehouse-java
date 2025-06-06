package com.advancedcomponentservices.acswarehouse

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.cdimascio.dotenv.dotenv
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.util.Base64


import com.advancedcomponentservices.acswarehouse.google.Client as GoogleClient

class GoogleClientTest {

    val dotenv = dotenv()
    val encoded = dotenv["GOOGLE_SERVICE_ACCOUNT"] ?: error("GOOGLE_SERVICE_ACCOUNT not found")
    val json = String(Base64.getDecoder().decode(encoded))

    val gson = Gson()
    val mapType: Type? = object : TypeToken<HashMap<String, String>>() {}.type
    val serviceAccount: HashMap<String, String> = gson.fromJson(json, mapType)

    @Test
    fun `test Google Client Authenticate`() {
        val gc = GoogleClient(serviceAccount)
        println(gc.getAccessToken())
    }

    @Test
    fun `test Get Spreadsheet`() {
        val gc = GoogleClient(serviceAccount)
        val spreadSheet = gc.getSpreadSheet("Commercial Order Process V2")
        println(spreadSheet)
    }

    @Test
    fun `test Read SpreadSheet` () {
        val gc = GoogleClient(serviceAccount)
        val spreadSheet = gc.getSpreadSheet("Commercial Order Process V2")!!
        val sheetResponse = gc.readSpreadSheet(spreadSheet.id, "PickListLog")
        println(sheetResponse)
    }

    @Test
    fun `test Bulk Sheet Read` ()
    {
        val gc = GoogleClient(serviceAccount)
        val spreadSheet = gc.getSpreadSheet("Commercial Order Process V2")!!
        val sheetsList = setOf("Open Order Status Report", "SKU Cross Index", "Items")
        val response = gc.readSpreadSheets(spreadSheet.id, sheetsList)
        println(response)
    }


}