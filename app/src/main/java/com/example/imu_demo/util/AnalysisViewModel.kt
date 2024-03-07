package com.example.imu_demo.util

import android.net.Uri
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Context
import android.media.MediaScannerConnection
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imu_demo.data.dao.SensorData
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AnalysisViewModel : ViewModel() {
    private val _sensorDataFlow = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorDataFlow: StateFlow<List<SensorData>> = _sensorDataFlow

    private val _selectedFileName = MutableStateFlow("")
    val selectedFileName: StateFlow<String> = _selectedFileName

    var time = 0

    fun loadFileData(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sensorDataList = mutableListOf<SensorData>()
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val tokens = line.split(", ")
                        Log.d("loadFileData", "Token size: ${tokens.size}")
                        if (tokens.size >= 8) {
                            try {
                                Log.d("loadFileData", "File load is success")
                                val data = SensorData(
                                    time = tokens[0].toLong(),
                                    accX = tokens[1].toFloat(),
                                    accY = tokens[2].toFloat(),
                                    accZ = tokens[3].toFloat(),
                                    gyroX = tokens[4].toFloat(),
                                    gyroY = tokens[5].toFloat(),
                                    gyroZ = tokens[6].toFloat(),
                                    motion = tokens[7].toInt(),
                                    risk = tokens[8].toFloat()
                                )
                                sensorDataList.add(data)
                            } catch (e: Exception) {
                                Log.d("loadFileData", "File load is failed")
                            }
                        }
                    }
                }
                _sensorDataFlow.value = sensorDataList
                _selectedFileName.value = getFileName(uri, contentResolver)
            }
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                // 파일 이름이 있는 컬럼의 인덱스를 얻습니다.
                val fileNameColumnIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                // 컬럼 인덱스를 사용하여 파일 이름을 얻습니다.
                fileName = it.getString(fileNameColumnIndex)
            }
        }

        return fileName
    }
    fun scanMediaFiles(context: Context, path: String) {
        viewModelScope.launch {
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                Toast.makeText(context, "Scan is finished", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun parseIMUData(data: ByteArray) {

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val bufferLength = data.size
        Log.d(TAG, "bufferLength: $bufferLength")

        val startMarker = String(data.sliceArray(0..1), Charsets.UTF_8)
        Log.d(TAG, "startMarker: $startMarker")

        val frameNumber = ByteBuffer.wrap(data.sliceArray(2..3)).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        Log.d(TAG, "frameNumber: $frameNumber")

        val seqNumber = ByteBuffer.wrap(data.sliceArray(4..5)).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val seqNumberRev = Integer.toHexString(seqNumber)
        Log.d(TAG, "seqNumber: $seqNumber")
        Log.d(TAG, "seqNumberRev: $seqNumberRev")

        val alarmInfo = seqNumber/4096
        Log.d(TAG, "alarmInfo: $alarmInfo")

//        val accX = convertBytesToInt(buffer.get(6), buffer.get(7))/8192.0
//        Log.d(TAG, "accX: $accX")
//
//        val accY = convertBytesToInt(buffer.get(8), buffer.get(9))/8192.0
//        Log.d(TAG, "accY: $accY")
//
//        val accZ = convertBytesToInt(buffer.get(10), buffer.get(11))/8192.0
//        Log.d(TAG, "accZ: $accZ")
//
//        val gyroX = convertBytesToInt(buffer.get(12), buffer.get(13))/65.536
//        Log.d(TAG, "gyroX: $gyroX")
//
//        val gyroY = convertBytesToInt(buffer.get(14), buffer.get(15))/65.536
//        Log.d(TAG, "gyroY: $gyroY")
//
//        val gyroZ = convertBytesToInt(buffer.get(16), buffer.get(17))/65.536
//        Log.d(TAG, "gyroZ: $gyroZ")


        if (startMarker == "SD") {
            var dataBlockNum = 0
            when (bufferLength) {
                130 -> {
                    dataBlockNum = 9
                }
                144 -> {
                    dataBlockNum = 10
                }
                else -> {
                    Log.e(TAG, "Number of data is lack")
                }
            }
            for (i in 0 until dataBlockNum) {
                val accX = convertBytesToInt(buffer.get(6+14*i), buffer.get(7+14*i))/8192.0
                val accY = convertBytesToInt(buffer.get(8+14*i), buffer.get(9+14*i))/8192.0
                val accZ = convertBytesToInt(buffer.get(10+14*i), buffer.get(11+14*i))/8192.0
                val gyroX = convertBytesToInt(buffer.get(12+14*i), buffer.get(13+14*i))/65.536
                val gyroY = convertBytesToInt(buffer.get(14+14*i), buffer.get(15+14*i))/65.536
                val gyroZ = convertBytesToInt(buffer.get(16+14*i), buffer.get(17+14*i))/65.536

                // 이제 각 값들을 StateFlow에 업데이트
//                _timerValueStateFlow.value = time
//                _accXValueStateFlow.value = accX
//                _accYValueStateFlow.value = accY
//                _accZValueStateFlow.value = accZ
//                _gyroXValueStateFlow.value = gyroX
//                _gyroYValueStateFlow.value = gyroY
//                _gyroZValueStateFlow.value = gyroZ
                time += 1
            }

        } else {
            Log.e(TAG, "Error parsing IMU data")
        }
    }
    private fun byteToFloat(byteValue: Byte, minVal: Float, maxVal: Float): Float {
        val normalized = (byteValue.toInt() and 0xFF) / 255f
        return minVal + (maxVal - minVal) * normalized
    }
    private fun convertBytesToInt(byte1: Byte, byte2: Byte): Int {
        // 바이트 배열로 변환
        var signedIntValue = 0
        val byteArray = byteArrayOf(byte1, byte2)

        // Little-endian 형식의 2바이트를 16진수로 읽어옴
        val intValue = (byteArray[1].toInt() and 0xFF shl 8) or (byteArray[0].toInt() and 0xFF)

        if (byte2 >= 0) {
            signedIntValue = intValue
        }
        else {
            signedIntValue = intValue - 65536
        }

        Log.d(TAG, "byte1: $byte1")
        Log.d(TAG, "byte2: $byte2")
        Log.d(TAG, "intValue: $signedIntValue")

        return signedIntValue
    }
}