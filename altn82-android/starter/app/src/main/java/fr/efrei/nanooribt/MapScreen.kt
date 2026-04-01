package fr.efrei.nanooribt

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val stations = MockData.stations
    
    // Configuration de base de MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(20.0, 0.0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stations au sol") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Centrer sur la position de l'utilisateur (Phase 3.6 Bonus GPS)
                mapView.controller.animateTo(GeoPoint(48.8566, 2.3522)) // Paris par défaut
                mapView.controller.setZoom(10.0)
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Me localiser")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Ajouter les marqueurs pour les stations
                    stations.forEach { station ->
                        val marker = Marker(view)
                        marker.position = GeoPoint(station.latitude, station.longitude)
                        marker.title = station.nomStation
                        marker.snippet = "Bande: X-Band | Débit: ${station.debitMax} Mbps"
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            )
        }
    }
}
