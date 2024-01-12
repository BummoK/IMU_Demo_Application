package com.example.imu_demo.util

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MotionRecogition(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var modelOutputClasses: Int = 0

    fun init() {
        try {
            val model = loadModelFile(MODEL_NAME)
            model.order(ByteOrder.nativeOrder())
            interpreter = Interpreter(model)

            initModelShape()
        } catch (e: IOException) {
            Log.d("TensorFlow Init", "Initiation Fail")
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val am = context.assets
        val afd = am.openFd(modelName)
        FileInputStream(afd.fileDescriptor).use { fis ->
            val fc = fis.channel
            val startOffset = afd.startOffset
            val declaredLength = afd.declaredLength
            return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    private fun initModelShape() {
        val outputTensor = interpreter?.getOutputTensor(0)
        val outputShape = outputTensor?.shape()
        modelOutputClasses = outputShape?.get(1) ?: 0
    }

    fun classify(inputData: Array<FloatArray>): Map<Int, Float> {
        val inputBuffer = convertToByteBuffer(inputData)

        val result = Array(1) { FloatArray(modelOutputClasses) }
        interpreter?.run(inputBuffer, result)

        val probabilities = getProbabilities(result[0])

        // 로그로 확률 값 출력
        Log.d("MotionRecognition", "Result: ${result[0].joinToString(", ")}")

        return probabilities
    }

    private fun convertToByteBuffer(data: Array<FloatArray>): ByteBuffer? {
        val byteBuffer = ByteBuffer.allocateDirect(data.size * data[0].size * 8)
        byteBuffer.order(ByteOrder.nativeOrder())
        for (i in data.indices) {
            for (j in data[i].indices) {
                byteBuffer.putFloat(data[i][j])
            }
        }
        return byteBuffer
    }

    private fun getProbabilities(array: FloatArray): Map<Int, Float> {
        return array.indices.associateWith { array[it] }
    }

    fun finish() {
        interpreter?.close()
    }

    companion object {
        private const val MODEL_NAME = "model01.tflite"
    }
}