package com.rrtry.tagify.ui.details

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.ui.components.GroupBy
import com.rrtry.tagify.ui.components.Style

@Composable
fun ArtistDetails(
    navController: NavHostController,
    artist: Artist,
    serviceBus: ServiceEventBus,
    viewModel: ArtistDetailsViewModel = hiltViewModel())
{
    LaunchedEffect(Unit) {
        viewModel.loadArtistTracks(artist)
    }

    val uiState by viewModel.uiState.collectAsState()
    val trkCnt = uiState.trackCount
    val albCnt = uiState.albumCount
    val subtitles = Pair(
        "$trkCnt ${if (trkCnt == 1) stringResource(id = R.string.track) else stringResource(
            id = R.string.tracks
        )} • $albCnt ${if (albCnt == 1) stringResource(id = R.string.album) else stringResource(
            id = R.string.albums
        )}",
        null
    )
    CollectionDetails(
        navController = navController,
        viewModel     = viewModel,
        serviceBus    = serviceBus,
        tracks        = uiState.tracks,
        title         = artist.name!!,
        subtitles     = subtitles,
        artwork       = artist.firstAlbum,
        groupBy       = GroupBy.ALBUM,
        style         = Style.ARTIST_DETAILS
    )
}