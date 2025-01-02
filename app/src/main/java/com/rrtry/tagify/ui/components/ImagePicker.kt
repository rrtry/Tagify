package com.rrtry.tagify.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.rrtry.tagify.ACTION_BATCH_SET_ARTWORK_FROM_FILE
import com.rrtry.tagify.service.TagWriterService
import com.rrtry.tagify.ui.home.BaseViewModel

private const val MIME_TYPE_PNG = "image/png"
private const val MIME_TYPE_JPG = "image/jpeg"

class ImagePickerLauncher {

    var launcher: ManagedActivityResultLauncher<Any, Any>? = null

    @Suppress("unchecked_cast")
    fun launch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (launcher as? ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>)?.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            (launcher as? ManagedActivityResultLauncher<Array<String>, Uri?>)?.launch(
                arrayOf(
                    MIME_TYPE_PNG,
                    MIME_TYPE_JPG
                )
            )
        }
    }
}

@Composable
fun ImagePicker(
    launcher:  ImagePickerLauncher,
    viewModel: BaseViewModel)
{
    ImagePicker(launcher) { uri ->
        if (uri != null) {
            viewModel.startBatchService(
                ACTION_BATCH_SET_ARTWORK_FROM_FILE,
                Bundle().apply { putString(TagWriterService.EXTRA_ARTWORK_URI, uri.toString()) }
            )
        }
    }
}

@Suppress("unchecked_cast")
@Composable
fun ImagePicker(
    imagePickerLauncher: ImagePickerLauncher,
    onResult: (uri: Uri?) -> Unit)
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        imagePickerLauncher.launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            onResult(uri)
        } as ManagedActivityResultLauncher<Any, Any>
    } else {
        imagePickerLauncher.launcher = rememberLauncherForActivityResult(contract = object : ActivityResultContracts.OpenDocument() {

            override fun createIntent(context: Context, input: Array<String>): Intent {
                return super.createIntent(context, input).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            }})
        { uri ->
            onResult(uri)
        } as ManagedActivityResultLauncher<Any, Any>
    }
}