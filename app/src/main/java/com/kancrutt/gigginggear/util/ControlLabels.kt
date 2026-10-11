package com.kancrutt.gigginggear.util

/**
 * R19 TEMUAN #5 (Hery): plugin-scoped display fixes for port names that are
 * too short to read on their own. Global expansions live in [LabelFormatter]
 * (TYPO_MAP); this layer only handles names whose meaning depends on the
 * plugin — a bare "T" is Time on C* Chorus I but Type/Tilt/Tone elsewhere,
 * so it can never be rewritten globally.
 */
object ControlLabels {

    /**
     * Returns the display label for [main] (already LabelFormatter-parsed)
     * on plugin [pluginName], or [main] unchanged when no rule matches.
     */
    @JvmStatic
    fun display(pluginName: String?, main: String?): String? {
        if (main == null || main.isEmpty()) return main
        val key = main.trim().lowercase()
        val p = (pluginName ?: "").lowercase()
        // C* Chorus I port "t (ms)" -> main "T": the delay time knob.
        if (key == "t" && (p.contains("chorusi") || p.contains("c* chorus"))) {
            return "Time"
        }
        return main
    }
}
