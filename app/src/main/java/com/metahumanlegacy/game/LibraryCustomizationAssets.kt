package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Curated gameplay presets rendered procedurally in the same pixel language as the live game.
 * Preset data stays stable for saves and career gates; only the old illustrated atlas previews are gone.
 */
internal data class LibraryCityPreset(
    val name: String,
    val atlasIndex: Int,
    val cityArchetype: String,
    val climate: String,
    val architecture: String,
    val cityMood: String
) {
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        cityArchetype = cityArchetype,
        climate = climate,
        architecture = architecture,
        cityMood = cityMood
    )

    fun matches(draft: UltimateCreationDraft): Boolean =
        draft.cityArchetype == cityArchetype && draft.climate == climate &&
            draft.architecture == architecture && draft.cityMood == cityMood
}

internal data class LibraryCostumePreset(
    val name: String,
    val atlasIndex: Int,
    val presentation: String,
    val palette: String,
    val mask: String,
    val emblem: String,
    val minimumEra: Int
) {
    fun apply(state: UltimateState): UltimateState = state.copy(
        heroPresentation = presentation,
        costumePalette = palette,
        maskStyle = mask,
        emblem = emblem
    )

    fun matches(state: UltimateState): Boolean =
        state.heroPresentation == presentation && state.costumePalette == palette &&
            state.maskStyle == mask && state.emblem == emblem
}

internal object LibraryCustomizationCatalog {
    const val CITY_COLUMNS = 4
    const val CITY_ROWS = 3
    const val COSTUME_COLUMNS = 4
    const val COSTUME_ROWS = 2

    // atlasIndex is retained as a stable visual-variant id for old data/tests; no raster atlas is loaded.
    val cityPresets = listOf(
        LibraryCityPreset("Matin résidentiel", 0, "Métropole verticale", "Quatre saisons", "Contemporaine", "Optimiste"),
        LibraryCityPreset("Rue ancienne", 1, "Capitale ancienne", "Quatre saisons", "Mixte historique", "Contrastes sociaux"),
        LibraryCityPreset("Skyline nocturne", 2, "Métropole verticale", "Brouillard côtier", "Contemporaine", "Nocturne"),
        LibraryCityPreset("Aube urbaine", 3, "Métropole verticale", "Quatre saisons", "Art déco", "Optimiste"),
        LibraryCityPreset("Nuit pluvieuse", 4, "Ville côtière", "Pluvieux", "Brique industrielle", "Nocturne"),
        LibraryCityPreset("Soleil technologique", 5, "Mégalopole technologique", "Chaud et sec", "Futur proche", "Ultra-connectée"),
        LibraryCityPreset("Toits sous tension", 6, "Ville en reconstruction", "Orageux", "Brutaliste", "Sous tension"),
        LibraryCityPreset("Refuge numérique", 7, "Mégalopole technologique", "Pollué", "Futur proche", "Ultra-connectée"),
        LibraryCityPreset("Cité historique", 8, "Capitale ancienne", "Quatre saisons", "Néo-classique", "Culture héroïque"),
        LibraryCityPreset("Centre institutionnel", 9, "Ville universitaire", "Quatre saisons", "Néo-classique", "Contrastes sociaux")
    )

    val costumePresets = listOf(
        LibraryCostumePreset("Prototype", 0, "Tactique", "Noir / argent", "Masque minimal", "Éclair", 1),
        LibraryCostumePreset("Première identité", 1, "Sobre", "Bleu / or", "Demi-masque", "Bouclier", 1),
        LibraryCostumePreset("Affirmé", 2, "Flamboyant", "Rouge / anthracite", "Visière", "Comète", 2),
        LibraryCostumePreset("Public", 3, "Institutionnel", "Ivoire / or", "Masque minimal", "Étoile fracturée", 2),
        LibraryCostumePreset("Renégat", 4, "Clandestin", "Noir / argent", "Capuche", "Œil stylisé", 2),
        LibraryCostumePreset("Vétéran", 5, "Intimidant", "Noir / argent", "Masque intégral", "Anneau", 3),
        LibraryCostumePreset("Iconique", 6, "Mystérieux", "Violet / noir", "Casque", "Monogramme", 4),
        LibraryCostumePreset("Legacy", 7, "Sobre", "Blanc / cobalt", "Masque minimal", "Bouclier", 4)
    )
}

@Composable
private fun ProceduralPresetCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tall: Boolean = false,
    preview: DrawScope.() -> Unit,
    onClick: () -> Unit
) {
    val border = when {
        selected -> UltimateGold
        enabled -> Color(0x664F6685)
        else -> Color(0x332E3743)
    }
    Column(
        Modifier
            .width(if (tall) 92.dp else 112.dp)
            .shadow(if (selected) 12.dp else 0.dp, CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (tall) .72f else 1f)
                .background(if (selected) Color(0xFF121D28) else Color(0xFF0B1017), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                .border(if (selected) 2.dp else 1.dp, border, CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
        ) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(if (tall) .72f else 1f)) {
                preview()
                if (!enabled) drawRect(Color.Black.copy(alpha = .62f), Offset.Zero, size)
            }
            if (selected) {
                Text(
                    "✓",
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    color = UltimateGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label.uppercase(),
            color = if (enabled) UltimateIvory else UltimateMuted.copy(alpha = .55f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2
        )
    }
}

private fun DrawScope.drawProceduralCityPreset(preset: LibraryCityPreset) {
    val night = preset.cityMood.contains("Nocturne", true) || preset.cityMood.contains("tension", true)
    val polluted = preset.climate.contains("Pollué", true) || preset.climate.contains("Brouillard", true)
    val sky = when {
        night -> Color(0xFF10192B)
        preset.climate.contains("Chaud", true) -> Color(0xFFD18A55)
        preset.climate.contains("Orage", true) -> Color(0xFF46536A)
        polluted -> Color(0xFF71807E)
        else -> Color(0xFF6FA7C9)
    }
    val ground = if (night) Color(0xFF11161D) else Color(0xFF27313A)
    drawRect(sky)
    if (!night) drawRect(Color(0xFFF2C86B), Offset(size.width * .72f, size.height * .12f), Size(size.width * .10f, size.width * .10f))
    if (polluted) drawRect(Color(0x558B9895), Offset(0f, size.height * .18f), Size(size.width, size.height * .34f))

    val variant = preset.atlasIndex
    val buildingColor = when {
        preset.architecture.contains("Brique", true) -> Color(0xFF774B3E)
        preset.architecture.contains("Brut", true) -> Color(0xFF596169)
        preset.architecture.contains("Futur", true) -> Color(0xFF223E55)
        preset.architecture.contains("Art déco", true) -> Color(0xFF5B5368)
        preset.architecture.contains("classique", true) -> Color(0xFF6B6255)
        else -> Color(0xFF35465A)
    }
    val window = if (night) Color(0xFFEBCB73) else Color(0xFFB9D5E7)
    val widths = listOf(.16f, .21f, .14f, .24f, .18f)
    var x = -size.width * .03f
    repeat(5) { i ->
        val w = size.width * widths[(i + variant) % widths.size]
        val heightFactor = .30f + (((i * 17 + variant * 11) % 42) / 100f)
        val h = size.height * heightFactor
        val y = size.height * .80f - h
        drawRect(buildingColor.copy(alpha = .92f), Offset(x, y), Size(w, h))
        val columns = ((w / (size.width * .055f)).toInt()).coerceIn(1, 4)
        repeat(columns) { col ->
            repeat(3) { row ->
                val wx = x + w * (.16f + col * .20f)
                val wy = y + h * (.22f + row * .20f)
                drawRect(window.copy(alpha = if ((col + row + variant) % 3 == 0) .95f else .35f), Offset(wx, wy), Size(size.width * .025f, size.height * .035f))
            }
        }
        if (preset.architecture.contains("Néo", true) && i == 2) {
            drawRect(Color(0xFFB7A88F), Offset(x + w * .18f, y - size.height * .06f), Size(w * .64f, size.height * .06f))
        }
        x += w * .88f
    }
    drawRect(ground, Offset(0f, size.height * .80f), Size(size.width, size.height * .20f))
    drawRect(Color(0xFF53606A), Offset(0f, size.height * .86f), Size(size.width, size.height * .025f))
    if (preset.climate.contains("Pluv", true) || preset.climate.contains("Orage", true)) {
        repeat(8) { i ->
            val rx = size.width * ((i * 13 + variant * 7) % 100) / 100f
            drawLine(Color(0x99B9D9EF), Offset(rx, size.height * .08f), Offset(rx - size.width * .05f, size.height * .28f), 2f)
        }
    }
    if (preset.cityMood.contains("connect", true)) {
        drawLine(Color(0xAA62D9EA), Offset(size.width * .08f, size.height * .69f), Offset(size.width * .92f, size.height * .48f), 2f)
    }
}

private fun DrawScope.drawProceduralCostumePreset(preset: LibraryCostumePreset) {
    val (main, accent) = pixelHeroPalette(preset.palette, "")
    val outline = Color(0xFF0A0E13)
    drawRect(Color(0xFF111923))
    val cx = size.width * .50f
    val headW = size.width * .30f
    val headH = size.height * .18f
    val headX = cx - headW / 2f
    val headY = size.height * .10f
    val torsoW = size.width * when (preset.presentation) {
        "Intimidant" -> .48f
        "Flamboyant" -> .45f
        "Clandestin" -> .34f
        else -> .40f
    }
    val torsoX = cx - torsoW / 2f
    val torsoY = size.height * .31f
    val torsoH = size.height * .38f

    drawRect(outline, Offset(headX - 2f, headY - 2f), Size(headW + 4f, headH + 4f))
    drawRect(Color(0xFFB88768), Offset(headX, headY), Size(headW, headH))
    when (preset.mask) {
        "Demi-masque", "Masque minimal" -> drawRect(accent.copy(alpha = .92f), Offset(headX + headW * .08f, headY + headH * .38f), Size(headW * .84f, headH * .28f))
        "Visière" -> drawRect(accent.copy(alpha = .88f), Offset(headX + headW * .04f, headY + headH * .30f), Size(headW * .92f, headH * .36f))
        "Masque intégral", "Casque" -> drawRect(accent.copy(alpha = .82f), Offset(headX, headY), Size(headW, headH))
        "Capuche" -> {
            drawRect(main, Offset(headX - headW * .18f, headY - headH * .18f), Size(headW * 1.36f, headH * .22f))
            drawRect(main, Offset(headX - headW * .18f, headY, headW * .16f), Size(headW * .16f, headH * 1.15f))
            drawRect(main, Offset(headX + headW * 1.02f, headY, headW * .16f), Size(headW * .16f, headH * 1.15f))
        }
    }

    drawRect(outline, Offset(torsoX - 3f, torsoY - 3f), Size(torsoW + 6f, torsoH + 6f))
    drawRect(main, Offset(torsoX, torsoY), Size(torsoW, torsoH))
    when (preset.presentation) {
        "Tactique" -> {
            drawRect(accent.copy(alpha = .65f), Offset(torsoX + torsoW * .12f, torsoY + torsoH * .30f), Size(torsoW * .76f, torsoH * .12f))
            drawRect(outline.copy(alpha = .7f), Offset(torsoX + torsoW * .18f, torsoY + torsoH * .60f), Size(torsoW * .18f, torsoH * .18f))
            drawRect(outline.copy(alpha = .7f), Offset(torsoX + torsoW * .64f, torsoY + torsoH * .60f), Size(torsoW * .18f, torsoH * .18f))
        }
        "Flamboyant" -> {
            drawRect(accent, Offset(torsoX, torsoY + torsoH * .08f), Size(torsoW * .12f, torsoH * .82f))
            drawRect(accent, Offset(torsoX + torsoW * .88f, torsoY + torsoH * .08f), Size(torsoW * .12f, torsoH * .82f))
        }
        "Institutionnel" -> {
            drawRect(accent, Offset(cx - torsoW * .035f, torsoY + torsoH * .08f), Size(torsoW * .07f, torsoH * .70f))
            drawRect(accent.copy(alpha = .75f), Offset(torsoX + torsoW * .14f, torsoY + torsoH * .18f), Size(torsoW * .72f, torsoH * .07f))
        }
        "Clandestin", "Mystérieux" -> drawRect(main.copy(alpha = .78f), Offset(torsoX + torsoW * .12f, torsoY + torsoH * .12f), Size(torsoW * .76f, torsoH * .76f))
        else -> drawRect(accent.copy(alpha = .85f), Offset(cx - torsoW * .12f, torsoY + torsoH * .24f), Size(torsoW * .24f, torsoH * .24f))
    }

    drawRect(main, Offset(torsoX - size.width * .11f, torsoY + size.height * .02f), Size(size.width * .10f, torsoH * .82f))
    drawRect(main, Offset(torsoX + torsoW + size.width * .01f, torsoY + size.height * .02f), Size(size.width * .10f, torsoH * .82f))
    drawRect(Color(0xFF17202B), Offset(cx - torsoW * .34f, torsoY + torsoH), Size(torsoW * .24f, size.height * .24f))
    drawRect(Color(0xFF17202B), Offset(cx + torsoW * .10f, torsoY + torsoH), Size(torsoW * .24f, size.height * .24f))
    drawRect(accent.copy(alpha = .90f), Offset(cx - torsoW * .10f, torsoY + torsoH * .46f), Size(torsoW * .20f, torsoH * .10f))
}

@Composable
private fun LibraryPresetHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text("PRESETS PIXEL PROCÉDURAUX", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Text(title.uppercase(), color = UltimateIvory, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
    Text(subtitle, color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
internal fun LibraryCityPresetStrip(draft: UltimateCreationDraft, onDraft: (UltimateCreationDraft) -> Unit) {
    LibraryPresetHeader(
        "Ambiances urbaines",
        "Chaque ambiance est dessinée en direct à partir du climat, de l'architecture et de l'humeur de la ville."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.cityPresets.forEach { preset ->
            ProceduralPresetCell(
                label = preset.name,
                selected = preset.matches(draft),
                enabled = true,
                preview = { drawProceduralCityPreset(preset) },
                onClick = { onDraft(preset.apply(draft)) }
            )
        }
    }
}

@Composable
internal fun LibraryAliasCostumePresetStrip(
    currentPresentation: String,
    currentPalette: String,
    currentMask: String,
    onPreset: (LibraryCostumePreset) -> Unit
) {
    LibraryPresetHeader(
        "Premiers costumes",
        "À l'éveil, seuls les looks de début de carrière sont disponibles. Les ères avancées restent verrouillées."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.costumePresets.filter { it.minimumEra <= 1 }.forEach { preset ->
            val selected = currentPresentation == preset.presentation && currentPalette == preset.palette && currentMask == preset.mask
            ProceduralPresetCell(
                label = preset.name,
                selected = selected,
                enabled = true,
                tall = true,
                preview = { drawProceduralCostumePreset(preset) },
                onClick = { onPreset(preset) }
            )
        }
    }
}

@Composable
internal fun LibraryCostumePresetStrip(
    state: UltimateState,
    maxEra: Int,
    onStateChange: (UltimateState) -> Unit
) {
    LibraryPresetHeader(
        "Archives costume",
        "Les costumes sont prévisualisés avec le renderer pixel du jeu. Une ère ne s'active que lorsque la carrière l'autorise."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.costumePresets.forEach { preset ->
            val enabled = preset.minimumEra <= maxEra
            ProceduralPresetCell(
                label = if (enabled) preset.name else "${preset.name} · ÈRE ${preset.minimumEra}",
                selected = enabled && preset.matches(state),
                enabled = enabled,
                tall = true,
                preview = { drawProceduralCostumePreset(preset) },
                onClick = { if (enabled) onStateChange(preset.apply(state)) }
            )
        }
    }
}
