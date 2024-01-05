package com.example.imu_demo.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insert(sensorData: SensorData)

    @Query("SELECT * FROM SensorData")
    fun getAll(): Flow<List<SensorData>> // LiveData 대신 Flow를 반환

    @Query("DELETE from SensorData")
    suspend fun deleteAll()
}