package com.rrtry.tagify.data.api

import com.rrtry.tagify.data.entities.Tag
import javax.inject.Inject
import javax.inject.Singleton

// Itunes API
private const val ITUNES_MAX_REQUESTS = 20
private const val ITUNES_WINDOW_SIZE  = 60 * 1000L

// AcoustID web service
private const val ACOUSTID_MAX_REQUESTS = 3
private const val ACOUSTID_WINDOW_SIZE  = 1000L

// MusicBrainz API
private const val MUSICBRAINZ_MAX_REQUESTS = 1
private const val MUSICBRAINZ_WINDOW_SIZE  = 1000L

@Singleton
class TagApiImpl @Inject constructor(
    private val acoustIDClient: AcoustIDClient,
    private val musicBrainzClient: MusicBrainzClient,
    private val itunesClient: ItunesClient): TagApi
{
    enum class Provider {
        ITUNES, MUSICBRAINZ
    }

    private val itunesLimiter      = SlidingWindowRateLimiter(ITUNES_MAX_REQUESTS, ITUNES_WINDOW_SIZE)
    private val musicBrainzLimiter = SlidingWindowRateLimiter(MUSICBRAINZ_MAX_REQUESTS, MUSICBRAINZ_WINDOW_SIZE)
    private val acoustIDLimiter    = SlidingWindowRateLimiter(ACOUSTID_MAX_REQUESTS, ACOUSTID_WINDOW_SIZE)

    override suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): Pair<Boolean, List<Tag>> {
        val response: List<Tag>? = acoustIDLimiter.makeRequest {
            acoustIDClient.fetchTags(apiKey, fingerprint, length)
        }
        return if (response == null) Pair(false, listOf()) else Pair(true, response)
    }

    override suspend fun fetchTags(artist: String, title: String, provider: Provider): Pair<Boolean, List<Tag>> {

        val limiter = if (provider == Provider.ITUNES) itunesLimiter else musicBrainzLimiter
        val client  = if (provider == Provider.ITUNES) itunesClient else musicBrainzClient

        val response: List<Tag>? = limiter.makeRequest {
            client.fetchTags(artist, title)
        }
        return if (response == null) Pair(false, listOf()) else Pair(true, response)
    }

    override suspend fun fetchArtwork(tag: Tag, size: Int) {
        if (tag is MusicBrainzTag) {
            acoustIDClient.fetchArtwork(tag, size)
        } else {
            itunesClient.fetchArtwork(tag as ItunesTag, size)
        }
    }

    override suspend fun fetchArtwork(
        artist: String,
        album: String,
        size: Int,
        provider: Provider): String?
    {
        val limiter = if (provider == Provider.ITUNES) itunesLimiter else musicBrainzLimiter
        val client  = if (provider == Provider.ITUNES) itunesClient else musicBrainzClient

        return limiter.makeRequest {
            client.fetchArtwork(artist, album, size)
        }
    }
}