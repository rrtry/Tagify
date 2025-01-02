package com.rrtry.tagify.ui.details

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.rrtry.tagify.ACTION_BATCH_LOOKUP_TAGS
import com.rrtry.tagify.ACTION_BATCH_SAVE
import com.rrtry.tagify.CATEGORY_BATCH_OP
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.TrackWithTags
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.service.ServiceEventBus.Event.OnBatchProcessingFinished
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.ui.components.UIEventHandler
import com.rrtry.tagify.ui.home.AnimatedProgressIndicator
import com.rrtry.tagify.util.createWriteRequest
import com.rrtry.tagify.util.resolveArtworkSanitized
import java.io.File

/*
@Composable
@Preview
fun TaggedTrackPreview() {

    val title  = "Feeling Good"
    val artist = "Muse"
    val album  = "Origin Of Symmetry"
    val file   = "Muse - Feeling Good.mp3"

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { },
        verticalAlignment = Alignment.CenterVertically)
    {
        Image(
            painter = painterResource(id = R.drawable.cover),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .size(55.dp)
        )
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(title,
                modifier = Modifier.padding(end = 4.dp),
                style    = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$artist • $album",
                modifier = Modifier.padding(end = 4.dp),
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier  = Modifier.padding(end = 4.dp),
                text      = file,
                style     = MaterialTheme.typography.bodySmall,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            Text("\u2713 Found: 5 Result",
                modifier = Modifier.padding(end = 4.dp, bottom = 8.dp),
                color    = Color(0xff2e9100),
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(modifier = Modifier
            .clickable { }
            .padding(end = 8.dp), onClick = {})
        {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = null
            )
            var expanded by rememberSaveable { mutableStateOf(true) }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false })
            {
                DropdownMenuItem(
                    onClick = { },
                    text = { Text("Option 1") }
                )
                DropdownMenuItem(
                    onClick = { },
                    text = {Text("Option 2") }
                )
            }
        }
    }
}
 */

private const val FILTER_ALL       = 0x0
private const val FILTER_FOUND     = 0x1
private const val FILTER_NOT_FOUND = 0x2

@Composable
fun TaggedTrack(
    result: TrackWithTags,
    onToggleSelection: () -> Unit,
    onTrackClick: () -> Unit)
{
    val tag    = result.tag
    val title  = tag?.title  ?: result.track.title
    val artist = tag?.artist ?: result.track.artist
    val album  = tag?.album  ?: result.track.album

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { if (result.tags.isNotEmpty()) onTrackClick() },
        verticalAlignment = Alignment.CenterVertically)
    {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(tag?.cover ?: LocalContext.current.resolveArtworkSanitized(result.track.album))
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .transformations(listOf(RoundedCornersTransformation(10f)))
                .placeholder(R.drawable.account_music)
                .error(R.drawable.image)
                .size(with(LocalDensity.current) { 60.dp.roundToPx() })
                .build(),
            null,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .size(60.dp)
        )
        Column(modifier = Modifier
            .fillMaxWidth(0.8f)
            .align(Alignment.CenterVertically))
        {
            Text(title,
                modifier = Modifier.padding(end = 4.dp),
                style    = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$artist • $album",
                modifier = Modifier.padding(end = 4.dp),
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier  = Modifier.padding(end = 4.dp),
                text      = File(result.track.path).name,
                style     = MaterialTheme.typography.bodySmall,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            Text(if (tag != null) "${if (result.apply) '\u2713' else '\u2715'} " +
                 "${stringResource(id = R.string.found)}: ${result.tags.size} "  +
                    stringResource(id = R.string.results) else stringResource(id = R.string.not_found),
                modifier = Modifier.padding(end = 4.dp, bottom = 8.dp),
                color    = Color(if (tag != null && result.apply) 0xff2e9100 else 0xff910000),
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        var expanded by rememberSaveable { mutableStateOf(false) }
        TextButton(
            modifier = Modifier.padding(end = 8.dp),
            onClick  = { if (tag != null) expanded = !expanded })
        {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = null
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false })
            {
                DropdownMenuItem(
                    onClick = {
                        onToggleSelection()
                        expanded = false
                    },
                    text = { Text(stringResource(id = R.string.toggle_selection)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupDetails(
    rootNavController: NavHostController,
    serviceBus: ServiceEventBus)
{
    val context      = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val sheetState   = rememberModalBottomSheetState()

    val lookupResults   by serviceBus.lookupResults.collectAsState()
    val inProgress      by serviceBus.inProgress.collectAsState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false)    }
    var selectedIndex   by rememberSaveable { mutableIntStateOf(0)     }
    var resultsFilter   by rememberSaveable { mutableIntStateOf(FILTER_ALL)  }

    val filteredLookupResults = remember(lookupResults, resultsFilter) {
        when (resultsFilter) {
            FILTER_NOT_FOUND -> lookupResults.filter { it.tag == null }
            FILTER_FOUND     -> lookupResults.filter { it.tag != null }
            else             -> null
        }
    }
    val onBackPressed = {
        context.stopService(Intent(context, TagWriterService::class.java))
        serviceBus.lookupResults.value = mutableListOf()
        rootNavController.navigateUp()
    }
    val mediaStoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startService(Intent(context, TagWriterService::class.java).apply {
                action = ACTION_BATCH_SAVE
                addCategory(CATEGORY_BATCH_OP)
            })
        }
    }

    BackHandler {
        onBackPressed()
    }
    UIEventHandler(serviceBus.eventFlow) {
        if (it is OnBatchProcessingFinished &&
            it.action == ACTION_BATCH_SAVE)
        {
            serviceBus.lookupResults.value = mutableListOf()
            rootNavController.navigateUp()
        }
    }
    LaunchedEffect(Unit) {
        if (!inProgress && lookupResults.isEmpty()) {
            context.startService(
                Intent(context, TagWriterService::class.java).apply {
                    action = ACTION_BATCH_LOOKUP_TAGS
                    addCategory(CATEGORY_BATCH_OP)
                }
            )
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost)
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        stringResource(id = R.string.tag_search),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        enabled = !inProgress && lookupResults.any { it.apply },
                        onClick = {
                            context.createWriteRequest(
                                serviceBus.tracks.map { it.trackUri },
                                mediaStoreLauncher)
                            {
                                context.startService(Intent(context, TagWriterService::class.java).apply {
                                    action = ACTION_BATCH_SAVE
                                    addCategory(CATEGORY_BATCH_OP)
                                })
                            }
                        })
                    {
                        Icon(painterResource(id = R.drawable.content_save), null)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            // TODO: implement
        })
    { innerPadding: PaddingValues ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize())
        {
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState)
                {
                    FetchedMetadata(
                        lookupResults[selectedIndex].tags,
                        false,
                        { tag, withArtwork ->
                            serviceBus.setTag(selectedIndex, tag, withArtwork)
                            showBottomSheet = false
                        },
                        { url ->
                            serviceBus.setArtwork(selectedIndex, url)
                            showBottomSheet = false
                        },
                        { /* refresh */ }
                    )
                }
            }
            Column {
                Row {
                    FilterChip(
                        modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                        selected = resultsFilter == FILTER_ALL,
                        label    = { Text(stringResource(id = R.string.all_results)) },
                        onClick  = { resultsFilter = FILTER_ALL }
                    )
                    FilterChip(
                        modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                        selected = resultsFilter == FILTER_FOUND,
                        label    = { Text(stringResource(id = R.string.found)) },
                        onClick  = { resultsFilter = FILTER_FOUND }
                    )
                    FilterChip(
                        modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                        selected = resultsFilter == FILTER_NOT_FOUND,
                        label    = { Text(stringResource(id = R.string.not_found)) },
                        onClick  = { resultsFilter = FILTER_NOT_FOUND }
                    )
                }
                val results = filteredLookupResults ?: lookupResults
                LazyColumn {
                    items(results.size) { index: Int ->
                        TaggedTrack(
                            results[index],
                            { serviceBus.toggleSelection(index) })
                        {
                            selectedIndex = if (filteredLookupResults != null) {
                                lookupResults.indexOf(filteredLookupResults[index])
                            } else {
                                index
                            }
                            showBottomSheet = true
                        }
                    }
                }
                Spacer(modifier = Modifier.height(75.dp))
            }
            AnimatedProgressIndicator(
                modifier     = Modifier.align(Alignment.BottomStart),
                visible      = inProgress,
                progressInfo = serviceBus.progressInfo)
            {
                context.stopService(Intent(context, TagWriterService::class.java))
            }
        }
    }
}