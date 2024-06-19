package com.example.imu_demo.util

interface DataCallback {
    fun onDataReceived(dataString: String, dataSize: Int)
}