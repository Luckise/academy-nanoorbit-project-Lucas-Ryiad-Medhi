package com.efrei.nanoorbit.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Body

interface NanoOrbitApi {
    @GET("satellites")
    suspend fun getSatellites(): List<SatelliteDto>

    @GET("satellites/{id}")
    suspend fun getSatelliteById(@Path("id") id: String): SatelliteDto

    @GET("satellites/{id}/fenetres")
    suspend fun getFenetresBySatellite(@Path("id") id: String): List<FenetreComDto>

    @POST("fenetres")
    suspend fun createFenetre(@Body fenetre: FenetreComDto)
}
