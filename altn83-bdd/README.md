# 🗄️ ALTN83 — Bases de données réparties

> **Module :** ALTN83 · Semestre 8 · EFREI  
> **SGBD :** Oracle 23ai — schéma `NANOORBIT_ADMIN` sur `FREEPDB1`

---

## 📐 Organisation du module

| Phase | Thème | Points | Durée |
|---|---|---|---|
| **Phase 1** | Conception & Architecture distribuée (MERISE) | 20 | 3h30 |
| **Phase 2** | Schéma Oracle & Triggers (DDL + DML + PL/SQL) | 28 | 3h00 |
| **Phase 3** | PL/SQL & Package `pkg_nanoOrbit` | 45 | 3h30 |
| **Phase 4** | Exploitation avancée & Optimisation | 50 | 3h30 |

La note finale sur 20 est calculée selon la pondération du module (50% projet).

---

## 📁 Contenu de ce dossier

### `sujets/`
- **`ALTN83_NanoOrbit_Projet_Fil_Rouge.pdf`** — Énoncé complet des 4 phases
- **`ALTN83_NanoOrbit_CDC_Phase1.pdf`** — Cahier des charges Phase 1 (dictionnaire, règles de gestion, travail demandé)
- **`ALTN83_NanoOrbit_AnnexeA_Donnees_Reference.pdf`** — Jeu de données de référence commenté (les 10 tables)

### `donnees/`
Les 10 fichiers CSV du jeu de données de référence, dans l'ordre d'insertion :

```
01_ORBITE.csv               → 3 lignes  (ORB-001 à ORB-003)
02_SATELLITE.csv            → 5 lignes  (SAT-001 à SAT-005)
03_INSTRUMENT.csv           → 4 lignes  (INS-CAM-01, INS-IR-01, INS-AIS-01, INS-SPEC-01)
04_EMBARQUEMENT.csv         → 7 lignes  (PK composite id_satellite + ref_instrument)
05_CENTRE_CONTROLE.csv      → 3 lignes  (Paris, Houston, Singapour)
06_STATION_SOL.csv          → 3 lignes  (GS-TLS-01, GS-KIR-01, GS-SGP-01)
07_AFFECTATION_STATION.csv  → 3 lignes  (PK composite id_centre + code_station)
08_MISSION.csv              → 3 lignes  (MSN-ARC-2023, MSN-DEF-2022, MSN-COAST-2024)
09_FENETRE_COM.csv          → 5 lignes  (3 Réalisées, 2 Planifiées)
10_PARTICIPATION.csv        → 7 lignes  (PK composite id_satellite + id_mission)
```

### `scripts/`
- **`ALTN83_NanoOrbit_Phase2_DML.sql`** — Script DML complet avec vérifications et tests de triggers
- **`ALTN83_NanoOrbit_Memo_SQL_PLSQL.pdf`** — Fiche mémo Oracle/SQL/PL/SQL pour le projet

---

## 🔑 MLD de référence (10 tables)

```
ORBITE          (id_orbite PK, type_orbite, altitude, inclinaison, periode_orbitale, excentricite, zone_couverture)
SATELLITE       (id_satellite PK, nom_satellite, date_lancement, masse, format_cubesat, statut, duree_vie_prevue, capacite_batterie, #id_orbite)
INSTRUMENT      (ref_instrument PK, type_instrument, modele, resolution, consommation, masse)
EMBARQUEMENT    (id_satellite PK FK, ref_instrument PK FK, date_integration, etat_fonctionnement)
CENTRE_CONTROLE (id_centre PK, nom_centre, ville, region_geo, fuseau_horaire, statut)
STATION_SOL     (code_station PK, nom_station, latitude, longitude, diametre_antenne, bande_frequence, debit_max, statut)
AFFECTATION_STATION (id_centre PK FK, code_station PK FK, date_affectation)
MISSION         (id_mission PK, nom_mission, objectif, zone_geo_cible, date_debut, date_fin, statut_mission)
FENETRE_COM     (id_fenetre PK, datetime_debut, duree, elevation_max, volume_donnees, statut, #id_satellite, #code_station)
PARTICIPATION   (id_satellite PK FK, id_mission PK FK, role_satellite)
```

---

## 🔴 Les 5 triggers à implémenter (Phase 2)

| Trigger | Événement | Règle | Code erreur |
|---|---|---|---|
| `trg_valider_fenetre` | BEFORE INSERT FENETRE_COM | Satellite Désorbité ou station Maintenance | ORA-20001 / ORA-20002 |
| `trg_no_chevauchement` | BEFORE INS/UPD FENETRE_COM | Pas de chevauchement temporel | ORA-20003 |
| `trg_volume_realise` | BEFORE INS/UPD FENETRE_COM | Volume NULL si non Réalisée | *(correction silencieuse)* |
| `trg_mission_terminee` | BEFORE INSERT PARTICIPATION | Mission Terminée bloquée | ORA-20004 |
| `trg_historique_statut` | AFTER UPDATE statut SATELLITE | Trace dans HISTORIQUE_STATUT | *(pas d'erreur)* |
