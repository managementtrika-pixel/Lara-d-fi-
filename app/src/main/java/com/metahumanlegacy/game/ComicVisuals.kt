package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object MetahumanColors {
    val Black = Color(0xFF05070A); val Coal = Color(0xFF080B10); val Ink = Color(0xFF020304)
    val Panel = Color(0xFF111720); val PanelRaised = Color(0xFF18212C); val NightBlue = Color(0xFF0C1B33)
    val ElectricBlue = Color(0xFF2E83FF); val DeepBlue = Color(0xFF1855B6); val Gold = Color(0xFFF0C65A)
    val WarmGold = Color(0xFFFFD977); val Ivory = Color(0xFFF5EFE2); val Muted = Color(0xFFAAB4C2)
    val Red = Color(0xFFE5483B); val DeepRed = Color(0xFF7B1E25); val Violet = Color(0xFF9D5CFF); val Green = Color(0xFF57C96B)
}

internal object MetahumanDimensions { val Screen = 16.dp; val Panel = 14.dp; val Gap = 10.dp; val Touch = 52.dp }
internal val MetahumanPanelShape = CutCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomEnd = 12.dp, bottomStart = 2.dp)
internal val MetahumanButtonShape = CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp)

private fun mhlIconAccent(key: String): Color = when {
    key.contains("danger_extreme") || key.contains("villain") -> MetahumanColors.Red
    key.contains("danger_high") || key.contains("fire") -> Color(0xFFFF8A4A)
    key.contains("care") || key.contains("family") || key.contains("green") -> MetahumanColors.Green
    key.contains("truth") || key.contains("psychic") || key.contains("mystic") || key.contains("cosmic") -> MetahumanColors.Violet
    key.contains("gold") || key.contains("legend") || key.contains("prestige") -> MetahumanColors.Gold
    key.contains("ice") || key.contains("water") || key.contains("wind") -> Color(0xFF74D4FF)
    else -> MetahumanColors.ElectricBlue
}

private fun DrawScope.drawMhlPixelIcon(key: String) {
    val accent = mhlIconAccent(key)
    val dark = MetahumanColors.Ink
    val panel = MetahumanColors.PanelRaised
    val unit = (minOf(size.width, size.height) / 12f).coerceAtLeast(1f)
    val ox = (size.width - unit * 12f) / 2f
    val oy = (size.height - unit * 12f) / 2f
    fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, c: Color = accent) {
        drawRect(c, Offset(ox + x * unit, oy + y * unit), Size(w * unit, h * unit))
    }

    drawRect(panel)
    drawRect(dark, Offset(ox + unit, oy + unit), Size(unit * 10f, unit * 10f), style = Stroke(width = unit * .55f))
    p(1, 1, 2, 1, accent.copy(alpha = .45f)); p(9, 10, 2, 1, accent.copy(alpha = .45f))

    when {
        key.startsWith("scope_") -> {
            val level = when {
                key.endsWith("street") -> 1; key.endsWith("district") -> 2; key.endsWith("city") -> 3
                key.endsWith("region") -> 4; key.endsWith("country") -> 5; else -> 6
            }
            repeat(4) { i ->
                val h = (2 + ((i + level) % 5)).coerceAtMost(7)
                p(2 + i * 2, 9 - h, 1, h, if (i < level.coerceAtMost(4)) accent else accent.copy(alpha = .45f))
            }
            p(1, 9, 10, 1, MetahumanColors.Muted.copy(alpha = .65f))
        }
        key.startsWith("danger_") -> {
            p(5, 2, 2, 1); p(4, 3, 4, 2); p(3, 5, 6, 2); p(2, 7, 8, 2)
            p(5, 4, 2, 3, dark); p(5, 8, 2, 1, dark)
        }
        key.startsWith("relation_") || key == "public_fear" -> {
            p(2, 3, 3, 3); p(7, 3, 3, 3)
            p(3, 7, 6, 1, accent.copy(alpha = .75f))
            p(4, 8, 4, 1, accent.copy(alpha = .55f))
            if (key.contains("rival") || key.contains("fear")) { p(5, 6, 2, 3, MetahumanColors.Red) }
        }
        key.startsWith("route_") -> {
            p(5, 2, 2, 2); p(4, 4, 4, 2); p(3, 6, 6, 2); p(5, 8, 2, 2)
            when {
                key.endsWith("care") -> { p(5, 4, 2, 1, MetahumanColors.Ivory); p(5, 6, 2, 1, MetahumanColors.Ivory) }
                key.endsWith("order") -> { p(3, 5, 6, 1, dark); p(3, 7, 6, 1, dark) }
                key.endsWith("truth") -> { p(4, 5, 4, 2, dark); p(5, 5, 2, 2, MetahumanColors.Ivory) }
                else -> p(5, 3, 2, 6, MetahumanColors.Ivory.copy(alpha = .8f))
            }
        }
        key.startsWith("rank_") || key.startsWith("prestige_") -> {
            p(5, 2, 2, 2); p(3, 4, 6, 1); p(4, 5, 4, 4); p(3, 9, 6, 1)
            p(5, 6, 2, 2, MetahumanColors.Ivory.copy(alpha = .8f))
        }
        key.startsWith("morality_") || key.startsWith("brand_") -> {
            p(3, 2, 6, 2); p(2, 4, 8, 5); p(4, 9, 4, 1)
            p(4, 5, 1, 1, dark); p(7, 5, 1, 1, dark)
            if (key.contains("villain")) p(4, 8, 4, 1, dark) else p(4, 7, 4, 1, MetahumanColors.Ivory.copy(alpha = .75f))
        }
        key.startsWith("power_") || key.startsWith("origin_") -> {
            val variant = (key.hashCode().toUInt().toLong() % 5L).toInt()
            when (variant) {
                0 -> { p(6, 2, 2, 3); p(4, 5, 3, 2); p(5, 7, 2, 3); p(7, 5, 2, 2) }
                1 -> { p(5, 2, 2, 2); p(3, 4, 6, 2); p(2, 6, 8, 2); p(4, 8, 4, 2) }
                2 -> { p(2, 5, 8, 2); p(5, 2, 2, 8); p(3, 3, 1, 1); p(8, 8, 1, 1) }
                3 -> { p(3, 3, 6, 6); p(4, 4, 4, 4, panel); p(5, 5, 2, 2) }
                else -> { p(5, 2, 2, 2); p(3, 4, 6, 1); p(2, 5, 8, 2); p(3, 7, 6, 1); p(5, 8, 2, 2) }
            }
        }
        else -> {
            val bits = key.hashCode().toUInt().toLong()
            repeat(5) { row ->
                repeat(5) { col ->
                    val on = ((bits ushr (row * 5 + col)) and 1L) == 1L
                    if (on) p(3 + col, 3 + row, 1, 1)
                }
            }
            p(5, 5, 2, 2, MetahumanColors.Ivory.copy(alpha = .45f))
        }
    }
}

@Composable internal fun MhlAsset(key: String, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 68.dp) {
    val m = modifier.size(size).semantics { this.contentDescription = contentDescription }
    Canvas(m) { drawMhlPixelIcon(key) }
}

@Composable internal fun MhlScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(MetahumanColors.Coal).padding(WindowInsets.safeDrawing.asPaddingValues())) { MhlHalftoneBackdrop(); content() }
}

@Composable private fun MhlHalftoneBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val gap = 22.dp.toPx(); val radius = 1.15.dp.toPx(); var y = gap / 2f; var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) gap / 2f else gap
            while (x < size.width) { drawCircle(Color(0x152E83FF), radius, Offset(x, y)); x += gap }
            y += gap; row++
        }
        drawRect(Color(0x161855B6), topLeft = Offset(0f, size.height * .78f), size = Size(size.width, size.height * .22f))
    }
}

@Composable internal fun MhlComicPanel(modifier: Modifier = Modifier, accent: Color = MetahumanColors.ElectricBlue, fill: Color = MetahumanColors.Panel, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(MetahumanPanelShape).background(fill).border(2.dp, MetahumanColors.Ink, MetahumanPanelShape).border(1.dp, accent.copy(alpha = .72f), MetahumanPanelShape).padding(MetahumanDimensions.Panel), content = content)
}

@Composable internal fun MhlSectionTitle(text: String, accent: Color = MetahumanColors.Gold) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.width(5.dp).height(22.dp).background(accent)); Spacer(Modifier.width(8.dp)); Text(text.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.6.sp) }
}

@Composable internal fun MhlPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(modifier.heightIn(min = MetahumanDimensions.Touch).clip(MetahumanButtonShape).background(if (enabled) MetahumanColors.Gold else Color(0xFF4D4F52)).border(2.dp, MetahumanColors.Ink, MetahumanButtonShape).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label.uppercase(), color = MetahumanColors.Black, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
    }
}

@Composable internal fun MhlSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.heightIn(min = MetahumanDimensions.Touch).clip(MetahumanButtonShape).background(MetahumanColors.PanelRaised).border(2.dp, MetahumanColors.Gold.copy(alpha = .78f), MetahumanButtonShape).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label.uppercase(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Bold)
    }
}

@Composable internal fun MhlStatBadge(icon: String, label: String, value: String, percent: Int? = null, accent: Color = MetahumanColors.ElectricBlue, modifier: Modifier = Modifier) {
    MhlComicPanel(modifier, accent, MetahumanColors.PanelRaised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MhlAsset(icon, label, size = 46.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), color = MetahumanColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                Text(value, color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (percent != null) { Spacer(Modifier.height(5.dp)); LinearProgressIndicator(progress = { percent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = accent, trackColor = Color(0xFF29313A)) }
            }
        }
    }
}

internal fun moralIcon(c: Campaign) = when { c.morality >= 35 -> "morality_hero"; c.morality <= -35 -> "morality_villain"; else -> "morality_neutral" }
internal fun prestigeIcon(c: Campaign) = when { c.prestige >= 75 -> "rank_legend"; c.prestige >= 35 -> "rank_gold"; else -> "rank_bronze" }
internal fun scopeIcon(scope: Scope) = when (scope) { Scope.STREET -> "scope_street"; Scope.DISTRICT -> "scope_district"; Scope.CITY -> "scope_city"; Scope.REGION -> "scope_region"; Scope.COUNTRY -> "scope_country"; Scope.WORLD -> "scope_world" }
internal fun dangerIcon(risk: Int) = when { risk >= 7 -> "danger_extreme"; risk >= 4 -> "danger_high"; else -> "danger_low" }
internal fun powerIcon(powerFamily: String): String {
    val p = powerFamily.lowercase()
    return when {
        listOf("feu", "flamme", "therm", "chaleur").any(p::contains) -> "power_fire"
        listOf("glace", "froid", "cry", "ice").any(p::contains) -> "power_ice"
        listOf("eau", "hydro", "marée", "water").any(p::contains) -> "power_water"
        listOf("air", "vent", "aéro", "wind").any(p::contains) -> "power_wind"
        listOf("vitesse", "speed", "cinétique").any(p::contains) -> "power_speed"
        listOf("force", "physique", "muscl", "densité").any(p::contains) -> "power_strength"
        listOf("énergie", "elect", "foudre", "plasma", "lightning").any(p::contains) -> "power_energy"
        listOf("temps", "chrono", "time").any(p::contains) -> "power_time"
        listOf("barrière", "bouclier", "champ", "force field").any(p::contains) -> "power_forcefield"
        listOf("mental", "psy", "télépath", "esprit", "mind").any(p::contains) -> "origin_psychic"
        listOf("myst", "occult", "mag", "rituel").any(p::contains) -> "origin_mystic"
        listOf("cosm", "grav", "espace", "dimension").any(p::contains) -> "power_cosmic"
        else -> "origin_unknown"
    }
}
internal fun civilProgressIcon(turn: Int) = when { turn <= 2 -> "scope_street"; turn <= 5 -> "scope_district"; turn <= 8 -> "alt_08"; else -> "origin_unknown" }
