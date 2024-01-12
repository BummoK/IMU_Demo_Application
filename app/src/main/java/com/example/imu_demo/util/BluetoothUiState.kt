package com.example.imu_demo.util

import com.example.imu_demo.domain.BluetoothDevice
import kotlinx.coroutines.flow.Flow

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,

    val timerValueStateFlow: Long? = null,
    val accXValueStateFlow: Float? = null,
    val accYValueStateFlow: Float? = null,
    val accZValueStateFlow: Float? = null,
    val gyroXValueStateFlow: Float? = null,
    val gyroYValueStateFlow: Float? = null,
    val gyroZValueStateFlow: Float? = null,
    val tempValueStateFlow: Float? = null,
)
