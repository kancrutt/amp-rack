package com.kancrutt.gigginggear;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Runtime reader for assets/guitar_plugins_catalog.json.
 *
 *  The catalog is the research-derived info layer for every curated guitar
 *  plugin: official manufacturer, project homepage/source, description,
 *  sub-category and tags. It is DISPLAY-ONLY metadata — visibility is still
 *  decided by {@link GuitarPluginCatalog}; the catalog never adds or removes a
 *  plugin from the browse list, it only enriches the browse card and the
 *  detail info panel. Null-safe by design: a missing/corrupt asset simply
 *  leaves every lookup returning null so callers fall back to the legacy
 *  per-plugin description, exactly as before the catalog existed. */
final class PluginCatalogInfo {
    private static final String TAG = "PluginCatalogInfo";
    private static final String ASSET = "guitar_plugins_catalog.json";

    static final class Info {
        final String name;
        final String manufacturer;
        final String homepage;
        final String source;
        final String description;
        final String subcategory;
        final String category;
        final String tags;
        Info(String name, String manufacturer, String homepage, String source,
             String description, String subcategory, String category, String tags) {
            this.name = name;
            this.manufacturer = emptyToNull(manufacturer);
            this.homepage = emptyToNull(homepage);
            this.source = emptyToNull(source);
            this.description = emptyToNull(description);
            this.subcategory = emptyToNull(subcategory);
            this.category = emptyToNull(category);
            this.tags = emptyToNull(tags);
        }
    }

    /** Lowercased plugin name -> info. Null until init() succeeds. */
    private static volatile Map<String, Info> sBy;

    private PluginCatalogInfo() {}

    /** Load the catalog asset. Safe to call more than once; never throws. */
    static void init(Context context) {
        if (sBy != null || context == null) return;
        try {
            JSONObject root = ConnectGuitar.loadJSONFromAssetFile(context, ASSET);
            if (root == null) return;
            JSONArray arr = root.optJSONArray("plugins");
            if (arr == null) return;
            Map<String, Info> m = new HashMap<>(arr.length() * 2);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String name = o.optString("name", "");
                if (name.isEmpty()) continue;
                JSONArray t = o.optJSONArray("tags");
                StringBuilder tags = new StringBuilder();
                if (t != null) {
                    for (int j = 0; j < t.length(); j++) {
                        if (j > 0) tags.append(", ");
                        tags.append(t.optString(j, ""));
                    }
                }
                m.put(name.toLowerCase(Locale.US).trim(), new Info(
                        name,
                        o.optString("manufacturer", ""),
                        o.optString("homepage", ""),
                        o.optString("source", ""),
                        o.optString("description", ""),
                        o.optString("subcategory", ""),
                        o.optString("category", ""),
                        tags.toString()));
            }
            sBy = m;
            try { Log.i(TAG, "catalog loaded: " + m.size() + " plugins"); } catch (Throwable ignored) {}
        } catch (Throwable t) {
            try { Log.w(TAG, "catalog asset unavailable, info lookups disabled", t); } catch (Throwable ignored) {}
            sBy = null;
        }
    }

    /** Test hook: clear the in-memory map (back to disabled). */
    static void clearForTesting() { sBy = null; }

    /** True when the catalog asset is loaded and populated. */
    static boolean isLoaded() { return sBy != null && !sBy.isEmpty(); }

    private static Info find(String pluginName) {
        Map<String, Info> m = sBy;
        if (m == null || pluginName == null) return null;
        String key = pluginName.toLowerCase(Locale.US).trim();
        Info i = m.get(key);
        if (i == null && key.contains(" ")) i = m.get(key.replace(' ', '_'));
        return i;
    }

    /** Official manufacturer for a plugin, or null when unknown. */
    static String manufacturerOf(String pluginName) {
        Info i = find(pluginName);
        return i == null ? null : i.manufacturer;
    }

    /** Browse-card subtitle: the manufacturer when known, else the given
     *  description fallback. Keeps the list informative for catalog plugins
     *  and unchanged for everything else. */
    static String brandLine(String pluginName, String fallbackDesc) {
        String man = manufacturerOf(pluginName);
        return man != null ? man : fallbackDesc;
    }

    /** Multi-line info-panel body: manufacturer / homepage / sub-category /
     *  tags / description. Returns null when the plugin is not in the catalog
     *  (caller falls back to the legacy description). */
    static String describe(String pluginName) {
        Info i = find(pluginName);
        if (i == null) return null;
        StringBuilder sb = new StringBuilder();
        if (i.manufacturer != null) sb.append(i.manufacturer).append('\n');
        String home = i.homepage != null ? i.homepage : i.source;
        if (home != null) sb.append(home).append('\n');
        // R2 (Hery): the category line follows the approved table, not the
        // research catalog's stale sub-category strings.
        CategoryHelper.Category c = CategoryHelper.getCategory(pluginName);
        String catLine = c != null && c != CategoryHelper.Category.UNKNOWN
                ? cap(c.name().toLowerCase(java.util.Locale.US))
                : cap(i.subcategory != null ? i.subcategory : i.category);
        if (catLine != null) {
            sb.append(catLine);
            if (i.tags != null) sb.append("  ·  ").append(i.tags);
            sb.append('\n');
        }
        if (i.description != null) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(i.description);
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
