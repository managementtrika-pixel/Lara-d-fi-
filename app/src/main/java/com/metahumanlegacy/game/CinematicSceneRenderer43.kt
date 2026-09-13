package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

internal data class CinematicScene43Profile(
    val night: Boolean,
    val rain: Boolean,
    val crisis: Boolean,
    val awakening: Boolean,
    val skylineLayers: Int,
    val practicalLights: Int,
    val sceneHeightDp: Int
)

internal object CinematicScene43 {
    fun profile(c: Campaign, state: UltimateState): CinematicScene43Profile {
        val climate = state.climate.lowercase()
        val mood = state.cityMood.lowercase()
        val rain = listOf("pluie", "orage", "humide", "rain").any(climate::contains)
        val crisis = state.cityCondition < 45 || listOf("crise", "tension", "chaos", "peur").any(mood::contains)
        val awakening = c.turn == 10 && !c.powerRevealed
        val night = awakening || c.turn >= 7 || listOf("nuit", "sombre").any(mood::contains)
        return CinematicScene43Profile(
            night = night,
            rain = rain,
            crisis = crisis,
            awakening = awakening,
            skylineLayers = if (awakening || crisis) 4 else 3,
            practicalLights = if (night) 11 else 6,
            sceneHeightDp = if (awakening) 330 else 285
        )
    }

    fun rainDropCount(profile: CinematicScene43Profile, reduceMotion: Boolean): Int =
        if (!profile.rain || reduceMotion) 0 else 34

    fun parallaxDepths(profile: CinematicScene43Profile): List<Float> =
        List(profile.skylineLayers) { index -> 0.18f + index * 0.17f }
}

@Composable
internal fun CinematicLifeScene43(
    c: Campaign,
    state: UltimateState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val scene = CinematicScene43.profile(c, state)
    val motion = LocalMetahumanMotion.current.settings
    val heroMode = c.powerRevealed
    Box(
        modifier.fillMaxWidth().height(scene.sceneHeightDp.dp)
            .background(if (scene.night) Color(0xFF03070C) else Color(0xFF102438))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val sky = if (scene.night) {
                Brush.verticalGradient(listOf(Color(0xFF06111E), Color(0xFF0B1724), Color(0xFF03070C)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF173B55), Color(0xFF27485A), Color(0xFF101A22)))
            }
            drawRect(sky)

            val horizon = size.height * .67f
            val depths = CinematicScene43.parallaxDepths(scene)
            depths.forEachIndexed { layer, depth ->
                val count = 7 + layer * 3
                val cell = size.width / (count - 1).coerceAtLeast(1)
                repeat(count) { i ->
                    val mixed = (c.seed + layer * 997L + i * 181L + c.turn * 71L).absoluteValue
                    val buildingHeight = size.height * (.10f + depth * .23f + (mixed % 23) / 180f)
                    val width = cell * (.70f + (mixed % 17) / 50f)
                    val x = i * cell - width * .35f
                    val y = horizon - buildingHeight
                    val shade = when (layer) {
                        0 -> Color(0xFF112435)
                        1 -> Color(0xFF0C1B29)
                        2 -> Color(0xFF08131E)
                        else -> Color(0xFF050D15)
                    }
                    drawRect(shade, Offset(x, y), Size(width, buildingHeight + size.height - horizon))

                    if (layer >= scene.skylineLayers - 2 && i % 2 == layer % 2) {
                        val light = if (scene.crisis && i % 3 == 0) UltimateRed else accent
                        drawRect(light.copy(alpha = .18f + depth * .12f), Offset(x + width * .28f, y + buildingHeight * .30f), Size(width * .10f, 3f))
                    }
                }
            }

            drawRect(Color(0xFF06090D), Offset(0f, horizon), Size(size.width, size.height - horizon))
            val roadPath = Path().apply {
                moveTo(size.width * .35f, size.height)
                lineTo(size.width * .47f, horizon)
                lineTo(size.width * .58f, horizon)
                lineTo(size.width * .78f, size.height)
                close()
            }
            drawPath(roadPath, Color(0xFF0E141B))
            drawLine(accent.copy(alpha = .16f), Offset(size.width * .53f, horizon), Offset(size.width * .56f, size.height), 2f)

            repeat(scene.practicalLights) { i ->
                val x = size.width * ((i + 1f) / (scene.practicalLights + 1f))
                val y = horizon - size.height * (.03f + (i % 3) * .025f)
                drawCircle(accent.copy(alpha = .055f), radius = size.minDimension * .055f, center = Offset(x, y))
                drawCircle(accent.copy(alpha = .42f), radius = 2.4f, center = Offset(x, y))
            }

            if (scene.rain) {
                repeat(CinematicScene43.rainDropCount(scene, motion.reduceMotion)) { i ->
                    val mixed = (c.seed + i * 131L + c.turn * 47L).absoluteValue
                    val x = (mixed % 1000) / 1000f * size.width
                    val y = ((mixed / 11L) % 1000) / 1000f * size.height
                    drawLine(Color(0x557DB8DA), Offset(x, y), Offset(x - 5f, y + 15f), 1.2f)
                }
            }

            if (scene.awakening) {
                val center = Offset(size.width * .73f, size.height * .47f)
                drawCircle(accent.copy(alpha = .07f), size.minDimension * .26f, center)
                drawCircle(accent.copy(alpha = .16f), size.minDimension * .18f, center, style = Stroke(width = 3f))
                drawCircle(accent.copy(alpha = .34f), size.minDimension * .11f, center, style = Stroke(width = 2f))
            }

            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent, Color(0xD9020509)),
                    startY = size.height * .42f,
                    endY = size.height
                )
            )
        }

        UltimatePortrait(
            c,
            state,
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 18.dp).width(112.dp).height(154.dp),
            heroMode = heroMode
        )

        Column(Modifier.align(Alignment.BottomStart).padding(start = 15.dp, end = 132.dp, bottom = 18.dp)) {
            Text(
                if (scene.awakening) "18 ANS · L'ÉVEIL" else "${c.age} ANS",
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp
            )
            Text(cinematicLocation43(c, state).uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 23.sp)
            Text(cinematicAtmosphere43(c, state, scene), color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

private fun cinematicLocation43(c: Campaign, state: UltimateState): String = when {
    c.turn < 3 -> "${c.district} · près de chez toi"
    c.turn < 7 -> "${c.city} · ${state.cityMood.lowercase()}"
    c.turn < 10 -> "${c.city} · fin de journée"
    c.turn == 10 -> "${c.city} · là où tout bascule"
    c.scope >= Scope.CITY -> "${c.city} · secteur sous tension"
    else -> "${c.district} · ton territoire"
}

private fun cinematicAtmosphere43(c: Campaign, state: UltimateState, scene: CinematicScene43Profile): String = when {
    scene.awakening -> "La ville continue de bouger. Pour toi, une seconde vient de durer beaucoup plus longtemps."
    scene.crisis -> "${state.climate} · la ville porte déjà les conséquences de ce qui s'y passe"
    c.health < 40 -> "Tu arrives diminué. Le décor n'attendra pas que tu récupères."
    else -> "${state.climate} · ${state.architecture.lowercase()} · ${state.cityMood.lowercase()}"
}
