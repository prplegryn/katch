# Akatcha

Akatcha is an Android app for extracting and downloading Xiaohongshu video formats with a saved desktop-browser cookie file.

## Stack

- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.13.2, Gradle 8.13, Kotlin 2.2.21
- Compose BOM 2026.05.01
- Chaquopy 17.0.0 + Python 3.11
- yt-dlp 2026.06.09
- `compileSdk` / `targetSdk`: 36

## Core Behavior

1. The Login tab opens `https://www.xiaohongshu.com` in an embedded WebView using a built-in desktop Chrome User-Agent.
2. After manual login, tap the top-right `保存 Cookie` button. The app exports WebView cookies to:

   ```text
   <app files>/cookies/xiaohongshu-cookies.txt
   ```

   The file is in Netscape cookie format and is passed to `yt-dlp`.

3. Paste any Xiaohongshu share text in the Download tab. The app extracts the first supported URL from the text, for example:

   ```text
   牙牙大屏小舞蹈 http://xhslink.com/o/AOASQXmnp3X 复制这段，去【小红书】发现更多好内容~
   ```

4. The probe step calls `yt-dlp` and preserves every returned `formats` item without deduping by resolution, codec, extension, bitrate, or URL.
5. The app also scans the resolved page source for `.mp4`, `.m3u8`, and `.mpd` links. Manifest links are expanded when yt-dlp can parse them, and the resulting entries are appended as `page-source` / `page-manifest`.
6. Choose one row from the format list and download it.

## Storage

The default output directory is:

```text
/storage/emulated/0/Akatcha
```

Android 11+ requires the user to grant "All files access" before the app can write there. If you do not want to grant that permission, use Settings -> Choose directory to select a SAF directory. The chosen directory, theme, last pasted text, and cookie state are remembered.

## Signing

Release and debug APKs are signed with the fixed local PKCS12 keystore:

```text
keystore/akatcha-release.p12
```

This keeps APK updates installable over earlier builds. Do not reuse this signing key for unrelated sensitive apps.

## CI Build

GitHub Actions builds on every push to `main` and uploads the signed release APK artifact:

```text
.github/workflows/android-build.yml
```

The workflow runs:

```bash
./gradlew --no-daemon test assembleRelease
```

Local Gradle execution is not required for normal development in this repository.
