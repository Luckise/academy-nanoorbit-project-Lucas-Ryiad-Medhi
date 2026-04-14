package fr.efrei.nanooribt

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Statut d'un satellite - Correspondance avec les valeurs CHECK Oracle (ALTN83)
 */
enum class StatutSatellite {
    OPERATIONNEL,
    EN_VEILLE,
    DEFAILLANT,
    DESORBITE
}

/**
 * Format standard d'un CubeSat
 */
enum class FormatCubeSat {
    U1, U3, U6, U12
}

/**
 * Statut d'une fenêtre de communication
 */
enum class StatutFenetre {
    PLANIFIEE,
    REALISEE,
    ANNULEE
}

/**
 * Type d'orbite
 */
enum class TypeOrbite {
    SSO, LEO, MEO, GEO
}

/**
 * Data class Satellite - Table SATELLITE Oracle
 */
data class Satellite(
    val idSatellite: String, // PK - VARCHAR2(20)
    val nomSatellite: String, // VARCHAR2(100)
    val statut: StatutSatellite, // VARCHAR2(30)
    val formatCubesat: FormatCubeSat, // VARCHAR2(5)
    val idOrbite: String, // FK - VARCHAR2(20)
    val dateLancement: LocalDate? = null, // DATE
    val masse: Double? = null // NUMBER
)

/**
 * Data class Orbite - Table ORBITE Oracle
 */
data class Orbite(
    val idOrbite: String, // PK - VARCHAR2(20)
    val typeOrbite: TypeOrbite, // VARCHAR2(20)
    val altitude: Int, // NUMBER
    val inclinaison: Double, // NUMBER
    val zoneCouverture: String? = null // VARCHAR2(100)
)

/**
 * Data class Instrument - Table INSTRUMENT Oracle
 */
data class Instrument(
    val refInstrument: String, // PK - VARCHAR2(20)
    val typeInstrument: String, // VARCHAR2(50)
    val modele: String, // VARCHAR2(100)
    val resolution: String? = null, // VARCHAR2(50)
    val consommation: Double? = null // NUMBER
)

/**
 * Data class FenetreCom - Table FENETRE_COM Oracle
 */
data class FenetreCom(
    val idFenetre: Int, // PK - NUMBER
    val datetimeDebut: LocalDateTime, // TIMESTAMP
    val duree: Int, // NUMBER [1-900]
    val statut: StatutFenetre, // VARCHAR2(20)
    val idSatellite: String, // FK - VARCHAR2(20)
    val codeStation: String, // FK - VARCHAR2(10)
    val volumeDonnees: Double? = null // NUMBER
)

/**
 * Data class StationSol - Table STATION_SOL Oracle
 */
data class StationSol(
    val codeStation: String, // PK - VARCHAR2(10)
    val nomStation: String, // VARCHAR2(100)
    val latitude: Double, // NUMBER(9,6)
    val longitude: Double, // NUMBER(9,6)
    val diametreAntenne: Double? = null, // NUMBER
    val debitMax: Double? = null // NUMBER
)

/**
 * Data class Mission - Table MISSION Oracle
 */
data class Mission(
    val idMission: String, // PK - VARCHAR2(20)
    val nomMission: String, // VARCHAR2(100)
    val objectif: String, // VARCHAR2(500)
    val dateDebut: LocalDate, // DATE
    val statutMission: String, // VARCHAR2(30)
    val dateFin: LocalDate? = null, // DATE
    val zoneGeoCible: String? = null // VARCHAR2(100)
)
