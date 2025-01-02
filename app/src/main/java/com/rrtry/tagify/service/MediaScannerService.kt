package com.rrtry.tagify.service

import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.jtagger.AbstractTag
import com.jtagger.AttachedPicture
import com.jtagger.MediaFile
import com.jtagger.StreamInfo
import com.rrtry.tagify.ACTION_SCAN_MEDIA
import com.rrtry.tagify.R
import com.rrtry.tagify.ui.home.BaseViewModel.ProgressInfo
import com.rrtry.tagify.data.MediaRepository
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.prefs.prefsHasRequiredFields
import com.rrtry.tagify.ui.home.BaseViewModel
import com.rrtry.tagify.util.closeQuietly
import com.rrtry.tagify.util.deleteArtwork
import com.rrtry.tagify.util.deleteThumbnail
import com.rrtry.tagify.util.getArtworkDir
import com.rrtry.tagify.util.getThumbsDir
import com.rrtry.tagify.util.parseNumberPair
import com.rrtry.tagify.util.parseYear
import com.rrtry.tagify.util.resolveArtwork
import com.rrtry.tagify.util.resolveThumbnail
import com.rrtry.tagify.util.sanitizeFilename
import com.rrtry.tagify.util.saveArtwork
import com.rrtry.tagify.util.saveThumbnail
import com.rrtry.tagify.util.scanCatching
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

val CONTENT_URI: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                       else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

private val PROJECTION = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.ALBUM_ID,
    MediaStore.Audio.Media.DATA
)

private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
private const val WAKELOCK_TAG = "com.rrtry.tagify::MediaScannerWakeLock"
private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L

@AndroidEntryPoint
class MediaScannerService: Service() {

    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var serviceBus: ServiceEventBus

    private val job    = Job()
    private val scope  = CoroutineScope(job + Dispatchers.IO)

    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    private lateinit var intent: Intent
    private lateinit var wakeLock: WakeLock

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            acquire(WAKELOCK_TIMEOUT)
        }
    }

    private fun writeThumbnail(mediaId: Long, picture: AttachedPicture?) {
        picture?.pictureData?.let { buffer ->
            FileOutputStream(resolveThumbnail(mediaId)).use { fos ->
                saveThumbnail(buffer, fos)
            }
        }
    }

    private fun writeArtwork(
        albums:   HashSet<String>,
        filename: String,
        picture:  AttachedPicture?,
        default:  String)
    {
        if (picture != null) {
            if (!albums.contains(filename)) {
                albums.add(filename)
                if (filename != default) {
                    writeArtwork(filename, picture)
                }
            }
        }
    }

    private fun writeArtwork(filename: String, picture: AttachedPicture?) {
        picture?.pictureData?.let { buffer ->
            FileOutputStream(resolveArtwork(filename)).use { fos ->
                saveArtwork(buffer, fos)
            }
        }
    }

    private suspend fun deleteInvalidTrack(trackId: Long) {
        mediaRepository.getTrackById(trackId)?.let { track ->
            mediaRepository.deleteTrackById(track.trackId)
            deleteThumbnail(trackId)
            if (mediaRepository.getTrackCountByAlbum(track.album) == 0) {
                deleteArtwork(track.album)
            }
        }
    }

    private suspend fun deleteMissingTracks(trkIdSet: HashSet<Long>) {
        mediaRepository.getTracks().forEach { track ->
            if (!trkIdSet.contains(track.trackId)) {
                mediaRepository.deleteTrackById(track.trackId)
            }
        }
    }

    private fun cleanupThumbnailFiles(thumbsDir: File, trkIds: Set<Long>) {
        thumbsDir.listFiles()?.forEach { file ->
            if (!trkIds.contains(file.name.toLong())) {
                file.delete()
            }
        }
    }

    private fun cleanupArtworkFiles(artworkDir: File, albums: Set<String>) {
        artworkDir.listFiles()?.forEach { file ->
            if (!albums.contains(file.name)) {
                file.delete()
            }
        }
    }

    private suspend fun scanMedia() = coroutineScope {

        val mediaFile  = MediaFile<AbstractTag, StreamInfo>()
        val artworkDir = getArtworkDir()
        val thumbsDir  = getThumbsDir()

        val update  = mediaRepository.getTrackCount() != 0
        var scanned = 0
        val trkIds  = hashSetOf<Long>()
        val albums  = hashSetOf<String>()

        contentResolver.query(
            CONTENT_URI,
            PROJECTION,
            SELECTION,
            null,
            null
        )?.use { cursor ->

            serviceBus.progressInfo.value = ProgressInfo(ACTION_SCAN_MEDIA, 0, cursor.count)
            serviceBus.inProgress.value   = true

            val idColumn   = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {

                ensureActive()

                val path     = cursor.getString(pathColumn)
                val trackId  = cursor.getLong(idColumn)
                val file     = File(path)
                val trackUri = ContentUris.withAppendedId(CONTENT_URI, trackId)

                if (!mediaFile.scanCatching(file, "r") || mediaFile.streamInfo == null) {
                    if (update) {
                        deleteInvalidTrack(trackId)
                    }
                } else {

                    trkIds.add(trackId)
                    val tag = mediaFile.tag
                    val streamInfo = mediaFile.streamInfo

                    val unknown       = getString(R.string.unknown)
                    val unknownAlbum  = getString(R.string.unknown_album)
                    val unknownArtist = getString(R.string.unknown_artist)

                    val title    = tag?.getStringField(AbstractTag.TITLE)?.ifBlank  { file.name }     ?: file.name
                    val album    = tag?.getStringField(AbstractTag.ALBUM)?.ifBlank  { unknownAlbum }  ?: unknownAlbum
                    val artist   = tag?.getStringField(AbstractTag.ARTIST)?.ifBlank { unknownArtist } ?: unknownArtist
                    val genre    = tag?.getStringField(AbstractTag.GENRE)?.ifBlank        { "" } ?: ""
                    val year     = tag?.getStringField(AbstractTag.YEAR)?.ifBlank         { "" } ?: ""
                    val trkNum   = tag?.getStringField(AbstractTag.TRACK_NUMBER)?.ifBlank { "" } ?: ""
                    val discNum  = tag?.getStringField(AbstractTag.DISC_NUMBER)?.ifBlank  { "" } ?: ""
                    val mimeType = mediaFile.mimeType ?: unknown

                    val trkPair  = trkNum.parseNumberPair()
                    val discPair = discNum.parseNumberPair()

                    val trkPos  = trkPair.first
                    val trkCnt  = trkPair.second

                    val discPos = discPair.first
                    val discCnt = discPair.second

                    val duration = streamInfo.duration
                    val picture  = tag?.pictureField
                    val hasTag   = prefsHasRequiredFields(this@MediaScannerService, tag)

                    writeThumbnail(trackId, picture)
                    writeArtwork(albums, album.sanitizeFilename(), picture, unknownAlbum)

                    mediaRepository.insertTrack(
                        Track(
                            trackId, title, album, artist, mimeType, year.parseYear(),
                            trkPos, trkCnt, discPos, discCnt,
                            genre, path, trackUri, hasTag, duration
                        )
                    )
                }
                mediaFile.closeQuietly()
                serviceBus.progressInfo.value = ProgressInfo(ACTION_SCAN_MEDIA, ++scanned, cursor.count)
                notificationManager.notify(
                    SERVICE_ID,
                    createScanServiceNotification(
                        this@MediaScannerService,
                        CHANNEL_ID,
                        scanned,
                        cursor.count
                    )
                )
            }
        }
        cleanupThumbnailFiles(thumbsDir, trkIds)
        cleanupArtworkFiles(artworkDir, albums)
        deleteMissingTracks(trkIds)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        this.intent = intent!!
        startForegroundServiceCompat(
            this,
            SERVICE_ID,
            createScanServiceNotification(
                this,
                CHANNEL_ID,
                null,
                null
            )
        )
        scope.launch {
            scanMedia()
        }.invokeOnCompletion {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        wakeLock.release()
        serviceBus.inProgress.value = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        fun startService(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaScannerService::class.java).apply {
                action = ACTION_SCAN_MEDIA
            })
        }

        private const val SERVICE_ID = 101
        private const val CHANNEL_ID = "com.rrtry.tagify.CHANNEL_SCAN_SERVICE"
    }
}