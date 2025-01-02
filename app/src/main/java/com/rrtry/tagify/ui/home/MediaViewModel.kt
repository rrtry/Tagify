package com.rrtry.tagify.ui.home

import android.content.Context
import android.os.Environment
import androidx.lifecycle.viewModelScope
import com.jtagger.AbstractTag
import com.jtagger.MediaFile
import com.jtagger.StreamInfo
import com.jtagger.utils.BytesIO.BUFFER_SIZE
import com.rrtry.tagify.R
import com.rrtry.tagify.RESTORED_FILES_DIR
import com.rrtry.tagify.data.MediaRepository
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.prefs.prefsGetLastModifiedFile
import com.rrtry.tagify.util.asyncMap
import com.rrtry.tagify.util.scanCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Long
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    @ApplicationContext private val appCxt: Context,
    mediaRepository: MediaRepository): BaseViewModel()
{
    sealed class UIEvent: BaseViewModel.UIEvent() {

        data object OnStartScanService: UIEvent()
        data object OnSettingsClick: UIEvent()
        data object OnImagePickerLaunch: UIEvent()

        data class OnBackupRestoreSucceeded(val path: String): UIEvent()
        data object OnBackupRestoreFailed: UIEvent()
        data object OnBackupDeleteFailed:  UIEvent()

        data class OnTrackClick(val track: Track):    UIEvent()
        data class OnAlbumClick(val album: Album):    UIEvent()
        data class OnArtistClick(val artist: Artist): UIEvent()
    }

    data class MediaContent(val type: String, val count: Int)
    data class BackupFile(
        val title:  String,
        val artist: String,
        val album:  String,
        val type:   String,
        val file:   File,
        val originalLocation: String
    )

    enum class FilterOption {
        ALL, UNTAGGED, TAGGED
    }

    enum class TopBar {
        DEFAULT, SELECTION, SEARCH
    }

    val searchMode   = MutableStateFlow(false)
    val searchQuery  = MutableStateFlow("")
    val currentRoute = MutableStateFlow(Screen.Tracks.route)
    val filterOption = MutableStateFlow(FilterOption.ALL)

    val tracks = combine(mediaRepository.observeTracks(), searchQuery, filterOption, searchMode) { tracks, query, filterOption, searchEnabled ->
        if (filterOption == FilterOption.ALL && (!searchEnabled || query.isBlank())) {
            tracks
        } else {
            tracks.filter {
                val filter = filterOption == FilterOption.ALL ||
                        ((it.hasTag && filterOption == FilterOption.TAGGED) ||
                        (!it.hasTag && filterOption == FilterOption.UNTAGGED))
                if (searchEnabled && query.isNotBlank()) {
                    (it.artist.contains(query, true) ||
                    it.album.contains(query, true)   ||
                    it.title.contains(query, true)) && filter
                } else {
                    filter
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    val albums = combine(mediaRepository.observeAlbums(), searchQuery, searchMode) { album, query, searchEnabled ->
        if (searchEnabled && query.isNotBlank()) {
            album.filter {
                it.title!!.contains(query, true)
            }
        } else {
            album
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    val artists = combine(
        mediaRepository.observeArtists().map { artists ->
            artists.asyncMap { artist ->
                if (artist.firstAlbum == null) {
                    artist.firstAlbum = mediaRepository.getFirstAlbum(artist.name!!)
                }
                artist
            }
        },
        searchQuery,
        searchMode)
    { artists, query, searchEnabled ->
        if (searchEnabled && query.isNotBlank()) {
            artists.filter {
                it.name!!.contains(query, true)
            }
        } else {
            artists
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    val mediaContent = combine(tracks, albums, artists, currentRoute) { tracks, albums, artists, page ->
        when (page) {
            Screen.Tracks.route  -> MediaContent(page, tracks.size)
            Screen.Albums.route  -> MediaContent(page, albums.size)
            Screen.Artists.route -> MediaContent(page, artists.size)
            else -> throw IllegalArgumentException("Unknown route: $page")
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), MediaContent(Screen.Tracks.route, 0)
    )

    val showFilterDialog  = MutableStateFlow(false)
    val showRestoreDialog = MutableStateFlow(false)

    var mediaScanInProgress     = false
    var backupFile: BackupFile? = null

    fun restoreBackup() {
        if (backupFile != null) {

            val inFile    = backupFile!!.file
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .resolve(RESTORED_FILES_DIR).apply {
                    if (!exists()) mkdir()
                }

            val extension = backupFile!!.originalLocation.substringAfterLast('.', "")
            var filename  = backupFile!!.originalLocation.substringBeforeLast('.')

            if (extension.isNotEmpty()) {
                filename += extension
            }

            val outFile = directory.resolve(filename)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    val buffer  = ByteArray(BUFFER_SIZE)
                    val length  = inFile.length()
                    var read    = 0L
                    var success = false
                    var size: Int

                    try {
                        FileInputStream(inFile).use { input ->
                            FileOutputStream(outFile).use { out ->
                                while (read < length) {
                                    ensureActive()
                                    size = Long.min(BUFFER_SIZE.toLong(), length - read).toInt()
                                    input.read(buffer, 0, size)
                                    out.write(buffer)
                                    read += size
                                }
                                success = read == length
                            }
                        }
                    } catch (e: IOException) {
                        success = false
                    } finally {
                        sendEvent(if (success) {
                            UIEvent.OnBackupRestoreSucceeded(outFile.canonicalPath)
                        } else {
                            UIEvent.OnBackupRestoreFailed
                        })
                    }
                }
            }
        }
    }

    fun setBackupFile(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val mediaFile = MediaFile<AbstractTag, StreamInfo>()
                if (mediaFile.scanCatching(file, "r") &&
                    mediaFile.streamInfo != null)
                {
                    val unknownTitle  = appCxt.getString(R.string.unknown_title)
                    val unknownArtist = appCxt.getString(R.string.unknown_artist)
                    val unknownAlbum  = appCxt.getString(R.string.unknown_album)

                    backupFile = BackupFile(
                        mediaFile.tag?.getStringField(AbstractTag.TITLE)?.ifBlank   { unknownTitle }  ?: unknownTitle,
                        mediaFile.tag?.getStringField(AbstractTag.ARTIST)?.ifBlank { unknownArtist } ?: unknownArtist,
                        mediaFile.tag?.getStringField(AbstractTag.ALBUM)?.ifBlank { unknownAlbum }  ?: unknownAlbum,
                        mediaFile.mimeType,
                        file,
                        prefsGetLastModifiedFile(appCxt)!!
                    )
                    showRestoreDialog.value = true
                }
            }
        }
    }

    fun enableSearchMode() {
        searchMode.value = true
        topBarState.value = TopBar.SEARCH
    }

    fun clearSearchMode() {
        searchMode.value   = false
        topBarState.value  = TopBar.DEFAULT
        searchQuery.value  = ""
    }

    override fun sendEvent(event: BaseViewModel.UIEvent) {
        if (mediaScanInProgress ||
            batchOperationInProgress != null)
        {
            return
        }
        super.sendEvent(event)
    }

    override fun onLongClickSelect(index: Int) {
        if (mediaScanInProgress) return
        super.onLongClickSelect(index)
    }

    override fun clearSelectionMode() {
        super.clearSelectionMode()
        if (searchQuery.value.isNotEmpty()) {
            topBarState.value = TopBar.SEARCH
        }
    }

    override fun getTracks(): List<Track> {
        return tracks.value
    }
}