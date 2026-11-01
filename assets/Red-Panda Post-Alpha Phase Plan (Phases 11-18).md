# Red-Panda: Post-Alpha Phase Plan (Phases 11-18)

Phases 1-8 and 10 are done. This plan finishes the app: Phases 11-13 fix what the alpha-1 recording showed, Phase 14 rebuilds the People and Places album screen to match your reference image, Phase 15 makes the data real, Phase 16 brings the web viewer back in (it was parked for after beta), and Phases 17-18 close every remaining gap and ship. The app ships with no sample albums, events, or seeded photos, so a fresh install starts empty. Each phase must compile and run before the next starts, and each ends with a done-when check you can test on a real device.

## What the recording showed

Timestamps are approximate. You means you reported it; Found means I spotted it in the video. Items marked *verify* are things I can see happening but whose cause I can't tell from a recording alone.

| # | Issue | Seen at | Source | Fixed in |
| --- | --- | --- | --- | --- |
| 1 | Timeline is unreliable: after creating the suggested event, the 9 pixel-art photos vanish from Photos and Today/Yesterday swap order. They come back, in the original order, after the theme is toggled in Settings, which points to a stale list that only fixes itself when the screen is recreated. | 0:15-0:20, 1:20-1:25 | You + Found | 11 |
| 2 | Album cover changed on a non-empty album. Anguilla's cover was the pixel-art image; after 6 peacock photos were added it became a peacock (card and hero). | 0:55-1:02 | You | 12 |
| 3 | Adding photos to an event rewrites its preview. In the photos event the peacocks took the large tile and pushed the earlier pixel-art photos out of view (+4 became +10). The earlier photos are still there, but the preview makes it look like they were replaced. | 0:55-1:02 | You | 12 |
| 4 | Add 6 Photos to Event dialog: the title is dark text on a dark surface and nearly unreadable, the event list is cut off mid-row (Amalfi Coast Drive is sliced), and the selected tab is orange while the radio buttons and Add button are white. | 0:48-0:56 | You | 13 |
| 5 | The album screen and its Places and People filters don't match your reference image: two loose pill chips instead of a floating filter card, each event boxed in its own card with a three-dot menu, a Local Photos subtitle with no place, and no slideshow button on the cover. | 0:25-0:33, 1:00-1:05 | You | 14 |
| 6 | Suggested event card said 6 untagged photos and a toast said the event was created for Nov 1, 2026, but no event with those 6 peacocks appears in Anguilla afterwards (still 13 photos). The 6 peacocks were still untagged and had to be added by hand. *verify* | 0:14-0:20, 0:55 | Found | 15 |
| 7 | Floating Home/Albums pill sits over content: it covers the bottom row of the album grid and the timeline, stays on top of the full-screen photo viewer, and toasts overlap it. In select mode the action bar and the pill stack as two floating bars. | throughout | Found | 13 |
| 8 | Library looks like test data: two albums named Anguilla, several San Francisco and Happy New Year albums with 45 photos but black placeholder covers, and repeats in the Recently row. | 0:25-0:45 | Found | 15 |
| 9 | Event names look auto-generated and inconsistent: photos (looks like a raw folder name) versus Event on Oct 31, 2026. | 0:27 | Found | 15 |
| 10 | Light theme: albums without a cover become flat grey washes with white text (weak contrast). Cold start also showed a blank white Photos screen with a spinner before the dark theme appeared. *verify* | 0:08, 1:20-1:35 | Found | 13 |
| 11 | Event previews with fewer than 5 photos show a dark empty tile that reads like a missing image. | 0:27-0:32 | Found | 12, 14 |
| 12 | The grid icon in the headers and See All were never used in the recording, so I can't tell whether they work. *verify* | throughout | Found | 17 |

## Phase 11: Timeline Correctness & Live Refresh

**Goal:** the Photos timeline always shows the right photos, in the right order, and updates itself.

- One sort rule for sections, newest first, so Today is always above Yesterday and nothing flips after an action.
- Find why photos disappear after Create Event and reappear only after the screen is recreated. Check for a stale cached list, a filter left on by the suggestion flow, and a list that isn't observing Room or MediaStore changes.
- Make the timeline reactive: adding to an event, creating an event, favoriting, and deleting all update it immediately, with no manual refresh and no theme toggle needed.
- Keep scroll position and selection state across those updates and across rotation.
- Full-screen viewer: its count (14 / 19) must match the visible timeline, and the bottom pill must hide while the viewer is open.
- Re-test with a large library (5,000+ photos) so the fix doesn't bring back jank.

**Done when:** you can create an event, add photos to events, and toggle the theme, and the timeline shows the same photos in the same order every time without leaving the screen.

## Phase 12: Stable Covers & Event Previews

**Goal:** the app never silently changes something you already set.

- Album cover rule: when an album has no photos and the first photos are added, the first photo becomes the cover. From then on the cover only changes when you change it manually (or when the cover photo itself is removed, in which case fall back and tell you).
- Store the cover explicitly (cover photo id plus a flag for set manually or automatically) instead of deriving it from the first photo in the list. This is a Room schema change, so it needs a migration, and existing albums should be backfilled with whatever cover they currently show so nothing changes on upgrade.
- Event previews use a stable order (the order photos were added, with new photos appended, not inserted at the front), so adding photos never pushes earlier ones out of the collage. The +N count stays accurate.
- Event cover follows the same rule as album cover.
- Show an empty slot as a clean placeholder instead of a dark tile that looks like a failed image.

**Done when:** you can add photos to an existing album or event five times in a row and the album cover and the first tiles of every event preview stay exactly the same, until you change the cover yourself.

## Phase 13: Dialogs, Theme & Navigation Overlap

**Goal:** every popup and overlay looks like the same app, in light and dark.

- Rebuild the Add to Event dialog on theme tokens only: readable title, one accent colour for tabs, radio buttons, and the Add button, and a disabled state that is clearly disabled.
- Make the event list scrollable with a fixed max height, with the Cancel and Add row pinned so no row is ever cut off, and a long event name wraps or truncates cleanly.
- Do the same pass on the New Event tab, create, rename, and delete dialogs, and the suggestion card.
- Add bottom content padding equal to the pill height so the last row is never hidden, hide the pill in the full-screen viewer, and place toasts and the select-mode action bar so they don't overlap the pill.
- Light theme: give cover-less album cards a placeholder with enough contrast, and check every screen once in both themes.
- Cold start: apply the saved theme before the first frame (splash or preloaded setting) so there is no white flash. *Verify first.*

**Done when:** every dialog and the select-mode bar are readable and consistent in light and dark, nothing is clipped on a small phone, and nothing sits under the pill.

## Phase 14: People & Places Layout

**Reference image needed:** send the People & Places screenshot you shared when you start this phase. It is the only phase that depends on it.

**Goal:** the album screen, with its Places and People filters and its events, matches the reference image in layout, in light and dark.

- Cover: full-width image with rounded bottom corners running under the status bar, the album name centred on it with the photo count underneath, a back button top-left, and one menu button top-right. Favorite, add event, edit, and delete move into that menu, so nothing is lost.
- Slideshow: a play button at the bottom-right of the cover that starts a simple auto-advancing slideshow of the album's photos.
- Filter card: replace the two loose pill chips with one floating rounded card that overlaps the bottom of the cover. Places and People sit side by side with a thin divider and a dropdown arrow on each. The active one is dark text, the inactive one is muted.
- Make the filters work. Places lists the places used by this album's events, People lists the people names, and picking one shows only matching events, with an All option to clear it. Place and people are plain text fields on an event that you fill in yourself, so the app stays offline with no online lookup and no face recognition. This needs a Room migration (a new field on events), handled the same way as the Phase 12 cover migration.
- Event sections replace the boxed event cards: no card background; the event name in bold, a muted line under it with the date range and place (for example Dec 5-26, North Treva, Switzerland, instead of Local Photos), and a chevron on the right that opens the event. Rename and delete move to the event screen and a long-press menu, replacing the per-event three-dot button.
- Photo mosaic under each event: one large tile on the left spanning two rows, a 2x2 grid of small tiles on the right, rounded corners, small equal gaps, and the last tile darkened with +N when there are more photos. With fewer than 5 photos the tiles fill the space instead of leaving an empty dark tile. Order follows the Phase 12 stable order.
- Event create and edit dialogs get Place and People fields, built on the Phase 13 theme tokens.
- Light and dark: the reference is light, so the dark version uses the same layout with the app's dark tokens for the filter card, text, chevrons, and the +N overlay.
- If time allows: the cover shows the edges of the neighbouring albums and swipes between them, as in the reference.

**Done when:** the album screen laid next to the reference image matches in layout (cover, floating filter card, open event sections, mosaic) in both themes, and choosing a place or person shows only the matching events.

## Phase 15: Real Data Only & Working Events

**Goal:** every album, event, and photo link in the app was created by the user, and the suggested-event flow does what its card says.

- Remove all seeded sample albums, events, and photo links, including any first-launch seeding code, from every build. A fresh install starts empty and shows the empty states from Phase 10. There is no cleanup tool and no sample-data setting to maintain.
- On your test phone, clear the app's storage once (or reinstall) so the old placeholder rows from earlier builds are gone.
- Trace the suggested-event flow end to end (the card, the toast, and where the event ends up). The photos on the card must be exactly the ones in the created event, and the event must be visible in its album afterwards.
- Suggested events need an album destination (existing or new), and the toast should say where the photos went.
- Better default event names: the date, plus the place once you have set one in Phase 14, with no raw folder names like photos, and an easy rename right after creating.
- The Recently row lists each real album once.

**Done when:** after a fresh install, Albums and Photos show nothing you didn't create, and accepting a suggestion produces a named event containing exactly the suggested photos, visible in its album.

## Phase 16: Web Viewer

**Goal:** read-only browser access to your albums on the local network, so the headline feature ships with the app instead of after it.

- Foreground service with a persistent notification, a Ktor CIO server, and a settings toggle (off by default).
- Local URL, QR code, and a random 6-digit PIN with a session token, plus rate limiting on PIN attempts.
- Read-only endpoints for albums, events, thumbnails, and full images, serving a responsive page that follows light and dark theme.
- The browser shows the same covers, event order, and place and people details as the phone, using the Phase 12 and Phase 14 rules, so the two never disagree.
- INTERNET permission added only in this phase. The server stops when toggled off or when the app closes, and it never writes or deletes anything.

**Done when:** a laptop on the same Wi-Fi can authenticate, browse everything, and lose access immediately when the server is toggled off.

## Phase 17: Completeness Pass, No Dead Controls

**Goal:** every visible control works, and every flow can be finished from a fresh install.

- Tap everything on every screen: the grid icon, See All, three-dot menus, hearts on cards, the new album-screen menu and slideshow button, and every row in Settings. Each one either works or is removed.
- Run the full flow from a fresh install: grant access, browse the timeline, create an album, create an event with a place and people, add photos from the timeline, filter by place and person, change a cover manually, favorite, delete an event and an album (the confirm dialog says your photos are untouched), back up and restore, then turn the web viewer on and browse.
- Backup and restore (if built in Phase 10) must also bring back the cover choice from Phase 12 and the place and people fields from Phase 14.
- Permission paths: partial access, denied, and revoked while the app is running.
- Anything half-built or marked TODO is either finished or cut from the release.

**Done when:** you can use the app as your only gallery for a full day with no crash, no dead button, and no manual workaround.

## Phase 18: Testing & Release

**Goal:** a finished app that is safe to publish.

- Regression tests for every bug in the table above: cover stays fixed after adds, event preview order is stable, timeline order and contents after create, add, and theme change, dialog layout on a small screen, and place and people filtering.
- Unit tests for the repository, clustering logic, selection logic, and view models. Instrumented tests for Room migrations (including the Phase 12 cover and Phase 14 place and people migrations) and the main flows.
- Web viewer tests: wrong PIN lockout, access lost on toggle off, and no write endpoints.
- Release build with R8 on, keep rules verified for Room and Ktor, and a signing config.
- Test on at least three devices or emulators: small screen, large screen, and Android 8 vs 14+.
- StrictMode, crash and ANR check, and a battery check for the web server service.
- Privacy statement (offline by default, no tracking, local server only when you switch it on), open-source license screen, and store listing basics.

**Done when:** a signed release APK installs cleanly on a fresh device, starts empty, and passes the full manual checklist, including a replay of the alpha-1 demo flow with none of the issues above and a web viewer session from a laptop.

## Order and risk if skipped

| Phase | Risk if skipped |
| --- | --- |
| 11 | Photos look missing or reordered and you stop trusting the home screen |
| 12 | The app keeps changing covers and previews on its own |
| 13 | Dialogs look unfinished and content stays hidden under the pill |
| 14 | The album screen keeps its old layout and the Places and People filters stay decorative |
| 15 | Placeholder data and a broken suggestion flow erode confidence in the library |
| 16 | The headline feature, viewing from a browser, is missing |
| 17 | Dead buttons and unfinished flows make it feel like a prototype |
| 18 | Unreliable release, and the fixes can regress |

## If time is short

Phases 11 and 12 are the core: they are about not losing or changing your data. Phase 16 is now required for the app to count as complete. If something has to give, trim Phase 13 to the Add to Event dialog and the pill overlap, drop the slideshow and cover swiping from Phase 14, and cut the suggested-event card from Phase 15 while keeping the manual add-to-event flow.

## Library approvals needed

| Library | Needed for | Phase |
| --- | --- | --- |
| Ktor server (CIO) | Web viewer | 16 |
| QR code generator | Web viewer pairing | 16 |

Paging 3 is already in use from the timeline work. Phase 14 needs no new library.
