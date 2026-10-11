package com.kancrutt.gigginggear;

import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * R24 (Hery): plugin favorites. Favourites are stored by STABLE KEY
 * (PluginKey format), never by display name, so a favourite cannot silently
 * retarget when two plugins share a name. The routing blocks participate with
 * synthetic keys ({@link #SPLIT_KEY} / {@link #MERGE_KEY}).
 *
 * <p>Persistence: the same private prefs file as the preset favourites
 * ({@code Activity.getPreferences(MODE_PRIVATE)}), key
 * {@link #PREF_KEY}. The pure set operations are separated so host tests can
 * exercise the logic without Android classes.</p>
 */
public final class FavStore {

    public static final String PREF_KEY = "favoritePluginKeys";
    public static final String SPLIT_KEY = "BUILTIN|split|0";
    public static final String MERGE_KEY = "BUILTIN|merge|0";

    private FavStore() {}

    // ---- pure logic (host-testable) ----

    public static boolean isFav(Set<String> favs, String key) {
        return key != null && favs != null && favs.contains(key);
    }

    /** Toggles membership in {@code favs}; returns true when the key ended up favourited. */
    public static boolean toggle(Set<String> favs, String key) {
        if (key == null) return false;
        if (favs.contains(key)) {
            favs.remove(key);
            return false;
        }
        favs.add(key);
        return true;
    }

    // ---- SharedPreferences wrapper ----

    public static Set<String> keys(SharedPreferences sp) {
        Set<String> stored = sp.getStringSet(PREF_KEY, null);
        return stored == null ? new LinkedHashSet<String>() : new LinkedHashSet<>(stored);
    }

    public static boolean isFav(SharedPreferences sp, String key) {
        return isFav(keys(sp), key);
    }

    public static boolean toggle(SharedPreferences sp, String key) {
        Set<String> favs = keys(sp);
        boolean added = toggle(favs, key);
        sp.edit().putStringSet(PREF_KEY, favs).apply();
        return added;
    }
}
