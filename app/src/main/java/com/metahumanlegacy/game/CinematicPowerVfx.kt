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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class PowerVfxKind { FIRE, ICE, WATER, WIND, SPEED, STRENGTH, ENERGY, TIME, FORCEFIELD, PSYCHIC, MYSTIC, COSMIC, UNKNOWN }

internal fun powerVfxKind(powerFamily: String): PowerVfxKind {
    val p = powerFamily.lowercase()
    return when {
        listOf("feu", "flamme", "therm", "chaleur").any(p::contains) -> PowerVfxKind.FIRE
        listOf("glace", "froid", "cry", "ice").any(p::contains) -> PowerVfxKind.ICE
        listOf("eau", "hydro", "marée", "water").any(p::contains) -> PowerVfxKind.WATER
        listOf("air", "vent", "aéro", "wind").any(p::contains) -> PowerVfxKind.WIND
        listOf("vitesse", "speed", "cinétique").any(p::contains) -> PowerVfxKind.SPEED
        listOf("force", "physique", "muscl", "densité").any(p::contains) -> PowerVfxKind.STRENGTH
        listOf("énergie", "elect", "foudre", "plasma", "lightning").any(p::contains) -> PowerVfxKind.ENERGY
        listOf("temps", "chrono", "time").any(p::contains) -> PowerVfxKind.TIME
        listOf("barrière", "bouclier", "champ", "force field").any(p::contains) -> PowerVfxKind.FORCEFIELD
        listOf("mental", "psy", "télépath", "esprit", "mind").any(p::contains) -> PowerVfxKind.PSYCHIC
        listOf("myst", "occult", "mag", "rituel").any(p::contains) -> PowerVfxKind.MYSTIC
        listOf("cosm", "grav", "espace", "dimension").any(p::contains) -> PowerVfxKind.COSMIC
        else -> PowerVfxKind.UNKNOWN
    }
}

@Composable
internal fun CinematicPowerVfx(
    powerFamily: String,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val settings = LocalMetahumanMotion.current.settings
    val profile = powerVisualProfile(powerFamily)
    val kind = powerVfxKind(powerFamily)
    val transition = rememberInfiniteTransition(label = "power-vfx-$kind")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(2600, settings), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "power-vfx-phase"
    )
    val p = if (settings.reduceMotion) .35f else phase
    val safeIntensity = intensity.coerceIn(0f, 1f)

    Canvas(modifier) {
        if (safeIntensity <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val center = Offset(w * .5f, h * .48f)
        val unit = size.minDimension

        fun particle(i: Int, radius: Float, speed: Float = 1f): Offset {
            val angle = (i * .71f + p * PI.toFloat() * 2f * speed)
            val r = radius * (.55f + (i % 5) * .10f)
            return Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
        }

        when (kind) {
            PowerVfxKind.FIRE -> {
                repeat(18) { i ->
                    val x = center.x + sin(i * .9f + p * 5f) * unit * .15f
                    val y = center.y + unit * .23f - ((p + i * .073f) % 1f) * unit * .48f
                    val r = unit * (.007f + (i % 4) * .002f)
                    drawCircle(if (i % 3 == 0) profile.secondary else profile.accent, r, Offset(x, y), alpha = .16f + safeIntensity * .36f)
                }
                drawCircle(profile.accent.copy(alpha = .035f * safeIntensity), unit * .33f, center)
            }
            PowerVfxKind.ICE -> {
                repeat(10) { i ->
                    val pt = particle(i, unit * .25f, .25f)
                    val r = unit * (.012f + (i % 3) * .004f)
                    val diamond = Path().apply {
                        moveTo(pt.x, pt.y - r); lineTo(pt.x + r, pt.y); lineTo(pt.x, pt.y + r); lineTo(pt.x - r, pt.y); close()
                    }
                    drawPath(diamond, profile.secondary.copy(alpha = .28f + safeIntensity * .35f))
                }
            }
            PowerVfxKind.WATER -> {
                repeat(4) { i ->
                    val rr = unit * (.12f + i * .055f + p * .015f)
                    drawCircle(profile.accent.copy(alpha = (.13f - i * .02f) * safeIntensity), rr, center, style = Stroke(unit * .004f))
                }
            }
            PowerVfxKind.WIND -> {
                repeat(7) { i ->
                    val y = h * (.28f + i * .07f)
                    val x = ((p + i * .13f) % 1f) * w
                    drawLine(profile.secondary.copy(alpha = .16f * safeIntensity), Offset(x - w * .22f, y), Offset(x + w * .08f, y - h * .015f), unit * .003f)
                }
            }
            PowerVfxKind.SPEED -> {
                repeat(11) { i ->
                    val y = h * (.25f + i * .045f)
                    val x = ((p * 1.8f + i * .09f) % 1f) * w
                    drawLine(profile.secondary.copy(alpha = .22f * safeIntensity), Offset(x - w * .32f, y), Offset(x, y), unit * (.002f + (i % 2) * .001f))
                }
            }
            PowerVfxKind.STRENGTH -> {
                val shake = sin(p * PI * 8).toFloat() * unit * .006f * safeIntensity
                repeat(8) { i ->
                    val a = i * PI.toFloat() / 4f
                    val inner = Offset(center.x + cos(a) * unit * .12f + shake, center.y + sin(a) * unit * .12f)
                    val outer = Offset(center.x + cos(a) * unit * .30f + shake, center.y + sin(a) * unit * .30f)
                    drawLine(profile.accent.copy(alpha = .17f * safeIntensity), inner, outer, unit * .004f)
                }
            }
            PowerVfxKind.ENERGY -> {
                repeat(7) { i ->
                    val base = particle(i, unit * .24f, 1.4f)
                    val mid = Offset((base.x + center.x) / 2f + sin(i + p * 10f) * unit * .035f, (base.y + center.y) / 2f)
                    val bolt = Path().apply { moveTo(center.x, center.y); lineTo(mid.x, mid.y); lineTo(base.x, base.y) }
                    drawPath(bolt, profile.secondary.copy(alpha = .30f * safeIntensity), style = Stroke(unit * .004f))
                }
            }
            PowerVfxKind.TIME -> {
                repeat(3) { i ->
                    val rr = unit * (.13f + i * .07f)
                    drawCircle(profile.accent.copy(alpha = .12f * safeIntensity), rr, center, style = Stroke(unit * .003f))
                    val a = p * PI.toFloat() * 2f * (if (i % 2 == 0) 1f else -1f) + i
                    drawCircle(profile.secondary.copy(alpha = .48f * safeIntensity), unit * .008f, Offset(center.x + cos(a) * rr, center.y + sin(a) * rr))
                }
            }
            PowerVfxKind.FORCEFIELD -> {
                val rr = unit * (.26f + sin(p * PI * 2).toFloat() * .01f)
                drawCircle(profile.accent.copy(alpha = .055f * safeIntensity), rr, center)
                drawCircle(profile.secondary.copy(alpha = .25f * safeIntensity), rr, center, style = Stroke(unit * .005f))
            }
            PowerVfxKind.PSYCHIC -> {
                repeat(5) { i ->
                    val offset = sin(p * PI * 2 + i) * unit * .012f
                    drawCircle(profile.accent.copy(alpha = (.10f - i * .012f) * safeIntensity), unit * (.13f + i * .035f), Offset(center.x + offset, center.y), style = Stroke(unit * .003f))
                }
            }
            PowerVfxKind.MYSTIC -> {
                val rr = unit * .22f
                val points = 6
                val path = Path()
                repeat(points + 1) { i ->
                    val a = -PI.toFloat() / 2f + i * (PI.toFloat() * 2f / points) + p * .18f
                    val pt = Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, profile.secondary.copy(alpha = .20f * safeIntensity), style = Stroke(unit * .004f))
            }
            PowerVfxKind.COSMIC -> {
                repeat(20) { i ->
                    val pt = particle(i, unit * (.13f + (i % 5) * .04f), .16f + (i % 3) * .07f)
                    drawCircle(if (i % 4 == 0) Color.White else profile.secondary, unit * (.004f + (i % 3) * .002f), pt, alpha = .20f + safeIntensity * .28f)
                }
                drawCircle(profile.accent.copy(alpha = .045f * safeIntensity), unit * .30f, center)
            }
            PowerVfxKind.UNKNOWN -> {
                drawCircle(profile.accent.copy(alpha = .04f * safeIntensity), unit * .25f, center)
            }
        }
    }
}
