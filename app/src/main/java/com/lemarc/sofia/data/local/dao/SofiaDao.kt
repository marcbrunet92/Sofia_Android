package com.lemarc.sofia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lemarc.sofia.data.local.B1610PointEntity
import com.lemarc.sofia.data.local.GbContextEntity
import com.lemarc.sofia.data.local.MelPointEntity
import com.lemarc.sofia.data.local.ProductionPointEntity
import com.lemarc.sofia.data.local.RemitNoticeEntity
import com.lemarc.sofia.data.local.UouDayEntity
import com.lemarc.sofia.data.local.WeatherPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SofiaDao {

    @Query("SELECT * FROM production_points  ORDER BY timeFrom ASC")
    fun getProductionPoints(): Flow<List<ProductionPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionPoints(points: List<ProductionPointEntity>)

    @Query("DELETE FROM production_points ")
    suspend fun clearProductionPoints()

    @Transaction
    suspend fun refreshProductionPoints(points: List<ProductionPointEntity>) {
        clearProductionPoints()
        insertProductionPoints(points)
    }

    @Query("SELECT * FROM b1610_points  ORDER BY timeFrom ASC")
    fun getB1610Points(): Flow<List<B1610PointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertB1610Points(points: List<B1610PointEntity>)

    @Query("DELETE FROM b1610_points ")
    suspend fun clearB1610Points()

    @Transaction
    suspend fun refreshB1610Points(points: List<B1610PointEntity>) {
        clearB1610Points()
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

    @Query("SELECT * FROM remit_notices  ORDER BY publishTime DESC")
    fun getRemitNotices(): Flow<List<RemitNoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemitNotices(notices: List<RemitNoticeEntity>)

    @Query("DELETE FROM remit_notices ")
    suspend fun clearRemitNotices()

    @Transaction
    suspend fun refreshRemitNotices(notices: List<RemitNoticeEntity>) {
        clearRemitNotices()
        insertRemitNotices(notices)
    }

    @Query("SELECT * FROM mel_points  ORDER BY timeFrom ASC")
    fun getMelPoints(): Flow<List<MelPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMelPoints(points: List<MelPointEntity>)

    @Query("DELETE FROM mel_points ")
    suspend fun clearMelPoints()

    @Transaction
    suspend fun refreshMelPoints(points: List<MelPointEntity>) {
        clearMelPoints()
        insertMelPoints(points)
    }

    @Query("SELECT * FROM uou_days  ORDER BY epochDay ASC")
    fun getUouDays(): Flow<List<UouDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUouDays(days: List<UouDayEntity>)

    @Query("DELETE FROM uou_days ")
    suspend fun clearUouDays()

    @Transaction
    suspend fun refreshUouDays(days: List<UouDayEntity>) {
        clearUouDays()
        insertUouDays(days)
    }

    @Query("SELECT * FROM gb_context WHERE id = 0")
    fun getGbContext(): Flow<GbContextEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGbContext(context: GbContextEntity)
}
