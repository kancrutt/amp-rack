package com.kancrutt.gigginggear;

import android.graphics.Color;

public class CategoryHelper {

    public static final int COLOR_AMP = Color.parseColor("#D96B58");
    public static final int COLOR_CAB = Color.parseColor("#D0D0D0");
    public static final int COLOR_DIST = Color.parseColor("#E07A5F");
    public static final int COLOR_MOD = Color.parseColor("#5C82E6");
    public static final int COLOR_DELAY = Color.parseColor("#52B788");
    public static final int COLOR_REVERB = Color.parseColor("#9D65C9");
    public static final int COLOR_COMP = Color.parseColor("#E09F3E");
    public static final int COLOR_EQ = Color.parseColor("#8CB369");
    public static final int COLOR_PITCH = Color.parseColor("#7B52AE");
    public static final int COLOR_WAH = Color.parseColor("#4EA8DE");
    public static final int COLOR_VOL = Color.parseColor("#48CAE4");
    public static final int COLOR_GATE = Color.parseColor("#F4A261");
    /** Routing utilities (SPLIT/MERGE) — neutral gray, deliberately not hue-coded. */
    public static final int COLOR_ROUTING = Color.parseColor("#8F94A0");
    public static final int COLOR_DEFAULT = Color.parseColor("#8F94A0");

    public enum Category {
        AMP("AMP", COLOR_AMP, R.drawable.ic_cat_amp, R.drawable.sl_cat_amp),
        CABINET("CAB", COLOR_CAB, R.drawable.ic_cat_cabinet, R.drawable.sl_cat_cabinet),
        DISTORTION("DIST", COLOR_DIST, R.drawable.ic_cat_distortion, R.drawable.sl_cat_distortion),
        MODULATION("MOD", COLOR_MOD, R.drawable.ic_cat_modulation, R.drawable.sl_cat_modulation),
        DELAY("DELAY", COLOR_DELAY, R.drawable.ic_cat_delay, R.drawable.sl_cat_delay),
        REVERB("REVERB", COLOR_REVERB, R.drawable.ic_cat_reverb, R.drawable.sl_cat_reverb),
        DYNAMICS("COMP", COLOR_COMP, R.drawable.ic_cat_dynamics, R.drawable.sl_cat_dynamics),
        EQ("EQ", COLOR_EQ, R.drawable.ic_cat_eq, R.drawable.sl_cat_eq),
        PITCH("PITCH", COLOR_PITCH, R.drawable.ic_cat_pitch, R.drawable.sl_cat_pitch),
        WAH("WAH", COLOR_WAH, R.drawable.ic_cat_wah, R.drawable.sl_cat_wah),
        UTILITY("UTILITY", COLOR_ROUTING, R.drawable.ic_cat_utility, R.drawable.sl_cat_utility),
        UNKNOWN("UNKNOWN", COLOR_DEFAULT, R.drawable.ic_cat_volume, R.drawable.sl_cat_unknown),
        GATE("GATE", COLOR_GATE, R.drawable.ic_cat_gate, R.drawable.sl_cat_gate),
        // Routing utilities (not audio effects): neutral/uncolored treatment,
        // same convention as CABINET staying uncolored.
        SPLIT("SPLIT", COLOR_ROUTING, R.drawable.ic_routing_split, R.drawable.ic_routing_split),
        MERGE("MERGE", COLOR_ROUTING, R.drawable.ic_routing_merge, R.drawable.ic_routing_merge);

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

    /** Canonical Gx/Guitarix, rkr, MDA & miscellaneous id -> effect family. */
    private static final java.util.Map<String, Category> OVERRIDES = new java.util.HashMap<>();

    static {
        // ---- Gx / Guitarix: Overdrive ----
        OVERRIDES.put("bottlerocket", Category.DISTORTION);
        OVERRIDES.put("dop250", Category.DISTORTION);
        OVERRIDES.put("dop270", Category.DISTORTION);
        OVERRIDES.put("guvnor", Category.DISTORTION);
        OVERRIDES.put("hotbox", Category.DISTORTION);
        OVERRIDES.put("sd1", Category.DISTORTION);
        OVERRIDES.put("sd1sim", Category.DISTORTION);
        OVERRIDES.put("sd2lead", Category.DISTORTION);
        OVERRIDES.put("valvecaster", Category.DISTORTION);
        OVERRIDES.put("boobtube", Category.DISTORTION);
        OVERRIDES.put("shakatube", Category.DISTORTION);
        OVERRIDES.put("bajatu", Category.DISTORTION);
        OVERRIDES.put("timray", Category.DISTORTION);
        OVERRIDES.put("luna", Category.DISTORTION);
        OVERRIDES.put("sloopy", Category.DISTORTION);
        OVERRIDES.put("sloopyblue", Category.DISTORTION);
        OVERRIDES.put("eternity", Category.DISTORTION);
        OVERRIDES.put("clubdrive", Category.DISTORTION);
        OVERRIDES.put("overdriver", Category.DISTORTION);
        OVERRIDES.put("liquiddrive", Category.DISTORTION);
        // ---- Gx: Fuzz / Distortion ----
        OVERRIDES.put("axisface", Category.DISTORTION);
        OVERRIDES.put("fz1", Category.DISTORTION);
        OVERRIDES.put("fz1b", Category.DISTORTION);
        OVERRIDES.put("fz1s", Category.DISTORTION);
        OVERRIDES.put("hyperion", Category.DISTORTION);
        OVERRIDES.put("heathkit", Category.DISTORTION);
        OVERRIDES.put("hzkit", Category.DISTORTION);
        OVERRIDES.put("knightfuzz", Category.DISTORTION);
        OVERRIDES.put("suppatone", Category.DISTORTION);
        OVERRIDES.put("saturator", Category.DISTORTION);
        OVERRIDES.put("sunface", Category.DISTORTION);
        OVERRIDES.put("superfuzz", Category.DISTORTION);
        OVERRIDES.put("tonemachine", Category.DISTORTION);
        OVERRIDES.put("vintagefuzzmaster", Category.DISTORTION);
        OVERRIDES.put("voodoo", Category.DISTORTION);
        OVERRIDES.put("fuzzface", Category.DISTORTION);
        OVERRIDES.put("fuzzfacefuller", Category.DISTORTION);
        OVERRIDES.put("fuzzmaster", Category.DISTORTION);
        OVERRIDES.put("bossds1", Category.DISTORTION);
        OVERRIDES.put("distortionplus", Category.DISTORTION);
        OVERRIDES.put("creammachine", Category.AMP); // TTL: tube power amp circuit
        OVERRIDES.put("screambird", Category.DISTORTION);
        OVERRIDES.put("screamingbird", Category.DISTORTION);
        OVERRIDES.put("metalhead", Category.DISTORTION);
        OVERRIDES.put("fatfrog", Category.AMP); // TTL: High Gain Amplifier
        OVERRIDES.put("osmutantes", Category.DISTORTION);
        OVERRIDES.put("muffd", Category.DISTORTION);
        OVERRIDES.put("alembic", Category.AMP); // TTL: 12AX7 preamp + tonestack
        OVERRIDES.put("hogsfoot", Category.DISTORTION);
        OVERRIDES.put("tordrive", Category.DISTORTION);
        OVERRIDES.put("voodo", Category.DISTORTION);
        OVERRIDES.put("screamingbird_", Category.DISTORTION);
        // ---- Gx: Boosters ----
        OVERRIDES.put("microamp", Category.DISTORTION); // R11 audit: booster -> OD/Dist
        OVERRIDES.put("rangemaster", Category.DISTORTION);
        OVERRIDES.put("rangem", Category.DISTORTION);
        OVERRIDES.put("booster", Category.DISTORTION);
        // ---- Gx: Amplifiers / preamps ----
        OVERRIDES.put("svt", Category.AMP);
        OVERRIDES.put("blueamp", Category.AMP);
        OVERRIDES.put("vmk2", Category.AMP);
        OVERRIDES.put("vmk2d", Category.CABINET); // TTL: Classic Cabinet Simulation
        OVERRIDES.put("uvox", Category.AMP);
        OVERRIDES.put("epic", Category.AMP);
        OVERRIDES.put("supersonic", Category.AMP);
        OVERRIDES.put("plexi", Category.AMP);
        OVERRIDES.put("ampegsvt", Category.AMP);
        OVERRIDES.put("voxbass", Category.AMP);
        OVERRIDES.put("studiopre", Category.AMP);
        OVERRIDES.put("jcm800pre", Category.AMP);
        OVERRIDES.put("amplifier", Category.AMP);
        OVERRIDES.put("microamp_amp", Category.AMP);
        OVERRIDES.put("guitarix", Category.AMP);
        OVERRIDES.put("metalamp", Category.AMP);
        // ---- Gx: Cabinets / IR ----
        OVERRIDES.put("ultracab", Category.CABINET);
        OVERRIDES.put("cabsim", Category.CABINET);
        OVERRIDES.put("cabir", Category.CABINET);
        // ---- Gx: Modulation ----
        OVERRIDES.put("chorusstereo", Category.MODULATION);
        OVERRIDES.put("flanger", Category.MODULATION);
        OVERRIDES.put("phaser", Category.MODULATION);
        OVERRIDES.put("phaser_stereo", Category.MODULATION);
        OVERRIDES.put("vibrato", Category.MODULATION);
        OVERRIDES.put("tubevibrato", Category.MODULATION);
        OVERRIDES.put("tremolo", Category.MODULATION);
        OVERRIDES.put("tubetremelo", Category.MODULATION);
        OVERRIDES.put("switchedtremolo", Category.MODULATION);
        OVERRIDES.put("vibe", Category.MODULATION);
        OVERRIDES.put("vibemono", Category.MODULATION);
        OVERRIDES.put("mbreverb", Category.REVERB); // TTL: Multi Band Reverb
        OVERRIDES.put("ringmod", Category.MODULATION);
        OVERRIDES.put("zoom", Category.DISTORTION); // TTL: Distortion pedal from early 80s
        OVERRIDES.put("saturater", Category.MODULATION);
        OVERRIDES.put("shimmizita", Category.REVERB); // TTL: Reverb pedal
        OVERRIDES.put("pushpull", Category.DISTORTION); // Guitarix pushpull distortion (normalized key)
        OVERRIDES.put("mobeemodule", Category.MODULATION);
        OVERRIDES.put("moduloso", Category.MODULATION);
        OVERRIDES.put("tonebender", Category.DISTORTION);
        // ---- Gx: Delay ----
        OVERRIDES.put("digitaldelay", Category.DELAY);
        OVERRIDES.put("reversedelay", Category.DELAY);
        
        OVERRIDES.put("echocat", Category.DELAY);
        OVERRIDES.put("redeyefx", Category.DISTORTION); // TTL: Distortion pedal
        OVERRIDES.put("multibanddelay", Category.DELAY);
        OVERRIDES.put("multibandecho", Category.DELAY);
        OVERRIDES.put("duckdelay", Category.DELAY);
        OVERRIDES.put("bentdelay", Category.DELAY);
        OVERRIDES.put("room_simulator", Category.REVERB);
        OVERRIDES.put("slapback", Category.DELAY);
        // ---- Gx: Reverb ----
        OVERRIDES.put("reverb", Category.REVERB);
        OVERRIDES.put("reverb_stereo", Category.REVERB);
        OVERRIDES.put("zitarev", Category.REVERB);
        OVERRIDES.put("room", Category.REVERB);
        // ---- Gx: Compressor / Dynamics ----
        OVERRIDES.put("compressor", Category.DYNAMICS);
        OVERRIDES.put("multibandcompressor", Category.DYNAMICS);
        OVERRIDES.put("expander", Category.DYNAMICS);
        OVERRIDES.put("noisegate", Category.GATE);
        OVERRIDES.put("denoiser", Category.GATE);
        OVERRIDES.put("denoiser2", Category.GATE); // TTL agrees (noise suppressor)
        OVERRIDES.put("susta", Category.DYNAMICS);
        // ---- Gx: EQ ----
        OVERRIDES.put("graphiceq", Category.EQ);
        OVERRIDES.put("4bandeq", Category.EQ);
        OVERRIDES.put("tilttone", Category.EQ);
        OVERRIDES.put("tonemender", Category.DISTORTION); // TTL: Clean boost pedal
        OVERRIDES.put("hfb", Category.EQ);
        OVERRIDES.put("fenderizer", Category.DISTORTION); // TTL: Distortion Simulation
        OVERRIDES.put("equalizer_", Category.EQ);
        OVERRIDES.put("tilt_tone", Category.EQ);
        // ---- Gx: Wah ----
        OVERRIDES.put("quack", Category.WAH);
        OVERRIDES.put("wah", Category.WAH);
        OVERRIDES.put("wahwah", Category.WAH);
        OVERRIDES.put("switchlesswah", Category.WAH);
        OVERRIDES.put("gcb95", Category.WAH); // Hery H2: GCB-95 = wah pedal (TTL DistortionPlugin is upstream junk)
        OVERRIDES.put("colwah", Category.WAH);
        OVERRIDES.put("autowah", Category.WAH);
        // ---- Gx: Volume / swell ----
        OVERRIDES.put("slowgear", Category.UTILITY);
        OVERRIDES.put("vfem", Category.UTILITY);
        OVERRIDES.put("volume", Category.UTILITY);
        OVERRIDES.put("gain", Category.UTILITY);
        // ---- Gx: Pitch / octave / synth ----
        OVERRIDES.put("oc2", Category.PITCH); // canonical plugin map policy
        OVERRIDES.put("oct", Category.PITCH);
        OVERRIDES.put("octaver", Category.PITCH);
        OVERRIDES.put("cstb", Category.DISTORTION); // TTL: Distortion, classic tone shaping pedal
        OVERRIDES.put("muscat", Category.PITCH);
        OVERRIDES.put("synth", Category.PITCH);
        // looper
        OVERRIDES.put("looper", Category.UTILITY);

        // ---- rkr / TAP vintage ----
        OVERRIDES.put("valve", Category.AMP);
        OVERRIDES.put("cabinet", Category.CABINET);
        OVERRIDES.put("dist", Category.DISTORTION);
        OVERRIDES.put("distorsion", Category.DISTORTION);
        OVERRIDES.put("distband", Category.DISTORTION);
        OVERRIDES.put("stompbox", Category.DISTORTION);
        OVERRIDES.put("stompfuzz", Category.DISTORTION);
        OVERRIDES.put("exciter", Category.DISTORTION);
        OVERRIDES.put("coilcrafter", Category.DISTORTION);
        OVERRIDES.put("dere", Category.DISTORTION);
        OVERRIDES.put("infinity", Category.DISTORTION);
        OVERRIDES.put("echo", Category.DELAY);
        OVERRIDES.put("echotron", Category.DELAY);
        OVERRIDES.put("delm", Category.DELAY);
        OVERRIDES.put("echoverse", Category.DELAY);
        OVERRIDES.put("reve", Category.REVERB);
        OVERRIDES.put("reverbtron", Category.REVERB);
        OVERRIDES.put("eqp", Category.EQ);
        OVERRIDES.put("eql", Category.EQ);
        OVERRIDES.put("shelfboost", Category.EQ);
        OVERRIDES.put("comp", Category.DYNAMICS);
        OVERRIDES.put("compband", Category.DYNAMICS);
        OVERRIDES.put("sustain", Category.DYNAMICS);
        OVERRIDES.put("limiter", Category.DYNAMICS);
        OVERRIDES.put("loudness", Category.DYNAMICS);
        OVERRIDES.put("gate", Category.GATE);
        OVERRIDES.put("aphas", Category.MODULATION);
        OVERRIDES.put("phas", Category.MODULATION);
        OVERRIDES.put("chor", Category.MODULATION);
        OVERRIDES.put("dual_flange", Category.MODULATION);
        OVERRIDES.put("dualflange", Category.MODULATION);
        OVERRIDES.put("vibe", Category.MODULATION);
        OVERRIDES.put("otrem", Category.MODULATION);
        OVERRIDES.put("pan", Category.MODULATION);
        OVERRIDES.put("ring", Category.MODULATION);
        OVERRIDES.put("varyband", Category.MODULATION);
        OVERRIDES.put("mutromojo", Category.MODULATION);
        OVERRIDES.put("arpie", Category.PITCH);
        OVERRIDES.put("sequence", Category.PITCH);
        OVERRIDES.put("shifter", Category.PITCH);
        OVERRIDES.put("har_no_midi", Category.PITCH);
        OVERRIDES.put("stereoharm_no_midi", Category.PITCH);
        OVERRIDES.put("vocoder", Category.PITCH);
        OVERRIDES.put("synthfilter", Category.PITCH);
        OVERRIDES.put("shuffle", Category.PITCH);
        OVERRIDES.put("awha", Category.WAH);
        OVERRIDES.put("wha", Category.WAH);
        OVERRIDES.put("mixter", Category.UTILITY);
        OVERRIDES.put("splitter", Category.SPLIT);
        OVERRIDES.put("stereo", Category.UTILITY);
        OVERRIDES.put("image", Category.UTILITY);
        OVERRIDES.put("dither", Category.UTILITY);

        // ---- MDA ----
        OVERRIDES.put("overdrive", Category.DISTORTION);
        OVERRIDES.put("bandisto", Category.DISTORTION);
        OVERRIDES.put("degrade", Category.DISTORTION);
        OVERRIDES.put("delay", Category.DELAY);
        OVERRIDES.put("dubdelay", Category.DELAY);
        OVERRIDES.put("repsycho", Category.DELAY);
        OVERRIDES.put("detune", Category.MODULATION);
        OVERRIDES.put("leslie", Category.MODULATION);
        OVERRIDES.put("ringmod", Category.MODULATION);
        OVERRIDES.put("thruzero", Category.MODULATION);
        OVERRIDES.put("combo", Category.MODULATION);
        OVERRIDES.put("roundpan", Category.MODULATION);
        OVERRIDES.put("tracker", Category.MODULATION);
        OVERRIDES.put("dynamics", Category.DYNAMICS);
        OVERRIDES.put("transient", Category.DYNAMICS);
        OVERRIDES.put("multiband", Category.DYNAMICS);
        OVERRIDES.put("deess", Category.DYNAMICS);
        OVERRIDES.put("rezfilter", Category.EQ);
        OVERRIDES.put("ambience", Category.REVERB);
        OVERRIDES.put("dx10", Category.PITCH);
        OVERRIDES.put("epiano", Category.PITCH);
        OVERRIDES.put("jx10", Category.PITCH);
        OVERRIDES.put("piano", Category.PITCH);
        OVERRIDES.put("beatsbox", Category.PITCH);
        OVERRIDES.put("beatbox", Category.PITCH);
        OVERRIDES.put("shepard", Category.PITCH);
        OVERRIDES.put("vocinput", Category.PITCH);
        OVERRIDES.put("castinette", Category.PITCH);
        OVERRIDES.put("talkbox", Category.WAH);
        OVERRIDES.put("testtone", Category.UTILITY);
        OVERRIDES.put("splitter2", Category.SPLIT);

        // ---- the infamous / famous singles ----
        OVERRIDES.put("cheapdist", Category.DISTORTION);
        OVERRIDES.put("coldfire", Category.DISTORTION);
        OVERRIDES.put("hip2b", Category.DISTORTION); // Hery r21: hysteresis square-wave dist (TTL: lv2:DistortionPlugin)
        OVERRIDES.put("octolo", Category.PITCH);
        OVERRIDES.put("powercut", Category.MODULATION);
        OVERRIDES.put("powerup", Category.UTILITY);
        OVERRIDES.put("stuck", Category.UTILITY); // R11 audit: clip looper/drone
        OVERRIDES.put("lushlife", Category.MODULATION);
        OVERRIDES.put("mindi", Category.UTILITY);
        OVERRIDES.put("ewham", Category.PITCH);
        OVERRIDES.put("envfollower", Category.WAH);
        OVERRIDES.put("casynth", Category.PITCH);
        OVERRIDES.put("cellular", Category.PITCH);
        OVERRIDES.put("bentdelay", Category.DELAY);
        OVERRIDES.put("bleelp", Category.PITCH);
        OVERRIDES.put("vocoder_", Category.PITCH);

        // ---- GNUitar / NAM / convo / others ----
        OVERRIDES.put("bluesbreaker", Category.DISTORTION); // TTL: Overdrive pedal simulation
        OVERRIDES.put("collisiondrive", Category.DISTORTION);
        OVERRIDES.put("metal", Category.DISTORTION);
        OVERRIDES.put("harmonicexciter", Category.DISTORTION); // TTL: half wave clipper
        OVERRIDES.put("nam", Category.AMP);
        OVERRIDES.put("aida", Category.AMP);
        OVERRIDES.put("convolution", Category.CABINET);
        OVERRIDES.put("ir", Category.CABINET);
        OVERRIDES.put("littlefly", Category.DISTORTION); // TTL: Overdrive/Distortion pedal simulation
        OVERRIDES.put("bmp", Category.DISTORTION); // Gx_bmp_: TTL Distortion pedal
        OVERRIDES.put("poweramps", Category.AMP); // TTL: Simulate valve amplifiers
        OVERRIDES.put("powerampimpulses", Category.AMP); // TTL: Simulate valve amplifiers
        OVERRIDES.put("tamgamp", Category.AMP); // TTL: Guitar amp simulator
        OVERRIDES.put("darkterror", Category.AMP);
        OVERRIDES.put("tinyterror", Category.AMP);
        OVERRIDES.put("rumor", Category.DISTORTION); // TTL: growling bass fuzz pedal
        OVERRIDES.put("vr50", Category.AMP);
        OVERRIDES.put("darc", Category.DYNAMICS); // TTL: x42 DARC = signal compressor
        OVERRIDES.put("poweramp", Category.AMP);
        OVERRIDES.put("abeast", Category.AMP);
        OVERRIDES.put("metaltone", Category.DISTORTION);

        // ---- full-name keys: PluginNaming.norm() keeps vendor prefixes, so the
        // short keys above NEVER match "MDA X" / "the infamous X" at runtime.
        // Total audit 2026-09: these 16 fell through to the MODULATION fallback
        // (Log.w). Each mirrors its short-key sibling's category.
        // MDA family (norm: "MDA BeatBox" -> "mdabeatbox")
        OVERRIDES.put("mdabeatbox", Category.PITCH); // drum samples; cf. "beatbox"
        OVERRIDES.put("mdacombo", Category.AMP); // combo amp sim; cf. "combo"
        OVERRIDES.put("mdadeess", Category.DYNAMICS); // de-esser; cf. "deess"
        OVERRIDES.put("mdadegrade", Category.DISTORTION); // sample-quality reduction; cf. "degrade"
        OVERRIDES.put("mdadx10", Category.PITCH); // DX-style synth; cf. "dx10"
        OVERRIDES.put("mdaepiano", Category.PITCH); // e-piano synth; cf. "epiano"
        OVERRIDES.put("mdajx10", Category.PITCH); // JX-style synth; cf. "jx10"
        OVERRIDES.put("mdapiano", Category.PITCH); // piano synth; cf. "piano"
        OVERRIDES.put("mdatesttone", Category.UTILITY); // signal generator; cf. "testtone"
        // Guitarix odd-one-out (norm: "Gx_vstb_" -> "vstb")
        OVERRIDES.put("vstb", Category.DISTORTION); // libGxSuppaToneBender: Tone Bender pedal sim
        // "the infamous X" family (norm keeps the prefix: "theinfamousewham")
        OVERRIDES.put("theinfamousewham", Category.PITCH); // cf. "ewham"
        OVERRIDES.put("theinfamouship2b", Category.DISTORTION); // Hery r21: cf. "hip2b", was GATE
        OVERRIDES.put("theinfamouslushlife", Category.MODULATION); // cf. "lushlife"
        OVERRIDES.put("theinfamousmindi", Category.UTILITY); // cf. "mindi"
        OVERRIDES.put("theinfamousoctolo", Category.PITCH); // cf. "octolo"
        OVERRIDES.put("theinfamouspowerup", Category.UTILITY); // cf. "powerup"
        OVERRIDES.put("theinfamousstuck", Category.UTILITY); // waveform drone; cf. "stuck"
        OVERRIDES.put("convolv2", Category.REVERB); // convoLV2 = convolution reverb (single conv-reverb slot)
    }

    /** Direct id -> category lookup (normalized name), null when not overridden. */
    static Category getCategoryOverride(String pluginName) {
        String k = PluginNaming.norm(pluginName);
        if (k == null || k.isEmpty()) return null;
        return OVERRIDES.get(k);
    }

    /**
     * Zero-Trust resolution order (authoritative first, guesses last):
     *  0. {@link CategoryGenerated} — derived from the plugins' OWN metadata
     *     (LADSPA ontology classes in assets/rdf/*.rdf, LV2 TTL descriptions).
     *     Example: uid 1189 "Ringmod with LFO" is declared ladspa:ModulatorPlugin
     *     by its author -> MODULATION. "x42 DARC" desc = compressor -> DYNAMICS.
     *  1. Legacy proper-noun overrides (GxPlugins README knowledge).
     *  2. Whole-token classifier below (mirrors tools/gen_category_map.py).
     *     Bare substrings (od/ir/pan/sub/rat/...) are FORBIDDEN here — they used
     *     to steal across families ("ringmod" -> DIST via "od", "first" -> CAB
     *     via "ir", "expand" -> MOD via "pan").
     *  3. UNKNOWN fallback + Log.w so unknowns stay visible, never silently become an effect.
     */
    public static Category getCategory(String pluginName) {
        if (pluginName == null) return Category.UTILITY;

        String k = PluginNaming.norm(pluginName);
        String g = CategoryGenerated.LADSPA_BY_NAME.get(k);
        if (g == null) g = CategoryGenerated.LV2_BY_NAME.get(k);
        if (g != null) {
            try {
                return normalizeRouting(Category.valueOf(g));
            } catch (IllegalArgumentException ignored) {}
        }

        Category byName = getCategoryOverride(pluginName);
        if (byName != null) return normalizeRouting(byName);

        Category tok = classifyTokens(pluginName);
        if (tok != null) return normalizeRouting(tok);

        android.util.Log.w("CategoryHelper",
                "no metadata for '" + pluginName + "' — fallback UNKNOWN");
        return Category.UNKNOWN;
    }

    /** Routing blocks (SPLIT/MERGE) have no picker chip — expose them as
     * UTILITY so they browse under the Utility chip. (R11: Hery.) */
    static Category normalizeRouting(Category c) {
        if (c == Category.SPLIT || c == Category.MERGE) return Category.UTILITY;
        return c;
    }

    /** Whole-token classifier. Must stay in sync with token_cat() in tools/gen_category_map.py. */
    static Category classifyTokens(String pluginName) {
        if (pluginName == null) return null;
        java.util.HashSet<String> toks = new java.util.HashSet<>();
        for (String t : pluginName.toLowerCase().split("[^a-z0-9]+")) {
            if (!t.isEmpty()) toks.add(t);
        }
        // Routing utilities (structural, not effects) — checked first.
        if (toks.contains("split")) return Category.UTILITY;
        if (toks.contains("merge")) return Category.UTILITY;
        if (hasAny(toks, "cabinet", "cab", "impulse", "convolver", "convolution",
                "speaker", "celestion", "greenback", "creamback", "v30")) return Category.CABINET;
        if (hasAny(toks, "reverb", "gverb", "freeverb", "ambience", "cathedral",
                "chapel", "spring", "plate", "shimmer")) return Category.REVERB;
        if (hasAny(toks, "delay", "echo", "slapback", "dub", "ping", "pong"))
            return Category.DELAY;
        if (hasAny(toks, "chorus", "flanger", "flange", "phaser", "phase",
                "tremolo", "vibrato", "vibe", "rotary", "leslie", "detune",
                "dimension", "doubler", "ringmod", "ring", "autopan", "panner",
                "univibe", "modulation", "modulate", "modulator"))
            return Category.MODULATION;
        if (hasAny(toks, "distortion", "overdrive", "fuzz", "muff", "bmp",
                "screamer", "ts9", "ts808", "klon", "centaur", "crunch",
                "saturation", "saturator", "clipper", "decimator", "bitcrush",
                "foldover", "chebyshev", "diode", "booster", "waveshaper",
                "wavewrapper", "aliasing")) return Category.DISTORTION;
        if (hasAny(toks, "compressor", "compress", "dynamics", "limiter",
                "sustain", "sustainer", "deesser", "transient", "maximiser",
                "maximizer", "leveler", "peak")) return Category.DYNAMICS;
        if (hasAny(toks, "gate", "noisegate", "denoise", "denoiser", "silencer",
                "suppressor")) return Category.GATE;
        // NOTE: bare tonestack words (tone/bass/treble/mid/presence) excluded —
        // they misfire on pedal names (ToneMachine fuzz, Vox Bass Preamp).
        if (hasAny(toks, "parametric", "equaliser", "equalizer", "eq", "graphic",
                "multiband", "shelf", "shelving", "notch", "lowpass", "highpass",
                "bandpass", "crossover", "allpass", "comb", "filter"))
            return Category.EQ;
        if (hasAny(toks, "pitch", "octave", "suboctave", "shifter", "synth",
                "harmonizer", "harmoniser", "whammy", "transpose", "autotalent",
                "vocoder", "oscillator", "generator", "organ", "poly"))
            return Category.PITCH;
        if (hasAny(toks, "wah", "crybaby", "talkbox", "envelope")) return Category.WAH;
        if (hasAny(toks, "amplifier", "amp", "preamp", "preamplifier", "nam",
                "aida", "marshall", "fender", "mesa", "boogie", "peavey", "engl",
                "diezel", "soldano", "bogner", "friedman", "orange", "vox",
                "plexi", "jcm", "rectifier", "twin", "bassman", "dumble", "ampeg",
                "svt", "5150", "ac30", "vh4", "uberschall", "terror", "tiny",
                "dark", "tamg", "poweramp", "combo", "tube", "valve", "warmth"))
            return Category.AMP;
        if (hasAny(toks, "volume", "vol", "gain", "level", "trim", "master",
                "mixer", "crossfade", "splitter", "utility", "mute", "thru"))
            return Category.UTILITY;
        return null;
    }

    private static boolean hasAny(java.util.Set<String> toks, String... words) {
        for (String w : words) if (toks.contains(w)) return true;
        return false;
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
