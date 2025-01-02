package com.rrtry.tagify.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rrtry.tagify.ui.components.Style
import com.rrtry.tagify.ui.components.TrackListItem
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent
import com.rrtry.tagify.util.getThumbnailTimestamp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tracks(viewModel: MediaViewModel) {

    val context   = LocalContext.current
    val listState = rememberLazyListState()

    val tracks   by viewModel.tracks.collectAsState()
    val selected by viewModel.selectedTracks.collectAsState()

    LazyColumn(state = listState) {
        itemsIndexed(key = { _, track -> Pair(track.trackId, context.getThumbnailTimestamp(track.trackId)) }, items = tracks) { index, track ->
            Row(modifier = Modifier.animateItemPlacement()) {
                TrackListItem(
                    track,
                    Style.TRACK_LISTING,
                    selected.contains(index),
                    {
                        if (viewModel.selectionMode.value) {
                            viewModel.onClickSelect(index)
                        } else {
                            viewModel.sendEvent(UIEvent.OnTrackClick(it))
                        }
                    })
                {
                    viewModel.onLongClickSelect(index)
                }
            }
        }
    }
}