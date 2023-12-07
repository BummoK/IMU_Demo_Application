package com.example.imu_demo.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.imu_demo.domain.BluetoothDeviceDomain
import com.example.imu_demo.domain.BluetoothController
import com.example.imu_demo.domain.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null

    private  val _isConnected = MutableStateFlow<Boolean>(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if(newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.tryEmit("Can't connect to a non-paired device.")
            }
        }
    }
    private val serviceUUID = UUID.fromString("ABF0E000-B597-4BE0-B869-6054B7ED0CE3")
    private val characteristicUUID = UUID.fromString("ABF0E002-B597-4BE0-B869-6054B7ED0CE3")

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermission((Manifest.permission.BLUETOOTH_SCAN))){
            return
        }
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()

        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    override fun closeConnection() {
        bluetoothGatt?.run {
            disconnect()
            close()
        }
        bluetoothGatt = null
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> = flow {
        val deviceAddress = device.address
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        if (bluetoothDevice == null) {
            emit(ConnectionResult.Error("Device not found or Bluetooth not available"))
            return@flow
        }

        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattClientCallback)
        // 연결 결과는 gattClientCallback에서 처리됩니다.
        // gattClientCallback에서 연결 상태에 따라 ConnectionResult를 emit해야 합니다.
    }

    override fun disconnectFromDevice(device: BluetoothDeviceDomain) {
        if (bluetoothGatt != null && bluetoothGatt?.device?.address == device.address) {
            bluetoothGatt?.disconnect()
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val deviceAddress = gatt.device.address

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    updateDeviceConnectionStatus(deviceAddress, true)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    updateDeviceConnectionStatus(deviceAddress, false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in gatt.services) {
                    if (service.uuid == serviceUUID) {
                        for (characteristic in service.characteristics) {
                            if (characteristic.uuid == characteristicUUID) {
                                // Arduino 센서 데이터를 읽기 위한 로직
                                gatt.readCharacteristic(characteristic)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 데이터 읽기 처리
                val data = characteristic.value
                // 데이터 처리 로직
            }
        }
        // 필요한 경우 데이터 쓰기 관련 콜백 메서드 추가
    }
    // 기타 필요한 메서드 및 처리 로직

    private fun updateDeviceConnectionStatus(deviceAddress: String, isConnected: Boolean) {
        _scannedDevices.update { devices ->
            devices.map { device ->
                if (device.address == deviceAddress) {
                    device.copy(isConnected = isConnected)
                } else {
                    device
                }
            }
        }

        // 이와 유사하게 _pairedDevices 또는 _connectedDevices 업데이트
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }

    private fun updatePairedDevices() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean{
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}