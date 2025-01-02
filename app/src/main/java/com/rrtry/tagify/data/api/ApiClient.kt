package com.rrtry.tagify.data.api

import com.rrtry.tagify.data.entities.Tag

interface ApiClient <T: Tag> {

    suspend fun fetchTags(artist: String, title: String): MutableList<T>
    suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): MutableList<T>

    suspend fun fetchArtwork(tag: T, coverSize: Int)
    suspend fun fetchArtwork(tag: T)
    suspend fun fetchArtwork(artist: String, album: String, size: Int): String?
}