#!/usr/bin/env python3
"""P0-3: fill parameter_metadata.json["lv2"] from bundled LV2 TTL ground truth.

Reads:
  app/src/main/assets/lv2/**/*.ttl + app/src/main/jniLibs/**/ *.ttl
  app/src/main/assets/lv2/**/*.json   (library-name resolution only)
  WIP/param_audit/audit-baseline.csv  (coverage report, optional)

Writes:
  app/src/main/assets/parameter_metadata.json  ("lv2" section only;
  "version" and "ladspa" are preserved as-is)

Consumer contract (must not break):
  C++ Plugin.cpp lv2Meta()          -> per-port keys: unit, default,
                                       enum_values, enum_labels (matched by
                                       symbol, keyed by LV2 descriptor URI)
  Java ParameterMetadata.fromJson   -> lv2[uri].ports[] (map key = symbol;
                                       MainActivity looks ports up by port
                                       NAME, see ParameterMetadata.getLv2Port)

Derivation mirrors native PluginControl:
  TOGGLE          lv2:toggled or designation contains "enabled"  (forced 0..1)
  ENUM            lv2:enumeration or scale points present
  INTEGER_STEPPER lv2:integer
  LOG_KNOB        port-props#logarithmic
  KNOB            otherwise

Ground rules:
  - blank-node scale points dropped by parser; blank labels filtered
  - None / empty values omitted (never nulls)
  - defaults clamped into [min,max] (native clamps at runtime too)
  - out-of-range enum points dropped (unreachable in the UI)
  - stale TTLs repaired: missing ports synthesized from runtime JSON,
    labels inherited from same-file siblings (same symbol + range)
  - curated libs without TTL get whole-plugin fallback from runtime JSON
    (URIs extracted from .so binary strings, see FALLBACK_URIS)
  - output must pass build_param_audit.validate_metadata before write
  - previous asset backed up to WIP/param_audit/ before overwrite

Usage:
  generate_lv2_metadata.py           # validate + backup + write
  generate_lv2_metadata.py --check   # report only, no write
"""

import argparse
import csv
import json
import shutil
import sys
import time
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build_param_audit as bpa  # noqa: E402  (main() is guarded, safe to import)

ROOT = HERE.parents[1]
ASSET = ROOT / "app" / "src" / "main" / "assets" / "parameter_metadata.json"
BACKUP_DIR = ROOT / "WIP" / "param_audit"
BASELINE_CSV = BACKUP_DIR / "audit-baseline.csv"

LV2 = "http://lv2plug.in/ns/lv2core#"
PPROPS = "http://lv2plug.in/ns/ext/port-props#"

# Curated libs that ship no TTL bundle. Plugin URIs were extracted from the
# .so binaries' string tables (LV2 URIs are embedded literals; no dlopen).
# zconvo: the app loads libzconvo.so index 3 with CfgMono.json, so the
# runtime descriptor is the CfgMono variant.
FALLBACK_URIS = {
    "libPowerAmps.so": ["urn:brummer:poweramps"],
    "libPowerAmpImpulses.so": ["urn:brummer:PowerAmpImpulses"],
    "libPreAmps.so": ["urn:brummer:PreAmps"],
    "libPreAmpImpulses.so": ["urn:brummer:PreAmpImpulses"],
    "libconvoLV2.so": ["http://gareus.org/oss/lv2/convoLV2#Mono"],
    "libfonin-distortion.lv2.so": ["http://shaji.in/plugins/fonin-distortion"],
    "libfonin-overdrive.lv2.so": ["http://shaji.in/plugins/fonin-overdrive"],
    "libfonin-sustain.lv2.so": ["http://shaji.in/plugins/fonin-sustain"],
    "libfonin-tubeamp.lv2.so": ["http://shaji.in/plugins/fonin-tubeamp"],
    "libgnuitar-distort.lv2.so": ["http://shaji.in/plugins/gnuitar-distort"],
    "libgnuitar-distort2.lv2.so": ["http://shaji.in/plugins/gnuitar-distort2"],
    "libgnuitar-distort2-np.lv2.so": ["http://shaji.in/plugins/gnuitar-distort2-np"],
    "libgnuitar-tubeamp.lv2.so": ["http://shaji.in/plugins/gnuitar-tubeamp-np"],
    "libgxmetal_amp.lv2.so": [
        "http://guitarix.sourceforge.net/plugins/gxmetal_amp#metal_amp"
    ],
    "libgxmetal_head.lv2.so": [
        "http://guitarix.sourceforge.net/plugins/gxmetal_head#metal_head"
    ],
    "libir_cab.so": ["http://brnv.lv2/plugins/ir-cab-sim#ir-cab-sim"],
    "libtamgamp.so": ["https://github.com/sadko4u/tamgamp.lv2/lv2/tamgamp_gx"],
    "nam.so": ["http://github.com/mikeoliphant/neural-amp-modeler-lv2"],
    "libzconvo.so": ["http://gareus.org/oss/lv2/zeroconvolv#CfgMono"],
}


# ─── port entry builder ─────────────────────────────────────────────
def build_port_entry(symbol, p, stats):
    props = set(p.get("port_properties") or [])
    desig = p.get("designation") or ""
    scale_points = p.get("scale_points") or []

    lo, hi = p.get("minimum"), p.get("maximum")
    default = p.get("default")
    toggled = (LV2 + "toggled") in props or "enabled" in desig
    enumeration = (LV2 + "enumeration") in props or bool(scale_points)
    integer = (LV2 + "integer") in props
    logarithmic = (PPROPS + "logarithmic") in props

    if lo is not None and hi is not None and lo > hi:
        stats["bad_range"] += 1
        lo = hi = None

    if toggled:
        # Native forces toggle semantics to 0..1; validate_metadata agrees.
        lo, hi = 0.0, 1.0

    if default is not None and lo is not None and hi is not None:
        if not (lo <= default <= hi):
            stats["clamped_defaults"] += 1
            default = max(lo, min(hi, default))
    elif default is not None and (lo is None or hi is None):
        default = None  # unanchored default, cannot validate it
        stats["dropped_default"] += 1

    unit = p.get("unit") or ""
    if "://" in unit:  # LV2_UNIT_MAP had no entry -> raw URI
        stats["dropped_unit_uri"] += 1
        unit = ""

    # control / plugin type (mirrors native PluginControl derivation order)
    if toggled:
        ctl_type, plugin_type = "TOGGLE", "BOOLEAN"
    elif enumeration:
        ctl_type, plugin_type = "ENUM", "ENUM"
    elif integer:
        ctl_type, plugin_type = "INTEGER_STEPPER", "INT"
    elif logarithmic:
        ctl_type, plugin_type = "LOG_KNOB", "FLOAT"
    else:
        ctl_type, plugin_type = "KNOB", "FLOAT"

    enum_vals, enum_labels = [], []
    if enumeration:
        for sp in scale_points:
            v, lbl = sp.get("value"), sp.get("label")
            if v is None or not isinstance(lbl, str) or not lbl.strip():
                stats["blank_labels"] += 1
                continue
            enum_vals.append(v)
            enum_labels.append(lbl)
        if enum_vals and lo is not None and hi is not None:
            # Out-of-range scale points are unreachable in the UI (the value
            # can never leave [min,max]) - drop those points, keep the rest.
            kept = [(v, lbl) for v, lbl in zip(enum_vals, enum_labels) if lo <= v <= hi]
            stats["enum_out_of_range"] += len(enum_vals) - len(kept)
            enum_vals = [v for v, _ in kept]
            enum_labels = [lbl for _, lbl in kept]
        if not enum_vals:
            stats["ports_no_labels"] += 1

    name = p.get("name") or symbol
    if not isinstance(name, str) or not name.strip():
        name = symbol

    entry = {"symbol": symbol, "name": name}
    idx = p.get("index")
    if idx is not None:
        entry["index"] = idx
    if lo is not None and hi is not None:
        entry["min"] = lo
        entry["max"] = hi
    if default is not None:
        entry["default"] = default
    if unit:
        entry["unit"] = unit
    entry["control_type"] = ctl_type
    entry["plugin_type"] = plugin_type
    if logarithmic:
        entry["logarithmic"] = True
    if integer:
        entry["integer"] = True
    if toggled:
        entry["toggled"] = True
    if enum_vals:
        entry["enum_values"] = enum_vals
        entry["enum_labels"] = enum_labels

    stats["ports"] += 1
    if unit:
        stats["with_unit"] += 1
    if default is not None:
        stats["with_default"] += 1
    if enum_vals:
        stats["with_enum"] += 1
    if toggled:
        stats["toggled"] += 1
    if integer:
        stats["integer"] += 1
    if logarithmic:
        stats["logarithmic"] += 1
    if ctl_type == "TOGGLE":
        stats["t_off_on"] += 1
    return entry


def build_lv2_section(ttl_plugins, lv2_json):
    stats = dict.fromkeys(
        [
            "plugins",
            "ports",
            "with_unit",
            "with_default",
            "with_enum",
            "toggled",
            "integer",
            "logarithmic",
            "clamped_defaults",
            "dropped_default",
            "dropped_unit_uri",
            "blank_labels",
            "enum_out_of_range",
            "ports_no_labels",
            "bad_range",
            "t_off_on",
            "inherited_scale_points",
        ],
        0,
    )

    # pluginName -> library dir (runtime JSON, for the cosmetic "library" field)
    name_to_lib = {}
    for lib, plugins in lv2_json.items():
        for fname, data in plugins.items():
            pname = data.get("-1", {}).get("pluginName")
            if isinstance(pname, str) and pname:
                name_to_lib.setdefault(pname, lib)

    lv2 = {}
    for uri in sorted(ttl_plugins):
        data = ttl_plugins[uri]
        plugin_name = data.get("name") or uri
        entries = []
        seen_names = {}
        for symbol in sorted(data["ports"]):
            p = data["ports"][symbol]
            if not (p.get("is_control") and p.get("is_input")):
                continue
            entry = build_port_entry(symbol, p, stats)
            nm = entry["name"]
            if nm in seen_names:
                stats.setdefault("dup_names", 0)
                stats["dup_names"] += 1
                print(f"  WARNING: duplicate name {nm!r} in {uri}")
            seen_names[nm] = symbol
            entries.append(entry)
        if not entries:
            continue
        stats["plugins"] += 1
        lv2[uri] = {
            "name": plugin_name,
            "library": name_to_lib.get(plugin_name, ""),
            "ports": entries,
        }
    return lv2, stats


def ttl_file_owners(ttl_dirs):
    """Map plugin URI -> set of TTL files declaring it (sibling grouping)."""
    try:
        from rdflib import Graph, RDF, Namespace
    except ImportError:
        return {}
    LV2NS = Namespace("http://lv2plug.in/ns/lv2core#")
    owners = {}
    for d, files in ttl_dirs.items():
        for f in files:
            g = Graph()
            try:
                g.parse(str(f), format="turtle")
            except Exception:
                continue
            for uri in g.subjects(RDF.type, LV2NS.Plugin):
                owners.setdefault(str(uri), set()).add(f)
    return owners


def inherit_sibling_scale_points(ttl, file_owners, stats):
    """Stale bundled TTLs can drop scalePoints a sibling port still declares.

    Inherit {value,label} scale points when all of these match:
      - sibling plugin declared in the same TTL file
      - identical symbol, minimum, maximum
    Only ports with the enumeration property and no scale points of their
    own are filled. Every inheritance is logged.
    """
    by_file = {}
    for uri, files in file_owners.items():
        for f in files:
            by_file.setdefault(f, []).append(uri)

    for uri, data in ttl.items():
        for symbol, p in data["ports"].items():
            props = set(p.get("port_properties") or [])
            if not (p.get("is_control") and p.get("is_input")):
                continue
            if not ((LV2 + "enumeration") in props or bool(p.get("scale_points"))):
                continue
            if p.get("scale_points"):
                continue
            lo, hi = p.get("minimum"), p.get("maximum")
            if lo is None or hi is None:
                continue
            donor = None
            for f in file_owners.get(uri, ()):
                for sib_uri in by_file.get(f, ()):
                    if sib_uri == uri:
                        continue
                    sib = ttl.get(sib_uri, {}).get("ports", {}).get(symbol)
                    if sib is None:
                        continue
                    if (sib.get("minimum"), sib.get("maximum")) != (lo, hi):
                        continue
                    if sib.get("scale_points"):
                        donor = sib
                        break
                if donor:
                    break
            if donor:
                p["scale_points"] = [dict(sp) for sp in donor["scale_points"]]
                stats["inherited_scale_points"] += 1
                print(
                    f"  inherited scale points {symbol}: {uri} <- {donor_uri_name(ttl, donor)}"
                )


def donor_uri_name(ttl, donor):
    for uri, data in ttl.items():
        for symbol, p in data["ports"].items():
            if p is donor:
                return f"{uri} ({data.get('name')})"
    return "unknown"


def _json_float(v):
    if v is None:
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def _port_symbol(jp):
    """Runtime JSON symbol, falling back to the port name.

    Some bundled JSONs omit symbols (gnuitar -np variants). Native reads
    symbol "" for those ports and never matches metadata by symbol, so the
    entry is inert there; the Java UI looks ports up by NAME, which still
    resolves. Name-derived symbols must be unique per plugin (enforced by
    the caller).
    """
    sym = jp.get("symbol")
    if isinstance(sym, str) and sym.strip():
        return sym
    name = jp.get("name")
    if isinstance(name, str) and name.strip():
        return name
    return None


def json_port_to_ttl_shape(jp):
    """Convert a runtime-JSON port entry into parse_lv2_ttl's shape.

    JSON values are strings-or-numbers; scalePoint may be a dangling
    blank-node string, a {label,value} object, or a list of objects.
    """
    props = jp.get("portProperty")
    if isinstance(props, str):
        props = [props]
    elif not isinstance(props, list):
        props = []
    unit_uri = jp.get("http://lv2plug.in/ns/extensions/units#unit")
    scale_points = []
    sp = jp.get("scalePoint")
    if isinstance(sp, dict):
        sp = [sp]
    if isinstance(sp, list):
        for e in sp:
            if isinstance(e, dict) and e.get("label") is not None:
                v = _json_float(e.get("value"))
                if v is not None:
                    scale_points.append({"value": v, "label": str(e["label"])})
    return {
        "symbol": _port_symbol(jp),
        "name": jp.get("name"),
        "index": jp.get("index"),
        "minimum": _json_float(jp.get("minimum")),
        "maximum": _json_float(jp.get("maximum")),
        "default": _json_float(jp.get("default")),
        "unit": bpa.LV2_UNIT_MAP.get(unit_uri, "") if isinstance(unit_uri, str) else "",
        "designation": jp.get("designation"),
        "port_properties": props,
        "scale_points": scale_points,
        "groups": [],
        "is_control": bool(jp.get("ControlPort")),
        "is_input": bool(jp.get("InputPort")),
    }


def synthesize_json_fallback_ports(ttl, lv2_json, stats):
    """Runtime JSONs can carry control ports the bundled TTL block lacks
    (stale TTL). Synthesize TTL-shaped entries so the metadata covers the
    full runtime contract. Labels arrive later via sibling inheritance.
    """
    name2uri = {}
    for uri, data in ttl.items():
        n = data.get("name")
        if n:
            name2uri.setdefault(n, uri)
    for lib, plugins in sorted(lv2_json.items()):
        for fname, data in sorted(plugins.items()):
            if "preset" in fname.lower() or not isinstance(data, dict):
                continue
            pname = data.get("-1", {}).get("pluginName")
            uri = name2uri.get(pname)
            if not uri:
                continue
            tports = ttl[uri]["ports"]
            for k, v in sorted(data.items()):
                if k == "-1" or not isinstance(v, dict):
                    continue
                if not (v.get("ControlPort") and v.get("InputPort")):
                    continue
                sym = _port_symbol(v)
                if not sym or sym in tports:
                    continue
                tports[sym] = json_port_to_ttl_shape(v)
                stats["json_fallback_ports"] += 1
                print(f"  json-fallback port {sym}: {uri} (from {lib}/{fname})")


def synthesize_json_fallback_plugins(ttl, lv2_json, stats):
    """Whole-plugin metadata for curated libs that ship no TTL bundle.

    The runtime JSON is the only ground truth for these libs; URIs come from
    FALLBACK_URIS (binary strings). One runtime JSON = one plugin per lib.
    """
    for lib, uris in FALLBACK_URIS.items():
        plugins = lv2_json.get(lib)
        if not plugins:
            print(f"  WARNING: fallback lib has no runtime JSON: {lib}")
            continue
        done = False
        for fname, data in sorted(plugins.items()):
            if "preset" in fname.lower() or not isinstance(data, dict):
                continue
            if done:
                continue  # one runtime JSON = one plugin per lib here
            pname = data.get("-1", {}).get("pluginName") or fname
            ports = {}
            for k, v in data.items():
                if k == "-1" or not isinstance(v, dict):
                    continue
                if not (v.get("ControlPort") and v.get("InputPort")):
                    continue
                sym = _port_symbol(v)
                if not sym:
                    continue
                if sym in ports:
                    sym = f"{sym}_{len(ports)}"
                ports[sym] = json_port_to_ttl_shape(v)
            if not ports:
                continue
            for uri in uris:
                if uri in ttl:
                    continue  # real TTL already covers it
                ttl[uri] = {"ports": dict(ports), "name": pname}
                stats["json_fallback_plugins"] += 1
                print(
                    f"  json-fallback plugin: {pname} -> {uri} ({len(ports)} ports, lib {lib})"
                )
            done = True


# ─── coverage report ────────────────────────────────────────────────
def coverage_report(lv2_section):
    lines = []
    ttl_dirs = bpa.find_lv2_ttl_dirs()
    assets_with_ttl = {
        d.name
        for d in (bpa.LV2_ASSETS).iterdir()
        if d.is_dir() and any(d.glob("*.ttl"))
    }
    jni_covered = set()
    if (bpa.LV2_JNI_ARM64 / "mda.lv2").is_dir():
        jni_covered.add("mda")
    if (bpa.LV2_JNI_ARM64 / "rkrlv2.so.lv2").is_dir():
        jni_covered.add("rkrlv2")

    libs_no_ttl = []
    curated_ports = 0
    if BASELINE_CSV.exists():
        with open(BASELINE_CSV, newline="") as f:
            rows = [
                r
                for r in csv.DictReader(f)
                if r["api"] == "lv2" and r["is_curated"] == "true"
            ]
        curated_ports = len(rows)
        libs = sorted({r["library"] for r in rows})
        for lib in libs:
            libdir = bpa.LV2_ASSETS / lib
            has_ttl = libdir.is_dir() and any(libdir.glob("*.ttl"))
            if not has_ttl and not any(
                lib == f"{m}.so" or lib.startswith(m) for m in jni_covered
            ):
                # jniLibs bundles: mda-*.so / rkrlv2.so map to bundle dirs
                if lib.startswith("mda-") and "mda" in jni_covered:
                    continue
                if lib == "rkrlv2.so" and "rkrlv2" in jni_covered:
                    continue
                libs_no_ttl.append(lib)
    lines.append(f"  TTL dirs scanned        : {len(ttl_dirs)}")
    lines.append(f"  plugins in lv2 section  : {stats_line(lv2_section)}")
    lines.append(f"  curated LV2 ports (CSV) : {curated_ports}")
    if libs_no_ttl:
        lines.append(f"  curated libs w/o TTL ({len(libs_no_ttl)}):")
        for lib in libs_no_ttl:
            lines.append(f"    - {lib}")
    return lines


def stats_line(lv2_section):
    n_ports = sum(len(p["ports"]) for p in lv2_section.values())
    return f"{len(lv2_section)} plugins / {n_ports} ports"


# ─── main ───────────────────────────────────────────────────────────
def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--check", action="store_true", help="report only, do not write")
    args = ap.parse_args()

    print("Parsing TTL ground truth...")
    t0 = time.time()
    ttl_dirs = bpa.find_lv2_ttl_dirs()
    ttl = bpa.parse_lv2_ttl(ttl_dirs)
    print(f"  {len(ttl)} plugins from {len(ttl_dirs)} dirs ({time.time() - t0:.1f}s)")

    print("Building lv2 section...")
    lv2_json = bpa.load_lv2_json()
    print("Repairing stale-TTL ports (JSON fallback + sibling scale points)...")
    file_owners = ttl_file_owners(ttl_dirs)
    pre_stats = {
        "json_fallback_ports": 0,
        "json_fallback_plugins": 0,
        "inherited_scale_points": 0,
    }
    synthesize_json_fallback_ports(ttl, lv2_json, pre_stats)
    synthesize_json_fallback_plugins(ttl, lv2_json, pre_stats)
    inherit_sibling_scale_points(ttl, file_owners, pre_stats)
    if pre_stats["json_fallback_ports"]:
        print(
            f"  {pre_stats['json_fallback_ports']} port(s) synthesized from runtime JSON"
        )
    if pre_stats["json_fallback_plugins"]:
        print(
            f"  {pre_stats['json_fallback_plugins']} plugin(s) synthesized from runtime JSON"
        )
    if pre_stats["inherited_scale_points"]:
        print(f"  {pre_stats['inherited_scale_points']} port(s) filled from siblings")
    lv2_section, stats = build_lv2_section(ttl, lv2_json)
    stats["json_fallback_ports"] = pre_stats["json_fallback_ports"]
    stats["json_fallback_plugins"] = pre_stats["json_fallback_plugins"]
    stats["inherited_scale_points"] = pre_stats["inherited_scale_points"]
    for k, v in stats.items():
        print(f"  {k:22s}: {v}")

    print("Merging with existing asset (ladspa/version preserved)...")
    with open(ASSET) as f:
        existing = json.load(f)
    merged = {
        "version": existing.get("version", 1),
        "ladspa": existing.get("ladspa", {}),
        "lv2": lv2_section,
    }

    print("Validating merged metadata...")
    bpa.validate_metadata(merged)
    print("  validate_metadata: OK")

    print("Coverage:")
    for line in coverage_report(lv2_section):
        print(line)

    old_ports = sum(len(p.get("ports", [])) for p in existing.get("lv2", {}).values())
    print(
        f"  lv2 ports: {old_ports} -> {sum(len(p['ports']) for p in lv2_section.values())}"
    )

    if args.check:
        print("CHECK only, not written.")
        return 0

    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = BACKUP_DIR / f"parameter_metadata.json.bak-{ts}"
    shutil.copy2(ASSET, backup)
    print(f"Backed up: {backup}")

    with open(ASSET, "w") as f:
        json.dump(merged, f, indent=2)
    print(f"Wrote: {ASSET}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
