# 📊 Jeu de données de référence — NanoOrbit

Ces 10 fichiers CSV constituent le **jeu de données commun** aux modules ALTN83 et ALTN82.  
Tous les exercices des Phases 2, 3 et 4 référencent ces identifiants.

## Format

- Séparateur : `;`
- Encodage : UTF-8 BOM (compatible ouverture directe dans Excel FR)
- Valeurs NULL : cellule vide (entre deux `;`)

## Ordre d'insertion Oracle

Les fichiers sont numérotés dans l'ordre strict des dépendances FK :

```
01 ORBITE               → pas de dépendance
02 SATELLITE            → dépend de ORBITE
03 INSTRUMENT           → pas de dépendance
04 EMBARQUEMENT         → dépend de SATELLITE + INSTRUMENT
05 CENTRE_CONTROLE      → pas de dépendance
06 STATION_SOL          → pas de dépendance
07 AFFECTATION_STATION  → dépend de CENTRE_CONTROLE + STATION_SOL
08 MISSION              → pas de dépendance
09 FENETRE_COM          → dépend de SATELLITE + STATION_SOL
10 PARTICIPATION        → dépend de SATELLITE + MISSION
```

## Cas limites intentionnels

| Valeur | Raison |
|---|---|
| `SAT-005` statut `Désorbité` | Teste trigger T1 (ORA-20001) et règle RG-S06 |
| `GS-SGP-01` statut `Maintenance` | Teste trigger T1 (ORA-20002) et règle RG-G03 |
| `MSN-DEF-2022` statut `Terminée` | Teste trigger T4 (ORA-20004) et règle RG-M04 |
| `INS-AIS-01` résolution vide | Teste la gestion du NULL avec NVL (Palier 2 Ex.4) |
| Fenêtres 4 et 5 volume vide | Planifiées → volume NULL (trigger T3, règle RG-F05) |
