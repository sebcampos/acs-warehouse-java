package com.advancedcomponentservices.acswarehouse

import org.junit.jupiter.api.Test

class ManagerTest {

    val manager = Manager()

    @Test
    fun `test Refresh Database`() {
        manager.refreshDatabase()
    }

    @Test
    fun `test Get Send To Warehouse`(){
        val sendToWarehouse = manager.getSendToWarehouse()
        println(sendToWarehouse)
    }

    @Test
    fun `test Create Picklist`(){
        manager.createPickList()
    }

}