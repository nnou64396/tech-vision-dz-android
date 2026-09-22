package com.techvisiondz.app.core.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/**
 * Result of asking the OS to install the verified APK.
 *
 * The updater never attempts a silent/root install. Launching the system
 * package installer is the only supported path: on Android 8.0+ that requires
 * the "Install unknown apps" permission, which we surface via
 * [UpdateApkInstaller.unknownAppSourcesSettingsIntent] for the UI to deep-link
 * the user to.
 */
sealed interface InstallLaunchResult {
    /** Installer activity launched; the user now owns the decision. */
    data object Launched : InstallLaunchResult

    /** App source permission missing but declared (Android 8.0+); the user can grant it. */
    data object PermissionRequired : InstallLaunchResult

    /** This APK does not declare REQUEST_INSTALL_PACKAGES; not fixable in place. */
    data object PermissionNotDeclared : InstallLaunchResult

    /** No activity on the device can handle the install intent for this APK. */
    data object InstallerUnavailable : InstallLaunchResult

    /** The installer activity existed but launching it failed at runtime. */
    data object LaunchFailed : InstallLaunchResult
}

/**
 * Outcome of probing the install-unknown-apps permission
 * ([PackageManager.canRequestPackageInstalls]).
 *
 * [InstallPermissionState] is the typed, never-throwing result of that probe
 * so the caller never has to catch the `SecurityException` that Android throws
 * when the running APK does not declare `REQUEST_INSTALL_PACKAGES` (the
 * historical pre-1.1.3 build bug). The two "cannot install" conditions stay
 * distinct: [Denied] is recoverable via settings, [NotDeclared] is not.
 */
sealed interface InstallPermissionState {
    /** This app may install unknown apps; launching the installer is safe. */
    data object Allowed : InstallPermissionState

    /** Declared in the manifest but not yet granted; recoverable via settings. */
    data object Denied : InstallPermissionState

    /** Not declared by the running APK at all; not fixable in place. */
    data object NotDeclared : InstallPermissionState
}

/**
 * The fully-decided outcome of preparing the install flow for a verified APK:
 * the permission probe and the activity resolution, separated from the actual
 * launch. Callers can re-resolve without starting the installer — e.g. to
 * confirm, after the user returns from the settings detour, that the install is
 * ready without ever touching the system once nothing can be launched.
 */
sealed interface InstallerResolution {
    /** Permission granted and an installer activity resolved; start [launchIntent]. */
    data class Ready(val launchIntent: Intent) : InstallerResolution

    /** Install-unknown-apps permission declared but denied (Android 8.0+). */
    data object PermissionRequired : InstallerResolution

    /** The running APK does not declare REQUEST_INSTALL_PACKAGES. */
    data object PermissionNotDeclared : InstallerResolution

    /** No activity on the device can handle the APK install intent. */
    data object InstallerUnavailable : InstallerResolution
}

interface UpdateApkInstaller {
    /** Typed, never-throwing outcome of the install-unknown-apps permission probe. */
    fun installPermissionState(): InstallPermissionState

    /**
     * Prepares the install for a verified [apk] without starting anything:
     * probes the permission and resolves an activity that can handle the APK.
     */
    fun resolveInstaller(apk: File): InstallerResolution

    /** Starts the system install flow for [apk] (which must already be verified). */
    fun launchInstaller(apk: File): InstallLaunchResult

    /** Settings deep link so the user can grant the install-unknown-apps permission. */
    fun unknownAppSourcesSettingsIntent(): Intent
}

/** FileProvider authority suffix shared with the manifest's android:authorities. */
const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

/** The FileProvider authority declared for the given application id. */
fun updateFileProviderAuthority(packageName: String): String =
    "$packageName$FILE_PROVIDER_AUTHORITY_SUFFIX"

/**
 * System-installer-backed implementation. The shared APK is exposed to the
 * installer through the FileProvider configured in the manifest (authority
 * from [updateFileProviderAuthority], narrowed to `<cache-path>/updater`), and
 * the grant is read-only and scoped to the single launch via
 * `FLAG_GRANT_READ_URI_PERMISSION`.
 *
 * The permission probe and the activity resolution are proactive: launching
 * never depends on a throwing probe or a blind [launchInstaller] hand-off, and
 * [InstallLaunchResult.InstallerUnavailable] catches the "no app can install
 * this APK" case instead of collapsing every failure into a generic launch
 * error.
 */
class AndroidUpdateApkInstaller(context: Context) : UpdateApkInstaller {

    private val appContext: Context = context.applicationContext

    override fun installPermissionState(): InstallPermissionState =
        permissionOutcome { canRequestPackageInstalls() }

    override fun resolveInstaller(apk: File): InstallerResolution {
        val intent = installIntent(packageUri = packageUriFor(apk))
        val activityResolvable = appContext.packageManager.resolveActivity(intent, 0) != null
        return installerResolutionFor(
            permission = installPermissionState(),
            activityResolvable = activityResolvable,
            launchIntent = intent,
        )
    }

    override fun launchInstaller(apk: File): InstallLaunchResult =
        when (val resolution = resolveInstaller(apk)) {
            is InstallerResolution.Ready ->
                installLaunchOutcome { appContext.startActivity(resolution.launchIntent) }
            InstallerResolution.PermissionRequired -> InstallLaunchResult.PermissionRequired
            InstallerResolution.PermissionNotDeclared -> InstallLaunchResult.PermissionNotDeclared
            InstallerResolution.InstallerUnavailable -> InstallLaunchResult.InstallerUnavailable
        }

    override fun unknownAppSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${appContext.packageName}".toUri(),
        )

    private fun packageUriFor(apk: File): android.net.Uri = FileProvider.getUriForFile(
        appContext,
        updateFileProviderAuthority(appContext.packageName),
        apk,
    )

    private fun installIntent(packageUri: android.net.Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(packageUri, ANDROID_PACKAGE_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun canRequestPackageInstalls(): Boolean =
        appContext.packageManager.canRequestPackageInstalls()

    private companion object {
        const val ANDROID_PACKAGE_MIME = "application/vnd.android.package-archive"
    }
}

/**
 * Probes [canRequest] and maps the two Android 8.0+ outcomes:
 *
 *  - [InstallPermissionState.NotDeclared]: the probe throws `SecurityException`
 *    because this very APK does not declare `REQUEST_INSTALL_PACKAGES` (the
 *    historical pre-1.1.3 build bug). The running binary cannot fix this by
 *    itself; the user must install manually.
 *  - [InstallPermissionState.Denied]: the permission is declared but the user
 *    (or system) has not allowed this app to install unknown apps — recoverable
 *    via [UpdateApkInstaller.unknownAppSourcesSettingsIntent].
 */
internal fun permissionOutcome(canRequest: () -> Boolean): InstallPermissionState = try {
    if (canRequest()) {
        InstallPermissionState.Allowed
    } else {
        InstallPermissionState.Denied
    }
} catch (e: SecurityException) {
    InstallPermissionState.NotDeclared
}

/**
 * Maps the permission probe + activity resolution into a single typed
 * [InstallerResolution]. Pure so the decision is JVM-testable; the caller
 * performs the OS calls (probe and [resolveActivity]) and passes the facts in.
 */
internal fun installerResolutionFor(
    permission: InstallPermissionState,
    activityResolvable: Boolean,
    launchIntent: Intent?,
): InstallerResolution = when (permission) {
    InstallPermissionState.Denied -> InstallerResolution.PermissionRequired
    InstallPermissionState.NotDeclared -> InstallerResolution.PermissionNotDeclared
    InstallPermissionState.Allowed ->
        if (activityResolvable && launchIntent != null) {
            InstallerResolution.Ready(launchIntent)
        } else {
            // No activity can open the APK (e.g. no package installer, or the
            // <queries> block is missing so resolution is empty on API 30+).
            InstallerResolution.InstallerUnavailable
        }
}

/**
 * Runs [startActivity] and maps the outcome. Only a true successful launch
 * becomes [InstallLaunchResult.Launched]; an absent installer at launch time or
 * *any other runtime failure* become [InstallLaunchResult.LaunchFailed] so no
 * unexpected exception can escape to the ViewModel. `Throwable` is
 * intentionally not caught.
 */
internal fun installLaunchOutcome(startActivity: () -> Unit): InstallLaunchResult = try {
    startActivity()
    InstallLaunchResult.Launched
} catch (e: ActivityNotFoundException) {
    InstallLaunchResult.LaunchFailed
} catch (e: RuntimeException) {
    InstallLaunchResult.LaunchFailed
}