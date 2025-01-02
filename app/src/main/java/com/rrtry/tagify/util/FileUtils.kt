package com.rrtry.tagify.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import com.jtagger.AbstractTag
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.ArrayList
import java.util.UUID
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume

const val MAX_FILENAME_LENGTH = 255
const val INVALID_FILENAME    = "(INVALID)"
const val REPLACEMENT_CHAR    = '_'

suspend fun scanFile(context: Context, path: String, mimeType: String): Boolean {
    return suspendCancellableCoroutine {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            arrayOf(mimeType))
        { path: String?, uri: Uri? ->
            it.resume(uri != null)
        }
    }
}

private fun isUUID(name: String): Boolean {
    return try { UUID.fromString(name); true } catch (e: IllegalArgumentException) { false }
}

fun Context.openFileDescriptor(uri: Uri, mode: String): ParcelFileDescriptor? {
    return try { contentResolver.openFileDescriptor(uri, mode) } catch (e: IOException) { null }
}

fun openRandomAccessFile(path: String, mode: String): RandomAccessFile? {
    return try { RandomAccessFile(path, mode) } catch (e: IOException) { null }
}

fun Context.openFileRef(track: Track, mode: String): FileReference? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        openFileDescriptor(track.trackUri, mode)?.let {
            ParcelFileDescriptorImpl(it)
        }
    } else {
        openRandomAccessFile(track.path, mode)?.let {
            RandomAccessFileImpl(it)
        }
    }
}

fun rename(oldPath: String, newPath: String): Boolean {
    return try { Os.rename(oldPath, newPath); true } catch (e: ErrnoException) { false }
}

fun getBackup(context: Context): File? {
    val files = context.filesDir.listFiles() ?: arrayOf<File>()
    for (file in files) {
        if (file.isFile &&
            isUUID(file.nameWithoutExtension))
        {
            return file
        }
    }
    return null
}

fun Char.isIllegalFilenameChar(): Boolean {
    return this == '/' || this == '\u0000'
}

fun String.sanitizeFilename(): String {

    if (this.isEmpty() ||
        this == ".."   ||
        this == ".")
    {
        return INVALID_FILENAME
    }

    val trimmed   = if (length > MAX_FILENAME_LENGTH) substring(0, MAX_FILENAME_LENGTH) else this
    val sanitized = StringBuilder(trimmed.length)

    trimmed.forEach { char ->
        sanitized.append(if (char.isIllegalFilenameChar()) REPLACEMENT_CHAR else char)
    }
    return sanitized.toString()
}

fun makeFilenameWithExtension(trackFile: File, newFilename: String): String {
    val extension = trackFile.name.substringAfterLast('.', "")
    return if (extension.isNotBlank()) "$newFilename.$extension" else newFilename
}

fun makeAbsolutePath(track: Track, displayName: String): String? {

    val currentFile  = File(track.path)
    val fullFilename = makeFilenameWithExtension(currentFile, displayName)
    val parentFile   = currentFile.parentFile

    val hasDuplicate = parentFile?.listFiles()?.any { file ->
        file.name == fullFilename
    } ?: false

    if (hasDuplicate) {
        Log.w("FilenameMatcher", "$fullFilename already exists in this directory")
        return null
    }
    return parentFile.absolutePath + File.separator + fullFilename
}

fun parseFilename(pattern: String, filename: String): Map<String, String>? {

    if (pattern.isBlank()) {
        return null
    }

    val tokens  = parseTokens(pattern) ?: return null
    val fTokens = tokens.first
    val sTokens = tokens.second

    if (fTokens.any { !AbstractTag.FIELDS.contains(it) }) {
        return null
    }

    val vMap       = hashMapOf<String, String>()
    var chIndex    = 0
    var fieldValue = ""

    while (chIndex < filename.length &&
        fTokens.isNotEmpty())
    {
        val char  = filename[chIndex]
        val field = fTokens.first()
        val sep   = sTokens.firstOrNull()

        if (sep == null) {
            filename.substring(chIndex, filename.length).let {
                if (it.isNotBlank()) {
                    fTokens.removeFirstOrNull()
                    vMap[field] = it.trim()
                }
            }
            break
        }
        // Check first char before allocating a substring
        if (sep.first() == char &&
            chIndex + sep.length <= filename.length &&
            filename.substring(chIndex, chIndex + sep.length) == sep)
        {
            if (fieldValue.isNotBlank()) {
                vMap[field] = fieldValue.trim()
                fTokens.removeFirst()
            }

            chIndex += sep.length
            sTokens.removeFirst()

            fieldValue = ""
            continue
        }
        fieldValue += char
        chIndex++
    }
    if (fTokens.isNotEmpty() || sTokens.isNotEmpty()) {
        return null
    }
    return vMap
}

fun parseTokens(pattern: String): Pair<ArrayDeque<String>, ArrayDeque<String>>? {

    val fTokens = ArrayDeque<String>()
    val sTokens = ArrayDeque<String>()

    var fToken: String? = null
    var sToken = ""

    pattern.forEach {
        if (it == '%') {
            if (sToken.isNotEmpty()) {
                sTokens.add(sToken)
                sToken = ""
            }
            fToken = if (fToken != null) {
                fTokens.add(fToken!!)
                null
            } else {
                ""
            }
        } else if (fToken != null) {
            fToken += it
        } else {
            sToken += it
        }
    }
    if (fToken != null) return null
    if (sToken.isNotEmpty()) sTokens.add(sToken)
    return Pair(fTokens, sTokens)
}

fun makeDisplayName(tag: AbstractTag, formatString: String): String? {

    val tokens  = parseTokens(formatString) ?: return null
    val fTokens = tokens.first
    val sTokens = tokens.second

    val formatStart = formatString.indexOfFirst { it == '%' }
    val formatEnd   = formatString.indexOfLast  { it == '%' }

    val parts   = ArrayList<String>()
    val hasTail = formatEnd   != (formatString.length - 1)
    val hasHead = formatStart != 0

    parts.ensureCapacity(fTokens.size + sTokens.size)
    if (hasHead) {
        parts += formatString.substring(0, formatStart)
        sTokens.removeAt(0)
    }

    for (fIndex in 0..<fTokens.size) {

        val field = fTokens[fIndex]
        val value = tag.getStringField(field)?.let {
            if (field == AbstractTag.YEAR &&
                it.length > 4)
            {
                it.safeSubstring(0, 4) // Trim string if it contains full timestamp
            } else {
                it
            }
        }
        if (!value.isNullOrBlank()) {
            if ((fIndex - 1) in 0..<sTokens.size &&
                (hasHead || parts.size > 0))
            {
                parts += sTokens[fIndex - 1]
            }
            parts += value
        } else {
            return null
        }
    }
    if (hasTail) {
        parts += sTokens.last()
    }
    return parts
        .joinToString("")
        .ifBlank { null }
        ?.sanitizeFilename()
}