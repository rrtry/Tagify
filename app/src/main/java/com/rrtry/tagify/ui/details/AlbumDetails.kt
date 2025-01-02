package com.rrtry.tagify.ui.details
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.R
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.ui.components.GroupBy
import com.rrtry.tagify.ui.components.Style

@Composable
fun AlbumsDetails(
    navController: NavController,
    album: Album,
    serviceBus: ServiceEventBus,
    viewModel: AlbumDetailsViewModel = hiltViewModel())
{
    LaunchedEffect(Unit) {
        viewModel.loadAlbumTracks(album.title!!)
    }

    val uiState by viewModel.uiState.collectAsState()
    var subtitles: Pair<String?, String?> = Pair(null, null)
    if (uiState.genre != null || uiState.year != null) {

        val year  = uiState.year
        val genre = uiState.genre

        subtitles = Pair(
            if (year != null && genre != null) "$year • $genre" else "${year ?: genre}",
            null
        )
    }
    subtitles = Pair(
        subtitles.first,
        "${album.trackCount} ${if (album.trackCount == 1) stringResource(id = R.string.track) else stringResource(
            id = R.string.tracks
        )}"
    )
    CollectionDetails(
        navController = navController,
        viewModel     = viewModel,
        serviceBus    = serviceBus,
        tracks        = uiState.tracks,
        title         = "${album.title} • ${uiState.artist}",
        subtitles     = subtitles,
        artwork       = album.title!!,
        groupBy       = GroupBy.DISC_NUMBER,
        style         = Style.ALBUM_DETAILS
    )
}