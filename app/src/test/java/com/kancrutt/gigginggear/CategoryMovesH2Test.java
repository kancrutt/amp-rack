package com.kancrutt.gigginggear;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** H2 (Bang Hery): requested category moves, surgically patched into
 *  CategoryGenerated + CategoryHelper OVERRIDES + gen_category_map.py. */
public class CategoryMovesH2Test {

    @Test public void h2Moves() {
        // #1 gcb95: DISTORTION -> WAH (GCB-95 = wah pedal).
        assertEquals(CategoryHelper.Category.WAH,
                CategoryHelper.getCategory("Gx_gcb_95_"));
        // #2 ts9sim: keep DISTORTION (no change).
        assertEquals(CategoryHelper.Category.DISTORTION,
                CategoryHelper.getCategory("Gxts9sim"));
        // #3/#4 tape family: DISTORTION -> DELAY.
        assertEquals(CategoryHelper.Category.DELAY,
                CategoryHelper.getCategory("GxTape"));
        assertEquals(CategoryHelper.Category.DELAY,
                CategoryHelper.getCategory("Gx Tape Stereo"));
        // #5/#6 LADSPA amps: UTILITY -> AMP.
        assertEquals(CategoryHelper.Category.AMP,
                CategoryHelper.getCategory("Simple amplifier"));
        assertEquals(CategoryHelper.Category.AMP,
                CategoryHelper.getCategory("mydsp"));
    }
}
