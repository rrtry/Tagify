package com.rrtry.tagify.data.entities

import android.os.Parcelable
import com.jtagger.AbstractTag
import kotlinx.parcelize.Parcelize
import java.net.URL

fun setArtwork(tag: Tag, url: URL) = Tag(
    tag.title,
    tag.date,
    tag.album,
    tag.artist,
    tag.albArt,
    tag.trkPos,
    tag.trkCnt,
    tag.discPos,
    tag.discCnt,
    tag.format,
    tag.genre,
    url.toString()
)

@Parcelize
open class Tag(
    val title:   String,
    val date:    String,
    val album:   String,
    val artist:  String,
    val albArt:  String,
    val trkPos:  String,
    val trkCnt:  String,
    val discPos: String,
    val discCnt: String,
    val format:  String,
    val genre:   String,
    var cover:   String? = null
): Parcelable {

    open fun setFields(tag: AbstractTag) {
        if (date.length >= 4) {
            tag.setStringField(AbstractTag.YEAR, date)
        }
        tag.setStringField(AbstractTag.TITLE,  title)
        tag.setStringField(AbstractTag.ALBUM,  album)
        tag.setStringField(AbstractTag.ARTIST, artist)
        tag.setStringField(AbstractTag.GENRE,  genre)
        tag.setStringField(AbstractTag.TRACK_NUMBER, "$trkPos/$trkCnt")
        tag.setStringField(AbstractTag.DISC_NUMBER, "$discPos/$discCnt")
    }
}
