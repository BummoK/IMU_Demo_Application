package com.example.imu_demo.util

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt
data class FallDetectionResult(
    val newVelV: Double,
    val detect: Boolean,
    val acc: Float,
    val gyro: Float
)
fun fallDetection(accX: Float, accY: Float, accZ: Float,
                  gyroX: Float, gyroY: Float, gyroZ: Float,
                  velV: Double
):FallDetectionResult {
    val dt = 0.0083

    var accXRev = accX*9.8f
    var accYRev = accY*9.8f
    var accZRev = accZ*9.8f

    var acc = sqrt(accXRev.pow(2)+accYRev.pow(2)+accZRev.pow(2))
    var gyro = sqrt(gyroX.pow(2)+gyroY.pow(2)+gyroZ.pow(2))

    var roll = atan(accYRev/(sqrt(accXRev.pow(2)+accZRev.pow(2)))*180/ PI)
    var pitch = atan(accXRev/(sqrt(accZRev.pow(2)+accZRev.pow(2)))*180/ PI)

    var accV = acc - 9.8

    var newVelV = velV
    if (accV >= 0.24) {
        newVelV += accV * dt
    } else if (accV < 0.24) {
        newVelV *= 0.9
    }

    val detect = newVelV > 0.5

    return FallDetectionResult(newVelV, detect, acc, gyro)
}
