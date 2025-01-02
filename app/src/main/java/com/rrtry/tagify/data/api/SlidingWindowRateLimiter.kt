package com.rrtry.tagify.data.api

import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowSizeInMillis: Long)
{
    private val clientTimestamps: Deque<Long> = ConcurrentLinkedDeque()

    suspend fun <T> makeRequest(request: suspend () -> T): T? {

        val currentTimeMillis = System.currentTimeMillis()
        while (!clientTimestamps.isEmpty() && currentTimeMillis - clientTimestamps.peekFirst()!! > windowSizeInMillis) {
            clientTimestamps.pollFirst()
        }

        if (clientTimestamps.size < maxRequests) {
            clientTimestamps.addLast(currentTimeMillis)
            return request()
        }
        return null
    }
}





