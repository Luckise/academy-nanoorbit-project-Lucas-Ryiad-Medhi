package fr.efrei.nanooribt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "satellites")
data class SatelliteEntity(
    @PrimaryKey val idSatellite: String,
    val nomSatellite: String,
    val statut: String,
    val formatCubesat: String,
    val idOrbite: String,
    val dateLancement: String?,
    val masse: Double?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "fenetres_com")
data class FenetreEntity(
    @PrimaryKey val idFenetre: Int,
    val datetimeDebut: String,
    val duree: Int,
    val statut: String,
    val idSatellite: String,
    val codeStation: String,
    val volumeDonnees: Double?
)

fun Satellite.toEntity() = SatelliteEntity(
    idSatellite = idSatellite,
    nomSatellite = nomSatellite,
    statut = statut.name,
    formatCubesat = formatCubesat.name,
    idOrbite = idOrbite,
    dateLancement = dateLancement?.toString(),
    masse = masse
)

fun SatelliteEntity.toDomain() = Satellite(
    idSatellite = idSatellite,
    nomSatellite = nomSatellite,
    statut = StatutSatellite.valueOf(statut),
    formatCubesat = FormatCubeSat.valueOf(formatCubesat),
    idOrbite = idOrbite,
    dateLancement = dateLancement?.let { java.time.LocalDate.parse(it) },
    masse = masse
)
