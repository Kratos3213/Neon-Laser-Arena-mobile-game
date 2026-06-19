package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta

// --- Weapon Specifications ---
enum class LaserType(val label: String, val projectileSpeed: Float, val baseDamage: Float, val maxBounces: Int, val description: String) {
    BLAST_BEAM("Pulse Beam", 28f, 25f, 0, "High velocity, direct linear pulse laser."),
    BOUNCE_SHOT("Ricochet Laser", 18f, 15f, 3, "Bounces off barriers and arena boundaries."),
    WAVE_WINDER("Scythe Beam", 14f, 40f, 1, "Slow moving massive dual energy projectile."),
    RECHARGE_PULSE("Overcharge Pulse", 22f, 30f, 2, "Balanced plasma charge with moderate rebound.")
}

// --- Coordinate Vector ---
data class Vec2(var x: Float, var y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    fun length() = kotlin.math.sqrt(x * x + y * y)
    fun normalized(): Vec2 {
        val l = length()
        return if (l > 0) Vec2(x / l, y / l) else Vec2(0f, 0f)
    }
    fun dot(other: Vec2): Float = x * other.x + y * other.y
}

// --- Player Character & Controller ---
data class Bullet(
    var id: Long,
    var position: Vec2,
    var velocity: Vec2,
    val type: LaserType,
    val isFromPlayer1: Boolean,
    var bouncesLeft: Int = type.maxBounces,
    val color: Color,
    val radius: Float = 10f,
    var shouldDestroy: Boolean = false,
    var trail: MutableList<Offset> = mutableListOf()
)

data class Spaceship(
    var id: Int, // 1 for Player 1, 2 for Player 2
    var position: Vec2,
    var velocity: Vec2 = Vec2(0f, 0f),
    var angle: Float = if (id == 1) 0f else 180f, // degrees
    var health: Float = 100f,
    val color: Color = if (id == 1) NeonCyan else NeonMagenta,
    val size: Float = 55f,
    var lastFiredTime: Long = 0L,
    var activeWeapon: LaserType = LaserType.BLAST_BEAM,
    var isShieldActive: Boolean = false,
    var shieldEnergy: Float = 100f,
    var statusTitle: String = "SQUADRON LEADER"
)

// --- obstacles ---
data class Obstacle(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val color: Color
)

// --- Particles ---
data class Particle(
    var position: Vec2,
    var velocity: Vec2,
    var color: Color,
    var life: Float = 1.0f, // 1.0 down to 0.0
    val decay: Float = 0.05f + (Math.random() * 0.05f).toFloat(),
    val size: Float = 5f + (Math.random() * 8f).toFloat()
)

// --- Game Settings & Configuration ---
data class MatchHighlightEvent(
    val eventType: String, // e.g., "First Blood", "Ricochet Kill", "Close Shave", "Double Rebound"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
