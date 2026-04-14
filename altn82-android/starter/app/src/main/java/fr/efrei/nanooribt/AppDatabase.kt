package fr.efrei.nanooribt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SatelliteEntity::class, FenetreEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun satelliteDao(): SatelliteDao
    abstract fun fenetreDao(): FenetreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nanoorbit_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
