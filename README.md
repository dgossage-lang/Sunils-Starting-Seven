# Sunil’s Starting Seven

A lightweight Android youth soccer roster app, built for a coach beside the pitch. Local-first, offline, and intended to help produce fair player rotations from attendance and playing history.

**Version 0.6.0 (versionCode 6)** — development build.

No backend, login, analytics, tracking or internet permission. Players, games, attendance and generated rosters stay in the local Room database. Android cloud backup and device transfer are disabled. Coaches can create and restore local JSON backup files through Android's document picker without broad storage permissions.

## Current status

Implemented:

- Branded Kotlin / Jetpack Compose / Material 3 Android foundation.
- Home, Team and Games navigation with useful empty states.
- Add/edit players with first name and one uppercase last initial, validation, active/inactive filters, deactivation and reactivation.
- A one-time development database reseed to the original roster: Nick, Cameron, Axel, Hughie, Jero, Samrith, Macson, Artie, Roman and Nico.
- First-name-only seeded player display without dangling periods; normal manually edited players still require a valid one-letter last initial.
- Draft game creation with round number, date picker and optional notes.
- Draft game detail/setup screen with editable round, date and notes.
- Game-specific attendance snapshots initialized from the active team.
- Available/Away tracking with immediate persistence, attendance summaries and under-seven guidance.
- Attendance changes invalidate generated rosters and return the game to Draft.
- Corrected portrait soccer pitch with top/bottom goals, horizontal halfway line and formation anchors for ST, LW, MID, RW, D1, D2 and GK.
- Fair four-rotation lineup generation for available players, including persisted on-field and bench assignments.
- Historical fairness from Played games only: exact-position counts, position-family counts, active rotation counts and bench counts.
- Absence compensation through history-aware scoring; Away players receive no assignments for that game and naturally fall behind for future rounds.
- Regenerate for Draft/Ready games, replacing the roster transactionally.
- READY lifecycle after successful generation.
- Mark as Played with confirmation; Played games become locked historical records and feed future fairness.
- Manual tap-to-swap editing for READY lineups, including pitch-to-pitch and pitch-to-bench swaps across all four rotations in one edit session.
- Transactional Save / Cancel workflow with lightweight Undo; unsaved swaps remain local until saved.
- Friendly advisory fairness notes for manual overrides while keeping structurally valid coach changes saveable.
- Played-game protection for attendance, regeneration and lineup edits; the saved edited roster becomes historical truth when marked Played.
- Team Fairness dashboard with Played-game participation history, attendance-normalised fairness states, active/inactive player filters, team position balance and bench balance.
- Player History screens reachable from Team and Fairness, showing games attended, games away, playing/bench rotations, exact-position history, role-family history, role coverage, likely next opportunities and game-by-game rotation chips.
- Room database, repositories and lifecycle-aware Coroutines / Flow state.
- Typed attendance, assignments, playing positions and position families.
- DataStore Preferences foundation for team preferences and the one-time development seed version.
- Settings screen reachable from the Home gear, with Data & Backup actions kept out of the bottom navigation.
- Local JSON backup export through Android Storage Access Framework CreateDocument, with a portable `sunils-starting-seven-backup` format covering players, games, attendance and assignments.
- Restore backup flow through Android Storage Access Framework OpenDocument, including pre-restore validation, preview, confirmation and transactional replace semantics.
- Local Poppins fonts, adaptive launcher icon and Android splash screen without an artificial delay.

Only Played games contribute to historical fairness. Draft, Ready and Cancelled games are excluded, and Away attendance is visible so absences do not make a player look unfairly behind.

Planned later: cloud backup, sharing/export conveniences and notifications.

## Development seed

This development build intentionally performs a one-time seed reset. If the stored `development_seed_version` is below `1`, the app transactionally clears assignments, attendance, games and players, inserts the ten original first-name-only players, then records seed version `1` in DataStore. Subsequent launches do not wipe data again. A successful backup restore records the current development seed version after replacing the database, so restored user data is not wiped by the seed on the next app start. This mechanism is for the development milestone and can be removed cleanly in a future release.

The seed creates player records only. It does not create fake games, fake historical assignments or fake fairness history.

## Rotation engine

The generator is implemented outside Compose in `domain/rotation`. It creates four rotations:

1. Rotation 1 — First Half - Start
2. Rotation 2 — First Half - Finish
3. Rotation 3 — Second Half - Start
4. Rotation 4 — Second Half - Finish

Each rotation fills exactly seven positions: GK, D1, D2, LW, MID, RW and ST. Every available player appears exactly once per rotation, either on field or on the bench. Away players receive no assignment.

The engine uses deterministic scoring with a central validator. It prioritizes structural validity, then balances within-game bench time, avoids consecutive bench periods for the normal ten-player case, prevents goalkeeper-to-bench transitions where feasible, discourages DEF-to-DEF and WING-to-WING transitions, rewards within-game family variety, and uses Played-game history to compensate exact-position, family, active-time and bench deficits over time.

Automatic fair roster generation remains the recommendation. A coach can then choose **Edit Lineup** on a READY game, tap two player cards to swap their assignment slots for the selected rotation, and save all four rotations as one transaction. Manual edits must remain structurally valid, but fairness concerns such as Goalkeeper-to-Bench or consecutive bench periods are advisory notes rather than blockers.

## Fairness history

The Fairness tab derives its dashboard from the same persisted historical truth used by the generator: explicit GameAttendance rows and saved Assignment rows for GameStatus.PLAYED games only. It does not store calculated totals in Room.

Participation labels use active rotations per attended Played game, rather than raw lifetime totals, so a player who missed matches is compared by opportunity rate. The app waits for at least three attended Played games before showing stronger labels such as Balanced, Needs opportunity or Slightly ahead; earlier records show as Building history. Exact-position and role-family summaries use the seven field positions and their families: GK, DEF, WING, MID and ATT.

## Local backup and restore

The Settings screen is opened from the Home gear so Data & Backup stays separate from the main coaching tabs. Create Backup launches Android's document picker with MIME type `application/json` and a suggested filename like `Sunils-Starting-Seven-Backup-YYYY-MM-DD_HH-mm.json`. Restore Backup launches the document picker for JSON/text files, validates the selected file before changing data, shows a preview of the current device versus the backup, and requires a destructive confirmation.

The JSON format is portable and explicit: `format`, `backupVersion`, `createdAt`, `appVersion`, `databaseSchemaVersion`, `players`, `games`, `attendance` and `assignments`. It does not include Android DataStore internals, the development seed marker, local file paths, device identifiers or account identifiers. Restore is replace semantics, not merge: players, games, attendance and assignments are replaced in one Room transaction while preserving IDs and history. Invalid JSON, wrong format, future backup versions, duplicate IDs and broken references are rejected before mutation.

## Build

One application module: `app`. Project identifier: `SunilsStartingSeven`; package: `com.sunilsstartingseven.app`.

- minSdk 26, targetSdk 36, compileSdk 36.
- JDK 21.
- Android SDK Platform 36 and Build Tools 36.0.0.
- Gradle 9.6.0 wrapper with pinned distribution checksum.
- Android Gradle Plugin 9.2.1, Kotlin Compose plugin 2.4.20, KSP 2.3.12.
- Stable Compose BOM 2026.03.00 selected for compileSdk 36 compatibility.
- Navigation Compose, Room 2.8.4, DataStore Preferences, Kotlinx Serialization JSON, lifecycle ViewModels and Coroutines / Flow.

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

`test` runs JVM domain tests, the rotation-engine regression suite and Robolectric persistence/backup tests against a file-backed Room database. Coverage includes validation, first-name-only display, the one-time seed, repository persistence, backup round-trip/restore rejection/seed safety/future-ID behavior, attendance behavior, roster structural validation, 7–12 player generation, Away players, transition constraints, reversed-order tie breaking, historical deficit influence, 17-round all-present balance and absence simulation.

The Compose instrumentation test needs a fresh disposable emulator/app installation. It verifies the seeded roster, opens Settings from the Home gear, checks the Data & Backup actions, creates Round 1, marks Nick Away, generates fair rotations, switches rotation, marks the game Played, verifies the played roster survives activity recreation, opens the Fairness tab and opens Nick's Player History with the Away game row.

See [validation record](docs/VALIDATION.md) for actual commands and results.

## Data architecture

Explicit `Application → AppContainer → Repository → ViewModel` dependencies; Compose never calls Room. Immutable screen state is exposed as StateFlow and collected with lifecycle awareness. Failed reads offer Retry; failed writes preserve the form and surface a friendly error while logging the exception locally.

| Table | Purpose and integrity |
| --- | --- |
| `players` | Auto-generated Long ID, first name, optional last initial for development seed records, active flag and creation timestamp. Display name is computed, not stored. No destructive delete API for normal UI. |
| `games` | Stable ID, positive round number, local date as epoch day, typed status, optional notes and creation timestamp. |
| `attendance` | Composite game/player key, Available/Away status, indexed player FK. |
| `assignments` | Composite game/rotation/player key, explicit On-field/Bench state and nullable exact position. Unique game/rotation/position index prevents two players in one field slot; multiple null positions support the bench. |

All historical foreign keys use RESTRICT. Editing a name or deactivating a player retains their ID and historical references. Attendance initialization is idempotent. Generated rosters are persisted transactionally. Played games are historical truth; only Played games contribute to future fairness calculations. Cancelled, Draft and Ready games are ignored by fairness history.

## Brand assets

The supplied original artwork is preserved in [docs/brand](docs/brand), with Android-safe copies in `drawable-nodpi`. No artwork is redesigned, stretched, recoloured or filtered.

## Git workflow

**THIS REPOSITORY USES ONLY THE `main` BRANCH.**

All development is committed directly to `main`. Do not create feature, development, release or temporary branches, or Git worktrees. Tags may be introduced later.

Private repository: [dgossage-lang/Sunils-Starting-Seven](https://github.com/dgossage-lang/Sunils-Starting-Seven).
