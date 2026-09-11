# Sunil’s Starting Seven

A lightweight Android youth soccer roster app, built for a coach beside the pitch. Local-first, offline, and intended to help produce fair player rotations as the product develops.

**Version 0.2.0 (versionCode 2)** — development build.

No backend, login, analytics, tracking or internet permission. Players and games stay in the local Room database. Android cloud backup and device transfer are disabled; uninstalling or clearing app storage removes data. Backup/restore is planned, not yet implemented.

## Current status

Implemented:

- Branded Kotlin / Jetpack Compose / Material 3 Android foundation.
- Home, Team and Games navigation with useful empty states.
- Add/edit players with first name and one uppercase last initial, validation, active/inactive filters, deactivation and reactivation.
- Draft game creation with round number, date picker and optional notes.
- Draft game detail/setup screen with editable round, date and notes.
- Game-specific attendance snapshots initialized from the active team.
- Available/Away tracking with immediate persistence, attendance summaries and under-seven guidance.
- Deliberate Add Missing Players action for draft games when new active team members were added after attendance initialization.
- Home and Games cards surface attendance summaries where available.
- Pitch-based lineup visual foundation with four rotation selections, seven position anchors, empty position cards and a bench strip.
- Room database, repositories and lifecycle-aware Coroutines / Flow state.
- Typed attendance, assignments, playing positions and position families for future work.
- DataStore Preferences foundation for lightweight team preferences.
- Reusable Canvas soccer pitch and selected Compose previews.
- Local Poppins fonts, adaptive launcher icon and Android splash screen without an artificial delay.

Planned (not implemented): fair roster generation, historical fairness engine, manual roster editing, statistics and backup/restore. The app still does not automatically generate lineups or rosters. No generated rosters, seeded players, fake games, player photos or copied mood-board sample content exist in production.

## Build

One application module: `app`. Project identifier: `SunilsStartingSeven`; package: `com.sunilsstartingseven.app`.

- minSdk 26, targetSdk 36, compileSdk 36.
- JDK 21 (validated with Android Studio's bundled JDK 21; daemon criteria are pinned in `gradle/gradle-daemon-jvm.properties`).
- Android SDK Platform 36 and Build Tools 36.0.0.
- Gradle 9.6.0 wrapper with pinned distribution checksum.
- Android Gradle Plugin 9.4.0, Kotlin Compose plugin 2.4.20, KSP 2.3.12.
- Stable Compose BOM 2026.03.00 selected for compileSdk 36 compatibility. BOM 2026.09.00 requires SDK 37 and is deliberately not used.
- Navigation Compose, Room 2.8.4, DataStore Preferences, lifecycle ViewModels and Coroutines / Flow.

AGP 9 supplies Kotlin support. There is no separate legacy Kotlin Android plugin. No serialization dependency is needed for these Room-backed models; JSON import/export is out of scope.

Open this directory in Android Studio. Set the Gradle JDK to a compatible installed JDK and install SDK 36 through SDK Manager. Let Android Studio create the untracked `local.properties`, or add `sdk.dir=C\:/path/to/Android/Sdk` locally. Initial dependency downloads require development-time internet access; the installed app does not.

On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

On macOS/Linux, use `./gradlew` (or `sh gradlew` if executable permission is unavailable).

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Install on a connected test device/emulator with:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Alternatively copy the APK to an Android device and allow installation from that source. This is a debug-signed development APK, not a production release.

## Tests

`test` runs JVM domain tests and Robolectric tests against an actual file-backed Room database: validation, position families, position labels/anchors, attendance counts, expected bench/shortage state, explicit bench state, reopen persistence, name updates, deactivation/reactivation, attendance snapshot behavior, foreign keys and duplicate constraints. Schema v1 is exported under `app/schemas`; Prompt 002 did not require a schema change. Future schema changes require explicit migrations. Destructive migration fallback is not enabled.

The Compose instrumentation test needs a **fresh disposable emulator/app installation**. It enters test players and a draft game, edits/deactivates/reactivates the first player, initializes attendance, marks one player Away, opens the lineup screen, checks the four rotation controls, and checks activity recreation. It intentionally writes only to the test installation. Do not run it against a coach's existing data.

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

See [validation record](docs/VALIDATION.md) for actual commands and results, including runtime verification limits.

## Data architecture

Explicit `Application → AppContainer → Repository → ViewModel` dependencies; Compose never calls Room. Immutable screen state is exposed as StateFlow and collected with lifecycle awareness. Failed reads offer Retry; failed writes preserve the form and surface a friendly error while logging the exception locally.

| Table | Purpose and integrity |
| --- | --- |
| `players` | Auto-generated Long ID, first name, last initial, active flag and creation timestamp. Display name is computed, not stored. No destructive delete API. |
| `games` | Stable ID, positive round number, local date as epoch day, typed status, optional notes and creation timestamp. UI creates Draft only. |
| `attendance` | Composite game/player key, Available/Away status, indexed player FK. |
| `assignments` | Composite game/rotation/player key, explicit On-field/Bench state and nullable exact position. Unique game/rotation/position index prevents two players in one field slot; multiple null positions support the bench. Zero-based rotation index is not limited to four. |

All historical foreign keys use RESTRICT. Editing a name or deactivating a player retains their ID and historical references. Attendance initialization is idempotent: the first initialization for a draft game snapshots currently active players as Playing, and later Team changes do not silently rewrite that game. New active players can be added to an initialized draft only through the deliberate Add Missing Players action. Assignment construction rejects a bench position, missing on-field position or negative rotation. The roster DAO persists future records but Prompt 002 still does not generate assignments. Existing stored assignments can be rendered by the lineup foundation. Only Played games will contribute to future participation calculations; no such calculations exist yet. Position families are derived from exact positions, including shared DEF and WING families.

DataStore contains only a lightweight team-name preference API, with no settings screen or historical records. Default data is empty.

## Brand assets

The supplied original artwork is preserved in [docs/brand](docs/brand), with byte-identical Android-safe copies in `drawable-nodpi`:

- `Logo.png`: authoritative emblem, compact app-bar branding and launcher/splash source.
- `Logo with Text.png`: full wordmark and visual reference, used for the initial Home empty experience.

No artwork is redesigned, stretched, recoloured or filtered. The adaptive foreground applies equal 22% insets around the square emblem; its corners fit within the central safe circle. A white background supports circular/squircle launcher masks. minSdk 26 means adaptive icons cover all supported Android versions. No app name is added to the icon.

The light Compose theme uses deep navy text (#032962), green/mint surfaces, blue (#075EFC) and restrained yellow accents. Action green is darkened to #087B39 for legible white button text. Colours, typography, shapes and spacing are centralized in `ui/theme`.

Poppins Regular, Medium, SemiBold and Bold are bundled locally from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/poppins), under the [SIL Open Font License](docs/licenses/Poppins-OFL.txt). Fonts require no runtime download. The supplied brand artwork remains the user's source material.

## Git workflow

**THIS REPOSITORY USES ONLY THE `main` BRANCH.**

All development is committed directly to `main`. Do not create feature, development, release or temporary branches, or Git worktrees. Tags may be introduced later.

Private repository: [dgossage-lang/Sunils-Starting-Seven](https://github.com/dgossage-lang/Sunils-Starting-Seven).

Build output, local SDK paths, IDE state, APKs, emulator files and signing secrets are ignored. The Gradle wrapper, source artwork, font licence and database schema are versioned.
