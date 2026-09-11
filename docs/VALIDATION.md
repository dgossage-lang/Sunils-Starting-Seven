# Prompt 001 validation

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
