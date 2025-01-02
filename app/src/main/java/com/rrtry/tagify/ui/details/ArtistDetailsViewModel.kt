package com.rrtry.tagify.ui.details

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.rrtry.tagify.data.MediaRepository
import com.rrtry.tagify.data.entities.Artist
import com.rrtry.tagify.data.entities.Track
import com.rrtry.tagify.ui.home.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
): BaseViewModel() {

    data class UIState(
        val tracks: List<Track>? = null,
        val trackCount: Int = 0,
        val albumCount: Int = 0
    )

    val uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState())

    fun loadArtistTracks(artist: Artist) {
        viewModelScope.launch {
            mediaRepository.getArtistTracks(artist.name!!).let {
                uiState.value = UIState(
                    it, it.size, it.distinctBy { t -> t.album }.size
                )
            }
        }
    }

    override fun getTracks(): List<Track> {
        return uiState.value.tracks!!
    }
}