package com.example.imu_demo.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.imu_demo.data.dao.SensorData

import com.github.mikephil.charting.data.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    // 낙상검출 알고리즘 파라미터
    var velV by remember { mutableDoubleStateOf(0.0) }
    var dect by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(true) }

    val timerValue by viewModel.timerValue.collectAsState()
    val accXValue by viewModel.accXValue.collectAsState()
    val accYValue by viewModel.accYValue.collectAsState()
    val accZValue by viewModel.accZValue.collectAsState()
    val gyroXValue by viewModel.gyroXValue.collectAsState()
    val gyroYValue by viewModel.gyroYValue.collectAsState()
    val gyroZValue by viewModel.gyroZValue.collectAsState()

    // 차트 데이터 리스트
    val accChartDataX = remember { mutableListOf<Entry>() }
    val accChartDataY = remember { mutableListOf<Entry>() }
    val accChartDataZ = remember { mutableListOf<Entry>() }
    val gyroChartDataX = remember { mutableListOf<Entry>() }
    val gyroChartDataY = remember { mutableListOf<Entry>() }
    val gyroChartDataZ = remember { mutableListOf<Entry>() }

    // 센서 값 업데이트
    LaunchedEffect(timerValue, accXValue, accYValue, accZValue, gyroXValue, gyroYValue, gyroZValue) {
        val sensorData = SensorData(
            time = System.currentTimeMillis(),
            accX = accXValue ?: 0f,
            accY = accYValue ?: 0f,
            accZ = accZValue ?: 0f,
            gyroX = gyroXValue ?: 0f,
            gyroY = gyroYValue ?: 0f,
            gyroZ = gyroZValue ?: 0f
        )

        if (isRecording) {
            viewModel.saveSensorData(sensorData)
        }

        val time = (timerValue ?: 0L) / 1000f // timerValue를 Float으로 변환
        updateChartData(accChartDataX, accXValue, time)
        updateChartData(accChartDataY, accYValue, time)
        updateChartData(accChartDataZ, accZValue, time)
        updateChartData(gyroChartDataX, gyroXValue, time)
        updateChartData(gyroChartDataY, gyroYValue, time)
        updateChartData(gyroChartDataZ, gyroZValue, time)

        val (newVelV, newDect) = fallDetection(
            accXValue ?: 0f, accYValue ?: 0f, accZValue ?: 0f,
            gyroXValue ?: 0f, gyroYValue ?: 0f, gyroZValue ?: 0f, velV
        )
        velV = newVelV
        dect = newDect

        if (dect) reset=false
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.secondary)
        .padding(16.dp)
    ) {
        Row(
        ){
            Card(
                modifier = Modifier.fillMaxWidth(0.5f).weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Parameter", style = MaterialTheme.typography.bodyLarge)
                    Text("Timer: ${timerValue ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Acceleration X: ${accXValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Acceleration Y: ${accYValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Acceleration Z: ${accZValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Gyroscope X: ${gyroXValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Gyroscope Y: ${gyroYValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Gyroscope Z: ${gyroZValue?.let { "%.2f".format(it) } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Current Velocity", style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.2f".format(velV)} m/s", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { reset = true },
                    colors = if (!reset) CardDefaults.cardColors(containerColor = Color.Red)
                    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Fall Detected", style = MaterialTheme.typography.bodyLarge)
                        Text("${if (reset) "No" else "Yes"}")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Accelerometer", accChartDataX, accChartDataY, accChartDataZ)
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Gyroscope", gyroChartDataX, gyroChartDataY, gyroChartDataZ)
        Button(onClick = {
            isRecording = !isRecording
            if (isRecording) {
                // 데이터 기록을 시작합니다. 이제 LaunchedEffect가 데이터를 저장하기 시작합니다.
                Log.d("MeasurementScreen", "Recording started")
            } else {
                // 데이터 기록을 중지합니다. LaunchedEffect는 더 이상 데이터를 저장하지 않습니다.
                // 저장된 데이터를 CSV 파일로 내보냅니다.
                viewModel.stopRecordingAndExportToCSV(context) { filePath ->
                    Toast.makeText(context, "Data saved to $filePath", Toast.LENGTH_LONG).show()
                    Log.d("MeasurementScreen", "Sensor data saved: $filePath")
                }
            }
        }) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
        Button(onClick = { viewModel.fetchAndLogSensorData() }) {
            Text("Log Sensor Data")
        }
    }
}
