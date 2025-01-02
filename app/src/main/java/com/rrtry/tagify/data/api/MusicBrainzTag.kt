package com.rrtry.tagify.data.api

import com.jtagger.AbstractTag
import com.rrtry.tagify.data.entities.Tag
import kotlinx.parcelize.Parcelize

val MUSICBRAINZ_ARTWORK_FORMATS = arrayOf(
    250, 500, 1200
)

@Parcelize
class MusicBrainzTag(
    val releaseId:       String,
    val releaseArtist:   String,
    val releaseTitle:    String,
    val releaseDate:     String,
    val recordingArtist: String,
    val recordingTitle:  String,
    val trackNumber:     String,
    val trackCount:      String,
    val discNumber:      String,
    val discCount:       String,
    val mediaFormat:     String,
    val recordingGenre:  String,
    val sources: Int = 0): Tag(
        recordingTitle,
        releaseDate,
        releaseTitle,
        recordingArtist,
        releaseArtist,
        trackNumber,
        trackCount,
        discNumber,
        discCount,
        mediaFormat,
        recordingGenre,
        null
    )
{
    override fun setFields(tag: AbstractTag) {
        super.setFields(tag)
        tag.setStringField(AbstractTag.ALBUM_ARTIST, releaseArtist)
    }
}