package com.techvisiondz.app.core.update

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * Transport-level update failure (DNS, connect, read, timeout).
 */
internal open class UpdateTransportException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * A URL — either the original request or a redirect hop — failed
 * [UpdateHostPolicy]. Never treated as a generic network failure.
 */
internal class UntrustedUpdateUrlException(message: String) : UpdateTransportException(message)

/**
 * The server answered with a non-success status code.
 */
internal class UnsuccessfulUpdateResponse(
    val statusCode: Int,
    message: String,
) : UpdateTransportException(message)

/**
 * Shared HTTP plumbing for the temporary updater.
 *
 * All updater traffic is HTTPS-only. Redirects (e.g. the GitHub release
 * `latest/download` hop to `objects.githubusercontent.com`) are allowed only
 * because a network interceptor validates the scheme and host of *every*
 * request OkHttp performs — including each redirect target — against
 * [UpdateHostPolicy]. A malicious or compromised manifest therefore cannot
 * redirect the updater to an arbitrary download host, and HTTPS→HTTP redirects
 * are never followed.
 */
internal object UpdateHttpClient {

    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val CALL_TIMEOUT_SECONDS = 300L

    fun newClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .addNetworkInterceptor(TrustedHostEnforcement)
        .build()

    private object TrustedHostEnforcement : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url.toString()
            if (!UpdateHostPolicy.isTrustedUrl(url)) {
                throw UntrustedUpdateUrlException("Updater refused untrusted URL: $url")
            }
            return chain.proceed(chain.request())
        }
    }
}