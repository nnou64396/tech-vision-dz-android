package com.techvisiondz.app.core.update

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches the raw update manifest over HTTPS.
 *
 * Injecting a slim interface keeps [UpdateRepository] JVM-testable with a fake
 * fetcher while the real implementation reuses the OkHttp client already on
 * the project's compile classpath.
 */
fun interface UpdateManifestFetcher {
    suspend fun fetchManifest(rawUrl: String): String
}

/**
 * OkHttp-backed fetcher. The pinned manifest URL is validated against
 * [UpdateHostPolicy] before the request is even built, and every redirect hop
 * is validated again by the client's network interceptor
 * ([UpdateHttpClient]). A non-success status is surfaced as a typed exception
 * so the repository can distinguish network from HTTP failures.
 */
class OkHttpUpdateManifestFetcher(
    private val client: OkHttpClient = UpdateHttpClient.newClient(),
) : UpdateManifestFetcher {

    override suspend fun fetchManifest(rawUrl: String): String = withContext(Dispatchers.IO) {
        if (!UpdateHostPolicy.isTrustedUrl(rawUrl)) {
            throw UntrustedUpdateUrlException("Manifest URL rejected: $rawUrl")
        }
        val request = Request.Builder()
            .url(rawUrl)
            .header("Accept", "application/json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw UnsuccessfulUpdateResponse(response.code, "Manifest HTTP ${response.code}")
                }
                response.body?.string()
                    ?: throw UpdateTransportException("Manifest response had no body")
            }
        } catch (e: UpdateTransportException) {
            throw e
        } catch (e: IOException) {
            throw UpdateTransportException("Manifest request failed", e)
        }
    }
}