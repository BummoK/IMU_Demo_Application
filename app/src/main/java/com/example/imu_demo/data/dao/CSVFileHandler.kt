package com.example.imu_demo.data.dao

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

object CSVFileHandler {
    fun saveDataToCSV(context: Context, sensorDataList: List<SensorData>, fileName: String) {
        val csvHeader = "Time, AccX, AccY, AccZ, GyroX, GyroY, GyroZ\n"
        val stringBuilder = StringBuilder(csvHeader)

        sensorDataList.forEach { data ->
            stringBuilder.append("${data.time}, ${data.accX}, ${data.accY}, ${data.accZ}, ${data.gyroX}, ${data.gyroY}, ${data.gyroZ}\n")
        }

        // Downloads 디렉토리 내에 Experiments 폴더 생성
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val experimentsDir = File(downloadsDir, "Experiments")
        if (!experimentsDir.exists()) {
            experimentsDir.mkdirs()
        }

        val file = File(experimentsDir, fileName)
        file.writeText(stringBuilder.toString())
    }

    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }
}