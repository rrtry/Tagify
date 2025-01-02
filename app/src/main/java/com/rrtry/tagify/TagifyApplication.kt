package com.rrtry.tagify

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp

const val DATABASE_NAME = "tagify-db"

const val MEMORY_CACHE_THRESHOLD = 0.25
const val FETCHED_ARTWORK_CACHE_SIZE = 40 * 1024 * 1024L

const val RESTORED_FILES_DIR         = "Restored files"
const val FETCHED_ARTWORK_CACHE_DIR  = "fetched-artwork"
const val FINGERPRINTS_CACHE_DIR     = "fingerprints"

const val LOCAL_ARTWORK_FILES_DIR   = "local-artwork"
const val LOCAL_THUMBNAIL_FILES_DIR = "local-thumbnails"

const val CATEGORY_BATCH_OP                  = "com.rrtry.tagify.intent.category.CATEGORY_BATCH_OPERATION"
const val ACTION_SCAN_MEDIA                  = "com.rrtry.tagify.intent.action.ACTION_SCAN_MEDIA"
const val ACTION_LOOKUP_TAGS                 = "com.rrtry.tagify.intent.action.ACTION_LOOKUP_TAGS"
const val ACTION_BATCH_SAVE                  = "com.rrtry.tagify.intent.action.ACTION_BATCH_SAVE"
const val ACTION_LOAD_TAG                    = "com.rrtry.tagify.intent.action.ACTION_LOAD_TAG"
const val ACTION_SAVE_TAG                    = "com.rrtry.tagify.intent.action.ACTION_SAVE_TAG"
const val ACTION_BATCH_LOOKUP_TAGS           = "com.rrtry.tagify.intent.action.ACTION_BATCH_LOOKUP_TAGS"
const val ACTION_BATCH_REMOVE_TAGS           = "com.rrtry.tagify.intent.action.ACTION_BATCH_REMOVE_TAGS"
const val ACTION_BATCH_REMOVE_ARTWORK        = "com.rrtry.tagify.intent.action.ACTION_BATCH_REMOVE_ARTWORK"
const val ACTION_BATCH_TAG_FROM_FILENAME     = "com.rrtry.tagify.intent.action.ACTION_BATCH_TAG_FROM_FILENAME"
const val ACTION_BATCH_LOOKUP_ALBUM_ARTWORK  = "com.rrtry.tagify.intent.action.ACTION_BATCH_LOOKUP_ALBUM_ARTWORK"
const val ACTION_BATCH_SET_ARTWORK_FROM_FILE = "com.rrtry.tagify.intent.action.ACTION_BATCH_SET_ARTWORK_FROM_FILE"

@HiltAndroidApp
class TagifyApplication: Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .logger(DebugLogger())
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MEMORY_CACHE_THRESHOLD)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve(FETCHED_ARTWORK_CACHE_DIR))
                    .maxSizeBytes(FETCHED_ARTWORK_CACHE_SIZE)
                    .build()
            }
            .build()
    }
}