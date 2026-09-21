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
 * [UpdateApkInstaller.unknownAppSourcesSettingsIntent] for the future UI to
 * deep-link the user to.
 */
sealed interface InstallLaunchResult {
    /** Installer activity launched; the user now owns the decision. */
    data object Launched : InstallLaunchResult

    /** App source permission missing but declared (Android 8.0+); the user can grant it. */
    data object PermissionRequired : InstallLaunchResult

    /** This APK does not declare REQUEST_INSTALL_PACKAGES; not fixable in place. */
    data object PermissionNotDeclared : InstallLaunchResult

    /** No activity could handle the install intent, or launching it failed. */
    data object LaunchFailed : InstallLaunchResult
}

interface UpdateApkInstaller {
    /** Android 8.0+: whether this package may install unknown apps. */
    fun canRequestPackageInstalls(): Boolean

    /** Starts the system install flow for [apk] (which must already be verified). */
    fun launchInstaller(apk: File): InstallLaunchResult

    /** Settings deep link so the user can grant the install-unknown-apps permission. */
    fun unknownAppSourcesSettingsIntent(): Intent
}

/**
 * System-installer-backed implementation. The shared APK is exposed to the
 * installer through the FileProvider configured in the manifest (authority
 * `<applicationId>.fileprovider`, narrowed to `<cache-path>/updater`), and the
 * grant is read-only and scoped to the single launch via
 * `FLAG_GRANT_READ_URI_PERMISSION`.
 */
class AndroidUpdateApkInstaller(context: Context) : UpdateApkInstaller {

    private val appContext: Context = context.applicationContext

    override fun canRequestPackageInstalls(): Boolean =
        appContext.packageManager.canRequestPackageInstalls()

    override fun launchInstaller(apk: File): InstallLaunchResult {
        when (permissionOutcome { canRequestPackageInstalls() }) {
            PermissionOutcome.Denied -> return InstallLaunchResult.PermissionRequired
            PermissionOutcome.NotDeclared -> return InstallLaunchResult.PermissionNotDeclared
            PermissionOutcome.Allowed -> Unit
        }

        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, ANDROID_PACKAGE_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return installLaunchOutcome { appContext.startActivity(intent) }
    }

    override fun unknownAppSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${appContext.packageName}".toUri(),
        )

    private companion object {
        const val ANDROID_PACKAGE_MIME = "application/vnd.android.package-archive"
    }
}

/**
 * Outcome of probing whether this APK may install unknown apps. Kept distinct
 * from [InstallLaunchResult] so the two "cannot install" conditions are never
 * collapsed into one: denied-but-declared is recoverable, undeclared is not.
 */
internal enum class PermissionOutcome { Allowed, Denied, NotDeclared }

/**
 * Probes [canRequest] and maps the two Android 8.0+ outcomes:
 *
 *  - [PermissionOutcome.NotDeclared]: the probe throws `SecurityException`
 *    because this very APK does not declare `REQUEST_INSTALL_PACKAGES` (the
 *    historical pre-1.1.3 build bug). The running binary cannot fix this by
 *    itself; the user must install manually.
 *  - [PermissionOutcome.Denied]: the permission is declared but the user
 *    (or system) has not allowed this app to install unknown apps — recoverable
 *    via [UpdateApkInstaller.unknownAppSourcesSettingsIntent].
 */
internal fun permissionOutcome(canRequest: () -> Boolean): PermissionOutcome = try {
    if (canRequest()) {
        PermissionOutcome.Allowed
    } else {
        PermissionOutcome.Denied
    }
} catch (e: SecurityException) {
    PermissionOutcome.NotDeclared
}

/**
 * Runs [startActivity] and maps the outcome. Only a true successful launch
 * becomes [InstallLaunchResult.Launched]; an absent installer
 * ([ActivityNotFoundException]) *and any other runtime failure* become
 * [InstallLaunchResult.LaunchFailed] so no unexpected exception can escape to
 * the ViewModel. `Throwable` is intentionally not caught.
 */
internal fun installLaunchOutcome(startActivity: () -> Unit): InstallLaunchResult = try {
    startActivity()
    InstallLaunchResult.Launched
} catch (e: ActivityNotFoundException) {
    InstallLaunchResult.LaunchFailed
} catch (e: RuntimeException) {
    InstallLaunchResult.LaunchFailed
}