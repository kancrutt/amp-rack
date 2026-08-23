# AGENTS.md — Project "Giggin' Gear"

**Mission:** Fork `djshaji/amp-rack` (Android NAM/LADSPA host), redesign UI/UX toward a modern dark, hardware-inspired signal-chain aesthetic (see design pillars in Phase 1), then package it as a kiosk-mode standalone appliance OS for a Google Pixel 3.

**Protocol:** Strict phase-gate. No advancing to next phase without explicit sign-off from Hery. Every phase closes with an evidence report (command output, screenshots/recordings, pass/fail against acceptance criteria). No model locking across phases.

---

## ⚠️ CRITICAL CONSTRAINT — READ BEFORE STARTING ANY PHASE

**postmarketOS has NO audio support on Google Pixel 3 (`blueline`).** Confirmed via official postmarketOS GitLab merge request: *"Support status is similar to other SDM845 devices, however audio support is missing."*

**Consequence:** The "wipe Android, install pure Linux mobile distro" path is **not viable** — audio is the entire point of this device. Do not attempt pmOS/Mobian/Sailfish routes for this project. Do not re-research this; it is settled.

**Resolution:** "Standalone OS" for this project means **Android stripped to kiosk mode** (Device Owner + Lock Task Mode, or full custom AOSP as a stretch goal in Phase 4). Android's kernel is Linux; the userspace is Android — this is the only currently-viable interpretation of "Linux-based OS" that preserves working audio on this hardware. State this plainly to the user in any report; do not present it as a compromise without explanation.

---

## Phase 0 — Environment & Repo Discovery

**Goal:** Understand what we're actually forking before touching anything.

Tasks:
1. Fork `djshaji/amp-rack` to Hery's own GitHub, clone to Ubuntu server.
2. Set up headless Android build toolchain (Android SDK + NDK + JDK, CLI-only, no Android Studio GUI needed on server). Confirm `./gradlew assembleDebug` completes clean — this is the smoke test for the whole phase.
3. Inventory and document:
   - App module structure (`app/` — confirm whether UI is legacy XML layouts/Views or Jetpack Compose)
   - The existing "skinnable UI / themes" system mentioned in upstream README — is it a proper theme engine (worth extending) or a hardcoded color-swap (worth replacing)?
   - Where NAM / AIDA-X plugin loading hooks into the audio engine (Oboe-based) — **flag these files as do-not-touch during UI work**
   - Preset save/load data model
   - Effect chain / signal routing UI (the "add plugin to chain" flow)
4. Confirm license compatibility: upstream is Apache-2.0 — fork/redesign/rebrand is permitted, but retain attribution per license terms. Note this explicitly in the forked README.

**Deliverable:** `PHASE0_FINDINGS.md` — architecture map, build reproducibility confirmation, list of files that are audio-engine-critical (hands off) vs UI-layer (in scope).

**Gate:** Do not start Phase 1 until PHASE0_FINDINGS.md is reviewed and approved.

---

## Phase 1 — UI/UX Redesign Spec (Design Pillars)

**Goal:** A concrete, screen-by-screen redesign plan — not code yet.

Target visual language (independently defined design pillars, not tied to any third-party product):
- Dark theme, near-black background with high-contrast accent color for active/selected states
- Horizontal "rack" metaphor for signal chain — pedal/amp blocks as cards in a row, not a vertical list
- Realistic rotary knob rendering with drag-to-turn gesture (not slider-style controls)
- Card-based tone/preset browser with thumbnail art per preset
- Bottom tab navigation between Rig View / Tone Library / Settings
- Tuner as a dedicated overlay, not buried in a menu

Tasks:
1. Map each design pillar above to the equivalent AmpRack screen identified in Phase 0.
2. For each screen in scope, decide: extend the existing theme system, or replace the View with new layout/Compose. Base this decision on what Phase 0 found — don't assume, verify.
3. Produce wireframe-level descriptions (text is fine, doesn't need to be pixel art) for:
   - Main rack/signal-chain view
   - Plugin/effect picker
   - Per-plugin parameter editor (knob layout)
   - Preset browser
   - Tuner overlay
4. Explicitly note anything that must NOT change: plugin engine calls, Oboe audio callback, NAM model loading logic, preset file format (backward compatibility with existing `.nam`/preset files).

**Deliverable:** `UI_UX_SPEC.md` — screen-by-screen redesign plan, explicit "in scope" vs "do not touch" boundary.

**Gate:** Review UI_UX_SPEC.md before writing implementation code.

---

## Phase 2 — Implementation

**Goal:** Build UI_UX_SPEC.md, one screen at a time.

Tasks:
1. Implement redesign per spec, screen by screen — do not batch all screens into one giant commit; one PR/commit per screen for reviewability.
2. After each screen: verify NAM A2 loading still works, verify preset save/load still works, verify no regression in audio latency (Oboe callback timing — benchmark before/after with the same test signal).
3. Rebrand: app name, icon, splash — confirm with Hery on final naming ("Giggin' Gear") and package ID before this step, since package ID changes affect Phase 3 kiosk provisioning.

**Deliverable:** Per-screen evidence (screenshot or screen recording) + latency benchmark comparison (before redesign vs after, same device, same preset).

**Gate:** All screens implemented and evidence reviewed before Phase 3.

---

## Phase 3 — Kiosk OS Packaging ("Giggin' Gear" on Pixel 3)

**Goal:** Pixel 3 boots directly into Giggin' Gear, full-screen, with no way to reach a home screen, notification shade, or other apps.

**Recommended approach — Device Owner + Lock Task Mode** (not a full AOSP rebuild; see Phase 4 for that option):

1. Base OS: LineageOS for `blueline` (well-supported, avoids stock Google apps/services bloat that a kiosk doesn't need). Flash via standard LineageOS recovery flow.
2. Provision Giggin' Gear as **Device Owner**:
   ```
   adb shell dpm set-device-owner <package.name>/.AdminReceiver
   ```
   (requires a `DeviceAdminReceiver` implementation added to the app — this is new code, not part of Phase 2's UI work, scope it separately)
3. Configure **Lock Task Mode** whitelisting only the Giggin' Gear package — this is what actually prevents leaving the app (no back-to-home, no recent apps, no notification pull-down).
4. Auto-launch on boot: `BOOT_COMPLETED` broadcast receiver → start the main activity directly, skip any lock screen.
5. Disable via Device Policy Manager: OTA update prompts, other pre-installed app visibility, USB debugging toggle exposure (keep debugging available for you, just not for anyone poking the device at a gig).

**Deliverable:** A flashable/provisionable build + a step-by-step provisioning runbook (so you can re-provision if the phone gets wiped/replaced) + video evidence of boot-to-kiosk with no escape path.

**Gate:** Confirm kiosk mode is genuinely inescapable (test: try to reach home screen, notification shade, recent apps — all three must fail) before calling this phase done.

---

## Phase 4 (Stretch, Optional) — Full Custom AOSP Image

Only pursue if Phase 3's Lock Task Mode kiosk proves insufficient in real-world use (e.g., some Android chrome still visible, boot time too slow, want the app baked in as the literal system UI).

Requires: full AOSP build environment, Pixel 3 device tree + kernel source, 100GB+ disk, several hours build time. This is a materially bigger effort than Phase 3 — do not start without explicit go-ahead, since Phase 3 likely satisfies the actual goal (phone that only runs the pedal app) without this cost.

---

## Zero-Trust Verification Protocol (added after repeated false-complete reports)

This section exists because of a recurring pattern across this project: work gets reported as "complete," "verified," or "fixed," and Hery's own device testing repeatedly reveals it wasn't — old skeuomorphic screens left untouched while only Rig View got redesigned, "Get Pro" text still present after removal was requested, routing-mode switching breaking the chain render, double-tap-to-parameter silently not working, card sizing issues resurfacing after being marked fixed. This is not one-off — it has happened across Phase 2.2 through the visual-enhancement addendum work (knob rendering, drag-and-drop chain, landscape reflow). The root cause each time was reporting based on code review / build success rather than actually exercising the running app and looking at what renders.

**Zero-trust rule: assume your own implementation is broken until you have personally reproduced it working on the actual running build.** Code compiling is not evidence. A build succeeding is not evidence. A feature being described correctly in your own report is not evidence. The only acceptable evidence is: you launched the build (Appetize or, when possible, a build handed to Hery for physical-device testing), personally exercised the exact user action being reported on, and captured what actually happened.

**Mandatory self-challenge before any completion report:**

1. **Enumerate, don't assume scope.** Before claiming a UI/screen task is done, grep the actual layout/fragment/activity structure to get the full list of screens/dialogs in the app. Do not assume "the redesign" means only the screen you were actively working on — confirm whether the request implied full-app coverage, and if so, check every screen against that list, not just the one you touched.

2. **Reproduce the exact user action, not an adjacent one.** If the task is "double-tap opens parameter editor," actually double-tap it in a live session. If the task is "switching routing mode reformats the chain visually," actually switch it and look at the result, don't just confirm the toggle UI renders. Testing a nearby but different behavior and reporting the target behavior as fixed is exactly the failure pattern being corrected here.

3. **Pull logcat for the specific action being verified**, even when there's no crash. Absence of a crash is not the same as correct behavior — several bugs in this project (adapter desync, movePlugin stub, routingMode parse failure) were silent/non-fatal but still broke functionality. A clean logcat during the exact reproduction of the target behavior is part of the evidence, not just a crash-free launch.

4. **Screenshot or screen-record the actual result**, attached or described precisely enough that Hery could distinguish a real screenshot from a plausible-sounding description. Text description of a UI change is not sufficient evidence on its own — this has been explicitly true since the "Complete" Phase 2.2 report understated the true screen-audit gaps.

5. **Check for regressions on previously-fixed items** before closing a new one. Several issues in this project resurfaced after being marked fixed (card sizing, double-tap). Before reporting a new fix, re-verify at least the 2-3 most recently closed items are still actually working in the current build — don't assume a fix is permanent just because it was true once.

6. **State uncertainty plainly when self-testing isn't possible.** If a verification step genuinely cannot be done in this environment (e.g., no local emulator, Playwright unavailable), say so explicitly and hand the exact reproduction steps to Hery rather than reporting the item as done anyway. An honest "I could not verify X, here's exactly how to check" is always acceptable. A false "verified" is not.

**Reporting format going forward:** every completion report must separate what was *self-tested and confirmed working* from what was *implemented but not yet independently verified*. Do not blend these into a single "done" status. If self-testing surfaces a problem, fix it and re-test before reporting at all — don't report the broken state and wait for Hery to catch it.

---

## Current Status (Updated — reflects actual state, not original phase plan)

Phase 2 has expanded substantially beyond the original screen-by-screen plan and is still open — not yet gated to Phase 3. Recorded here so status doesn't have to be reconstructed from scrollback.

**Navigation model changed from the original spec.** Phase 1's "Rig View / Tone Library / Settings" 3-tab plan was superseded by a 5-tab model: **BOARD / PRESET / TUNER / LOOPS & DRUM / SETTING** (icon + text label, not icon-only). This is the current approved navigation — treat it as canonical over the older Phase 1 wireframe language.

**Signal chain is now a full node-graph canvas, not a static horizontal card row.** Built across C1 (GraphSolver DAG engine, native JNI topology bridge, series/parallel execution) and C2 (SignalChainCanvasView — draggable nodes, port-to-port wire creation/deletion, visual branching for parallel routing, explicit INPUT/OUTPUT anchor nodes). This is core functionality now, not a visual nice-to-have — do not let future UI passes silently regress it back to a linear chain (this happened once already during the 5-tab redesign and had to be confirmed-not-broken via code citation).

**Icon system finalized:** two-axis color model — HUE = category identity (Amp/Cabinet/Distortion/Modulation/Delay/Reverb/Dynamics/EQ/Pitch/Wah/Volume/Noise Gate each have a fixed hue), SATURATION/GLOW = state (dimmed when inactive, full+glow when active). Documented in UI_UX_SPEC.md Section 4 (superseded any earlier ToneX-referenced or monochrome-only language). Implemented via real `colors.xml` resources and `android:state_activated` selectors — never generic `?attr/colorControlNormal`, which was a recurring root cause of "icons all render one color" bugs early on.

**Device testing has expanded beyond the Pixel 3.** Hery is now also testing on a Samsung S23 and an Android tablet, and most recently an Alldocube iPlay 60 mini Pro (LineageOS) surfaced in a crash log. Pixel 3 remains the target for eventual Phase 3 kiosk provisioning, but responsive-layout correctness now needs to hold across all these form factors, not just Pixel 3 — a recurring class of bug (content clipped on phone-class screens, fine on tablets) has resurfaced multiple times and needs verification per-device-class going forward, not just on whichever device happened to be tested most recently.

**Known standing risk:** release builds (signed, non-debuggable, used to avoid Play Protect scan prompts on sideload) must be checked for R8/minification interference with any reflection- or name-based native/JNI lookups before being treated as equivalent to debug builds for testing purposes.

**Not yet gated to Phase 3.** Kiosk packaging has not started. Do not begin Phase 3 work until Phase 2's outstanding responsive-layout and native-crash issues (see current round) are resolved and confirmed across device classes.

---



- Evidence over assertion: command output, screenshots, benchmarks — not "it should work now."
- Explicit pass/fail against the phase's stated acceptance criteria.
- Flag any deviation from this plan immediately rather than silently improvising — especially if Phase 0 findings suggest the theme system or audio engine is structured differently than assumed here.
