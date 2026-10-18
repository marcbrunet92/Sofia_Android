package com.lemarc.sofia.data.local

import android.content.Context
import android.util.Log
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
        RemitNoticeEntity::class,
        MelPointEntity::class,
        UouDayEntity::class,
        GbContextEntity::class,
    ],
    // v2 : + mel_points, uou_days, gb_context. C'est un cache : on le recrée au lieu de migrer.
    version = 4,
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

        private const val DATABASE_NAME = "sofia_database"

        fun getDatabase(context: Context): SofiaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: openOrRecreate(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): SofiaDatabase =
            Room.databaseBuilder(context, SofiaDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * Room n'ouvre la base qu'à la première requête : on force l'ouverture ici pour que l'erreur
         * « Room cannot verify the data integrity » (schéma modifié sans changer la version) arrive
         * maintenant, et pas plus tard dans un DAO. C'est un cache : on supprime le fichier et on recrée.
         */
        private fun openOrRecreate(context: Context): SofiaDatabase {
            val db = build(context)
            return try {
                db.openHelper.writableDatabase
                db
            } catch (e: IllegalStateException) {
                Log.w("SofiaDatabase", "Base invalide, suppression et recréation", e)
                db.close()
                context.deleteDatabase(DATABASE_NAME)
                build(context)
            }
        }
    }
}
