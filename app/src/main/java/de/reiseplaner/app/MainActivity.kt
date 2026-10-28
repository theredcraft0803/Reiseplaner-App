package de.reiseplaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.reiseplaner.app.ui.ReiseTheme
import de.reiseplaner.app.ui.TripDetailScreen
import de.reiseplaner.app.ui.TripListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
private fun AppNav() {
    val vm: MainViewModel = viewModel()
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "trips") {
        composable("trips") {
            TripListScreen(vm = vm, onOpen = { id -> nav.navigate("trip/$id") })
        }
        composable(
            route = "trip/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            TripDetailScreen(vm = vm, tripId = id, onBack = { nav.popBackStack() })
        }
    }
}
