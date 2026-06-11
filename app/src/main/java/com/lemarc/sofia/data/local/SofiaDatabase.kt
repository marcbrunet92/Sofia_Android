package com.lemarc.sofia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.lemarc.sofia.data.local.dao.SofiaDao
import java.time.Instant

@Database(
    entities = [
        ProductionPointEntity::class,
        B1610PointEntity::class,
        WeatherPointEntity::class,
        RemitNoticeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SofiaDatabase.Converters::class)
abstract class SofiaDatabase : RoomDatabase() {
    abstract fun sofiaDao(): SofiaDao

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Instant? {
            return value?.let { Instant.ofEpochMilli(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Instant?): Long? {
            return date?.toEpochMilli()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SofiaDatabase? = null

        fun getDatabase(context: Context): SofiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SofiaDatabase::class.java,
                    "sofia_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
