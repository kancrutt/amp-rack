package com.kancrutt.gigginggear;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** R23 (Bang Hery): x42 LV2 convolution moved CABINET -> REVERB
 *  (was showing the Cab icon in the picker; an IR convolver is a reverb
 *  tool here). convoLV2 (multi-tap convolver) stays REVERB too. */
public class CategoryMovesR23Test {

    @Test public void x42IrConvolverIsReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("x42 - IR Convolver Mono => Stereo"));
    }

    @Test public void convoLv2StaysReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("convoLV2"));
    }
}
