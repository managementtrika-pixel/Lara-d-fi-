package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** MetaHuman Legacy 4.0 global visual language. */
internal object VisualRebuild4 {
    val ink = Color(0xFF03050A)
    val midnight = Color(0xFF07111F)
    val steel = Color(0xFF14263A)
    val ivory = Color(0xFFF4F0E7)
    val amber = Color(0xFFFFC75A)
    val electric = Color(0xFF55B8FF)

    fun sceneAccent(scene: String): Color = when (scene.uppercase()) {
        "HOME", "LEGACY", "HALL" -> amber
        "CREATE", "ALIAS" -> Color(0xFF7EDCFF)
        "DESTIN", "CHRONIQUE" -> electric
        "VILLE", "MONDE" -> Color(0xFF73E6D2)
        else -> Color(0xFF8EA7FF)
    }

    fun particleCount(reduceMotion: Boolean): Int = if (reduceMotion) 0 else 28
    fun vignetteAlpha(highContrast: Boolean): Float = if (highContrast) .72f else .48f
}

private data class VisualSnapshot(val campaign: Campaign?, val state: UltimateState?)

private fun loadVisualSnapshot(context: Context): VisualSnapshot {
    val campaign = loadCampaignV4(context)
    val state = campaign?.let { UltimateStore.load(context, it) }
    return VisualSnapshot(campaign, state)
}

private fun sceneHint(campaign: Campaign?): String = when {
    campaign == null -> "HOME"
    campaign.finished -> "LEGACY"
    campaign.age == 18 && campaign.powerRevealed -> "ALIAS"
    campaign.age < 18 -> "DESTIN"
    else -> "MONDE"
}

/**
 * The 4.0 presentation stays outside simulation authority. It observes persisted state only,
 * so visual failures can never mutate story, saves, relationships or progression.
 */
@Composable
internal fun VisualRebuild4App(context: Context) {
    var snapshot by remember { mutableStateOf(loadVisualSnapshot(context)) }
    LaunchedEffect(context) {
        while (true) {
            delay(450)
            val next = loadVisualSnapshot(context)
            if (next != snapshot) snapshot = next
        }
    }
    val motion = LocalMetahumanMotion.current.settings
    Box(Modifier.fillMaxSize()) {
        GameplayRebuildApp(context)
        CinematicSceneLayer(
            campaign = snapshot.campaign,
            state = snapshot.state,
            scene = sceneHint(snapshot.campaign),
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = if (motion.highContrast) .10f else .17f
            }
        )
        VisualRebuild4Overlay(
            campaign = snapshot.campaign,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VisualRebuild4Overlay(campaign: Campaign?, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val transition = rememberInfiniteTransition(label = "visual-rebuild-4")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(11000, settings), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cinematic-atmosphere"
    )
    val slowPhase = if (settings.reduceMotion) .37f else phase
    val revealed = campaign?.powerRevealed == true
    val power = powerVisualProfile(campaign?.powerFamily.orEmpty())

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0x160B2B4C),
                .40f to Color.Transparent,
                .72f to Color(0x0800A7C4),
                1f to Color(0x31010207)
            )
        )

        val pulse = .82f + sin(slowPhase * PI * 2).toFloat() * .18f
        drawCircle(Color(0x1055B8FF).copy(alpha = .055f * pulse), w * .34f, Offset(w * .08f, h * .20f))
        drawCircle(Color(0x10FFC75A).copy(alpha = .045f * pulse), w * .26f, Offset(w * .92f, h * .73f))

        if (revealed) {
            val auraAlpha = if (settings.highContrast) .025f else .055f
            drawCircle(power.accent.copy(alpha = auraAlpha * pulse), w * .48f, Offset(w * .5f, h * .47f))
            drawCircle(power.secondary.copy(alpha = auraAlpha * .54f), w * .31f, Offset(w * .5f, h * .47f))
        }

        repeat(VisualRebuild4.particleCount(settings.reduceMotion)) { i ->
            val baseX = ((i * 47) % 101) / 100f
            val baseY = ((i * 73) % 97) / 100f
            val y = ((baseY + slowPhase * (.10f + (i % 5) * .018f)) % 1f) * h
            val x = baseX * w + sin((slowPhase + i) * PI).toFloat() * w * .012f
            val alpha = .035f + (i % 4) * .012f
            val color = if (revealed && i % 5 == 0) power.secondary else Color(0xFFB9E7FF)
            drawLine(color.copy(alpha = alpha), Offset(x, y), Offset(x - w * .006f, y + h * .018f), (1f + i % 2))
        }

        val grainStep = (w / 38f).coerceAtLeast(8f)
        var gy = grainStep * .5f
        var row = 0
        while (gy < h) {
            var gx = grainStep * .5f + if (row % 2 == 0) 0f else grainStep * .5f
            while (gx < w) {
                val n = (((gx / grainStep).toInt() * 17 + row * 31) and 7)
                if (n == 0) drawRect(Color.White.copy(alpha = .018f), Offset(gx, gy), Size(1.2f, 1.2f))
                gx += grainStep
            }
            gy += grainStep
            row++
        }

        val va = VisualRebuild4.vignetteAlpha(settings.highContrast)
        val edge = w * .12f
        val topEdge = h * .08f
        val bottomEdge = h * .12f
        val left = Path().apply { moveTo(0f, 0f); lineTo(edge, 0f); lineTo(edge * .34f, h); lineTo(0f, h); close() }
        val right = Path().apply { moveTo(w, 0f); lineTo(w - edge, 0f); lineTo(w - edge * .34f, h); lineTo(w, h); close() }
        drawPath(left, Color.Black.copy(alpha = va * .28f))
        drawPath(right, Color.Black.copy(alpha = va * .28f))
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = va * .32f), Color.Transparent)), size = Size(w, topEdge))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = va * .42f))), topLeft = Offset(0f, h - bottomEdge), size = Size(w, bottomEdge))
        drawRect(Color.White.copy(alpha = .035f), style = Stroke(1f))
    }
}
