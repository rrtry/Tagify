package com.rrtry.tagify.ui.details

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jtagger.AbstractTag
import com.jtagger.AttachedPicture
import com.jtagger.Tag
import com.rrtry.tagify.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jtagger.AbstractTag.*
import com.jtagger.mp3.id3.ID3V2Tag
import com.jtagger.mp3.id3.TextEncoding.ENCODING_LATIN_1
import com.jtagger.mp3.id3.TextEncoding.getEncodingForVersion
import com.jtagger.mp3.id3.TextFrame
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FILENAME
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_QUERY
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_NONE
import com.rrtry.tagify.prefs.prefsGetManualModePreferredLookupMethod
import com.rrtry.tagify.prefs.prefsHasRequiredFields
import com.rrtry.tagify.prefs.prefsSetManualModePreferredLookupMethod
import com.rrtry.tagify.util.setArtwork
import com.rrtry.tagify.util.parseNum
import com.rrtry.tagify.util.parseYear
import com.rrtry.tagify.data.api.TagApiImpl.*
import com.rrtry.tagify.prefs.prefsGetFixEncoding
import com.rrtry.tagify.util.decodeString
import com.rrtry.tagify.util.isIncorrectlyEncoded
import com.rrtry.tagify.util.loadImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import com.rrtry.tagify.util.exportArtworkDownloads
import com.rrtry.tagify.util.sanitizeFilename

private val providers = listOf(
    Provider.ITUNES,
    Provider.MUSICBRAINZ
)

const val FILTER_PROVIDER_ITUNES = 0x1
const val FILTER_PROVIDER_MB     = 0x2

@HiltViewModel
class TrackDetailsViewModel @Inject constructor(private val app: Application): AndroidViewModel(app) {

    sealed class UIEvent {

        data class OnStartTagLookup(val method: LookupMethod): UIEvent()
        data class OnIOError(val message: String, val exitAction: Boolean): UIEvent()
        data class OnExpandBottomSheet(val layoutId: Int): UIEvent()
        data class OnArtworkExported(
            val success: Boolean,
            val file: File
        ): UIEvent()

        data object OnExportArtwork:      UIEvent()
        data object OnSaveChanges:        UIEvent()
        data object OnHideBottomSheet:    UIEvent()
        data object OnArtworkChange:      UIEvent()
        data object OnAPILimitExceeded:   UIEvent()
        data object OnChromaprintFailure: UIEvent()
        data object OnMediaScannerFailed: UIEvent()
        data object OnNavigateUpEvent:    UIEvent()
    }

    sealed class LookupMethod {

        data object Fingerprint: LookupMethod()
        data object Filename: LookupMethod()
        data class  Query(val artist: String, val title: String): LookupMethod()
    }

    var editedTag: AbstractTag? = null
    var editedTrack: Track? = null

    lateinit var mimeType: String
    lateinit var filePath: String
    lateinit var uri: Uri

    lateinit var lastLookupParams: LookupMethod
        private set

    var lookupMethod = PREFS_VALUE_LOOKUP_METHOD_NONE
        private set

    var duration = 0
    var changedArtwork = false
    private var requestMade = false

    val trkNumError = MutableStateFlow(false)
    val dskNumError = MutableStateFlow(false)
    val yearError   = MutableStateFlow(false)
    val ioError     = MutableStateFlow(false)

    val showLookupMethodDialog = MutableStateFlow(false)
    val showLookupQueryDialog  = MutableStateFlow(false)

    val artwork  = MutableStateFlow<Any?>(null)
    val title    = MutableStateFlow("")
    val artist   = MutableStateFlow("")
    val album    = MutableStateFlow("")
    val year     = MutableStateFlow("")
    val genre    = MutableStateFlow("")
    val trkNum   = MutableStateFlow("")
    val dskNum   = MutableStateFlow("")
    val comment  = MutableStateFlow("")
    val albArt   = MutableStateFlow("")
    val composer = MutableStateFlow("")

    val finishedScanning = MutableStateFlow(false)
    val finishedSaving   = MutableStateFlow(true)

    val errorState = combine(trkNumError, dskNumError, yearError, ioError) { trk, dsk, year, io ->
        trk || dsk || year || io
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), false)

    private val eventChannel: Channel<UIEvent> = Channel(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        lookupMethod = prefsGetManualModePreferredLookupMethod(app)
    }

    fun getField(tag: AbstractTag?, field: String): String {
        return tag?.getStringField(field) ?: ""
    }

    fun setField(tag: AbstractTag, key: String, value: String) {
        if (value.isBlank()) tag.removeField(key) else tag.setStringField(key, value)
    }

    private fun getTrack(tag: AbstractTag): Track {

        val strGenre     = tag.getStringField(GENRE)?.ifBlank { "" } ?: ""
        val trkNumParts  = trkNum.value.split("/")
        val discNumParts = dskNum.value.split("/")

        return Track(
            uri.lastPathSegment!!.toLong(),
            title.value.ifBlank  { app.getString(R.string.unknown_title)  },
            album.value.ifBlank  { app.getString(R.string.unknown_album)  },
            artist.value.ifBlank { app.getString(R.string.unknown_artist) },
            mimeType,
            year.value.parseYear(),
            trkNumParts[0].parseNum(),
            if (trkNumParts.size == 2) trkNumParts[1].parseNum() else 0,
            discNumParts[0].parseNum(),
            if (discNumParts.size == 2) discNumParts[1].parseNum() else 0,
            strGenre,
            filePath,
            uri,
            prefsHasRequiredFields(app, tag),
            duration
        )
    }

    fun saveChanges(scannedTag: AbstractTag?) {

        finishedSaving.update { false }

        val tag = scannedTag ?: Tag()
        setField(tag, TITLE,  title.value)
        setField(tag, ARTIST, artist.value)
        setField(tag, ALBUM,  album.value)
        setField(tag, GENRE,  genre.value)
        setField(tag, YEAR,   year.value)

        setField(tag, TRACK_NUMBER, trkNum.value)
        setField(tag, DISC_NUMBER,  dskNum.value)
        setField(tag, ALBUM_ARTIST, albArt.value)
        setField(tag, COMPOSER,     composer.value)

        val onCompletion = {
            this.editedTag = tag
            this.editedTrack = getTrack(tag)
            sendEvent(UIEvent.OnSaveChanges)
        }

        if (artwork.value != null && changedArtwork) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    setArtwork(
                        tag,
                        loadImage(app, artwork.value),
                        tag.pictureField ?: AttachedPicture()
                    )
                }
            }.invokeOnCompletion {
                onCompletion()
            }
        }
        else if (artwork.value == null) {
            tag.removeField(PICTURE)
            onCompletion()
        } else {
            onCompletion()
        }
    }

    fun setTag(tag: AbstractTag?) {

        finishedScanning.update { true }
        title.value    = getField(tag, TITLE)
        artist.value   = getField(tag, ARTIST)
        album.value    = getField(tag, ALBUM)
        year.value     = getField(tag, YEAR)
        genre.value    = getField(tag, GENRE)
        trkNum.value   = getField(tag, TRACK_NUMBER)
        dskNum.value   = getField(tag, DISC_NUMBER)
        comment.value  = getField(tag, COMMENT)
        albArt.value   = getField(tag, ALBUM_ARTIST)
        composer.value = getField(tag, COMPOSER)
        artwork.value  = tag?.pictureField?.pictureData

        if (tag is ID3V2Tag && prefsGetFixEncoding(app)) {

            val fields = setOf(TITLE, ARTIST, ALBUM, GENRE, COMPOSER, ALBUM_ARTIST)
            val from   = StandardCharsets.ISO_8859_1
            val to     = Charset.forName("Windows-1251")

            fields.forEach { field ->

                val frameId = ID3V2Tag.FIELD_MAP_V24[field]
                val frame   = tag.frameMap[frameId] as? TextFrame

                if (frame != null &&
                    frame.encoding == ENCODING_LATIN_1 &&
                    frame.text.isIncorrectlyEncoded())
                {
                    frame.encoding = getEncodingForVersion(tag.version)
                    frame.text     = decodeString(frame.text, from, to)

                    val stateFlow = when (field) {

                        TITLE  -> title
                        ARTIST -> artist
                        ALBUM  -> album
                        GENRE  -> genre

                        COMPOSER     -> composer
                        ALBUM_ARTIST -> albArt
                        else         -> throw IllegalArgumentException("Unsupported field $field")
                    }
                    stateFlow.value = frame.text
                }
            }
        }
    }

    fun setTag(tag: com.rrtry.tagify.data.entities.Tag, setArtwork: Boolean) {

        sendEvent(UIEvent.OnHideBottomSheet)
        title.value  = tag.title
        album.value  = tag.album
        artist.value = tag.artist
        albArt.value = tag.albArt
        year.value   = tag.date
        genre.value  = tag.genre
        trkNum.value = "${tag.trkPos}/${tag.trkCnt}"
        dskNum.value = "${tag.discPos}/${tag.discCnt}"

        if (setArtwork) {
            tag.cover?.let {
                setArtwork(URL(it))
            }
        }
    }

    fun refreshTagList(inProgress: Boolean) {
        lookupMetadata(lastLookupParams, true)
    }

    private fun performLookup(lookupMethod: Int) {
        val refresh: Boolean = this.lookupMethod != lookupMethod
        when (lookupMethod) {
            PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT -> lookupMetadata(LookupMethod.Fingerprint, refresh)
            PREFS_VALUE_LOOKUP_METHOD_FILENAME    -> lookupMetadata(LookupMethod.Filename, refresh)
            PREFS_VALUE_LOOKUP_METHOD_QUERY       -> showLookupQueryDialog.value = true
        }
        this.lookupMethod = lookupMethod
    }

    fun showLookupMethodDialog(inProgress: Boolean) {
        if (inProgress) {
            sendEvent(UIEvent.OnExpandBottomSheet(BOTTOM_SHEET_METADATA))
        } else {
            showLookupMethodDialog.value = true
        }
    }

    fun onLookupMethodSelected(method: Int, remember: Boolean) {
        if (remember) {
            prefsSetManualModePreferredLookupMethod(app, method)
        }
        showLookupMethodDialog.value = false
        performLookup(method)
    }

    fun onSearchButtonClick(inProgress: Boolean) {
        if (inProgress) {
            sendEvent(UIEvent.OnExpandBottomSheet(BOTTOM_SHEET_METADATA))
            return
        }
        if (prefsGetManualModePreferredLookupMethod(app) == PREFS_VALUE_LOOKUP_METHOD_NONE) {
            showLookupMethodDialog.value = true
            return
        }
        performLookup(lookupMethod)
    }

    fun lookupMetadata(lookupMethodParams: LookupMethod, refresh: Boolean = false) {

        sendEvent(UIEvent.OnExpandBottomSheet(BOTTOM_SHEET_METADATA))
        if (!refresh && requestMade) return

        lastLookupParams = lookupMethodParams
        requestMade      = true
        sendEvent(UIEvent.OnStartTagLookup(lookupMethodParams))
    }

    fun setArtwork(url: URL) {
        artwork.value  = url.toString()
        changedArtwork = true
        sendEvent(UIEvent.OnArtworkChange)
    }

    fun setArtwork(uri: Uri?) {
        artwork.value  = uri
        changedArtwork = true
        sendEvent(UIEvent.OnArtworkChange)
    }

    fun exportArtwork() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = exportArtworkDownloads(app, artwork.value, "${artist.value} - ${album.value}".sanitizeFilename())
                eventChannel.send(UIEvent.OnHideBottomSheet)
                eventChannel.send(UIEvent.OnArtworkExported(
                    result.first, result.second
                ))
            }
        }
    }

    fun sendEvent(event: UIEvent, delayMillis: Long = 0) {
        viewModelScope.launch {
            delay(delayMillis)
            eventChannel.send(event)
        }
    }
}