package com.kancrutt.gigginggear;

import java.util.HashMap;
import java.util.Map;

/**
 * Global display-name formatter for plugin effects.
 *
 * LV2 bundles expose raw/internal names (GxHotBox, Gx_guvnor_, the infamous x, rkr ..., MDA ...).
 * This maps them to clean, musician-friendly names (Title Case), with a commercial
 * name dictionary for the well-known Guitarix (Gx) pedals backed by the GxPlugins.lv2 README,
 * and a generic fallback (strip prefixes / separators, camel-case split, capitalize words)
 * covering the long tail so every name renders cleanly.
 */
public final class PluginNaming {

    private PluginNaming() {}

    /** Purpose-built musical names keyed by normalized plugin id (see {@link #norm}). */
    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        // ---- Overdrive (GxPlugins.lv2 README) ----
        NAMES.put("bottlerocket", "Bottle Rocket");
        NAMES.put("dop250", "DOD OD-250");
        NAMES.put("guvnor", "Guv'nor Overdrive");
        NAMES.put("hotbox", "Hot Box");
        NAMES.put("sd1", "SD-1 Super Overdrive");
        NAMES.put("sd1sim", "SD-1 Overdrive");
        NAMES.put("bossds1", "Boss DS-1 Distortion");
        NAMES.put("sd2lead", "SD-2 Dual Overdrive");
        NAMES.put("valvecaster", "Valve Caster");
        NAMES.put("boobtube", "Boob Tube");
        NAMES.put("shakatube", "Shaka Tube");
        NAMES.put("bajatube", "BaJa Tube Driver");
        NAMES.put("bajatubedriver", "BaJa Tube Driver");
        NAMES.put("timray", "Jan Ray Overdrive");
        NAMES.put("luna", "Luna Overdrive");
        NAMES.put("sloopy", "Sloopy Blue Overdrive");
        NAMES.put("sloopyblue", "Sloopy Blue Overdrive");
        NAMES.put("eternity", "Eternity Overdrive");
        NAMES.put("clubdrive", "Club Drive");
        NAMES.put("overdriver", "Overdriver");
        NAMES.put("overdriver_", "Overdriver");
        // ---- Fuzz / Distortion ---- 
        NAMES.put("axisface", "Axis Face Fuzz");
        NAMES.put("fz1", "Maestro FZ-1B Fuzz");
        NAMES.put("fz1b", "Maestro FZ-1B Fuzz");
        NAMES.put("fz1s", "Maestro FZ-1S Fuzz");
        NAMES.put("hyperion", "Hyperion Fuzz");
        NAMES.put("heathkit", "Heathkit Distortion");
        NAMES.put("hzkit", "Heathkit Distortion");
        NAMES.put("knightfuzz", "Knight Fuzz");
        NAMES.put("liquiddrive", "Liquid Drive");
        NAMES.put("suppatone", "Supa Tonebender Fuzz");
        NAMES.put("saturator", "Satchurator Distortion");
        NAMES.put("sunface", "Sun Face Fuzz");
        NAMES.put("superfuzz", "Super-Fuzz");
        NAMES.put("tonemachine", "Tone Machine Fuzz");
        NAMES.put("tubedistortion", "Tube Distortion");
        NAMES.put("vintagefuzzmaster", "Vintage Fuzz Master");
        NAMES.put("voodoofuzz", "Voodoo Fuzz");
        NAMES.put("voodo", "Voodoo Fuzz");
        NAMES.put("fuzzface", "Fuzz Face");
        NAMES.put("fuzzfacefuller", "Fuzz Face Full Range");
        NAMES.put("fuzz", "Fuzz");
        NAMES.put("distortionplus", "Distortion+");
        NAMES.put("screamingbird", "Screaming Bird");
        NAMES.put("metalhead", "Metal Head");
        NAMES.put("metalamp", "Metal Amp");
        NAMES.put("multibanddist", "Multi-Band Distortion");
        NAMES.put("multibanddistortion", "Multi-Band Distortion");
        NAMES.put("aclipper", "A Current Clipper");
        NAMES.put("hogsfoot", "Hogs Foot");
        NAMES.put("mxrdist", "MXR Distortion");
        NAMES.put("muff", "Big Muff");
        NAMES.put("fenderizer", "Fenderizer");
        NAMES.put("saturate", "Saturation");
        NAMES.put("screambird", "Screaming Bird");
        // ---- Boost / preamp pedals ----
        NAMES.put("microamp", "Micro Amp Booster");
        NAMES.put("rangemaster", "Range Master");
        NAMES.put("rangem", "Range Master");
        NAMES.put("booster", "Booster");
        NAMES.put("boost", "Booster");
        // ---- Amplifiers ----
        NAMES.put("svt", "SVT Bass Amp");
        NAMES.put("blueamp", "Blue Amp");
        NAMES.put("vmk2", "Vox MKII Preamp");
        NAMES.put("vmk2d", "Vox MKII Preamp");
        NAMES.put("uvox", "Univox 720k Amp");
        NAMES.put("creammachine", "Cream Machine");
        NAMES.put("epic", "Epic Amp");
        NAMES.put("supersonic", "Supersonic Amp");
        NAMES.put("plexi", "Plexi");
        NAMES.put("ampegsvt", "Ampeg SVT");
        NAMES.put("voxbass", "Vox Bass Preamp");
        NAMES.put("studiopre", "Studio Preamp");
        NAMES.put("jcm800pre", "JCM800 Preamp");
        NAMES.put("valvecaster_amp", "Valve Caster");
        // ---- Cabinets ----
        NAMES.put("ultracab", "Ultra Cabinet");
        NAMES.put("cabsim", "Cab Simulator");
        NAMES.put("cabinet", "Speaker Cabinet");
        NAMES.put("room_simulator", "Room Simulator");
        // ---- Modulation ----
        NAMES.put("chorus", "Chorus");
        NAMES.put("flanger", "Flanger");
        NAMES.put("phaser", "Phaser");
        NAMES.put("vibrato", "Vibrato");
        NAMES.put("tubevibrato", "Tube Vibrato");
        NAMES.put("tremolo", "Tremolo");
        NAMES.put("tubetremelo", "Tube Tremolo");
        NAMES.put("switchedtremolo", "Switched Tremolo");
        NAMES.put("vibe", "Vibe");
        NAMES.put("vibemono", "Vibe Mono");
        NAMES.put("oct", "Octave Up");
        NAMES.put("oc2", "OC-2 Octave");
        NAMES.put("gcb95", "GCB-95 Wah");
        NAMES.put("shimmizita", "Shimmizita");
        // ---- Delay / Reverb ----
        NAMES.put("digitaldelay", "Digital Delay");
        NAMES.put("reversedelay", "Reverse Delay");
        NAMES.put("tape", "Tape Delay");
        NAMES.put("echocat", "Echo Cat");
        NAMES.put("multibanddelay", "Multi-Band Delay");
        NAMES.put("multibandecho", "Multi-Band Echo");
        NAMES.put("ddelay", "Duck Delay");
        NAMES.put("duckdelay", "Duck Delay");
        NAMES.put("bentdelay", "Bent Delay");
        NAMES.put("mbreverb", "Multi-Band Reverb");
        NAMES.put("reverb", "Reverb");
        NAMES.put("freeverb", "Freeverb");
        NAMES.put("zitarev", "Zita Reverb");
        // ---- Compressor / Dynamics ----
        NAMES.put("compressor", "Compressor");
        NAMES.put("multibandcomp", "Multi-Band Compressor");
        NAMES.put("multibandcompressor", "Multi-Band Compressor");
        NAMES.put("expander", "Expander");
        NAMES.put("noisegate", "Noise Gate");
        NAMES.put("denoiser", "Denoiser");
        NAMES.put("denoiser2", "Denoiser");
        NAMES.put("susta", "Sustainer");
        // ---- EQ ----
        NAMES.put("graphiceq", "Graphic EQ");
        NAMES.put("4bandeq", "4-Band EQ");
        NAMES.put("tilttone", "Tilt Tone");
        NAMES.put("equalizer", "Equalizer");
        NAMES.put("hfbrightener", "HF Brightener");
        NAMES.put("hfb", "HF Brightener");
        NAMES.put("tonemender", "Tone Mender");
        NAMES.put("pusatorte", "Fuzz");
        // ---- Wah ----
        NAMES.put("quack", "Quack Autowah");
        NAMES.put("wah", "Wah");
        NAMES.put("wahwah", "Wah-Wah");
        NAMES.put("switchlesswah", "Switchless Wah");
        NAMES.put("colwah", "Wah");
        NAMES.put("autowah", "Autowah");
        // ---- Volume / swell ----
        NAMES.put("slowgear", "Slow Gear");
        NAMES.put("volume", "Volume");
        // ---- Misc / famous individual plugins ----
        NAMES.put("gclipper", "Current Clipper");
        NAMES.put("voodoolab", "Voodoo Lab SuperFuzz");
        NAMES.put("toxic", "Toxic Fuzz");
        // Guitarix Meta amp / other known ids (raw fallback safety)
        NAMES.put("guitarix", "Guitarix");
        NAMES.put("hornet", "Hornet");
        NAMES.put("mole", "Mole");
        NAMES.put("alembic", "Alembic");
        NAMES.put("osmutantes", "Os Mutantes");
        NAMES.put("darkterror", "Dark Terror");
        NAMES.put("tinyterror", "Tiny Terror");
        NAMES.put("littlefly", "Little Fly");
        NAMES.put("bmp", "Big Muff Pi");
        NAMES.put("bigmuff", "Big Muff Pi");
        NAMES.put("axisfacer", "Axis Face Fuzz");
        NAMES.put("hogsfoot", "Hogs Foot");
        NAMES.put("bluesbreaker", "Bluesbreaker");
        NAMES.put("mkii", "Mk II");
        NAMES.put("metal", "Metal");
        NAMES.put("metalhead", "Metal Head");
    }

    /**
     * Normalize a raw plugin id into a comparator used by the dictionary.
     * Drops ".lv2"/".so", all separators, prefixes "Gx"/"gx" then underscores & spaces.
     */
    static String norm(String n) {
        if (n == null) return "";
        String s = n.toLowerCase();
        s = s.replace(".lv2", "").replace(".so", "");
        if (s.startsWith("lib")) s = s.substring(3);
        s = s.replaceAll("[_\\s\\-]+", "");
        if (s.startsWith("gx")) s = s.substring(2);
        // safety: re-apply underscore/space collapse in case prefix trim left one
        s = s.replaceAll("[_\\s\\-]+", "");
        return s.trim();
    }

    /**
     * Returns the cleaned display name for a plugin, else the original string if unknown.
     */
    public static String displayName(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) return pluginName;
        String nice = NAMES.get(norm(pluginName));
        if (nice != null && !nice.isEmpty()) return nice;
        return fallbackClean(pluginName);
    }

    /** Generic cleaner: strips prefixes/separators, splits camel case, capitalizes words. */
    static String fallbackClean(String n) {
        if (n == null || n.isEmpty()) return n;
        String s = n.trim();
        s = s.replaceAll("(?i)\\.(so|lv2|so\\.lv2)$", "");
        s = s.replaceFirst("(?i)^the infamous\\s+", "");
        s = s.replaceFirst("(?i)^rkr\\s+", "");
        s = s.replaceFirst("(?i)^mda\\s+", "");
        s = s.replace('_', ' ');
        s = s.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        s = s.replaceFirst("(?i)^Gx\\s+", "");
        s = s.replaceFirst("(?i)^Gx", "").trim();
        s = s.replaceAll("\\s{2,}", " ").trim();
        return capitalizeWords(s);
    }

    static String capitalizeWords(String s) {
        if (s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            String lw = w.toLowerCase();
            if (isAllCapsToken(lw)) sb.append(lw.toUpperCase());
            else sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static boolean isAllCapsToken(String word) {
        return word.length() <= 4 && (word.equals("eq") || word.equals("ir") || word.equals("ui")
                || word.equals("od") || word.equals("sd") || word.equals("ts") || word.equals("db")
                || word.equals("sv") || word.equals("mk") || word.equals("jcm") || word.equals("nam")
                || word.equals("aida") || word.equals("dim"));
    }
}