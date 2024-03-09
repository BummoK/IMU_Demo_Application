package com.example.imu_demo.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.imu_demo.data.dao.SensorData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.patrykandpatrick.vico.compose.component.textComponent

@Composable
fun LineChartComposable(
    title: String,
    dataX: List<Entry>,
    dataY: List<Entry>,
    dataZ: List<Entry>
) {
    val darkTheme = isSystemInDarkTheme()
    var modeTextColor = if (darkTheme) Color.White else Color.Black

    val dataSetX = LineDataSet(dataX, "X").apply {
        color = Color.Red.toArgb()
        setDrawCircles(false)
        setDrawValues(false)
    }
    val dataSetY = LineDataSet(dataY, "Y").apply {
        color = Color.Green.toArgb()
        setDrawCircles(false)
        setDrawValues(false)
    }
    val dataSetZ = LineDataSet(dataZ, "Z").apply {
        color = Color.Blue.toArgb()
        setDrawCircles(false)
        setDrawValues(false)
    }

    // 가속도 데이터에 대한 차트
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(10.dp)
            ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                factory = { context ->
                    LineChart(context).apply {
                        data = LineData(dataSetX, dataSetY, dataSetZ)

                        // 축 설정
                        axisRight.isEnabled = false
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            textColor = modeTextColor.toArgb()
                        }
                        axisLeft.apply {
                            textColor = modeTextColor.toArgb()
                        }
                        legend.apply {
                            textColor = modeTextColor.toArgb()
                        }
                        description.isEnabled = false
                        setTouchEnabled(true)
                        setPinchZoom(true)
                    }
                },
                update = { lineChart ->
                    // X축 데이터셋 설정
                    lineChart.data = LineData(dataSetX, dataSetY, dataSetZ)
                    lineChart.notifyDataSetChanged()
                    lineChart.invalidate()
                }
            )
        }
    }
}

fun updateChartData(chartData: MutableList<Entry>, value: Float?, time: Float) {
    value?.let {
        chartData.add(Entry(time, it)) // timerValue를 X축 값으로 사용
    }
    if (chartData.size > 50) {
        chartData.removeFirst()
    }
}

@Composable
fun AccChartComposable(sensorDataList: List<SensorData>) {
    val darkTheme = isSystemInDarkTheme()
    var modeTextColor = if (darkTheme) Color.White else Color.Black

    val entriesX = sensorDataList.mapIndexed { index, data -> Entry(index.toFloat(), data.accX) }
    val dataSetX = LineDataSet(entriesX, "X Axis").apply {
        color = android.graphics.Color.RED
        setDrawCircles(false)
        setDrawValues(false)
    }

    // Y축 데이터
    val entriesY = sensorDataList.mapIndexed { index, data -> Entry(index.toFloat(), data.accY) }
    val dataSetY = LineDataSet(entriesY, "Y Axis").apply {
        color = android.graphics.Color.GREEN
        setDrawCircles(false)
        setDrawValues(false)
    }

    // Z축 데이터
    val entriesZ = sensorDataList.mapIndexed { index, data -> Entry(index.toFloat(), data.accZ) }
    val dataSetZ = LineDataSet(entriesZ, "Z Axis").apply {
        color = android.graphics.Color.BLUE
        setDrawCircles(false)
        setDrawValues(false)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        factory = { context ->
            LineChart(context).apply {
                // LineChart에 데이터 세트 추가
                this.data = LineData(dataSetX, dataSetY, dataSetZ)
//                this.setVisibleXRangeMaximum(20f)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = modeTextColor.toArgb()
                }
                axisLeft.textColor = modeTextColor.toArgb()
                axisRight.apply {
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                }
                legend.textColor = modeTextColor.toArgb()
                setTouchEnabled(true)
                setPinchZoom(true)
                description.apply {
                    text = "unit: g"
                    textColor = modeTextColor.toArgb()
                    textSize = 10f
                }
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = false
                invalidate()
            }
        },
        update = { lineChart ->
            lineChart.data = LineData(dataSetX, dataSetY, dataSetZ)
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}



@Composable
fun GyroChartComposable(sensorDataList: List<SensorData>) {
    val darkTheme = isSystemInDarkTheme()
    var modeTextColor = if (darkTheme) Color.White else Color.Black

    val radianFactor = kotlin.math.PI / 180

    val entriesX = sensorDataList.mapIndexed { index, data ->
        Entry(index.toFloat(), data.gyroX * radianFactor.toFloat()) }
    val dataSetX = LineDataSet(entriesX, "X Axis").apply {
        color = android.graphics.Color.RED
        setDrawCircles(false)
        setDrawValues(false)
    }

    val entriesY = sensorDataList.mapIndexed { index, data ->
        Entry(index.toFloat(), data.gyroY * radianFactor.toFloat()) }
    val dataSetY = LineDataSet(entriesY, "Y Axis").apply {
        color = android.graphics.Color.GREEN
        setDrawCircles(false)
        setDrawValues(false)
    }

    val entriesZ = sensorDataList.mapIndexed { index, data ->
        Entry(index.toFloat(), data.gyroZ * radianFactor.toFloat()) }
    val dataSetZ = LineDataSet(entriesZ, "Z Axis").apply {
        color = android.graphics.Color.BLUE
        setDrawCircles(false)
        setDrawValues(false)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        factory = { context ->
            LineChart(context).apply {
                // LineChart에 데이터 세트 추가
                this.data = LineData(dataSetX, dataSetY, dataSetZ)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = modeTextColor.toArgb()
                }
                axisLeft.apply {
                    textColor = modeTextColor.toArgb()
                    textSize = 8f
                }
                axisRight.apply {
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                }
                legend.textColor = modeTextColor.toArgb()
                setTouchEnabled(true)
                setPinchZoom(true)
                description.apply {
                    text = "unit: rad/s"
                    textColor = modeTextColor.toArgb()
                    textSize = 10f
                }
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = false
                invalidate()
            }
        },
        update = { lineChart ->
            lineChart.data = LineData(dataSetX, dataSetY, dataSetZ)
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun MotionChartComposable(sensorDataList: List<SensorData>) {
    val darkTheme = isSystemInDarkTheme()
    var modeTextColor = if (darkTheme) Color.White else Color.Black

    val entry = sensorDataList.mapIndexed { index, data -> Entry(index.toFloat(), data.motion.toFloat()) }
    val dataSet = LineDataSet(entry, "Motion").apply {
        color = android.graphics.Color.CYAN
        setDrawCircles(false)
        setDrawValues(false)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        factory = { context ->
            LineChart(context).apply {
                // LineChart에 데이터 세트 추가
                this.data = LineData(dataSet)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = modeTextColor.toArgb()
                }
                axisLeft.apply {
                    textColor = modeTextColor.toArgb()
                    isAutoScaleMinMaxEnabled = false
                    axisMinimum = -1f
                    axisMaximum = 6f
                    granularity = 1f
                }
                axisRight.apply {
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                }
                legend.textColor = modeTextColor.toArgb()
                setTouchEnabled(true)
                setPinchZoom(true)
                description.isEnabled = false
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = false
                invalidate()
            }
        },
        update = { lineChart ->
            lineChart.data = LineData(dataSet)
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun RiskChartComposable(sensorDataList: List<SensorData>) {
    val darkTheme = isSystemInDarkTheme()
    var modeTextColor = if (darkTheme) Color.White else Color.Black

    val entry = sensorDataList.mapIndexed { index, data -> Entry(index.toFloat(), data.risk) }
    val dataSet = LineDataSet(entry, "Risk").apply {
        color = android.graphics.Color.MAGENTA
        setDrawCircles(false)
        setDrawValues(false)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        factory = { context ->
            LineChart(context).apply {
                // LineChart에 데이터 세트 추가
                this.data = LineData(dataSet)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = modeTextColor.toArgb()
                }
                axisLeft.textColor = modeTextColor.toArgb()
                axisRight.apply {
                    setDrawLabels(false)
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                }
                legend.textColor = modeTextColor.toArgb()
                setTouchEnabled(true)
                setPinchZoom(true)
                description.apply {
                    text = "unit: g"
                    textColor = modeTextColor.toArgb()
                    textSize = 10f
                }
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = false
                invalidate()
            }
        },
        update = { lineChart ->
            lineChart.data = LineData(dataSet)
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun CustomTable() {
    val labels = listOf(
        "-1: Unknown", "0: Stand",
        "1: Sit to stand", "2: Walking",
        "3: Jump", "4: Forward fall",
        "5: Backward fall", "6: Lateral fall"
    )

    Column(modifier = Modifier.padding(5.dp)) {
        for (i in 0 until 2) {
            Row {
                for (j in 0 until 4) {
                    Text(
                        text = labels[i * 4 + j],
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp),
                        fontSize = 9.sp)
                }
            }
        }
    }
}