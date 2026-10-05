package com.kancrutt.gigginggear.metadata;

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
     * Control-name keywords whose native 0..1 range is conventionally a
     * percentage. Musicians read "Feedback 0..1" as 0..100%, so a unitless
     * 0..1 control with one of these name hints renders (and accepts input)
     * as a percent. Keyword matches are substring-based on the lowercased
     * control name (LADSPA/LV2 names are short compound words).
     */
    private static final String[] PERCENT_NAME_HINTS = {
            "feedback", "mix", "wet", "dry", "blend", "depth", "amount",
            "width", "damping", "diffusion", "intensity", "crossfade",
            "balance", "spread", "regeneration", "level"
    };

    private static final String[] TEN_SCALE_NAME_HINTS = {
            "gain", "drive", "overdrive", "distortion", "boost", "volume",
            "bass", "middle", "mid", "treble", "presence", "tone",
            "peakgain", "makeup"
    };

    private static final String[] RAW_NORMALIZED_NAME_HINTS = {
            "time", "attack", "release", "rate", "speed", "position",
            "ratio", "frequency", "cutoff", "select", "channel", "stage"
    };

    /**
     * P0-2: envelope/dynamics prefixes that must never be shown as percent on
     * a unitless 0..1 range. "Attack 75%" implies a physical ratio the plugin
     * does not declare (CAPS/MDA/Gx document these as normalized 0..1
     * controls). Prefix match, mirroring the Phase 1 audit rule
     * (audit-baseline.py PERCENT_RISK). Semantic percents (Feedback, Mix,
     * Wet, Depth, ...) are unaffected, and a DECLARED "%" unit always wins.
     */
    private static final String[] PERCENT_EXCLUDED_PREFIXES = {
            "attack", "release", "threshold", "time", "rate", "hold"
    };

    private static boolean nameIsPercentExcluded(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(LOCALE).trim();
        for (String k : PERCENT_EXCLUDED_PREFIXES) {
            if (n.startsWith(k)) return true;
        }
        return false;
    }

    private static boolean nameIsPercentLike(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(LOCALE);
        for (String k : PERCENT_NAME_HINTS) {
            if (n.contains(k)) return true;
        }
        return false;
    }

    private static boolean nameIsTenScaleLike(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(LOCALE).replaceAll("[^a-z0-9]", "");
        for (String k : TEN_SCALE_NAME_HINTS) {
            if (n.contains(k)) return true;
        }
        return false;
    }

    private static boolean nameIsRawNormalized(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(LOCALE);
        for (String k : RAW_NORMALIZED_NAME_HINTS) {
            if (n.contains(k)) return true;
        }
        return false;
    }

    /** True when the range is (approximately) the native 0..1 span. */
    private static boolean isUnityRange(float min, float max) {
        return Math.abs(min) < 0.0001f && Math.abs(max - 1f) < 0.0001f;
    }

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
        // 3.2-C: sample-rate-relative ports (e.g. normalized delay buffers) carry
        // no declared unit and a 0..1 range. Append "/sr" so the renderer shows
        // e.g. "0.42/sr" instead of a bare "0.42".
        if (meta.isSampleRate && unit.isEmpty()) {
            unit = "/sr";
        }

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
        } else if (isTenScale(meta)) {
            text = formatRaw(raw * 10f);
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
        if (isTenScale(meta)) {
            return clamp(display / 10f, meta.min, meta.max);
        }
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
        return String.format(Locale.US, "%.0f", v);
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
        return isUnityRange(min, max);
    }

    /** Name-aware variant used by the manual-input dialog. A unitless 0..1
     *  control whose name hints at a blend/level semantic (Feedback, Mix,
     *  Wet/Dry, ...) is percent-scaled even without a declared unit.
     *  Envelope/dynamics prefixes (Attack, Release, Threshold, Time, Rate,
     *  Hold) are excluded — they render as the raw normalized value. */
    public static boolean isPercentScaled(String controlName, String unit, float min, float max) {
        String u = canonicalUnit(unit);
        if (u != null && !u.isEmpty()) return isPercentScaled(u, min, max);
        return isUnityRange(min, max) && !nameIsTenScaleLike(controlName)
                && !nameIsPercentExcluded(controlName);
    }

    /** Meta-driven variant of {@link #isPercentScaled(String, float, float)}. */
    public static boolean isPercentScaled(ParameterMetadata.PortMeta meta) {
        if (meta == null) return false;
        if (meta.isSampleRate) return false;
        if (meta.controlType == ParameterMetadata.ControlType.TOGGLE) return false;
        if (meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER) return false;
        String u = canonicalUnit(meta.unit);
        if (u != null && !u.isEmpty()) return isPercentScaled(u, meta.min, meta.max);
        return isUnityRange(meta.min, meta.max) && !nameIsTenScaleLike(meta.name)
                && !nameIsPercentExcluded(meta.name);
    }

    /** Normalized amp/gain controls use the familiar 0..10 face value. */
    public static boolean isTenScale(ParameterMetadata.PortMeta meta) {
        if (meta == null) return false;
        if (meta.controlType == ParameterMetadata.ControlType.TOGGLE
                || meta.controlType == ParameterMetadata.ControlType.ENUM_PICKER) return false;
        if (!canonicalUnit(meta.unit).isEmpty()) return false;
        return isUnityRange(meta.min, meta.max)
                && nameIsTenScaleLike(meta.name)
                && !nameIsPercentLike(meta.name)
                && !nameIsRawNormalized(meta.name);
    }

    /** Name-aware manual-input equivalent of the 0..10 profile. */
    public static boolean isTenScale(String controlName, String unit, float min, float max) {
        return canonicalUnit(unit).isEmpty() && isUnityRange(min, max)
                && nameIsTenScaleLike(controlName)
                && !nameIsPercentLike(controlName)
                && !nameIsRawNormalized(controlName);
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
