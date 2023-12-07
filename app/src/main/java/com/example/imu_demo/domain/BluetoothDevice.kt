package com.example.imu_demo.domain

typealias BluetoothDeviceDomain = BluetoothDevice
data class BluetoothDevice(
    val name: String?,
    val address: String,
    val isConnected: Boolean
)