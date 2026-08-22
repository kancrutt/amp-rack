package com.kancrutt.gigginggear;

import android.graphics.Color;

public class CategoryHelper {

    public static final int COLOR_AMP = Color.parseColor("#FFD1DC");
    public static final int COLOR_CAB = Color.parseColor("#E3E2E6");
    public static final int COLOR_DIST = Color.parseColor("#FFD9C2");
    public static final int COLOR_MOD = Color.parseColor("#CBE4F9");
    public static final int COLOR_DELAY = Color.parseColor("#D8F3DC");
    public static final int COLOR_REVERB = Color.parseColor("#E8D7F1");
    public static final int COLOR_COMP = Color.parseColor("#FFE8B8");
    public static final int COLOR_EQ = Color.parseColor("#D7F0DB");
    public static final int COLOR_PITCH = Color.parseColor("#D8D4F7");
    public static final int COLOR_WAH = Color.parseColor("#CFF2EE");
    public static final int COLOR_VOL = Color.parseColor("#CCF0E4");
    public static final int COLOR_GATE = Color.parseColor("#F5EBD8");
    public static final int COLOR_DEFAULT = Color.parseColor("#8F94A0");

    public enum Category {
        AMP("AMP", COLOR_AMP, R.drawable.ic_cat_amp, R.drawable.sl_cat_amp),
        CABINET("CAB", COLOR_CAB, R.drawable.ic_cat_cabinet, R.drawable.sl_cat_cabinet),
        DISTORTION("DIST", COLOR_DIST, R.drawable.ic_cat_distortion, R.drawable.sl_cat_distortion),
        MODULATION("MOD", COLOR_MOD, R.drawable.ic_cat_modulation, R.drawable.sl_cat_modulation),
        DELAY("DLY", COLOR_DELAY, R.drawable.ic_cat_delay, R.drawable.sl_cat_delay),
        REVERB("REVERB", COLOR_REVERB, R.drawable.ic_cat_reverb, R.drawable.sl_cat_reverb),
        DYNAMICS("COMP", COLOR_COMP, R.drawable.ic_cat_dynamics, R.drawable.sl_cat_dynamics),
        EQ("EQ", COLOR_EQ, R.drawable.ic_cat_eq, R.drawable.sl_cat_eq),
        PITCH("PITCH", COLOR_PITCH, R.drawable.ic_cat_pitch, R.drawable.sl_cat_pitch),
        WAH("WAH", COLOR_WAH, R.drawable.ic_cat_wah, R.drawable.sl_cat_wah),
        VOLUME("VOL", COLOR_VOL, R.drawable.ic_cat_volume, R.drawable.sl_cat_volume),
        GATE("GATE", COLOR_GATE, R.drawable.ic_cat_gate, R.drawable.sl_cat_gate);

        public final String code;
        public final int color;
        public final int iconRes;
        public final int selectorRes;

        Category(String code, int color, int iconRes, int selectorRes) {
            this.code = code;
            this.color = color;
            this.iconRes = iconRes;
            this.selectorRes = selectorRes;
        }
    }

    public static Category getCategory(String pluginName) {
        if (pluginName == null) return Category.VOLUME;
        String n = pluginName.toLowerCase().trim();

        // 1. AMP
        if (n.contains("neural amp") || n.contains("nam") || n.contains("aida") || n.equals("amp")
                || n.contains("amp head") || n.contains("amplifier") || n.contains("gx_amp") || n.contains("preamp")
                || n.contains("clean twin") || n.contains("plexi") || n.contains("jcm") || n.contains("rectifier")
                || n.contains("bogner") || n.contains("friedman") || n.contains("mesa") || n.contains("fender")
                || n.contains("marshall") || n.contains("soldano") || n.contains("diezel") || n.contains("engl")
                || n.contains("peavey") || n.contains("5150") || n.contains("twin") || n.contains("ac30")
                || n.contains("bassman") || n.contains("dumble") || n.contains("ampeg") || n.contains("svt")) {
            return Category.AMP;
        }

        // 2. CABINET / IR
        if (n.contains("cab") || n.contains("cabinet") || n.contains("ir") || n.contains("impulse")
                || n.contains("speaker") || n.contains("convol") || n.contains("celestion") || n.contains("4x12")
                || n.contains("2x12") || n.contains("1x12") || n.contains("4x10") || n.contains("1x15")
                || n.contains("v30") || n.contains("greenback") || n.contains("creamback")) {
            return Category.CABINET;
        }

        // 3. REVERB (prioritas sebelum modulation/delay)
        if (n.contains("reverb") || n.contains("spring") || n.contains("hall") || n.contains("plate")
                || n.contains("room") || n.contains("space") || n.contains("shimmer") || n.contains("freeverb")
                || n.contains("gigaverb") || n.contains("ambience") || n.contains("early ref")) {
            return Category.REVERB;
        }

        // 4. DELAY / ECHO
        if (n.contains("delay") || n.contains("echo") || n.contains("tape") || n.contains("bext")
                || n.contains("ping pong") || n.contains("dub") || n.contains("memory") || n.contains("analog delay")
                || n.contains("digital delay") || n.contains("slapback")) {
            return Category.DELAY;
        }

        // 5. MODULATION (Chorus, Flanger, Phaser, Tremolo, Vibrato, Rotary, Univibe, Detune, Dimension)
        if (n.contains("chorus") || n.contains("flang") || n.contains("phas") || n.contains("phase")
                || n.contains("trem") || n.contains("vibe") || n.contains("vibrato") || n.contains("rotary")
                || n.contains("detune") || n.contains("dimension") || n.contains("doubler") || n.contains("modul")
                || n.contains("spin") || n.contains("leslie") || n.contains("ring mod") || n.contains("pan")
                || n.contains("autopan") || n.contains("panner") || n.contains("tape mod")) {
            return Category.MODULATION;
        }

        // 6. DISTORTION / OVERDRIVE / FUZZ
        if (n.contains("dist") || n.contains("drive") || n.contains("fuzz") || n.contains("overdrive")
                || n.contains("screamer") || n.contains("ts9") || n.contains("ts808") || n.contains("rat")
                || n.contains("muff") || n.contains("klon") || n.contains("centaur") || n.contains("boost")
                || n.contains("crunch") || n.contains("lead") || n.contains("metal") || n.contains("saturation")
                || n.contains("clipper") || n.contains("blues") || n.contains("od") || n.contains("ds-1")) {
            return Category.DISTORTION;
        }

        // 7. DYNAMICS / COMPRESSOR / LIMITER
        if (n.contains("comp") || n.contains("dynamics") || n.contains("limiter") || n.contains("limit")
                || n.contains("sustainer") || n.contains("leveler") || n.contains("peak") || n.contains("dyn")) {
            return Category.DYNAMICS;
        }

        // 8. EQ / FILTER / TONE
        if (n.contains("eq") || n.contains("equalizer") || n.contains("parametric") || n.contains("graphic")
                || n.contains("tone") || n.contains("bandpass") || n.contains("lowpass") || n.contains("highpass")
                || n.contains("shelf") || n.contains("notch") || n.contains("mid") || n.contains("treble")) {
            return Category.EQ;
        }

        // 9. PITCH / SYNTH / OCTAVE
        if (n.contains("pitch") || n.contains("synth") || n.contains("octave") || n.contains("harmonizer")
                || n.contains("whammy") || n.contains("transpose") || n.contains("shifter") || n.contains("sub")
                || n.contains("generator") || n.contains("organ") || n.contains("poly")) {
            return Category.PITCH;
        }

        // 10. WAH / ENVELOPE FILTER
        if (n.contains("wah") || n.contains("crybaby") || n.contains("autowah") || n.contains("auto-wah")
                || n.contains("envelope") || n.contains("q-filter") || n.contains("talkbox")) {
            return Category.WAH;
        }

        // 11. NOISE GATE / EXPANDER
        if (n.contains("noise") || n.contains("gate") || n.contains("suppressor") || n.contains("expander")
                || n.contains("hiss") || n.contains("hum") || n.contains("denoise") || n.contains("silencer")) {
            return Category.GATE;
        }

        // 12. VOLUME / UTILITY
        if (n.contains("volume") || n.contains("vol") || n.contains("gain") || n.contains("master")
                || n.contains("trim") || n.contains("level") || n.contains("attenuat") || n.contains("mute")
                || n.contains("splitter") || n.contains("mixer") || n.contains("thru")) {
            return Category.VOLUME;
        }

        return Category.MODULATION; // Fallback cerdas jika mengandung unsur efek
    }

    public static String getCode(String pluginName) {
        return getCategory(pluginName).code;
    }

    public static int getColor(String pluginName) {
        return getCategory(pluginName).color;
    }

    public static int getIconRes(String pluginName) {
        return getCategory(pluginName).iconRes;
    }

    public static int getSelectorRes(String pluginName) {
        return getCategory(pluginName).selectorRes;
    }
}
