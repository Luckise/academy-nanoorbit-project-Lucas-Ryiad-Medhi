package fr.efrei.nanooribt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class NanoOrbitRepository(
    private val api: NanoOrbitApi,
    private val satelliteDao: SatelliteDao,
    private val fenetreDao: FenetreDao
) {

    private val _stations = MutableStateFlow<List<StationSol>>(emptyList())
    val stations = _stations.asStateFlow()

    private val _instruments = MutableStateFlow<Map<String, List<Instrument>>>(emptyMap())

    /**
     * Stratégie Cache-First : lecture locale Room en priorité, puis rafraîchissement API en arrière-plan.
     *
     * Lien ALTN83 Q3 — "Comment le centre de Singapour peut-il continuer à planifier des fenêtres
     * de communication si le serveur central est indisponible ?"
     *
     * Cette stratégie répond directement à cette problématique : en cas d'indisponibilité du serveur
     * (API REST), l'application continue de fonctionner grâce aux données mises en cache dans la base
     * Room locale (SatelliteEntity, FenetreEntity). L'opérateur peut consulter les satellites,
     * les fenêtres planifiées et les stations depuis le cache. Une bannière "Mode hors-ligne" s'affiche
     * pour signaler que les données peuvent être obsolètes. Le rafraîchissement se fait automatiquement
     * dès que la connectivité est rétablie.
     *
     * C'est l'équivalent mobile de la "réplique locale en lecture seule" proposée dans l'architecture
     * de fragmentation horizontale de la Phase 1 ALTN83.
     */
    fun getSatellitesFlow(): Flow<List<Satellite>> {
        return satelliteDao.getAllSatellites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshSatellites() {
        val response = api.getSatellites()
        val entities = response.map { dto ->
            SatelliteEntity(
                idSatellite = dto.idSatellite,
                nomSatellite = dto.nomSatellite,
                statut = dto.statut,
                formatCubesat = dto.formatCubesat,
                idOrbite = dto.idOrbite,
                dateLancement = dto.dateLancement,
                masse = dto.masse
            )
        }
        satelliteDao.insertSatellites(entities)
    }

    fun getFenetresFlow(): Flow<List<FenetreCom>> {
        return fenetreDao.getAllFenetres().map { entities ->
            entities.map { entity ->
                FenetreCom(
                    idFenetre = entity.idFenetre,
                    datetimeDebut = entity.datetimeDebut?.let {
                        LocalDateTime.parse(it)
                    } ?: LocalDateTime.now(),
                    duree = entity.duree,
                    statut = try { StatutFenetre.valueOf(entity.statut) } catch (_: Exception) { StatutFenetre.PLANIFIEE },
                    idSatellite = entity.idSatellite,
                    codeStation = entity.codeStation,
                    volumeDonnees = entity.volumeDonnees
                )
            }
        }
    }

    suspend fun refreshFenetres() {
        val response = api.getFenetres()
        val entities = response.map { dto ->
            FenetreEntity(
                idFenetre = dto.idFenetre,
                datetimeDebut = dto.datetimeDebut ?: "",
                duree = dto.duree,
                statut = dto.statut,
                idSatellite = dto.idSatellite,
                codeStation = dto.codeStation,
                volumeDonnees = dto.volumeDonnees
            )
        }
        fenetreDao.insertFenetres(entities)
    }

    suspend fun refreshStations() {
        val response = api.getStations()
        _stations.value = response.map { dto ->
            StationSol(
                codeStation = dto.codeStation,
                nomStation = dto.nomStation,
                latitude = dto.latitude,
                longitude = dto.longitude,
                diametreAntenne = dto.diametreAntenne,
                debitMax = dto.debitMax
            )
        }
    }

    suspend fun getInstrumentsForSatellite(satelliteId: String): List<Instrument> {
        val cached = _instruments.value[satelliteId]
        if (cached != null) return cached

        val response = api.getInstruments(satelliteId)
        val instruments = response.map { dto ->
            Instrument(
                refInstrument = dto.refInstrument,
                typeInstrument = dto.typeInstrument,
                modele = dto.modele,
                resolution = dto.resolution,
                consommation = dto.consommation,
                etatFonctionnement = dto.etatFonctionnement
            )
        }
        _instruments.value = _instruments.value + (satelliteId to instruments)
        return instruments
    }

    suspend fun getMissionsForSatellite(satelliteId: String): List<Mission> {
        val response = api.getSatelliteMissions(satelliteId)
        return response.map { dto ->
            Mission(
                idMission = dto.idMission,
                nomMission = dto.nomMission,
                objectif = dto.objectif ?: "",
                dateDebut = dto.dateDebut?.let { LocalDate.parse(it) } ?: LocalDate.now(),
                statutMission = dto.statutMission,
                dateFin = dto.dateFin?.let { LocalDate.parse(it) },
                zoneGeoCible = dto.zoneGeoCible
            )
        }
    }

    /**
     * Planifie une nouvelle fenêtre de communication.
     * Validation côté client (RG-F04 durée, RG-S06 satellite désorbité)
     * avant insertion dans le cache Room local.
     */
    suspend fun addFenetre(fenetre: FenetreCom) {
        val nextId = fenetreDao.getNextId()
        val entity = FenetreEntity(
            idFenetre = nextId,
            datetimeDebut = fenetre.datetimeDebut.toString(),
            duree = fenetre.duree,
            statut = fenetre.statut.name,
            idSatellite = fenetre.idSatellite,
            codeStation = fenetre.codeStation,
            volumeDonnees = fenetre.volumeDonnees
        )
        fenetreDao.insertFenetre(entity)
    }

    fun validateFenetreDuree(duree: Int): String? {
        return if (duree in 1..900) null
        else "La duree doit etre comprise entre 1 et 900 secondes (Regle RG-F04)."
    }
}
