package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

internal val UltimateGold = Color(0xFFF1C75B)
internal val UltimateBlue = Color(0xFF3A8DFF)
internal val UltimateRed = Color(0xFFFF554D)
internal val UltimateGreen = Color(0xFF57D884)
internal val UltimateViolet = Color(0xFFA57BFF)
internal val UltimateInk = Color(0xFF070A0F)
internal val UltimatePanelColor = Color(0xED111925)
internal val UltimateMuted = Color(0xFFA8B3C2)
internal val UltimateIvory = Color(0xFFF5F1E8)

@Composable
internal fun UltimatePanel(
    modifier: Modifier = Modifier,
    accent: Color = UltimateBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(CutCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF2161F2C), Color(0xF20B1018))))
            .border(1.dp, accent.copy(alpha = .72f), CutCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
internal fun UltimateSectionHeader(kicker: String, title: String, subtitle: String? = null, accent: Color = UltimateGold) {
    Text(kicker.uppercase(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    Text(title.uppercase(), color = UltimateIvory, fontSize = 23.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black)
    if (!subtitle.isNullOrBlank()) Text(subtitle, color = UltimateMuted, fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable
internal fun UltimatePill(text: String, accent: Color = UltimateBlue, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(accent.copy(alpha = .13f))
            .border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
    }
}

@Composable
internal fun UltimateMeter(label: String, value: Int, accent: Color, modifier: Modifier = Modifier, rangeMin: Int = 0, rangeMax: Int = 100) {
    val safe = ((value - rangeMin).toFloat() / (rangeMax - rangeMin).coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(value.toString(), color = UltimateIvory, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(progress = { safe }, modifier = Modifier.fillMaxWidth().height(4.dp), color = accent, trackColor = Color(0xFF26303C))
    }
}

/**
 * Single source of truth for the player character's appearance.
 * The creator, story, profile, alias screen and Hall all render the same PixelAvatar state.
 * Power presentation is layered around the avatar instead of replacing it with a second portrait renderer.
 */
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
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF111C2A), Color(0xFF070A0F))))
            .border(1.dp, (if (heroMode) profile.accent else UltimateBlue).copy(alpha = .55f), CutCornerShape(8.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val step = (size.minDimension / 14f).coerceAtLeast(8f)
            var x = 0f
            while (x < size.width) {
                drawLine(Color.White.copy(alpha = .025f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color.White.copy(alpha = .025f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
            if (showAura) {
                drawCircle(profile.accent.copy(alpha = .12f), size.minDimension * .42f, center)
                drawCircle(profile.secondary.copy(alpha = .10f), size.minDimension * .31f, center)
            }
        }
        PixelAvatar(
            state = state,
            modifier = Modifier.fillMaxSize().padding(if (heroMode) 4.dp else 2.dp),
            age = c.age,
            temperament = c.temperament,
            heroMode = heroMode,
            powerFamily = c.powerFamily
        )
        if (heroMode) {
            Text(
                c.alias.ifBlank { c.name }.uppercase(),
                color = UltimateIvory,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .58f)).padding(horizontal = 5.dp, vertical = 2.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun UltimateCityArtwork(c: Campaign, state: UltimateState, modifier: Modifier = Modifier) {
    val profile = powerVisualProfile(c.powerFamily)
    Canvas(modifier) {
        val horizon = size.height * .66f
        val skyTop = when {
            state.climate.contains("Brouillard") -> Color(0xFF607184)
            state.climate.contains("Orage") -> Color(0xFF171C35)
            state.climate.contains("Chaud") -> Color(0xFF5D2D2B)
            state.climate.contains("Pollué") -> Color(0xFF414840)
            else -> Color(0xFF0B1E37)
        }
        drawRect(Brush.verticalGradient(listOf(skyTop, Color(0xFF16131A), Color(0xFF05070A))))
        val sun = if (state.climate.contains("Pluv", true) || state.climate.contains("Orage", true)) Color(0xFF9AB6C8) else UltimateGold
        drawCircle(sun.copy(alpha = .35f), size.minDimension * .09f, Offset(size.width * .78f, size.height * .20f))
        val buildings = when {
            state.cityArchetype.contains("vertical", true) -> 18
            state.cityArchetype.contains("Mégalopole", true) -> 22
            state.cityArchetype.contains("ancienne", true) -> 14
            else -> 16
        }
        repeat(buildings) { i ->
            val x = size.width * i / buildings
            val bw = size.width / buildings * (1.04f + (i % 3) * .10f)
            val seed = ((c.seed ushr (i % 15)) + i * 31).toInt().absoluteValue
            val bh = size.height * (.18f + (seed % 42) / 100f)
            val y = horizon - bh
            val buildingColor = when {
                state.architecture.contains("Brique", true) -> Color(0xFF382525)
                state.architecture.contains("Brut", true) -> Color(0xFF292D32)
                state.architecture.contains("Futur", true) -> Color(0xFF192B3C)
                else -> Color(0xFF202731)
            }
            drawRect(buildingColor, Offset(x, y), Size(bw, bh))
            drawRect(Color.Black.copy(alpha = .4f), Offset(x + bw * .78f, y), Size(bw * .22f, bh))
            val rows = (bh / (size.height * .05f)).toInt().coerceAtLeast(2)
            repeat(rows) { row ->
                if ((seed + row) % 3 != 0) drawRect(UltimateGold.copy(alpha = .35f), Offset(x + bw * .18f, y + bh * .12f + row * size.height * .05f), Size(bw * .12f, size.height * .012f))
            }
        }
        val road = Path().apply {
            moveTo(size.width * .18f, size.height); lineTo(size.width * .45f, horizon); lineTo(size.width * .57f, horizon); lineTo(size.width * .92f, size.height); close()
        }
        drawPath(road, Color(0xFF11151B))
        drawLine(UltimateBlue.copy(alpha = .35f), Offset(size.width * .50f, horizon), Offset(size.width * .56f, size.height), size.width * .008f)
        if (state.cityCondition < 55) repeat(((55 - state.cityCondition) / 6).coerceAtLeast(1)) { i ->
            drawCircle(UltimateRed.copy(alpha = .20f), size.minDimension * (.04f + i * .004f), Offset(size.width * (.08f + (i * .17f) % .82f), horizon * (.50f + (i % 3) * .11f)))
        }
        if (state.cityTech >= 55) repeat(4) { i -> drawLine(profile.accent.copy(alpha = .34f), Offset(size.width * (.15f + i * .20f), horizon * .92f), Offset(size.width * (.22f + i * .20f), horizon * .25f), size.width * .004f) }
        if (state.climate.contains("Pluv", true)) repeat(28) { i ->
            val x = size.width * ((i * 37 % 101) / 100f); val y = size.height * ((i * 61 % 97) / 100f)
            drawLine(Color.White.copy(alpha = .18f), Offset(x, y), Offset(x - size.width * .018f, y + size.height * .05f), size.width * .002f)
        }
        drawRect(UltimateGold.copy(alpha = .55f), Offset(0f, size.height - size.width * .01f), Size(size.width, size.width * .01f))
    }
}

@Composable
internal fun UltimateHeroBanner(c: Campaign, state: UltimateState, modifier: Modifier = Modifier) {
    val profile = powerVisualProfile(c.powerFamily)
    Box(
        modifier.height(118.dp).clip(CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF101A28), profile.accent.copy(alpha = .22f), Color(0xFF090C12))))
            .border(1.dp, profile.accent.copy(alpha = .6f), CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp))
    ) {
        MhlBoardTexture(if (c.powerRevealed) MotionBoard.AURA else MotionBoard.PANEL_TRANSITION, Modifier.matchParentSize(), profile.accent, .10f)
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            UltimatePortrait(c, state, Modifier.width(82.dp).fillMaxHeight().clip(CutCornerShape(10.dp)), heroMode = c.powerRevealed)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(c.alias.ifBlank { c.name }.uppercase(), color = UltimateIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("${c.age} ANS · ${if (c.powerRevealed) state.heroPresentation else state.civilianStyle}", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    UltimatePill(if (c.powerRevealed) c.scope.label else "Civil", profile.accent)
                    UltimatePill(state.ageAppearance(c), UltimateMuted)
                }
                if (c.powerRevealed) {
                    Spacer(Modifier.height(5.dp))
                    Text("${state.costumePalette} · ${state.maskStyle}", color = UltimateMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
internal fun UltimateActionTile(title: String, subtitle: String, accent: Color = UltimateBlue, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .background(if (enabled) Color(0xE8141D29) else Color(0x9910161E))
            .border(1.dp, if (enabled) accent.copy(alpha = .6f) else Color(0xFF343B45), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(11.dp)
    ) {
        Text(title, color = if (enabled) UltimateIvory else UltimateMuted, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(subtitle, color = UltimateMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
