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

@Dao
interface SensorDataDaoSW {
    @Insert
    suspend fun insert(sensorDataSW: SensorDataSW)

    @Query("SELECT * FROM SensorDataSW")
    fun getAll(): Flow<List<SensorDataSW>> // LiveData 대신 Flow를 반환

    @Query("DELETE from SensorDataSW")
    suspend fun deleteAll()
}

@Dao
interface SensorDataDaoSWRaw {
    @Insert
    suspend fun insert(sensorDataSWRaw: SensorDataSWRaw)

    @Query("SELECT * FROM SensorDataSWRaw")
    fun getAll(): Flow<List<SensorDataSWRaw>> // LiveData 대신 Flow를 반환

    @Query("DELETE from SensorDataSWRaw")
    suspend fun deleteAll()
}