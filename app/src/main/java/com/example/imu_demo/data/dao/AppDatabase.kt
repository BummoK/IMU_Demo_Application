package com.example.imu_demo.data.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SensorData::class, SensorDataSW::class, SensorDataSWRaw::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun sensorDataDaoSW(): SensorDataDaoSW
    abstract fun sensorDataDaoSWRaw(): SensorDataDaoSWRaw

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensor_data_database"
                ).fallbackToDestructiveMigration() // 파괴적 마이그레이션 허용
                    .build()
                INSTANCE = instance
                // 반환 값
                instance
            }
        }
    }
}