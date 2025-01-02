package com.rrtry.tagify.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.net.toUri
import com.arthenica.ffmpegkit.FFmpegKit
import com.jtagger.AbstractTag
import com.jtagger.AbstractTag.PICTURE
import com.jtagger.AttachedPicture
import com.jtagger.MediaFile
import com.jtagger.StreamInfo
import com.rrtry.tagify.ACTION_BATCH_LOOKUP_TAGS
import com.rrtry.tagify.ACTION_BATCH_REMOVE_ARTWORK
import com.rrtry.tagify.ACTION_BATCH_REMOVE_TAGS
import com.rrtry.tagify.ACTION_BATCH_SAVE
import com.rrtry.tagify.ACTION_BATCH_SET_ARTWORK_FROM_FILE
import com.rrtry.tagify.ACTION_BATCH_TAG_FROM_FILENAME
import com.rrtry.tagify.ACTION_LOAD_TAG
import com.rrtry.tagify.ACTION_LOOKUP_TAGS
import com.rrtry.tagify.ACTION_SAVE_TAG
import com.rrtry.tagify.CATEGORY_BATCH_OP
import com.rrtry.tagify.FINGERPRINTS_CACHE_DIR
import com.rrtry.tagify.LOCAL_ARTWORK_FILES_DIR
import com.rrtry.tagify.LOCAL_THUMBNAIL_FILES_DIR
import com.rrtry.tagify.R
import com.rrtry.tagify.data.MediaRepository
import com.rrtry.tagify.data.TagRepository
import com.rrtry.tagify.data.api.TagApiImpl.Provider
import com.rrtry.tagify.data.entities.Tag
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.data.entities.TrackWithTags
import com.rrtry.tagify.data.entities.fromTag
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FILENAME
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_NONE
import com.rrtry.tagify.prefs.PREFS_VALUE_LOOKUP_METHOD_QUERY
import com.rrtry.tagify.prefs.prefsGetApiKey
import com.rrtry.tagify.prefs.prefsGetArtworkSize
import com.rrtry.tagify.prefs.prefsGetBatchModePreferredLookupMethod
import com.rrtry.tagify.prefs.prefsGetFilenamePattern
import com.rrtry.tagify.prefs.prefsGetRenameFiles
import com.rrtry.tagify.prefs.prefsHasRequiredFields
import com.rrtry.tagify.prefs.prefsSetLastModifiedFile
import com.rrtry.tagify.service.ServiceEventBus.Event
import com.rrtry.tagify.ui.home.BaseViewModel.ProgressInfo
import com.rrtry.tagify.util.DEFAULT_ARTWORK_SIZE
import com.rrtry.tagify.util.FileReference
import com.rrtry.tagify.util.asyncMap
import com.rrtry.tagify.util.closeQuietly
import com.rrtry.tagify.util.getAttachedPicture
import com.rrtry.tagify.util.loadImage
import com.rrtry.tagify.util.makeAbsolutePath
import com.rrtry.tagify.util.makeDisplayName
import com.rrtry.tagify.util.openFileDescriptor
import com.rrtry.tagify.util.openFileRef
import com.rrtry.tagify.util.openRandomAccessFile
import com.rrtry.tagify.util.parseFilename
import com.rrtry.tagify.util.removeArtworkFromCache
import com.rrtry.tagify.util.removeThumbnailFromCache
import com.rrtry.tagify.util.rename
import com.rrtry.tagify.util.resolveArtworkSanitized
import com.rrtry.tagify.util.resolveThumbnail
import com.rrtry.tagify.util.sanitizeFilename
import com.rrtry.tagify.util.saveArtwork
import com.rrtry.tagify.util.saveThumbnail
import com.rrtry.tagify.util.scanCatching
import com.rrtry.tagify.util.scanFile
import com.rrtry.tagify.util.setArtwork
import com.rrtry.tagify.util.sortedTags
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.lang.Long.min
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "TagWriterService"
private const val WAKELOCK_TAG     = "com.rrtry.tagify::TagWriterWakeLock"
private const val WAKELOCK_TIMEOUT = 30 * 60 * 1000L

private val isR      = Build.VERSION.SDK_INT == Build.VERSION_CODES.R
private val openMode = if (isR) "rw" else "r"

val fieldSet = setOf(
    AbstractTag.TITLE, AbstractTag.ARTIST, AbstractTag.ALBUM, AbstractTag.GENRE,
    AbstractTag.YEAR, AbstractTag.TRACK_NUMBER, AbstractTag.DISC_NUMBER,
    AbstractTag.ALBUM_ARTIST, AbstractTag.COMPOSER
)

@AndroidEntryPoint
class TagWriterService @Inject constructor(): Service() {

    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var tagRepository: TagRepository
    @Inject lateinit var serviceBus: ServiceEventBus

    private val job    = Job()
    private val scope  = CoroutineScope(Dispatchers.IO + job)

    private lateinit var mediaFile: MediaFile<AbstractTag, StreamInfo>
    private lateinit var intent: Intent

    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    private var wakeLock: PowerManager.WakeLock? = null

    private var artworkSize  = DEFAULT_ARTWORK_SIZE
    private var formatString = "%ARTIST% - %TITLE%"
    private var renameFiles  = false

    private lateinit var albumPlaceholder: String
    private lateinit var artistPlaceholder: String

    private var queryTitle: String?  = null
    private var queryArtist: String? = null
    private var acoustIdKey: String? = null
    private var fileRef: FileReference? = null

    override fun onCreate() {
        super.onCreate()

        mediaFile = MediaFile()
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        formatString = prefsGetFilenamePattern(this)
        renameFiles  = prefsGetRenameFiles(this)
        acoustIdKey  = prefsGetApiKey(this)

        albumPlaceholder  = application.getString(R.string.unknown_album)
        artistPlaceholder = application.getString(R.string.unknown_artist)
    }

    private fun isTagEmpty(tag: AbstractTag): Boolean {
        return fieldSet.all {
            tag.getStringField(it).isNullOrBlank()
        } && tag.pictureField == null
    }

    private suspend fun loadTag() {
        openFileRef(serviceBus.track!!, openMode)?.let { ref ->
            fileRef = ref
            if (mediaFile.scanCatching(fileRef!!) &&
                mediaFile.streamInfo != null)
            {
                serviceBus.tag = mediaFile.tag
                serviceBus.streamInfo = mediaFile.streamInfo
                serviceBus.send(Event.OnFileLoaded)
            }
            else {
                serviceBus.send(Event.OnFileLoadFailed)
            }
        } ?: serviceBus.send(Event.OnFileLoadFailed)
    }

    private suspend fun updateMediaStore(track: Track, mimeType: String, newFile: File?) {
        val scanned = scanFile(
            applicationContext,
            track.path,
            mimeType
        )
        if (scanned) {
            if (newFile != null) {
                if (isR) {
                    contentResolver.update(
                        track.trackUri,
                        ContentValues().apply { put(DISPLAY_NAME, newFile.name) },
                        null,
                        null
                    )
                } else {
                    rename(track.path, newFile.canonicalPath)
                }
            }
        }
    }

    private suspend fun updateIndex(track: Track, newAbsolutePath: String?) {
        if (newAbsolutePath != null) {
            mediaRepository.insertTrack(track.copy(path = newAbsolutePath))
        } else {
            mediaRepository.insertTrack(track)
        }
    }

    private fun updateThumbnail(mediaId: Long, apic: AttachedPicture?) {

        removeThumbnailFromCache(mediaId)
        val thumbnail = resolveThumbnail(mediaId)

        if (apic != null) {
            FileOutputStream(thumbnail).use { fos ->
                saveThumbnail(apic.pictureData, fos)
            }
        } else {
            thumbnail.delete()
        }
    }

    private fun updateArtwork(newAlbum: String, apic: AttachedPicture?) {

        val savedArtwork = resolveArtworkSanitized(newAlbum)
        val writeArtwork = (intent.hasCategory(CATEGORY_BATCH_OP) || serviceBus.writeApic) &&
                            newAlbum != application.getString(R.string.unknown_album)

        if (writeArtwork && apic != null) {
            FileOutputStream(savedArtwork).use { fos ->
                saveArtwork(apic.pictureData, fos)
            }
        }
    }

    private suspend fun cleanupLocalFiles(currentAlbum: String) {

        val albumTracks = mediaRepository.getAlbumTrackIds(currentAlbum)
        val thumbnails  = if (albumTracks.isEmpty()) null else filesDir
            .resolve(LOCAL_THUMBNAIL_FILES_DIR)
            .listFiles { file ->
                file.name.toLong() in albumTracks
            }

        removeArtworkFromCache(currentAlbum)
        if (thumbnails.isNullOrEmpty()) {
            filesDir
                .resolve(LOCAL_ARTWORK_FILES_DIR)
                .resolve(currentAlbum.sanitizeFilename())
                .delete()
        }
    }

    private suspend fun copy(from: File, to: File, restore: Boolean = false) = coroutineScope {

        if (!restore && isR) {
            prefsSetLastModifiedFile(applicationContext, from.canonicalPath)
        }

        val input = FileInputStream(from)
        val out   = FileOutputStream(to)

        val bufferSize = 8192
        val buffer = ByteArray(bufferSize)

        val length = from.length()
        var read: Long = 0
        var size: Int

        var success = true
        return@coroutineScope try {
            while (read < length) {
                ensureActive()
                size = min(bufferSize.toLong(), length - read).toInt()
                input.read(buffer, 0, size)
                out.write(buffer)
                read += size
            }
            read == length
        } catch (e: Exception) {
            success = false
            if (e is CancellationException) {
                throw e
            }
            false
        } finally {
            input.closeQuietly()
            out.closeQuietly()
            if (restore) {
                if (success) {
                    from.delete() // Delete backup if restoration succeeded
                }
            } else {
                if (!success) {
                    to.delete() // Delete incompletely written backup file
                }
            }
        }
    }

    private suspend fun save(
        track: Track,
        tag: AbstractTag?,
        fileRef: FileReference,
        sendEvents: Boolean = false): Boolean
    {
        val src = File(track.path)
        val dst = src.parentFile!!.resolve(".${UUID.randomUUID()}.${src.extension}")

        if (copy(src, dst)) {
            if (!isR) {
                val f = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                    openFileDescriptor(dst.toUri(), "rw")
                } else {
                    openRandomAccessFile(dst.canonicalPath, "rw")
                } ?: return false
                fileRef.setFileReference(f)
            }
            return save(src, dst, track, tag, sendEvents)
        }
        return false
    }

    private suspend fun saveTag() {

        val tag   = serviceBus.tag!!
        val track = serviceBus.track!!
        val path  = track.path

        notificationManager.notify(
            SERVICE_ID,
            createWriterServiceNotification(
                this,
                CHANNEL_ID,
                path
            )
        )

        if (isTagEmpty(tag)) {
            mediaFile.removeTag()
        } else {
            mediaFile.setTag(tag)
        }
        save(track, tag, fileRef!!, true)
    }

    private suspend fun save(
        orig:  File,
        copy:  File,
        track: Track,
        tag:   AbstractTag?,
        sendEvents: Boolean = false): Boolean
    {
        return coroutineScope {

            var success = true
            ensureActive()

            try {
                mediaFile.save()
            } catch (e: Exception) {
                success = false
                if (sendEvents) serviceBus.send(Event.OnFileWriteFailed)
            } finally {
                mediaFile.closeQuietly()
                if (!success || !isActive) {
                    if (isR) withContext(NonCancellable) { copy(copy, orig, true) }
                } else {
                    if (!isR) success = rename(copy.canonicalPath, orig.canonicalPath)
                    copy.delete()
                }
            }

            ensureActive()
            if (success) {

                val currentAlbum    = mediaRepository.getTrackById(track.trackId)!!.album
                val newDisplayName  = if (tag != null && renameFiles) makeDisplayName(tag, formatString) else null
                val newAbsolutePath = if (newDisplayName != null) makeAbsolutePath(track, newDisplayName) else null

                updateThumbnail(track.trackId, tag?.pictureField)
                updateArtwork(track.album, tag?.pictureField)
                updateIndex(track, newAbsolutePath)
                updateMediaStore(
                    track,
                    track.mimeType,
                    if (newAbsolutePath != null) File(newAbsolutePath) else null
                )
                cleanupLocalFiles(currentAlbum)

                if (sendEvents) serviceBus.send(Event.OnFileWritten)
                return@coroutineScope true

            } else if (sendEvents) {
                serviceBus.send(Event.OnFileWriteFailed)
            }
            return@coroutineScope false
        }
    }

    private suspend fun lookupByFingerprint(
        path: String,
        length: Int,
        id: Long,
        lookupArtwork: Boolean): Pair<Boolean, List<Tag>>?
    {
        val fingerprint = generateFingerprint(applicationContext, path, id)
        if (!fingerprint.second) {
            return null
        }

        var success: Boolean
        var results = tagRepository.fetchTags(acoustIdKey!!, readFingerprint(fingerprint.first), length)
        var tags    = results.second
        success     = results.first

        if (tags.isNotEmpty()) {

            val first   = tags.first()
            val title   = first.title
            val artist  = first.artist
            val sortStr = "$artist - $title"

            results = tagRepository.fetchTags(artist, title, Provider.ITUNES)
            tags    = tags + results.second
            success = success || results.first

            tags = withContext(Dispatchers.Default) { tags.sortedTags(sortStr) }.run {
                if (lookupArtwork) {
                    asyncMap { tag ->
                        tagRepository.fetchArtworkCatching(tag, artworkSize)
                        tag
                    }
                } else this
            }
            return Pair(success, tags)
        }
        return Pair(
            results.first,
            tags
        )
    }

    private suspend fun lookupByQuery(artist: String, title: String, lookupArtwork: Boolean): Pair<Boolean, List<Tag>> = coroutineScope {

        val responses = listOf(Provider.MUSICBRAINZ, Provider.ITUNES).map { provider ->
            async {
                val response = tagRepository.fetchTags(artist, title, provider)
                Pair(
                    response.first,
                    response.second.run {
                        if (lookupArtwork) {
                            asyncMap { tag ->
                                tagRepository.fetchArtworkCatching(tag, artworkSize)
                                tag
                            }
                        } else this
                    }
                )
            }
        }

        var success = false
        var tags    = listOf<Tag>()
        val mutex   = Mutex()
        val sortBy  = "$artist - $title"

        for (i in responses.indices) {

            val active = responses.filter { it.isActive }
            if (active.isEmpty()) {
                break
            }

            val result = select {
                active.forEach { response ->
                    response.onAwait {
                        if (it.first && it.second.isNotEmpty()) {
                            mutex.withLock {
                                tags = (tags + it.second).sortedTags(sortBy)
                            }
                        }
                        it
                    }
                }
            }
            if (result.first) {
                success = true
                break
            }
        }
        responses.filter { it.isActive }.forEach { it.cancel() }
        Pair(success, tags)
    }

    private suspend fun lookupByFilename(path: String, lookupArtwork: Boolean): Pair<Boolean, List<Tag>>? {

        val fields = withContext(Dispatchers.Default) {
            parseFilename(
                prefsGetFilenamePattern(applicationContext),
                File(path).nameWithoutExtension
            )
        } ?: return null

        val artist = fields[AbstractTag.ARTIST] ?: return null
        val title  = fields[AbstractTag.TITLE]  ?: return null
        return lookupByQuery(artist, title, lookupArtwork)
    }

    private suspend fun lookupTags(track: Track, lookupMethod: Int, lookupArtwork: Boolean): Pair<Boolean, List<Tag>>? {
        return when (lookupMethod) {
            PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT -> lookupByFingerprint(track.path, track.duration, track.trackId, lookupArtwork)
            PREFS_VALUE_LOOKUP_METHOD_FILENAME    -> lookupByFilename(track.path, lookupArtwork)
            PREFS_VALUE_LOOKUP_METHOD_QUERY       -> lookupByQuery(queryArtist!!, queryTitle!!, true) // Can be used only in editing mode
            else -> throw IllegalArgumentException(
                "Unknown lookup method: $lookupMethod"
            )
        }
    }

    private suspend fun saveFetchedTag(track: Track, newTag: Tag): Boolean {

        val fileRef = openFileRef(track, openMode) ?: return false
        if (!mediaFile.scanCatching(fileRef) ||
            mediaFile.streamInfo == null)
        {
            return false
        }

        val tag = mediaFile.tag ?: com.jtagger.Tag()
        newTag.setFields(tag)

        val newTrack = track.fromTag(
            applicationContext,
            tag,
            File(track.path).name,
            albumPlaceholder,
            artistPlaceholder
        )
        if (newTag.cover != null) {
            setArtwork(
                tag,
                loadImage(application, newTag.cover),
                AttachedPicture()
            )
        }

        mediaFile.setTag(tag)
        return save(newTrack, tag, fileRef)
    }

    private fun updateProgress(processed: Int, total: Int, success: Boolean) {
        notificationManager.notify(
            SERVICE_ID,
            createBatchWriterServiceNotification(
                applicationContext,
                CHANNEL_ID,
                intent.action!!,
                processed,
                total
            )
        )
        serviceBus.progressInfo.value = ProgressInfo(
            intent.action!!,
            processed,
            total
        )
    }

    private fun updateLookupResults(track: Track, tags: List<Tag>) {
        val updated = serviceBus.lookupResults.value.toMutableList()
        updated.add(TrackWithTags(track, tags.firstOrNull(), tags))
        serviceBus.lookupResults.value = updated
    }

    private suspend fun lookupTags(track: Track) {

        serviceBus.lookupResults.value = mutableListOf()
        artworkSize = prefsGetArtworkSize(applicationContext)

        val response = lookupTags(
            track,
            intent.getIntExtra(EXTRA_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_NONE),
            true
        )
        if (!response?.second.isNullOrEmpty()) {
            updateLookupResults(track, response!!.second)
        }
    }

    private suspend fun saveTags() = coroutineScope {

        artworkSize = prefsGetArtworkSize(applicationContext)
        renameFiles = prefsGetRenameFiles(applicationContext)

        val total     = serviceBus.progressInfo.value.total
        var processed = 0

        serviceBus.lookupResults.value.forEach {
            ensureActive()
            if (it.apply) {
                updateProgress(++processed, total, saveFetchedTag(it.track, it.tag!!))
            }
        }
    }

    private suspend fun lookupTags(tracks: List<Track>) = coroutineScope {

        serviceBus.lookupResults.value = mutableListOf()
        artworkSize = prefsGetArtworkSize(applicationContext)

        val mutex        = Mutex()
        val lookupMethod = prefsGetBatchModePreferredLookupMethod(applicationContext)
        val tracksDeque  = ArrayDeque(tracks)
        val total        = tracksDeque.size
        var processed    = 0

        while (tracksDeque.isNotEmpty()) {

            ensureActive()

            val track    = tracksDeque.first()
            val response = lookupTags(track, lookupMethod, false) ?: continue
            var tags: List<Tag>

            if (response.first) {

                tracksDeque.removeFirst()
                tags = response.second

                launch {
                    tags = tags.asyncMap { tag ->
                        tagRepository.fetchArtworkCatching(tag, artworkSize)
                        tag
                    }
                    mutex.withLock {
                        updateLookupResults(track, tags)
                        updateProgress(++processed, total, true)
                    }
                }
            } else {
                yield()
                delay(1000L)
            }
        }
        coroutineContext.job.children
            .filter  { it.isActive }
            .forEach { it.join()   }
    }

    private suspend fun removeTags(tracks: List<Track>, onlyArtwork: Boolean) = coroutineScope {
        for (index in tracks.indices) {

            ensureActive()
            var hasTag = false
            var success: Boolean

            val track   = tracks[index]
            val fileRef = openFileRef(track, openMode)
            success     = fileRef != null

            if (success) {
                success = mediaFile.scanCatching(fileRef!!) && mediaFile.streamInfo != null
                hasTag  = mediaFile.tag != null
            }
            if (success && hasTag) {
                val newTrack = Track(
                    track.trackId,
                    if (onlyArtwork) track.title  else File(track.path).name,
                    if (onlyArtwork) track.album  else application.getString(R.string.unknown_album),
                    if (onlyArtwork) track.artist else application.getString(R.string.unknown_artist),
                    track.mimeType,
                    if (onlyArtwork) track.year    else 0,
                    if (onlyArtwork) track.trkPos  else 0,
                    if (onlyArtwork) track.trkCnt  else 0,
                    if (onlyArtwork) track.discPos else 0,
                    if (onlyArtwork) track.discCnt else 0,
                    if (onlyArtwork) track.genre   else "",
                    track.path,
                    track.trackUri,
                    prefsHasRequiredFields(applicationContext, mediaFile.tag),
                    track.duration
                )
                if (onlyArtwork) mediaFile.tag?.removeField(PICTURE) else mediaFile.removeTag()
                success = save(newTrack, mediaFile.tag, fileRef!!, false)
            }
            updateProgress(
                index + 1,
                tracks.size,
                success
            )
        }
    }

    private suspend fun setArtwork(tracks: List<Track>) = coroutineScope {

        artworkSize = prefsGetArtworkSize(applicationContext)
        val uri     = Uri.parse(intent.getStringExtra(EXTRA_ARTWORK_URI))
        val apic    = getAttachedPicture(
            this@TagWriterService,
            uri,
            artworkSize
        )

        if (apic != null) {
            tracks.forEachIndexed { index, track ->

                ensureActive()
                val fileRef = openFileRef(track, openMode)
                var success = fileRef != null

                if (success) {
                    success = mediaFile.scanCatching(fileRef!!) &&
                              mediaFile.streamInfo != null
                }
                if (success) {

                    val tag = mediaFile.tag ?: com.jtagger.Tag()
                    tag.pictureField = apic

                    mediaFile.setTag(tag)
                    success = save(track, tag, fileRef!!)
                }
                updateProgress(index + 1, tracks.size, success)
            }
        }
    }

    private suspend fun importTags(tracks: List<Track>) = coroutineScope {
        formatString = prefsGetFilenamePattern(applicationContext)
        tracks.forEachIndexed { index, track ->

            ensureActive()
            var success = false
            val src     = File(track.path)
            val fields  = parseFilename(
                formatString,
                src.nameWithoutExtension
            )

            if (!fields.isNullOrEmpty()) {

                val fileRef = openFileRef(track, openMode)
                success = fileRef != null

                if (success) {
                    success = mediaFile.scanCatching(fileRef!!) &&
                              mediaFile.streamInfo != null
                }
                if (success) {

                    val tag = mediaFile.tag ?: com.jtagger.Tag()
                    for (entry in fields.entries) {
                        tag.setStringField(entry.key, entry.value)
                    }

                    val newTrack = track.fromTag(
                        applicationContext,
                        tag,
                        src.name,
                        albumPlaceholder,
                        artistPlaceholder,
                    )
                    mediaFile.setTag(tag)
                    success = save(newTrack, tag, fileRef!!)
                }
            }
            updateProgress(
                index + 1,
                tracks.size,
                success
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        this.intent = intent!!
        val action  = intent.action!!
        val isBatch = intent.hasCategory(CATEGORY_BATCH_OP)

        queryArtist = intent.getStringExtra(EXTRA_ARTIST)
        queryTitle  = intent.getStringExtra(EXTRA_TITLE)

        if (wakeLock == null &&
            intent.hasCategory(CATEGORY_BATCH_OP))
        {
            wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                acquire(WAKELOCK_TIMEOUT)
            }
        }
        scope.launch {
            if (isBatch || action == ACTION_SAVE_TAG) {
                startForegroundServiceCompat(
                    this@TagWriterService,
                    SERVICE_ID,
                    createWriterServiceNotification(applicationContext, CHANNEL_ID)
                )
            }
            if (isBatch) {
                if (action != ACTION_BATCH_SAVE) {
                    serviceBus.progressInfo.value = ProgressInfo(
                        action, 0, serviceBus.tracks.size
                    )
                } else {
                    serviceBus.progressInfo.value = ProgressInfo(
                        action, 0, serviceBus.lookupResults.value.count { it.apply }
                    )
                }
            }
            serviceBus.inProgress.value = true
            when (action) {
                ACTION_BATCH_SET_ARTWORK_FROM_FILE -> setArtwork(serviceBus.tracks)
                ACTION_BATCH_TAG_FROM_FILENAME     -> importTags(serviceBus.tracks)
                ACTION_BATCH_REMOVE_TAGS           -> removeTags(serviceBus.tracks, false)
                ACTION_BATCH_REMOVE_ARTWORK        -> removeTags(serviceBus.tracks, true)
                ACTION_BATCH_LOOKUP_TAGS           -> lookupTags(serviceBus.tracks)
                ACTION_BATCH_SAVE                  -> saveTags()
                ACTION_LOAD_TAG                    -> loadTag()
                ACTION_SAVE_TAG                    -> saveTag()
                ACTION_LOOKUP_TAGS                 -> lookupTags(serviceBus.track!!)
            }
            if (isBatch) serviceBus.send(Event.OnBatchProcessingFinished(action))
            serviceBus.inProgress.value = false
        }.invokeOnCompletion {
            if (it != null) {
                stopSelf()
            }
            else if (action == ACTION_SAVE_TAG || isBatch) {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        scope.cancel()
        mediaFile.closeQuietly()
        fileRef?.close()
        wakeLock?.release()
        wakeLock = null

        serviceBus.tag        = null
        serviceBus.track      = null
        serviceBus.streamInfo = null
        serviceBus.writeApic  = false
        serviceBus.inProgress.value = false
    }

    companion object {

        private const val SERVICE_ID = 103
        private const val CHANNEL_ID = "com.rrtry.tagify.CHANNEL_WRITER_SERVICE"

        const val EXTRA_ARTWORK_URI   = "com.rrtry.tagify.EXTRA_ARTWORK_URI"
        const val EXTRA_LOOKUP_METHOD = "com.rrtry.tagify.EXTRA_LOOKUP_METHOD"

        const val EXTRA_TITLE  = "com.rrtry.tagify.EXTRA_TITLE"
        const val EXTRA_ARTIST = "com.rrtry.tagify.EXTRA_ARTIST"
        const val EXTRA_ALBUM  = "com.rrtry.tagify.EXTRA_ALBUM"
    }
}

fun startForegroundServiceCompat(
    service: Service,
    serviceId: Int,
    notification: Notification)
{
    startForeground(
        service,
        serviceId,
        notification,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
    )
}

fun readFingerprint(fpFile: File): String {
    val fingerprint: String
    FileReader(fpFile).use {
        fingerprint = it.readText()
    }
    return fingerprint
}

fun generateFingerprint(context: Context, path: String, id: Long): Pair<File, Boolean> {

    val cacheDir = context.cacheDir.resolve(FINGERPRINTS_CACHE_DIR)
    if (!cacheDir.exists()) {
        cacheDir.mkdir()
    }

    val fpFile = cacheDir.resolve("$id")
    val result = if (!fpFile.exists()) FFmpegKit.execute("-y -t 120 -i '$path' -f chromaprint $fpFile")
        .returnCode.isValueSuccess else fpFile.totalSpace > 0

    return Pair(fpFile, result)
}