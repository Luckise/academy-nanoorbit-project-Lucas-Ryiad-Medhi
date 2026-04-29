package fr.efrei.nanooribt

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import fr.efrei.nanooribt.ui.theme.*
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Custom pin icon: white outer ring + accent inner dot + subtle glow
 */
fun createStationPin(context: Context): BitmapDrawable {
    val size = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = size / 2f
    val cy = size / 2f - size * 0.1f
    val outerRadius = size * 0.28f
    val innerRadius = size * 0.14f

    // Glow effect
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            cx, cy, outerRadius * 2f,
            intArrayOf(
                android.graphics.Color.argb(60, 255, 255, 255),
                android.graphics.Color.argb(20, 255, 255, 255),
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy, outerRadius * 2f, glowPaint)

    // Outer ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        strokeWidth = size * 0.04f
    }
    canvas.drawCircle(cx, cy, outerRadius, ringPaint)

    // Inner dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(cx, cy, innerRadius, dotPaint)

    // Drop line (pin stem)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.argb(120, 255, 255, 255)
        strokeWidth = size * 0.025f
    }
    val lineTop = cy + outerRadius + size * 0.04f
    val lineBottom = size.toFloat() - size * 0.05f
    canvas.drawLine(cx, lineTop, cx, lineBottom, linePaint)

    // Bottom dot
    val bottomDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.argb(180, 255, 255, 255)
    }
    canvas.drawCircle(cx, lineBottom, size * 0.03f, bottomDotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Custom dark InfoWindow matching the SpaceX design language
 */
class StationInfoWindow(
    mapView: MapView,
    private val station: StationSol
) : InfoWindow(buildView(mapView.context, station), mapView) {

    override fun onOpen(item: Any?) {
        // View is already built in the constructor
    }

    override fun onClose() {}

    companion object {
        private fun buildView(context: Context, station: StationSol): View {
            val density = context.resources.displayMetrics.density
            val minWidth = (260 * density).toInt()

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                minimumWidth = minWidth
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.argb(245, 14, 14, 14))
                    cornerRadius = 12 * density
                    setStroke((1 * density).toInt(), android.graphics.Color.argb(80, 51, 51, 51))
                }
                val padH = (20 * density).toInt()
                val padV = (16 * density).toInt()
                setPadding(padH, padV, padH, padV)
                elevation = 12 * density
            }

            // Station name
            container.addView(TextView(context).apply {
                text = station.nomStation.uppercase()
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                letterSpacing = 0.08f
                isSingleLine = false
                maxLines = 2
            })

            // Code label
            container.addView(TextView(context).apply {
                text = station.codeStation
                setTextColor(android.graphics.Color.argb(180, 112, 112, 112))
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                letterSpacing = 0.06f
                val topMargin = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, topMargin, 0, 0) }
            })

            // Divider
            container.addView(buildDivider(context, density))

            // Data rows (vertical, each row is label + value side by side)
            station.diametreAntenne?.let {
                container.addView(buildDataRow(context, "ANTENNA", "${it} m", density))
            }

            station.debitMax?.let {
                container.addView(buildDataRow(context, "MAX BITRATE", "${it.toInt()} Mbps", density))
            }

            val lat = String.format("%.4f", station.latitude)
            val lon = String.format("%.4f", station.longitude)
            container.addView(buildDataRow(context, "LATITUDE", lat, density))
            container.addView(buildDataRow(context, "LONGITUDE", lon, density))

            // Second divider
            container.addView(buildDivider(context, density))

            // Band info
            container.addView(buildDataRow(context, "BAND", "X-Band", density))

            return FrameLayout(context).apply {
                addView(container)
                container.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            }
        }

        private fun buildDataRow(context: Context, label: String, value: String, density: Float): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val marginV = (3 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, marginV, 0, marginV) }

                addView(TextView(context).apply {
                    text = label
                    setTextColor(android.graphics.Color.argb(160, 112, 112, 112))
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    letterSpacing = 0.1f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                })

                addView(TextView(context).apply {
                    text = value
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
            }
        }

        private fun buildDivider(context: Context, density: Float): View {
            return View(context).apply {
                setBackgroundColor(android.graphics.Color.argb(50, 80, 80, 80))
                val marginV = (10 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (1 * density).toInt()
                ).apply { setMargins(0, marginV, 0, marginV) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: NanoOrbitViewModel) {
    val context = LocalContext.current
    val stations by viewModel.stations.collectAsStateWithLifecycle()

    val darkTileSource = remember {
        XYTileSource(
            "CartoDB_DarkMatter",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/"
            )
        )
    }

    val pinIcon = remember { createStationPin(context) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(darkTileSource)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(20.0, 0.0))
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            Surface(color = Surface0) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "GROUND NETWORK",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextTertiary,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    text = "Stations",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(BorderSubtle)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    mapView.controller.animateTo(GeoPoint(48.8566, 2.3522))
                    mapView.controller.setZoom(10.0)
                },
                containerColor = SpaceWhite,
                contentColor = SpaceBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Locate me",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Surface0)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.overlays.removeAll { it is Marker }
                    InfoWindow.closeAllInfoWindowsOn(view)

                    stations.forEach { station ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(station.latitude, station.longitude)
                            icon = pinIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            infoWindow = StationInfoWindow(view, station)
                        }
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            )
        }
    }
}
