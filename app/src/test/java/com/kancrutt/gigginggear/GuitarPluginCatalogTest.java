package com.kancrutt.gigginggear;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuitarPluginCatalogTest {

    private static void loadBundledCatalog() throws Exception {
        GuitarPluginCatalog.clearForTesting();
        File asset = new File("src/main/assets/curated_plugins.json");
        assertTrue("catalog asset missing at " + asset.getAbsolutePath(), asset.exists());
        String json = new String(Files.readAllBytes(asset.toPath()), "UTF-8");
        GuitarPluginCatalog.loadFromJson(json);
        GuitarPluginCatalog.loadFromJson(json); // idempotent
    }

    @Test public void guitarPluginsAreCurated() throws Exception {
        loadBundledCatalog();
        // Phase-2 additions that the legacy name-token gate missed.
        assertTrue(GuitarPluginCatalog.isCurated("the infamous octolo"));
        // Round-5 dedup: the bare "octolo" duplicate (lv2_plugins key 24200,
        // identical .so/index) was removed from the curated names list —
        // exactly ONE picker tile may remain for that library.
        assertFalse(GuitarPluginCatalog.isCurated("octolo"));
        // Hery r21 dedup: bare "hip2b"/"lushlife" aliases (lv2_plugins keys
        // 23700/23900, identical .so/index) removed like octolo — exactly ONE
        // picker tile per library.
        assertFalse(GuitarPluginCatalog.isCurated("hip2b"));
        assertFalse(GuitarPluginCatalog.isCurated("lushlife"));
        // Hery dedup: "C* SpiceX2 - Not an exciter either" (all_plugins key
        // 10302, libSaturate.so stereo variant) removed like octolo — the
        // mono "C* Spice - Not an exciter" is the single tile (mono chain).
        assertFalse(GuitarPluginCatalog.isCurated("c* spicex2 - not an exciter either"));
        assertTrue(GuitarPluginCatalog.isCurated("C* Spice - Not an exciter"));
        assertTrue(GuitarPluginCatalog.isCurated("the infamous Hip2B"));
        assertTrue(GuitarPluginCatalog.isCurated("the infamous ewham"));
        assertTrue(GuitarPluginCatalog.isCurated("the infamous mindi"));
        assertTrue(GuitarPluginCatalog.isCurated("bluesbreaker"));
        assertTrue(GuitarPluginCatalog.isCurated("Harmonic Exciter"));
        assertTrue(GuitarPluginCatalog.isCurated("MDA Bandisto"));
        assertTrue(GuitarPluginCatalog.isCurated("MDA ThruZero"));
        // Hery: Tamg Amp removed entirely (lv2_plugins key 34900,
        // libtamgamp.so entry deleted) — must not be curated.
        assertFalse(GuitarPluginCatalog.isCurated("Tamg Amp"));
        assertTrue(GuitarPluginCatalog.isCurated("convoLV2"));
        // Case-insensitive.
        assertTrue(GuitarPluginCatalog.isCurated("THE INFAMOUS OCTOLO"));
    }

    @Test public void nonGuitarPluginsStayBlacklisted() throws Exception {
        loadBundledCatalog();
        // Lab DSP / MIDI-only / mastering.
        assertFalse(GuitarPluginCatalog.isCurated("MDA TestTone"));
        assertFalse(GuitarPluginCatalog.isCurated("MDA TalkBox"));
        assertFalse(GuitarPluginCatalog.isCurated("Vocoder"));
        assertFalse(GuitarPluginCatalog.isCurated("z-1"));
        assertFalse(GuitarPluginCatalog.isCurated("TAP DeEsser"));
        assertFalse(GuitarPluginCatalog.isCurated("Barry's Satan Maximiser"));
        // NAM single-loader policy.
        assertFalse(GuitarPluginCatalog.isCurated("Neural Amp Modeler"));
        assertFalse(GuitarPluginCatalog.isCurated("AIDA-X"));
        assertTrue(GuitarPluginCatalog.isCurated("Neural Amp Modeler NG"));
        // IR-cab policy: exactly one (x42 zconvo). Simple IR Loader and irdsp
        // rejected (crashy/unverifiable); also removed from the curated list.
        assertFalse(GuitarPluginCatalog.isCurated("Impulse Loader"));
        assertFalse(GuitarPluginCatalog.isCurated("SWH Impulse convolver"));
        assertFalse(GuitarPluginCatalog.isCurated("rkr Reverbtron"));
        assertFalse(GuitarPluginCatalog.isCurated("x42 DARC"));
        assertFalse(GuitarPluginCatalog.isCurated("Simple IR Loader"));
        assertFalse(GuitarPluginCatalog.isCurated("irdsp"));
        assertTrue(GuitarPluginCatalog.isCurated("x42 - IR Convolver Mono => Stereo"));
        // Reverb-IR policy: convoLV2 is the sole reverb-IR convolver.
        assertTrue(GuitarPluginCatalog.isCurated("convoLV2"));
        // Empty input.
        assertFalse(GuitarPluginCatalog.isCurated(null));
        assertFalse(GuitarPluginCatalog.isCurated(""));
    }

    @Test public void blacklistOverridesCurated() throws Exception {
        loadBundledCatalog();
        // Bundled blacklist hides Simple IR Loader (crashy ir-cab-sim bundle);
        // octolo stays curated because it is not on the blacklist.
        File bl = new File("src/main/assets/blacklist.json");
        assertTrue("blacklist asset missing at " + bl.getAbsolutePath(), bl.exists());
        GuitarPluginCatalog.loadBlacklistFromJson(
                new String(Files.readAllBytes(bl.toPath()), "UTF-8"));
        // Round-5 dedup: bare "octolo" left the curated names list entirely;
        // only "the infamous octolo" remains curated.
        assertFalse(GuitarPluginCatalog.isCurated("octolo"));
        assertTrue(GuitarPluginCatalog.isCurated("the infamous octolo"));
        // Bundled blacklist hides the crashy ir-cab-sim bundle and mindi
        // (MIDI-only, missing mindi.json manifest asset).
        assertFalse(GuitarPluginCatalog.isCurated("Simple IR Loader"));
        assertFalse(GuitarPluginCatalog.isCurated("the infamous mindi"));

        // A non-empty blacklist hides even curated names, case-insensitively;
        // entries with value 0 are ignored.
        GuitarPluginCatalog.loadBlacklistFromJson(
                "{\"octolo\": 1, \"The Infamous Octolo\": 1, \"disabled\": 0}");
        assertFalse(GuitarPluginCatalog.isCurated("octolo"));
        assertFalse(GuitarPluginCatalog.isCurated("THE INFAMOUS OCTOLO"));
        assertTrue(GuitarPluginCatalog.isCurated("bluesbreaker"));
        GuitarPluginCatalog.clearForTesting();
    }

    @Test public void legacyFallbackWhenAssetMissing() {
        GuitarPluginCatalog.clearForTesting();
        // Without the asset the legacy name-token heuristics must keep the
        // catalog alive (no empty browse list).
        assertTrue(GuitarPluginCatalog.isCurated("C* Compress"));
        assertTrue(GuitarPluginCatalog.isCurated("TAP Echo"));
        assertTrue(GuitarPluginCatalog.isCurated("rkr T2O"));
        assertFalse(GuitarPluginCatalog.isCurated("Gong model"));
        assertFalse(GuitarPluginCatalog.isCurated("Neural Amp Modeler"));
        GuitarPluginCatalog.clearForTesting();
    }
}
