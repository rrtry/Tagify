package com.rrtry.tagify.ui.details

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.rrtry.tagify.R
import com.rrtry.tagify.data.MediaRepository
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.ui.home.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val app: Application,
    private val mediaRepository: MediaRepository): BaseViewModel()
{
    data class UIState(
        val artist: String = "",
        val tracks: List<Track>? = null,
        val year: Int? = null,
        val genre: String? = null
    )

    val uiState = MutableStateFlow(UIState())
    var singleDisc = false
        private set

    fun loadAlbumTracks(album: String) {
        viewModelScope.launch {
            mediaRepository.getAlbumTracks(album).let {

                val tracks = it
                val distinctByArtist  = it.distinctBy { track -> track.artist  }
                val distinctByDiscNum = it.distinctBy { track -> track.discPos }
                val distinctByGenre   = it.distinctBy { track -> track.genre   }.sortedBy { track -> track.genre.length }
                val distinctByYear    = it.distinctBy { track -> track.year    }.sortedBy { track -> track.year }

                val showYear = (distinctByYear.size == 1 && distinctByYear[0].year != 0) ||
                                distinctByYear.size == 2
                val showGenre = (distinctByGenre.size == 1 && distinctByGenre[0].genre.isNotBlank()) ||
                                distinctByGenre.size == 2

                singleDisc = distinctByDiscNum.size == 1
                val artist = if (distinctByArtist.size != 1) app.getString(R.string.various_artists) else distinctByArtist[0].artist

                var viewState = UIState()
                if (showYear) {
                    viewState = viewState.copy(year = distinctByYear.last().year)
                }
                if (showGenre) {
                    viewState = viewState.copy(genre = distinctByGenre.last().genre)
                }
                uiState.value = viewState.copy(
                    tracks = tracks,
                    artist = artist
                )
            }
        }
    }

    override fun getTracks(): List<Track> {
        return uiState.value.tracks!!
    }
}