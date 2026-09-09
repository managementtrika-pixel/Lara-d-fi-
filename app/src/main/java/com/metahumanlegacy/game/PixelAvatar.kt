package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor

private data class PixelTone(val base: Color, val shade: Color, val light: Color)

private fun pxSkin(name: String): PixelTone = when (name) {
    "Très clair" -> PixelTone(Color(0xFFF1CFB8), Color(0xFFC99072), Color(0xFFFFE4D2))
    "Clair" -> PixelTone(Color(0xFFE1B493), Color(0xFFB97958), Color(0xFFF6D0B5))
    "Moyen" -> PixelTone(Color(0xFFC88962), Color(0xFF92583E), Color(0xFFE3AA82))
    "Mat" -> PixelTone(Color(0xFFA96D4B), Color(0xFF75432F), Color(0xFFC98B66))
    "Foncé" -> PixelTone(Color(0xFF77482F), Color(0xFF4D2B20), Color(0xFF9B6447))
    "Très foncé" -> PixelTone(Color(0xFF4B2B21), Color(0xFF2B1916), Color(0xFF704738))
    else -> PixelTone(Color(0xFFC88962), Color(0xFF92583E), Color(0xFFE3AA82))
}

private fun pxHair(name: String): PixelTone = when (name) {
    "Noir" -> PixelTone(Color(0xFF17191F), Color(0xFF090B0F), Color(0xFF343843))
    "Brun" -> PixelTone(Color(0xFF452C22), Color(0xFF261710), Color(0xFF6D4734))
    "Châtain" -> PixelTone(Color(0xFF704A31), Color(0xFF432A1A), Color(0xFF9A6A47))
    "Blond" -> PixelTone(Color(0xFFD1AB5B), Color(0xFF977332), Color(0xFFE8C979))
    "Roux" -> PixelTone(Color(0xFFA34B28), Color(0xFF692A19), Color(0xFFD36C3B))
    "Gris" -> PixelTone(Color(0xFF969AA2), Color(0xFF62666D), Color(0xFFC2C6CC))
    "Blanc" -> PixelTone(Color(0xFFE4E4E0), Color(0xFFAEB2B6), Color(0xFFF7F7F2))
    else -> PixelTone(Color(0xFF452C22), Color(0xFF261710), Color(0xFF6D4734))
}

private fun pxCivilOutfit(style: String): Triple<Color, Color, Color> = when {
    style.contains("Sport", true) -> Triple(Color(0xFF2D6FA8), Color(0xFF183A5A), Color(0xFF9FD0FF))
    style.contains("Class", true) -> Triple(Color(0xFF313841), Color(0xFF171C22), Color(0xFFC8D1DA))
    style.contains("Créat", true) -> Triple(Color(0xFF7B427F), Color(0xFF47254B), Color(0xFFE0A7EC))
    style.contains("Profession", true) -> Triple(Color(0xFF445873), Color(0xFF253142), Color(0xFFB7CCE5))
    style.contains("Vintage", true) -> Triple(Color(0xFF805D3D), Color(0xFF4B3422), Color(0xFFE0BB88))
    style.contains("Minimal", true) -> Triple(Color(0xFF38424D), Color(0xFF1D252C), Color(0xFF8B98A6))
    else -> Triple(Color(0xFF2A4767), Color(0xFF14283E), Color(0xFF75AEE8))
}

private fun pxHeroOutfit(palette: String): Triple<Color, Color, Color> = when (palette) {
    "Bleu / or" -> Triple(Color(0xFF2D6FD3), Color(0xFF18396C), Color(0xFFE7BE55))
    "Noir / argent" -> Triple(Color(0xFF20242C), Color(0xFF0B0E12), Color(0xFFB8C1CC))
    "Rouge / anthracite" -> Triple(Color(0xFFB63A42), Color(0xFF4D1B20), Color(0xFF353B44))
    "Blanc / cobalt" -> Triple(Color(0xFFE8EDF2), Color(0xFF9099A4), Color(0xFF2B61C9))
    "Violet / noir" -> Triple(Color(0xFF7147C4), Color(0xFF321D63), Color(0xFF14171C))
    "Vert / cuivre" -> Triple(Color(0xFF337A5D), Color(0xFF194634), Color(0xFFC47E48))
    "Ivoire / or" -> Triple(Color(0xFFECE4D2), Color(0xFF9B8F79), Color(0xFFD7AA39))
    else -> Triple(Color(0xFF364D68), Color(0xFF172536), Color(0xFF60A9FF))
}

private fun pxEye(name: String): Color = when (name) {
    "Bleus" -> Color(0xFF4D8CCB)
    "Verts" -> Color(0xFF5E9668)
    "Noisette" -> Color(0xFF8A673B)
    "Gris" -> Color(0xFF8793A0)
    "Très sombres" -> Color(0xFF17191E)
    else -> Color(0xFF51382C)
}

internal fun pixelLegHeight(age: Int, stature: String): Int {
    val base = when {
        age < 13 -> 8
        age < 18 -> 10
        else -> 12
    }
    val delta = when (stature) {
        "Petite" -> -2
        "Grande" -> 2
        else -> 0
    }
    return (base + delta).coerceIn(6, 14)
}

internal fun pixelAgeTier(age: Int): Int = when {
    age < 18 -> 0
    age < 35 -> 1
    age < 50 -> 2
    age < 65 -> 3
    else -> 4
}

internal fun pixelVisualKey(state: UltimateState, age: Int, heroMode: Boolean): String = listOf(
    state.bodyBuild, state.stature, state.skinTone, state.faceShape, state.hair, state.hairColor,
    if (age >= 16) state.facialHair else "Aucune", state.eyes, state.civilianStyle, state.accessory,
    if (heroMode) state.heroPresentation else "civil",
    if (heroMode) state.costumePalette else "civil",
    if (heroMode) state.maskStyle else "Aucun",
    if (heroMode) state.emblem else "Aucun",
    pixelAgeTier(age).toString()
).joinToString("|")

@Composable
internal fun PixelAvatar(
    state: UltimateState,
    modifier: Modifier = Modifier,
    age: Int = 18,
    temperament: String = "Prudent",
    heroMode: Boolean = false
) {
    Canvas(modifier) {
        val cols = 32
        val rows = 48
        val rawCell = minOf(size.width / cols, size.height / rows)
        val cell = floor(rawCell).coerceAtLeast(1f)
        val ox = floor((size.width - cols * cell) / 2f)
        val oy = floor((size.height - rows * cell) / 2f)

        fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color) {
            if (w <= 0 || h <= 0) return
            drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
        }

        val skin = pxSkin(state.skinTone)
        val hair = pxHair(state.hairColor)
        val outfit = if (heroMode) pxHeroOutfit(state.costumePalette) else pxCivilOutfit(state.civilianStyle)
        val cloth = outfit.first
        val clothShade = outfit.second
        val accent = outfit.third
        val outline = Color(0xFF0B1017)
        val white = Color(0xFFF4F5F2)
        val eye = pxEye(state.eyes)
        val child = age < 13
        val teen = age in 13..17

        val torsoY = when { child -> 25; teen -> 23; else -> 22 }
        val torsoH = when { child -> 9; teen -> 11; else -> 12 }
        val legH = pixelLegHeight(age, state.stature)
        val legY = torsoY + torsoH - 1
        val bodyWAdult = when (state.bodyBuild) {
            "Fin" -> 12
            "Massif" -> 20
            "Robuste" -> 18
            "Souple" -> 14
            else -> 16
        }
        val bodyW = (bodyWAdult - if (child) 3 else if (teen) 1 else 0).coerceAtLeast(10)
        val bodyX = (cols - bodyW) / 2

        // Floor shadow: broad, flat, deliberately pixel-clean.
        p(7, (legY + legH + 2).coerceAtMost(47), 18, 1, Color.Black.copy(alpha = .36f))
        p(10, (legY + legH + 1).coerceAtMost(46), 12, 1, Color.Black.copy(alpha = .20f))

        // Legs and shoes.
        val pants = if (heroMode) clothShade else Color(0xFF1B2938)
        val legW = if (state.bodyBuild == "Massif" && !child) 5 else 4
        val leftLegX = bodyX + 2
        val rightLegX = bodyX + bodyW - legW - 2
        p(leftLegX, legY, legW, legH, outline)
        p(leftLegX + 1, legY, legW - 2, legH - 1, pants)
        p(rightLegX, legY, legW, legH, outline)
        p(rightLegX + 1, legY, legW - 2, legH - 1, pants)
        p(leftLegX - 1, legY + legH - 1, legW + 2, 2, outline)
        p(rightLegX - 1, legY + legH - 1, legW + 2, 2, outline)
        p(leftLegX, legY + legH - 1, legW + 1, 1, if (heroMode) accent else Color(0xFF27384B))
        p(rightLegX, legY + legH - 1, legW + 1, 1, if (heroMode) accent else Color(0xFF27384B))

        // Torso silhouette and depth.
        p(bodyX, torsoY, bodyW, torsoH, outline)
        p(bodyX + 1, torsoY + 1, bodyW - 2, torsoH - 2, cloth)
        p(bodyX + 1, torsoY + torsoH - 3, bodyW - 2, 2, clothShade)
        p(bodyX + 2, torsoY + 2, (bodyW - 4).coerceAtLeast(2), 1, accent.copy(alpha = .45f))
        p(bodyX + bodyW / 2, torsoY + 2, 1, torsoH - 4, clothShade.copy(alpha = .65f))

        // Civil/hero style cues.
        if (heroMode) {
            when (state.heroPresentation) {
                "Tactique" -> {
                    p(bodyX + 2, torsoY + 4, bodyW - 4, 2, clothShade)
                    p(bodyX + 3, torsoY + 7, 3, 3, accent.copy(alpha = .72f))
                    p(bodyX + bodyW - 6, torsoY + 7, 3, 3, accent.copy(alpha = .72f))
                }
                "Flamboyant" -> {
                    p(bodyX - 2, torsoY + 1, 2, torsoH + 2, accent.copy(alpha = .85f))
                    p(bodyX + bodyW, torsoY + 1, 2, torsoH + 2, accent.copy(alpha = .85f))
                }
                "Intimidant" -> {
                    p(bodyX + 1, torsoY + 1, bodyW - 2, 2, clothShade)
                    p(bodyX - 1, torsoY + 2, 2, 4, clothShade)
                    p(bodyX + bodyW - 1, torsoY + 2, 2, 4, clothShade)
                }
                "Institutionnel" -> p(bodyX + 3, torsoY + 3, bodyW - 6, 1, accent)
                "Clandestin", "Mystérieux" -> {
                    p(bodyX + 1, torsoY + 1, bodyW - 2, 2, Color(0xFF121820))
                    p(bodyX + 2, torsoY + 3, bodyW - 4, 1, clothShade)
                }
            }
            drawPixelEmblem(state.emblem, accent, bodyX + bodyW / 2, torsoY + 6, ::p)
        } else {
            when {
                state.civilianStyle.contains("Sport", true) -> p(bodyX + bodyW / 2 - 1, torsoY + 3, 2, torsoH - 5, accent.copy(alpha = .65f))
                state.civilianStyle.contains("Class", true) || state.civilianStyle.contains("Profession", true) -> {
                    p(bodyX + bodyW / 2 - 1, torsoY + 2, 2, 5, accent.copy(alpha = .72f))
                    p(bodyX + bodyW / 2 - 2, torsoY + 7, 4, 1, accent.copy(alpha = .72f))
                }
                state.civilianStyle.contains("Créat", true) -> {
                    p(bodyX + 2, torsoY + 5, 3, 3, accent.copy(alpha = .65f))
                    p(bodyX + bodyW - 5, torsoY + 4, 3, 4, accent.copy(alpha = .45f))
                }
            }
        }

        // Arms and hands.
        val armH = torsoH - 2
        p(bodyX - 3, torsoY + 2, 3, armH, outline)
        p(bodyX - 2, torsoY + 3, 2, armH - 2, if (heroMode) cloth else skin.base)
        p(bodyX + bodyW, torsoY + 2, 3, armH, outline)
        p(bodyX + bodyW, torsoY + 3, 2, armH - 2, if (heroMode) cloth else skin.base)
        p(bodyX - 2, torsoY + armH + 1, 2, 2, skin.base)
        p(bodyX + bodyW, torsoY + armH + 1, 2, 2, skin.base)

        // Neck.
        p(13, 18, 6, 5, outline)
        p(14, 18, 4, 5, skin.base)
        p(14, 21, 4, 1, skin.shade.copy(alpha = .55f))

        // Head with face-shape-specific silhouette.
        val headW = when (state.faceShape) {
            "Fin" -> 12
            "Rond" -> 15
            "Carré" -> 14
            "Anguleux" -> 14
            else -> 13
        }
        val headX = (cols - headW) / 2
        val headY = if (child) 4 else 5
        val headH = if (child) 15 else 14
        p(headX + 1, headY, headW - 2, 1, outline)
        p(headX, headY + 1, headW, headH - 3, outline)
        p(headX + 1, headY + headH - 2, headW - 2, 1, outline)
        p(headX + 2, headY + 1, headW - 4, headH - 4, skin.base)
        p(headX + 1, headY + 4, 2, headH - 7, skin.shade)
        p(headX + headW - 3, headY + 3, 1, headH - 7, skin.light.copy(alpha = .58f))
        if (state.faceShape == "Rond") {
            p(headX + 2, headY + headH - 3, headW - 4, 1, skin.base)
        } else if (state.faceShape == "Anguleux") {
            p(headX + 2, headY + headH - 3, 2, 1, outline)
            p(headX + headW - 4, headY + headH - 3, 2, 1, outline)
        }

        // Ears.
        p(headX - 1, headY + 6, 2, 4, outline)
        p(headX - 1, headY + 7, 1, 2, skin.base)
        p(headX + headW - 1, headY + 6, 2, 4, outline)
        p(headX + headW, headY + 7, 1, 2, skin.base)

        // Eyes: two-tone eyes + catchlights, still pixel-readable at phone size.
        val eyeY = headY + 7
        val leftEyeX = headX + 3
        val rightEyeX = headX + headW - 6
        p(leftEyeX, eyeY, 3, 2, outline)
        p(rightEyeX, eyeY, 3, 2, outline)
        p(leftEyeX + 1, eyeY, 2, 1, white)
        p(rightEyeX + 1, eyeY, 2, 1, white)
        p(leftEyeX + 2, eyeY + 1, 1, 1, eye)
        p(rightEyeX + 1, eyeY + 1, 1, 1, eye)

        // Brows carry temperament.
        val brow = hair.shade
        when (temperament) {
            "Curieux" -> {
                p(leftEyeX, eyeY - 2, 3, 1, brow)
                p(rightEyeX, eyeY - 1, 3, 1, brow)
            }
            "Méfiant" -> {
                p(leftEyeX, eyeY - 1, 3, 1, brow)
                p(rightEyeX, eyeY - 2, 3, 1, brow)
            }
            "Impulsif", "Ambitieux" -> {
                p(leftEyeX, eyeY - 2, 3, 1, brow)
                p(rightEyeX, eyeY - 2, 3, 1, brow)
                p(leftEyeX + 2, eyeY - 1, 1, 1, brow)
                p(rightEyeX, eyeY - 1, 1, 1, brow)
            }
            else -> {
                p(leftEyeX, eyeY - 1, 3, 1, brow)
                p(rightEyeX, eyeY - 1, 3, 1, brow)
            }
        }

        // Nose and mouth.
        val mid = headX + headW / 2
        p(mid, eyeY + 2, 1, 2, skin.shade.copy(alpha = .75f))
        p(mid + 1, eyeY + 3, 1, 1, skin.light.copy(alpha = .55f))
        val mouth = Color(0xFF713C40)
        p(mid - 2, eyeY + 5, 4, 1, mouth)
        when (temperament) {
            "Curieux" -> p(mid + 1, eyeY + 6, 1, 1, mouth)
            "Méfiant" -> p(mid - 2, eyeY + 6, 1, 1, mouth)
        }

        // Subtle age evolution preserves identity instead of swapping portraits.
        when (pixelAgeTier(age)) {
            2 -> {
                p(headX + 2, eyeY + 3, 1, 1, skin.shade.copy(alpha = .45f))
                p(headX + headW - 3, eyeY + 3, 1, 1, skin.shade.copy(alpha = .45f))
            }
            3 -> {
                p(headX + 2, eyeY + 2, 1, 3, skin.shade.copy(alpha = .55f))
                p(headX + headW - 3, eyeY + 2, 1, 3, skin.shade.copy(alpha = .55f))
                p(mid - 3, eyeY + 7, 6, 1, skin.shade.copy(alpha = .42f))
            }
            4 -> {
                p(headX + 2, eyeY + 2, 1, 3, skin.shade.copy(alpha = .62f))
                p(headX + headW - 3, eyeY + 2, 1, 3, skin.shade.copy(alpha = .62f))
                p(mid - 3, eyeY + 7, 6, 1, skin.shade.copy(alpha = .48f))
            }
        }

        drawPixelHairV2(state.hair, hair, headX, headY, headW, cell, ox, oy)

        // Facial hair is an age-gated layer over the same face.
        if (age >= 16 && state.facialHair != "Aucune") {
            when (state.facialHair) {
                "Moustache" -> p(mid - 3, eyeY + 4, 6, 1, hair.base)
                "Bouc" -> {
                    p(mid - 2, eyeY + 4, 4, 1, hair.base)
                    p(mid - 1, eyeY + 5, 2, 3, hair.base)
                }
                "Barbe courte" -> {
                    p(headX + 2, eyeY + 4, headW - 4, 2, hair.base.copy(alpha = .86f))
                    p(headX + 3, eyeY + 6, headW - 6, 2, hair.shade.copy(alpha = .86f))
                }
                else -> {
                    p(headX + 1, eyeY + 3, headW - 2, 3, hair.base)
                    p(headX + 2, eyeY + 6, headW - 4, 3, hair.shade)
                }
            }
        }

        // Civil accessories remain real layers instead of textual metadata.
        when {
            state.accessory.contains("Lun", true) -> {
                p(leftEyeX - 1, eyeY - 1, 5, 4, outline)
                p(rightEyeX - 1, eyeY - 1, 5, 4, outline)
                p(leftEyeX, eyeY, 3, 2, Color(0xFF527B9D).copy(alpha = .72f))
                p(rightEyeX, eyeY, 3, 2, Color(0xFF527B9D).copy(alpha = .72f))
                p(leftEyeX + 4, eyeY, (rightEyeX - leftEyeX - 4).coerceAtLeast(1), 1, outline)
            }
            state.accessory.contains("Casquette", true) && !heroMode -> {
                p(headX - 1, headY - 2, headW + 2, 3, Color(0xFF36506E))
                p(headX + headW - 2, headY + 1, 5, 1, Color(0xFF36506E))
                p(headX, headY - 2, headW, 1, Color(0xFF6D8BAA))
            }
            state.accessory.contains("Bonnet", true) && !heroMode -> {
                p(headX - 1, headY - 3, headW + 2, 4, Color(0xFF6A4B72))
                p(headX, headY - 3, headW, 1, Color(0xFF9872A0))
            }
            state.accessory.contains("Boucle", true) -> p(headX - 1, headY + 9, 1, 2, Color(0xFFE0B94F))
            state.accessory.contains("Chaîne", true) -> {
                p(bodyX + bodyW / 2 - 3, torsoY + 2, 6, 1, Color(0xFFD8B452))
                p(bodyX + bodyW / 2, torsoY + 3, 1, 2, Color(0xFFD8B452))
            }
        }

        if (heroMode) {
            drawPixelMask(state.maskStyle, accent, headX, headY, headW, eyeY, ::p)
        }

        if (state.injuries.isNotEmpty()) {
            p(headX + headW - 4, eyeY + 2, 1, 4, Color(0xFF8F3D42))
            p(headX + headW - 3, eyeY + 5, 1, 1, Color(0xFF8F3D42))
        }
    }
}

private fun DrawScope.drawPixelHairV2(
    style: String,
    tone: PixelTone,
    headX: Int,
    headY: Int,
    headW: Int,
    cell: Float,
    ox: Float,
    oy: Float
) {
    fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color = tone.base) {
        drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
    }
    when (style) {
        "Rasé" -> {
            p(headX + 1, headY, headW - 2, 2, tone.shade)
            p(headX + 2, headY + 2, headW - 4, 1, tone.base)
        }
        "Long" -> {
            p(headX - 1, headY - 2, headW + 2, 4, tone.shade)
            p(headX, headY - 1, headW, 3)
            p(headX - 1, headY + 2, 3, 13)
            p(headX + headW - 2, headY + 2, 3, 13)
            p(headX, headY - 2, headW - 3, 1, tone.light)
        }
        "Tresses" -> {
            p(headX, headY - 2, headW, 3, tone.shade)
            p(headX + 1, headY - 1, headW - 2, 2)
            repeat(5) { i ->
                val x = headX + 1 + i * ((headW - 2).coerceAtLeast(5) / 5)
                p(x, headY + 1, 1, 12, if (i % 2 == 0) tone.base else tone.shade)
            }
        }
        "Boucles" -> {
            p(headX, headY - 1, headW, 2, tone.shade)
            repeat(7) { i ->
                p(headX - 1 + (i % 5) * 3, headY - 3 + (i % 2), 3, 3, if (i % 3 == 0) tone.light else tone.base)
            }
            p(headX - 1, headY + 1, 2, 4)
            p(headX + headW - 1, headY + 1, 2, 4)
        }
        "Undercut" -> {
            p(headX + 1, headY - 1, headW - 2, 2, tone.shade)
            p(headX + 4, headY - 4, headW - 4, 4)
            p(headX + headW - 3, headY - 2, 3, 3, tone.light)
        }
        "Attaché" -> {
            p(headX, headY - 2, headW, 3, tone.shade)
            p(headX + 1, headY - 1, headW - 2, 2)
            p(headX + headW / 2 - 2, headY - 5, 4, 3, tone.base)
            p(headX + headW / 2 - 1, headY - 6, 2, 1, tone.light)
        }
        "Dégradé" -> {
            p(headX, headY - 1, headW, 3, tone.shade)
            p(headX + 2, headY - 2, headW - 4, 3)
            p(headX + 3, headY - 2, headW - 6, 1, tone.light)
            p(headX, headY + 2, 2, 3, tone.shade)
        }
        else -> {
            p(headX, headY - 2, headW, 3, tone.shade)
            p(headX + 1, headY - 3, headW - 4, 3)
            p(headX + 4, headY - 4, headW - 5, 2)
            p(headX + 2, headY - 3, headW - 6, 1, tone.light)
        }
    }
}

private fun drawPixelMask(
    style: String,
    color: Color,
    headX: Int,
    headY: Int,
    headW: Int,
    eyeY: Int,
    p: (Int, Int, Int, Int, Color) -> Unit
) {
    if (style == "Aucun") return
    when (style) {
        "Masque intégral", "Casque" -> {
            p(headX, headY + 1, headW, 4, color.copy(alpha = .88f))
            p(headX + 1, eyeY - 1, headW - 2, 4, color.copy(alpha = .92f))
        }
        "Visière" -> p(headX + 2, eyeY - 1, headW - 4, 3, color.copy(alpha = .88f))
        "Capuche" -> {
            p(headX - 2, headY - 2, headW + 4, 2, color.copy(alpha = .82f))
            p(headX - 2, headY, 2, 9, color.copy(alpha = .82f))
            p(headX + headW, headY, 2, 9, color.copy(alpha = .82f))
        }
        else -> {
            p(headX + 2, eyeY - 1, headW - 4, 2, color.copy(alpha = .88f))
            p(headX + 3, eyeY + 1, headW - 6, 1, color.copy(alpha = .78f))
        }
    }
}

private fun drawPixelEmblem(
    emblem: String,
    color: Color,
    cx: Int,
    cy: Int,
    p: (Int, Int, Int, Int, Color) -> Unit
) {
    if (emblem == "Aucun") return
    when {
        emblem.contains("Étoile") || emblem.contains("Comète") -> {
            p(cx, cy - 2, 1, 5, color)
            p(cx - 2, cy, 5, 1, color)
            p(cx - 1, cy - 1, 3, 3, color.copy(alpha = .75f))
        }
        emblem.contains("Anneau") -> {
            p(cx - 2, cy - 2, 5, 1, color)
            p(cx - 2, cy + 2, 5, 1, color)
            p(cx - 2, cy - 1, 1, 3, color)
            p(cx + 2, cy - 1, 1, 3, color)
        }
        emblem.contains("Bouclier") -> {
            p(cx - 2, cy - 2, 5, 1, color)
            p(cx - 2, cy - 1, 1, 3, color)
            p(cx + 2, cy - 1, 1, 3, color)
            p(cx - 1, cy + 2, 3, 1, color)
            p(cx, cy + 3, 1, 1, color)
        }
        else -> {
            p(cx - 2, cy, 5, 1, color)
            p(cx, cy - 2, 1, 5, color)
        }
    }
}
