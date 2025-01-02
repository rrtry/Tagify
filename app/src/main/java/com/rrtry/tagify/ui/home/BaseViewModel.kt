package com.rrtry.tagify.ui.home

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

abstract class BaseViewModel: ViewModel() {

    data class ProgressInfo(
        val action: String?,
        val processed: Int,
        val total: Int
    )

    sealed class UIEvent {

        data class OnStartBatchService(
            val tracks: ArrayList<Track>,
            val action: String,
            val extras: Bundle? = null
        ): UIEvent()
        data class OnStopService(val action: String): UIEvent()
        data object OnOpenLookupDetails: UIEvent()
    }

    protected val eventChannel = Channel<UIEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow().shareIn(
        viewModelScope, SharingStarted.WhileSubscribed(0L)
    )

    var batchOperationInProgress: String? = null
    val selectedTracks  = MutableStateFlow(linkedSetOf<Int>())
    val topBarState     = MutableStateFlow(MediaViewModel.TopBar.DEFAULT)
    val selectionMode   = MutableStateFlow(false)
    val showAlbumDialog = MutableStateFlow(false)

    val artistQuery = MutableStateFlow("")
    val albumQuery  = MutableStateFlow("")
    var pendingEvent: UIEvent? = null

    abstract fun getTracks(): List<Track>

    open fun clearSelectionMode() {
        selectionMode.value  = false
        topBarState.value    = MediaViewModel.TopBar.DEFAULT
        selectedTracks.value = linkedSetOf()
    }

    private fun enableSelectionMode() {
        selectionMode.value = true
        topBarState.value   = MediaViewModel.TopBar.SELECTION
    }

    fun selectAll() {
        enableSelectionMode()
        selectedTracks.value = LinkedHashSet<Int>().apply {
            (0..<getTracks().size).forEach { index ->
                add(index)
            }
        }
    }

    fun addSelectionRange(range: IntRange) {
        selectedTracks.value = (selectedTracks.value + range) as LinkedHashSet<Int>
    }

    fun removeSelectionRange(range: IntRange) {
        selectedTracks.value = (selectedTracks.value - range) as LinkedHashSet<Int>
    }

    fun addSelection(index: Int) {
        selectedTracks.value = (selectedTracks.value + index) as LinkedHashSet<Int>
    }

    fun removeSelection(index: Int) {
        selectedTracks.value = (selectedTracks.value - index) as LinkedHashSet<Int>
    }

    fun onClickSelect(index: Int) {
        enableSelectionMode()
        if (index in selectedTracks.value) {
            removeSelection(index)
        } else {
            addSelection(index)
        }
        if (selectedTracks.value.isEmpty()) {
            clearSelectionMode()
        }
    }

    open fun onLongClickSelect(index: Int) {

        if (batchOperationInProgress != null) return
        val tracks = getTracks()

        if (!selectionMode.value) {
            onClickSelect(index)
        }
        else {

            val firstSelection = tracks.indices.first {
                it in selectedTracks.value
            }

            var reversed = false
            val removeRange = index + 1..<tracks.size
            val addRange = if (index > firstSelection) {
                firstSelection..index
            } else {
                reversed = true
                index..firstSelection
            }
            if (firstSelection == index || !reversed) {
                if (!removeRange.isEmpty()) removeSelectionRange(removeRange)
            }
            if (firstSelection != index) {
                addSelectionRange(addRange)
            }
        }
    }

    fun startBatchService(action: String, bundle: Bundle? = null) {
        viewModelScope.launch {
            val tracks = getTracks()
            eventChannel.send(
                UIEvent.OnStartBatchService(
                    ArrayList<Track>(selectedTracks.value.size).apply {
                        ensureCapacity(selectedTracks.value.size)
                        selectedTracks.value.forEach { index ->
                            add(tracks[index])
                        }
                    },
                    action,
                    bundle
                )
            )
        }
    }

    fun stopService(action: String) {
        viewModelScope.launch {
            eventChannel.send(UIEvent.OnStopService(action))
        }
    }

    open fun sendEvent(event: UIEvent) {
        if (batchOperationInProgress != null) return
        viewModelScope.launch {
            eventChannel.send(event)
        }
    }
}