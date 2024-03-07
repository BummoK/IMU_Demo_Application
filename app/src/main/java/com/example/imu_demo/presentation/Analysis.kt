package com.example.imu_demo.presentation

import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.imu_demo.R
import com.example.imu_demo.util.AccChartComposable
import com.example.imu_demo.util.AnalysisViewModel
import com.example.imu_demo.util.CustomTable
import com.example.imu_demo.util.GyroChartComposable
import com.example.imu_demo.util.MotionChartComposable
import com.example.imu_demo.util.RiskChartComposable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    val context = LocalContext.current
    val selectFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { selectedFileUri ->
                Log.d("FilePicker", "Selected file URI: $selectedFileUri")
                viewModel.loadFileData(selectedFileUri, context.contentResolver)
            }
        }
    }

    // StateFlow를 collectAsState를 통해 Composable에 연결
    val selectedFileName by viewModel.selectedFileName.collectAsState()

    val isButtonClicked = remember { mutableStateOf(false) }

    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = if (darkTheme) {
        // 다크 모드일 때 사용할 색상
        Color(0xFF303030) // 예시 색상, 필요에 따라 변경 가능
    } else {
        // 라이트 모드일 때 사용할 색상
        Color(0xFFE6E6FA)
    }

    val textColor = if (darkTheme) {
        // 다크 모드일 때 사용할 색상
        Color.White
    } else {
        // 라이트 모드일 때 사용할 색상
        Color.Black
    }

    Column {
        TextField(
            value = selectedFileName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = {
                IconButton(onClick = {
                    viewModel.scanMediaFiles(context, Environment.getExternalStorageDirectory().absolutePath)
                    filePicker.launch(selectFileIntent)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_insert_file),
                        contentDescription = "Button for file load",
                    )
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                containerColor = backgroundColor, // 연분홍색 배경
                disabledTextColor = textColor, // 비활성화 시 검은색 텍스트
            ),
            placeholder = {
                Text("File name")
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )

        val sensorDataList by viewModel.sensorDataFlow.collectAsState(initial = emptyList())

        if (sensorDataList.isNotEmpty()) {
            Text("  Acceleration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            AccChartComposable(sensorDataList = sensorDataList)
            Text("  Angular velocity", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            GyroChartComposable(sensorDataList = sensorDataList)
            Text("  Motion", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            MotionChartComposable(sensorDataList = sensorDataList)
            CustomTable()
            Text("  Risk", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            RiskChartComposable(sensorDataList = sensorDataList)
        }
        IconButton(
            onClick = {
                // 버튼 클릭 시 parseIMUData 호출
                isButtonClicked.value = true
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_insert_file), // 아이콘 리소스
                contentDescription = "Button for parseIMUData" // 아이콘 설명
            )
        }
        val data:ByteArray = byteArrayOf(
            83, 68, 113, 0, -7, 3, 69, 2, 25, -1, -18, -34, -14, -1, 7, 0, 5, 0, -6, 3, 69, 2,
            -2, -2, 34, -33, -23, -1, 4, 0, 7, 0, -5, 3, 40, 2, -10, -2, 35, -33, -20, -1, 14,
            0, -7, -1, -4, 3, 28, 2, 10, -1, 8, -33, -19, -1, 22, 0, 7, 0, -3, 3, 65, 2, 42, -1,
            -5, -34, -31, -1, 7, 0, 17, 0, -2, 3, 48, 2, 43, -1, 22, -33, -24, -1, 17, 0, 0, 0,
            -1, 3, 59, 2, 21, -1, -10, -34, -18, -1, 5, 0, 13, 0, 0, 4, 57, 2, 35, -1, 35, -33,
            -20, -1, 10, 0, 9, 0, 1, 4, 74, 2, 24, -1, -6, -34, -22, -1, 10, 0, 13, 0
        )

        if (isButtonClicked.value) {
            // 버튼이 클릭되었으므로 false로 재설정
            isButtonClicked.value = false
            viewModel.parseIMUData(data) // 이곳에 parseIMUData 호출하는 코드 추가
        }
    }
}