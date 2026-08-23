# UI_UX_SPEC.md — Giggin' Gear Redesign Spec (ToneX Control Reference)

**Date:** 2026-08-13  
**Status:** PROPOSED (Final Revision for Hery Sign-Off)  
**Target Hardware:** Google Pixel 3 (`blueline`) running Kiosk Mode Android  
**Visual Reference:** IK Multimedia ToneX Control (`com.ikmultimediaus.android.tonexcontrol`)

---

## 1. Executive Summary & Design Goals

"Giggin' Gear" transforms the open-source AmpRack audio plugin host into a dedicated, stage-ready pedalboard appliance. The primary objective is replacing AmpRack's multi-color/skinnable XML UI with a sleek, dark, hardware-like visual language inspired by IK Multimedia ToneX Control, while preserving AmpRack's low-latency native audio engine (Oboe + LADSPA/LV2/NAM).

### Key Design Pillars
1. **Dark, Hardware-Like Aesthetic**: Near-black background (`#121214`), dark matte surfaces, subtle metallic borders, high-contrast amber/gold (`#FFAA00`) and green accents (`#00E676`) for active states.
2. **Horizontal "Rack" Metaphor**: Signal chain displayed as a horizontal row of stompboxes / amp heads that scrolls horizontally, replacing AmpRack's vertical list.
3. **Realistic Rotary Controls**: Touch-friendly rotary knobs with vertical drag gestures, status LEDs, and numeric value readouts.
4. **Stage-Optimized Navigation**: Large touch targets (minimum 48dp, recommended 56dp+ for stage use with fingers), permanent 4-tab bottom navigation (**Rig View**, **Presets**, **Tuner**, **Settings**), zero modal distraction.
5. **Zero Cloud Dependency**: 100% offline local JSON preset storage. Firebase stripped.

---

## 2. ToneX Control Visual Language Mapping

| ToneX Control Pattern | AmpRack Legacy State | Giggin' Gear Redesign Target |
|---|---|---|
| **Dark Theme** (`#121214` background, dark grey cards) | ~250 XML color schemes + 6 bitmap skins (`SkinEngine.java`) | Single dark theme across all screens; eliminate bitmap skins & switch statement |
| **Horizontal Pedal/Amp Chain** | Vertical `RecyclerView` in `Rack.java` | Horizontal scrolling `RecyclerView` (`LinearLayoutManager.HORIZONTAL`) with pedal-styled cards |
| **Rotary Knobs with Drag Gesture** | `RotarySeekbar.java` (custom View) & Android `Slider` | Refactored `RotarySeekbar` with clean dark rendering, arc progress indicator, vertical touch drag |
| **Card-based Preset Browser** | Vertical list in `MyPresets.java` / `Presets.java` | Grid/Card layout with amp/pedal icon tags, author, date, and quick-load buttons |
| **Bottom Navigation Bar** | `BottomNavigationView` with 5 tabs | 4 core stage tabs: **Rig View** (Rack), **Presets**, **Tuner** (Overlay), **Settings** |
| **Dedicated Tuner Overlay** | Buried in Rack menu/toolbar | Instant full-screen or slide-up sheet overlay triggered from bottom bar |

---

## 3. Screen-by-Screen Redesign Plan

```
+-------------------------------------------------------------------+
|                        TOP BAR (Input/Output Meter, BPM, Status) |
+-------------------------------------------------------------------+
|                                                                   |
|   +--------------+   +--------------+   +--------------+   [+]   |
|   |  OVERDRIVE   |   |   NAM AMP    |   |    CAB IR    |  ADD  |
|   | (O) (O) (O)  |   | (O) (O) (O)  |   | (O) (O) (O)  | BLOCK |
|   | [ENABLE]     |   | [ENABLE]     |   | [ENABLE]     |       |
|   +--------------+   +--------------+   +--------------+       |
|            <--- Horizontal Scrollable Signal Chain --->           |
|                                                                   |
+-------------------------------------------------------------------+
|               PARAMETER EDITOR PANEL (Expanded Block)             |
|   [ GAIN ]     [ BASS ]     [ MID ]     [ TREBLE ]    [ VOL ]     |
|     (O)          (O)          (O)          (O)          (O)       |
|    6.5          4.0          5.5          7.0          8.0        |
+-------------------------------------------------------------------+
|  [ RIG VIEW ]    [ PRESETS ]    [ TUNER ]    [ SETTINGS ]        |
+-------------------------------------------------------------------+
```

### 3.1 Rig View (Main Signal Chain & Rack)
- **Layout**: `fragment_rack.xml` (replace layout root with horizontal chain + parameter expansion drawer).
- **Signal Flow**: Left-to-right audio flow visualization (Input -> Block 1 -> Block 2 -> ... -> Output).
- **Pedal Cards**: Each active plugin rendered as a hardware pedal or amp block:
  - Header: Plugin category badge (DISTORTION, AMP, CAB, MOD, DELAY, REVERB) + Name.
  - Foot-switch button: Glows green (`#00E676`) when active, dark grey (`#2C2C30`) when bypassed.
  - Top 3 primary knobs rendered directly on the card; extra controls expand into the Parameter Editor.
  - Drag handle / reorder buttons: Long-press or top handles to drag-reorder blocks in chain.
- **Add Block Button**: Fixed card at the end of the chain (`[+] ADD BLOCK`) to trigger Plugin Picker.

### 3.2 Plugin Picker (Add Plugin Dialog)
- **Layout**: Replace `mainActivity.pluginDialog` custom XML dialog with a full-height `BottomSheetDialogFragment` (`dialog_plugin_picker.xml`).
- **Category Filter Tabs**: ALL, AMPS (NAM/AIDA-X), DISTORTION, MODULATION, DELAY/REVERB, UTILITY.
- **Card Items**: Grid of available LADSPA/LV2 plugins showing:
  - Plugin title and description
  - Category icon
  - CPU usage indicator (low / medium / heavy for NAM models)
  - Quick-add button.

### 3.3 Parameter Editor (Per-Plugin Detailed Control)
- **Layout**: Inline expandable drawer below the rack, or fullscreen when a pedal card is tapped.
- **Knob Grid**:
  - Up to 8 high-precision rotary knobs arranged in 1 or 2 rows.
  - Touch gesture: Vertical drag anywhere on knob area (drag up = increase, drag down = decrease). Double-tap resets to default.
  - Label above, numeric readout below (e.g. "6.5 dB", "120 ms", "44.1 kHz").
- **File / Model Selector Ports**:
  - Dedicated file picker card for NAM (`.nam`) model files and Impulse Response (`.wav`) cabinet files with quick file browser.

### 3.4 Preset Browser
- **Layout**: `fragment_presets.xml` (rebuild into dark card grid).
- **Tabs**: **My Presets** (local storage) | **Factory Presets** (pre-bundled stage rigs).
- **Preset Card**:
  - Rig Title (e.g. "Plexi Crunch + TS9", "Clean Chorus Twin")
  - Tag Pills: [NAM] [CAB] [DELAY]
  - Creation Date & Author
  - Quick Load button (instant JNI preset swap without closing browser)
  - Save Current Rig button (FAB floating over bottom-right).

### 3.5 Tuner Overlay
- **Layout**: Fullscreen / Halfscreen modal overlay (`dialog_tuner.xml`).
- **Visual Meter**: Arc needle or horizontal cents bar (-50 to +50 cents).
- **Note Display**: Giant note readout (e.g. **E2**, **A2**, **D3**) in center with frequency (Hz).
- **Color Feedback**: Green highlight when in tune (+/- 3 cents), Red/Amber when flat/sharp.
- **Mute Audio toggle**: Mutes engine output automatically when tuner opens (configurable).

### 3.6 Settings Screen
- **Layout**: `fragment_settings.xml` (restyled preference layout adhering to dark hardware aesthetic).
- **Sections**:
  1. **Audio Hardware & Latency**:
     - Audio API Selector: AAudio (Recommended for low latency) vs OpenSL ES.
     - Sample Rate: 44.1 kHz / 48.0 kHz.
     - Buffer Size & Latency Tuner trigger (`AudioEngine.latencyTuner()`).
     - Input & Output Device Selection (USB Interface vs Built-in Mic/Headphone jack).
  2. **MIDI Control Mapping**:
     - Connected MIDI Device Selector (USB MIDI pedalboards / foot controllers).
     - MIDI CC/PC Learn Mode toggle.
  3. **Kiosk & Appliance Behavior**:
     - Keep Screen Awake (Always ON for stage performance).
     - Auto-Boot to Rig View on launch.
     - Lock Task Mode Status (Kiosk active / inactive).
  4. **Utility Tools**:
     - Access Backing Track Recorder / Player (secondary utility screen, moved off main stage nav bar).
     - Export / Backup all local JSON presets to external storage.

### 3.7 Scope Decision: Navigation Tabs & Secondary Features
- **Core Navigation (4 Tabs)**: **Rig View**, **Presets**, **Tuner**, **Settings**.
- **Scope Cut**: "Tracks" (backing track player) and "Drums" (drum machine) are **removed from the primary bottom navigation bar**.
- **Rationale**: For a stage-focused pedalboard kiosk appliance, screen real estate and navigation simplicity are critical. Guitarists on stage need instant access to tone tweaking, preset changing, tuning, and basic settings. Auxiliary audio playback features clutter stage navigation and are relegated to a sub-menu under Settings.

---

## 4. Iconography, Color, Node-Canvas Typography & Splash Screen (FINAL REPLACEMENT)

**Status:** Supersedes all prior Section 4 content, including any remaining ToneX Control references. This is the single source of truth for iconography, color, node-canvas typography, and splash screen — implement exactly as written, do not reinterpret shapes/values.

### 4.1 Core Icon Style
- **Grid:** 24dp x 24dp bounding box.
- **Stroke Weight:** Uniform 2dp for all line-art elements.
- **Caps & Joins:** 1.5dp rounded caps and joins.
- **Aesthetics:** Minimalist, geometric, clean vector line art. No gradients, no skeuomorphism, no solid-fill blocks (stroke-based, not filled shapes, except where explicitly noted e.g. knob dots).

### 4.2 Color & State Logic (Two-Axis System)
**HUE = category identity** (constant per category). **SATURATION/GLOW = state** (inactive vs active/engaged). This is the approved final system — do not revert to monochrome-only or single-accent-only.

#### Category hues (final, approved)

| Category | Hue | Approved pastel hex | Inactive dim hex |
|---|---|---|---|
| Amp | Warm muted rose | `#D96B58` | `#5A2C26` |
| Cabinet/IR | Neutral silver (not hue-coded) | `#D0D0D0` | `#555555` |
| Distortion/Overdrive | Dusty coral | `#E07A5F` | `#5E3326` |
| Modulation | Muted blue | `#5C82E6` | `#24345E` |
| Delay | Sage green | `#52B788` | `#214A37` |
| Reverb | Muted purple | `#9D65C9` | `#3E2752` |
| Dynamics/Compressor | Warm amber | `#E09F3E` | `#5C4119` |
| EQ | Olive green | `#8CB369` | `#384729` |
| Pitch/Synth | Dusty violet | `#7B52AE` | `#322247` |
| Wah/Filter | Soft teal | `#4EA8DE` | `#1F455C` |
| Volume/Pan | Muted mint | `#45B89D` | `#1C4A3D` |
| Noise Gate | Pale amber (same family as Dynamics — distinguish by glyph shape) | `#D4A853` | `#5C4721` |

*Approved 2026-08-23. These muted/pastel tones replace the earlier fully-saturated hues per Hery's review.*

#### State encoding
- **Inactive/bypassed:** desaturated/dimmed version of the category hue (~40% saturation, muted). Applied via multiply/desaturate, never by swapping to a flat unrelated dim color and never via `PorterDuff.Mode.SRC_IN` on a multi-tone icon (SRC_IN flattens all non-transparent pixels to one color and will destroy any icon with more than one tone, e.g. Reverb's 3-arc decay glyph).
- **Active/engaged:** full-saturation category hue with a subtle glow.
- **Implementation:** real per-category color resources in `colors.xml` (`cat_<name>_sel` for active, `cat_<name>_dim` for inactive), referenced directly in each icon's `strokeColor`/`fillColor` — never through generic theme attrs like `?attr/colorControlNormal`, which apply one shared color to every icon regardless of category and defeat this entire system. Any selector must use `android:state_activated` — `android:state_active` is not a valid Android framework attribute and fails silently.

### 4.3 Plugin Category Icon Glyphs
- **Amp:** rectangular amp-head silhouette — outer panel outline, a row of 5 knob dots across the top control panel, horizontal vent-grille lines in the lower section. Not a vacuum tube, not a full amp+cab stack.
- **Cabinet/IR:** speaker cabinet silhouette — rectangular box outline with a grille texture pattern, or a single driver cone with mounting bolts if a simpler glyph is needed at small size. Neutral gray-white, no category hue.
- **Distortion/Overdrive:** clipped/squared waveform (flat-topped peaks showing clipping) — must read distinctly from Delay at small size.
- **Modulation:** LFO sine wave cycle.
- **Delay:** discrete repeating echo pulses (distinct shape from Distortion's clipped wave and from Reverb's decay arcs).
- **Reverb:** three wave-arcs of decreasing height trailing off left-to-right, representing decay over time. Distinct from Delay's discrete pulse motif — this is a decay/trail shape, not repeated identical pulses.
- **Dynamics/Compressor:** waveform with a horizontal clamp/threshold line across the peaks.
- **EQ:** frequency curve with slider ticks, or a simple multi-band curve line.
- **Pitch/Synth:** musical note combined with a small waveform.
- **Wah/Filter:** sweep curve (single S-curve or low-pass filter slope shape).
- **Volume/Pan:** vertical fader with L/R arrows.
- **Noise Gate:** waveform with a vertical gate-threshold line cutting through it.

### 4.4 Navigation Icon Glyphs (previously missing from the repo — this is the authoritative definition)

These four icons were approved early in the project but were never actually committed as drawable definitions anywhere in the repo or prior spec revisions — a grep of the codebase confirms zero references. This section is the first real definition; implement from here.

| Icon ID | Nav Tab | Glyph |
|---|---|---|
| `ic_nav_rig` | Rig View | Three small connected rectangles in a row (representing pedal blocks in a chain), joined by short connector lines |
| `ic_nav_presets` | Presets | Stacked card deck — 2-3 overlapping rounded rectangles with a small bookmark/tag tab on the top card |
| `ic_nav_tuner` | Tuner | Tuning fork silhouette with 1-2 small curved lines beside the tines suggesting vibration/sound waves |
| `ic_nav_settings` | Settings | Precision 6-tooth gear outline, evenly spaced teeth, clean circular hub in the center — no malformed/repeating arc artifacts |

State: same two-state system — inactive = muted silver `#8E8E93` outline, active (selected tab) = filled or glowing outline in the app's single navigation accent color `#FFAA00` (navigation icons use ONE shared accent, not per-category hue coding — hue-coding is for plugin categories only, not app-level navigation).

### 4.5 Chain/Interaction & Status Icons
- **Drag handle:** 6-dot grip (2×3 matrix), muted silver.
- **Add block:** rounded square outline with centered `+`, accent color `#FFAA00`.
- **Delete/trash:** standard trash-can outline.
- **Signal flow connector:** simple directional arrow/chevron, used both for the static series-chain divider and as the base style for the C2 canvas wire endpoints.
- **Series/Parallel routing indicator:** `[A]→[B]→[C]` for series, branching fork glyph for parallel — muted silver inactive, `#FFAA00` when that mode is the active routing state. Note: once C2's graph-driven routing fully replaces the toggle (per the approved "full replace" decision), these glyphs' role shifts to passive iconography inside the canvas rather than an interactive top-bar toggle — do not remove them from the icon set, their usage context changes, not the requirement to have them.
- **Footswitch/bypass:** circular stomp-switch glyph, dark gray `#2C2C30` when bypassed, green `#00E676` when engaged.
- **Tuner mute:** speaker-with-slash glyph, outline when unmuted, red `#FF3B30` when muted.
- **CPU/status indicator:** small numeric-badge-friendly icon (chip or activity-pulse glyph), paired with the live CPU % text.
- **Level meter clip warning:** triangle-exclamation glyph, red `#FF3B30`.

### 4.6 Node-Canvas Typography & Sizing Rules (C2 Signal Chain Canvas)

Added after a confirmed layout failure where node labels overlapped port dots and overflowed node bounds. These rules are mandatory for `SignalChainCanvasView`:

- **Node width is NOT fixed.** Auto-size each node's width to its label's measured text width, with a minimum width (enough to comfortably fit the icon + a short label) and a maximum width cap (~280dp). If a label's measured width exceeds the cap, either wrap to 2 lines or ellipsize — never let text render past the node's own background bounds, and never let text visually overlap a port dot (leave fixed padding, e.g. 12dp, between text end and the nearest port).
- **Icon + label share one row**, icon left-aligned inside the node header, label text immediately to its right, both vertically centered together.
- **Port dots sit outside the text-safe zone** — port hit-radius (currently 30px per the last implementation round) must not visually or functionally overlap the label text bounds at any node width.
- Apply the same auto-sizing/overflow-safety logic to the horizontal card-row view (pre-C2 fallback / Presets browser cards, wherever plugin names render), not just the canvas — the same class of bug (fixed-width containers with unbounded text) should be audited everywhere plugin/preset names are displayed.

### 4.7 Splash Screen
- Background must be the app's dark theme color `#121214` on all supported API levels — this includes both the legacy `Theme.MySplash` path (pre-API 31) and the `Theme.SplashScreen` path (API 31+, in `values-v31/themes.xml`). Both theme definitions must independently set a dark background; verify neither silently falls back to a default light/white system background due to a missing `windowBackground` declaration on the `NoActionBar` variant actually used by the launching Activity.
- No white, default-light-theme, or system-default splash background is acceptable in any configuration.

---

**Implementation note for whoever applies this:** this document replaces the old ToneX Control-referenced Section 4 wholesale. Do not merge/preserve old monochrome-only or single-accent-only color language — the two-axis category system in 4.2 is final.

## 5. Implementation Strategy (Per-Screen View Decisions)

| Screen | Existing Class | Strategy | Layout Action |
|---|---|---|---|
| **Rig View** | `Rack.java` | **Refactor Class + Replace Layout** | Replace vertical layout with horizontal `RecyclerView` |
| **Plugin Picker** | `MainActivity.java` (dialog) | **Extract to `PluginPickerFragment`** | Replace legacy Dialog layout with `BottomSheetDialogFragment` |
| **Parameter Editor** | `DataAdapter.java` | **Refactor Adapter + RotarySeekbar** | Restyle `RotarySeekbar.java`, replace item XML |
| **Preset Browser** | `Presets.java` & `MyPresets.java` | **Combine & Replace Layout** | Unified local JSON browser using `RecyclerView` |
| **Tuner Overlay** | `Rack.java` (inline tuner) | **Extract to `TunerDialogFragment`** | Dedicated high-visibility layout overlay |
| **Settings Screen** | `SettingsActivity.java` | **Restyle Layout** | Dark theme layout with hardware & latency settings |

---

## 6. Explicit "Do Not Touch" Boundaries

During all Phase 2 UI implementation steps, the following files and sub-systems MUST NOT be altered, edited, or refactored:

1. **C++ Native Audio Engine** (`app/src/main/cpp/*`):
   - `Engine.cpp`, `FullDuplexPass.h`, `Plugin.cpp`, `SharedLibrary.cpp`, `native-lib.cpp`, etc.
   - Exception: The `CMakeLists.txt` include-path line fix added during Phase 0 remains intact.
2. **Prebuilt Plugin Binaries** (`app/src/main/jniLibs/*`):
   - All `.so` and `.a` files across all ABIs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
3. **LV2 / LADSPA Assets** (`app/src/main/assets/lv2/*`, `app/src/main/assets/rdf/*`).
4. **JNI Declaration Bridge** (`AudioEngine.java`):
   - Do not edit method signatures, JNI method names, or class package path `com.shajikhan.ladspa.amprack.AudioEngine`.

---

## 7. MainActivity.java Decomposition Plan

`MainActivity.java` currently stands at **5,276 lines**, serving as a monolithic God Activity. Safe screen-by-screen redesign in Phase 2 requires extracting non-UI responsibilities and decoupling UI fragments from direct MainActivity field references.

### 7.1 Responsibility Mapping & Extraction Matrix

| Responsibilities / Feature | Approx Line Range | Target Component | JNI Calls Involved (`AudioEngine.*`) | Tangle & Coupling Description | Extraction Risk |
|---|---|---|---|---|---|
| **App Boot, Permissions & Splash** | 1–850 | `MainActivity.java` (Core Lifecycle) | `create()`, `setLibraryPath()`, `setLazyLoad()` | Clean lifecycle. Receives permissions, initializes native engine. | **LOW** |
| **Navigation & Tab Switching** | 851–1020 | `NavigationManager.java` | None | Manages `BottomNavigationView` with 4 core tabs (**Rig View**, **Presets**, **Tuner**, **Settings**). | **LOW** |
| **Theme / Skin Management** | 1021–1380 | `ThemeManager.java` (or replace) | None | `SkinEngine.java` calls and theme switch statement. Highly tangled with XML views across all fragments. | **MEDIUM** |
| **Plugin Dialog & Selection** | 1381–1500 | `PluginPickerFragment.java` | `getSharedLibraries()`, `getLibraryName()`, `getPlugins()`, `addPlugin()` | Dialog created inline inside MainActivity. `Rack.java` calls `mainActivity.pluginDialog`. Tangled with JNI. | **MEDIUM** |
| **Audio Device & I/O Setup** | 1501–1650 | `AudioDeviceManager.java` | `setRecordingDeviceId()`, `setPlaybackDeviceId()`, `debugInfo()`, `delete()` | Audio device enumeration & permission checks. | **LOW** |
| **Engine Lifecycle & State** | 1651–1780 | `AudioEngineManager.java` | `setEffectOn()`, `wasLowLatency()`, `getRecordingFileName()` | Controls engine start/stop, low-latency warning dialogs. | **LOW** |
| **Preset Serialization & Firestore** | 1781–2450 | `PresetManager.java` (Local Storage) | `getActivePlugins()`, `getActivePluginValues()`, `getActivePluginName()`, `addPluginByName()`, `setPluginControl()` | Preset serialization (`presetToMap`) reads directly from JNI and writes to Firebase Firestore. Deeply tangled with UI toasts & progress dialogs. | **HIGH** |
| **Plugin Chain Loading & Lazy Load** | 2451–2540 | `AudioEngineManager.java` | `addPlugin()`, `addPluginLazy()`, `addPluginLazyLV2()`, `loadLibrary()`, `loadPlugins()` | JNI plugin loading helpers. | **MEDIUM** |
| **File Import / Model Loading** | 2541–2800 | `ModelImportManager.java` | `setPluginBuffer()`, `setAtomPort()`, `setPluginFilename()` | Handles SAF URIs for `.nam` and `.wav` IR files. Reads audio files, converts samples, passes arrays via JNI. | **HIGH** |
| **MIDI Controller Handling** | 2801–3500 | `MidiManager.java` | `setPluginControl()`, `togglePlugin()` | Listens to USB MIDI events, maps CC/PC to plugin parameters. | **MEDIUM** |
| **Camera & Video Recording** | 3501–4200 | `CameraManager.java` (or optional module) | `toggleVideoRecording()`, `getTimeStamp()` | Video recording feature. Non-essential for core pedalboard kiosk. | **LOW** |
| **Billing / Purchases** | 4201–4600 | Strip / Remove | None | Google Play Billing Client integration. Incompatible with kiosk mode. | **LOW** |
| **Miscellaneous Utility / Toasts** | 4601–5276 | `AppUtils.java` | `toggleMixer()`, `printActiveChain()` | General utility methods, toast wrappers, file helper functions. | **LOW** |

### 7.2 Key Tangles & Circular Dependencies

1. **Fragment -> MainActivity Direct Field Access**:
   - `Rack.java` reaches into `mainActivity.pluginDialog`, `mainActivity.totalPlugins`, `mainActivity.useTheme`, `mainActivity.lastRecordedFileName`.
   - `DataAdapter.java` reaches into `mainActivity.useTheme`, `mainActivity.theme`, `mainActivity.quickPatch`.
   - `Presets.java` reaches into `mainActivity.presetToMap()`, `mainActivity.loadPreset()`.
   - **Resolution**: Introduce interface callbacks / ViewModels (`AudioEngineViewModel`, `PresetViewModel`) to break direct activity field access before altering layouts.

2. **UI & JNI Tangle in Preset Serialization (`presetToMap`)**:
   - `presetToMap()` at lines 2573-2610 reads live plugin values directly via `AudioEngine.getActivePluginValues(i)` and builds a HashMap while displaying UI progress indicators.
   - **Resolution**: Extract `presetToMap` into a pure Java `PresetSerializer` utility class that takes a snapshot of `AudioEngine` state without touching any UI views or toasts.

3. **File Port / NAM Sample Conversion Tangle**:
   - Lines 1950–2080 read audio samples from ContentResolver URIs, convert them into `float[]` arrays, and push them to JNI via `AudioEngine.setPluginBuffer()`.
   - **Resolution**: Isolate audio file loading into `AudioFileLoader.java`.

---

## 8. Preset Storage Migration Plan

### 8.1 Offline Local JSON Decision
- **Confirm**: Firebase Firestore is completely removed and replaced with local offline JSON storage.
- **Rationale**: Kiosk devices (Google Pixel 3 running custom LineageOS) operate standalone on stage without Google Play Services or guaranteed network connectivity.
- **Firebase Removal**: Strip `firebase-auth`, `firebase-firestore`, `firebase-crashlytics`, `firebase-analytics`, and `com.google.gms.google-services` plugin from `build.gradle`. Delete `FirestoreDB.java` and `FirebaseUI.java`.

### 8.2 Local Preset JSON File Schema

Presets will be stored as `.json` files in the app's private internal storage directory (`/data/user/0/com.shajikhan.ladspa.amprack/files/presets/`) and exportable/importable from external storage.

#### Schema Definition (`preset_schema_v1.json`)

```json
{
  "schema_version": 1,
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Plexi Crunch + TS9",
  "description": "80s Hard Rock Lead Tone with NAM JCM800 and Tube Screamer",
  "author": "GigginGear Stage",
  "created_at": 1770979200000,
  "modified_at": 1770979200000,
  "tags": ["NAM", "CAB", "OVERDRIVE"],
  "favorite": true,
  "master_input_volume": 1.0,
  "master_output_volume": 0.85,
  "plugins": [
    {
      "index": 0,
      "name": "Tube Screamer TS9",
      "library": "libOverdrive.so",
      "unique_id": 1042,
      "enabled": true,
      "controls": [
        { "index": 0, "name": "Drive", "value": 0.45 },
        { "index": 1, "name": "Tone", "value": 0.60 },
        { "index": 2, "name": "Level", "value": 0.80 }
      ],
      "file_ports": []
    },
    {
      "index": 1,
      "name": "Neural Amp Modeler",
      "library": "neural_amp_modeler.so",
      "unique_id": 9001,
      "enabled": true,
      "controls": [
        { "index": 0, "name": "Input Level", "value": 0.0 },
        { "index": 1, "name": "Output Level", "value": -3.0 }
      ],
      "file_ports": [
        {
          "port_index": 0,
          "port_name": "NAM Model File",
          "file_name": "JCM800_Crunch.nam",
          "relative_path": "models/JCM800_Crunch.nam"
        }
      ]
    },
    {
      "index": 2,
      "name": "Impulse Response Cab",
      "library": "libCabIII.so",
      "unique_id": 2015,
      "enabled": true,
      "controls": [
        { "index": 0, "name": "Blend", "value": 1.0 }
      ],
      "file_ports": [
        {
          "port_index": 0,
          "port_name": "IR Wave File",
          "file_name": "Marshall_4x12_V30.wav",
          "relative_path": "ir/Marshall_4x12_V30.wav"
        }
      ]
    }
  ]
}
```

### 8.3 Backwards Compatibility with Legacy AmpRack Presets

AmpRack's legacy Firestore format uses a semicolon-delimited float string for controls:
`"controls": { "0": { "name": "PluginName", "controls": "0.5;0.7;1.0" } }`

The new `LocalPresetManager.java` will include an automatic **Legacy Converter**:
1. When importing a legacy preset string/JSON, parse the semicolon-delimited `controls` string (`"0.5;0.7;1.0"` -> `[0.5, 0.7, 1.0]`).
2. Map float positions to control indices `0, 1, 2...`.
3. Save output as a valid `preset_schema_v1.json` file.

---

## 9. Summary of Deliverables & Phase 2 Roadmap

Upon approval of `UI_UX_SPEC.md`, Phase 2 will execute screen-by-screen in distinct PRs/commits:

- **Phase 2.1**: Decompose `MainActivity.java` & Strip Firebase (Create `LocalPresetManager`, break circular references).
- **Phase 2.2**: Rig View Redesign (Horizontal pedalboard `RecyclerView` & cards in `Rack.java`).
- **Phase 2.3**: Parameter Editor & Rotary Knob Redesign (Refactor `RotarySeekbar.java` & control cards in `DataAdapter.java`).
- **Phase 2.4**: Plugin Picker Modal (BottomSheetDialogFragment).
- **Phase 2.5**: Preset Browser Redesign (Local JSON grid browser in `Presets.java`).
- **Phase 2.6**: Tuner Overlay (Dedicated modal overlay).
- **Phase 2.7**: Rebranding & Kiosk Preparation (Package ID, app icon, splash screen, `DeviceAdminReceiver` hooks).

---
