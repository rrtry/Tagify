package com.rrtry.tagify.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.rrtry.tagify.data.RESULTS_LIMIT
import com.rrtry.tagify.util.levenshtein
import com.rrtry.tagify.util.luceneEscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.NullPointerException

private const val MB_RECORDINGS = "https://musicbrainz.org/ws/2/recording?query="
private const val MB_RELEASES   = "https://musicbrainz.org/ws/2/release?query="
private const val COVER_API_URL = "https://coverartarchive.org/release/"

fun getOrDefault(parse: () -> String): String {
    return try { parse() } catch (e: NullPointerException) { "" }
}

@Singleton
open class MusicBrainzClient @Inject constructor(private val httpClient: OkHttpClientImpl): ApiClient<MusicBrainzTag> {

    override suspend fun fetchTags(artist: String, title: String): MutableList<MusicBrainzTag> {
        val url = URL(
            MB_RECORDINGS + URLEncoder.encode(
                "\"%s\" AND artist:\"%s\"".format(title.luceneEscape(), artist.luceneEscape()),
                StandardCharsets.UTF_8.toString()
            ) + "?inc=tags+artist-credits+media"
        )
        val response = httpClient.getResponse(url)
        if (response != null) {
            return parseRecordings(response, "$artist - $title")
        }
        return mutableListOf()
    }

    override suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): MutableList<MusicBrainzTag> {
        throw IllegalStateException("Provider does not support fingerprint lookups")
    }

    override suspend fun fetchArtwork(artist: String, album: String, size: Int): String? {
        val url = URL(
            MB_RELEASES + URLEncoder.encode(
                "\"%s\" AND artist:\"%s\"".format(album.luceneEscape(), artist.luceneEscape()),
                StandardCharsets.UTF_8.toString()
            )
        )
        val response = httpClient.getResponse(url) ?: return null
        return getReleaseId(response, "$artist - $album")
    }

    override suspend fun fetchArtwork(tag: MusicBrainzTag) {
        fetchArtwork(tag, MUSICBRAINZ_ARTWORK_FORMATS[1])
    }

    override suspend fun fetchArtwork(tag: MusicBrainzTag, coverSize: Int) {
        tag.cover = fetchArtwork(tag.releaseId, coverSize)
    }

    private suspend fun fetchArtwork(releaseId: String, coverSize: Int): String? {
        val url = URL(
            COVER_API_URL + URLEncoder.encode(
                releaseId,
                StandardCharsets.UTF_8.toString()
            )
        )
        val response = httpClient.getResponse(url)
        if (response != null) {
            return parseArtworkURL(response, MUSICBRAINZ_ARTWORK_FORMATS.first { knownSize ->
                knownSize >= coverSize
            })
        }
        return null
    }

    private suspend fun getReleaseId(response: String, query: String): String? {
        return withContext(Dispatchers.Default) {

            val gson = Gson()
            val releaseList = mutableListOf<Pair<String, String>>()

            val jsonResponse = gson.fromJson(response, JsonObject::class.java)
            val releases = jsonResponse.getAsJsonArray("releases")

            for (r in releases) {

                val releaseEntry  = r.asJsonObject
                val releaseStatus = getOrDefault { releaseEntry["status"].asString }

                if (releaseStatus != "Official") {
                    continue
                }

                val releaseId     = releaseEntry["id"].asString
                val releaseTitle  = releaseEntry["title"].asString
                val releaseArtist = releaseEntry["artist-credit"]
                    ?.asJsonArray
                    ?.firstOrNull()
                    ?.asJsonObject
                    ?.get("name")
                    ?.asString ?: ""

                releaseList.add(
                    Pair(releaseId, "$releaseArtist - $releaseTitle")
                )
            }
            releaseList.sortBy {
                levenshtein(query, it.second)
            }
            releaseList.firstOrNull()?.first
        }
    }

    private suspend fun parseRecordings(response: String, query: String): MutableList<MusicBrainzTag> {
        return withContext(Dispatchers.Default) {

            val tags = mutableListOf<MusicBrainzTag>()
            val gson = Gson()

            val jsonResponse = gson.fromJson(response, JsonObject::class.java)
            val recordings   = jsonResponse.getAsJsonArray("recordings")

            outer@ for (rec in recordings) {

                val recEntry = rec.asJsonObject
                val releases = recEntry["releases"] ?: continue

                val title  = recEntry["title"].asString
                val artist = recEntry["artist-credit"]
                    .asJsonArray[0]
                    .asJsonObject["name"]
                    .asString

                var genre  = ""
                val genres = recEntry["tags"]

                if (genres != null) {
                    var count = 0
                    for (entry in genres.asJsonArray) {
                        entry.asJsonObject.let {
                            if (it["count"].asInt > count) {
                                count = it["count"].asInt
                                genre = it["name"].asString.replaceFirstChar { char -> char.uppercaseChar() }
                            }
                        }
                    }
                }
                for (release in releases.asJsonArray) {
                    try {

                        if (tags.size >= RESULTS_LIMIT) {
                            break@outer
                        }

                        val releaseEntry  = release.asJsonObject
                        val releaseStatus = getOrDefault { releaseEntry["status"].asString }

                        if (releaseStatus != "Official") {
                            continue
                        }

                        var releaseDate = getOrDefault { releaseEntry["date"].asString }
                        if (releaseDate.length > 4) {
                            releaseDate = releaseDate.substring(0..<4)
                        }

                        val releaseId     = releaseEntry["id"].asString
                        val releaseTitle  = releaseEntry["title"].asString
                        val releaseArtist = releaseEntry["artist-credit"]
                            ?.asJsonArray
                            ?.firstOrNull()
                            ?.asJsonObject
                            ?.get("name")
                            ?.asString ?: ""

                        val mediaEntry = releaseEntry["media"]
                            ?.asJsonArray
                            ?.firstOrNull()
                            ?.asJsonObject

                        val mediaFormat = mediaEntry?.get("format")?.asString   ?: ""
                        val discNumber  = mediaEntry?.get("position")?.asString ?: ""
                        val discCount   = releaseEntry["media"]
                            ?.asJsonArray
                            ?.size()
                            ?.toString() ?: ""

                        val trackCount  = getOrDefault { releaseEntry["track-count"].asString }
                        val trackNumber = mediaEntry?.get("track")
                            ?.asJsonArray
                            ?.firstOrNull()
                            ?.asJsonObject
                            ?.get("number")
                            ?.asString ?: ""

                        tags.add(MusicBrainzTag(
                            releaseId,
                            releaseArtist,
                            releaseTitle,
                            releaseDate,
                            artist,
                            title,
                            trackNumber,
                            trackCount,
                            discNumber,
                            discCount,
                            mediaFormat,
                            genre,
                            0
                        ))
                    } catch (e: NullPointerException) {
                        Log.d("MusicBrainzClient", "NPE for $query")
                    }
                }
            }
            tags.sortBy { tag ->
                levenshtein("${tag.artist} - ${tag.title}", query)
            }
            tags
        }
    }

    private fun parseArtworkURL(jsonString: String, coverSize: Int): String? {

        val gson   = Gson()
        val json   = gson.fromJson(jsonString, JsonObject::class.java)
        val images = json.getAsJsonArray("images") ?: return null

        if (images.isEmpty) {
            return null
        }

        var imageEntry = json.getAsJsonArray("images")[0].asJsonObject
        for (element in images) {

            val image      = element.asJsonObject
            val isFront    = image["front"].asBoolean
            val isApproved = image["approved"].asBoolean

            if (isFront && isApproved) {
                imageEntry = element.asJsonObject
                break
            }
        }

        val originalImageUrl = imageEntry["image"].asString
        var imageUrl = originalImageUrl
        val imageElement: JsonElement

        try {
            val thumbnails = imageEntry["thumbnails"].asJsonObject
            when (coverSize) {
                250 -> {
                    imageElement = thumbnails["250"]
                    imageUrl = if (imageElement == null) thumbnails["small"].asString else imageElement.asString
                }
                500 -> {
                    imageElement = thumbnails["500"]
                    imageUrl = if (imageElement == null) thumbnails["large"].asString else imageElement.asString
                }
                1200 -> {
                    imageUrl = thumbnails["1200"].asString
                }
            }
        } catch (e: NullPointerException) {
            imageUrl = originalImageUrl
            System.err.println("Failed to obtain image of specified format, falling back to original")
        }
        return imageUrl
    }
}