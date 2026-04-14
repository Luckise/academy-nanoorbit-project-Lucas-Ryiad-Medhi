package fr.efrei.nanooribt

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.efrei.nanooribt.ui.theme.*
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
        containerColor = Surface0,
        topBar = {
            Surface(color = Surface0) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "COMMUNICATIONS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextTertiary,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    text = "Planning",
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
                onClick = { showPlanDialog = true },
                containerColor = SpaceWhite,
                contentColor = SpaceBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plan", modifier = Modifier.size(20.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Surface0)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Station filter tabs
            ScrollableTabRow(
                selectedTabIndex = if (selectedStationCode == null) 0 else stations.indexOfFirst { it.codeStation == selectedStationCode } + 1,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                indicator = {},
                divider = {}
            ) {
                Tab(
                    selected = selectedStationCode == null,
                    onClick = { selectedStationCode = null },
                    text = {
                        Text(
                            "ALL",
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 1.5.sp,
                            color = if (selectedStationCode == null) TextPrimary else TextTertiary
                        )
                    }
                )
                stations.forEach { station ->
                    Tab(
                        selected = selectedStationCode == station.codeStation,
                        onClick = { selectedStationCode = station.codeStation },
                        text = {
                            Text(
                                station.nomStation.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.sp,
                                color = if (selectedStationCode == station.codeStation) TextPrimary else TextTertiary
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(BorderSubtle)
            )

            // Quick stats
            val totalDuration = filteredFenetres.sumOf { it.duree }
            val totalVolume = filteredFenetres.sumOf { it.volumeDonnees ?: 0.0 }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Surface1,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "WINDOWS", value = "${filteredFenetres.size}")
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(36.dp)
                            .background(BorderSubtle)
                    )
                    StatItem(label = "DURATION", value = "${totalDuration / 60} min")
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(36.dp)
                            .background(BorderSubtle)
                    )
                    StatItem(label = "DATA VOL.", value = String.format("%.1f GB", totalVolume))
                }
            }

            // Windows list
            if (filteredFenetres.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO WINDOWS SCHEDULED",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextDisabled,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + to plan a new communication",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDisabled
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(filteredFenetres) { index, fenetre ->
                        val stationName = stations.find { it.codeStation == fenetre.codeStation }?.nomStation ?: "Unknown"
                        AnimatedSection(delayMs = index * 60) {
                            FenetreCard(fenetre = fenetre, nomStation = stationName)
                        }
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
            onConfirm = { }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
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
        containerColor = Surface2,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                "SCHEDULE WINDOW",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Satellite info
                Surface(
                    color = Surface1,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "SATELLITE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                selectedSatId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        if (isDesorbite) {
                            Text(
                                "DEORBITED",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusFailed,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                if (isDesorbite) {
                    Text(
                        "Cannot schedule for a deorbited satellite (RG-S06)",
                        color = StatusFailed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = dureeStr,
                    onValueChange = { dureeStr = it },
                    label = {
                        Text(
                            "DURATION (SECONDS)",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp
                        )
                    },
                    isError = !isDureeValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Surface1,
                        focusedContainerColor = Surface1,
                        unfocusedBorderColor = BorderSubtle,
                        focusedBorderColor = BorderMedium,
                        errorBorderColor = StatusFailed,
                        cursorColor = SpaceWhite,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                if (!isDureeValid) {
                    Text(
                        "Duration must be between 1 and 900s (RG-F04)",
                        color = StatusFailed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss() },
                enabled = !isDesorbite && isDureeValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpaceWhite,
                    contentColor = SpaceBlack,
                    disabledContainerColor = Surface3,
                    disabledContentColor = TextDisabled
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "CONFIRM",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )
            }
        }
    )
}
