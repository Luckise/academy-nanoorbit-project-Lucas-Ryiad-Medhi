package fr.efrei.nanooribt

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SatelliteDao {
    @Query("SELECT * FROM satellites")
    fun getAllSatellites(): Flow<List<SatelliteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatellites(satellites: List<SatelliteEntity>)

    @Query("DELETE FROM satellites")
    suspend fun deleteAll()
}

@Dao
interface FenetreDao {
    @Query("SELECT * FROM fenetres_com")
    fun getAllFenetres(): Flow<List<FenetreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFenetres(fenetres: List<FenetreEntity>)

    @Query("DELETE FROM fenetres_com")
    suspend fun deleteAll()
}
