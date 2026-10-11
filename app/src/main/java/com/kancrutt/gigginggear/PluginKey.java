package com.kancrutt.gigginggear;

/** Stable plugin identity (Step B1).
 *
 *  Key = source|lib|index where:
 *    - LADSPA: index is the plugin UniqueID (id), lib is the .so name
 *    - LV2:    index is the descriptor ordinal inside the library bundle
 *
 *  Display names are labels only and are never used to pick between
 *  entries (an ambiguous name fails visibly). */
public final class PluginKey {

    public static final String LADSPA = "LADSPA";
    public static final String LV2 = "LV2";

    public final String source;
    public final String lib;
    public final String index;

    private PluginKey(String source, String lib, String index) {
        this.source = source;
        this.lib = lib;
        this.index = index;
    }

    public static String make(String source, String lib, Object idOrIndex) {
        return source + "|" + lib + "|" + String.valueOf(idOrIndex);
    }

    /** Returns null when the string is not a well-formed key. */
    public static PluginKey parse(String key) {
        if (key == null) return null;
        String[] p = key.split("\\|", -1);
        if (p.length != 3) return null;
        if (!LADSPA.equals(p[0]) && !LV2.equals(p[0])) return null;
        if (p[1].isEmpty() || p[2].isEmpty()) return null;
        return new PluginKey(p[0], p[1], p[2]);
    }

    public String key() { return source + "|" + lib + "|" + index; }

    @Override public String toString() { return key(); }
}
