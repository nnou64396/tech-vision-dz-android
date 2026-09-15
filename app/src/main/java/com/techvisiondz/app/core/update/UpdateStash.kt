package com.techvisiondz.app.core.update

import android.content.Context
import java.io.File

/**
 * Owns the app's dedicated internal updater directory.
 *
 * The APK is staged under `cacheDir/updater` — an app-private directory that
 * never touches external storage. The path maps 1:1 to the FileProvider entry
 * in `res/xml/file_paths.xml`, so only this directory is ever exposed to the
 * system package installer.
 */
object UpdateStash {

    const val UPDATE_APK_NAME = "tech-vision-dz-update.apk"

    /** The dedicated updater directory, created on first access. */
    fun directory(context: Context): File =
        File(context.cacheDir, "updater").apply { mkdirs() }

    /** Deterministic staging location for the current download. */
    fun apk(context: Context): File = File(directory(context), UPDATE_APK_NAME)

    /** Removes a previously staged update APK. Returns true when it is gone. */
    fun deleteApk(context: Context): Boolean =
        apk(context).let { !it.exists() || it.delete() }

    /**
     * Deletes every file in the updater directory except [keep]. Prevents
     * stale APKs accumulating across releases; the current download is kept
     * until it is verified or handed to the installer.
     */
    fun cleanup(context: Context, keep: File? = null) {
        directory(context).listFiles()?.forEach { file ->
            if (file != keep) file.delete()
        }
    }
}