package com.example.imu_demo.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val pairedDevices: StateFlow<List<BluetoothDevice>>
    val errors: SharedFlow<String>

    val timerValueStateFlow: StateFlow<Long?>
    val accXValueStateFlow: StateFlow<Float?>
    val accYValueStateFlow: StateFlow<Float?>
    val accZValueStateFlow: StateFlow<Float?>
    val gyroXValueStateFlow: StateFlow<Float?>
    val gyroYValueStateFlow: StateFlow<Float?>
    val gyroZValueStateFlow: StateFlow<Float?>
    val batteryValueStateFlow: StateFlow<Float?>

    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> // 수정된 시그니처
    fun disconnectFromDevice(device: BluetoothDeviceDomain) // 추가된 메서드
    fun closeConnection()
    fun release()
}