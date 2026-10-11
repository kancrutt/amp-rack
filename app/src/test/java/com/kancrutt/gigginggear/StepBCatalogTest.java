package com.kancrutt.gigginggear;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/** Step B host tests: identity keys, one category source, single blacklist
 *  gate, golden-table equality, preset key round-trip. */
public class StepBCatalogTest {

    private static final Set<String> CANONICAL = new HashSet<>(Arrays.asList(
            "AMP", "CABINET", "DISTORTION", "MODULATION", "DELAY", "REVERB",
            "DYNAMICS", "GATE", "EQ", "PITCH", "WAH", "UTILITY"));

    // ---------- B2: one category source, nothing UNKNOWN reachable ----------

    @Test public void everyEntryHasOneCanonicalCategory() {
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            assertTrue("non-canonical category at " + PluginCatalogData.KEY[i]
                            + ": " + PluginCatalogData.CATEGORY[i],
                    CANONICAL.contains(PluginCatalogData.CATEGORY[i]));
        }
    }

    @Test public void noUnknownReachableForAnyAlias() {
        // names/aliases resolve through the category table; keys resolve
        // through the registry (getCategory is name-only by design)
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            for (String alias : PluginCatalog.aliasesOf(i)) {
                CategoryHelper.Category c = CategoryHelper.getCategory(alias);
                assertNotNull("null category for " + alias, c);
                assertTrue("UNKNOWN reachable for " + alias + " -> " + c,
                        CANONICAL.contains(c.name()));
            }
            int j = PluginCatalog.indexOfKey(PluginCatalogData.KEY[i]);
            assertTrue("key not resolvable: " + PluginCatalogData.KEY[i], j >= 0);
            assertTrue("non-canonical category via key " + PluginCatalogData.KEY[i],
                    CANONICAL.contains(PluginCatalog.categoryOf(j)));
        }
    }

    @Test public void unknownNameLandsInOtherAndStaysVisible() {
        assertEquals(CategoryHelper.Category.UNKNOWN, CategoryHelper.getCategory("zzz-no-such-plugin"));
        // browsing Other must not hide anything: entry still classifiable
        assertNotNull(CategoryHelper.getCategory("GxEpic"));
    }

    // ---------- B1: stable identity ----------

    @Test public void noTwoEntriesShareAStableKey() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            assertTrue("duplicate key " + PluginCatalogData.KEY[i],
                    keys.add(PluginCatalogData.KEY[i]));
        }
    }

    @Test public void flangerCollisionIsDisambiguatedByKeyAndNameAmbiguityFails() {
        // LADSPA "Flanger" and LV2 "Gx_flanger" share norm key "flanger"
        int ladspaFlanger = -1, lv2Flanger = -1;
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            if (PluginCatalogData.KEY[i].startsWith("LADSPA|") && "Flanger".equals(PluginCatalogData.NAME[i]))
                ladspaFlanger = i;
            if (PluginCatalogData.KEY[i].startsWith("LV2|") && "Gx_flanger".equals(PluginCatalogData.NAME[i]))
                lv2Flanger = i;
        }
        assertTrue("LADSPA Flanger missing", ladspaFlanger >= 0);
        assertTrue("LV2 Gx_flanger missing", lv2Flanger >= 0);
        // distinct stable keys
        assertNotEquals(PluginCatalogData.KEY[ladspaFlanger], PluginCatalogData.KEY[lv2Flanger]);
        // each display name resolves uniquely to its own key (no cross-source
        // silent pick); the old norm-key world had both under "flanger"
        assertEquals(ladspaFlanger, PluginCatalog.resolveName("Flanger"));
        assertEquals(lv2Flanger, PluginCatalog.resolveName("Gx_flanger"));
        assertEquals("AUDIT", 1, PluginCatalogData.indicesForName("Gx_flanger").length);
    }

    @Test public void addByKeyParsesAndResolves() {
        int i = 0;
        String key = PluginCatalogData.KEY[i];
        assertNotNull(PluginKey.parse(key));
        assertEquals(i, PluginCatalog.indexOfKey(key));
        assertNull(PluginKey.parse("nonsense"));
        assertNull(PluginKey.parse("BAD|lib.so|0"));
    }

    // ---------- B3: one blacklist, gate everywhere ----------

    @Test public void blacklistedKeyInvisibleEverywhere() throws Exception {
        File asset = new File("src/main/assets/blacklist.json");
        assertTrue(asset.exists());
        String json = new String(Files.readAllBytes(asset.toPath()), StandardCharsets.UTF_8);
        Blacklist.loadFromJson(json);
        org.json.JSONObject root = new org.json.JSONObject(json);
        org.json.JSONArray keys = root.optJSONArray("keys");
        assertNotNull("blacklist must be key-based", keys);
        assertTrue("blacklist must not be empty", keys.length() > 0);
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.optString(i, "");
            int ri = PluginCatalog.indexOfKey(key);
            assertTrue("blacklisted key not in catalog: " + key, ri >= 0);
            assertFalse("blacklisted key visible in browse: " + key, PluginCatalog.isVisible(ri));
            assertTrue("blacklisted key not caught by Blacklist", Blacklist.isKeyBlacklisted(key));
            String name = PluginCatalog.nameOf(ri);
            assertTrue("name browse must hide it: " + name, Blacklist.isNameBlacklisted(name));
            // searching by display name or by its category must not surface it
            for (String q : new String[]{name.toLowerCase(), name, PluginCatalogData.CATEGORY[ri]}) {
                assertFalse("blacklisted '" + name + "' surfaced for query '" + q + "'",
                        searchHits(q).contains(key));
            }
        }
    }

    /** Simulated search surface: gated catalog scan (what every picker loop must do). */
    private static Set<String> searchHits(String q) {
        Set<String> hits = new HashSet<>();
        String qn = (q == null ? "" : q).toLowerCase().replaceAll("[^a-z0-9]", "");
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            if (!PluginCatalog.isVisible(i)) continue;
            String name = PluginCatalogData.NAME[i].toLowerCase().replaceAll("[^a-z0-9]", "");
            String cat = PluginCatalogData.CATEGORY[i].toLowerCase().replaceAll("[^a-z0-9]", "");
            if (name.contains(qn) || cat.contains(qn)) hits.add(PluginCatalogData.KEY[i]);
        }
        return hits;
    }

    @Test public void blacklistedLibRefusedByGuardApi() {
        // Engine.cpp calls MainActivity.isBlacklistedLib -> Blacklist.isLibBlacklisted
        Blacklist.clearForTesting();
        Blacklist.loadFromJson("{\"libs\": [\"libbad.so\"], \"keys\": []}");
        assertTrue(Blacklist.isLibBlacklisted("libbad.so"));
        assertFalse(Blacklist.isLibBlacklisted("libfine.so"));
        Blacklist.clearForTesting();
    }

    @Test public void octoloNotBlacklistedAfterA1() throws Exception {
        String json = new String(Files.readAllBytes(
                new File("src/main/assets/blacklist.json").toPath()), StandardCharsets.UTF_8);
        Blacklist.loadFromJson(json);
        assertFalse("A1 verdict USABLE: octolo must not be blacklisted",
                Blacklist.isNameBlacklisted("the infamous octolo"));
        int ri = PluginCatalog.resolveName("the infamous octolo");
        assertTrue("octolo must stay curated", ri >= 0 && PluginCatalogData.CURATED[ri]);
        Blacklist.clearForTesting();
    }

    // ---------- B4/B5: gate counts + golden table ----------

    @Test public void visibleCountEqualsCuratedMinusBlacklistMinusSynth() throws Exception {
        Blacklist.loadFromJson(new String(Files.readAllBytes(
                new File("src/main/assets/blacklist.json").toPath()), StandardCharsets.UTF_8));
        int curated = 0, synth = 0, bl_curated = 0, syn_curated = 0, visible = 0;
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            if (PluginCatalogData.CURATED[i]) curated++;
            boolean bl = Blacklist.isKeyBlacklisted(PluginCatalogData.KEY[i])
                    || Blacklist.isLibBlacklisted(PluginCatalogData.LIB[i]);
            if (PluginCatalogData.SYNTH[i] && PluginCatalogData.CURATED[i]) syn_curated++;
            if (bl && PluginCatalogData.CURATED[i]) bl_curated++;
            if (PluginCatalog.isVisible(i)) visible++;
        }
        assertEquals("visible must equal curated - blacklisted(curated) - synth(curated)",
                curated - bl_curated - syn_curated, visible);
        Blacklist.clearForTesting();
    }

    @Test public void goldenTableEqualsApprovedList() throws Exception {
        File golden = new File("../WIP/audit_r24/approved_categories.json");
        assertTrue("approved golden file missing at " + golden.getAbsolutePath(), golden.exists());
        String json = new String(Files.readAllBytes(golden.toPath()), StandardCharsets.UTF_8);
        org.json.JSONObject root = new org.json.JSONObject(json);
        org.json.JSONObject entries = root.getJSONObject("entries");
        assertEquals(PluginCatalogData.COUNT, entries.length());
        for (int i = 0; i < PluginCatalogData.COUNT; i++) {
            org.json.JSONObject e = entries.getJSONObject(PluginCatalogData.KEY[i]);
            assertEquals("category drift for " + PluginCatalogData.KEY[i],
                    e.getString("cat"), PluginCatalogData.CATEGORY[i]);
        }
    }

    // ---------- B1: preset round-trip with stable key ----------

    @Test public void presetRoundTripKeepsKey() throws Exception {
        LocalPresetManager.Preset p = new LocalPresetManager.Preset();
        p.name = "step-b test";
        LocalPresetManager.PluginItem item = new LocalPresetManager.PluginItem();
        item.name = "GxEpic";
        item.key = PluginCatalogData.KEY[0];
        item.uniqueId = 0;
        item.enabled = true;
        item.controls = new java.util.ArrayList<>();
        p.plugins.add(item);

        org.json.JSONObject json = LocalPresetManager.presetToJson(p);
        // serializer -> schema parser (public static), no file I/O needed
        LocalPresetManager.Preset q = LocalPresetManager.jsonToPreset(json);
        assertEquals(1, q.plugins.size());
        assertEquals(PluginCatalogData.KEY[0], q.plugins.get(0).key);
        assertEquals("GxEpic", q.plugins.get(0).name);
    }
}
