package fr.efrei.nanooribt

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Planning : Screen("planning", "Planning", Icons.Default.DateRange)
    object Map : Screen("map", "Carte", Icons.Default.Place)
    object AR : Screen("ar", "AR", Icons.Default.Star)
    object Detail : Screen("detail/{satelliteId}", "Détail") {
        fun createRoute(satelliteId: String) = "detail/$satelliteId"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Planning,
    Screen.Map,
    Screen.AR
)
