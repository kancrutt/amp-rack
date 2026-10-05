#!/usr/bin/env python3
"""Manual LADSPA metadata patches for curated plugins the host probe cannot reach.

The LADSPA host probe (tools/ladspa_host_probe) cannot dlopen Android ELF
binaries, so a few curated plugins have no probe row. Curated ones get a
reviewed manual entry here; non-curated ones stay out of the metadata.

Currently only 2606 C* CabinetIV. Sources:
  - descriptor port names : strings libCabIV.so ("model", "gain (dB)")
  - ranges / defaults     : LADSPA descriptor hints (mirrored by the MLT
                            FilterLadspa-2606 page: model 0..24 def 12,
                            gain -24..24 dB def 0)
  - enum labels           : upstream caps.rdf
                            (http://quitte.de/dsp/caps.html#CabinetIV)

Applies the entries on top of parameter_metadata.json["ladspa"], validates
the whole asset, backs it up and writes. Idempotent.

Usage:
  patch_manual_ladspa.py           # validate + backup + write
  patch_manual_ladspa.py --check   # report only, no write
"""

import argparse
import json
import shutil
import sys
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build_param_audit as bpa  # noqa: E402  (main() is guarded)

ROOT = HERE.parents[1]
ASSET = ROOT / "app" / "src" / "main" / "assets" / "parameter_metadata.json"
BACKUP_DIR = ROOT / "WIP" / "param_audit"

CABINETIV_MODEL_LABELS = [
    "mega wookie 800",
    "mega wookie 812",
    "mega wookie 828",
    "mega wookie 868",
    "mega wookie 908",
    "mega wookie 912",
    "mega wookie 936",
    "mega wookie 968",
    "mega wookie 992",
    "unmatched",
    "twin A",
    "twin B",
    "twin C",
    "blue A",
    "blue B",
    "tweedie A",
    "tweedie B",
    "mini wookie A",
    "mini wookie B",
    "rosie A",
    "rosie B",
    "indigo",
    "angel",
    "sixty-one",
    "sixty-two",
]

MANUAL_LADSPA = {
    "2606": {
        "name": "C* CabinetIV - Idealised loudspeaker cabinet",
        "library": "libCabIV.so",
        "ports": [
            {
                "port_index": 0,
                "name": "model",
                "symbol": "port_0",
                "default": 12.0,
                "default_source": "hint",
                "min": 0.0,
                "max": 24.0,
                "unit": "",
                "unit_source": "none",
                "plugin_type": "ENUM",
                "control_type": "ENUM",
                "logarithmic": False,
                "integer": True,
                "toggled": False,
                "sample_rate": False,
                "enum_values": [float(i) for i in range(len(CABINETIV_MODEL_LABELS))],
                "enum_labels": CABINETIV_MODEL_LABELS,
                "rdf_default": None,
                "hint_default": 12.0,
                "conflict_default": False,
            },
            {
                "port_index": 1,
                "name": "gain (dB)",
                "symbol": "port_1",
                "default": 0.0,
                "default_source": "hint",
                "min": -24.0,
                "max": 24.0,
                "unit": "dB",
                "unit_source": "declared",
                "plugin_type": "FLOAT",
                "control_type": "KNOB",
                "logarithmic": False,
                "integer": False,
                "toggled": False,
                "sample_rate": False,
                "enum_values": [],
                "enum_labels": [],
                "rdf_default": None,
                "hint_default": 0.0,
                "conflict_default": False,
            },
        ],
    },
}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--check", action="store_true", help="report only, no write")
    args = ap.parse_args()

    assert len(CABINETIV_MODEL_LABELS) == 25, "CabinetIV model labels must be 0..24"

    with open(ASSET) as f:
        existing = json.load(f)

    ladspa = dict(existing.get("ladspa", {}))
    for uid, entry in MANUAL_LADSPA.items():
        status = "overwrite" if uid in ladspa else "new"
        ladspa[uid] = entry
        print(
            f"  ladspa[{uid}] {status}: {entry['name']} ({len(entry['ports'])} ports)"
        )

    merged = {
        "version": existing.get("version", 1),
        "ladspa": ladspa,
        "lv2": existing.get("lv2", {}),
    }

    bpa.validate_metadata(merged)
    print("  validate_metadata: OK")

    if args.check:
        print("CHECK only, not written.")
        return 0

    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup = BACKUP_DIR / f"parameter_metadata.json.bak-{ts}"
    shutil.copy2(ASSET, backup)
    print(f"  backed up: {backup}")

    with open(ASSET, "w") as f:
        json.dump(merged, f, indent=2)
    print(f"  wrote: {ASSET}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
