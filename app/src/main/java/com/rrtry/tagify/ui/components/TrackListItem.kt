package com.rrtry.tagify.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.util.resolveThumbnail

const val THUMBNAIL_CACHE_KEY_PREFIX = "THUMBNAIL"

enum class Style {
    TRACK_LISTING, ALBUM_DETAILS, ARTIST_DETAILS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    track: Track,
    style: Style,
    selected: Boolean,
    onItemClick: (track: Track) -> Unit,
    onItemLongClick: (track: Track) -> Unit)
{
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(0.5f) else Color.Transparent)
            .combinedClickable(
                onClick = { onItemClick(track) },
                onLongClick = { onItemLongClick(track) }
            ),
        verticalAlignment = Alignment.CenterVertically)
    {
        if (style == Style.TRACK_LISTING) {

            val cacheKey  = "$THUMBNAIL_CACHE_KEY_PREFIX:${track.trackId}"
            val thumbnail = LocalContext.current.resolveThumbnail(track.trackId)

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(cacheKey)
                    .data(thumbnail)
                    .crossfade(true)
                    .placeholder(R.drawable.music)
                    .build(),
                null,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .size(50.dp)
            )
            Column {
                Text(track.title,
                    modifier = Modifier.padding(end = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text("${track.artist} • ${track.album}",
                    modifier = Modifier.padding(end = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        } else {
            Text(if (track.trkPos != 0) track.trkPos.toString() else "-",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.titleMedium
            )
            Column {
                Text(track.title,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                var subtitle = "%02d:%02d"
                if (style == Style.ALBUM_DETAILS) {
                    subtitle = ("$subtitle • %s").format(
                        track.duration / 60,
                        track.duration % 60,
                        track.artist
                    )
                } else if (track.discPos != 0) {
                    subtitle = ("$subtitle • %s: %d").format(
                        track.duration / 60,
                        track.duration % 60,
                        stringResource(id = R.string.disc_number),
                        track.discPos
                    )
                } else {
                    subtitle = subtitle.format(
                        track.duration / 60,
                        track.duration % 60
                    )
                }
                Text(subtitle,
                    modifier = Modifier.padding(start = 16.dp, end = 5.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}