package com.kancrutt.gigginggear;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AudioEngine {
    private static final long THROTTLE_INTERVAL_MS = 250;
    private static final Map<String, Long> lastCallTime = new HashMap<>();
    private static final Map<String, Float> lastCallValue = new HashMap<>();
    // Measurement stats
    private static int totalCalls = 0;
    private static int nativeCalls = 0;
    private static int throttledCalls = 0;
    private static long firstCallTime = 0;
    private static long lastCallTimeMs = 0;
    private static long minNativeIntervalMs = Long.MAX_VALUE;

    private static synchronized float getWithThrottle(int plugin, int control, boolean passive) {
        String key = plugin + "," + control;
        if (passive) {
            long now = System.currentTimeMillis();
            if (firstCallTime == 0) firstCallTime = now;
            lastCallTimeMs = now;
            totalCalls++;
            Long last = lastCallTime.get(key);
            if (last != null && now - last < THROTTLE_INTERVAL_MS) {
                Float cached = lastCallValue.get(key);
                if (cached != null) {
                    throttledCalls++;
                    return cached;
                }
            }
            lastCallTime.put(key, now);
            nativeCalls++;
            // Track minimum interval between native calls
            if (lastCallTimeMs > 0) {
                long interval = now - lastCallTimeMs;
                if (interval < minNativeIntervalMs) minNativeIntervalMs = interval;
            }
            lastCallTimeMs = now;
            float val = getActivePluginValue(plugin, control);
            lastCallValue.put(key, val);
            return val;
        } else {
            totalCalls++;
            nativeCalls++;
            long now = System.currentTimeMillis();
            if (lastCallTimeMs > 0) {
                long interval = now - lastCallTimeMs;
                if (interval < minNativeIntervalMs) minNativeIntervalMs = interval;
            }
            lastCallTimeMs = now;
            return getActivePluginValue(plugin, control);
        }
    }

    public static synchronized void resetThrottleStats() {
        totalCalls = 0;
        nativeCalls = 0;
        throttledCalls = 0;
        firstCallTime = 0;
        lastCallTimeMs = 0;
        minNativeIntervalMs = Long.MAX_VALUE;
    }

    public static synchronized String getThrottleStats() {
        if (firstCallTime == 0) return "no calls yet";
        long elapsedMs = lastCallTimeMs - firstCallTime;
        if (elapsedMs <= 0) elapsedMs = 1;
        double callsPerSec = totalCalls * 1000.0 / elapsedMs;
        double nativePerSec = nativeCalls * 1000.0 / elapsedMs;
        long minInterval = (minNativeIntervalMs == Long.MAX_VALUE) ? 0 : minNativeIntervalMs;
        return String.format(
            "total=%d native=%d throttled=%d elapsed=%.0fs calls/s=%.1f native/s=%.1f min_interval=%dms",
            totalCalls, nativeCalls, throttledCalls, elapsedMs / 1000.0,
            callsPerSec, nativePerSec, minInterval
        );
    }

    public static float getActivePluginValueThrottled(int plugin, int control) {
        return getWithThrottle(plugin, control, true);
    }

    public static void clearThrottleCache() {
        lastCallTime.clear();
        lastCallValue.clear();
    }

    public static synchronized void addNativeCallInterval(long now) {
        // Track minimum interval between native calls
    }

    public static void printThrottleStats() {
        Log.d("AudioEngine", "THROTTLE_STATS=" + getThrottleStats());
    }

    public static native void loadLibrary (String filename);
    public static native void loadPlugins ();
    public static native int getSharedLibraries ();
    public static native void setLazyLoad (boolean lazyLoad);
    public static native String getLibraryName (int library) ;
    public static native int getPlugins (int library) ;
    public static native int getTotalPlugins ();
    public static native String getPluginName (int library, int plugin);
    public static native int getPluginUniqueID (int library, int plugin);
    // activePlugins
    public static native int getActivePlugins ();
    public static native float [] getActivePluginValues (int plugin);
    public static native float getActivePluginValue (int plugin, int control);
    public static native float getActivePluginValueByIndex (int plugin, int control);
    public static native int getActivePluginID (int plugin);
    public static native String getActivePluginUri (int plugin);
    public static native int getPluginControls (int plugin) ;
    public static native boolean getFilePort (int plugin);
    public static native void setFilePortValue (int plugin, String filename);
    public static native void setFileName (String fileName);
    public static native void setAtomPort (int plugin, int control, String text);
    public static native float [] getPluginControlValues (int plugin, int control) ;
    public static native float getPluginPresetValue (int plugin, int control) ;
    public static native boolean getControlIsLogarithmic (int plugin, int control);
    public static native String getControlName (int plugin, int control) ;
    public static native int getControlType (int plugin, int control);
    public static native String getControlUnit (int plugin, int control);
    public static native String [] getControlEnumLabels (int plugin, int control);
    public static native float [] getControlEnumValues (int plugin, int control);
    public static native float getControlDefault (int plugin, int control);
    public static native int getControlPortIndex (int plugin, int control);
    public static native void setParameterMetadata (String json);
    public static native String getActivePluginName (int plugin);

    public static native String getRecordingFileName ();
    public static native void setLowLatency (boolean lowLatency) ;
    public static native void setSampleRate (int sampleRate);

    // return active plugin *ID*
    public static native int addPlugin (int library, int plugin) ;
    public static native int addPluginLazy (String library, int plugin);
    public static native int addPluginLazyLV2(String library, int plugin);
    public static native int addPluginByName (String name);
    public static native boolean deletePlugin (int plugin) ;
    public static native int addRoutingBlock (boolean isSplit);
    public static native int insertRoutingBlock (int index, boolean isSplit);
    public static native void clearActiveQueue ();
    public static native void toggleRecording (boolean state) ;

    public static native void setPluginControl (int plugin, int control, float value);
    public static native void setPluginControlByIndex (int plugin, int control, float value);
    public static native void setPresetValue (int plugin, int control, float value);
    public static native int movePlugin (int plugin, int position) ;
    public static native int movePluginUp (int plugin) ;
    public static native int movePluginDown (int plugin) ;

    public static native boolean togglePlugin (int plugin, boolean state) ;
    public static native boolean getActivePluginEnabled (int plugin);
    public static native void bypass (boolean state) ;
    public static native void pause (boolean state) ;

    public static native void setExportFormat (int format);

    public static native void debugInfo ();
    public static native boolean getTunerEnabled () ;
    public static native void setTunerEnabled (boolean enabled) ;
    public static native void setExternalStoragePath (String path) ;
    public static native void setRecordingActive (boolean active) ;
    public static native void setOpusBitRate (int bitrate);
    public static native void setAttributionTag (String tag);

    public static native boolean create() ;
    public static native boolean wasLowLatency ();
    public static native boolean isAAudioRecommended () ;
    public static native boolean setAPI(int apiType);
    public static native boolean setEffectOn(boolean isEffectOn);
    public static native void popFunction ();
    public static native void setRecordingDeviceId(int deviceId);
    public static native void setPlaybackDeviceId(int deviceId);
    public static native void delete();
    public static native void native_setDefaultStreamValues(int defaultSampleRate, int defaultFramesPerBurst);

    public static native void setLibraryPath (String path);
    public static native void setInputVolume (float volume);
    public static native void setOutputVolume (float volume);
    public static native float getInputVolume();
    public static native float getOutputVolume();
    public static native void toggleMixer (boolean toggle);
    public static native void printActiveChain ();

    public static native void testLV2 ();
    public static native void setPluginBuffer (float  [] data, int plugin);
    public static native void setPluginBufferStereo (float  [] data_left, float [] data_right, int plugin);
    public static native void setRoutingMode (int mode);
    public static native int getRoutingMode ();
    public static native void setGraph (int[] from, int[] to);
    public static native int[] getGraphEdges ();
    public static native String setPluginFilename (String filename, int plugin);
    /** Engine truth: can a model file actually attach to this slot? Use for all UI gating (labels, badges, targeting). */
    public static native boolean canLoadModelFile (int plugin);
    /** Engine-level read-back: returns the model file actually connected to the plugin, or null if none. */
    public static native String getPluginLoadedFileName (int plugin);
    public static native String tuneLatency ();

    static void setDefaultStreamValues(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            int defaultSampleRate = Integer.parseInt(sampleRateStr);
            String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);

            native_setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst);
        }
    }

    static ProgressDialog progress ;
    static void showProgress (Context context) {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setIndeterminate(true);
//        progress.setIcon(R.drawable.logo);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
    }

    void setInputMeter (float value) {

    }

    static void hideProgress () {
        progress.hide();
    }

    static void warnLowLatency (Context context) {
        Log.d("AudioEngine", "warnLowLatency: unable to get low latency");
//        MainActivity.toast(context.getResources().getString(R.string.lowLatencyWarning));
    }

    public static native int getSampleRate () ;
    public static native void toggleVideoRecording (boolean toggle);
    public static native long getTimeStamp () ;
    public static native double getLatency (boolean input) ;
    public static native void setMainActivityClassName (String className);
    public static native void pushToLockFreeBeforeOutputVolumeAaaaaargh (boolean setting) ;
    public static native void setLamePreset (int preset);
    public static native int getActiveEnabledPlugins ();
    public static native int getBufferSizeInFrames (boolean input) ;
    public static native void fixGlitches ();
    public static native void minimizeLatency ();
    public static native void setBufferSizeFactor (float factor);
    public static native void latencyTuner ();
}
