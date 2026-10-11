package com.kancrutt.gigginggear;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** R24 (Hery): plugin favourites — pure logic half (the SharedPreferences
 *  wrapper is a two-line delegation tested on device). */
public class FavStoreTest {

    @Test public void toggleRoundTrip() {
        Set<String> favs = new LinkedHashSet<>();
        String key = "LV2|libGxUltraCab.lv2.so|0";
        assertTrue(FavStore.toggle(favs, key));
        assertTrue(FavStore.isFav(favs, key));
        assertFalse(FavStore.toggle(favs, key));
        assertFalse(FavStore.isFav(favs, key));
    }

    @Test public void favsAreKeyScopedNotNameScoped() {
        Set<String> favs = new LinkedHashSet<>();
        // the Flanger name collision: two different plugins, one display name
        String ladspa = "LADSPA|libflanger_1191.so|1191";
        String lv2 = "LV2|libgx_flanger.lv2.so|0";
        FavStore.toggle(favs, ladspa);
        assertTrue(FavStore.isFav(favs, ladspa));
        assertFalse("favouriting one flanger must not favourite the other",
                FavStore.isFav(favs, lv2));
    }

    @Test public void routingBlocksCanFavourite() {
        Set<String> favs = new LinkedHashSet<>();
        assertTrue(FavStore.toggle(favs, FavStore.SPLIT_KEY));
        assertTrue(FavStore.toggle(favs, FavStore.MERGE_KEY));
        assertTrue(FavStore.isFav(favs, FavStore.SPLIT_KEY));
        assertTrue(FavStore.isFav(favs, FavStore.MERGE_KEY));
    }

    @Test public void nullSafe() {
        assertFalse(FavStore.isFav(new LinkedHashSet<>(), null));
        assertFalse(FavStore.toggle(new LinkedHashSet<>(), null));
    }
}
