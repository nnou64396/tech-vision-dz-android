package com.techvisiondz.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure decision rules of [AndroidUpdateApkInstaller]: mapping the
 * install-unknown-apps probe and the launch activity outcome into typed
 * results. These run entirely on the JVM without Android APIs — the same
 * pattern as [UpdateApkVerifierRulesTest].
 */
class UpdateApkInstallerRulesTest {

    @Test
    fun `allowed probe maps to allowed`() {
        assertEquals(PermissionOutcome.Allowed, permissionOutcome { true })
    }

    @Test
    fun `denied probe maps to denied not not declared`() {
        assertEquals(PermissionOutcome.Denied, permissionOutcome { false })
    }

    @Test
    fun `security exception from probe maps to not declared not denied`() {
        // Android 8.0+ throws SecurityException when this APK does not declare
        // REQUEST_INSTALL_PACKAGES. That is the historical pre-1.1.3 build bug and
        // is a distinct condition from "declared but denied" (which returns false).
        val outcome = permissionOutcome {
            throw SecurityException(
                "Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api",
            )
        }
        assertEquals(PermissionOutcome.NotDeclared, outcome)
    }

    @Test
    fun `only security exceptions are mapped by the probe`() {
        val outcome = runCatching {
            permissionOutcome { throw IllegalStateException("boom") }
        }
        assertTrue("expected a non-security failure to rethrow, got $outcome", outcome.isFailure)
    }

    @Test
    fun `successful activity launch maps to launched`() {
        assertEquals(InstallLaunchResult.Launched, installLaunchOutcome { })
    }

    @Test
    fun `runtime failure on launch maps to launch failed`() {
        assertEquals(InstallLaunchResult.LaunchFailed, installLaunchOutcome { throw IllegalStateException("boom") })
    }

    @Test
    fun `launch outcome never leaks a successful launch as a failure`() {
        var launched = false
        val result = installLaunchOutcome { launched = true }
        assertEquals(InstallLaunchResult.Launched, result)
        assertTrue(launched)
    }
}