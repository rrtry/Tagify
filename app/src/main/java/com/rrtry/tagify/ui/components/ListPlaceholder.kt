package com.rrtry.tagify.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ListPlaceholder(@DrawableRes iconRes: Int, @StringRes textRes: Int, contentPadding: Dp = 0.dp, heightFraction: Float = 1f) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(heightFraction)) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Image(modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                  painter = painterResource(id = iconRes), contentDescription = null)
            Text(modifier = Modifier.padding(5.dp),
                 text = stringResource(id = textRes), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}