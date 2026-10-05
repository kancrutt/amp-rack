package com.kancrutt.gigginggear.metadata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParameterFormatterContractTest {
    private static ParameterMetadata.PortMeta meta(String unit, float min, float max) {
        return new ParameterMetadata.PortMeta("p", "p", 0, min, min, max, unit,
                ParameterMetadata.ControlType.KNOB, ParameterMetadata.PluginType.FLOAT,
                false, false, false, null, null);
    }

    private static ParameterMetadata.PortMeta named(String name, String unit, float min, float max) {
        return new ParameterMetadata.PortMeta(name, name, 0, min, min, max, unit,
                ParameterMetadata.ControlType.KNOB, ParameterMetadata.PluginType.FLOAT,
                false, false, false, null, null);
    }

    @Test public void envelopeNamesAreNotPercentOnUnitlessZeroOne() {
        // P0-2: unitless 0..1 envelope/dynamics ports must not render as %.
        ParameterMetadata.PortMeta attack = named("Attack", "", 0, 1);
        assertEquals(false, ParameterFormatter.isPercentScaled(attack));
        assertEquals(false, ParameterFormatter.isPercentScaled("Attack", "", 0, 1));
        assertEquals(false, ParameterFormatter.isPercentScaled("threshold", "", 0, 1));
        assertEquals(false, ParameterFormatter.isPercentScaled("Release Time", "", 0, 1));
        assertEquals(false, ParameterFormatter.isPercentScaled("Time Tracking", "", 0, 1));
        assertEquals(false, ParameterFormatter.isPercentScaled("HOLD", "", 0, 1));
        assertEquals(false, ParameterFormatter.isPercentScaled("Rate", "", 0, 1));
        // Raw normalized display: no % suffix, round-trip stays in 0..1.
        assertEquals("1", ParameterFormatter.rawToDisplay(0.75f, attack).text);
        assertEquals("0", ParameterFormatter.rawToDisplay(0.25f, attack).text);
        assertEquals(0.75f, ParameterFormatter.displayToRaw(0.75f, attack), 0.0001f);
        assertEquals("Min: 0  Max: 1", ParameterFormatter.getRangeString(attack));
        // Manual dialog and knob readout must agree (same decision function).
        assertEquals(false, ParameterFormatter.isPercentScaled("Attack", "", 0, 1));
    }

    @Test public void semanticPerentsAndTenScaleSurviveP02() {
        // User directive: Feedback/Mix/Wet/Depth keep percent on 0..1.
        assertEquals(true, ParameterFormatter.isPercentScaled("Feedback", "", 0, 1));
        assertEquals(true, ParameterFormatter.isPercentScaled("Mix", "", 0, 1));
        assertEquals(true, ParameterFormatter.isPercentScaled("Wet", "", 0, 1));
        assertEquals(true, ParameterFormatter.isPercentScaled("Dry", "", 0, 1));
        assertEquals(true, ParameterFormatter.isPercentScaled(named("Depth", "", 0, 1)));
        assertEquals("75%", ParameterFormatter.rawToDisplay(0.75f, named("Feedback", "", 0, 1)).text);
        // Gain/drive family keeps the 0..10 face value.
        ParameterMetadata.PortMeta drive = named("Drive", "", 0, 1);
        assertEquals(true, ParameterFormatter.isTenScale(drive));
        assertEquals("5", ParameterFormatter.rawToDisplay(0.5f, drive).text);
        // A DECLARED "%" unit always wins over the name exclusion.
        assertEquals(true, ParameterFormatter.isPercentScaled("Attack", "%", 0, 1));
        assertEquals("50%", ParameterFormatter.rawToDisplay(0.5f, named("Attack", "%", 0, 1)).text);
    }

    @Test public void rawDbAndFrequencyRemainRaw() {
        assertEquals("dB", ParameterFormatter.canonicalUnit("d B"));
        assertEquals("-60dB", ParameterFormatter.rawToDisplay(-60f, meta("dB", -60, 6)).text);
        assertEquals("1dB", ParameterFormatter.rawToDisplay(1.001f, meta("dB", -60, 6)).text);
        assertEquals("20000Hz", ParameterFormatter.rawToDisplay(20000f, meta("Hz", 20, 20000)).text);
    }

    @Test public void rawTimeRemainsRaw() {
        assertEquals("250ms", ParameterFormatter.rawToDisplay(250f, meta("ms", 0, 1000)).text);
    }

    @Test public void percentZeroOneScalesToHundred() {
        // Genuine % with a 0..1 range displays scaled (raw 0.5 -> "50%").
        assertEquals("50%", ParameterFormatter.rawToDisplay(0.5f, meta("%", 0, 1)).text);
        assertEquals(0.5f, ParameterFormatter.displayToRaw(50f, meta("%", 0, 1)), 0.0001f);
        // Non-0..1 percent ranges stay raw (raw IS the percentage).
        assertEquals("50%", ParameterFormatter.rawToDisplay(50f, meta("%", 0, 100)).text);
        assertEquals(50f, ParameterFormatter.displayToRaw(50f, meta("%", 0, 100)), 0.0001f);
    }

    @Test public void integerStepperNeverCrashesFormat() {
        // Device crash (09-05): %.0f received Math.round's int -> IllegalFormatConversionException.
        ParameterMetadata.PortMeta m = new ParameterMetadata.PortMeta("p", "p", 0, 0, 0, 3, "",
                ParameterMetadata.ControlType.INTEGER_STEPPER, ParameterMetadata.PluginType.INT,
                false, true, false, null, null);
        assertEquals("2", ParameterFormatter.rawToDisplay(2f, m).text);
        assertEquals("0", ParameterFormatter.rawToDisplay(0.2f, m).text);
        assertEquals("3", ParameterFormatter.rawToDisplay(2.7f, m).text);
    }

    @Test public void percentAliasesNormalize() {
        assertEquals("%", ParameterFormatter.canonicalUnit("pc"));
        assertEquals("%", ParameterFormatter.canonicalUnit("pct"));
        assertEquals("%", ParameterFormatter.canonicalUnit("percent"));
        assertEquals("%", ParameterFormatter.canonicalUnit(" % "));
        assertEquals("%", ParameterFormatter.canonicalUnit("PC"));
        assertEquals("", ParameterFormatter.canonicalUnit(""));
        assertEquals("", ParameterFormatter.canonicalUnit(null));
        assertEquals("", ParameterFormatter.canonicalUnit("   "));
    }

    @Test public void emptyMetadataFallsBackToNative() {
        assertEquals("%", ParameterFormatter.resolveUnit("", "pc"));
        assertEquals("%", ParameterFormatter.resolveUnit(null, "pc"));
        assertEquals("%", ParameterFormatter.resolveUnit("", "%"));
        assertEquals("dB", ParameterFormatter.resolveUnit("", "db"));
        // Metadata wins when non-empty.
        assertEquals("dB", ParameterFormatter.resolveUnit("dB", "pc"));
        assertEquals("%", ParameterFormatter.resolveUnit("pc", "db"));
        // Both empty stays unitless.
        assertEquals("", ParameterFormatter.resolveUnit("", ""));
        assertEquals("", ParameterFormatter.resolveUnit(null, null));
    }

    @Test public void nativePcYieldsPercent() {
        assertEquals("%", ParameterFormatter.canonicalNativeUnit("pc"));
        assertEquals("%", ParameterFormatter.canonicalNativeUnit("pct"));
        assertEquals("%", ParameterFormatter.canonicalNativeUnit("percent"));
        assertEquals("%", ParameterFormatter.canonicalNativeUnit("%"));
    }

    @Test public void unitlessZeroOneDefaultsToPercent() {
        assertEquals("50%", ParameterFormatter.rawToDisplay(0.5f, meta("", 0, 1)).text);
        assertEquals(false, ParameterFormatter.isPercentScaled("", 0, 1));
        assertEquals(true, ParameterFormatter.isPercentScaled(meta("", 0, 1)));
        assertEquals(true, ParameterFormatter.isPercentScaled("Feedback", "", 0, 1));
    }

    @Test public void unitlessZeroOneTenScaleProfile() {
        ParameterMetadata.PortMeta gain = new ParameterMetadata.PortMeta("Drive", "drive", 0, 0,
                0, 1, "", ParameterMetadata.ControlType.KNOB,
                ParameterMetadata.PluginType.FLOAT, false, false, false, null, null);
        assertEquals(true, ParameterFormatter.isTenScale(gain));
        assertEquals(false, ParameterFormatter.isPercentScaled(gain));
        assertEquals("5", ParameterFormatter.rawToDisplay(0.5f, gain).text);
        assertEquals("0", ParameterFormatter.rawToDisplay(0.0f, gain).text);
        assertEquals("10", ParameterFormatter.rawToDisplay(1.0f, gain).text);
        assertEquals(0.5f, ParameterFormatter.displayToRaw(5f, gain), 0.0001f);
        assertEquals("Min: 0  Max: 10", ParameterFormatter.getRangeString(gain));
    }

    @Test public void percentZeroOneDisplaysScaled() {
        assertEquals("0%", ParameterFormatter.rawToDisplay(0f, meta("%", 0, 1)).text);
        assertEquals("71%", ParameterFormatter.rawToDisplay(0.71f, meta("%", 0, 1)).text);
        assertEquals("100%", ParameterFormatter.rawToDisplay(1f, meta("%", 0, 1)).text);
        assertEquals(0.71f, ParameterFormatter.displayToRaw(71f, meta("%", 0, 1)), 0.0001f);
        assertEquals("Min: 0%  Max: 100%", ParameterFormatter.getRangeString(meta("%", 0, 1)));
    }

    @Test public void rangeStringKeepsUnit() {
        assertEquals("Min: -60dB  Max: 6dB", ParameterFormatter.getRangeString(meta("dB", -60, 6)));
        assertEquals("Min: 0%  Max: 100%", ParameterFormatter.getRangeString(meta("", 0, 1)));
    }

    @Test public void noQuestionMarkPlaceholder() {
        // Unknown units collapse to "" and never render "?".
        assertEquals("", ParameterFormatter.canonicalUnit("frobnicate"));
        assertEquals("50%", ParameterFormatter.rawToDisplay(0.5f, meta("frobnicate", 0, 1)).text);
        assertEquals("Min: 0%  Max: 100%", ParameterFormatter.getRangeString(meta("frobnicate", 0, 1)));
    }

    @Test public void toggleNeverBecomesPercent() {
        ParameterMetadata.PortMeta p = new ParameterMetadata.PortMeta("toggle", "toggle", 0, 0,
                0, 1, "%", ParameterMetadata.ControlType.TOGGLE,
                ParameterMetadata.PluginType.BOOLEAN, false, false, true, null, null);
        assertEquals("OFF", ParameterFormatter.rawToDisplay(0f, p).text);
        assertEquals("ON", ParameterFormatter.rawToDisplay(1f, p).text);
        assertEquals(false, ParameterFormatter.isPercentScaled(p));
    }

    @Test public void explicitEnumValuesAreNotIndexed() {
        ParameterMetadata.PortMeta p = new ParameterMetadata.PortMeta("mode", "mode", 0, 10,
                10, 30, "", ParameterMetadata.ControlType.ENUM_PICKER,
                ParameterMetadata.PluginType.ENUM, false, false, false,
                new float[]{10f, 20f, 30f}, new String[]{"A", "B", "C"});
        assertEquals("B", ParameterFormatter.rawToDisplay(20f, p).text);
        assertEquals(30f, ParameterFormatter.labelToRaw("C", p), 0.0001f);
    }

    @Test public void toggleDisplaysStateAndUsesRawZeroOne() {
        ParameterMetadata.PortMeta p = new ParameterMetadata.PortMeta("toggle", "toggle", 0, 0,
                0, 1, "", ParameterMetadata.ControlType.TOGGLE,
                ParameterMetadata.PluginType.BOOLEAN, false, false, true, null, null);
        assertEquals("OFF", ParameterFormatter.rawToDisplay(0f, p).text);
        assertEquals("ON", ParameterFormatter.rawToDisplay(1f, p).text);
        assertEquals(1f, ParameterFormatter.displayToRaw(1f, p), 0.0001f);
    }

    @Test public void unknownUnitHasNoUserFacingPlaceholder() {
        assertEquals("0%", ParameterFormatter.rawToDisplay(0f, meta("?", 0, 1)).text);
        assertEquals("0%", ParameterFormatter.rawToDisplay(0f, meta("", 0, 1)).text);
    }

    @Test public void continuousUnitlessFloatScaledToIntegerPercent() {
        assertEquals("25%", ParameterFormatter.rawToDisplay(0.25f, meta("", 0, 1)).text);
        assertEquals("50%", ParameterFormatter.rawToDisplay(0.5f, meta("", 0, 1)).text);
        assertEquals("75%", ParameterFormatter.rawToDisplay(0.75f, meta("", 0, 1)).text);
    }

    @Test public void microAmpBypassContractIsToggle() {
        ParameterMetadata.PortMeta bypass = new ParameterMetadata.PortMeta("BYPASS", "BYPASS", 2, 1,
                0, 1, "", ParameterMetadata.ControlType.TOGGLE,
                ParameterMetadata.PluginType.BOOLEAN, false, false, true, null, null);
        ParameterMetadata.PortMeta gain = new ParameterMetadata.PortMeta("GAIN", "GAIN", 3, 0,
                0, 1, "", ParameterMetadata.ControlType.KNOB,
                ParameterMetadata.PluginType.FLOAT, false, false, false, null, null);
        assertEquals(ParameterMetadata.ControlType.TOGGLE, bypass.controlType);
        assertEquals(0f, bypass.min, 0.0001f);
        assertEquals(1f, bypass.max, 0.0001f);
        assertEquals("OFF", ParameterFormatter.rawToDisplay(0f, bypass).text);
        assertEquals("ON", ParameterFormatter.rawToDisplay(1f, bypass).text);
        assertEquals("0", ParameterFormatter.rawToDisplay(0f, gain).text);
    }

    @Test public void sampleRateBlankUnitRendersSlashSr() {
        // 3.2-C: a sample-rate-relative port (blank unit, 0..1) renders "/sr".
        ParameterMetadata.PortMeta srPort = new ParameterMetadata.PortMeta("dly", "dly", 0, 0.42f,
                0, 1, "", ParameterMetadata.ControlType.KNOB,
                ParameterMetadata.PluginType.FLOAT, false, false, false, true,
                null, null);
        assertEquals("0/sr", ParameterFormatter.rawToDisplay(0f, srPort).text);
        assertEquals("1/sr", ParameterFormatter.rawToDisplay(1f, srPort).text);
    }
}
