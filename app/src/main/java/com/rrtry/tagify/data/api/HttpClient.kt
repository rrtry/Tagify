package com.rrtry.tagify.data.api

import java.net.URL

interface HttpClient {

    suspend fun getResponse(url: URL): String?
}