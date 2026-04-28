package fr.efrei.nanooribt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import fr.efrei.nanooribt.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

private const val CAMERA_HFOV_DEG = 65.0
private const val CAMERA_VFOV_DEG = 50.0
private const val MIN_ELEVATION_DEG = -5.0

private data class TrackedSatellite(
    val id: String,
    val name: String,
    val accent: Color,
    val sky: OrbitalMechanics.SkyPosition,
    val info: List<Pair<String, String>>
)

private data class DeviceOrientation(
    val azimuthDeg: Float,
    val elevationDeg: Float
)

private sealed class TleState {
    object Loading : TleState()
    data class Ready(val elements: List<TleElement>) : TleState()
    data class Error(val message: String) : TleState()
}

@Composable
fun ARScreen(viewModel: NanoOrbitViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        ARContent(onBack = onBack)
    } else {
        PermissionRationale(
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = onBack
        )
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Surface0).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CAMERA REQUIRED", style = MaterialTheme.typography.labelLarge,
                color = TextTertiary, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "AR Sky-Track needs camera access to overlay live satellite positions.",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SpaceWhite, contentColor = SpaceBlack)
            ) { Text("GRANT ACCESS", letterSpacing = 1.5.sp) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Surface3, contentColor = TextPrimary)
            ) { Text("BACK", letterSpacing = 1.5.sp) }
        }
    }
}

@Composable
private fun ARContent(onBack: () -> Unit) {
    val orientation = rememberDeviceOrientation()
    val location = rememberObserverLocation()
    val tleState = rememberTleData()
    val tracked = rememberTrackedFromTle(tleState, location)
    var selected by remember { mutableStateOf<TrackedSatellite?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Surface0)) {
        CameraPreview(modifier = Modifier.fillMaxSize())

        SkyOverlay(
            tracked = tracked,
            orientation = orientation,
            onSatelliteClick = { selected = it },
            modifier = Modifier.fillMaxSize()
        )

        TopBarOverlay(
            azimuthDeg = orientation.azimuthDeg,
            elevationDeg = orientation.elevationDeg,
            location = location,
            visibleCount = tracked.count { it.sky.isAboveHorizon },
            tleState = tleState,
            onBack = onBack
        )

        Crosshair(modifier = Modifier.align(Alignment.Center))

        selected?.let { sat ->
            SatelliteInfoCard(
                sat = sat,
                onClose = { selected = null },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun rememberDeviceOrientation(): DeviceOrientation {
    val context = LocalContext.current
    var state by remember { mutableStateOf(DeviceOrientation(0f, 0f)) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotMat = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                // Rear camera looks along -device Z. World frame: X=East, Y=North, Z=Up.
                val lookE = -rotMat[2]
                val lookN = -rotMat[5]
                val lookU = -rotMat[8]
                val az = Math.toDegrees(atan2(lookE.toDouble(), lookN.toDouble()))
                val normAz = ((az + 360.0) % 360.0).toFloat()
                val el = Math.toDegrees(asin(lookU.coerceIn(-1f, 1f).toDouble())).toFloat()
                state = DeviceOrientation(normAz, el)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return state
}

@Composable
private fun rememberObserverLocation(): Location? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            try {
                location = lm.getProviders(true)
                    .mapNotNull { @Suppress("MissingPermission") lm.getLastKnownLocation(it) }
                    .maxByOrNull { it.time }
            } catch (_: SecurityException) { }

            val listener = android.location.LocationListener { location = it }
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    @Suppress("MissingPermission")
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, listener)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    @Suppress("MissingPermission")
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener)
                }
            } catch (_: SecurityException) { }

            onDispose { runCatching { lm.removeUpdates(listener) } }
        } else {
            onDispose { }
        }
    }
    return location
}

@Composable
private fun rememberTleData(): TleState {
    var state by remember { mutableStateOf<TleState>(TleState.Loading) }
    LaunchedEffect(Unit) {
        try {
            val list = CelesTrakClient.api.getGroup(group = "visual", format = "json")
            state = TleState.Ready(list)
        } catch (e: Exception) {
            state = TleState.Error(e.message ?: "TLE fetch failed")
        }
    }
    return state
}

@Composable
private fun rememberTrackedFromTle(
    tleState: TleState,
    location: Location?
): List<TrackedSatellite> {
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { tick = System.currentTimeMillis(); delay(500) }
    }
    val elements = (tleState as? TleState.Ready)?.elements ?: emptyList()

    return remember(elements, location, tick) {
        val obsLat = location?.latitude ?: 48.8566 // Paris fallback
        val obsLon = location?.longitude ?: 2.3522

        elements.mapNotNull { el ->
            val epoch = runCatching { OrbitalMechanics.parseOmmEpoch(el.epoch) }.getOrNull()
                ?: return@mapNotNull null
            val sky = runCatching {
                OrbitalMechanics.skyPositionFromElements(
                    epoch = epoch,
                    meanMotionRevPerDay = el.meanMotion,
                    eccentricity = el.eccentricity,
                    inclinationDeg = el.inclinationDeg,
                    raanDeg = el.raanDeg,
                    argPericenterDeg = el.argPericenterDeg,
                    meanAnomalyAtEpochDeg = el.meanAnomalyDeg,
                    obsLatDeg = obsLat,
                    obsLonDeg = obsLon,
                    timeMs = tick
                )
            }.getOrNull() ?: return@mapNotNull null

            val periodMin = if (el.meanMotion > 0) 1440.0 / el.meanMotion else 0.0
            val altKm = approxAltitudeKm(el.meanMotion)

            TrackedSatellite(
                id = el.noradId?.toString() ?: el.objectName,
                name = el.objectName,
                accent = pickAccent(el.objectName),
                sky = sky,
                info = listOfNotNull(
                    el.noradId?.let { "NORAD ID" to it.toString() },
                    "PERIOD" to "${"%.1f".format(periodMin)} min",
                    "ALTITUDE" to "${altKm.toInt()} km",
                    "INCLINATION" to "${"%.1f".format(el.inclinationDeg)}°",
                    "ECCENTRICITY" to "%.4f".format(el.eccentricity),
                    "AZIMUTH" to "${sky.azimuthDeg.toInt()}°",
                    "ELEVATION" to "${sky.elevationDeg.toInt()}°",
                    "RANGE" to "${sky.rangeKm.toInt()} km"
                )
            )
        }
    }
}

private fun approxAltitudeKm(meanMotionRevPerDay: Double): Double {
    if (meanMotionRevPerDay <= 0) return 0.0
    val n = meanMotionRevPerDay * 2 * PI / 86400.0
    val a = (398600.4418 / (n * n)).pow(1.0 / 3.0)
    return a - 6371.0
}

/**
 * Stable color buckets so satellites keep their color across recompositions
 * without needing a full state-color table.
 */
private fun pickAccent(name: String): Color {
    val palette = listOf(
        Color(0xFF00E676), Color(0xFF4DA6FF), Color(0xFFFFAB00),
        Color(0xFFFF80AB), Color(0xFFB388FF), Color(0xFF80DEEA)
    )
    var h = 0
    for (c in name) h = h * 31 + c.code
    return palette[((h % palette.size) + palette.size) % palette.size]
}

private data class MarkerHit(
    val tracked: TrackedSatellite,
    val x: Float,
    val y: Float,
    val onScreen: Boolean
)

@Composable
private fun SkyOverlay(
    tracked: List<TrackedSatellite>,
    orientation: DeviceOrientation,
    onSatelliteClick: (TrackedSatellite) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            letterSpacing = 0.06f
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }
    val subPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(220, 200, 200, 200)
            textSize = 20f
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val markers = remember(tracked, orientation, canvasSize) {
        if (canvasSize == IntSize.Zero) emptyList()
        else computeMarkers(tracked, orientation, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(markers) {
                detectTapGestures { tap ->
                    val tapRadius = 70f
                    val nearest = markers.filter { it.onScreen }
                        .minByOrNull { hypot(tap.x - it.x, tap.y - it.y) }
                    if (nearest != null && hypot(tap.x - nearest.x, tap.y - nearest.y) <= tapRadius) {
                        onSatelliteClick(nearest.tracked)
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            markers.forEach { m ->
                val ts = m.tracked
                val accent = ts.accent
                if (!m.onScreen) {
                    drawCircle(color = accent.copy(alpha = 0.4f), radius = 7f, center = Offset(m.x, m.y))
                    drawCircle(color = Color.White, radius = 7f, center = Offset(m.x, m.y),
                        style = Stroke(width = 1.5f))
                    return@forEach
                }
                drawCircle(color = accent.copy(alpha = 0.15f), radius = 36f, center = Offset(m.x, m.y))
                drawCircle(color = accent, radius = 18f, center = Offset(m.x, m.y),
                    style = Stroke(width = 2.5f))
                drawCircle(color = accent, radius = 4f, center = Offset(m.x, m.y))
                drawContext.canvas.nativeCanvas.apply {
                    val sub = "${ts.sky.elevationDeg.toInt()}° EL  ${ts.sky.rangeKm.toInt()} KM"
                    drawText(ts.name.uppercase(), m.x + 26f, m.y - 4f, labelPaint)
                    drawText(sub, m.x + 26f, m.y + 22f, subPaint)
                }
            }
        }
    }
}

private fun computeMarkers(
    tracked: List<TrackedSatellite>,
    orientation: DeviceOrientation,
    w: Float,
    h: Float
): List<MarkerHit> {
    val cx = w / 2f
    val cy = h / 2f
    val halfH = (CAMERA_HFOV_DEG / 2.0).toFloat()
    val halfV = (CAMERA_VFOV_DEG / 2.0).toFloat()
    return tracked.mapNotNull { ts ->
        if (ts.sky.elevationDeg < MIN_ELEVATION_DEG) return@mapNotNull null
        var deltaAz = (ts.sky.azimuthDeg - orientation.azimuthDeg).toFloat()
        deltaAz = ((deltaAz + 540f) % 360f) - 180f
        val deltaEl = (ts.sky.elevationDeg.toFloat() - orientation.elevationDeg)
        val onScreen = abs(deltaAz) <= halfH && abs(deltaEl) <= halfV
        if (onScreen) {
            val sx = cx + (deltaAz / halfH) * (w / 2f)
            val sy = cy + (-deltaEl / halfV) * (h / 2f)
            MarkerHit(ts, sx, sy, true)
        } else {
            val ax = deltaAz / halfH
            val ay = -deltaEl / halfV
            val k = max(abs(ax), abs(ay)).coerceAtLeast(1f)
            val inset = 24f
            val sx = (cx + (ax / k) * (w / 2f - inset)).coerceIn(inset, w - inset)
            val sy = (cy + (ay / k) * (h / 2f - inset)).coerceIn(inset, h - inset)
            MarkerHit(ts, sx, sy, false)
        }
    }
}

@Composable
private fun TopBarOverlay(
    azimuthDeg: Float,
    elevationDeg: Float,
    location: Location?,
    visibleCount: Int,
    tleState: TleState,
    onBack: () -> Unit
) {
    val cardinal = compassCardinal(azimuthDeg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AR SKY-TRACK", style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary, letterSpacing = 3.sp)
                Text(
                    text = when (tleState) {
                        TleState.Loading -> "Fetching satellites…"
                        is TleState.Ready -> "$visibleCount of ${tleState.elements.size} above horizon"
                        is TleState.Error -> "TLE error"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${azimuthDeg.toInt()}° $cardinal", color = TextPrimary,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("EL ${elevationDeg.toInt()}°", color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        when (tleState) {
            is TleState.Error -> Text(
                "Source CelesTrak unreachable — ${tleState.message}",
                color = Color(0xFFFF5252),
                style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp
            )
            else -> {
                val locText = if (location == null) "GPS unavailable — Paris fallback observer"
                else "OBS  ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                val color = if (location == null) Color(0xFFFFAB00) else TextTertiary
                Text(locText, color = color,
                    style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
            }
        }
    }
}

@Composable
private fun Crosshair(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.7f), CircleShape)
        )
    }
}

@Composable
private fun SatelliteInfoCard(
    sat: TrackedSatellite,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xF20A0A0A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(sat.accent)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(sat.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp)
                    if (sat.id != sat.name) {
                        Text(sat.id, style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary, letterSpacing = 0.5.sp)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            sat.info.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Text(label, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 1.5.sp)
                    Text(value, style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun compassCardinal(azimuth: Float): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((azimuth + 22.5f) / 45f).toInt() % 8
    return dirs[(idx + 8) % 8]
}
