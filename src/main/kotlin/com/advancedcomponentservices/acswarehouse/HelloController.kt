package com.advancedcomponentservices.acswarehouse

import com.advancedcomponentservices.acswarehouse.db.connectToDatabase
import com.advancedcomponentservices.acswarehouse.etl.parseBPItems
import com.advancedcomponentservices.acswarehouse.etl.parseItems
import com.advancedcomponentservices.acswarehouse.etl.parseOpenOrderStatus
import com.advancedcomponentservices.acswarehouse.etl.parseSkuCrossIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.cdimascio.dotenv.dotenv
import com.advancedcomponentservices.acswarehouse.google.Client as GoogleClient
import javafx.fxml.FXML
import javafx.scene.control.Label
import java.lang.reflect.Type
import java.sql.Connection
import java.util.Base64

class HelloController {

    private var connection: Connection = connectToDatabase()!!

    @FXML
    private lateinit var welcomeText: Label

    @FXML
    private fun onHelloButtonClick() {
        welcomeText.text = "Welcome to JavaFX Application!"
    }

    private fun refreshDatabase() {


    }

}