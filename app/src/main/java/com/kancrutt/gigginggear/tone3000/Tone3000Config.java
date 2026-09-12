package com.kancrutt.gigginggear.tone3000;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * TONE3000 configuration — publishable client_id only.
 * Secret key (t3k_cs_...) is NEVER embedded in the app (server-only).
 *
 * Configuration precedence:
 *  1. BuildConfig field TONE3000_CLIENT_ID (gradle -P or local.properties)
 *  2. AndroidManifest meta-data "com.kancrutt.gigginggear.TONE3000_CLIENT_ID"
 *  3. String resource fallback (if present)
 *
 * If none is configured, the integration infrastructure still builds and the
 * entry UI will surface a single missing-config value message (no mock data).
 */
public final class Tone3000Config {
    private static final String TAG = "Tone3000Config";

    public static final String BASE_URL = "https://www.tone3000.com";
    public static final String AUTHORIZE_URL = BASE_URL + "/api/v1/oauth/authorize";
    public static final String TOKEN_URL = BASE_URL + "/api/v1/oauth/token";
    public static final String API_BASE = BASE_URL + "/api/v1";

    // Deep-link callback — scheme chosen to match spec example, namespaced by app id.
    // Registered in AndroidManifest as an intent-filter on MainActivity (singleTop).
    // Keep this in sync with Tone3000AuthManager.REDIRECT_URI and manifest.
    public static final String REDIRECT_SCHEME = "gigginggear";
    public static final String REDIRECT_HOST = "tone3000";
    public static final String REDIRECT_PATH = "/callback";
    public static final String REDIRECT_URI = REDIRECT_SCHEME + "://" + REDIRECT_HOST + REDIRECT_PATH;

    // Publishable key placeholder — MUST be replaced via build config / manifest meta-data.
    // Do NOT hardcode a real t3k_cs_... secret here.
    private static String cachedClientId = null;

    private Tone3000Config() {}

    public static String getClientId(Context ctx) {
        if (cachedClientId != null) return cachedClientId;
        // 1. Try BuildConfig via reflection (avoids hard compile-time dependency)
        try {
            Class<?> bc = Class.forName("com.kancrutt.gigginggear.BuildConfig");
            Object v = bc.getField("TONE3000_CLIENT_ID").get(null);
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (!s.isEmpty() && !s.equals("null") && !s.startsWith("__")) {
                    cachedClientId = s;
                    return s;
                }
            }
        } catch (Throwable ignore) {}
        // 2. Manifest meta-data
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            Bundle md = ai.metaData;
            if (md != null) {
                String v = md.getString("com.kancrutt.gigginggear.TONE3000_CLIENT_ID");
                if (v != null) {
                    v = v.trim();
                    if (!v.isEmpty() && !v.startsWith("__")) {
                        cachedClientId = v;
                        return v;
                    }
                }
            }
        } catch (Throwable ignore) {}
        // 3. Not configured
        Log.w(TAG, "TONE3000_CLIENT_ID not configured — set BuildConfig TONE3000_CLIENT_ID or manifest meta-data");
        cachedClientId = "";
        return "";
    }

    public static boolean isConfigured(Context ctx) {
        String id = getClientId(ctx);
        return id != null && !id.isEmpty();
    }

    /** For tests / debug: reset cache after changing meta-data. */
    static void resetCache() { cachedClientId = null; }
}
