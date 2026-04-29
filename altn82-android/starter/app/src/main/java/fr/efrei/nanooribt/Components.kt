package fr.efrei.nanooribt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.efrei.nanooribt.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun StatusBadge(statut: StatutSatellite, modifier: Modifier = Modifier) {
    val color = when (statut) {
        StatutSatellite.OPERATIONNEL -> StatusOperational
        StatutSatellite.EN_VEILLE -> StatusStandby
        StatutSatellite.DEFAILLANT -> StatusFailed
        StatutSatellite.DESORBITE -> StatusDeorbited
    }

    // Pulsing dot for operational status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (statut == StatutSatellite.OPERATIONNEL) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
        Text(
            text = statut.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun SatelliteCard(satellite: Satellite, onClick: () -> Unit) {
    val isDesorbite = satellite.statut == StatutSatellite.DESORBITE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                enabled = !isDesorbite,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = Surface1,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDesorbite) BorderSubtle else BorderMedium.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = satellite.nomSatellite.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDesorbite) TextDisabled else TextPrimary,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = satellite.idSatellite,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDesorbite) TextDisabled else TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }
                StatusBadge(statut = satellite.statut)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Data row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                DataLabel(
                    label = "FORMAT",
                    value = satellite.formatCubesat.name,
                    dimmed = isDesorbite
                )
                DataLabel(
                    label = "ORBIT",
                    value = satellite.idOrbite,
                    dimmed = isDesorbite
                )
            }
        }
    }
}

@Composable
fun DataLabel(label: String, value: String, dimmed: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (dimmed) TextDisabled else TextTertiary,
            letterSpacing = 1.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) TextDisabled else TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FenetreCard(fenetre: FenetreCom, nomStation: String) {
    val statusColor = when (fenetre.statut) {
        StatutFenetre.PLANIFIEE -> AccentBlue
        StatutFenetre.REALISEE -> StatusOperational
        StatutFenetre.ANNULEE -> StatusFailed
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Surface1,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Status accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nomStation.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = fenetre.statut.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    DataLabel(label = "START", value = fenetre.datetimeDebut.format(formatter))
                    DataLabel(label = "DURATION", value = "${fenetre.duree}s")
                    fenetre.volumeDonnees?.let {
                        DataLabel(label = "DATA", value = "$it GB")
                    }
                }
            }
        }
    }
}

@Composable
fun InstrumentItem(instrument: Instrument, etatFonctionnement: String) {
    val statusColor = when (etatFonctionnement) {
        "OK" -> StatusOperational
        "DEGRADED" -> StatusStandby
        else -> StatusFailed
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        color = Surface1,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instrument.modele.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${instrument.typeInstrument} ${instrument.resolution?.let { "- $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            instrument.consommation?.let {
                Text(
                    text = "${it}W",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = TextTertiary,
        letterSpacing = 2.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewComponents() {
    NanoOribtTheme {
        Column(
            modifier = Modifier
                .background(Surface0)
                .padding(vertical = 16.dp)
        ) {
            StatusBadge(StatutSatellite.OPERATIONNEL)
            Spacer(modifier = Modifier.height(8.dp))
            SatelliteCard(MockData.satellites[0]) {}
            SatelliteCard(MockData.satellites[4]) {}
            Spacer(modifier = Modifier.height(8.dp))
            FenetreCard(MockData.fenetres[0], "Kiruna Arctic Station")
            Spacer(modifier = Modifier.height(8.dp))
            InstrumentItem(MockData.instruments[0], "OK")
        }
    }
}
