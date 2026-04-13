package fr.efrei.nanooribt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(viewModel: NanoOrbitViewModel) {
    val fenetres by viewModel.fenetres.collectAsStateWithLifecycle()
    val satellites by viewModel.filteredSatellites.collectAsStateWithLifecycle()
    val stations = MockData.stations
    
    var selectedStationCode by remember { mutableStateOf<String?>(null) }
    var showPlanDialog by remember { mutableStateOf(false) }
    
    val filteredFenetres = fenetres.filter { 
        selectedStationCode == null || it.codeStation == selectedStationCode 
    }.sortedBy { it.datetimeDebut }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planning des Communications") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPlanDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Planifier")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Sélecteur de station
            ScrollableTabRow(
                selectedTabIndex = if (selectedStationCode == null) 0 else stations.indexOfFirst { it.codeStation == selectedStationCode } + 1,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedStationCode == null,
                    onClick = { selectedStationCode = null },
                    text = { Text("Toutes") }
                )
                stations.forEach { station ->
                    Tab(
                        selected = selectedStationCode == station.codeStation,
                        onClick = { selectedStationCode = station.codeStation },
                        text = { Text(station.nomStation) }
                    )
                }
            }

            // Statistiques rapides
            val totalDuration = filteredFenetres.sumOf { it.duree }
            val totalVolume = filteredFenetres.sumOf { it.volumeDonnees ?: 0.0 }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Durée totale", style = MaterialTheme.typography.labelSmall)
                    Text("${totalDuration / 60} min", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Volume total", style = MaterialTheme.typography.labelSmall)
                    Text(String.format("%.1f GB", totalVolume), fontWeight = FontWeight.Bold)
                }
            }

            if (filteredFenetres.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune fenêtre planifiée pour cette station")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredFenetres) { fenetre ->
                        val stationName = stations.find { it.codeStation == fenetre.codeStation }?.nomStation ?: "Inconnue"
                        FenetreCard(fenetre = fenetre, nomStation = stationName)
                    }
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanDialog(
            satellites = satellites,
            stations = stations,
            onDismiss = { showPlanDialog = false },
            onConfirm = { /* Logique d'ajout Phase 3 */ }
        )
    }
}

@Composable
fun PlanDialog(
    satellites: List<Satellite>,
    stations: List<StationSol>,
    onDismiss: () -> Unit,
    onConfirm: (FenetreCom) -> Unit
) {
    var selectedSatId by remember { mutableStateOf(satellites.firstOrNull()?.idSatellite ?: "") }
    var selectedStationCode by remember { mutableStateOf(stations.firstOrNull()?.codeStation ?: "") }
    var dureeStr by remember { mutableStateOf("300") }
    
    val selectedSat = satellites.find { it.idSatellite == selectedSatId }
    val isDesorbite = selectedSat?.statut == StatutSatellite.DESORBITE
    val duree = dureeStr.toIntOrNull() ?: 0
    val isDureeValid = duree in 1..900

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planifier une fenêtre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Satellite : $selectedSatId ${if(isDesorbite) "(DÉSORBITÉ)" else ""}")
                if (isDesorbite) {
                    Text(
                        "Erreur : Impossible de planifier pour un satellite désorbité (RG-S06).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                OutlinedTextField(
                    value = dureeStr,
                    onValueChange = { dureeStr = it },
                    label = { Text("Durée (secondes)") },
                    isError = !isDureeValid,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isDureeValid) {
                    Text(
                        "La durée doit être comprise entre 1 et 900s (RG-F04).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss() },
                enabled = !isDesorbite && isDureeValid
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
