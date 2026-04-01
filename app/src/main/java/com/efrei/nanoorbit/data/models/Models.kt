package com.efrei.nanoorbit.data.models

import java.util.Date

enum class StatutSatellite {
    OPERATIONNEL, EN_VEILLE, DEFAILLANT, DESORBITE
}

enum class StatutFenetre {
    PLANIFIEE, REALISEE, ANNULEE
}

data class Orbite(
    val idOrbite: Int,
    val typeOrbite: String,
    val altitude: Int,
    val inclinaison: Double,
    val periodeOrbitale: Int,
    val excentricite: Double,
    val zoneCouverture: String
)

data class Satellite(
    val idSatellite: String,
    val nomSatellite: String,
    val dateLancement: Date,
    val masse: Double,
    val formatCubesat: String,
    val statut: StatutSatellite,
    val dureeViePrevue: Int,
    val capaciteBatterie: Int,
    val idOrbite: Int
)

data class Instrument(
    val refInstrument: String,
    val typeInstrument: String,
    val modele: String,
    val resolution: String?,
    val consommation: Double,
    val masse: Double
)

data class FenetreCom(
    val idFenetre: Int,
    val datetimeDebut: Date,
    val duree: Int,
    val elevationMax: Double,
    val volumeDonnees: Double?,
    val statut: StatutFenetre,
    val idSatellite: String,
    val codeStation: String
)
