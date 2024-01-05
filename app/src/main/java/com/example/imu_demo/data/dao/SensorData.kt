package com.example.imu_demo.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SensorData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: Long,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float
)