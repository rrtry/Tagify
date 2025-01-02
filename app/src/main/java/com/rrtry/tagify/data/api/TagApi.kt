package com.rrtry.tagify.data.api

import com.rrtry.tagify.data.entities.Tag

interface TagApi {

    suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): Pair<Boolean, List<Tag>>
    suspend fun fetchTags(artist: String, title: String, provider: TagApiImpl.Provider): Pair<Boolean, List<Tag>>
    suspend fun fetchArtwork(tag: Tag, size: Int)
    suspend fun fetchArtwork(
        artist: String,
        album: String,
        size: Int,
        provider: TagApiImpl.Provider
    ): String?
}