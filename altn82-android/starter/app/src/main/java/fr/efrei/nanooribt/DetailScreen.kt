package fr.efrei.nanooribt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    satelliteId: String,
    viewModel: NanoOrbitViewModel,
    onBack: () -> Unit
) {
    val satellites by viewModel.filteredSatellites.collectAsStateWithLifecycle()
    val satellite = satellites.find { it.idSatellite == satelliteId }
    
    // Simulation d'instruments pour le satellite
    val instruments = MockData.instruments
    
    var showAnomalyDialog by remember { mutableStateOf(false) }
    var anomalyText by remember { mutableStateOf("") }

    if (satellite == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Satellite non trouvé")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(satellite.nomSatellite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAnomalyDialog = true },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                text = { Text("Signaler une anomalie") },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Section Statut
            item {
                Text("Statut & Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(statut = satellite.statut)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Format: ${satellite.formatCubesat}", style = MaterialTheme.typography.bodyLarge)
                }
                Text("ID Orbite: ${satellite.idOrbite}", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Section Télémétrie
            item {
                Text("Télémétrie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Masse")
                            Text("${satellite.masse ?: "N/A"} kg", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Capacité Batterie")
                        LinearProgressIndicator(
                            progress = { 0.85f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF4CAF50)
                        )
                        Text("85%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Durée de vie estimée: 4.2 ans", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Section Instruments
            item {
                Text("Instruments embarqués", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(instruments) { instrument ->
                InstrumentItem(instrument = instrument, etatFonctionnement = "OK")
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Missions actives", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Monitoring Déforestation Amazonie", style = MaterialTheme.typography.bodyLarge)
                Text("• Étude courants marins Atlantique Nord", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(80.dp)) // Espace pour le FAB
            }
        }
    }

    if (showAnomalyDialog) {
        AlertDialog(
            onDismissRequest = { showAnomalyDialog = false },
            title = { Text("Signaler une anomalie") },
            text = {
                OutlinedTextField(
                    value = anomalyText,
                    onValueChange = { anomalyText = it },
                    label = { Text("Description de l'anomalie") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        // Envoyer l'anomalie (Phase 3)
                        showAnomalyDialog = false 
                    },
                    enabled = anomalyText.isNotBlank()
                ) {
                    Text("Envoyer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnomalyDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
