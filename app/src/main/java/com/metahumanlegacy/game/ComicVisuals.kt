package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.math.absoluteValue

internal enum class ComicAsset(val atlas: Int, val col: Int) {
    BRAND_HERO(1, 0), BRAND_VILLAIN(1, 1), ROUTE_CARE(1, 2), ROUTE_ORDER(1, 3), ROUTE_TRUTH(1, 4),
    ROUTE_ASCEND(2, 0), MORALITY_HERO(2, 1), MORALITY_NEUTRAL(2, 2), MORALITY_VILLAIN(2, 3), RANK_BRONZE(2, 4),
    RANK_GOLD(3, 0), RANK_LEGEND(3, 1), SCOPE_STREET(3, 2), SCOPE_DISTRICT(3, 3), SCOPE_CITY(3, 4),
    SCOPE_REGION(4, 0), SCOPE_COUNTRY(4, 1), SCOPE_WORLD(4, 2), DANGER_LOW(4, 3), DANGER_HIGH(4, 4),
    DANGER_EXTREME(5, 0), POWER_STRENGTH(5, 1), POWER_SPEED(5, 2), POWER_SENSES(5, 3), POWER_FIRE(5, 4),
    POWER_ICE(6, 0), POWER_WATER(6, 1), POWER_WIND(6, 2), POWER_ENERGY(6, 3), POWER_COSMIC(6, 4),
    POWER_FORCEFIELD(7, 0), POWER_TIME(7, 1), ORIGIN_PSYCHIC(7, 2), ORIGIN_MYSTIC(7, 3), ORIGIN_UNKNOWN(7, 4),
    RELATION_FAMILY(8, 0), RELATION_RIVAL(8, 1), RELATION_MEDIA(8, 2), PUBLIC_FEAR(8, 3), FACTION_HERO(8, 4),
    ALT_01(9, 0), ALT_02(9, 1), ALT_03(9, 2), ALT_04(9, 3), ALT_05(9, 4),
    ALT_06(10, 0), ALT_07(10, 1), ALT_08(10, 2), ALT_09(10, 3), ALT_10(10, 4)
}

private object ComicAtlasStore {
    private val cache = mutableMapOf<Int, ImageBitmap>()
    private var loaded = false
    private const val expectedSha256 = "3c6314b5ff27e2c23052321bee4ad844e5c0779c09f1aca0e15bbfaf09f9d9d4"

    @Synchronized
    fun image(context: Context, atlas: Int): ImageBitmap {
        if (!loaded) load(context)
        return cache[atlas] ?: error("Comic atlas $atlas missing")
    }

    private fun load(context: Context) {
        val encoded = buildString(765_000) {
            for (i in 1..32) {
                val name = "comic_bundle/cb_${i.toString().padStart(2, '0')}.b64"
                append(context.assets.open(name).bufferedReader(Charsets.US_ASCII).use { it.readText().trim() })
            }
        }
        val zipBytes = Base64.decode(encoded, Base64.DEFAULT)
        val digest = MessageDigest.getInstance("SHA-256").digest(zipBytes).joinToString("") { "%02x".format(it) }
        require(digest == expectedSha256) { "Comic visual bundle SHA-256 mismatch" }

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.startsWith("comic_atlas_") && entry.name.endsWith(".webp")) {
                    val out = ByteArrayOutputStream()
                    zip.copyTo(out)
                    val bytes = out.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: error("Unable to decode ${entry.name}")
                    val index = entry.name.removePrefix("comic_atlas_").removeSuffix(".webp").toInt()
                    cache[index] = bitmap.asImageBitmap()
                }
                zip.closeEntry()
            }
        }
        require(cache.size == 10) { "Expected 10 comic atlases, got ${cache.size}" }
        loaded = true
    }
}

@Composable
internal fun ComicIcon(
    asset: ComicAsset,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val context = LocalContext.current
    val image = remember(asset.atlas) { ComicAtlasStore.image(context.applicationContext, asset.atlas) }
    Canvas(modifier) {
        drawImage(
            image = image,
            srcOffset = IntOffset(asset.col * 160, 0),
            srcSize = IntSize(160, 160),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
            alpha = alpha,
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
internal fun CampaignSigil(seed: Long, modifier: Modifier = Modifier) {
    ComicIcon(variantAsset(seed), modifier)
}

internal fun variantAsset(seed: Long): ComicAsset {
    val variants = listOf(
        ComicAsset.ALT_01, ComicAsset.ALT_02, ComicAsset.ALT_03, ComicAsset.ALT_04, ComicAsset.ALT_05,
        ComicAsset.ALT_06, ComicAsset.ALT_07, ComicAsset.ALT_08, ComicAsset.ALT_09, ComicAsset.ALT_10
    )
    return variants[(seed % variants.size).toInt().absoluteValue]
}

internal fun routeAsset(approach: String): ComicAsset = when (approach.uppercase()) {
    "CARE", "PROTECT", "HUMAN" -> ComicAsset.ROUTE_CARE
    "ORDER", "TACTICAL", "CONTROL" -> ComicAsset.ROUTE_ORDER
    "TRUTH", "ANALYZE", "REVEAL" -> ComicAsset.ROUTE_TRUTH
    "ASCEND", "AMBITION", "DOMINATE" -> ComicAsset.ROUTE_ASCEND
    else -> ComicAsset.BRAND_HERO
}

internal fun scopeAsset(scope: Scope): ComicAsset = when (scope) {
    Scope.STREET -> ComicAsset.SCOPE_STREET
    Scope.DISTRICT -> ComicAsset.SCOPE_DISTRICT
    Scope.CITY -> ComicAsset.SCOPE_CITY
    Scope.REGION -> ComicAsset.SCOPE_REGION
    Scope.COUNTRY -> ComicAsset.SCOPE_COUNTRY
    Scope.WORLD -> ComicAsset.SCOPE_WORLD
}

internal fun moralityAsset(c: Campaign): ComicAsset = when {
    c.morality >= 28 && c.fear < 55 -> ComicAsset.MORALITY_HERO
    c.morality <= -28 || c.fear >= 70 -> ComicAsset.MORALITY_VILLAIN
    else -> ComicAsset.MORALITY_NEUTRAL
}

internal fun dangerAsset(risk: Int): ComicAsset = when {
    risk >= 7 -> ComicAsset.DANGER_EXTREME
    risk >= 4 -> ComicAsset.DANGER_HIGH
    else -> ComicAsset.DANGER_LOW
}

internal fun rankAsset(prestige: Int): ComicAsset = when {
    prestige >= 75 -> ComicAsset.RANK_LEGEND
    prestige >= 35 -> ComicAsset.RANK_GOLD
    else -> ComicAsset.RANK_BRONZE
}

internal fun powerAsset(powerFamily: String): ComicAsset {
    val p = powerFamily.lowercase()
    return when {
        "feu" in p || "therm" in p || "flamme" in p -> ComicAsset.POWER_FIRE
        "glace" in p || "froid" in p || "cryo" in p -> ComicAsset.POWER_ICE
        "eau" in p || "hydro" in p || "océan" in p -> ComicAsset.POWER_WATER
        "air" in p || "vent" in p || "aéro" in p -> ComicAsset.POWER_WIND
        "vitesse" in p || "rapid" in p -> ComicAsset.POWER_SPEED
        "force" in p || "phys" in p || "densité" in p -> ComicAsset.POWER_STRENGTH
        "sens" in p || "vision" in p || "perception" in p -> ComicAsset.POWER_SENSES
        "temps" in p || "chron" in p -> ComicAsset.POWER_TIME
        "bouclier" in p || "champ" in p || "barrière" in p -> ComicAsset.POWER_FORCEFIELD
        "cosm" in p || "grav" in p || "espace" in p -> ComicAsset.POWER_COSMIC
        "psych" in p || "mental" in p || "télépath" in p -> ComicAsset.ORIGIN_PSYCHIC
        "mag" in p || "myst" in p || "occul" in p -> ComicAsset.ORIGIN_MYSTIC
        else -> ComicAsset.POWER_ENERGY
    }
}

internal fun relationAsset(kind: String): ComicAsset = when (kind.uppercase()) {
    "FAMILY" -> ComicAsset.RELATION_FAMILY
    "RIVAL" -> ComicAsset.RELATION_RIVAL
    "MEDIA" -> ComicAsset.RELATION_MEDIA
    "GOVERNMENT", "FACTION" -> ComicAsset.FACTION_HERO
    else -> ComicAsset.RELATION_FAMILY
}

@Composable
internal fun ComicWorldBackdrop(scope: Scope, seed: Long, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF080A0E))
        val blue = Color(0xFF0D2A48)
        val ink = Color(0xFF020305)
        val gold = Color(0x33F4C44E)
        val red = Color(0x221E0808)

        val horizon = size.height * .70f
        val buildings = 11
        val step = size.width / buildings
        repeat(buildings) { i ->
            val h = size.height * (.06f + (((i * 37 + scope.ordinal * 19 + (seed % 23).toInt()) % 17) / 100f))
            drawRect(
                color = blue.copy(alpha = .30f),
                topLeft = Offset(i * step, horizon - h),
                size = Size(step - 3f, h)
            )
        }

        val slash = Path().apply {
            moveTo(size.width * .63f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * .48f)
            lineTo(size.width * .84f, size.height * .36f)
            close()
        }
        drawPath(slash, red)

        val beam = Path().apply {
            moveTo(-size.width * .1f, size.height * .15f)
            lineTo(size.width * .58f, 0f)
            lineTo(size.width * .66f, 0f)
            lineTo(0f, size.height * .23f)
            close()
        }
        drawPath(beam, gold)

        val spacing = 18f
        var y = 24f
        while (y < size.height) {
            var x = if (((y / spacing).toInt() and 1) == 0) 12f else 21f
            while (x < size.width) {
                val fade = (1f - y / size.height).coerceIn(0f, 1f)
                drawCircle(Color.White.copy(alpha = .018f * fade), radius = 1.7f, center = Offset(x, y))
                x += spacing
            }
            y += spacing
        }

        drawLine(ink.copy(alpha = .9f), Offset(0f, size.height * .28f), Offset(size.width, size.height * .20f), strokeWidth = 5f)
        drawLine(Color(0x225CA9E6), Offset(0f, size.height * .285f), Offset(size.width, size.height * .205f), strokeWidth = 1.2f)
    }
}

@Composable
internal fun ComicBurst(modifier: Modifier = Modifier, accent: Color = Color(0xFFF4C44E)) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        repeat(24) { i ->
            val a = Math.toRadians((i * 15.0))
            val inner = size.minDimension * .34f
            val outer = size.minDimension * if (i % 2 == 0) .50f else .43f
            drawLine(
                accent.copy(alpha = .18f),
                Offset(cx + kotlin.math.cos(a).toFloat() * inner, cy + kotlin.math.sin(a).toFloat() * inner),
                Offset(cx + kotlin.math.cos(a).toFloat() * outer, cy + kotlin.math.sin(a).toFloat() * outer),
                strokeWidth = if (i % 2 == 0) 2.2f else 1.2f
            )
        }
    }
}
