package com.rrtry.tagify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.rrtry.tagify.ACTION_BATCH_REMOVE_ARTWORK
import com.rrtry.tagify.ACTION_BATCH_REMOVE_TAGS
import com.rrtry.tagify.ACTION_BATCH_SET_ARTWORK_FROM_FILE
import com.rrtry.tagify.ACTION_BATCH_LOOKUP_TAGS
import com.rrtry.tagify.ACTION_BATCH_SAVE
import com.rrtry.tagify.ACTION_BATCH_TAG_FROM_FILENAME
import com.rrtry.tagify.ACTION_SCAN_MEDIA
import com.rrtry.tagify.R

@RequiresApi(Build.VERSION_CODES.O)
private fun createNotificationChannel(
    context: Context,
    channelId: String,
    @StringRes chName: Int,
    @StringRes chDesc: Int,
    importance: Int)
{
    val name = context.getString(chName)
    val desc = context.getString(chDesc)
    val mChannel = NotificationChannel(channelId, name, importance)
    mChannel.description = desc

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(mChannel)
}

fun getActionDisplayNameRes(action: String?): Int {
    return when (action) {
        ACTION_SCAN_MEDIA                  -> R.string.scanning_music
        ACTION_BATCH_SAVE                  -> R.string.batch_operation_save_tags
        ACTION_BATCH_LOOKUP_TAGS                -> R.string.batch_operation_lookup_tags
        ACTION_BATCH_REMOVE_TAGS            -> R.string.batch_operation_remove_tags
        ACTION_BATCH_REMOVE_ARTWORK        -> R.string.batch_operation_remove_artwork
        ACTION_BATCH_SET_ARTWORK_FROM_FILE -> R.string.batch_operation_set_artwork_from_file
        ACTION_BATCH_TAG_FROM_FILENAME     -> R.string.batch_operation_tag_from_filename
        else -> R.string.unknown
    }
}

fun createBatchWriterServiceNotification(
    context: Context,
    channelId: String,
    action: String,
    progress: Int,
    maxProgress: Int): Notification
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            channelId,
            R.string.writer_service_channel,
            R.string.writer_service_description,
            IMPORTANCE_LOW
        )
    }
    return NotificationCompat.Builder(context, channelId)
        .setOngoing(true)
        .setProgress(maxProgress, progress, false)
        .setContentTitle(context.getString(getActionDisplayNameRes(action)))
        .setSmallIcon(R.drawable.file_music)
        .setSubText("$progress/$maxProgress ${context.getString(R.string.files)}")
        .build()
}

fun createWriterServiceNotification(context: Context, channelId: String): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            channelId,
            R.string.writer_service_channel,
            R.string.writer_service_description,
            IMPORTANCE_LOW
        )
    }
    return NotificationCompat.Builder(context, channelId)
        .setOngoing(true)
        .setContentTitle(context.getString(R.string.writer_service_channel))
        .setSmallIcon(R.drawable.file_music)
        .setContentText(context.getString(R.string.writer_service_on_start))
        .build()
}

fun createWriterServiceNotification(context: Context, channelId: String, path: String): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            channelId,
            R.string.writer_service_channel,
            R.string.writer_service_description,
            IMPORTANCE_LOW
        )
    }
    return NotificationCompat.Builder(context, channelId)
        .setOngoing(true)
        .setContentTitle(context.getString(R.string.writer_service_channel))
        .setSmallIcon(R.drawable.file_music)
        .setContentText("${context.getString(R.string.writing_tag_to)} $path")
        .build()
}

fun createScanServiceNotification(
    context: Context,
    channelId: String,
    progress: Int?,
    maxProgress: Int?): Notification
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationChannel(
            context,
            channelId,
            R.string.scan_service_channel,
            R.string.scan_service_description,
            IMPORTANCE_LOW
        )
    }
    return if (maxProgress != null && progress != null) {
        NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setProgress(maxProgress, progress, false)
            .setContentTitle(context.getString(R.string.scan_service_channel))
            .setSmallIcon(R.drawable.file_music)
            .setContentText("$progress/$maxProgress ${context.getString(R.string.files)}")
            .build()
    } else {
        NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setContentTitle(context.getString(R.string.scan_service_channel))
            .setSmallIcon(R.drawable.file_music)
            .setContentText(context.getString(R.string.scan_service_on_start))
            .build()
    }
}