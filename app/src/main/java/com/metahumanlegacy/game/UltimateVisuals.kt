package com.metahumanlegacy.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

internal val UltimateGold = Color(0xFFFFC857)
internal val UltimateBlue = Color(0xFF67B7FF)
internal val UltimateRed = Color(0xFFFF625C)
internal val UltimateGreen = Color(0xFF66E29A)
internal val UltimateViolet = Color(0xFFB88CFF)
internal val UltimateInk = Color(0xFF03060B)
internal val UltimatePanelColor = Color(0xD90B111A)
internal val UltimateMuted = Color(0xFF9EACBC)
internal val UltimateIvory = Color(0xFFF4F1EA)

private val CinematicPanelShape = RoundedCornerShape(18.dp)

@Composable
internal fun UltimatePanel(
    modifier: Modifier = Modifier,
    accent: Color = UltimateBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .clip(CinematicPanelShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE6111924), Color(0xE9070B11))
                )
            )
            .border(1.dp, Color.White.copy(alpha = .075f), CinematicPanelShape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(accent.copy(alpha = .72f), Offset(0f, 0f), Size(size.width * .012f, size.height))
            drawCircle(accent.copy(alpha = .055f), size.minDimension * .72f, Offset(size.width * .92f, size.height * .08f))
            drawLine(Color.White.copy(alpha = .035f), Offset(size.width * .06f, size.height - 1f), Offset(size.width * .94f, size.height - 1f), 1f)
        }
        Column(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), content = content)
    }
}

@Composable
internal fun UltimateSectionHeader(kicker: String, title: String, subtitle: String? = null, accent: Color = UltimateGold) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(24.dp).height(2.dp).background(accent))
        Spacer(Modifier.width(7.dp))
        Text(kicker.uppercase(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
    }
    Spacer(Modifier.height(5.dp))
    Text(title.uppercase(), color = UltimateIvory, fontSize = 25.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, letterSpacing = .2.sp)
    if (!subtitle.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = UltimateMuted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
internal fun UltimatePill(text: String, accent: Color = UltimateBlue, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.Black.copy(alpha = .26f))
            .border(1.dp, accent.copy(alpha = .36f), RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), color = accent.copy(alpha = .94f), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
    }
}

@Composable
internal fun UltimateMeter(label: String, value: Int, accent: Color, modifier: Modifier = Modifier, rangeMin: Int = 0, rangeMax: Int = 100) {
    val safe = ((value - rangeMin).toFloat() / (rangeMax - rangeMin).coerceAtLeast(1)).coerceIn(0f, 1f)
    val settings = LocalMetahumanMotion.current.settings
    val animated by animateFloatAsState(
        targetValue = safe,
        animationSpec = tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.NORMAL, settings), easing = MetahumanMotionTokens.Standard),
        label = "meter-$label"
    )
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp)
            Text(value.toString(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = accent,
            trackColor = Color.White.copy(alpha = .08f)
        )
    }
}

/** Same persistent Pixel DNA is used in creator, story, profile, alias and legacy screens. */
@Composable
internal fun UltimatePortrait(
    c: Campaign,
    state: UltimateState,
    modifier: Modifier = Modifier,
    heroMode: Boolean = c.powerRevealed,
    showAura: Boolean = c.powerRevealed,
    contentDescription: String = "Avatar pixel persistant du personnage"
) {
    val profile = powerVisualProfile(c.powerFamily)
    val accent = if (heroMode) profile.accent else UltimateBlue
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF16283C), Color(0xFF080D14), Color(0xFF040609))))
            .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(22.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val horizon = size.height * .72f
            drawCircle(accent.copy(alpha = if (showAura) .10f else .035f), size.minDimension * .48f, Offset(size.width * .5f, size.height * .48f))
            if (showAura) {
                drawCircle(profile.secondary.copy(alpha = .07f), size.minDimension * .35f, Offset(size.width * .5f, size.height * .48f))
            }
            repeat(7) { i ->
                val x = size.width * i / 6f
                val bh = size.height * (.05f + (i % 4) * .025f)
                drawRect(Color(0xFF0B141F).copy(alpha = .9f), Offset(x - size.width * .06f, horizon - bh), Size(size.width * .13f, bh + size.height - horizon))
            }
            drawRect(Color.Black.copy(alpha = .32f), Offset(0f, size.height * .90f), Size(size.width, size.height * .10f))
        }
        PixelAvatar(
            state = state,
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 2.dp),
            age = c.age,
            temperament = c.temperament,
            heroMode = heroMode,
            powerFamily = c.powerFamily
        )
        if (heroMode) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f))))
                    .padding(top = 16.dp, bottom = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(c.alias.ifBlank { c.name }.uppercase(), color = UltimateIvory, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun UltimateCityArtwork(c: Campaign, state: UltimateState, modifier: Modifier = Modifier) {
    val profile = powerVisualProfile(c.powerFamily)
    Canvas(modifier) {
        val horizon = size.height * .68f
        val storm = state.climate.contains("Orage", true) || state.climate.contains("Pluv", true)
        val skyTop = when {
            state.climate.contains("Brouillard", true) -> Color(0xFF4D6172)
            state.climate.contains("Orage", true) -> Color(0xFF11172A)
            state.climate.contains("Chaud", true) -> Color(0xFF57302E)
            state.climate.contains("Pollué", true) -> Color(0xFF343E39)
            else -> Color(0xFF071B31)
        }
        drawRect(Brush.verticalGradient(listOf(skyTop, Color(0xFF171820), Color(0xFF05070A))))
        drawCircle((if (storm) Color(0xFFABC0D0) else UltimateGold).copy(alpha = .28f), size.minDimension * .10f, Offset(size.width * .78f, size.height * .20f))

        val buildings = when {
            state.cityArchetype.contains("vertical", true) -> 20
            state.cityArchetype.contains("Mégalopole", true) -> 24
            state.cityArchetype.contains("ancienne", true) -> 15
            else -> 18
        }
        repeat(buildings) { i ->
            val x = size.width * i / buildings
            val bw = size.width / buildings * 1.12f
            val seed = ((c.seed ushr (i % 15)) + i * 31).toInt().absoluteValue
            val bh = size.height * (.17f + (seed % 44) / 100f)
            val y = horizon - bh
            val building = when {
                state.architecture.contains("Brique", true) -> Color(0xFF342023)
                state.architecture.contains("Brut", true) -> Color(0xFF252A30)
                state.architecture.contains("Futur", true) -> Color(0xFF12283A)
                else -> Color(0xFF172331)
            }
            drawRect(building, Offset(x, y), Size(bw, bh + size.height - horizon))
            drawRect(Color.Black.copy(alpha = .30f), Offset(x + bw * .82f, y), Size(bw * .18f, bh))
            repeat((bh / (size.height * .05f)).toInt().coerceAtLeast(2)) { row ->
                if ((seed + row) % 3 != 0) drawRect(UltimateGold.copy(alpha = .28f), Offset(x + bw * .18f, y + bh * .13f + row * size.height * .05f), Size(bw * .11f, size.height * .008f))
            }
        }

        val road = Path().apply {
            moveTo(size.width * .16f, size.height); lineTo(size.width * .44f, horizon); lineTo(size.width * .57f, horizon); lineTo(size.width * .94f, size.height); close()
        }
        drawPath(road, Color(0xFF080D13))
        drawLine(Color.White.copy(alpha = .08f), Offset(size.width * .50f, horizon), Offset(size.width * .57f, size.height), size.width * .004f)

        if (state.cityCondition < 55) repeat(((55 - state.cityCondition) / 7).coerceAtLeast(1)) { i ->
            drawCircle(UltimateRed.copy(alpha = .15f), size.minDimension * (.04f + i * .003f), Offset(size.width * (.10f + (i * .19f) % .78f), horizon * (.54f + (i % 3) * .10f)))
        }
        if (state.cityTech >= 55) repeat(4) { i ->
            drawLine(profile.accent.copy(alpha = .24f), Offset(size.width * (.15f + i * .20f), horizon * .94f), Offset(size.width * (.22f + i * .20f), horizon * .28f), size.width * .003f)
        }
        if (storm) repeat(32) { i ->
            val x = size.width * ((i * 37 % 101) / 100f)
            val y = size.height * ((i * 61 % 97) / 100f)
            drawLine(Color.White.copy(alpha = .17f), Offset(x, y), Offset(x - size.width * .016f, y + size.height * .045f), size.width * .0016f)
        }
    }
}

@Composable
internal fun UltimateHeroBanner(c: Campaign, state: UltimateState, modifier: Modifier = Modifier) {
    val profile = powerVisualProfile(c.powerFamily)
    Box(
        modifier.height(132.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xE90A111A), Color(0xD70D1722), profile.accent.copy(alpha = .13f))))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(22.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(profile.accent.copy(alpha = .08f), size.height * 1.5f, Offset(size.width * .98f, size.height * .5f))
            drawLine(profile.accent.copy(alpha = .55f), Offset(0f, size.height - 2f), Offset(size.width * .36f, size.height - 2f), 2f)
        }
        Row(Modifier.fillMaxSize().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            UltimatePortrait(c, state, Modifier.width(86.dp).fillMaxHeight(), heroMode = c.powerRevealed)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(c.alias.ifBlank { c.name }.uppercase(), color = UltimateIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = .4.sp)
                Text("${c.age} ANS  ·  ${if (c.powerRevealed) state.heroPresentation else state.civilianStyle}", color = profile.accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    UltimatePill(if (c.powerRevealed) c.scope.label else "Civil", profile.accent)
                    UltimatePill(state.ageAppearance(c), UltimateMuted)
                }
                if (c.powerRevealed) {
                    Spacer(Modifier.height(6.dp))
                    Text("${state.costumePalette}  ·  ${state.maskStyle}", color = UltimateMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
internal fun UltimateActionTile(title: String, subtitle: String, accent: Color = UltimateBlue, enabled: Boolean = true, onClick: () -> Unit) {
    val settings = LocalMetahumanMotion.current.settings
    val haptic = rememberMetahumanHaptic()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !settings.reduceMotion) .985f else 1f,
        animationSpec = tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.MICRO, settings), easing = MetahumanMotionTokens.Impact),
        label = "action-tile-scale"
    )
    val surface by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0x9910161E)
            pressed -> accent.copy(alpha = .16f)
            else -> Color(0xD90A1119)
        },
        animationSpec = tween(MetahumanMotionTokens.duration(MetahumanMotionTokens.MICRO, settings)),
        label = "action-tile-surface"
    )
    Box(
        Modifier.fillMaxWidth().heightIn(min = 62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(1.dp, if (enabled) Color.White.copy(alpha = if (pressed) .15f else .07f) else Color.White.copy(alpha = .035f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) {
                haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                onClick()
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(accent.copy(alpha = if (enabled) .72f else .20f), Offset(0f, 0f), Size(3f, size.height))
            if (pressed) drawCircle(accent.copy(alpha = .08f), size.height * 1.2f, Offset(size.width, size.height * .5f))
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(title, color = if (enabled) UltimateIvory else UltimateMuted, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = UltimateMuted.copy(alpha = if (enabled) 1f else .62f), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
