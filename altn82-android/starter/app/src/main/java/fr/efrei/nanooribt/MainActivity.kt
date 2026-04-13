package fr.efrei.nanooribt

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.*
import fr.efrei.nanooribt.ui.theme.NanoOribtTheme
import org.osmdroid.config.Configuration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialisation osmdroid (Phase 3.6)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        // Initialisation de la base de données et de l'API (Phase 2 & 3)
        val database = AppDatabase.getDatabase(this)
        
        // Note: URL fictive car l'API n'existe pas encore réellement (Phase 2)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.nanoorbit.com/") 
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(NanoOrbitApi::class.java)
        
        val repository = NanoOrbitRepository(
            api = api,
            satelliteDao = database.satelliteDao(),
            fenetreDao = database.fenetreDao()
        )
        
        val viewModelFactory = NanoOrbitViewModel.Factory(repository)

        // Bonus Phase 3.7 : Planification du WorkManager pour les notifications
        val workRequest = PeriodicWorkRequestBuilder<FenetreWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FenetreCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        enableEdgeToEdge()
        setContent {
            NanoOribtTheme {
                val viewModel: NanoOrbitViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: NanoOrbitViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Barre de navigation (Phase 3.2) - Masquée sur l'écran de détail
            if (currentRoute != null && !currentRoute.startsWith("detail")) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> 
                        navController.navigate(Screen.Detail.createRoute(id)) 
                    }
                )
            }
            composable(Screen.Planning.route) {
                PlanningScreen(viewModel)
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("satelliteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val satelliteId = backStackEntry.arguments?.getString("satelliteId") ?: ""
                DetailScreen(
                    satelliteId = satelliteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
