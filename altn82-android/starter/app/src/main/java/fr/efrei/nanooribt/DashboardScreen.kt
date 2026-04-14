package fr.efrei.nanooribt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.efrei.nanooribt.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NanoOrbitViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val satellites by viewModel.filteredSatellites.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatut by viewModel.selectedStatut.collectAsStateWithLifecycle()

    // Refresh spin animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "refreshRotation"
    )

    Scaffold(
        containerColor = Surface0,
        topBar = {
            Surface(
                color = Surface0,
                shadowElevation = 0.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "GROUND CONTROL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextTertiary,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    text = "NanoOrbit",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshSatellites() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = TextSecondary,
                                    modifier = if (isLoading) Modifier.rotate(rotation) else Modifier
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = TextPrimary
                        )
                    )

                    // Thin separator line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(BorderSubtle)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Surface0)
        ) {
            // Offline banner
            AnimatedVisibility(
                visible = isOffline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusStandby,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "OFFLINE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusStandby,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "Search satellites...",
                        color = TextDisabled,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Surface1,
                    focusedContainerColor = Surface2,
                    unfocusedBorderColor = BorderSubtle,
                    focusedBorderColor = BorderMedium,
                    cursorColor = SpaceWhite,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpaceFilterChip(
                    label = "ALL",
                    selected = selectedStatut == null,
                    onClick = { viewModel.onStatutFilterChange(null) }
                )
                StatutSatellite.entries.forEach { statut ->
                    SpaceFilterChip(
                        label = statut.name.replace("_", " "),
                        selected = selectedStatut == statut,
                        onClick = { viewModel.onStatutFilterChange(statut) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Result count
            Text(
                text = "${satellites.size} SATELLITES",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Satellite list with staggered animation
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && satellites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SpaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp)
                    ) {
                        itemsIndexed(satellites) { index, satellite ->
                            val animatedAlpha = remember { Animatable(0f) }
                            val animatedOffset = remember { Animatable(30f) }

                            LaunchedEffect(satellite.idSatellite) {
                                kotlinx.coroutines.delay(index * 50L)
                                launch {
                                    animatedAlpha.animateTo(
                                        1f,
                                        animationSpec = tween(400, easing = EaseOut)
                                    )
                                }
                                launch {
                                    animatedOffset.animateTo(
                                        0f,
                                        animationSpec = tween(400, easing = EaseOut)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = animatedAlpha.value
                                        translationY = animatedOffset.value
                                    }
                            ) {
                                SatelliteCard(
                                    satellite = satellite,
                                    onClick = { onNavigateToDetail(satellite.idSatellite) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (selected) SpaceWhite else Color.Transparent,
        border = if (!selected) {
            androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
        } else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SpaceBlack else TextSecondary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
