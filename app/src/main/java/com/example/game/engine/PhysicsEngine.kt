package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonYellow
import kotlin.math.*

object PhysicsEngine {

    /**
     * Updates spaceship physics, friction, and clamp to arena bounds.
     */
    fun updateSpaceship(ship: Spaceship, width: Float, height: Float, obstacles: List<Obstacle>) {
        // Apply velocity with high friction/inertia dampening (space friction)
        ship.position.x += ship.velocity.x
        ship.position.y += ship.velocity.y

        ship.velocity.x *= 0.94f // Dampen speed
        ship.velocity.y *= 0.94f

        val halfSize = ship.size / 2f

        // Clamp to screen bounds
        if (ship.position.x < halfSize) {
            ship.position.x = halfSize
            ship.velocity.x = 0f
        }
        if (ship.position.x > width - halfSize) {
            ship.position.x = width - halfSize
            ship.velocity.x = 0f
        }
        if (ship.position.y < halfSize) {
            ship.position.y = halfSize
            ship.velocity.y = 0f
        }
        if (ship.position.y > height - halfSize) {
            ship.position.y = height - halfSize
            ship.velocity.y = 0f
        }

        // Bounce/push out of circular obstacles
        for (obs in obstacles) {
            val dx = ship.position.x - obs.centerX
            val dy = ship.position.y - obs.centerY
            val dist = sqrt(dx * dx + dy * dy)
            val minDist = obs.radius + halfSize
            if (dist < minDist && dist > 0f) {
                // Resolve collision: push out
                val pushX = (dx / dist) * (minDist - dist)
                val pushY = (dy / dist) * (minDist - dist)
                ship.position.x += pushX
                ship.position.y += pushY
                // Halt speed
                ship.velocity.x = 0f
                ship.velocity.y = 0f
            }
        }
    }

    /**
     * Updates lasers/bullets movement, manages bounces, and checks collisions.
     */
    fun updateBulletsAndParticles(
        bullets: MutableList<Bullet>,
        particles: MutableList<Particle>,
        player1: Spaceship,
        player2: Spaceship,
        obstacles: List<Obstacle>,
        width: Float,
        height: Float,
        onHit: (bullet: Bullet, victimId: Int, isBounced: Boolean) -> Unit
    ) {
        // 1. Update existing Bullet objects
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            
            // Record trail
            bullet.trail.add(Offset(bullet.position.x, bullet.position.y))
            if (bullet.trail.size > 8) {
                bullet.trail.removeAt(0)
            }

            // Move
            bullet.position.x += bullet.velocity.x
            bullet.position.y += bullet.velocity.y

            // Arena border bounces
            var bouncedThisFrame = false
            if (bullet.position.x < bullet.radius) {
                bullet.position.x = bullet.radius
                if (bullet.bouncesLeft > 0) {
                    bullet.velocity.x = -bullet.velocity.x
                    bullet.bouncesLeft--
                    bouncedThisFrame = true
                } else bullet.shouldDestroy = true
            } else if (bullet.position.x > width - bullet.radius) {
                bullet.position.x = width - bullet.radius
                if (bullet.bouncesLeft > 0) {
                    bullet.velocity.x = -bullet.velocity.x
                    bullet.bouncesLeft--
                    bouncedThisFrame = true
                } else bullet.shouldDestroy = true
            }

            if (bullet.position.y < bullet.radius) {
                bullet.position.y = bullet.radius
                if (bullet.bouncesLeft > 0) {
                    bullet.velocity.y = -bullet.velocity.y
                    bullet.bouncesLeft--
                    bouncedThisFrame = true
                } else bullet.shouldDestroy = true
            } else if (bullet.position.y > height - bullet.radius) {
                bullet.position.y = height - bullet.radius
                if (bullet.bouncesLeft > 0) {
                    bullet.velocity.y = -bullet.velocity.y
                    bullet.bouncesLeft--
                    bouncedThisFrame = true
                } else bullet.shouldDestroy = true
            }

            if (bouncedThisFrame) {
                spawnSparks(bullet.position, bullet.color, particles)
            }

            // Obstacle rebounds
            for (obs in obstacles) {
                val dx = bullet.position.x - obs.centerX
                val dy = bullet.position.y - obs.centerY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < obs.radius + bullet.radius) {
                    // We hit a circle! Normal projection bounce
                    if (bullet.bouncesLeft > 0 && dist > 0f) {
                        // Specs vector
                        val normalX = dx / dist
                        val normalY = dy / dist
                        
                        // Reflected vector: V_new = V - 2 * (V.N) * N
                        val dotProduct = bullet.velocity.x * normalX + bullet.velocity.y * normalY
                        bullet.velocity.x = bullet.velocity.x - 2 * dotProduct * normalX
                        bullet.velocity.y = bullet.velocity.y - 2 * dotProduct * normalY

                        // Push bullet out of obstacle slightly
                        bullet.position.x = obs.centerX + normalX * (obs.radius + bullet.radius + 1f)
                        bullet.position.y = obs.centerY + normalY * (obs.radius + bullet.radius + 1f)

                        bullet.bouncesLeft--
                        spawnSparks(bullet.position, bullet.color, particles)
                    } else {
                        bullet.shouldDestroy = true
                    }
                    break
                }
            }

            // check ship collisions
            // Victim 2 (Player 2)
            if (!bullet.shouldDestroy && bullet.isFromPlayer1) {
                val dx = bullet.position.x - player2.position.x
                val dy = bullet.position.y - player2.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < player2.size / 1.7f) {
                    bullet.shouldDestroy = true
                    val isBounced = (bullet.type.maxBounces - bullet.bouncesLeft) > 0
                    onHit(bullet, 2, isBounced)
                    spawnExplosion(bullet.position, player2.color, particles)
                }
            }

            // Victim 1 (Player 1)
            if (!bullet.shouldDestroy && !bullet.isFromPlayer1) {
                val dx = bullet.position.x - player1.position.x
                val dy = bullet.position.y - player1.position.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < player1.size / 1.7f) {
                    bullet.shouldDestroy = true
                    val isBounced = (bullet.type.maxBounces - bullet.bouncesLeft) > 0
                    onHit(bullet, 1, isBounced)
                    spawnExplosion(bullet.position, player1.color, particles)
                }
            }

            if (bullet.shouldDestroy) {
                bulletIterator.remove()
            }
        }

        // 2. Update particle systems
        val pIterator = particles.iterator()
        while (pIterator.hasNext()) {
            val p = pIterator.next()
            p.position.x += p.velocity.x
            p.position.y += p.velocity.y
            p.life -= p.decay
            if (p.life <= 0f) {
                pIterator.remove()
            }
        }
    }

    private fun spawnSparks(position: Vec2, color: Color, particles: MutableList<Particle>) {
        for (i in 0..6) {
            val angle = Math.random() * 2.0 * Math.PI
            val speed = 2f + (Math.random() * 4f).toFloat()
            particles.add(
                Particle(
                    position = Vec2(position.x, position.y),
                    velocity = Vec2(cos(angle).toFloat() * speed, sin(angle).toFloat() * speed),
                    color = color
                )
            )
        }
    }

    private fun spawnExplosion(position: Vec2, color: Color, particles: MutableList<Particle>) {
        for (i in 0..20) {
            val angle = Math.random() * 2.0 * Math.PI
            val speed = 3f + (Math.random() * 8f).toFloat()
            particles.add(
                Particle(
                    position = Vec2(position.x, position.y),
                    velocity = Vec2(cos(angle).toFloat() * speed, sin(angle).toFloat() * speed),
                    color = if (Math.random() > 0.4) color else NeonYellow
                )
            )
        }
    }

    /**
     * Super polished responsive AI bot algorithm.
     */
    fun performBotAI(
        bot: Spaceship,
        victim: Spaceship,
        bullets: MutableList<Bullet>,
        width: Float,
        height: Float,
        currentTime: Long,
        onFired: () -> Unit
    ) {
        if (bot.health <= 0f || victim.health <= 0f) return

        // Vector to target
        val vectorToTarget = victim.position - bot.position
        val distance = vectorToTarget.length()

        // Face target
        if (distance > 0f) {
            val targetAngle = Math.toDegrees(atan2(vectorToTarget.y.toDouble(), vectorToTarget.x.toDouble())).toFloat()
            bot.angle = targetAngle
        }

        // Decision logic
        val desiredDist = 200f
        val followSpeed = 2.2f

        if (distance > desiredDist + 80) {
            // Fly closer to the user
            val normDir = vectorToTarget.normalized()
            bot.velocity.x += normDir.x * followSpeed
            bot.velocity.y += normDir.y * followSpeed
        } else if (distance < desiredDist - 40) {
            // Back away slightly to keep tactical distance
            val normDir = vectorToTarget.normalized()
            bot.velocity.x -= normDir.x * followSpeed
            bot.velocity.y -= normDir.y * followSpeed
        } else {
            // Circle left/right
            val normDir = vectorToTarget.normalized()
            val circleDir = Vec2(-normDir.y, normDir.x)
            bot.velocity.x += circleDir.x * (followSpeed * 1.5f)
            bot.velocity.y += circleDir.y * (followSpeed * 1.5f)
        }

        // Fire laser weapon!
        val fireCooldown = when(bot.activeWeapon) {
            LaserType.BLAST_BEAM -> 600L
            LaserType.BOUNCE_SHOT -> 800L
            LaserType.WAVE_WINDER -> 1600L
            LaserType.RECHARGE_PULSE -> 1000L
        }

        val predictionBias = if (bot.activeWeapon == LaserType.WAVE_WINDER) 0.5f else 0.25f
        if (currentTime - bot.lastFiredTime > fireCooldown && distance < 750f) {
            // Shoot at player with lead predictive heading (leads with player's actual velocity)
            val leadTarget = Vec2(victim.position.x + victim.velocity.x * predictionBias * 10, victim.position.y + victim.velocity.y * predictionBias * 10)
            val shootVector = (leadTarget - bot.position).normalized()

            val shootAngle = Math.toDegrees(atan2(shootVector.y.toDouble(), shootVector.x.toDouble())).toFloat()
            bot.angle = shootAngle

            val projectileVelocity = shootVector * bot.activeWeapon.projectileSpeed
            
            bullets.add(
                Bullet(
                    id = System.nanoTime(),
                    position = Vec2(bot.position.x, bot.position.y),
                    velocity = projectileVelocity,
                    type = bot.activeWeapon,
                    isFromPlayer1 = false,
                    color = bot.color
                )
            )
            bot.lastFiredTime = currentTime
            onFired()
        }

        // Randomly activate defensive cyber shielding
        if (distance < 250f && bot.shieldEnergy > 40f && !bot.isShieldActive && Math.random() < 0.03) {
            bot.isShieldActive = true
        } else if (bot.isShieldActive && (bot.shieldEnergy < 15f || Math.random() < 0.05)) {
            bot.isShieldActive = false
        }

        if (bot.isShieldActive) {
            bot.shieldEnergy = max(0f, bot.shieldEnergy - 0.45f)
        } else {
            bot.shieldEnergy = min(100f, bot.shieldEnergy + 0.22f)
        }
    }
}
