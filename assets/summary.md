Here is the phase-wise summary of the **Red-Panda** offline-first photo gallery app, starting with the project folder structure.

---

### 📂 Folder Structure

```
Red-panda/
├── .gitignore
├── build.gradle.kts                              # Root build file
├── settings.gradle.kts                           # Module & repository declarations
├── gradle.properties
├── gradle/
│   └── libs.versions.toml                       # Centralized Version Catalog dependencies
├── assets/                                      # Phase documentation specs
│   ├── phase-1.md
│   ├── phase-2.md
│   ├── phase-3.md
│   └── phase-4.md
└── app/
    ├── build.gradle.kts                          # App-level module configuration & dependencies
    ├── keepRules/
    │   └── rules.keep                            # ProGuard / R8 keep rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml               # App manifest (permissions, main activity)
        │   ├── java/com/example/red_panda/
        │   │   ├── MainActivity.kt               # Entry point activity initializing NavGraph & Edge-to-Edge Theme
        │   │   ├── RedPandaApplication.kt        # Application class initializing Room DB instance
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── AppDatabase.kt        # Room database definition
        │   │   │   │   ├── MediaStoreHelper.kt   # Utility for querying device MediaStore photos
        │   │   │   │   ├── dao/
        │   │   │   │   │   ├── CategoryDao.kt    # Category DB operations
        │   │   │   │   │   ├── EventDao.kt       # Event DB operations
        │   │   │   │   │   └── PhotoDao.kt       # PhotoRef & EventPhoto DB operations
        │   │   │   │   └── entity/
        │   │   │   │       ├── CategoryEntity.kt # Category DB table
        │   │   │   │       ├── EventEntity.kt    # Event DB table
        │   │   │   │       ├── EventPhotoEntity.kt # Many-to-many event-photo join table
        │   │   │   │       └── PhotoRefEntity.kt # Photo metadata DB table
        │   │   │   ├── paging/
        │   │   │   │   ├── MediaStorePagingSource.kt # Paging 3 source for MediaStore photos
        │   │   │   │   └── TimelineItem.kt       # Timeline header and photo item models
        │   │   │   └── repository/
        │   │   │       ├── GalleryRepository.kt  # Repository interface
        │   │   │       ├── GalleryRepositoryImpl.kt # Repository implementation
        │   │   │       ├── SettingsRepository.kt # DataStore settings repository interface
        │   │   │       └── SettingsRepositoryImpl.kt # DataStore preferences repository implementation
        │   │   └── ui/
        │   │       ├── components/
        │   │       │   ├── AddToEventDialog.kt   # Dialog for assigning photos to existing or new events
        │   │       │   ├── CategoryDialog.kt     # Create/edit category dialog
        │   │       │   ├── ConfirmDeleteDialog.kt# Confirmation dialog for item deletion
        │   │       │   ├── EventDialog.kt        # Create/edit event dialog with date picker
        │   │       │   └── FloatingPillNavigationBar.kt # Floating pill bottom navigation bar
        │   │       ├── model/
        │   │       │   └── Album.kt              # UI data representation models
        │   │       ├── navigation/
        │   │       │   └── NavGraph.kt           # Compose Navigation routes & screen mapping
        │   │       ├── screens/
        │   │       │   ├── MainShellScreen.kt    # Main navigation host for Photos & Albums tabs
        │   │       │   ├── PhotosTimelineScreen.kt # Paged Google Photos style photo timeline
        │   │       │   ├── HomeScreen.kt         # Albums category grid screen
        │   │       │   ├── DetailScreen.kt       # Category detail view with cover edit dialog & event collages
        │   │       │   ├── EventDetailScreen.kt  # Photos grid inside a specific event
        │   │       │   ├── FullScreenPhotoViewer.kt # Pinch-zoom, swipe photo gallery viewer
        │   │       │   └── SettingsScreen.kt     # Appearance, Photos, Storage, Web Viewer & About settings
        │   │       ├── theme/
        │   │       │   ├── Color.kt              # App light & dark mode color palette
        │   │       │   ├── Theme.kt              # Material 3 light/dark/system theme setup
        │   │       │   └── Type.kt               # Material typography configuration
        │   │       └── viewmodel/
        │   │           ├── PhotosTimelineViewModel.kt # ViewModel for PhotosTimelineScreen
        │   │           ├── HomeViewModel.kt      # ViewModel for HomeScreen
        │   │           ├── DetailViewModel.kt    # ViewModel for DetailScreen
        │   │           ├── EventDetailViewModel.kt # ViewModel for EventDetailScreen
        │   │           ├── SettingsViewModel.kt  # ViewModel for SettingsScreen
        │   │           └── ViewModelFactory.kt   # Manual DI ViewModel Provider Factory
        │   └── res/
        │       ├── values/
        │       │   ├── colors.xml
        │       │   ├── strings.xml
        │       │   └── themes.xml                # Black window background to prevent startup flashes
        │       └── xml/
        ├── androidTest/                          # Instrumentation tests
        └── test/                                 # Unit tests
```

---

### 📋 Phase-Wise Execution Summary

#### 🔹 Phase 1: Project Setup, Dark Theme & Home Screen (Mock Data)
* **What was executed:**
  * Configured build dependencies in [libs.versions.toml](file:///home/joy/Documents/red-panda/gradle/libs.versions.toml) and [app/build.gradle.kts](file:///home/joy/Documents/red-panda/app/build.gradle.kts) for Jetpack Compose, Material 3, Navigation Compose, Coil, and Room.
  * Configured the dark-mode theme palette in [Color.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Color.kt) (`#121214` background, `#1C1C20` card surface) and applied it via [Theme.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Theme.kt).
  * Built [HomeScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) with a two-column staggered card layout displaying category cards with photo background placeholders, bottom dark gradients, favorite badges, and contextual popup menus.
  * Established single-activity navigation using `NavHost` in [NavGraph.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/navigation/NavGraph.kt) attached to [MainActivity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/MainActivity.kt).
* **How it was executed:**
  * Utilized Jetpack Compose `LazyVerticalGrid` and `StaggeredGrid` with 24dp rounded corners.
  * Created mock album models in [Album.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/model/Album.kt) to validate UI rendering before database wiring.
* **Why it was executed:**
  * To establish a solid foundation for theme aesthetics, screen layout structures, and navigation flows prior to attaching persistent database and MediaStore layers.

---

#### 🔹 Phase 2: Category Detail Screen, Shared Element Transitions & Layout Polish
* **What was executed:**
  * Developed [DetailScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt) featuring a hero header with category title, back button, photo count, filter chips, and event collage rows (1 large + 4 small thumbnails with a "+N" overflow badge).
  * Implemented Compose `SharedTransitionLayout` / `SharedTransitionScope` to create expanding shared-element card-to-detail screen animations.
  * Refined action button layouts (`Favorite` heart icon and `MoreVert` menu) on album cards in [HomeScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) to dynamically adjust spacing without hardcoded offsets, resolving touch-target overlap issues across varying screen aspect ratios.
* **How it was executed:**
  * Integrated `ExperimentalSharedTransitionApi` using `.sharedBounds()` modifiers linked to unique item keys (`album-cover-${album.id}`).
  * Used custom Compose modifier sizing (`Modifier.size(32.dp)` + `CircleShape`) and `Arrangement.spacedBy()` inside top-right card overlays.
* **Why it was executed:**
  * To deliver fluid navigation transitions and ensure reliable UI rendering across different phone screen sizes.

---

#### 🔹 Phase 3: Room Database, DAOs & Repository Pattern
* **What was executed:**
  * Created the local Room database schema:
    * Entities: [CategoryEntity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/entity/CategoryEntity.kt), [EventEntity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/entity/EventEntity.kt), [PhotoRefEntity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/entity/PhotoRefEntity.kt), and [EventPhotoEntity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/entity/EventPhotoEntity.kt).
    * DAOs: [CategoryDao.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/dao/CategoryDao.kt), [EventDao.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/dao/CategoryDao.kt), and [PhotoDao.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/dao/PhotoDao.kt).
  * Implemented the application class [RedPandaApplication.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/RedPandaApplication.kt) providing database singletons.
  * Created [GalleryRepository.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepository.kt) and [GalleryRepositoryImpl.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt) to handle reactive data mapping.
  * Added management dialogs: [CategoryDialog.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/components/CategoryDialog.kt), [EventDialog.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/components/EventDialog.kt), and [ConfirmDeleteDialog.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/components/ConfirmDeleteDialog.kt).
  * Connected [HomeViewModel.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/HomeViewModel.kt), [DetailViewModel.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/DetailViewModel.kt), and [ViewModelFactory.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/ViewModelFactory.kt) to replace hardcoded mock data with reactive `StateFlow` queries.
* **How it was executed:**
  * Implemented Room transactions (`@Transaction`) for category-event-photo relation mappings.
  * Used Kotlin `Flow` and `viewModelScope` with `Dispatchers.IO` for asynchronous DB operations.
* **Why it was executed:**
  * To enable offline persistence, permitting users to create, rename, edit, favorite, and delete categories and dated events locally.

---

#### 🔹 Phase 4: MediaStore Integration, Photo Picker, Full-Screen Viewer & Suggested Events
* **What was executed:**
  * Added media permissions in [AndroidManifest.xml](file:///home/joy/Documents/red-panda/app/src/main/java/AndroidManifest.xml) (`READ_MEDIA_IMAGES` for API 33+ and `READ_EXTERNAL_STORAGE` for older Android versions).
  * Built [MediaStoreHelper.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/MediaStoreHelper.kt) to query device images, retrieve Uris, taken timestamps, dimensions, and generate content hashes to re-resolve stale MediaStore references.
  * Integrated system Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`) to allow users to pick device photos and attach them to specific events.
  * Implemented [EventDetailScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/EventDetailScreen.kt) with photo grid displays and integrated Coil thumbnail disk/memory caching.
  * Created [FullScreenPhotoViewer.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/FullScreenPhotoViewer.kt) featuring a `HorizontalPager` with pinch-to-zoom (`rememberTransformableState`) and double-tap zoom capabilities.
  * Added Date-Based Suggested Events logic in [GalleryRepositoryImpl.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt): automatically scanning untagged photos, clustering them by creation date, and prompting the user to batch-create events.
* **How it was executed:**
  * Queried device `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` via `ContentResolver`.
  * Used Compose `graphicsLayer(scaleX, scaleY, translationX, translationY)` for photo zooming and panning gestures.
* **Why it was executed:**
  * To enable media management without copying or moving original photo files, providing an inspection experience and automatic photo clustering.

---

#### 🔹 Phase 5: Theming, Settings & Hero Cover
* **What was executed:**
  * Integrated `androidx.datastore:datastore-preferences` dependency in [libs.versions.toml](file:///home/joy/Documents/red-panda/gradle/libs.versions.toml) and [app/build.gradle.kts](file:///home/joy/Documents/red-panda/app/build.gradle.kts).
  * Created [SettingsRepository.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/SettingsRepository.kt) to manage `ThemeSetting` (System Default, Light, Dark) preferences asynchronously.
  * Updated [Color.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Color.kt) and [Theme.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Theme.kt) with complete `LightColorScheme` and `DarkColorScheme` palettes.
  * Updated [themes.xml](file:///home/joy/Documents/red-panda/app/src/main/res/values/themes.xml) with a dark window background and updated [MainActivity.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/MainActivity.kt) with dynamic `enableEdgeToEdge()` system bar icon styling to eliminate startup theme flashes.
  * Built [SettingsScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/SettingsScreen.kt) with Appearance, Photos, Storage, Web Viewer (disabled Phase 8 placeholder), and About sections, accessible via a top bar gear icon on [HomeScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt).
  * Implemented unified, deterministic Category Cover resolution in [GalleryRepositoryImpl.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt): default category cover resolves to the first photo of the earliest event by date, shared between Home cards and Detail hero headers.
  * Implemented Hero Cover editing in [DetailScreen.kt](file:///home/joy/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt) offering "Use first photo (Default)", "Pick from Category Photos", and "Pick from Device Gallery" (which automatically creates a `PhotoRefEntity` entry in Room and assigns `coverPhotoId`).
  * Ensured text overlaid on photo cards and hero headers remains pure white (`Color.White`) across both Light and Dark themes for contrast over dark gradient overlays.
* **How it was executed:**
  * Used `preferencesDataStore` with `Flow<ThemeSetting>` and Compose `collectAsState()`.
  * Used Room `photoDao.getEarliestPhotoForCategorySync()` SQL queries and Coil `AsyncImage` image loading.
  * Integrated System Photo Picker `ActivityResultContracts.PickVisualMedia` for device cover photo selection.
* **Why it was executed:**
  * To complete theme customization, settings management, and category hero image customization before proceeding to Phase 6 data integrity & photo access hardening.

---

#### 🔹 Phase 6: Data Integrity & Photo Access Hardening
* **What was executed:**
  * Updated [AndroidManifest.xml](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/AndroidManifest.xml) to declare `READ_MEDIA_VISUAL_USER_SELECTED` for Android 14+ (API 34+) partial photo access.
  * Enhanced [MediaStoreHelper.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/MediaStoreHelper.kt) with `PhotoAccessState` (`FULL`, `PARTIAL`, `DENIED`), URI accessibility checks (`isUriAccessible`), MD5 content hash generation (`calculateContentHash`), and MediaStore hash re-resolution (`reResolvePhotoByHash`).
  * Updated permission handling in [HomeScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) to use `ActivityResultContracts.RequestMultiplePermissions()`, adding explicit UI cards for Limited Access ("Selected Photos Only") and Denied Access ("Access Required") with quick actions to select more photos or launch system App Settings.
  * Added a `LifecycleEventObserver` listening to `ON_RESUME` in [HomeScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) and [SettingsScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/SettingsScreen.kt) to re-evaluate photo permission status whenever returning from system settings or system photo selection dialogs.
  * Enabled Room schema export (`exportSchema = true`) in [AppDatabase.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/AppDatabase.kt), configured KSP schema output directory in [app/build.gradle.kts](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/build.gradle.kts), and established Room migration architecture (`MIGRATION_1_2`) alongside existing foreign key cascade deletes and table indices.
  * Implemented stale MediaStore reference handling and missing photo cleanup logic in [PhotoDao.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/local/dao/PhotoDao.kt) and [GalleryRepositoryImpl.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt) (`cleanupMissingPhotos`), allowing users to scan and purge orphaned database records via a new button in [SettingsScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/SettingsScreen.kt).
  * Implemented custom cover photo fallback logic in [GalleryRepositoryImpl.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt) (`resolveCoverPhotoUri`): if a user-selected custom cover photo disappears from the device, the app automatically falls back to the earliest accessible category photo or background color placeholder without crashing or displaying broken image assets.
  * Updated delete dialog wording in [ConfirmDeleteDialog.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/components/ConfirmDeleteDialog.kt), [HomeScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt), [DetailScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt), and [EventDetailScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/EventDetailScreen.kt) to explicitly reassure users that deleting an album or event removes app organization data only, leaving original device gallery photos untouched.
* **How it was executed:**
  * Utilized Android 14+ `READ_MEDIA_VISUAL_USER_SELECTED` API and `ActivityResultContracts.RequestMultiplePermissions()` for granular permissions management.
  * Leveraged `ContentResolver.openInputStream` for stream access verification and MD5 hashing.
  * Used Room foreign key cascading (`ForeignKey.CASCADE`) and KSP compiler schema export arguments (`room.schemaLocation`).
* **Why it was executed:**
  * To guarantee data integrity and prevent app crashes or broken UI states when permissions are revoked, photos are deleted outside the app, or categories/events are deleted locally.

---

#### 🔹 Phase 7: Navigation Shell & Photos Timeline
* **What was executed:**
  * Integrated Paging 3 dependencies (`androidx.paging:paging-runtime` and `androidx.paging:paging-compose`) in [libs.versions.toml](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/gradle/libs.versions.toml) and [app/build.gradle.kts](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/build.gradle.kts).
  * Built [MediaStorePagingSource.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/paging/MediaStorePagingSource.kt) to query device MediaStore photos directly with limit/offset pagination, attaching a `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` to automatically invalidate and refresh the timeline when device photos change.
  * Defined timeline data structures in [TimelineItem.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/data/paging/TimelineItem.kt) and created [PhotosTimelineViewModel.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/PhotosTimelineViewModel.kt) with `Pager` date separator transforms (`insertSeparators`) for "Today", "Yesterday", and formatted date headers.
  * Built the floating pill style navigation bar in [FloatingPillNavigationBar.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/components/FloatingPillNavigationBar.kt) featuring elevated capsule styling, dark/light theme support, spring selection animations, and navigation bar inset padding.
  * Built [MainShellScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/MainShellScreen.kt) to host top-level tabs ("Photos" and "Albums") with independent state preservation and smooth tab switching, moving the Settings gear icon to the top bar header of both tabs.
  * Implemented [PhotosTimelineScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt) featuring a 3-4 column toggleable photo grid, full-width section headers, Coil thumbnail downsampling (`.size(300, 300)`), and full-screen viewer overlay with neighbor swipe gestures.
  * Updated [HomeScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) (Albums tab) to adjust bottom grid content padding (`contentPadding = PaddingValues(bottom = 90.dp)`) so items scroll cleanly above the floating pill navigation bar while preserving shared-element card transitions to [DetailScreen.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt).
  * Updated [NavGraph.kt](file:///C:/Users/yahsh/OneDrive/Documents/red-panda/app/src/main/java/com/example/red_panda/ui/navigation/NavGraph.kt) to attach `MainShellScreen` as the starting destination (`Screen.MainShell.route`).
* **How it was executed:**
  * Utilized Paging 3 `Pager` and `PagingSource` querying `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` with `QUERY_ARG_LIMIT` and `QUERY_ARG_OFFSET`.
  * Applied Coil `ImageRequest.Builder` with `.size(300, 300)` and `.scale(Scale.FILL)` for fast thumbnail decoding.
  * Combined Compose `Surface(shape = CircleShape)`, `Modifier.navigationBarsPadding()`, and `animateColorAsState` for the floating pill navbar.
* **Why it was executed:**
  * To provide a Google Photos-style high-performance home screen timeline capable of instantly displaying and scrolling thousands of device photos without memory spikes or UI jank.

---

#### 🔹 Phase 8: Select Mode & Organizing From the Timeline
* **What was executed:**
  * Implemented selection mode on [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt):
    * Moved "Select Photos" and "Untagged Only" options into the top bar 3-dots overflow dropdown menu (`MoreVert`), preventing title truncation and text wrapping.
    * Gesture long-press (`combinedClickable(onLongClick)`) on photo tiles to initiate multi-selection mode.
    * Multi-select photos across different date sections, displaying top-right checkmark overlay indicators on selected tiles.
    * Select-all-in-day functionality on date section headers ("Select All" / "Deselect") to batch select or clear all photos under a date header.
  * Added floating bottom action bar displaying the count of selected photos, an "Add to Event" action button, and a clear selection button.
  * Built [AddToEventDialog.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/AddToEventDialog.kt) allowing users to assign selected photos to an existing event or create a new event (with title, category, and date picker).
  * Added reactive tagged photo indicators in [PhotoDao.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/local/dao/PhotoDao.kt) (`getAllTaggedPhotoUrisFlow()`) and [GalleryRepositoryImpl.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt) (`getTaggedPhotoUris()`), subtly displaying a bookmark badge on already-tagged timeline photos.
  * Integrated "Untagged Only" filter option in the 3-dots overflow menu (with an inline active chip indicator next to the "Photos" header) to filter [MediaStorePagingSource.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/paging/MediaStorePagingSource.kt) and display only untagged photos.
  * Surfaced dismissible suggested event cards at the top of the Photos tab timeline for date-clustered untagged photos with a quick "Create Event" action.
  * Offloaded photo clustering to background coroutine threads (`Dispatchers.IO`) in [PhotosTimelineViewModel.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/PhotosTimelineViewModel.kt) (`loadSuggestedEvents()`) and cached results in `StateFlow`.
  * Added "System Photo Picker" secondary option in top bar overflow menu using `ActivityResultContracts.PickMultipleVisualMedia()`, allowing users with partial permissions to import photos from the system gallery and add them to events.
* **How it was executed:**
  * Utilized Compose `combinedClickable` for gesture-driven long-press selection initiation.
  * Combined Room `Flow` queries (`getAllTaggedPhotoUrisFlow()`) with Compose `snapshotFlow` and Paging 3 `flatMapLatest` in [PhotosTimelineViewModel.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/viewmodel/PhotosTimelineViewModel.kt) for reactive timeline re-paging.
  * Offloaded Room batch insertions (`addPhotosToEvent` / `createEvent`) to `Dispatchers.IO` coroutines.
* **Why it was executed:**
  * To enable fast, effortless organization of device photos directly from the timeline grid into local events and categories.

---

#### 🔹 Phase 10: UX Polish & iOS 26 Frosted Glass Theme
* **What was executed:**
  * Implemented the iOS 26 Frosted Glass Design across the entire application UI for both Dark Mode and Light (White) Mode:
    * **Glass Theme Architecture**: Created reusable glass modifiers in [Glass.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Glass.kt) (`glassCard`, `glassNavbar`) combining translucent background overlays, specular highlight gradient borders, ambient drop shadows, and soft rounded corners.
    * **Dark & Light Mode Companion Glass Palettes**: Updated [Color.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Color.kt) and [Theme.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/theme/Theme.kt) with deep obsidian slate canvas (`#0C0D12`) for dark mode and soft luminous slate-white canvas (`#F2F4F8`) for light mode.
    * **Floating Frosted Glass Navigation Bar**: Redesigned [FloatingPillNavigationBar.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/FloatingPillNavigationBar.kt) with a floating glass capsule container, specular border outline, and active tab glass pill ("Home" and "Albums") with crisp white outline stroke highlights matching the reference mockups.
    * **Albums Tab Glass Cards**: Upgraded [HomeScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) with 22.dp rounded glass album cards, specular highlight strokes, circular glass favorite heart badges, and clean section headers ("Recently", "Other Albums", "See All").
    * **Photos Tab Grid & Rounded Thumbnails**: Updated [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt) to feature 14.dp rounded corner photo grid thumbnails, tracked uppercase section headers ("TODAY", "YESTERDAY"), and a floating frosted glass multi-select action bar.
    * **Frosted Glass Dialogs & Settings**: Upgraded [CategoryDialog.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/CategoryDialog.kt), [EventDialog.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/EventDialog.kt), [AddToEventDialog.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/AddToEventDialog.kt), and [SettingsScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/SettingsScreen.kt) with translucent frosted glass surface containers and specular borders.
  * Implemented predictive back gestures and `BackHandler` routines across the navigation and screen structure:
    * [MainShellScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/MainShellScreen.kt): Pressing back on the Albums tab seamlessly navigates back to the Photos tab before exiting the app.
    * [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt): Pressing back exits photo multi-selection mode, clears active filters, and closes full-screen photo viewer overlays.
    * [HomeScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt), [DetailScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt), and [EventDetailScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/EventDetailScreen.kt): Back gestures cleanly dismiss category/event creation dialogs, delete confirmation popups, and full-screen viewers.
  * Built polished empty states and error recovery states with retry.
  * Implemented photo favorites, timeline filtering, and zero-dependency local JSON backup & restore.
  * Accessibility and branding pass with content descriptions, minimum 48dp touch targets, scalable SP typography, and centralized Android resource strings in [strings.xml](file:///home/joy/Projects/red-panda/app/src/main/res/values/strings.xml).
* **How it was executed:**
  * Created custom Compose `Modifier.glassCard()` and `Modifier.glassNavbar()` using `Brush.linearGradient` specular borders, `Color.copy(alpha = ...)` translucent fills, and `shadow` ambient elevation.
  * Applied `RoundedCornerShape(14.dp)` thumbnail clipping in `PhotosTimelineScreen.kt` and `EventDetailScreen.kt`.
  * Integrated Jetpack Compose `BackHandler`, `animateColorAsState`, and Material 3 `Surface` with `Role.Tab` semantics.
* **Why it was executed:**
  * To elevate the gallery application with an iOS 26 frosted glass visual design featuring translucent glass surfaces, specular highlights, rounded thumbnail grids, floating glass navigation, and full light/dark theme adaptability.

---

#### 🔹 Phase 11: Timeline Correctness & Live Refresh
* **What was executed:**
  * Corrected MediaStore paging keys in [MediaStorePagingSource.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/paging/MediaStorePagingSource.kt) to use absolute row offsets. This prevents gaps and overlaps when Paging uses a larger initial load than subsequent loads.
  * Made timeline ordering deterministic by sorting newest-first on capture time, then date added and MediaStore ID. Refreshes now return to the start of the anchored page instead of misinterpreting a row offset as a page number.
  * Kept MediaStore observer invalidation and Room-driven tagged/favorite filtering, and refreshed the paging source and suggested-event data when the Photos screen resumes.
  * Preserved the grid scroll state and viewer index across configuration changes. Viewer and date-group selection now read only already-loaded Paging snapshots, avoiding accidental loading of the entire library when opening a photo or selecting a date.
  * Hid the floating navigation pill while the full-screen viewer is open.
* **How it was executed:**
  * Used absolute offsets for MediaStore `LIMIT`/`OFFSET` requests and deterministic descending sort columns.
  * Used `rememberLazyGridState` / `rememberSaveable` for UI state and `itemSnapshotList` for non-triggering access to currently loaded items.
  * Validation: `app:testDebugUnitTest` passed (1 test), `app:assembleDebug` completed successfully, and the emulator showed Yesterday above Today; the viewer displayed `1 / 19` without the navigation pill. A 5,000+ photo stress run was not performed.
* **Why it was executed:**
  * To keep the timeline complete and consistently ordered as pages load or data changes, preserve the user's place and selection, and avoid eager loading that could make large libraries janky.

---

#### 🔹 Phase 12: Stable Covers & Event Previews
* **What was executed:**
  * Added persisted `coverPhotoIsManual` state for categories and events. New album covers are assigned from the first successfully added photo and remain unchanged as later photos are appended; cover edits are marked manual, and resetting uses the stable first photo.
  * Repaired removed or inaccessible covers by selecting the next available photo and clearing the manual flag. Removing a cover from an event shows a snackbar explaining that a replacement was selected.
  * Added an explicit `position` to event-photo links. New photos append at the next position, event photo queries and album mosaics use that order, and imported event links retain their JSON order.
  * Added the Room 1→2 migration in [AppDatabase.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/local/AppDatabase.kt): it backfills the currently displayed category covers, initializes event covers from the current first preview photo, and preserves existing preview order. Removed destructive migration fallback so an unsupported upgrade cannot silently erase gallery data.
  * Made album and event queries observe event-photo link changes so preview counts and photos refresh when links are added or removed.
  * Removed fabricated 45-photo event counts and placeholder colors. Empty preview slots now use a neutral themed image placeholder, and `+N` is based on the real event photo count.
* **How it was executed:**
  * Stored ordering and cover metadata in Room entities and DAO queries; handled cover assignment and fallback in [GalleryRepositoryImpl.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt).
  * Validation: `app:assembleDebug` succeeded, `app:testDebugUnitTest` passed (1 test), and Room generated the version-2 schema. The repeated-add cover check was not manually run on a device.
* **Why it was executed:**
  * To prevent album covers and event mosaics from changing when users add photos, while ensuring existing data survives the schema upgrade and empty slots do not resemble failed image loads.

---

#### 🔹 Phase 13: Dialogs, Theme & Navigation Overlap
* **What was executed:**
  * Rebuilt Add to Event, album create/edit, and event create/edit dialogs with Material theme surface/content tokens. The Add to Event tabs, radio buttons, and Add action share the amber accent; its disabled state is visibly muted.
  * Constrained dialogs to the available display height, made form content scrollable while keeping actions pinned, bounded the event list, and truncated long event and album names without clipping rows. Creating an event from the dialog now requires a real selected album instead of falling back to an invalid ID.
  * Kept delete confirmation dialogs on the same Material surface tokens, and verified the suggested-event cards already use theme-derived surfaces, text, and controls.
  * Reserved a bottom viewport area on the album and photo grids so visible rows cannot render underneath the navigation pill, moved the selection action bar above the pill, and replaced timeline system toasts with in-app snackbars positioned above it. The full-screen viewer continues to hide the pill.
  * Replaced low-contrast coverless album tiles with a themed placeholder and theme-aware text and controls.
  * Added AndroidX SplashScreen support and held the splash until DataStore emits the saved theme, so the app's first visible content uses the selected theme instead of briefly rendering Photos in the default theme.
* **How it was executed:**
  * Centralized dialog foreground/background colors through `MaterialTheme.colorScheme`; used bounded scroll containers and fixed action rows for small displays.
  * Validation: `app:assembleDebug` succeeded and `app:testDebugUnitTest` passed (1 test). Emulator checks covered the Photos viewport and select-mode action bar against the pill, the Add to Event existing/new tabs with pinned actions and fully visible event rows, and Settings/Photos in light and dark themes. The full device-size matrix was not run.
* **Why it was executed:**
  * To keep dialogs legible and usable across themes and screen sizes, and prevent persistent navigation or feedback overlays from obscuring gallery content.

---

#### 🔹 Phase 14.5: Below-Cover Compact Filter Card, Truly Floating Switcher & Full-Screen Viewer Strip Hiding
* **What was executed:**
  * **Below-Cover Compact Filter Card**: Moved the Places and People filter card (`FloatingFilterCard`) in [DetailScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/DetailScreen.kt) directly below the album cover photo instead of overlapping on top of it. Reduced its height/padding (`vertical = 8.dp`) and font size (`14.sp`) to a compact `0.85f` max-width card.
  * **Truly Floating Photo/Album Switcher**: Updated [FloatingPillNavigationBar.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/FloatingPillNavigationBar.kt) so the navigation pill is explicitly measured to its content (`wrapContentSize` with unbounded child constraints), not a full-width tray; its glass background and shadow remain limited to the capsule.
  * **Full-Screen Photo Viewer and Image Navigation**: Updated [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt) to draw `FullScreenPhotoViewer` above the entire Scaffold, hiding the Photos app bar (title, overflow, grid, and settings actions) while viewing an image. The pager supports horizontal navigation with right = next and left = previous, and retains pinch-to-zoom.
* **How it was executed:**
  * Re-ordered layout hierarchy in `DetailScreen.kt` using separate `item` entries for `HeroHeader` and `FloatingFilterCard`.
  * Applied content-sized measurement to the switcher, configured the viewer pager's direction, and moved `FullScreenPhotoViewer` outside the Scaffold content in `PhotosTimelineScreen.kt`.
  * Validation: `app:assembleDebug` completed successfully.
* **Why it was executed:**
  * To prevent the filter card from obscuring cover photos, convert the bottom navbar into a truly floating island pill, and hide the top app bar strip when viewing photos in full-screen.

---

#### 🔹 Phase 15: Real Data Only, Working Events & Truly Floating Switcher Over Content
* **What was executed:**
  * **Removed All Seeded Sample & Mock Data**:
    * Removed `mockAlbums` dataset from [Album.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/model/Album.kt).
    * Removed first-launch database seeding routines (`seedInitialDataIfEmpty()`) from [RedPandaApplication.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/RedPandaApplication.kt), [GalleryRepository.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepository.kt), and [GalleryRepositoryImpl.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/data/repository/GalleryRepositoryImpl.kt).
    * Guaranteed that fresh installations start completely empty, displaying Phase 10 empty state placeholders until real albums or events are created by the user.
  * **End-to-End Working Suggested Events Flow**:
    * Built [SuggestedEventDialog.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/SuggestedEventDialog.kt) enabling users to confirm event titles (prefilled with formatted dates, eliminating raw folder names like `"photos"` or `"Local Photos"`), preview untagged photos, and select or create destination albums.
    * Connected suggested event creation in [HomeScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) and [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt) to insert exact untagged photo references into Room, clear those items from the untagged suggested pool, and present in-app confirmation snackbars (`"Event '$title' created in '$albumName'!"`).
    * Updated default location attributes to clean empty strings (`""`) instead of hardcoded strings like `"Local Gallery"` or `"Local Photos"`.
    * Ensured each real album is uniquely listed in its designated section ("Recently" / "Other Albums").
  * **Truly Floating Photo & Album Switcher Layout Fix**:
    * Configured [HomeScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/HomeScreen.kt) and [PhotosTimelineScreen.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/screens/PhotosTimelineScreen.kt) `Scaffold` elements with `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
    * Removed fixed bottom padding from `LazyVerticalGrid` `Modifier` and applied `contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 90.dp)` directly to grid containers.
    * Extended the grid viewports all the way to the bottom edge of the display window. Albums and photos now scroll directly behind and to the sides of [FloatingPillNavigationBar.kt](file:///home/joy/Projects/red-panda/app/src/main/java/com/example/red_panda/ui/components/FloatingPillNavigationBar.kt) instead of rendering on top of a solid black bar.
* **How it was executed:**
  * Purged mock dataset definitions and seeding routines across the application architecture.
  * Created `SuggestedEventDialog.kt` and wired it into `HomeViewModel` and `PhotosTimelineViewModel` with coroutine snackbar confirmations.
  * Shifted viewport bounds to fill `fillMaxSize()` and placed insets inside `LazyVerticalGrid` `contentPadding`.
  * Validation: Executed `app:assembleDebug` and `app:testDebugUnitTest` (all unit tests passed successfully).
* **Why it was executed:**
  * To ensure all application data is 100% user-generated on fresh installs, fix suggested event flow routing with explicit album destinations and clean titles, and make the navigation pill switcher truly float over scrolling gallery content without a black bar underneath.

---

#### 🔹 Phase 16: Navigation, Home Layout & Suggested Event Dismissal
* **What was executed:**
  * Replaced route-level shared-element transitions with consistent 280 ms slide/fade transitions for forward navigation and Back. Disabled Android’s predictive-back callback preview so the system Back gesture and in-app Back use the same route animation. Bottom-tab changes use a 130 ms fade.
  * Moved the newest active suggested event into the Photos timeline grid as its first full-span item, removed duplicated top inset padding, set date-header spacing to 16dp, and tied bottom scroll padding to the floating-nav height. Raised the Photos selection action bar, including “Add to Event,” by 16dp.
  * Added stable suggestion keys based on the local date group and sorted MediaStore photo IDs. Persisted dismissed keys in DataStore; Photos shows only the newest active suggestion while Albums shows all active suggestions, and both cards can be dismissed. Made the Albums suggestion carousel snap with cards constrained to about 85% of screen width.
  * Updated the grid/list control icons and refined the floating photo/album switcher with a slightly translucent surface and border in both light and dark themes. Gave the NavHost and screen roots opaque theme backgrounds.
  * Removed the cover-color palette from the New Album dialog while retaining the existing default cover color. Updated user-facing app branding and exported-backup metadata to “Ouro.”
  * Added horizontal swipe paging to the album slideshow: swiping left advances and swiping right returns to the previous photo, while preserving autoplay and the previous/next controls. In the shared full-screen photo viewer, horizontal swipes page unless the photo is zoomed in.
  * New events created from selected photos now default to the latest available photo capture date from MediaStore metadata (`DATE_TAKEN`, with `DATE_ADDED` as fallback) rather than today. If no usable date is available, the field stays blank and requires manual selection.
  * Reconciles missing photo references on app start and after MediaStore changes when full photo access is available; removed the manual cleanup button. Current-year timeline headers omit the year, and Coil retains its EXIF-aware decoding for MediaStore images.
  * Changed the installed application ID to `com.yahshua.ouro`, while retaining `com.example.red_panda` as the Kotlin source namespace; updated the instrumented-test package expectation accordingly. Rebuilt the debug APK as `APKs/ouro-1.0.0.apk`.
  * Added videos to the Photos timeline by paging images and videos together from MediaStore, sorted by capture date. Video tiles show generated frame thumbnails and a play badge; image and video permissions are requested together on supported Android versions.
  * Updated Photo Picker flows to select both images and videos when importing to the timeline or attaching to an event. Persisted picker URI read access so selected media remains available to albums after the picker session.
  * Reused album/event media references for video attachments without a database schema change. Event grids and album mosaics display video thumbnails; full-screen photo viewing and album slideshows now play videos with native playback controls, and album autoplay pauses on video pages. Missing-media hash recovery now checks video items too.
* **How it was executed:**
  * Applied shared route transitions to each NavHost destination and adjusted the Android back callback setting for matching Back behavior.
  * Centralized suggestion IDs and dismissed-ID persistence in the existing repository/DataStore path, then wired both screens to the shared active suggestion list.
  * Updated the album slideshow to use a horizontal pager, added zoom-aware gesture handling to the full-screen pager, and queried selected-media date metadata before initializing the Add to Event dialog. Added generated video thumbnails and native VideoView playback to the timeline, event, and slideshow viewers.
  * Validation: `app:assembleDebug` and `app:testDebugUnitTest` succeeded after the video support changes (1 unit test passed).
* **Why it was executed:**
  * To make navigation and photo browsing feel consistent, improve the placement and appearance of gallery controls, simplify album creation, keep branding consistent, and ensure event dates reflect when selected photos were captured.

---

#### 🔹 Phase 17: Continuous Photos Wall & Scroll Date Bubble
* **What was executed:**
  * Removed date separator items from the Photos timeline so photos and videos render as one continuous wall, without date-group headers or gaps.
  * Replaced the right-edge drag scrubber with a small, centered bubble at the top of the photo wall. It appears only while the grid is scrolling and fades away when scrolling stops.
  * The bubble displays the localized weekday, day, month, and year for the first visible photo row. It uses the photo's `takenAt` value, which already falls back to MediaStore's added date, in the device's local time zone.
  * Replaced the Photos app-bar title with the Ouro wordmark tinted to the current theme, then shifted the logo 8dp left for closer alignment with the reference.
  * Removed the grid-toggle and settings actions circled in the supplied reference image.
  * Changed the timeline to a three-column staggered photo/video layout, sizing each tile from its MediaStore width and height to preserve the original media aspect ratio.
  * Persisted the Albums layout switch selection across tab changes and app restarts, rendering album cards at 1:1 in square mode and 2:1 in wide mode until switched again.
  * Removed the Suggested Event card from the Photos timeline so it no longer interrupts the continuous photo/video wall.
  * Added a dismiss-all X to the Suggested Events header in Albums; dismissing it saves the hidden suggestions.
* **How it was executed:**
  * Looked up the visible photo date from the existing Paging snapshot, with no extra MediaStore query during scroll. The bubble label is recomputed only when the visible photo index or date changes.
  * **Validation:** `:app:assembleDebug` succeeded, `:app:testDebugUnitTest` passed (1 unit test), and the updated screen was launched on the emulator.
* **Why it was executed:**
  * To match the supplied Ouro gallery reference: a continuous, naturally proportioned photo/video wall with temporary date context during scrolling.
