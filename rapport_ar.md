# Rapport Bonus — Fonctionnalité AR Sky-Track

> Module : ALTN82 — Développement Mobile Android (livrable bonus hors sujet de base)
> Stack : Kotlin · Jetpack Compose · CameraX · SensorManager (TYPE_ROTATION_VECTOR) · LocationManager · Retrofit · API CelesTrak (TLE/OMM)
> Branche : `Bonus-AR`

Ce rapport décrit la fonctionnalité de réalité augmentée ajoutée à l'application NanoOrbit Ground Control. Elle superpose en temps réel sur le flux caméra du téléphone la position des satellites visibles à l'œil nu depuis la position GPS de l'observateur. Le code source se trouve dans `altn82-android/starter/app/src/main/java/fr/efrei/nanooribt/`.

---

## 1. Objectif et périmètre

L'application existante affiche les satellites de la constellation NanoOrbit sous forme de listes, fiches détaillées, planning de fenêtres de communication et carte des stations sol. Le bonus AR ajoute une cinquième vue, accessible depuis la barre de navigation basse via l'icône étoile, qui permet de **pointer le téléphone vers le ciel et de voir s'afficher en surimpression les satellites réellement présents au-dessus de la position de l'observateur**.

Concrètement la vue AR :

- récupère en direct le catalogue des satellites « visibles à l'œil nu » depuis l'API publique CelesTrak (groupe `visual`, ~150 objets) ;
- propage la trajectoire de chaque satellite à partir de ses éléments orbitaux (Kepler à deux corps) ;
- transforme la position ECI → ECEF → topocentrique (azimut, élévation, distance) pour la position GPS de l'utilisateur ;
- lit l'attitude du téléphone (azimut/élévation de l'axe optique de la caméra arrière) via le capteur `TYPE_ROTATION_VECTOR` ;
- projette les satellites visibles dans le champ de vision de la caméra et dessine leurs marqueurs en overlay sur le `PreviewView` CameraX.

C'est un livrable **bonus** : aucun élément du sujet ALTN82 n'imposait de feature AR. Les paliers Socle/Avancé/Excellence du sujet de base restent couverts par l'application déjà livrée.

---

## 2. Architecture de la feature

| Fichier | Rôle |
|---|---|
| `ARScreen.kt` | UI Compose complète : permission caméra, preview CameraX, overlay des marqueurs, barre d'état azimut/élévation, fiche détail satellite |
| `CelesTrakApi.kt` | Client Retrofit vers `https://celestrak.org/NORAD/elements/gp.php` + DTO `TleElement` mappé sur le format OMM JSON |
| `OrbitalMechanics.kt` | Propagateur képlérien (résolution de l'équation de Kepler par Newton-Raphson) + transformations ECI/ECEF/ENU |
| `MainActivity.kt` (modifié) | Ajoute la route `Screen.AR` dans le `NavHost` |
| `Routes.kt` (modifié) | Ajoute `Screen.AR` (icône `Star`, route `"ar"`) à la `bottomNavItems` |
| `AndroidManifest.xml` (modifié) | Permission `CAMERA` + `uses-feature` non-required pour la caméra et les capteurs |
| `libs.versions.toml` + `build.gradle.kts` (modifiés) | Ajout des quatre artefacts CameraX 1.3.4 (`core`, `camera2`, `lifecycle`, `view`) |

La feature ne dépend ni de Room, ni du backend Oracle/REST de NanoOrbit : elle est **autonome** vis-à-vis du reste de l'application. Le ViewModel `NanoOrbitViewModel` est passé en paramètre par cohérence d'architecture mais n'est pas utilisé par le rendu AR (les données satellites AR proviennent de CelesTrak en direct, pas du backend).

---

## 3. Pipeline temps réel

À chaque tick de 500 ms, le `Composable` `ARContent` recompose et exécute la chaîne suivante :

1. **Lecture capteurs** — `rememberDeviceOrientation()` enregistre un `SensorEventListener` sur `TYPE_ROTATION_VECTOR`. La matrice de rotation 3×3 est lue ; l'axe optique de la caméra arrière est `-Z` du repère device. On le projette sur le repère monde ENU pour obtenir azimut (`atan2(East, North)`) et élévation (`asin(Up)`).
2. **Position observateur** — `rememberObserverLocation()` enregistre des callbacks GPS et Network sur `LocationManager` (intervalle 5 s, 10 m). En l'absence de fix, fallback Paris (48.8566, 2.3522).
3. **Propagation orbitale** — pour chaque `TleElement` reçu de CelesTrak, `OrbitalMechanics.skyPositionFromElements(...)` calcule la position dans le ciel local :
   - `n = meanMotion · 2π / 86400` (rad/s) → demi-grand axe `a = ³√(GM / n²)`
   - propagation de l'anomalie moyenne : `M = M₀ + n · Δt`
   - résolution de Kepler `M = E − e sin E` par 8 itérations de Newton-Raphson
   - position perifocale → ECI par rotations 3-1-3 (ω, i, Ω)
   - ECI → ECEF via le temps sidéral de Greenwich (formule IAU 1982 simplifiée)
   - ECEF → topocentrique ENU → (azimut, élévation, distance)
4. **Projection écran** — `computeMarkers(...)` calcule pour chaque satellite l'écart d'azimut et d'élévation entre sa position et l'axe optique de la caméra. Si l'écart tient dans le champ de vision (HFOV 65°, VFOV 50°), le marqueur est dessiné à `(cx + Δaz/halfH · w/2, cy − Δel/halfV · h/2)`. Sinon, un petit indicateur hors-champ est tracé sur le bord de l'écran dans la direction du satellite.
5. **Rendu** — `androidx.compose.foundation.Canvas` dessine pour chaque marqueur visible un cercle de halo, un anneau coloré, un point central, le nom du satellite et une ligne secondaire avec l'élévation et la distance. Un tap dans un rayon de 70 px du marqueur le plus proche ouvre une `SatelliteInfoCard` Material3 listant NORAD ID, période orbitale, altitude, inclinaison, excentricité, azimut, élévation et distance.

L'ensemble est entièrement réactif Compose : aucune `View` legacy n'intervient hors `PreviewView` (CameraX) qui est un point d'interopérabilité obligé.

---

## 4. Mécanique orbitale — détails

Le propagateur implémenté est un **propagateur képlérien à deux corps**, et non SGP4. Cela suffit pour de l'AR éducatif sur orbite basse :

- pour des satellites avec `e < 0,01` (ISS, Starlink, NOAA, Tiangong…), l'erreur de position après quelques heures depuis l'époque OMM reste inférieure à la taille angulaire du marqueur affiché ;
- les corrections J2 (aplatissement terrestre) et la traînée atmosphérique sont volontairement ignorées : l'application rafraîchit les TLE à chaque ouverture de la vue, donc l'écart depuis l'époque est typiquement de quelques heures à quelques jours.

Une seconde fonction `skyPosition(satelliteId, altitudeKm, inclinationDeg, …)` (orbite circulaire seedée par un hash stable de l'identifiant) est conservée pour pouvoir afficher en AR des satellites du backend NanoOrbit qui ne disposeraient pas de TLE, mais elle n'est pas câblée dans la version actuelle.

Constantes physiques utilisées :
- `R_terre = 6371 km`
- `GM = 398 600,4418 km³/s²`
- `ω_terre = 7,2921159 · 10⁻⁵ rad/s`

---

## 5. Intégration application

### Navigation
`Routes.kt` ajoute `object AR : Screen("ar", "AR", Icons.Default.Star)` et l'inclut dans `bottomNavItems`. La barre de navigation passe ainsi de 3 à 4 onglets : Dashboard, Planning, Carte, AR. `MainActivity.kt` ajoute la `composable("ar") { ARScreen(...) }` correspondante au `NavHost`.

### Permissions runtime
`ARScreen` gère la permission caméra avec `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. Si l'utilisateur refuse, un `PermissionRationale` Compose explique pourquoi la caméra est requise et propose de relancer la demande ou de revenir en arrière. La permission de localisation est facultative — la fonctionnalité dégrade vers Paris si elle est absente, et un avertissement orange s'affiche dans la barre supérieure.

### Manifest
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="false" />
<uses-feature android:name="android.hardware.sensor.compass" android:required="false" />
```
Les `uses-feature` sont déclarées `required="false"` pour ne pas filtrer les téléphones sans capteur de rotation au niveau du Play Store : la feature dégrade gracieusement (orientation à 0/0) plutôt que de bloquer l'install de l'app entière.

### Dépendances ajoutées
| Artefact | Version |
|---|---|
| `androidx.camera:camera-core` | 1.3.4 |
| `androidx.camera:camera-camera2` | 1.3.4 |
| `androidx.camera:camera-lifecycle` | 1.3.4 |
| `androidx.camera:camera-view` | 1.3.4 |

Aucune dépendance Retrofit n'a été ajoutée : le client CelesTrak réutilise les artefacts `retrofit` et `retrofit-gson` déjà présents pour le backend Oracle.

---

## 6. Choix de design notables

- **API CelesTrak `GROUP=visual`** plutôt que `active` ou `stations` : ~150 objets visibles à l'œil nu, c'est-à-dire un compromis entre densité d'overlay (lisible) et richesse du contenu. `active` afficherait ~10 000 objets et noierait l'écran.
- **Capteur `TYPE_ROTATION_VECTOR`** plutôt que la fusion manuelle accéléromètre + magnétomètre : Android effectue déjà la fusion capteurs (incluant le gyroscope quand disponible), avec une latence et un jitter bien meilleurs.
- **`SensorManager.SENSOR_DELAY_GAME`** : ~20 ms, suffisant pour une UI fluide sans saturer le CPU.
- **Tick de recomposition 500 ms** : la position d'un satellite LEO change d'environ 0,5° par seconde dans le ciel ; rafraîchir à 2 Hz est imperceptible à l'œil et économise la batterie.
- **HFOV/VFOV en dur (65°/50°)** : valeurs typiques d'une caméra de smartphone. Une amélioration future serait de lire les `CameraCharacteristics` de la caméra retenue par CameraX et d'en déduire le FOV exact.
- **Indicateurs hors-champ** : projection sur le bord de l'écran dans la direction du satellite, pour aider l'utilisateur à trouver l'objet en bougeant le téléphone. Sans ça, l'AR ne montre que ce qui est déjà dans le cadre, ce qui est frustrant pour un utilisateur qui ne sait pas où regarder.
- **Couleur stable par satellite** : un hash FNV-1a du nom mappé sur une palette de 6 couleurs garantit qu'un satellite garde la même couleur à travers les recompositions sans avoir à maintenir une table d'état.

---

## 7. Limitations connues

- **Pas de SGP4** : la précision se dégrade au-delà de quelques jours depuis l'époque OMM. Acceptable parce que CelesTrak met à jour ses OMM toutes les 8 h et que l'app les retéléverse à chaque ouverture de la vue AR.
- **Pas de prise en compte de la calibration boussole** : si le magnétomètre du téléphone est mal calibré, l'azimut affiché sera biaisé. L'utilisateur doit faire un « ∞ » avec son téléphone à la première utilisation (geste Android standard).
- **Pas de filtrage par éclairage solaire** : un satellite « visible » au sens CelesTrak n'est réellement visible à l'œil nu que s'il est éclairé par le Soleil et que l'observateur est dans la nuit. L'overlay affiche aussi les satellites « théoriquement présents mais invisibles » de jour. Une amélioration serait de calculer la position du Soleil et de griser les satellites dans l'ombre de la Terre.
- **Fallback observateur Paris** : si le GPS n'est pas dispo, on retombe sur Paris. Pour un utilisateur loin de Paris, les positions affichées seront fausses tant que le fix GPS n'est pas obtenu.

---

## 8. Test manuel

Procédure de validation rapide à l'extérieur, ciel dégagé, de nuit :

1. Lancer l'application, accorder les permissions caméra et localisation.
2. Aller sur l'onglet AR (étoile, dernier onglet de la barre basse).
3. Vérifier en haut à droite que l'azimut indiqué correspond grossièrement à un point cardinal connu (pointer vers le nord magnétique → azimut proche de 0°/N).
4. Vérifier que la barre supérieure affiche `OBS lat, lon` cohérent avec la position réelle (pas le fallback Paris).
5. Vérifier que `X of Y above horizon` affiche un nombre non nul (en pratique entre 30 et 60 selon l'heure).
6. Pointer le téléphone vers le ciel et déplacer lentement : des marqueurs colorés doivent apparaître. Tap sur un marqueur → fiche détail avec NORAD ID et paramètres orbitaux.
7. Pour vérification croisée : ouvrir Heavens-Above ou Stellarium au même endroit/heure et vérifier que la position d'un satellite connu (ISS si visible, NORAD 25544) coïncide à quelques degrés près.

En intérieur ou sans GPS, la fonctionnalité reste démontrable mais les positions affichées seront cohérentes avec l'observateur Paris par défaut, pas avec la position réelle.
