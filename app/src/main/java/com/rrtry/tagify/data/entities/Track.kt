package com.rrtry.tagify.data.entities

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jtagger.AbstractTag
import com.rrtry.tagify.prefs.prefsHasRequiredFields
import com.rrtry.tagify.util.parseNumberPair
import com.rrtry.tagify.util.parseYear
import kotlinx.parcelize.Parcelize
import java.io.File

@Entity
@Parcelize
data class Track(

    @PrimaryKey
    var trackId: Long,

    val title:    String,
    val album:    String,
    val artist:   String,
    val mimeType: String,
    val year:     Int,
    val trkPos:   Int,
    val trkCnt:   Int,
    val discPos:  Int,
    val discCnt:  Int,
    val genre:    String,
    val path:     String,
    val trackUri: Uri,
    val hasTag:   Boolean,
    val duration: Int
): Parcelable {

    override fun toString(): String {
        return File(path).name
    }
}

fun Track.fromTag(
    appContext: Context,
    tag: AbstractTag,
    titlePlaceholder: String,
    albumPlaceholder: String,
    artistPlaceholder: String): Track
{
    val trkPair  = tag.getStringField(AbstractTag.TRACK_NUMBER)?.parseNumberPair() ?: Pair(0, 0)
    val discPair = tag.getStringField(AbstractTag.DISC_NUMBER)?.parseNumberPair()  ?: Pair(0, 0)

    return this.copy(
        title   = tag.getStringField(AbstractTag.TITLE)?.ifBlank  { titlePlaceholder }  ?: titlePlaceholder,
        album   = tag.getStringField(AbstractTag.ALBUM)?.ifBlank  { albumPlaceholder }  ?: albumPlaceholder,
        artist  = tag.getStringField(AbstractTag.ARTIST)?.ifBlank { artistPlaceholder } ?: artistPlaceholder,
        year    = tag.getStringField(AbstractTag.YEAR)?.parseYear() ?: 0,
        genre   = tag.getStringField(AbstractTag.GENRE) ?: "",
        trkPos  = trkPair.first,
        trkCnt  = trkPair.second,
        discPos = discPair.first,
        discCnt = discPair.second,
        hasTag  = prefsHasRequiredFields(appContext, tag)
    )
}
