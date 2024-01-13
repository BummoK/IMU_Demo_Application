package com.example.imu_demo.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun LineChartComposable(
    title: String,
    dataX: List<Entry>,
    dataY: List<Entry>,
    dataZ: List<Entry>
) {
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
                        // 데이터 설정
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

                        data = LineData(dataSetX, dataSetY, dataSetZ)

                        // 축 설정
                        axisRight.isEnabled = false
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        description.isEnabled = false
                        setTouchEnabled(true)
                        setPinchZoom(true)
                    }
                },
                update = { lineChart ->
                    // X축 데이터셋 설정
                    val dataSetX = LineDataSet(dataX, "X Axis").apply {
                        color = Color.Red.toArgb()
                        setDrawCircles(false)
                        setDrawValues(false)
                        lineWidth = 2f
                        // 추가 설정...
                    }

                    // Y축 데이터셋 설정
                    val dataSetY = LineDataSet(dataY, "Y Axis").apply {
                        color = Color.Green.toArgb()
                        setDrawCircles(false)
                        setDrawValues(false)
                        lineWidth = 2f
                        // 추가 설정...
                    }

                    // Z축 데이터셋 설정
                    val dataSetZ = LineDataSet(dataZ, "Z Axis").apply {
                        color = Color.Blue.toArgb()
                        setDrawCircles(false)
                        setDrawValues(false)
                        lineWidth = 2f
                        // 추가 설정...
                    }

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
