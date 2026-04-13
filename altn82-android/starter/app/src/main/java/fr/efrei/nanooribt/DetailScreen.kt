package fr.efrei.nanooribt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.efrei.nanooribt.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    satelliteId: String,
    viewModel: NanoOrbitViewModel,
    onBack: () -> Unit
) {
    val satellites by viewModel.filteredSatellites.collectAsStateWithLifecycle()
    val satellite = satellites.find { it.idSatellite == satelliteId }
    val instruments = MockData.instruments

    var showAnomalyDialog by remember { mutableStateOf(false) }
    var anomalyText by remember { mutableStateOf("") }

    if (satellite == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface0),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "SATELLITE NOT FOUND",
                style = MaterialTheme.typography.labelLarge,
                color = TextTertiary,
                letterSpacing = 3.sp
            )
        }
        return
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
                                    text = "SATELLITE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = satellite.nomSatellite.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    letterSpacing = 1.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextSecondary
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
                onClick = { showAnomalyDialog = true },
                containerColor = SpaceWhite,
                contentColor = SpaceBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Report anomaly",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Surface0),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Status section
            item {
                AnimatedSection(delayMs = 0) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("STATUS & CONFIGURATION")
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = Surface1,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(statut = satellite.statut)
                                    Text(
                                        text = satellite.formatCubesat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                                ) {
                                    DataLabel(label = "SAT ID", value = satellite.idSatellite)
                                    DataLabel(label = "ORBIT ID", value = satellite.idOrbite)
                                }
                            }
                        }
                    }
                }
            }

            // Telemetry section
            item {
                AnimatedSection(delayMs = 100) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader("TELEMETRY")
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = Surface1,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Mass row
                                TelemetryRow(label = "MASS", value = "${satellite.masse ?: "N/A"} kg")

                                Spacer(modifier = Modifier.height(16.dp))

                                // Battery
                                Text(
                                    text = "BATTERY CAPACITY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Animated progress bar
                                val batteryProgress = remember { Animatable(0f) }
                                LaunchedEffect(Unit) {
                                    batteryProgress.animateTo(
                                        0.85f,
                                        animationSpec = tween(1200, 300, EaseOut)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Surface3)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = batteryProgress.value)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(StatusOperational)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "85%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusOperational,
                                    modifier = Modifier.align(Alignment.End)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                TelemetryRow(label = "EST. LIFESPAN", value = "4.2 years")
                            }
                        }
                    }
                }
            }

            // Instruments section
            item {
                AnimatedSection(delayMs = 200) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader("ONBOARD INSTRUMENTS")
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            itemsIndexed(instruments) { index, instrument ->
                AnimatedSection(delayMs = 250 + index * 50) {
                    InstrumentItem(instrument = instrument, etatFonctionnement = "OK")
                }
            }

            // Missions section
            item {
                AnimatedSection(delayMs = 400) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader("ACTIVE MISSIONS")
                        Spacer(modifier = Modifier.height(8.dp))

                        MissionItem(name = "Amazon Deforestation Monitoring")
                        MissionItem(name = "North Atlantic Ocean Currents Study")
                    }
                }
            }
        }
    }

    // Anomaly dialog
    if (showAnomalyDialog) {
        AlertDialog(
            onDismissRequest = { showAnomalyDialog = false },
            containerColor = Surface2,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    "REPORT ANOMALY",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = anomalyText,
                    onValueChange = { anomalyText = it },
                    placeholder = {
                        Text(
                            "Describe the anomaly...",
                            color = TextDisabled,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Surface1,
                        focusedContainerColor = Surface1,
                        unfocusedBorderColor = BorderSubtle,
                        focusedBorderColor = BorderMedium,
                        cursorColor = SpaceWhite,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAnomalyDialog = false
                    },
                    enabled = anomalyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceWhite,
                        contentColor = SpaceBlack,
                        disabledContainerColor = Surface3,
                        disabledContentColor = TextDisabled
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "SUBMIT",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnomalyDialog = false }) {
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
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MissionItem(name: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = Surface1,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentBlue)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun AnimatedSection(delayMs: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        launch {
            alpha.animateTo(1f, animationSpec = tween(500, easing = EaseOut))
        }
        launch {
            offsetY.animateTo(0f, animationSpec = tween(500, easing = EaseOut))
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        content()
    }
}
