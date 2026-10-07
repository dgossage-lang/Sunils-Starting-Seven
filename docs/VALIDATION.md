# Validation record

## Prompt 007 validation

Validated on 12 September 2026 on Windows, using the existing API 35 `StartingSevenApi35` emulator for connected instrumentation. No physical Android phone was attached during this validation pass, so real-device camera hardware capture was not manually exercised.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat test --console=plain` | PASS - 60 JVM/Robolectric tests across 8 suites, with 0 failures, 0 errors and 0 skipped. |
| `.\gradlew.bat test assembleDebug lint --console=plain` | PASS - JVM tests, debug APK build and lint completed. Lint reported 0 errors and 23 advisory warnings. |
| `$env:ANDROID_SERIAL='emulator-5554'; .\gradlew.bat connectedDebugAndroidTest --console=plain` | PASS - one Compose instrumentation flow on `StartingSevenApi35`, including Settings/Data & Backup, game creation, attendance, fair rotation generation, played-game persistence and Player History. |

The package clear command printed `Failed` before the connected test run because the package was not installed at that moment; Gradle installed the app and test APKs and `connectedDebugAndroidTest` completed successfully.

### Photo and media coverage

- Version is now `0.7.0` with `versionCode 7`.
- Player portraits are optional and are stored as nullable `players.photoPath` references to app-owned files under `media/players/`.
- Game-day photos are optional for Draft, Ready and Played games and are stored as nullable `games.photoPath` references to app-owned files under `media/games/`.
- The UI shows portraits in Team, pitch cards, bench strip, attendance rows, Fairness rows and Player History, with initials fallback when no app-owned photo exists.
- The media repository copies picker/camera content into app-owned storage, applies EXIF orientation, rescales to bounded dimensions, re-encodes JPEG to strip metadata and removes old files when photos are replaced or deleted.
- Media unit tests cover player photo add/replace/remove and verify that changing a Played game photo does not change played-game status, assignments or fairness history.
- No `INTERNET`, `CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permission was added. The merged debug manifest contains no `uses-permission` entries matching those names.

### Backup and restore coverage

- Backup version 2 writes `.sssbackup` archives through Android Storage Access Framework `CreateDocument` with MIME type `application/octet-stream` and suggested filenames ending in `.sssbackup`.
- Each v2 archive contains `manifest.json` plus referenced app-owned media files; unsafe entries, duplicate archive entries, unexpected media files, missing referenced media and oversized content are rejected.
- Backup preview now includes player and game photo counts.
- Legacy v1 JSON backups from Prompt 006 remain restorable and restore without media references.
- Version 2 JSON manifests that reference media are rejected outside the `.sssbackup` archive path so media references cannot silently restore without files.
- Restore remains replace semantics for the database while preserving IDs and historical records, then restores app-owned media and cleans orphaned app-owned media.
- Backup tests cover v2 archive round-trip with media, v1 JSON restore compatibility and unsafe/missing archive rejection.

### Database and migration coverage

Room database version is now 2. `MIGRATION_1_2` adds nullable `photoPath` columns to `players` and `games`. The migration test creates a version 1 database, runs the Room migration path, verifies existing player/game rows remain readable and confirms both new columns default to null.

Debug APK:

```text
C:\Users\Drew\Documents\GitHub\Projects\12 - Sunil's Starting Seven\app\build\outputs\apk\debug\app-debug.apk
```

Debug APK SHA-256:

```text
4501407D004589A5A7020B3BA46251E7AC339D45D5571004924EB1326A8CABF7
```

## Prompt 005 validation

Validated on 12 September 2026 on Windows, using the existing API 35 `StartingSevenApi35` emulator for instrumentation.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat test --console=plain` | PASS - 49 JVM/Robolectric tests: 18 domain, 5 fairness-history, 5 lineup-editing, 13 persistence and 8 rotation-engine tests. |
| `.\gradlew.bat test assembleDebug lint --console=plain` | PASS - JVM tests, debug APK build and lint completed. Lint reported 0 errors and 20 advisory warnings. |
| `$env:ANDROID_SERIAL='emulator-5554'; .\gradlew.bat connectedDebugAndroidTest --console=plain` | PASS - one Compose instrumentation flow on `StartingSevenApi35`, including Fairness navigation and Nick Player History with an Away row. |

The emulator package clear command printed `Failed` before the passing instrumentation run because the package was not installed yet; Gradle installed the app and test APKs and the connected test task completed successfully.

### Prompt 005 coverage

- Fairness was added as the fourth bottom-navigation destination: Home, Team, Games and Fairness.
- The Team screen now opens Player History from a player row while the edit icon still opens Edit Player.
- History calculations use only `GameStatus.PLAYED` games and explicit attendance rows. Draft, Ready and Cancelled games are excluded.
- Away attendance increments `gamesAway` and contributes no active rotations, bench rotations, exact-position counts or role-family counts.
- Fairness states are attendance-normalised with `activeRotationsPerAttendedGame`; current active players define the main dashboard averages, while inactive players remain available behind a filter.
- Minimum-history behaviour uses Building history until a player has at least three attended Played games.
- Exact-position next opportunities are derived from position deficits against the player's own expected distribution and are capped to three positions.
- The manual-edit persistence regression now confirms generated roster -> manual swap -> Save -> Mark Played -> Player History uses the manually saved assignment.
- The 17-round all-present simulation, absence-compensation simulation and Prompt 004 manual-edit tests remain passing.

### Database and build outputs

No Room schema change was made. Database version remains 1; no migration or reseed was introduced.

Debug APK:

```text
C:\Users\Drew\Documents\GitHub\Projects\12 - Sunil's Starting Seven\app\build\outputs\apk\debug\app-debug.apk
```

Debug APK SHA-256:

```text
BAA4624C1D671FCCADFE6D4AE6AD8CCDC13A90D19CE8B03F7B0C64F23A0E8616
```

## Prompt 004 validation

Validated on 11 September 2026 on Windows.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat test --console=plain` | PASS — 44 JVM/Robolectric tests, including manual swap, structural validation, fairness-note, repository save/cancel and historical-truth coverage. |
| `.\gradlew.bat test assembleDebug lint --console=plain` | PASS — JVM tests and debug APK build; lint completed without errors. |
| `$env:ANDROID_SERIAL='emulator-5554'; .\gradlew.bat connectedDebugAndroidTest --console=plain` | PASS — one Compose instrumentation flow on the disposable API 35 emulator, including entering and cancelling READY-lineup edit mode before the game is marked Played. |

### Manual editing coverage

- On-field-to-on-field swaps exchange exact positions while retaining one player per position.
- Pitch-to-bench swaps transfer the exact field position to the former bench player; bench-to-bench swaps are ignored.
- Swaps affect only the chosen rotation. Pending changes remain in memory until Save Changes; Cancel/no-save leaves Room rows unchanged.
- Save Changes validates all four rotations and replaces the assignment set inside one Room transaction. A Played game rejects a further manual save.
- Fairness notes surface Goalkeeper-to-Bench, consecutive Bench, Defence-to-Defence and Wing-to-Wing transitions without blocking a structurally valid save.
- The persistence test confirms a manually swapped assignment is the one later returned by the Played-history query.

### Regression status

The Prompt 003 rotation-engine suite remains passing, including the 17-round all-present balance simulation and absence compensation simulation. The automatic generator was not changed. Attendance still invalidates persisted assignments before a future lineup is generated.

## Prompt 003 validation

Validated on 11 September 2026 on Windows, using Android Studio's JDK/Android SDK setup and the isolated `StartingSevenApi35` Android 15 / API 35 x86_64 emulator. The attached physical device was not used for automated validation.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat test --console=plain` | PASS — JVM/Robolectric suite, including rotation engine, seed and persistence tests. |
| `.\gradlew.bat test assembleDebug --console=plain` | PASS — tests plus debug APK build. |
| `.\gradlew.bat lint --console=plain` | PASS — lint completed with no errors. |
| `.\gradlew.bat connectedDebugAndroidTest --console=plain` with `ANDROID_SERIAL=emulator-5554` | PASS — one Compose instrumentation test on the disposable emulator. |
| `adb -s emulator-5554 shell am instrument -w com.sunilsstartingseven.app.test/androidx.test.runner.AndroidJUnitRunner` | PASS — instrumentation repeated directly to leave generated roster data for visual inspection. |

Final APK: `app/build/outputs/apk/debug/app-debug.apk` (23,227,820 bytes).

Final APK SHA-256:

```text
B5DA78B001532FC79AE121D85D10B21DA1312994A2133F8C12626AB03B60866E
```

### UI correction validation

Manual emulator inspection confirmed seeded player names render horizontally on Game Setup. First-name-only players display as `Nick`, `Cameron`, `Axel`, etc., without dangling periods. The Playing/Away segmented control no longer crushes the name column, and Played-game attendance controls are disabled as historical records.

The generated Lineup screen shows a portrait pitch with top and bottom goals, a horizontal halfway line, ST near the top goal, GK in front of the bottom goal, D1/D2 below midfield, and LW/MID/RW in the attacking/midfield channels. Populated cards display readable player names plus the position label. Empty cards show only one position label in previews and ungenerated states.

Screenshots captured under `verification/` include:

- `003-home.png` — one-time seed visible as 10 active players.
- `003-attendance-played.png` — Played attendance with Nick Away and horizontal seeded names.
- `003-lineup-played.png` — generated portrait lineup with persisted player cards.

### Development reseed results

The one-time development seed uses DataStore key `development_seed_version`. If it is below `1`, the app transactionally clears assignments, attendance, games and players, inserts exactly these players, then stores version `1`:

Nick, Cameron, Axel, Hughie, Jero, Samrith, Macson, Artie, Roman, Nico.

All seeded players are active and use an empty `lastInitial`; no fake games or assignments are seeded. The persistence test verifies the seed runs once and that a later user-created player survives a second initialization.

### Rotation-engine tests and metrics

The JVM rotation suite covers fewer-than-seven rejection, 7, 8, 9, 10, 11 and 12 available players, one/multiple Away player handling, structural validation, reversed-order input, GK-to-bench prevention, no consecutive bench for the normal ten-player case, historical position deficit influence, 17-round all-present simulation, and an absence simulation.

17-round all-present metrics for 10 players:

- Active rotations per player: 47–48.
- Bench rotations per player: 20–21.
- Exact-position counts per player per position: 6–7 for GK, D1, D2, LW, MID, RW and ST.
- Family distribution follows the exact-position totals: GK 6–7, DEF 13–14, WING 13–14, MID 6–7, ATT 6–7.
- Hard-rule violation count: 0.

Absence simulation:

- Simulated 17 rounds with player 3 away in selected rounds.
- Away player received zero assignments in missed rounds.
- Subsequent rounds remained structurally valid and history-aware scoring compensated progressively rather than trying to repay a whole missed match in one game.
- Hard-rule violation count: 0.

### Persistence and lifecycle validation

Generation saves all four rotations transactionally and changes the game to READY. Regenerate replaces stored assignments for Draft/Ready games. Attendance edits on non-Played games clear generated assignments and return the game to Draft. Mark as Played validates the stored roster, changes READY to PLAYED, locks normal editing, and makes assignments eligible for future fairness history. Draft, Ready and Cancelled games are ignored by the fairness history query.

The Compose instrumentation test exercises the seeded roster, Round 1 creation, Nick Away, Generate Fair Rotations, rotation switching, Mark as Played confirmation and activity recreation with the played roster retained.

### Remaining limits

Runtime validation used an API 35 emulator rather than a physical Samsung device. Manual drag/drop roster editing, swaps, statistics dashboards, backup/restore, export/sharing and notifications remain intentionally out of scope.
## Prompt 002 validation

Validated on 11 September 2026 on Windows, using Android Studio's OpenJDK 21.0.10, SDK 36, and the isolated `StartingSevenApi35` Android 15 / API 35 x86_64 emulator. A physical Android device was connected, but automated tests were intentionally run on the disposable emulator so test data did not touch the physical device.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat lint --console=plain` before Prompt 002 edits | PASS — baseline captured before changing code. |
| `.\gradlew.bat test --console=plain` | PASS — 28 JVM tests: 18 domain tests and 10 file-backed Room/repository tests. |
| `.\gradlew.bat assembleDebug --console=plain` | PASS — debug APK produced. |
| `.\gradlew.bat lint --console=plain` | PASS — 0 errors, 7 warnings. |
| `.\gradlew.bat test assembleDebug lint --console=plain` | PASS — final combined JVM/build/lint run. |
| `.\gradlew.bat connectedDebugAndroidTest --console=plain` with `ANDROID_SERIAL=emulator-5554` | PASS — one Compose instrumentation test on the isolated emulator. |
| `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` and `adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | PASS — app and test APKs installed for retained-data verification. |
| `adb -s emulator-5554 shell am instrument -w com.sunilsstartingseven.app.test/androidx.test.runner.AndroidJUnitRunner` | PASS — instrumentation repeated while keeping app data for relaunch verification. |
| `adb -s emulator-5554 shell am force-stop com.sunilsstartingseven.app` followed by `adb -s emulator-5554 shell monkey -p com.sunilsstartingseven.app 1` | PASS — process relaunch preserved Round 7, seven active players, notes, and the attendance summary `6 Playing · 1 Away`. |
| `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` | PASS — package `com.sunilsstartingseven.app`, version `0.2.0`/`2`, minSdk 26, targetSdk 36, compileSdk 36, no INTERNET permission. |

Final APK: `app/build/outputs/apk/debug/app-debug.apk` (23,225,567 bytes).

Final APK SHA-256:

```text
6CC44216EB79868CE758A6EBC60264398202975C1427F75791C29ECFB49D90D3
```

### What was exercised

- Attendance repository behavior: active-only initialization, inactive exclusion, default Available state, idempotent initialization, Away state preservation, per-game independence, historical snapshot retention after deactivation, missing-player discovery and deliberate add without duplicates, and persistence after database reopen.
- Counts and readiness: available count, away count, expected bench count above seven, zero bench at seven or fewer, ready-for-lineup state at seven or more, and shortage state below seven.
- Position presentation domain: exact seven position labels, semantic families and pitch anchor bounds.
- UI flow: create players, edit/deactivate/reactivate the first player, create a draft game, open Game Detail from the Games card, initialize attendance, mark Mason-Junior G. Away with an explicit row control, open Lineup, verify all four rotation controls, return to Game Detail, recreate the activity, and verify Away remains visible.
- Process restart: after a retained instrumentation run, force-stop/relaunch showed Home with Round 7, notes and `6 Playing · 1 Away`.
- Mood board usage: compact segmented controls, player rows, pitch cards, bench strip and mint/green summary surfaces influenced the UI. TeamTalk-specific branding, sample names, photos, slogans, coach quotes and statistics were not introduced.

### Lint and remaining limits

Prompt 001 lint baseline was 0 errors and 19 warnings. Prompt 002 final lint is **0 errors and 7 warnings**. The remaining warnings are dependency/SDK/icon guidance and are not new functional problems. No Room schema change was made; database version remains 1 and no migration was required.

Runtime verification used an API 35 emulator. A physical Android device was connected but not used for automated testing. Landscape and tablet layouts were not exhaustively exercised. The fair rotation engine, roster generation, fairness scoring, drag/drop, tap-to-assign and manual lineup editing remain intentionally out of scope.

## Prompt 001 validation

Validated on 11 September 2026 on Windows, using Android Studio's OpenJDK 21.0.10, SDK 36, and an isolated Pixel 6 Android 15 / API 35 x86_64 emulator.

## Commands and results

The build commands used this process-local JDK setting:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

| Exact command | Final result |
| --- | --- |
| `.\gradlew.bat help --console=plain` | PASS — wrapper/toolchain resolved. |
| `.\gradlew.bat test assembleDebug lint --console=plain` | PASS — 20 JVM tests, debug APK, lint. Final run: 20 seconds. |
| `.\gradlew.bat connectedDebugAndroidTest --console=plain` | PASS — one end-to-end Compose test on the isolated API 35 emulator. |
| `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` | PASS — final APK installed and launched. |
| `adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | PASS — test APK installed for retained-data verification. |
| `adb -s emulator-5554 shell am instrument -w com.sunilsstartingseven.app.test/androidx.test.runner.AndroidJUnitRunner` | PASS — UI test repeated with data retained for the restart check. |
| `adb -s emulator-5554 shell am force-stop com.sunilsstartingseven.app` followed by `adb -s emulator-5554 shell am start -W -n com.sunilsstartingseven.app/.MainActivity` | PASS — full process restart preserved edited active player and draft game. |
| `adb -s emulator-5554 shell settings put system font_scale 1.5` | PASS — enlarged Home text/navigation visually reviewed; scrolling retained access to content. Restored to 1.0 afterward. |
| `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` | PASS — correct label, package, version 0.1.0/1 and SDK 26/36/36; no INTERNET permission. |
| `adb -s emulator-5554 logcat -d -s AndroidRuntime:E StartingSeven:E` | PASS — no runtime/persistence errors reported during the inspected run. |

`adb` and `aapt` were invoked by their absolute installed SDK paths. Logs and screenshots are local, ignored files under `verification/` and `*.log`; Gradle reports are under `app/build/reports`.

## What was exercised

- 15 domain tests: blank/whitespace first names, valid names, initial normalization, one-letter/blank/multiple/numeric/punctuation/accented inputs, all seven family mappings and assignment state invariants.
- Five file-backed Room/repository tests: reopen persistence, edit and activation preservation of ID/creation timestamp/history, duplicate position/player rejection, multiple bench assignments, foreign-key deletion/orphan protection and invalid repository inputs.
- UI test: empty Team, invalid inputs, add Mason G., edit to Mason-Junior G., deactivate, filter inactive, reactivate, create Round 7 Draft with notes, activity recreation, and retained player/game visibility.
- Process restart: Home showed one active player, Round 7, Draft and “Bring water”; Team showed Mason-Junior G. as active after the app process was killed and relaunched.
- Visual review: branded empty Home, populated Home, Team, final green navigation selection, pitch foundation and 150% font scaling. Test records exist only in the disposable emulator, not in app source or the APK.
- SHA-256 comparison confirmed both repository originals and Android asset copies exactly match the user-provided PNGs.

Final APK: `app/build/outputs/apk/debug/app-debug.apk` (23,592,644 bytes).

Final APK SHA-256:

```text
40CEDE0E1D52DD0278D330695E0FFACEA369161E66F8F2EDDCE7BAAF9E29A030
```

## Resolved failures and remaining limits

The first build failed because Compose BOM 2026.09.00 requires compileSdk 37. Stable BOM 2026.03.00 builds against the requested SDK 36. Current stable AGP 9.4.0 and Kotlin 2.4.20 are retained; Gradle 9.6.0 follows [AGP's documented compatibility requirement](https://developer.android.com/build/releases/agp-9-4-0-release-notes).

The first lint run failed on an API 27 navigation-bar XML attribute and local SDK property escaping. Both were corrected. An attempted lint-suggested relocation of the adaptive icon caused resource linking to fail; its working API-qualified directory was restored. Final full build/test/lint passed.

Lint reports **0 errors and 19 warnings**. These concern newer dependency/SDK/Gradle versions and adaptive-icon recommendations. Requested SDK 36 remains unchanged. The icon keeps its working `mipmap-anydpi-v26` location and authoritative coloured artwork; no custom recoloured monochrome variant is introduced. These warnings are not suppressed.

Runtime verification used API 35, not a physical device or API 26/36 device. Other manufacturer icon masks and device layouts have not been exhaustively exercised. The final navigation-only colour change was rebuilt, unit-tested, linted and visually checked; the earlier end-to-end test covers the unchanged functional flow.

No core Prompt 001 feature is intentionally deferred. Attendance workflows, fairness/rotation generation, lineup editing, statistics and backup/restore remain out of scope. The user-selected workspace folder is retained; Gradle project, package, resource and GitHub repository identifiers use safe names.

## Prompt 008 validation — v0.9.0 release candidate

Validated on 12 September 2026 on Windows, using the local Android SDK/Gradle environment and the API 35 emulator `emulator-5554`.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `git status --short --branch` / `git branch --show-current` / `git rev-parse HEAD` | PASS — started on `main` at `322c32bccbc6daab260442b77624a9d39349e142` with a clean tree. |
| `./gradlew.bat test --console=plain` | PASS — JVM test suite. |
| `./gradlew.bat connectedDebugAndroidTest --console=plain` | PASS — one Compose app-flow test on `StartingSevenApi35(AVD) - 15`. |
| `./gradlew.bat lint assembleDebug --console=plain` | PASS — lint and debug APK build. |
| `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` | PASS — package `com.sunilsstartingseven.app`, version `0.9.0`/`9`, compileSdk 36, no INTERNET permission. |
| `rg -n "�" app/src README.md docs -S` | PASS — no replacement-character encoding damage found. |

### What was exercised

- Fresh-start behavior: the app now opens to the empty Home prompt instead of applying the previous startup development seed.
- Test workflow: after confirming the empty state, the instrumentation test creates its own roster data through the app repository, opens backup/settings actions, creates a draft game, initializes attendance, marks one player Away, generates fair rotations, opens manual lineup editing, cancels editing, switches rotations, marks the game Played, and verifies the played/locked state survives activity recreation.
- Release-candidate UI polish: on-pitch player cards were slightly reduced while preserving rounded portraits and the centered position label layout from Prompt 007B.
- Recent Prompt 007/007B features were retained during the build and connected workflow: silhouette fallback resources remain in use, game-photo UI remains present, and the plain-language generated-rotation confirmation remains visible after generation.

### Notes and limits

- The development seed repository remains in source for existing tests and restore-marker compatibility, but it is no longer constructed or called during normal app startup.
- The connected test intentionally stops at the restored played-state persistence boundary. A post-recreate back-stack experiment returned the emulator to the launcher, so Fairness navigation after deep restoration was not kept as a brittle assertion in the release test.
- The APK contains the AndroidX dynamic receiver permission generated by dependencies; no app INTERNET permission is declared.

## Prompt 009 validation — v1.0.0 production release

Validated on 12 September 2026 on Windows, using the local Android SDK/Gradle environment and the API 35 emulator `emulator-5554`.

### Version and signing

- Final version: `1.0.0` / `versionCode 10`.
- Release signing configuration reads required user-level Gradle properties and fails the build if any are missing.
- Signing secrets and the permanent keystore are outside Git and were not recorded in source or documentation.
- Final release APK verifies with Android APK Signature Scheme v2.
- Signing certificate SHA-256 fingerprint: `bf319e56995bfb02213aba76417f184fb1e55a0403ef139180315887f14177c5`.
- Signer certificate DN: `C=AU, ST=Queensland, L=Hatton Vale, O=Sunil's Starting Seven, OU=Development, CN=Drew Gossage`.
- The production APK is not signed with the standard Android debug certificate.

The production signing key must be kept permanently and backed up securely. Future versions of the app must use the same key to update an existing installation. The key and signing passwords are not stored in GitHub.

### Final APK

- Gradle output APK: `app/build/outputs/apk/release/app-release.apk`.
- Distribution copy: `app/build/outputs/release-distribution/Sunils-Starting-Seven-v1.0.0.apk`.
- Distribution APK size: `16,193,510` bytes, approximately `15.44 MiB`.
- Distribution APK SHA-256: `78E9EEECB308F201743FEB8817C3F906B3D58A31985C868819A6A58F3AF3FA46`.
- The distribution copy is byte-for-byte identical to the verified signed release APK.
- v0.9.0 unsigned release size was `16,181,226` bytes; v1.0.0 is `12,284` bytes larger, consistent with signing/version metadata.

### Commands and results

| Exact command | Final result |
| --- | --- |
| `git status`, `git branch --show-current`, `git log -3 --oneline`, `git fetch origin` | PASS — on `main`, clean, tracking `origin/main`, starting from pushed RC commit `7e02f3cd530937917e569dfc07e687be66a5057f`. |
| `./gradlew.bat test --console=plain` | PASS. |
| `./gradlew.bat lint --console=plain` | PASS — zero errors, 25 warnings. |
| `./gradlew.bat assembleDebug --console=plain` | PASS. |
| `./gradlew.bat clean assembleRelease --console=plain` | PASS — produced signed release APK. |
| `./gradlew.bat connectedDebugAndroidTest --console=plain` | PASS — one Compose app-flow test on `StartingSevenApi35(AVD) - 15`. |
| `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk` | PASS — v2 signature verifies, one production signer, certificate fingerprint recorded above. |
| `aapt dump badging app/build/outputs/release-distribution/Sunils-Starting-Seven-v1.0.0.apk` | PASS — package `com.sunilsstartingseven.app`, version `1.0.0`/`10`, minSdk 26, targetSdk 36, compileSdk 36. |
| `adb -s emulator-5554 install -r app/build/outputs/release-distribution/Sunils-Starting-Seven-v1.0.0.apk` | PASS — signed release installed on the emulator. |
| `adb -s emulator-5554 shell am start -W -n com.sunilsstartingseven.app/.MainActivity` | PASS — cold launch succeeded. |
| `adb -s emulator-5554 shell uiautomator dump /sdcard/ss7-release-ui.xml` | PASS — release launch showed fresh empty Home onboarding with `Let’s build your team`, `Add Players`, `Create Game`, bottom navigation and Settings. |

### Lint status

Lint has zero errors and 25 warnings:

- `GradleDependency`: 13 warnings.
- `NewerVersionAvailable`: 4 warnings.
- `ComposableNaming`: 2 warnings.
- `AndroidGradlePluginVersion`: 1 warning.
- `IconLocation`: 1 warning.
- `MonochromeLauncherIcon`: 1 warning.
- `ObsoleteSdkInt`: 1 warning.
- `OldTargetApi`: 1 warning.
- `UseKtx`: 1 warning.

These are advisory dependency/version/icon/API suggestions and are not release blockers.

### Production permission audit

Final production APK requested permissions:

- `com.sunilsstartingseven.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

Confirmed absent:

- `INTERNET`.
- `READ_EXTERNAL_STORAGE`.
- `WRITE_EXTERNAL_STORAGE`.
- `MANAGE_EXTERNAL_STORAGE`.
- `READ_MEDIA_IMAGES`.
- `READ_MEDIA_VIDEO`.
- `READ_MEDIA_AUDIO`.
- `CAMERA`.

Camera/photo flows use Android system intents and scoped URI access rather than app camera or broad storage permissions.

### Regression status

Automated release-critical coverage remained green:

- 7-player roster generation.
- 8-player roster generation.
- 9-player roster generation.
- 10-player roster generation.
- 11-player roster generation.
- 12-player roster generation.
- Fewer-than-7 shortage handling.
- 17-round all-present fairness simulation.
- Mixed-absence compensation simulation.
- Manual lineup editing and transactional save.
- Played-history preservation.
- V1 JSON backup restore.
- V2 media backup archive round-trip.
- Player portraits and local media replacement/removal.
- Game-day photos and local media references.
- Fresh install empty state with no development reseed.

### Release smoke result

The signed production APK installed and cold-launched on the API 35 emulator. The launch state showed the normal empty coach-facing Home screen with no seeded development roster. The automated connected workflow separately covered Settings, game creation, attendance, rotation generation, manual lineup editing entry/cancel, rotation viewing, Mark Played and activity-recreation persistence.

A previously installed DEBUG build normally cannot be updated directly by the production-signed APK because the signatures differ. If testing v1.0.0 on a device currently running the debug build, create a backup if its data matters, save it somewhere safe, uninstall the debug app, install the production-signed v1.0.0 APK, then restore the backup if desired. A fresh device such as Sunil's phone can simply install the signed APK.

### Known remaining issues

BLOCKER: none.

IMPORTANT: none.

MINOR:

- Lint retains 25 advisory warnings listed above.
- GitHub release asset upload was not performed in this local release flow.
# v1.1.0-dev seasons and configurable layouts — 2026-10-07

This section records development validation; the earlier v1.0.0 release records remain historical evidence for that release.

- Build identity: `1.1.0-dev`, versionCode `11`; Room schema `3`; new backup exports use format `3`.
- JVM/Robolectric regression suite: **73 tests passed, zero failures** (`./gradlew.bat test --console=plain`).
- Lint: **zero errors, 25 warnings**, with the same categories as the v1.0 baseline: GradleDependency (13), NewerVersionAvailable (4), ComposableNaming (2), OldTargetApi (1), AndroidGradlePluginVersion (1), ObsoleteSdkInt (1), MonochromeLauncherIcon (1), IconLocation (1), UseKtx (1).
- Debug APK and instrumentation APK builds: passed.
- `connectedDebugAndroidTest`: **one Compose workflow passed** on a fresh disposable Android 15 / API 35 emulator, including season creation, the per-player fairness toggle, standard-position removal, custom-position creation and visual placement, attendance changes, generation, manual-edit entry, Played history and activity recreation.
- Verified the requested example: nine playing players, six configured field slots and three bench assignments in each of four rotations.
- Repository tests prove independent season fairness, per-player carry on/off, inherited layouts without copied assignments, preserved attendance after layout changes, immutable Played layouts and persistence after reopen.
- Custom-position tests cover saved definitions, movement/rename snapshots, inheritance of removed positions as reusable options, and 8–12 on-field slot generation.
- Upgrade tests prove Room 1→3 migration and migration from the tracked released v2 schema preserve player/game rows, media references, attendance and assignments; existing games become part of an Earlier games season with the original seven slots.
- Backup tests cover v3 season/layout/preference/media round-trip, v1 JSON and v2 restore compatibility, and invalid season/layout rejection.
- `git diff --check`: passed. Source work remains local on `main`; no new release has been published. Signing credentials and generated build/emulator files remain outside tracked source.

Full behavior and compatibility details: [v1.1 development plan](RELEASE_2.0.0.md).

Custom-position deletion and toggle polish follow-up (2026-10-07): 73 JVM tests passed; debug/test APK builds and lint passed; the explicitly emulator-targeted Compose workflow passed, including deleting a custom option and confirming it stays deleted after saving. Repository coverage verifies inactive deletion preserves a Ready lineup, active deletion clears assignments while preserving attendance, built-in positions cannot be deleted, future layout inheritance excludes deleted options, and backup restore preserves both deletions and earlier game history. A separately connected Samsung SM-G998B failed to launch a Compose hierarchy during the initial all-device attempt; physical-device UI validation remains incomplete. The repeat run used ANDROID_SERIAL=emulator-5580 and passed.

Pitch styling and drag follow-up (2026-10-07): debug build and lint passed; the emulator-targeted Compose workflow passed with a real drag gesture, checking that the custom position moves right and up before release, then exercising turn-off, deletion, saved six-position generation and activity recreation. Inactive positions now render at 50% opacity using the earlier green/white styling. Testing explicitly targeted emulator-5580; the seeded Samsung installation was not modified by this follow-up.

# Version 2.0.0 production release — 2026-10-07

Release baseline: main at c8b969b86fdd21991fb82205d9b9df43d907cf7e, tracking origin/main with zero divergence before release preparation. All Version 2.0 feature changes were local on main and are included in the release commit. No other branch or worktree was created.

## Build and signature

- Version: 2.0.0, versionCode 20; application ID unchanged.
- AGP 9.2.1, Gradle wrapper 9.6.0, Kotlin/Compose plugin 2.4.20, Room schema 3.
- Clean build: `./gradlew.bat clean test lint assembleDebug assembleRelease --console=plain` passed.
- A production smoke test discovered that the backup preview could hide confirmation buttons. Settings was made scrollable, then `test lint assembleDebug assembleRelease` passed again. Restore was exercised at normal emulator display density.
- JVM/Robolectric tests: 73 passed, zero failures. Includes 7–12-player generation, shortages relative to selected layout, 17-round all-present and mixed-absence simulation, manual editing, Played history, seasons, custom positions, migrations, portraits, photos, v1/v2 compatibility and v3 media round-trip.
- Emulator-only `connectedDebugAndroidTest` passed, including real custom-position drag/drop, deletion and saved lineup recreation. ANDROID_SERIAL explicitly selected emulator-5580.
- `apksigner verify --verbose --print-certs` passed for the final distribution APK (v2 signature scheme; one signer).
- Public production certificate SHA-256: bf319e56995bfb02213aba76417f184fb1e55a0403ef139180315887f14177c5. It matches the downloaded v1.0.0 release APK.
- Debug certificate SHA-256: c0a8b09f312b19d4452026aaace2dc31d0cd17018336e06f8d61f802f531abcd. Production signing is distinct; the final APK is non-debuggable.
- Signing properties/keystore remain external. No passwords, keystores, private keys, APK/AAB files, build outputs or user Gradle configuration belong in the source commit.

## Distribution artifact

- Filename: Sunils-Starting-Seven-v2.0.0.apk.
- Local path: app/build/outputs/release-distribution/Sunils-Starting-Seven-v2.0.0.apk.
- Size: 16,275,434 bytes (15.52 MiB / 16.28 MB); 81,924 bytes larger than v1.0.0.
- SHA-256: ad28cd9e69093224bb7fd8ee1e02aaa3be30f699576f947a8ceed55d73ca8458.
- The distribution copy is byte-identical to app/build/outputs/apk/release/app-release.apk.
- Debug APK size: 23,804,247 bytes.
- Final manifest requested permission: com.sunilsstartingseven.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION only. INTERNET, CAMERA, READ/WRITE/MANAGE_EXTERNAL_STORAGE and READ_MEDIA permissions are absent.

## Production smoke and persistence

The published signed v1.0.0 APK was downloaded and its checksum verified against GitHub. On an isolated API 35 emulator, v1.0.0 restored a controlled v2 media archive with eight players, two games, attendance, 32 Played assignments, one portrait and one game photo.

Installing signed v2.0.0 with `adb install -r` succeeded without uninstalling or clearing that installation. The eight-player roster, both games, Earlier games season, saved Played lineup, portrait, game-photo thumbnail and fairness history survived. This exercises the actual production signing and Room migration path, in addition to schema migration tests.

The production v2 app exported a v3 archive. Its manifest was inspected: appVersion 2.0.0, backupVersion 3, schemaVersion 3, eight players, two games, 32 assignments, one season and two media files. Clearing only the disposable emulator's app data and restarting proved the production fresh-install state is empty, with no test seed. Restoring the exported v3 archive restored the roster, games, season and game photo. The scrollable preview confirmation was verified at normal density. The seeded Samsung debug installation was not modified by release validation.

## Lint and remaining issues

Zero lint errors; 25 advisory warnings: GradleDependency 13, NewerVersionAvailable 4, ComposableNaming 2, OldTargetApi 1, AndroidGradlePluginVersion 1, ObsoleteSdkInt 1, MonochromeLauncherIcon 1, IconLocation 1, UseKtx 1.

- BLOCKER: none known after the backup-screen fix.
- IMPORTANT: none known.
- MINOR: existing lint advisories, Gradle deprecated-feature notices, and native-library symbol-stripping notices. Automated device coverage is API 35 emulator; broader physical-device compatibility has not been exhaustively tested.

The v2.0.0 release uses a normal main push, then an annotated tag after origin/main contains the validated source commit. Binaries are distributed as GitHub release assets rather than committed source. The production key matches v1.0.0; debug installations require backup, uninstall, production install and restore when switching signatures.
