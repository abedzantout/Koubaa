package com.kobaatv.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin async wrapper around XtreamClient. Moves the blocking OkHttp calls off
 * the main thread and returns Result so callers (ViewModels) handle success and
 * failure explicitly instead of catching exceptions inline.
 */
class XtreamRepository(
    private val host: String,
    private val username: String,
    private val password: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val client = XtreamClient(host, username, password)

    suspend fun login(): Result<Unit> = runOnIo {
        client.login()
        Unit
    }

    suspend fun liveStreams(categoryId: String? = null): Result<List<XtreamClient.Channel>> = runOnIo {
        client.liveStreams(categoryId)
    }

    fun hlsUrl(streamId: Int): String = client.hlsUrl(streamId)

    private suspend fun <T> runOnIo(block: () -> T): Result<T> =
        withContext(io) { runCatching(block) }
}
