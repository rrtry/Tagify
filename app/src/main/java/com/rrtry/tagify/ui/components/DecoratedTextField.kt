package com.rrtry.tagify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoratedTextField(modifier: Modifier,
                       value: String,
                       onChange: (changed: String) -> Unit,
                       label: String,
                       @DrawableRes iconRes: Int? = null,
                       keyboardType: KeyboardType = KeyboardType.Text)
{

    val interactionSource = remember { MutableInteractionSource() }
    val enabled    = true
    val singleLine = true

    BasicTextField(
        value = value,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        onValueChange = { changed -> onChange(changed) },
        interactionSource = interactionSource,
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
        singleLine = singleLine,
        modifier = modifier
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            label = { Text(text = label) },
            value = value,
            innerTextField = it,
            leadingIcon = { iconRes?.let { icon -> Icon(painter = painterResource(id = icon), null) }},
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.outlinedTextFieldColors(),
            contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                start = 8.dp, end = 8.dp
            ),
            container = {
                OutlinedTextFieldDefaults.ContainerBox(enabled, false, interactionSource, TextFieldDefaults.outlinedTextFieldColors())
            },
        )
    }
}