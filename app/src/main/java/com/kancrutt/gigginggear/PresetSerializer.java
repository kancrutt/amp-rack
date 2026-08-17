package com.kancrutt.gigginggear;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure Java preset serializer/deserializer.
 * Takes snapshots of AudioEngine state without touching UI views or toasts.
 * Decouples JNI calls from MainActivity.java.
 */
public class PresetSerializer {
    private static final String TAG = "PresetSerializer";

    /**
     * Reads current active plugin chain state from AudioEngine JNI and returns a HashMap map.
     */
    public static Map<String, Object> captureCurrentRackState() {
        Map<String, Object> presetMap = new HashMap<>();
        int totalPlugins = AudioEngine.getActivePlugins();
        Map<String, Object> controlsMap = new HashMap<>();

        for (int i = 0; i < totalPlugins; i++) {
            float[] values = AudioEngine.getActivePluginValues(i);
            StringBuilder sb = new StringBuilder();
            if (values != null) {
                for (int v = 0; v < values.length; v++) {
                    sb.append(values[v]);
                    if (v < values.length - 1) sb.append(";");
                }
            }

            Map<String, Object> pluginMap = new HashMap<>();
            pluginMap.put("name", AudioEngine.getActivePluginName(i));
            pluginMap.put("controls", sb.toString());
            pluginMap.put("enabled", AudioEngine.getActivePluginEnabled(i));
            controlsMap.put(String.valueOf(i), pluginMap);
        }

        presetMap.put("controls", controlsMap);
        presetMap.put("timestamp", System.currentTimeMillis());
        return presetMap;
    }

    /**
     * Converts a live rack snapshot map to a LocalPresetManager.Preset model object.
     */
    public static LocalPresetManager.Preset mapToPresetModel(String name, String description, Map<String, Object> rackMap) {
        LocalPresetManager.Preset p = new LocalPresetManager.Preset();
        p.name = name;
        p.description = description;
        p.createdAt = System.currentTimeMillis();
        p.modifiedAt = p.createdAt;

        Map<String, Object> controlsMap = (Map<String, Object>) rackMap.get("controls");
        if (controlsMap != null) {
            p.plugins = new ArrayList<>();
            for (Map.Entry<String, Object> entry : controlsMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> pluginObj = (Map<String, Object>) entry.getValue();
                    LocalPresetManager.PluginItem item = new LocalPresetManager.PluginItem();
                    item.name = (String) pluginObj.get("name");
                    item.enabled = pluginObj.get("enabled") != null ? (Boolean) pluginObj.get("enabled") : true;

                    String ctrlStr = (String) pluginObj.get("controls");
                    if (ctrlStr != null && !ctrlStr.isEmpty()) {
                        item.controls = new ArrayList<>();
                        String[] parts = ctrlStr.split(";");
                        for (int c = 0; c < parts.length; c++) {
                            LocalPresetManager.ControlItem ci = new LocalPresetManager.ControlItem();
                            ci.name = "Control " + c;
                            try {
                                ci.value = Float.parseFloat(parts[c].trim());
                            } catch (NumberFormatException nfe) {
                                ci.value = 0.0f;
                            }
                            item.controls.add(ci);
                        }
                    }
                    p.plugins.add(item);
                }
            }
        }
        return p;
    }
}
