package com.rrtry.tagify.ui.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rrtry.tagify.ACTION_BATCH_LOOKUP_ALBUM_ARTWORK
import com.rrtry.tagify.ACTION_SCAN_MEDIA
import com.rrtry.tagify.R
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.ui.components.Artwork
import com.rrtry.tagify.ui.components.GroupBy
import com.rrtry.tagify.ui.components.GroupedTrackList
import com.rrtry.tagify.ui.components.ImagePicker
import com.rrtry.tagify.ui.components.ImagePickerLauncher
import com.rrtry.tagify.ui.components.Style
import com.rrtry.tagify.ui.components.UIEventHandler
import com.rrtry.tagify.ui.components.handleOnStartBatchService
import com.rrtry.tagify.ui.components.handleOnTrackClick
import com.rrtry.tagify.ui.home.AnimatedProgressIndicator
import com.rrtry.tagify.ui.home.BaseViewModel
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent.OnImagePickerLaunch
import com.rrtry.tagify.ui.home.BaseViewModel.UIEvent.OnStartBatchService
import com.rrtry.tagify.ui.home.BaseViewModel.UIEvent.OnStopService
import com.rrtry.tagify.ui.home.BaseViewModel.UIEvent.OnOpenLookupDetails
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent.OnTrackClick
import com.rrtry.tagify.ui.home.MultiSelectionBottomBar
import com.rrtry.tagify.ui.home.openLookupDetails
import com.rrtry.tagify.util.resolveArtworkSanitized

@Composable
fun CollectionDetails(
    navController: NavController,
    viewModel: BaseViewModel,
    serviceBus: ServiceEventBus,
    tracks: List<Track>?,
    title: String,
    subtitles: Pair<String?, String?>,
    artwork: String?,
    groupBy: GroupBy,
    style: Style)
{
    if (tracks != null && tracks.isEmpty()) {
        navController.navigateUp()
    }

    val localContext  = LocalContext.current
    val hostState     = remember { SnackbarHostState()   }
    val imagePicker   = remember { ImagePickerLauncher() }
    val scrollState   = rememberScrollState()
    val gradientColor = MaterialTheme.colorScheme.background

    val mediaStoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.pendingEvent?.let {
                viewModel.sendEvent(it)
            }
        }
    }

    val inProgress      by serviceBus.inProgress.collectAsStateWithLifecycle()
    val selectionMode   by viewModel.selectionMode.collectAsState()
    val showAlbumDialog by viewModel.showAlbumDialog.collectAsState()
    val artworkFile     by remember {
        derivedStateOf {
            if (!inProgress && artwork != null) {
                localContext.resolveArtworkSanitized(artwork)
            } else {
                null
            }
        }
    }
    ImagePicker(imagePicker, viewModel)
    LifecycleStartEffect(inProgress) {
        viewModel.batchOperationInProgress = if (inProgress) serviceBus.progressInfo.value.action else null
        onStopOrDispose {}
    }
    UIEventHandler(viewModel.eventFlow) { event ->
        when (event) {
            is OnImagePickerLaunch -> imagePicker.launch()
            is OnOpenLookupDetails -> {
                val tracks = viewModel.getTracks()
                serviceBus.tracks = ArrayList<Track>().apply {
                    ensureCapacity(viewModel.selectedTracks.value.size)
                    for (index in viewModel.selectedTracks.value) {
                        add(tracks[index])
                    }
                }
                openLookupDetails(navController)
            }
            is OnTrackClick -> handleOnTrackClick(
                localContext, viewModel, event, mediaStoreLauncher, navController
            )
            is OnStartBatchService -> handleOnStartBatchService(
                localContext, event, mediaStoreLauncher, viewModel, serviceBus
            )
            is OnStopService -> {
                if (event.action != ACTION_SCAN_MEDIA) {
                    viewModel.batchOperationInProgress = null
                    localContext.stopService(Intent(localContext, TagWriterService::class.java))
                }
            }
            else -> {}
        }
    }
    Scaffold(
        snackbarHost = {
             SnackbarHost(hostState = hostState)
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInVertically(initialOffsetY = { it / 2 }),
                exit = slideOutVertically(targetOffsetY  = { it }))
            {
                MultiSelectionBottomBar(viewModel)
            }
        }
    )
    { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(state = scrollState))
            {
                Artwork(
                    artworkFile,
                    gradientColor,
                    R.drawable.album,
                    null)
                {

                }
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (subtitles.first != null) {
                    Text(
                        modifier   = Modifier.padding(16.dp),
                        text       = subtitles.first!!,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (subtitles.second != null) {
                    Text(
                        modifier   = Modifier.padding(16.dp),
                        text       = subtitles.second!!,
                        color      = MaterialTheme.colorScheme.primary,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                GroupedTrackList(
                    selectedTracksState = viewModel.selectedTracks,
                    tracks = tracks,
                    onClick = { index, track ->
                        if (viewModel.selectionMode.value) {
                            viewModel.onClickSelect(index)
                        } else {
                            viewModel.sendEvent(OnTrackClick(track))
                        }
                    },
                    onLongClick = { index, _ ->
                        viewModel.onLongClickSelect(index)
                    },
                    groupBy = groupBy,
                    style = style
                )
                Spacer(modifier = Modifier.height(75.dp))
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
    if (showAlbumDialog) {
        LookupQueryDialog(
            Pair(viewModel.artistQuery, viewModel.albumQuery),
            Pair(R.string.artist, R.string.album),
            { artist, album ->
                viewModel.startBatchService(
                    ACTION_BATCH_LOOKUP_ALBUM_ARTWORK,
                    Bundle().apply {
                        putString(TagWriterService.EXTRA_ARTIST, artist)
                        putString(TagWriterService.EXTRA_ALBUM, album)
                    }
                )
            },
            { viewModel.showAlbumDialog.value = false }
        )
    }
}