package com.lemarc.sofia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lemarc.sofia.data.local.B1610PointEntity
import com.lemarc.sofia.data.local.ProductionPointEntity
import com.lemarc.sofia.data.local.RemitNoticeEntity
import com.lemarc.sofia.data.local.WeatherPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SofiaDao {

    @Query("SELECT * FROM production_points WHERE isTestMode = :testMode ORDER BY timeFrom ASC")
    fun getProductionPoints(testMode: Boolean): Flow<List<ProductionPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionPoints(points: List<ProductionPointEntity>)

    @Query("DELETE FROM production_points WHERE isTestMode = :testMode")
    suspend fun clearProductionPoints(testMode: Boolean)

    @Transaction
    suspend fun refreshProductionPoints(testMode: Boolean, points: List<ProductionPointEntity>) {
        clearProductionPoints(testMode)
        insertProductionPoints(points)
    }

    @Query("SELECT * FROM b1610_points WHERE isTestMode = :testMode ORDER BY timeFrom ASC")
    fun getB1610Points(testMode: Boolean): Flow<List<B1610PointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertB1610Points(points: List<B1610PointEntity>)

    @Query("DELETE FROM b1610_points WHERE isTestMode = :testMode")
    suspend fun clearB1610Points(testMode: Boolean)

    @Transaction
    suspend fun refreshB1610Points(testMode: Boolean, points: List<B1610PointEntity>) {
        clearB1610Points(testMode)
        insertB1610Points(points)
    }

    @Query("SELECT * FROM weather_points ORDER BY timeFrom ASC")
    fun getWeatherPoints(): Flow<List<WeatherPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherPoints(points: List<WeatherPointEntity>)

    @Query("DELETE FROM weather_points")
    suspend fun clearWeatherPoints()

    @Transaction
    suspend fun refreshWeatherPoints(points: List<WeatherPointEntity>) {
        clearWeatherPoints()
        insertWeatherPoints(points)
    }

    @Query("SELECT * FROM remit_notices WHERE isTestMode = :testMode ORDER BY publishTime DESC")
    fun getRemitNotices(testMode: Boolean): Flow<List<RemitNoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemitNotices(notices: List<RemitNoticeEntity>)

    @Query("DELETE FROM remit_notices WHERE isTestMode = :testMode")
    suspend fun clearRemitNotices(testMode: Boolean)

    @Transaction
    suspend fun refreshRemitNotices(testMode: Boolean, notices: List<RemitNoticeEntity>) {
        clearRemitNotices(testMode)
        insertRemitNotices(notices)
    }
}
