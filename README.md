# TECH VISION DZ — Android App

The official Android application for **TECH VISION DZ**, an Algerian technology platform covering the latest in tech news, AI, smartphones, software, and digital trends.

## Download

| | |
|---|---|
| **Latest version** | v1.0.0 |
| **Min Android** | Android 8.0 (API 26) |
| **Package** | `com.techvisiondz.app` |
| **APK** | [Download from GitHub Releases](https://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.0.0/app-release.apk) |

## Features

- **Technology news & articles** — Stay up to date with the latest stories.
- **Search** — Find articles quickly by keyword.
- **Categories** — Browse content organized by topic.
- **Authors & tags** — Discover writers and explore related topics.
- **Registration & login** — Create an account or sign in.
- **Saved articles / bookmarks** — Save your favorite reads for later.
- **Public-first browsing** — Browse articles without needing an account.
- **Mobile-friendly experience** — Designed for Android with a clean, native interface.

## About TECH VISION DZ

TECH VISION DZ covers:

- Technology and the latest industry news
- Artificial intelligence
- Smartphones and mobile devices
- Software and applications
- Android & iOS ecosystems
- Hardware and gadgets
- Digital services and emerging trends

Visit the official website at **[techvisiondz.com](https://techvisiondz.com)**.

## Privacy & Security

- All network communication uses **HTTPS only**.
- The app requests only the permissions necessary for its core functionality.
- No private credentials, signing keys, or secrets are included in this public repository.

## Technology Stack

- **Kotlin**
- **Jetpack Compose**
- **Supabase**
- **Ktor**
- **Coil**

## Project Status

**v1.0.0** is publicly available for testing and feedback.

## Distribution

- **GitHub Releases** — Direct APK download is available now.
- **Google Play** — Coming soon.

## Temporary In-App Updater

Until the app is on Google Play, updates are delivered through the built-in temporary updater, with **GitHub Releases** as the distribution channel.

**Stable manifest URL:**

```
https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json
```

**Release assets expected on GitHub Releases:**

- `app-release.apk` — the signed APK for the new version
- `update-manifest.json` — the version manifest the updater reads

**Manifest fields:**

| Field | Required | Meaning |
|---|---|---|
| `versionCode` | yes | New version code; must be **higher** than the previous release |
| `versionName` | yes | Human-readable version, e.g. `1.1.0` |
| `downloadUrl` | yes | HTTPS URL of the APK asset (GitHub Releases host only) |
| `sha256` | yes | Lowercase hex SHA-256 of the APK file |
| `releaseNotes` | no | Optional short changelog shown in the update dialog |
| `minimumVersionCode` | no | Minimum installed version allowed to update (`0` or `> versionCode` is rejected) |

**Publishing an update:**

1. Publish a new GitHub Release.
2. Upload the signed `app-release.apk` for that release.
3. Upload/create `update-manifest.json` (must reference the released APK URL and its correct `versionCode`/`versionName`/`sha256`).
4. Verify the SHA-256 in the manifest matches the uploaded APK exactly.
5. Test the update flow on a device with the previous version installed.

**Important rules:**

- `versionCode` must **increase** for every published update.
- The **same release signing key** must be reused for every update (the app refuses APKs signed by a different key).
- Android 8.0+ may require enabling **"Install unknown apps"** for this app before an update can be installed; the updater guides users to the proper settings screen.

**Future plans:**

- The updater is temporary and will be replaced by **Google Play In-App Updates** once the app is published on Google Play.
- It can be switched off at any time by setting **`UPDATER_ENABLED = false`** in `app/build.gradle.kts`.

## Feedback

Found a bug or have a suggestion? Please open an issue on the [GitHub Issues](https://github.com/nnou64396/tech-vision-dz-android/issues) page.
