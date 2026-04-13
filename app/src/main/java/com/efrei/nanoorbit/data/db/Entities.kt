package com.efrei.nanoorbit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.efrei.nanoorbit.data.models.StatutSatellite
import com.efrei.nanoorbit.data.models.StatutFenetre

@Entity(tableName = "satellites")
data class SatelliteEntity(
    @PrimaryKey val idSatellite: String,
    val nomSatellite: String,
    val statut: StatutSatellite,
    val formatCubesat: String,
    val idOrbite: Int
)

@Entity(tableName = "fenetres_com")
data class FenetreEntity(
    @PrimaryKey val idFenetre: Int,
    val datetimeDebut: Long, // Stocké en timestamp pour Room
    val duree: Int,
    val statut: StatutFenetre,
    val idSatellite: String,
    val codeStation: String
)
