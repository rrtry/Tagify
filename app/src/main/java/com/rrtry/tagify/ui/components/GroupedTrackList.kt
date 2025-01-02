package com.rrtry.tagify.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.flow.MutableStateFlow

enum class GroupBy {
    ALBUM, DISC_NUMBER
}

@Composable
fun GroupedTrackList(
    selectedTracksState: MutableStateFlow<LinkedHashSet<Int>>,
    tracks: List<Track>?,
    onClick: (index: Int, track: Track) -> Unit,
    onLongClick: (index: Int, track: Track) -> Unit,
    groupBy: GroupBy,
    style:   Style)
{
    val selectedTracks by selectedTracksState.collectAsState()
    var album   = ""
    var discPos = 0

    (tracks ?: listOf()).fastForEachIndexed { i, track ->
        if (groupBy == GroupBy.DISC_NUMBER) {
            if (track.discPos != discPos && track.discPos != 0) {
                discPos = track.discPos
                Text(
                    modifier = Modifier.padding(16.dp),
                    text  = "${stringResource(id = R.string.disc_number)}: $discPos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (groupBy == GroupBy.ALBUM && track.album != album) {
            album = track.album
            Text(
                modifier = Modifier.padding(16.dp),
                text = album,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        TrackListItem(track, style, selectedTracks.contains(i), {
            onClick(i, it)
        })
        {
            onLongClick(i, it)
        }
    }
}