package com.rrtry.tagify.prefs

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rrtry.tagify.R
import com.rrtry.tagify.ui.components.RadioButtonDialog
import com.rrtry.tagify.ui.components.RadioButtonOptionInfo
import com.rrtry.tagify.util.MAX_ARTWORK_SIZE
import com.rrtry.tagify.util.MIN_ARTWORK_SIZE
import com.rrtry.tagify.util.parseTokens
import kotlinx.coroutines.flow.MutableStateFlow
import com.jtagger.AbstractTag.*
import com.rrtry.tagify.ui.components.CheckboxItem
import com.rrtry.tagify.ui.components.CheckboxListDialog
import com.rrtry.tagify.ui.components.TextFieldDialog

@Composable
fun SwitchPreferenceTile(
    @StringRes titleRes: Int,
    stateFlow: MutableStateFlow<Boolean>)
{
    val checked by stateFlow.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { stateFlow.value = !stateFlow.value })
    {
        Text(
            modifier = Modifier
                .padding(start = 58.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(0.8f),
            text  = stringResource(id = titleRes),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            modifier = Modifier.padding(end = 16.dp),
            checked  = checked,
            onCheckedChange = { stateFlow.value = !stateFlow.value }
        )
    }
}

@Composable
fun <T: Any> PreferenceTile(
    modifier: Modifier = Modifier,
    prefKey: String,
    @StringRes titleRes: Int,
    setValue: MutableStateFlow<T>,
    onClick: () -> Unit,
    showHint: Boolean = false,
    onHintClick: (() -> Unit)? = null)
{
    val value by setValue.collectAsState()
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() })
    {
        Column(modifier = modifier) {
            Text(modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                overflow = TextOverflow.Ellipsis,
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleSmall
            )
            Text(modifier = Modifier
                .padding(bottom = 8.dp)
                .width(200.dp),
                text      = context.preferenceToDisplayName(prefKey, value),
                style     = MaterialTheme.typography.bodyMedium
            )
        }
        if (showHint) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(modifier = Modifier.padding(8.dp), onClick = {
                if (onHintClick != null) onHintClick()
            })
            {
                Icon(imageVector = Icons.Filled.Info, null)
            }
        }
    }
}

@Composable
fun ExpandablePreferenceGroup(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    animLabel: String,
    content: @Composable (visible: Boolean) -> Unit)
{
    var visible by rememberSaveable { mutableStateOf(false) }
    val iconRotation: Float by animateFloatAsState(targetValue = if (visible) 180f else 0f,
        label = animLabel
    )

    Row(modifier = Modifier.clickable { visible = !visible },
        verticalAlignment = Alignment.CenterVertically) {
        Icon(modifier = Modifier.padding(start = 16.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null)
        Text(modifier = Modifier.padding(16.dp), text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.weight(1f, true))
        Icon(modifier = Modifier
            .padding(end = 16.dp)
            .rotate(iconRotation),
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null
        )
    }
    content(visible)
}

@Composable
fun ArtworkSizeDialog(
    value: MutableStateFlow<Int>,
    onDismissRequest: () -> Unit)
{
    TextFieldDialog(
        R.string.change_artwork_size,
        R.string.artwork_size,
        R.string.artwork_size_error,
        KeyboardOptions(keyboardType = KeyboardType.Number),
        value,
        { onDismissRequest() },
        { it.toInt() })
    {
        it.toIntOrNull() in (MIN_ARTWORK_SIZE..MAX_ARTWORK_SIZE)
    }
}

@Composable
fun FilenamePatternDialog(
    value: MutableStateFlow<String>,
    onDismissRequest: () -> Unit)
{
    TextFieldDialog(
        R.string.change_filename_pattern,
        R.string.filename_pattern,
        R.string.filename_pattern_error,
        KeyboardOptions(keyboardType = KeyboardType.Number),
        value,
        { onDismissRequest() },
        { it })
    {
        val tokens = parseTokens(it)?.first ?: listOf()
        !tokens.any { fToken -> !FIELDS.contains(fToken) } &&
         tokens.containsAll(setOf(TITLE, ARTIST))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavHostController, viewModel: PreferencesViewModel = hiltViewModel()) {

    var showBatchModeLookupMethodDialog  by rememberSaveable { mutableStateOf(false) }
    var showManualModeLookupMethodDialog by rememberSaveable { mutableStateOf(false) }

    var showArtworkSizeDialog     by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsDialog  by rememberSaveable { mutableStateOf(false) }
    var showFilenamePatternDialog by rememberSaveable { mutableStateOf(false) }

    val onBackPressed = {
        viewModel.saveChanges()
        navController.navigateUp()
    }
    BackHandler {
        onBackPressed()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(
                            stringResource(id = R.string.settings),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    )
    { paddingValues ->
        Column(modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(paddingValues))
        {
            // Tags
            ExpandablePreferenceGroup(R.string.tags, R.drawable.baseline_sell_24, "tags_group") {
                AnimatedVisibility(visible = it) {
                    Column {
                        Text(modifier = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            text = stringResource(id = R.string.tag_lookup)
                        )
                        PreferenceTile(
                            modifier    = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            prefKey     = PREFS_KEY_BATCH_LOOKUP_METHOD,
                            titleRes    = R.string.batch_mode_lookup_method,
                            setValue    = viewModel.batchLookupMethod,
                            onClick     = { showBatchModeLookupMethodDialog = true},
                            showHint    = true,
                            onHintClick = { /*TODO*/ }
                        )
                        PreferenceTile(
                            modifier = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            prefKey  = PREFS_KEY_MANUAL_LOOKUP_METHOD,
                            titleRes = R.string.manual_mode_lookup_method,
                            setValue = viewModel.manualLookupMethod,
                            onClick  = { showManualModeLookupMethodDialog = true },
                            showHint = true,
                            onHintClick = { /*TODO*/}
                        )
                        Text(modifier  = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                            text       = stringResource(id = R.string.artwork))
                        PreferenceTile(
                            modifier = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            prefKey  = PREFS_KEY_ARTWORK_SIZE,
                            titleRes = R.string.artwork_size,
                            setValue = viewModel.artworkSize,
                            onClick  = { showArtworkSizeDialog = true }
                        )
                        Text(modifier = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            text = stringResource(id = R.string.tag_fields)
                        )
                        PreferenceTile(
                            modifier    = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            prefKey     = PREFS_KEY_REQUIRED_FIELDS,
                            titleRes    = R.string.required_fields,
                            setValue    = viewModel.tagFields,
                            onClick     = { showRequiredFieldsDialog = true },
                            showHint    = true,
                            onHintClick = {

                            }
                        )
                        SwitchPreferenceTile(
                            R.string.prefs_fix_encoding,
                            viewModel.fixEncoding
                        )
                    }
                }
            }
            // Filename pattern
            ExpandablePreferenceGroup(
                titleRes  = R.string.filename_pattern,
                iconRes   = R.drawable.file_music,
                animLabel = "files_group")
            {
                AnimatedVisibility(visible = it) {
                    Column {
                        SwitchPreferenceTile(
                            R.string.rename_files,
                            viewModel.renameFiles
                        )
                        PreferenceTile(
                            modifier = Modifier.padding(start = 58.dp, top = 8.dp, bottom = 8.dp),
                            prefKey  = PREFS_KEY_FILENAME_PATTERN,
                            titleRes = R.string.change_filename_pattern,
                            setValue = viewModel.filenamePattern,
                            onClick  = { showFilenamePatternDialog = true},
                        )
                    }
                }
            }
            // Appearance group
            ExpandablePreferenceGroup(
                titleRes  = R.string.appearance,
                iconRes   = R.drawable.baseline_palette_24,
                animLabel = "appearance_group")
            {

            }
        }
    }
    if (showBatchModeLookupMethodDialog) {
        RadioButtonDialog(
            titleRes = R.string.select_lookup_method,
            options  = listOf(
                RadioButtonOptionInfo(
                    R.string.fingerprint_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT,
                    { selection -> viewModel.batchLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT }
                ),
                RadioButtonOptionInfo(
                    R.string.filename_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_FILENAME,
                    { selection -> viewModel.batchLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_FILENAME }
                )
            ),
            currentSelection = viewModel.batchLookupMethod as MutableStateFlow<Any?>)
        {
            showBatchModeLookupMethodDialog = false
        }
    }
    if (showManualModeLookupMethodDialog) {
        RadioButtonDialog(
            titleRes = R.string.select_lookup_method,
            options  = listOf(
                RadioButtonOptionInfo(
                    R.string.fingerprint_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT,
                    { selection -> viewModel.manualLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT }
                ),
                RadioButtonOptionInfo(
                    R.string.filename_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_FILENAME,
                    { selection -> viewModel.manualLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_FILENAME }
                ),
                RadioButtonOptionInfo(
                    R.string.manual_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_QUERY,
                    { selection -> viewModel.manualLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_QUERY }
                ),
                RadioButtonOptionInfo(
                    R.string.choose_lookup_method,
                    PREFS_VALUE_LOOKUP_METHOD_NONE,
                    { selection -> viewModel.manualLookupMethod.value = selection as Int },
                    { selection -> selection == PREFS_VALUE_LOOKUP_METHOD_NONE }
                ),
            ),
            currentSelection = viewModel.manualLookupMethod as MutableStateFlow<Any?>)
        {
            showManualModeLookupMethodDialog = false
        }
    }
    if (showArtworkSizeDialog) {
        ArtworkSizeDialog(value = viewModel.artworkSize) {
            showArtworkSizeDialog = false
        }
    }
    if (showRequiredFieldsDialog) {
        CheckboxListDialog(
            viewModel.tagFields,
            setOf(
                CheckboxItem(TITLE,        R.string.title,    false),
                CheckboxItem(ARTIST,       R.string.artist,   false),
                CheckboxItem(ALBUM,        R.string.album,    false),
                CheckboxItem(PICTURE,      R.string.artwork,  true),
                CheckboxItem(YEAR,         R.string.year,     true),
                CheckboxItem(GENRE,        R.string.genre,    true),
                CheckboxItem(ALBUM_ARTIST, R.string.alb_art,  true),
                CheckboxItem(COMPOSER,     R.string.composer, true),
                CheckboxItem(TRACK_NUMBER, R.string.trk_num,  true),
                CheckboxItem(DISC_NUMBER,  R.string.dsk_num,  true)
            ))
        {
            showRequiredFieldsDialog = false
        }
    }
    if (showFilenamePatternDialog) {
        FilenamePatternDialog(value = viewModel.filenamePattern) {
            showFilenamePatternDialog = false
        }
    }
}