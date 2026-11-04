
<img width="1446" height="1087" alt="Ouro_Futuristic_Media_Gallery_Poster_final" src="https://github.com/user-attachments/assets/6ed54213-ca72-4ca1-84ba-c63acf1bdb00" />

An offline-first Android gallery that lets you organize the photos and videos already on your phone into **albums** and **events**, without copying, moving, or uploading a single file.

Ouro reads your media straight from the device's MediaStore, stores only lightweight references and organization data locally, and leaves your originals untouched. Deleting an album or event in Ouro never deletes a photo from your device.

> Formerly developed under the working name **Red-Panda**. The Kotlin source namespace is still `com.example.red_panda`; the installed application ID is `com.yahshua.ouro`.

---

## Features

### Photos
- **Continuous photo and video wall** in a three-column staggered layout that preserves each item's original aspect ratio
- **Scroll date bubble**: a small bubble at the top shows the weekday, date and year of the first visible row while you scroll, then fades away
- **Fast on large libraries**: Paging 3 over MediaStore with deterministic newest-first ordering, thumbnail downsampling, and live refresh when device media changes
- **Select mode**: long-press to multi-select, then add the selection to an event
- **Untagged only** filter to see what you haven't organized yet
- **Photo Picker import** for images and videos, with persisted read access so picked media stays available

### Albums and events
- **Albums** (categories) contain dated **events**; events contain photos and videos
- **Stable covers**: a new album or event takes its cover from the first photo added and keeps it as more are added. Covers can be changed manually, and are repaired automatically if the cover media disappears
- **Event collages**: one large preview plus four small thumbnails with a `+N` overflow badge based on the real count
- **Ordered event media**: photos keep the order in which they were added
- **Slideshow** with autoplay and swipe paging; autoplay pauses on videos
- **Favorites** for albums and photos
- **Albums layout switch** between square (1:1) and wide (2:1) cards, remembered across restarts

### Suggested events
Ouro scans untagged media, clusters it by capture date, and suggests events you can create in one tap. Titles are prefilled from the date, and you choose or create the destination album. Suggestions can be dismissed individually or all at once, and dismissals persist.

### Viewer
- Full-screen viewer with swipe paging, pinch-to-zoom and double-tap zoom (swipes page unless the image is zoomed in)
- Native video playback with standard controls

### Look and feel
- Light, dark, or system theme, applied before first paint so there is no theme flash on launch
- Frosted-glass surfaces, a floating pill switcher for Photos and Albums, and content that scrolls behind it
- Consistent slide/fade navigation and system Back handling throughout
- Edge-to-edge layout, accessible touch targets, and scalable typography

### Data safety
- **No sample data**: a fresh install starts empty and shows empty states until you create something
- **Partial photo access** supported on Android 14+, with clear in-app prompts for limited or denied access
- **Missing media recovery**: references are reconciled on launch and after MediaStore changes, using content hashes to re-find files that moved
- **Migrations without data loss**: Room schemas are exported and versioned, and destructive migration fallback is disabled
- **Local JSON backup and restore** with no extra dependencies

---

## Architecture

MVVM with a repository layer:

```
UI (Compose screens, dialogs)
   └─ ViewModels (StateFlow)
        └─ Repositories (GalleryRepository, SettingsRepository)
             ├─ Room (categories, events, photo refs, event-photo links)
             ├─ DataStore (theme, layout, dismissed suggestions)
             └─ MediaStore / Paging 3 (device photos and videos)
```

Photos are never stored by the app. Room keeps only references (URI, capture date, dimensions, content hash) plus the albums, events and ordering you create.

---

## Getting started

### Requirements
- Android Studio (recent stable release) with the Android SDK installed
- JDK matching the version configured in `app/build.gradle.kts`
- A device or emulator with photos and videos in its gallery

### Build and run

```bash
# Debug build
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:testDebugUnitTest
```

Or open the project in Android Studio and run the `app` configuration.

A prebuilt debug APK is kept at `APKs/ouro-1.0.0.apk`.

### Permissions

| Android version | Permission |
|---|---|
| 14+ | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED` (partial access) |
| 13 | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` |
| 12 and below | `READ_EXTERNAL_STORAGE` |

If you grant only partial access, the system Photo Picker lets you add more media to Ouro.

---

## Status

Ouro is in active development. The build has been validated with `assembleDebug` and unit tests on an emulator. Not yet done:

- Stress testing on libraries of 5,000+ items
- A full device-size test matrix
- The **Web Viewer** section in Settings is a disabled placeholder

Per-phase design notes live in `assets/phase-*.md`.

---

## License

No license has been chosen yet.
