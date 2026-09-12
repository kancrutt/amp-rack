#!/usr/bin/env python3
"""
Zero-Trust Parameter Exporter for Giggin' Gear
Extracts all default parameters, ranges (min/max), units, types, and scale points
for every LV2 and LADSPA effect plugin in the app assets.
Outputs JSON, CSV, and Markdown into WIP/ for Bang Hery to review and sort.
"""

import os
import glob
import json
import csv
import re

LV2_DIR = "/home/hery/giggin-gear/app/src/main/assets/lv2"
RDF_DIR = "/home/hery/giggin-gear/app/src/main/assets/rdf"
ALL_PLUGINS_JSON = "/home/hery/giggin-gear/app/src/main/assets/all_plugins.json"
LV2_PLUGINS_JSON = "/home/hery/giggin-gear/app/src/main/assets/lv2_plugins.json"
OUT_DIR = "/home/hery/giggin-gear/WIP"

os.makedirs(OUT_DIR, exist_ok=True)

all_plugins = []

# -------------------------------------------------------------
# 1. Parse LV2 Plugins from individual asset JSON files
# -------------------------------------------------------------
lv2_files = sorted(glob.glob(os.path.join(LV2_DIR, "*", "*.json")))

for path in lv2_files:
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        meta = data.get("-1", {})
        plugin_name = meta.get(
            "http://usefulinc.com/ns/doap#name", meta.get("pluginName", "")
        )
        if not plugin_name:
            plugin_name = os.path.splitext(os.path.basename(path))[0]

        desc = meta.get(
            "http://usefulinc.com/ns/doap#description",
            meta.get("http://www.w3.org/2000/01/rdf-schema#comment", ""),
        )
        if isinstance(desc, str):
            desc = desc.strip().replace("\r", " ").replace("\n", " ")
        else:
            desc = ""

        rel_path = os.path.relpath(path, "/home/hery/giggin-gear")

        controls = []
        # Sort ports by numerical index
        port_keys = [k for k in data.keys() if k != "-1"]
        port_keys.sort(key=lambda x: int(x) if x.isdigit() else 9999)

        for k in port_keys:
            v = data[k]
            if not isinstance(v, dict):
                continue
            if v.get("ControlPort") and v.get("InputPort"):
                p_name = v.get("name", v.get("symbol", f"Port_{k}"))
                p_symbol = v.get("symbol", "")
                p_min = v.get("minimum", "")
                p_max = v.get("maximum", "")
                p_def = v.get("default", "")
                p_prop = v.get("portProperty", "")
                p_unit = v.get(
                    "http://lv2plug.in/ns/extensions/units#unit", v.get("unit", "")
                )

                # Format scale points (enums)
                scale_points = []
                if "scalePoint" in v:
                    sp = v["scalePoint"]
                    if isinstance(sp, list):
                        for s in sp:
                            val_str = str(s.get("value", "?"))
                            lbl_str = str(s.get("label", s.get("name", "")))
                            scale_points.append(f"{val_str}={lbl_str}")

                controls.append(
                    {
                        "port_index": k,
                        "parameter_name": p_name,
                        "symbol": p_symbol,
                        "default_value": p_def,
                        "min_value": p_min,
                        "max_value": p_max,
                        "unit": p_unit,
                        "property": p_prop,
                        "scale_points": "; ".join(scale_points),
                    }
                )

        if controls:
            all_plugins.append(
                {
                    "type": "LV2",
                    "plugin_name": plugin_name,
                    "file_source": rel_path,
                    "description": desc[:140],
                    "controls_count": len(controls),
                    "parameters": controls,
                }
            )
    except Exception as e:
        pass

# -------------------------------------------------------------
# 2. Parse LADSPA Plugins from RDF Definitions
# -------------------------------------------------------------
rdf_files = sorted(glob.glob(os.path.join(RDF_DIR, "*.rdf")))

for r_path in rdf_files:
    try:
        with open(r_path, "r", encoding="latin1") as f:
            content = f.read()

        rel_path = os.path.relpath(r_path, "/home/hery/giggin-gear")
        plugin_matches = re.findall(
            r"<ladspa:([A-Za-z0-9_]+)\s+rdf:about=\"&ladspa;(\d+)\">(.*?)</ladspa:\1>",
            content,
            re.DOTALL,
        )

        for p_class, p_id, p_body in plugin_matches:
            t_match = re.search(r"<dc:title>(.*?)</dc:title>", p_body)
            title = t_match.group(1).strip() if t_match else f"LADSPA_{p_id}"

            ports = {}
            for port_m in re.finditer(
                r"<ladspa:InputControlPort\s+rdf:about=\"&ladspa;\d+\.(\d+)\"\s+ladspa:hasLabel=\"([^\"]+)\"",
                p_body,
            ):
                p_idx = port_m.group(1)
                p_lbl = port_m.group(2)
                ports[p_idx] = {
                    "port_index": p_idx,
                    "parameter_name": p_lbl,
                    "symbol": p_lbl,
                    "default_value": "",
                    "min_value": "",
                    "max_value": "",
                    "unit": "",
                    "property": "",
                    "scale_points": "",
                }

            for def_m in re.finditer(
                r"<ladspa:PortValue\s+rdf:value=\"([^\"]+)\">\s*<ladspa:forPort\s+rdf:resource=\"&ladspa;\d+\.(\d+)\"",
                p_body,
            ):
                val = def_m.group(1)
                idx = def_m.group(2)
                if idx in ports:
                    ports[idx]["default_value"] = val

            if ports:
                all_plugins.append(
                    {
                        "type": "LADSPA",
                        "plugin_name": title,
                        "file_source": f"{rel_path} (ID: {p_id})",
                        "description": f"Class: {p_class.replace('Plugin', '')}",
                        "controls_count": len(ports),
                        "parameters": sorted(
                            list(ports.values()), key=lambda x: int(x["port_index"])
                        ),
                    }
                )
    except Exception as e:
        pass

# Sort all plugins by Type and Name
all_plugins.sort(key=lambda x: (x["type"], x["plugin_name"].lower()))

# -------------------------------------------------------------
# 3. Export to JSON
# -------------------------------------------------------------
json_out = os.path.join(OUT_DIR, "all_plugin_parameters.json")
with open(json_out, "w", encoding="utf-8") as f:
    json.dump(all_plugins, f, indent=2, ensure_ascii=False)
print(f"[OK] Exported JSON: {json_out} ({len(all_plugins)} plugins)")

# -------------------------------------------------------------
# 4. Export to CSV (Spreadsheet)
# -------------------------------------------------------------
csv_out = os.path.join(OUT_DIR, "all_plugin_parameters.csv")
with open(csv_out, "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(
        [
            "Plugin Type",
            "Plugin Name",
            "Source File",
            "Port Index",
            "Parameter Name",
            "Symbol",
            "Default Value",
            "Min Value",
            "Max Value",
            "Unit",
            "Property / Type",
            "Scale Points / Enums",
        ]
    )

    total_rows = 0
    for p in all_plugins:
        p_type = p["type"]
        p_name = p["plugin_name"]
        p_file = p["file_source"]
        for c in p["parameters"]:
            writer.writerow(
                [
                    p_type,
                    p_name,
                    p_file,
                    c["port_index"],
                    c["parameter_name"],
                    c["symbol"],
                    c["default_value"],
                    c["min_value"],
                    c["max_value"],
                    c["unit"],
                    c["property"],
                    c["scale_points"],
                ]
            )
            total_rows += 1

print(f"[OK] Exported CSV: {csv_out} ({total_rows} parameter rows)")

# -------------------------------------------------------------
# 5. Export Markdown Overview
# -------------------------------------------------------------
md_out = os.path.join(OUT_DIR, "all_plugin_parameters_summary.md")
with open(md_out, "w", encoding="utf-8") as f:
    f.write("# Master Parameter Catalog — Giggin' Gear\n\n")
    f.write(
        f"Total Plugins: **{len(all_plugins)}** | Total Parameters: **{total_rows}**\n\n"
    )
    f.write("> File CSV lengkap siap sortir: `WIP/all_plugin_parameters.csv`\n")
    f.write("> File JSON lengkap siap olah: `WIP/all_plugin_parameters.json`\n\n")

    current_type = None
    for p in all_plugins:
        if p["type"] != current_type:
            current_type = p["type"]
            f.write(f"\n## {current_type} Plugins\n\n")

        f.write(f"### {p['plugin_name']}\n")
        f.write(f"- **Source:** `{p['file_source']}`\n")
        if p.get("description"):
            f.write(f"- **Desc:** {p['description']}\n")
        f.write(
            "\n| Port | Parameter | Default | Min | Max | Unit | Type / Properties | Scale Points |\n"
        )
        f.write("| :---: | :--- | :---: | :---: | :---: | :---: | :--- | :--- |\n")
        for c in p["parameters"]:
            f.write(
                f"| {c['port_index']} | **{c['parameter_name']}** | `{c['default_value']}` | {c['min_value']} | {c['max_value']} | {c['unit']} | {c['property']} | {c['scale_points']} |\n"
            )
        f.write("\n")

print(f"[OK] Exported Markdown Summary: {md_out}")
