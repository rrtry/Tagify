package com.rrtry.tagify.ui.components

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.rrtry.tagify.CATEGORY_BATCH_OP
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.ui.home.BaseViewModel
import com.rrtry.tagify.ui.home.openTrackDetails
import com.rrtry.tagify.util.createWriteRequest
import kotlinx.coroutines.flow.Flow
import com.rrtry.tagify.ui.home.MediaViewModel.UIEvent.*
import com.rrtry.tagify.ui.home.BaseViewModel.UIEvent.*

typealias MediaStoreLauncher = ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>

fun handleOnStartBatchService(
    context:    Context,
    event:      OnStartBatchService,
    launcher:   MediaStoreLauncher,
    viewModel:  BaseViewModel,
    serviceBus: ServiceEventBus)
{
    viewModel.pendingEvent = event
    context.createWriteRequest(
        event.tracks.map { it.trackUri },
        launcher)
    {
        viewModel.pendingEvent = null
        viewModel.clearSelectionMode()
        viewModel.batchOperationInProgress = event.action
        serviceBus.tracks = event.tracks
        context.startService(
            Intent(context, TagWriterService::class.java).apply {
                action = event.action
                addCategory(CATEGORY_BATCH_OP)
                if (event.extras != null) putExtras(event.extras)
            }
        )
    }
}

fun handleOnTrackClick(context:       Context,
                       viewModel:     BaseViewModel,
                       event:         OnTrackClick,
                       launcher:      MediaStoreLauncher,
                       navController: NavController)
{
    viewModel.pendingEvent = event
    context.createWriteRequest(
        listOf(event.track.trackUri),
        launcher)
    {
        viewModel.pendingEvent = null
        openTrackDetails(navController, event.track)
    }
}

/*
@Composable
fun ServiceEventHandler(
    eventFlow: SharedFlow<ServiceEventBus.Event>,
    viewModel: BaseViewModel,
    events:    List<Class<out ServiceEventBus.Event>>? = null)
{
    UIEventHandler(eventFlow) { event ->
        if (events == null || events.any { it == event::class.java }) {
            when (event) {
                is RestoreBackup -> {
                    val mediaViewModel = viewModel as MediaViewModel
                    if (mediaViewModel.backupFile == null) {
                        var backup = MediaViewModel.BackupFile(
                            "Unknown",
                            "Unknown",
                            "Unknown",
                            "audio/x-unknown",
                            event.file,
                            event.originalPath
                        )
                        withContext(Dispatchers.IO) {
                            val mediaFile = MediaFile<AbstractTag, StreamInfo>()
                            try {
                                mediaFile.scanCatching(event.file, "r")
                                mediaFile.tag?.let { tag ->
                                    backup = MediaViewModel.BackupFile(
                                        tag.getStringField(AbstractTag.TITLE),
                                        tag.getStringField(AbstractTag.ARTIST),
                                        tag.getStringField(AbstractTag.ALBUM),
                                        mediaFile.mimeType,
                                        event.file,
                                        event.originalPath
                                    )
                                }
                            } finally {
                                mediaFile.closeQuietly()
                            }
                        }
                        mediaViewModel.showRestoreDialog.value = true
                        mediaViewModel.backupFile = backup
                    }
                }

                is OnScannerServiceConnected -> {
                    (viewModel as MediaViewModel).updateScanProgress(event.progress)
                }
                is OnWriterServiceConnected -> {
                    if (event.action != ACTION_LOAD_TAG &&
                        event.action != ACTION_SAVE_TAG)
                    {
                        viewModel.updateBatchOperationProgress(event.action!!, event.progress)
                    }
                }

                is OnFileProcessed -> { /* viewModel.progressInfo.update { event.progressInfo } */ }
                is OnFileScanned   -> (viewModel as MediaViewModel).updateScanProgress(event.progressInfo)

                is OnBatchProcessingStarted  -> viewModel.updateBatchOperationProgress(event.action, event.progressInfo)
                is OnMediaScanStarted        -> (viewModel as MediaViewModel).updateScanProgress(event.progressInfo)

                is OnBatchProcessingFinished -> { viewModel.batchOperation.value = null }
                is OnMediaScanFinished       -> { (viewModel as MediaViewModel).showScanProgress.value = false }

                else -> {
                    Log.d("ServiceEventBus", "Event::$event")
                }
            }
        }
    }
}
*/

@Composable
fun <T : Any> UIEventHandler(
    eventFlow: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit)
{
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(eventFlow) {
        eventFlow
            .flowWithLifecycle(lifecycleOwner.lifecycle, lifecycleState)
            .collect {
                collector(it)
            }
    }
}