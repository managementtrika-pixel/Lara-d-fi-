package com.metahumanlegacy.game

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Curated Library assets are intentionally exposed as whole visual presets rather than arbitrary
 * generated layers. Each preset maps back to the stable procedural avatar/city/costume fields, so
 * incompatible eyes, hair, masks, limbs or backgrounds can never drift out of alignment.
 */
internal data class LibraryFacePreset(
    val name: String,
    val atlasIndex: Int,
    val skinTone: String,
    val faceShape: String,
    val hair: String,
    val hairColor: String,
    val facialHair: String,
    val eyes: String,
    val civilianStyle: String
) {
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        skinTone = skinTone,
        faceShape = faceShape,
        hair = hair,
        hairColor = hairColor,
        facialHair = facialHair,
        eyes = eyes,
        civilianStyle = civilianStyle
    )

    fun matches(draft: UltimateCreationDraft): Boolean =
        draft.skinTone == skinTone && draft.faceShape == faceShape && draft.hair == hair &&
            draft.hairColor == hairColor && draft.facialHair == facialHair && draft.eyes == eyes
}

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
    const val FACE_COLUMNS = 4
    const val FACE_ROWS = 3
    const val CITY_COLUMNS = 4
    const val CITY_ROWS = 3
    const val COSTUME_COLUMNS = 4
    const val COSTUME_ROWS = 2

    val facePresets = listOf(
        LibraryFacePreset("Classique", 0, "Très clair", "Ovale", "Court texturé", "Brun", "Aucune", "Bleus", "Classique"),
        LibraryFacePreset("Sculpté", 1, "Foncé", "Anguleux", "Rasé", "Noir", "Aucune", "Bruns", "Minimal"),
        LibraryFacePreset("Sobre", 2, "Clair", "Fin", "Dégradé", "Noir", "Aucune", "Très sombres", "Professionnel"),
        LibraryFacePreset("Urbain", 3, "Très foncé", "Carré", "Court texturé", "Noir", "Barbe courte", "Bruns", "Street sobre"),
        LibraryFacePreset("Élégante", 4, "Clair", "Ovale", "Attaché", "Noir", "Aucune", "Noisette", "Classique"),
        LibraryFacePreset("Athlétique", 5, "Foncé", "Ovale", "Rasé", "Noir", "Aucune", "Bruns", "Sportif"),
        LibraryFacePreset("Audacieuse", 6, "Mat", "Anguleux", "Rasé", "Brun", "Aucune", "Noisette", "Créatif"),
        LibraryFacePreset("Minimaliste", 7, "Clair", "Fin", "Rasé", "Noir", "Aucune", "Très sombres", "Minimal"),
        LibraryFacePreset("Nordique", 8, "Très clair", "Fin", "Court texturé", "Blond", "Aucune", "Bleus", "Vintage"),
        LibraryFacePreset("Créative", 9, "Clair", "Ovale", "Boucles", "Brun", "Aucune", "Verts", "Créatif"),
        LibraryFacePreset("Nocturne", 10, "Mat", "Anguleux", "Court texturé", "Noir", "Aucune", "Très sombres", "Street sobre"),
        LibraryFacePreset("Libre", 11, "Très clair", "Ovale", "Rasé", "Blond", "Aucune", "Gris", "Sportif")
    )

    // The source atlas contains two deliberately non-city fantasy/cosmic cells. They are not
    // offered here because the user asked us to reject anything that cannot fit the city system.
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
private fun LibraryAtlasCell(
    @DrawableRes drawable: Int,
    index: Int,
    columns: Int,
    rows: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tall: Boolean = false,
    onClick: () -> Unit
) {
    val bitmap = ImageBitmap.imageResource(id = drawable)
    val border = when {
        selected -> UltimateGold
        enabled -> Color(0x664F6685)
        else -> Color(0x332E3743)
    }
    Column(
        Modifier
            .width(if (tall) 82.dp else 92.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (tall) .72f else 1f)
                .background(Color(0xFF0B1017), CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                .border(1.dp, border, CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
        ) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(if (tall) .72f else 1f)) {
                val sourceW = bitmap.width / columns
                val sourceH = bitmap.height / rows
                val col = index % columns
                val row = index / columns
                if (row < rows) {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset(col * sourceW, row * sourceH),
                        srcSize = IntSize(sourceW, sourceH),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
                    )
                    if (!enabled) drawRect(Color.Black.copy(alpha = .62f), Offset.Zero, size)
                }
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

@Composable
private fun LibraryPresetHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text("ASSETS BIBLIOTHÈQUE VALIDÉS", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Text(title.uppercase(), color = UltimateIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
    Text(subtitle, color = UltimateMuted, fontSize = 9.sp, lineHeight = 13.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
internal fun LibraryFacePresetStrip(draft: UltimateCreationDraft, onDraft: (UltimateCreationDraft) -> Unit) {
    LibraryPresetHeader(
        "Looks illustrés",
        "Chaque vignette applique un preset complet aux paramètres stables. Aucun fragment généré n'est superposé au hasard."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.facePresets.forEach { preset ->
            LibraryAtlasCell(
                drawable = R.drawable.mhl_library_faces,
                index = preset.atlasIndex,
                columns = LibraryCustomizationCatalog.FACE_COLUMNS,
                rows = LibraryCustomizationCatalog.FACE_ROWS,
                label = preset.name,
                selected = preset.matches(draft),
                enabled = true,
                onClick = { onDraft(preset.apply(draft)) }
            )
        }
    }
}

@Composable
internal fun LibraryCityPresetStrip(draft: UltimateCreationDraft, onDraft: (UltimateCreationDraft) -> Unit) {
    LibraryPresetHeader(
        "Ambiances urbaines",
        "Seules les scènes compatibles avec le système de ville sont proposées; les cellules hors sujet restent exclues."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.cityPresets.forEach { preset ->
            LibraryAtlasCell(
                drawable = R.drawable.mhl_library_city,
                index = preset.atlasIndex,
                columns = LibraryCustomizationCatalog.CITY_COLUMNS,
                rows = LibraryCustomizationCatalog.CITY_ROWS,
                label = preset.name,
                selected = preset.matches(draft),
                enabled = true,
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
            LibraryAtlasCell(
                drawable = R.drawable.mhl_library_costumes,
                index = preset.atlasIndex,
                columns = LibraryCustomizationCatalog.COSTUME_COLUMNS,
                rows = LibraryCustomizationCatalog.COSTUME_ROWS,
                label = preset.name,
                selected = selected,
                enabled = true,
                tall = true,
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
        "Les planches 101–110 servent de presets d'époque. Une ère n'est activée que lorsque la carrière l'autorise."
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LibraryCustomizationCatalog.costumePresets.forEach { preset ->
            val enabled = preset.minimumEra <= maxEra
            LibraryAtlasCell(
                drawable = R.drawable.mhl_library_costumes,
                index = preset.atlasIndex,
                columns = LibraryCustomizationCatalog.COSTUME_COLUMNS,
                rows = LibraryCustomizationCatalog.COSTUME_ROWS,
                label = if (enabled) preset.name else "${preset.name} · ÈRE ${preset.minimumEra}",
                selected = enabled && preset.matches(state),
                enabled = enabled,
                tall = true,
                onClick = { if (enabled) onStateChange(preset.apply(state)) }
            )
        }
    }
}
