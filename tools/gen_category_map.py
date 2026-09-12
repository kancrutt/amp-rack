#!/usr/bin/env python3
"""Zero-Trust category map generator — Giggin' Gear.

Reads AUTHORITATIVE plugin metadata (not hand-written guesses):
  1. app/src/main/assets/rdf/*.rdf — LADSPA ontology classes
     (ladspa:ModulatorPlugin, DistortionPlugin, ...). Ringmod with LFO (uid 1189)
     is declared <ladspa:ModulatorPlugin> by its own author.
  2. app/src/main/assets/all_plugins.json — LADSPA uid -> {name, desc, library}.
  3. app/src/main/assets/lv2_plugins.json — LV2 name -> {desc, library}
     (desc strings come from the bundles' own TTL manifests).

Output:
  app/src/main/java/com/kancrutt/gigginggear/CategoryGenerated.java
  (static maps: LADSPA_BY_NAME, LADSPA_BY_UID, LV2_BY_NAME)

Re-running must be idempotent (git diff empty) — that is the verification proof.
"""

import json, re, glob, os, collections

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(BASE, "app", "src", "main", "assets")
RDF_DIR = os.path.join(ASSETS, "rdf")
OUT = os.path.join(
    BASE,
    "app",
    "src",
    "main",
    "java",
    "com",
    "kancrutt",
    "gigginggear",
    "CategoryGenerated.java",
)

# ---- RDF class -> Category (LADSPA ontology, author-declared) ----
CLASS_CAT = {
    "AmplifierPlugin": None,  # per-title (Simple amplifier->UTILITY, *-monoamp->AMP)
    "AllpassPlugin": "DELAY",
    "CombPlugin": "DELAY",
    "DelayPlugin": "DELAY",
    "TimePlugin": "DELAY",
    "DistortionPlugin": "DISTORTION",
    "WaveshaperPlugin": "DISTORTION",
    "CompressorPlugin": "DYNAMICS",
    "DynamicsPlugin": "DYNAMICS",
    "LimiterPlugin": "DYNAMICS",
    "GatePlugin": "GATE",
    "ReverbPlugin": "REVERB",
    "EQPlugin": "EQ",
    "ParaEQPlugin": "EQ",
    "MultiEQPlugin": "EQ",
    "FilterPlugin": "EQ",
    "HighpassPlugin": "EQ",
    "LowpassPlugin": "EQ",
    "BandpassPlugin": "EQ",
    "NotchPlugin": "EQ",
    "ModulatorPlugin": "MODULATION",
    "ChorusPlugin": "MODULATION",
    "FlangerPlugin": "MODULATION",
    "PhaserPlugin": "MODULATION",
    "PitchPlugin": "PITCH",
    "FrequencyMeterPlugin": "PITCH",
    "OscillatorPlugin": "PITCH",
    "GeneratorPlugin": "PITCH",
    "FrequencyPlugin": None,  # per-title (guitarix-IR -> CABINET)
    "SpectralPlugin": None,  # per-title (convolver -> CABINET, shifter -> PITCH)
    "MixerPlugin": "UTILITY",
    "UtilityPlugin": "UTILITY",
    "SimulatorPlugin": None,  # per-title (mixed bag, see below)
}


def sim_title_cat(title):
    t = title.lower()
    if any(k in t for k in ("crybaby", "wah")):
        return "WAH"
    if "cabinet" in t or t.startswith("cab"):
        return "CABINET"
    if "rotary" in t or "doubler" in t or "stereo fx" in t:
        return "MODULATION"
    if "gong" in t:
        return "PITCH"
    if "tube" in t:  # Invada Tube, TAP TubeWarmth
        return "DISTORTION"
    if "amp" in t or "guitarix" in t:
        return "AMP"
    return "UTILITY"


def per_title_cat(cls, title):
    t = (title or "").lower()
    if cls == "AmplifierPlugin":
        return "AMP" if ("guitarix" in t or "guitar" in t) else "UTILITY"
    if cls == "FrequencyPlugin":
        return (
            "CABINET" if ("ir" in t.split() or "impulse" in t or "-ir" in t) else "EQ"
        )
    if cls == "SpectralPlugin":
        return "CABINET" if "convol" in t else "PITCH"
    if cls == "SimulatorPlugin":
        return sim_title_cat(title or "")
    return CLASS_CAT.get(cls, "UTILITY")


def parse_rdf():
    """uid(int) -> (class, title_or_None). Handles " and ' quotes, multiline tags."""
    out = {}
    for f in sorted(glob.glob(os.path.join(RDF_DIR, "*.rdf"))):
        txt = open(f, encoding="iso-8859-1", errors="replace").read()
        for m in re.finditer(
            r"<ladspa:(\w+Plugin)\s*\n?\s*rdf:about=['\"]([^'\"]+)['\"]", txt
        ):
            cls, about = m.group(1), m.group(2)
            uid = about.split("#")[-1].split(";")[-1]
            try:
                uid = int(re.sub(r"\D", "", uid))
            except ValueError:
                continue
            window = txt[m.end() : m.end() + 4000]
            tm = re.search(r"<dc:title>(.*?)</dc:title>", window, re.S)
            title = tm.group(1).strip() if tm else None
            if uid not in out:
                out[uid] = (cls, title)
    return out


def norm(n):
    if not n:
        return ""
    s = n.lower().replace(".lv2", "").replace(".so", "")
    if s.startswith("lib"):
        s = s[3:]
    s = re.sub(r"[_\s\-]+", "", s)
    if s.startswith("gx"):
        s = s[2:]
    s = re.sub(r"[_\s\-]+", "", s)
    return s.strip()


# ---- LV2 desc -> Category (bundle's own TTL description) ----
LV2_RULES = [
    (
        (
            "distortion",
            "overdrive",
            "overdirve",  # upstream typo in GxSD2Lead TTL ("overdirve pedal")
            "fuzz",
            "booster",
            "boost pedal",
            "boost plugin",
            "clean boost",
            "volume booster",
            "tube warmth",
            "waveshaper",
            "decimator",
            "bitcrush",
            "big muff",
        ),
        "DISTORTION",
    ),
    (
        (
            "compressor",
            "limiter",
            "dynamics",
            "deesser",
            "de-esser",
            "sustain",
            "transient",
            "loudness",
            "maximis",
        ),
        "DYNAMICS",
    ),
    (
        ("noise gate", "denois", "suppress", "gate", "expander"),
        "GATE",
    ),
    (
        (
            "amplifier",
            "amp simulator",
            "guitar amp",
            "preamp",
            "valve amp",
            "neural amp",
            "amp model",
        ),
        "AMP",
    ),
    # NOTE: REVERB is deliberately checked BEFORE CABINET. Convolution/IR
    # vocabulary ("convolution", "IR samples") belongs to reverb plugins too
    # (e.g. rkr Reverbtron "Convolution-based reverb effect. IR samples..."),
    # while cabinet descs essentially never say "reverb". Checking CABINET
    # first stole convolution reverbs into the cabinet tab.
    (("reverb",), "REVERB"),
    (
        (
            "cabinet",
            "impulse",
            "convolution",
            "speaker sim",
            "ir loader",
            "guitar cabinet",
        ),
        "CABINET",
    ),
    (("delay", "echo", "tape "), "DELAY"),
    (
        (
            "chorus",
            "flanger",
            "phaser",
            "tremolo",
            "vibrato",
            "ring mod",
            "ringmod",
            "panner",
            "autopan",
            "leslie",
            "rotary",
            "vibe",
            "detune",
            "doubler",
            "dimension",
        ),
        "MODULATION",
    ),
    (
        (
            "equali",
            "parametric",
            "graphic eq",
            "tone mender",
            "tilt",
            "filter",
            "crossover",
            "shelf",
        ),
        "EQ",
    ),
    (("wah", "envelope filter", "talkbox", "talk box"), "WAH"),
    (
        (
            "pitch",
            "octave",
            "shifter",
            "synth",
            "harmoniz",
            "vocoder",
            "organ",
            "epiano",
            "piano",
            "beatbox",
            "shepard",
        ),
        "PITCH",
    ),
    (
        (
            "volume",
            "level meter",
            "test tone",
            "tuner",
            "mute",
            "splitter",
            "mixer",
            "stereo",
            "mono",
            "dither",
            "looper",
            "slow gear",
        ),
        "UTILITY",
    ),
]
LV2_LIB_RULES = [
    ("poweramp", "AMP"),
    ("tamgamp", "AMP"),
    ("neural", "AMP"),
    ("aida", "AMP"),
    ("darc", "DYNAMICS"),
    ("bmp", "DISTORTION"),
    ("muff", "DISTORTION"),
    ("cab", "CABINET"),
    ("ir", "CABINET"),
    ("convol", "CABINET"),
    ("reverb", "REVERB"),
    ("delay", "DELAY"),
    ("echo", "DELAY"),
    ("chorus", "MODULATION"),
    ("flang", "MODULATION"),
    ("phas", "MODULATION"),
    ("tremolo", "MODULATION"),
    ("vibrato", "MODULATION"),
    ("ringmod", "MODULATION"),
    ("compress", "DYNAMICS"),
    ("limit", "DYNAMICS"),
    ("gate", "GATE"),
    ("eq", "EQ"),
    ("filter", "EQ"),
    ("wah", "WAH"),
    ("pitch", "PITCH"),
    ("octav", "PITCH"),
    ("synth", "PITCH"),
    ("distort", "DISTORTION"),
    ("fuzz", "DISTORTION"),
    ("overdrive", "DISTORTION"),
]

# Hardware-oriented corrections for known plugin identities. These are keyed
# by stable LADSPA UID or normalized LV2 name, never by a UI label substring.
LADSPA_CATEGORY_OVERRIDES = {
    1190: "EQ",
    1207: "UTILITY",
    1210: "UTILITY",
    1218: "MODULATION",
    1337: "PITCH",
    1407: "MODULATION",
    1411: "UTILITY",
    1895: "MODULATION",
    1896: "MODULATION",
    1897: "MODULATION",
    1905: "MODULATION",
    9005: "MODULATION",
    9010: "MODULATION",
}
LV2_CATEGORY_OVERRIDES = {
    "ts9sim": "DISTORTION",
    "mdatesttone": "UTILITY",
    "microamp": "UTILITY",
    "mdadither": "UTILITY",
    "mdaloudness": "UTILITY",
    "theinfamouspowercut": "MODULATION",
    "pushpull": "PITCH",
    "oc2": "PITCH",
    "collisiondrive": "DISTORTION",
    "axisface": "DISTORTION",
    "slowgear": "DYNAMICS",
    "vstb": "DISTORTION",
    "osmutantes": "DISTORTION",
    "tonemachine": "DISTORTION",
    "mdabeatbox": "UTILITY",
    "mdadeess": "DYNAMICS",
    "mdadegrade": "DISTORTION",
    "mdadx10": "PITCH",
    "mdaepiano": "PITCH",
    "mdajx10": "PITCH",
    "mdapiano": "PITCH",
    "theinfamouspowerup": "MODULATION",
    "theinfamousstuck": "PITCH",
    "hip2b": "DISTORTION",
    "theinfamouship2b": "DISTORTION",
    "lushlife": "MODULATION",
    "theinfamouslushlife": "MODULATION",
    "octolo": "PITCH",
    "theinfamousoctolo": "PITCH",
    "theinfamousewham": "PITCH",
    "theinfamousmindi": "UTILITY",
}


def token_cat(name, desc="", lib=""):
    """Shared token-keyword classifier (mirrored in CategoryHelper.java).
    Whole-token matching only — bare substrings like od/ir/pan/sub/rat
    are FORBIDDEN (they steal: mod->DIST via 'od', first->CAB via 'ir', ...)."""
    import re as _re

    toks = set(_re.split(r"[^a-z0-9]+", (name or "").lower())) | set(
        _re.split(r"[^a-z0-9]+", (desc or "").lower())
    )
    libt = (lib or "").lower()
    has = lambda *ws: any(w in toks for w in ws)
    # order matters: most specific families first
    if has(
        "cabinet",
        "cab",
        "impulse",
        "convolver",
        "convolution",
        "speaker",
        "celestion",
        "greenback",
        "creamback",
        "v30",
    ):
        return "CABINET"
    if has(
        "reverb",
        "gverb",
        "freeverb",
        "ambience",
        "cathedral",
        "chapel",
        "spring",
        "plate",
        "shimmer",
    ):
        return "REVERB"
    if has("delay", "echo", "slapback", "dub", "ping", "pong", "pingpong"):
        return "DELAY"
    if has(
        "chorus",
        "flanger",
        "flange",
        "phaser",
        "phase",
        "tremolo",
        "vibrato",
        "vibe",
        "rotary",
        "leslie",
        "detune",
        "dimension",
        "doubler",
        "ringmod",
        "ring",
        "autopan",
        "panner",
        "univibe",
        "modulation",
        "modulate",
        "modulator",
    ):
        return "MODULATION"
    if has(
        "distortion",
        "overdrive",
        "fuzz",
        "muff",
        "bmp",
        "screamer",
        "ts9",
        "ts808",
        "klon",
        "centaur",
        "crunch",
        "saturation",
        "saturator",
        "clipper",
        "decimator",
        "bitcrush",
        "foldover",
        "chebyshev",
        "diode",
        "booster",
        "waveshaper",
        "wavewrapper",
        "aliasing",
    ):
        return "DISTORTION"
    if has(
        "compressor",
        "compress",
        "dynamics",
        "limiter",
        "sustain",
        "sustainer",
        "deesser",
        "transient",
        "maximiser",
        "maximizer",
        "leveler",
        "peak",
    ):
        return "DYNAMICS"
    if has(
        "gate",
        "noisegate",
        "denoise",
        "denoiser",
        "silencer",
        "suppressor",
    ):
        return "GATE"
    if has(
        "parametric",
        "equaliser",
        "equalizer",
        "eq",
        "graphic",
        "multiband",
        "shelf",
        "shelving",
        "notch",
        "lowpass",
        "highpass",
        "bandpass",
        "crossover",
        "allpass",
        "comb",
        "filter",
        # NOTE: bare tonestack words (tone/bass/treble/mid/presence) deliberately
        # excluded — they misfire on pedal names (ToneMachine fuzz, Vox Bass Preamp).
        "dj",
        "hermes",
        "glame",
        "butterworth",
    ):
        return "EQ"
    if has(
        "pitch",
        "octave",
        "suboctave",
        "shifter",
        "synth",
        "harmonizer",
        "harmoniser",
        "whammy",
        "transpose",
        "autotalent",
        "vocoder",
        "oscillator",
        "generator",
        "organ",
        "poly",
        "gong",
        "beater",
        "harmonic",
        "fm",
        "terrain",
        "divider",
        "tracker",
    ):
        return "PITCH"
    if has("wah", "crybaby", "talkbox", "envelope"):
        return "WAH"
    if has(
        "amplifier",
        "amp",
        "preamp",
        "preamplifier",
        "nam",
        "aida",
        "marshall",
        "fender",
        "mesa",
        "boogie",
        "peavey",
        "engl",
        "diezel",
        "soldano",
        "bogner",
        "friedman",
        "orange",
        "vox",
        "plexi",
        "jcm",
        "rectifier",
        "twin",
        "bassman",
        "dumble",
        "ampeg",
        "svt",
        "5150",
        "ac30",
        "vh4",
        "uberschall",
        "terror",
        "tiny",
        "dark",
        "tamg",
        "poweramp",
        "combo",
        "tube",
        "valve",
        "warmth",
    ):
        return "AMP"
    if has(
        "volume",
        "vol",
        "gain",
        "level",
        "trim",
        "master",
        "mixer",
        "crossfade",
        "splitter",
        "utility",
        "inverter",
        "hilbert",
        "pinknoise",
        "click",
        "metronome",
        "mono",
        "stereo",
        "balance",
        "panorama",
        "constant",
        "decay",
        "sifter",
        "signal",
        "mute",
        "thru",
    ):
        return "UTILITY"
    if "poweramp" in libt or "tamgamp" in libt:
        return "AMP"
    if "cab" in libt or "convol" in libt:
        return "CABINET"
    return None


def lv2_cat(name, desc, lib):
    dl = (desc or "").lower()
    for keys, cat in LV2_RULES:
        if any(k in dl for k in keys):
            return cat, "desc"
    ll = (lib or "").lower()
    for key, cat in LV2_LIB_RULES:
        if key in ll:
            return cat, "lib"
    nl = (name or "").lower()
    for key, cat in LV2_LIB_RULES:
        if key in nl.replace(" ", ""):
            return cat, "name"
    return None, "none"


def main():
    rdf = parse_rdf()
    ladspa = json.load(open(os.path.join(ASSETS, "all_plugins.json")))
    lv2 = json.load(open(os.path.join(ASSETS, "lv2_plugins.json")))

    lad_by_name, lad_by_uid = {}, {}
    unmapped_lad = []
    for key, info in ladspa.items():
        # NOTE: dict key is a sequential index; the LADSPA UniqueID is info['id'].
        try:
            uid = int(info.get("id", key))
        except (ValueError, TypeError):
            continue
        name = info.get("name", "")
        cls_title = rdf.get(uid)
        if cls_title is None:
            unmapped_lad.append((uid, name))
            cat = token_cat(name, info.get("desc", ""), info.get("library", ""))
            if cat is None:
                cat = "UTILITY"
                src = "fallback(no-rdf)"
            else:
                src = "name-rule(no-rdf)"
        else:
            cls, title = cls_title
            base = CLASS_CAT.get(cls)
            cat = base if base else per_title_cat(cls, title or name)
            src = f"rdf:{cls}"
        if uid in LADSPA_CATEGORY_OVERRIDES:
            cat = LADSPA_CATEGORY_OVERRIDES[uid]
            src = "hardware-override"
        # Only emit evidence-backed entries into the NAME map: pure fallbacks
        # are omitted so curated layers (legacy Gx-proper-noun overrides,
        # token classifier) can still resolve them instead of being
        # shadowed by a blind UTILITY. UID map stays complete as audit trail.
        if src != "fallback(no-rdf)":
            lad_by_name[norm(name)] = cat
        lad_by_uid[uid] = (cat, src, name)

    lv2_by_name = {}
    lv2_src = {}
    for key, info in lv2.items():
        name = info.get("name", key)
        cat, src = lv2_cat(name, info.get("desc", ""), info.get("library", ""))
        if cat is None:
            cat = token_cat(name, info.get("desc", ""), info.get("library", ""))
            src = "name-rule" if cat else "fallback"
            if cat is None:
                cat = "UTILITY"
        if norm(name) in LV2_CATEGORY_OVERRIDES:
            cat = LV2_CATEGORY_OVERRIDES[norm(name)]
            src = "hardware-override"
        k = norm(name)
        # first writer wins; record collisions for the report.
        # Pure fallbacks are NOT emitted (see LADSPA note above).
        if k not in lv2_by_name and src != "fallback":
            lv2_by_name[k] = cat
            lv2_src[k] = (src, name)

    # ---- pinned exceptions: bundle desc is demonstrably misleading ----
    # GxAmplifier TTL says "head, tonestack and cabinet" (mentions cabinet, so the
    # desc rule fires CABINET) — but the plugin is a full amp RIG. Its own library
    # is libgx_amp and its Guitarix role is the amp head. -> AMP.
    #
    # Audit 2026-09 (total scan, 16 fallbacks + 8 misfires fixed):
    #  - rkropticaltrem: "tremolo ... built into a guitar amplifier" — the generic
    #    "guitar amp" phrase beats the specific "tremolo". It varies volume via
    #    LFO = tremolo. -> MODULATION
    #  - rkrreverbtron: "Convolution-based reverb effect. IR samples..." — "convolution"
    #    fired CABINET (now also fixed by REVERB-before-CABINET reorder). -> REVERB
    #  - rkrsequence: "8-step modulation sequencer ... pitch shifting" -> PITCH
    #    (mirrors legacy short-key override "sequence"->PITCH).
    #  - rkrflangerchorus: "A flanger or chorus (depending on delay time)" — "delay"
    #    fired DELAY, but flanger/chorus live in MODULATION. -> MODULATION
    #  - mdaleslie: "Rotary speaker simulator. No overdrive ..." — the NEGATED
    #    "overdrive" fired DISTORTION. -> MODULATION
    #  - mdathruzero: "tape-flanging ... phasing and simple chorusing" — stray "delay
    #    effects" fired DELAY. Thru-zero flanging = modulation. -> MODULATION
    #  - mdabandisto: "3 bands of clipping instead of compression" -> DISTORTION
    #  - mdasplitter: "Frequency/level crossover ..." (mentions reverb/gate/filter
    #    as USE CASES) — it is a routing utility. -> SPLIT
    #  - mdasubsynth: sub-bass synthesizer ("More bass than you could ever need").
    #    -> PITCH (synth family)
    PINNED = {
        "amplifier": ("AMP", "pinned:GxAmplifier full rig (head+stack+cab)"),
        "rkropticaltrem": (
            "MODULATION",
            "pinned:tremolo-via-LFO, 'guitar amp' is context only",
        ),
        "rkrreverbtron": (
            "REVERB",
            "pinned:convolution REVERB, IR mention is not a cabinet",
        ),
        "rkrsequence": ("PITCH", "pinned:step sequencer w/ pitch shifting"),
        # NOTE: norm() keeps '/' ("rkr Flanger/Chorus" -> "rkrflanger/chorus").
        "rkrflanger/chorus": (
            "MODULATION",
            "pinned:flanger/chorus, 'delay time' is a parameter",
        ),
        "mdaleslie": ("MODULATION", "pinned:rotary sim, 'No overdrive' is a negation"),
        "mdathruzero": ("MODULATION", "pinned:tape-flanging/phasing/chorusing"),
        "mdabandisto": ("DISTORTION", "pinned:3 bands of clipping"),
        "mdasplitter": ("SPLIT", "pinned:freq/level crossover utility"),
        "mdasubsynth": ("PITCH", "pinned:sub-bass synthesizer"),
    }
    for k, (cat, src) in PINNED.items():
        lv2_by_name[k] = cat
        lv2_src[k] = (src, "(pinned)")

    # ---- conflict report vs legacy hand-written OVERRIDES (informational) ----
    print(f"RDF plugins parsed : {len(rdf)}")
    print(f"LADSPA mapped      : {len(lad_by_name)}  (no-rdf: {len(unmapped_lad)})")
    for uid, name in unmapped_lad[:10]:
        print(f"   no-rdf uid={uid} name={name!r}")
    dist = collections.Counter(lad_by_uid[u][0] for u in lad_by_uid)
    print("LADSPA by category :", dict(dist))
    print(f"LV2 mapped         : {len(lv2_by_name)}")
    print("LV2 by category    :", dict(collections.Counter(lv2_by_name.values())))

    # key spot-checks demanded by the ultimatum
    for probe in [
        "Ringmod with LFO",
        "x42 DARC",
        "Gx_bmp_",
        "PowerAmps",
        "PowerAmpImpulses",
        "Tamg Amp",
        "MDA RingMod",
    ]:
        k = norm(probe)
        print(
            f"probe {probe!r:22} norm={k!r:20} lad={lad_by_name.get(k)} lv2={lv2_by_name.get(k)}"
        )

    # ---- emit Java ----
    def jmap(varname, d):
        return "\n".join(
            f'        {varname}.put("{k}", "{v}");' for k, v in sorted(d.items())
        )

    java = f"""package com.kancrutt.gigginggear;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AUTO-GENERATED by tools/gen_category_map.py — DO NOT EDIT BY HAND.
 * Authoritative effect categories derived from the plugins' OWN metadata:
 *  - LADSPA: ontology class from app/src/main/assets/rdf/*.rdf
 *    (e.g. uid 1189 "Ringmod with LFO" is declared ladspa:ModulatorPlugin
 *    by Steve Harris himself — it is MODULATION, never DISTORTION).
 *  - LV2: bundle TTL descriptions (app/src/main/assets/lv2_plugins.json),
 *    e.g. "x42 DARC" desc = "General purpose audio signal compressor" = DYNAMICS,
 *    "PowerAmps" desc = "Simulate valve amplifiers" = AMP.
 * Re-run the script to regenerate; a no-op re-run (empty git diff) is the proof.
 */
public final class CategoryGenerated {{
    private CategoryGenerated() {{}}

    /** Normalized LADSPA descriptor name -> Category enum name. */
    public static final Map<String, String> LADSPA_BY_NAME;
    /** LADSPA UniqueID -> Category enum name (for native/JNI use). */
    public static final Map<Integer, String> LADSPA_BY_UID;
    /** Normalized LV2 plugin name -> Category enum name. */
    public static final Map<String, String> LV2_BY_NAME;

    static {{
        HashMap<String, String> n = new HashMap<>();
{jmap("n", lad_by_name)}
        LADSPA_BY_NAME = Collections.unmodifiableMap(n);

        HashMap<Integer, String> u = new HashMap<>();
"""
    for uid in sorted(lad_by_uid):
        cat, src, name = lad_by_uid[uid]
        java += f'        u.put({uid}, "{cat}"); // {src} :: {name}\n'
    java += f"""        LADSPA_BY_UID = Collections.unmodifiableMap(u);

        HashMap<String, String> v = new HashMap<>();
{jmap("v", lv2_by_name)}
        LV2_BY_NAME = Collections.unmodifiableMap(v);
    }}
}}
"""
    open(OUT, "w").write(java)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
