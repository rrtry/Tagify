package com.rrtry.tagify

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rrtry.tagify.prefs.PreferencesScreen
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.ui.details.AlbumsDetails
import com.rrtry.tagify.ui.details.ArtistDetails
import com.rrtry.tagify.ui.details.LookupDetails
import com.rrtry.tagify.ui.details.TrackDetails
import com.rrtry.tagify.ui.home.HomeScreen
import com.rrtry.tagify.ui.onboarding.OnboardingScreen
import com.rrtry.tagify.util.isGranted
import com.rrtry.tagify.util.permissions

const val SCREEN_HOME           = "screen_home"
const val SCREEN_ONBOARDING     = "screen_onboarding"
const val SCREEN_PREFERENCES    = "screen_preferences"
const val SCREEN_TRACK_DETAILS  = "screen_track_details"
const val SCREEN_ALBUM_DETAILS  = "screen_album_details"
const val SCREEN_ARTIST_DETAILS = "screen_artist_details"
const val SCREEN_LOOKUP_DETAILS = "screen_lookup_details"

const val NAV_ARG_URIS   = "nav_arg_uris"
const val NAV_ARG_TRACK  = "nav_arg_track"
const val NAV_ARG_ALBUM  = "nav_arg_album"
const val NAV_ARG_ARTIST = "nav_arg_artist"

fun NavController.navigate(
    route: String,
    args: Bundle,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null)
{
    val nodeId = graph.findNode(route = route)?.id
    if (nodeId != null) {
        navigate(nodeId, args, navOptions, navigatorExtras)
    }
}

@Composable
fun TagifyApp(serviceBus: ServiceEventBus) {

    val navController = rememberNavController()
    val context       = LocalContext.current
    val startDest     = if (permissions.all { isGranted(context, it) }) SCREEN_HOME else SCREEN_ONBOARDING

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController, startDestination = startDest) {
            composable(route = SCREEN_HOME) {
                HomeScreen(navController, serviceBus)
            }
            composable(route = SCREEN_ONBOARDING) {
                OnboardingScreen(navController)
            }
            composable(route = SCREEN_PREFERENCES) {
                PreferencesScreen(navController)
            }
            composable(route = SCREEN_LOOKUP_DETAILS) {
                LookupDetails(
                    navController,
                    serviceBus
                )
            }
            composable(route = SCREEN_TRACK_DETAILS) {entry ->
                TrackDetails(
                    navController,
                    serviceBus,
                    requireNotNull(entry.arguments?.getParcelable(NAV_ARG_TRACK))
                )
            }
            composable(route = SCREEN_ALBUM_DETAILS) { entry ->
                AlbumsDetails(
                    navController,
                    requireNotNull(entry.arguments?.getParcelable(NAV_ARG_ALBUM)),
                    serviceBus
                )
            }
            composable(route = SCREEN_ARTIST_DETAILS) {entry ->
                ArtistDetails(
                    navController,
                    requireNotNull(entry.arguments?.getParcelable(NAV_ARG_ARTIST)),
                    serviceBus
                )
            }
        }
    }
}