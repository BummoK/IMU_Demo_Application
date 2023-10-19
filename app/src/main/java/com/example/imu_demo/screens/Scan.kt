package com.example.imu_demo.screens

import android.app.Activity
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.imu_demo.MainActivity.Companion.BLUETOOTH_PERMISSION_CODE
import com.example.imu_demo.ui.theme.IMU_DemoTheme

@Composable
fun ScanScreen(navController: NavHostController) {
    val context = LocalContext.current

    /** 요청할 권한 **/
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    val launcherMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
        /** 권한 요청시 동의 했을 경우 **/
        if (areGranted) {
            Log.d("test5", "권한이 동의되었습니다.")
        }
        /** 권한 요청시 거부 했을 경우 **/
        else {
            Log.d("test5", "권한이 거부되었습니다.")
        }
    }

    fun checkAndRequestPermissions(
        context: Context,
        permissions: Array<String>,
        launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    ) {

        /** 권한이 이미 있는 경우 **/
        if (permissions.all {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            Log.d("test5", "권한이 이미 존재합니다.")
        }

        /** 권한이 없는 경우 **/
        else {
            launcher.launch(permissions)
            Log.d("test5", "권한을 요청하였습니다.")
        }
    }

    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var connecting by remember { mutableStateOf(false) }

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    if (bluetoothAdapter == null) {
        Text(text = "Bluetooth is not supported on this device")
        return
    }

    if (!bluetoothAdapter.isEnabled) {
        EnableBluetoothButton()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = {
            checkAndRequestPermissions(
                context,
                permissions,
                launcherMultiplePermissions
            )
        }) {
            Text(text = "권한 요청하기")
        }

        Button(
            onClick = {
                // Check Bluetooth permission before scanning
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Scanning for Bluetooth devices
                    devices = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
                } else {
                    // Request Bluetooth permission
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH),
                        BLUETOOTH_PERMISSION_CODE
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Scan for Devices")
        }

        Spacer(modifier = Modifier.height(16.dp))

        @Composable
        fun DeviceListItem(device: BluetoothDevice, selectedDevice: BluetoothDevice?, onItemSelected: (BluetoothDevice) -> Unit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onItemSelected(device)
                        }
                )
                if (selectedDevice == device) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        if (devices.isNotEmpty()) {
            LazyColumn {
                itemsIndexed(devices) { index, device ->
                    DeviceListItem(device = device, selectedDevice = selectedDevice) {
                        selectedDevice = it
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    connecting = true
                    // In a real app, you would handle Bluetooth connection here
                    // and navigate to the next screen upon successful connection.
                    navController.navigate("data")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(text = "Connect to ${selectedDevice?.name ?: "Unknown Device"}")
            }
        } else {
            Text(text = "No devices found")
        }
    }
}

@Composable
fun EnableBluetoothButton() {
    val enableBtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth is enabled, you can proceed with other actions
        } else {
            // The user didn't enable Bluetooth
        }
    }

    Button(
        onClick = {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "Enable Bluetooth")
    }
}


@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    val navController = rememberNavController()
    IMU_DemoTheme {
        ScanScreen(navController = navController)
    }
}