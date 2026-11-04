<img width="1446" height="1087" alt="Ouro_Futuristic_Media_Gallery_Poster_final" src="https://github.com/user-attachments/assets/6ed54213-ca72-4ca1-84ba-c63acf1bdb00" />

# Ouro

An offline-first Android gallery that organizes the photos and videos already on your phone into **albums** and **events**, without copying, moving, or uploading anything.

Ouro reads media straight from MediaStore and stores only lightweight references locally. Deleting an album or event never deletes a photo from your device.

## Features

- **Fast browsing**: Paging 3 over MediaStore, newest-first, with live refresh when device media changes
- **Albums and events**: albums contain dated events, which contain photos and videos, with stable auto-covers, collages, and ordered media
- **Suggested events**: untagged media is clustered by capture date so you can create events in one tap
- **Viewer**: full-screen swipe paging, pinch and double-tap zoom, native video playback
- **Slideshow, favorites, and a square/wide album layout switch**
- **Themes**: light, dark, or system, with no flash on launch
- **Data safety**: no sample data, partial photo access on Android 14+, missing-media recovery via content hashes, Room migrations without data loss, and local JSON backup/restore

## Getting started

Requires Android Studio (recent stable), the Android SDK, and the JDK version set in `app/build.gradle.kts`.

```bash
./gradlew :app:assembleDebug        # debug build
./gradlew :app:testDebugUnitTest    # unit tests
```

A prebuilt debug APK is at `APKs/ouro-1.0.0.apk`.

**Permissions:** Android 14+ uses `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, and `READ_MEDIA_VISUAL_USER_SELECTED`. Android 13 uses the first two. Android 12 and below uses `READ_EXTERNAL_STORAGE`. With partial access, the system Photo Picker lets you add more media.

## Status

In active development. Still to do: stress testing on 5,000+ item libraries, a full device-size test matrix, and the Web Viewer (currently a disabled placeholder in Settings). Design notes are in `assets/phase-*.md`.

## Credits 
**Base UI/UX of app :** (Crank from dribble)[https://dribbble.com/crankwh]

**Agents used :** Claude (for phase divisions and code verifications), gemini 3.6 flash and github copilot (for writing code and summary.md)
