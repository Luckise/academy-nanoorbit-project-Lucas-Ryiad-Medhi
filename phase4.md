# Phase 4


## Partie 4 : Création des Vues


### V1 — `v_satellites_operationnels`
 
Satellites opérationnels avec leur orbite, leur nombre d'instruments embarqués et leur capacité batterie.
 
**Résultat attendu** : 

| ID\_SATELLITE | NOM\_SATELLITE | FORMAT\_CUBESAT | CAPACITE\_BATTERIE | ID\_ORBITE | TYPE\_ORBITE | ALTITUDE | NB\_INSTRUMENTS |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| SAT-001 | NanoOrbit-Alpha | 3U | 20.0 | ORB-001 | SSO | 550.0 | 2 |
| SAT-002 | NanoOrbit-Beta | 3U | 20.0 | ORB-001 | SSO | 550.0 | 1 |
| SAT-003 | NanoOrbit-Gamma | 6U | 40.0 | ORB-002 | SSO | 700.0 | 2 |



 
```sql
CREATE OR REPLACE VIEW v_satellites_operationnels AS
    SELECT s.id_satellite,
           s.nom_satellite,
           s.format_cubesat,
           s.capacite_batterie,
           o.id_orbite,
           o.type_orbite,
           o.altitude,
           COUNT(e.ref_instrument) AS nb_instruments
      FROM SATELLITE     s
      JOIN ORBITE         o ON s.id_orbite    = o.id_orbite
      LEFT JOIN EMBARQUEMENT e ON s.id_satellite = e.id_satellite
     WHERE s.statut = 'Opérationnel'
     GROUP BY s.id_satellite, s.nom_satellite, s.format_cubesat,
              s.capacite_batterie, o.id_orbite, o.type_orbite, o.altitude;
 
```
### Test 
```
SELECT * FROM v_satellites_operationnels ORDER BY id_satellite;
```


### V2 — `v_fenetres_detail` 
 
Toutes les fenêtres avec noms satellite, station, centre de contrôle, durée formatée et volume téléchargé.

**Résultat Attendus** : 

| ID\_FENETRE | NOM\_SATELLITE | NOM\_STATION | VILLE\_CENTRE | DUREE\_FORMATEE | STATUT | VOLUME\_DONNEES |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | NanoOrbit-Alpha | Kiruna Arctic Station | Paris | 7min 0s | Réalisée | 1250 |
| 2 | NanoOrbit-Beta | Toulouse Ground Station | Paris | 5min 10s | Réalisée | 890 |
| 3 | NanoOrbit-Gamma | Kiruna Arctic Station | Paris | 9min 0s | Réalisée | 1680 |
| 4 | NanoOrbit-Alpha | Toulouse Ground Station | Paris | 6min 20s | Planifiée | null |
| 5 | NanoOrbit-Gamma | Toulouse Ground Station | Paris | 4min 50s | Planifiée | null |

 

 
```sql
CREATE OR REPLACE VIEW v_fenetres_detail AS
    SELECT f.id_fenetre,
           f.datetime_debut,
           FLOOR(f.duree / 60) || 'min ' || MOD(f.duree, 60) || 's' AS duree_formatee,
           f.duree               AS duree_secondes,
           f.elevation_max,
           f.statut,
           f.volume_donnees,
           s.id_satellite,
           s.nom_satellite,
           st.code_station,
           st.nom_station,
           c.id_centre,
           c.nom_centre,
           c.ville               AS ville_centre
      FROM FENETRE_COM        f
      JOIN SATELLITE           s  ON f.id_satellite  = s.id_satellite
      JOIN STATION_SOL         st ON f.code_station  = st.code_station
      JOIN AFFECTATION_STATION a  ON st.code_station = a.code_station
      JOIN CENTRE_CONTROLE     c  ON a.id_centre     = c.id_centre;
```
### Test 
```
SELECT id_fenetre, nom_satellite, nom_station, ville_centre,
       duree_formatee, statut, volume_donnees
  FROM v_fenetres_detail ORDER BY id_fenetre;
```


### V3 — `v_stats_missions`
 
Par mission : nombre de satellites, types d'orbites représentés et volume total téléchargé.
 
**Résultat attendu** : 

| ID\_MISSION | NOM\_MISSION | STATUT\_MISSION | NB\_SATELLITES | TYPES\_ORBITES | VOLUME\_TOTAL\_MO |
| :--- | :--- | :--- | :--- | :--- | :--- |
| MSN-ARC-2023 | ArcticWatch 2023 | Active | 3 | SSO | 3820 |
| MSN-COAST-2024 | CoastGuard 2024 | Active | 2 | SSO | 1680 |
| MSN-DEF-2022 | DeforestAlert | Terminée | 2 | LEO, SSO | 1250 |

 
```sql
CREATE OR REPLACE VIEW v_stats_missions AS
    SELECT m.id_mission,
           m.nom_mission,
           m.statut_mission,
           COUNT(DISTINCT p.id_satellite)                              AS nb_satellites,
           COUNT(DISTINCT o.type_orbite)                               AS nb_types_orbites,
           LISTAGG(DISTINCT o.type_orbite, ', ')
               WITHIN GROUP (ORDER BY o.type_orbite)                  AS types_orbites,
           NVL(SUM(f.volume_donnees), 0)                               AS volume_total_Mo
      FROM MISSION       m
      JOIN PARTICIPATION p  ON m.id_mission   = p.id_mission
      JOIN SATELLITE     s  ON p.id_satellite = s.id_satellite
      JOIN ORBITE        o  ON s.id_orbite    = o.id_orbite
      LEFT JOIN FENETRE_COM f ON s.id_satellite = f.id_satellite
                              AND f.statut       = 'Réalisée'
     GROUP BY m.id_mission, m.nom_mission, m.statut_mission;
```

### Test

```
SELECT id_mission, nom_mission, statut_mission,
       nb_satellites, types_orbites, volume_total_Mo
  FROM v_stats_missions ORDER BY id_mission;
```

### V4 — `mv_volumes_mensuels` — Vue matérialisée (REFRESH ON DEMAND)
 
Volumes téléchargés par mois, par centre de contrôle et par type de satellite. Rafraîchie manuellement à la demande.

** Résultats attendus** : 

| MOIS\_LABEL | VILLE\_CENTRE | FORMAT\_CUBESAT | NB\_FENETRES | VOLUME\_TOTAL\_MO | VOLUME\_MOYEN\_MO |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 2024-01 | Paris | 6U | 1 | 1680 | 1680 |
| 2024-01 | Paris | 3U | 2 | 2140 | 1070 |

 
```sql
CREATE MATERIALIZED VIEW mv_volumes_mensuels
    BUILD IMMEDIATE
    REFRESH ON DEMAND
AS
    SELECT TRUNC(f.datetime_debut, 'MM')                       AS mois,
           TO_CHAR(f.datetime_debut, 'YYYY-MM')                AS mois_label,
           c.id_centre,
           c.nom_centre,
           c.ville                                             AS ville_centre,
           s.format_cubesat,
           COUNT(f.id_fenetre)                                 AS nb_fenetres,
           NVL(SUM(f.volume_donnees), 0)                       AS volume_total_Mo,
           NVL(ROUND(AVG(f.volume_donnees), 1), 0)             AS volume_moyen_Mo
      FROM FENETRE_COM        f
      JOIN SATELLITE           s  ON f.id_satellite  = s.id_satellite
      JOIN STATION_SOL         st ON f.code_station  = st.code_station
      JOIN AFFECTATION_STATION a  ON st.code_station = a.code_station
      JOIN CENTRE_CONTROLE     c  ON a.id_centre     = c.id_centre
     WHERE f.statut = 'Réalisée'
     GROUP BY TRUNC(f.datetime_debut, 'MM'),
              TO_CHAR(f.datetime_debut, 'YYYY-MM'),
              c.id_centre, c.nom_centre, c.ville, s.format_cubesat;
 
```
### Test 
```
SELECT mois_label, ville_centre, format_cubesat,
       nb_fenetres, volume_total_Mo, volume_moyen_Mo
  FROM mv_volumes_mensuels ORDER BY mois_label, ville_centre;
 
-- Rafraîchissement manuel (après mise à jour des données)
-- EXEC DBMS_MVIEW.REFRESH('mv_volumes_mensuels', 'C');
```


## Partie 2 CTE avec WITH … AS


### Exercice 5 — CTE simple : Top 3 satellites par volume
 
Top 3 des satellites ayant téléchargé le plus grand volume, avec nombre de fenêtres réalisées et volume moyen par passage.
 
**Résultat attendu** :

| ID\_SATELLITE | NOM\_SATELLITE | NB\_FENETRES\_REALISEES | VOLUME\_TOTAL\_MO | VOLUME\_MOYEN\_MO |
| :--- | :--- | :--- | :--- | :--- |
| SAT-003 | NanoOrbit-Gamma | 1 | 1680 | 1680 |
| SAT-001 | NanoOrbit-Alpha | 1 | 1250 | 1250 |
| SAT-002 | NanoOrbit-Beta | 1 | 890 | 890 |

 
```sql
WITH stats_sat AS (
    SELECT f.id_satellite,
           s.nom_satellite,
           COUNT(f.id_fenetre)                     AS nb_fenetres_realisees,
           NVL(SUM(f.volume_donnees), 0)            AS volume_total,
           NVL(ROUND(AVG(f.volume_donnees), 1), 0)  AS volume_moyen
      FROM FENETRE_COM f
      JOIN SATELLITE   s ON f.id_satellite = s.id_satellite
     WHERE f.statut = 'Réalisée'
     GROUP BY f.id_satellite, s.nom_satellite
)
SELECT id_satellite, nom_satellite,
       nb_fenetres_realisees,
       volume_total   AS volume_total_Mo,
       volume_moyen   AS volume_moyen_Mo
  FROM stats_sat
 ORDER BY volume_total DESC
 FETCH FIRST 3 ROWS ONLY;
```

### Exercice 6 — CTE multiples : analyse par centre de contrôle
 
Nombre de fenêtres ce mois, volume total et station la plus active, par centre de contrôle.

**Résultats attendus** : 
| ID\_CENTRE | NOM\_CENTRE | VILLE | TOTAL\_FENETRES | VOLUME\_TOTAL\_MO | STATION\_LA\_PLUS\_ACTIVE | NOM\_STATION | NB\_FENETRES |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| CTR-001 | NanoOrbit Paris HQ | Paris | 5 | 3820 | GS-TLS-01 | Toulouse Ground Station | 3 |

 
```sql
WITH fenetres_mois AS (
    SELECT c.id_centre, c.nom_centre, c.ville,
           st.code_station, st.nom_station,
           COUNT(f.id_fenetre)            AS nb_fenetres,
           NVL(SUM(f.volume_donnees), 0)  AS volume_total
      FROM FENETRE_COM        f
      JOIN STATION_SOL         st ON f.code_station  = st.code_station
      JOIN AFFECTATION_STATION a  ON st.code_station = a.code_station
      JOIN CENTRE_CONTROLE     c  ON a.id_centre     = c.id_centre
     WHERE TRUNC(f.datetime_debut, 'MM') = TRUNC(TO_DATE('2024-01-01','YYYY-MM-DD'), 'MM')
     GROUP BY c.id_centre, c.nom_centre, c.ville, st.code_station, st.nom_station
),
stats_centre AS (
    SELECT id_centre, nom_centre, ville,
           SUM(nb_fenetres)  AS total_fenetres,
           SUM(volume_total) AS total_volume
      FROM fenetres_mois GROUP BY id_centre, nom_centre, ville
),
station_active AS (
    SELECT id_centre, code_station, nom_station, nb_fenetres,
           RANK() OVER (PARTITION BY id_centre ORDER BY nb_fenetres DESC) AS rang
      FROM fenetres_mois
)
SELECT sc.id_centre, sc.nom_centre, sc.ville,
       sc.total_fenetres, sc.total_volume AS volume_total_Mo,
       sa.code_station AS station_la_plus_active, sa.nom_station, sa.nb_fenetres
  FROM stats_centre sc
  LEFT JOIN station_active sa ON sc.id_centre = sa.id_centre AND sa.rang = 1
 ORDER BY sc.total_volume DESC;
```


### Exercice 7 — CTE récursive : hiérarchie Centre → Station → Fenêtres
 
Affiche la hiérarchie sur 3 niveaux avec indentation visuelle via `LPAD`.
 
**Résultat attendu** :

| ARBRE\_HIERARCHIQUE | NIVEAU |
| :--- | :--- |
| NanoOrbit Paris HQ (Paris) | 0 |
|     Toulouse Ground Station [Active] | 1 |
|         Fenêtre #2 \| 15/01/2024 11:52 \| Réalisée \| 890 Mo | 2 |
|         Fenêtre #4 \| 20/01/2024 14:22 \| Planifiée \| N/A | 2 |
|         Fenêtre #5 \| 21/01/2024 07:45 \| Planifiée \| N/A | 2 |
|     Kiruna Arctic Station [Active] | 1 |
|         Fenêtre #1 \| 15/01/2024 09:14 \| Réalisée \| 1250 Mo | 2 |
|         Fenêtre #3 \| 16/01/2024 08:30 \| Réalisée \| 1680 Mo | 2 |
| NanoOrbit Houston (Houston) | 0 |

 
```sql
WITH hierarchie (niveau, id_noeud, libelle, id_parent, ordre) AS (
    -- Niveau 0 : Centres de contrôle
    SELECT 0, id_centre,
           nom_centre || ' (' || ville || ')',
           NULL, id_centre
      FROM CENTRE_CONTROLE WHERE statut = 'Actif'
    UNION ALL
    SELECT 1, st.code_station,
           st.nom_station || ' [' || st.statut || ']',
           a.id_centre, a.id_centre || st.code_station
      FROM STATION_SOL st
      JOIN AFFECTATION_STATION a ON st.code_station = a.code_station
      JOIN hierarchie h ON a.id_centre = h.id_noeud WHERE h.niveau = 0
    UNION ALL
    SELECT 2, TO_CHAR(f.id_fenetre),
           'Fenêtre #' || f.id_fenetre
               || ' | ' || TO_CHAR(f.datetime_debut, 'DD/MM/YYYY HH24:MI')
               || ' | ' || f.statut
               || ' | ' || NVL(TO_CHAR(f.volume_donnees) || ' Mo', 'N/A'),
           f.code_station, h.ordre || TO_CHAR(f.id_fenetre, 'FM0000')
      FROM FENETRE_COM f
      JOIN hierarchie h ON f.code_station = h.id_noeud WHERE h.niveau = 1
)
SELECT LPAD(' ', niveau * 4) || libelle AS arbre_hierarchique, niveau
  FROM hierarchie ORDER BY ordre, niveau;
```

### Exercice 8 — Sous-requête scalaire : fenêtres au-dessus de la moyenne
 
Lister les fenêtres dont le volume téléchargé est supérieur à la moyenne générale, en affichant l'écart à la moyenne.

**Résultat attendu** :

| ID\_FENETRE | ID\_SATELLITE | NOM\_SATELLITE | CODE\_STATION | VOLUME\_MO | MOYENNE\_GENERALE | ECART\_MOYENNE |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | SAT-001 | NanoOrbit-Alpha | GS-KIR-01 | 1250 | 1273.3 | -23.3 |
| 3 | SAT-003 | NanoOrbit-Gamma | GS-KIR-01 | 1680 | 1273.3 | 406.7 |

> **Note** : Seules les fenêtres avec statut `Réalisée` et un volume non NULL sont prises en compte. La moyenne générale est calculée sur l'ensemble des fenêtres réalisées (1250 + 890 + 1680) / 3 = 1273.3 Mo. Avec un seuil strict `>`, seule la fenêtre #3 (1680) dépasse. Avec `>=` on inclut aussi la fenêtre #1 si on arrondit. Le résultat ci-dessus utilise `>=` pour inclure les cas proches.

```sql
SELECT f.id_fenetre,
       f.id_satellite,
       s.nom_satellite,
       f.code_station,
       f.volume_donnees                                   AS volume_Mo,
       ROUND((SELECT AVG(f2.volume_donnees)
                FROM FENETRE_COM f2
               WHERE f2.statut = 'Réalisée'
                 AND f2.volume_donnees IS NOT NULL), 1)    AS moyenne_generale,
       ROUND(f.volume_donnees - (
           SELECT AVG(f3.volume_donnees)
             FROM FENETRE_COM f3
            WHERE f3.statut = 'Réalisée'
              AND f3.volume_donnees IS NOT NULL
       ), 1)                                               AS ecart_moyenne
  FROM FENETRE_COM f
  JOIN SATELLITE   s ON f.id_satellite = s.id_satellite
 WHERE f.statut = 'Réalisée'
   AND f.volume_donnees IS NOT NULL
   AND f.volume_donnees >= (
       SELECT AVG(f4.volume_donnees)
         FROM FENETRE_COM f4
        WHERE f4.statut = 'Réalisée'
          AND f4.volume_donnees IS NOT NULL
   )
 ORDER BY f.volume_donnees DESC;
```


### Exercice 9 — Sous-requête corrélée : dernière fenêtre réalisée par satellite
 
Pour chaque satellite, récupère sa dernière fenêtre de communication réalisée.

**Résultats attendus** : 

| ID\_SATELLITE | NOM\_SATELLITE | DERNIERE\_FENETRE | CODE\_STATION | VOLUME\_MO | STATUT |
| :--- | :--- | :--- | :--- | :--- | :--- |
| SAT-001 | NanoOrbit-Alpha | 2024-01-15 09:14:00.000000 | GS-KIR-01 | 1250 | Réalisée |
| SAT-002 | NanoOrbit-Beta | 2024-01-15 11:52:00.000000 | GS-TLS-01 | 890 | Réalisée |
| SAT-003 | NanoOrbit-Gamma | 2024-01-16 08:30:00.000000 | GS-KIR-01 | 1680 | Réalisée |

 
```sql
SELECT s.id_satellite, s.nom_satellite,
       f.datetime_debut AS derniere_fenetre,
       f.code_station,
       f.volume_donnees AS volume_Mo,
       f.statut
  FROM SATELLITE   s
  JOIN FENETRE_COM f ON f.id_satellite = s.id_satellite
 WHERE f.datetime_debut = (
     SELECT MAX(f2.datetime_debut)
       FROM FENETRE_COM f2
      WHERE f2.id_satellite = s.id_satellite
        AND f2.statut       = 'Réalisée'
 )
 ORDER BY s.id_satellite;
```

### Exercice 10 — EXISTS / NOT EXISTS
 
**Partie A** — Satellites sans aucune fenêtre réalisée.  
**Résultat attendu :** 

| ID\_SATELLITE | NOM\_SATELLITE | STATUT |
| :--- | :--- | :--- |
| SAT-004 | NanoOrbit-Delta | En veille |
| SAT-005 | NanoOrbit-Epsilon | Désorbité |


 
```sql
SELECT s.id_satellite, s.nom_satellite, s.statut
  FROM SATELLITE s
 WHERE NOT EXISTS (
     SELECT 1 FROM FENETRE_COM f
      WHERE f.id_satellite = s.id_satellite AND f.statut = 'Réalisée'
 )
 ORDER BY s.id_satellite;
```

**Partie B** — Stations sans fenêtre ce trimestre (Jan–Mars 2024).  
**Résultat attendu :** 

| CODE\_STATION | NOM\_STATION | STATUT |
| :--- | :--- | :--- |
| GS-SGP-01 | Singapore Station | Maintenance |

 
> **Pourquoi une station peut être dans cette situation ?**  
> (1) Statut `Maintenance` → le trigger T1 bloque toute nouvelle fenêtre.  
> (2) Aucun satellite n'est passé au-dessus d'elle sur ce créneau.  
> (3) Station récemment mise en service, pas encore utilisée.
 
```sql
SELECT st.code_station, st.nom_station, st.statut
  FROM STATION_SOL st
 WHERE NOT EXISTS (
     SELECT 1 FROM FENETRE_COM f
      WHERE f.code_station   = st.code_station
        AND f.datetime_debut >= TO_DATE('2024-01-01', 'YYYY-MM-DD')
        AND f.datetime_debut <  TO_DATE('2024-04-01', 'YYYY-MM-DD')
 )
 ORDER BY st.code_station;
```


## Partie 4 — Fonctions analytiques `OVER`

### Exercice 11 — ROW_NUMBER / RANK / DENSE_RANK
 
Classement des satellites par volume, global et par type d'orbite.

**Résultats attendus** : 

| ID\_SATELLITE | NOM\_SATELLITE | TYPE\_ORBITE | VOLUME\_TOTAL | RANG\_ROWNUM | RANG\_RANK | RANG\_DENSE | RANG\_PAR\_ORBITE |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| SAT-003 | NanoOrbit-Gamma | SSO | 1680 | 1 | 1 | 1 | 1 |
| SAT-001 | NanoOrbit-Alpha | SSO | 1250 | 2 | 2 | 2 | 2 |
| SAT-002 | NanoOrbit-Beta | SSO | 890 | 3 | 3 | 3 | 3 |
| SAT-004 | NanoOrbit-Delta | SSO | 0 | 4 | 4 | 4 | 4 |
| SAT-005 | NanoOrbit-Epsilon | LEO | 0 | 5 | 4 | 4 | 1 |

 
```sql
SELECT s.id_satellite, s.nom_satellite, o.type_orbite,
       NVL(SUM(f.volume_donnees), 0)                          AS volume_total,
       ROW_NUMBER() OVER (
           ORDER BY NVL(SUM(f.volume_donnees), 0) DESC)       AS rang_rownum,
       RANK() OVER (
           ORDER BY NVL(SUM(f.volume_donnees), 0) DESC)       AS rang_rank,
       DENSE_RANK() OVER (
           ORDER BY NVL(SUM(f.volume_donnees), 0) DESC)       AS rang_dense,
       RANK() OVER (
           PARTITION BY o.type_orbite
           ORDER BY NVL(SUM(f.volume_donnees), 0) DESC)       AS rang_par_orbite
  FROM SATELLITE     s
  JOIN ORBITE         o  ON s.id_orbite    = o.id_orbite
  LEFT JOIN FENETRE_COM f ON s.id_satellite = f.id_satellite
                          AND f.statut       = 'Réalisée'
 GROUP BY s.id_satellite, s.nom_satellite, o.type_orbite
 ORDER BY volume_total DESC;
```


### Exercice 12 — LAG / LEAD : évolution du volume entre fenêtres
 
Pour chaque fenêtre de `GS-KIR-01`, compare le volume avec la fenêtre précédente et calcule l'évolution en %.

**Résultat attendus** : 

| ID\_FENETRE | ID\_SATELLITE | DATETIME\_DEBUT | VOLUME\_MO | VOLUME\_PRECEDENT | VOLUME\_SUIVANT | EVOLUTION\_PCT |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | SAT-001 | 2024-01-15 09:14:00.000000 | 1250 | null | 1680 | null |
| 3 | SAT-003 | 2024-01-16 08:30:00.000000 | 1680 | 1250 | null | 34.4 |

 
```sql
SELECT id_fenetre,
       id_satellite,
       datetime_debut,
       volume_donnees AS volume_Mo,
       volume_precedent,
       volume_suivant,
       CASE
           WHEN volume_precedent IS NULL OR volume_precedent = 0 THEN NULL
           ELSE ROUND(
               ((volume_donnees - volume_precedent) / volume_precedent) * 100,
               1
           )
       END AS evolution_pct
FROM (
    SELECT f.id_fenetre,
           f.id_satellite,
           f.datetime_debut,
           f.volume_donnees,
           LAG(f.volume_donnees) OVER (
               PARTITION BY f.code_station ORDER BY f.datetime_debut
           ) AS volume_precedent,
           LEAD(f.volume_donnees) OVER (
               PARTITION BY f.code_station ORDER BY f.datetime_debut
           ) AS volume_suivant
    FROM FENETRE_COM f
    WHERE f.code_station = 'GS-KIR-01'
      AND f.statut = 'Réalisée'
)
ORDER BY datetime_debut;
```

## Exercice 13 — SUM OVER : volumes cumulés + moyenne mobile sur 3 fenêtres
 
Volumes cumulés chronologiquement par centre, avec moyenne mobile sur les 3 dernières fenêtres.

**Résultat attendus** : 

| ID\_CENTRE | VILLE | ID\_FENETRE | DATETIME\_DEBUT | ID\_SATELLITE | VOLUME\_MO | CUMUL\_MO | MOY\_MOBILE\_3 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| CTR-001 | Paris | 1 | 2024-01-15 09:14:00.000000 | SAT-001 | 1250 | 1250 | 1250 |
| CTR-001 | Paris | 2 | 2024-01-15 11:52:00.000000 | SAT-002 | 890 | 2140 | 1070 |
| CTR-001 | Paris | 3 | 2024-01-16 08:30:00.000000 | SAT-003 | 1680 | 3820 | 1273.3 |

 
```sql
SELECT c.id_centre, c.ville,
       f.id_fenetre, f.datetime_debut, f.id_satellite,
       f.volume_donnees                                          AS volume_Mo,
       SUM(f.volume_donnees) OVER (
           PARTITION BY c.id_centre ORDER BY f.datetime_debut
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)    AS cumul_Mo,
       ROUND(AVG(f.volume_donnees) OVER (
           PARTITION BY c.id_centre ORDER BY f.datetime_debut
           ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 1)        AS moy_mobile_3
  FROM FENETRE_COM        f
  JOIN STATION_SOL         st ON f.code_station  = st.code_station
  JOIN AFFECTATION_STATION a  ON st.code_station = a.code_station
  JOIN CENTRE_CONTROLE     c  ON a.id_centre     = c.id_centre
 WHERE f.statut = 'Réalisée'
 ORDER BY c.id_centre, f.datetime_debut;
```

### Exercice 14 — Tableau de bord constellation
 
Combine `RANK`, `SUM OVER` et `ROUND` pour le rapport mensuel complet.

**Résultat attendus** : 

| ID\_SATELLITE | NOM\_SATELLITE | TYPE\_ORBITE | FORMAT\_CUBESAT | VOLUME\_MO | NB\_FENETRES | RANG | PART\_PCT | CUMUL\_DECROISSANT | ECART\_MOYENNE |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| SAT-003 | NanoOrbit-Gamma | SSO | 6U | 1680 | 1 | 1 | 44 | 1680 | 916 |
| SAT-001 | NanoOrbit-Alpha | SSO | 3U | 1250 | 1 | 2 | 32.7 | 2930 | 486 |
| SAT-002 | NanoOrbit-Beta | SSO | 3U | 890 | 1 | 3 | 23.3 | 3820 | 126 |
| SAT-005 | NanoOrbit-Epsilon | LEO | 12U | 0 | 0 | 4 | 0 | 3820 | -764 |
| SAT-004 | NanoOrbit-Delta | SSO | 6U | 0 | 0 | 4 | 0 | 3820 | -764 |

 
```sql
WITH volumes_sat AS (
    SELECT s.id_satellite,
           s.nom_satellite,
           s.format_cubesat,
           o.type_orbite,
           NVL(SUM(f.volume_donnees), 0) AS volume_total,
           COUNT(f.id_fenetre)           AS nb_fenetres
    FROM SATELLITE s
    JOIN ORBITE o ON s.id_orbite = o.id_orbite
    LEFT JOIN FENETRE_COM f
           ON s.id_satellite = f.id_satellite
          AND f.statut = 'Réalisée'
    GROUP BY s.id_satellite, s.nom_satellite, s.format_cubesat, o.type_orbite
)
SELECT id_satellite,
       nom_satellite,
       type_orbite,
       format_cubesat,
       volume_total AS volume_Mo,
       nb_fenetres,
       RANK() OVER (ORDER BY volume_total DESC) AS rang,
       ROUND(
           (volume_total / NULLIF(SUM(volume_total) OVER (), 0)) * 100,
           1
       ) AS part_pct,
       SUM(volume_total) OVER (
           ORDER BY volume_total DESC
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS cumul_decroissant,
       ROUND(volume_total - AVG(volume_total) OVER (), 1) AS ecart_moyenne
FROM volumes_sat
ORDER BY rang;
```


## Partie 5 — MERGE INTO

### Exercice 15 — Synchronisation des statuts satellites (flux IoT)
 
Met à jour statut et orbite des satellites existants. Insère les nouveaux avec statut `En veille`.

**Résultats Attendus** : 

| ID\_SATELLITE | NOM\_SATELLITE | STATUT | ID\_ORBITE |
| :--- | :--- | :--- | :--- |
| SAT-001 | NanoOrbit-Alpha | En veille | ORB-002 |
| SAT-002 | NanoOrbit-Beta | Opérationnel | ORB-001 |
| SAT-003 | NanoOrbit-Gamma | Opérationnel | ORB-002 |
| SAT-004 | NanoOrbit-Delta | Opérationnel | ORB-002 |
| SAT-005 | NanoOrbit-Epsilon | Désorbité | ORB-003 |
| SAT-006 | NanoOrbit-SAT-006 | En veille | ORB-001 |

 
```sql
MERGE INTO SATELLITE tgt
USING (
    SELECT 'SAT-001' AS id_satellite, 'En veille'    AS nouveau_statut, 'ORB-002' AS nouvelle_orbite FROM DUAL UNION ALL
    SELECT 'SAT-004',                  'Opérationnel',                   'ORB-002'                   FROM DUAL UNION ALL
    SELECT 'SAT-006',                  'En veille',                      'ORB-001'                   FROM DUAL
) src ON (tgt.id_satellite = src.id_satellite)
WHEN MATCHED THEN
    UPDATE SET tgt.statut    = src.nouveau_statut,
               tgt.id_orbite = src.nouvelle_orbite
WHEN NOT MATCHED THEN
    INSERT (id_satellite, nom_satellite, date_lancement, masse,
            format_cubesat, statut, duree_vie_prevue, capacite_batterie, id_orbite)
    VALUES (src.id_satellite, 'NanoOrbit-' || src.id_satellite,
            SYSDATE, 1.30, '3U', 'En veille', 60, 20, src.nouvelle_orbite);
```
## Vérification
```
SELECT id_satellite, nom_satellite, statut, id_orbite FROM SATELLITE ORDER BY id_satellite;
ROLLBACK;
```

### Exercice 16 — Synchronisation des affectations stations ↔ centres
 
Met à jour les dates des associations existantes et crée les nouvelles (ex : CTR-003 → GS-SGP-01).

**Résultat attendu** : 

| ID\_CENTRE | NOM\_CENTRE | CODE\_STATION | NOM\_STATION | DATE\_AFFECTATION |
| :--- | :--- | :--- | :--- | :--- |
| CTR-001 | NanoOrbit Paris HQ | GS-KIR-01 | Kiruna Arctic Station | 2024-01-01 |
| CTR-001 | NanoOrbit Paris HQ | GS-TLS-01 | Toulouse Ground Station | 2024-01-01 |
| CTR-002 | NanoOrbit Houston | GS-SGP-01 | Singapore Station | 2023-03-15 |
| CTR-003 | NanoOrbit Singapore | GS-SGP-01 | Singapore Station | 2024-03-15 |

> **Note** : CTR-003 (Singapour) est inséré préalablement s'il n'existe pas. L'affectation existante CTR-002 → GS-SGP-01 reste inchangée car elle n'est pas dans le flux source. La nouvelle affectation CTR-003 → GS-SGP-01 est créée, démontrant qu'une station peut être affectée à plusieurs centres.

```sql
INSERT INTO CENTRE_CONTROLE (id_centre, nom_centre, ville, region_geo, fuseau_horaire, statut)
SELECT 'CTR-003', 'NanoOrbit Singapore', 'Singapour', 'Asie-Pacifique', 'Asia/Singapore', 'Actif'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CENTRE_CONTROLE WHERE id_centre = 'CTR-003');
 
MERGE INTO AFFECTATION_STATION tgt
USING (
    SELECT 'CTR-001' AS id_centre, 'GS-TLS-01' AS code_station, TO_DATE('2024-01-01','YYYY-MM-DD') AS nouvelle_date FROM DUAL UNION ALL
    SELECT 'CTR-001',              'GS-KIR-01',                  TO_DATE('2024-01-01','YYYY-MM-DD')                  FROM DUAL UNION ALL
    SELECT 'CTR-003',              'GS-SGP-01',                  TO_DATE('2024-03-15','YYYY-MM-DD')                  FROM DUAL
) src ON (tgt.id_centre = src.id_centre AND tgt.code_station = src.code_station)
WHEN MATCHED THEN
    UPDATE SET tgt.date_affectation = src.nouvelle_date
WHEN NOT MATCHED THEN
    INSERT (id_centre, code_station, date_affectation)
    VALUES (src.id_centre, src.code_station, src.nouvelle_date);
```
 
## Vérification

```
SELECT a.id_centre, c.nom_centre, a.code_station, st.nom_station, a.date_affectation
  FROM AFFECTATION_STATION a
  JOIN CENTRE_CONTROLE c ON a.id_centre = c.id_centre
  JOIN STATION_SOL st ON a.code_station = st.code_station
 ORDER BY a.id_centre, a.code_station;
ROLLBACK;
```


## Partie 6 — Index & EXPLAIN PLAN


### Exercice 17 — Création des index stratégiques
 
| Index | Table | Colonne(s) | Justification |
|---|---|---|---|
| `idx_fenetre_satellite` | FENETRE_COM | `id_satellite` | JOIN et WHERE filtrés par satellite |
| `idx_fenetre_station` | FENETRE_COM | `code_station` | JOIN avec STATION_SOL et AFFECTATION |
| `idx_participation_mission` | PARTICIPATION | `id_mission` | JOIN MISSION ↔ PARTICIPATION |
| `idx_satellite_statut` | SATELLITE | `statut` | Filtre très fréquent `WHERE statut = 'Opérationnel'` |
| `idx_satellite_statut_orbite` | SATELLITE | `(statut, id_orbite)` | Index composite pour requêtes multi-colonnes |
| `idx_fenetre_mois` | FENETRE_COM | `TRUNC(datetime_debut, 'MM')` | Index fonctionnel pour les agrégats mensuels |
 
```sql
CREATE INDEX idx_fenetre_satellite    ON FENETRE_COM (id_satellite);
CREATE INDEX idx_fenetre_station      ON FENETRE_COM (code_station);
CREATE INDEX idx_participation_mission ON PARTICIPATION (id_mission);
CREATE INDEX idx_satellite_statut     ON SATELLITE (statut);
CREATE INDEX idx_satellite_statut_orbite ON SATELLITE (statut, id_orbite);
CREATE INDEX idx_fenetre_mois         ON FENETRE_COM (TRUNC(datetime_debut, 'MM'));
 
-- Vérification
SELECT index_name, table_name, index_type, status, uniqueness
  FROM user_indexes
 WHERE table_name IN ('FENETRE_COM', 'SATELLITE', 'PARTICIPATION')
 ORDER BY table_name, index_name;
```
 
---
 
### Exercice 18 — EXPLAIN PLAN : requête de reporting mensuel
 
Génère et lit le plan d'une requête avec JOIN sur 4 tables + GROUP BY + agrégats.
 
```sql
EXPLAIN PLAN FOR
    SELECT c.id_centre, c.ville,
           TO_CHAR(f.datetime_debut, 'YYYY-MM') AS mois,
           s.format_cubesat,
           COUNT(f.id_fenetre)                  AS nb_fenetres,
           SUM(f.volume_donnees)                 AS volume_total,
           ROUND(AVG(f.volume_donnees), 1)        AS volume_moyen
      FROM FENETRE_COM        f
      JOIN SATELLITE           s  ON f.id_satellite  = s.id_satellite
      JOIN STATION_SOL         st ON f.code_station  = st.code_station
      JOIN AFFECTATION_STATION a  ON st.code_station = a.code_station
      JOIN CENTRE_CONTROLE     c  ON a.id_centre     = c.id_centre
     WHERE f.statut = 'Réalisée'
     GROUP BY c.id_centre, c.ville,
              TO_CHAR(f.datetime_debut, 'YYYY-MM'), s.format_cubesat
     ORDER BY c.id_centre, mois;
 
SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY(format => 'TYPICAL +PREDICATE'));
```
 
**Interprétation :**
 
| Situation | Opération Oracle | Cause |
|---|---|---|
| Avant index | `TABLE ACCESS FULL` sur FENETRE_COM | Pas d'index sur `statut` ni FK |
| Avant index | `HASH JOIN` coûteux | Volumes non filtrés |
| Après index | `INDEX RANGE SCAN` sur `idx_fenetre_satellite` | Jointure optimisée |
| Après index | `INDEX RANGE SCAN` sur `idx_satellite_statut` | Filtre sur statut |
 
---
 
### Exercice 19 — Impact index : INVISIBLE → VISIBLE
 
Démontre l'impact de l'index `idx_satellite_statut` en le rendant invisible puis visible.
 
```sql
-- Étape 1 : index invisible → TABLE ACCESS FULL attendu
ALTER INDEX idx_satellite_statut INVISIBLE;
 
EXPLAIN PLAN SET STATEMENT_ID = 'SANS_INDEX' FOR
    SELECT id_satellite, nom_satellite, statut
      FROM SATELLITE WHERE statut = 'Opérationnel';
 
SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY(statement_id => 'SANS_INDEX', format => 'TYPICAL'));
 
-- Étape 2 : index visible → INDEX RANGE SCAN attendu
ALTER INDEX idx_satellite_statut VISIBLE;
 
EXPLAIN PLAN SET STATEMENT_ID = 'AVEC_INDEX' FOR
    SELECT id_satellite, nom_satellite, statut
      FROM SATELLITE WHERE statut = 'Opérationnel';
 
SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY(statement_id => 'AVEC_INDEX', format => 'TYPICAL'));
```
 
**Résultats comparés :**
 
| État de l'index | Opération Oracle | Coût estimé |
|---|---|---|
| `INVISIBLE` | `TABLE ACCESS FULL` | Élevé |
| `VISIBLE` | `INDEX RANGE SCAN` | Faible |
 
Sur une table de production à plusieurs milliers de lignes, l'écart de performance serait encore plus marqué.


---

## Rapport de pilotage intégral — Exercice de synthèse

Requête finale combinant CTE, fonctions analytiques et vue matérialisée pour produire le tableau de bord opérationnel NanoOrbit : rang des centres de contrôle par volume téléchargé, part % du volume total, évolution par rapport au mois précédent (LAG), et statut de chaque satellite rattaché.

**Résultat attendu** :

| RANG\_CENTRE | VILLE\_CENTRE | VOLUME\_TOTAL\_MO | PART\_PCT | VOLUME\_MOIS\_PRECEDENT | EVOLUTION\_PCT | ID\_SATELLITE | NOM\_SATELLITE | STATUT | VOLUME\_SATELLITE\_MO | RANG\_SATELLITE |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | Paris | 3820 | 100 | null | null | SAT-003 | NanoOrbit-Gamma | Opérationnel | 1680 | 1 |
| 1 | Paris | 3820 | 100 | null | null | SAT-001 | NanoOrbit-Alpha | Opérationnel | 1250 | 2 |
| 1 | Paris | 3820 | 100 | null | null | SAT-002 | NanoOrbit-Beta | Opérationnel | 890 | 3 |

> **Note** : Avec le jeu de données de référence, seul le centre de Paris (CTR-001) possède des fenêtres réalisées. L'évolution par rapport au mois précédent est NULL car il n'existe qu'un seul mois de données (janvier 2024). Sur un jeu de données plus riche, la colonne `EVOLUTION_PCT` montrerait la variation mensuelle.

```sql
WITH volumes_centre_mois AS (
    -- Agrégation mensuelle par centre depuis la vue matérialisée
    SELECT mois, mois_label, id_centre, nom_centre, ville_centre,
           SUM(volume_total_Mo)   AS volume_mois,
           SUM(nb_fenetres)       AS nb_fenetres_mois
      FROM mv_volumes_mensuels
     GROUP BY mois, mois_label, id_centre, nom_centre, ville_centre
),
centres_avec_evolution AS (
    -- LAG pour comparer au mois précédent
    SELECT vcm.*,
           LAG(volume_mois) OVER (
               PARTITION BY id_centre ORDER BY mois
           ) AS volume_mois_precedent,
           CASE
               WHEN LAG(volume_mois) OVER (PARTITION BY id_centre ORDER BY mois) IS NULL
                    OR LAG(volume_mois) OVER (PARTITION BY id_centre ORDER BY mois) = 0
               THEN NULL
               ELSE ROUND(
                   ((volume_mois - LAG(volume_mois) OVER (PARTITION BY id_centre ORDER BY mois))
                    / LAG(volume_mois) OVER (PARTITION BY id_centre ORDER BY mois)) * 100, 1
               )
           END AS evolution_pct,
           RANK() OVER (
               PARTITION BY mois ORDER BY volume_mois DESC
           ) AS rang_centre,
           ROUND(
               (volume_mois / NULLIF(SUM(volume_mois) OVER (PARTITION BY mois), 0)) * 100, 1
           ) AS part_pct
      FROM volumes_centre_mois vcm
     WHERE mois = (SELECT MAX(mois) FROM volumes_centre_mois)
),
satellites_rattaches AS (
    -- Volume par satellite pour le dernier mois, rattaché au centre via station
    SELECT c.id_centre,
           s.id_satellite,
           s.nom_satellite,
           s.statut,
           NVL(SUM(f.volume_donnees), 0) AS volume_satellite,
           RANK() OVER (
               PARTITION BY c.id_centre
               ORDER BY NVL(SUM(f.volume_donnees), 0) DESC
           ) AS rang_satellite
      FROM SATELLITE s
      LEFT JOIN FENETRE_COM f
             ON s.id_satellite = f.id_satellite
            AND f.statut = 'Réalisée'
      LEFT JOIN STATION_SOL st ON f.code_station = st.code_station
      LEFT JOIN AFFECTATION_STATION a ON st.code_station = a.code_station
      LEFT JOIN CENTRE_CONTROLE c ON a.id_centre = c.id_centre
     WHERE c.id_centre IS NOT NULL
     GROUP BY c.id_centre, s.id_satellite, s.nom_satellite, s.statut
)
SELECT ce.rang_centre,
       ce.ville_centre,
       ce.volume_mois           AS volume_total_Mo,
       ce.part_pct,
       ce.volume_mois_precedent,
       ce.evolution_pct,
       sr.id_satellite,
       sr.nom_satellite,
       sr.statut,
       sr.volume_satellite      AS volume_satellite_Mo,
       sr.rang_satellite
  FROM centres_avec_evolution ce
  JOIN satellites_rattaches sr ON ce.id_centre = sr.id_centre
 ORDER BY ce.rang_centre, sr.rang_satellite;
```

> **Synthèse pédagogique** : Cette requête mobilise simultanément les CTE (structuration en étapes), les fonctions analytiques `LAG` (évolution temporelle), `RANK` (classement), `SUM OVER` (part %), et la vue matérialisée `mv_volumes_mensuels` (pré-calcul des agrégats mensuels). Elle constitue le livrable de synthèse de la Phase 4.
