package com.techvisiondz.app.core.update

import com.techvisiondz.app.BuildConfig
import com.techvisiondz.app.core.config.AppConfig
import kotlinx.coroutines.CancellationException

/**
 * Result of a version check against the pinned update manifest.
 *
 * [NoUpdate] covers "same version" and "older remote version" — the download
 * is only ever considered for a strictly newer [UpdateInfo].
 */
sealed interface UpdateCheckResult {
    /** The updater is disabled (future Google-Play state); no check should have run. */
    data object Disabled : UpdateCheckResult

    /** Remote version is not strictly newer than the installed one. */
    data object NoUpdate : UpdateCheckResult

    data class UpdateAvailable(val update: UpdateInfo) : UpdateCheckResult

    data class Failed(val error: UpdateError) : UpdateCheckResult
}

/**
 * Fetches and validates the update manifest and compares its [versionCode]
 * against the currently installed app.
 *
 * The updater must be enabled ([AppConfig.UPDATER_ENABLED]) or the check
 * short-circuits to [UpdateCheckResult.Disabled]. All failures are converted
 * into [UpdateCheckResult.Failed] with a structured [UpdateError]; network and
 * parsing problems never crash the caller.
 *
 * @param manifestUrl pinned [AppConfig.UPDATE_MANIFEST_URL]
 * @param currentVersionCode [BuildConfig.VERSION_CODE] of the installed app
 */
class UpdateRepository(
    private val manifestUrl: String = AppConfig.UPDATE_MANIFEST_URL,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    private val enabled: Boolean = AppConfig.UPDATER_ENABLED,
    private val fetcher: UpdateManifestFetcher = OkHttpUpdateManifestFetcher(),
) {

    suspend fun checkForUpdate(): UpdateCheckResult {
        if (!enabled) return UpdateCheckResult.Disabled
        return try {
            val raw = fetcher.fetchManifest(manifestUrl)
            when (val validation = UpdateManifestValidator.validate(raw)) {
                UpdateManifestValidator.Validation.Malformed ->
                    UpdateCheckResult.Failed(UpdateError.MalformedManifest)

                UpdateManifestValidator.Validation.InvalidHash ->
                    UpdateCheckResult.Failed(UpdateError.InvalidHash)

                UpdateManifestValidator.Validation.UntrustedUrl ->
                    UpdateCheckResult.Failed(UpdateError.UntrustedUrl)

                is UpdateManifestValidator.Validation.Valid -> {
                    val update = validation.update
                    // A device below the manifest's minimumVersionCode cannot
                    // install this update by itself (the self-install permission
                    // did not exist yet); block before any download and tell the
                    // user precisely, instead of downloading an APK that is then
                    // discarded at verification.
                    if (update.minimumVersionCode != null &&
                        currentVersionCode < update.minimumVersionCode
                    ) {
                        UpdateCheckResult.Failed(UpdateError.BelowMinimum)
                    } else if (update.versionCode <= currentVersionCode) {
                        UpdateCheckResult.NoUpdate
                    } else {
                        UpdateCheckResult.UpdateAvailable(update)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UntrustedUpdateUrlException) {
            UpdateCheckResult.Failed(UpdateError.UntrustedUrl)
        } catch (e: UnsuccessfulUpdateResponse) {
            UpdateCheckResult.Failed(UpdateError.Http)
        } catch (e: UpdateTransportException) {
            UpdateCheckResult.Failed(UpdateError.Network)
        } catch (e: Exception) {
            UpdateCheckResult.Failed(UpdateError.Network)
        }
    }
}