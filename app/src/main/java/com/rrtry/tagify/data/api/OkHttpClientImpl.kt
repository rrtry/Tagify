package com.rrtry.tagify.data.api

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.URL
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

private const val USER_AGENT      = "com.rrtry.Tagify/1.0 (fedormocalov36@gmail.com)"
private const val HTTP_CACHE_DIR  = "http_cache"
private const val CALL_TIMEOUT    = 30 * 1000L
private const val HTTP_CACHE_SIZE = 1024 * 1024 * 100L

@Singleton
class OkHttpClientImpl @Inject constructor(@ApplicationContext private val context: Context): HttpClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofMillis(CALL_TIMEOUT))
        .cache(Cache(context.cacheDir.resolve(HTTP_CACHE_DIR), HTTP_CACHE_SIZE))
        .build()

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine {
            enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    if (!it.isCancelled) {
                        it.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    it.resume(response)
                }
            })
            it.invokeOnCancellation {
                cancel()
            }
        }
    }

    override suspend fun getResponse(url: URL): String? {
        var response: Response? = null
        return try {

            val request = Request.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .url(url)
                .build()

            val call = client.newCall(request)
            response = call.await()

            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: IOException) {
            null
        } finally {
            response?.closeQuietly()
        }
    }
}