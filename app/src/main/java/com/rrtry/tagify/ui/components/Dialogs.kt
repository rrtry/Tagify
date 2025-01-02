package com.rrtry.tagify.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rrtry.tagify.R
import com.rrtry.tagify.ui.home.MediaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

data class CheckboxItem<T>(
    val value: T,
    @StringRes
    val displayNameRes: Int,
    val enabled: Boolean
)

data class RadioButtonOptionInfo(
    @StringRes val text: Int,
    val selection: Any?,
    val onClick: (a: Any?) -> Unit,
    val isSelected: (a: Any?) -> Boolean
)

@Composable
fun RadioButtonOption(
    currentSelection: Any?,
    info: RadioButtonOptionInfo)
{
    Row(modifier = Modifier
        .clickable {
            info.onClick(info.selection)
        }
        .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = info.isSelected(currentSelection),
            onClick  = { info.onClick(info.selection) }
        )
        Text(text = stringResource(id = info.text))
    }
}

@Composable
fun FileRestoreDialogLayout(
    showConfirmLayout: MutableState<Boolean>,
    backupFile: MediaViewModel.BackupFile,
    onRestore: () -> Unit)
{
    Box {
        Column {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text  = stringResource(id = R.string.file_restore),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp)) {
                Column {
                    Text(
                        text = stringResource(id = R.string.title) + ":",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier.width(130.dp),
                        text     = backupFile.title,
                        style    = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.artist) + ":",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier.width(130.dp),
                        text = backupFile.artist,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.album) + ":",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier.width(130.dp),
                        text = backupFile.album,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column {
                    Text(
                        text  = stringResource(id = R.string.original_path) + ":",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier.width(150.dp),
                        text = backupFile.originalLocation,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            TextButton(onClick = { showConfirmLayout.value = true }) {
                Text(stringResource(id = R.string.delete))
            }
            TextButton(onClick = { onRestore() }) {
                Text(stringResource(id = R.string.restore))
            }
        }
    }
}

@Composable
fun ConfirmDeleteLayout(
    confirmLayout: MutableState<Boolean>,
    onDeleteConfirmed: () -> Unit)
{
    Box {
        Column {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text  = stringResource(id = R.string.delete_backup_confirm_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                text = stringResource(id = R.string.delete_backup_confirm)
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
        Row(modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 4.dp))
        {
            TextButton(onClick = { confirmLayout.value = false }) {
                Text(stringResource(id = R.string.no))
            }
            TextButton(onClick = { onDeleteConfirmed() }) {
                Text(stringResource(id = R.string.yes))
            }
        }
    }
}

@Composable
fun FileRestoreDialog(
    backupFile: MediaViewModel.BackupFile,
    onDismissRequest: () -> Unit,
    onRestoreBackup:  (backupFile: MediaViewModel.BackupFile) -> Unit)
{
    Dialog(onDismissRequest = {  }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp))
        {
            val showConfirmLayout = rememberSaveable { mutableStateOf(false) }
            if (!showConfirmLayout.value) {
                FileRestoreDialogLayout(showConfirmLayout, backupFile) {
                    onRestoreBackup(backupFile)
                }
            } else {
                ConfirmDeleteLayout(showConfirmLayout) {
                    onDismissRequest()
                }
            }
        }
    }
}

@Composable
fun RadioButtonDialog(
    @StringRes titleRes: Int,
    options: List<RadioButtonOptionInfo>,
    currentSelection: MutableStateFlow<Any?>,
    onDismissRequest: () -> Unit)
{
    val selection by currentSelection.collectAsState()
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp))
        {
            Text(modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text  = stringResource(id = titleRes),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                Modifier
                    .selectableGroup()
                    .padding(top = 8.dp, bottom = 16.dp))
            {
                for (option in options) {
                    RadioButtonOption(
                        selection,
                        option
                    )
                }
            }
        }
    }
}

@Composable
fun <T> CheckboxListDialog(
    selectedItems: MutableStateFlow<Set<T>>,
    checkboxItems: Set<CheckboxItem<T>>,
    onDismissRequest: () -> Unit)
{
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp))
        {
            val checkedItems by selectedItems.collectAsState()
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = R.string.select_fields),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()))
            {
                val onClick = { item: CheckboxItem<T> ->
                    if (item.enabled) {
                        if (selectedItems.value.contains(item.value)) {
                            selectedItems.value = selectedItems.value.minus(item.value)
                        } else {
                            selectedItems.value = selectedItems.value.plus(item.value)
                        }
                    }
                }
                for (item in checkboxItems) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClick(item)
                            })
                    {
                        Checkbox(
                            enabled = item.enabled,
                            checked = checkedItems.contains(item.value),
                            onCheckedChange = {
                                onClick(item)
                            }
                        )
                        Text(
                            text = stringResource(id = item.displayNameRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onDismissRequest() }) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun <T: Any> TextFieldDialog(
    @StringRes titleRes: Int,
    @StringRes labelRes: Int?,
    @StringRes errorRes: Int?,
    keyboardOptions: KeyboardOptions,
    value: MutableStateFlow<T>,
    onDismissRequest: (b: Boolean) -> Unit,
    convert: (s: String) -> T,
    validate: ((s: String) -> Boolean)?)
{
    val focusRequester = remember { FocusRequester() }
    var text    by rememberSaveable { mutableStateOf(value.value.toString()) }
    var focused by rememberSaveable { mutableStateOf(false) }
    val valid   by remember {
        derivedStateOf {
            if (validate != null) validate(text) else true
        }
    }

    Dialog(onDismissRequest = { onDismissRequest(false) }) {
        Card(
            modifier = Modifier
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp)
        )
        {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text = stringResource(id = titleRes),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                isError = !valid && focused,
                supportingText = {
                    if (!valid &&
                        focused &&
                        errorRes != null)
                    {
                        Text(stringResource(id = errorRes))
                    }
                },
                label = {
                    if (labelRes != null) {
                        Text(text = stringResource(id = labelRes))
                    }
                },
                value = text,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                onValueChange = { newValue: String ->
                    text = newValue
                }
            )
            Row(modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onDismissRequest(false) }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                TextButton(onClick = {
                    focusRequester.requestFocus()
                    if (valid) {
                        value.value = convert(text)
                        onDismissRequest(true)
                    }
                })
                {
                    Text(text = stringResource(id = R.string.confirm))
                }
            }
        }
    }
}