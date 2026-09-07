package com.techvisiondz.app.core.data

import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Domain error used to report data-layer failures up to the UI.
 *
 * Keeps the ViewModel free of SDK-specific exceptions so the same states can
 * be covered by both fake and real repositories.
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Required configuration (Supabase URL / anon key) is missing. */
    class ConfigurationNotSet(message: String) : DataException(message)

    /** Unable to reach or respond (connectivity / timeout). */
    class Network(message: String = "Could not reach the server. Check your connection", cause: Throwable? = null) :
        DataException(message, cause)

    /** Invalid credentials / anon key rejected. */
    class Authentication(message: String = "Authentication required", cause: Throwable? = null) :
        DataException(message, cause)

    /** Valid credentials but the server denied access (RLS). */
    class Authorization(message: String = "Access denied by the server", cause: Throwable? = null) :
        DataException(message, cause)

    /** The requested resource does not exist. */
    class NotFound(message: String = "Requested content was not found", cause: Throwable? = null) :
        DataException(message, cause)

    /** The response did not match expectations (columns, types, malformed payload). */
    class Schema(message: String = "The server returned an unexpected response", cause: Throwable? = null) :
        DataException(message, cause)

    /** Server-side failure. */
    class Server(message: String = "The server reported an error", cause: Throwable? = null) :
        DataException(message, cause)

    /** Anything else. */
    class Unknown(message: String = "Something went wrong", cause: Throwable? = null) :
        DataException(message, cause)
}

/**
 * Maps Supabase/Ktor failures into a [DataException]. Coroutine cancellation is
 * rethrown unchanged so scopes can abort cleanly.
 */
fun Throwable.toDataException(): DataException {
    if (this is CancellationException) throw this
    return when (this) {
        is DataException -> this
        is PostgrestRestException -> mapByStatus(
            authorization = { DataException.Authorization(cause = this) },
            authentication = { DataException.Authentication(cause = this) },
            notFound = { DataException.NotFound(cause = this) },
            badRequest = { DataException.Schema(cause = this) },
            server = { DataException.Server("Server error (HTTP $statusCode)", this) },
        )
        is UnauthorizedRestException -> DataException.Authentication(cause = this)
        is NotFoundRestException -> DataException.NotFound(cause = this)
        is BadRequestRestException -> DataException.Schema(message = "The server rejected the request", cause = this)
        is RestException -> mapByStatus(
            authorization = { DataException.Authorization(cause = this) },
            authentication = { DataException.Authentication(cause = this) },
            notFound = { DataException.NotFound(cause = this) },
            badRequest = { DataException.Schema(cause = this) },
            server = { DataException.Server("Server error (HTTP $statusCode)", this) },
        )
        is HttpRequestTimeoutException -> DataException.Network("Request timed out", this)
        is HttpRequestException -> DataException.Network(cause = this)
        is IOException -> DataException.Network(cause = this)
        else -> DataException.Unknown(cause = this)
    }
}

private inline fun Throwable.mapByStatus(
    authentication: () -> DataException,
    authorization: () -> DataException,
    notFound: () -> DataException,
    badRequest: () -> DataException,
    server: () -> DataException,
): DataException = when ((this as? RestException)?.statusCode) {
    401 -> authentication()
    403 -> authorization()
    404 -> notFound()
    400 -> badRequest()
    else -> server()
}