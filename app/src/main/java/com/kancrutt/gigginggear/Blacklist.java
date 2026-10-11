package com.kancrutt.gigginggear;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Single blacklist (Step B3), one file enforced everywhere.
 *
 *  assets/blacklist.json:
 *    { "keys": ["LV2|mindi.so|0"], "libs": ["libbad.so"] }
 *  A key is the stable PluginKey (source|lib|index); a lib entry blocks the
 *  whole library. Name entries are no longer supported — names are labels,
 *  identities are keys. Enforced at catalog build, browse, search (picker +
 *  SELECT MODEL), addPluginByKey, the legacy menu, preset load, and in the
 *  native loader (see Engine.cpp, which refuses dlopen for blacklisted libs). */
final class Blacklist {

    private static final String TAG = "Blacklist";
    private static final String ASSET = "blacklist.json";

    private static Set<String> sKeys = Collections.emptySet();
    private static Set<String> sLibs = Collections.emptySet();

    private Blacklist() {}

    static void init(Context context) {
        try {
            InputStream is = context.getAssets().open(ASSET);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            loadFromJson(new String(buf, "UTF-8"));
        } catch (Throwable t) {
            try { Log.w(TAG, "blacklist asset unavailable, nothing blocked", t); } catch (Throwable ignored) {}
            sKeys = Collections.emptySet();
            sLibs = Collections.emptySet();
        }
    }

    /** Package-private for unit tests. */
    static void loadFromJson(String json) {
        Set<String> keys = new HashSet<>();
        Set<String> libs = new HashSet<>();
        try {
            JSONObject root = new JSONObject(json);
            org.json.JSONArray k = root.optJSONArray("keys");
            if (k != null) {
                for (int i = 0; i < k.length(); i++) {
                    String v = k.optString(i, "").trim();
                    if (!v.isEmpty()) keys.add(v);
                }
            }
            org.json.JSONArray l = root.optJSONArray("libs");
            if (l != null) {
                for (int i = 0; i < l.length(); i++) {
                    String v = l.optString(i, "").trim();
                    if (!v.isEmpty()) libs.add(v);
                }
            }
        } catch (Throwable t) {
            try { Log.e(TAG, "failed to parse blacklist asset", t); } catch (Throwable ignored) {}
            keys.clear();
            libs.clear();
        }
        sKeys = keys;
        sLibs = libs;
        try { Log.i(TAG, "blacklist loaded: " + sKeys.size() + " keys, " + sLibs.size() + " libs"); }
        catch (Throwable ignored) {}
    }

    static boolean isKeyBlacklisted(String key) { return sKeys.contains(key); }

    static boolean isLibBlacklisted(String lib) {
        if (lib == null) return false;
        return sLibs.contains(lib);
    }

    /** Resolve a display name to its key(s) via the catalog registry. */
    static boolean isNameBlacklisted(String name) {
        if (name == null) return false;
        for (int i : PluginCatalogData.indicesForName(name)) {
            if (sKeys.contains(PluginCatalogData.KEY[i])) return true;
            if (sLibs.contains(PluginCatalogData.LIB[i])) return true;
        }
        return false;
    }

    /** Test helper: drop in-memory state. */
    static void clearForTesting() {
        sKeys = Collections.emptySet();
        sLibs = Collections.emptySet();
    }
}
