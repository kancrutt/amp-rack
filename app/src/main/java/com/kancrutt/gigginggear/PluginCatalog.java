package com.kancrutt.gigginggear;

import android.util.Log;

/** Step-B registry over the generated PluginCatalogData.
 *
 *  ONE gate for every UI surface (picker browse/search, SELECT MODEL
 *  browse/search, legacy menu, preset load, addPluginByKey):
 *      visible = curated AND NOT blacklisted AND NOT synth
 *  Ambiguous names never resolve — callers must fail visibly. */
final class PluginCatalog {

    private static final String TAG = "PluginCatalog";

    private PluginCatalog() {}

    static int indexOfKey(String key) { return PluginCatalogData.indexOfKey(key); }

    static String keyOf(int i) { return PluginCatalogData.KEY[i]; }
    static String nameOf(int i) { return PluginCatalogData.NAME[i]; }
    static String libOf(int i) { return PluginCatalogData.LIB[i]; }
    static int idxOf(int i) { return PluginCatalogData.IDX[i]; }
    static boolean isLv2(int i) { return PluginKey.LV2.equals(PluginCatalogData.SOURCE[i]); }
    static String categoryOf(int i) { return PluginCatalogData.CATEGORY[i]; }

    static String[] aliasesOf(int i) { return PluginCatalogData.ALIASES[i]; }

    /** The single curation gate. */
    static boolean isVisible(int i) {
        if (i < 0 || i >= PluginCatalogData.COUNT) return false;
        if (!PluginCatalogData.CURATED[i]) return false;
        if (PluginCatalogData.SYNTH[i]) return false;
        if (Blacklist.isKeyBlacklisted(PluginCatalogData.KEY[i])) return false;
        if (Blacklist.isLibBlacklisted(PluginCatalogData.LIB[i])) return false;
        return true;
    }

    /** Curated-and-not-blacklisted for a display name (legacy callers). */
    static boolean isNameVisible(String name) {
        for (int i : PluginCatalogData.indicesForName(name)) {
            if (isVisible(i)) return true;
        }
        return false;
    }

    /** Unique resolution for a name; -1 when unknown, -2 when ambiguous. */
    static int resolveName(String name) {
        int[] hits = PluginCatalogData.indicesForName(name);
        if (hits.length == 0) return -1;
        if (hits.length > 1) {
            try { Log.w(TAG, "ambiguous plugin name '" + name + "' resolves to "
                    + hits.length + " keys; refusing to auto-pick"); } catch (Throwable ignored) {}
            return -2;
        }
        return hits[0];
    }

    /** Human-readable result of an ambiguous/unknown resolution, or null. */
    static String describeNameProblem(String name) {
        int[] hits = PluginCatalogData.indicesForName(name);
        if (hits.length == 0) return null;
        if (hits.length == 1) return null;
        StringBuilder sb = new StringBuilder("Ambiguous name '" + name + "' (")
                .append(hits.length).append(" matches):");
        for (int i : hits) sb.append(' ').append(PluginCatalogData.KEY[i]);
        return sb.toString();
    }
}
