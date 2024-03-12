package com.example.imu_demo.data.dao

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

object CSVFileHandler {
    fun <T> saveDataToCSV(
        context: Context,
        dataList: List<T>,
        fileName: String,
        csvHeader: String, // CSV 헤더를 직접 매개변수로 받습니다.
        convertToCsvLine: (T) -> String, // 각 데이터 항목을 CSV 라인으로 변환하는 함수
        experimentsDirName: String = "Experiments"
    ) {
        val stringBuilder = StringBuilder(csvHeader)

        dataList.forEach { data ->
            stringBuilder.append("\n").append(convertToCsvLine(data))
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val experimentsDir = File(downloadsDir, experimentsDirName)
        if (!experimentsDir.exists()) {
            experimentsDir.mkdirs()
        }

        val file = File(experimentsDir, fileName)
        file.writeText(stringBuilder.toString())
    }
}