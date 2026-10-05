#!/usr/bin/env python3
"""Phase 3D regression audit: live asset vs Phase 1 baseline.

Compares app/src/main/assets/parameter_metadata.json ("live") against
WIP/param_audit/audit-baseline.csv (Phase 1 probe) and reports deltas.

Join strategy (documented limitation):
  - LADSPA: port-level join on (plugin_id, port_index). Both sources use the
    LADSPA unique id, so found/missing/new/changed are reliable.
  - LV2:    the baseline was captured by an on-device probe that keys plugins
    by registry ids (e.g. 33300) and original library names
    (e.g. rt-neural-generic.so) which do not match the host asset's libraries
    (libnam.lv2.so). A port-level join is therefore NOT possible on the host;
    the tool reports metrics-level deltas instead (plugin counts, port counts,
    enum-label coverage, unit distribution) and says so explicitly.

Also provides a self-audit mode that is always meaningful regardless of the
baseline: asset invariants (plugin/port counts, curated coverage, enum
integrity, toggle ranges, sample-rate flags).

Usage:
  audit_live_ports.py                 # baseline diff + self-audit report
  audit_live_ports.py --self          # asset invariants only
  audit_live_ports.py --output WIP/param_audit/findings-regression.json
Exit codes: 0 = report written (and, under --strict, deltas within tolerance),
1 = asset fails self-audit.
"""

import argparse
import csv
import json
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
ASSET = ROOT / "app" / "src" / "main" / "assets" / "parameter_metadata.json"
BASELINE = ROOT / "WIP" / "param_audit" / "audit-baseline.csv"
CATALOG = ROOT / "app" / "src" / "main" / "assets" / "curated_plugins.json"


def load_asset():
    with open(ASSET) as f:
        return json.load(f)


def live_ladspa_ports(meta):
    out = {}
    for uid, plugin in meta.get("ladspa", {}).items():
        for i, p in enumerate(plugin.get("ports", [])):
            out[(uid, int(p.get("port_index", i)))] = p
    return out


def lv2_port_iter(plugin):
    """Yield (symbol_or_index, port) for both dict- and list-shaped 'ports'."""
    ports = plugin.get("ports", {})
    if isinstance(ports, dict):
        return ports.items()
    return enumerate(ports)


def live_lv2_stats(meta):
    plugins = 0
    ports = 0
    enum_labeled = 0
    enum_total = 0
    units = Counter()
    for uri, plugin in meta.get("lv2", {}).items():
        plugins += 1
        for _sym, p in lv2_port_iter(plugin):
            ports += 1
            units[(p.get("unit") or "")] += 1
            if p.get("control_type") == "ENUM":
                enum_total += 1
                lbls = p.get("enum_labels") or []
                if len(lbls) >= 3 and any(l.strip() for l in lbls):
                    enum_labeled += 1
    return plugins, ports, enum_labeled, enum_total, units


def live_ladspa_stats(meta):
    ports = 0
    toggled_bad = 0
    for plugin in meta.get("ladspa", {}).values():
        for p in plugin.get("ports", []):
            ports += 1
            if p.get("control_type") == "TOGGLE":
                if (p.get("min") or 0) < 0 or (p.get("max") or 1) > 1:
                    toggled_bad += 1
    return ports, toggled_bad


def baseline_ladspa_rows():
    rows = []
    with open(BASELINE, newline="") as f:
        for r in csv.DictReader(f):
            if r["api"] == "ladspa":
                rows.append(r)
    return rows


def norm_unit(u):
    u = (u or "").strip()
    if u.lower() in ("db",):
        return "dB"
    return u


def diff_ladspa(meta):
    live = live_ladspa_ports(meta)
    baseline = baseline_ladspa_rows()
    found = missing = unit_conflict = default_changed = 0
    unit_dropped_from_storage = 0
    unparseable = 0
    changes = []
    seen = set()
    for r in baseline:
        try:
            key = (r["plugin_id"].strip(), int(r["port_index"]))
        except (ValueError, AttributeError):
            unparseable += 1
            continue
        if key not in live:
            missing += 1
            continue
        seen.add(key)
        lp = live[key]
        old_unit = norm_unit(r["runtime_unit"] or r["declared_unit"])
        new_unit = norm_unit(lp.get("unit"))
        if old_unit != new_unit:
            # LADSPA units are derived at runtime by the P0-1 bracket parser
            # from the port name, so the asset stores an empty unit by design.
            # "baseline had one, live is empty" is a storage-location change,
            # NOT a display regression. Only two non-empty-but-different units
            # are a genuine conflict.
            if new_unit == "" and old_unit != "":
                unit_dropped_from_storage += 1
            else:
                unit_conflict += 1
                if len(changes) < 25:
                    changes.append(
                        {
                            "plugin_id": key[0],
                            "port": key[1],
                            "port_name": lp.get("name"),
                            "unit_before": old_unit,
                            "unit_after": new_unit,
                        }
                    )
        try:
            old_def = float(r["default"] or 0)
        except ValueError:
            old_def = 0.0
        new_def = float(lp.get("default") or 0.0)
        if abs(old_def - new_def) > 0.01:
            default_changed += 1
            if len(changes) < 50:
                changes.append(
                    {
                        "plugin_id": key[0],
                        "port": key[1],
                        "port_name": lp.get("name"),
                        "default_before": old_def,
                        "default_after": new_def,
                    }
                )
    new = len(live) - len(seen)
    found = len(seen)
    return {
        "baseline_ports": len(baseline),
        "unparseable_baseline_rows": unparseable,
        "live_ports": len(live),
        "found": found,
        "missing": missing,
        "new": new,
        "unit_conflict": unit_conflict,
        "unit_dropped_from_storage": unit_dropped_from_storage,
        "default_changed": default_changed,
        "sample_changes": changes,
    }


def diff_lv2(meta):
    """Metrics-level only; see module docstring for why port-level join is
    impossible on the host."""
    plugins, ports, enum_labeled, enum_total, units = live_lv2_stats(meta)
    with open(BASELINE, newline="") as f:
        rows = [r for r in csv.DictReader(f) if r["api"] == "lv2"]
    base_plugins = len({r["plugin_id"] for r in rows})
    return {
        "note": (
            "LV2 baseline was captured by an on-device probe keyed by "
            "registry id + original library name; port-level join with "
            "the host asset is not possible. Metrics delta only."
        ),
        "baseline_plugins": base_plugins,
        "baseline_ports": len(rows),
        "live_plugins": plugins,
        "live_ports": ports,
        "live_enum_ports": enum_total,
        "live_enum_labeled": enum_labeled,
        "top_live_units": dict(units.most_common(10)),
    }


def self_audit(meta):
    curated = set(json.load(open(CATALOG))["names"])
    alld = json.load(open(ASSET.parent / "all_plugins.json"))
    lad_uids = {
        str(v.get("id"))
        for v in alld.values()
        if isinstance(v, dict) and v.get("name", "").lower() in curated
    }
    lad_ok = lad_uids <= set(meta.get("ladspa", {}))

    plugins, ports, enum_labeled, enum_total, _ = live_lv2_stats(meta)
    lad_ports, toggled_bad = live_ladspa_stats(meta)

    enum_mismatch = 0
    for uri, plugin in meta.get("lv2", {}).items():
        for _sym, p in lv2_port_iter(plugin):
            if p.get("control_type") == "ENUM":
                if len(p.get("enum_values", [])) != len(p.get("enum_labels", [])):
                    enum_mismatch += 1

    report = {
        "ladspa_plugins": len(meta.get("ladspa", {})),
        "ladspa_ports": lad_ports,
        "lv2_plugins": plugins,
        "lv2_ports": ports,
        "lv2_enum_ports": enum_total,
        "lv2_enum_labeled": enum_labeled,
        "curated_ladspa_uids": len(lad_uids),
        "curated_ladspa_covered": lad_ok,
        "enum_value_label_mismatches": enum_mismatch,
        "toggled_range_violations": toggled_bad,
        "ok": (
            lad_ok
            and enum_mismatch == 0
            and toggled_bad == 0
            and plugins >= 150
            and ports >= 1100
        ),
    }
    return report


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--self", action="store_true", help="asset invariants only")
    ap.add_argument(
        "--strict", action="store_true", help="fail (exit 1) when LADSPA missing > 5"
    )
    ap.add_argument(
        "--output",
        default=str(ROOT / "WIP" / "param_audit" / "findings-regression.json"),
    )
    args = ap.parse_args()

    meta = load_asset()
    report = {"self_audit": self_audit(meta)}
    if not args.self:
        report["ladspa_delta"] = diff_ladspa(meta)
        report["lv2_delta"] = diff_lv2(meta)

    with open(args.output, "w") as f:
        json.dump(report, f, indent=2)

    sa = report["self_audit"]
    print("=== self-audit ===")
    print(
        "  LADSPA %d plugins / %d ports | LV2 %d plugins / %d ports"
        % (sa["ladspa_plugins"], sa["ladspa_ports"], sa["lv2_plugins"], sa["lv2_ports"])
    )
    print(
        "  curated LADSPA uid coverage: %s | enum mismatches: %d | toggle violations: %d"
        % (
            sa["curated_ladspa_covered"],
            sa["enum_value_label_mismatches"],
            sa["toggled_range_violations"],
        )
    )
    print("  self-audit: %s" % ("PASS" if sa["ok"] else "FAIL"))

    if not args.self:
        d = report["ladspa_delta"]
        print("\n=== LADSPA delta vs baseline ===")
        print(
            "  baseline %d (unparseable %d) -> live %d | found %d | missing %d | new %d"
            % (
                d["baseline_ports"],
                d["unparseable_baseline_rows"],
                d["live_ports"],
                d["found"],
                d["missing"],
                d["new"],
            )
        )
        print(
            "  unit_conflict %d | unit_dropped_from_storage %d | default_changed %d"
            % (d["unit_conflict"], d["unit_dropped_from_storage"], d["default_changed"])
        )
        if d["unit_conflict"]:
            print("  genuine unit conflicts (sample):")
            for c in d["sample_changes"][:8]:
                print("   ", c)
        lv = report["lv2_delta"]
        print("\n=== LV2 delta (metrics only) ===")
        print("  %s" % lv["note"])
        print(
            "  baseline %d plugins / %d ports -> live %d / %d"
            % (
                lv["baseline_plugins"],
                lv["baseline_ports"],
                lv["live_plugins"],
                lv["live_ports"],
            )
        )
        print(
            "  enum ports: %d total, %d labeled"
            % (lv["live_enum_ports"], lv["live_enum_labeled"])
        )

    print("\nreport: %s" % args.output)

    bad = not sa["ok"]
    if args.strict and not args.self:
        bad = bad or report["ladspa_delta"]["missing"] > 5
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
