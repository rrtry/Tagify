package com.rrtry.tagify.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rrtry.tagify.ui.components.AlbumListItem

@Composable
fun Albums(viewModel: MediaViewModel) {
    val albums by viewModel.albums.collectAsState()
    CollectionsGrid(albums) { album ->
        AlbumListItem(album, viewModel)
    }
}