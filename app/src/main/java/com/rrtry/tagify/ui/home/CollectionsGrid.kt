package com.rrtry.tagify.ui.home

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun <T> CollectionsGrid(
    items: List<T>,
    content: @Composable (item: T) -> Unit)
{
    val state   = rememberLazyGridState()
    val config  = LocalConfiguration.current
    val minSize = config.screenWidthDp.dp / (if (config.orientation == ORIENTATION_LANDSCAPE) 4 else 2)

    LazyVerticalGrid(state = state, columns = GridCells.Adaptive(minSize)) {
        items(count = items.size) {
            content(items[it])
        }
    }
}