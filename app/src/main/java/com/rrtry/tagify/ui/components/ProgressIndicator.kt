package com.rrtry.tagify.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressIndicator(heightFraction: Float = 1f) {
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(heightFraction)) {
        CircularProgressIndicator(
            modifier = Modifier.width(64.dp).align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}