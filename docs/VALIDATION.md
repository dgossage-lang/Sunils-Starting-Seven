# Validation record

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
