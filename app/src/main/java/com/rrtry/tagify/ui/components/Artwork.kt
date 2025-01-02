package com.rrtry.tagify.ui.components

import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.RoundedCornersTransformation

val artworkSize = 250.dp
val backgroundCoverHeight = 300.dp

@Composable
fun Artwork(artwork: Any?,
            gradientColor: Color,
            @DrawableRes placeholder: Int,
            cacheKey: String?,
            onArtworkClick: () -> Unit)
{
    Log.d("Artwork", "$artwork")
    Box(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .transformations(RoundedCornersTransformation(10f))
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .memoryCacheKey(if (cacheKey != null) "$ARTWORK_CACHE_KEY_PREFIX:$cacheKey" else null)
                .crossfade(true)
                .size {
                    with(density) { artworkSize.roundToPx() }.let {
                        Size(it, it)
                    }
                }
                .data(artwork)
                .build()
        )
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .clickable { onArtworkClick() }
                .align(Alignment.Center)
                .zIndex(2f)
                .clip(RoundedCornerShape(10f))
                .size(artworkSize)
        )
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                .height(backgroundCoverHeight)
                .drawWithCache {

                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            gradientColor,
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) Color.Black.copy(
                                alpha = 0.8f
                            ) else Color.Transparent,
                        ),
                        startY = size.height,
                        endY = size.height / 2f
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(gradient)
                    }
                }
        )
    }
}