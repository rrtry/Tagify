package com.rrtry.tagify.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val title: String?,
    val artist: String?,
    @ColumnInfo(name = "track_count")
    val trackCount: Int?,
    @ColumnInfo(name = "artist_count")
    val artistCount: Int?,
): Parcelable
