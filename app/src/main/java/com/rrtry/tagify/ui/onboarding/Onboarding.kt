package com.rrtry.tagify.ui.onboarding

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rrtry.tagify.R
import com.rrtry.tagify.SCREEN_HOME
import com.rrtry.tagify.SCREEN_ONBOARDING
import com.rrtry.tagify.ui.components.TextFieldDialog
import com.rrtry.tagify.ui.components.UIEventHandler
import com.rrtry.tagify.ui.onboarding.OnboardingViewModel.UIEvent.FinishSetup
import com.rrtry.tagify.ui.onboarding.OnboardingViewModel.UIEvent.ShowSettingsSnackbar
import com.rrtry.tagify.ui.onboarding.OnboardingViewModel.UIEvent.ShowSnackbar
import com.rrtry.tagify.util.isGranted
import com.rrtry.tagify.util.isPermanentlyDenied
import com.rrtry.tagify.util.openApplicationSettings
import com.rrtry.tagify.util.permissions
import kotlinx.coroutines.launch

private const val ANNOTATION_TAG = "URL"
private const val ANNOTATION_URL = "https://acoustid.org/new-application"

@Composable
fun adaptiveIconPainterResource(@DrawableRes id: Int): Painter {

    val res   = LocalContext.current.resources
    val theme = LocalContext.current.theme

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val adaptiveIcon = ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
        if (adaptiveIcon != null) {
            BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
        } else {
            painterResource(id)
        }
    } else {
        painterResource(id)
    }
}

@Composable
fun PageIndicator(modifier: Modifier = Modifier, currentPage: Int) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween)
    {
        for (i in 0..<3) {
            val animatedScale by animateFloatAsState(
                targetValue = if (i == currentPage) 0.8f else 0.5f,
                label = "indicator_scale"
            )
            Box(
                modifier = Modifier
                    .animateContentSize(tween(100))
                    .graphicsLayer {
                        val scale = animatedScale
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun Tile(
    modifier: Modifier,
    iconRes:  Int,
    title:    String,
    subtitle: AnnotatedString)
{
    val context = LocalContext.current
    Row(modifier = modifier,
        verticalAlignment = Alignment.CenterVertically)
    {
        Icon(
            modifier = Modifier.size(32.dp),
            painter  = painterResource(id = iconRes),
            contentDescription = null
        )
        Column(modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxWidth(0.9f))
        {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleMedium
            )
            ClickableText(
                style   = MaterialTheme.typography.bodySmall,
                text    = subtitle,
                onClick = {
                    subtitle
                        .getStringAnnotations(ANNOTATION_TAG, it, it)
                        .firstOrNull()?.let { stringAnnotation ->
                            Intent(ACTION_VIEW).apply {
                                data = Uri.parse(stringAnnotation.item)
                                context.startActivity(this)
                            }
                        }
                }
            )
        }
    }
}

@Composable
fun Tile(
    modifier: Modifier,
    iconRes:  Int,
    title:    String,
    subtitle: String)
{
    Row(modifier = modifier,
        verticalAlignment = Alignment.CenterVertically)
    {
        Icon(
            modifier = Modifier.size(32.dp),
            painter  = painterResource(id = iconRes),
            contentDescription = null
        )
        Column(modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxWidth(0.9f)) {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleMedium
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AudioIdentificationScreen(viewModel: OnboardingViewModel) {

    var showDialog by rememberSaveable { mutableStateOf(false) }
    val keyIsValid by viewModel.keyIsValid.collectAsState()
    val validating by viewModel.validatingKey.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start)
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .size(24.dp),
                painter = painterResource(id = R.drawable.baseline_fingerprint_24),
                contentDescription = null
            )
            Text(modifier  = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                text       = stringResource(id = R.string.audio_identification),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Tile(
            Modifier.padding(start = 16.dp, end = 16.dp),
            R.drawable.baseline_vpn_key_24,
            stringResource(id = R.string.get_api_key),
            buildAnnotatedString {

                val primaryStyle = SpanStyle(color = MaterialTheme.colorScheme.primary)
                val onBackground = SpanStyle(color = MaterialTheme.colorScheme.onBackground)

                withStyle(style = onBackground) { append(stringResource(id = R.string.visit) + " ") }
                pushStringAnnotation(
                    ANNOTATION_TAG,
                    ANNOTATION_URL
                )
                withStyle(style = primaryStyle) { append("AcoustID") }
                pop()
                withStyle(style = onBackground) { append(" " + stringResource(id = R.string.register_app)) }
            }
        )
        Button(modifier = Modifier
            .align(Alignment.End)
            .padding(16.dp),
            onClick = { if (!validating && !keyIsValid) showDialog = true })
        {
            if (validating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.inversePrimary,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(24.dp)
                            .padding(top = 4.dp, end = 8.dp)
                    )
                    Text(text = stringResource(id = R.string.verifying))
                }
            } else {
                Text(text = stringResource(id = if (!keyIsValid) R.string.enter_api_key else R.string.key_is_valid))
            }
        }
        if (showDialog) {
            TextFieldDialog(
                R.string.enter_api_key,
                R.string.api_key,
                R.string.cannot_be_empty,
                KeyboardOptions(keyboardType = KeyboardType.Text),
                viewModel.apiKey,
                {
                    showDialog = false
                    if (it) {
                        viewModel.verifyKey()
                    }
                },
                { it })
            {
                it.isNotEmpty()
            }
        }
    }
}

@Composable
fun IntroductionScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Image(
            painter = adaptiveIconPainterResource(R.mipmap.ic_launcher),
            contentDescription = null
        )
        Text(
            modifier   = Modifier.padding(16.dp),
            text       = stringResource(id = R.string.app_welcome),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier  = Modifier.padding(start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center,
            text      = stringResource(id = R.string.app_description),
            style     = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun Permissions() {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            Tile(
                Modifier.padding(start = 16.dp, end = 16.dp),
                R.drawable.folder_music,
                stringResource(id = R.string.permission_read_audio),
                stringResource(id = R.string.permission_read_audio_desc)
            )
            Tile(
                Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                R.drawable.baseline_circle_notifications_24,
                stringResource(id = R.string.permission_post_notifications),
                stringResource(id = R.string.permission_post_notifications_desc)
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            Tile(
                Modifier.padding(start = 16.dp, end = 16.dp),
                R.drawable.folder_music,
                stringResource(id = R.string.permission_read_external_storage),
                stringResource(id = R.string.permission_read_external_storage_desc)
            )
        }
        else -> {
            Tile(
                Modifier.padding(start = 16.dp, end = 16.dp),
                R.drawable.folder_music,
                stringResource(id = R.string.permission_read_external_storage),
                stringResource(id = R.string.permission_read_external_storage_desc)
            )
            Tile(
                Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                R.drawable.folder_music,
                stringResource(id = R.string.permission_write_external_storage),
                stringResource(id = R.string.permission_write_external_storage_desc)
            )
        }
    }
}

@Composable
fun PermissionsScreen(viewModel: OnboardingViewModel) {

    val context  = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
    { permissionMap: Map<String, Boolean> ->
        when {
            permissionMap.keys.all { isGranted(context, it) } -> {
                viewModel.nextPage()
            }
            permissionMap.keys.any { isPermanentlyDenied(context, it) } -> {
                viewModel.sendEvent(ShowSettingsSnackbar)
            }
            else -> {
                viewModel.sendEvent(ShowSnackbar(R.string.permission_rationale))
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 90.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start)
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .size(24.dp),
                painter = painterResource(id = R.drawable.baseline_shield_24),
                contentDescription = null
            )
            Text(modifier = Modifier.padding(top = 16.dp, end = 16.dp, bottom = 16.dp),
                text = stringResource(id = R.string.permissions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Permissions()
        Button(modifier = Modifier
            .padding(16.dp)
            .align(Alignment.End),
            onClick = {
                launcher.launch(permissions)
            })
        {
            Text(stringResource(id = R.string.grant_permission))
        }
    }
}

@Composable
fun OnboardingScreen(navController: NavHostController, viewModel: OnboardingViewModel = hiltViewModel()) {

    val localContext      = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val composeScope      = rememberCoroutineScope()

    val currentPage by viewModel.currentPage.collectAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (currentPage == 0) 0f else 1f,
        label = "button_scale"
    )

    UIEventHandler(eventFlow = viewModel.eventFlow) { event ->
        when (event) {
            FinishSetup -> {
                navController.navigate(SCREEN_HOME) {
                    popUpTo(SCREEN_ONBOARDING) {
                        inclusive = true
                    }
                }
            }
            ShowSettingsSnackbar -> {
                composeScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        localContext.resources.getString(R.string.permission_in_settings),
                        localContext.resources.getString(R.string.open_settings),
                        false,
                        SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        localContext.openApplicationSettings()
                    }
                }
            }
            is ShowSnackbar -> {
                composeScope.launch {
                    snackbarHostState.showSnackbar(
                        localContext.resources.getString(event.stringRes)
                    )
                }
            }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { contentPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding))
        {
            AnimatedContent(
                label = "page_transition",
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                })
            {
                when (it) {
                    0 -> IntroductionScreen()
                    1 -> PermissionsScreen(viewModel)
                    2 -> AudioIdentificationScreen(viewModel)
                }
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween)
            {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .scale(buttonScale),
                    onClick  = { if (currentPage != 0) viewModel.currentPage.value -= 1 })
                {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                PageIndicator(currentPage = currentPage)
                FloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    onClick  = { viewModel.nextPage() })
                {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}