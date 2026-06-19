package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.LaserType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaponCustomizeScreen(
    currentSelection: LaserType,
    botSelection: LaserType,
    onSelectWeapon: (LaserType) -> Unit,
    onSelectBotWeapon: (LaserType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LASER CORES LAB", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("weapons_back")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDark)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicDark)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                color = SpaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("HYPER-CELL CORES MODIFIER", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Deploy specific ammunition modules to alter reflective bounds/ricochets.", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("CHOOSE PLAYER MODULE (CYAN)", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(8.dp))

            LaserType.entries.forEach { lType ->
                val isSelected = currentSelection == lType
                val cardBorder = if (isSelected) NeonCyan else NebulaTerminal

                Surface(
                    color = SpaceCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectWeapon(lType) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectWeapon(lType) },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan, unselectedColor = TextMuted)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(lType.label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(lType.description, color = TextMuted, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("BOUNCES: ${lType.maxBounces}", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                Text("DMG: ${lType.baseDamage.toInt()}", color = NeonMagenta, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("CHOOSE BOT/OPPONENT MODULE (MAGENTA)", color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LaserType.entries.take(4).forEach { lType ->
                    val isSelected = botSelection == lType
                    val col = if (isSelected) NeonMagenta else SpaceCard
                    val strokeCol = if (isSelected) NeonMagenta else NebulaTerminal

                    Surface(
                        color = col.copy(alpha = if (isSelected) 0.15f else 1.0f),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, strokeCol, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectBotWeapon(lType) }
                            .padding(vertical = 10.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = lType.label.split(" ").first(),
                            color = if (isSelected) NeonMagenta else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
