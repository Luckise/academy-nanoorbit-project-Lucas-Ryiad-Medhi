# 🛰️ NanoOrbit — Projet fil rouge EFREI S8

> **Modules concernés :** ALTN83 — Bases de données réparties · ALTN82 — Développement Mobile Android  
> **Niveau :** 2ème année cycle ingénieur (Semestre 8)  
> **Campus :** EFREI — Bordeaux  
> **Année :** 2025–2026

---

## 🌍 Contexte

NanoOrbit est une startup fictive qui exploite une constellation de **CubeSats** pour surveiller des zones climatiques sensibles : déforestation, fonte des glaces, qualité de l'air, évolution du trait de côte.

Ce dépôt regroupe l'ensemble des ressources pédagogiques du projet fil rouge NanoOrbit, commun aux deux modules ALTN83 et ALTN82. Les deux projets sont **intentionnellement cohérents** : mêmes entités, mêmes identifiants, mêmes règles métier.

---

## 🗂️ Structure du dépôt

```
nanoorbit/
├── README.md                        ← ce fichier
├── .gitignore
│
├── altn83-bdd/                      ← Module Bases de données réparties
│   ├── README.md
│   ├── sujets/                      ← Énoncés des 4 phases
│   │   ├── ALTN83_NanoOrbit_Projet_Fil_Rouge.pdf
│   │   ├── ALTN83_NanoOrbit_CDC_Phase1.pdf
│   │   └── ALTN83_NanoOrbit_AnnexeA_Donnees_Reference.pdf
│   ├── donnees/                     ← Jeu de données de référence (CSV)
│   │   ├── ALTN83_NanoOrbit_01_ORBITE.csv
│   │   ├── ALTN83_NanoOrbit_02_SATELLITE.csv
│   │   ├── ALTN83_NanoOrbit_03_INSTRUMENT.csv
│   │   ├── ALTN83_NanoOrbit_04_EMBARQUEMENT.csv
│   │   ├── ALTN83_NanoOrbit_05_CENTRE_CONTROLE.csv
│   │   ├── ALTN83_NanoOrbit_06_STATION_SOL.csv
│   │   ├── ALTN83_NanoOrbit_07_AFFECTATION_STATION.csv
│   │   ├── ALTN83_NanoOrbit_08_MISSION.csv
│   │   ├── ALTN83_NanoOrbit_09_FENETRE_COM.csv
│   │   └── ALTN83_NanoOrbit_10_PARTICIPATION.csv
│   └── scripts/                     ← Scripts SQL Oracle
│       ├── ALTN83_NanoOrbit_Phase2_DML.sql
│       └── ALTN83_NanoOrbit_Memo_SQL_PLSQL.pdf
│
└── altn82-android/                  ← Module Développement Mobile Android
    ├── README.md
    ├── sujets/                      ← Énoncés des TPs
    │   ├── ALTN82_TP01_Android.pdf
    │   ├── ALTN82_TP02_BordeauxVeloLib.pdf
    │   ├── ALTN82_TP03_MVVM_Navigation.pdf
    │   ├── ALTN82_TP04_Cartographie.pdf
    │   └── ALTN82_NanoOrbit_Projet_Android.pdf
    └── starter/                     ← Projet Android de démarrage
        └── README.md
```

---

## 🔗 Synergie entre les deux modules

| Point de cohérence | ALTN83 — Oracle | ALTN82 — Android |
|---|---|---|
| **Modèles de données** | Table `SATELLITE` avec types Oracle | `data class Satellite` en Kotlin |
| **Règle RG-F04** | `CHECK (duree BETWEEN 1 AND 900)` | Validation côté client avant envoi |
| **Règle RG-S06** | Trigger `trg_valider_fenetre` (ORA-20001) | Message d'erreur affiché dans l'UI |
| **Mode hors-ligne** | Q3 Phase 1 : continuité si serveur indisponible | Room Cache-First + bannière offline |

---

## 📦 Jeu de données de référence

Les **10 fichiers CSV** dans `altn83-bdd/donnees/` constituent le jeu de données commun aux deux modules. Tous les exercices des Phases 2, 3 et 4 référencent ces identifiants.

| # | Table | Lignes | Points clés |
|---|---|---|---|
| 01 | `ORBITE` | 3 | SSO × 2, LEO × 1 |
| 02 | `SATELLITE` | 5 | 3 Opérationnels, 1 En veille, **1 Désorbité (SAT-005)** |
| 03 | `INSTRUMENT` | 4 | Résolution NULL sur INS-AIS-01 |
| 04 | `EMBARQUEMENT` | 7 | PK composite, états variés |
| 05 | `CENTRE_CONTROLE` | 3 | Paris · Houston · Singapour |
| 06 | `STATION_SOL` | 3 | **GS-SGP-01 en Maintenance** |
| 07 | `AFFECTATION_STATION` | 3 | PK composite |
| 08 | `MISSION` | 3 | 2 Actives, **1 Terminée (MSN-DEF-2022)** |
| 09 | `FENETRE_COM` | 5 | 3 Réalisées, 2 Planifiées (volume NULL) |
| 10 | `PARTICIPATION` | 7 | PK composite, rôles variés |

> **Format CSV :** séparateur `;` · encodage UTF-8 BOM · valeurs NULL = cellule vide

---

## ⚡ Cas limites à connaître

Ces valeurs du jeu de données sont **intentionnellement là pour tester les contraintes**.

- **SAT-005** (`Désorbité`) → doit déclencher ORA-20001 sur tout INSERT dans `FENETRE_COM`
- **GS-SGP-01** (`Maintenance`) → doit déclencher ORA-20002 sur tout INSERT dans `FENETRE_COM`
- **MSN-DEF-2022** (`Terminée`) → doit déclencher ORA-20004 sur tout INSERT dans `PARTICIPATION`
- **INS-AIS-01** (`resolution = NULL`) → exploité dans les exercices NVL (Palier 2)
- **Fenêtres 4 et 5** (`volume_donnees = NULL`) → vérifiées par le trigger T3

---

## 🚀 Démarrage rapide — ALTN83

```bash
# 1. Connexion Oracle
sqlplus NANOORBIT_ADMIN/[password]@FREEPDB1

# 2. Créer le schéma (DDL — livrable L2-A)
@chemin/vers/votre_DDL.sql

# 3. Charger les données de référence
@altn83-bdd/scripts/ALTN83_NanoOrbit_Phase2_DML.sql

# 4. Vérifier
SELECT table_name, num_rows FROM user_tables ORDER BY table_name;
```

---

## 🚀 Démarrage rapide — ALTN82

```bash
# Cloner le dépôt
git clone https://github.com/ntiacademy/nanoorbit.git

# Ouvrir le projet starter dans Android Studio
# File > Open > nanoorbit/altn82-android/starter/
```

---

## 📋 Conventions de nommage des livrables étudiants

```
# ALTN83
GROUPE_NomA_NomB_NanoOrbit_Phase1.pdf
GROUPE_NomA_NomB_NanoOrbit_Phase2.sql
GROUPE_NomA_NomB_NanoOrbit_Phase3.sql
GROUPE_NomA_NomB_NanoOrbit_Phase4.sql

# ALTN82
GROUPE_NomA_NomB_NanoOrbit_Android.zip
```

---

## ⚠️ Plagiat

> Tout partage de code ou de modèle entre groupes est sanctionné par la note **0/20** et une convocation en conseil de discipline.  
> Les différences de choix de modélisation entre groupes sont **attendues et valorisées**.

---

## 📬 Contact

**Module ALTN83** — Khadija SLIMANI · Marwa HARZI  
**Module ALTN82** — Christian KHOURY  
**Responsables** — Youssef AIT EL MAHJOUB · Marwa HARZI  

*EFREI Paris Panthéon-Assas Université — Département Logiciel et systèmes d'information*
