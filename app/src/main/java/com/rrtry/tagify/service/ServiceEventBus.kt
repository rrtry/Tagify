package com.rrtry.tagify.service

import android.util.Log
import com.jtagger.AbstractTag
import com.jtagger.StreamInfo
import com.rrtry.tagify.data.entities.Tag
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.data.entities.TrackWithTags
import com.rrtry.tagify.data.entities.setArtwork
import com.rrtry.tagify.ui.home.BaseViewModel.ProgressInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

typealias LookupResults = MutableList<TrackWithTags>

@Singleton
class ServiceEventBus @Inject constructor() {

    sealed class Event {

        data class RestoreBackup(
            val file: File,
            val originalPath: String
        ): Event()

        data object OnFileLoadFailed: Event()
        data object OnFileWriteFailed: Event()

        data object OnFileLoaded: Event()
        data object OnFileWritten: Event()

        data class OnBatchProcessingFinished(val action: String): Event()
    }

    @Volatile var tag: AbstractTag?  = null
    @Volatile var track: Track?      = null
    @Volatile var writeApic: Boolean = false
    var streamInfo: StreamInfo?      = null

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()

    val lookupResults: MutableStateFlow<LookupResults> = MutableStateFlow(mutableListOf())
    val inProgress:    MutableStateFlow<Boolean>       = MutableStateFlow(false)
    val progressInfo:  MutableStateFlow<ProgressInfo>  = MutableStateFlow(ProgressInfo(null, 0, 0))

    var tracks: List<Track> = listOf()

    fun setTag(index: Int, tag: Tag, withArtwork: Boolean) {
        val updated    = lookupResults.value.toMutableList()
        val current    = updated[index]
        updated[index] = current.copy(tag = tag.apply { if (!withArtwork) cover = current.tag?.cover })
        lookupResults.value = updated
    }

    fun setArtwork(index: Int, url: URL) {
        if (lookupResults.value[index].tag != null) {
            val updated = lookupResults.value.toMutableList()
            val current = updated[index]
            updated[index] = current.copy(tag = setArtwork(current.tag!!, url))
            lookupResults.value = updated
        }
    }

    fun toggleSelection(index: Int) {
        if (lookupResults.value[index].tag != null) {
            val updated    = lookupResults.value.toMutableList()
            val current    = updated[index]
            updated[index] = current.copy(apply = !current.apply)
            lookupResults.value = updated
        }
    }

    suspend fun send(event: Event) {
        eventChannel.send(event)
    }

    fun trySend(event: Event) {
        eventChannel.trySend(event)
    }
}