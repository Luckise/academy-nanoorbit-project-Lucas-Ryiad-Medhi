package fr.efrei.nanooribt

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Jeu de données simulées cohérent avec le MLD de référence ALTN83 (Oracle).
 *
 * Correspondances exactes :
 * - 5 satellites (SAT-001 à SAT-005) dont 1 Désorbité (SAT-005)
 * - 3 orbites (ORB-001 SSO 550km, ORB-002 SSO 700km, ORB-003 LEO 400km)
 * - 4 instruments (INS-CAM-01, INS-IR-01, INS-AIS-01, INS-SPEC-01)
 * - 5 fenêtres de communication (3 Réalisées, 2 Planifiées)
 * - 3 stations au sol (GS-TLS-01, GS-KIR-01, GS-SGP-01)
 *
 * ⚠ SAT-005 (Désorbité) doit être traité différemment dans l'UI :
 *   pastille grise, interaction désactivée — miroir du trigger T1 Oracle.
 */
object MockData {

    // ORBITE — 3 lignes (SSO × 2, LEO × 1) — cf. table Oracle ORBITE
    val orbites = listOf(
        Orbite("ORB-001", TypeOrbite.SSO, 550, 97.6, "Polaire globale Europe / Arctique"),
        Orbite("ORB-002", TypeOrbite.SSO, 700, 98.2, "Polaire globale haute latitude"),
        Orbite("ORB-003", TypeOrbite.LEO, 400, 51.6, "Équatoriale zone tropicale")
    )

    // SATELLITE — 5 lignes (3 Opérationnel, 1 En veille, 1 Désorbité) — cf. table Oracle SATELLITE
    val satellites = listOf(
        Satellite("SAT-001", "NanoOrbit-Alpha", StatutSatellite.OPERATIONNEL, FormatCubeSat.U3, "ORB-001", LocalDate.of(2022, 3, 15), 1.30),
        Satellite("SAT-002", "NanoOrbit-Beta", StatutSatellite.OPERATIONNEL, FormatCubeSat.U3, "ORB-001", LocalDate.of(2022, 3, 15), 1.30),
        Satellite("SAT-003", "NanoOrbit-Gamma", StatutSatellite.OPERATIONNEL, FormatCubeSat.U6, "ORB-002", LocalDate.of(2023, 6, 10), 2.00),
        Satellite("SAT-004", "NanoOrbit-Delta", StatutSatellite.EN_VEILLE, FormatCubeSat.U6, "ORB-002", LocalDate.of(2023, 6, 10), 2.00),
        Satellite("SAT-005", "NanoOrbit-Epsilon", StatutSatellite.DESORBITE, FormatCubeSat.U12, "ORB-003", LocalDate.of(2021, 11, 20), 4.50)
    )

    // INSTRUMENT — 4 lignes — cf. table Oracle INSTRUMENT
    // Note : INS-AIS-01 a resolution = null (capteur AIS sans résolution optique)
    val instruments = listOf(
        Instrument("INS-CAM-01", "Caméra optique", "PlanetScope-Mini", "3m", 2.5),
        Instrument("INS-IR-01", "Infrarouge", "FLIR-Lepton-3", "160m", 1.2),
        Instrument("INS-AIS-01", "Récepteur AIS", "ShipTrack-V2", null, 0.8),
        Instrument("INS-SPEC-01", "Spectromètre", "HyperSpec-Nano", "30m", 3.1)
    )

    // STATION_SOL — 3 lignes — cf. table Oracle STATION_SOL
    // GS-SGP-01 est en Maintenance → le trigger T1 bloque toute fenêtre vers cette station
    val stations = listOf(
        StationSol("GS-TLS-01", "Toulouse Ground Station", 43.6047, 1.4442, 3.5, 150.0),
        StationSol("GS-KIR-01", "Kiruna Arctic Station", 67.8557, 20.2253, 5.4, 400.0),
        StationSol("GS-SGP-01", "Singapore Station", 1.3521, 103.8198, 3.0, 120.0)
    )

    // FENETRE_COM — 5 lignes (3 Réalisées, 2 Planifiées) — cf. table Oracle FENETRE_COM
    // volume_donnees est NULL pour les fenêtres non Réalisées (trigger T3)
    val fenetres = listOf(
        FenetreCom(1, LocalDateTime.of(2024, 1, 15, 9, 14), 420, StatutFenetre.REALISEE, "SAT-001", "GS-KIR-01", 1250.0),
        FenetreCom(2, LocalDateTime.of(2024, 1, 15, 11, 52), 310, StatutFenetre.REALISEE, "SAT-002", "GS-TLS-01", 890.0),
        FenetreCom(3, LocalDateTime.of(2024, 1, 16, 8, 30), 540, StatutFenetre.REALISEE, "SAT-003", "GS-KIR-01", 1680.0),
        FenetreCom(4, LocalDateTime.of(2024, 1, 20, 14, 22), 380, StatutFenetre.PLANIFIEE, "SAT-001", "GS-TLS-01"),
        FenetreCom(5, LocalDateTime.of(2024, 1, 21, 7, 45), 290, StatutFenetre.PLANIFIEE, "SAT-003", "GS-TLS-01")
    )
}
