package com.techvisiondz.app.core.update

/**
 * Structured failures surfaced by the temporary updater so the UI can present
 * precise, localized messages instead of raw exceptions. Kept small and flat
 * on purpose — the stages map 1:1 to distinct user-facing outcomes.
 */
sealed interface UpdateError {
    /** Updater switched off (post Google-Play state); no check is performed. */
    data object Disabled : UpdateError

    /** Transport-level failure while reaching the manifest/download (DNS, connect, read, timeout). */
    data object Network : UpdateError

    /** The server answered with a non-success HTTP status. */
    data object Http : UpdateError

    /** The manifest could not be parsed or failed field validation. */
    data object MalformedManifest : UpdateError

    /** The manifest did not carry a valid 64-character hexadecimal SHA-256 field. */
    data object InvalidHash : UpdateError

    /** A URL (manifest endpoint or manifest-sourced APK URL) failed the trusted-host policy. */
    data object UntrustedUrl : UpdateError

    /** The APK download failed (streaming/transport error). */
    data object Download : UpdateError

    /** The downloaded APK's SHA-256 did not match the manifest. */
    data object HashMismatch : UpdateError

    /** The downloaded APK was not a readable package archive. */
    data object InvalidApk : UpdateError

    /** The downloaded APK is for a different application id. */
    data object WrongPackage : UpdateError

    /** The downloaded APK's version does not match the manifest version. */
    data object WrongVersion : UpdateError

    /** The installed app is older than the manifest's minimumVersionCode; it cannot self-update. */
    data object BelowMinimum : UpdateError

    /** The app is not allowed to request package installations (declared but denied). */
    data object InstallationPermissionRequired : UpdateError

    /** This APK does not declare the permission to install packages; not fixable in place. */
    data object PermissionNotDeclared : UpdateError

    /** Android's system package installer could not be launched. */
    data object InstallerLaunch : UpdateError

    /** No activity on the device can open the update APK. */
    data object InstallerUnavailable : UpdateError

    /** The user or the system cancelled the update flow. */
    data object Cancelled : UpdateError
}