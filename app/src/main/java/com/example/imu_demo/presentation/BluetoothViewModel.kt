package com.example.imu_demo.presentation

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.imu_demo.data.dao.AppDatabase
import com.example.imu_demo.data.dao.CSVFileHandler
import com.example.imu_demo.data.dao.CSVFileHandler.saveDataToCSV
import com.example.imu_demo.data.dao.SensorData
import com.example.imu_demo.data.dao.SensorDataDao
import com.example.imu_demo.domain.BluetoothController
import com.example.imu_demo.domain.BluetoothDeviceDomain
import com.example.imu_demo.domain.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val sensorDataDao: SensorDataDao,
    private val appDatabase: AppDatabase
): ViewModel() {

    private val TAG = "BLEViewModel"


    val timerValue = bluetoothController.timerValueStateFlow
    val accXValue = bluetoothController.accXValueStateFlow
    val accYValue = bluetoothController.accYValueStateFlow
    val accZValue = bluetoothController.accZValueStateFlow
    val gyroXValue = bluetoothController.gyroXValueStateFlow
    val gyroYValue = bluetoothController.gyroYValueStateFlow
    val gyroZValue = bluetoothController.gyroZValueStateFlow
    val batteryValue = bluetoothController.batteryValueStateFlow

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var deviceConnectionJob: Job? = null

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)
        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(
                errorMessage = error
            ) }
        }.launchIn(viewModelScope)
    }


    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .onCompletion {
                _state.update { it.copy(isConnecting = false) } // 연결 작업이 완료되면 isConnecting을 false로 업데이트합니다.
            }
            .listen()
    }

    fun disconnectFromDevice(device: BluetoothDeviceDomain) {
        bluetoothController.disconnectFromDevice(device)
        _state.update { it.copy(
            isConnected = false,
            isConnecting = false
        ) }
    }

    fun startScan() {
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when(result) {
                ConnectionResult.ConnectionEstablished -> {
                    _state.update { it.copy(
                        isConnected = true,
                        isConnecting = false,
                        errorMessage = null
                    ) }
                }
                is ConnectionResult.Error -> {
                    _state.update { it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    ) }
                }
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _state.update { it.copy(
                    isConnected = false,
                    isConnecting = false,
                ) }
            }
            .launchIn(viewModelScope)
    }


    fun saveSensorData(sensorData: SensorData) {
        viewModelScope.launch {
            sensorDataDao.insert(sensorData)
        }
    }

    fun fetchAndLogSensorData() {
        viewModelScope.launch {
            sensorDataDao.getAll()
                .collect { sensorDataList -> // Flow를 수집합니다.
                    sensorDataList.forEach { sensorData ->
                        Log.d("SensorDataLog", "Data: $sensorData")
                    }
                }
        }
    }

    fun stopRecordingAndExportToCSV(context: Context, onExportComplete: (String) -> Unit) {
        viewModelScope.launch {
            val sensorDataList = sensorDataDao.getAll().first()
            val fileName = "SensorData_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

            try {
                CSVFileHandler.saveDataToCSV(context, sensorDataList, fileName)
                val fullPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Experiments/$fileName").absolutePath
                Log.d("stopRecording", "Sensor data saved: $fullPath")
                onExportComplete(fullPath) // 전체 경로를 콜백으로 전달
                sensorDataDao.deleteAll()
            } catch (e: Exception) {
                Log.e("stopRecording", "Error saving sensor data: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}