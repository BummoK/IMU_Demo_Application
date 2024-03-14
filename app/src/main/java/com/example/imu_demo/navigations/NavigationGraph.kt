package com.example.imu_demo.navigations

import WelcomeScreen
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.imu_demo.presentation.AnalysisScreen
import com.example.imu_demo.presentation.MeasurementBuiltInScreen
import com.example.imu_demo.util.BluetoothViewModel
import com.example.imu_demo.presentation.MeasurementScreen
import com.example.imu_demo.presentation.ScanScreen
import com.example.imu_demo.presentation.SensorChoice
import com.example.imu_demo.presentation.currentChoiceState
import com.example.imu_demo.util.AnalysisViewModel
import com.example.imu_demo.util.BuiltInViewModel

@Composable
fun NavigationGraph(navController: NavHostController) {
    val bluetoothViewModel = hiltViewModel<BluetoothViewModel>()
    val analysisViewModel = hiltViewModel<AnalysisViewModel>()
    val sensorViewModel = hiltViewModel<BuiltInViewModel>()
    val state by bluetoothViewModel.state.collectAsState()

    val selectedSensor = when (currentChoiceState.value) {
        SensorChoice.SENSOR_1 -> 0
        SensorChoice.SENSOR_2 -> 1
        SensorChoice.SENSOR_3 -> 2
    }

    NavHost(
        navController = navController,
        startDestination = "Welcome"

    ){
        composable("Welcome") { // 스플래시 화면 추가
            WelcomeScreen(navController)
        }

        composable(BottomNavItem.Scan.screenRoute){
            ScanScreen(
                state = state,
                onStartScan = bluetoothViewModel::startScan,
                onStopScan = bluetoothViewModel::stopScan,
                onDeviceClick = bluetoothViewModel::connectToDevice,
                onDeviceDisconnect = bluetoothViewModel::disconnectFromDevice,
                currentChoiceState = currentChoiceState
                )
        }

        composable(BottomNavItem.Measurement.screenRoute){
            if (selectedSensor == 0) {
                MeasurementBuiltInScreen(
                    bluetoothViewModel = bluetoothViewModel
                )
            } else {
                MeasurementScreen(
                    bluetoothViewModel = bluetoothViewModel
                )
            }
        }

        composable(BottomNavItem.Analysis.screenRoute){
            AnalysisScreen(viewModel = analysisViewModel)
        }
    }
}

@Composable
fun BottomNavigation(navController: NavHostController
) {
    val items = listOf<BottomNavItem>(
        BottomNavItem.Scan,
        BottomNavItem.Measurement,
        BottomNavItem.Analysis
    )
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF3F414E)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(

                selected = currentRoute == item.screenRoute,
                onClick = {
                    navController.navigate(item.screenRoute) {
                        navController.graph.startDestinationRoute?.let {
                            popUpTo(it) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = stringResource(id = item.title),
                        modifier = Modifier
                            .width(25.dp)
                            .height(25.dp)
                    )
                },
                label = { Text(stringResource(id = item.title), fontSize = 9.sp) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun NavHostController.currentRoute(): String? {
    val navBackStackEntry by currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}