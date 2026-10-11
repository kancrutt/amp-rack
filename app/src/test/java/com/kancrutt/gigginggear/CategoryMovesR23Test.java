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

    /** The chain canvas classifies by the ENGINE name, not the catalog name.
     *  "IR Convolver..." / "x42 LV2 Convolution" fell through to the old
     *  CABINET token rule ("convolver") — that is why the canvas tile kept the
     *  Cab icon after the picker card was fixed. */
    @Test public void convolverTokenIsReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("IR Convolver Mono => Stereo"));
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("x42 LV2 Convolution"));
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("Convolution"));
    }

    @Test public void convoLv2StaysReverb() {
        assertEquals(CategoryHelper.Category.REVERB,
                CategoryHelper.getCategory("convoLV2"));
    }

    /** Genuine cab/IR loaders must keep the Cab icon: cab/cabinet/impulse
     *  tokens still map CABINET. */
    @Test public void cabLoadersStayCabinet() {
        assertEquals(CategoryHelper.Category.CABINET,
                CategoryHelper.getCategory("Impulse response loader"));
        assertEquals(CategoryHelper.Category.CABINET,
                CategoryHelper.getCategory("Speaker Cabinet"));
        assertEquals(CategoryHelper.Category.CABINET,
                CategoryHelper.getCategory("Impulse Response"));
    }
}
