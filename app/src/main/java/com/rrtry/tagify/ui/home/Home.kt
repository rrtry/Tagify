package com.rrtry.tagify.ui.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rrtry.tagify.ACTION_BATCH_LOOKUP_ALBUM_ARTWORK
import com.rrtry.tagify.ACTION_BATCH_REMOVE_ARTWORK
import com.rrtry.tagify.ACTION_BATCH_REMOVE_TAGS
import com.rrtry.tagify.ACTION_BATCH_TAG_FROM_FILENAME
import com.rrtry.tagify.ACTION_SCAN_MEDIA
import com.rrtry.tagify.NAV_ARG_ALBUM
import com.rrtry.tagify.NAV_ARG_ARTIST
import com.rrtry.tagify.navigate
import com.rrtry.tagify.NAV_ARG_TRACK
import com.rrtry.tagify.R
import com.rrtry.tagify.SCREEN_ALBUM_DETAILS
import com.rrtry.tagify.SCREEN_ARTIST_DETAILS
import com.rrtry.tagify.SCREEN_LOOKUP_DETAILS
import com.rrtry.tagify.SCREEN_PREFERENCES
import com.rrtry.tagify.SCREEN_TRACK_DETAILS
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.service.MediaScannerService
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.service.TagWriterService.Companion.EXTRA_ALBUM
import com.rrtry.tagify.service.TagWriterService.Companion.EXTRA_ARTIST
import com.rrtry.tagify.service.getActionDisplayNameRes
import com.rrtry.tagify.ui.components.FileRestoreDialog
import com.rrtry.tagify.ui.components.ImagePicker
import com.rrtry.tagify.ui.components.ImagePickerLauncher
import com.rrtry.tagify.ui.components.UIEventHandler
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent
import com.rrtry.tagify.ui.components.RadioButtonDialog
import com.rrtry.tagify.ui.components.RadioButtonOptionInfo
import com.rrtry.tagify.ui.components.handleOnStartBatchService
import com.rrtry.tagify.ui.components.handleOnTrackClick
import com.rrtry.tagify.ui.details.LookupQueryDialog
import com.rrtry.tagify.ui.home.MediaViewModel.FilterOption
import com.rrtry.tagify.ui.home.MediaViewModel.TopBar.*
import kotlinx.coroutines.flow.MutableStateFlow
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent.*
import com.rrtry.tagify.ui.home.BaseViewModel.UIEvent.*
import com.rrtry.tagify.util.getBackup
import kotlinx.coroutines.launch

private const val SCREEN_TRACKS  = "Tracks"
private const val SCREEN_ALBUMS  = "Albums"
private const val SCREEN_ARTISTS = "Artists"

private const val TOPBAR_VISIBILITY_LABEL      = "topbar_visibility"
private const val PROGRESSBAR_VISIBILITY_LABEL = "progressbar_visibility"

sealed class Screen(val route: String,
                    @StringRes val descRes: Int,
                    @DrawableRes val drawableRes: Int)
{
    data object Tracks  : Screen(SCREEN_TRACKS,  R.string.tracks_route,  R.drawable.music)
    data object Albums  : Screen(SCREEN_ALBUMS,  R.string.albums_route,  R.drawable.album)
    data object Artists : Screen(SCREEN_ARTISTS, R.string.artists_route, R.drawable.account_music)
}

private val navScreens = listOf(
    Screen.Tracks,
    Screen.Albums,
    Screen.Artists
)

fun openPreferences(navController: NavController) {
    navController.navigate(route = SCREEN_PREFERENCES)
}

fun openLookupDetails(navController: NavController) {
    navController.navigate(route = SCREEN_LOOKUP_DETAILS)
}

fun openTrackDetails(navController: NavController, track: Track) {
    navController.navigate(route = SCREEN_TRACK_DETAILS, args = bundleOf(
        NAV_ARG_TRACK to track
    ))
}

fun openAlbumDetails(navController: NavController, album: Album) {
    navController.navigate(route = SCREEN_ALBUM_DETAILS, args = bundleOf(
            NAV_ARG_ALBUM to album
    ))
}

fun openArtistDetails(navController: NavController, artist: Artist) {
    navController.navigate(route = SCREEN_ARTIST_DETAILS, args = bundleOf(
        NAV_ARG_ARTIST to artist
    ))
}

@Composable
fun MultiSelectionTopBar(viewModel: MediaViewModel) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background)
        .height(64.dp),
        verticalAlignment = Alignment.CenterVertically)
    {
        IconButton(onClick = { viewModel.clearSelectionMode() }) {
            Icon(painter = painterResource(id = R.drawable.baseline_cancel_24), contentDescription = null)
        }
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.selection_mode),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionButton(
    action: () -> Unit,
    @DrawableRes drawableRes: Int,
    @StringRes stringRes: Int)
{
    TextButton(onClick = { action() }) {
        Column {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(24.dp),
                painter = painterResource(id = drawableRes),
                contentDescription = null
            )
            Text(
                textAlign = TextAlign.Center,
                modifier  = Modifier.width(80.dp),
                text = stringResource(id = stringRes),
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MultiSelectionBottomBar(viewModel: BaseViewModel) {
    val selectedTracks by viewModel.selectedTracks.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Icon(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .clickable { viewModel.clearSelectionMode() },
                    painter = painterResource(id = R.drawable.baseline_cancel_24),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "${stringResource(id = R.string.selected)}: ${selectedTracks.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(modifier = Modifier
                .padding(8.dp)
                .align(Alignment.Bottom), onClick = { viewModel.selectAll() }) {
                Text(text = stringResource(id = R.string.select_all))
            }
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround)
        {
            ActionButton(
                {
                    viewModel.sendEvent(OnOpenLookupDetails) // ACTION_BATCH_LOOKUP
                },
                R.drawable.baseline_search_24,
                R.string.batch_operation_lookup_tags
            )
            ActionButton(
                { viewModel.startBatchService(ACTION_BATCH_REMOVE_TAGS) }, // ACTION_BATCH_REMOVE_TAG
                R.drawable.baseline_bookmark_remove_24,
                R.string.batch_operation_remove_tags
            )
            ActionButton(
                { viewModel.startBatchService(ACTION_BATCH_REMOVE_ARTWORK) }, // ACTION_BATCH_REMOVE_ARTWORK
                R.drawable.baseline_hide_image_24,
                R.string.batch_operation_remove_artwork
            )
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround)
        {
            ActionButton(
                { viewModel.sendEvent(OnImagePickerLaunch) }, // ACTION_BATCH_SET_ARTWORK_FROM_FILE
                R.drawable.image,
                R.string.batch_operation_set_artwork_from_file
            )
            ActionButton(
                { viewModel.startBatchService(ACTION_BATCH_TAG_FROM_FILENAME) }, // ACTION_BATCH_TAG_FROM_FILENAME
                R.drawable.file_music,
                R.string.batch_operation_tag_from_filename
            )
        }
    }
}

@Composable
fun SearchBar(viewModel: MediaViewModel) {
    Surface(modifier = Modifier
        .fillMaxWidth()
        .then(Modifier.height(64.dp)),
        color = MaterialTheme.colorScheme.background)
    {
        Row(Modifier.padding(3.dp)) {
            val searchQuery by viewModel.searchQuery.collectAsState()
            IconButton(modifier = Modifier.padding(5.dp),
                onClick = {
                    viewModel.clearSearchMode()
                })
            {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            TextField(
                modifier   = Modifier.fillMaxWidth(),
                value      = searchQuery,
                singleLine = true,
                label      = { Text(text = stringResource(id = R.string.search)) },
                onValueChange = { newValue: String ->
                    viewModel.searchQuery.value = newValue
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBar(viewModel: MediaViewModel, mediaContent: MediaViewModel.MediaContent) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Column {
                Text(
                    mediaContent.type.capitalize(Locale.current),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${stringResource(id = R.string.count)}: ${mediaContent.count}",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            IconButton(onClick = { viewModel.sendEvent(UIEvent.OnSettingsClick)}) {
                Icon(painter = painterResource(id = R.drawable.baseline_settings_24), null)
            }
            IconButton(onClick = { viewModel.sendEvent(OnStartScanService)}) {
                Icon(painter = painterResource(id = R.drawable.baseline_refresh_24), null)
            }
            if (mediaContent.type == Screen.Tracks.route) {
                IconButton(onClick = { viewModel.showFilterDialog.value = true }) {
                    Icon(painter = painterResource(id = R.drawable.baseline_filter_list_24), null)
                }
            }
            IconButton(onClick = {
                viewModel.enableSearchMode()
            })
            {
                Icon(painter = painterResource(id = R.drawable.magnify), null)
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* do something */ }) {
                Icon(imageVector = Icons.Filled.Menu, null)
            }
        }
    )
}

@Composable
fun TopBar(viewModel: MediaViewModel) {

    val mediaContent by viewModel.mediaContent.collectAsState()
    val topBarState  by viewModel.topBarState.collectAsState()

    Crossfade(targetState = topBarState, label = TOPBAR_VISIBILITY_LABEL) {
        when (it) {
            DEFAULT -> ToolBar(viewModel, mediaContent)
            SEARCH  -> SearchBar(viewModel)
            else    -> MultiSelectionTopBar(viewModel)
        }
    }
}

@Composable
fun TrackFilterOptionsDialog(viewModel: MediaViewModel, onDismissRequest: () -> Unit) {
    RadioButtonDialog(
        R.string.filter_options_title,
        listOf(
            RadioButtonOptionInfo(
                R.string.filter_option_tagged_only,
                FilterOption.TAGGED,
                { onDismissRequest(); viewModel.filterOption.value = it as FilterOption },
                { it == FilterOption.TAGGED }
            ),
            RadioButtonOptionInfo(
                R.string.filter_option_untagged_only,
                FilterOption.UNTAGGED,
                { onDismissRequest(); viewModel.filterOption.value = it as FilterOption },
                { it == FilterOption.UNTAGGED }
            ),
            RadioButtonOptionInfo(
                R.string.filter_option_all,
                FilterOption.ALL,
                { onDismissRequest(); viewModel.filterOption.value = it as FilterOption },
                { it == FilterOption.ALL }
            )
        ),
        viewModel.filterOption as MutableStateFlow<Any?>,
        onDismissRequest
    )
}

@Composable
fun DynamicBottomBar(viewModel: MediaViewModel, navController: NavHostController) {
    val selectionMode by viewModel.selectionMode.collectAsState()
    NavigationBar(modifier = Modifier.animateContentSize()) {
        if (!selectionMode) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            navScreens.forEach { screen ->
                NavigationBarItem(
                    icon     = { Icon(painter = painterResource(id = screen.drawableRes), contentDescription = null) },
                    label    = { Text(stringResource(screen.descRes)) },
                    selected = currentDestination?.hierarchy?.any { selected -> selected.route == screen.route } == true,
                    onClick  = {

                        viewModel.currentRoute.value = screen.route
                        viewModel.clearSearchMode()

                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        } else {
            MultiSelectionBottomBar(viewModel)
        }
    }
}

@Composable
fun ProgressIndicator(
    progressInfoState: MutableStateFlow<BaseViewModel.ProgressInfo>,
    onStop: () -> Unit)
{
    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer

    val progressInfo by progressInfoState.collectAsState()
    val progress     by remember {
        derivedStateOf {
            progressInfo.processed.toFloat() / progressInfo.total
        }
    }

    Column(Modifier.zIndex(2f)) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(shape = RoundedCornerShape(8.dp), width = 2.dp, color = primaryColor)
                .background(secondaryColor)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically)
        {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text  = String.format(
                    "%s %d%% (%d/%d)",
                    stringResource(id = getActionDisplayNameRes(progressInfo.action)),
                    (progress * 100).toInt(),
                    progressInfo.processed,
                    progressInfo.total
                ),
                modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                modifier = Modifier
                    .padding(8.dp)
                    .size(35.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                onClick = { onStop() })
            {
                Icon(Icons.Filled.Clear, null)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AnimatedProgressIndicator(modifier: Modifier,
                              visible: Boolean,
                              progressInfo: MutableStateFlow<BaseViewModel.ProgressInfo>,
                              onStop: () -> Unit)
{
    AnimatedVisibility(
        visible  = visible,
        modifier = modifier,
        enter    = fadeIn(tween(600))  + scaleIn(tween(300)),
        exit     = fadeOut(tween(300)) + scaleOut(tween(600)),
        label    = PROGRESSBAR_VISIBILITY_LABEL)
    {
        ProgressIndicator(progressInfo) {
            onStop()
        }
    }
}

@Composable
fun HomeScreen(rootNavController: NavHostController,
               serviceBus: ServiceEventBus,
               viewModel: MediaViewModel = hiltViewModel())
{
    val navController  = rememberNavController()
    val snackbarHost   = remember { SnackbarHostState()   }
    val imagePicker    = remember { ImagePickerLauncher() }
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current

    val inProgress        by serviceBus.inProgress.collectAsStateWithLifecycle()
    val showFilterDialog  by viewModel.showFilterDialog.collectAsState()
    val showRestoreDialog by viewModel.showRestoreDialog.collectAsState()
    val showAlbumDialog   by viewModel.showAlbumDialog.collectAsState()

    val mediaStoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.pendingEvent?.let {
                viewModel.sendEvent(it)
            }
        }
    }
    ImagePicker(imagePicker, viewModel)
    LifecycleEventEffect(event = Lifecycle.Event.ON_CREATE) {
        if (!serviceBus.inProgress.value) {
            getBackup(context)?.let { backupFile ->
                viewModel.setBackupFile(backupFile)
            }
        }
    }
    LifecycleStartEffect(inProgress) {
        if (inProgress) {
            val progress = serviceBus.progressInfo.value
            if (progress.action == ACTION_SCAN_MEDIA) {
                viewModel.mediaScanInProgress = true
            } else {
                viewModel.batchOperationInProgress = progress.action
            }
        } else {
            viewModel.mediaScanInProgress = false
            viewModel.batchOperationInProgress   = null
        }
        onStopOrDispose {}
    }
    UIEventHandler(eventFlow = viewModel.eventFlow) { event ->
        when (event) {

            is OnOpenLookupDetails -> {
                // Pass track list through singleton instead of intent's bundle
                serviceBus.tracks = ArrayList<Track>().apply {
                    ensureCapacity(viewModel.selectedTracks.value.size)
                    for (index in viewModel.selectedTracks.value) {
                        add(viewModel.tracks.value[index])
                    }
                }
                openLookupDetails(rootNavController)
            }
            is OnTrackClick -> {
                handleOnTrackClick(
                    context, viewModel, event,
                    mediaStoreLauncher, rootNavController
                )
            }

            is OnSettingsClick -> openPreferences(rootNavController)
            is OnAlbumClick    -> openAlbumDetails(rootNavController, event.album)
            is OnArtistClick   -> openArtistDetails(rootNavController, event.artist)

            is OnStartScanService -> MediaScannerService.startService(context)
            is OnStopService      -> {
                if (event.action == ACTION_SCAN_MEDIA) {
                    viewModel.mediaScanInProgress = false
                    context.stopService(Intent(context, MediaScannerService::class.java))
                } else {
                    viewModel.batchOperationInProgress = null
                    context.stopService(Intent(context, TagWriterService::class.java))
                }
            }

            is OnImagePickerLaunch -> imagePicker.launch()
            is OnStartBatchService -> handleOnStartBatchService(
                context, event, mediaStoreLauncher, viewModel, serviceBus
            )

            is OnBackupRestoreSucceeded -> {
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        message  = event.path,
                        duration = SnackbarDuration.Long,
                    )
                }
            }
            is OnBackupRestoreFailed -> {
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        message  = context.getString(R.string.backup_restore_failed),
                        duration = SnackbarDuration.Short
                    )
                }
            }
            is OnBackupDeleteFailed -> {
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        message  = context.getString(R.string.backup_delete_failed),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    if (showAlbumDialog) {
        LookupQueryDialog(
            Pair(viewModel.artistQuery, viewModel.albumQuery),
            Pair(R.string.artist, R.string.album),
            { artist, album ->
                viewModel.startBatchService(
                    ACTION_BATCH_LOOKUP_ALBUM_ARTWORK,
                    Bundle().apply {
                        putString(EXTRA_ARTIST, artist)
                        putString(EXTRA_ALBUM, album)
                    }
                )
            },
            { viewModel.showAlbumDialog.value = false }
        )
    }
    if (showFilterDialog) {
        TrackFilterOptionsDialog(viewModel = viewModel) {
            viewModel.showFilterDialog.value = false
        }
    }
    if (showRestoreDialog) {
        FileRestoreDialog(
            viewModel.backupFile!!,
            {
                val success = viewModel.backupFile!!.file.run {
                    delete()
                }
                if (!success) {
                    viewModel.sendEvent(OnBackupDeleteFailed)
                }
                viewModel.showRestoreDialog.value = false
                viewModel.backupFile = null
            })
        {
            viewModel.restoreBackup()
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost)
        },
        topBar = {
            TopBar(viewModel)
        },
        bottomBar = {
            DynamicBottomBar(viewModel, navController)
        })
    { innerPadding: PaddingValues ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize())
        {
            NavHost(navController, startDestination = Screen.Tracks.route) {
                composable(route = Screen.Tracks.route) {
                    Tracks(viewModel)
                }
                composable(route = Screen.Albums.route) {
                    Albums(viewModel)
                }
                composable(route = Screen.Artists.route) {
                    Artists(viewModel)
                }
            }
            AnimatedProgressIndicator(
                modifier     = Modifier.align(Alignment.BottomStart),
                visible      = inProgress,
                progressInfo = serviceBus.progressInfo)
            {
                viewModel.stopService(serviceBus.progressInfo.value.action!!)
            }
        }
    }
}