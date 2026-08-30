package com.metahumanlegacy.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

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
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = .13f))
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
        LinearProgressIndicator(
            progress = { safe },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = accent,
            trackColor = Color(0xFF26303C)
        )
    }
}

private fun skinColor(name: String): Color = when (name) {
    "Très clair" -> Color(0xFFF3D2BD)
    "Clair" -> Color(0xFFE8BFA4)
    "Moyen" -> Color(0xFFC88E67)
    "Mat" -> Color(0xFFAA714F)
    "Foncé" -> Color(0xFF794A34)
    "Très foncé" -> Color(0xFF4C2B22)
    else -> Color(0xFFC88E67)
}

private fun hairColor(name: String): Color = when (name) {
    "Noir" -> Color(0xFF111318)
    "Brun" -> Color(0xFF3B261E)
    "Châtain" -> Color(0xFF6A4630)
    "Blond" -> Color(0xFFD4B06A)
    "Roux" -> Color(0xFFA9502B)
    "Gris" -> Color(0xFF9699A0)
    "Blanc" -> Color(0xFFE8E5DE)
    else -> Color(0xFF3B261E)
}

private fun outfitColor(style: String): Color = when {
    style.contains("Sport", true) -> Color(0xFF27496E)
    style.contains("Class", true) -> Color(0xFF283038)
    style.contains("Créat", true) -> Color(0xFF70435B)
    style.contains("Profession", true) -> Color(0xFF38475D)
    style.contains("Vintage", true) -> Color(0xFF745A3E)
    else -> Color(0xFF26354A)
}

private fun paletteColors(palette: String, power: String): Pair<Color, Color> = when (palette) {
    "Bleu / or" -> Color(0xFF226BD7) to Color(0xFFF1C75B)
    "Noir / argent" -> Color(0xFF171B22) to Color(0xFFB7C0CA)
    "Rouge / anthracite" -> Color(0xFFB53037) to Color(0xFF252B33)
    "Blanc / cobalt" -> Color(0xFFE9EDF4) to Color(0xFF245ED7)
    "Violet / noir" -> Color(0xFF673CC1) to Color(0xFF101216)
    "Vert / cuivre" -> Color(0xFF267257) to Color(0xFFC47B45)
    "Ivoire / or" -> Color(0xFFF0E8D8) to Color(0xFFD6A938)
    else -> powerVisualProfile(power).accent to Color(0xFF171C25)
}

@Composable
internal fun UltimatePortrait(
    c: Campaign,
    state: UltimateState,
    modifier: Modifier = Modifier,
    heroMode: Boolean = c.powerRevealed,
    showAura: Boolean = c.powerRevealed,
    contentDescription: String = "Portrait évolutif du personnage"
) {
    val profile = powerVisualProfile(c.powerFamily)
    val motion = LocalMetahumanMotion.current.settings
    val inf = rememberInfiniteTransition(label = "ultimate-portrait")
    val pulse by inf.animateFloat(
        initialValue = .88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(MetahumanMotionTokens.duration(1800, motion), easing = MetahumanMotionTokens.Standard),
            RepeatMode.Reverse
        ),
        label = "portrait-aura"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF111C2A), Color(0xFF070A0F))))
        drawComicRays(if (heroMode) profile.accent else UltimateBlue, .09f)
        if (showAura) {
            drawCircle(profile.accent.copy(alpha = if (motion.reduceMotion) .15f else .10f + .11f * pulse), radius = w * .42f, center = Offset(w * .5f, h * .42f))
            drawCircle(profile.secondary.copy(alpha = .18f), radius = w * .32f, center = Offset(w * .5f, h * .42f), style = Stroke(w * .014f))
        }

        val skin = skinColor(state.skinTone)
        val hair = if (c.age >= 65 && state.hairColor !in listOf("Blanc", "Gris")) Color(0xFFB5B4B0) else hairColor(state.hairColor)
        val clothes = if (heroMode) paletteColors(state.costumePalette, c.powerFamily) else outfitColor(state.civilianStyle) to Color(0xFF10151C)
        val shoulderY = h * .70f
        val bodyWidth = when (state.bodyBuild) { "Fin" -> .34f; "Massif" -> .52f; "Robuste" -> .48f; else -> .42f }
        val torso = Path().apply {
            moveTo(w * (.5f - bodyWidth / 2), h)
            lineTo(w * (.5f - bodyWidth / 2.15f), shoulderY)
            quadraticBezierTo(w * .5f, h * .62f, w * (.5f + bodyWidth / 2.15f), shoulderY)
            lineTo(w * (.5f + bodyWidth / 2), h)
            close()
        }
        drawPath(torso, clothes.first)
        drawPath(torso, clothes.second.copy(alpha = .8f), style = Stroke(w * .018f))

        // Neck
        drawRoundRect(skin, topLeft = Offset(w * .43f, h * .52f), size = Size(w * .14f, h * .17f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .035f))

        val faceW = when (state.faceShape) { "Fin" -> .27f; "Rond" -> .34f; "Carré" -> .33f; else -> .31f }
        val faceH = when (state.faceShape) { "Rond" -> .26f; "Anguleux" -> .32f; else -> .30f }
        drawOval(skin, topLeft = Offset(w * (.5f - faceW / 2), h * .25f), size = Size(w * faceW, h * faceH))
        // Jaw / age lines
        if (state.faceShape == "Carré" || state.faceShape == "Anguleux") {
            drawLine(skin.copy(alpha = .7f), Offset(w * .37f, h * .45f), Offset(w * .42f, h * .55f), w * .028f)
            drawLine(skin.copy(alpha = .7f), Offset(w * .63f, h * .45f), Offset(w * .58f, h * .55f), w * .028f)
        }
        if (c.age >= 45) {
            drawLine(Color.Black.copy(alpha = .18f), Offset(w * .39f, h * .43f), Offset(w * .45f, h * .44f), w * .006f)
            drawLine(Color.Black.copy(alpha = .18f), Offset(w * .55f, h * .44f), Offset(w * .61f, h * .43f), w * .006f)
        }

        // Eyes
        val eye = if (heroMode && c.power >= 55) profile.secondary else Color(0xFFE8EDF5)
        drawOval(eye, Offset(w * .405f, h * .395f), Size(w * .06f, h * .025f))
        drawOval(eye, Offset(w * .535f, h * .395f), Size(w * .06f, h * .025f))
        drawCircle(Color(0xFF1C2733), w * .012f, Offset(w * .435f, h * .407f))
        drawCircle(Color(0xFF1C2733), w * .012f, Offset(w * .565f, h * .407f))

        drawHair(state.hair, hair, w, h)
        if (state.facialHair != "Aucune") drawFacialHair(state.facialHair, hair, w, h)

        if (heroMode) drawMask(state.maskStyle, clothes.second, w, h)
        if (heroMode && state.emblem != "Aucun") drawEmblem(state.emblem, clothes.second, Offset(w * .5f, h * .79f), w * .10f)
        if (state.injuries.isNotEmpty()) {
            drawLine(UltimateRed.copy(alpha = .65f), Offset(w * .56f, h * .35f), Offset(w * .60f, h * .47f), w * .007f)
        }
        drawRect(UltimateGold.copy(alpha = .7f), topLeft = Offset(0f, h - w * .016f), size = Size(w, w * .016f))
    }
}

private fun DrawScope.drawHair(style: String, color: Color, w: Float, h: Float) {
    when (style) {
        "Rasé" -> drawArc(color, 190f, 160f, true, Offset(w * .355f, h * .235f), Size(w * .29f, h * .18f))
        "Long" -> {
            drawOval(color, Offset(w * .34f, h * .21f), Size(w * .32f, h * .28f))
            drawRect(color, Offset(w * .34f, h * .32f), Size(w * .055f, h * .28f))
            drawRect(color, Offset(w * .605f, h * .32f), Size(w * .055f, h * .28f))
        }
        "Tresses" -> {
            drawArc(color, 188f, 165f, true, Offset(w * .345f, h * .215f), Size(w * .31f, h * .20f))
            repeat(5) { i -> drawLine(color, Offset(w * (.39f + i * .055f), h * .25f), Offset(w * (.37f + i * .065f), h * .59f), w * .018f) }
        }
        "Boucles" -> {
            repeat(9) { i ->
                val x = w * (.365f + (i % 5) * .065f)
                val y = h * (.245f + (i / 5) * .045f)
                drawCircle(color, w * .045f, Offset(x, y))
            }
        }
        "Undercut" -> {
            drawArc(color, 190f, 160f, true, Offset(w * .36f, h * .22f), Size(w * .28f, h * .18f))
            drawRect(color, Offset(w * .43f, h * .205f), Size(w * .20f, h * .055f))
        }
        else -> {
            drawArc(color, 188f, 165f, true, Offset(w * .355f, h * .215f), Size(w * .29f, h * .19f))
            if (style.contains("Dégradé")) drawRect(color.copy(alpha = .55f), Offset(w * .365f, h * .31f), Size(w * .035f, h * .08f))
        }
    }
}

private fun DrawScope.drawFacialHair(style: String, color: Color, w: Float, h: Float) {
    when (style) {
        "Moustache" -> drawLine(color, Offset(w * .44f, h * .485f), Offset(w * .56f, h * .485f), w * .018f)
        "Bouc" -> {
            drawLine(color, Offset(w * .45f, h * .486f), Offset(w * .55f, h * .486f), w * .012f)
            drawOval(color, Offset(w * .465f, h * .505f), Size(w * .07f, h * .07f))
        }
        else -> drawArc(color.copy(alpha = .9f), 15f, 150f, false, Offset(w * .37f, h * .405f), Size(w * .26f, h * .17f), style = Stroke(w * if (style.contains("pleine")) .035f else .018f))
    }
}

private fun DrawScope.drawMask(style: String, color: Color, w: Float, h: Float) {
    if (style == "Aucun") return
    when (style) {
        "Masque intégral", "Casque" -> drawArc(color.copy(alpha = .75f), 180f, 180f, true, Offset(w * .355f, h * .25f), Size(w * .29f, h * .28f))
        "Capuche" -> drawArc(color.copy(alpha = .62f), 190f, 160f, false, Offset(w * .32f, h * .18f), Size(w * .36f, h * .39f), style = Stroke(w * .038f))
        "Visière" -> drawRoundRect(color.copy(alpha = .78f), Offset(w * .39f, h * .375f), Size(w * .22f, h * .06f), androidx.compose.ui.geometry.CornerRadius(w * .02f))
        else -> {
            val p = Path().apply {
                moveTo(w * .38f, h * .36f); lineTo(w * .48f, h * .385f); lineTo(w * .50f, h * .44f)
                lineTo(w * .52f, h * .385f); lineTo(w * .62f, h * .36f); lineTo(w * .60f, h * .45f)
                lineTo(w * .5f, h * .47f); lineTo(w * .40f, h * .45f); close()
            }
            drawPath(p, color.copy(alpha = .82f))
        }
    }
}

private fun DrawScope.drawEmblem(emblem: String, color: Color, center: Offset, radius: Float) {
    when {
        emblem.contains("Étoile") || emblem.contains("Comète") -> {
            val p = Path()
            repeat(10) { i ->
                val angle = -PI / 2 + i * PI / 5
                val rr = if (i % 2 == 0) radius else radius * .42f
                val pt = Offset(center.x + cos(angle).toFloat() * rr, center.y + sin(angle).toFloat() * rr)
                if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
            }
            p.close(); drawPath(p, color)
        }
        emblem.contains("Anneau") -> drawCircle(color, radius, center, style = Stroke(radius * .22f))
        emblem.contains("Bouclier") -> {
            val p = Path().apply { moveTo(center.x, center.y - radius); lineTo(center.x + radius * .75f, center.y - radius * .4f); lineTo(center.x + radius * .55f, center.y + radius * .7f); lineTo(center.x, center.y + radius); lineTo(center.x - radius * .55f, center.y + radius * .7f); lineTo(center.x - radius * .75f, center.y - radius * .4f); close() }
            drawPath(p, color)
        }
        else -> {
            drawCircle(color.copy(alpha = .2f), radius, center)
            drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), radius * .18f)
            drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), radius * .18f)
        }
    }
}

private fun DrawScope.drawComicRays(color: Color, alpha: Float) {
    val maxR = size.maxDimension
    repeat(18) { i ->
        val a = i * (2 * PI / 18)
        drawLine(color.copy(alpha = alpha), center, Offset(center.x + cos(a).toFloat() * maxR, center.y + sin(a).toFloat() * maxR), size.minDimension * .012f)
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
                if ((seed + row) % 3 != 0) {
          val windowTop = Offset(x + bw * .18f, y + bh * .12f + row.toFloat() * size.height * .05f)
          val windowSize = Size(bw * .12f, size.height * .012f)
          drawRect(color = UltimateGold.copy(alpha = .35f), topLeft = windowTop, size = windowSize)
      }
            }
        }
        // Street and river/road perspective
        val road = Path().apply {
            moveTo(size.width * .18f, size.height)
            lineTo(size.width * .45f, horizon)
            lineTo(size.width * .57f, horizon)
            lineTo(size.width * .92f, size.height)
            close()
        }
        drawPath(road, Color(0xFF11151B))
        drawLine(UltimateBlue.copy(alpha = .35f), Offset(size.width * .50f, horizon), Offset(size.width * .56f, size.height), size.width * .008f)

        if (state.cityCondition < 55) {
            repeat(((55 - state.cityCondition) / 6).coerceAtLeast(1)) { i ->
                val x = size.width * (.08f + (i * .17f) % .82f)
                val y = horizon * (.50f + (i % 3) * .11f)
                drawCircle(UltimateRed.copy(alpha = .20f), size.minDimension * (.04f + i * .004f), Offset(x, y))
            }
        }
        if (state.cityTech >= 55) {
            repeat(4) { i -> drawLine(profile.accent.copy(alpha = .34f), Offset(size.width * (.15f + i * .20f), horizon * .92f), Offset(size.width * (.22f + i * .20f), horizon * .25f), size.width * .004f) }
        }
        if (state.climate.contains("Pluv", true)) repeat(28) { i -> {
            val x = size.width * ((i * 37 % 101) / 100f); val y = size.height * ((i * 61 % 97) / 100f)
            drawLine(Color.White.copy(alpha = .18f), Offset(x, y), Offset(x - size.width * .018f, y + size.height * .05f), size.width * .002f)
        } }
        drawRect(UltimateGold.copy(alpha = .55f), Offset(0f, size.height - size.width * .01f), Size(size.width, size.width * .01f))
    }
}

@Composable
internal fun UltimateHeroBanner(c: Campaign, state: UltimateState, modifier: Modifier = Modifier) {
    val profile = powerVisualProfile(c.powerFamily)
    Box(
        modifier
            .height(118.dp)
            .clip(CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp))
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
        Modifier.fillMaxWidth()
            .clip(CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .background(if (enabled) Color(0xE8141D29) else Color(0x9910161E))
            .border(1.dp, if (enabled) accent.copy(alpha = .6f) else Color(0xFF343B45), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(11.dp)
    ) {
        Text(title, color = if (enabled) UltimateIvory else UltimateMuted, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(subtitle, color = UltimateMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
