package com.metahumanlegacy.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.floor
import kotlin.math.sin

private fun pxSkin(name: String): Color = when (name) {
    "Très clair" -> Color(0xFFF3D2BD)
    "Clair" -> Color(0xFFE8BFA4)
    "Moyen" -> Color(0xFFC88E67)
    "Mat" -> Color(0xFFAA714F)
    "Foncé" -> Color(0xFF794A34)
    "Très foncé" -> Color(0xFF4C2B22)
    else -> Color(0xFFC88E67)
}

private fun pxHair(name: String): Color = when (name) {
    "Noir" -> Color(0xFF111318)
    "Brun" -> Color(0xFF3B261E)
    "Châtain" -> Color(0xFF6A4630)
    "Blond" -> Color(0xFFD4B06A)
    "Roux" -> Color(0xFFA9502B)
    "Gris" -> Color(0xFF9699A0)
    "Blanc" -> Color(0xFFE8E5DE)
    else -> Color(0xFF3B261E)
}

private fun pxOutfit(style: String): Pair<Color, Color> = when {
    style.contains("Sport", true) -> Color(0xFF235F8F) to Color(0xFF9DD7FF)
    style.contains("Class", true) -> Color(0xFF202A35) to Color(0xFFE2E9F2)
    style.contains("Créat", true) -> Color(0xFF713C7B) to Color(0xFFF0B9FF)
    style.contains("Profession", true) -> Color(0xFF344B67) to Color(0xFFD4E1EF)
    style.contains("Vintage", true) -> Color(0xFF6E5037) to Color(0xFFEBC79C)
    else -> Color(0xFF213C58) to Color(0xFF79BAF0)
}

private fun pxPowerPalette(power: String): Pair<Color, Color> = when (power) {
    "Énergie" -> Color(0xFF154CA8) to Color(0xFF72DDFF)
    "Force" -> Color(0xFF922E3A) to Color(0xFFFFC75A)
    "Vitesse" -> Color(0xFF173D79) to Color(0xFF67E9FF)
    "Télékinésie" -> Color(0xFF593793) to Color(0xFFCFA6FF)
    "Élémentaire" -> Color(0xFF27644E) to Color(0xFFA7F096)
    "Mental" -> Color(0xFF47317E) to Color(0xFFE5B6FF)
    "Technologique" -> Color(0xFF263E54) to Color(0xFF67DAE9)
    else -> Color(0xFF28384E) to Color(0xFFE6C35A)
}

internal fun pixelHeroPalette(costumePalette: String, powerFamily: String): Pair<Color, Color> = when (costumePalette) {
    "Bleu / or" -> Color(0xFF1F4E9A) to Color(0xFFF2C85A)
    "Noir / argent" -> Color(0xFF15191F) to Color(0xFFC6CDD6)
    "Rouge / anthracite" -> Color(0xFF8F2831) to Color(0xFF343A42)
    "Blanc / cobalt" -> Color(0xFFE7ECF2) to Color(0xFF2456B8)
    "Violet / noir" -> Color(0xFF573A8A) to Color(0xFF15131B)
    "Vert / cuivre" -> Color(0xFF2F684E) to Color(0xFFC07A43)
    "Ivoire / or" -> Color(0xFFE8DFC8) to Color(0xFFD0A841)
    else -> pxPowerPalette(powerFamily)
}

internal fun pixelLegHeight(age: Int, stature: String): Int {
    val base = if (age < 13) 5 else 6
    return (base + when (stature) { "Petite" -> -1; "Grande" -> 1; else -> 0 }).coerceIn(4, 7)
}

internal fun pixelAgeTier(age: Int): Int = when {
    age < 18 -> 0
    age < 35 -> 1
    age < 50 -> 2
    age < 65 -> 3
    else -> 4
}

/**
 * 4.0 Pixel DNA renderer.
 * Uses a taller 48x64 virtual sprite with layered silhouette, face, hair, clothes, costume,
 * highlights and age details. The same renderer remains authoritative in creator and gameplay.
 */
@Composable
internal fun PixelAvatar(
    state: UltimateState,
    modifier: Modifier = Modifier,
    age: Int = 18,
    temperament: String = "Prudent",
    heroMode: Boolean = false,
    powerFamily: String = ""
) {
    val settings = LocalMetahumanMotion.current.settings
    val idle = rememberInfiniteTransition(label = "pixel-dna-idle")
    val breath by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MetahumanMotionTokens.duration(2100, settings), easing = MetahumanMotionTokens.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pixel-dna-breath"
    )
    val breathing = if (settings.reduceMotion) 0f else sin(breath * 3.1415927f) * .35f

    Canvas(modifier) {
        val cols = 48
        val rows = 64
        val cell = floor(minOf(size.width / cols, size.height / rows)).coerceAtLeast(1f)
        val ox = floor((size.width - cols * cell) / 2f)
        val oy = floor((size.height - rows * cell) / 2f + breathing * cell)
        fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color) {
            if (w > 0 && h > 0) drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
        }

        val skin = pxSkin(state.skinTone)
        val skinShadow = Color(skin.red * .78f, skin.green * .78f, skin.blue * .78f, 1f)
        val skinLight = Color(
            (skin.red * 1.08f).coerceAtMost(1f),
            (skin.green * 1.08f).coerceAtMost(1f),
            (skin.blue * 1.08f).coerceAtMost(1f), 1f
        )
        val oldHair = if (age >= 65 && state.hairColor !in listOf("Blanc", "Gris")) Color(0xFFB9BBC0) else pxHair(state.hairColor)
        val hairShadow = Color(oldHair.red * .66f, oldHair.green * .66f, oldHair.blue * .66f, 1f)
        val (civilMain, civilTrim) = pxOutfit(state.civilianStyle)
        val hero = pixelHeroPalette(state.costumePalette, powerFamily)
        val main = if (heroMode) hero.first else civilMain
        val trim = if (heroMode) hero.second else civilTrim
        val outline = Color(0xFF080C12)
        val deepest = Color(0xFF030509)
        val white = Color(0xFFF3F5F7)
        val eye = when (state.eyes) {
            "Bleus" -> Color(0xFF5EAEDE)
            "Verts" -> Color(0xFF6BB87A)
            "Noisette" -> Color(0xFFA37942)
            "Gris" -> Color(0xFFA2AAB4)
            "Très sombres" -> Color(0xFF17191C)
            else -> Color(0xFF594234)
        }
        val identity = pixelFaceIdentity(state)
        val child = age < 13
        val teen = age in 13..17
        val elder = age >= 58

        val bodyWidth = when (state.bodyBuild) {
            "Fin" -> 16
            "Massif" -> 27
            "Robuste" -> 24
            "Souple" -> 18
            else -> 21
        } + if (heroMode && state.heroPresentation == "Intimidant") 2 else 0
        val torsoW = bodyWidth.coerceIn(15, 29)
        val torsoX = (cols - torsoW) / 2
        val torsoY = when { child -> 32; teen -> 29; else -> 27 }
        val torsoH = when { child -> 13; teen -> 16; else -> 18 }
        val legScale = pixelLegHeight(age, state.stature) - 6
        val legH = (15 + legScale * 2 - if (child) 5 else if (teen) 2 else 0).coerceIn(9, 19)

        // Soft pixel shadow anchors the character instead of letting it float in the UI.
        p(11, 60, 26, 2, Color.Black.copy(alpha = .30f))
        p(15, 59, 18, 1, Color.Black.copy(alpha = .45f))

        // Legs: outline, trousers/hero suit, light-facing strip, boots.
        val legY = torsoY + torsoH - 2
        val legMain = if (heroMode) main else Color(0xFF172331)
        val leftLegX = torsoX + 3
        val rightLegX = torsoX + torsoW - 9
        listOf(leftLegX, rightLegX).forEach { x ->
            p(x, legY, 7, legH, outline)
            p(x + 1, legY, 5, legH - 2, legMain)
            p(x + 1, legY + 1, 1, legH - 4, trim.copy(alpha = if (heroMode) .25f else .10f))
            p(x - 1, (legY + legH - 3).coerceAtMost(61), 9, 3, deepest)
            p(x + 1, (legY + legH - 3).coerceAtMost(61), 6, 1, Color.White.copy(alpha = .05f))
        }

        // Torso silhouette has shoulders and waist tapering rather than one rectangular block.
        p(torsoX + 2, torsoY - 2, torsoW - 4, 3, outline)
        p(torsoX, torsoY + 1, torsoW, torsoH - 3, outline)
        p(torsoX + 2, torsoY + torsoH - 2, torsoW - 4, 3, outline)
        p(torsoX + 2, torsoY, torsoW - 4, torsoH - 2, main)
        p(torsoX + 3, torsoY + 1, (torsoW - 6).coerceAtLeast(5), 2, Color.White.copy(alpha = .08f))
        p(torsoX + 3, torsoY + torsoH - 4, torsoW - 6, 2, Color.Black.copy(alpha = .16f))

        // Arms are slimmer at the hands and slightly asymmetric for a less mannequin-like pose.
        val armTop = torsoY + 1
        val armH = torsoH - 2
        p(torsoX - 5, armTop, 6, armH - 2, outline)
        p(torsoX - 4, armTop + 1, 4, armH - 4, main)
        p(torsoX - 4, armTop + armH - 4, 4, 4, skin)
        p(torsoX + torsoW - 1, armTop, 6, armH - 1, outline)
        p(torsoX + torsoW, armTop + 1, 4, armH - 4, main)
        p(torsoX + torsoW, armTop + armH - 4, 4, 4, skin)
        p(torsoX - 3, armTop + 2, 1, armH - 7, Color.White.copy(alpha = .07f))

        // Costume language: presentation and era materially change the sprite.
        if (heroMode) {
            when (state.heroPresentation) {
                "Tactique" -> {
                    p(torsoX + 4, torsoY + 5, torsoW - 8, 3, trim.copy(alpha = .58f))
                    p(torsoX + 4, torsoY + 10, 5, 5, deepest.copy(alpha = .72f))
                    p(torsoX + torsoW - 9, torsoY + 10, 5, 5, deepest.copy(alpha = .72f))
                }
                "Flamboyant" -> {
                    p(torsoX + 2, torsoY + 2, 3, torsoH - 5, trim)
                    p(torsoX + torsoW - 5, torsoY + 2, 3, torsoH - 5, trim)
                    p(torsoX + 7, torsoY + 4, torsoW - 14, 2, trim.copy(alpha = .72f))
                }
                "Institutionnel" -> {
                    p(23, torsoY + 3, 2, torsoH - 7, trim)
                    p(torsoX + 5, torsoY + 4, torsoW - 10, 2, trim.copy(alpha = .70f))
                }
                "Clandestin", "Mystérieux" -> {
                    p(torsoX + 4, torsoY + 2, torsoW - 8, torsoH - 6, deepest.copy(alpha = .34f))
                    p(22, torsoY + 5, 4, 7, trim.copy(alpha = .54f))
                }
                else -> {
                    p(20, torsoY + 5, 8, 7, trim.copy(alpha = .88f))
                    p(22, torsoY + 7, 4, 3, main)
                }
            }
            if (state.costumeEra >= 2) {
                p(torsoX + 3, torsoY + torsoH - 6, torsoW - 6, 2, trim.copy(alpha = .88f))
                p(torsoX - 3, armTop + 2, 2, 5, trim.copy(alpha = .60f))
                p(torsoX + torsoW + 1, armTop + 2, 2, 5, trim.copy(alpha = .60f))
            }
            if (state.costumeEra >= 3) {
                p(torsoX + 4, torsoY, 4, 3, trim)
                p(torsoX + torsoW - 8, torsoY, 4, 3, trim)
            }
        } else {
            when {
                state.civilianStyle.contains("Sport", true) -> p(23, torsoY + 2, 2, torsoH - 6, trim.copy(alpha = .72f))
                state.civilianStyle.contains("Class", true) || state.civilianStyle.contains("Profession", true) -> {
                    p(23, torsoY + 2, 2, torsoH - 6, trim)
                    p(20, torsoY + 8, 8, 2, trim.copy(alpha = .45f))
                }
                state.civilianStyle.contains("Créat", true) -> {
                    p(torsoX + 4, torsoY + 6, 5, 4, trim)
                    p(torsoX + torsoW - 9, torsoY + 4, 5, 6, trim.copy(alpha = .62f))
                }
                state.civilianStyle.contains("Vintage", true) -> {
                    p(torsoX + 4, torsoY + 6, torsoW - 8, 2, Color(0xFF493123))
                    p(torsoX + 6, torsoY + 12, torsoW - 12, 2, Color(0xFF493123))
                }
            }
        }

        // Neck and head use more vertical resolution so eyes/hair remain identifiable in gameplay.
        p(20, 22, 8, 7, outline)
        p(21, 22, 6, 7, skin)
        val headW = when (state.faceShape) { "Fin" -> 17; "Rond" -> 21; "Carré" -> 20; "Anguleux" -> 19; else -> 19 }
        val headH = if (child) 20 else 21
        val headX = (cols - headW) / 2
        val headY = if (child) 4 else 3
        p(headX, headY, headW, headH, outline)
        p(headX + 1, headY + 1, headW - 2, headH - 2, skin)
        p(headX + 2, headY + 3, 2, headH - 7, skinLight.copy(alpha = .52f))
        p(headX + headW - 4, headY + 5, 2, headH - 8, skinShadow.copy(alpha = .65f))
        p(headX - 1, headY + 8, 2, 6, skinShadow)
        p(headX + headW - 1, headY + 8, 2, 6, skinShadow)

        if (state.faceShape == "Fin") {
            p(headX + 1, headY + headH - 5, 2, 4, outline)
            p(headX + headW - 3, headY + headH - 5, 2, 4, outline)
        }
        if (state.faceShape == "Carré") {
            p(headX, headY + headH - 6, 2, 5, outline)
            p(headX + headW - 2, headY + headH - 6, 2, 5, outline)
        }

        val eyeY = headY + 10 + identity.eyeLevel
        val lx = headX + 4 - identity.eyeInset
        val rx = headX + headW - 8 + identity.eyeInset
        p(lx, eyeY, 4, 3, white)
        p(rx, eyeY, 4, 3, white)
        p(lx + 1, eyeY, 2, 3, eye)
        p(rx + 1, eyeY, 2, 3, eye)
        p(lx + 1, eyeY, 1, 1, Color.White.copy(alpha = .78f))
        p(rx + 1, eyeY, 1, 1, Color.White.copy(alpha = .78f))
        val browLeft = (eyeY - 3 + identity.browOffset).coerceAtLeast(headY + 5)
        val browRight = (eyeY - 3 + if (temperament == "Méfiant") -1 else identity.browOffset).coerceAtLeast(headY + 5)
        p(lx, browLeft, 4, 1, hairShadow)
        p(rx, browRight, 4, 1, hairShadow)

        val noseX = 23 + identity.noseOffset
        p(noseX, eyeY + 3, 2, 4, skinShadow.copy(alpha = .70f))
        p(noseX, eyeY + 3, 1, 2, skinLight.copy(alpha = .65f))
        val mouthY = headY + 17
        val mouthInset = identity.mouthInset.coerceIn(0, 2)
        p(headX + 6 + mouthInset, mouthY, (headW - 12 - mouthInset * 2).coerceAtLeast(4), 1, Color(0xFF6C3D3B).copy(alpha = .86f))
        if (identity.cheekMark == 1) p(headX + 3, mouthY - 3, 2, 1, skinShadow.copy(alpha = .42f))
        if (identity.cheekMark == 2) p(headX + headW - 5, mouthY - 3, 2, 1, skinShadow.copy(alpha = .42f))

        // Hair has silhouette, shadow and highlight rather than a single cap.
        val hairTop = when {
            state.hair.contains("Ras", true) -> 3
            state.hair.contains("Long", true) -> 7
            state.hair.contains("Boucl", true) -> 5
            else -> 4
        }
        p(headX - 1, headY - 1, headW + 2, hairTop, hairShadow)
        p(headX + 1, headY - 1, headW - 2, (hairTop - 1).coerceAtLeast(2), oldHair)
        p(headX + 3, headY, (headW / 3).coerceAtLeast(4), 1, Color.White.copy(alpha = .10f))
        if (state.hair.contains("Long", true)) {
            p(headX - 1, headY + 3, 3, 15, hairShadow)
            p(headX + headW - 2, headY + 3, 3, 15, hairShadow)
            p(headX, headY + 4, 2, 12, oldHair)
            p(headX + headW - 1, headY + 4, 2, 12, oldHair)
        }
        if (state.hair.contains("Boucl", true)) {
            p(headX - 2, headY, 3, 5, oldHair)
            p(headX + headW - 1, headY, 3, 5, oldHair)
            p(headX + 4, headY - 2, 4, 3, oldHair)
            p(headX + headW - 8, headY - 2, 4, 3, oldHair)
        }

        // Mask is genuinely visible in hero mode.
        if (heroMode && state.maskStyle != "Aucun") {
            when {
                state.maskStyle.contains("intégr", true) -> {
                    p(headX + 2, eyeY - 2, headW - 4, 8, deepest.copy(alpha = .92f))
                    p(lx, eyeY, 4, 3, white)
                    p(rx, eyeY, 4, 3, white)
                }
                state.maskStyle.contains("demi", true) -> p(headX + 2, eyeY - 2, headW - 4, 5, deepest.copy(alpha = .90f))
                else -> {
                    p(lx - 1, eyeY - 1, 6, 5, deepest.copy(alpha = .88f))
                    p(rx - 1, eyeY - 1, 6, 5, deepest.copy(alpha = .88f))
                    p(headX + headW / 2 - 2, eyeY, 4, 2, deepest.copy(alpha = .88f))
                }
            }
        }

        // Aging is readable without replacing the underlying identity.
        if (pixelAgeTier(age) >= 2) {
            p(headX + 3, eyeY + 4, 3, 1, skinShadow.copy(alpha = .38f))
            p(headX + headW - 6, eyeY + 4, 3, 1, skinShadow.copy(alpha = .38f))
        }
        if (elder) {
            p(headX + 4, mouthY + 2, headW - 8, 1, skinShadow.copy(alpha = .28f))
            p(headX + 2, headY + 7, 1, 7, Color.White.copy(alpha = .12f))
        }

        // Hero trim catches light and makes powers/costume readable at thumbnail size.
        if (heroMode) {
            p(torsoX + 2, torsoY, torsoW - 4, 1, trim.copy(alpha = .78f))
            p(leftLegX + 1, legY + 2, 1, (legH - 6).coerceAtLeast(2), trim.copy(alpha = .30f))
            p(rightLegX + 1, legY + 2, 1, (legH - 6).coerceAtLeast(2), trim.copy(alpha = .30f))
        }
    }
}
