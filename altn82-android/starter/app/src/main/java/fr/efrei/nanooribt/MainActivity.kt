package fr.efrei.nanooribt

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.*
import fr.efrei.nanooribt.ui.theme.*
import org.osmdroid.config.Configuration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        val database = AppDatabase.getDatabase(this)

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
        containerColor = Surface0,
        bottomBar = {
            if (currentRoute != null && !currentRoute.startsWith("detail")) {
                Surface(
                    color = Surface0,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        // Top separator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(BorderSubtle)
                        )

                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = TextPrimary,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(64.dp)
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            screen.icon!!,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            screen.title.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            letterSpacing = 1.5.sp
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SpaceWhite,
                                        selectedTextColor = SpaceWhite,
                                        unselectedIconColor = TextDisabled,
                                        unselectedTextColor = TextDisabled,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200))
            }
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
