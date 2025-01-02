package com.rrtry.tagify.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rrtry.tagify.data.RESULTS_LIMIT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

private const val API_BASE_URL = "https://api.acoustid.org/v2/lookup?"

@Singleton
class AcoustIDClient @Inject constructor(private val httpClient: OkHttpClientImpl): MusicBrainzClient(httpClient) {

    override suspend fun fetchTags(artist: String, title: String): MutableList<MusicBrainzTag> {
        throw IllegalStateException()
    }

    override suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): MutableList<MusicBrainzTag> {
        val url = URL(
            String.format(
                API_BASE_URL + "client=%s&duration=%d&meta=tracks+sources+recordings+releases&fingerprint=%s&limit=5",
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString()),
                length,
                URLEncoder.encode(fingerprint, StandardCharsets.UTF_8.toString())
            )
        )
        val response = httpClient.getResponse(url) ?: return mutableListOf()
        return withContext(Dispatchers.Default) {

            val gson    = Gson()
            val json    = gson.fromJson(response, JsonObject::class.java)
            val results = json.getAsJsonArray("results")
            val entries = ArrayList<MusicBrainzTag>()

            outer@ for (result in results) {

                val resultObj  = result.asJsonObject
                val recordings = resultObj["recordings"] ?: continue

                for (r in recordings.asJsonArray) {

                    val recording = r.asJsonObject
                    val recTitle  = getOrDefault { recording["title"].asString }
                    val releases  = recording["releases"]?.asJsonArray ?: continue
                    val sources   = recording["sources"]?.asInt ?: 0
                    val recArtist = recording["artists"]
                        ?.asJsonArray
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.get("name")
                        ?.asString ?: ""

                    for (release in releases) {

                        if (entries.size >= RESULTS_LIMIT) {
                            break@outer
                        }

                        val releaseObj   = release.asJsonObject
                        val releaseId    = releaseObj["id"].asString
                        val releaseTitle = releaseObj["title"].asString
                        val releaseYear  = getOrDefault {
                            releaseObj["date"]
                                .asJsonObject["year"]
                                .asString
                        }

                        val releaseArtist = releaseObj["artists"]
                            ?.asJsonArray
                            ?.firstOrNull()
                            ?.asJsonObject
                            ?.get("name")
                            ?.asString ?: ""

                        val mediumEntry = releaseObj["mediums"]
                            ?.asJsonArray
                            ?.firstOrNull()
                            ?.asJsonObject

                        val mediaFormat = mediumEntry?.get("format")?.asString   ?: ""
                        val discCount   = releaseObj["medium_count"]?.asString   ?: ""
                        val discNumber  = mediumEntry?.get("position")?.asString ?: ""

                        val trackCount  = mediumEntry?.get("track_count")?.asString ?: ""
                        val trackNumber = mediumEntry
                            ?.getAsJsonArray("tracks")
                            ?.firstOrNull()
                            ?.asJsonObject
                            ?.get("position")
                            ?.asString ?: ""

                        entries.add(
                            MusicBrainzTag(
                                releaseId,
                                releaseArtist,
                                releaseTitle,
                                releaseYear,
                                recArtist,
                                recTitle,
                                trackNumber,
                                trackCount,
                                discNumber,
                                discCount,
                                mediaFormat,
                                "",
                                sources
                            )
                        )
                    }
                }
            }
            entries.sortByDescending { tag ->
                tag.sources
            }
            entries
        }
    }
}