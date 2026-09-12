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

## Notes

- Project path: `/home/hery/giggin-gear`
- Language: Kotlin + Java (Android)
- Build system: Gradle
