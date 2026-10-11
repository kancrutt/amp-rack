package com.kancrutt.gigginggear;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** R23 (Bang Hery): x42 LV2 convolution moved CABINET -> REVERB
 *  (was showing the Cab icon in the picker; an IR convolver is a reverb
 *  tool here). convoLV2 (multi-tap convolver) stays REVERB too.
 *  Step B: classification is table-only — the runtime token classifier is
 *  gone, unknown name forms land in OTHER (visible) instead of guessed. */
public class CategoryMovesR23Test {

    @Test public void x42IrConvolverIsReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("x42 - IR Convolver Mono => Stereo"));
    }

    /** No token heuristics at runtime: an unknown spelling is OTHER, not a guess. */
    @Test public void unknownSpellingIsOtherNotGuessed() {
        assertEquals(CategoryHelper.Category.OTHER,
                CategoryHelper.getCategory("x42 LV2 Convolution"));
        assertEquals(CategoryHelper.Category.OTHER,
                CategoryHelper.getCategory("Convolution"));
    }

    @Test public void convoLv2StaysReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("convoLV2"));
    }

    /** Genuine cab/IR loaders keep the Cab category through the approved table. */
    @Test public void cabLoadersStayCabinet() {
        // (only real catalog entries; "Convolution Reverb" is a loader shim row,
        // not a catalog entry — Step B: shims classify OTHER)
        for (String n : new String[]{"GxUltraCab", "Gx_cabsim_"}) {
            assertEquals("expected CABINET for " + n,
                    CategoryHelper.Category.CABINET,
                    CategoryHelper.getCategory(n));
        }
    }
}
