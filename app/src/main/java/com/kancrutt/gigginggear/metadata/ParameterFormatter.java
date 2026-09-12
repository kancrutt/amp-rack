package com.kancrutt.gigginggear.metadata;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Metadata-driven parameter value formatting.
 * Replaces heuristic-based detectControlUnit/getPercentScale/formatParamValue.
 * Raw <-> Display conversions are reversible and metadata-driven.
 */
public final class ParameterFormatter {
    private static final Locale LOCALE = Locale.US;

    public static final class DisplayValue {
        public final String text;
        public final float rawValue;

        public DisplayValue(String text, float rawValue) {
            this.text = text;
            this.rawValue = rawValue;
        }
    }

    private ParameterFormatter() {}

    /**
     * Convert raw DSP value to display value using metadata.
     * @param raw Raw parameter value from DSP
     * @param meta Parameter metadata
     * @return DisplayValue with formatted text and raw value
     */
    public static DisplayValue rawToDisplay(float raw, ParameterMetadata.PortMeta meta) {
        if (meta == null) {
            // Fallback: raw value with minimal formatting
            return new DisplayValue(formatRaw(raw), raw);
        }

        String unit = canonicalUnit(meta.unit);

        // Percent scaling ONLY when the resolved unit is "%" AND the range
        // is 0..1 (raw 0.71 -> "71%"). Unitless 0..1 params stay plain
        // numbers; other units never scale.

        String text;
        if (meta.controlType == ParameterMetadata.ControlType.TOGGLE) {
            text = (raw >= 0.5f) ? "ON" : "OFF";
        } else if (meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER && meta.hasEnum()
                && meta.enumLabels != null && meta.enumLabels.length == meta.enumValues.length) {
            // Find matching enum label
            int idx = findEnumIndex(raw, meta);
            String label = (idx >= 0 && idx < meta.enumLabels.length) ? meta.enumLabels[idx] : formatRaw(raw);
            text = label;
        } else if (meta.controlType == ParameterMetadata.ControlType.INTEGER_STEPPER) {
            // Math.round(float) returns int — never pass it to a %f conversion
            // (IllegalFormatConversionException crashed sliders on device).
            text = String.valueOf(Math.round(raw));
        } else if (isPercentScaled(meta)) {
            text = formatRaw(raw * 100f) + "%";
        } else {
            text = formatWithUnit(raw, unit);
        }
        return new DisplayValue(text, raw);
    }

    /**
     * Convert display value (from user input) back to raw DSP value.
     * @param display Display value entered by user
     * @param meta Parameter metadata
     * @return Raw DSP value, clamped to [min, max]
     */
    public static float displayToRaw(float display, ParameterMetadata.PortMeta meta) {
        if (meta == null) {
            return clamp(display, 0, 1);
        }

        float raw = display;
        // Percent round-trip: display 0..100 -> raw 0..1, but ONLY for
        // genuinely percent-scaled params (unit "%" with 0..1 range).
        // dB/Hz/ms display values always equal raw values.
        // The only special case is ENUM: user selects by label, we map to enum value
        if (isPercentScaled(meta)) {
            return clamp(display / 100f, meta.min, meta.max);
        }
        if (meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER && meta.hasEnum()) {
            // This shouldn't be called for enum - use enum value directly
        }
        return clamp(raw, meta.min, meta.max);
    }

    /**
     * Convert enum label to raw value.
     * @param label Selected enum label
     * @param meta Parameter metadata
     * @return Raw enum value, or clamped default if label not found
     */
    public static float labelToRaw(String label, ParameterMetadata.PortMeta meta) {
        if (meta == null || !meta.hasEnum() || meta.enumLabels == null
                || meta.enumLabels.length != meta.enumValues.length) return 0;
        for (int i = 0; i < meta.enumLabels.length; i++) {
            if (meta.enumLabels[i].equals(label)) {
                return meta.enumValues[i];
            }
        }
        // Label not found - return default or first enum value
        return meta.enumValues.length > 0 ? meta.enumValues[0] : meta.defaultValue;
    }

    private static int findEnumIndex(float raw, ParameterMetadata.PortMeta meta) {
        if (!meta.hasEnum()) return -1;
        float bestDiff = Float.MAX_VALUE;
        int bestIdx = 0;
        for (int i = 0; i < meta.enumValues.length; i++) {
            float diff = Math.abs(raw - meta.enumValues[i]);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String formatRaw(float v) {
        // Keep meaningful float precision for continuous controls. Toggle and
        // integer controls are formatted by their dedicated branches above.
        String text = String.format(Locale.US, "%.1f", v);
        while (text.contains(".") && text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    /** Public number formatting shared with the manual-input dialog. */
    public static String formatNumber(float v) {
        return formatRaw(v);
    }

    private static String formatWithUnit(float v, String unit) {
        if (unit == null || unit.isEmpty()) {
            return formatRaw(v);
        }

        // Keep the declared unit and raw numeric value together. Do not convert
        // Hz to kHz or seconds to milliseconds here: manual entry must round-trip.
        return formatRaw(v) + unit;
    }

    /** Normalize known unit tokens only; parameter labels are never changed. */
    public static String canonicalUnit(String unit) {
        if (unit == null) return "";
        String compact = unit.trim().replaceAll("\\s+", "");
        if (compact.isEmpty()) return "";
        if (compact.equalsIgnoreCase("db")) return "dB";
        if (compact.equalsIgnoreCase("khz")) return "kHz";
        if (compact.equalsIgnoreCase("hz")) return "Hz";
        if (compact.equalsIgnoreCase("ms")) return "ms";
        if (compact.equalsIgnoreCase("bpm") || compact.equalsIgnoreCase("beat")) return "BPM";
        if (compact.equalsIgnoreCase("midinote")) return "MIDI Note";
        if (compact.equalsIgnoreCase("semitones")) return "semitones";
        if (compact.equalsIgnoreCase("cent") || compact.equalsIgnoreCase("cents")) return "cents";
        if (compact.equalsIgnoreCase("%") || compact.equalsIgnoreCase("pc")
                || compact.equalsIgnoreCase("pct") || compact.equalsIgnoreCase("percent")) return "%";
        if (compact.equalsIgnoreCase("s")) return "s";
        if (compact.equalsIgnoreCase("m")) return "m";
        if (compact.equalsIgnoreCase("coef")) return "coef";
        if (compact.equalsIgnoreCase("st") || compact.equalsIgnoreCase("semitone")
                || compact.equalsIgnoreCase("semitones")) return "semitones";
        if (compact.equalsIgnoreCase("oct") || compact.equalsIgnoreCase("octave")
                || compact.equalsIgnoreCase("octaves")) return "oct";
        if (compact.equalsIgnoreCase("samples")) return "samples";
        return "";
    }

    /**
     * Normalize a native (DSP-reported) unit token. Mirrors the legacy
     * native chain so behavior is identical, but centralized and testable.
     */
    public static String canonicalNativeUnit(String nativeUnit) {
        if (nativeUnit == null) return "";
        String nu = nativeUnit.trim();
        if (nu.isEmpty()) return "";
        if (nu.equalsIgnoreCase("pc") || nu.equalsIgnoreCase("pct")
                || nu.equalsIgnoreCase("percent") || nu.equals("%")) return "%";
        if (nu.equalsIgnoreCase("db")) return "dB";
        if (nu.equalsIgnoreCase("khz")) return "kHz";
        if (nu.equalsIgnoreCase("hz")) return "Hz";
        if (nu.equalsIgnoreCase("ms")) return "ms";
        if (nu.equalsIgnoreCase("s")) return "s";
        if (nu.equalsIgnoreCase("bpm") || nu.equalsIgnoreCase("beat")) return "BPM";
        if (nu.equalsIgnoreCase("m")) return "m";
        if (nu.equalsIgnoreCase("coef")) return "";
        if (nu.equalsIgnoreCase("cent") || nu.equalsIgnoreCase("cents")) return "cents";
        if (nu.equalsIgnoreCase("st") || nu.toLowerCase(LOCALE).contains("semitone")) return "st";
        return canonicalUnit(nu);
    }

    /**
     * Resolve the final display unit: metadata wins when non-empty,
     * otherwise fall back to the native unit. An empty metadata unit must
     * never shadow a native unit such as "pc". Unitless stays "" — a 0..1
     * range alone never implies percent.
     */
    public static String resolveUnit(String metaUnit, String nativeUnit) {
        String m = canonicalUnit(metaUnit);
        if (m != null && !m.isEmpty()) return m;
        return canonicalNativeUnit(nativeUnit);
    }

    /**
     * True only for genuine percent params: resolved unit "%" with a 0..1
     * range. Display shows raw*100 with a "%" suffix.
     */
    public static boolean isPercentScaled(String unit, float min, float max) {
        if (!"%".equals(unit)) return false;
        return Math.abs(min) < 0.0001f && Math.abs(max - 1f) < 0.0001f;
    }

    /** Meta-driven variant of {@link #isPercentScaled(String, float, float)}. */
    public static boolean isPercentScaled(ParameterMetadata.PortMeta meta) {
        if (meta == null) return false;
        if (meta.controlType == ParameterMetadata.ControlType.TOGGLE) return false;
        if (meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER) return false;
        return isPercentScaled(canonicalUnit(meta.unit), meta.min, meta.max);
    }

    /**
     * Get display range string for manual input dialog.
     * @param meta Parameter metadata
     * @return Formatted "Min: X Max: Y unit"
     */
    public static String getRangeString(ParameterMetadata.PortMeta meta) {
        if (meta == null) return "";
        // Format through the meta itself so the resolved unit (and any
        // percent scaling) is preserved. Never emit "?" for empty units.
        DisplayValue dMin = rawToDisplay(meta.min, meta);
        DisplayValue dMax = rawToDisplay(meta.max, meta);
        return String.format(Locale.US, "Min: %s  Max: %s", dMin.text, dMax.text);
    }

    /**
     * Get step count for discrete controls (enum, integer, toggle).
     * @param meta Parameter metadata
     * @return Number of discrete steps, or 0 for continuous
     */
    public static int getStepCount(ParameterMetadata.PortMeta meta) {
        if (meta == null) return 0;
        if (meta.controlType == ParameterMetadata.ControlType.TOGGLE) return 2;
        if (meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER && meta.hasEnum()) {
            return meta.enumValues.length;
        }
        if (meta.controlType == ParameterMetadata.ControlType.INTEGER_STEPPER) {
            return (int) (meta.max - meta.min + 1);
        }
        return 0; // continuous
    }
}
