package com.rrtry.tagify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

private val ARTWORK_SIZE = 200.dp

@Composable
fun ArtworkCard(
    title: String,
    subtitles: Array<String>,
    artworkData: Any?,
    cacheKey: String,
    onClick: () -> Unit)
{
    Column(modifier = Modifier
        .padding(4.dp)
        .clickable { onClick() })
    {
        AsyncImage(
            contentScale = ContentScale.Crop,
            model = ImageRequest.Builder(LocalContext.current)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCacheKey(cacheKey)
                .size(with(LocalDensity.current) { ARTWORK_SIZE.roundToPx() })
                .data(artworkData)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.CenterHorizontally)
                .size(ARTWORK_SIZE)
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(4.dp),
            text = title, maxLines = 1, style = MaterialTheme.typography.titleMedium
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp),
            text = "${subtitles[0]} • ${subtitles[1]}", maxLines = 1, style = MaterialTheme.typography.bodySmall
        )
    }
}