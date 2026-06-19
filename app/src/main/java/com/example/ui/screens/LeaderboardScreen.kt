package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.LeaderboardEntry
import com.example.data.database.GameRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val leaderboardList by repository.leaderboard.collectAsStateWithLifecycle(initialValue = emptyList())

    // Auto-seed leaderboard if empty
    LaunchedEffect(leaderboardList) {
        if (leaderboardList.isEmpty()) {
            repository.seedLeaderboardIfEmpty(leaderboardList)
        }
    }

    var registerDialogVisible by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputScore by remember { mutableStateOf("6400") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GLOBAL LEAGUE GRID", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { registerDialogVisible = true },
                containerColor = NeonCyan,
                contentColor = CosmicDark,
                modifier = Modifier.testTag("add_leaderboard_fab")
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = "Add rating record")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicDark)
                .padding(innerPadding)
        ) {
            if (leaderboardList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, NeonYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        color = SpaceCard,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonYellow,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("APEX LEADERS SEASON VIII", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Refreshed live after server sync relays. Battle to claim your Apex Tier medallion.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NebulaTerminal)
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RANK & PILOT MATCH", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("TIER", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                        Text("PTS SCORE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(leaderboardList) { index, entry ->
                            val isTop3 = index < 3
                            val rankColor = when (index) {
                                0 -> NeonYellow
                                1 -> NeonCyan
                                2 -> NeonMagenta
                                else -> TextMuted
                            }

                            val itemBackground = if (entry.isLocalPlayer) {
                                Brush.linearGradient(colors = listOf(SpaceCard, NebulaTerminal))
                            } else {
                                Brush.linearGradient(colors = listOf(SpaceCard, SpaceCard))
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (entry.isLocalPlayer) 1.5.dp else 1.dp,
                                        brush = if (entry.isLocalPlayer) Brush.linearGradient(colors = listOf(NeonCyan, NeonMagenta)) else Brush.linearGradient(colors = listOf(NebulaTerminal, NebulaTerminal)),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(itemBackground)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(2f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = rankColor,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.width(28.dp),
                                                textAlign = TextAlign.Center,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = entry.name,
                                                        color = if (entry.isLocalPlayer) NeonCyan else TextWhite,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (entry.isLocalPlayer) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(color = NeonCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                                                            Text("YOU", color = NeonCyan, fontSize = 8.sp, modifier = Modifier.padding(2.dp))
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "W/L ${entry.winRate * 100}% | K/D ${(entry.kills.toFloat()/entry.deaths.coerceAtLeast(1).toFloat()).coerceAtMost(9.9f)}",
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Surface(
                                            color = NebulaTerminal,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .border(1.dp, if (isTop3) rankColor.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .weight(1.2f)
                                        ) {
                                            Text(
                                                text = entry.tier.uppercase(),
                                                color = if (isTop3) rankColor else TextWhite,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Text(
                                            text = "${entry.score}",
                                            color = if (index == 0) NeonYellow else TextWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            if (registerDialogVisible) {
                AlertDialog(
                    onDismissRequest = { registerDialogVisible = false },
                    title = { Text("REGISTER LEAGUE CABIN", color = TextWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Broadcast your battle accomplishments on the shared cyberleaderboards.", color = TextMuted, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Pilot Callsign") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    focusedLabelColor = NeonCyan,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = inputScore,
                                onValueChange = { inputScore = it },
                                label = { Text("Lobby Score") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    focusedLabelColor = NeonCyan,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val scr = inputScore.toIntOrNull() ?: 5000
                                coroutineScope.launch {
                                    val newEntry = LeaderboardEntry(
                                        rank = 11,
                                        name = if (inputName.isEmpty()) "PilotX" else inputName,
                                        score = scr,
                                        kills = 85,
                                        deaths = 22,
                                        winRate = 0.79f,
                                        tier = "Elite Master",
                                        isLocalPlayer = true
                                    )
                                    repository.insertLeaderboard(newEntry)
                                    registerDialogVisible = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("BROADCAST SEAT", color = CosmicDark, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { registerDialogVisible = false }) {
                            Text("ABORT", color = TextMuted)
                        }
                    },
                    containerColor = SpaceCard
                )
            }
        }
    }
}
