package com.example.imu_demo.util

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.imu_demo.data.ble.AndroidBluetoothController
import com.example.imu_demo.data.dao.AppDatabase
import com.example.imu_demo.data.dao.CSVFileHandler
import com.example.imu_demo.data.dao.SensorData
import com.example.imu_demo.data.dao.SensorDataDao
import com.example.imu_demo.data.dao.SensorDataDaoSW
import com.example.imu_demo.data.dao.SensorDataDaoSWRaw
import com.example.imu_demo.data.dao.SensorDataSW
import com.example.imu_demo.data.dao.SensorDataSWRaw
import com.example.imu_demo.domain.BluetoothController
import com.example.imu_demo.domain.BluetoothDeviceDomain
import com.example.imu_demo.domain.ConnectionResult
import com.example.imu_demo.presentation.SensorChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothController: BluetoothController,
    private val sensorDataDao: SensorDataDao,
    private val sensorDataDaoSWRaw: SensorDataDaoSWRaw,
    private val appDatabase: AppDatabase
): ViewModel(), DataCallback {

    private val TAG = "BLEViewModel"


    val timerValue = bluetoothController.timerValueStateFlow
    val accXValue = bluetoothController.accXValueStateFlow
    val accYValue = bluetoothController.accYValueStateFlow
    val accZValue = bluetoothController.accZValueStateFlow
    val gyroXValue = bluetoothController.gyroXValueStateFlow
    val gyroYValue = bluetoothController.gyroYValueStateFlow
    val gyroZValue = bluetoothController.gyroZValueStateFlow
    val batteryValue = bluetoothController.batteryValueStateFlow
    val alarmInfoValue = bluetoothController.alarmInfoValueStateFlow
    val rawDataString = bluetoothController.rawDataStringStateFlow
    val receivedDataSize = bluetoothController.receivedDataSizeStateFlow

    private val _state = MutableStateFlow(BluetoothUiState())

    val isRecording = bluetoothController.isRecording

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

        if (bluetoothController is AndroidBluetoothController) {
            bluetoothController.setDataCallback(this)
        }

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


    private fun determineCsvHeader(dao: Any): String {
        return when (dao) {
            is SensorDataDao -> "Time, AccX, AccY, AccZ, GyroX, GyroY, GyroZ, Motion, Risk"
            is SensorDataDaoSW -> "Time, AccX, AccY, AccZ, GyroX, GyroY, GyroZ, Motion, Risk, Fall"
            is SensorDataDaoSWRaw -> "DataSize, Data"
            else -> throw IllegalArgumentException("Unsupported DAO type")
        }
    }

    override fun onDataReceived(dataString: String, dataSize: Int) {
        handleIncomingData(dataString, dataSize)
    }

    private fun handleIncomingData(dataString: String, dataSize: Int) {
        if (isRecording.value) {
            val sensorDataSWRaw = SensorDataSWRaw(
                dataSize = dataSize,
                dataString = dataString
            )
            saveSensorData(sensorDataSWRaw, sensorDataDaoSWRaw)
        }
    }

    fun <T> saveSensorData(data: T, dao: Any) {
        viewModelScope.launch {
            when (dao) {
                is SensorDataDao -> dao.insert(data as SensorData)
                is SensorDataDaoSW -> dao.insert(data as SensorDataSW)
                is SensorDataDaoSWRaw -> dao.insert(data as SensorDataSWRaw)
                else -> throw IllegalArgumentException("Unsupported DAO type")
            }
        }
    }

    fun toggleRecording(){
        bluetoothController.toggleRecording()
    }

    fun stopRecordingAndExport() {
        stopRecordingAndExportToCSV(
            dao = sensorDataDaoSWRaw,
            fileNamePrefix = "SensorDataSWRaw",
            convertToCsvLine = { data ->
                if (data is SensorDataSWRaw) {
                    "${data.dataSize}, ${data.dataString}"
                } else ""
            },
            onExportComplete = { fullPath ->
                Log.d(TAG, "Sensor data saved: $fullPath")
            }
        )
    }

    fun stopRecordingAndExportToCSV(
        dao: Any,
        fileNamePrefix: String,
        convertToCsvLine: (Any) -> String,
        onExportComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            val fileName = "${fileNamePrefix}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val dataList = when (dao) {
                is SensorDataDao -> dao.getAll().first().map { convertToCsvLine(it) }
                is SensorDataDaoSW -> dao.getAll().first().map { convertToCsvLine(it) }
                is SensorDataDaoSWRaw -> dao.getAll().first().map { convertToCsvLine(it) }
                else -> throw IllegalArgumentException("Unsupported DAO type")
            }

            try {
                val csvHeader = determineCsvHeader(dao)
                CSVFileHandler.saveDataToCSV(context, dataList, fileName, csvHeader, { it }, "Experiments/$fileNamePrefix")
                val fullPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Experiments/$fileNamePrefix/$fileName").absolutePath
                Log.d("stopRecording", "Sensor data saved: $fullPath")
                onExportComplete(fullPath)
                when (dao) {
                    is SensorDataDao -> dao.deleteAll()
                    is SensorDataDaoSW -> dao.deleteAll()
                    is SensorDataDaoSWRaw -> dao.deleteAll()
                }
            } catch (e: Exception) {
                Log.e("stopRecording", "Error saving sensor data: ${e.message}")
            }
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


    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}