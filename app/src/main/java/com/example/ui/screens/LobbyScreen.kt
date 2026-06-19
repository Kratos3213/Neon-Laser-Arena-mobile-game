package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    onNavigateToMatch: (isLocalPvp: Boolean) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToWeapons: () -> Unit,
    playerTitle: String = "SQUADRON ELITE"
) {
    // Glowing animated value
    val infiniteTransition = rememberInfiniteTransition(label = "lobby_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val neonRainbowBrush = Brush.linearGradient(
        colors = listOf(NeonCyan, NeonMagenta, NeonYellow)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .testTag("lobby_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP TITLE HEADER ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RANKED SYSTEM ACTIVE",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "NEON LASER",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 3.sp,
                )

                Text(
                    text = "ARENA",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonMagenta,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "PHYSICS-BASED MULTIPLAYER FLIGHT COMMENCE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            // --- HERO SHIP BANNER PLACEHOLDER / DECORATION ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpaceCard)
                    .border(1.5.dp, neonRainbowBrush, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonMagenta.copy(alpha = 0.25f), Color.Transparent),
                                radius = 350f
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Spaceship logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PILOT CLASS: $playerTitle",
                        color = NeonYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "HYPERBEAM LAUNCHER INSTALLED",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // --- MENU SELECTION BUTTONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Dual Device Match (Local PVP)
                Button(
                    onClick = { onNavigateToMatch(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("local_match_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceCard),
                    border = BorderStroke(1.5.dp, NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Local Match", tint = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("LOCAL SHIELD DUEL", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Two combatants. Same device screen.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Go", tint = NeonCyan)
                    }
                }

                // 2. Play Online Bots Battle Match (with simulated matchmaking queue)
                Button(
                    onClick = { onNavigateToMatch(false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("online_match_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceCard),
                    border = BorderStroke(1.5.dp, NeonMagenta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Online Arena", tint = NeonMagenta)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ONLINE ARCH-ARENA", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Simulation multiplayer matchmaking.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Go", tint = NeonMagenta)
                    }
                }

                // 3. Grid of weapon customization and leaderboards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Leaderboard
                    Button(
                        onClick = onNavigateToLeaderboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("leaderboards_menu_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonYellow.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Leaderboard", tint = NeonYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LEAGUE", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Weapons
                    Button(
                        onClick = onNavigateToWeapons,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("weapons_menu_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Weapons", tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HYPER-CELLS", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // AI Match historic Highlights library
                Button(
                    onClick = onNavigateToHighlights,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("highlights_menu_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDark),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Highlights", tint = TextWhite.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI MATCH HIGHLIGHT LOGS", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
