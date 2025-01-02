package com.rrtry.tagify.util

import android.content.Context
import android.os.Build
import com.jtagger.AbstractTag
import com.jtagger.FileWrapper
import com.jtagger.MediaFile
import com.jtagger.StreamInfo
import com.rrtry.tagify.data.api.ItunesTag
import com.rrtry.tagify.data.api.TagApiImpl
import com.rrtry.tagify.data.entities.Tag
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.IOException

suspend fun <A, B> Iterable<A>.asyncMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun Iterable<Tag>.sortedTags(sortStr: String): List<Tag> {
    return sortedWith(
        compareBy<Tag> { if (it is ItunesTag) TagApiImpl.Provider.ITUNES else TagApiImpl.Provider.MUSICBRAINZ }
            .thenBy {
                levenshtein(sortStr, "${it.artist} - ${it.title}")
            }
            .thenBy {
                it.date.parseYear().let { year -> if (year != 0) year else Int.MAX_VALUE }
            }
    )
}

private fun catch(f: () -> Unit): Boolean {
    return try {
        f()
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        false
    } catch (e: IllegalStateException) {
        e.printStackTrace()
        false
    }
}

fun <T: AbstractTag, S: StreamInfo> MediaFile<T, S>.closeQuietly(): Boolean {
    return try {
        close()
        true
    } catch (e: IOException) {
        false
    }
}

fun <T: AbstractTag, S: StreamInfo> MediaFile<T, S>.scanCatching(context: Context, track: Track, mode: String): Boolean {
    return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R &&
               mode == "rw")
    {
        context.contentResolver.openFileDescriptor(track.trackUri, mode)?.let { pfd ->
            scanCatching(ParcelFileDescriptorImpl(pfd))
        } ?: false
    } else {
        scanCatching(File(track.path), mode)
    }
}

fun <T: AbstractTag, S: StreamInfo> MediaFile<T, S>.scanCatching(file: FileWrapper): Boolean {
    return catch { scan(file) }
}

fun <T: AbstractTag, S: StreamInfo> MediaFile<T, S>.scanCatching(file: File, mode: String): Boolean {
    return catch { scan(file, mode) }
}