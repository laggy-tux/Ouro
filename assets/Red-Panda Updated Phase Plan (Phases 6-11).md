# Red-Panda: Updated Phase Plan (Phases 6-11)

Phases 1-5 are done. Each phase must compile and run before the next starts, and each ends with a "done when" check you can test on a real device.

## Phase 6: Data Integrity & Photo Access Hardening

**Goal:** the library can't get into a broken state, and the timeline will have a safe foundation.

- Handle Android 14+ partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED`), plus denied and revoked permission paths, with a clear in-app prompt to change access.
- Missing-photo handling: placeholder when a MediaStore reference goes stale, re-resolve by hash, and a "remove missing photos" cleanup.
- Room safety: foreign keys with cascade deletes, indices on the join table, schema export on, and a migration setup in place before any further schema change.
- Delete behavior: removing a category or event never touches the real photos, and the confirm dialogs say so.
- Cover fallback when a custom cover photo disappears from the device.

**Done when:** you can revoke permission, delete photos in the system gallery, and delete categories without crashes or orphaned rows.

## Phase 7: Navigation Shell & Photos Timeline

**Goal:** the Google Photos-style home, built to handle 5,000+ photos from day one.

- Bottom navigation with two top-level tabs, Photos and Albums, in the floating pill style from the render. It needs dark and light variants.
- Restructure navigation so each tab keeps its own back stack, and move the settings gear to a sensible spot in the new layout.
- Paged MediaStore source for the timeline. It reads directly, not through Room, and refreshes when the device library changes.
- Date section headers (Today, Yesterday, then by date or month), a 3-4 column grid, and stable keys.
- Thumbnail sizing and Coil cache tuning so the grid never decodes full-size images.
- Full-screen viewer reading from the paged source, with swipe between neighbors.
- Make sure the Albums shared-element transitions still work inside the tab structure.
- Library approval needed: Paging 3 (`androidx.paging` runtime and Compose).

**Done when:** a 5,000-photo library opens quickly, scrolls without jank or memory spikes, and switching tabs and rotating preserve scroll position.

## Phase 8: Select Mode & Organizing From the Timeline

**Goal:** make organizing photos fast, which is the app's reason to exist.

- "Select" in the Photos header, with long-press to start, multi-select across date sections, and a select-all-in-day option.
- Bottom action bar: "Add to event" (choose an existing event or create one), and favorite.
- Mark already-tagged photos subtly in the timeline, with a filter for "untagged only".
- Surface suggested events on the Photos tab as a dismissible card: "Create event from these N photos".
- Move clustering to the background and cache results, so it doesn't rescan the whole library at every launch.
- Keep the old system photo picker as a secondary option for people with limited access.

**Done when:** you can select 50 photos spread across several days and add them to a new event in under a minute, and they show up in the right event.

## Phase 9: Web Viewer

**Goal:** read-only browser access on the local network.

- Foreground service with a persistent notification, a Ktor CIO server, and a settings toggle (off by default).
- Local URL, QR code, and a random 6-digit PIN with a session token, plus rate limiting on PIN attempts.
- Read-only endpoints for categories, events, thumbnails, and full images, serving a responsive page that follows light and dark theme.
- INTERNET permission added only now. The server stops when toggled off or the app closes.
- Library approvals needed: Ktor and a QR generator.

**Done when:** a laptop on the same Wi-Fi can authenticate, browse everything, and lose access immediately when the server is toggled off.

## Phase 10: UX Polish & Missing Pieces

**Goal:** it feels finished.

- Empty states (no photos, no categories, no events) and error states with retry.
- Back gestures and predictive back across the tab structure.
- Photo favorites, and search or filters if time allows.
- Backup and restore of the organization data (export and import file) from Settings.
- Accessibility pass: content descriptions, touch targets, font scaling.
- App icon, splash, and all strings moved into resources.

**Done when:** a first-time user can open the app with a full device library and an empty app, and understand what to do without instructions.

## Phase 11: Testing & Release Prep

**Goal:** safe to publish.

- Unit tests for the repository, clustering logic, selection logic, and viewmodels. Instrumented tests for Room migrations and the main flows.
- Release build with R8 on, keep rules verified for Room and Ktor, and a signing config.
- Test on at least three devices or emulators: small screen, large screen, and Android 8 vs 14+.
- StrictMode, crash and ANR check, and a battery check for the web server service.
- Privacy statement (offline, no tracking), open-source license screen, and store listing basics if publishing on Play.

**Done when:** a signed release APK installs cleanly on a fresh device and passes the full manual checklist.

## Order and risk if skipped

| Phase | Risk if skipped |
| --- | --- |
| 6 | Data loss, crashes, and a broken timeline under partial access |
| 7 | No new home screen, or a laggy one on big libraries |
| 8 | A timeline you can only look at, not organize from |
| 9 | Missing headline feature |
| 10 | Feels like a prototype |
| 11 | Unreliable release |

## If time is short

Phases 6, 7, and 8 are the core: they turn the app into a usable daily gallery. Phase 9 can slip after Phase 10 if a polished offline app should ship first.

## Library approvals needed

| Library | Needed for | Phase |
| --- | --- | --- |
| Paging 3 | Timeline | 7 |
| Ktor server | Web viewer | 9 |
| QR code generator | Web viewer | 9 |
