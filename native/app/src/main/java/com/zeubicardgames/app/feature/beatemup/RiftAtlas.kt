package com.zeubicardgames.app.feature.beatemup

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class RiftFrame(val x: Int, val y: Int, val w: Int, val h: Int)
internal data class RiftAtlas(val bitmap: ImageBitmap, val frames: Map<String, RiftFrame>)

@Composable
internal fun rememberRiftAtlas(): RiftAtlas? {
    val context = LocalContext.current
    val atlas by produceState<RiftAtlas?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = context.assets.open("rift/rift_sprites.webp").use(BitmapFactory::decodeStream).asImageBitmap()
                val json = context.assets.open("rift/sprites.json").bufferedReader().use { it.readText() }
                val items = JSONObject(json).getJSONObject("items")
                val map = buildMap {
                    val keys = items.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val item = items.getJSONObject(key)
                        put(key, RiftFrame(item.getInt("x"), item.getInt("y"), item.getInt("w"), item.getInt("h")))
                    }
                }
                RiftAtlas(bitmap, map)
            }.getOrNull()
        }
    }
    return atlas
}

internal fun DrawScope.drawRiftSprite(
    atlas: RiftAtlas,
    frameName: String,
    center: Offset,
    height: Float,
    facing: Int = 1,
    alpha: Float = 1f,
) {
    val frame = atlas.frames[frameName] ?: return
    val aspect = frame.w.toFloat() / frame.h.coerceAtLeast(1)
    val width = height * aspect
    val left = center.x - width / 2f
    val top = center.y - height
    withTransform({
        if (facing < 0) scale(-1f, 1f, pivot = Offset(center.x, center.y - height / 2f))
    }) {
        drawImage(
            image = atlas.bitmap,
            srcOffset = IntOffset(frame.x, frame.y),
            srcSize = IntSize(frame.w, frame.h),
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
            alpha = alpha,
        )
    }
}

@Composable
internal fun RiftFrameImage(
    atlas: RiftAtlas?,
    frameName: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val ready = atlas ?: return@Canvas
        val frame = ready.frames[frameName] ?: return@Canvas
        val frameAspect = frame.w.toFloat() / frame.h.coerceAtLeast(1)
        val boxAspect = size.width / size.height.coerceAtLeast(1f)
        val h: Float
        val w: Float
        if (frameAspect > boxAspect) { w = size.width; h = w / frameAspect } else { h = size.height; w = h * frameAspect }
        drawImage(
            image = ready.bitmap,
            srcOffset = IntOffset(frame.x, frame.y),
            srcSize = IntSize(frame.w, frame.h),
            dstOffset = IntOffset(((size.width - w) / 2f).toInt(), ((size.height - h) / 2f).toInt()),
            dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
        )
    }
}
