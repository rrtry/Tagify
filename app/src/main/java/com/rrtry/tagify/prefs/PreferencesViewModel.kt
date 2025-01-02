package com.rrtry.tagify.prefs

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.rrtry.tagify.util.DEFAULT_ARTWORK_SIZE
import kotlinx.coroutines.flow.MutableStateFlow
import com.jtagger.AbstractTag.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(private val app: Application): ViewModel() {

    val batchLookupMethod  = MutableStateFlow(PREFS_VALUE_LOOKUP_METHOD_FINGERPRINT)
    val manualLookupMethod = MutableStateFlow(PREFS_VALUE_LOOKUP_METHOD_NONE)
    val artworkSize        = MutableStateFlow(DEFAULT_ARTWORK_SIZE)
    val tagFields          = MutableStateFlow(setOf(TITLE, ARTIST, ALBUM, YEAR, PICTURE))
    val filenamePattern    = MutableStateFlow(DEFAULT_FILENAME_PATTERN)
    val renameFiles        = MutableStateFlow(false)
    val fixEncoding        = MutableStateFlow(true)

    init {
        batchLookupMethod.value  = prefsGetBatchModePreferredLookupMethod(app)
        manualLookupMethod.value = prefsGetManualModePreferredLookupMethod(app)

        artworkSize.value     = prefsGetArtworkSize(app)
        renameFiles.value     = prefsGetRenameFiles(app)
        tagFields.value       = prefsGetRequiredFields(app)
        fixEncoding.value     = prefsGetFixEncoding(app)
        filenamePattern.value = prefsGetFilenamePattern(app)
    }

    fun saveChanges() {
        app.getPreferences().edit {

            putInt(PREFS_KEY_BATCH_LOOKUP_METHOD,  batchLookupMethod.value)
            putInt(PREFS_KEY_MANUAL_LOOKUP_METHOD, manualLookupMethod.value)

            putBoolean(PREFS_KEY_RENAME_FILES, renameFiles.value)
            putBoolean(PREFS_KEY_FIX_ENCODING, fixEncoding.value)

            putInt(PREFS_KEY_ARTWORK_SIZE, artworkSize.value)
            putStringSet(PREFS_KEY_REQUIRED_FIELDS, tagFields.value)
            putString(PREFS_KEY_FILENAME_PATTERN, filenamePattern.value)
        }
    }
}