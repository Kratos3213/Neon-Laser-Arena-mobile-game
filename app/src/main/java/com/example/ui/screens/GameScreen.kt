package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MatchHighlight
import com.example.data.database.GameRepository
import com.example.data.network.GeminiClient
import com.example.game.engine.LaserType
import com.example.game.ui.GameCanvasView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    isLocalPvp: Boolean,
    playerWeapon: LaserType,
    botWeapon: LaserType,
    repository: GameRepository,
    onBackToLobby: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Match Phases: "MATCHMAKING", "LIVE", "SUMMARY"
    var currentPhase by remember { mutableStateOf(if (isLocalPvp) "LIVE" else "MATCHMAKING") }

    // Matchmaking simulated values
    var matchMakingProgress by remember { mutableStateOf(0.0f) }
    var matchMakingStatus by remember { mutableStateOf("SCANNING CYBER DUEL CHANNELS...") }
    var matchedOpponentName by remember { mutableStateOf("") }
    var matchedOpponentRank by remember { mutableStateOf("") }

    // State parameters feeding back from Arena view
    var matchWinnerId by remember { mutableStateOf(0) }
    var player1Hits by remember { mutableStateOf(0) }
    var player1TotalShots by remember { mutableStateOf(0) }
    val matchEventTexts = remember { mutableStateListOf<String>() }

    // AI Generative Commentary parameters
    var aiCommentaryText by remember { mutableStateOf("") }
    var isGeneratingCommentary by remember { mutableStateOf(false) }
    var highlightSaved by remember { mutableStateOf(false) }

    // Run Matchmaking simulation on enter
    LaunchedEffect(currentPhase) {
        if (currentPhase == "MATCHMAKING") {
            val statusMessages = listOf(
                "PINGING APAC-EAST MULTIPLEXER...",
                "SEARCHING CORRESPONDING PHANTOM RATINGS...",
                "SYNCHRONIZING GRAVITY VECTORS...",
                "OPPONENT ACQUIRED: SYNCING ARENA PORTS!"
            )
            val opponnets = listOf(
                Pair("QuantumScarecrow", "Apex Champion"),
                Pair("DriftWidow", "Rogue Diamond"),
                Pair("LazerStriker", "Grandmaster"),
                Pair("PulsarBeast", "Platinum V")
            )

            val chosenOpponent = opponnets.random()
            matchedOpponentName = chosenOpponent.first
            matchedOpponentRank = chosenOpponent.second

            for (i in 1..100) {
                delay(30)
                matchMakingProgress = i / 100.0f
                if (i == 25) matchMakingStatus = statusMessages[1]
                if (i == 60) matchMakingStatus = statusMessages[2]
                if (i == 85) matchMakingStatus = statusMessages[3]
            }
            delay(800)
            currentPhase = "LIVE"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentPhase) {
                "MATCHMAKING" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = NeonMagenta.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(1.5.dp, NeonMagenta, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Connecting",
                                tint = NeonMagenta,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "ESTABLISHING CHANNELS",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { matchMakingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonMagenta,
                            trackColor = SpaceCard,
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = matchMakingStatus,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                "LIVE" -> {
                    GameCanvasView(
                        modifier = Modifier.fillMaxSize(),
                        isLocalPvp = isLocalPvp,
                        playerWeapon = playerWeapon,
                        botWeapon = botWeapon,
                        isMatchActive = true,
                        onMatchFinished = { winnerId, hits, shots, events ->
                            matchWinnerId = winnerId
                            player1Hits = hits
                            player1TotalShots = shots
                            matchEventTexts.clear()
                            matchEventTexts.addAll(events.shuffled().take(5))
                            currentPhase = "SUMMARY"
                        }
                    )
                }

                "SUMMARY" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Banner Result
                        val didUserWin = matchWinnerId == 1
                        val matchResultTitle = if (didUserWin) "ARENA MASTERED" else "SYSTEM CRASH"
                        val matchResultColor = if (didUserWin) NeonCyan else NeonMagenta

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, matchResultColor, RoundedCornerShape(16.dp)),
                            color = SpaceCard,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = matchResultTitle,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = matchResultColor,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = if (isLocalPvp) "TACTICAL DUEL COMMUNIQUE" else "ONLINE SERVER LEAGUE EXCHANGED",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Grid of Stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PRECISION", fontSize = 11.sp, color = TextMuted)
                                        val accuracyPercent = if (player1TotalShots > 0) {
                                            ((player1Hits.toFloat() / player1TotalShots.toFloat()) * 100).toInt()
                                        } else 0
                                        Text("$accuracyPercent%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonYellow)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PHOTON HITS", fontSize = 11.sp, color = TextMuted)
                                        Text("$player1Hits", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("CELLS DISCHARGED", fontSize = 11.sp, color = TextMuted)
                                        Text("$player1TotalShots", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // AI highlight generator card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NebulaTerminal, RoundedCornerShape(12.dp)),
                            color = SpaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "AI commentator",
                                        tint = NeonYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "NEON-CORE COM Downlink",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (aiCommentaryText.isEmpty() && !isGeneratingCommentary) {
                                    Text(
                                        text = "Compile an Esports Broadcast summary of this match with generative AI commentary detailing bounce tactics.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            isGeneratingCommentary = true
                                            coroutineScope.launch {
                                                aiCommentaryText = GeminiClient.generateEsportsCommentary(
                                                    gameMode = if (isLocalPvp) "Local Co-op Arena" else "Online simulated Arena Match",
                                                    playerScore = player1Hits,
                                                    opponentScore = if (didUserWin) 0 else 5,
                                                    events = matchEventTexts.toList()
                                                )
                                                isGeneratingCommentary = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SYNTHESIS COMMENTARY HIGHLIGHTS", color = CosmicDark, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isGeneratingCommentary) {
                                    CircularProgressIndicator(
                                        color = NeonYellow,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                    Text("CONTACTING NEON-DOWNLINK RE-SYNTHESIS CORE...", color = NeonYellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    Surface(
                                        color = CosmicDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = aiCommentaryText,
                                            color = NeonGreen,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val newHighlight = MatchHighlight(
                                                        title = if (didUserWin) "ARENA GLORY #X" else "CAPACITOR LOCKDOWN",
                                                        gameMode = if (isLocalPvp) "Local PVP" else "Online Arena",
                                                        result = if (didUserWin) "Victory" else "Defeat",
                                                        scoreSummary = "$player1Hits - ${if (didUserWin) 2 else 5}",
                                                        narrative = aiCommentaryText,
                                                        matchEventsText = matchEventTexts.joinToString(",")
                                                    )
                                                    repository.insertHighlight(newHighlight)
                                                    highlightSaved = true
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (highlightSaved) TextMuted else NeonCyan
                                            ),
                                            enabled = !highlightSaved,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (highlightSaved) Icons.Default.CheckCircle else Icons.Default.Check,
                                                contentDescription = "Save highlights",
                                                tint = CosmicDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (highlightSaved) "PRESERVED" else "SAVE LOGS", color = CosmicDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "📱 CHECK MY NEON LASER HIGHLIGHT! ✨\n\n$aiCommentaryText"
                                                    )
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Highlight To Socials"))
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = CosmicDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SOCIAL SHARE", color = CosmicDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onBackToLobby,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("back_lobby_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SpaceCard),
                            border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RETURN TO DOCK LOBBY", color = TextWhite, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
