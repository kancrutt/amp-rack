#!/usr/bin/env python3
"""Build assets/curated_plugins.json - the guitar curation list.

Inventory (source of truth):
  app/src/main/assets/all_plugins.json   (LADSPA)
  app/src/main/assets/lv2_plugins.json   (LV2)

Policy (reviewed 2026-10-04, Phase 1 audit + user review):
  Only guitar-relevant processors are exposed in the browse list.
  Everything else is blacklisted: lab/DSP building blocks, MIDI-only
  synths, mastering tools, and the single-NAM-loader / single-IR-loader
  policy items. The EXCLUDED list below is the reviewed blacklist;
  curated = inventory - EXCLUDED.

Usage:
  build_catalog.py           # validate + write asset
  build_catalog.py --check   # report only, no write
"""

import argparse
import json
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ALL = ROOT / "app" / "src" / "main" / "assets" / "all_plugins.json"
LV2 = ROOT / "app" / "src" / "main" / "assets" / "lv2_plugins.json"
OUT = ROOT / "app" / "src" / "main" / "assets" / "curated_plugins.json"

EXCLUDED = [
    "4 x 4 pole allpass",
    "AIDA-X",
    "Aliasing",
    "Allpass delay line, cubic spline interpolation",
    "Allpass delay line, linear interpolation",
    "Allpass delay line, noninterpolating",
    "Artificial latency",
    "Autotalent",
    "Barry's Satan Maximiser",
    "Bode frequency shifter",
    "Bode frequency shifter (CV)",
    "Comb Filter",
    "Comb Splitter",
    "Comb delay line, cubic spline interpolation",
    "Comb delay line, linear interpolation",
    "Comb delay line, noninterpolating",
    "Crossfade",
    "Crossfade (4 outs)",
    "DC Offset Remover",
    "Decimator",
    "Declipper",
    "Exponential signal decay",
    "Fractionally Addressed Delay Line",
    "Frequency tracker",
    "Gong beater",
    "Gong model",
    "Hilbert transformer",
    "Impulse Loader",
    "Inverter",
    "L/C/R Delay",
    "LS Filter",
    "MDA BeatBox",
    "MDA De-ess",
    "MDA Degrade",
    "MDA Dither",
    "MDA Image",
    "MDA Loudness",
    "MDA MultiBand",
    "MDA RePsycho!",
    "MDA RingMod",
    "MDA RoundPan",
    "MDA Splitter",
    "MDA Stereo",
    "MDA TalkBox",
    "MDA TestTone",
    "MDA Tracker",
    "MDA Transient",
    "Matrix Spatialiser",
    "Matrix: MS to Stereo",
    "Matrix: Stereo to MS",
    "Modulatable delay",
    "Mono to Stereo splitter",
    "Neural Amp Modeler",
    "Pointer cast distortion",
    "RT 2nd-Order Allpass",
    "RT 2nd-order highpass",
    "RT 2nd-order lowpass",
    "RT First Order Allpass",
    "RT First Order Highpass",
    "RT First Order Lowpass",
    "RT High Shelf",
    "RT LR4 highpass",
    "RT LR4 lowpass",
    "RT Low Shelf",
    "RT mls-based stereo decorrelation",
    "Ratatouille",
    "Rate shifter",
    "Ringmod with LFO",
    "Ringmod with two inputs",
    "SWH Impulse convolver",
    "Signal sifter",
    "Simple delay line, cubic spline interpolation",
    "Simple delay line, linear interpolation",
    "Simple delay line, noninterpolating",
    "Sinus wavewrapper",
    "Smooth Decimator",
    "State Variable Filter",
    "Step Demuxer",
    "Surround matrix encoder",
    "TAP DeEsser",
    "Transient mangler",
    "VLevel (Mono)",
    "VLevel (Stereo)",
    "Valve rectifier",
    "Vocoder",
    "VyNil (Vinyl Effect)",
    "Wave shaper",
    "rkr Reverbtron",
    "rkr Sequence",
    "rkr Vocoder",
    "x42 DARC",
    "z-1",
]

MUST_INCLUDE = [
    "the infamous octolo",
    "octolo",
    "the infamous hip2b",
    "hip2b",
    "the infamous ewham",
    "the infamous lush life",
    "lushlife",
    "the infamous mindi",
    "the infamous envelope follower",
    "the infamous power cut",
    "the infamous power up",
    "the infamous stuck",
    "fatfrog",
    "littlefly_",
    "xdarkterror",
    "x_tinyterror_",
    "bluesbreaker",
    "collisiondrive",
    "harmonic exciter",
    "metaltone",
    "rumor",
    "vintageac30",
    "tamg amp",
    "mda bandisto",
    "mda ambience",
    "mda thruzero",
    "simple ir loader",
    "neural amp modeler ng",
    "c* compress - compressor and saturating limiter",
    "tap reverberator",
    "rt parametric eq",
]

MUST_EXCLUDE = [
    "aida-x",
    "neural amp modeler",
    "impulse loader",
    "swh impulse convolver",
    "rkr reverbtron",
    "x42 darc",
    "mda testtone",
    "mda talkbox",
    "mda beatbox",
    "tap deesser",
    "ratatouille",
    "vocoder",
    "z-1",
]


def inventory_names():
    names = set()
    for path in (ALL, LV2):
        with open(path) as f:
            data = json.load(f)
        for v in data.values():
            if isinstance(v, dict) and v.get("name"):
                names.add(str(v["name"]).strip())
    return names


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--check", action="store_true", help="report only, no write")
    args = ap.parse_args()

    inventory = inventory_names()
    inv_l = {n.lower() for n in inventory}
    excl_l = {e.lower() for e in EXCLUDED}

    unknown = sorted(excl_l - inv_l)
    if unknown:
        print("WARNING: excluded names not found in inventory: %s" % unknown)

    curated = sorted(n for n in inv_l if n not in excl_l)

    missing = [m for m in MUST_INCLUDE if m.lower() not in set(curated)]
    leaked = [m for m in MUST_EXCLUDE if m.lower() in set(curated)]
    if missing or leaked:
        print("CURATION SANITY FAILED: missing=%s leaked=%s" % (missing, leaked))
        return 1

    print("Inventory      : %d (LADSPA+LV2)" % len(inv_l))
    print("Excluded       : %d" % len(excl_l & inv_l))
    print("Curated        : %d" % len(curated))
    for m in sorted(missing):
        print("  MISSING:", m)

    if args.check:
        print("CHECK only, not written.")
        return 0

    out = {
        "version": 1,
        "generated": str(date.today()),
        "source": "all_plugins.json + lv2_plugins.json minus EXCLUDED (build_catalog.py)",
        "names": curated,
    }
    with open(OUT, "w") as f:
        json.dump(out, f, indent=2)
        f.write("\n")
    print("Wrote: %s" % OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
