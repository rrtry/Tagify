package com.rrtry.tagify.ui.details

import android.system.Os.*
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.text.isDigitsOnly
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.rrtry.tagify.ACTION_LOAD_TAG
import com.rrtry.tagify.ACTION_LOOKUP_TAGS
import com.rrtry.tagify.ACTION_SAVE_TAG
import com.rrtry.tagify.R
import com.rrtry.tagify.data.api.ItunesTag
import com.rrtry.tagify.data.api.MusicBrainzTag
import com.rrtry.tagify.data.entities.Tag
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.data.entities.TrackWithTags
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FILENAME
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_QUERY
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.ui.components.Artwork
import com.rrtry.tagify.ui.components.ListPlaceholder
import com.rrtry.tagify.ui.components.ProgressIndicator
import com.rrtry.tagify.ui.components.UIEventHandler
import com.rrtry.tagify.ui.details.TrackDetailsViewModel.UIEvent
import com.rrtry.tagify.ui.details.TrackDetailsViewModel.LookupMethod
import com.rrtry.tagify.service.ServiceEventBus.Event.*
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.service.TagWriterService.Companion.EXTRA_ARTIST
import com.rrtry.tagify.service.TagWriterService.Companion.EXTRA_LOOKUP_METHOD
import com.rrtry.tagify.service.TagWriterService.Companion.EXTRA_TITLE
import com.rrtry.tagify.ui.components.ImagePicker
import com.rrtry.tagify.ui.components.ImagePickerLauncher
import com.rrtry.tagify.ui.components.MyFloatingActionButton
import com.rrtry.tagify.util.checkUriPermission
import com.rrtry.tagify.util.isNumPairInvalid
import com.rrtry.tagify.util.joinIfNotBlank
import com.rrtry.tagify.util.safeSubstring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL

const val BOTTOM_SHEET_METADATA = 0
const val BOTTOM_SHEET_ARTWORK_OPTIONS = 1

const val BOTTOM_SHEET_META_TAGS    = 2
const val BOTTOM_SHEET_META_ARTWORK = 8
const val BOTTOM_SHEET_META_FIELDS  = 16

@Composable
fun LookupQueryDialog(
    values: Pair<StateFlow<String>, StateFlow<String>>,
    paramTitle: Pair<Int, Int>,
    onConfirm: (artist: String, title: String) -> Unit,
    onDismissRequest: () -> Unit)
{
    var artist by rememberSaveable { mutableStateOf(values.first.value)  }
    var title  by rememberSaveable { mutableStateOf(values.second.value) }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(8.dp))
        {
            Text(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                text  = stringResource(id = R.string.lookup_query),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                value      = artist,
                singleLine = true,
                label = { Text(text = stringResource(id = paramTitle.first).capitalize()) },
                onValueChange = { newValue: String ->
                    artist = newValue
                }
            )
            OutlinedTextField(
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                value      = title,
                singleLine = true,
                label = { Text(text = stringResource(id = paramTitle.second)) },
                onValueChange = { newValue: String ->
                    title = newValue
                }
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                    TextButton(modifier = Modifier.padding(8.dp), onClick = { onDismissRequest() }) {
                        Text(text = stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(modifier = Modifier.padding(8.dp), onClick = {
                        onDismissRequest()
                        onConfirm(artist, title)
                    })
                    {
                        Text(text = stringResource(id = R.string.confirm), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LookupMethodDialog(viewModel: TrackDetailsViewModel, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp))
        {
            var rememberChoice by rememberSaveable { mutableStateOf(false) }
            Text(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                text = stringResource(R.string.select_lookup_method),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onLookupMethodSelected(
                            PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT,
                            rememberChoice
                        )
                    },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Icon(modifier = Modifier.padding(16.dp),
                        painter = painterResource(id = R.drawable.baseline_fingerprint_24),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                        Text(text = stringResource(id = R.string.fingerprint_lookup_method), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(id = R.string.fingerprint_lookup_method_explanation))
                    }
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onLookupMethodSelected(
                            PREFS_VALUE_LOOKUP_METHOD_FILENAME,
                            rememberChoice
                        )
                    },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Icon(modifier = Modifier.padding(16.dp),
                        painter = painterResource(id = R.drawable.file_music),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                        Text(text = stringResource(id = R.string.filename_lookup_method), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(id = R.string.filename_lookup_method_explanation))
                    }
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onLookupMethodSelected(
                            PREFS_VALUE_LOOKUP_METHOD_QUERY,
                            rememberChoice
                        )
                    },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Icon(modifier = Modifier.padding(16.dp),
                        painter = painterResource(id = R.drawable.magnify),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                        Text(text = stringResource(id = R.string.manual_lookup_method), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(id = R.string.manual_lookup_method_explanation))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberChoice = !rememberChoice },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = !rememberChoice }
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 8.dp, bottom = 16.dp),
                        text = stringResource(id = R.string.remember_lookup_method)
                    )
                }
            }
        }
    }
}

@Composable
fun FileInfoDialog(serviceBus: ServiceEventBus, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier
            .wrapContentSize(),
            shape = RoundedCornerShape(8.dp))
        {
            Text(modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text = stringResource(id = R.string.file_info),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)

            val picture    = serviceBus.tag?.pictureField
            val streamInfo = serviceBus.streamInfo
            val duration   = streamInfo?.duration!!

            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${stringResource(id = R.string.path)}: ${serviceBus.track!!.path}")
            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${stringResource(id = R.string.file_mime_type)}: ${serviceBus.track!!.mimeType}")
            if (picture != null) {
                Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    text = "${stringResource(id = R.string.artwork_mime_type)}: ${picture.mimeType}"
                )
            }
            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${stringResource(id = R.string.duration)}: %02d:%02d".format(duration / 60, duration % 60))
            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${stringResource(id = R.string.sample_rate)}: ${streamInfo.sampleRate} ${stringResource(
                    id = R.string.hertz
                )}")
            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = "${stringResource(id = R.string.bitrate)}: ${streamInfo.bitrate} ${stringResource(
                    id = R.string.kb_per_second
                )}")
            Text(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp),
                text = "${stringResource(id = R.string.channels)}: ${streamInfo.channelCount}"
            )
        }
    }
}

@Composable
fun SupportingText(@StringRes errorLabelRes: Int) {
    Text(
        modifier = Modifier.padding(0.dp),
        text     = stringResource(id = errorLabelRes),
        style    = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun NumericTagField(
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    field: MutableStateFlow<String>,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    @StringRes errorLabelRes: Int? = null,
    error: MutableStateFlow<Boolean>,
    isError: (s: String) -> Boolean)
{
    val value by field.collectAsState()
    val errorState by error.collectAsState()

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        keyboardOptions = keyboardOptions,
        isError    = errorState,
        singleLine = true,
        value      = value,
        onValueChange = { newValue: String ->
            field.value = newValue
            error.value = isError(newValue)
        },
        supportingText = if (errorLabelRes != null) {{ if (errorState) SupportingText(errorLabelRes) }} else null,
        leadingIcon    = { Icon(imageVector = ImageVector.vectorResource(iconRes), null)},
        label          = { Text(text = stringResource(id = labelRes)) }
    )
}

@Composable
fun TagField(
    field: MutableStateFlow<String>,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int)
{
    val value by field.collectAsState()
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        singleLine = true,
        value      = value,
        onValueChange = { newValue: String ->
            field.value = newValue
        },
        leadingIcon = { Icon(imageVector = ImageVector.vectorResource(iconRes), null)},
        label       = { Text(text = stringResource(id = labelRes)) }
    )
}

@Composable
fun TagFields(viewModel: TrackDetailsViewModel) {
    Column (modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp))
    {
        TagField(
            viewModel.title,
            R.drawable.format_title,
            R.string.title
        )
        TagField(
            viewModel.artist,
            R.drawable.account_music,
            R.string.artist
        )
        TagField(
            viewModel.album,
            R.drawable.album,
            R.string.album
        )
        NumericTagField(
            Modifier,
            KeyboardOptions(keyboardType = KeyboardType.Number),
            viewModel.year,
            R.drawable.calendar_star_outline,
            R.string.year,
            null,
            viewModel.yearError)
        {
            if (it.isBlank()) false else (!it.isDigitsOnly() || it.length != 4)
        }
        TagField(
            viewModel.genre,
            R.drawable.guitar_electric,
            R.string.genre
        )
        TagField(
            viewModel.albArt,
            R.drawable.account_box_multiple,
            R.string.alb_art
        )
        TagField(
            viewModel.composer,
            R.drawable.music,
            R.string.composer
        )
        Row {
            NumericTagField(
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                field    = viewModel.trkNum,
                iconRes  = R.drawable.numeric_1_box,
                labelRes = R.string.trk_num,
                errorLabelRes = R.string.trk_num_format,
                error = viewModel.trkNumError,
                isError = {
                    isNumPairInvalid(it)
                }
            )
            NumericTagField(
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                field    = viewModel.dskNum,
                iconRes  = R.drawable.numeric_1_circle_outline,
                labelRes = R.string.dsk_num,
                errorLabelRes = R.string.trk_num_format,
                error = viewModel.dskNumError,
                isError = {
                    isNumPairInvalid(it)
                }
            )
        }
    }
}

@Composable
fun TagItem(tag: Tag, withArtwork: Boolean, onTagClick: (tag: Tag, withArtwork: Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onTagClick(tag, withArtwork) }
            .fillMaxSize())
    {
        Icon(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
                .size(16.dp),
            painter = painterResource(id = if (tag is ItunesTag) R.drawable.apple_logo_black else R.drawable.musicbrainz_icon_detail),
            contentDescription = null
        )
        Row(modifier = Modifier
            .fillMaxWidth())
        {
            if (withArtwork) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(tag.cover)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .transformations(listOf(RoundedCornersTransformation(10f)))
                        .placeholder(R.drawable.account_music)
                        .error(R.drawable.image)
                        .size(with(LocalDensity.current) { 100.dp.roundToPx() })
                        .build(),
                    null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(start = 10.dp, end = 10.dp)
                )
            }
            Column(modifier = Modifier.padding(5.dp)) {
                Text(text = tag.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(text = tag.album, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(text = tag.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(text = tag.date.safeSubstring(0, 4).joinIfNotBlank(tag.genre, "•"), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(text = "${tag.trkPos}/${tag.trkCnt}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun ArtworkGrid(fetchState: Boolean, tags: List<Tag>, onArtworkClick: (url: URL) -> Unit) {
    if (fetchState && tags.none { it.cover != null }) {
        ProgressIndicator(0.25f)
    } else if (tags.none { it.cover != null }) {
        ListPlaceholder(iconRes = R.drawable.image_search, textRes = R.string.no_content, heightFraction = 0.25f)
    } else {
        LazyVerticalGrid(
            modifier = Modifier.padding(4.dp),
            columns = GridCells.Adaptive(130.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp))
        {
            items(items = tags.filter { it.cover != null }) { tag ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .transformations(listOf(RoundedCornersTransformation(10f)))
                        .data(tag.cover)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .placeholder(R.drawable.image)
                        .size(with(LocalDensity.current) { 130.dp.roundToPx() })
                        .build(),
                    null,
                    modifier = Modifier
                        .clickable {
                            tag.cover?.let {
                                onArtworkClick(URL(it))
                            }
                        }
                        .size(130.dp)
                )
            }
        }
    }
}

@Composable
fun TagList(
    fetchState: Boolean,
    tags: List<Tag>,
    withArtwork: Boolean,
    onTagClick: (tag: Tag, withArtwork: Boolean) -> Unit)
{
    if (fetchState && tags.isEmpty()) {
        ProgressIndicator(0.25f)
    } else if (tags.isEmpty()) {
        ListPlaceholder(iconRes = R.drawable.image_search, textRes = R.string.no_content)
    } else {
        LazyColumn(verticalArrangement = Arrangement.SpaceBetween) {
            items(items = tags) { item ->
                TagItem(item, withArtwork, onTagClick)
            }
        }
    }
}

@Composable
fun FetchedMetadata(
    fetchedTags: List<Tag>,
    fetchState: Boolean,
    setTag: (tag: Tag, withArtwork: Boolean) -> Unit,
    setArtwork: (url: URL) -> Unit,
    refresh: () -> Unit)
{
    var layout         by rememberSaveable { mutableIntStateOf(BOTTOM_SHEET_META_TAGS) }
    var providerFilter by rememberSaveable { mutableIntStateOf(0x0) }

    val filteredTags = remember(providerFilter, fetchedTags) {
        if (providerFilter == 0x0 || providerFilter == 0x3) {
            null
        } else {
            fetchedTags.filterIsInstance(
                if ((providerFilter and FILTER_PROVIDER_ITUNES) != 0) ItunesTag::class.java else MusicBrainzTag::class.java
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (!fetchState) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { refresh() }) {
                Icon(painter = painterResource(id = R.drawable.baseline_refresh_24), null)
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
                    .align(Alignment.CenterEnd),
            )
        }
        Column (modifier = Modifier
            .wrapContentSize()
            .padding(end = 8.dp)) {
            Row {
                FilterChip(
                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                    selected = layout == BOTTOM_SHEET_META_TAGS,
                    label    = { Text(text = stringResource(id = R.string.tags)) },
                    onClick  = { layout = BOTTOM_SHEET_META_TAGS }
                )
                FilterChip(
                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                    selected = layout == BOTTOM_SHEET_META_FIELDS,
                    label    = { Text(text = stringResource(id = R.string.fields)) },
                    onClick  = { layout = BOTTOM_SHEET_META_FIELDS }
                )
                FilterChip(
                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                    selected = layout == BOTTOM_SHEET_META_ARTWORK,
                    label    = { Text(text = stringResource(id = R.string.artwork)) },
                    onClick  = { layout = BOTTOM_SHEET_META_ARTWORK }
                )
            }
            Row {
                FilterChip(
                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                    leadingIcon = { Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(id = R.drawable.apple_logo_black),
                        contentDescription = null
                    )},
                    selected = (providerFilter and FILTER_PROVIDER_ITUNES) != 0,
                    onClick  = { providerFilter = providerFilter xor FILTER_PROVIDER_ITUNES },
                    label    = { Text(text = stringResource(id = R.string.provider_itunes)) }
                )
                FilterChip(
                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                    leadingIcon = { Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(id = R.drawable.musicbrainz_icon_detail),
                        contentDescription = null
                    )},
                    selected = (providerFilter and FILTER_PROVIDER_MB) != 0,
                    onClick  = { providerFilter = providerFilter xor FILTER_PROVIDER_MB },
                    label    = { Text(text = stringResource(id = R.string.provider_musicbrainz)) }
                )
            }
        }
    }
    if (layout == BOTTOM_SHEET_META_ARTWORK) {
        ArtworkGrid(fetchState, filteredTags ?: fetchedTags) { url ->
            setArtwork(url)
        }
    } else {
        TagList(
            fetchState,
            filteredTags ?: fetchedTags,
            layout != BOTTOM_SHEET_META_FIELDS)
        { tag, withArtwork ->
            setTag(tag, withArtwork)
        }
    }
}

@Composable
fun FetchedMetadata(
    tags: MutableStateFlow<MutableList<TrackWithTags>>,
    fetching: MutableStateFlow<Boolean>,
    setTag: (tag: Tag, withArtwork: Boolean) -> Unit,
    setArtwork: (url: URL) -> Unit,
    refresh: () -> Unit)
{
    val fetchedTags by tags.collectAsState()
    val fetchState  by fetching.collectAsState()

    FetchedMetadata(
        fetchedTags.firstOrNull()?.tags ?: listOf(),
        fetchState,
        setTag,
        setArtwork,
        refresh
    )
}

@Composable
fun ArtworkOptions(viewModel: TrackDetailsViewModel,
                   showBottomSheet: MutableState<Boolean>,
                   launchImagePicker: () -> Unit)
{
    Column(modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showBottomSheet.value = false
                launchImagePicker()
            }
            .padding(10.dp))
        {
            Icon(painter = painterResource(id = R.drawable.image),
                modifier = Modifier.padding(end = 10.dp),
                contentDescription = null
            )
            Text(text = stringResource(id = R.string.select_cover), style = MaterialTheme.typography.titleMedium)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                /* TODO: implement */
            }
            .padding(10.dp))
        {
            Icon(imageVector = ImageVector.vectorResource(id = R.drawable.image_search),
                modifier = Modifier.padding(end = 10.dp),
                contentDescription = null
            )
            Text(text = stringResource(id = R.string.search_cover), style = MaterialTheme.typography.titleMedium)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.sendEvent(UIEvent.OnExportArtwork)
            }
            .padding(10.dp))
        {
            Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_drive_file_move_outline_24),
                modifier = Modifier.padding(end = 10.dp),
                contentDescription = null
            )
            Text(text = stringResource(id = R.string.export_artwork), style = MaterialTheme.typography.titleMedium)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showBottomSheet.value = false
                viewModel.artwork.value = null
                viewModel.changedArtwork = true
            }
            .padding(10.dp))
        {
            Icon(imageVector = Icons.Default.Close,
                modifier = Modifier.padding(end = 10.dp),
                contentDescription = null
            )
            Text(text = stringResource(id = R.string.remove_cover), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetails(rootNavController: NavHostController,
                 serviceEventBus: ServiceEventBus,
                 track: Track,
                 viewModel: TrackDetailsViewModel = hiltViewModel())
{
    val context = LocalContext.current
    val gradientColor = MaterialTheme.colorScheme.background

    val scrollState  = rememberScrollState()
    val sheetState   = rememberModalBottomSheetState()
    val composeScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val imagePicker       = remember { ImagePickerLauncher() }
    val showInfoDialog    = rememberSaveable  { mutableStateOf(false) }
    val showBottomSheet   = rememberSaveable { mutableStateOf(false) }
    val bottomSheetLayout = rememberSaveable { mutableIntStateOf(BOTTOM_SHEET_ARTWORK_OPTIONS) }

    val errorState by viewModel.errorState.collectAsState()
    val artwork by viewModel.artwork.collectAsState()

    val showLookupMethodDialog by viewModel.showLookupMethodDialog.collectAsState()
    val showLookupQueryDialog by viewModel.showLookupQueryDialog.collectAsState()

    val finishedScanning by viewModel.finishedScanning.collectAsState()
    val finishedSaving   by viewModel.finishedSaving.collectAsState()

    val onBackPressed = {
        context.stopService(Intent(context, TagWriterService::class.java))
        serviceEventBus.lookupResults.value = mutableListOf()
        rootNavController.navigateUp()
    }
    BackHandler {
        onBackPressed()
    }
    LaunchedEffect(Unit) {
        if (serviceEventBus.track == null) {
            serviceEventBus.track = track
            viewModel.filePath    = track.path
            viewModel.uri         = track.trackUri
            viewModel.mimeType    = track.mimeType
            context.startService(Intent(context, TagWriterService::class.java).apply {
                action = ACTION_LOAD_TAG
            })
        }
    }
    ImagePicker(imagePicker) { uri ->
        if (uri != null) viewModel.setArtwork(uri)
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        enabled = finishedScanning && finishedSaving,
                        onClick = { showInfoDialog.value = true })
                    {
                        Icon(Icons.Default.Info, null)
                    }
                    IconButton(
                        enabled = !errorState && finishedScanning && finishedSaving,
                        onClick = {
                            viewModel.saveChanges(serviceEventBus.tag)
                        })
                    {
                        Icon(imageVector = ImageVector.vectorResource(R.drawable.content_save), null)
                    }
                    IconButton(
                        onClick = {
                            Intent(ACTION_VIEW).apply {
                                data  = track.trackUri
                                flags = FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(this)
                            }
                        })
                    {
                        Icon(Icons.Filled.PlayArrow, null)
                    }
                    IconButton(
                        onClick = {
                            onBackPressed()
                        })
                    {
                        Icon(imageVector = ImageVector.vectorResource(R.drawable.exit_to_app), null)
                    }
                },
                floatingActionButton = {
                    MyFloatingActionButton(
                        Icons.Filled.Search,
                        {
                            if (finishedScanning && finishedSaving) {
                                viewModel.onSearchButtonClick(serviceEventBus.inProgress.value)
                            }
                        },
                        {
                            if (finishedSaving &&
                                finishedScanning)
                            {
                                viewModel.showLookupMethodDialog(serviceEventBus.inProgress.value)
                            }
                        }
                    )
                }
            )
        })
    { paddingValues ->
        UIEventHandler(serviceEventBus.eventFlow) {
            when (it) {
                is OnFileWritten -> {
                    serviceEventBus.lookupResults.value = mutableListOf()
                    rootNavController.navigateUp()
                }
                is OnFileLoadFailed -> {
                    context.stopService(Intent(context, TagWriterService::class.java))
                    composeScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message     = context.getString(R.string.failed_to_load_file),
                            actionLabel = context.getString(R.string.exit),
                            duration    = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            rootNavController.navigateUp()
                        }
                    }
                }
                is OnFileWriteFailed -> {
                    composeScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message     = context.getString(R.string.tag_write_failed),
                            actionLabel = context.getString(R.string.exit),
                            duration    = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            serviceEventBus.lookupResults.value = mutableListOf()
                            rootNavController.navigateUp()
                        }
                    }
                }
                is OnFileLoaded -> {
                    if (!viewModel.finishedScanning.value) {
                        viewModel.duration = serviceEventBus.streamInfo!!.duration
                        viewModel.setTag(serviceEventBus.tag)
                    }
                }
                else -> {}
            }
        }
        UIEventHandler(viewModel.eventFlow) {
            when (it) {
                is UIEvent.OnStartTagLookup -> {
                    context.startService(Intent(context, TagWriterService::class.java).apply {
                        action = ACTION_LOOKUP_TAGS
                        when (it.method) {
                            is LookupMethod.Fingerprint -> putExtra(EXTRA_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT)
                            is LookupMethod.Filename    -> putExtra(EXTRA_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_FILENAME)
                            is LookupMethod.Query       -> {
                                putExtra(EXTRA_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_QUERY)
                                putExtra(EXTRA_ARTIST, it.method.artist)
                                putExtra(EXTRA_TITLE,  it.method.title)
                            }
                        }
                    })
                }
                is UIEvent.OnExportArtwork -> {
                    viewModel.exportArtwork()
                }
                is UIEvent.OnArtworkExported -> {
                    composeScope.launch {
                        if (it.success) {
                            snackbarHostState.showSnackbar(
                                message = "${context.getString(R.string.exported_artwork_path)}: ${it.file.path}",
                                withDismissAction = false,
                                duration = SnackbarDuration.Long
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message  = context.getString(R.string.artwork_export_failed),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
                is UIEvent.OnSaveChanges -> {
                    serviceEventBus.tag   = viewModel.editedTag
                    serviceEventBus.track = viewModel.editedTrack
                    context.startService(Intent(context, TagWriterService::class.java).apply {
                        action = ACTION_SAVE_TAG
                    })
                }
                is UIEvent.OnIOError -> {
                    composeScope.launch {
                        if (it.exitAction) {
                            val result = snackbarHostState.showSnackbar(
                                message = it.message,
                                actionLabel = context.getString(R.string.exit),
                                withDismissAction = false,
                                SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                rootNavController.navigateUp()
                            }
                        } else {
                            snackbarHostState.showSnackbar(message = it.message)
                        }
                    }
                }
                is UIEvent.OnHideBottomSheet -> {
                    showBottomSheet.value = false
                }
                is UIEvent.OnExpandBottomSheet -> {
                    bottomSheetLayout.intValue = it.layoutId
                    showBottomSheet.value = true
                }
                is UIEvent.OnArtworkChange       -> serviceEventBus.writeApic = true
                is UIEvent.OnAPILimitExceeded    -> composeScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.api_limit_exceeded)) }
                is UIEvent.OnMediaScannerFailed  -> composeScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.mediastore_failed)) }
                is UIEvent.OnChromaprintFailure  -> composeScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.chromaprint_failed)) }
                is UIEvent.OnNavigateUpEvent     -> rootNavController.navigateUp()
            }
        }
        if (showLookupQueryDialog) {
            LookupQueryDialog(
                Pair(viewModel.artist, viewModel.title),
                Pair(R.string.artist, R.string.title),
                { artist, title -> viewModel.lookupMetadata(
                    LookupMethod.Query(artist, title),
                    viewModel.lookupMethod != PREFS_VALUE_LOOKUP_METHOD_QUERY
                )
                },
                { viewModel.showLookupQueryDialog.value = false }
            )
        }
        if (showLookupMethodDialog) {
            LookupMethodDialog(viewModel) {
                viewModel.showLookupMethodDialog.value = false
            }
        }
        if (showInfoDialog.value) {
            FileInfoDialog(serviceEventBus) {
                showInfoDialog.value = false
            }
        }
        if (showBottomSheet.value) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet.value = false
                },
                sheetState = sheetState) 
            {
                when (bottomSheetLayout.intValue) {
                    BOTTOM_SHEET_METADATA -> FetchedMetadata(
                        serviceEventBus.lookupResults,
                        serviceEventBus.inProgress,
                        { tag, withArtwork -> viewModel.setTag(tag, withArtwork) },
                        { url -> viewModel.setArtwork(url) },
                        { /* TODO: refresh() */ }
                    )
                    BOTTOM_SHEET_ARTWORK_OPTIONS -> ArtworkOptions(viewModel, showBottomSheet) { imagePicker.launch() }
                }
            }
        }
        Column(modifier = Modifier
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(state = scrollState))
        {
            Artwork(artwork, gradientColor, R.drawable.music, null) {
                bottomSheetLayout.intValue = BOTTOM_SHEET_ARTWORK_OPTIONS
                showBottomSheet.value = true
            }
            TagFields(viewModel)
        }
    }
}
