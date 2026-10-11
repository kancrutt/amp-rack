package com.kancrutt.gigginggear.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LabelFormatterTest {
    @Test public void eqBypassKeepsAcronym() {
        assertEquals("EQ Bypass", LabelFormatter.formatControlLabel("Eqbypass"));
        assertEquals("EQ Bypass", LabelFormatter.formatControlLabel("EQBYPASS"));
        assertEquals("EQ Bypass", LabelFormatter.formatControlLabel("eqBypass"));
    }

    @Test public void allcapsCompoundsSplit() {
        assertEquals("Bass Boost", LabelFormatter.formatControlLabel("BASSBOOST"));
        assertEquals("CAB Switch", LabelFormatter.formatControlLabel("CABSWITCH"));
        assertEquals("Dry Wet", LabelFormatter.formatControlLabel("DRYWET"));
        assertEquals("Wet Dry", LabelFormatter.formatControlLabel("WETDRY"));
        assertEquals("High Gain", LabelFormatter.formatControlLabel("HIGHGAIN"));
        assertEquals("High Pass", LabelFormatter.formatControlLabel("HIGHPASS"));
        assertEquals("Low Switch", LabelFormatter.formatControlLabel("LOWSWITCH"));
        assertEquals("Mid Switch", LabelFormatter.formatControlLabel("MIDSWITCH"));
        assertEquals("Reverb Level", LabelFormatter.formatControlLabel("REVERBLEVEL"));
        assertEquals("Room Size", LabelFormatter.formatControlLabel("ROOMSIZE"));
        assertEquals("Switch Freq", LabelFormatter.formatControlLabel("SWITCHFREQ"));
        assertEquals("Down Time", LabelFormatter.formatControlLabel("DOWNTIME"));
        assertEquals("Up Time", LabelFormatter.formatControlLabel("UPTIME"));
        assertEquals("Host BPM", LabelFormatter.formatControlLabel("HOSTBPM"));
        assertEquals("Predelay MS", LabelFormatter.formatControlLabel("PREDELAYMS"));
        assertEquals("MRB Select", LabelFormatter.formatControlLabel("MRBSELECT"));
        assertEquals("PS Dry Wet", LabelFormatter.formatControlLabel("PSDRYWET"));
    }

    @Test public void flatLowerCompoundsSplit() {
        assertEquals("High Pass", LabelFormatter.formatControlLabel("highpass"));
        assertEquals("Low Cut", LabelFormatter.formatControlLabel("lowcut"));
        assertEquals("High Cut", LabelFormatter.formatControlLabel("highcut"));
        assertEquals("Low Pass", LabelFormatter.formatControlLabel("lowpass"));
        assertEquals("Low High Cut", LabelFormatter.formatControlLabel("lowhighcut"));
        assertEquals("Drive Gain", LabelFormatter.formatControlLabel("drivegain"));
        assertEquals("Drive Level", LabelFormatter.formatControlLabel("drivelevel"));
        assertEquals("Peak Gain", LabelFormatter.formatControlLabel("peakgain"));
        assertEquals("Feedback Gain", LabelFormatter.formatControlLabel("feedbackgain"));
        assertEquals("Feed Forward Gain", LabelFormatter.formatControlLabel("feedforwardgain"));
        assertEquals("Tone Stack", LabelFormatter.formatControlLabel("tonestack"));
        assertEquals("Room Size", LabelFormatter.formatControlLabel("roomsize"));
        assertEquals("Wave Shape", LabelFormatter.formatControlLabel("waveshape"));
        assertEquals("Dry Wet", LabelFormatter.formatControlLabel("drywet"));
    }

    @Test public void realWordsNeverSplit() {
        assertEquals("Modulation", LabelFormatter.formatControlLabel("Modulation"));
        assertEquals("Detune", LabelFormatter.formatControlLabel("Detune"));
        assertEquals("Overlap", LabelFormatter.formatControlLabel("Overlap"));
        assertEquals("Crossfade", LabelFormatter.formatControlLabel("Crossfade"));
        assertEquals("Waveform", LabelFormatter.formatControlLabel("Waveform"));
        assertEquals("Measure", LabelFormatter.formatControlLabel("measure"));
        assertEquals("Smoothing", LabelFormatter.formatControlLabel("Smoothing"));
        assertEquals("Overdrive", LabelFormatter.formatControlLabel("overdrive"));
        assertEquals("Overdrive", LabelFormatter.formatControlLabel("Overdrive"));
        assertEquals("Preamp", LabelFormatter.formatControlLabel("preamp"));
        assertEquals("Stereo", LabelFormatter.formatControlLabel("Stereo"));
    }

    @Test public void typosCanonicalized() {
        assertEquals("Feedback", LabelFormatter.formatControlLabel("FFEEDBACK"));
        assertEquals("Release", LabelFormatter.formatControlLabel("RELESE"));
        assertEquals("Threshold", LabelFormatter.formatControlLabel("TRESHOLD"));
        assertEquals("Presence", LabelFormatter.formatControlLabel("PRESENSE"));
        assertEquals("Threshold", LabelFormatter.formatControlLabel("Thresh"));
        assertEquals("S Threshold", LabelFormatter.formatControlLabel("S Thresh"));
        assertEquals("Low Pass", LabelFormatter.formatControlLabel("HOWPASS"));
        assertEquals("Flutter Freq", LabelFormatter.formatControlLabel("FluterFreq"));
    }

    @Test public void acronymsStayUppercase() {
        assertEquals("MIDI Channel", LabelFormatter.formatControlLabel("MIDI Channel"));
        assertEquals("High Gain", LabelFormatter.formatControlLabel("hi.gain"));
        assertEquals("Low Gain", LabelFormatter.formatControlLabel("lo.gain"));
        assertEquals("Low Compress", LabelFormatter.formatControlLabel("lo.compress"));
        assertEquals("Peak/RMS", LabelFormatter.formatControlLabel("Peak/RMS"));
    }

    @Test public void slashCompoundsKeepSlash() {
        assertEquals("Dry/Wet", LabelFormatter.formatControlLabel("Dry/Wet"));
        assertEquals("Dry/Wet Mix", LabelFormatter.formatControlLabel("Dry/wet mix"));
        assertEquals("Wet/Dry Mix", LabelFormatter.formatControlLabel("Wet/Dry Mix"));
        assertEquals("Left/Right", LabelFormatter.formatControlLabel("Left/Right"));
        assertEquals("Rotor/Horn Mix", LabelFormatter.formatControlLabel("Rotor/Horn Mix"));
    }

    @Test public void percentUnitPreserved() {
        // Unit in parentheses survives the cosmetic pass untouched
        assertEquals("Mix (%)", LabelFormatter.formatControlLabel("Mix (%)"));
        assertEquals("Depth [%]", LabelFormatter.formatControlLabel("Depth [%]"));
        // Bare trailing % is preserved as-is by the cosmetic pass ...
        assertEquals("Gain %", LabelFormatter.formatControlLabel("Gain %"));
        // ... while the full parser strips pure unit suffixes so they are not duplicated with value units
        assertEquals("Gain", LabelFormatter.parseControlLabel("Gain %").getMain());
        assertEquals("Wet/Dry Mix", LabelFormatter.parseControlLabel("Wet/Dry Mix (%)").getMain());
        assertEquals("R/Haas Feedback", LabelFormatter.parseControlLabel("R/Haas Feedback [%]").getMain());
    }

    @Test public void arrowsStayReadable() {
        assertEquals("Env -> VCF", LabelFormatter.formatControlLabel("Env->VCF"));
        assertEquals("LFO -> VCF", LabelFormatter.formatControlLabel("LFO->VCF"));
    }

    @Test public void midStringAnnotationsKeptVerbatim() {
        // Real audit findings: pure unit suffixes are stripped from tail groups
        // while non-unit annotations ([-3dB], shapes) are preserved verbatim.
        assertEquals("Frequency [-3dB]",
                LabelFormatter.parseControlLabel("Frequency (Hz) [-3dB]").getMain());
        assertEquals("Frequency [-6dB]",
                LabelFormatter.parseControlLabel("Frequency (Hz) [-6dB]").getMain());
        assertEquals("Amps Gain",
                LabelFormatter.parseControlLabel("Amps gain (dB)").getMain());
        assertEquals("LFO Shape (square->sine->tri)",
                LabelFormatter.parseControlLabel("LFO shape (square->sine->tri)").getMain());
        assertEquals("Depth",
                LabelFormatter.parseControlLabel("Depth [%]").getMain());
    }

    @Test public void bareDbNeverShredded() {
        assertEquals("Zero dB", LabelFormatter.formatControlLabel("Zero dB"));
        assertEquals("dB Limit", LabelFormatter.parseControlLabel("dB limit").getMain());
        assertEquals("Zero dB", LabelFormatter.parseControlLabel("Zero dB").getMain());
    }

    @Test public void auditCompoundsSplit() {
        assertEquals("DC Blocker", LabelFormatter.formatControlLabel("DCBLOCKER"));
        assertEquals("Max Level", LabelFormatter.formatControlLabel("MAXLEVEL"));
        assertEquals("Min Level", LabelFormatter.formatControlLabel("MINLEVEL"));
        assertEquals("Net Bypass", LabelFormatter.formatControlLabel("NETBYPASS"));
        // ... while real words containing max/min/mod stay whole
        assertEquals("Maximum", LabelFormatter.formatControlLabel("Maximum"));
        assertEquals("Minimum", LabelFormatter.formatControlLabel("Minimum"));
        assertEquals("Model", LabelFormatter.formatControlLabel("Model"));
        assertEquals("Modern", LabelFormatter.formatControlLabel("Modern"));
    }

    @Test public void neuralAmpShortPrefixesSplit() {
        // rt-neural-generic params: single-letter band prefixes + typo.
        assertEquals("EQ Pos", LabelFormatter.formatControlLabel("EQPOS"));
        assertEquals("B Freq", LabelFormatter.formatControlLabel("BFREQ"));
        assertEquals("M Freq", LabelFormatter.formatControlLabel("MFREQ"));
        assertEquals("T Freq", LabelFormatter.formatControlLabel("TFREQ"));
        assertEquals("M Type", LabelFormatter.formatControlLabel("MTYPE"));
        assertEquals("Mid Q", LabelFormatter.formatControlLabel("MIDQ"));
        assertEquals("Sub Octave", LabelFormatter.formatControlLabel("Suboctave"));
        assertEquals("Threshold", LabelFormatter.formatControlLabel("Thresold"));
        assertEquals("Pre Gain", LabelFormatter.formatControlLabel("PREGAIN"));
        // ... while short real words never split
        assertEquals("Bass", LabelFormatter.formatControlLabel("BASS"));
        assertEquals("Mid", LabelFormatter.formatControlLabel("MID"));
        assertEquals("Pan", LabelFormatter.formatControlLabel("PAN"));
        assertEquals("Mix", LabelFormatter.formatControlLabel("MIX"));
        assertEquals("Freq", LabelFormatter.formatControlLabel("FREQ"));
    }

    @Test public void acronymWordsNeverShredded() {
        assertEquals("MIDI Channel", LabelFormatter.formatControlLabel("MIDI Channel"));
        assertEquals("Peak/RMS", LabelFormatter.formatControlLabel("Peak/RMS"));
        assertEquals("Low RT60", LabelFormatter.formatControlLabel("LOW_RT60"));
        assertEquals("Mid RT60", LabelFormatter.formatControlLabel("MID_RT60"));
        assertEquals("EQ 1 Freq", LabelFormatter.formatControlLabel("EQ1_FREQ"));
    }

    @Test public void numberedParamsKeepIndex() {
        assertEquals("Attack 1", LabelFormatter.formatControlLabel("ATTACK1"));
        assertEquals("Makeup Threshold 1", LabelFormatter.formatControlLabel("MAKEUPTHRESHOLD1"));
        assertEquals("Room Size 2", LabelFormatter.formatControlLabel("ROOMSIZE2"));
        assertEquals("Octave 1", LabelFormatter.formatControlLabel("OCTAVE1"));
    }

    @Test public void onOffPairDetection() {
        assertTrue(LabelFormatter.isOnOffPair(new String[]{"Off", "On"}));
        assertTrue(LabelFormatter.isOnOffPair(new String[]{"On", "Off"}));
        assertTrue(LabelFormatter.isOnOffPair(new String[]{"off", "on"}));
        assertTrue(LabelFormatter.isOnOffPair(new String[]{"OFF", "ON"}));
        assertTrue(LabelFormatter.isOnOffPair(new String[]{"  Off ", "On\t"}));
        assertFalse(LabelFormatter.isOnOffPair(null));
        assertFalse(LabelFormatter.isOnOffPair(new String[]{"Off"}));
        assertFalse(LabelFormatter.isOnOffPair(new String[]{"Off", "On", "Auto"}));
        assertFalse(LabelFormatter.isOnOffPair(new String[]{"On", "On"}));
        assertFalse(LabelFormatter.isOnOffPair(new String[]{"Yes", "No"}));
        assertFalse(LabelFormatter.isOnOffPair(new String[]{"", "On"}));
    }
}
