package com.rrtry.tagify.data

import com.rrtry.tagify.data.api.TagApiImpl
import com.rrtry.tagify.data.entities.Tag
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val RESULTS_LIMIT = 10

@Singleton
class TagRepository @Inject constructor(private val tagApi: TagApiImpl) {

    suspend fun fetchTags(artist: String, title: String, provider: TagApiImpl.Provider): Pair<Boolean, List<Tag>> {
        return tagApi.fetchTags(artist, title, provider)
    }

    suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): Pair<Boolean, List<Tag>> {
        return tagApi.fetchTags(apiKey, fingerprint, length)
    }

    suspend fun fetchArtworkCatching(
        artist: String,
        album: String,
        size: Int,
        provider: TagApiImpl.Provider): String?
    {
        return try {
            tagApi.fetchArtwork(artist, album, size, provider)
        } catch (e: IOException) {
            null
        }
    }

    suspend fun fetchArtworkCatching(tag: Tag, size: Int) {
        try {
            tagApi.fetchArtwork(tag, size)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}