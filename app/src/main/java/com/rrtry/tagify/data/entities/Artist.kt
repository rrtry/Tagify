package com.rrtry.tagify.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(
    val name: String?,
    @ColumnInfo(name = "track_count")
    val trackCount: Int?,
    @ColumnInfo(name = "album_count")
    val albumCount: Int?,
    @ColumnInfo(name = "first_album")
    var firstAlbum: String?
): Parcelable
