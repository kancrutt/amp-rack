package com.kancrutt.gigginggear.util

/**
 * Zero-Trust helper for control labels across all effect blocks.
 * Converts raw native names (camelCase / PascalCase / snake_case / concatenated lower)
 * into proper Title Case with spaces, e.g. "Wowdepth" -> "Wow Depth", "Fluterfreq" -> "Flutter Freq".
 *
 * Applied globally in MainActivity.populateInlineParameterEditor for every block type.
 */
object LabelFormatter {

    // Common audio-term dictionary for splitting concatenated strings
    // (flat lower, Title, or ALLCAPS). Sorted longest first for greedy matching.
    // Every entry is evidenced by real plugin port names (LADSPA/LV2 audit).
    private val KNOWN_WORDS = listOf(
        "frequency", "resonance", "flutter", "fluter", "feedback",
        "threshold", "attack", "release", "sustain", "ratio",
        "depth", "amount", "level", "drive", "gain", "volume",
        "tone", "bass", "middle", "mid", "treble", "presence",
        "mix", "blend", "delay", "time", "rate", "speed", "wow",
        "freq", "cutoff", "filter", "lfo", "hpf", "lpf", "mod",
        "input", "output", "bypass", "enable", "enabled", "phase",
        "stereo", "mono", "width", "pan", "balance", "damping",
        "diffusion", "size", "decay", "predelay", "reverb", "room",
        "select", "model", "style", "boost", "tube", "harmonics",
        "direct", "sample", "space", "bright", "warmth", "colour",
        "color", "noise", "pulse", "shape", "stages", "stage",
        "octave", "pitch", "shift", "channel", "crossover",
        "irfile", "file", "top", "amp", "ir", "cab", "pre", "post",
        "dry", "wet", "midfreq",
        // Compound halves evidenced in the wild: BASSBOOST, HIGHPASS,
        // CABSWITCH, DRYWET, REVERBLEVEL, SWITCHFREQ, Eqbypass,
        // MAKEUPTHRESHOLD, HOSTBPM, PREDELAYMS, LOWHIGHCUT,
        // FEEDFORWARDGAIN, TONESTACK, PEAKGAIN, HIGH/LOWCUT.
        "switch", "makeup", "forward", "overdrive",
        "high", "low", "pass", "peak", "stack", "wave", "host",
        "over", "feed", "down", "cut", "up", "bpm", "ms",
        // Evidenced by audit sweep: DCBLOCKER, MAXLEVEL, MINLEVEL, NETBYPASS.
        // Full-coverage rule keeps "Maximum"/"Minimum"/"Model" whole.
        "max", "min", "net", "blocker",
        // Evidenced by neural-amp params: EQPOS, MTYPE, Suboctave.
        "pos", "type", "sub", "octave",
        // Standalone acronyms that must survive as whole words and never
        // reach the single-letter affix rules ("MIDI" -> "Mid I",
        // "RMS" -> "R MS").
        "midi", "rms", "bpf"
    ).sortedByDescending { it.length }

    // Short acronyms allowed INSIDE concatenated tokens (eq+bypass, ps+dry+wet).
    // Kept minimal: 2-letter fragments only join when the WHOLE token is
    // covered, so real words ("measure", "detune") never split.
    private val SPLIT_ACRONYMS = setOf("eq", "ps", "mrb", "ms", "dc")

    // Words always rendered fully uppercased (audio acronyms).
    private val ACRONYMS = setOf(
        "lfo", "hpf", "lpf", "bpf", "eq", "bpm", "ms", "ir",
        "hf", "lf", "rt60", "dd", "ps", "mrb", "midi", "rms",
        "cab", "fx", "am", "fm", "cc", "pg", "vcf", "vca", "vco", "dc"
    )

    // Typo corrections and canonical replacements (whole-token, checked
    // before splitting so "overdrive" never becomes "Over Drive").
    private val TYPO_MAP = mapOf(
        "fluter" to "Flutter",
        "irfile" to "IR File",
        "ir_file" to "IR File",
        "irfile1" to "IR File 1",
        "irfile2" to "IR File 2",
        "indicaton" to "Indication",
        "topboost" to "Top Boost",
        "ampselect" to "Amp Select",
        "midfreq" to "Mid Freq",
        "ffeedback" to "Feedback",
        "relese" to "Release",
        "treshold" to "Threshold",
        "presense" to "Presence",
        "thresh" to "Threshold",
        "preamp" to "Preamp",
        "overdrive" to "Overdrive",
        "hi" to "High",
        "lo" to "Low",
        // Real upstream typo in rkr Shifter ("Thresold" next to Attack/Decay).
        "thresold" to "Threshold",
        // Gxdigital_delay "HOWPASS" sits next to HIGHPASS: the low-pass mate.
        "howpass" to "Low Pass",
        // R19 TEMUAN #5 (Hery): unclear/abbreviated port names made readable.
        // "MIDS" (GxUltraCab) was being shredded into the broken "Mid S".
        "mids" to "Mid",
        // C* Amp VTS / CabIV / Compress "Over" = the overdrive stage.
        "over" to "Overdrive",
        "over2" to "Overdrive 2",
        // "Fc" is the universal LADSPA cutoff-frequency short form.
        "fc" to "Cutoff Freq"
    )

    /** Split result: main label for the UI + raw enum hint for helper text/tooltip. */
    data class ControlLabel(val main: String, val hint: String?)

    /**
     * Zero-Trust control parser (global, all blocks A-Z).
     * LADSPA embeds enums in the port name ("Modulation depth (0=none, 1=AM, 2=RM)")
     * because LADSPA has no scale-points. The parenthetical is NOT part of the name:
     * it is stripped from the main label (case preserved) and returned as [hint]
     * for helper text / tooltip / enum-picker labels.
     */
    @JvmStatic
    fun parseControlLabel(raw: String?): ControlLabel {
        if (raw == null) return ControlLabel("", null)
        // Port names commonly carry display annotations such as [dB], [%],
        // or [ms]. Keep the annotation visible, but use parentheses and
        // canonical unit spelling so the label never becomes "d B".
        var s = normalizeUnitAnnotations(raw.trim())
        if (s.isEmpty()) return ControlLabel(s, null)

        // Peel trailing annotation groups — "(...)" / "[...]" — repeatedly.
        // Each group is classified, so mid-string units never reach the
        // cosmetic pass that would lowercase them ("(Hz)" -> "(hz)") or
        // shred them ("[-3dB]" -> "[ 3 D B]"):
        //   * contains "="   -> enum hint (first one wins)
        //   * pure unit      -> canonical unit suffix (first one wins)
        //   * anything else  -> kept verbatim ("[-3dB]", "(square->sine->tri)")
        val tailGroups = mutableListOf<String>()
        while (true) {
            val g = Regex("\\s*(\\([^()]*\\)|\\[[^\\]]*\\])\\s*$").find(s) ?: break
            tailGroups.add(0, g.groupValues[1])
            s = s.removeRange(g.range).trim()
            if (s.isEmpty()) break
        }
        var hint: String? = null
        var unitSuffix: String? = null
        val keepGroups = mutableListOf<String>()
        for (g in tailGroups) {
            val inner = g.substring(1, g.length - 1).trim()
            if ('=' in inner) {
                if (hint == null) hint = inner.ifEmpty { null } else keepGroups.add(g)
                continue
            }
            val u = parseUnitToken(inner)
            if (u != null) {
                if (unitSuffix == null) unitSuffix = u else keepGroups.add(g)
                continue
            }
            keepGroups.add(g)
        }
        // Fallback: a trailing bare % sign ("Mix %")
        if (unitSuffix == null) {
            val percentMatch = Regex("\\s*%\\s*$").find(s)
            if (percentMatch != null) {
                unitSuffix = "%"
                s = s.removeRange(percentMatch.range).trim()
            }
        }
        // The unit is NOT re-appended to the label: values render the unit
        // beside the number (ParameterFormatter, "0.5dB"), so keeping "(dB)"
        // in the label duplicated it ("Dry Level (dB)" + "0.5dB").
        val tail = buildString {
            for (g in keepGroups) append(" ").append(g)
        }
        if (s.isEmpty()) return ControlLabel(tail.trim(), hint)
        return ControlLabel((formatControlLabel(s) + tail).trim(), hint)
    }

    /**
     * Strict unit classifier: pure unit tokens only. Anything else
     * (e.g. "-3dB", "square->sine->tri") returns null so it is kept verbatim.
     */
    private fun parseUnitToken(inner: String): String? {
        return when (inner.replace("\\s".toRegex(), "").lowercase()) {
            "db" -> "dB"
            "khz" -> "kHz"
            "hz" -> "Hz"
            "ms" -> "ms"
            "bpm" -> "BPM"
            "%" -> "%"
            "s" -> "s"
            "inch", "inches" -> "inches"
            "semitone", "semitones" -> "semitones"
            "deg" -> "deg"
            else -> null
        }
    }

    private fun normalizeUnitAnnotations(value: String): String {
        return value.replace(Regex("\\[\\s*(d\\s*b|k\\s*hz|h\\s*z|m\\s*s|b\\s*pm|%|s|inches?|semitones?|deg)\\s*\\]", RegexOption.IGNORE_CASE)) { match ->
            val unit = canonicalUnit(match.groupValues[1])
            "(" + unit + ")"
        }
    }

    private fun canonicalUnit(value: String): String {
        return when (value.replace("\\s".toRegex(), "").lowercase()) {
            "db" -> "dB"
            "khz" -> "kHz"
            "hz" -> "Hz"
            "ms" -> "ms"
            "bpm" -> "BPM"
            "%" -> "%"
            "s" -> "s"
            "inch", "inches" -> "inches"
            "semitone", "semitones" -> "semitones"
            else -> value.trim()
        }
    }

    /**
     * Parses an enum hint ("0=none, 1=AM, 2=RM") into ordered labels indexed by
     * integer value. Returns null when the hint is not a clean 0..N or min..max
     * sequence. Label case is PRESERVED (AM stays AM, not Am).
     */
    @JvmStatic
    fun parseEnumHint(hint: String?, minVal: Float, maxVal: Float): Array<String>? {
        if (hint == null) return null
        val parts = hint.split(Regex("[,;]")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val pairs = parts.map { p ->
            val kv = p.split(Regex("="), limit = 2)
            if (kv.size != 2) return null
            val idx = kv[0].trim().toIntOrNull() ?: return null
            idx to kv[1].trim().ifEmpty { return null }
        }
        val lo = minVal.toInt()
        val expected = (lo..maxVal.toInt()).toList()
        if (pairs.map { it.first } != expected) return null
        return pairs.map { it.second }.toTypedArray()
    }

    /**
     * True when the labels are exactly an Off/On pair in any order, case or
     * surrounding whitespace ("Off"/"On", "off"/"on", "OFF"/"ON").
     * Guitarix-style 0..1 ENUMs carrying such labels render as toggles.
     * Anything else (single label, 3+ options, other words) stays a dropdown.
     */
    @JvmStatic
    fun isOnOffPair(labels: Array<String>?): Boolean {
        if (labels == null || labels.size != 2) return false
        val norm = labels.map { it.trim().lowercase() }
        if (norm.any { it.isEmpty() }) return false
        return norm.toSet() == setOf("off", "on")
    }

    @JvmStatic
    fun formatControlLabel(raw: String?): String {
        if (raw == null) return ""
        var s = raw.trim()
        if (s.isEmpty()) return s

        // Slash-separated compounds keep their slashes: each side is
        // formatted on its own ("Dry/Wet" stays "Dry/Wet", not "Dry/wet").
        if ("/" in s) {
            return s.split("/").joinToString("/") { formatControlLabel(it) }
        }

        // Routing arrows are word separators ("Env->VCF" -> "Env -> VCF").
        // Parked as a placeholder: the generic separator pass would eat the
        // dash, and "<>" pairs must be left alone. Restored at the end.
        s = s.replace("->", " \uE000 ")

        // 1. Preserve special port names (INPUT00 -> Input, etc.) — keep original behavior
        val lowForSpecial = s.lowercase()
        if (lowForSpecial.matches(Regex("^(in|input)\\d*$"))) return "Input"
        if (lowForSpecial.matches(Regex("^(out|output)\\d*$"))) return "Output"
        if (lowForSpecial.matches(Regex("^g?ain\\d*$"))) return "Gain"

        // 2. Normalize separators: _ - . -> space, collapse multiple spaces
        s = s.replace(Regex("[_\\-\\.]+"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()

        // Park standalone dB / RT60 tokens AFTER separator normalization
        // (so LOW_RT60 -> "LOW RT60" parks cleanly; "_" is a word char
        // and would defeat \b before that). The camel/letter-digit
        // splitter would shred them ("dB" -> "d B", "RT60" -> "Rt 60").
        s = s.replace(Regex("(?i)\\bdb\\b"), " \uE001 ")
        s = s.replace(Regex("(?i)\\brt60\\b"), " \uE002 ")

        // 3. Insert spaces for camelCase / PascalCase boundaries
        //    a) lower -> Upper : "wowDepth" -> "wow Depth"
        //    b) Acronym -> Word : "LFOFreq" -> "LFO Freq", "HPFCutoff" -> "HPF Cutoff"
        //    c) letter <-> digit : "Freq1" -> "Freq 1", "1Freq" -> "1 Freq"
        var spaced = s
        spaced = spaced.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        spaced = spaced.replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")
        spaced = spaced.replace(Regex("(?<=[a-zA-Z])(?=[0-9])"), " ")
        spaced = spaced.replace(Regex("(?<=[0-9])(?=[a-zA-Z])"), " ")

        // 4. Split single concatenated tokens (flat lower, Title, or ALLCAPS):
        //    "Eqbypass" -> "EQ Bypass", "BASSBOOST" -> "Bass Boost",
        //    "MAKEUPTHRESHOLD" -> "Makeup Threshold", "highpass" -> "High Pass".
        //    Whole-token canonical forms win first, so "overdrive" stays whole.
        val tokens = spaced.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val expandedTokens = mutableListOf<String>()
        for (token in tokens) {
            val canonical = TYPO_MAP[token.lowercase()]
            if (canonical != null) {
                expandedTokens.add(canonical)
                continue
            }
            // Strict full-coverage split is safe even for short tokens:
            // "MIDQ" -> "Mid Q" while "BASS"/"MID"/"PAN" stay whole.
            if (token.length >= 3 && token.matches(Regex("^[A-Za-z]{3,}$"))) {
                val split = splitConcatenated(token)
                if (split != null) {
                    expandedTokens.addAll(split)
                    continue
                }
            }
            expandedTokens.add(token)
        }
        spaced = expandedTokens.joinToString(" ")

        // 5. Clean up multiple spaces again after splits
        spaced = spaced.replace(Regex("\\s+"), " ").trim()
        if (spaced.isEmpty()) return spaced

        // 6. Title Case each word, preserve acronyms and apply typo corrections
        val words = spaced.split(Regex("\\s+"))
        val titled = words.joinToString(" ") { word ->
            val lower = word.lowercase()
            // Check typo map first (e.g. fluter -> Flutter)
            TYPO_MAP[lower]?.let { return@joinToString it }
            // Preserve known acronyms uppercased
            if (lower == "db") return@joinToString "dB"
            if (lower == "hz") return@joinToString "Hz"
            if (lower == "khz") return@joinToString "kHz"
            if (lower in ACRONYMS) {
                return@joinToString word.uppercase()
            }
            // Fragments stay uppercased only for single initials ("L", "R")
            // or real acronyms ("EQ", "CAB", "LFO", "VCF", "CC").
            // Plain words use Title case ("MID" -> "Mid", "UP" -> "Up").
            if (word.all { it.isUpperCase() }
                && (word.length <= 1 || lower in ACRONYMS)) {
                return@joinToString word.uppercase()
            }
            // Normal Title Case
            lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // 7. Final cleanup: restore placeholders, single spaces, trim
        return titled.replace("\uE000", "->").replace("\uE001", "dB").replace("\uE002", "RT60").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Iteratively splits a concatenated token like "Wowdepth", "BASSBOOST",
     * or "MAKEUPTHRESHOLD1"-style stems via longest-match against the gear
     * dictionary (+ short acronyms). Returns null if no confident split found.
     * STRICT: the WHOLE token must be covered by known words, otherwise null.
     * Real words ("Modulation", "Detune", "Overlap", "Crossfade", "Waveform",
     * "Measure", "Smoothing") are never mangled. Never again "Mod Ulation".
     */
    private fun splitConcatenated(token: String): List<String>? {
        splitInner(token)?.let { return it }
        // Single-letter affixes on ALLCAPS stems (neural-amp style):
        // "BFREQ" -> "B"+"Freq", "MTYPE" -> "M"+"Type", "MIDQ" -> "Mid"+"Q".
        // The rest must still fully tokenize, so "BASS"/"MID"/"PAN" never split.
        if (token.matches(Regex("^[A-Z0-9]{3,}$"))) {
            val head = token.substring(0, token.length - 1)
            val tail = token.substring(token.length - 1)
            if (tail[0].isLetter() && head.length >= 2 && splitInner(head) != null) {
                return listOf(head, tail)
            }
            val lead = token.substring(0, 1)
            val rest = token.substring(1)
            if (lead[0].isLetter() && splitInner(rest) != null) {
                return listOf(lead, rest)
            }
        }
        return null
    }

    private fun splitInner(token: String): List<String>? {
        val lower = token.lowercase()
        if (lower.length < 2) return null
        val vocab = (KNOWN_WORDS + SPLIT_ACRONYMS).sortedByDescending { it.length }
        val parts = mutableListOf<String>()
        var i = 0
        while (i < lower.length) {
            var match: String? = null
            for (w in vocab) {
                if (lower.startsWith(w, i)) {
                    match = w
                    break
                }
            }
            if (match == null) return null
            parts.add(token.substring(i, i + match.length))
            i += match.length
        }
        return parts.ifEmpty { null }
    }

    @JvmStatic
    fun formatForDisplay(raw: String?): String {
        // Alias for Java interop
        return formatControlLabel(raw)
    }
}
