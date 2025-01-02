package com.rrtry.tagify.data.api

import com.jtagger.AbstractTag
import com.rrtry.tagify.data.entities.Tag
import kotlinx.parcelize.Parcelize

val ITUNES_ARTWORK_FORMATS = setOf(
    30, 40, 60, 100, 110, 130, 150, 160, 170,
    200, 220, 230, 250, 340, 400, 440, 450,
    460, 600, 1200, 1400
)

@Parcelize
class ItunesTag(
    val trackTitle:  String,
    val releaseDate: String,
    val albumName:   String,
    val artistName:  String,
    val trackNumber: String,
    val trackCount:  String,
    val discNumber:  String,
    val discCount:   String,
    val trackGenre:  String,
    val country:     String,
    val artworkURL:  String): Tag(
        trackTitle,
        releaseDate,
        albumName,
        artistName,
        artistName,
        trackNumber,
        trackCount,
        discNumber,
        discCount,
        "",
        trackGenre,
        artworkURL
    )
{
    override fun setFields(tag: AbstractTag) {
        super.setFields(tag)
        tag.setStringField(AbstractTag.RELEASE_COUNTRY, country)
    }
}