package com.kancrutt.gigginggear;

import android.graphics.Color;

public class CategoryHelper {

    public static final int COLOR_AMP = Color.parseColor("#D96B58");
    public static final int COLOR_CAB = Color.parseColor("#D0D0D0");
    public static final int COLOR_DIST = Color.parseColor("#E07A5F");
    public static final int COLOR_MOD = Color.parseColor("#5C82E6");
    public static final int COLOR_DELAY = Color.parseColor("#52B788");
    public static final int COLOR_REVERB = Color.parseColor("#9D65C9");
    public static final int COLOR_COMP = Color.parseColor("#E09F3E");
    public static final int COLOR_EQ = Color.parseColor("#8CB369");
    public static final int COLOR_PITCH = Color.parseColor("#7B52AE");
    public static final int COLOR_WAH = Color.parseColor("#4EA8DE");
    public static final int COLOR_VOL = Color.parseColor("#48CAE4");
    public static final int COLOR_GATE = Color.parseColor("#F4A261");
    /** Routing utilities (SPLIT/MERGE) — neutral gray, deliberately not hue-coded. */
    public static final int COLOR_ROUTING = Color.parseColor("#8F94A0");
    public static final int COLOR_DEFAULT = Color.parseColor("#8F94A0");

    public enum Category {
        AMP("AMP", COLOR_AMP, R.drawable.ic_cat_amp, R.drawable.sl_cat_amp),
        CABINET("CAB", COLOR_CAB, R.drawable.ic_cat_cabinet, R.drawable.sl_cat_cabinet),
        DISTORTION("DIST", COLOR_DIST, R.drawable.ic_cat_distortion, R.drawable.sl_cat_distortion),
        MODULATION("MOD", COLOR_MOD, R.drawable.ic_cat_modulation, R.drawable.sl_cat_modulation),
        DELAY("DELAY", COLOR_DELAY, R.drawable.ic_cat_delay, R.drawable.sl_cat_delay),
        REVERB("REVERB", COLOR_REVERB, R.drawable.ic_cat_reverb, R.drawable.sl_cat_reverb),
        DYNAMICS("COMP", COLOR_COMP, R.drawable.ic_cat_dynamics, R.drawable.sl_cat_dynamics),
        EQ("EQ", COLOR_EQ, R.drawable.ic_cat_eq, R.drawable.sl_cat_eq),
        PITCH("PITCH", COLOR_PITCH, R.drawable.ic_cat_pitch, R.drawable.sl_cat_pitch),
        WAH("WAH", COLOR_WAH, R.drawable.ic_cat_wah, R.drawable.sl_cat_wah),
        UTILITY("UTILITY", COLOR_ROUTING, R.drawable.ic_cat_utility, R.drawable.sl_cat_utility),
        GATE("GATE", COLOR_GATE, R.drawable.ic_cat_gate, R.drawable.sl_cat_gate),
        // Routing utilities (not audio effects): neutral/uncolored treatment,
        // same convention as CABINET staying uncolored.
        SPLIT("SPLIT", COLOR_ROUTING, R.drawable.ic_routing_split, R.drawable.ic_routing_split),
        MERGE("MERGE", COLOR_ROUTING, R.drawable.ic_routing_merge, R.drawable.ic_routing_merge),
        /** Step B: unknown/unclassified entries land here and stay VISIBLE
         *  (Other chip). Nothing is hidden for being unknown. */
        OTHER("OTHER", COLOR_DEFAULT, R.drawable.ic_cat_volume, R.drawable.sl_cat_unknown);

        public final String code;
        public final int color;
        public final int iconRes;
        public final int selectorRes;

        Category(String code, int color, int iconRes, int selectorRes) {
            this.code = code;
            this.color = color;
            this.iconRes = iconRes;
            this.selectorRes = selectorRes;
        }
    }


    /**
     * Step B: ONE category source with fixed precedence (no runtime heuristics):
     *   1. canonical enum-name passthrough (callers pass cat.name())
     *   2. PluginCatalogData approved table (Hery-approved 2026-10-11), which is
     *      itself resolved by the generator as: approved override by stable key
     *      > LV2 class URI / LADSPA documented category > Other
     *   3. OTHER — unknown entries stay visible in browse, never hidden
     * Ambiguous names resolve to the first (deterministic) catalog entry and
     * log a warning; identity decisions must go through the stable key.
     */
    public static Category getCategory(String pluginName) {
        if (pluginName == null) return Category.OTHER;
        String n = pluginName.trim();
        // Enum-name passthrough (getIconRes("AMP")-style callers). valueOf
        // instead of a static values() map: a static initializer here would
        // re-enter Category.<clinit> when the enum is touched first.
        Category direct = null;
        try { direct = Category.valueOf(n); } catch (IllegalArgumentException ignored) {}
        if (direct != null) return normalizeRouting(direct);
        String lower = n.toLowerCase(java.util.Locale.US);
        if (lower.equals("split") || lower.equals("signal split")) return Category.UTILITY;
        if (lower.equals("merge") || lower.equals("signal merge")) return Category.UTILITY;

        int[] hits = PluginCatalogData.indicesForName(n);
        if (hits.length == 0) return Category.OTHER;
        if (hits.length > 1) {
            try { android.util.Log.w("CategoryHelper",
                    "ambiguous name '" + pluginName + "' (" + hits.length
                    + " catalog entries); using " + PluginCatalogData.KEY[hits[0]]); }
            catch (Throwable ignored) {}
        }
        try {
            return normalizeRouting(Category.valueOf(PluginCatalogData.CATEGORY[hits[0]]));
        } catch (IllegalArgumentException e) {
            return Category.OTHER;
        }
    }

    /** Routing blocks (SPLIT/MERGE) have no picker chip — expose them as
     * UTILITY so they browse under the Utility chip. (R11: Hery.) */
    static Category normalizeRouting(Category c) {
        if (c == Category.SPLIT || c == Category.MERGE) return Category.UTILITY;
        return c;
    }

    public static String getCode(String pluginName) {
        return getCategory(pluginName).code;
    }

    public static int getColor(String pluginName) {
        return getCategory(pluginName).color;
    }

    public static int getIconRes(String pluginName) {
        return getCategory(pluginName).iconRes;
    }

    public static int getSelectorRes(String pluginName) {
        return getCategory(pluginName).selectorRes;
    }
}
