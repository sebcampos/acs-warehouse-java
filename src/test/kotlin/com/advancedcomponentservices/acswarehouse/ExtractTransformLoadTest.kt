package com.advancedcomponentservices.acswarehouse

import com.advancedcomponentservices.acswarehouse.etl.parseOpenOrderStatus
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

    @Test
    fun `test Download Orders Sent to Warehouse` ()
    {
        val gSheetName = "Commercial Order Process v2"
        val sheetName = "Open Order Status Report"
        val gSheet = gc.getSpreadSheet(gSheetName)!!
        val response = gc.readSpreadSheet(gSheet.id, sheetName)
        parseOpenOrderStatus(response.values)
        println(response)
    }


}