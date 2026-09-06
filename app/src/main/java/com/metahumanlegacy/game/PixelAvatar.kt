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
    style.contains("Sport", true) -> Color(0xFF2E6CA4) to Color(0xFFB9D9FF)
    style.contains("Class", true) -> Color(0xFF27303A) to Color(0xFFD7DFEA)
    style.contains("Créat", true) -> Color(0xFF7A3F7E) to Color(0xFFE3B7F0)
    style.contains("Profession", true) -> Color(0xFF3B4D68) to Color(0xFFC9D7E8)
    style.contains("Vintage", true) -> Color(0xFF795B3E) to Color(0xFFE1C49F)
    else -> Color(0xFF2A415D) to Color(0xFF8AB4E8)
}

@Composable
internal fun PixelAvatar(
    state: UltimateState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val cols = 20
        val rows = 30
        val rawCell = minOf(size.width / cols, size.height / rows)
        val cell = floor(rawCell).coerceAtLeast(1f)
        val ox = floor((size.width - cols * cell) / 2f)
        val oy = floor((size.height - rows * cell) / 2f)

        fun p(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color) {
            drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
        }

        val skin = pxSkin(state.skinTone)
        val hair = pxHair(state.hairColor)
        val (shirt, trim) = pxOutfit(state.civilianStyle)
        val outline = Color(0xFF121820)
        val white = Color(0xFFF4F4F0)
        val eyeDark = when (state.eyes) {
            "Bleus" -> Color(0xFF3E78A8)
            "Verts" -> Color(0xFF4E7D59)
            "Noisette" -> Color(0xFF7A5A34)
            else -> Color(0xFF342821)
        }

        // Legs / stance
        val legSpread = if (state.bodyBuild == "Massif") 1 else 0
        p(6 - legSpread, 22, 3 + legSpread, 6, Color(0xFF1A2532))
        p(11, 22, 3 + legSpread, 6, Color(0xFF1A2532))
        p(5 - legSpread, 27, 4 + legSpread, 2, Color(0xFF0D1117))
        p(11, 27, 4 + legSpread, 2, Color(0xFF0D1117))

        // Torso
        val torsoX = when (state.bodyBuild) {
            "Fin" -> 6
            "Massif" -> 3
            "Robuste" -> 4
            else -> 5
        }
        val torsoW = when (state.bodyBuild) {
            "Fin" -> 8
            "Massif" -> 14
            "Robuste" -> 12
            else -> 10
        }
        p(torsoX, 14, torsoW, 9, outline)
        p(torsoX + 1, 15, torsoW - 2, 7, shirt)
        p(torsoX + 1, 21, torsoW - 2, 1, trim.copy(alpha = .85f))
        // shirt highlight + archetype details
        p(torsoX + 2, 16, (torsoW - 4).coerceAtLeast(2), 1, Color.White.copy(alpha = .08f))
        when {
            state.civilianStyle.contains("Sport", true) -> {
                p(torsoX + torsoW / 2, 15, 1, 6, trim.copy(alpha = .75f))
            }
            state.civilianStyle.contains("Class", true) || state.civilianStyle.contains("Profession", true) -> {
                p(torsoX + torsoW / 2, 15, 1, 5, Color(0xFFB7C0CC))
                p(torsoX + torsoW / 2 - 1, 20, 3, 1, Color(0xFFB7C0CC))
            }
            state.civilianStyle.contains("Créat", true) -> {
                p(torsoX + 2, 18, 2, 2, trim.copy(alpha = .9f))
                p(torsoX + torsoW - 4, 17, 2, 3, trim.copy(alpha = .65f))
            }
            state.civilianStyle.contains("Vintage", true) -> {
                p(torsoX + 2, 17, torsoW - 4, 1, Color(0xFF4D3425))
                p(torsoX + 3, 19, (torsoW - 6).coerceAtLeast(2), 1, Color(0xFF4D3425))
            }
        }
        p(torsoX + 1, 22, torsoW - 2, 1, Color.Black.copy(alpha = .16f))

        // Arms
        p((torsoX - 2).coerceAtLeast(1), 15, 2, 7, outline)
        p((torsoX - 1).coerceAtLeast(2), 16, 1, 5, skin)
        p((torsoX + torsoW).coerceAtMost(18), 15, 2, 7, outline)
        p((torsoX + torsoW).coerceAtMost(18), 16, 1, 5, skin)

        // Neck
        p(8, 12, 4, 3, outline)
        p(9, 12, 2, 3, skin)

        // Head silhouette
        val headX = when (state.faceShape) {
            "Fin" -> 6
            "Rond" -> 5
            else -> 5
        }
        val headW = when (state.faceShape) {
            "Fin" -> 8
            "Rond" -> 10
            else -> 10
        }
        val headY = 3
        p(headX, headY, headW, 9, outline)
        p(headX + 1, headY + 1, headW - 2, 7, skin)
        if (state.faceShape == "Carré" || state.faceShape == "Anguleux") {
            p(headX + 1, headY + 7, 2, 1, outline.copy(alpha = .6f))
            p(headX + headW - 3, headY + 7, 2, 1, outline.copy(alpha = .6f))
        }

        // Ears
        p(headX - 1, 6, 1, 3, skin)
        p(headX + headW, 6, 1, 3, skin)

        // Eyes
        val eyeY = 7
        p(headX + 2, eyeY, 2, 1, white)
        p(headX + headW - 4, eyeY, 2, 1, white)
        p(headX + 3, eyeY, 1, 1, eyeDark)
        p(headX + headW - 3, eyeY, 1, 1, eyeDark)

        // Brows / expression
        when (state.eyes) {
            "Bleus" -> {
                p(headX + 2, 6, 2, 1, hair.copy(alpha = .95f))
                p(headX + headW - 4, 6, 2, 1, hair.copy(alpha = .95f))
            }
            "Verts" -> {
                p(headX + 2, 6, 2, 1, hair.copy(alpha = .9f))
                p(headX + headW - 4, 5, 2, 1, hair.copy(alpha = .9f))
            }
            "Noisette" -> {
                p(headX + 2, 5, 2, 1, hair.copy(alpha = .9f))
                p(headX + headW - 4, 6, 2, 1, hair.copy(alpha = .9f))
            }
            else -> {
                p(headX + 2, 6, 2, 1, hair.copy(alpha = .9f))
                p(headX + headW - 4, 6, 2, 1, hair.copy(alpha = .9f))
            }
        }

        // Nose + mouth / attitude
        p(headX + headW / 2, 8, 1, 2, skin.copy(alpha = .72f))
        val mouth = when {
            state.civilianStyle.contains("Créat", true) -> Color(0xFF8A4550)
            state.civilianStyle.contains("Sport", true) -> Color(0xFF6D3A36)
            else -> Color(0xFF6A3433)
        }
        when (state.eyes) {
            "Verts" -> {
                p(headX + 3, 10, headW - 6, 1, mouth)
                p(headX + headW - 4, 11, 1, 1, mouth)
            }
            "Bleus" -> {
                p(headX + 3, 10, headW - 6, 1, mouth)
                p(headX + 3, 11, 1, 1, mouth)
                p(headX + headW - 4, 11, 1, 1, mouth)
            }
            else -> p(headX + 3, 10, headW - 6, 1, mouth)
        }
        if (state.bodyBuild == "Massif") {
            p(headX + 2, 11, headW - 4, 1, Color.Black.copy(alpha = .10f))
        }

        // Hair
        drawPixelHair(state.hair, hair, headX, headY, headW, cell, ox, oy)

        // Facial hair
        if (state.facialHair != "Aucune") {
            when (state.facialHair) {
                "Moustache" -> p(headX + 3, 9, headW - 6, 1, hair)
                "Bouc" -> {
                    p(headX + 3, 9, headW - 6, 1, hair)
                    p(headX + headW / 2 - 1, 10, 2, 2, hair)
                }
                else -> {
                    p(headX + 1, 9, headW - 2, 2, hair.copy(alpha = .9f))
                    p(headX + 2, 11, headW - 4, 1, hair.copy(alpha = .9f))
                }
            }
        }

        // Accessory
        when {
            state.accessory.contains("Lun", true) -> {
                p(headX + 1, 7, 4, 2, Color(0xFF1A1F26))
                p(headX + headW - 5, 7, 4, 2, Color(0xFF1A1F26))
                p(headX + 5, 7, (headW - 10).coerceAtLeast(1), 1, Color(0xFF1A1F26))
                p(headX + 2, 7, 2, 1, Color(0xFF5C7FA1))
                p(headX + headW - 4, 7, 2, 1, Color(0xFF5C7FA1))
            }
            state.accessory.contains("Casquette", true) -> {
                p(headX, 2, headW, 2, Color(0xFF384F6C))
                p(headX + headW - 2, 4, 3, 1, Color(0xFF384F6C))
                p(headX + 1, 2, headW - 2, 1, Color.White.copy(alpha = .08f))
            }
            state.accessory.contains("Bonnet", true) -> {
                p(headX, 1, headW, 3, Color(0xFF6A4A70))
                p(headX + 1, 1, headW - 2, 1, Color.White.copy(alpha = .08f))
            }
            state.accessory.contains("Boucle", true) -> {
                p(headX - 1, 8, 1, 2, Color(0xFFE0B94F))
            }
            state.accessory.contains("Chaîne", true) -> {
                p(torsoX + 2, 16, (torsoW - 4).coerceAtLeast(2), 1, Color(0xFFD3B15A))
                p(torsoX + torsoW / 2, 17, 1, 2, Color(0xFFD3B15A))
            }
        }

        // Tiny floor shadow
        p(4, 29, 12, 1, Color.Black.copy(alpha = .28f))
    }
}

private fun DrawScope.drawPixelHair(
    style: String,
    color: Color,
    headX: Int,
    headY: Int,
    headW: Int,
    cell: Float,
    ox: Float,
    oy: Float
) {
    fun p(x: Int, y: Int, w: Int = 1, h: Int = 1) {
        drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(w * cell, h * cell))
    }
    when (style) {
        "Rasé" -> {
            p(headX + 1, headY, headW - 2, 1)
            p(headX + 2, headY + 1, headW - 4, 1)
        }
        "Long" -> {
            p(headX, headY - 1, headW, 3)
            p(headX, headY + 2, 2, 8)
            p(headX + headW - 2, headY + 2, 2, 8)
        }
        "Tresses" -> {
            p(headX, headY - 1, headW, 2)
            repeat(4) { i ->
                p(headX + 1 + i * 2, headY + 1, 1, 9)
            }
        }
        "Boucles" -> {
            p(headX, headY - 1, headW, 2)
            repeat(5) { i -> p(headX + i * 2, headY - 2 + (i % 2), 2, 2) }
        }
        "Undercut" -> {
            p(headX + 2, headY - 2, headW - 2, 2)
            p(headX + headW - 2, headY, 2, 2)
        }
        else -> {
            p(headX, headY - 1, headW, 2)
            p(headX + 1, headY + 1, headW - 2, 1)
        }
    }
}
