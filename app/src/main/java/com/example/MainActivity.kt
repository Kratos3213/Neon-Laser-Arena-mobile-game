package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.GameDatabase
import com.example.data.database.GameRepository
import com.example.game.engine.LaserType
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.HighlightsScreen
import com.example.ui.screens.WeaponCustomizeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Local Room Engine
        val database = GameDatabase.getDatabase(applicationContext)
        val repository = GameRepository(database.gameDao())

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Persistent player selected weapon systems
                var playerWeapon by remember { mutableStateOf(LaserType.BLAST_BEAM) }
                var botWeapon by remember { mutableStateOf(LaserType.BOUNCE_SHOT) }

                NavHost(
                    navController = navController,
                    startDestination = "lobby",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Lobby Core
                    composable("lobby") {
                        LobbyScreen(
                            onNavigateToMatch = { isLocal ->
                                navController.navigate("game/$isLocal")
                            },
                            onNavigateToLeaderboard = {
                                navController.navigate("leaderboard")
                            },
                            onNavigateToHighlights = {
                                navController.navigate("highlights")
                            },
                            onNavigateToWeapons = {
                                navController.navigate("weapons")
                            }
                        )
                    }

                    // Active Game Arena Screen
                    composable(
                        route = "game/{isLocalPvp}",
                        arguments = listOf(navArgument("isLocalPvp") { type = NavType.BoolType })
                    ) { backStackEntry ->
                        val isLocal = backStackEntry.arguments?.getBoolean("isLocalPvp") ?: true
                        GameScreen(
                            isLocalPvp = isLocal,
                            playerWeapon = playerWeapon,
                            botWeapon = botWeapon,
                            repository = repository,
                            onBackToLobby = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Leaderboards Scroll View
                    composable("leaderboard") {
                        LeaderboardScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Stored Highlights List
                    composable("highlights") {
                        HighlightsScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Weapon customization custom Lab
                    composable("weapons") {
                        WeaponCustomizeScreen(
                            currentSelection = playerWeapon,
                            botSelection = botWeapon,
                            onSelectWeapon = { playerWeapon = it },
                            onSelectBotWeapon = { botWeapon = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
