package com.example.imu_demo.data

data class IMUResult(
    val temperature:Float,
    val humidity:Float,
    val connectionState: ConnectionState
)
