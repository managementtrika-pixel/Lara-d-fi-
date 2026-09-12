package com.metahumanlegacy.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

private object MhlVisualStore {
    private const val CELL = 72
    private const val COLUMNS = 5
    private var atlas: Bitmap? = null
    private val crops = mutableMapOf<String, ImageBitmap>()
    private val indices = mapOf(
        "brand_hero" to 0, "brand_villain" to 1, "route_care" to 2, "route_order" to 3, "route_truth" to 4, "route_ascend" to 5,
        "morality_hero" to 6, "morality_neutral" to 7, "morality_villain" to 8, "rank_bronze" to 9, "rank_gold" to 10, "rank_legend" to 11,
        "scope_street" to 12, "scope_district" to 13, "scope_city" to 14, "scope_region" to 15, "scope_country" to 16, "scope_world" to 17,
        "danger_low" to 18, "danger_high" to 19, "danger_extreme" to 20, "power_strength" to 21, "power_speed" to 22, "power_senses" to 23,
        "power_fire" to 24, "power_ice" to 25, "power_water" to 26, "power_wind" to 27, "power_energy" to 28, "power_cosmic" to 29,
        "power_forcefield" to 30, "power_time" to 31, "origin_psychic" to 32, "origin_mystic" to 33, "origin_unknown" to 34,
        "relation_family" to 35, "relation_rival" to 36, "relation_media" to 37, "public_fear" to 38, "prestige_hero" to 39,
        "alt_01" to 40, "alt_02" to 41, "alt_03" to 42, "alt_04" to 43, "alt_05" to 44, "alt_06" to 45, "alt_07" to 46,
        "alt_08" to 47, "alt_09" to 48, "alt_10" to 49
    )

    fun image(context: Context, key: String): ImageBitmap? = synchronized(this) {
        crops[key]?.let { return@synchronized it }
        val index = indices[key] ?: return@synchronized null
        val source = atlas ?: runCatching {
            val encoded = buildString(194_000) {
                for (i in 1..10) {
                    val path = "metahuman/comic_atlas_${i.toString().padStart(2, '0')}.b64"
                    append(context.assets.open(path).bufferedReader().use { it.readText() }.trim())
                }
            }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()?.also { atlas = it } ?: return@synchronized null
        val x = (index % COLUMNS) * CELL; val y = (index / COLUMNS) * CELL
        if (x + CELL > source.width || y + CELL > source.height) return@synchronized null
        Bitmap.createBitmap(source, x, y, CELL, CELL).asImageBitmap().also { crops[key] = it }
    }
}

@Composable internal fun MhlAsset(key: String, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 68.dp) {
    val context = LocalContext.current
    val image = remember(key) { MhlVisualStore.image(context, key) }
    val m = modifier.size(size).semantics { this.contentDescription = contentDescription }
    if (image != null) Image(image, contentDescription, m, contentScale = ContentScale.Fit)
    else Canvas(m) {
        drawCircle(MetahumanColors.PanelRaised); drawCircle(MetahumanColors.Gold, style = Stroke(width = 3f))
        drawLine(MetahumanColors.Gold, center.copy(x = center.x * .55f), center.copy(x = center.x * 1.45f), 3f)
        drawLine(MetahumanColors.Gold, center.copy(y = center.y * .55f), center.copy(y = center.y * 1.45f), 3f)
    }
}

@Composable internal fun MhlScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(MetahumanColors.Coal).padding(WindowInsets.safeDrawing.asPaddingValues())) { MhlHalftoneBackdrop(); content() }
}

@Composable private fun MhlHalftoneBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val gap = 22.dp.toPx(); val radius = 1.15.dp.toPx(); var y = gap / 2f; var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) gap / 2f else gap
            while (x < size.width) { drawCircle(Color(0x152E83FF), radius, androidx.compose.ui.geometry.Offset(x, y)); x += gap }
            y += gap; row++
        }
        drawRect(Color(0x161855B6), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * .78f), size = androidx.compose.ui.geometry.Size(size.width, size.height * .22f))
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
