# AGENTS.md — Giggin' Gear Android Project

## Codegraph Setup (First Run)

**Index project ini ke codegraph:**

```bash
cd /home/hery/giggin-gear
/home/hery/.npm-global/bin/codegraph-mcp --mcp
```

**Yang WAJIB di-index:**
- `app/src/` — main source (kotlin/java + resources)
- Kalau ada module lain (domain, data), index per module

**Yang JANGAN di-index (exclude):**
- `build/` — generated code (R.java, GeneratedMessageV)
- `.gradle/` — gradle cache
- `android-sdk/` — system headers (NDK), BUKAN project ini
- `app/build/` — compiled output
- `*.apk`, `*.aab`
- Any `**/generated/` directories

**Kalau codegraph udah jalan, AI bisa:**
- Symbol search tanpa grep
- Understand code structure
- Navigate references & definitions
- Better refactoring support

## Android Skills Auto-Invocation

Official Android Skills dari Google (`android/skills`) udah terpasang di `~/.config/opencode/skills/`.

**WAJIB: sebelum kerjain task Android, cek skill yang relevan dan auto-invoke.**

Aturan:
- Baca `SKILL.md` skill yang relevan SEBELUM ngerjain task-nya, jangan ngarang dari pengetahuan umum.
- Auto-invoke berdasar jenis task (mapping di bawah).
- Skill lama kalah: kalau skill resmi Android konflik sama kebiasaan lama, ikutin skill resmi.

**Mapping task → skill:**

| Task | Skill |
|---|---|
| Build/run/manage SDK/emulator, adb, screenshot UI, setup awal | `android-cli` |
| Build gagal, error gradle/AGP, diagnose build system | `android-cli` + baca `app/build.gradle` |
| Analisis R8/proguard, shrinker, optimizer config | `r8-analyzer` |
| Perf: jank, memory leak, startup lambat, heap dump, trace, SQL ad-hoc | `android-profiler` |
| UI gak full-screen, status bar/navigation bar overlap | `edge-to-edge` |
| Layout beda device/foldable/tablet, window size class | `adaptive` |
| Setup/strategi testing (unit, UI, screenshot, E2E) | `testing-setup` |
| Navigasi antar screen/handler back button | `navigation-3` atau `navigation-event` |
| Play Billing / in-app purchase | `play-billing-library-version-upgrade` |
| Play Store policy/compliance | `play-policy-insights` |

**SKILL YANG DILARANG PAKAI DI PROJECT INI (jangan invoke):**

- `migrate-xml-views-to-jetpack-compose` — project ini XML-based, MIGRASI DILARANG.
- `agp-9-upgrade` — project pake AGP 8.13.0, UPGRADE DILARANG.
- `wear-compose-m3`, `leanback-to-compose-tv-migration` — bukan Wear/TV app.

Alasan: perubahan arsitektur/dependency hanya boleh atas permintaan eksplisit dari Hery.

## Notes

- Project path: `/home/hery/giggin-gear`
- Language: Java 72 files + XML 41 layouts (bukan Compose)
- Build system: Gradle, AGP 8.13.0, Kotlin 1.9.22
- SDK: compileSdk/targetSdk 31, minSdk 23
- Skill update: `android skills add --all` (idempotent, update semua)
