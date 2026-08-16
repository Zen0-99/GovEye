package com.goveye.app.data.api

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        while (attempt < MAX_RETRIES) {
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                lastException = e
                attempt++
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(BACKOFF_DELAYS[attempt - 1])
                }
            }
        }
        throw lastException ?: IOException("Retry exhausted with no exception")
    }

    companion object {
        private const val MAX_RETRIES = 3
        private val BACKOFF_DELAYS = longArrayOf(1000L, 2000L, 4000L)
    }
}
