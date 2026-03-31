# 📱 ALTN82 — Développement Mobile Android

> **Module :** ALTN82 · Semestre 8 · EFREI  
> **Stack :** Kotlin · Jetpack Compose · MVVM · Room · Retrofit · osmdroid

---

## 📐 Organisation du module

| Séance | Thème | Concepts clés |
|---|---|---|
| **TP 1** | Ma première application connectée | Compose, State, LazyColumn, API REST |
| **TP 2** | Bordeaux VéloLib | Retrofit, données réelles, filtrage temps réel |
| **TP 3** | MVVM & Navigation | ViewModel, StateFlow, NavHost, Géolocalisation |
| **TP 4** | Cartographie OSM | osmdroid, marqueurs, BottomNavigationBar |
| **Projet** | NanoOrbit Ground Control | Toutes les notions + Room + cohérence ALTN83 |

---

## 📁 Contenu de ce dossier

### `sujets/`
- **`ALTN82_TP01_Android.pdf`** — Installation, Compose, navigation, API
- **`ALTN82_TP02_BordeauxVeloLib.pdf`** — Retrofit, LazyColumn, données réelles
- **`ALTN82_TP03_MVVM_Navigation.pdf`** — Architecture MVVM, NavHost, GPS
- **`ALTN82_TP04_Cartographie.pdf`** — osmdroid, marqueurs, BottomNav
- **`ALTN82_NanoOrbit_Projet_Android.pdf`** — Sujet du projet fil rouge (3 phases)

### `starter/`
Projet Android Studio vide pré-configuré avec :
- Dépendances Gradle déjà déclarées (Retrofit, Navigation, Room, osmdroid)
- `Models.kt` vide à compléter (data classes cohérentes avec le MLD Oracle)
- Structure de packages suggérée

---

## 🏗️ Architecture cible du projet

```
com.efrei.nanoorbit/
├── data/
│   ├── models/          ← data classes Kotlin (miroir du MLD Oracle)
│   ├── api/             ← NanoOrbitApi (Retrofit)
│   ├── db/              ← Room (SatelliteEntity, FenetreEntity)
│   └── repository/      ← NanoOrbitRepository (Cache-First)
├── ui/
│   ├── dashboard/       ← DashboardScreen + SatelliteCard
│   ├── detail/          ← DetailScreen
│   ├── planning/        ← PlanningScreen
│   └── map/             ← MapScreen (osmdroid)
└── viewmodel/
    └── NanoOrbitViewModel.kt
```

---

## 🔗 Cohérence avec ALTN83

Les data classes Kotlin doivent correspondre aux tables Oracle :

```kotlin
// Kotlin                          Oracle
data class Satellite(              -- SATELLITE
    val idSatellite: String,       -- id_satellite   VARCHAR2(20)
    val nomSatellite: String,      -- nom_satellite  VARCHAR2(100)
    val statut: StatutSatellite,   -- statut         VARCHAR2(30) CHECK
    val formatCubesat: String,     -- format_cubesat VARCHAR2(5)  CHECK
    val idOrbite: Int              -- id_orbite      NUMBER FK
)

enum class StatutSatellite {
    OPERATIONNEL, EN_VEILLE, DEFAILLANT, DESORBITE
    // Miroir exact du CHECK Oracle sur la colonne statut
}
```

**Règle RG-F04 côté client** — avant tout envoi :
```kotlin
if (dureeSecondes !in 1..900) {
    // Même message que ORA-20010 dans pkg_nanoOrbit
    showError("Durée invalide : entre 1 et 900 secondes")
}
```

---

## 📋 Livrables attendus

```
GROUPE_NomA_NomB_NanoOrbit_Android.zip   ← projet Android Studio complet (sans /build)
README.md dans le ZIP                     ← 1 page : choix techniques + lien ALTN83
```
