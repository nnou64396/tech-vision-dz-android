package com.techvisiondz.app.core.update

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Downloads the update APK to the dedicated [UpdateStash] location.
 *
 * The URL is validated against [UpdateHostPolicy] before the request is built;
 * every redirect hop is validated again by the shared client's network
 * interceptor. Bytes stream straight to disk (nothing is buffered in memory)
 * so the process never holds the whole APK. Progress (0..1, or null when the
 * server did not report a length) is surfaced for the future UI.
 *
 * Cancellation is cooperative through the calling coroutine: each chunk is
 * checked with [ensureActive], and a cancelled or failed download deletes the
 * partial file so no stale or corrupt APK can ever be reused.
 */
interface UpdateApkDownloader {
    suspend fun download(url: String, dest: File, onProgress: (Float?) -> Unit)
}

class OkHttpUpdateApkDownloader(
    private val client: OkHttpClient = UpdateHttpClient.newClient(),
) : UpdateApkDownloader {

    override suspend fun download(
        url: String,
        dest: File,
        onProgress: (Float?) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        if (!UpdateHostPolicy.isTrustedUrl(url)) {
            throw UntrustedUpdateUrlException("APK URL rejected: $url")
        }
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw UnsuccessfulUpdateResponse(response.code, "APK HTTP ${response.code}")
                }
                val body = response.body ?: throw UpdateTransportException("APK response had no body")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    dest.outputStream().buffered().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(
                                if (total > 0) {
                                    (downloaded.toFloat() / total).coerceIn(0f, 1f)
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
                onProgress(1f)
            }
        } catch (e: CancellationException) {
            dest.delete()
            throw e
        } catch (e: UpdateTransportException) {
            dest.delete()
            throw e
        } catch (e: IOException) {
            dest.delete()
            throw UpdateTransportException("APK download failed", e)
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}