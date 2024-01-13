package com.example.imu_demo.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.imu_demo.R
import com.example.imu_demo.data.dao.SensorData
import com.example.imu_demo.util.BluetoothViewModel
import com.example.imu_demo.util.DataPreprocessorMR
import com.example.imu_demo.util.DataPreprocessorRP
import com.example.imu_demo.util.LineChartComposable
import com.example.imu_demo.util.MotionRecognition
import com.example.imu_demo.util.RiskPrediction
import com.example.imu_demo.util.fallDetection
import com.example.imu_demo.util.updateChartData

import com.github.mikephil.charting.data.*
import java.lang.Math.abs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    bluetoothViewModel: BluetoothViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    // 낙상검출 알고리즘 파라미터
    var velV by remember { mutableDoubleStateOf(0.0) }
    var acc by remember { mutableFloatStateOf(0.0F) }
    var gyro by remember { mutableFloatStateOf(0.0F) }
    var dect by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(true) }

    val sensorDataBufferMR = remember { Array(8) { MutableList(60) { 0f } } }
    val sensorDataBufferRP = remember { Array(8) { MutableList(15) { 0f } } }
    val classifierMR = remember { MotionRecognition(context).apply { init() } }
    val classifierRP = remember { RiskPrediction(context).apply { init() } }

    var maxClassStateMR by remember { mutableIntStateOf(0) } // 클래스의 초기값은 0
    var maxProbStateMR by remember { mutableFloatStateOf(0f) } // 확률의 초기값은 0f
    var maxClassStateRP by remember { mutableIntStateOf(0) } // 클래스의 초기값은 0
    var maxProbStateRP by remember { mutableFloatStateOf(0f) } // 확률의 초기값은 0f

    val motionName = when (maxClassStateMR) {
        1 -> "서있기"
        2 -> "앉았다 일어서기"
        3 -> "보행"
        4 -> "점프"
        5 -> "전방낙상"
        6 -> "후방낙상"
        7 -> "측방낙상"
        else -> "알 수 없음"
    }


    val timerValue by bluetoothViewModel.timerValue.collectAsState()
    val accXValue by bluetoothViewModel.accXValue.collectAsState()
    val accYValue by bluetoothViewModel.accYValue.collectAsState()
    val accZValue by bluetoothViewModel.accZValue.collectAsState()
    val gyroXValue by bluetoothViewModel.gyroXValue.collectAsState()
    val gyroYValue by bluetoothViewModel.gyroYValue.collectAsState()
    val gyroZValue by bluetoothViewModel.gyroZValue.collectAsState()

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
            bluetoothViewModel.saveSensorData(sensorData)
        }

        val time = (timerValue ?: 0L) / 1000f // timerValue를 Float으로 변환
        updateChartData(accChartDataX, accXValue, time)
        updateChartData(accChartDataY, accYValue, time)
        updateChartData(accChartDataZ, accZValue, time)
        updateChartData(gyroChartDataX, gyroXValue, time)
        updateChartData(gyroChartDataY, gyroYValue, time)
        updateChartData(gyroChartDataZ, gyroZValue, time)

        val (newVelV, newDect, newAcc, newGyro) = fallDetection(
            accXValue ?: 0f, accYValue ?: 0f, accZValue ?: 0f,
            gyroXValue ?: 0f, gyroYValue ?: 0f, gyroZValue ?: 0f, velV
        )
        velV = newVelV
        dect = newDect
        acc = newAcc
        gyro = newGyro

        if (dect) reset=false

        // 버퍼에 데이터 추가
        val sensorValues = arrayOf(accXValue, accYValue, accZValue,
            gyroXValue, gyroYValue, gyroZValue, acc, gyro)

        for (i in sensorValues.indices) {
            sensorDataBufferMR[i].add(sensorValues[i] ?: 0f)
            sensorDataBufferRP[i].add(sensorValues[i] ?: 0f)
        }
        DataPreprocessorMR.shiftBufferMR(sensorDataBufferMR)
        DataPreprocessorRP.shiftBufferRP(sensorDataBufferRP)


        // 버퍼가 가득 찼을 때 처리
        if (sensorDataBufferMR[0].size == 60) {
            Log.d("Buffer", "full")
            val modelInputDataMR = DataPreprocessorMR.prepareModelInputMR(sensorDataBufferMR)


            val classificationMR = classifierMR.classify(modelInputDataMR)

            // 가장 높은 확률을 가진 클래스 찾기
            val maxEntryMR = classificationMR.maxByOrNull { it.value }

            if (maxEntryMR != null) {
                maxClassStateMR = maxEntryMR.key
                maxProbStateMR = maxEntryMR.value
                Log.d("MRResult", "success")
            } else {
                maxClassStateMR = 0 // 기본값 할당
                maxProbStateMR = 0f // 기본값 할당
                Log.d("MRResult", "Fail")
            }

            // 분류 결과 로그로 출력
//            Log.d("MeasurementScreen", "Class: $maxClassState, Probability: $maxProbState")

        }

        if (sensorDataBufferRP[0].size == 15) {
            Log.d("Buffer", "full")
            val modelInputDataRP = DataPreprocessorRP.prepareModelInputRP(sensorDataBufferRP)


            val classificationRP = classifierRP.classify(modelInputDataRP)

            // 가장 높은 확률을 가진 클래스 찾기
            val maxEntryRP = classificationRP.maxByOrNull { it.value }

            if (maxEntryRP != null) {
                maxClassStateRP = maxEntryRP.key
                maxProbStateRP = maxEntryRP.value
                Log.d("RPResult", "success")
            } else {
                maxClassStateRP = 0 // 기본값 할당
                maxProbStateRP = 0f // 기본값 할당
                Log.d("RPResult", "Fail")
            }

            // 분류 결과 로그로 출력
//            Log.d("MeasurementScreen", "Class: $maxClassState, Probability: $maxProbState")

        }
    }
    DisposableEffect(Unit) {
        onDispose {
            classifierMR.finish()
            classifierRP.finish()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFFFEBEE))
        .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .weight(1f),
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
                    modifier = Modifier.fillMaxWidth().height(81.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Current Velocity", style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.2f".format(velV)} m/s", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(81.dp),
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
        Row{
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Current Motion", style = MaterialTheme.typography.bodyLarge)
                    Text("Class $maxClassStateMR: $motionName", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Risk Prediction", style = MaterialTheme.typography.bodyLarge)
                    Text("Risk: ${"%.2f".format(kotlin.math.abs(maxProbStateRP))}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Accelerometer", accChartDataX, accChartDataY, accChartDataZ)
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Gyroscope", gyroChartDataX, gyroChartDataY, gyroChartDataZ)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.padding(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ){
            RecordButton(isRecording = isRecording, onRecordClick = {
                isRecording = !isRecording
                if (isRecording) {
                    // 데이터 기록을 시작합니다. 이제 LaunchedEffect가 데이터를 저장하기 시작합니다.
                    Log.d("MeasurementScreen", "Recording started")
                } else {
                    // 데이터 기록을 중지합니다. LaunchedEffect는 더 이상 데이터를 저장하지 않습니다.
                    // 저장된 데이터를 CSV 파일로 내보냅니다.
                    bluetoothViewModel.stopRecordingAndExportToCSV(context) { filePath ->
                        Toast.makeText(context, "Data saved to $filePath", Toast.LENGTH_LONG).show()
                        Log.d("MeasurementScreen", "Sensor data saved: $filePath")
                    }
                }
            })
        }
    }
}

@Composable
fun RecordButton(isRecording: Boolean, onRecordClick: () -> Unit) {
    IconButton(onClick = onRecordClick) {
        Icon(
            painter = painterResource(id = if (isRecording) R.drawable.ic_stop else R.drawable.ic_record),
            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
            tint = if (isRecording) Color.Black else Color.Red
        )
    }
}