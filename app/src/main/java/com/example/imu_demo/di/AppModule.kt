package com.example.imu_demo.di

import android.content.Context
import androidx.room.Room
import com.example.imu_demo.data.ble.AndroidBluetoothController
import com.example.imu_demo.data.dao.AppDatabase
import com.example.imu_demo.data.dao.SensorDataDao
import com.example.imu_demo.data.dao.SensorDataDaoSWRaw
import com.example.imu_demo.domain.BluetoothController
import com.example.imu_demo.util.BluetoothViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "IMU_database_ver01"
        ).build()
    }

    @Provides
    fun provideSensorDataDao(appDatabase: AppDatabase): SensorDataDao {
        return appDatabase.sensorDataDao()
    }

    @Provides
    fun provideSensorDataDaoSWRaw(appDatabase: AppDatabase): SensorDataDaoSWRaw {
        return appDatabase.sensorDataDaoSWRaw()
    }

    @Provides
    @Singleton
    fun provideBluetoothController(
        @ApplicationContext context: Context,
    ): BluetoothController {
        return AndroidBluetoothController(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothViewModel(
        @ApplicationContext context: Context,
        bluetoothController: BluetoothController,
        sensorDataDao: SensorDataDao,
        sensorDataDaoSWRaw: SensorDataDaoSWRaw,
        appDatabase: AppDatabase
    ): BluetoothViewModel {
        return BluetoothViewModel(context, bluetoothController, sensorDataDao, sensorDataDaoSWRaw, appDatabase)
    }
}