# PHASE0_FINDINGS.md — Giggin' Gear (AmpRack Fork) Architecture Report

**Date:** 2026-08-13  
**Repo:** https://github.com/kancrutt/amp-rack (forked from djshaji/amp-rack)  
**Local clone:** `/home/hery/giggin-gear`  
**Upstream license:** Apache License 2.0 — fork/redesign/rebrand permitted; retain attribution per Section 4.

---

## 1. Build Reproducibility

| Item | Value |
|---|---|
| AGP version | 8.13.0 |
| Gradle wrapper | 8.13 |
| compileSdk | 35 |
| minSdk | 24 |
| targetSdk | 35 |
| NDK version | 26.1.10909125 |
| JDK | OpenJDK 17.0.19 |
| CMake | 3.18.1 |
| Language | Java (Kotlin commented out) |
| APK size (debug) | 255 MB (multi-ABI: arm64-v8a, armeabi-v7a, x86, x86_64) |

### Build Fix Required

**CMakeLists.txt needed a one-line fix:** added `${CMAKE_CURRENT_SOURCE_DIR}` to `target_include_directories` for the `amprack` library. Without this, LV2 headers (`lv2/core/lv2.h`, `lv2/atom/atom.h`) were not found. The upstream repo likely builds in Android Studio where IDE-managed include paths mask this omission.

**google-services.json:** Not present in upstream repo (correctly gitignored). A placeholder was created for build to succeed. For production kiosk build, Firebase should be stripped entirely (see Section 8).

### Smoke Test Result

```
$ ./gradlew assembleDebug
BUILD SUCCESSFUL in 1m 40s
30 actionable tasks: 26 executed, 4 up-to-date
Output: app/build/outputs/apk/debug/app-debug.apk (255 MB)
```

**PASS** — clean build on headless Ubuntu 26.04 server with CLI-only Android SDK.

---

## 2. Module Structure

**Single-module app** — only `:app` in `settings.gradle`.

Dead sub-module stubs exist (`app/amp/`, `app/distortionx/`, `app/crybabyx/`, `app/tubex/`) — each has a `build.gradle` but all are **commented out** in `app/build.gradle:136-139`. These are vestigial and never built.

### Codebase Size

| Layer | Files | Lines |
|---|---|---|
| Java (app logic + UI) | 52 `.java` | ~20,700 |
| Kotlin (unused stubs) | 4 `.kt` | ~80 |
| C/C++ (audio engine) | ~30 `.cpp/.c/.h` | ~19,700 |
| XML layouts | 50 | — |
| Theme XML | 1 (`themes.xml`) | 1,552 |
| Asset themes (bitmap skins) | 6 folders | — |

---

## 3. UI Layer — Legacy XML (NOT Compose)

Despite `compose true` in `buildFeatures` and Compose dependencies in `build.gradle`, there are **zero `@Composable` functions** in the codebase. The 4 Kotlin files are auto-generated scaffolding that nothing references. The entire UI is **legacy Android XML layouts with View Binding**.

### Key Activities

| Class | Lines | Role |
|---|---|---|
| `MainActivity` | 5,276 | God Activity — navigation, plugin dialog, preset serialization, theme, billing, MIDI, camera, Bluetooth |
| `SettingsActivity` | 536 | PreferenceFragmentCompat-based settings |
| `FirebaseUI` | — | Google Sign-In flow |
| `NAMDownloader` | — | WebView to tonehunt.org for NAM model browsing |
| `ConnectGuitar`, `About`, `Camera`, `Files`, `Purchase`, `DrumMachineActivity`, `Onboard`, `FeaturedVideos`, `DeleteAccount`, `DeleteData` | — | Ancillary screens |

### Key Fragments (hosted in MainActivity)

| Fragment | Layout XML | Role |
|---|---|---|
| `Rack` (1,681 lines) | `rack.xml` | Main signal chain UI, plugin controls, mixer |
| `Presets` (443 lines) | `activity_presets.xml` | Preset browser (My/Library tabs) |
| `Tracks` (432 lines) | `tracks.xml` | Audio file player/recorder |
| `DrumMachine` | `drum_machine.xml` | Drum pattern player |
| `QuickPatch` | — | Quick patch loading |
| `MyPresets` (384 lines) | `my_presets.xml` | RecyclerView preset list |

### Navigation

**BottomNavigationView + manual Fragment show/hide** — no Navigation Component used for main flow.

5 tabs: Quick Patch → Rack (default) → Presets → Tracks → Drums

All fragments are added to the same container, toggled via `show()`/`hide()` transactions. Sub-screens launch as separate Activities.

---

## 4. Theme/Skin System — REPLACE ENTIRELY

Two independent theme systems layered on top of each other:

### 4a. Android XML Color Scheme (`themes.xml` + `SkinEngine.java`)

- ~250 combinatorial color-swap themes named `{Primary}.{Secondary}` (e.g., `Red.Pink`, `Blue.Cyan`)
- Selected via a **1,250-line `switch` statement** in `SkinEngine.setColorScheme()` (lines 72–1252)
- Maps string names to `R.style.*` resource IDs
- Theme name stored in SharedPreferences key `"color_scheme"`

**Verdict: Brute-force switch/case — not worth extending. Replace.**

### 4b. Custom Bitmap Skin Engine (`SkinEngine.java` + `Skinner.java`)

- JSON-driven bitmap skin system: 6 built-in themes (`Adwaita`, `Analog`, `Classic`, `Hybrid`, `Modern`, `TubeAmp`) each with PNG bitmaps for buttons, sliders, knobs, cards, backgrounds
- Theme config in `assets/themes/{name}/theme.json`
- Supports user-imported themes from ZIP files
- Invoked manually throughout `Rack.java`, `DataAdapter.java`, `MyPresetsAdapter.java` with `if (mainActivity.useTheme)` guards
- `SkinEngine.java` total: 1,955 lines

**Verdict: The concept (JSON-driven theme packs) is architecturally interesting, but implementation is deeply coupled to the XML layout tree. The data model is salvageable; the rendering code is not. Replace.**

---

## 5. Audio Engine Architecture (DO NOT TOUCH)

### Signal Flow

```
Input (Mic/USB) → Oboe InputStream
    → FullDuplexPass.onBothStreamsReady()
        → Input Volume adjustment
        → Plugin Chain: plugin[0].run() → plugin[1].run() → ... → plugin[N].run()
        → Output Volume adjustment
        → LockFreeQueue (for recording/metering)
    → Oboe OutputStream → Output (Headphone/Speaker/USB)
```

### Key C++ Files

| File | Role |
|---|---|
| `Engine.cpp/h` | Oboe stream lifecycle, plugin chain management, `loadPlugin()`, `addPluginToRack()`, `buildPluginChain()` |
| `FullDuplexPass.h` | **THE audio callback** — `onBothStreamsReady()` runs the plugin chain in real-time |
| `FullDuplexStream.cpp/h` | Base full-duplex I/O class |
| `Plugin.cpp/h`, `Plugin2.cpp` | LADSPA/LV2 plugin instance management |
| `PluginControl.cpp/h` | Per-plugin control port values |
| `SharedLibrary.cpp/h` | `dlopen()` loads `.so` plugin files, gets `ladspa_descriptor`/`lv2_descriptor` |
| `native-lib.cpp` | **JNI bridge** — 1,182 lines, ~90 JNI functions |
| `FileWriter.cpp` | Recording to WAV/OGG/MP3 |
| `Export.cpp` | Export codec handling |
| `LockFreeQueue.cpp` | Lock-free audio buffer passing |
| `Meter.cpp` | Metering/tuner data |
| `lv2_ext.cpp` | LV2 extension handling |
| `atom.cpp`, `symap.c`, `worker.c` | LV2 atom ports, symbol map, worker threads |
| `ringbuffer.c`, `vringbuffer.c` | Ring buffer implementations |
| `upwaker.c` | Thread wakeup mechanism |

### NAM/AIDA-X Model Loading

- NAM runs as an **LV2 plugin**: `assets/lv2/neural_amp_modeler.so/neural-amp-modeler-lv2.json`
- Models loaded via LV2 file ports: `Plugin.setFilePortValue()` / `Plugin.setFileName()` in C++
- Java side: `AudioEngine.setPluginFilename(filename, plugin)` → JNI → C++
- `NAMDownloader.java` is a WebView pointed at `https://tonehunt.org` for model browsing/download

### Chain Topology

**Mono-only serial chain.** Input → plugin[0] → plugin[1] → ... → plugin[N] → output. No parallel routing, no buses, no sends.

---

## 6. JNI Bridge (`AudioEngine.java` → `native-lib.cpp`)

~90 native methods in categories:

| Category | Key Methods |
|---|---|
| Plugin Loading | `loadLibrary`, `loadPlugins`, `getSharedLibraries`, `setLazyLoad` |
| Active Chain | `addPlugin`, `addPluginLazy`, `addPluginByName`, `deletePlugin`, `movePlugin`, `movePluginUp/Down`, `clearActiveQueue` |
| Control Values | `setPluginControl`, `getActivePluginValues`, `setPresetValue` |
| Plugin State | `togglePlugin`, `getActivePluginEnabled`, `bypass`, `pause` |
| File/Atom Ports | `getFilePort`, `setFilePortValue`, `setFileName`, `setAtomPort`, `setPluginFilename` |
| Audio Engine | `create`, `delete`, `setEffectOn`, `setAPI`, `setLowLatency`, `setSampleRate` |
| Recording | `toggleRecording`, `setExportFormat` |
| Metering/Tuner | `getTunerEnabled`, `setTunerEnabled`, `tuneLatency` |
| Volumes | `setInputVolume`, `setOutputVolume`, `toggleMixer` |
| Latency | `getLatency`, `getBufferSizeInFrames`, `fixGlitches`, `minimizeLatency` |

Static library loaded at `MainActivity.java:411`: `System.loadLibrary("amprack")`

---

## 7. Preset System

### Storage: Firebase Firestore (cloud-only)

- `FirestoreDB.java` handles all CRUD (`savePreset`, `loadUserPresets`, `deletePreset`)
- Presets stored in `"presets"` Firestore collection
- Favorites in `"collections"` collection

### Data Model (Firestore Document)

```json
{
  "name": "Preset Name",
  "desc": "Description",
  "uid": "firebase-user-id",
  "public": true,
  "likes": 0,
  "timestamp": "<ServerTimestamp>",
  "controls": {
    "0": { "name": "PluginName", "controls": "0.5;0.7;1.0", "midi": "...", "selectedModel": "2" },
    "1": { "name": "AnotherPlugin", "controls": "1.0;0.3" }
  }
}
```

- Control values are **semicolon-delimited float arrays** read from `AudioEngine.getActivePluginValues()`
- `presetToMap()` at `MainActivity.java:2573` builds the map from the live plugin chain
- Loading reconstructs the chain: iterate controls map → `AudioEngine.addPluginByName()` → `AudioEngine.setPluginControl()`

### Local Quick-Save

`Rack.java:112` writes `plugins.json` to internal storage as a JSON dump of current rack state — a quick-save, not a structured preset DB.

### Implications for Kiosk Build

Firebase Firestore preset storage **must be replaced** with a local alternative (JSON files on disk or Room/SQLite) since the kiosk will not have Google Play Services or Firebase configuration. The data model (semicolon-delimited controls map) should be preserved for backward compatibility with existing preset sharing.

---

## 8. Firebase & Google Dependencies — Strip for Kiosk

| Dependency | Used For | Strippable? |
|---|---|---|
| `firebase-auth` + `firebase-ui-auth` | Google Sign-In for preset cloud sync | Yes |
| `firebase-firestore` | Cloud preset storage | Yes — need local replacement |
| `firebase-crashlytics` + `firebase-crashlytics-ndk` | Crash reporting | Yes |
| `firebase-analytics` | Usage analytics | Yes |
| `google-services` plugin | Firebase config injection | Remove with above |
| `com.android.billingclient:billing` | In-app purchases | Yes |

**Audio engine has ZERO Firebase dependency.** All Firebase coupling is in: `Presets.java`, `FirestoreDB.java`, `FirebaseUI.java`, `DeleteData.java`, `DeleteAccount.kt`, and preset-related code in `MainActivity`/`Rack`.

**Security note:** OAuth client IDs are hardcoded in `Presets.java:96` and `FirebaseUI.java:40` — these should be removed/rotated for the public fork.

---

## 9. Effect Chain UI (Signal Chain Display)

### Add Plugin Flow

1. User taps FAB → `Rack.java` shows `mainActivity.pluginDialog` (custom Dialog)
2. Dialog has RecyclerView with `PluginDialogAdapter` listing available plugins from loaded shared libraries
3. User taps plugin → calls `AudioEngine.addPlugin(library, plugin)` (JNI)
4. Native: `Engine::addPluginToRack()` → `Plugin::load()` → wires I/O → adds to `activePlugins`
5. `Engine::buildPluginChain()` copies function pointers into `FullDuplexPass` arrays
6. Java: `DataAdapter` creates plugin UI controls (sliders, toggles, file chooser)

### Signal Chain Display

- `MainActivity` has a `RecyclerView` with `DataAdapter` — each item = one active plugin
- `DataAdapter.java` (1,301 lines) renders: plugin name, enable/bypass toggle, control sliders (Material Slider or custom `RotarySeekbar` knobs), move up/down buttons, delete button
- `RotarySeekbar.java` (1,202 lines) — custom rotary knob widget with touch gesture
- Reorder via `AudioEngine.movePluginUp/Down()` (JNI)

---

## 10. File Classification — In-Scope vs Do-Not-Touch

### DO NOT TOUCH (Audio-Engine-Critical)

These files constitute the audio processing pipeline and JNI bridge. Any modification risks audio latency regression, plugin loading failures, or crashes.

```
app/src/main/cpp/                          (ENTIRE directory — all C/C++ sources)
app/src/main/jniLibs/                      (prebuilt .a/.so plugin binaries, all ABIs)
app/src/main/assets/lv2/                   (LV2 plugin descriptors/manifests)
app/src/main/assets/rdf/                   (LADSPA RDF metadata files)
app/src/main/java/.../AudioEngine.java     (JNI method declarations — pure bridge, no UI)
```

### IN SCOPE (UI Layer — Redesign Targets)

| File | Lines | What Changes |
|---|---|---|
| `MainActivity.java` | 5,276 | Decompose God Activity; extract navigation, theme, preset, MIDI logic |
| `Rack.java` | 1,681 | Rebuild signal chain UI to horizontal rack metaphor |
| `DataAdapter.java` | 1,301 | Rebuild plugin control cards (knob layout, card styling) |
| `RotarySeekbar.java` | 1,202 | May keep or replace with new knob widget |
| `SkinEngine.java` | 1,955 | Replace entirely — new dark theme system |
| `Skinner.java` | — | Replace entirely |
| `Presets.java` | 443 | Rebuild preset browser UI (card-based) |
| `MyPresets.java` | 384 | Rebuild (integrated into new preset browser) |
| `MyPresetsAdapter.java` | — | Rebuild |
| `PluginDialogAdapter.java` | — | Rebuild (effect picker) |
| `Tracks.java` | 432 | Restyle to match new theme |
| `DrumMachine.java` | — | Restyle to match new theme |
| `SettingsActivity.java` | 536 | Restyle |
| `FirestoreDB.java` | 596 | Replace with local preset storage for kiosk |
| `FirebaseUI.java` | — | Remove (no Google Sign-In in kiosk) |
| All 50 XML layouts in `res/layout/` | — | Rebuild or heavily modify |
| `res/values/themes.xml` | 1,552 | Replace with single dark theme |
| `res/values/colors.xml` | — | Replace with ToneX-inspired palette |
| `assets/themes/*` | 6 dirs | Remove bitmap skins (new theme is code-driven) |

### GRAY ZONE (Touch With Caution)

| File | Reason |
|---|---|
| `AndroidManifest.xml` | Need to update for kiosk (DeviceAdminReceiver, package rename) but carefully — permissions and intent-filters affect audio engine behavior |
| `app/build.gradle` | Strip Firebase deps, change `applicationId` — but preserve NDK/CMake/Oboe config exactly |

---

## 11. Key Risks & Recommendations

1. **MainActivity is a God Object (5,276 lines).** Phase 2 should decompose it *before* implementing UI changes, not after. Extract navigation, preset serialization, theme application, and MIDI handling into separate classes/managers.

2. **Preset backward compatibility.** The semicolon-delimited controls format should be preserved. New local storage should read/write the same data model, just backed by JSON files instead of Firestore.

3. **Compose migration decision.** The project already has Compose dependencies but doesn't use them. Phase 1 should decide: (a) rebuild in Compose (cleaner long-term, but bigger effort), or (b) stay in XML with new layouts (faster, leverages existing View Binding and RecyclerView patterns). Recommendation: **stay in XML** for Phase 2, consider Compose migration as a Phase 4 stretch goal.

4. **APK size (255 MB).** For kiosk, build only `arm64-v8a` ABI (Pixel 3 is arm64). This should cut APK to ~70-80 MB.

5. **Firebase removal is safe.** Audio engine has zero Firebase coupling. Clean boundary through `FirestoreDB.java`. Strip Firebase, add local JSON preset storage, and the core app works identically.

6. **CMakeLists.txt fix committed.** Added `${CMAKE_CURRENT_SOURCE_DIR}` to include paths for LV2 headers. This is a legitimate build fix, not a hack.

---

## Appendix: Build Environment Setup

```bash
# Ubuntu 26.04 LTS, headless server
sudo apt install openjdk-17-jdk-headless unzip wget

# Android SDK (CLI-only)
export ANDROID_HOME=/home/hery/android-sdk
mkdir -p "$ANDROID_HOME/cmdline-tools"
wget "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O /tmp/cmdline-tools.zip
unzip /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

# Install SDK components
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;26.1.10909125" "cmake;3.18.1"

# Build
cd /home/hery/giggin-gear
./gradlew assembleDebug
```
