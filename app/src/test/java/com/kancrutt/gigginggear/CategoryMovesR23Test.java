package com.kancrutt.gigginggear;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** R23 (Bang Hery): x42 LV2 convolution moved CABINET -> REVERB
 *  (was showing the Cab icon in the picker; an IR convolver is a reverb
 *  tool here). R24 revision (Bang Hery): convoLV2 moved back REVERB ->
 *  CABINET — it is the go-to cab-IR loader in this rig; zconvo stays
 *  REVERB. Step B: classification is table-only — the runtime token
 *  classifier is gone; unknown name forms land in the internal UNKNOWN
 *  fallback (R1), never a guessed category and never an "Other" chip. */
public class CategoryMovesR23Test {

    @Test public void x42IrConvolverIsReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("x42 - IR Convolver Mono => Stereo"));
    }

    /** The engine-reported name (the bundle json's pluginName, what
     *  AudioEngine.getActivePluginName returns) is an alias of the convoLV2
     *  entry — canvas tiles for it render CABINET (R24), not unknown. */
    @Test public void engineNameResolvesViaAlias() {
        assertEquals(CategoryHelper.Category.CABINET,
                CategoryHelper.getCategory("x42 LV2 Convolution"));
    }

    /** No token heuristics at runtime: an unknown spelling is the internal
     *  UNKNOWN fallback — no guess, no "Other" chip (R1). */
    @Test public void unknownSpellingIsNotGuessed() {
        assertEquals(CategoryHelper.Category.UNKNOWN,
                CategoryHelper.getCategory("Convolution"));
    }

    @Test public void convoLv2IsCabinet() {
        assertEquals(CategoryHelper.Category.CABINET,
                CategoryHelper.getCategory("convoLV2"));
    }

    /** Genuine cab/IR loaders keep the Cab category through the approved table. */
    @Test public void cabLoadersStayCabinet() {
        for (String n : new String[]{"GxUltraCab", "Gx_cabsim_", "ImpulseLoader"}) {
            assertEquals("expected CABINET for " + n,
                    CategoryHelper.Category.CABINET,
                    CategoryHelper.getCategory(n));
        }
    }
}
