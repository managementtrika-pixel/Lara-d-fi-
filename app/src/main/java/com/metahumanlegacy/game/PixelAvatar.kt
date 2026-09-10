package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor

private fun pxSkin(name: String): Color = when (name) {
    "Très clair" -> Color(0xFFF3D2BD); "Clair" -> Color(0xFFE8BFA4); "Moyen" -> Color(0xFFC88E67)
    "Mat" -> Color(0xFFAA714F); "Foncé" -> Color(0xFF794A34); "Très foncé" -> Color(0xFF4C2B22); else -> Color(0xFFC88E67)
}
private fun pxHair(name: String): Color = when (name) {
    "Noir" -> Color(0xFF111318); "Brun" -> Color(0xFF3B261E); "Châtain" -> Color(0xFF6A4630); "Blond" -> Color(0xFFD4B06A)
    "Roux" -> Color(0xFFA9502B); "Gris" -> Color(0xFF9699A0); "Blanc" -> Color(0xFFE8E5DE); else -> Color(0xFF3B261E)
}
private fun pxOutfit(style: String): Pair<Color, Color> = when {
    style.contains("Sport", true) -> Color(0xFF2E6CA4) to Color(0xFFB9D9FF)
    style.contains("Class", true) -> Color(0xFF27303A) to Color(0xFFD7DFEA)
    style.contains("Créat", true) -> Color(0xFF7A3F7E) to Color(0xFFE3B7F0)
    style.contains("Profession", true) -> Color(0xFF3B4D68) to Color(0xFFC9D7E8)
    style.contains("Vintage", true) -> Color(0xFF795B3E) to Color(0xFFE1C49F)
    else -> Color(0xFF2A415D) to Color(0xFF8AB4E8)
}
private fun pxHero(power: String): Pair<Color, Color> = when (power) {
    "Énergie" -> Color(0xFF205BD7) to Color(0xFF78D7FF); "Force" -> Color(0xFF8D2834) to Color(0xFFFFC85B)
    "Vitesse" -> Color(0xFF183D7A) to Color(0xFF66E5FF); "Télékinésie" -> Color(0xFF55338E) to Color(0xFFC69BFF)
    "Élémentaire" -> Color(0xFF2B6B54) to Color(0xFF9BE88B); "Mental" -> Color(0xFF44327D) to Color(0xFFE0B4FF)
    "Technologique" -> Color(0xFF273D52) to Color(0xFF61D5E8); else -> Color(0xFF28384E) to Color(0xFFE6C35A)
}

internal fun pixelLegHeight(age: Int, stature: String): Int {
    val base = if (age < 13) 5 else 6
    return (base + when (stature) { "Petite" -> -1; "Grande" -> 1; else -> 0 }).coerceIn(4, 7)
}
internal fun pixelAgeTier(age: Int): Int = when { age < 18 -> 0; age < 35 -> 1; age < 50 -> 2; age < 65 -> 3; else -> 4 }

@Composable
internal fun PixelAvatar(
    state: UltimateState,
    modifier: Modifier = Modifier,
    age: Int = 18,
    temperament: String = "Prudent",
    heroMode: Boolean = false,
    powerFamily: String = ""
) {
    Canvas(modifier) {
        // 32x40 native logical canvas: enough facial identity for modern mobile pixel art,
        // still scaled on integer cells so every edge remains deliberately crisp.
        val cols = 32; val rows = 40
        val cell = floor(minOf(size.width / cols, size.height / rows)).coerceAtLeast(1f)
        val ox = floor((size.width - cols * cell) / 2f); val oy = floor((size.height - rows * cell) / 2f)
        fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color) = drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))

        val skin = pxSkin(state.skinTone); val skinShade = skin.copy(red = skin.red * .82f, green = skin.green * .82f, blue = skin.blue * .82f)
        val hair = if (age >= 65 && state.hairColor !in listOf("Blanc", "Gris")) Color(0xFFB9BBC0) else pxHair(state.hairColor)
        val (civilMain, civilTrim) = pxOutfit(state.civilianStyle); val hero = pxHero(powerFamily)
        val shirt = if (heroMode) hero.first else civilMain; val trim = if (heroMode) hero.second else civilTrim
        val outline = Color(0xFF0B1017); val white = Color(0xFFF1F3F4)
        val eye = when (state.eyes) { "Bleus" -> Color(0xFF4B8BC4); "Verts" -> Color(0xFF5A8D62); "Noisette" -> Color(0xFF8B693D); "Gris" -> Color(0xFF87929D); "Très sombres" -> Color(0xFF17191C); else -> Color(0xFF49362C) }
        val child = age < 13; val teen = age in 13..17
        val headW = when (state.faceShape) { "Fin" -> 12; "Rond" -> 16; "Carré" -> 15; "Anguleux" -> 14; else -> 14 }
        val headX = (cols - headW) / 2; val headY = if (child) 4 else 3
        val torsoY = if (child) 22 else if (teen) 20 else 19
        val torsoH = if (child) 8 else if (teen) 10 else 11
        val torsoW = when (state.bodyBuild) { "Fin" -> 12; "Massif" -> 20; "Robuste" -> 18; "Souple" -> 13; else -> 15 } - if (child) 2 else 0
        val torsoX = (cols - torsoW) / 2; val legH = pixelLegHeight(age, state.stature)

        // Ground and readable stance.
        p(7, 38, 18, 1, Color.Black.copy(alpha = .34f))
        val legY = torsoY + torsoH - 1
        p(torsoX + 2, legY, 4, legH, outline); p(torsoX + 3, legY, 3, legH - 1, Color(0xFF182331))
        p(torsoX + torsoW - 6, legY, 4, legH, outline); p(torsoX + torsoW - 5, legY, 3, legH - 1, Color(0xFF182331))
        p(torsoX + 1, (legY + legH - 1).coerceAtMost(38), 6, 2, Color(0xFF080B10)); p(torsoX + torsoW - 6, (legY + legH - 1).coerceAtMost(38), 6, 2, Color(0xFF080B10))

        // Layered torso: outline, base, highlight, seam. This gives clothing volume without anti-aliasing.
        p(torsoX, torsoY, torsoW, torsoH, outline); p(torsoX + 1, torsoY + 1, torsoW - 2, torsoH - 2, shirt)
        p(torsoX + 2, torsoY + 2, (torsoW - 4).coerceAtLeast(3), 1, Color.White.copy(alpha = .11f))
        p(torsoX + 1, torsoY + torsoH - 2, torsoW - 2, 1, trim.copy(alpha = .75f))
        if (heroMode) {
            p(torsoX + torsoW / 2 - 2, torsoY + 3, 4, 4, trim.copy(alpha = .85f))
            p(torsoX + torsoW / 2 - 1, torsoY + 4, 2, 2, shirt)
        } else when {
            state.civilianStyle.contains("Sport", true) -> p(torsoX + torsoW / 2, torsoY + 2, 1, torsoH - 4, trim.copy(alpha = .7f))
            state.civilianStyle.contains("Class", true) || state.civilianStyle.contains("Profession", true) -> { p(torsoX + torsoW / 2, torsoY + 2, 1, torsoH - 4, trim); p(torsoX + torsoW / 2 - 1, torsoY + 6, 3, 1, trim) }
            state.civilianStyle.contains("Créat", true) -> { p(torsoX + 2, torsoY + 4, 3, 2, trim); p(torsoX + torsoW - 5, torsoY + 3, 3, 3, trim.copy(alpha = .7f)) }
            state.civilianStyle.contains("Vintage", true) -> { p(torsoX + 2, torsoY + 4, torsoW - 4, 1, Color(0xFF4D3425)); p(torsoX + 3, torsoY + 7, torsoW - 6, 1, Color(0xFF4D3425)) }
        }

        // Arms and hands follow body width, so silhouette choices are obvious even as thumbnails.
        val armH = torsoH - 1
        p((torsoX - 3).coerceAtLeast(1), torsoY + 1, 3, armH, outline); p((torsoX - 2).coerceAtLeast(2), torsoY + 2, 2, armH - 2, shirt); p((torsoX - 2).coerceAtLeast(2), torsoY + armH - 1, 2, 2, skin)
        p((torsoX + torsoW).coerceAtMost(29), torsoY + 1, 3, armH, outline); p((torsoX + torsoW).coerceAtMost(29), torsoY + 2, 2, armH - 2, shirt); p((torsoX + torsoW).coerceAtMost(29), torsoY + armH - 1, 2, 2, skin)

        // Neck + face with a two-tone pixel ramp.
        p(13, 16, 6, 5, outline); p(14, 16, 4, 5, skin)
        p(headX, headY, headW, 13, outline); p(headX + 1, headY + 1, headW - 2, 11, skin)
        p(headX + 1, headY + 9, 2, 2, skinShade.copy(alpha = .55f)); p(headX + headW - 3, headY + 9, 2, 2, skinShade.copy(alpha = .55f))
        if (state.faceShape == "Fin") { p(headX + 1, headY + 10, 2, 2, outline); p(headX + headW - 3, headY + 10, 2, 2, outline) }
        if (state.faceShape == "Carré") { p(headX, headY + 9, 2, 3, outline); p(headX + headW - 2, headY + 9, 2, 3, outline) }
        p(headX - 1, headY + 5, 1, 4, skinShade); p(headX + headW, headY + 5, 1, 4, skinShade)

        // Separate eyes / irises / brows: higher identity density than the old 20x30 renderer.
        val eyeY = headY + 6; val lx = headX + 3; val rx = headX + headW - 6
        p(lx, eyeY, 3, 2, white); p(rx, eyeY, 3, 2, white); p(lx + 1, eyeY, 1, 2, eye); p(rx + 1, eyeY, 1, 2, eye)
        p(lx + 1, eyeY, 1, 1, Color.White.copy(alpha = .55f)); p(rx + 1, eyeY, 1, 1, Color.White.copy(alpha = .55f))
        val browYLeft = headY + if (temperament == "Curieux") 4 else 5; val browYRight = headY + if (temperament == "Méfiant") 4 else 5
        p(lx, browYLeft, 3, 1, hair); p(rx, browYRight, 3, 1, hair)
        if (temperament in listOf("Impulsif", "Ambitieux")) { p(lx + 2, headY + 4, 2, 1, hair); p(rx - 1, headY + 4, 2, 1, hair) }

        // Nose, mouth and age marks.
        p(headX + headW / 2, headY + 7, 1, 3, skinShade.copy(alpha = .72f)); p(headX + headW / 2 - 1, headY + 9, 2, 1, skinShade.copy(alpha = .55f))
        val mouth = Color(0xFF713B3B); p(headX + 4, headY + 11, (headW - 8).coerceAtLeast(3), 1, mouth)
        when (pixelAgeTier(age)) {
            2 -> { p(headX + 2, headY + 9, 1, 1, outline.copy(alpha = .28f)); p(headX + headW - 3, headY + 9, 1, 1, outline.copy(alpha = .28f)) }
            3 -> { p(headX + 2, headY + 8, 1, 2, outline.copy(alpha = .36f)); p(headX + headW - 3, headY + 8, 1, 2, outline.copy(alpha = .36f)); p(headX + 5, headY + 12, headW - 10, 1, outline.copy(alpha = .20f)) }
            4 -> { p(headX + 2, headY + 8, 1, 3, outline.copy(alpha = .42f)); p(headX + headW - 3, headY + 8, 1, 3, outline.copy(alpha = .42f)); p(headX + 4, headY + 12, headW - 8, 1, outline.copy(alpha = .28f)) }
        }

        drawPremiumHair(state.hair, hair, headX, headY, headW, cell, ox, oy)
        if (age >= 16 && state.facialHair != "Aucune") when (state.facialHair) {
            "Moustache" -> p(headX + 4, headY + 10, headW - 8, 1, hair)
            "Bouc" -> { p(headX + 5, headY + 10, headW - 10, 1, hair); p(headX + headW / 2 - 1, headY + 11, 2, 2, hair) }
            "Barbe courte" -> { p(headX + 2, headY + 10, headW - 4, 2, hair.copy(alpha = .82f)); p(headX + 4, headY + 12, headW - 8, 1, hair.copy(alpha = .82f)) }
            else -> { p(headX + 1, headY + 9, headW - 2, 3, hair.copy(alpha = .92f)); p(headX + 3, headY + 12, headW - 6, 2, hair.copy(alpha = .92f)) }
        }

        // Accessories are independent paper-doll layers and therefore persist everywhere.
        when {
            state.accessory.contains("Lun", true) -> { p(lx - 1, eyeY - 1, 5, 4, outline); p(rx - 1, eyeY - 1, 5, 4, outline); p(lx, eyeY, 3, 2, Color(0xFF5C7FA1)); p(rx, eyeY, 3, 2, Color(0xFF5C7FA1)); p(lx + 4, eyeY, (rx - lx - 4).coerceAtLeast(1), 1, outline) }
            state.accessory.contains("Casquette", true) -> { p(headX, headY - 2, headW, 3, Color(0xFF384F6C)); p(headX + headW - 3, headY + 1, 5, 1, Color(0xFF384F6C)) }
            state.accessory.contains("Bonnet", true) -> { p(headX, headY - 3, headW, 4, Color(0xFF6A4A70)); p(headX + 2, headY - 3, headW - 4, 1, Color.White.copy(alpha = .10f)) }
            state.accessory.contains("Boucle", true) -> p(headX - 2, headY + 8, 1, 3, Color(0xFFE0B94F))
            state.accessory.contains("Chaîne", true) -> { p(torsoX + 4, torsoY + 1, torsoW - 8, 1, Color(0xFFD3B15A)); p(15, torsoY + 2, 2, 2, Color(0xFFD3B15A)) }
            state.accessory.contains("Montre", true) -> p((torsoX + torsoW + 1).coerceAtMost(30), torsoY + 7, 2, 2, Color(0xFFC8B56A))
        }

        // Hero equipment overlays the same body rather than swapping the player's identity.
        if (heroMode) when (state.maskStyle) {
            "Demi-masque", "Masque minimal" -> { p(lx - 1, eyeY - 1, 5, 3, trim.copy(alpha = .88f)); p(rx - 1, eyeY - 1, 5, 3, trim.copy(alpha = .88f)); p(lx, eyeY, 3, 1, eye); p(rx, eyeY, 3, 1, eye) }
            "Visière" -> { p(headX + 2, eyeY - 1, headW - 4, 4, trim.copy(alpha = .82f)); p(headX + 3, eyeY, headW - 6, 1, Color.White.copy(alpha = .25f)) }
            "Masque intégral", "Casque" -> { p(headX, headY + 1, 2, 10, trim.copy(alpha = .72f)); p(headX + headW - 2, headY + 1, 2, 10, trim.copy(alpha = .72f)); p(headX + 2, headY + 1, headW - 4, 2, trim.copy(alpha = .72f)) }
            "Capuche" -> { p(headX - 2, headY - 2, headW + 4, 2, shirt); p(headX - 2, headY, 2, 12, shirt); p(headX + headW, headY, 2, 12, shirt) }
        }
        if (heroMode && state.signatureItem == "Manteau") { p((torsoX - 2).coerceAtLeast(1), torsoY + 2, 2, torsoH + 5, shirt.copy(alpha = .88f)); p((torsoX + torsoW).coerceAtMost(29), torsoY + 2, 2, torsoH + 5, shirt.copy(alpha = .88f)) }
    }
}

private fun DrawScope.drawPremiumHair(style: String, color: Color, x: Int, y: Int, w: Int, cell: Float, ox: Float, oy: Float) {
    fun p(px: Int, py: Int, pw: Int = 1, ph: Int = 1, c: Color = color) = drawRect(c, Offset(ox + px * cell, oy + py * cell), Size(pw * cell, ph * cell))
    val hi = color.copy(red = (color.red * 1.18f).coerceAtMost(1f), green = (color.green * 1.18f).coerceAtMost(1f), blue = (color.blue * 1.18f).coerceAtMost(1f))
    when (style) {
        "Rasé" -> { p(x + 1, y - 1, w - 2, 2); p(x + 3, y - 1, w - 6, 1, hi) }
        "Long" -> { p(x, y - 2, w, 4); p(x, y + 2, 3, 12); p(x + w - 3, y + 2, 3, 12); p(x + 3, y - 2, w - 6, 1, hi) }
        "Tresses" -> { p(x, y - 2, w, 3); repeat(5) { i -> p(x + 1 + i * ((w - 2) / 5).coerceAtLeast(2), y + 1, 1, 12) }; p(x + 2, y - 2, w - 4, 1, hi) }
        "Boucles" -> { p(x, y - 1, w, 3); repeat(7) { i -> p(x - 1 + (i * 2) % (w + 1), y - 3 + i % 2, 3, 3, if (i % 3 == 0) hi else color) } }
        "Undercut" -> { p(x + 3, y - 3, w - 3, 4); p(x + w - 3, y, 3, 3); p(x + 5, y - 3, w - 6, 1, hi) }
        "Attaché" -> { p(x, y - 2, w, 3); p(x + w - 1, y + 1, 3, 4); p(x + w, y + 4, 2, 3); p(x + 2, y - 2, w - 4, 1, hi) }
        "Dégradé" -> { p(x + 1, y - 2, w - 2, 3); p(x, y + 1, 2, 3, color.copy(alpha = .65f)); p(x + w - 2, y + 1, 2, 3, color.copy(alpha = .65f)); p(x + 4, y - 2, w - 7, 1, hi) }
        else -> { p(x, y - 2, w, 3); p(x + 1, y + 1, w - 2, 2); p(x + 3, y - 2, w - 6, 1, hi) }
    }
}
