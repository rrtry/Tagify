package com.rrtry.tagify.data.db

import android.net.Uri
import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromUri(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let {
            Uri.parse(it)
        }
    }
}