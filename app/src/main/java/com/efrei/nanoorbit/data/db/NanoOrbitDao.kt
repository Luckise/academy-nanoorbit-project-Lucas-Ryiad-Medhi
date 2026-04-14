package com.efrei.nanoorbit.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NanoOrbitDao {
    @Query("SELECT * FROM satellites")
    fun getAllSatellites(): Flow<List<SatelliteEntity>>

    @Query("SELECT * FROM satellites WHERE idSatellite = :id")
    suspend fun getSatelliteById(id: String): SatelliteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellites(satellites: List<SatelliteEntity>)

    @Query("SELECT * FROM fenetres_com WHERE idSatellite = :satelliteId ORDER BY datetimeDebut ASC")
    fun getFenetresForSatellite(satelliteId: String): Flow<List<FenetreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFenetres(fenetres: List<FenetreEntity>)

    @Query("DELETE FROM satellites")
    suspend fun clearSatellites()
}
