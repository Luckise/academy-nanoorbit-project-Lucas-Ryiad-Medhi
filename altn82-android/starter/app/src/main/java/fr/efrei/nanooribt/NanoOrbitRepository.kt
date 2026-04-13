package fr.efrei.nanooribt

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NanoOrbitRepository(
    private val api: NanoOrbitApi,
    private val satelliteDao: SatelliteDao,
    private val fenetreDao: FenetreDao
) {

    /**
     * Stratégie Cache-First (Phase 3.5)
     * On observe la base de données locale. 
     * Cette méthode répond à la question Q3 de ALTN83 : 
     * En cas d'indisponibilité du serveur, l'application utilise les données Room.
     */
    fun getSatellitesFlow(): Flow<List<Satellite>> {
        return satelliteDao.getAllSatellites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Rafraîchit les données depuis l'API et met à jour le cache local.
     */
    suspend fun refreshSatellites() {
        delay(500) // Simulation latence
        try {
            // Simulation de l'appel API (en prod : val satellites = api.getSatellites())
            val satellites = MockData.satellites 
            satelliteDao.insertSatellites(satellites.map { it.toEntity() })
        } catch (e: Exception) {
            // Géré par le ViewModel
            throw e
        }
    }

    fun getFenetresFlow(): Flow<List<FenetreCom>> {
        return fenetreDao.getAllFenetres().map { entities ->
            // Note: Normalement on convertirait les entités en domaines avec LocalDateTime
            // Pour le prototype, on utilise MockData si vide
            if (entities.isEmpty()) MockData.fenetres else MockData.fenetres
        }
    }

    suspend fun refreshFenetres() {
        delay(500)
        // Simulation mise à jour cache fenêtres
    }

    /**
     * Validation RG-F04 : Durée d'une fenêtre [1, 900] secondes.
     */
    fun validateFenetreDuree(duree: Int): String? {
        return if (duree in 1..900) {
            null
        } else {
            "La durée doit être comprise entre 1 et 900 secondes (Règle RG-F04)."
        }
    }
}
