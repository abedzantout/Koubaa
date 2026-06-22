package com.kobaatv.app

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Single shared OkHttpClient for the whole app, so its connection pool and
 * dispatcher threads are reused instead of being recreated per request.
 */
object Network {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}
