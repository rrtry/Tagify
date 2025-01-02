package com.rrtry.tagify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.ui.home.MediaViewModel
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent
import com.rrtry.tagify.util.resolveArtworkSanitized
import com.rrtry.tagify.util.sanitizeFilename

const val ARTWORK_CACHE_KEY_PREFIX = "ARTWORK"

@Composable
fun AlbumListItem(album: Album, viewModel: MediaViewModel) {

    val title = album.title!!
    val subtitles = arrayOf(
        if (album.artistCount!! == 1) album.artist!! else stringResource(id = R.string.various_artists),
        "${album.trackCount} ${if (album.trackCount != 1) stringResource(id = R.string.tracks) else stringResource(
            id = R.string.track
        )}")

    val artworkPath = LocalContext.current.resolveArtworkSanitized(title)
    ArtworkCard(title, subtitles, artworkPath, "${ARTWORK_CACHE_KEY_PREFIX}:${title.sanitizeFilename()}") {
        viewModel.sendEvent(UIEvent.OnAlbumClick(album))
    }
}