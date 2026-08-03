package com.zeubicardgames.app.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AssetImage(path: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = if (path.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { context.assets.open(path).use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    if (bitmap == null) Box(modifier.background(ZeubiSecondary))
    else Image(bitmap!!.asImageBitmap(), contentDescription, modifier, contentScale = contentScale)
}
