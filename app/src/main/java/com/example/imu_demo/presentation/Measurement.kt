package com.example.imu_demo.presentation

import android.content.ContentValues
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.MutableState
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
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.imu_demo.R
import com.example.imu_demo.util.BluetoothViewModel
import com.example.imu_demo.util.DataPreprocessorMR
import com.example.imu_demo.util.DataPreprocessorRP
import com.example.imu_demo.util.LineChartComposable
import com.example.imu_demo.util.MotionRecognition
import com.example.imu_demo.util.RiskPrediction
import com.example.imu_demo.util.fallDetection
import com.example.imu_demo.util.updateChartData

import com.example.imu_demo.data.dao.AppDatabase
import com.example.imu_demo.data.dao.CSVFileHandler
import com.example.imu_demo.data.dao.SensorData
import com.example.imu_demo.data.dao.SensorDataDao
import com.example.imu_demo.data.dao.SensorDataDaoSW
import com.example.imu_demo.data.dao.SensorDataDaoSWRaw
import com.example.imu_demo.data.dao.SensorDataSW
import com.example.imu_demo.data.dao.SensorDataSWRaw

import com.github.mikephil.charting.data.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    bluetoothViewModel: BluetoothViewModel = hiltViewModel()
) {
    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = if (darkTheme) {
        // 다크 모드일 때 사용할 색상
        Color(0xFF303030) // 예시 색상, 필요에 따라 변경 가능
    } else {
        // 라이트 모드일 때 사용할 색상
        Color(0xFFFFEBEE)
    }

    val context = LocalContext.current
//    var isRecording by remember { mutableStateOf(false) }
    val isRecording by bluetoothViewModel.isRecording.collectAsState()

    var firstTimeRecord by remember { mutableStateOf(true) }
    var isOnset by remember { mutableStateOf(true) }
    // Parameters of Fall Detection Algorithm
    var velV by remember { mutableDoubleStateOf(0.0) }
    var acc by remember { mutableFloatStateOf(0.0F) }
    var gyro by remember { mutableFloatStateOf(0.0F) }
    var dect by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(true) }
    var resetSW by remember { mutableStateOf(true) }
    var motionreset by remember { mutableStateOf(true) }
    var mcuFall by remember { mutableStateOf(false) }

    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }.build()

    val sensorDataBufferMR = remember { Array(8) { MutableList(60) { 0f } } }
    val sensorDataBufferRP = remember { Array(8) { MutableList(15) { 0f } } }
    val classifierMR = remember { MotionRecognition(context).apply { init() } }
    val classifierRP = remember { RiskPrediction(context).apply { init() } }

    var maxClassStateMR by remember { mutableIntStateOf(-1) } // 클래스의 초기값은 0
    var maxProbStateMR by remember { mutableFloatStateOf(0f) } // 확률의 초기값은 0f

    var maxFallClassStateMR by remember { mutableIntStateOf(-1) } // 클래스의 초기값은 0

    var maxClassStateRP by remember { mutableIntStateOf(0) } // 클래스의 초기값은 0
    var maxProbStateRP by remember { mutableFloatStateOf(0f) } // 확률의 초기값은 0f

    val motionName = when (maxClassStateMR) {
        0 -> "서있기"
        1 -> "앉았다 일어서기"
        2 -> "보행"
        3 -> "점프"
        4 -> "전방낙상"
        5 -> "후방낙상"
        6 -> "측방낙상"
        else -> "알 수 없음"
    }

    val fallMotionName = when (maxFallClassStateMR) {
        4 -> "전방낙상"
        5 -> "후방낙상"
        6 -> "측방낙상"
        else -> "알 수 없음"
    }

    val displayedMotion = if (motionreset) {
        when (maxClassStateMR) {
            0 -> "stand"
            1 -> "sit_to_stand"
            2 -> "walking"
            3 -> "jump"
            4 -> "forward_fall"
            5 -> "backward_fall"
            6 -> "lateral_fall"
            else -> "unknown"
        }
    } else {
        if (maxClassStateMR >= 4) {
            motionreset = false
            maxFallClassStateMR = maxClassStateMR
        }
        when (maxFallClassStateMR) {
            4 -> "forward_fall"
            5 -> "backward_fall"
            6 -> "lateral_fall"
            else -> "unknown"
        }
    }

    val imageResource01 = when (displayedMotion) {
        "stand" -> R.drawable.stand
        "sit_to_stand" -> R.drawable.sit_to_stand
        "walking" -> R.drawable.walking
        "jump" -> R.drawable.jump
        "forward_fall" -> R.drawable.forward_fall
        "backward_fall" -> R.drawable.backward_fall
        "lateral_fall" -> R.drawable.lateral_fall
        else -> R.drawable.unknown // default or unknown image
    }

    val imageResource02 = when {
        kotlin.math.abs(maxProbStateRP) < 25 -> R.drawable.safe
        kotlin.math.abs(maxProbStateRP) < 50 -> R.drawable.warning
        else -> R.drawable.danger
    }

    val timerValue by bluetoothViewModel.timerValue.collectAsState()
    val accXValue by bluetoothViewModel.accXValue.collectAsState()
    val accYValue by bluetoothViewModel.accYValue.collectAsState()
    val accZValue by bluetoothViewModel.accZValue.collectAsState()
    val gyroXValue by bluetoothViewModel.gyroXValue.collectAsState()
    val gyroYValue by bluetoothViewModel.gyroYValue.collectAsState()
    val gyroZValue by bluetoothViewModel.gyroZValue.collectAsState()
    val alarmInfoValue by bluetoothViewModel.alarmInfoValue.collectAsState()
    val rawDataString by bluetoothViewModel.rawDataString.collectAsState()
    val receivedDataSize by bluetoothViewModel.receivedDataSize.collectAsState()

    val mcuAlarm = when (alarmInfoValue) {
        0 -> "Nothing"
        2 -> "Low Battery"
        4 -> "Standby Battery"
        8 -> "Fall Detection"
        else -> "Unknown"
    }

    var fallMCU = 0

    val selectedSensor = when (currentChoiceState.value) {
        SensorChoice.SENSOR_1 -> 0
        SensorChoice.SENSOR_2 -> 1
        SensorChoice.SENSOR_3 -> 2
    }


    // 차트 데이터 리스트
    val accChartDataX = remember { mutableListOf<Entry>() }
    val accChartDataY = remember { mutableListOf<Entry>() }
    val accChartDataZ = remember { mutableListOf<Entry>() }
    val gyroChartDataX = remember { mutableListOf<Entry>() }
    val gyroChartDataY = remember { mutableListOf<Entry>() }
    val gyroChartDataZ = remember { mutableListOf<Entry>() }

    val appContext = LocalContext.current
    val db = AppDatabase.getDatabase(appContext)
    val sensorDataDao: SensorDataDao = db.sensorDataDao()
    val sensorDataDaoSW: SensorDataDaoSW = db.sensorDataDaoSW()
    val sensorDataDaoSWRaw: SensorDataDaoSWRaw = db.sensorDataDaoSWRaw()

    var sensorData: SensorData? = null
    var sensorDataSW: SensorDataSW? = null
    var sensorDataSWRaw: SensorDataSWRaw? = null

    val onExportComplete: (String) -> Unit = { fullPath ->
        // 파일 내보내기 완료 시 실행될 코드
        Log.d("ExportComplete", "File exported to: $fullPath")
    }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            Log.d("MeasurementScreen", "Recording started")
            firstTimeRecord = false
        } else {
            if (!firstTimeRecord){
                if (selectedSensor == 0 or 2) {
                    bluetoothViewModel.stopRecordingAndExportToCSV(
                        dao = sensorDataDao,
                        fileNamePrefix = "SensorData",
                        convertToCsvLine = { data ->
                            if (data is SensorData) {
                                "${data.time}, ${data.accX}, ${data.accY}, ${data.accZ}, ${data.gyroX}, ${data.gyroY}, ${data.gyroZ}, ${data.motion}, ${data.risk}"
                            } else ""
                        },
                        onExportComplete = onExportComplete
                    )
                    Toast.makeText(context, "Data saved to Download/Experiment/", Toast.LENGTH_LONG).show()
                } else if (selectedSensor == 1)
                {
                    bluetoothViewModel.stopRecordingAndExport()
                    Toast.makeText(context, "Data saved to Download/Experiment/", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Update the value of sensors
    LaunchedEffect(rawDataString) {

        if (selectedSensor==0 or 2)
        {
            sensorData = SensorData(
                time = System.currentTimeMillis(),
                accX = accXValue ?: 0f,
                accY = accYValue ?: 0f,
                accZ = accZValue ?: 0f,
                gyroX = gyroXValue ?: 0f,
                gyroY = gyroYValue ?: 0f,
                gyroZ = gyroZValue ?: 0f,
                motion = maxClassStateMR,
                risk = kotlin.math.abs(maxProbStateRP)
            )
        }
        else if (selectedSensor == 1)
        {
            fallMCU = if(alarmInfoValue==8) 1 else 0
            if (fallMCU==1) resetSW=false
        }



        if (isRecording) {
            if (selectedSensor == 0 or 2) {
                bluetoothViewModel.saveSensorData(sensorData, sensorDataDao)

            } else if (selectedSensor == 1)
            {
//                bluetoothViewModel.saveSensorData(sensorDataSWRaw, sensorDataDaoSWRaw)
            }
        }

        val time = (timerValue ?: 0L) / 1000f // timerValue를 Float으로 변환
        updateChartData(accChartDataX, accXValue, time)
        updateChartData(accChartDataY, accYValue, time)
        updateChartData(accChartDataZ, accZValue, time)
        updateChartData(gyroChartDataX, gyroXValue, time)
        updateChartData(gyroChartDataY, gyroYValue, time)
        updateChartData(gyroChartDataZ, gyroZValue, time)

        if (isOnset) {
            val (newVelV, newDect, newAcc, newGyro) = fallDetection(
                accXValue ?: 0f, accYValue ?: 0f, accZValue ?: 0f,
                gyroXValue ?: 0f, gyroYValue ?: 0f, gyroZValue ?: 0f, velV
            )
            velV = newVelV
            dect = newDect
            acc = newAcc
            gyro = newGyro

            if (dect) reset=false
            if (maxClassStateMR >= 4) {
                motionreset = false
                maxFallClassStateMR = maxClassStateMR
            }

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
//            Log.d("Buffer", "full")
                val modelInputDataMR = DataPreprocessorMR.prepareModelInputMR(sensorDataBufferMR)

                val classificationMR = classifierMR.classify(modelInputDataMR)

                // 가장 높은 확률을 가진 클래스 찾기
                val maxEntryMR = classificationMR.maxByOrNull { it.value }

                if (maxEntryMR != null) {
                    maxClassStateMR = maxEntryMR.key
                    maxProbStateMR = maxEntryMR.value
                } else {
                    maxClassStateMR = -1 // 기본값 할당
                    maxProbStateMR = 0f // 기본값 할당
                }
            }

            if (sensorDataBufferRP[0].size == 15) {
//            Log.d("Buffer", "full")
                val modelInputDataRP = DataPreprocessorRP.prepareModelInputRP(sensorDataBufferRP)


                val classificationRP = classifierRP.classify(modelInputDataRP)

                // 가장 높은 확률을 가진 클래스 찾기
                val maxEntryRP = classificationRP.maxByOrNull { it.value }

                if (maxEntryRP != null) {
                    maxClassStateRP = maxEntryRP.key
                    maxProbStateRP = maxEntryRP.value
                } else {
                    maxClassStateRP = 0 // 기본값 할당
                    maxProbStateRP = 0f // 기본값 할당
                }
            }
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
        .background(backgroundColor)
        .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(200.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(5.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text("Current Velocity", style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.2f".format(velV)} m/s", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    onClick = { reset = true },
                    colors = if (!reset) CardDefaults.cardColors(containerColor = Color.Red)
                    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text("Fall Detected", style = MaterialTheme.typography.bodyLarge)
                        Text(if (reset) "No" else "Yes", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    onClick = { resetSW = true },
                    colors = if (!resetSW) CardDefaults.cardColors(containerColor = Color.Red)
                    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Text("MCU Alarm", style = MaterialTheme.typography.bodyLarge)
                        Text(if (resetSW) "$alarmInfoValue: $mcuAlarm" else "8: Fall Detection", style = MaterialTheme.typography.bodySmall)
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
                onClick = { motionreset = true },
                colors = if (!motionreset) CardDefaults.cardColors(containerColor = Color.Red)
                else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(5.dp)
                ) {
                    Text("Current Motion", style = MaterialTheme.typography.bodyLarge)
                    Text("Class ${if (motionreset) "$maxClassStateMR: $motionName"
                    else "$maxFallClassStateMR: $fallMotionName"}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    // Box를 추가하여 이미지를 중앙에 배치
                    Box(
                        modifier = Modifier
                            .width(100.dp) // Box의 너비 설정
                            .height(75.dp) // Box의 높이 설정
                            .align(Alignment.CenterHorizontally) // Column 내에서 수평 중앙 정렬
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(imageResource01, imageLoader),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(), // 이미지를 Box의 크기에 맞춤
                            alignment = Alignment.Center // 이미지 내부에서의 중앙 정렬
                        )
                    }
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
                    modifier = Modifier.padding(5.dp)
                ) {
                    Text("Risk Prediction", style = MaterialTheme.typography.bodyLarge)
                    Text("Risk: ${"%.2f".format(kotlin.math.abs(maxProbStateRP))}",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    // Box를 추가하여 이미지를 중앙에 배치
                    Box(
                        modifier = Modifier
                            .width(100.dp) // Box의 너비 설정
                            .height(75.dp) // Box의 높이 설정
                            .align(Alignment.CenterHorizontally) // Column 내에서 수평 중앙 정렬
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(imageResource02, imageLoader),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(), // 이미지를 Box의 크기에 맞춤
                            alignment = Alignment.Center // 이미지 내부에서의 중앙 정렬
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Accelerometer", accChartDataX, accChartDataY, accChartDataZ)
        Spacer(modifier = Modifier.height(4.dp))
        LineChartComposable("Gyroscope", gyroChartDataX, gyroChartDataY, gyroChartDataZ)
        Spacer(modifier = Modifier.height(4.dp))
        Row{
            Card(
                modifier = Modifier.padding(5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ){
                RecordButton(isRecording = isRecording, onRecordClick = {
                    bluetoothViewModel.toggleRecording()
                })
            }
            Card(
                modifier = Modifier.padding(5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ){
                FullModeButton(isOnset = isOnset, onClick ={
                    isOnset = !isOnset
                })
            }
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

@Composable
fun FullModeButton(isOnset: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(id = if (isOnset) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24),
            contentDescription = if (isOnset) "Full mode" else "Limited mode",
            tint = if (isOnset) Color.Black else Color.Black
        )
    }
}

fun convertBytesToInt(byte1: Byte, byte2: Byte): Int {
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

    return signedIntValue
}