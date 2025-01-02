package com.rrtry.tagify.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rrtry.tagify.ui.components.ArtistListItem

@Composable
fun Artists(viewModel: MediaViewModel) {
    val artists by viewModel.artists.collectAsState()
    CollectionsGrid(artists) { artist ->
        ArtistListItem(artist, viewModel)
    }
}