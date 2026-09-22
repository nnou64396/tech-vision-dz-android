package com.techvisiondz.app.core.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * On-device coverage of the FileProvider install plumbing. Deliberately NO real
 * install: nothing is ever handed to the system package installer here. These
 * tests prove the deployed manifest wiring (provider authority, cache-path
 * mapping) and the typed, never-throwing installer decisions, all of which are
 * impossible to exercise on the JVM.
 */
class AndroidUpdateApkInstallerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun installer() = AndroidUpdateApkInstaller(context)

    private fun stagedApk(): File {
        val file = UpdateStash.apk(context)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x50, 0x4B)) // PK zip header; not a real APK
        return file
    }

    @Test
    fun fileProviderAuthorityIsPackageScoped() {
        assertEquals(
            "${context.packageName}.fileprovider",
            updateFileProviderAuthority(context.packageName),
        )
        assertTrue(context.packageName.isNotBlank())
    }

    @Test
    fun stagedApkResolvesThroughTheConfiguredFileProvider() {
        val apk = stagedApk()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                updateFileProviderAuthority(context.packageName),
                apk,
            )
            // the authority must match the manifest merge; the cache-path mapping
            // (res/xml/file_paths.xml) must exist or getUriForFile throws.
            assertEquals("content", uri.scheme)
            assertEquals(updateFileProviderAuthority(context.packageName), uri.authority)
            assertTrue(uri.path!!.contains("updater"))
            assertTrue(uri.path!!.endsWith(UpdateStash.UPDATE_APK_NAME))
        } finally {
            apk.delete()
        }
    }

    @Test
    fun installPermissionStateNeverThrowsAndIsTyped() {
        val state = installer().installPermissionState()
        assertTrue(
            "unexpected state $state",
            state == InstallPermissionState.Allowed || state == InstallPermissionState.Denied,
        )
    }

    @Test
    fun resolveInstallerReturnsATypedOutcomeWithoutLaunching() {
        val apk = stagedApk()
        try {
            val resolution = installer().resolveInstaller(apk)
            // Debug builds declare REQUEST_INSTALL_PACKAGES, so NotDeclared would
            // mean the probe is broken. The exact answer depends on device state:
            // either permission still needed, or ready to launch — never a throw.
            assertTrue("unexpected resolution $resolution", resolution is InstallerResolution.Ready ||
                resolution == InstallerResolution.PermissionRequired)
        } finally {
            apk.delete()
        }
    }

    @Test
    fun unknownAppSourcesSettingsIntentTargetsThisPackage() {
        val intent = installer().unknownAppSourcesSettingsIntent()
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertNotNull(intent.data)
        assertEquals("package:${context.packageName}", intent.data.toString())
        assertTrue(intent.resolveActivity(context.packageManager) != null)
    }

    @Test
    fun installIntentCarriesTheApkMimeAndReadGrant() {
        val apk = stagedApk()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                updateFileProviderAuthority(context.packageName),
                apk,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("application/vnd.android.package-archive", intent.type)
            assertTrue(intent.data!!.scheme == "content")
            assertEquals(updateFileProviderAuthority(context.packageName), intent.data!!.authority)
            assertTrue(intent.data!!.path!!.endsWith(UpdateStash.UPDATE_APK_NAME))
            assertTrue(
                intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
            )
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        } finally {
            apk.delete()
        }
    }
}