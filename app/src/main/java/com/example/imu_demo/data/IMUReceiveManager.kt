package com.example.imu_demo.data

import com.example.imu_demo.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface IMUReceiveManager {
    val data: MutableSharedFlow<Resource<IMUResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()
}