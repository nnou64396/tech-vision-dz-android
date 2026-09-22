# In-app updater — manual QA checklist (v1.1.5 / versionCode 8)

Scope: the permission round-trip fix, the typed installer decisions, and the
deterministic `minimumVersionCode` floor. Follow every step on a physical device
or emulator. These steps intentionally happen only by hand — the automated
instrumentation tests never perform a real install.

Automated signals to confirm first:
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.techvisiondz.app.feature.update.UpdateViewModelTest" --tests "com.techvisiondz.app.core.update.UpdateApkInstallerRulesTest"`
- `.\gradlew.bat :buildSrc:test`
- `.\gradlew.bat :app:lintDebug`
- `.\gradlew.bat :app:verifyReleaseApk` (needs the production signing keystore)

## Setup

1. Install v1.1.4 (versionCode 7) from a previous build.
2. Build v1.1.5: `.\gradlew.bat :app:assembleRelease` (or install the debug
   APK for the non-signing paths).
3. Have a stale `update-manifest.json` available that points at the new APK, or
   rely on the pinned GitHub manifest once a real release exists.

## A. Fresh-install file provider wiring

1. Fresh-install v1.1.5; open Account → Settings → Updates.
2. "Check for updates" → new version offered → "Update now" → download →
   verification → "Ready to install". No console `SecurityException`.
3. Tap Install. The system package installer must open (unknown-apps granted on
   a fresh install flow or via Settings).

## B. Permission round-trip (the core regression)

Prerequisite: REQUEST_INSTALL_PACKAGES for TECH VISION DZ is DENIED.
On a fresh device, deny "Install unknown apps" under
Settings → Apps → TECH VISION DZ → Install unknown apps.

1. Offer the update, download to "Ready to install". Tap Install.
2. Dialog switches to "Installation permission required".
3. Tap "Open settings" → the "Install unknown apps" page for this package opens.
4. Toggle the permission ON; press Back to return to the app.
5. The dialog must return to "Ready to install" WITHOUT re-downloading
   (Resources/Network tab shows a single GET of the APK).
6. Tap Install → system installer opens.
7. Repeat but return from Settings WITHOUT granting: the dialog must stay on
   "Installation permission required".
8. From that same state tap "Install" after granting in a second visit: the
   install must start directly (no re-download).

## C. "Too old to self-update" (below minimum)

1. Install a build with versionCode < 6 (e.g. a local build of v1.1.1, vc4).
2. Point the manifest floor at 6 or above. "Check for updates".
3. Expect the distinct "can't update itself / install manually" error and that
   NO download starts (the below-minimum guard is at check time).

## D. Staged APK lost (cache cleared)

1. Download to "Ready to install".
2. Clear the app's cache (or delete `cacheDir/updater`) from Settings.
3. Tap Install → expect the visible "downloaded update is no longer available"
   error dialog, NOT a silent dismiss.
4. Same after the settings round-trip (grant while the APK is gone) → same error.

## E. No installer available

1. On a device/emulator with the package installer removed/disabled, tap
   Install → expect the "No app on this device can open the update" error, not
   a crash or a silent no-op.

## F. Pre-1.1.3 bootstrap (manual path)

1. Run a v1.1.2 (vc5) APK that cannot self-update.
2. "Check for updates" must NOT crash and must show the manual-install guidance
   ("download the latest APK ... manually"), confirming the SecurityException
   path is mapped.

## G. Localization

Repeat B steps with app language = العربية and = English. All four dialog
states (permission required, install error, apk missing, too old) must render
correct RTL/LTR copy with no missing-string placeholders.

## H. Release gate

1. `.\gradlew.bat :app:verifyReleaseApk` on a fully signed release build must
   pass and log a "FileProvider config: ... ok" line.
2. Confirm the generated `update-manifest.json` contains exactly
   `"minimumVersionCode": 6` and the CI validation rejects any other floor.
3. Confirm versionCode 8 > 7 monotonicity is accepted by the release pipeline.

## Do not do
- Never perform a real install from the automated tests.
- Never publish/commit from this checklist run without explicit approval.