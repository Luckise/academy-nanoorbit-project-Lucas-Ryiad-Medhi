package fr.efrei.nanooribt

import retrofit2.http.GET
import retrofit2.http.Path

interface NanoOrbitApi {
    @GET("satellites")
    suspend fun getSatellites(): List<SatelliteApiResponse>

    @GET("satellites/{id}/instruments")
    suspend fun getInstruments(@Path("id") id: String): List<InstrumentApiResponse>

    @GET("fenetres")
    suspend fun getFenetres(): List<FenetreApiResponse>

    @GET("stations")
    suspend fun getStations(): List<StationApiResponse>

    @GET("satellites/{id}/missions")
    suspend fun getSatelliteMissions(@Path("id") id: String): List<MissionApiResponse>
}

data class SatelliteApiResponse(
    val idSatellite: String,
    val nomSatellite: String,
    val statut: String,
    val formatCubesat: String,
    val idOrbite: String,
    val dateLancement: String?,
    val masse: Double?,
    val dureeViePrevue: Int?,
    val capaciteBatterie: Double?
)

data class InstrumentApiResponse(
    val refInstrument: String,
    val typeInstrument: String,
    val modele: String,
    val resolution: String?,
    val consommation: Double?,
    val etatFonctionnement: String?,
    val dateIntegration: String?
)

data class FenetreApiResponse(
    val idFenetre: Int,
    val datetimeDebut: String?,
    val duree: Int,
    val elevationMax: Double?,
    val statut: String,
    val idSatellite: String,
    val codeStation: String,
    val volumeDonnees: Double?
)

data class StationApiResponse(
    val codeStation: String,
    val nomStation: String,
    val latitude: Double,
    val longitude: Double,
    val diametreAntenne: Double?,
    val bandeFrequence: String?,
    val debitMax: Double?,
    val statut: String?
)

data class MissionApiResponse(
    val idMission: String,
    val nomMission: String,
    val objectif: String?,
    val zoneGeoCible: String?,
    val dateDebut: String?,
    val dateFin: String?,
    val statutMission: String,
    val roleSatellite: String?
)
