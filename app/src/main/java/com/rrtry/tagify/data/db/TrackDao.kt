package com.rrtry.tagify.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rrtry.tagify.data.entities.Album
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.data.entities.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Query("DELETE FROM Track WHERE trackId = :id")
    suspend fun deleteTrackById(id: Long)

    @Query("SELECT COUNT(*) FROM Track WHERE album = :album")
    suspend fun getTrackCountByAlbum(album: String): Int

    @Query("SELECT * FROM Track WHERE trackId = (:id)")
    suspend fun getTrackById(id: Long): Track?

    @Query("SELECT COUNT(*) FROM Track")
    suspend fun getTrackCount(): Int

    @Query("SELECT * FROM Track")
    suspend fun getTracks(): List<Track>

    @Query("SELECT * FROM Track " +
            "WHERE Track.artist = :artist " +
            "ORDER BY Track.album, Track.discPos, Track.trkPos"
    )
    suspend fun getArtistTracks(artist: String): List<Track>

    @Query("SELECT trackId FROM Track " +
            "WHERE Track.album = :album"
    )
    suspend fun getAlbumTrackIds(album: String): List<Long>

    @Query("SELECT * FROM Track " +
            "WHERE Track.album = :album " +
            "ORDER BY Track.discPos, Track.trkPos ASC"
    )
    suspend fun getAlbumTracks(album: String): List<Track>

    @Query("SELECT album FROM Track " +
            "WHERE artist = :artist " +
            "ORDER BY album ASC " +
            "LIMIT 1")
    suspend fun getFirstAlbum(artist: String): String

    @Query("SELECT * FROM Track")
    fun observeTracks(): Flow<List<Track>>

    @Query("SELECT Track.album AS title, artist, " +
            "COUNT(trackId) AS track_count, " +
            "COUNT(DISTINCT Track.artist) as artist_count " +
            "FROM Track GROUP BY Track.album"
    )
    fun observeAlbums(): Flow<List<Album>>

    @Query("SELECT Track.artist AS name, " +
            "COUNT(trackId) AS track_count, " +
            "COUNT(DISTINCT album) AS album_count " +
            "FROM Track GROUP BY Track.artist"
    )
    fun observeArtists(): Flow<List<Artist>>

}