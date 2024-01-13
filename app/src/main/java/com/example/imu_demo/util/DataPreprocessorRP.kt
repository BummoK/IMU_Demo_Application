package com.example.imu_demo.util

class DataPreprocessorRP {
    companion object {
        // 모델 입력 데이터 준비
        fun prepareModelInputRP(buffer: Array<MutableList<Float>>): Array<FloatArray> {
            // (60, 8) 형태의 2차원 배열 생성
            val inputData = Array(15) { FloatArray(8) }

            // 각 시간 단계에서 모든 센서 값 추출
            for (i in 0 until 15) {
                for (j in buffer.indices) {
                    inputData[i][j] = buffer[j][i]
                }
            }

//            inputData.forEachIndexed { index, data ->
//                Log.d("ModelInputData", "Time step $index: ${data.joinToString(", ")}")
//            }

            return inputData
        }

        // 버퍼 쉬프팅 함수
        fun shiftBufferRP(buffer: Array<MutableList<Float>>) {
            val numberOfElementsToRemove = 5 // 한 번에 제거할 요소의 수

            for (list in buffer) {
                // 리스트의 크기가 15을 초과하면, 가장 오래된 5개 데이터를 제거합니다.
                if (list.size > 15) {
                    repeat(numberOfElementsToRemove) {
                        list.removeAt(0)
                    }
                }
            }
        }
    }
}