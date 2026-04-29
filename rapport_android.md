# Rapport ALTN82 — NanoOrbit Ground Control / Application Android

> Module : ALTN82 — Développement Mobile Android
> Sujet : `altn82-android/sujets/ALTN82_NanoOrbit_GroundControl_Projet.pdf`
> Stack : Kotlin · Jetpack Compose · MVVM · StateFlow · Retrofit · Room · osmdroid · WorkManager

Ce rapport résume la solution livrée pour répondre au sujet ALTN82. Le code source se trouve dans `altn82-android/starter/app/src/main/java/fr/efrei/nanooribt/`.

---

## 1. Couverture du sujet

| Phase | Palier visé | Livrables | Statut |
|---|---|---|---|
| Phase 1 — Interface & Données | Socle | L1-A à L1-D | Réalisé |
| Phase 2 — Architecture MVVM & API REST | Socle | L2-A à L2-D | Réalisé |
| Phase 3 — Fonctionnalités Avancées | Avancé | L3-A à L3-E | Réalisé |
| Bonus — Notifications | Excellence ★ | L3-F | Réalisé |

Palier visé : **Excellence ★** (Phases 1+2+3 + bonus notifications via WorkManager).

---

## 2. Phase 1 — Interface & Données

### 2.1 Modèles de données Kotlin (L1-A) — `Models.kt`

**Enums miroir des CHECK Oracle :**
- `StatutSatellite` (OPERATIONNEL, EN_VEILLE, DEFAILLANT, DESORBITE)
- `FormatCubeSat` (U1, U3, U6, U12)
- `StatutFenetre` (PLANIFIEE, REALISEE, ANNULEE)
- `TypeOrbite` (SSO, LEO, MEO, GEO)

**Data classes** mappées sur le MLD ALTN83 (commentaires de correspondance Oracle dans le code) :
| Data class | Table Oracle miroir | Champs nullables |
|---|---|---|
| `Satellite` | SATELLITE | `dateLancement`, `masse` |
| `Orbite` | ORBITE | `zoneCouverture` |
| `Instrument` | INSTRUMENT (+ EMBARQUEMENT) | `resolution`, `consommation`, `etatFonctionnement` |
| `FenetreCom` | FENETRE_COM | `volumeDonnees` |
| `StationSol` | STATION_SOL | `diametreAntenne`, `debitMax` |
| `Mission` | MISSION | `dateFin`, `zoneGeoCible` |

### 2.2 Mock Data (L1-B) — `MockData.kt`
Jeu de données aligné avec la référence ALTN83 : 5 satellites (dont SAT-005 `DESORBITE`), 3 orbites (2 SSO + 1 LEO), 4 instruments, 5 fenêtres (3 Réalisée + 2 Planifiée), 3 stations (latitude/longitude réelles).

### 2.3 Composants Compose (L1-C) — `Components.kt`
| Composant | Rôle |
|---|---|
| `SatelliteCard(satellite, onClick)` | Pastille colorée par statut, nom, format CubeSat, orbite. Carte grisée + texte DÉSORBITÉ pour SAT-005 |
| `StatusBadge(statut)` | Chip Material3 colorée — vert/orange/rouge/gris |
| `FenetreCard(fenetre, nomStation)` | Durée formatée, badge statut Planifiée/Réalisée/Annulée, volume si dispo |
| `InstrumentItem(instrument)` | Type, modèle, résolution avec `N/A` si NULL, indicateur état |

`@Preview` pour chaque composant.

### 2.4 DashboardScreen (L1-D)
- `LazyColumn` des satellites avec `SatelliteCard`
- Barre de recherche `TextField` filtrant par nom ou type d'orbite (temps réel)
- Compteur "{X}/{N} satellites opérationnels"
- Gestion visuelle des satellites désorbités (interaction désactivée — miroir trigger T1)
- Affichage du chargement / erreur

Réponses Q1-Q3 du sujet en commentaire dans le code (`LazyColumn` vs `Column`, intérêt de l'enum, blocage SAT-005).

---

## 3. Phase 2 — Architecture MVVM & API REST

Réorganisation 3 couches : **Models** (pur Kotlin) → **ViewModel** (StateFlow) → **Vue** (Compose).

### 3.1 NanoOrbitViewModel (L2-A) — `NanoOrbitViewModel.kt`

**StateFlow exposés :**
- `isLoading: StateFlow<Boolean>`
- `errorMessage: StateFlow<String?>`
- `searchQuery: StateFlow<String>`
- `selectedStatut: StateFlow<StatutSatellite?>`
- `filteredSatellites: StateFlow<List<Satellite>>` (calculé via `combine()` — testabilité côté ViewModel)
- `fenetres`, `stations`, `instruments`, `missions`
- `isOffline: StateFlow<Boolean>`

**Fonctions publiques :** `loadSatellites()`, `refreshData()`, `onSearchQueryChange()`, `onStatutFilterChange()`, `loadSatelliteDetails()`, `addFenetre()`. Le tout dans `viewModelScope.launch` avec `try/catch` qui bascule l'état en mode hors-ligne.

`Factory : ViewModelProvider.Factory` pour injecter le `Repository`.

### 3.2 Couche données (L2-B) — `NanoOrbitRepository.kt` + `NanoOrbitApi.kt`

**Interface Retrofit `NanoOrbitApi`** :
```
GET /satellites
GET /satellites/{id}/instruments
GET /satellites/{id}/missions
GET /fenetres
GET /stations
```
DTOs séparés (`SatelliteApiResponse`, `FenetreApiResponse`, etc.) pour découpler le réseau du domaine.

**Repository** : abstraction de la source de données, conversion DTO → Entity → Domain, exposition de `Flow<List<Satellite>>` et `Flow<List<FenetreCom>>` issus de Room.

### 3.3 Connexion Vue ↔ ViewModel (L2-C)
Dans `DashboardScreen` (et autres) :
- `viewModel: NanoOrbitViewModel = viewModel(factory = …)`
- `collectAsStateWithLifecycle()` sur chaque StateFlow
- `CircularProgressIndicator` pendant `isLoading`
- Bannière d'erreur + bouton Réessayer si `errorMessage != null`
- Filtres `FilterChip` sur `StatutSatellite` + chip `Tous`, compteur de résultats temps réel

### 3.4 Validation RG-F04 (L2-D)
`NanoOrbitRepository.validateFenetreDuree(duree: Int)` retourne le message d'erreur si `duree !in 1..900`. Comparaison avec le trigger Oracle T3/CHECK commentée dans le code. Utilisée dans `PlanningScreen` avant tout INSERT.

---

## 4. Phase 3 — Fonctionnalités Avancées

### 4.1 Navigation Compose (L3-A) — `Routes.kt` + `MainActivity.kt`

`sealed class Screen` avec 4 routes :
| Route | Écran | Paramètres |
|---|---|---|
| `dashboard` | `DashboardScreen` | — |
| `detail/{satelliteId}` | `DetailScreen` | `satelliteId: String` |
| `planning` | `PlanningScreen` | — |
| `map` | `MapScreen` | — |

`NavHost` central dans `MainScreen` avec transitions `fadeIn/slideInVertically`. `BottomNavigationBar` (3 onglets Dashboard / Planning / Carte) **masquée sur DetailScreen** via `if (currentRoute != null && !currentRoute.startsWith("detail"))`.

### 4.2 DetailScreen (L3-B) — `DetailScreen.kt`
- TopAppBar avec nom du satellite et bouton Retour (`ArrowBack`)
- Section Statut (StatusBadge, format, type d'orbite, altitude)
- Section Télémétrie (masse, capacité batterie avec indicateur visuel, durée de vie restante estimée)
- Section Instruments embarqués (`LazyColumn` de `InstrumentItem`)
- Section Missions actives + rôle du satellite
- Bouton "Signaler une anomalie" → dialog de saisie + validation

### 4.3 PlanningScreen (L3-C) — `PlanningScreen.kt`
- Sélecteur de station via `FilterChip`
- Liste triée par `datetime_debut` des `FenetreCard`
- Indicateur durée totale + volume total planifié
- Couleurs distinctes : Planifiée (bleu), Réalisée (vert), Annulée (rouge)
- Validation côté client : durée [1–900 s] (RG-F04) + satellite non désorbité (miroir RG-S06)
- Bouton "+" pour ajouter une fenêtre, déléguant à `viewModel.addFenetre()`

### 4.4 Persistance Room — Cache-First (L3-D)

**Entités** (`Entities.kt`) :
- `SatelliteEntity` (table `satellites`) — tous les champs + `lastUpdated: Long`
- `FenetreEntity` (table `fenetres_com`)

**DAOs** (`Daos.kt`) avec `getAllSatellites(): Flow<List<SatelliteEntity>>` + `insertSatellites()` / `getNextId()` etc.

**`AppDatabase`** : RoomDatabase singleton.

**Stratégie Cache-First** dans `NanoOrbitRepository` :
- `getSatellitesFlow()` lit Room en premier (Flow réactif)
- `refreshSatellites()` appelle l'API et met à jour le cache
- Bannière "Mode hors-ligne" affichée quand `isOffline = true` (`errorMessage = "Mode hors-ligne (serveur indisponible)"`)
- Lien explicite avec **ALTN83 Q3** commenté dans `NanoOrbitRepository.kt` : équivalent mobile de la "réplique locale en lecture seule" de la fragmentation horizontale Phase 1 ALTN83

### 4.5 MapScreen — Cartographie OSM (L3-E) — `MapScreen.kt`
Intégration **osmdroid** (initialisé dans `MainActivity.onCreate()` via `Configuration.getInstance().load()`) :
- Carte OpenStreetMap avec marqueurs pour chaque station au sol
- Marqueurs colorés par état (Active/Maintenance/Hors service)
- Infobulle au clic : nom, bande de fréquence, débit max
- Bouton FAB "Me localiser" pour centrer la carte
- Affichage de la distance à chaque station si la géolocalisation est active

---

## 5. Bonus — Notifications locales (L3-F ★)

`FenetreWorker.kt` + enregistrement dans `MainActivity` :
- `CoroutineWorker` interrogeant Room pour les fenêtres `PLANIFIEE` dont `datetimeDebut` est dans **[0, 15] minutes** (`ChronoUnit.MINUTES.between(now, dt) in 0..15`)
- `PeriodicWorkRequest` toutes les 15 minutes
- Canal de notification `nanoorbit_passages` avec `IMPORTANCE_HIGH`
- Notification : titre "Passage imminent : {idSatellite}", contenu "Station {codeStation} - Début dans quelques minutes", `PRIORITY_HIGH`

---

## 6. Synergie ALTN82 / ALTN83

Les trois points de cohérence imposés par le sujet sont implémentés :

| Point de synergie | Implémentation côté Android |
|---|---|
| **Modèles de données** | `data class Satellite` strictement alignée sur la table `SATELLITE` Oracle (types, noms, contraintes). Enums `StatutSatellite`, `FormatCubeSat`, `StatutFenetre` reflétant les `CHECK` Oracle |
| **Règle RG-F04** | `NanoOrbitRepository.validateFenetreDuree(duree)` retourne une erreur identique côté client si la valeur sort de [1, 900] s — miroir du trigger Oracle T3 / CHECK constraint |
| **Disponibilité réseau (Q3)** | Stratégie Cache-First Room + bannière hors-ligne. Réponse explicite à la question Q3 ALTN83 commentée dans `NanoOrbitRepository.getSatellitesFlow()` |

---

## 7. Architecture du code

```
fr.efrei.nanooribt/
├── MainActivity.kt           ← entry point, NavHost, init Retrofit/Room/WorkManager
├── Routes.kt                 ← Screen sealed class + bottomNavItems
├── Models.kt                 ← data classes + enums (domaine pur)
├── MockData.kt               ← Phase 1 mock
├── Components.kt             ← SatelliteCard, StatusBadge, FenetreCard, InstrumentItem
├── DashboardScreen.kt        ← liste + recherche + filtres + compteur
├── DetailScreen.kt           ← fiche satellite (5 sections + dialog anomalie)
├── PlanningScreen.kt         ← fenêtres com. + validation RG-F04/RG-S06
├── MapScreen.kt              ← osmdroid + géolocalisation
├── NanoOrbitViewModel.kt     ← MVVM, StateFlow, combine() pour le filtrage
├── NanoOrbitRepository.kt    ← Cache-First Room + Retrofit + lien ALTN83 Q3
├── NanoOrbitApi.kt           ← interface Retrofit + DTOs
├── AppDatabase.kt            ← Room database
├── Entities.kt               ← SatelliteEntity, FenetreEntity + mappers
├── Daos.kt                   ← SatelliteDao, FenetreDao
└── FenetreWorker.kt          ← bonus WorkManager
```

Total : ~3 000 lignes Kotlin (hors thème UI), 19 fichiers source.
