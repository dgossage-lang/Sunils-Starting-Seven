# Sunil’s Starting Seven

A lightweight Android youth soccer roster app, built for a coach beside the pitch. Local-first, offline, and intended to help produce fair player rotations from attendance and playing history.

**Version 1.0.0 (versionCode 10)** — first production release.

No backend, login, analytics, tracking or internet permission. Players, games, attendance, generated rosters and imported photos stay in local app-owned storage. Android cloud backup and device transfer are disabled. Coaches can create and restore local `.sssbackup` files through Android's document picker without broad storage permissions.

## What it does

Sunil’s Starting Seven helps a coach manage a youth seven-a-side soccer team on game day:

- Build a local team list with first name and one last initial.
- Add optional player portraits with a child-silhouette fallback when no portrait is set.
- Create draft games, record attendance and attach optional game-day photos.
- Generate fair four-rotation lineups from attendance and Played-game history.
- Manually edit generated lineups with tap-to-swap pitch and bench changes.
- Confirm before regenerating a Draft or Ready lineup.
- Mark games as Played so they become locked historical records.
- Review Player History and team Fairness, including role coverage and likely next opportunities.
- Back up and restore local data through portable `.sssbackup` files.

## Implemented functionality

Implemented:

- Branded Kotlin / Jetpack Compose / Material 3 Android foundation.
- Home, Team, Games, Fairness and Settings flows.
- Add/edit players with first name and one uppercase last initial, validation, active/inactive filters, deactivation and reactivation.
- Fresh installs start empty and guide the coach to add players before creating a game.
- Draft game creation with round number, date picker and optional notes.
- Draft game detail/setup screen with editable round, date and notes.
- Game-specific attendance snapshots initialized from the active team.
- Available/Away attendance with shortage and bench summaries.
- Add Missing Players for players added after a draft's attendance was initialized.
- Seven youth soccer positions: GK, D1, D2, MID, LW, RW and ST.
- Fair four-rotation lineup generation for available players, including persisted on-field and bench assignments.
- Historical fairness from Played games only: exact-position counts, position-family counts, active rotation counts and bench counts.
- Absence compensation through history-aware scoring; Away players receive no assignments for that game and naturally fall behind for future rounds.
- Regenerate confirmation for Draft/Ready games, replacing the roster transactionally only after coach approval.
- READY lifecycle after successful generation.
- Mark as Played with confirmation; Played games become locked historical records and feed future fairness.
- Manual tap-to-swap editing for READY lineups, including pitch-to-pitch and pitch-to-bench swaps across all four rotations in one edit session.
- Transactional manual lineup save with structural validation and clear invalid-edit errors.
- Game Detail now reopens Played games directly to the saved historical lineup, including manual edits.
- Player History screens reachable from Team and Fairness, showing games attended, games away, playing/bench rotations, exact-position history, role-family history, role coverage, likely next opportunities and game-by-game rotation chips.
- Room database, repositories and lifecycle-aware Coroutines / Flow state.
- Typed attendance, assignments, playing positions and position families.
- DataStore Preferences foundation for local app preferences.
- Optional player portraits and game-day photos from Android's camera capture flow or system photo picker, copied into app-owned storage with a child-silhouette fallback when no photo is set.
- Local media storage that strips metadata by re-encoding imported images, resizes portraits and game-day photos for app use, and avoids internet, cloud upload, face recognition and broad storage permissions.
- Local `.sssbackup` archive export through Android Storage Access Framework CreateDocument, with a portable `sunils-starting-seven-backup` manifest plus referenced app-owned media files.
- Restore backup flow through Android Storage Access Framework OpenDocument, including v1 JSON compatibility, v2 archive/media validation, preview, confirmation and transactional database replace semantics.
- Backup and restore preserve manually edited Played rosters as historical truth.

Only Played games contribute to historical fairness. Draft, Ready and Cancelled games do not skew future rotation decisions.

Supported backup versions: v1 JSON backups remain restorable; v2 archive backups include database content plus referenced app-owned media files.

## Fresh installs

Fresh installs start with no players, games, attendance or history. The Home and Team screens guide the coach to add players manually. The earlier development-only startup reseed is no longer wired into app startup, so launching the app never clears or replaces existing local data.

## Installation note

A previously installed DEBUG build normally cannot be updated directly by the production-signed APK because the signatures differ.

If testing v1.0.0 on a device currently running the debug build:

1. Create a backup in the existing app if its data matters.
2. Save the backup somewhere safe.
3. Uninstall the debug app.
4. Install the production-signed v1.0.0 APK.
5. Restore the backup if desired.

A fresh device such as Sunil's phone can simply install the signed APK.

The production signing key must be kept permanently and backed up securely. Future versions of the app must use the same key to update an existing installation. The key and signing passwords are not stored in this repository.

## Rotation engine

The rotation engine creates four periods. Every available player receives one assignment per period: either on field with an exact position or on the bench. The validator rejects duplicate position use, duplicate per-period player assignment, missing assignments, Away-player assignments and invalid roster structures.

The generator supports 7–12 available players. With more than seven players, bench counts are distributed as evenly as possible over the four rotations while still filling all seven on-field positions. For fewer than seven available players, the UI reports the shortage instead of generating an incomplete lineup.

Historical scoring favours players who are behind on total active rotations, bench balance, exact-position exposure and role-family exposure. Long-run regression tests cover all-present balance over 17 rounds and mixed absence catch-up.

## Privacy and storage

- No internet permission.
- No analytics or telemetry.
- No account or login.
- No server storage.
- No cloud upload.
- No broad storage permission.
- Camera and photo picker usage is delegated to Android system apps and scoped URI access.
- Imported media is copied into app-owned storage and re-encoded for app use.
- Android cloud backup is disabled for app data.
- Local backups are created only when the coach chooses to export one.

## Build

Prerequisites:

- Android Studio / JDK 17+
- Android SDK with compileSdk 36
- Gradle wrapper from this repository

Useful commands:

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat lint --console=plain
.\gradlew.bat assembleDebug --console=plain
.\gradlew.bat assembleRelease --console=plain
```

Release signing is configured through user-level Gradle properties. The repository defines the property names but does not store signing secrets.

Required user-level properties:

- `SUNILS_RELEASE_STORE_FILE`
- `SUNILS_RELEASE_STORE_PASSWORD`
- `SUNILS_RELEASE_KEY_ALIAS`
- `SUNILS_RELEASE_KEY_PASSWORD`

Do not commit keystores, APKs, AABs, passwords or local signing configuration.

## Tests

`test` runs JVM domain tests, the rotation-engine regression suite and Robolectric persistence/backup tests against a file-backed Room database. Coverage includes validation, repository persistence, Room 1→2 migration, media import/replace/remove behavior, v2 archive backup round-trip, v1 JSON restore compatibility, archive safety rejection, backup seed safety/future-ID behavior, attendance behavior, roster structural validation, regenerate failure safety, 7–12 player generation, Away players, transition constraints, reversed-order tie breaking, historical deficit influence, 17-round all-present balance and absence simulation.

The Compose instrumentation test needs a fresh disposable emulator/app installation. It verifies the fresh empty Home prompt, creates its own test roster through the app repository, opens Settings from the Home gear, checks the Data & Backup actions, creates Round 1, marks Artie away, generates fair rotations, switches rotation, marks the game Played, and verifies the played roster survives activity recreation.

See [validation record](docs/VALIDATION.md) for actual commands and results.

## Data architecture

Explicit `Application → AppContainer → Repository → ViewModel` dependencies keep persistence and generation separate from Compose screens.

| Table | Purpose and integrity |
| --- | --- |
| `players` | Auto-generated Long ID, first name, one-letter last initial for normal UI records, active flag, nullable app-owned portrait path and creation timestamp. Display name is computed, not stored. No destructive delete API for normal UI. |
| `games` | Stable ID, positive round number, local date as epoch day, typed status, optional notes, nullable app-owned game-day photo path and creation timestamp. |
| `attendance` | Composite game/player key, Available/Away status, indexed player FK. |
| `assignments` | Composite game/rotation/player key, explicit On-field/Bench state and nullable exact position. Unique game/rotation/position index prevents two players in one field slot; multiple null positions support the bench. |

Database schema version is 2. Migration 1→2 adds nullable `photoPath` columns for player portraits and game-day photos while preserving existing rows.

## Git workflow

**THIS REPOSITORY USES ONLY THE `main` BRANCH.**

All development is committed directly to `main`. Do not create feature, development, release or temporary branches, or Git worktrees. Tags may be introduced later.

Private repository: [dgossage-lang/Sunils-Starting-Seven](https://github.com/dgossage-lang/Sunils-Starting-Seven).
