package com.rrtry.tagify.data

import com.rrtry.tagify.data.db.TrackDao
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(private val trackDao: TrackDao) {

    suspend fun insertTrack(track: Track) {
        withContext(Dispatchers.IO) {
            trackDao.insertTrack(track)
        }
    }

    suspend fun deleteTrackById(id: Long) {
        withContext(Dispatchers.IO) {
            trackDao.deleteTrackById(id)
        }
    }

    suspend fun getTrackById(id: Long): Track? {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackById(id)
        }
    }

    suspend fun getTrackCountByAlbum(album: String): Int {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackCountByAlbum(album)
        }
    }

    suspend fun getTrackCount(): Int {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackCount()
        }
    }

    suspend fun getTracks(): List<Track> {
        return withContext(Dispatchers.IO) {
            trackDao.getTracks()
        }
    }

    suspend fun getAlbumTrackIds(album: String): List<Long> {
        return withContext(Dispatchers.IO) {
            trackDao.getAlbumTrackIds(album)
        }
    }

    suspend fun getAlbumTracks(album: String): List<Track> {
        return withContext(Dispatchers.IO) {
            trackDao.getAlbumTracks(album)
        }
    }

    suspend fun getFirstAlbum(artist: String): String {
        return withContext(Dispatchers.IO) {
            trackDao.getFirstAlbum(artist)
        }
    }

    suspend fun getArtistTracks(artist: String): List<Track> {
        return withContext(Dispatchers.IO) {
            trackDao.getArtistTracks(artist)
        }
    }

    fun observeTracks(): Flow<List<Track>> {
        return trackDao.observeTracks()
    }

    fun observeAlbums(): Flow<List<Album>> {
        return trackDao.observeAlbums()
    }

    fun observeArtists(): Flow<List<Artist>> {
        return trackDao.observeArtists()
    }
}