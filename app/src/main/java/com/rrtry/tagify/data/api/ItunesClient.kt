package com.rrtry.tagify.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rrtry.tagify.data.RESULTS_LIMIT
import com.rrtry.tagify.util.levenshtein
import com.rrtry.tagify.util.safeSubstring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.String.join
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import javax.inject.Inject
import javax.inject.Singleton

private const val API_BASE_URL = "https://itunes.apple.com/search?"

@Singleton
class ItunesClient @Inject constructor(private val httpClient: OkHttpClientImpl): ApiClient<ItunesTag> {

    override suspend fun fetchTags(artist: String, title: String): MutableList<ItunesTag> {
        val query = String.format("term=%s-%s&media=music&entity=song",
            URLEncoder.encode(artist, UTF_8.toString()),
            URLEncoder.encode(title, UTF_8.toString())
        )
        val response = httpClient.getResponse(URL(API_BASE_URL + query))
        return if (response != null) parseSongs(artist, title, response) else mutableListOf()
    }

    override suspend fun fetchTags(apiKey: String, fingerprint: String, length: Int): MutableList<ItunesTag> {
        throw IllegalStateException("Client does not support fingerprint lookups")
    }

    override suspend fun fetchArtwork(tag: ItunesTag) {
        fetchArtwork(tag, 600)
    }

    override suspend fun fetchArtwork(artist: String, album: String, size: Int): String? {
        val query = String.format("term=%s-%s&media=music&entity=album",
            URLEncoder.encode(artist, UTF_8.toString()),
            URLEncoder.encode(album, UTF_8.toString())
        )
        val response = httpClient.getResponse(URL(API_BASE_URL + query)) ?: return null
        return getAlbumArtwork(artist, album, response, size)
    }

    override suspend fun fetchArtwork(tag: ItunesTag, coverSize: Int) {
        tag.cover = getArtworkURL(tag.cover!!, coverSize)
    }

    private fun getArtworkURL(url: String, coverSize: Int): String {

        var artworkURL = url
        val knownSize  = ITUNES_ARTWORK_FORMATS.first { knownSize ->
            knownSize >= coverSize
        }

        if (coverSize != 100) {
            val parts = artworkURL.split("/".toRegex()).toTypedArray()
            parts[parts.size - 1] = String.format("%dx%dbb.jpg", knownSize, knownSize)
            artworkURL = join("/", *parts)
        }
        return artworkURL
    }

    private suspend fun getAlbumArtwork(artist: String, album: String, response: String, size: Int): String? {
        return withContext(Dispatchers.Default) {

            val gson    = Gson()
            val query   = "$artist - $album"
            val json    = gson.fromJson(response, JsonObject::class.java)
            val results = json.getAsJsonArray("results")
            val albums  = ArrayList<Pair<String, String>>()

            for (result in results) {

                val albumEntry = result.asJsonObject
                val albumName  = getOrDefault { albumEntry["collectionName"].asString }
                val artistName = getOrDefault { albumEntry["artistName"].asString     }
                val artworkURL = getOrDefault { albumEntry["artworkUrl100"].asString  }

                if (artworkURL.isEmpty()) {
                    continue
                }
                albums.add(Pair(getArtworkURL(artworkURL, size), "$artistName - $albumName"))
            }
            albums.sortBy {
                levenshtein(query, it.second)
            }
            albums.firstOrNull()?.first
        }
    }

    private suspend fun parseSongs(artistQuery: String, titleQuery: String, response: String): MutableList<ItunesTag> {
        return withContext(Dispatchers.Default) {

            val gson     = Gson()
            val query    = "$artistQuery - $titleQuery"
            val json     = gson.fromJson(response, JsonObject::class.java)
            val elements = json.getAsJsonArray("results")
            val entries  = ArrayList<ItunesTag>()

            for (element in elements) {

                if (entries.size >= RESULTS_LIMIT) {
                    break
                }

                val entry       = element.asJsonObject
                val title       = getOrDefault { entry["trackName"].asString        }
                val albumName   = getOrDefault { entry["collectionName"].asString   }
                val artistName  = getOrDefault { entry["artistName"].asString       }
                val trackNumber = getOrDefault { entry["trackNumber"].asString      }
                val trackCount  = getOrDefault { entry["trackCount"].asString       }
                val discNumber  = getOrDefault { entry["discNumber"].asString       }
                val discCount   = getOrDefault { entry["discCount"].asString        }
                val genre       = getOrDefault { entry["primaryGenreName"].asString }
                val country     = getOrDefault { entry["country"].asString          }
                val artworkURL  = getOrDefault { entry["artworkUrl100"].asString    }
                val releaseDate = getOrDefault {
                    val timestamp = entry["releaseDate"].asString
                    if (timestamp.isNotEmpty()) {
                        timestamp.safeSubstring(0, timestamp.length - 1)
                    } else {
                        timestamp
                    }
                }
                entries.add(
                    ItunesTag(
                        title,
                        releaseDate,
                        albumName,
                        artistName,
                        trackNumber,
                        trackCount,
                        discNumber,
                        discCount,
                        genre,
                        country,
                        artworkURL
                    )
                )
            }
            entries.sortBy {
                levenshtein(query, "${it.artist} - ${it.title}")
            }
            entries
        }
    }
}