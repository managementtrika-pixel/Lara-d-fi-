package com.metahumanlegacy.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.sin

internal enum class CinematicSceneKind { HOME, CHILDHOOD, TEEN, AWAKENING, CITY, WORLD, LEGACY, DOSSIER }

internal fun cinematicSceneKind(scene: String, campaign: Campaign?): CinematicSceneKind {
    val s = scene.uppercase()
    return when {
        s.contains("LEGACY") || campaign?.finished == true -> CinematicSceneKind.LEGACY
        s.contains("ALIAS") || (campaign?.age == 18 && campaign.powerRevealed) -> CinematicSceneKind.AWAKENING
        s.contains("VILLE") || s.contains("MONDE") -> CinematicSceneKind.CITY
        s.contains("CHRONIQUE") || s.contains("HALL") || s.contains("SETTINGS") -> CinematicSceneKind.DOSSIER
        campaign == null || s.contains("HOME") || s.contains("CREATE") -> CinematicSceneKind.HOME
        campaign.age < 13 -> CinematicSceneKind.CHILDHOOD
        campaign.age < 18 -> CinematicSceneKind.TEEN
        else -> CinematicSceneKind.WORLD
    }
}

@Composable
internal fun CinematicSceneLayer(
    campaign: Campaign?,
    state: UltimateState?,
    scene: String,
    modifier: Modifier = Modifier
) {
    val motion = LocalMetahumanMotion.current.settings
    val kind = cinematicSceneKind(scene, campaign)
    val seed = campaign?.seed ?: 41L
    val power = powerVisualProfile(campaign?.powerFamily.orEmpty())
    val transition = rememberInfiniteTransition(label = "cinematic-scene-$scene")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(13000, motion), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinematic-scene-drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(3200, motion), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cinematic-scene-pulse"
    )
    val animatedDrift = if (motion.reduceMotion) 0f else drift
    val animatedPulse = if (motion.reduceMotion) .45f else pulse

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = when (kind) {
            CinematicSceneKind.CHILDHOOD -> h * .61f
            CinematicSceneKind.TEEN -> h * .66f
            CinematicSceneKind.AWAKENING -> h * .72f
            else -> h * .69f
        }

        val sky = when (kind) {
            CinematicSceneKind.HOME -> listOf(Color(0xFF090D16), Color(0xFF101827), Color(0xFF06070B))
            CinematicSceneKind.CHILDHOOD -> listOf(Color(0xFF273B55), Color(0xFF17283A), Color(0xFF0A0E14))
            CinematicSceneKind.TEEN -> listOf(Color(0xFF101C31), Color(0xFF1C2037), Color(0xFF080A0F))
            CinematicSceneKind.AWAKENING -> listOf(Color(0xFF080914), power.accent.copy(alpha = .38f), Color(0xFF05060A))
            CinematicSceneKind.CITY, CinematicSceneKind.WORLD -> listOf(Color(0xFF0A1B31), Color(0xFF151C29), Color(0xFF05070A))
            CinematicSceneKind.LEGACY -> listOf(Color(0xFF15120D), Color(0xFF171016), Color(0xFF050505))
            CinematicSceneKind.DOSSIER -> listOf(Color(0xFF090D12), Color(0xFF10151D), Color(0xFF060708))
        }
        drawRect(Brush.verticalGradient(sky))

        // Far atmospheric glow.
        val glow = when (kind) {
            CinematicSceneKind.LEGACY -> UltimateGold
            CinematicSceneKind.AWAKENING -> power.accent
            else -> Color(0xFF6DA9E8)
        }
        drawCircle(glow.copy(alpha = .07f + animatedPulse * .05f), w * .55f, Offset(w * (.76f + animatedDrift * .015f), h * .18f))
        drawCircle(Color(0xFF90C7FF).copy(alpha = .04f), w * .42f, Offset(w * .12f, h * .32f))

        // Distant skyline with deterministic silhouettes.
        val buildingCount = 18
        val step = w / buildingCount
        repeat(buildingCount) { index ->
            val hash = abs(((seed ushr (index % 16)) + index * 97L).toInt())
            val height = h * (.08f + (hash % 20) / 100f)
            val x = index * step + animatedDrift * ((index % 4) - 1.5f) * 2.1f
            val y = horizon - height
            val brickBias = state?.architecture?.contains("Brique", true) == true
            val base = if (brickBias) Color(0xFF24181A) else Color(0xFF131C29)
            drawRect(base.copy(alpha = .92f), Offset(x, y), Size(step * 1.08f, height + h - horizon))
            if (index % 2 == 0) {
                repeat(4) { row ->
                    if ((hash + row) % 3 != 0) {
                        drawRect(
                            UltimateGold.copy(alpha = .18f + (hash % 4) * .04f),
                            Offset(x + step * .18f, y + height * (.15f + row * .18f)),
                            Size(step * .12f, h * .006f)
                        )
                    }
                }
            }
        }

        // Midground architectural masses and parallax framing.
        val midOffset = animatedDrift * w * .008f
        drawRect(Color(0xFF0B111A).copy(alpha = .94f), Offset(-w * .08f + midOffset, horizon * .93f), Size(w * .33f, h))
        drawRect(Color(0xFF0A0F17).copy(alpha = .94f), Offset(w * .79f - midOffset, horizon * .88f), Size(w * .34f, h))

        // Road / ground perspective.
        val road = Path().apply {
            moveTo(w * .20f, h)
            lineTo(w * .44f, horizon)
            lineTo(w * .57f, horizon)
            lineTo(w * .92f, h)
            close()
        }
        drawPath(road, Color(0xFF090D12))
        drawLine(Color(0xFFB4C5D8).copy(alpha = .08f), Offset(w * .50f, horizon), Offset(w * .56f, h), w * .004f)

        // Weather, age and scene atmosphere.
        val rainy = state?.climate?.contains("Pluv", true) == true || state?.climate?.contains("Orage", true) == true || kind == CinematicSceneKind.TEEN
        if (rainy) {
            repeat(38) { i ->
                val x = w * (((i * 37 + (seed % 23).toInt()) % 101) / 100f)
                val baseY = h * (((i * 61 + (seed % 17).toInt()) % 97) / 100f)
                val y = (baseY + animatedPulse * h * .035f) % h
                drawLine(Color(0xFFA6C8E4).copy(alpha = .15f), Offset(x, y), Offset(x - w * .012f, y + h * .032f), w * .0015f)
            }
        }

        if (kind == CinematicSceneKind.CHILDHOOD) {
            // Warm window glow: the early years feel intimate before the metahuman spectacle.
            drawRect(Color(0xFFFFD98C).copy(alpha = .12f), Offset(w * .12f, h * .28f), Size(w * .26f, h * .17f))
            drawCircle(Color(0xFFFFD88A).copy(alpha = .10f), w * .22f, Offset(w * .23f, h * .35f))
        }

        if (kind == CinematicSceneKind.AWAKENING) {
            val rr = size.minDimension * (.16f + animatedPulse * .05f)
            drawCircle(power.accent.copy(alpha = .08f), rr * 2.2f, Offset(w * .50f, h * .55f))
            drawCircle(power.accent.copy(alpha = .19f), rr, Offset(w * .50f, h * .55f))
            repeat(18) { i ->
                val phase = (i * 0.31f + animatedPulse * 2f)
                val px = w * .5f + sin(phase) * rr * (1f + (i % 4) * .22f)
                val py = h * .55f - rr * .9f + (i % 7) * rr * .24f
                drawCircle(power.secondary.copy(alpha = .42f), size.minDimension * .006f, Offset(px, py))
            }
        }

        if (kind == CinematicSceneKind.LEGACY) {
            // A distant monument silhouette gives end screens a mythic, archived feeling.
            drawRect(Color(0xFF0B0A09), Offset(w * .46f, h * .36f), Size(w * .08f, h * .34f))
            drawCircle(UltimateGold.copy(alpha = .16f), w * .13f, Offset(w * .50f, h * .34f))
        }

        // Foreground silhouettes create depth without obscuring the UI.
        drawRect(Color.Black.copy(alpha = .34f), Offset(0f, h * .91f), Size(w, h * .09f))
        drawCircle(Color.Black.copy(alpha = .30f), w * .19f, Offset(-w * .03f, h * .91f))
        drawCircle(Color.Black.copy(alpha = .28f), w * .17f, Offset(w * 1.02f, h * .90f))
    }
}
