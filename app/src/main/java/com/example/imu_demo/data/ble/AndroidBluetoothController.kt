package com.example.imu_demo.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.example.imu_demo.domain.BluetoothController
import com.example.imu_demo.domain.BluetoothDeviceDomain
import com.example.imu_demo.domain.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val TAG = "Central"

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


    private val _timerValueStateFlow = MutableStateFlow<Long?>(null)
    override val timerValueStateFlow: StateFlow<Long?> = _timerValueStateFlow

    private val _accXValueStateFlow = MutableStateFlow<Float?>(null)
    override val accXValueStateFlow: StateFlow<Float?> = _accXValueStateFlow

    private val _accYValueStateFlow = MutableStateFlow<Float?>(null)
    override val accYValueStateFlow: StateFlow<Float?> = _accYValueStateFlow

    private val _accZValueStateFlow = MutableStateFlow<Float?>(null)
    override val accZValueStateFlow: StateFlow<Float?> = _accZValueStateFlow

    private val _gyroXValueStateFlow = MutableStateFlow<Float?>(null)
    override val gyroXValueStateFlow: StateFlow<Float?> = _gyroXValueStateFlow

    private val _gyroYValueStateFlow = MutableStateFlow<Float?>(null)
    override val gyroYValueStateFlow: StateFlow<Float?> = _gyroYValueStateFlow

    private val _gyroZValueStateFlow = MutableStateFlow<Float?>(null)
    override val gyroZValueStateFlow: StateFlow<Float?> = _gyroZValueStateFlow

    private val _batteryValueStateFlow = MutableStateFlow<Float?>(null)
    override val batteryValueStateFlow: StateFlow<Float?> = _batteryValueStateFlow

    // UUID 정의
    private val serviceUUID = UUID.fromString("ABF0E000-B597-4BE0-B869-6054B7ED0CE3")
    private val imuDataCharUUID = UUID.fromString("ABF0E001-B597-4BE0-B869-6054B7ED0CE3") // 수정된 특성 UUID

    // 특성 참조 추가
    private var timerChar: BluetoothGattCharacteristic? = null
    private var accXChar: BluetoothGattCharacteristic? = null
    private var accYChar: BluetoothGattCharacteristic? = null
    private var accZChar: BluetoothGattCharacteristic? = null
    private var gyroXChar: BluetoothGattCharacteristic? = null
    private var gyroYChar: BluetoothGattCharacteristic? = null
    private var gyroZChar: BluetoothGattCharacteristic? = null
    private var tempChar: BluetoothGattCharacteristic? = null

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
                    Log.d(TAG, "Connected to the GATT server")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    updateDeviceConnectionStatus(deviceAddress, false)
                    Log.d(TAG, "Disconnected to the GATT server")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(serviceUUID)
                service?.let {
                    it.characteristics.forEach { characteristic ->
                        // 알림이 가능한 특성인지 확인
                        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            gatt.setCharacteristicNotification(characteristic, true)

                            // Descriptor 설정
                            val descriptor = characteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            )
                            descriptor?.let {
                                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(it)
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            // characteristic.getValue()는 데이터의 바이트 배열을 반환합니다.
            val data = characteristic.value

            // 받아온 데이터의 바이트 수를 로그로 출력
//            Log.d(TAG, "Received data length: ${data.size} bytes")

            if (characteristic.uuid == imuDataCharUUID) {
                val dataString = data.joinToString(", ") { it.toString() }
//                Log.d(TAG, "Received data: $dataString")
                parseIMUData(characteristic.value)
            }
        }

        private fun parseIMUData(data: ByteArray) {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val startMarker = String(data.sliceArray(0..1), Charsets.UTF_8)
            val endMarker = String(data.sliceArray(15..16), Charsets.UTF_8)

            if (startMarker == "ST" && endMarker == "ED") {
//                Log.e(TAG, "parsing success")
                val time = ByteBuffer.wrap(data.sliceArray(4..7)).order(ByteOrder.LITTLE_ENDIAN).getInt().toLong()
                val accX = byteToFloat(buffer.get(8), -16.0f, 16.0f)    // 변환된 가속도 X
                val accY = byteToFloat(buffer.get(9), -16.0f, 16.0f)    // 변환된 가속도 Y
                val accZ = byteToFloat(buffer.get(10), -16.0f, 16.0f)    // 변환된 가속도 Z
                val gyroX = byteToFloat(buffer.get(11), -2000f, 2000f) // 변환된 자이로스코프 X
                val gyroY = byteToFloat(buffer.get(12), -2000f, 2000f) // 변환된 자이로스코프 Y
                val gyroZ = byteToFloat(buffer.get(13), -2000f, 2000f) // 변환된 자이로스코프 Z
                val battery = byteToFloat(buffer.get(14), -0f, 100f)      // 변환된 온도

                // 이제 각 값들을 StateFlow에 업데이트
                _timerValueStateFlow.value = time
                _accXValueStateFlow.value = accX
                _accYValueStateFlow.value = accY
                _accZValueStateFlow.value = accZ
                _gyroXValueStateFlow.value = gyroX
                _gyroYValueStateFlow.value = gyroY
                _gyroZValueStateFlow.value = gyroZ
                _batteryValueStateFlow.value = battery
            } else {
                Log.e(TAG, "Error parsing IMU data")
            }
        }

        private fun byteToFloat(byteValue: Byte, minVal: Float, maxVal: Float): Float {
            val normalized = (byteValue.toInt() and 0xFF) / 255f
            return minVal + (maxVal - minVal) * normalized
        }
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