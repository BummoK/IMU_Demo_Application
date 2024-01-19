package com.example.imu_demo.util

import android.net.Uri
import android.content.ContentResolver
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

class AnalysisViewModel : ViewModel() {
    private val _sensorDataFlow = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorDataFlow: StateFlow<List<SensorData>> = _sensorDataFlow

    private val _selectedFileName = MutableStateFlow("")
    val selectedFileName: StateFlow<String> = _selectedFileName

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
}