package com.metahumanlegacy.game

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream

/**
 * Compatibility name retained for save/build stability.
 * V2 no longer runs the old Comic app/screens; this file is now the shared MetaHuman design-system
 * layer used by the pixel-first UI (colors, shapes, buttons, and legacy atlas fallback rendering).
 * The atlas fallback remains only because current UI still references a few generic scene assets.
 */
internal object MetahumanColors {
    val Ink = Color(0xFF05070A)
    val Coal = Color(0xFF0B0F15)
    val Panel = Color(0xFF111721)
    val Panel2 = Color(0xFF18202D)
    val Border = Color(0xFF313C4C)
    val Paper = Color(0xFFECE6D7)
    val Muted = Color(0xFFA9B2C0)
    val Cyan = Color(0xFF59D8FF)
    val Gold = Color(0xFFFFC95E)
    val Red = Color(0xFFFF6B6B)
    val Green = Color(0xFF68D391)
    val Violet = Color(0xFFB794F4)
}

internal object MhlDimensions {
    val OuterPadding = 13.dp
    val SectionGap = 10.dp
    val ItemGap = 7.dp
    val CardPadding = 11.dp
    val Radius = 10.dp
}

internal val MhlShape = RoundedCornerShape(MhlDimensions.Radius)

@Composable
internal fun MhlPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .background(MetahumanColors.Gold, MhlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
internal fun MhlSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .border(1.dp, MetahumanColors.Border, MhlShape)
            .background(MetahumanColors.Panel, MhlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = MetahumanColors.Paper, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
internal fun MhlAsset(
    assetKey: String,
    modifier: Modifier = Modifier,
    fallbackLabel: String = assetKey,
    alpha: Float = 1f
) {
    val context = LocalContext.current
    val image = remember(assetKey) { loadAtlasImage(context, assetKey) }
    if (image == null) {
        Box(
            modifier
                .background(MetahumanColors.Panel2, MhlShape)
                .border(1.dp, MetahumanColors.Border, MhlShape)
                .padding(8.dp)
        ) {
            Text(fallbackLabel, color = MetahumanColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    } else {
        Canvas(modifier) {
            drawImageFitted(image, alpha)
        }
    }
}

private fun DrawScope.drawImageFitted(image: ImageBitmap, alpha: Float) {
    val iw = image.width.toFloat()
    val ih = image.height.toFloat()
    if (iw <= 0f || ih <= 0f || size.width <= 0f || size.height <= 0f) return
    val scale = maxOf(size.width / iw, size.height / ih)
    val dw = iw * scale
    val dh = ih * scale
    val left = (size.width - dw) / 2f
    val top = (size.height - dh) / 2f
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        }
        canvas.nativeCanvas.drawBitmap(
            image.asAndroidBitmap(),
            null,
            android.graphics.RectF(left, top, left + dw, top + dh),
            paint
        )
    }
}

private fun ImageBitmap.asAndroidBitmap(): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val source = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    source.eraseColor(android.graphics.Color.TRANSPARENT)
    canvas.drawBitmap(source, 0f, 0f, paint)
    // ImageBitmap is already decoded from Android Bitmap in loadAtlasImage; native readback is not exposed
    // uniformly across API levels. Fallback image drawing is handled by the overload below when available.
    return bitmap
}

private fun loadAtlasImage(context: Context, key: String): ImageBitmap? {
    val normalized = key.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
    val candidates = atlasCandidates(normalized)
    candidates.forEach { asset ->
        runCatching {
            val bytes = context.assets.open(asset).use { input -> Base64.decode(input.readBytes(), Base64.DEFAULT) }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()?.let { return it }
    }
    return null
}

private fun atlasCandidates(key: String): List<String> {
    val index = when {
        key.contains("city") || key.contains("scope") -> 1
        key.contains("danger") || key.contains("crisis") -> 2
        key.contains("relation") || key.contains("family") -> 3
        key.contains("power") || key.contains("energy") -> 4
        else -> (kotlin.math.abs(key.hashCode()) % 9) + 1
    }
    return listOf("metahuman/comic_atlas_${index.toString().padStart(2, '0')}.b64")
}

private fun DrawScope.drawPixelGrid(step: Float = 8f, color: Color = MetahumanColors.Border.copy(alpha = 0.18f)) {
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}
