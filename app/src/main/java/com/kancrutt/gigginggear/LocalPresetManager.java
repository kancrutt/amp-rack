package com.kancrutt.gigginggear;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local offline JSON preset manager for Giggin' Gear.
 * Replaces Firebase Firestore completely.
 * Stores presets in app internal files directory under 'presets/'.
 * Supports preset_schema_v1 and legacy AmpRack preset conversion.
 */
public class LocalPresetManager {
    private static final String TAG = "LocalPresetManager";
    private static final String PRESETS_DIR_NAME = "presets";
    private static final int SCHEMA_VERSION = 1;

    private final Context context;
    private final File presetsDir;

    public LocalPresetManager(Context context) {
        this.context = context.getApplicationContext();
        this.presetsDir = new File(this.context.getFilesDir(), PRESETS_DIR_NAME);
        if (!presetsDir.exists()) {
            boolean created = presetsDir.mkdirs();
            Log.i(TAG, "Created presets directory: " + created);
        }
    }

    public File getPresetsDir() {
        return presetsDir;
    }

    /**
     * Save preset using standard schema v1 JSON structure.
     */
    public boolean savePreset(Preset preset) {
        if (preset.id == null || preset.id.isEmpty()) {
            preset.id = UUID.randomUUID().toString();
        }
        long now = System.currentTimeMillis();
        if (preset.createdAt == 0) preset.createdAt = now;
        preset.modifiedAt = now;

        try {
            JSONObject json = presetToJson(preset);
            File file = new File(presetsDir, preset.id + ".json");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            Log.i(TAG, "Preset saved successfully: " + preset.name + " (" + file.getAbsolutePath() + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save preset: " + preset.name, e);
            return false;
        }
    }

    /**
     * Load all saved local presets.
     */
    public List<Preset> loadAllPresets() {
        List<Preset> list = new ArrayList<>();
        File[] files = presetsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return list;

        for (File f : files) {
            Preset p = loadPresetFromFile(f);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Load a single preset by ID.
     */
    public Preset loadPreset(String id) {
        File file = new File(presetsDir, id + ".json");
        if (!file.exists()) return null;
        return loadPresetFromFile(file);
    }

    /**
     * Delete preset by ID. Primary path is the <id>.json file itself; the fallback
     * matches the STORED "id" only — never the display name (names are not unique).
     * Every miss is logged loudly; silent false deletes are a UI-level lie.
     */
    public boolean deletePreset(String id) {
        if (id == null || id.isEmpty()) {
            Log.w(TAG, "deletePreset: refused null/empty id");
            return false;
        }
        File file = new File(presetsDir, id + ".json");
        if (file.exists()) {
            boolean ok = file.delete();
            if (!ok) Log.w(TAG, "deletePreset: file.delete() failed for " + file.getAbsolutePath());
            return ok;
        }
        // Fallback: filename stem drifted from the stored id (copied/renamed files).
        File[] files = presetsDir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    JSONObject o = new JSONObject(readFully(f));
                    if (id.equals(o.optString("id"))) {
                        boolean ok = f.delete();
                        if (!ok) Log.w(TAG, "deletePreset: file.delete() failed for " + f.getAbsolutePath());
                        return ok;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "deletePreset: skipping unreadable " + f.getName(), e);
                }
            }
        }
        Log.w(TAG, "deletePreset: no preset file with id=" + id);
        return false;
    }

    /** Fully-draining read: a single InputStream.read() is NOT guaranteed to fill. */
    private static String readFully(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] data = new byte[(int) f.length()];
            int off = 0;
            while (off < data.length) {
                int r = fis.read(data, off, data.length - off);
                if (r < 0) break;
                off += r;
            }
            return new String(data, 0, off, StandardCharsets.UTF_8);
        }
    }

    /**
     * Read JSON file and parse to Preset object.
     */
    private Preset loadPresetFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read <= 0) return null;
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonStr);
            Preset p = jsonToPreset(json);
            // Files predating stable ids (legacy saves) carry no usable "id":
            // derive it from the filename stem so delete-by-id always resolves
            // to this exact file instead of a fresh random UUID per load.
            if (!json.has("id") || json.optString("id").isEmpty()) {
                String n = file.getName();
                p.id = (n.toLowerCase().endsWith(".json") && n.length() > 5)
                        ? n.substring(0, n.length() - 5) : n;
            }
            return p;
        } catch (Exception e) {
            Log.e(TAG, "Error loading preset file: " + file.getName(), e);
            return null;
        }
    }

    /**
     * Convert Preset model to JSONObject (schema_version = 1).
     */
    public static JSONObject presetToJson(Preset p) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("schema_version", SCHEMA_VERSION);
        root.put("id", p.id != null ? p.id : UUID.randomUUID().toString());
        root.put("name", p.name != null ? p.name : "Untitled Preset");
        root.put("description", p.description != null ? p.description : "");
        root.put("author", p.author != null ? p.author : "GigginGear Stage");
        root.put("created_at", p.createdAt);
        root.put("modified_at", p.modifiedAt);
        root.put("favorite", p.favorite);
        root.put("master_input_volume", p.masterInputVolume);
        root.put("master_output_volume", p.masterOutputVolume);

        JSONArray tagsArr = new JSONArray();
        if (p.tags != null) {
            for (String tag : p.tags) tagsArr.put(tag);
        }
        root.put("tags", tagsArr);

        JSONArray pluginsArr = new JSONArray();
        if (p.plugins != null) {
            for (int i = 0; i < p.plugins.size(); i++) {
                PluginItem item = p.plugins.get(i);
                JSONObject pObj = new JSONObject();
                pObj.put("index", i);
                pObj.put("name", item.name);
                pObj.put("library", item.library);
                pObj.put("unique_id", item.uniqueId);
                pObj.put("enabled", item.enabled);

                JSONArray controlsArr = new JSONArray();
                if (item.controls != null) {
                    for (int c = 0; c < item.controls.size(); c++) {
                        ControlItem ci = item.controls.get(c);
                        JSONObject cObj = new JSONObject();
                        cObj.put("index", c);
                        cObj.put("name", ci.name);
                        cObj.put("value", ci.value);
                        controlsArr.put(cObj);
                    }
                }
                pObj.put("controls", controlsArr);

                JSONArray filePortsArr = new JSONArray();
                if (item.filePorts != null) {
                    for (FilePortItem fpi : item.filePorts) {
                        JSONObject fpObj = new JSONObject();
                        fpObj.put("port_index", fpi.portIndex);
                        fpObj.put("port_name", fpi.portName);
                        fpObj.put("file_name", fpi.fileName);
                        fpObj.put("relative_path", fpi.relativePath);
                        filePortsArr.put(fpObj);
                    }
                }
                pObj.put("file_ports", filePortsArr);
                // TONE3000 metadata (only if present)
                if (item.tone3000ToneId != null) pObj.put("tone3000ToneId", item.tone3000ToneId);
                if (item.tone3000ModelId != null) pObj.put("tone3000ModelId", item.tone3000ModelId);
                if (item.tone3000Creator != null) pObj.put("tone3000Creator", item.tone3000Creator);
                if (item.tone3000ToneTitle != null) pObj.put("tone3000ToneTitle", item.tone3000ToneTitle);
                if (item.tone3000Format != null) pObj.put("tone3000Format", item.tone3000Format);
                if (item.tone3000GearType != null) pObj.put("tone3000GearType", item.tone3000GearType);
                if (item.tone3000SourceUrl != null) pObj.put("tone3000SourceUrl", item.tone3000SourceUrl);
                pluginsArr.put(pObj);
            }
        }
        root.put("plugins", pluginsArr);

        // Persist routing graph edges and node positions (preserves custom parallel routing layout)
        if (p.graph != null && p.graph.length > 0) {
            JSONArray gArr = new JSONArray();
            for (int g : p.graph) gArr.put(g);
            root.put("graph", gArr);
        }
        if (p.nodePositions != null && !p.nodePositions.isEmpty()) {
            JSONObject npObj = new JSONObject();
            for (Map.Entry<String, float[]> e : p.nodePositions.entrySet()) {
                JSONArray xyArr = new JSONArray();
                xyArr.put((double) e.getValue()[0]);
                xyArr.put((double) e.getValue()[1]);
                npObj.put(e.getKey(), xyArr);
            }
            root.put("node_positions", npObj);
        }

        // Helix routing state — same keys/shape as presetToString() so the
        // loadPreset() apply-side reads both formats identically.
        if (p.routingMode >= 0) root.put("routingMode", p.routingMode);
        if (p.splitMode >= 0) root.put("splitMode", p.splitMode);
        if (p.routingGains != null && !p.routingGains.isEmpty()) {
            JSONObject rgObj = new JSONObject();
            for (Map.Entry<String, float[]> e : p.routingGains.entrySet()) {
                JSONObject g = new JSONObject();
                g.put("a", e.getValue()[0]);
                g.put("b", e.getValue()[1]);
                rgObj.put(e.getKey(), g);
            }
            root.put("routingGains", rgObj);
        }

        return root;
    }

    /**
     * Convert JSONObject to Preset model. Supports legacy format fallback.
     */
    public static Preset jsonToPreset(JSONObject json) throws JSONException {
        // Fallback for legacy format if "schema_version" is missing but "controls" object exists
        if (!json.has("schema_version") && json.has("controls")) {
            return convertLegacyJsonToPreset(json);
        }

        Preset p = new Preset();
        p.id = json.optString("id", UUID.randomUUID().toString());
        p.name = json.optString("name", "Untitled Preset");
        p.description = json.optString("description", "");
        p.author = json.optString("author", "GigginGear Stage");
        p.createdAt = json.optLong("created_at", System.currentTimeMillis());
        p.modifiedAt = json.optLong("modified_at", System.currentTimeMillis());
        p.favorite = json.optBoolean("favorite", false);
        p.masterInputVolume = (float) json.optDouble("master_input_volume", 1.0);
        p.masterOutputVolume = (float) json.optDouble("master_output_volume", 1.0);

        JSONArray tagsArr = json.optJSONArray("tags");
        if (tagsArr != null) {
            p.tags = new ArrayList<>();
            for (int i = 0; i < tagsArr.length(); i++) {
                p.tags.add(tagsArr.getString(i));
            }
        }

        JSONArray pluginsArr = json.optJSONArray("plugins");
        if (pluginsArr != null) {
            p.plugins = new ArrayList<>();
            for (int i = 0; i < pluginsArr.length(); i++) {
                JSONObject pObj = pluginsArr.getJSONObject(i);
                PluginItem item = new PluginItem();
                item.name = pObj.optString("name", "");
                item.library = pObj.optString("library", "");
                item.uniqueId = pObj.optInt("unique_id", 0);
                item.enabled = pObj.optBoolean("enabled", true);

                JSONArray controlsArr = pObj.optJSONArray("controls");
                if (controlsArr != null) {
                    item.controls = new ArrayList<>();
                    for (int c = 0; c < controlsArr.length(); c++) {
                        JSONObject cObj = controlsArr.getJSONObject(c);
                        ControlItem ci = new ControlItem();
                        ci.name = cObj.optString("name", "Control " + c);
                        ci.value = (float) cObj.optDouble("value", 0.0);
                        item.controls.add(ci);
                    }
                }

                JSONArray fpArr = pObj.optJSONArray("file_ports");
                if (fpArr != null) {
                    item.filePorts = new ArrayList<>();
                    for (int fp = 0; fp < fpArr.length(); fp++) {
                        JSONObject fpObj = fpArr.getJSONObject(fp);
                        FilePortItem fpi = new FilePortItem();
                        fpi.portIndex = fpObj.optInt("port_index", 0);
                        fpi.portName = fpObj.optString("port_name", "");
                        fpi.fileName = fpObj.optString("file_name", "");
                        fpi.relativePath = fpObj.optString("relative_path", "");
                        item.filePorts.add(fpi);
                    }
                }
                // TONE3000 metadata — defensive, nullable, never assume string
                item.tone3000ToneId = pObj.isNull("tone3000ToneId") ? null : pObj.optString("tone3000ToneId", null);
                if (item.tone3000ToneId != null && item.tone3000ToneId.isEmpty()) item.tone3000ToneId = null;
                item.tone3000ModelId = pObj.isNull("tone3000ModelId") ? null : pObj.optString("tone3000ModelId", null);
                if (item.tone3000ModelId != null && item.tone3000ModelId.isEmpty()) item.tone3000ModelId = null;
                item.tone3000Creator = pObj.isNull("tone3000Creator") ? null : pObj.optString("tone3000Creator", null);
                if (item.tone3000Creator != null && item.tone3000Creator.isEmpty()) item.tone3000Creator = null;
                item.tone3000ToneTitle = pObj.isNull("tone3000ToneTitle") ? null : pObj.optString("tone3000ToneTitle", null);
                if (item.tone3000ToneTitle != null && item.tone3000ToneTitle.isEmpty()) item.tone3000ToneTitle = null;
                item.tone3000Format = pObj.isNull("tone3000Format") ? null : pObj.optString("tone3000Format", null);
                if (item.tone3000Format != null && item.tone3000Format.isEmpty()) item.tone3000Format = null;
                item.tone3000GearType = pObj.isNull("tone3000GearType") ? null : pObj.optString("tone3000GearType", null);
                if (item.tone3000GearType != null && item.tone3000GearType.isEmpty()) item.tone3000GearType = null;
                item.tone3000SourceUrl = pObj.isNull("tone3000SourceUrl") ? null : pObj.optString("tone3000SourceUrl", null);
                if (item.tone3000SourceUrl != null && item.tone3000SourceUrl.isEmpty()) item.tone3000SourceUrl = null;
                p.plugins.add(item);
            }
        }

        // Restore graph edges if present
        JSONArray gArr = json.optJSONArray("graph");
        if (gArr != null && gArr.length() > 0) {
            int[] g = new int[gArr.length()];
            for (int i = 0; i < gArr.length(); i++) g[i] = gArr.optInt(i, 0);
            p.graph = g;
        }

        // Restore custom node positions
        JSONObject npObj = json.optJSONObject("node_positions");
        if (npObj != null) {
            Iterator<String> keys = npObj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONArray xyArr = npObj.optJSONArray(k);
                if (xyArr != null && xyArr.length() >= 2) {
                    float[] xy = new float[]{(float) xyArr.optDouble(0, 0.0), (float) xyArr.optDouble(1, 0.0)};
                    p.nodePositions.put(k, xy);
                }
            }
        }

        // Restore Helix routing state (absent in presets saved before R15 §6)
        p.routingMode = json.optInt("routingMode", -1);
        p.splitMode = json.optInt("splitMode", -1);
        JSONObject rgObj = json.optJSONObject("routingGains");
        if (rgObj != null) {
            Iterator<String> rgKeys = rgObj.keys();
            while (rgKeys.hasNext()) {
                String k = rgKeys.next();
                JSONObject g = rgObj.optJSONObject(k);
                if (g == null) continue;
                p.routingGains.put(k, new float[]{
                        (float) g.optDouble("a", 1.0f),
                        (float) g.optDouble("b", 1.0f)});
            }
        }

        return p;
    }

    /**
     * Converts legacy AmpRack Firestore/JSON preset format (controls semicolon-delimited string) to Preset model.
     * Legacy format:
     * {
     *   "name": "Preset Name",
     *   "controls": {
     *     "0": { "name": "PluginName", "controls": "0.5;0.7;1.0", "selectedModel": "file.nam" }
     *   }
     * }
     */
    public static Preset convertLegacyJsonToPreset(JSONObject legacyJson) {
        Preset p = new Preset();
        p.id = UUID.randomUUID().toString();
        p.name = legacyJson.optString("name", "Converted Legacy Preset");
        p.description = legacyJson.optString("desc", "Imported from legacy AmpRack");
        p.author = legacyJson.optString("author", "AmpRack User");
        p.createdAt = System.currentTimeMillis();
        p.modifiedAt = p.createdAt;

        JSONObject controlsObj = legacyJson.optJSONObject("controls");
        if (controlsObj != null) {
            p.plugins = new ArrayList<>();
            Iterator<String> keys = controlsObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject pluginObj = controlsObj.getJSONObject(key);
                    PluginItem item = new PluginItem();
                    item.name = pluginObj.optString("name", "Plugin");
                    item.enabled = pluginObj.optBoolean("enabled", true);

                    String ctrlStr = pluginObj.optString("controls", "");
                    if (!ctrlStr.isEmpty()) {
                        item.controls = new ArrayList<>();
                        String[] parts = ctrlStr.split(";");
                        for (int c = 0; c < parts.length; c++) {
                            ControlItem ci = new ControlItem();
                            ci.name = "Control " + c;
                            try {
                                ci.value = Float.parseFloat(parts[c].trim());
                            } catch (NumberFormatException nfe) {
                                ci.value = 0.0f;
                            }
                            item.controls.add(ci);
                        }
                    }

                    if (pluginObj.has("selectedModel")) {
                        item.filePorts = new ArrayList<>();
                        FilePortItem fpi = new FilePortItem();
                        fpi.portIndex = 0;
                        fpi.portName = "Model File";
                        fpi.fileName = pluginObj.optString("selectedModel");
                        fpi.relativePath = fpi.fileName;
                        item.filePorts.add(fpi);
                    }

                    p.plugins.add(item);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing legacy plugin item: " + key, e);
                }
            }
        }
        return p;
    }

    // Data Models
    public static class Preset {
        public String id;
        public String name;
        public String description;
        public String author;
        public long createdAt;
        public long modifiedAt;
        public boolean favorite;
        public float masterInputVolume = 1.0f;
        public float masterOutputVolume = 1.0f;
        public List<String> tags = new ArrayList<>();
        public List<PluginItem> plugins = new ArrayList<>();
        public int[] graph;
        public Map<String, float[]> nodePositions = new HashMap<>(); // pluginName/id -> [x, y]
        // Helix routing state (-1 = not captured, keep engine current).
        public int routingMode = -1;
        public int splitMode = -1;
        public Map<String, float[]> routingGains = new HashMap<>(); // split slot -> [gainA, gainB]
    }

    public static class PluginItem {
        public String name;
        public String library;
        public int uniqueId;
        public boolean enabled = true;
        public List<ControlItem> controls = new ArrayList<>();
        public List<FilePortItem> filePorts = new ArrayList<>();
        // TONE3000 origin metadata (nullable — existing presets without this continue to load)
        public String tone3000ToneId;
        public String tone3000ModelId;
        public String tone3000Creator;
        public String tone3000ToneTitle;
        public String tone3000Format;
        public String tone3000GearType;
        public String tone3000SourceUrl;
    }

    public static class ControlItem {
        public String name;
        public float value;
    }

    public static class FilePortItem {
        public int portIndex;
        public String portName;
        public String fileName;
        public String relativePath;
    }
}
