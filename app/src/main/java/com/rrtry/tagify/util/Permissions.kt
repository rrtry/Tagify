package com.rrtry.tagify.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission

val permissions = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    else -> {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

fun Context.openApplicationSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    )
}

fun Context.checkUriPermission(uri: Uri): Boolean {
    return checkUriPermission(
        uri, Process.myPid(),
        Process.myUid(),
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.createWriteRequest(uris: Collection<Uri>,
                               writeRequestLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
                               onGranted: () -> Unit)
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!uris.all { uri -> checkUriPermission(uri) }) {
            MediaStore.createWriteRequest(contentResolver, uris).let {
                writeRequestLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build())
            }
        } else {
            onGranted()
        }
    } else {
        onGranted()
    }
}

fun isGranted(context: Context, permission: String): Boolean {
    return checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun isPermanentlyDenied(context: Context, permission: String): Boolean {
    return !isGranted(context, permission) &&
            !shouldShowRequestPermissionRationale(context as Activity, permission)
}