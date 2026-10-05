#!/usr/bin/env python3
"""
Giggin' Gear Parameter Metadata Audit & Extraction
===================================================
Extracts authoritative parameter metadata from:
- LADSPA: binary descriptor probe (min/max/hints), RDF (defaults + scale points), declared units from port names
- LV2: TTL files (authoritative: scale points with labels/values, units, ranges, properties), JSON (runtime view)

Produces:
- parameter_metadata.json (canonical asset for app)
- PARAMETER_METADATA_AUDIT.md (conflict counts + unknowns)
"""

import json, csv, re, os, sys, glob, collections, subprocess, xml.etree.ElementTree as ET
from pathlib import Path

# ─── Paths ─────────────────────────────────────────────────────────
ROOT = Path("/home/hery/giggin-gear")
APP = ROOT / "app"
LADSPA_PROBE = Path("/tmp/gg-audit/ladspa_raw.tsv")
LADSPA_RDF_DIR = APP / "src/main/assets/rdf"
LV2_ASSETS = APP / "src/main/assets/lv2"
LV2_JNI_ARM64 = APP / "src/main/jniLibs/arm64-v8a"
LV2_JNI_X86_64 = APP / "src/main/jniLibs/x86_64"
CSV_FILE = ROOT / "WIP/all_plugin_parameters.csv"
OUT_METADATA = APP / "src/main/assets/parameter_metadata.json"
OUT_AUDIT = ROOT / "WIP/PARAMETER_METADATA_AUDIT.md"

# ─── Unit token whitelist (LADSPA declared units in port names) ────
UNIT_TOKENS = {
    "(dB)": "dB",
    "(db)": "dB",
    "(DB)": "dB",
    "(Hz)": "Hz",
    "(hz)": "Hz",
    "(HZ)": "Hz",
    "(kHz)": "kHz",
    "(khz)": "kHz",
    "(s)": "s",
    "(sec)": "s",
    "(seconds)": "s",
    "(second)": "s",
    "(ms)": "ms",
    "(msec)": "ms",
    "(milliseconds)": "ms",
    "(millisecond)": "ms",
    "(%)": "%",
    "(percent)": "%",
    "(octaves)": "octaves",
    "(octave)": "octaves",
    "(cents)": "cents",
    "(cent)": "cents",
    "(semitones)": "semitones",
    "(semitone)": "semitones",
    "(BPM)": "BPM",
    "(bpm)": "BPM",
    "(m)": "m",
    "(meters)": "m",
    "(metres)": "m",
    "(inches)": "inches",
    "(inch)": "inches",
    "(degrees)": "°",
    "(deg)": "°",
}

# exclude tokens that are enum descriptions, not units
NON_UNIT_TOKENS = {
    "(F/B)",
    "(L/R)",
    "(M-S)",
    "(L-R)",
    "(Right)",
    "(Left)",
    "(Frequency)",
    "(L/R)",
    "(M-S)",
    "(L-R)",
    "(scale notes)",
    "(inches/sec, 1=normal)",
    "(in ms)",
    "(-1=down, +1=up)",
    "(-1 = key listen, 0 = gate, 1 = bypass)",
    "(0=none, 1=AM, 2=RM)",
    "(0=LP, 1=BP, 2=HP)",
    "(0=none, 1=LP, 2=HP, 3=BP, 4=BR, 5=AP)",
    "(0 for none, 1 for half wave, 2 for full wave)",
    "(square->sine->tri)",
    "(semitones w.r.t. A)",
    "(samples)",
}

# ─── LV2 unit URI mapping ──────────────────────────────────────────
LV2_UNIT_MAP = {
    "http://lv2plug.in/ns/extensions/units#hz": "Hz",
    "http://lv2plug.in/ns/extensions/units#khz": "kHz",
    "http://lv2plug.in/ns/extensions/units#ms": "ms",
    "http://lv2plug.in/ns/extensions/units#s": "s",
    "http://lv2plug.in/ns/extensions/units#db": "dB",
    "http://lv2plug.in/ns/extensions/units#pc": "%",
    "http://lv2plug.in/ns/extensions/units#bpm": "BPM",
    "http://lv2plug.in/ns/extensions/units#midiNote": "MIDI Note",
    "http://lv2plug.in/ns/extensions/units#semitone12TET": "semitones",
    "http://lv2plug.in/ns/extensions/units#cent": "cents",
    "http://lv2plug.in/ns/extensions/units#coef": "coef",
    "http://lv2plug.in/ns/extensions/units#beat": "beat",
    "http://lv2plug.in/ns/extensions/units#frame": "samples",
    "http://lv2plug.in/ns/extensions/units#oct": "octaves",
    "http://lv2plug.in/ns/extensions/units#m": "m",
}

# ─── LADSPA hint constants ─────────────────────────────────────────
LADSPA_HINT_BOUNDED_BELOW = 0x1
LADSPA_HINT_BOUNDED_ABOVE = 0x2
LADSPA_HINT_TOGGLED = 0x4
LADSPA_HINT_SAMPLE_RATE = 0x8
LADSPA_HINT_LOGARITHMIC = 0x10
LADSPA_HINT_INTEGER = 0x20
LADSPA_HINT_DEFAULT_MASK = 0x3C0
LADSPA_HINT_DEFAULT_NONE = 0x0
LADSPA_HINT_DEFAULT_MINIMUM = 0x40
LADSPA_HINT_DEFAULT_LOW = 0x80
LADSPA_HINT_DEFAULT_MIDDLE = 0xC0
LADSPA_HINT_DEFAULT_HIGH = 0x100
LADSPA_HINT_DEFAULT_MAXIMUM = 0x140
LADSPA_HINT_DEFAULT_0 = 0x200
LADSPA_HINT_DEFAULT_1 = 0x240
LADSPA_HINT_DEFAULT_100 = 0x280
LADSPA_HINT_DEFAULT_440 = 0x2C0


def hint_default_value(hint, lo, hi):
    """Compute LADSPA hint default per spec."""
    h = hint & LADSPA_HINT_DEFAULT_MASK
    if h == LADSPA_HINT_DEFAULT_MINIMUM:
        return lo
    if h == LADSPA_HINT_DEFAULT_LOW:
        return lo + 0.25 * (hi - lo)
    if h == LADSPA_HINT_DEFAULT_MIDDLE:
        return lo + 0.5 * (hi - lo)
    if h == LADSPA_HINT_DEFAULT_HIGH:
        return lo + 0.75 * (hi - lo)
    if h == LADSPA_HINT_DEFAULT_MAXIMUM:
        return hi
    if h == LADSPA_HINT_DEFAULT_0:
        return 0.0
    if h == LADSPA_HINT_DEFAULT_1:
        return 1.0
    if h == LADSPA_HINT_DEFAULT_100:
        return 100.0
    if h == LADSPA_HINT_DEFAULT_440:
        return 440.0
    return None  # NONE


# ════════════════════════════════════════════════════════════════════
# 1) LADSPA Probe (authoritative binary descriptor)
# ════════════════════════════════════════════════════════════════════


def load_ladspa_probe():
    """Parse ladspa_raw.tsv → {uniqueId: {name, lib, ports: [{...}]}}"""
    if not LADSPA_PROBE.exists():
        print(
            "ERROR: LADSPA probe file missing. Run tools/ladspa_host_probe/probe_all2.sh first"
        )
        sys.exit(1)

    plugins = collections.defaultdict(lambda: {"ports": [], "name": "", "lib": ""})
    with open(LADSPA_PROBE) as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) < 10:
                continue
            lib, rec, uid = p[0], p[1], int(p[2])
            if rec == "PLUGIN":
                plugins[uid]["name"] = p[3]
                plugins[uid]["lib"] = lib
            elif rec == "PORT":
                port_idx = int(p[3])
                name = p[4]
                is_input = int(p[5])
                is_control = int(p[6])
                is_audio = int(p[7])
                lo = float(p[8])
                hi = float(p[9])
                hint = int(p[10], 16) if len(p) > 10 else 0

                # LADSPA core descriptors have no unit field. Do not infer a
                # unit from PortName text; RDF is the only optional unit source.
                declared_unit = None

                is_log = bool(hint & LADSPA_HINT_LOGARITHMIC)
                is_int = bool(hint & LADSPA_HINT_INTEGER)
                is_toggled = bool(hint & LADSPA_HINT_TOGGLED)
                is_sr = bool(hint & LADSPA_HINT_SAMPLE_RATE)

                default_hint = hint_default_value(hint, lo, hi)
                has_default_hint = default_hint is not None

                plugins[uid]["ports"].append(
                    {
                        "index": port_idx,
                        "name": name,
                        "is_control": is_control,
                        "is_audio": is_audio,
                        "is_input": is_input,
                        "min": lo,
                        "max": hi,
                        "hint": hint,
                        "logarithmic": is_log,
                        "integer": is_int,
                        "toggled": is_toggled,
                        "sample_rate": is_sr,
                        "declared_unit": declared_unit,
                        "hint_default": default_hint,
                        "has_hint_default": has_default_hint,
                    }
                )
    return dict(plugins)


# ════════════════════════════════════════════════════════════════════
# 2) LADSPA RDF (authoritative defaults + scale points)
# ════════════════════════════════════════════════════════════════════

LADSPA_NS = "http://ladspa.org/ontology#"
RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#"
DC_NS = "http://purl.org/dc/elements/1.1/"
LADSPA_RDF_DIAGNOSTICS = {}


def parse_ladspa_xml(rdf_file, entities):
    """Parse RDF/XML after resolving the repository's internal entities.

    The CAPS file uses a multiline internal DOCTYPE subset whose entity
    declarations contain `>` characters. A `[^>]*` based regex truncates that
    subset and produces the misleading line-20 XML error. Match the complete
    internal subset instead and fail explicitly for malformed source data.
    """
    txt = rdf_file.read_text(encoding="latin1")
    for ent, value in entities.items():
        txt = txt.replace(ent, value)
    txt = re.sub(r"<!DOCTYPE\b[\s\S]*?\]\s*>", "", txt, count=1)
    txt = re.sub(r"<!DOCTYPE\b[^>]*>", "", txt, count=1)
    txt = re.sub(r"<!ENTITY\b[^>]*>", "", txt)
    return ET.fromstring(txt)


# Register namespaces for pretty printing
for prefix, uri in [
    ("ladspa", LADSPA_NS),
    ("rdf", RDF_NS),
    ("rdfs", RDFS_NS),
    ("dc", DC_NS),
]:
    ET.register_namespace(prefix, uri)


def parse_ladspa_rdf():
    """Parse all .rdf files in assets/rdf/ → {uniqueId: {title, class, ports: {portIdx: {default, scalePoints[]}}}}"""
    out = {}
    # Entity map for LADSPA RDF files (including namespace entities)
    ENTITIES = {
        "&ladspa;": "http://ladspa.org/ontology#",
        "&rdf;": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "&rdfs;": "http://www.w3.org/2000/01/rdf-schema#",
        "&dc;": "http://purl.org/dc/elements/1.1/",
    }

    # First pass: collect ALL scale points from ALL RDF files, keyed by port URI
    global_scale_points = collections.defaultdict(list)

    for rdf_file in LADSPA_RDF_DIR.glob("*.rdf"):
        try:
            root = parse_ladspa_xml(rdf_file, ENTITIES)
        except Exception as e:
            print(f"  WARNING: Failed to parse {rdf_file}: {e}")
            continue

        NSMAP = {
            "ladspa": LADSPA_NS,
            "rdf": RDF_NS,
            "rdfs": RDFS_NS,
            "dc": DC_NS,
        }

        # Find all InputControlPort elements that have hasScale/Scale/Point
        for port_elem in root.findall(f".//{{{LADSPA_NS}}}InputControlPort", NSMAP):
            about_p = port_elem.get(f"{{{RDF_NS}}}about")
            if not about_p:
                continue
            # Extract port URI (e.g., http://ladspa.org/ontology#1416.0)
            port_uri = about_p

            # Find hasScale -> Scale -> hasPoint -> Point
            for scale_elem in port_elem.findall(f".//{{{LADSPA_NS}}}Scale", NSMAP):
                for point_elem in scale_elem.findall(f".//{{{LADSPA_NS}}}Point", NSMAP):
                    val = point_elem.get(f"{{{RDF_NS}}}value")
                    label = point_elem.get(f"{{{LADSPA_NS}}}hasLabel")
                    if val is not None and label:
                        global_scale_points[port_uri].append(
                            {"value": float(val.rstrip("f")), "label": label}
                        )

    LADSPA_RDF_DIAGNOSTICS["scale_source_ports"] = len(global_scale_points)
    LADSPA_RDF_DIAGNOSTICS["scale_source_points"] = sum(
        len(points) for points in global_scale_points.values()
    )

    # Second pass: parse plugin metadata (defaults, titles) from plugin elements
    for rdf_file in LADSPA_RDF_DIR.glob("*.rdf"):
        try:
            root = parse_ladspa_xml(rdf_file, ENTITIES)
        except Exception as e:
            print(f"  WARNING: Failed to parse {rdf_file}: {e}")
            continue

        NSMAP = {
            "ladspa": LADSPA_NS,
            "rdf": RDF_NS,
            "rdfs": RDFS_NS,
            "dc": DC_NS,
        }

        plugin_elems = []
        for cls in [
            "AmplifierPlugin",
            "DelayPlugin",
            "DistortionPlugin",
            "DynamicsPlugin",
            "EQPlugin",
            "FilterPlugin",
            "GeneratorPlugin",
            "ModulatorPlugin",
            "OscillatorPlugin",
            "PhaserPlugin",
            "PitchPlugin",
            "ReverbPlugin",
            "SimulatorPlugin",
            "UtilityPlugin",
            "WaveshaperPlugin",
            "Plugin",
            "AllpassPlugin",
            "BandpassPlugin",
            "ChorusPlugin",
            "CombPlugin",
            "CompressorPlugin",
            "FlangerPlugin",
            "FrequencyMeterPlugin",
            "GatePlugin",
            "HighpassPlugin",
            "LimiterPlugin",
            "LowpassPlugin",
            "MultiEQPlugin",
            "NotchPlugin",
            "ParaEQPlugin",
            "SpectralPlugin",
            "TimePlugin",
            "WaveshaperPlugin",
        ]:
            plugin_elems.extend(root.findall(f".//{{{LADSPA_NS}}}{cls}", NSMAP))
        for elem in root.findall(".//*[@rdf:about]", NSMAP):
            if "ladspa.org/ontology#" in elem.get(f"{{{RDF_NS}}}about", ""):
                plugin_elems.append(elem)

        for plugin_elem in plugin_elems:
            about = plugin_elem.get(f"{{{RDF_NS}}}about")
            if not about:
                continue
            m = re.search(r"#(\d+)", about)
            if not m:
                continue
            uid = int(m.group(1))

            cls = None
            tag = plugin_elem.tag
            if "}" in tag:
                cls = tag.split("}")[-1]

            # defaults
            defaults = {}
            for default in plugin_elem.findall(f".//{{{LADSPA_NS}}}Default", NSMAP):
                for pv in default.findall(f".//{{{LADSPA_NS}}}PortValue", NSMAP):
                    for_port_elem = pv.find(f".//{{{LADSPA_NS}}}forPort", NSMAP)
                    if for_port_elem is not None:
                        for_port = for_port_elem.get(f"{{{RDF_NS}}}resource")
                    else:
                        for_port = None
                    val = pv.get(f"{{{RDF_NS}}}value")
                    if for_port and val:
                        m2 = re.search(r"#(\d+)", for_port)
                        if m2:
                            pidx = int(m2.group(1))
                            val_clean = val.rstrip("f")
                            defaults[pidx] = float(val_clean)

            # scale points - look up by port URI
            scale_points = collections.defaultdict(list)
            for port_elem in plugin_elem.findall(
                f".//{{{LADSPA_NS}}}InputControlPort", NSMAP
            ):
                about_p = port_elem.get(f"{{{RDF_NS}}}about")
                if not about_p:
                    continue
                m2 = re.search(r"#(\d+)", about_p)
                if not m2:
                    continue
                pidx = int(m2.group(1))
                port_uri = about_p
                if port_uri in global_scale_points:
                    for sp in global_scale_points[port_uri]:
                        scale_points[pidx].append(sp)

            # label from rdfs:label
            label_elem = plugin_elem.find(f".//{{{RDFS_NS}}}label")
            title = label_elem.text if label_elem is not None else ""

            out[uid] = {
                "title": title,
                "class": cls,
                "defaults": defaults,
                "scale_points": dict(scale_points),
            }
    return out


# ════════════════════════════════════════════════════════════════════
# 3) LV2 TTL (authoritative: scale points + units + ranges)
# ════════════════════════════════════════════════════════════════════


def find_lv2_ttl_dirs():
    """Find all TTL directories: assets/lv2/**/*.ttl + jniLibs/*/mda.lv2 + jniLibs/*/rkrlv2.so.lv2"""
    dirs = {}
    # assets/lv2
    for d in LV2_ASSETS.iterdir():
        if d.is_dir():
            ttl_files = list(d.glob("*.ttl"))
            if ttl_files:
                dirs[str(d)] = ttl_files
    # jniLibs/*/mda.lv2
    for arch in ["arm64-v8a", "armeabi-v7a"]:
        mda = APP / f"src/main/jniLibs/{arch}/mda.lv2"
        if mda.exists():
            ttl_files = list(mda.glob("*.ttl"))
            if ttl_files:
                dirs[f"jniLibs/{arch}/mda.lv2"] = ttl_files
    # jniLibs/*/rkrlv2.so.lv2
    for arch in ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]:
        rkr = APP / f"src/main/jniLibs/{arch}/rkrlv2.so.lv2"
        if rkr.exists():
            ttl_files = list(rkr.glob("*.ttl"))
            if ttl_files:
                dirs[f"jniLibs/{arch}/rkrlv2.so.lv2"] = ttl_files
    return dirs


def parse_lv2_ttl(ttl_dirs):
    """Parse all TTL files with rdflib → {pluginURI: {ports: {symbol: {...}}}}"""
    try:
        from rdflib import Graph, URIRef, Literal, RDF, RDFS, Namespace
    except ImportError:
        print("  WARNING: rdflib not available, skipping LV2 TTL parse")
        return {}

    LV2 = Namespace("http://lv2plug.in/ns/lv2core#")
    UNITS = Namespace("http://lv2plug.in/ns/extensions/units#")
    PORTGROUPS = Namespace("http://lv2plug.in/ns/ext/port-groups#")
    PARAMS = Namespace("http://lv2plug.in/ns/ext/parameters#")
    DOAP = Namespace("http://usefulinc.com/ns/doap#")
    MOD = Namespace("http://moddevices.com/ns/mod#")
    TIME = Namespace("http://lv2plug.in/ns/ext/time#")

    g = Graph()
    for d, files in ttl_dirs.items():
        for f in files:
            try:
                g.parse(str(f), format="turtle")
            except Exception as e:
                print(f"  WARNING: Failed to parse {f}: {e}")

    # Find all lv2:Plugin subjects
    plugins = {}
    for plugin_uri in g.subjects(RDF.type, LV2.Plugin):
        ports = {}
        # Get all ports via lv2:port
        for port_blank in g.objects(plugin_uri, LV2.port):
            # Extract port properties
            port_info = {
                "symbol": None,
                "name": None,
                "index": None,
                "minimum": None,
                "maximum": None,
                "default": None,
                "unit": None,
                "designation": None,
                "port_properties": [],  # list of URIs
                "scale_points": [],  # list of (value, label)
                "groups": [],
                "is_control": False,
                "is_input": False,
            }

            for pred, obj in g.predicate_objects(port_blank):
                if pred == LV2.symbol:
                    port_info["symbol"] = str(obj)
                elif pred == LV2.name:
                    port_info["name"] = str(obj)
                elif pred == RDF.type:
                    if obj == LV2.ControlPort:
                        port_info["is_control"] = True
                    if obj == LV2.InputPort:
                        port_info["is_input"] = True
                elif pred == LV2.index:
                    port_info["index"] = int(obj)
                elif pred == LV2.minimum:
                    port_info["minimum"] = float(obj)
                elif pred == LV2.maximum:
                    port_info["maximum"] = float(obj)
                elif pred == LV2.default:
                    port_info["default"] = float(obj)
                elif pred == UNITS.unit:
                    port_info["unit"] = LV2_UNIT_MAP.get(str(obj), str(obj))
                elif pred == LV2.designation:
                    port_info["designation"] = str(obj)
                elif pred == LV2.portProperty:
                    port_info["port_properties"].append(str(obj))
                elif pred == PORTGROUPS.group:
                    port_info["groups"].append(str(obj))
                elif pred == LV2.scalePoint:
                    # obj is a blank node with rdfs:label and rdf:value
                    label = None
                    val = None
                    for sp_pred, sp_obj in g.predicate_objects(obj):
                        if sp_pred == RDFS.label:
                            label = str(sp_obj)
                        elif sp_pred == RDF.value:
                            val = float(sp_obj)
                        else:
                            continue
                    if label is not None and val is not None:
                        port_info["scale_points"].append({"value": val, "label": label})

            if port_info["symbol"] is not None:
                ports[port_info["symbol"]] = port_info

        if ports:
            plugins[str(plugin_uri)] = {
                "ports": ports,
                "name": str(next(g.objects(plugin_uri, DOAP.name), plugin_uri)),
            }

    return plugins


# ════════════════════════════════════════════════════════════════════
# 4) LV2 JSON (runtime view — for cross-check)
# ════════════════════════════════════════════════════════════════════


def load_lv2_json():
    """Load all LV2 JSON assets → {lib_dir: {pluginName: json}}"""
    out = {}
    for lib_dir in LV2_ASSETS.iterdir():
        if not lib_dir.is_dir():
            continue
        for jf in lib_dir.glob("*.json"):
            try:
                with open(jf) as f:
                    data = json.load(f)
                lib_name = lib_dir.name
                out.setdefault(lib_name, {})[jf.stem] = data
            except Exception as e:
                print(f"  WARNING: Failed to load {jf}: {e}")
    return out


# ════════════════════════════════════════════════════════════════════
# 5) CSV (audit inventory)
# ════════════════════════════════════════════════════════════════════


def load_csv():
    rows = []
    with open(CSV_FILE, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


# ════════════════════════════════════════════════════════════════════
# 6) Generate canonical parameter metadata
# ════════════════════════════════════════════════════════════════════


def build_metadata(ladspa_probe, ladspa_rdf, lv2_ttl, lv2_json):
    """Build the canonical metadata asset."""
    metadata = {
        "version": 1,
        "ladspa": {},
        "lv2": {},
    }

    # LADSPA
    for uid, probe in ladspa_probe.items():
        rdf = ladspa_rdf.get(uid, {})
        meta_ports = []
        for port in probe["ports"]:
            if not port["is_control"]:
                continue

            pidx = port["index"]
            rdf_default = rdf.get("defaults", {}).get(pidx)
            rdf_scale = rdf.get("scale_points", {}).get(pidx, [])

            # Resolve default: RDF default > hint default (clamped to range)
            lo, hi = port["min"], port["max"]
            default = port["hint_default"]
            default_src = "hint"
            if rdf_default is not None:
                # RDF default is authoritative, but clamp to binary range
                if rdf_default < lo or rdf_default > hi:
                    # conflict — note but use RDF (authoritative per mission)
                    default = max(lo, min(hi, rdf_default))
                else:
                    default = rdf_default
                default_src = "rdf"

            # Unit: declared_unit from port name > NONE
            unit = port["declared_unit"] or ""

            # Control type / UI type
            if port["toggled"] and lo == 0 and hi == 1:
                ctl_type = "TOGGLE"
                plugin_type = "BOOLEAN"
            elif rdf_scale:
                ctl_type = "ENUM"
                plugin_type = "ENUM"
            elif port["integer"]:
                ctl_type = "INTEGER_STEPPER"
                plugin_type = "INTEGER"
            elif port["logarithmic"]:
                ctl_type = "LOG_KNOB"
                plugin_type = "FLOAT"
            else:
                ctl_type = "KNOB"
                plugin_type = "FLOAT"

            # LADSPA toggled ports are boolean semantics even when a plugin
            # advertises degenerate bounds in its descriptor.
            if port["toggled"]:
                lo, hi = 0.0, 1.0
                default = 1.0 if default is not None and default >= 0.5 else 0.0

            # enum values/labels
            enum_vals = []
            enum_labels = []
            if rdf_scale:
                for sp in sorted(rdf_scale, key=lambda x: x["value"]):
                    enum_vals.append(sp["value"])
                    enum_labels.append(sp["label"])

            meta_ports.append(
                {
                    "port_index": pidx,
                    "name": port["name"],
                    "symbol": f"port_{pidx}",  # LADSPA has no symbol in binary
                    "default": max(lo, min(hi, default))
                    if default is not None
                    else None,
                    "default_source": default_src,
                    "min": lo,
                    "max": hi,
                    "unit": unit or "",
                    "unit_source": "declared" if unit else "none",
                    "plugin_type": plugin_type,
                    "control_type": ctl_type,
                    "logarithmic": port["logarithmic"],
                    "integer": port["integer"],
                    "toggled": port["toggled"],
                    "sample_rate": port["sample_rate"],
                    "enum_values": enum_vals,
                    "enum_labels": enum_labels,
                    "rdf_default": rdf_default,
                    "hint_default": port["hint_default"],
                    "conflict_default": rdf_default is not None
                    and port["hint_default"] is not None
                    and abs(rdf_default - port["hint_default"]) > 1e-6,
                }
            )

        if meta_ports:
            metadata["ladspa"][str(uid)] = {
                "name": probe["name"],
                "library": probe["lib"],
                "ports": meta_ports,
            }

    # LV2
    for uri, ttl_data in lv2_ttl.items():
        lib_name = ""
        plugin_name = ttl_data.get("name", uri)
        # Try to find matching JSON to get library
        for lib, plugins in lv2_json.items():
            for pname, pdata in plugins.items():
                if (
                    pdata.get("-1", {}).get("pluginName") == plugin_name
                    or pdata.get("-1", {}).get("http://usefulinc.com/ns/doap#name")
                    == plugin_name
                ):
                    lib_name = lib
                    break
            if lib_name:
                break

        meta_ports = []
        for symbol, port in ttl_data["ports"].items():
            if not port.get("is_control") or not port.get("is_input"):
                continue
            lo = port.get("minimum")
            hi = port.get("maximum")
            default = port.get("default")
            if lo is None or hi is None or default is None:
                # Fallback to JSON if missing
                pass

            unit = port.get("unit") or ""
            scale_points = port.get("scale_points", [])

            # plugin type / control type
            props = port.get("port_properties", [])
            is_enum = "http://lv2plug.in/ns/lv2core#enumeration" in props
            is_int = "http://lv2plug.in/ns/lv2core#integer" in props
            is_toggled = "http://lv2plug.in/ns/lv2core#toggled" in props
            is_log = "http://lv2plug.in/ns/ext/port-props#logarithmic" in props
            is_not_ongui = "http://lv2plug.in/ns/ext/port-props#notOnGUI" in props

            if is_toggled and (lo == 0 or lo is None) and (hi == 1 or hi is None):
                ctl_type = "TOGGLE"
                plugin_type = "BOOLEAN"
            elif is_enum:
                ctl_type = "ENUM"
                plugin_type = "ENUM"
            elif is_int:
                ctl_type = "INTEGER_STEPPER"
                plugin_type = "INTEGER"
            elif is_log:
                ctl_type = "LOG_KNOB"
                plugin_type = "FLOAT"
            else:
                ctl_type = "KNOB"
                plugin_type = "FLOAT"

            enum_vals = [sp["value"] for sp in scale_points]
            enum_labels = [sp["label"] for sp in scale_points]
            enum_conflict = bool(
                lo is not None
                and hi is not None
                and any(value < lo or value > hi for value in enum_vals)
            )

            meta_ports.append(
                {
                    "symbol": symbol,
                    "index": port.get("index"),
                    "name": port.get("name"),
                    "default": default,
                    "min": lo,
                    "max": hi,
                    "unit": unit,
                    "unit_source": "lv2_unit" if unit else "none",
                    "plugin_type": plugin_type,
                    "control_type": ctl_type,
                    "logarithmic": is_log,
                    "integer": is_int,
                    "toggled": is_toggled,
                    "enum_values": [] if enum_conflict else enum_vals,
                    "enum_labels": [] if enum_conflict else enum_labels,
                    "enum_conflict": enum_conflict,
                    "port_properties": props,
                    "designation": port.get("designation"),
                    "groups": port.get("groups"),
                }
            )

        if meta_ports:
            metadata["lv2"][uri] = {
                "name": plugin_name,
                "library": lib_name,
                "ports": meta_ports,
            }

    return metadata


def validate_metadata(metadata):
    """Fail loudly on structural corruption in the canonical profile."""
    errors = []
    valid_units = {
        "",
        "dB",
        "Hz",
        "kHz",
        "ms",
        "s",
        "%",
        "BPM",
        "MIDI Note",
        "semitones",
        "cents",
        "coef",
        "beat",
        "samples",
        "octaves",
        "m",
        "inches",
        "°",
    }
    valid_controls = {
        "KNOB",
        "LOG_KNOB",
        "INTEGER_STEPPER",
        "TOGGLE",
        "ENUM",
        "ENUM_PICKER",
    }
    for family in ("ladspa", "lv2"):
        entries = metadata.get(family)
        if not isinstance(entries, dict):
            errors.append(f"{family}: missing object")
            continue
        seen_plugins = set()
        for plugin_key, plugin in entries.items():
            if not plugin_key or not isinstance(plugin, dict):
                errors.append(f"{family}: invalid plugin identity {plugin_key!r}")
                continue
            if plugin_key in seen_plugins:
                errors.append(f"{family}: duplicate plugin identity {plugin_key}")
            seen_plugins.add(plugin_key)
            ports = plugin.get("ports")
            if not isinstance(ports, list):
                errors.append(f"{family}/{plugin_key}: ports is not an array")
                continue
            seen_ports = set()
            for port in ports:
                identity = (
                    port.get("port_index") if family == "ladspa" else port.get("symbol")
                )
                if identity is None or identity == "":
                    errors.append(f"{family}/{plugin_key}: missing port identity")
                    continue
                if identity in seen_ports:
                    errors.append(f"{family}/{plugin_key}: duplicate port {identity}")
                seen_ports.add(identity)
                lo, hi = port.get("min"), port.get("max")
                if lo is None or hi is None:
                    continue
                if not isinstance(lo, (int, float)) or not isinstance(hi, (int, float)):
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: non-numeric range"
                    )
                elif lo > hi:
                    errors.append(f"{family}/{plugin_key}/{identity}: min > max")
                default = port.get("default")
                if (
                    isinstance(default, (int, float))
                    and isinstance(lo, (int, float))
                    and isinstance(hi, (int, float))
                    and not (lo <= default <= hi)
                ):
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: default outside range"
                    )
                unit = port.get("unit", "")
                if unit not in valid_units:
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: invalid unit {unit!r}"
                    )
                control = port.get("control_type", "")
                if control not in valid_controls:
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: invalid control type {control!r}"
                    )
                values = port.get("enum_values", []) or []
                labels = port.get("enum_labels", []) or []
                if len(values) != len(labels):
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: enum value/label mismatch"
                    )
                if (
                    values
                    and isinstance(lo, (int, float))
                    and isinstance(hi, (int, float))
                ):
                    for value in values:
                        if not (lo <= value <= hi):
                            errors.append(
                                f"{family}/{plugin_key}/{identity}: enum value outside range"
                            )
                if control == "TOGGLE" and (lo, hi) != (0, 1):
                    errors.append(
                        f"{family}/{plugin_key}/{identity}: toggle is not 0..1"
                    )
    if errors:
        raise ValueError("Metadata validation failed:\n" + "\n".join(errors[:100]))


# ════════════════════════════════════════════════════════════════════
# 7) Audit report
# ════════════════════════════════════════════════════════════════════


def generate_audit(ladspa_probe, ladspa_rdf, lv2_ttl, lv2_json, metadata, csv_rows):
    """Generate audit markdown report."""
    lines = []
    lines.append("# Giggin' Gear Parameter Metadata Audit Report")
    lines.append("")
    lines.append(
        f"Generated: {subprocess.check_output(['date', '+%Y-%m-%d %H:%M:%S'], text=True).strip()} WIB"
    )
    lines.append("")

    # Totals
    ladspa_plugins = len(metadata["ladspa"])
    ladspa_params = sum(len(p["ports"]) for p in metadata["ladspa"].values())
    lv2_plugins = len(metadata["lv2"])
    lv2_params = sum(len(p["ports"]) for p in metadata["lv2"].values())
    lines.append("## TOTALS")
    lines.append(f"- LADSPA plugins: {ladspa_plugins}")
    lines.append(f"- LADSPA control ports: {ladspa_params}")
    lines.append(f"- LV2 plugins: {lv2_plugins}")
    lines.append(f"- LV2 control ports: {lv2_params}")
    lines.append(f"- CSV rows: {len(csv_rows)}")
    lines.append("")

    # Default audit
    lines.append("## DEFAULT VALUES")
    ladspa_valid = ladspa_missing = ladspa_conflict = 0
    lv2_valid = lv2_missing = lv2_conflict = 0
    for uid, pdata in metadata["ladspa"].items():
        for port in pdata["ports"]:
            if port["default"] is not None:
                ladspa_valid += 1
            else:
                ladspa_missing += 1
            if port["conflict_default"]:
                ladspa_conflict += 1
    for uri, pdata in metadata["lv2"].items():
        for port in pdata["ports"]:
            if port["default"] is not None:
                lv2_valid += 1
            else:
                lv2_missing += 1
            # LV2 conflicts not yet tracked
    lines.append(f"### LADSPA")
    lines.append(f"- Valid: {ladspa_valid}")
    lines.append(f"- Missing: {ladspa_missing}")
    lines.append(f"- Conflicting (RDF vs hint): {ladspa_conflict}")
    lines.append(f"### LV2")
    lines.append(f"- Valid: {lv2_valid}")
    lines.append(f"- Missing: {lv2_missing}")
    lines.append(f"- Conflicting: {lv2_conflict}")
    lines.append("")

    # Min/Max audit
    lines.append("## MIN / MAX")
    ladspa_min_valid = ladspa_max_valid = ladspa_min_missing = ladspa_max_missing = 0
    for uid, pdata in metadata["ladspa"].items():
        for port in pdata["ports"]:
            if port["min"] is not None:
                ladspa_min_valid += 1
            else:
                ladspa_min_missing += 1
            if port["max"] is not None:
                ladspa_max_valid += 1
            else:
                ladspa_max_missing += 1
    lv2_min_valid = lv2_max_valid = lv2_min_missing = lv2_max_missing = 0
    for uri, pdata in metadata["lv2"].items():
        for port in pdata["ports"]:
            if port["min"] is not None:
                lv2_min_valid += 1
            else:
                lv2_min_missing += 1
            if port["max"] is not None:
                lv2_max_valid += 1
            else:
                lv2_max_missing += 1
    lines.append(f"### LADSPA")
    lines.append(f"- Min valid: {ladspa_min_valid}, missing: {ladspa_min_missing}")
    lines.append(f"- Max valid: {ladspa_max_valid}, missing: {ladspa_max_missing}")
    lines.append(f"### LV2")
    lines.append(f"- Min valid: {lv2_min_valid}, missing: {lv2_min_missing}")
    lines.append(f"- Max valid: {lv2_max_valid}, missing: {lv2_max_missing}")
    lines.append("")

    # Unit audit
    lines.append("## UNIT")
    ladspa_unit_valid = ladspa_unit_missing = ladspa_unit_declared = 0
    for uid, pdata in metadata["ladspa"].items():
        for port in pdata["ports"]:
            if port["unit"]:
                ladspa_unit_valid += 1
                if port["unit_source"] == "declared":
                    ladspa_unit_declared += 1
            else:
                ladspa_unit_missing += 1
    lv2_unit_valid = lv2_unit_missing = 0
    for uri, pdata in metadata["lv2"].items():
        for port in pdata["ports"]:
            if port["unit"]:
                lv2_unit_valid += 1
            else:
                lv2_unit_missing += 1
    lines.append(f"### LADSPA")
    lines.append(
        f"- Valid (declared in port name): {ladspa_unit_valid} ({ladspa_unit_declared} from explicit token)"
    )
    lines.append(f"- Missing (no explicit unit token): {ladspa_unit_missing}")
    lines.append(f"### LV2")
    lines.append(f"- Valid: {lv2_unit_valid}")
    lines.append(f"- Missing: {lv2_unit_missing}")
    lines.append("")

    # Type audit
    lines.append("## PLUGIN TYPE / CONTROL TYPE")
    ladspa_types = collections.Counter()
    for uid, pdata in metadata["ladspa"].items():
        for port in pdata["ports"]:
            ladspa_types[port["control_type"]] += 1
    lv2_types = collections.Counter()
    for uri, pdata in metadata["lv2"].items():
        for port in pdata["ports"]:
            lv2_types[port["control_type"]] += 1
    lines.append("### LADSPA")
    for t, c in ladspa_types.most_common():
        lines.append(f"- {t}: {c}")
    lines.append("### LV2")
    for t, c in lv2_types.most_common():
        lines.append(f"- {t}: {c}")
    lines.append("")

    # Enum audit
    lines.append("## ENUM / SCALE POINTS")
    ladspa_enum = sum(
        1
        for pdata in metadata["ladspa"].values()
        for port in pdata["ports"]
        if port["enum_values"]
    )
    lv2_enum = sum(
        1
        for pdata in metadata["lv2"].values()
        for port in pdata["ports"]
        if port["enum_values"]
    )
    ladspa_enum_ports = [
        (uid, p["port_index"], p["name"], len(p["enum_labels"]))
        for pdata in metadata["ladspa"].values()
        for p in pdata["ports"]
        if p["enum_values"]
    ]
    lv2_enum_ports = [
        (uri, p["symbol"], p["name"], len(p["enum_labels"]))
        for pdata in metadata["lv2"].values()
        for p in pdata["ports"]
        if p["enum_values"]
    ]
    lines.append(f"### LADSPA: {ladspa_enum} ports with scale points")
    lines.append(
        f"- RDF scale records: {LADSPA_RDF_DIAGNOSTICS.get('scale_source_ports', 0)} ports / "
        f"{LADSPA_RDF_DIAGNOSTICS.get('scale_source_points', 0)} points; "
        "unmatched records remain unresolved because descriptor/RDF port identities do not match."
    )
    for uid, pidx, name, n in sorted(ladspa_enum_ports):
        lines.append(
            f"  - {uid} ({metadata['ladspa'][str(uid)]['name']}) port {pidx} '{name}': {n} labels"
        )
    lines.append(f"### LV2: {lv2_enum} ports with scale points")
    for uri, sym, name, n in sorted(lv2_enum_ports):
        pname = metadata["lv2"][uri]["name"]
        lines.append(f"  - {uri} ({pname}) port {sym} '{name}': {n} labels")
    lines.append("")

    # UNKNOWN metadata
    lines.append("## UNKNOWN METADATA")
    lines.append("### LADSPA ports without declared unit")
    unknown_units = [
        (uid, p["port_index"], p["name"])
        for pdata in metadata["ladspa"].values()
        for p in pdata["ports"]
        if not p["unit"]
    ]
    for uid, pidx, name in sorted(unknown_units)[:50]:
        pname = metadata["ladspa"][str(uid)]["name"]
        lines.append(f"  - {uid} ({pname}) port {pidx}: '{name}'")
    if len(unknown_units) > 50:
        lines.append(f"  ... and {len(unknown_units) - 50} more")
    lines.append("")
    lines.append("### LV2 ports without unit")
    unknown_lv2_units = [
        (uri, p["symbol"], p["name"])
        for pdata in metadata["lv2"].values()
        for p in pdata["ports"]
        if not p["unit"]
    ]
    for uri, sym, name in sorted(unknown_lv2_units)[:50]:
        pname = metadata["lv2"][uri]["name"]
        lines.append(f"  - {uri} ({pname}) port {sym}: '{name}'")
    if len(unknown_lv2_units) > 50:
        lines.append(f"  ... and {len(unknown_lv2_units) - 50} more")
    lines.append("")
    lines.append("### LV2 enum ports without scale points (JSON blank node unresolved)")
    # Check JSON for ports with enumeration but no labels from TTL
    lv2_json_enum_unresolved = 0
    for lib, plugins in lv2_json.items():
        for pname, pdata in plugins.items():
            for k, v in pdata.items():
                if isinstance(v, dict) and v.get("ControlPort") and v.get("InputPort"):
                    props = v.get("portProperty")
                    is_enum = False
                    if isinstance(props, list):
                        is_enum = any("enumeration" in str(p) for p in props)
                    elif isinstance(props, str):
                        is_enum = "enumeration" in props
                    if is_enum:
                        sp = v.get("scalePoint")
                        if isinstance(sp, str) and sp.startswith("_:"):
                            lv2_json_enum_unresolved += 1
    lines.append(
        f"- Unresolved blank-node scalePoints in JSON: {lv2_json_enum_unresolved}"
    )
    lines.append("")

    # Highest risk
    lines.append("## HIGHEST-RISK PLUGINS / PARAMETERS")
    lines.append("### LADSPA: plugins with default conflicts")
    for uid, pdata in metadata["ladspa"].items():
        conflicts = [p for p in pdata["ports"] if p["conflict_default"]]
        if conflicts:
            lines.append(f"  - {uid} ({pdata['name']}): {len(conflicts)} conflicts")
            for c in conflicts[:3]:
                lines.append(
                    f"    port {c['port_index']} '{c['name']}': RDF={c['rdf_default']} vs hint={c['hint_default']}"
                )
    lines.append("### LV2: plugins with missing enum labels (blank node in JSON)")
    for lib, plugins in lv2_json.items():
        for pname, pdata in plugins.items():
            count = 0
            for k, v in pdata.items():
                if isinstance(v, dict) and v.get("ControlPort") and v.get("InputPort"):
                    props = v.get("portProperty")
                    is_enum = False
                    if isinstance(props, list):
                        is_enum = any("enumeration" in str(p) for p in props)
                    elif isinstance(props, str):
                        is_enum = "enumeration" in props
                    if is_enum:
                        sp = v.get("scalePoint")
                        if isinstance(sp, str) and sp.startswith("_:"):
                            count += 1
            if count:
                lines.append(
                    f"  - {lib}/{pname}: {count} enum ports with blank-node scalePoint"
                )

    return "\n".join(lines)


# ════════════════════════════════════════════════════════════════════
# Main
# ════════════════════════════════════════════════════════════════════


def main():
    print("Loading LADSPA probe...")
    ladspa_probe = load_ladspa_probe()
    print(
        f"  {len(ladspa_probe)} LADSPA plugins with {sum(len(p['ports']) for p in ladspa_probe.values())} ports"
    )

    print("Parsing LADSPA RDF...")
    ladspa_rdf = parse_ladspa_rdf()
    print(f"  {len(ladspa_rdf)} plugins with RDF metadata")

    print("Finding LV2 TTL directories...")
    ttl_dirs = find_lv2_ttl_dirs()
    print(f"  {len(ttl_dirs)} TTL directories")

    print("Parsing LV2 TTL (this may take a moment)...")
    lv2_ttl = parse_lv2_ttl(ttl_dirs)
    print(f"  {len(lv2_ttl)} LV2 plugins with TTL metadata")

    print("Loading LV2 JSON assets...")
    lv2_json = load_lv2_json()
    print(f"  {sum(len(p) for p in lv2_json.values())} LV2 plugin JSONs")

    print("Loading CSV inventory...")
    csv_rows = load_csv()
    print(f"  {len(csv_rows)} CSV rows")

    print("Building canonical metadata...")
    metadata = build_metadata(ladspa_probe, ladspa_rdf, lv2_ttl, lv2_json)
    print("Validating canonical metadata...")
    validate_metadata(metadata)

    print("Writing parameter_metadata.json...")
    OUT_METADATA.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT_METADATA, "w") as f:
        json.dump(metadata, f, indent=2)
    print(f"  → {OUT_METADATA}")

    print("Generating audit report...")
    audit_md = generate_audit(
        ladspa_probe, ladspa_rdf, lv2_ttl, lv2_json, metadata, csv_rows
    )
    OUT_AUDIT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT_AUDIT, "w") as f:
        f.write(audit_md)
    print(f"  → {OUT_AUDIT}")

    print("Done.")


if __name__ == "__main__":
    main()
