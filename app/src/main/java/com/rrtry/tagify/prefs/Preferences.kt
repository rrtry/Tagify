package com.rrtry.tagify.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jtagger.AbstractTag
import com.jtagger.AbstractTag.ALBUM
import com.jtagger.AbstractTag.ALBUM_ARTIST
import com.jtagger.AbstractTag.ARTIST
import com.jtagger.AbstractTag.COMPOSER
import com.jtagger.AbstractTag.DISC_NUMBER
import com.jtagger.AbstractTag.GENRE
import com.jtagger.AbstractTag.PICTURE
import com.jtagger.AbstractTag.TITLE
import com.jtagger.AbstractTag.TRACK_NUMBER
import com.jtagger.AbstractTag.YEAR
import com.rrtry.tagify.R
import com.rrtry.tagify.util.DEFAULT_ARTWORK_SIZE

fun getFieldDisplayNameRes(field: String): Int {
    return when (field) {
        TITLE        -> R.string.title
        ARTIST       -> R.string.artist
        ALBUM        -> R.string.album
        PICTURE      -> R.string.artwork
        YEAR         -> R.string.year
        GENRE        -> R.string.genre
        ALBUM_ARTIST -> R.string.alb_art
        COMPOSER     -> R.string.composer
        TRACK_NUMBER -> R.string.trk_num
        DISC_NUMBER  -> R.string.dsk_num
        else -> throw IllegalArgumentException("Unsupported field supplied: $field")
    }
}

fun getLookupMethodDisplayName(cxt: Context, method: Int): String {
    return when (method) {
        PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT -> cxt.getString(R.string.fingerprint_lookup_method)
        PREFS_VALUE_LOOKUP_METHOD_FILENAME    -> cxt.getString(R.string.filename_lookup_method)
        PREFS_VALUE_LOOKUP_METHOD_QUERY       -> cxt.getString(R.string.manual_lookup_method)
        PREFS_VALUE_LOOKUP_METHOD_NONE        -> cxt.getString(R.string.choose_lookup_method)
        else -> throw IllegalArgumentException("Illegal lookup method: $method")
    }
}

fun <T: Any> Context.preferenceToDisplayName(key: String, value: T): String {
    return when (key) {
        PREFS_KEY_ARTWORK_SIZE         -> "${value}x${value}"
        PREFS_KEY_BATCH_LOOKUP_METHOD  -> getLookupMethodDisplayName(this, value as Int)
        PREFS_KEY_MANUAL_LOOKUP_METHOD -> getLookupMethodDisplayName(this, value as Int)
        PREFS_KEY_REQUIRED_FIELDS      -> (value as Set<*>).joinToString(", ") {
            getString(getFieldDisplayNameRes(it as String))
        }
        else -> value.toString()
    }
}

fun Context.getPreferences(): SharedPreferences = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

fun prefsGetBatchModePreferredLookupMethod(context: Context): Int {
    return context.getPreferences()
        .getInt(PREFS_KEY_BATCH_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT)
}

fun prefsGetManualModePreferredLookupMethod(context: Context): Int {
    return context.getPreferences()
        .getInt(PREFS_KEY_MANUAL_LOOKUP_METHOD, PREFS_VALUE_LOOKUP_METHOD_NONE)
}

fun prefsGetArtworkSize(context: Context): Int {
    return context.getPreferences()
        .getInt(PREFS_KEY_ARTWORK_SIZE, DEFAULT_ARTWORK_SIZE)
}

fun prefsGetRequiredFields(context: Context): MutableSet<String> {
    return context.getPreferences().getStringSet(
        PREFS_KEY_REQUIRED_FIELDS, setOf(TITLE, ALBUM, ARTIST)
    )!!
}

fun prefsGetFixEncoding(context: Context): Boolean {
    return context.getPreferences()
        .getBoolean(PREFS_KEY_FIX_ENCODING, true)
}

fun prefsGetRenameFiles(context: Context): Boolean {
    return context.getPreferences()
        .getBoolean(PREFS_KEY_RENAME_FILES, false)
}

fun prefsGetFilenamePattern(context: Context): String {
    return context.getPreferences()
        .getString(PREFS_KEY_FILENAME_PATTERN, DEFAULT_FILENAME_PATTERN)!!
}

fun prefsHasRequiredFields(context: Context, tag: AbstractTag?): Boolean {
    if (tag == null) return false
    for (field in prefsGetRequiredFields(context)) {
        if (field == PICTURE) {
            if (tag.pictureField == null) {
                return false
            }
        } else if (tag.getStringField(field).isNullOrBlank()) {
            return false
        }
    }
    return true
}

fun prefsGetApiKey(context: Context): String? = context.getPreferences().getString(PREFS_KEY_API_KEY, null)

fun prefsSetManualModePreferredLookupMethod(context: Context, option: Int) {
    if (option != PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT &&
        option != PREFS_VALUE_LOOKUP_METHOD_FILENAME &&
        option != PREFS_VALUE_LOOKUP_METHOD_QUERY &&
        option != PREFS_VALUE_LOOKUP_METHOD_NONE)
    {
        throw IllegalArgumentException("Invalid preference value: $option for $PREFS_KEY_MANUAL_LOOKUP_METHOD")
    }
    context.getPreferences().edit {
        putInt(PREFS_KEY_MANUAL_LOOKUP_METHOD, option)
    }
}

fun prefsGetLastModifiedFile(context: Context): String? {
    return context.getPreferences()
        .getString(PREFS_KEY_LAST_MODIFIED_FILE, null)
}

fun prefsSetLastModifiedFile(context: Context, path: String) {
    context.getPreferences().edit(commit = true) {
        putString(PREFS_KEY_LAST_MODIFIED_FILE, path)
    }
}

fun prefsSetApiKey(context: Context, key: String) {
    context.getPreferences().edit(commit = true) {
        putString(PREFS_KEY_API_KEY, key)
    }
}

const val SHARED_PREFERENCES           = "com.rrtry.tagify.PREFERENCES"
const val PREFS_KEY_API_KEY            = "api_key"
const val PREFS_KEY_REQUIRED_FIELDS    = "required_fields"
const val PREFS_KEY_ARTWORK_SIZE       = "artwork_size"
const val PREFS_KEY_FIX_ENCODING       = "fix_encoding"
const val PREFS_KEY_RENAME_FILES       = "rename_files"
const val PREFS_KEY_FILENAME_PATTERN   = "filename_pattern"
const val PREFS_KEY_LAST_MODIFIED_FILE = "last_modified_file"
const val DEFAULT_FILENAME_PATTERN     = "%$ARTIST% - %$TITLE%"

const val PREFS_KEY_MANUAL_LOOKUP_METHOD = "manual_lookup_method"
const val PREFS_KEY_BATCH_LOOKUP_METHOD  = "batch_lookup_method"

const val PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT = 0x4
const val PREFS_VALUE_LOOKUP_METHOD_FILENAME    = 0x2
const val PREFS_VALUE_LOOKUP_METHOD_QUERY       = 0x1
const val PREFS_VALUE_LOOKUP_METHOD_NONE        = 0x0
