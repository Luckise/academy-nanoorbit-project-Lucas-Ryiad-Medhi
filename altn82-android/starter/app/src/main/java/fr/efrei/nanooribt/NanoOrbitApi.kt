package fr.efrei.nanooribt

import retrofit2.http.GET
import retrofit2.http.Path

interface NanoOrbitApi {
    @GET("satellites")
    suspend fun getSatellites(): List<Satellite>

    @GET("satellites/{id}/instruments")
    suspend fun getInstruments(@Path("id") id: String): List<Instrument>

    @GET("fenetres")
    suspend fun getFenetres(): List<FenetreCom>
}
