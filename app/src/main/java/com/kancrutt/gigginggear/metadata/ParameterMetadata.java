package com.kancrutt.gigginggear.metadata;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable parameter metadata loaded from parameter_metadata.json asset.
 * Drives UI rendering: unit, control type, enum values/labels, default, min/max.
 */
public final class ParameterMetadata {
    private static final String TAG = "ParameterMetadata";

    public enum ControlType {
        KNOB,
        LOG_KNOB,
        INTEGER_STEPPER,
        TOGGLE,
        ENUM_PICKER,
        ATOM
    }

    public enum PluginType {
        FLOAT,
        INT,
        BOOLEAN,
        ENUM,
        ATOM
    }

    public static final class PortMeta {
        public final String name;
        public final String symbol;
        public final int portIndex;
        public final float defaultValue;
        public final float min;
        public final float max;
        public final String unit;
        public final ControlType controlType;
        public final PluginType pluginType;
        public final boolean isLogarithmic;
        public final boolean isInteger;
        public final boolean isToggled;
        public final float[] enumValues;
        public final String[] enumLabels;
        public final int stepCount;
        public final boolean isSampleRate;

        public PortMeta(String name, String symbol, int portIndex, float defaultValue,
                        float min, float max, String unit, ControlType controlType,
                        PluginType pluginType, boolean isLogarithmic, boolean isInteger,
                        boolean isToggled, float[] enumValues, String[] enumLabels) {
            this(name, symbol, portIndex, defaultValue, min, max, unit, controlType,
                    pluginType, isLogarithmic, isInteger, isToggled, false, enumValues, enumLabels);
        }

        public PortMeta(String name, String symbol, int portIndex, float defaultValue,
                        float min, float max, String unit, ControlType controlType,
                        PluginType pluginType, boolean isLogarithmic, boolean isInteger,
                        boolean isToggled, boolean isSampleRate, float[] enumValues,
                        String[] enumLabels) {
            this.name = name;
            this.symbol = symbol;
            this.portIndex = portIndex;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.unit = unit == null ? "" : unit;
            this.controlType = controlType;
            this.pluginType = pluginType;
            this.isLogarithmic = isLogarithmic;
            this.isInteger = isInteger;
            this.isToggled = isToggled;
            this.isSampleRate = isSampleRate;
            this.enumValues = enumValues;
            this.enumLabels = enumLabels;
            this.stepCount = enumValues != null ? enumValues.length : 0;
        }

        public boolean hasEnum() {
            return enumValues != null && enumValues.length > 0;
        }
    }

    private final Map<String, Map<Integer, PortMeta>> ladspaMeta = new HashMap<>();
    private final Map<String, Map<String, PortMeta>> lv2Meta = new HashMap<>();

    private ParameterMetadata() {}

    private static void warn(String message) {
        try { Log.w(TAG, message); } catch (Throwable ignored) { }
    }

    private static void error(String message, Throwable error) {
        try { Log.e(TAG, message, error); } catch (Throwable ignored) { }
    }

    public static ParameterMetadata loadFromAsset(android.content.Context ctx) {
        ParameterMetadata meta = new ParameterMetadata();
        if (ctx == null) {
            warn("Cannot load metadata without a context; using empty registry");
            return meta;
        }
        try {
            String json = loadJsonFromAsset(ctx, "parameter_metadata.json");
            if (json == null || json.isEmpty()) {
                warn("parameter_metadata.json not found or empty");
                return meta;
            }
            return fromJson(json);
        } catch (Throwable e) {
            error("Failed to parse parameter_metadata.json", e);
        }
        return meta;
    }

    /** Safe parser used by startup and JVM tests. Never throws for bad metadata. */
    public static ParameterMetadata fromJson(String json) {
        ParameterMetadata meta = new ParameterMetadata();
        try {
            if (json == null || json.trim().isEmpty()) return meta;
            JSONObject root = new JSONObject(json);
            // LADSPA
            if (root.has("ladspa")) {
                JSONObject ladspa = root.getJSONObject("ladspa");
                java.util.Iterator<String> ladspaKeys = ladspa.keys();
                while (ladspaKeys.hasNext()) {
                    String uidStr = ladspaKeys.next();
                    JSONObject p = ladspa.getJSONObject(uidStr);
                    String lib = p.optString("library", "");
                    String pname = p.optString("name", "");
                    String key = uidStr; // uniqueId as key
                    Map<Integer, PortMeta> ports = new HashMap<>();
                    if (p.has("ports")) {
                        JSONArray portsArr = p.getJSONArray("ports");
                        for (int i = 0; i < portsArr.length(); i++) {
                            JSONObject port = portsArr.getJSONObject(i);
                            PortMeta pm = parsePortMeta(port);
                            if (pm != null) {
                                ports.put(pm.portIndex, pm);
                            }
                        }
                    }
                    meta.ladspaMeta.put(key, ports);
                }
            }
            // LV2
            if (root.has("lv2")) {
                JSONObject lv2 = root.getJSONObject("lv2");
                java.util.Iterator<String> lv2Keys = lv2.keys();
                while (lv2Keys.hasNext()) {
                    String uri = lv2Keys.next();
                    JSONObject p = lv2.getJSONObject(uri);
                    String lib = p.optString("library", "");
                    String pname = p.optString("name", "");
                    Map<String, PortMeta> ports = new HashMap<>();
                    if (p.has("ports")) {
                        JSONArray portsArr = p.getJSONArray("ports");
                        for (int i = 0; i < portsArr.length(); i++) {
                            JSONObject port = portsArr.getJSONObject(i);
                            PortMeta pm = parsePortMeta(port);
                            if (pm != null) {
                                ports.put(pm.symbol, pm);
                            }
                        }
                    }
                    meta.lv2Meta.put(uri, ports);
                }
            }
            try { Log.d(TAG, "Loaded metadata: " + meta.ladspaMeta.size() + " LADSPA plugins, " + meta.lv2Meta.size() + " LV2 plugins"); } catch (Throwable ignored) { }
        } catch (Throwable e) {
            error("Failed to parse parameter metadata", e);
        }
        return meta;
    }

    private static String loadJsonFromAsset(android.content.Context ctx, String path) {
        try {
            java.io.InputStream is = ctx.getAssets().open(path);
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            is.close();
            return result;
        } catch (Exception e) {
            error("Failed to load asset: " + path, e);
            return null;
        }
    }

    private static PortMeta parsePortMeta(JSONObject port) {
        try {
            String name = port.optString("name", "");
            int portIndex = port.has("port_index") ? port.optInt("port_index", 0) : port.optInt("index", 0);
            String symbol = port.optString("symbol", "port_" + portIndex);
            boolean hasMin = port.has("min") && !port.isNull("min") && port.opt("min") instanceof Number;
            boolean hasMax = port.has("max") && !port.isNull("max") && port.opt("max") instanceof Number;
            float min = hasMin ? (float) port.optDouble("min") : 0.0f;
            float max = hasMax ? (float) port.optDouble("max") : 1.0f;
            if (!Float.isFinite(min) || !Float.isFinite(max) || min > max) {
                warn("Ignoring invalid metadata range for " + symbol);
                min = 0.0f;
                max = 1.0f;
            }
            float defaultValue = (float) port.optDouble("default", min);
            if (!Float.isFinite(defaultValue)) defaultValue = min;
            defaultValue = Math.max(min, Math.min(max, defaultValue));
            String unit = port.optString("unit", "");

            String ctlTypeStr = port.optString("control_type", "KNOB");
            String pluginTypeStr = port.optString("plugin_type", "FLOAT");
            boolean isLogarithmic = port.optBoolean("logarithmic", false);
            boolean isInteger = port.optBoolean("integer", false);
            boolean isToggled = port.optBoolean("toggled", false);
            boolean isSampleRate = port.optBoolean("sample_rate", false);
            // 3.2-B: log when rdf_default and hint_default disagree (declared vs
            // descriptor source). Non-fatal: the merged "default" still wins.
            if (port.optBoolean("conflict_default", false)) {
                warn("Default source conflict for " + symbol + " in " + name
                        + " (rdf_default=" + port.opt("rdf_default")
                        + ", hint_default=" + port.opt("hint_default") + ")");
            }

            ControlType controlType = ControlType.KNOB;
            if ("ENUM".equals(ctlTypeStr)) ctlTypeStr = "ENUM_PICKER";
            try { controlType = ControlType.valueOf(ctlTypeStr); } catch (IllegalArgumentException ignored) {}
            PluginType pluginType = PluginType.FLOAT;
            try { pluginType = PluginType.valueOf(pluginTypeStr); } catch (IllegalArgumentException ignored) {}

            float[] enumValues = null;
            String[] enumLabels = null;
            if (port.has("enum_values") && port.has("enum_labels")) {
                JSONArray vals = port.getJSONArray("enum_values");
                JSONArray lbls = port.getJSONArray("enum_labels");
                int n = Math.min(vals.length(), lbls.length());
                enumValues = new float[n];
                enumLabels = new String[n];
                for (int i = 0; i < n; i++) {
                    enumValues[i] = (float) vals.optDouble(i, 0.0);
                    enumLabels[i] = lbls.optString(i, "");
                }
            }

            return new PortMeta(name, symbol, portIndex, defaultValue, min, max, unit,
                    controlType, pluginType, isLogarithmic, isInteger, isToggled,
                    isSampleRate, enumValues, enumLabels);
        } catch (JSONException e) {
            return null;
        }
    }

    public PortMeta getLadspaPort(String uniqueId, int portIndex) {
        Map<Integer, PortMeta> ports = ladspaMeta.get(uniqueId);
        return ports != null ? ports.get(portIndex) : null;
    }

    public java.util.Set<String> lv2Keys() {
        return lv2Meta.keySet();
    }

    public java.util.Set<String> ladspaKeys() {
        return ladspaMeta.keySet();
    }

    public Map<String, PortMeta> getLv2Plugin(String uri) {
        return lv2Meta.get(uri);
    }

    public Map<Integer, PortMeta> getLadspaPlugin(String uniqueId) {
        return ladspaMeta.get(uniqueId);
    }

    public PortMeta getLv2Port(String uri, String key) {
        Map<String, PortMeta> ports = lv2Meta.get(uri);
        if (ports == null || key == null) return null;
        PortMeta bySymbol = ports.get(key);
        if (bySymbol != null) return bySymbol;
        // MainActivity looks LV2 ports up by display name
        // (AudioEngine.getControlName returns the port name, not the symbol).
        // Fall back to the first port whose name matches.
        for (PortMeta p : ports.values()) {
            if (key.equals(p.name)) return p;
        }
        return null;
    }
}
