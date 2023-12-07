package com.example.imu_demo.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.imu_demo.domain.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isConnected = false
    )
}