package com.example.imu_demo.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

// HiltViewModel 어노테이션을 사용하여 Hilt가 이 ViewModel을 관리하도록 설정
@HiltViewModel
class BuiltInViewModel @Inject constructor(
    @ApplicationContext private val context: Context // ApplicationContext 주입
) : ViewModel(), SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // MutableStateFlow를 사용하여 센서 데이터 상태 관리
    private val _accelerometerData = MutableStateFlow<FloatArray?>(null)
    val accelerometerData: StateFlow<FloatArray?> = _accelerometerData

    private val _gyroscopeData = MutableStateFlow<FloatArray?>(null)
    val gyroscopeData: StateFlow<FloatArray?> = _gyroscopeData

    // 타이머 카운터를 위한 MutableStateFlow 선언 및 초기화
    private val _sensorUpdateCounter = MutableStateFlow(0L)
    val sensorUpdateCounter: StateFlow<Long> = _sensorUpdateCounter

    private val _accelerometerDetails = MutableStateFlow<String?>(null)
    val accelerometerDetails: StateFlow<String?> = _accelerometerDetails

    private val _gyroscopeDetails = MutableStateFlow<String?>(null)
    val gyroscopeDetails: StateFlow<String?> = _gyroscopeDetails

    private var samplingRateInMicroseconds: Int = 20000 // 50Hz

    init {
        // 센서 리스너 등록
        accelerometer?.also {
            sensorManager.registerListener(this, it, samplingRateInMicroseconds)
            accelerometer?.let { updateSensorDetails(it, _accelerometerDetails) }
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, samplingRateInMicroseconds)
            gyroscope?.let { updateSensorDetails(it, _gyroscopeDetails) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        viewModelScope.launch {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> _accelerometerData.value = event.values
                Sensor.TYPE_GYROSCOPE -> _gyroscopeData.value = event.values
            }
            incrementSensorUpdateCounter()
//            Log.d(TAG, "onSensorChanged: ${event.values[0]}")
        }
    }

    private fun incrementSensorUpdateCounter() {
        _sensorUpdateCounter.value = _sensorUpdateCounter.value + 1
    }

    private fun updateSensorDetails(sensor: Sensor, detailsFlow: MutableStateFlow<String?>) {
        val rangeUnit: String
        val resolutionUnit: String

        when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                rangeUnit = "m/s²"
                resolutionUnit = "m/s²"
            }
            Sensor.TYPE_GYROSCOPE -> {
                rangeUnit = "rad/s"
                resolutionUnit = "rad/s"
            }
            else -> {
                rangeUnit = ""
                resolutionUnit = ""
            }
        }

        val details = "Vendor: ${sensor.vendor}\n" +
                "Name: ${sensor.name}\n" +
                "Range: ${sensor.maximumRange} $rangeUnit\n" +
                "Resolution: ${BigDecimal(sensor.resolution.toDouble()).setScale(10, RoundingMode.HALF_EVEN).toPlainString()} $resolutionUnit"
        detailsFlow.value = details
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 센서 정확도 변경에 필요한 로직 구현 (필요한 경우)
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 파괴 시 센서 리스너 해제
        sensorManager.unregisterListener(this)
    }
}