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
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

internal data class WorldVisualPressure(
    val damage: Float,
    val surveillance: Float,
    val technology: Float,
    val publicAttention: Float
)

internal fun worldVisualPressure(state: UltimateState): WorldVisualPressure {
    val legal = state.legalStatus.lowercase()
    val media = state.mediaFrame.lowercase()
    return WorldVisualPressure(
        damage = ((100 - state.cityCondition).coerceIn(0, 100) / 100f),
        surveillance = when {
            listOf("recherch", "hors-la-loi", "ennemi", "fugitif").any(legal::contains) -> .95f
            listOf("surveill", "encadr", "enregistr").any(legal::contains) -> .62f
            else -> .18f
        },
        technology = (state.cityTech.coerceIn(0, 100) / 100f),
        publicAttention = when {
            listOf("icône", "légende", "menace", "controvers", "célèbre").any(media::contains) -> .90f
            listOf("connu", "visible", "débat").any(media::contains) -> .64f
            media.contains("inconnu") -> .12f
            else -> .36f
        }
    )
}

@Composable
internal fun WorldStateVisualLayer4(state: UltimateState, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val pressure = worldVisualPressure(state)
    val transition = rememberInfiniteTransition(label = "world-state-v4")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(5000, settings), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "world-state-phase"
    )
    val p = if (settings.reduceMotion) .45f else phase

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * .70f

        // Damage persists as broken silhouettes and smoke instead of disappearing after narration.
        val damageCount = (pressure.damage * 9f).toInt()
        repeat(damageCount) { i ->
            val x = w * (.08f + (i * .113f) % .84f)
            val baseY = horizon - h * (.02f + (i % 4) * .025f)
            drawLine(Color(0xFF090B0E).copy(alpha = .42f), Offset(x, baseY), Offset(x + w * .035f, baseY - h * .035f), w * .006f)
            val smokeY = baseY - ((p + i * .09f) % 1f) * h * .10f
            drawCircle(Color(0xFF77818A).copy(alpha = .025f + pressure.damage * .05f), w * (.025f + (i % 3) * .006f), Offset(x, smokeY))
        }

        // High-tech cities acquire emissive infrastructure and data lines.
        if (pressure.technology > .38f) {
            val lines = (2 + pressure.technology * 7f).toInt()
            repeat(lines) { i ->
                val x = w * ((i + 1f) / (lines + 1f))
                val alpha = .035f + pressure.technology * .07f
                drawLine(Color(0xFF61D9FF).copy(alpha = alpha), Offset(x, h * .18f), Offset(x + sin(i + p * 6f) * w * .012f, horizon), w * .0018f)
                drawCircle(Color(0xFF73E6D2).copy(alpha = alpha * 1.6f), w * .004f, Offset(x, h * (.28f + (i % 5) * .07f)))
            }
        }

        // Legal pressure becomes moving search-light cones / surveillance points.
        if (pressure.surveillance > .35f) {
            val count = (1 + pressure.surveillance * 5f).toInt()
            repeat(count) { i ->
                val x = w * (.12f + (i * .19f) % .76f)
                val sweep = sin((p * 6.283f) + i * .9f) * w * .055f
                drawLine(
                    Color(0xFFFFE39A).copy(alpha = .025f + pressure.surveillance * .045f),
                    Offset(x, h * .10f),
                    Offset(x + sweep, horizon),
                    w * .010f
                )
                drawCircle(Color(0xFFFF695E).copy(alpha = .10f + pressure.surveillance * .16f), w * .0045f, Offset(x, h * (.13f + (i % 4) * .08f)))
            }
        }

        // Media/public attention appears as distant screens rather than another UI statistic.
        if (pressure.publicAttention > .45f) {
            val screens = (1 + pressure.publicAttention * 4f).toInt()
            repeat(screens) { i ->
                val sw = w * (.055f + (i % 2) * .012f)
                val sh = h * .025f
                val x = w * (.11f + (i * .21f) % .78f)
                val y = h * (.28f + (i % 3) * .09f)
                val flicker = .7f + sin(p * 12f + i) * .3f
                drawRect(Color(0xFF8BD9FF).copy(alpha = (.08f + pressure.publicAttention * .08f) * flicker), Offset(x, y), Size(sw, sh))
                drawRect(Color.White.copy(alpha = .035f), Offset(x + sw * .10f, y + sh * .22f), Size(sw * .65f, sh * .10f))
            }
        }
    }
}
