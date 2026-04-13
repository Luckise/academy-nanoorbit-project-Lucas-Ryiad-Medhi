package fr.efrei.nanooribt

import java.time.LocalDate
import java.time.LocalDateTime

object MockData {
    val orbites = listOf(
        Orbite("ORB-001", TypeOrbite.SSO, 500, 97.4, "Polaire"),
        Orbite("ORB-002", TypeOrbite.SSO, 600, 98.0, "Polaire"),
        Orbite("ORB-003", TypeOrbite.LEO, 400, 51.6, "Équatoriale")
    )

    val satellites = listOf(
        Satellite("SAT-001", "Echo-1", StatutSatellite.OPERATIONNEL, FormatCubeSat.U3, "ORB-001", LocalDate.of(2023, 5, 12), 4.5),
        Satellite("SAT-002", "Nova-X", StatutSatellite.EN_VEILLE, FormatCubeSat.U6, "ORB-001", LocalDate.of(2023, 8, 20), 10.2),
        Satellite("SAT-003", "Astra-3", StatutSatellite.DEFAILLANT, FormatCubeSat.U1, "ORB-002", LocalDate.of(2022, 11, 5), 1.3),
        Satellite("SAT-004", "Iris-S", StatutSatellite.OPERATIONNEL, FormatCubeSat.U12, "ORB-003", LocalDate.of(2024, 1, 15), 18.0),
        Satellite("SAT-005", "Legacy-0", StatutSatellite.DESORBITE, FormatCubeSat.U3, "ORB-001", LocalDate.of(2020, 3, 10), 4.2)
    )

    val instruments = listOf(
        Instrument("INST-001", "Caméra", "OptiView-4K", "1m", 15.0),
        Instrument("INST-002", "Spectromètre", "SpecScan-X", "N/A", 8.5),
        Instrument("INST-003", "Radar", "TerraMap-SAR", "5m", 45.0),
        Instrument("INST-004", "Capteur Air", "AeroQual-Z", "N/A", 5.0)
    )

    val stations = listOf(
        StationSol("STA-TLS", "Toulouse Space Center", 43.6047, 1.4442, 12.0, 500.0),
        StationSol("STA-KRN", "Kiruna Arctic Station", 67.8558, 20.2253, 15.0, 1000.0),
        StationSol("STA-SGP", "Singapore Gateway", 1.3521, 103.8198, 10.0, 800.0)
    )

    val fenetres = listOf(
        FenetreCom(1, LocalDateTime.now().minusHours(5), 600, StatutFenetre.REALISEE, "SAT-001", "STA-TLS", 150.5),
        FenetreCom(2, LocalDateTime.now().minusHours(2), 450, StatutFenetre.REALISEE, "SAT-002", "STA-KRN", 80.0),
        FenetreCom(3, LocalDateTime.now().plusHours(1), 300, StatutFenetre.PLANIFIEE, "SAT-001", "STA-SGP"),
        FenetreCom(4, LocalDateTime.now().plusHours(4), 900, StatutFenetre.PLANIFIEE, "SAT-004", "STA-TLS"),
        FenetreCom(5, LocalDateTime.now().minusDays(1), 500, StatutFenetre.REALISEE, "SAT-005", "STA-KRN", 45.2)
    )
}
