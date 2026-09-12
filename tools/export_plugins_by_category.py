#!/usr/bin/env python3
"""Export the app's complete plugin inventory grouped by its generated category map."""

import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
GENERATED = ROOT / "app/src/main/java/com/kancrutt/gigginggear/CategoryGenerated.java"
OUT = ROOT / "WIP/plugins_by_category.csv"


def norm(name):
    s = (name or "").lower().replace(".lv2", "").replace(".so", "")
    if s.startswith("lib"):
        s = s[3:]
    s = re.sub(r"[_\s-]+", "", s)
    if s.startswith("gx"):
        s = s[2:]
    return re.sub(r"[_\s-]+", "", s).strip()


def load_generated_maps():
    text = GENERATED.read_text()
    ladspa = {
        int(uid): category
        for uid, category in re.findall(r'u\.put\((\d+), "([A-Z]+)"\)', text)
    }
    lv2 = {
        key: category
        for key, category in re.findall(r'v\.put\("([^"]+)", "([A-Z]+)"\)', text)
    }
    return ladspa, lv2


def main():
    ladspa_map, lv2_map = load_generated_maps()
    ladspa = json.loads((ASSETS / "all_plugins.json").read_text())
    lv2 = json.loads((ASSETS / "lv2_plugins.json").read_text())
    rows = []

    for key, item in ladspa.items():
        uid = int(item.get("id", key))
        rows.append(
            {
                "plugin_type": "LADSPA",
                "plugin_id": uid,
                "plugin_index": item.get("plugin", ""),
                "plugin_name": item.get("name", ""),
                "library": item.get("library", ""),
                "category": ladspa_map.get(uid, "UNKNOWN"),
                "description": item.get("desc", ""),
            }
        )

    for key, item in lv2.items():
        name = item.get("name", "")
        rows.append(
            {
                "plugin_type": "LV2",
                "plugin_id": item.get("id", ""),
                "plugin_index": item.get("index", ""),
                "plugin_name": name,
                "library": item.get("library", ""),
                "category": lv2_map.get(norm(name), "UNKNOWN"),
                "description": item.get("desc", ""),
            }
        )

    rows.sort(key=lambda r: (r["category"], r["plugin_type"], r["plugin_name"].lower()))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    print(f"Wrote {len(rows)} plugins to {OUT}")
    for category in sorted({r["category"] for r in rows}):
        print(category, sum(r["category"] == category for r in rows))


if __name__ == "__main__":
    main()
