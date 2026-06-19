package com.example.game.ui

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameCanvasView(
    modifier: Modifier = Modifier,
    isLocalPvp: Boolean,
    playerWeapon: LaserType,
    botWeapon: LaserType,
    isMatchActive: Boolean,
    onMatchFinished: (winnerId: Int, playerHits: Int, totalShots: Int, events: List<String>) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.toFloat().toDp().toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.toFloat().toDp().toPx() }

    // --- 3D Projection Camera Customisation state ---
    var targetTiltDegrees by remember { mutableStateOf(52f) }
    var targetHeight3D by remember { mutableStateOf(120f) }

    val cameraTiltDegrees by animateFloatAsState(
        targetValue = targetTiltDegrees,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "cameraTilt"
    )

    val cameraHeight3D by animateFloatAsState(
        targetValue = targetHeight3D,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "cameraHeight"
    )

    // 3D Perspective Projection Mathematics helper
    fun project3D(x: Float, y: Float, z: Float, width3d: Float, height3d: Float): Offset {
        if (cameraTiltDegrees == 0f) {
            return Offset(x, y - z) // Flat 2D view with layered vertical heights
        }
        val cx = width3d / 2f
        val cy = height3d / 2f

        val dx = x - cx
        val dy = y - cy

        val angleRad = Math.toRadians(cameraTiltDegrees.toDouble())
        val cosT = cos(angleRad).toFloat()
        val sinT = sin(angleRad).toFloat()

        // Euler projection skewing depth
        val rotatedY = dy * cosT - z * sinT
        val depth = dy * sinT + z * cosT + cameraHeight3D

        val d = 1100f
        val safeDepth = max(60f, depth + 600f)
        val perspectiveFactor = d / safeDepth

        val px = cx + dx * perspectiveFactor
        val py = cy + rotatedY * perspectiveFactor

        return Offset(px, py)
    }

    // Depth Scale Finder to scale circles, ships and bullets
    fun getPerspectiveScale(y: Float, z: Float): Float {
        if (cameraTiltDegrees == 0f) {
            return 1.0f
        }
        val dy = y - (screenHeightPx / 2f)
        val angleRad = Math.toRadians(cameraTiltDegrees.toDouble())
        val cosT = cos(angleRad).toFloat()
        val sinT = sin(angleRad).toFloat()
        val depth = dy * sinT + z * cosT + cameraHeight3D
        val d = 1100f
        val safeDepth = max(60f, depth + 600f)
        return d / safeDepth
    }

    // --- Core Game Entities State ---
    val player1 = remember {
        mutableStateOf(
            Spaceship(
                id = 1,
                position = Vec2(screenWidthPx * 0.25f, screenHeightPx * 0.75f),
                color = NeonCyan,
                statusTitle = "CYAN GUARDIAN"
            )
        )
    }

    val player2 = remember {
        mutableStateOf(
            Spaceship(
                id = 2,
                position = Vec2(screenWidthPx * 0.75f, screenHeightPx * 0.25f),
                color = NeonMagenta,
                statusTitle = if (isLocalPvp) "MAGENTA DEFENDER" else "VIRTUAL PHANTOM"
            )
        )
    }

    val bullets = remember { mutableStateListOf<Bullet>() }
    val particles = remember { mutableStateListOf<Particle>() }
    
    // --- Static Arena Obstacles ---
    val obstacles = remember(screenWidthPx, screenHeightPx) {
        val cx = screenWidthPx / 2f
        val cy = screenHeightPx / 2f
        listOf(
            Obstacle(id = 1, centerX = cx, centerY = cy, radius = 65f, color = NeonBlue),
            Obstacle(id = 2, centerX = cx - 180f, centerY = cy - 250f, radius = 45f, color = SpaceCard),
            Obstacle(id = 3, centerX = cx + 180f, centerY = cy + 250f, radius = 45f, color = SpaceCard)
        )
    }

    // --- Interactive Touch Controllers state ---
    var p1JoystickCenter by remember { mutableStateOf(Offset.Zero) }
    var p1JoystickOffset by remember { mutableStateOf(Offset.Zero) }
    var p1IsFiring by remember { mutableStateOf(false) }

    var p2JoystickCenter by remember { mutableStateOf(Offset.Zero) }
    var p2JoystickOffset by remember { mutableStateOf(Offset.Zero) }
    var p2IsFiring by remember { mutableStateOf(false) }

    // --- Live Game Match stats ---
    var p1AmmoCount by remember { mutableStateOf(20) }
    var p2AmmoCount by remember { mutableStateOf(20) }
    var player1Shots by remember { mutableStateOf(0) }
    var player1Hits by remember { mutableStateOf(0) }

    val matchHighlightEvents = remember { mutableStateListOf<String>() }

    // Synchronize weapon selections
    LaunchedEffect(playerWeapon) {
        player1.value = player1.value.copy(activeWeapon = playerWeapon)
    }
    LaunchedEffect(botWeapon) {
        player2.value = player2.value.copy(activeWeapon = botWeapon)
    }

    // Set initial custom positioning on layout ready
    LaunchedEffect(screenWidthPx, screenHeightPx) {
        if (screenWidthPx > 0 && screenHeightPx > 0) {
            player1.value = player1.value.copy(
                position = Vec2(screenWidthPx * 0.5f, screenHeightPx * 0.8f)
            )
            player2.value = player2.value.copy(
                position = Vec2(screenWidthPx * 0.5f, screenHeightPx * 0.2f)
            )

            p1JoystickCenter = Offset(150f, screenHeightPx - 250f)
            p2JoystickCenter = Offset(screenWidthPx - 150f, 250f)
        }
    }

    // --- CONTINUOUS GAME REFRESH LOOP ---
    LaunchedEffect(isMatchActive) {
        if (!isMatchActive) return@LaunchedEffect

        var frameCounter = 0
        while (isMatchActive && player1.value.health > 0f && player2.value.health > 0f) {
            frameCounter++

            // Apply virtual joystick move vectors
            val dx1 = p1JoystickOffset.x
            val dy1 = p1JoystickOffset.y
            if (p1JoystickOffset != Offset.Zero) {
                player1.value.angle = Math.toDegrees(atan2(dy1.toDouble(), dx1.toDouble())).toFloat()
                player1.value.velocity.x += (dx1 / 100f) * 0.82f
                player1.value.velocity.y += (dy1 / 100f) * 0.82f
                
                // Combat thruster engine dust
                if (frameCounter % 3 == 0) {
                    particles.add(
                        Particle(
                            position = Vec2(player1.value.position.x - cos(Math.toRadians(player1.value.angle.toDouble())).toFloat() * 25f,
                                            player1.value.position.y - sin(Math.toRadians(player1.value.angle.toDouble())).toFloat() * 25f),
                            velocity = Vec2(-player1.value.velocity.x * 0.3f, -player1.value.velocity.y * 0.3f),
                            color = NeonCyan,
                            life = 0.5f
                        )
                    )
                }
            }

            // Player 2 Flight controllers
            if (isLocalPvp) {
                val dx2 = p2JoystickOffset.x
                val dy2 = p2JoystickOffset.y
                if (p2JoystickOffset != Offset.Zero) {
                    player2.value.angle = Math.toDegrees(atan2(dy2.toDouble(), dx2.toDouble())).toFloat()
                    player2.value.velocity.x += (dx2 / 100f) * 0.82f
                    player2.value.velocity.y += (dy2 / 100f) * 0.82f

                    if (frameCounter % 3 == 0) {
                        particles.add(
                            Particle(
                                position = Vec2(player2.value.position.x - cos(Math.toRadians(player2.value.angle.toDouble())).toFloat() * 25f,
                                                player2.value.position.y - sin(Math.toRadians(player2.value.angle.toDouble())).toFloat() * 25f),
                                velocity = Vec2(-player2.value.velocity.x * 0.3f, -player2.value.velocity.y * 0.3f),
                                color = NeonMagenta,
                                life = 0.5f
                            )
                        )
                    }
                }
            } else {
                // Run bot path-finding AI
                PhysicsEngine.performBotAI(
                    bot = player2.value,
                    victim = player1.value,
                    bullets = bullets,
                    width = screenWidthPx,
                    height = screenHeightPx,
                    currentTime = System.currentTimeMillis(),
                    onFired = {
                        matchHighlightEvents.add("Bot launched ${player2.value.activeWeapon.label} attack! Velocity ${player2.value.activeWeapon.projectileSpeed}")
                    }
                )
            }

            // Fire weapons on continuous holding
            val timeNow = System.currentTimeMillis()
            if (p1IsFiring && p1AmmoCount > 0) {
                val fireCooldown = when (player1.value.activeWeapon) {
                    LaserType.BLAST_BEAM -> 350L
                    LaserType.BOUNCE_SHOT -> 440L
                    LaserType.WAVE_WINDER -> 1100L
                    LaserType.RECHARGE_PULSE -> 650L
                }
                if (timeNow - player1.value.lastFiredTime > fireCooldown) {
                    val angleRad = Math.toRadians(player1.value.angle.toDouble())
                    val bulletVel = Vec2(
                        cos(angleRad).toFloat() * player1.value.activeWeapon.projectileSpeed,
                        sin(angleRad).toFloat() * player1.value.activeWeapon.projectileSpeed
                    )
                    bullets.add(
                        Bullet(
                            id = System.nanoTime(),
                            position = Vec2(player1.value.position.x, player1.value.position.y),
                            velocity = bulletVel,
                            type = player1.value.activeWeapon,
                            isFromPlayer1 = true,
                            color = NeonCyan
                        )
                    )
                    player1.value.lastFiredTime = timeNow
                    player1Shots++
                    p1AmmoCount = (p1AmmoCount - 1).coerceAtLeast(0)
                    matchHighlightEvents.add("Player fired ${player1.value.activeWeapon.label} laser.")
                }
            }

            // Local Player 2 Firing inputs
            if (isLocalPvp && p2IsFiring && p2AmmoCount > 0) {
                val fireCooldown = when (player2.value.activeWeapon) {
                    LaserType.BLAST_BEAM -> 350L
                    LaserType.BOUNCE_SHOT -> 440L
                    LaserType.WAVE_WINDER -> 1100L
                    LaserType.RECHARGE_PULSE -> 650L
                }
                if (timeNow - player2.value.lastFiredTime > fireCooldown) {
                    val angleRad = Math.toRadians(player2.value.angle.toDouble())
                    val bulletVel = Vec2(
                        cos(angleRad).toFloat() * player2.value.activeWeapon.projectileSpeed,
                        sin(angleRad).toFloat() * player2.value.activeWeapon.projectileSpeed
                    )
                    bullets.add(
                        Bullet(
                            id = System.nanoTime(),
                            position = Vec2(player2.value.position.x, player2.value.position.y),
                            velocity = bulletVel,
                            type = player2.value.activeWeapon,
                            isFromPlayer1 = false,
                            color = NeonMagenta
                        )
                    )
                    player2.value.lastFiredTime = timeNow
                    p2AmmoCount = (p2AmmoCount - 1).coerceAtLeast(0)
                    matchHighlightEvents.add("P2 fired ${player2.value.activeWeapon.label} laser.")
                }
            }

            // Recharge empty ammo tanks automatically over time
            if (frameCounter % 15 == 0) {
                p1AmmoCount = (p1AmmoCount + 1).coerceAtMost(25)
                p2AmmoCount = (p2AmmoCount + 1).coerceAtMost(25)
            }

            // Physics execution step
            PhysicsEngine.updateSpaceship(player1.value, screenWidthPx, screenHeightPx, obstacles)
            PhysicsEngine.updateSpaceship(player2.value, screenWidthPx, screenHeightPx, obstacles)

            PhysicsEngine.updateBulletsAndParticles(
                bullets = bullets,
                particles = particles,
                player1 = player1.value,
                player2 = player2.value,
                obstacles = obstacles,
                width = screenWidthPx,
                height = screenHeightPx,
                onHit = { hitBullet, victimId, isBounced ->
                    if (victimId == 2) {
                        player2.value.health = max(0f, player2.value.health - hitBullet.type.baseDamage)
                        player1Hits++
                        if (isBounced) {
                            matchHighlightEvents.add("SPECTACULAR BOUNCE SHOT! Player ricochet hit Opponent for ${hitBullet.type.baseDamage} damage!")
                        } else {
                            matchHighlightEvents.add("Direct Hit on Opponent using ${hitBullet.type.label}!")
                        }
                    } else {
                        player1.value.health = max(0f, player1.value.health - hitBullet.type.baseDamage)
                        if (isBounced) {
                            matchHighlightEvents.add("TRICKY BOUNCE HITS PLAYER! Rebound laser landed on Cyan hull!")
                        } else {
                            matchHighlightEvents.add("Player armor fractured! Took hit from opponent!")
                        }
                    }
                }
            )

            // Trigger Compose Redraw
            p1JoystickOffset = p1JoystickOffset
            delay(16)
        }

        // Match terminating condition
        val finalWinnerId = if (player1.value.health <= 0f) 2 else if (player2.value.health <= 0f) 1 else 0
        if (finalWinnerId > 0) {
            matchHighlightEvents.add("COMBAT COMPLETE. Winner: ID $finalWinnerId")
            onMatchFinished(finalWinnerId, player1Hits, player1Shots, matchHighlightEvents.toList())
        }
    }

    // --- VIEW COMPONENT ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
            .testTag("game_canvas_root")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { motionEvent ->
                    val pointerCount = motionEvent.pointerCount

                    // Set default centers on first interactive tap if zero
                    if (p1JoystickCenter == Offset.Zero) {
                        p1JoystickCenter = Offset(150f, screenHeightPx - 200f)
                    }
                    if (isLocalPvp && p2JoystickCenter == Offset.Zero) {
                        p2JoystickCenter = Offset(screenWidthPx - 150f, 200f)
                    }

                    // Scan and categorise all active finger touches
                    for (i in 0 until pointerCount) {
                        val touchX = motionEvent.getX(i)
                        val touchY = motionEvent.getY(i)
                        val touchOffset = Offset(touchX, touchY)

                        // Is touch in Player 1 zone (Bottom half of screen)
                        val isBottomHalf = touchY > screenHeightPx * 0.5f
                        if (isBottomHalf) {
                            // Check if inside Joy controllers circle
                            val joyDist = (touchOffset - p1JoystickCenter).getDistance()
                            
                            // Fire action: right side of bottom screen is fire, left is joy
                            val isLeftOfBottomScreen = touchX < screenWidthPx * 0.45f
                            if (isLeftOfBottomScreen) {
                                if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                                    p1JoystickOffset = Offset.Zero
                                } else {
                                    val angleRad = atan2(touchY - p1JoystickCenter.y, touchX - p1JoystickCenter.x)
                                    val radius = min(joyDist, 100f)
                                    p1JoystickOffset = Offset(cos(angleRad) * radius, sin(angleRad) * radius)
                                }
                            } else {
                                p1IsFiring = (motionEvent.actionMasked != MotionEvent.ACTION_UP && motionEvent.actionMasked != MotionEvent.ACTION_POINTER_UP)
                            }
                        } else {
                            // Player 2 zone (Top half of screen)
                            if (isLocalPvp) {
                                val joyDist = (touchOffset - p2JoystickCenter).getDistance()
                                
                                val isRightOfTopScreen = touchX > screenWidthPx * 0.55f
                                if (isRightOfTopScreen) {
                                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                                        p2JoystickOffset = Offset.Zero
                                    } else {
                                        val angleRad = atan2(touchY - p2JoystickCenter.y, touchX - p2JoystickCenter.x)
                                        val radius = min(joyDist, 100f)
                                        p2JoystickOffset = Offset(cos(angleRad) * radius, sin(angleRad) * radius)
                                    }
                                } else {
                                    p2IsFiring = (motionEvent.actionMasked != MotionEvent.ACTION_UP && motionEvent.actionMasked != MotionEvent.ACTION_POINTER_UP)
                                }
                            }
                        }
                    }

                    // Reset when all fingers lift off
                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                        p1JoystickOffset = Offset.Zero
                        p2JoystickOffset = Offset.Zero
                        p1IsFiring = false
                        p2IsFiring = false
                    }

                    true
                }
        ) {
            // Draw holographic 3D backdrop grids & cage enclosing arena boundaries
            drawBackdropGrid3D(
                project = { x, y, z -> project3D(x, y, z, size.width, size.height) },
                screenWidth = screenWidthPx,
                scrollHeight = screenHeightPx
            )

            // Draw Obstacles in arena as glowing 3D Columns
            for (obs in obstacles) {
                val obsHeight = 45f
                val baseCenter = project3D(obs.centerX, obs.centerY, 0f, size.width, size.height)
                val topCenter = project3D(obs.centerX, obs.centerY, obsHeight, size.width, size.height)
                
                val scaleBase = getPerspectiveScale(obs.centerY, 0f)
                val scaleTop = getPerspectiveScale(obs.centerY, obsHeight)
                
                val rBase = obs.radius * scaleBase
                val rTop = obs.radius * scaleTop

                // Circle bases
                drawCircle(
                    color = obs.color.copy(alpha = 0.12f),
                    radius = rBase,
                    center = baseCenter
                )
                drawCircle(
                    color = obs.color.copy(alpha = 0.35f),
                    radius = rBase,
                    center = baseCenter,
                    style = Stroke(width = 2f)
                )

                // Cylinder wire pillars connecting base & top caps
                val numSegments = 12
                for (i in 0 until numSegments) {
                    val angle1 = (i * 2 * PI / numSegments).toFloat()
                    val cos1 = cos(angle1)
                    val sin1 = sin(angle1)

                    val pB = project3D(obs.centerX + cos1 * obs.radius, obs.centerY + sin1 * obs.radius, 0f, size.width, size.height)
                    val pT = project3D(obs.centerX + cos1 * obs.radius, obs.centerY + sin1 * obs.radius, obsHeight, size.width, size.height)

                    drawLine(
                        color = obs.color.copy(alpha = 0.22f),
                        start = pB,
                        end = pT,
                        strokeWidth = 1f
                    )
                }

                // Top caps
                drawCircle(
                    color = obs.color.copy(alpha = 0.2f),
                    radius = rTop,
                    center = topCenter
                )
                drawCircle(
                    color = obs.color,
                    radius = rTop,
                    center = topCenter,
                    style = Stroke(width = 3.2f)
                )
                drawCircle(
                    color = obs.color.copy(alpha = 0.5f),
                    radius = (obs.radius - 12f) * scaleTop,
                    center = topCenter,
                    style = Stroke(width = 1f)
                )
            }

            // Draw Energy Projectile Trail Lines
            for (bullet in bullets) {
                val bulletZ = 12f // Hovering flight elevation
                if (bullet.trail.size > 1) {
                    val path = Path()
                    val startProj = project3D(bullet.trail[0].x, bullet.trail[0].y, bulletZ, size.width, size.height)
                    path.moveTo(startProj.x, startProj.y)
                    
                    for (index in 1 until bullet.trail.size) {
                        val pt = project3D(bullet.trail[index].x, bullet.trail[index].y, bulletZ, size.width, size.height)
                        path.lineTo(pt.x, pt.y)
                    }
                    val scaleFactor = getPerspectiveScale(bullet.position.y, bulletZ)
                    drawPath(
                        path = path,
                        color = bullet.color.copy(alpha = 0.45f),
                        style = Stroke(width = bullet.radius * 1.8f * scaleFactor, cap = StrokeCap.Round)
                    )
                }

                val currentBOffset = project3D(bullet.position.x, bullet.position.y, bulletZ, size.width, size.height)
                val currentScale = getPerspectiveScale(bullet.position.y, bulletZ)

                drawCircle(
                    color = Color.White,
                    radius = bullet.radius * 0.75f * currentScale,
                    center = currentBOffset
                )
                drawCircle(
                    color = bullet.color,
                    radius = bullet.radius * currentScale,
                    center = currentBOffset,
                    style = Stroke(width = 2.5f)
                )
            }

            // Draw Spark / Explosion Particles ascending in height inside canvas
            for (p in particles) {
                val particleZ = 12f + (1f - p.life) * 85f
                val pProj = project3D(p.position.x, p.position.y, particleZ, size.width, size.height)
                val pScale = getPerspectiveScale(p.position.y, particleZ)
                drawCircle(
                    color = p.color.copy(alpha = p.life),
                    radius = p.size * p.life * pScale,
                    center = pProj
                )
            }

            // Draw Spaceship models
            if (player1.value.health > 0f) {
                drawPlayerShip3D(
                    ship = player1.value,
                    project = { x, y, z -> project3D(x, y, z, size.width, size.height) },
                    scaleOf = { y, z -> getPerspectiveScale(y, z) },
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx
                )
            }
            if (player2.value.health > 0f) {
                drawPlayerShip3D(
                    ship = player2.value,
                    project = { x, y, z -> project3D(x, y, z, size.width, size.height) },
                    scaleOf = { y, z -> getPerspectiveScale(y, z) },
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx
                )
            }

            // Draw HUD Indicators
            drawHUD(player1 = player1.value, player2 = player2.value, isLocalPvp = isLocalPvp, p1Ammo = p1AmmoCount, p2Ammo = p2AmmoCount, width = size.width, height = size.height)

            // Draw Interactive 2D Virtual Joysticks
            if (p1JoystickCenter != Offset.Zero) {
                drawVirtualJoystick(p1JoystickCenter, p1JoystickOffset, NeonCyan)
            }
            if (isLocalPvp && p2JoystickCenter != Offset.Zero) {
                drawVirtualJoystick(p2JoystickCenter, p2JoystickOffset, NeonMagenta)
            }
        }

        // --- 3D Camera Preset Overlay Controller ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(CosmicDark.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(1.dp, NebulaTerminal, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cameraPresets = listOf(
                Triple("2D TACTICAL", 0f, 0f),
                Triple("3D ISOMETRIC", 42f, 180f),
                Triple("3D CINEMATIC", 54f, 120f),
                Triple("3D DEEP DRONE", 64f, 75f)
            )
            cameraPresets.forEach { (label, tilt, height) ->
                val isSelected = targetTiltDegrees == tilt && targetHeight3D == height
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else TextMuted.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            targetTiltDegrees = tilt
                            targetHeight3D = height
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ---------------- Helper Render Methods ----------------

private fun DrawScope.drawBackdropGrid3D(
    project: (Float, Float, Float) -> Offset,
    screenWidth: Float,
    scrollHeight: Float
) {
    val sizeStep = 80f
    val cols = (screenWidth / sizeStep).toInt()
    val rows = (scrollHeight / sizeStep).toInt()

    // Horizontal Lines in 3D perspective
    for (i in 0..rows) {
        val y = i * sizeStep
        val startPoint = project(0f, y, 0f)
        val endPoint = project(screenWidth, y, 0f)
        drawLine(
            color = Color(0xFF1E1C4F).copy(alpha = 0.45f),
            start = startPoint,
            end = endPoint,
            strokeWidth = 1.8f
        )
    }

    // Vertical Lines in 3D perspective
    for (i in 0..cols) {
        val x = i * sizeStep
        val startPoint = project(x, 0f, 0f)
        val endPoint = project(x, scrollHeight, 0f)
        drawLine(
            color = Color(0xFF1E1C4F).copy(alpha = 0.45f),
            start = startPoint,
            end = endPoint,
            strokeWidth = 1.8f
        )
    }

    // 3D glow cage around entire combat arena bounds
    val pRect = 20f
    val cageFloorTL = project(pRect, pRect, 0f)
    val cageFloorTR = project(screenWidth - pRect, pRect, 0f)
    val cageFloorBR = project(screenWidth - pRect, scrollHeight - pRect, 0f)
    val cageFloorBL = project(pRect, scrollHeight - pRect, 0f)

    val cageHeight = 50f
    val cageCeilTL = project(pRect, pRect, cageHeight)
    val cageCeilTR = project(screenWidth - pRect, pRect, cageHeight)
    val cageCeilBR = project(screenWidth - pRect, scrollHeight - pRect, cageHeight)
    val cageCeilBL = project(pRect, scrollHeight - pRect, cageHeight)

    // Join floor base path
    val pathFloor = Path().apply {
        moveTo(cageFloorTL.x, cageFloorTL.y)
        lineTo(cageFloorTR.x, cageFloorTR.y)
        lineTo(cageFloorBR.x, cageFloorBR.y)
        lineTo(cageFloorBL.x, cageFloorBL.y)
        close()
    }
    drawPath(pathFloor, color = NeonBlue.copy(alpha = 0.25f), style = Stroke(width = 3f))

    // Join ceil ceiling path
    val pathCeil = Path().apply {
        moveTo(cageCeilTL.x, cageCeilTL.y)
        lineTo(cageCeilTR.x, cageCeilTR.y)
        lineTo(cageCeilBR.x, cageCeilBR.y)
        lineTo(cageCeilBL.x, cageCeilBL.y)
        close()
    }
    drawPath(pathCeil, color = NeonBlue.copy(alpha = 0.65f), style = Stroke(width = 3f))

    // Pillars supporting corners of cyber dungeon
    drawLine(color = NeonBlue.copy(alpha = 0.4f), start = cageFloorTL, end = cageCeilTL, strokeWidth = 2.5f)
    drawLine(color = NeonBlue.copy(alpha = 0.4f), start = cageFloorTR, end = cageCeilTR, strokeWidth = 2.5f)
    drawLine(color = NeonBlue.copy(alpha = 0.4f), start = cageFloorBR, end = cageCeilBR, strokeWidth = 2.5f)
    drawLine(color = NeonBlue.copy(alpha = 0.4f), start = cageFloorBL, end = cageCeilBL, strokeWidth = 2.5f)

    // Intermediate horizontal support frame
    for (f in listOf(0.4f)) {
        val levelHeight = cageHeight * f
        val lvlTL = project(pRect, pRect, levelHeight)
        val lvlTR = project(screenWidth - pRect, pRect, levelHeight)
        val lvlBR = project(screenWidth - pRect, scrollHeight - pRect, levelHeight)
        val lvlBL = project(pRect, scrollHeight - pRect, levelHeight)
        
        val buildPath = Path().apply {
            moveTo(lvlTL.x, lvlTL.y)
            lineTo(lvlTR.x, lvlTR.y)
            lineTo(lvlBR.x, lvlBR.y)
            lineTo(lvlBL.x, lvlBL.y)
            close()
        }
        drawPath(buildPath, color = NeonBlue.copy(alpha = 0.15f), style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawPlayerShip3D(
    ship: Spaceship,
    project: (Float, Float, Float) -> Offset,
    scaleOf: (Float, Float) -> Float,
    screenWidth: Float,
    screenHeight: Float
) {
    val s = ship.size
    val angleRad = Math.toRadians(ship.angle.toDouble())
    val cosA = cos(angleRad).toFloat()
    val sinA = sin(angleRad).toFloat()

    // 3D coordinates relative to center (dx, dy, dz)
    val noseXVal = 0.65f * s
    val noseYVal = 0f
    val noseZVal = 10f

    val wingLX = -0.55f * s
    val wingLY = -0.45f * s
    val wingLZ = 2f

    val wingRX = -0.55f * s
    val wingRY = 0.45f * s
    val wingRZ = 2f

    val tailXVal = -0.35f * s
    val tailYVal = 0f
    val tailZVal = 5f

    val cabinXVal = -0.1f * s
    val cabinYVal = 0f
    val cabinZVal = 22f

    fun getRotatedProjected(dx: Float, dy: Float, dz: Float): Offset {
        val rotX = dx * cosA - dy * sinA
        val rotY = dx * sinA + dy * cosA
        return project(ship.position.x + rotX, ship.position.y + rotY, dz)
    }

    val pNose = getRotatedProjected(noseXVal, noseYVal, noseZVal)
    val pWingL = getRotatedProjected(wingLX, wingLY, wingLZ)
    val pWingR = getRotatedProjected(wingRX, wingRY, wingRZ)
    val pTail = getRotatedProjected(tailXVal, tailYVal, tailZVal)
    val pCabin = getRotatedProjected(cabinXVal, cabinYVal, cabinZVal)

    val floorPath = Path().apply {
        moveTo(pWingL.x, pWingL.y)
        lineTo(pNose.x, pNose.y)
        lineTo(pWingR.x, pWingR.y)
        lineTo(pTail.x, pTail.y)
        close()
    }

    val leftFacet = Path().apply {
        moveTo(pNose.x, pNose.y)
        lineTo(pCabin.x, pCabin.y)
        lineTo(pWingL.x, pWingL.y)
        close()
    }

    val rightFacet = Path().apply {
        moveTo(pNose.x, pNose.y)
        lineTo(pCabin.x, pCabin.y)
        lineTo(pWingR.x, pWingR.y)
        close()
    }

    val tailFacet = Path().apply {
        moveTo(pWingL.x, pWingL.y)
        lineTo(pCabin.x, pCabin.y)
        lineTo(pWingR.x, pWingR.y)
        lineTo(pTail.x, pTail.y)
        close()
    }

    // Shaded face polygons
    drawPath(floorPath, color = ship.color.copy(alpha = 0.12f))
    drawPath(leftFacet, color = ship.color.copy(alpha = 0.32f))
    drawPath(rightFacet, color = ship.color.copy(alpha = 0.22f))
    drawPath(tailFacet, color = ship.color.copy(alpha = 0.18f))

    // Mesh lines
    drawPath(floorPath, color = ship.color, style = Stroke(width = 3.2f, join = StrokeJoin.Round))
    drawPath(leftFacet, color = ship.color, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
    drawPath(rightFacet, color = ship.color, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
    drawPath(tailFacet, color = ship.color, style = Stroke(width = 1.8f, join = StrokeJoin.Round))

    // Combustion engine visual core glow
    val exhaustCenter = getRotatedProjected(-0.45f * s, 0f, 6f)
    val floatScale = scaleOf(ship.position.y, 6f)
    drawCircle(
        color = NeonYellow,
        radius = s * 0.16f * floatScale,
        center = exhaustCenter
    )

    // Shield bubble
    if (ship.isShieldActive) {
        val sCenter = project(ship.position.x, ship.position.y, 10f)
        val sRadius = s * 0.95f * scaleOf(ship.position.y, 10f)
        drawCircle(
            color = ship.color.copy(alpha = 0.1f),
            radius = sRadius,
            center = sCenter
        )
        drawCircle(
            color = ship.color,
            radius = sRadius,
            center = sCenter,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f)))
        )
    }
}

private fun DrawScope.drawVirtualJoystick(center: Offset, offset: Offset, col: Color) {
    drawCircle(
        color = col.copy(alpha = 0.12f),
        radius = 100f,
        center = center
    )
    drawCircle(
        color = col.copy(alpha = 0.45f),
        radius = 100f,
        center = center,
        style = Stroke(width = 3f)
    )

    val handleCenter = center + offset
    drawCircle(
        color = col,
        radius = 35f,
        center = handleCenter
    )
    drawCircle(
        color = Color.White,
        radius = 20f,
        center = handleCenter
    )
}

private fun DrawScope.drawHUD(
    player1: Spaceship,
    player2: Spaceship,
    isLocalPvp: Boolean,
    p1Ammo: Int,
    p2Ammo: Int,
    width: Float,
    height: Float
) {
    val marginX = 40f
    val hBarWidth = 240f
    val hBarHeight = 12f

    // P1 Health bar
    val p1HUDOffset = Offset(marginX + 10f, height - 70f)
    drawRect(
        color = Color.DarkGray,
        topLeft = p1HUDOffset,
        size = Size(hBarWidth, hBarHeight)
    )
    drawRect(
        color = NeonCyan,
        topLeft = p1HUDOffset,
        size = Size(hBarWidth * (player1.health / 100f), hBarHeight)
    )
    val p1AmmoWidth = (hBarWidth / 25) * p1Ammo
    drawRect(
        color = NeonGreen.copy(alpha = 0.85f),
        topLeft = Offset(p1HUDOffset.x, p1HUDOffset.y + 18f),
        size = Size(p1AmmoWidth, 6f)
    )

    // P2 Health bar
    val p2HUDOffset = Offset(width - marginX - hBarWidth, 50f)
    drawRect(
        color = Color.DarkGray,
        topLeft = p2HUDOffset,
        size = Size(hBarWidth, hBarHeight)
    )
    drawRect(
        color = NeonMagenta,
        topLeft = p2HUDOffset,
        size = Size(hBarWidth * (player2.health / 100f), hBarHeight)
    )
    val p2AmmoWidth = (hBarWidth / 25) * p2Ammo
    drawRect(
        color = NeonGreen.copy(alpha = 0.85f),
        topLeft = Offset(p2HUDOffset.x + (hBarWidth - p2AmmoWidth), p2HUDOffset.y + 18f),
        size = Size(p2AmmoWidth, 6f)
    )
}
