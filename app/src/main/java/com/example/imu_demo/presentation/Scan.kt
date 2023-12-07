package com.example.imu_demo.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext

import com.example.imu_demo.domain.BluetoothDevice
import com.example.imu_demo.ui.theme.IMU_DemoTheme

@Composable
fun ScanScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onDeviceDisconnect: (BluetoothDevice) -> Unit
) {
    val applicationContext = LocalContext.current

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
                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClickConnect = onDeviceClick, // 연결
                    onClickDisconnect = onDeviceDisconnect, // 연결 해제
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
            ) {
                BluetoothDeviceList(
                    pairedDevices = state.pairedDevices,
                    scannedDevices = state.scannedDevices,
                    onClickConnect = onDeviceClick, // 연결
                    onClickDisconnect = onDeviceDisconnect, // 연결 해제
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
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClickConnect: (BluetoothDevice) -> Unit,
    onClickDisconnect: (BluetoothDevice) -> Unit, // 추가
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "Paired Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(pairedDevices) {device ->
            val textColor = if (device.isConnected) Color.Red else Color.Black
            Text(
                text = device.name ?:"(No name)",
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

        item {
            Text(
                text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(scannedDevices) {device ->
            val textColor = if (device.isConnected) Color.Red else Color.Black
            Text(text = device.name ?:"(No name)",
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

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    val viewModel = hiltViewModel<BluetoothViewModel>()
    val state by viewModel.state.collectAsState()
    IMU_DemoTheme {
        ScanScreen(
            state = state,
            onStartScan = viewModel::startScan,
            onStopScan = viewModel::stopScan,
            onDeviceClick = viewModel::connectToDevice,
            onDeviceDisconnect = viewModel::disconnectFromDevice // 연결 해제 콜백 추가
        )
    }
}