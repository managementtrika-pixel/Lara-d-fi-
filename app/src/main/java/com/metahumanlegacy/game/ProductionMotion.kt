package com.metahumanlegacy.game

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class MetahumanMotionLevel {
    MOTION_NONE,
    MOTION_SUBTLE,
    MOTION_STANDARD,
    MOTION_MAJOR,
    MOTION_LEGENDARY
}

internal enum class MetahumanMotionSpeed { NORMAL, FAST }

@Stable
internal data class MetahumanMotionSettings(
    val reduceMotion: Boolean = false,
    val speed: MetahumanMotionSpeed = MetahumanMotionSpeed.NORMAL,
    val haptics: Boolean = true,
    val highContrast: Boolean = false,
    val textScalePercent: Int = 100
)

@Stable
internal data class MetahumanMotionController(
    val settings: MetahumanMotionSettings,
    val update: (MetahumanMotionSettings) -> Unit
)

internal val LocalMetahumanMotion = compositionLocalOf {
    MetahumanMotionController(MetahumanMotionSettings()) { }
}

internal object MetahumanMotionPreferences {
    private const val FILE = "mhl_visual"

    fun load(context: Context): MetahumanMotionSettings {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return MetahumanMotionSettings(
            reduceMotion = p.getBoolean("reduce_motion", false),
            speed = if (p.getBoolean("motion_fast", false)) MetahumanMotionSpeed.FAST else MetahumanMotionSpeed.NORMAL,
            haptics = p.getBoolean("haptics", true),
            highContrast = p.getBoolean("high_contrast", false),
            textScalePercent = p.getInt("text_scale", 100).coerceIn(90, 120)
        )
    }

    fun save(context: Context, value: MetahumanMotionSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("reduce_motion", value.reduceMotion)
            .putBoolean("motion_fast", value.speed == MetahumanMotionSpeed.FAST)
            .putBoolean("haptics", value.haptics)
            .putBoolean("high_contrast", value.highContrast)
            .putInt("text_scale", value.textScalePercent.coerceIn(90, 120))
            .apply()
    }
}

internal object MetahumanMotionTokens {
    const val MICRO = 140
    const val FAST = 230
    const val NORMAL = 360
    const val EMPHASIS = 560
    const val REVEAL = 900
    const val LEGENDARY = 1800

    val Standard = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)
    val Enter = CubicBezierEasing(0.12f, 0.72f, 0.20f, 1.00f)
    val Exit = CubicBezierEasing(0.40f, 0.00f, 1.00f, 1.00f)
    val Impact = CubicBezierEasing(0.18f, 0.90f, 0.34f, 1.15f)
    val Legendary = CubicBezierEasing(0.16f, 0.00f, 0.15f, 1.00f)

    fun duration(base: Int, settings: MetahumanMotionSettings): Int = when {
        settings.reduceMotion -> minOf(base, 90)
        settings.speed == MetahumanMotionSpeed.FAST -> (base * 0.67f).toInt().coerceAtLeast(80)
        else -> base
    }

    fun duration(level: MetahumanMotionLevel, settings: MetahumanMotionSettings): Int = duration(
        when (level) {
            MetahumanMotionLevel.MOTION_NONE -> 1
            MetahumanMotionLevel.MOTION_SUBTLE -> MICRO
            MetahumanMotionLevel.MOTION_STANDARD -> NORMAL
            MetahumanMotionLevel.MOTION_MAJOR -> REVEAL
            MetahumanMotionLevel.MOTION_LEGENDARY -> LEGENDARY
        },
        settings
    )
}

internal object MotionBoard {
    const val PANEL_TRANSITION = 61
    const val CHOICE_FEEDBACK = 62
    const val OUTCOME_REVEAL = 63
    const val STAT_CHANGE = 64
    const val PRE_AWAKENING = 65
    const val AWAKENING = 66
    const val AURA = 67
    const val PROJECTILE = 68
    const val DEFENSE = 69
    const val MOVEMENT = 70
    const val IMPACT = 71
    const val OVERLOAD = 72
    const val INJURY = 73
    const val MEDIA = 74
    const val RELATION = 75
    const val AGING = 76
    const val CHAPTER = 77
    const val CLIMAX = 78
    const val AFTERMATH = 79
    const val LEGACY = 80
}

internal enum class PowerMovementStyle { GROUNDED, DASH, FLIGHT, TELEPORT }

internal data class PowerVisualProfile(
    val iconKey: String,
    val accent: Color,
    val secondary: Color,
    val movement: PowerMovementStyle,
    val auraBoard: Int = MotionBoard.AURA,
    val projectileBoard: Int = MotionBoard.PROJECTILE,
    val defenseBoard: Int = MotionBoard.DEFENSE,
    val movementBoard: Int = MotionBoard.MOVEMENT,
    val overloadBoard: Int = MotionBoard.OVERLOAD,
    val legendaryBoard: Int = MotionBoard.CLIMAX
)

internal fun powerVisualProfile(powerFamily: String): PowerVisualProfile {
    val p = powerFamily.lowercase()
    return when {
        listOf("feu", "flamme", "therm", "chaleur").any(p::contains) ->
            PowerVisualProfile("power_fire", Color(0xFFFF6B35), Color(0xFFFFC857), PowerMovementStyle.DASH)
        listOf("glace", "froid", "cry", "ice").any(p::contains) ->
            PowerVisualProfile("power_ice", Color(0xFF79D8FF), Color(0xFFE7FAFF), PowerMovementStyle.GROUNDED)
        listOf("eau", "hydro", "marée", "water").any(p::contains) ->
            PowerVisualProfile("power_water", Color(0xFF45A7FF), Color(0xFF8DEBFF), PowerMovementStyle.GROUNDED)
        listOf("air", "vent", "aéro", "wind").any(p::contains) ->
            PowerVisualProfile("power_wind", Color(0xFF95E7FF), Color(0xFFF1FFFF), PowerMovementStyle.FLIGHT)
        listOf("vitesse", "speed", "cinétique").any(p::contains) ->
            PowerVisualProfile("power_speed", Color(0xFFFFD84A), Color(0xFFFF7A1A), PowerMovementStyle.DASH)
        listOf("force", "physique", "muscl", "densité").any(p::contains) ->
            PowerVisualProfile("power_strength", Color(0xFFE94B5F), Color(0xFFFFA26B), PowerMovementStyle.GROUNDED)
        listOf("énergie", "elect", "foudre", "plasma", "lightning").any(p::contains) ->
            PowerVisualProfile("power_energy", Color(0xFF5BC0FF), Color(0xFFC365FF), PowerMovementStyle.DASH)
        listOf("temps", "chrono", "time").any(p::contains) ->
            PowerVisualProfile("power_time", Color(0xFFB47CFF), Color(0xFF8CF3FF), PowerMovementStyle.TELEPORT)
        listOf("barrière", "bouclier", "champ", "force field").any(p::contains) ->
            PowerVisualProfile("power_forcefield", Color(0xFF4EE0D1), Color(0xFF72A7FF), PowerMovementStyle.GROUNDED)
        listOf("mental", "psy", "télépath", "esprit", "mind").any(p::contains) ->
            PowerVisualProfile("origin_psychic", Color(0xFFC873FF), Color(0xFFFF7EDB), PowerMovementStyle.TELEPORT)
        listOf("myst", "occult", "mag", "rituel").any(p::contains) ->
            PowerVisualProfile("origin_mystic", Color(0xFF9B6CFF), Color(0xFFFFC95A), PowerMovementStyle.TELEPORT)
        listOf("cosm", "grav", "espace", "dimension").any(p::contains) ->
            PowerVisualProfile("power_cosmic", Color(0xFF8B7CFF), Color(0xFF55E0FF), PowerMovementStyle.FLIGHT)
        else -> PowerVisualProfile("origin_unknown", Color(0xFF7E8FA8), Color(0xFFD2D9E2), PowerMovementStyle.GROUNDED)
    }
}

internal object MetahumanAudioHooks {
    var onChoice: () -> Unit = { }
    var onImpact: () -> Unit = { }
    var onAwakening: () -> Unit = { }
    var onChapter: () -> Unit = { }
    var onClimax: () -> Unit = { }
    var onLegacy: () -> Unit = { }
}

@Composable
internal fun rememberMetahumanHaptic(): (MetahumanMotionLevel) -> Unit {
    val view = LocalView.current
    val enabled = LocalMetahumanMotion.current.settings.haptics
    return remember(view, enabled) {
        { level ->
            if (enabled) {
                val constant = when (level) {
                    MetahumanMotionLevel.MOTION_NONE -> null
                    MetahumanMotionLevel.MOTION_SUBTLE -> HapticFeedbackConstants.CLOCK_TICK
                    MetahumanMotionLevel.MOTION_STANDARD -> HapticFeedbackConstants.KEYBOARD_TAP
                    MetahumanMotionLevel.MOTION_MAJOR -> HapticFeedbackConstants.CONTEXT_CLICK
                    MetahumanMotionLevel.MOTION_LEGENDARY -> HapticFeedbackConstants.LONG_PRESS
                }
                if (constant != null) view.performHapticFeedback(constant)
            }
        }
    }
}

private fun semanticAccent(key: String): Color = when {
    key.contains("villain") || key.contains("fear") || key.contains("danger") || key.contains("rival") -> Color(0xFFE5483B)
    key.contains("gold") || key.contains("legend") || key.contains("prestige") || key.contains("brand") -> Color(0xFFF0C65A)
    key.contains("mystic") || key.contains("psychic") || key.contains("cosmic") || key.contains("time") -> Color(0xFF9D5CFF)
    key.contains("family") || key.contains("care") -> Color(0xFF57C96B)
    key.contains("fire") -> Color(0xFFFF6B35)
    key.contains("ice") || key.contains("water") -> Color(0xFF6ED8FF)
    else -> Color(0xFF2E83FF)
}

@Composable
internal fun MhlProductionAsset(
    key: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    pulse: Boolean = false
) {
    val settings = LocalMetahumanMotion.current.settings
    val accent = semanticAccent(key)
    val transition = rememberInfiniteTransition(label = "asset-$key")
    val breathing by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(2200, settings), easing = MetahumanMotionTokens.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "asset-breathe"
    )
    val scale = if (settings.reduceMotion || !pulse) 1f else breathing
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { this.contentDescription = contentDescription }
    ) {
        val r = this.size.minDimension * 0.43f
        val c = center
        val stroke = this.size.minDimension * 0.045f
        drawCircle(Color(0xFF101722), r)
        drawCircle(accent.copy(alpha = 0.95f), r, style = Stroke(stroke))
        drawCircle(accent.copy(alpha = 0.12f), r * 0.78f)

        when {
            key.contains("scope") -> {
                drawCircle(accent, r * 0.55f, style = Stroke(stroke * 0.72f))
                drawLine(accent, Offset(c.x - r * .75f, c.y), Offset(c.x + r * .75f, c.y), stroke * .6f)
                drawLine(accent, Offset(c.x, c.y - r * .75f), Offset(c.x, c.y + r * .75f), stroke * .6f)
            }
            key.contains("relation") || key.contains("family") -> {
                drawCircle(accent, r * .22f, Offset(c.x - r * .30f, c.y - r * .10f))
                drawCircle(accent, r * .22f, Offset(c.x + r * .30f, c.y - r * .10f))
                drawLine(accent, Offset(c.x - r * .45f, c.y + r * .42f), Offset(c.x + r * .45f, c.y + r * .42f), stroke)
            }
            key.contains("fire") || key.contains("energy") || key.contains("speed") -> {
                val path = Path().apply {
                    moveTo(c.x + r * .08f, c.y - r * .65f)
                    lineTo(c.x - r * .42f, c.y + r * .05f)
                    lineTo(c.x - r * .04f, c.y + r * .02f)
                    lineTo(c.x - r * .18f, c.y + r * .65f)
                    lineTo(c.x + r * .46f, c.y - r * .12f)
                    lineTo(c.x + r * .08f, c.y - r * .08f)
                    close()
                }
                drawPath(path, accent)
            }
            key.contains("forcefield") || key.contains("order") -> {
                val path = Path().apply {
                    moveTo(c.x, c.y - r * .65f)
                    lineTo(c.x + r * .52f, c.y - r * .30f)
                    lineTo(c.x + r * .38f, c.y + r * .48f)
                    lineTo(c.x, c.y + r * .68f)
                    lineTo(c.x - r * .38f, c.y + r * .48f)
                    lineTo(c.x - r * .52f, c.y - r * .30f)
                    close()
                }
                drawPath(path, accent.copy(alpha = .22f))
                drawPath(path, accent, style = Stroke(stroke * .75f))
            }
            key.contains("rank") || key.contains("prestige") || key.contains("brand") -> {
                val path = Path().apply {
                    for (i in 0 until 10) {
                        val a = -PI / 2 + i * PI / 5
                        val rr = if (i % 2 == 0) r * .63f else r * .28f
                        val x = c.x + cos(a).toFloat() * rr
                        val y = c.y + sin(a).toFloat() * rr
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, accent)
            }
            else -> {
                drawCircle(accent, r * .34f)
                drawCircle(Color(0xFF05070A), r * .14f)
                drawLine(accent, Offset(c.x - r * .66f, c.y), Offset(c.x + r * .66f, c.y), stroke * .55f)
                drawLine(accent, Offset(c.x, c.y - r * .66f), Offset(c.x, c.y + r * .66f), stroke * .55f)
            }
        }
    }
}

@Composable
internal fun MhlProductionBackdrop(scene: String, seed: Long, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val transition = rememberInfiniteTransition(label = "backdrop-$scene")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(12000, settings), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val dx = if (settings.reduceMotion) 0f else drift
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(if (settings.highContrast) Color(0xFF010203) else Color(0xFF080B10))
        val horizon = h * .70f
        val step = w / 9f
        for (i in 0..10) {
            val hash = ((seed ushr (i % 16)) + i * 37L).toInt()
            val bh = h * (.05f + ((hash and 7) / 90f))
            val x = i * step + dx * 4f * (1f + i % 3)
            drawRect(Color(0xFF101B2B).copy(alpha = if (scene == "HOME") .92f else .55f), Offset(x, horizon - bh), Size(step * .78f, bh + h * .30f))
        }
        val dot = 20.dp.toPx()
        var y = dot * .5f
        while (y < h) {
            var x = dot * .5f
            while (x < w) {
                drawCircle(Color(0x132E83FF), 1.05.dp.toPx(), Offset(x + dx * 2f, y))
                x += dot
            }
            y += dot
        }
        if (scene == "HOME" || scene == "LEGACY") {
            drawCircle(Color(0x12F0C65A), w * .42f, Offset(w * .50f + dx * 4f, h * .28f))
            drawCircle(Color(0x0B2E83FF), w * .58f, Offset(w * .15f - dx * 3f, h * .18f))
        }
    }
}

@Composable
internal fun MhlBoardTexture(board: Int, modifier: Modifier = Modifier, accent: Color = Color(0xFF2E83FF), alpha: Float = .14f) {
    val settings = LocalMetahumanMotion.current.settings
    val transition = rememberInfiniteTransition(label = "board-$board")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(2600 + (board % 4) * 500, settings), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val p = if (settings.reduceMotion) .45f else phase
    Canvas(modifier.fillMaxSize()) {
        when (board) {
            MotionBoard.PANEL_TRANSITION, MotionBoard.CHAPTER -> {
                val width = size.width * (.18f + p * .22f)
                drawRect(accent.copy(alpha = alpha), Offset(-size.width * .05f + p * size.width * .08f, 0f), Size(width, size.height))
                drawLine(accent.copy(alpha = alpha * 1.6f), Offset(size.width * .18f, 0f), Offset(size.width * .45f, size.height), 4.dp.toPx())
            }
            MotionBoard.CHOICE_FEEDBACK, MotionBoard.STAT_CHANGE -> {
                val rr = size.minDimension * (.08f + p * .06f)
                drawCircle(accent.copy(alpha = alpha * (1f - p * .45f)), rr, center, style = Stroke(3.dp.toPx()))
                drawCircle(accent.copy(alpha = alpha * .6f), rr * 1.7f, center, style = Stroke(1.dp.toPx()))
            }
            MotionBoard.PRE_AWAKENING, MotionBoard.OVERLOAD -> {
                for (i in 0 until 6) {
                    val yy = size.height * ((i * .17f + p * .09f) % 1f)
                    drawLine(accent.copy(alpha = alpha * .65f), Offset(0f, yy), Offset(size.width, yy + (i % 2) * 3f), 1.dp.toPx())
                }
            }
            MotionBoard.AURA, MotionBoard.AWAKENING -> {
                val rr = size.minDimension * (.22f + .09f * sin(p * PI * 2).toFloat())
                drawCircle(accent.copy(alpha = alpha * .75f), rr, center, style = Stroke(5.dp.toPx()))
                drawCircle(accent.copy(alpha = alpha * .35f), rr * 1.35f, center, style = Stroke(2.dp.toPx()))
            }
            MotionBoard.PROJECTILE, MotionBoard.MOVEMENT, MotionBoard.IMPACT, MotionBoard.CLIMAX -> {
                for (i in 0 until 9) {
                    val yy = size.height * (i + 1) / 10f
                    val start = size.width * ((p + i * .07f) % 1f)
                    drawLine(accent.copy(alpha = alpha), Offset(start - size.width * .34f, yy), Offset(start, yy), (1 + i % 3).dp.toPx())
                }
            }
            MotionBoard.DEFENSE -> {
                drawCircle(accent.copy(alpha = alpha * .55f), size.minDimension * .32f, center)
                drawCircle(accent.copy(alpha = alpha * 1.4f), size.minDimension * .32f, center, style = Stroke(3.dp.toPx()))
            }
            MotionBoard.INJURY -> drawRect(Color(0xFF9E2931).copy(alpha = alpha * .65f))
            MotionBoard.MEDIA -> {
                for (i in 0 until 12) {
                    val yy = size.height * i / 12f
                    drawLine(Color.White.copy(alpha = alpha * .18f), Offset(0f, yy), Offset(size.width, yy), 1f)
                }
            }
            MotionBoard.RELATION -> {
                drawCircle(accent.copy(alpha = alpha), size.minDimension * .09f, Offset(size.width * .40f, size.height * .50f))
                drawCircle(accent.copy(alpha = alpha), size.minDimension * .09f, Offset(size.width * .60f, size.height * .50f))
                drawLine(accent.copy(alpha = alpha), Offset(size.width * .45f, size.height * .50f), Offset(size.width * .55f, size.height * .50f), 3.dp.toPx())
            }
            MotionBoard.AGING, MotionBoard.AFTERMATH, MotionBoard.LEGACY -> {
                val top = Color(0x002E83FF)
                val bottom = if (board == MotionBoard.LEGACY) Color(0x44F0C65A) else Color(0x332E83FF)
                drawRect(brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(top, bottom.copy(alpha = alpha * 1.8f))))
            }
            else -> drawRect(accent.copy(alpha = alpha * .2f))
        }
    }
}

@Composable
internal fun MhlSceneFrame(sceneKey: Any, board: Int, level: MetahumanMotionLevel, modifier: Modifier = Modifier, accent: Color = Color(0xFF2E83FF), content: @Composable BoxScope.() -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val duration = MetahumanMotionTokens.duration(level, settings)
    val alphaAnim = remember(sceneKey) { Animatable(if (settings.reduceMotion) 1f else 0f) }
    val y = remember(sceneKey) { Animatable(if (settings.reduceMotion) 0f else 12f) }

    LaunchedEffect(sceneKey, settings.reduceMotion, settings.speed) {
        if (!settings.reduceMotion) {
            alphaAnim.snapTo(0f)
            y.snapTo(12f)
            alphaAnim.animateTo(1f, tween(duration, easing = MetahumanMotionTokens.Enter))
            y.animateTo(0f, tween(duration, easing = MetahumanMotionTokens.Enter))
        } else {
            alphaAnim.snapTo(1f)
            y.snapTo(0f)
        }
    }

    Box(modifier.graphicsLayer { this.alpha = alphaAnim.value; translationY = y.value }) {
        MhlBoardTexture(board, Modifier.matchParentSize(), accent, .12f)
        content()
    }
}

@Composable
internal fun MhlImpactOverlay(trigger: Any, accent: Color, level: MetahumanMotionLevel, board: Int = MotionBoard.IMPACT, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val p = remember(trigger) { Animatable(1f) }
    LaunchedEffect(trigger, settings.reduceMotion, settings.speed) {
        p.snapTo(0f)
        p.animateTo(1f, tween(MetahumanMotionTokens.duration(level, settings), easing = MetahumanMotionTokens.Impact))
    }
    val progress = if (settings.reduceMotion) 1f else p.value
    Canvas(modifier.fillMaxSize()) {
        if (progress < 1f) {
            val radius = size.minDimension * (.06f + progress * .48f)
            drawCircle(accent.copy(alpha = (1f - progress) * .28f), radius, center)
            drawCircle(accent.copy(alpha = (1f - progress) * .82f), radius, center, style = Stroke(2.5.dp.toPx()))
            if (board == MotionBoard.IMPACT || board == MotionBoard.CLIMAX) {
                for (i in 0 until 12) {
                    val a = i * (PI * 2 / 12)
                    val start = Offset(center.x + cos(a).toFloat() * radius * .6f, center.y + sin(a).toFloat() * radius * .6f)
                    val end = Offset(center.x + cos(a).toFloat() * radius * 1.35f, center.y + sin(a).toFloat() * radius * 1.35f)
                    drawLine(accent.copy(alpha = (1f - progress) * .55f), start, end, 1.5.dp.toPx())
                }
            }
        }
    }
}

@Composable
internal fun MhlChoiceMotion(selected: Boolean, dimmed: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val scale by animateFloatAsState(
        targetValue = if (selected && !settings.reduceMotion) .985f else 1f,
        animationSpec = tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.MICRO, settings), easing = MetahumanMotionTokens.Impact),
        label = "choice-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (dimmed) .48f else 1f,
        animationSpec = tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.MICRO, settings)),
        label = "choice-alpha"
    )
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }, content = content)
}

@Composable
internal fun MhlSaveFeedback(savedKey: Any, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val visible = remember(savedKey) { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(savedKey) {
        visible.value = true
        delay(MetahumanMotionTokens.duration(850, settings).toLong())
        visible.value = false
    }
    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn(tween(MetahumanMotionTokens.duration(160, settings))) + slideInVertically { it / 4 },
        exit = fadeOut(tween(MetahumanMotionTokens.duration(180, settings))) + slideOutVertically { -it / 4 },
        modifier = modifier
    ) {
        Box(
            Modifier.clip(CutCornerShape(7.dp)).background(Color(0xE6111720)).border(1.dp, Color(0x6657C96B), CutCornerShape(7.dp)).size(width = 82.dp, height = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text("SAUVEGARDÉ", color = Color(0xFFBFE8C8), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun MhlStatChangePulse(campaign: Campaign, modifier: Modifier = Modifier) {
    val settings = LocalMetahumanMotion.current.settings
    val signature = listOf(campaign.morality, campaign.prestige, campaign.opinion, campaign.fear, campaign.power, campaign.influence, campaign.health, campaign.familyBond).joinToString(":")
    val p = remember(signature) { Animatable(1f) }
    LaunchedEffect(signature) {
        p.snapTo(0f)
        p.animateTo(1f, tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.FAST, settings)))
    }
    if (!settings.reduceMotion && p.value < 1f) {
        Canvas(modifier.fillMaxSize()) { drawRect(Color(0xFFF0C65A).copy(alpha = (1f - p.value) * .035f)) }
    }
}

internal enum class MhlShakeStrength(val px: Float) { LIGHT(2.5f), MEDIUM(5.0f), HEAVY(8.5f) }

@Composable
internal fun MhlShakeContainer(trigger: Any, strength: MhlShakeStrength, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val x = remember(trigger) { Animatable(0f) }
    val y = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger, settings.reduceMotion) {
        x.snapTo(0f); y.snapTo(0f)
        if (!settings.reduceMotion) {
            val s = strength.px
            x.snapTo(s); y.snapTo(-s * .45f); delay(18)
            x.snapTo(-s * .75f); y.snapTo(s * .35f); delay(18)
            x.snapTo(s * .40f); y.snapTo(-s * .20f); delay(18)
            x.animateTo(0f, tween(42, easing = MetahumanMotionTokens.Exit))
            y.animateTo(0f, tween(42, easing = MetahumanMotionTokens.Exit))
        }
    }
    Box(modifier.graphicsLayer { translationX = x.value; translationY = y.value }, content = content)
}
