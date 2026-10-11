package com.kancrutt.gigginggear.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ControlLabelsTest {

    @Test public void chorusITimeKnob() {
        assertEquals("Time", ControlLabels.display("C* ChorusI", "T"));
        assertEquals("Time", ControlLabels.display("C* Chorus I", "t"));
    }

    @Test public void genericTUnchanged() {
        // Bare "T" means Type/Tilt/Tone on other plugins — never rewritten.
        assertEquals("T", ControlLabels.display("Some Amp", "T"));
        assertEquals("T", ControlLabels.display(null, "T"));
    }

    @Test public void nullAndEmptySafe() {
        assertNull(ControlLabels.display("X", null));
        assertEquals("", ControlLabels.display("X", ""));
        assertEquals("Width", ControlLabels.display("C* ChorusI", "Width"));
    }

    @Test public void typoMapAbbreviations() {
        // R19 TEMUAN #5: GxUltraCab "MIDS" no longer shreds into "Mid S".
        assertEquals("Mid", LabelFormatter.formatControlLabel("MIDS"));
        assertEquals("Overdrive", LabelFormatter.formatControlLabel("Over"));
        assertEquals("Overdrive 2", LabelFormatter.formatControlLabel("over2"));
        assertEquals("Cutoff Freq", LabelFormatter.formatControlLabel("Fc"));
        // T8 (r20): short labels expanded.
        assertEquals("Alt Voicing", LabelFormatter.formatControlLabel("alt"));
        assertEquals("Damping", LabelFormatter.formatControlLabel("damp"));
        assertEquals("Damping", LabelFormatter.formatControlLabel("DAMP"));
    }

    @Test public void ultraCabTopEnd() {
        // T8 (r20): GxUltraCab "TOP" -> "Top End"; other plugins keep bare "Top".
        assertEquals("Top End", ControlLabels.display("GxUltraCab", "Top"));
        assertEquals("Top", ControlLabels.display("Some Amp", "Top"));
    }
}
