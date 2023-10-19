package com.example.imu_demo

sealed class BottomNavItem(
    val title: Int, val icon: Int, val screenRoute: String
) {
    object Scan : BottomNavItem(R.string.text_scan, R.drawable.ic_scan, SCAN)
    object Measurement : BottomNavItem(R.string.text_measurement, R.drawable.ic_measurement, MEASUREMENT)
    object Analysis : BottomNavItem(R.string.text_analysis, R.drawable.ic_analysis, ANALYSIS)
}
