package com.kancrutt.gigginggear;

import android.content.Context;
import android.util.Log;

/**
 * Manages low-level audio engine lifecycle and configuration via AudioEngine JNI.
 * Extracted from MainActivity to decompose God Activity.
 */
public class AudioEngineManager {
    private static final String TAG = "AudioEngineManager";

    private final Context context;
    private boolean engineCreated = false;
    private boolean effectActive = false;

    public AudioEngineManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void initEngine(boolean lazyLoad) {
        if (!engineCreated) {
            AudioEngine.create();
            AudioEngine.setLibraryPath(context.getApplicationInfo().nativeLibraryDir);
            AudioEngine.setLazyLoad(lazyLoad);
            engineCreated = true;
            Log.i(TAG, "Audio engine created with lazyLoad=" + lazyLoad);
        }
    }

    public void startEngine() {
        if (engineCreated) {
            effectActive = AudioEngine.setEffectOn(true);
            Log.i(TAG, "Audio engine started: " + effectActive);
        }
    }

    public void stopEngine() {
        if (engineCreated) {
            AudioEngine.setEffectOn(false);
            effectActive = false;
            Log.i(TAG, "Audio engine stopped.");
        }
    }

    public void destroyEngine() {
        if (engineCreated) {
            AudioEngine.delete();
            engineCreated = false;
            effectActive = false;
            Log.i(TAG, "Audio engine destroyed.");
        }
    }

    public boolean isEngineCreated() {
        return engineCreated;
    }

    public boolean isEffectActive() {
        return effectActive;
    }
}
