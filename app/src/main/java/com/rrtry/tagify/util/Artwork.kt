package com.rrtry.tagify.util

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.provider.MediaStore.VOLUME_EXTERNAL
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.jtagger.AbstractTag
import com.jtagger.AttachedPicture
import com.rrtry.tagify.LOCAL_ARTWORK_FILES_DIR
import com.rrtry.tagify.LOCAL_THUMBNAIL_FILES_DIR
import com.rrtry.tagify.prefs.prefsGetArtworkSize
import com.rrtry.tagify.ui.components.ARTWORK_CACHE_KEY_PREFIX
import com.rrtry.tagify.ui.components.THUMBNAIL_CACHE_KEY_PREFIX
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val MIN_ARTWORK_SIZE = 100
const val MAX_ARTWORK_SIZE = 1200

const val THUMBNAIL_SIZE       = 94
const val DEFAULT_ARTWORK_SIZE = 600

private const val ARTWORK_MIME_TYPE = "image/jpeg"
private const val ARTWORK_DIRECTORY = "Exported artwork"
private const val ARTWORK_QUALITY   = 100
private const val THUMBNAIL_QUALITY = 20

suspend fun exportArtworkDownloads(
    app: Application,
    source: Any?,
    nameWithoutExt: String): Pair<Boolean, File>
{
    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .resolve(ARTWORK_DIRECTORY).apply {
            if (!exists()) mkdir()
        }
    val file    = directory.resolve("$nameWithoutExt.jpg")
    val written = loadImage(app, source)?.let { bitmap ->
        try {
            val result = FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, out)
            }
            Pair(result, file)
        } catch (e: IOException) {
            Pair(false, file)
        }
    } ?: Pair(false, file)
    val scanned = scanFile(
        app.applicationContext,
        file.canonicalPath,
        ARTWORK_MIME_TYPE
    )
    return Pair(
        written.first && scanned,
        file
    )
}

private fun Application.removeFromMemoryCache(key: String) {
    imageLoader.memoryCache?.remove(MemoryCache.Key(key))
}

private fun Context.resolveFilesDir(dir: String): File {
    return filesDir.resolve(dir).apply {
        if (!exists()) mkdir()
    }
}

fun Context.resolveArtworkSanitized(album: String) = getArtworkDir().resolve(album.sanitizeFilename())
fun Context.resolveArtwork(filename: String) = getArtworkDir().resolve(filename)
fun Context.resolveThumbnail(id: Long) = getThumbsDir().resolve(id.toString())

fun Context.getThumbsDir()  = resolveFilesDir(LOCAL_THUMBNAIL_FILES_DIR)
fun Context.getArtworkDir() = resolveFilesDir(LOCAL_ARTWORK_FILES_DIR)

fun Context.deleteThumbnail(id: Long)    = resolveThumbnail(id).delete()
fun Context.deleteArtwork(title: String) = resolveArtworkSanitized(title).delete()

fun Context.getThumbnailTimestamp(mediaId: Long): Long {
    val thumbnail = resolveThumbnail(mediaId)
    return if (thumbnail.exists()) thumbnail.lastModified() else 0L
}

fun Context.removeThumbnailFromCache(mediaId: Long) {
    (applicationContext as Application)
        .removeFromMemoryCache("${THUMBNAIL_CACHE_KEY_PREFIX}:$mediaId")
}

fun Context.removeArtworkFromCache(album: String) {
    (applicationContext as Application)
        .removeFromMemoryCache("${ARTWORK_CACHE_KEY_PREFIX}:$album")
}

fun decodeBitmapFromFd(fd: FileDescriptor, reqWidth: Int, reqHeight: Int): Bitmap? {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(fd, null, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        BitmapFactory.decodeFileDescriptor(fd, null, this)
    }
}

fun decodeBitmapFromArray(
    array: ByteArray,
    subsampleFactor: Int): Bitmap?
{
    return BitmapFactory.Options().run {
        inSampleSize = subsampleFactor
        BitmapFactory.decodeByteArray(array, 0, array.size, this)
    }
}

fun decodeBitmapFromArray(
    array: ByteArray,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(array, 0, array.size, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        BitmapFactory.decodeByteArray(array, 0, array.size, this)
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun getInSampleSize(
    buffer: ByteArray,
    reqWidth: Int,
    reqHeight: Int): Int
{
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(buffer, 0, buffer.size, this)
        calculateInSampleSize(this, reqWidth, reqHeight)
    }
}

fun saveArtwork(
    buffer: ByteArray,
    outStream: FileOutputStream)
{
    decodeBitmapFromArray(buffer, DEFAULT_ARTWORK_SIZE, DEFAULT_ARTWORK_SIZE)
        .compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, outStream)
}

fun saveThumbnail(
    buffer: ByteArray,
    outStream: FileOutputStream)
{
    decodeBitmapFromArray(buffer, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
        .compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outStream)
}

fun getImageMimeType(context: Context, uri: Uri): String? {
    context.contentResolver.query(
        uri, null, null, null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE))
        }
    }
    return null
}

fun getAttachedPicture(context: Context, uri: Uri, artworkSize: Int): AttachedPicture? {
    val imageType = getImageMimeType(context, uri)
    val byteArray = try {
        context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        }
    } catch (e: IOException) {
        null
    }
    if (byteArray != null) {
        val inSampleSize = getInSampleSize(byteArray, artworkSize, artworkSize)
        if (inSampleSize > 1) {
            decodeBitmapFromArray(byteArray, inSampleSize)?.let { bitmap ->
                return getAttachedPicture(bitmap)
            }
        }
        return AttachedPicture().apply {
            pictureData = byteArray
            if (imageType != null) mimeType = imageType
        }
    }
    return null
}

fun getAttachedPicture(bitmap: Bitmap): AttachedPicture? {

    val outStream = ByteArrayOutputStream()
    val success   = bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, outStream)
    if (!success) return null

    val apic = AttachedPicture()
    apic.mimeType    = "image/jpg"
    apic.pictureData = outStream.toByteArray()
    return apic
}

fun setArtwork(tag: AbstractTag, bitmap: Bitmap?, apic: AttachedPicture) {
    if (bitmap != null) {

        val outStream = ByteArrayOutputStream()
        apic.mimeType = ARTWORK_MIME_TYPE
        bitmap.compress(Bitmap.CompressFormat.JPEG, ARTWORK_QUALITY, outStream)

        apic.pictureData = outStream.toByteArray()
        tag.pictureField = apic
    }
}

suspend fun loadImage(app: Application, source: Any?, cache: Boolean = true): Bitmap? {

    if (source == null) {
        return null
    }

    val loader  = app.imageLoader
    var builder = ImageRequest.Builder(app.applicationContext)
        .size(prefsGetArtworkSize(app))
        .data(source)

    builder = if (cache) {
        builder.networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
    } else {
        builder.networkCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
    }
    val result = loader.execute(builder.build())
    return if (result is SuccessResult) (result.drawable as BitmapDrawable).bitmap else null
}