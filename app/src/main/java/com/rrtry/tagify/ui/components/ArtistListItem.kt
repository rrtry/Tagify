package com.rrtry.tagify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.ui.home.MediaViewModel
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent
import com.rrtry.tagify.util.resolveArtworkSanitized
import com.rrtry.tagify.util.sanitizeFilename

@Composable
fun ArtistListItem(artist: Artist, viewModel: MediaViewModel) {

    val title  = artist.name!!
    val albums = artist.albumCount
    val tracks = artist.trackCount
    val subtitles = arrayOf(
        "$albums ${if (albums != 1) stringResource(id = R.string.albums) else stringResource(id = R.string.album)}",
        "$tracks ${if (tracks != 1) stringResource(id = R.string.tracks) else stringResource(id = R.string.track)}"
    )

    val artworkPath = if (artist.firstAlbum != null) {
        LocalContext.current.resolveArtworkSanitized(artist.firstAlbum!!)
    } else {
        null
    }
    ArtworkCard(
        title,
        subtitles,
        artworkPath,
        "${ARTWORK_CACHE_KEY_PREFIX}:${artist.firstAlbum!!.sanitizeFilename()}")
    {
        viewModel.sendEvent(UIEvent.OnArtistClick(artist))
    }
}