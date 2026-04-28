package fr.efrei.nanooribt

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CelesTrak GP (General Perturbations) JSON endpoint — public, key-free, free-to-use.
 *
 * Docs: https://celestrak.org/NORAD/documentation/gp-data-formats.php
 * Each element is an Orbit Mean-Elements Message (OMM) at a specific epoch.
 *
 * For the AR feature we use GROUP=visual which returns ~150 satellites that are
 * naked-eye visible from the ground — a sensible curation for an AR sky-tracker.
 */
interface CelesTrakApi {
    @GET("NORAD/elements/gp.php")
    suspend fun getGroup(
        @Query("GROUP") group: String = "visual",
        @Query("FORMAT") format: String = "json"
    ): List<TleElement>
}

data class TleElement(
    @SerializedName("OBJECT_NAME") val objectName: String,
    @SerializedName("OBJECT_ID") val objectId: String? = null,
    @SerializedName("EPOCH") val epoch: String, // ISO-8601 UTC, e.g. "2024-01-15T12:34:56.123"
    @SerializedName("MEAN_MOTION") val meanMotion: Double, // revolutions per day
    @SerializedName("ECCENTRICITY") val eccentricity: Double,
    @SerializedName("INCLINATION") val inclinationDeg: Double,
    @SerializedName("RA_OF_ASC_NODE") val raanDeg: Double,
    @SerializedName("ARG_OF_PERICENTER") val argPericenterDeg: Double,
    @SerializedName("MEAN_ANOMALY") val meanAnomalyDeg: Double,
    @SerializedName("NORAD_CAT_ID") val noradId: Int? = null
)

object CelesTrakClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://celestrak.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val api: CelesTrakApi by lazy { retrofit.create(CelesTrakApi::class.java) }
}
