package com.rrtry.tagify.ui.onboarding

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrtry.tagify.R
import com.rrtry.tagify.data.api.OkHttpClientImpl
import com.rrtry.tagify.prefs.prefsGetApiKey
import com.rrtry.tagify.prefs.prefsSetApiKey
import com.rrtry.tagify.util.isGranted
import com.rrtry.tagify.util.permissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor
    (@ApplicationContext private val appCxt: Context): ViewModel()
{

    @Inject
    lateinit var httpClient: OkHttpClientImpl

    sealed class UIEvent {

        data object FinishSetup: UIEvent()
        data object ShowSettingsSnackbar: UIEvent()
        data class ShowSnackbar(
            @StringRes val stringRes: Int
        ): UIEvent()
    }

    private val eventChannel = Channel<UIEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()
    val prefsKey  = prefsGetApiKey(appCxt)

    val currentPage   = MutableStateFlow(0)
    val apiKey        = MutableStateFlow(prefsKey ?: "")
    val keyIsValid    = MutableStateFlow(prefsKey != null)
    val validatingKey = MutableStateFlow(false)

    fun verifyKey() {
        viewModelScope.launch {
            validatingKey.value = true
            withContext(Dispatchers.IO) {

                val fingerprint = try {
                    appCxt
                        .resources
                        .openRawResource(R.raw.test_fingerprint)
                        .reader().use {
                            it.readText()
                        }
                } catch (e: IOException) {
                    null
                }

                var success = false
                if (fingerprint != null) {
                    success = httpClient.getResponse(
                        URL("https://api.acoustid.org/v2/lookup?client=${apiKey.value}" +
                                "&duration=641&fingerprint=$fingerprint"
                        )
                    ) != null
                    if (success) {
                        keyIsValid.value = true
                        prefsSetApiKey(appCxt, apiKey.value)
                    }
                }
                if (!success) {
                    sendEvent(UIEvent.ShowSnackbar(R.string.failed_to_verify_key))
                }
                validatingKey.value = false
            }
        }
    }

    fun nextPage() {
        when (currentPage.value) {
            0 -> {
                currentPage.value += 1
            }
            1 -> {
                if (!permissions.any { !isGranted(appCxt, it) }) {
                    currentPage.value += 1
                } else {
                    sendEvent(UIEvent.ShowSnackbar(R.string.grant_required_permissions))
                }
            }
            2 -> {
                sendEvent(if (keyIsValid.value) {
                    UIEvent.FinishSetup
                } else {
                    UIEvent.ShowSnackbar(R.string.enter_valid_api_key)
                })
            }
        }
    }

    fun sendEvent(event: UIEvent) {
        viewModelScope.launch {
            eventChannel.send(event)
        }
    }
}