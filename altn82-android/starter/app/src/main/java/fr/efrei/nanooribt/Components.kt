package fr.efrei.nanooribt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.efrei.nanooribt.ui.theme.NanoOribtTheme
import java.time.format.DateTimeFormatter

@Composable
fun StatusBadge(statut: StatutSatellite, modifier: Modifier = Modifier) {
    val color = when (statut) {
        StatutSatellite.OPERATIONNEL -> Color(0xFF4CAF50) // Vert
        StatutSatellite.EN_VEILLE -> Color(0xFFFF9800) // Orange
        StatutSatellite.DEFAILLANT -> Color(0xFFF44336) // Rouge
        StatutSatellite.DESORBITE -> Color(0xFF9E9E9E) // Gris
    }

    SuggestionChip(
        onClick = { },
        label = { Text(statut.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            labelColor = color
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = color
        )
    )
}

@Composable
fun SatelliteCard(satellite: Satellite, onClick: () -> Unit) {
    val isDesorbite = satellite.statut == StatutSatellite.DESORBITE
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(enabled = !isDesorbite) { onClick() },
        colors = if (isDesorbite) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = satellite.nomSatellite,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDesorbite) Color.Gray else Color.Unspecified
                )
                StatusBadge(statut = satellite.statut)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "ID: ${satellite.idSatellite}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Format: ${satellite.formatCubesat}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Orbite: ${satellite.idOrbite}", style = MaterialTheme.typography.bodyMedium)
            
            if (isDesorbite) {
                Text(
                    text = "DÉSORBITÉ",
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun FenetreCard(fenetre: FenetreCom, nomStation: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = nomStation, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = fenetre.statut.name,
                    color = when(fenetre.statut) {
                        StatutFenetre.PLANIFIEE -> Color.Blue
                        StatutFenetre.REALISEE -> Color(0xFF4CAF50)
                        StatutFenetre.ANNULEE -> Color.Red
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            Text(text = "Début: ${fenetre.datetimeDebut.format(formatter)}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Durée: ${fenetre.duree}s", style = MaterialTheme.typography.bodySmall)
            
            fenetre.volumeDonnees?.let {
                Text(text = "Volume: $it GB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InstrumentItem(instrument: Instrument, etatFonctionnement: String) {
    ListItem(
        headlineContent = { Text(instrument.modele) },
        supportingContent = { 
            Column {
                Text("${instrument.typeInstrument} - Res: ${instrument.resolution ?: "N/A"}")
                Text("État: $etatFonctionnement", color = if (etatFonctionnement == "OK") Color(0xFF4CAF50) else Color.Red)
            }
        },
        trailingContent = {
            instrument.consommation?.let {
                Text("$it W", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewComponents() {
    NanoOribtTheme {
        Column {
            StatusBadge(StatutSatellite.OPERATIONNEL)
            SatelliteCard(MockData.satellites[0]) {}
            SatelliteCard(MockData.satellites[4]) {} // Désorbité
            FenetreCard(MockData.fenetres[0], "Toulouse Space Center")
            InstrumentItem(MockData.instruments[0], "OK")
        }
    }
}
