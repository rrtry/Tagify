package com.rrtry.tagify.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyFloatingActionButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit)
{
    Surface(modifier = Modifier
        .combinedClickable(
            onClick     = onClick,
            onLongClick = onLongClick
        )
        .semantics { Role.Button },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary)
    {
        Box(
            modifier = Modifier
                .defaultMinSize(
                    minWidth  = 56.dp,
                    minHeight = 56.dp,
                ),
            contentAlignment = Alignment.Center,
        ) { Icon(imageVector, null) }
    }
}