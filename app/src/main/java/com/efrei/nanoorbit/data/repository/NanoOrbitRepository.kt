package com.efrei.nanoorbit.data.repository

import com.efrei.nanoorbit.data.api.NanoOrbitApi
import com.efrei.nanoorbit.data.api.SatelliteDto
import com.efrei.nanoorbit.data.api.FenetreComDto
import com.efrei.nanoorbit.data.db.NanoOrbitDao
import com.efrei.nanoorbit.data.db.SatelliteEntity
import com.efrei.nanoorbit.data.db.FenetreEntity
import com.efrei.nanoorbit.data.models.Satellite
import com.efrei.nanoorbit.data.models.FenetreCom
import com.efrei.nanoorbit.data.models.StatutSatellite
import com.efrei.nanoorbit.data.models.StatutFenetre
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NanoOrbitRepository(
    private val api: NanoOrbitApi,
    private val dao: NanoOrbitDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    // --- Satellites ---
    val satellites: Flow<List<Satellite>> = dao.getAllSatellites().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun refreshSatellites() {
        try {
            val dtos = api.getSatellites()
            dao.insertSatellites(dtos.map { it.toEntity() })
        } catch (e: Exception) {
            // Log error
        }
    }

    // --- Fenetres ---
    fun getFenetresForSatellite(satelliteId: String): Flow<List<FenetreCom>> {
        return dao.getFenetresForSatellite(satelliteId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun refreshFenetres(satelliteId: String) {
        try {
            val dtos = api.getFenetresBySatellite(satelliteId)
            dao.insertFenetres(dtos.map { it.toEntity() })
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun createFenetre(fenetre: FenetreCom) {
        // RG-F04
        if (fenetre.duree !in 1..900) {
            throw IllegalArgumentException("Durée invalide : entre 1 et 900 secondes")
        }
        api.createFenetre(fenetre.toDto())
        refreshFenetres(fenetre.idSatellite)
    }

    // --- Mapper: DTO -> Entity ---
    private fun SatelliteDto.toEntity() = SatelliteEntity(
        idSatellite = id,
        nomSatellite = nom,
        statut = StatutSatellite.valueOf(statut.uppercase()),
        formatCubesat = format,
        idOrbite = idOrbite
    )

    private fun FenetreComDto.toEntity() = FenetreEntity(
        idFenetre = id,
        datetimeDebut = dateFormat.parse(debut)?.time ?: 0L,
        duree = duree,
        statut = StatutFenetre.valueOf(statut.uppercase()),
        idSatellite = idSatellite,
        codeStation = codeStation
    )

    // --- Mapper: Entity -> Domain ---
    private fun SatelliteEntity.toDomainModel() = Satellite(
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        dateLancement = Date(),
        masse = 0.0,
        formatCubesat = formatCubesat,
        statut = statut,
        dureeViePrevue = 0,
        capaciteBatterie = 0,
        idOrbite = idOrbite
    )

    private fun FenetreEntity.toDomainModel() = FenetreCom(
        idFenetre = idFenetre,
        datetimeDebut = Date(datetimeDebut),
        duree = duree,
        elevationMax = 0.0,
        volumeDonnees = null,
        statut = statut,
        idSatellite = idSatellite,
        codeStation = codeStation
    )

    // --- Mapper: Domain -> DTO ---
    private fun FenetreCom.toDto() = FenetreComDto(
        id = idFenetre,
        debut = dateFormat.format(datetimeDebut),
        duree = duree,
        statut = statut.name,
        idSatellite = idSatellite,
        codeStation = codeStation
    )
}
