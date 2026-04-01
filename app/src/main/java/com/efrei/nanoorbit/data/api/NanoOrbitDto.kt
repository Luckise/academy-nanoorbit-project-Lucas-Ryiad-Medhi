package com.efrei.nanoorbit.data.api

import com.google.gson.annotations.SerializedName

data class SatelliteDto(
    @SerializedName("id_satellite") val id: String,
    @SerializedName("nom_satellite") val nom: String,
    @SerializedName("statut") val statut: String,
    @SerializedName("format_cubesat") val format: String,
    @SerializedName("id_orbite") val idOrbite: Int,
    @SerializedName("date_lancement") val dateLancement: String? = null
)

data class FenetreComDto(
    @SerializedName("id_fenetre") val id: Int,
    @SerializedName("datetime_debut") val debut: String,
    @SerializedName("duree") val duree: Int,
    @SerializedName("statut") val statut: String,
    @SerializedName("id_satellite") val idSatellite: String,
    @SerializedName("code_station") val codeStation: String,
    @SerializedName("elevation_max") val elevation: Double? = null,
    @SerializedName("volume_donnees") val volume: Double? = null
)

data class MissionDto(
    @SerializedName("id_mission") val id: String,
    @SerializedName("nom_mission") val nom: String,
    @SerializedName("statut_mission") val statut: String
)
