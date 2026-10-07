# Sunil's Starting Seven Version 2.0

Version `2.0.0`, versionCode `20`. Release date: 2026-10-07.

## New functionality

- Seasons organise games and history. Create/rename seasons and filter Home, Games, Fairness and Player History by season or All seasons.
- Each game stores its own pitch layout, with the original seven positions and custom positions with names, short labels, role groups and locations. Layouts support 1–12 field slots.
- New games inherit the latest created game's layout in their season. A season's first game uses the latest game overall. The engine creates fresh assignments rather than copying them.
- Field capacity, shortages and bench counts follow the layout. Nine playing players with Striker removed means six on field and three on the bench.
- Each player's edit page includes Carry fairness across seasons, off by default. It controls whether future generation uses that player's current-season or all-season Played history.
- Custom positions can be turned off or deleted. Turning off retains an option; deletion removes it from future inherited layouts. Earlier games retain their snapshots.
- Place on Pitch supports drag and drop, tap placement and accessible sliders. Movement is constrained to pitch boundaries. Save Layout persists changes.
- The original green/white position styling is retained, with inactive buttons at 50% opacity and no plus/minus symbols.
- Draft and Ready layouts are editable. Changing an active layout clears assignments after confirmation while preserving attendance; deleting only inactive options preserves a Ready lineup. Played games remain locked history.
- Settings scrolls so backup-preview confirmation remains reachable on smaller displays.

## Data and privacy

Room schema 3 preserves old records and media references. Upgraded games are grouped into Earlier games with their original seven positions. Fresh installs start empty. The testing roster seeded on the development phone is not included in the production app.

New v3 backup archives preserve seasons, custom-position snapshots, player fairness preferences and media. V1 JSON and v2 media backups remain restorable. V3 backups require Version 2.0 or later to restore.

The app remains offline, without internet, camera, broad storage or media-read permissions. Its only requested permission is the generated `com.sunilsstartingseven.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

## Installation

Production v1.0.0 can be updated directly with the signed v2.0.0 APK. Keep a backup before upgrading important data.

A debug installation uses a different signature. To switch the seeded debug phone to production, export a backup, keep it safe, uninstall the debug app, install the production APK, then restore the backup. Release work does not uninstall or replace the development phone's app.

## Distribution

- Tag: `v2.0.0`.
- APK: `Sunils-Starting-Seven-v2.0.0.apk`.
- Size: **16,275,434 bytes** (approximately 15.52 MiB / 16.28 MB).
- SHA-256: `ad28cd9e69093224bb7fd8ee1e02aaa3be30f699576f947a8ceed55d73ca8458`.
- Public signing certificate SHA-256: `bf319e56995bfb02213aba76417f184fb1e55a0403ef139180315887f14177c5`.
- The production certificate matches v1.0.0 and differs from the Android debug certificate. The APK is non-debuggable and verifies with apksigner.
- Download: [GitHub release](https://github.com/dgossage-lang/Sunils-Starting-Seven/releases/tag/v2.0.0).

## Verification

73 JVM/Robolectric tests pass, including the original 7–12-player and 17-round/mixed-absence regressions, season fairness, dynamic/custom layouts, manual swaps, history, media, backup safety and migrations. Lint has zero errors and 25 advisory warnings.

The API 35 emulator workflow covers season creation, per-player fairness, position toggles, custom-position drag/drop, deletion, lineup generation and activity recreation. Production smoke tests use the published signed v1.0.0 APK, restore a v2 media fixture, then upgrade with the signed v2.0.0 APK without uninstalling. Eight players, two games, Played assignments, season grouping, portrait, game photo and fairness history survive. Actual production v3 export and restore are also verified, along with empty fresh-install state.

No known BLOCKER or IMPORTANT issue remains. MINOR items are the existing lint advisories, Gradle deprecation notices and native-library symbol-stripping notices. Automated device coverage is an Android 15 / API 35 emulator; broader physical-device coverage remains limited.

Detailed command results: [VALIDATION.md](VALIDATION.md).
