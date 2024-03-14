package com.example.imu_demo.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.imu_demo.R

import com.example.imu_demo.domain.BluetoothDevice
import com.example.imu_demo.util.BluetoothUiState
import com.example.imu_demo.util.BuiltInViewModel

enum class SensorChoice {
    SENSOR_1, SENSOR_2, SENSOR_3
}
val currentChoiceState: MutableState<SensorChoice> = mutableStateOf(SensorChoice.SENSOR_1)


@Composable
fun ScanScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onDeviceDisconnect: (BluetoothDevice) -> Unit,
    currentChoiceState: MutableState<SensorChoice>,
    sensorViewModel: BuiltInViewModel = hiltViewModel()
) {
    val applicationContext = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }

    val currentChoice = currentChoiceState.value // 현재 선택된 센서

    val wantedText = when (currentChoice) {
        SensorChoice.SENSOR_1 -> ""
        SensorChoice.SENSOR_2 -> "SW"
        SensorChoice.SENSOR_3 -> "IMU"
    }
    val accelerometerDetails by sensorViewModel.accelerometerDetails.collectAsState()
    val gyroscopeDetails by sensorViewModel.gyroscopeDetails.collectAsState()

    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = if (darkTheme) {
        // 다크 모드일 때 사용할 색상
        Color(0xFF303030) // 예시 색상, 필요에 따라 변경 가능
    } else {
        // 라이트 모드일 때 사용할 색상
        Color(0xFFFFEBEE)
    }

    LaunchedEffect(key1 = state.errorMessage) {
        state.errorMessage?.let {message ->
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(key1 = state.isConnected) {
        if(state.isConnected) {
            Toast.makeText(
                applicationContext,
                "You're connected!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    when {
        state.isConnecting -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ){
                CircularProgressIndicator()
                Text(text = "Connecting...")
            }
        }
        state.isConnected -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                BluetoothDeviceList(
//                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClickConnect = onDeviceClick, // 연결
                    onClickDisconnect = onDeviceDisconnect, // 연결 해제
                    wantedText = wantedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = onStartScan) {
                        Text(text = "Start scan")
                    }
                    Button(onClick = onStopScan) {
                        Text(text = "Stop scan")
                    }
                }
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {

                if (currentChoice == SensorChoice.SENSOR_1) {
                    Text(
                        "내장 센서 정보:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("Built In Accelerometer", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                            Text("$accelerometerDetails", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("Built In Gyroscope", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                            Text("$gyroscopeDetails", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    BluetoothDeviceList(
//                    pairedDevices = state.pairedDevices,
                        scannedDevices = state.scannedDevices,
                        onClickConnect = onDeviceClick, // 연결
                        onClickDisconnect = onDeviceDisconnect, // 연결 해제
                        wantedText = wantedText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CustomButton(
                        onClick = {
                            // 버튼 클릭 시 currentChoiceState 업데이트
                            currentChoiceState.value = when (currentChoiceState.value) {
                                SensorChoice.SENSOR_1 -> SensorChoice.SENSOR_2
                                SensorChoice.SENSOR_2 -> SensorChoice.SENSOR_3
                                SensorChoice.SENSOR_3 -> SensorChoice.SENSOR_1
                            }
                            Log.d("ScanScreen", "Current choice updated to: ${currentChoiceState.value}")
                        },
                        iconResId = R.drawable.ic_developer_board,
                        iconColor = when (currentChoice) {
                            SensorChoice.SENSOR_1 -> Color(0xFFE96982)
                            SensorChoice.SENSOR_2 -> Color(0xFFFFEBEE)
                            SensorChoice.SENSOR_3 -> Color(0xFFE96982)
                        },
                        text = when (currentChoice) {
                            SensorChoice.SENSOR_1 -> "내장IMU"
                            SensorChoice.SENSOR_2 -> "SW_airbag"
                            SensorChoice.SENSOR_3 -> "nano33BLE"
                        },
                        textColor = when (currentChoice) {
                            SensorChoice.SENSOR_1 -> Color(0xFFE96982)
                            SensorChoice.SENSOR_2 -> Color(0xFFFFEBEE)
                            SensorChoice.SENSOR_3 -> Color(0xFFE96982)
                        },
                        backgroundColor = when (currentChoice) {
                            SensorChoice.SENSOR_1 -> Color(0xFFFFEBEE)
                            SensorChoice.SENSOR_2 -> Color(0xFFE96982)
                            SensorChoice.SENSOR_3 -> Color(0xFFFFEBEE)
                        },
                        borderColor = Color(0xFFE96982),
                        contentColor = when (currentChoice) {
                            SensorChoice.SENSOR_1 -> Color(0xFFE96982)
                            SensorChoice.SENSOR_2 -> Color(0xFFFFEBEE)
                            SensorChoice.SENSOR_3 -> Color(0xFFE96982)
                        }
                    )
                    ScanButton(isScanning = isScanning, onScanClick = {
                        isScanning = !isScanning
                        if (isScanning) onStartScan() else onStopScan()
                    })
                }
            }
        }
    }
}

@Composable
fun ScanButton(isScanning: Boolean, onScanClick: () -> Unit) {
    IconButton(onClick = onScanClick) {
        Icon(
            painter = painterResource(id = if (isScanning) R.drawable.ic_ble_stop else R.drawable.ic_ble_scan),
            contentDescription = if (isScanning) "Stop Scanning" else "Start Scanning",
            tint = if (isScanning) Color.Black else Color(0xFF3D85C6)
        )
    }
}

@Composable
fun CustomButton(
    iconResId: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    iconColor: Color = Color.Black,
    borderColor: Color = Color.Gray,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color.Gray
) {
    androidx.compose.material.Button(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(ButtonDefaults.OutlinedBorderSize, borderColor),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null, // contentDescription을 설정하지 않음
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.button,
                color = textColor,
                modifier = Modifier.padding(start = 8.dp) // 아이콘과 텍스트 간격 조절
            )
        }
    }
}

@Composable
fun BluetoothDeviceList(
//    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClickConnect: (BluetoothDevice) -> Unit,
    onClickDisconnect: (BluetoothDevice) -> Unit, // 추가
    modifier: Modifier = Modifier,
    wantedText: String
) {
    LazyColumn(
        modifier = modifier
    ) {
//        item {
//            Text(
//                text = "Paired Devices",
//                fontWeight = FontWeight.Bold,
//                fontSize = 24.sp,
//                modifier = Modifier.padding(16.dp)
//            )
//        }

//        items(pairedDevices) {device ->
//            val textColor = if (device.isConnected) Color.Red else Color.Black
//            Text(
//                text = (device.name ?: "No name") + " " + "(${device.address})",
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable {
//                        if (device.isConnected) {
//                            onClickDisconnect(device) // 연결 해제
//                        } else {
//                            onClickConnect(device) // 연결
//                        }
//                    }
//                    .padding(16.dp),
//                color = textColor
//            )
//        }

        item {
            Text(
                text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(scannedDevices.filter { it.name?.contains(wantedText, ignoreCase = true) == true }) { device ->
            val textColor = if (device.isConnected) Color.Red else Color.Black
            Text(
                text = (device.name ?: "No name") + " " + "(${device.address})",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (device.isConnected) {
                            onClickDisconnect(device) // 연결 해제
                        } else {
                            onClickConnect(device) // 연결
                        }
                    }
                    .padding(16.dp),
                color = textColor
            )
        }
    }
}

class FakeBluetoothViewModel : ViewModel() {

    var state by mutableStateOf(BluetoothUiState()) // 초기 상태

    fun startScan() {
        // 가짜로 스캔을 시작하고 상태 업데이트
    }

    fun stopScan() {
        // 가짜로 스캔을 중지하고 상태 업데이트
    }

    fun connectToDevice(device: BluetoothDevice) {
        // 가짜로 장치를 연결하고 상태 업데이트
        val updatedPairedDevices = state.pairedDevices.map {
            if (it.address == device.address) {
                it.copy(isConnected = true)
            } else {
                it
            }
        }
        state = state.copy(pairedDevices = updatedPairedDevices)
    }

    fun disconnectFromDevice(device: BluetoothDevice) {
        // 가짜로 장치 연결을 해제하고 상태 업데이트
        val updatedPairedDevices = state.pairedDevices.map {
            if (it.address == device.address) {
                it.copy(isConnected = false)
            } else {
                it
            }
        }
        state = state.copy(pairedDevices = updatedPairedDevices)
    }
}

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    val fakeViewModel = FakeBluetoothViewModel() // 가짜 뷰모델 생성
    val state = fakeViewModel.state // 상태 가져오기
    CompositionLocalProvider(LocalContext provides LocalContext.current) {
        ScanScreen(
            state = state,
            onStartScan = fakeViewModel::startScan,
            onStopScan = fakeViewModel::stopScan,
            onDeviceClick = fakeViewModel::connectToDevice,
            onDeviceDisconnect = fakeViewModel::disconnectFromDevice,
            currentChoiceState = currentChoiceState
        )
    }
}