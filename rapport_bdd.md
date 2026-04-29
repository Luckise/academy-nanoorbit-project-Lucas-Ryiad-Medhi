# Rapport ALTN83 — NanoOrbit / Bases de Données Réparties

> Module : ALTN83 — Bases de Données Réparties
> Sujet : `altn83-bdd/sujets/ALTN83_NanoOrbit_Projet_BD_Réparties.pdf`
> Cahier des charges Phase 1 : `ALTN83_NanoOrbit_AnnexeA_CDC_Phase1.pdf`
> Données de référence : `ALTN83_NanoOrbit_AnnexeB_Donnees_Reference.pdf`

Ce rapport résume la solution livrée pour répondre au sujet ALTN83. Le détail des scripts, schémas et résultats se trouve dans les fichiers `Phase1.md`, `phase2.md`, `phase3.md`, `phase4.md` à la racine du dépôt, dans `mcd/` (MCD) et dans `screens/` (captures d'écran des résultats des triggers et exercices PL/SQL).

---

## 1. Couverture du sujet

| Phase | Niveau visé | Livrable du sujet | Statut |
|---|---|---|---|
| Phase 1 — Conception & Architecture distribuée | Niveau 1 | L1-A à L1-D | Réalisé |
| Phase 2 — Schéma Oracle & Triggers | Niveau 1 + bonus | L2-A à L2-D + 2 triggers bonus | Réalisé |
| Phase 3 — PL/SQL & Package `pkg_nanoOrbit` | Niveau 1 + bonus | L3-A à L3-D (Paliers 1–6) | Réalisé |
| Phase 4 — Exploitation avancée & Optimisation | Niveau 2 (Excellence) | L4-A à L4-D + rapport de pilotage | Réalisé |

Niveau visé : **Niveau 2 — Bonus complet** (80 pts socle + 45 pts bonus).

---

## 2. Phase 1 — Conception & Architecture distribuée

Documenté dans `Phase1.md`.

### 2.1 Dictionnaire des données (L1-A)
Tableau complet des 11 tables (`ORBITE`, `SATELLITE`, `INSTRUMENT`, `EMBARQUEMENT`, `CENTRE_CONTROLE`, `STATION_SOL`, `AFFECTATION_STATION`, `MISSION`, `FENETRE_COM`, `PARTICIPATION`, `HISTORIQUE_STATUT`) avec types Oracle, NOT NULL/UNIQUE et classification de chaque règle de gestion (Structure / Contrainte / Mécanisme procédural).

### 2.2 MCD (L1-B)
Schéma `mcd/mcd.webp` couvrant :
- L'entité-association porteuse `EMBARQUEMENT (date_integration, etat_fonctionnement)` — RG-S04
- L'entité-association porteuse `PARTICIPATION (role_satellite)` — RG-M03
- La relation binaire `FENETRE_COM` ↔ `SATELLITE` ↔ `STATION_SOL`
- L'intégration de `CENTRE_CONTROLE` via `AFFECTATION_STATION`

### 2.3 MLD (L1-C)
10 tables relationnelles + `HISTORIQUE_STATUT` (technique pour T5), avec PK soulignées, FK annotées `#`, types Oracle cibles. Vérification 3NF. Colonnes candidates à l'indexation identifiées (FK de `FENETRE_COM`, `statut` de `SATELLITE`).

### 2.4 Note de modélisation — Architecture distribuée (L1-D)

| Question | Réponse retenue |
|---|---|
| **Q1 — Tables locales** | `FENETRE_COM` (planification opérationnelle propre à chaque centre) et `HISTORIQUE_STATUT` (journal d'audit local alimenté par T5) |
| **Q2 — Tables globales** | `ORBITE`, `SATELLITE`, `INSTRUMENT`, `EMBARQUEMENT`, `CENTRE_CONTROLE`, `STATION_SOL`, `MISSION`, `PARTICIPATION` — réplication en lecture seule sur chaque nœud |
| **Q3 — Continuité Singapour** | Fragmentation horizontale : chaque centre dispose d'un nœud autonome avec son fragment local de `FENETRE_COM` + une réplique en lecture seule des tables globales |
| **Q4 — Risques de cohérence** | Scénario 1 : mise à jour simultanée du statut d'un satellite (Paris=`En veille` vs Houston=`Désorbité`) → mitigation 2PC. Scénario 2 : insertion sur réplique obsolète d'un satellite entre-temps désorbité → lecture maître obligatoire à l'insertion. Lecture du théorème CAP : disponibilité privilégiée vs cohérence. |

---

## 3. Phase 2 — Schéma Oracle & Triggers

Documenté dans `phase2.md`. Connexion `NANOORBIT_ADMIN` sur `FREEPDB1`.

### 3.1 Script DDL complet (L2-A)
11 tables créées dans l'ordre des dépendances FK :
`ORBITE → SATELLITE → INSTRUMENT → EMBARQUEMENT → CENTRE_CONTROLE → STATION_SOL → AFFECTATION_STATION → MISSION → FENETRE_COM → PARTICIPATION → HISTORIQUE_STATUT`.

Exigences couvertes :
- **E1** : tous NOT NULL sauf `MISSION.date_fin` et `FENETRE_COM.volume_donnees`
- **E2** : ENUM via CHECK sur `statut`, `format_cubesat`, `etat_fonctionnement`, `type_orbite`
- **E3** : `UNIQUE (altitude, inclinaison)` sur `ORBITE` (RG-O02)
- **E4** : `CHECK (duree BETWEEN 1 AND 900)` sur `FENETRE_COM` (RG-F04)
- **E5** : FK ON DELETE RESTRICT (par défaut Oracle)
- **E6** : PK composites sur `EMBARQUEMENT` et `PARTICIPATION`

`FENETRE_COM.id_fenetre` et `HISTORIQUE_STATUT.id_historique` en `GENERATED ALWAYS AS IDENTITY`.

### 3.2 Script DML (L2-B)
Jeu de données de référence inséré dans l'ordre référentiel : 3 orbites · 5 satellites (1 Désorbité = SAT-005) · 4 instruments (résolution NULL sur INS-AIS-01) · 7 embarquements · 2 centres (CTR-001 Paris, CTR-002 Houston) · 3 stations (GS-SGP-01 en Maintenance) · 3 affectations · 3 missions (1 Terminée = MSN-DEF-2022) · 5 fenêtres (3 Réalisée, 2 Planifiée) · 7 participations. `COMMIT` final.

### 3.3 Script Triggers (L2-C) — 5 triggers

| Trigger | Niveau | Événement | Règle |
|---|---|---|---|
| **T1 — `trg_valider_fenetre`** | 1 | BEFORE INSERT FENETRE_COM | Bloque si satellite Désorbité (RG-S06) ou station Maintenance (RG-G03) — `ORA-20101/20102` |
| **T2 — `trg_no_chevauchement`** | 1 | BEFORE INSERT/UPDATE FENETRE_COM | Pas de chevauchement satellite (RG-F02) ni station (RG-F03) — `ORA-20201/20202` |
| **T3 — `trg_volume_realise`** | 1 | BEFORE INSERT/UPDATE FENETRE_COM | Force `volume_donnees = NULL` si statut ≠ Réalisée (RG-F05) — correctif non bloquant |
| **T4 — `trg_mission_terminee`** | 2 (bonus) | BEFORE INSERT PARTICIPATION | Bloque l'ajout sur mission Terminée (RG-M04) — `ORA-20301` |
| **T5 — `trg_historique_statut`** | 2 (bonus) | AFTER UPDATE OF statut SATELLITE | Trace `(:OLD.statut, :NEW.statut, SYSTIMESTAMP)` dans `HISTORIQUE_STATUT` |

Chaque trigger est testé avec un cas valide et un cas en erreur. Captures dans `screens/t1.png` à `screens/t5.png`.

### 3.4 Script de contrôle (L2-D)
Vérifications sur `user_tables`, `user_constraints`, `user_cons_columns`, `user_triggers` + comptages par table + contrôles métier (répartition statuts, fenêtre sur satellite désorbité, contenu `HISTORIQUE_STATUT`).

---

## 4. Phase 3 — PL/SQL & Package `pkg_nanoOrbit`

Documenté dans `phase3.md`. `SET SERVEROUTPUT ON` en tête.

### 4.1 Paliers 1 à 5 — 16 exercices (L3-A)

| Palier | Exercices | Notion couverte |
|---|---|---|
| 1 — Bloc anonyme | Ex 1, 2 | DECLARE/BEGIN/END, DBMS_OUTPUT, SELECT INTO sur SAT-001 |
| 2 — Variables & types | Ex 3, 4 | `%ROWTYPE` sur SATELLITE, `NVL` sur résolution INS-AIS-01 |
| 3 — Structures de contrôle | Ex 5, 6, 7 | IF/ELSIF (catégorisation SAT-004), CASE (vitesse orbitale), boucle FOR (grille volumes) |
| 4 — Curseurs | Ex 8, 9, 10, 11 | `SQL%ROWCOUNT`, Cursor FOR Loop, OPEN/FETCH/CLOSE explicite, curseur paramétré sur GS-KIR-01 |
| 5 — Procédures & fonctions | Ex 12, 13, 14, 15, 16 | NO_DATA_FOUND, RAISE_APPLICATION_ERROR, `afficher_statut_satellite`, `mettre_a_jour_statut` (OUT), fonction `calculer_volume_session` |

Captures de chaque résultat attendu dans `screens/q1_phase3.png` à `screens/q16_phase3.png`.

### 4.2 Palier 6 (bonus) — Package `pkg_nanoOrbit`

**SPEC (L3-B)** :
- Type public `t_stats_satellite` (nb_fenetres, volume_total, duree_moy_secondes)
- Constantes : `c_statut_min_fenetre`, `c_duree_max_fenetre = 900`, `c_seuil_revision = 50`
- 4 procédures : `planifier_fenetre`, `cloturer_fenetre`, `affecter_satellite_mission`, `mettre_en_revision`
- 3 fonctions : `calculer_volume_theorique`, `statut_constellation`, `stats_satellite`

**BODY (L3-C)** : implémentation complète avec gestion des exceptions `-20xxx`. Les procédures déclenchent volontairement les triggers de la Phase 2 (T1 via `planifier_fenetre`, T3 via `cloturer_fenetre`, T4 via `affecter_satellite_mission`, T5 via `mettre_en_revision`).

**Scénario de validation (L3-D)** : bloc enchaîné qui exécute les 7 sous-programmes (planifier SAT-001 → GS-KIR-01, clôturer 1100 Mo, affecter SAT-004 à MSN-ARC-2023, mettre SAT-002 en révision, calculer volume théorique, stats SAT-001, statut constellation) avec `ROLLBACK` final.

---

## 5. Phase 4 — Exploitation avancée & Optimisation (Niveau 2 ★)

Documenté dans `phase4.md`.

### 5.1 Vues (L4-A)
| Vue | Type | Contenu |
|---|---|---|
| `v_satellites_operationnels` | Vue simple filtrée | Satellites Opérationnels + orbite + nb instruments + capacité batterie |
| `v_fenetres_detail` | Vue jointure dénormalisée | 5 jointures (FENETRE_COM, SATELLITE, STATION_SOL, AFFECTATION_STATION, CENTRE_CONTROLE) + durée formatée `Xmin Ys` |
| `v_stats_missions` | Vue avec agrégats | Par mission : nb satellites, types d'orbites (`LISTAGG`), volume total |
| `mv_volumes_mensuels` | Vue matérialisée | `BUILD IMMEDIATE REFRESH ON DEMAND`, agrégats mensuels par centre × format CubeSat |

### 5.2 CTE & Sous-requêtes (L4-B)
- **Ex 5** — CTE simple : Top 3 satellites par volume téléchargé
- **Ex 6** — CTE multiples (`fenetres_mois`, `stats_centre`, `station_active` avec `RANK()`) : analyse comparative par centre
- **Ex 7** — CTE récursive : hiérarchie Centre → Station → Fenêtres avec indentation `LPAD(' ', niveau*4)`
- **Ex 8** — Sous-requête scalaire : fenêtres au-dessus de la moyenne + écart à la moyenne
- **Ex 9** — Sous-requête corrélée : dernière fenêtre Réalisée par satellite via `MAX(datetime_debut)`
- **Ex 10** — `NOT EXISTS` : satellites sans fenêtre Réalisée (SAT-004, SAT-005) + stations sans fenêtre Q1-2024 (GS-SGP-01)

### 5.3 Fonctions analytiques & MERGE (L4-C)
- **Ex 11** — `ROW_NUMBER` / `RANK` / `DENSE_RANK` global et `RANK() OVER (PARTITION BY type_orbite)`
- **Ex 12** — `LAG` / `LEAD` sur `GS-KIR-01` avec calcul de l'évolution en %
- **Ex 13** — `SUM OVER (... ROWS UNBOUNDED PRECEDING)` cumulés + `AVG OVER (... ROWS BETWEEN 2 PRECEDING)` (moyenne mobile sur 3 fenêtres)
- **Ex 14** — Tableau de bord constellation combinant `RANK`, part %, cumul décroissant, écart moyenne
- **Ex 15** — `MERGE INTO SATELLITE` (flux IoT : update statut + orbite si match, insert avec statut `En veille` sinon)
- **Ex 16** — `MERGE INTO AFFECTATION_STATION` (synchronisation fichier de configuration), avec création préalable de CTR-003 Singapour

### 5.4 Index & EXPLAIN PLAN (L4-D)
6 index stratégiques :
- `idx_fenetre_satellite`, `idx_fenetre_station` — FK FENETRE_COM
- `idx_participation_mission` — FK PARTICIPATION
- `idx_satellite_statut`, `idx_satellite_statut_orbite` (composite)
- `idx_fenetre_mois` — index fonctionnel `TRUNC(datetime_debut, 'MM')`

**Ex 18** — `EXPLAIN PLAN` sur reporting mensuel (4 jointures + GROUP BY) avec interprétation TABLE ACCESS FULL → INDEX RANGE SCAN.
**Ex 19** — Démonstration `ALTER INDEX … INVISIBLE/VISIBLE` sur `idx_satellite_statut` avec comparaison des plans.

### 5.5 Rapport de pilotage intégral
Requête finale combinant 3 CTE (`volumes_centre_mois`, `centres_avec_evolution`, `satellites_rattaches`), la vue matérialisée `mv_volumes_mensuels`, et les fonctions analytiques `LAG` (évolution mensuelle), `RANK` (classement centres et satellites), `SUM OVER` (part %).

---

## 6. Synthèse des règles de gestion couvertes

| Règle | Mécanisme |
|---|---|
| RG-S01 (id satellite unique) | PK |
| RG-S04 (instrument multi-satellites) | PK composite EMBARQUEMENT |
| RG-S06 (satellite désorbité bloqué) | Trigger T1 + T4 |
| RG-O02 (unicité altitude+inclinaison) | UNIQUE constraint |
| RG-M01 (date_fin nullable) | NOT NULL hors `date_fin` |
| RG-M03 (rôle satellite-mission) | PK composite PARTICIPATION |
| RG-M04 (mission terminée bloquée) | Trigger T4 |
| RG-F02 (chevauchement satellite) | Trigger T2 |
| RG-F03 (chevauchement station) | Trigger T2 |
| RG-F04 (durée 1–900s) | CHECK constraint |
| RG-F05 (volume NULL si pas Réalisée) | Trigger T3 |
| RG-G03 (station maintenance bloquée) | Trigger T1 |

---

## 7. Synergie avec ALTN82 (Android)

Les trois points de cohérence sont assurés :
- **Modèles** : la structure des tables Oracle est miroir des `data class` Kotlin (cf. `rapport_android.md`)
- **RG-F04** : `CHECK (duree BETWEEN 1 AND 900)` côté Oracle ↔ `validateFenetreDuree()` côté Android
- **Q3 hors-ligne** : la stratégie de fragmentation horizontale (réplique locale) discutée en Phase 1 est implémentée concrètement par le cache Room Cache-First côté Android (commenté explicitement dans `NanoOrbitRepository.kt`)
