package com.metahumanlegacy.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun AwakeningCinematic4(
    campaign: Campaign,
    modifier: Modifier = Modifier
) {
    if (campaign.age != 18 || !campaign.powerRevealed) return
    val settings = LocalMetahumanMotion.current.settings
    val profile = powerVisualProfile(campaign.powerFamily)
    val progress = remember(campaign.seed, campaign.powerFamily) { Animatable(if (settings.reduceMotion) 1f else 0f) }

    LaunchedEffect(campaign.seed, campaign.powerFamily, settings.reduceMotion, settings.speed) {
        if (settings.reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = MetahumanMotionTokens.duration(MetahumanMotionTokens.LEGENDARY, settings),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Canvas(modifier) {
        val p = progress.value.coerceIn(0f, 1f)
        val center = Offset(size.width * .5f, size.height * .48f)
        val unit = size.minDimension

        // Brief white-hot core that falls away into the family's color.
        val coreAlpha = ((1f - p) * 0.28f).coerceIn(0f, .28f)
        if (coreAlpha > .005f) drawCircle(Color.White.copy(alpha = coreAlpha), unit * (.08f + p * .38f), center)

        val waveRadius = unit * (.10f + p * .54f)
        val waveAlpha = ((1f - p) * .70f).coerceIn(0f, .70f)
        drawCircle(profile.accent.copy(alpha = waveAlpha * .34f), waveRadius, center)
        drawCircle(profile.secondary.copy(alpha = waveAlpha), waveRadius, center, style = Stroke(unit * .007f))
        drawCircle(profile.accent.copy(alpha = waveAlpha * .42f), waveRadius * .72f, center, style = Stroke(unit * .003f))

        repeat(18) { i ->
            val a = i * (PI.toFloat() * 2f / 18f) + p * .12f
            val inner = unit * (.08f + p * .08f)
            val outer = unit * (.18f + p * (.25f + (i % 4) * .025f))
            val start = Offset(center.x + cos(a) * inner, center.y + sin(a) * inner)
            val end = Offset(center.x + cos(a) * outer, center.y + sin(a) * outer)
            drawLine(
                if (i % 3 == 0) profile.secondary.copy(alpha = waveAlpha * .68f) else profile.accent.copy(alpha = waveAlpha * .46f),
                start,
                end,
                unit * if (i % 4 == 0) .005f else .0025f
            )
        }

        // Persistent low-intensity halo after the reveal keeps age 18 visually exceptional.
        val settled = (p * .08f).coerceAtMost(.08f)
        drawCircle(profile.accent.copy(alpha = settled), unit * .31f, center)
    }
}
