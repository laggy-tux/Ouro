# Ouro – Performance & Polish Instruction Set

For the coding agent. Stack: Kotlin, Compose, Room, Coil, DataStore (package `com.yahshua.ouro`). Source: the Nov 6 screen recording plus the three bugs the owner reported.

## Ground rules

- Do not change the visual design, copy, or data model unless a fix below says so. Prefer a file-based cache over a Room schema change.
- Stay fully offline-first. No network calls.
- Measure first. Do all timing on a **release build** (R8 on, not debuggable); debug Compose is much slower and will mislead you.
- One fix per commit. After each, report what you changed, what you measured before and after, and anything you could not verify.
- Test with the real worst case: the "scenes" album (176 items) and its event "25-10-26 to 26-10-26" (101 items, mostly videos).

## Order of work

1. Baseline measurements (A0)
2. Thumbnail pipeline (A1)
3. Album grid scrolling (A3) and event photo ordering (A4)
4. Tab switching (A2)
5. Polish items (B1–B7)

## A. Reported bugs

### A0. Baseline

Add JankStats (or a Macrobenchmark) and log: time to first visible thumbnail for a video, dropped frames while flinging an album grid, and dropped frames during a Home ↔ Albums switch. Record numbers before touching anything.

### A1. Video thumbnails are slow and never cached

**Observed:** In the album and event screens, video tiles stay as dark placeholders for many seconds. The cover tile of the 101-item event stays blank on the album screen. Scrolling away and back, or leaving and re-entering, blanks them again, so nothing is being reused.

**Likely causes to check:** video frames decoded at full resolution; Coil's disk cache does not store frames decoded from local content URIs, so the "Coil disk cache" claimed in Settings may not apply here; more than one `ImageLoader` instance, so memory caches are not shared; `ImageRequest` rebuilt on every recomposition without a stable cache key; unbounded parallel decoding.

**Do this:**

1. Create a single app-wide `ImageLoader` (Application class or DI) with a memory cache and a disk cache, and make sure every screen uses it.
2. Build a `ThumbnailRepository` that generates and persists thumbnails itself:
   - Source: `ContentResolver.loadThumbnail` on API 29+, with `MediaMetadataRetriever` (scaled frame) as a fallback.
   - Size: roughly the tile size in px, with a second, larger size for covers and mosaic hero tiles.
   - Take the video frame around 1s in (or \~10% of duration), not frame 0, to avoid black frames.
   - Store as WebP (quality \~70) in `cacheDir/thumbs/`, keyed `mediaId_dateModified_size`.
   - Invalidate automatically when `dateModified` changes.
3. Plug it into Coil with a custom `Fetcher` and `Keyer`, so the same cache serves images and videos.
4. Decode on `Dispatchers.IO` with limited concurrency (a `Semaphore` of 3–4) and de-duplicate in-flight requests for the same key.
5. Prefetch: when an album or event screen opens, queue the cover thumbnail first, then the first \~60 items. Prefetch the next \~30 items ahead of scroll direction.
6. Wire Settings → "Clear Image Cache" to clear Coil's caches **and** `thumbs/`, and show the cache size. Fix the Settings description if it ends up inaccurate.

**Done when:** a previously seen video thumbnail appears in under \~100 ms, including after an app restart; a first-time thumbnail appears in under \~1 s; the 101-item event shows no blank tiles after a few seconds at rest.

### A2. Home ↔ Albums switching is laggy

**Observed:** noticeable stutter on tab switch. In the light-theme frames, **two bottom nav pills are visible at once** during the transition, which suggests each tab has its own nav bar inside its own Scaffold.

**Check and fix:**

1. Hoist the bottom nav into one top-level Scaffold so it is never duplicated or animated per screen.
2. Use `navigate` with `launchSingleTop`, `restoreState` and `saveState` for the two top-level tabs, so scroll position and state survive.
3. Scope the Home and Albums ViewModels so they are not recreated on every switch. Expose data as `StateFlow` with `stateIn(WhileSubscribed(5000))` and collect with `collectAsStateWithLifecycle`.
4. Move grouping, sorting and date bucketing off the main thread (`Dispatchers.Default`); keep that logic out of composables.
5. Hoist `LazyStaggeredGridState` / `LazyGridState` so returning to Home does not reset or rebuild the grid.
6. Use no transition between top-level tabs, or a short (≤150 ms) fade. Do not animate whole screens.

**Done when:** a tab switch shows no ghost pill, no dropped frames beyond a handful, and Home returns at the same scroll position.

### A3. Scrolling through album photos lags

**Observed:** while scrolling event and album grids, whole rows of tiles are blank for a while, then pop in.

**Check and fix:**

1. Every `ImageRequest` must set `size` to the tile's pixel size. Never decode full-resolution images for a grid.
2. Give `items()` stable `key = mediaId` and a `contentType`.
3. Make list parameters stable: use an `@Immutable` wrapper or `kotlinx.collections.immutable`, so tiles do not recompose on every parent change.
4. Disable or shorten the per-tile crossfade; hardware bitmaps on.
5. Anything driven by scroll (the timeline scrubber, the date bubble) must read scroll state through `derivedStateOf` or inside `Modifier.graphicsLayer` / draw phase, never directly in composition near the top of the tree.
6. Do not build the same grouping list on each recomposition; compute once in the ViewModel.
7. Add a Baseline Profile and confirm R8 keeps Compose and Coil rules intact.

**Done when:** a fast fling through the 176-item album drops few frames and tiles are filled in at rest within about a second.

### A4. Photos inside an event must be sorted by when they were taken

**Required behavior:** inside every event and album, order photos and videos by their capture date. The date the album was created, or the date a photo was added to it, must never affect the order. Keep the current direction (newest first unless the app already has a sort setting).

**Do this:**

1. Use this sort key: `DATE_TAKEN` from MediaStore; if missing, EXIF `DateTimeOriginal` (images) or the container's creation time (videos); if still missing, the file's modified time. Break ties by media ID so the order is stable.
2. Store the capture time with each photo reference (for example a `takenAt` column), filled at import and sync. If the app already stores one, reuse it. If a column is added, write the Room migration and backfill existing rows off the main thread.
3. Sort in the DAO query (`ORDER BY takenAt DESC, mediaId DESC`), not in composables.
4. Photos added to an event later must land at their real date position, not at the start or end of the list.
5. Use the same order for the event's mosaic preview (the first photos shown, +N count) and for the Play/slideshow button on the album cover.
6. Do not change the album cover rule: the first photo becomes the cover only when the album was empty, and it stays until changed manually.

**Done when:** in the "25-10-26 to 26-10-26" event the photos run strictly by capture time; adding an older photo places it at its true date position; the order survives an app restart; and a test photo with no `DATE_TAKEN` is placed by its fallback date instead of jumping to the top or bottom.

## B. Other problems seen in the recording

**B1. Light theme looks empty.** Unloaded tiles use a placeholder color that is nearly invisible on the light surface, so Home appears as floating images with empty space and a date bubble. Use a theme-aware placeholder (`surfaceVariant` or similar) on Home, album, and event grids, and check Albums too.

**B2. Possible theme mismatch.** Settings showed "Dark Theme" selected, and the next frame (Albums) rendered in light. This may be a real change made mid-recording, so verify. Confirm the theme comes from a single DataStore flow at the app root, applies before the first frame (no white flash on cold start), and that the Settings radio always reflects the active theme.

**B3. Inconsistent video badge.** Home tiles show a small badge in the top-left; album and event tiles show a play triangle at the bottom-right. Make one shared `VideoBadge` composable (play icon plus duration, bottom-end) and use it everywhere. Do not show it over an unloaded placeholder.

**B4. Event cover tiles that are videos.** The big hero tile in the event mosaic on the album screen (e.g. "25-10-26 to 26-10-26") stays blank. It must use the same thumbnail pipeline at the larger cover size and be prefetched when the album screen opens (see A1).

**B5. Floating nav pill overlaps content.** Tiles scroll under the pill with no scrim, and the last row can be hidden. Add bottom content padding equal to the pill height plus margin. Raise contrast on the unselected "Home"/"Albums" label in light theme.

**B6. FAB covers grid content.** The "+" button in album and event screens sits over the last column. Add bottom content padding to the grid so the final row can scroll fully clear of it.

**B7. Settings accuracy.** The "Thumbnail Cache" card claims Coil disk and memory caching. After A1, make the text and the Clear button match what actually exists.

## Report format

For each item: status (done / partial / not reproducible), the commit, before/after numbers from the release build, and open questions. If something in this list does not match the code, say so instead of guessing.
