package com.kancrutt.gigginggear.tone3000;

import android.content.Context;
import android.util.Log;

import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.ModelDto;
import com.kancrutt.gigginggear.tone3000.model.ToneDto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Authenticated HTTP client for TONE3000 REST API.
 * Separation: UI -> Repository -> Client -> HTTP.
 * Do NOT call from UI thread; callers should use background executors.
 * Handles 401 -> refresh -> retry once; 429/5xx surfaced as typed errors.
 */
public final class Tone3000ApiClient {
    private static final String TAG = "Tone3000Api";
    private final OkHttpClient http;
    private final Context appCtx;

    public Tone3000ApiClient(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        this.http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // -- GET /tones/{id} --

    public ToneDto getTone(String toneId) throws ApiException {
        if (toneId == null || toneId.isEmpty()) throw new ApiException(0, "missing_tone_id", "toneId is empty");
        // Try A2 first, then legacy (no arch), then A1 — fixes "Not supported" for A2-only tones
        String[] archs = new String[]{"2", null, "1"};
        ApiException last = null;
        for (String arch : archs) {
            String url = Tone3000Config.API_BASE + "/tones/" + enc(toneId);
            if (arch != null) url += "?architecture=" + enc(arch);
            try {
                JSONObject o = getJson(url);
                return ToneDto.fromJson(o);
            } catch (ApiException e) {
                last = e;
                // "Not supported" is 400/404 with architecture mismatch — try next arch
                if (e.httpCode == 400 || e.httpCode == 404) {
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("not supported") || msg.contains("architecture") || e.errorCode.contains("architecture") || e.httpCode == 404) {
                        continue;
                    }
                }
                // For other errors (401/403/429/5xx/network), don't retry different arch
                if (e.httpCode == 401 || e.httpCode == 403 || e.httpCode == 429 || e.httpCode >= 500 || e.httpCode == 0) throw e;
                if (arch == null) throw e; // last legacy try failed
                // else try next
            }
        }
        if (last != null) throw last;
        throw new ApiException(404, "not_found", "Tone not found: " + toneId);
    }

    public ToneDto getTone(long toneId) throws ApiException { return getTone(String.valueOf(toneId)); }

    // -- GET /models?tone_id=... --

    public static final class ModelsPage {
        public final List<ModelDto> data;
        public final int page;
        public final int pageSize;
        public final int total;
        ModelsPage(List<ModelDto> data, int page, int pageSize, int total) { this.data=data; this.page=page; this.pageSize=pageSize; this.total=total; }
    }

    public ModelsPage listModels(String toneId, int page, int pageSize) throws ApiException {
        if (toneId == null || toneId.isEmpty()) throw new ApiException(0, "missing_tone_id", "toneId required for listModels");
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 300) pageSize = 300;
        // Support both NAM A1 and A2: try A2 first (modern), then legacy A1+Custom, merge unique models
        // This fixes "Not supported — architecture requirements" for A2-only tones when app requests without architecture
        java.util.LinkedHashMap<Long, ModelDto> merged = new java.util.LinkedHashMap<>();
        int total = 0;
        String[] archAttempts = new String[]{"2", null, "1", "custom"};
        for (String arch : archAttempts) {
            String url = Tone3000Config.API_BASE + "/models?tone_id=" + enc(toneId) + "&page=" + page + "&page_size=" + pageSize;
            if (arch != null) url += "&architecture=" + enc(arch);
            JSONObject root;
            try {
                root = getJson(url);
            } catch (ApiException e) {
                // 404/400 for arch that has no models — try next arch, don't fail whole request
                if (e.httpCode == 404 || e.httpCode == 400) continue;
                // For first arch attempt, if it fails with other error, propagate; otherwise continue
                if (merged.isEmpty()) throw e;
                continue;
            }
            // API paginates as { data: [...], page, page_size, total } or { data, pagination }
            JSONArray arr = null;
            if (root.has("data") && !root.isNull("data")) {
                try { arr = root.getJSONArray("data"); } catch (Exception ignore) {}
            }
            if (arr == null && root.has("models") && !root.isNull("models")) {
                try { arr = root.getJSONArray("models"); } catch (Exception ignore) {}
            }
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject jo = arr.getJSONObject(i);
                        ModelDto m = ModelDto.fromJson(jo);
                        if (!merged.containsKey(m.id)) merged.put(m.id, m);
                    } catch (ApiException ae) {
                        Log.w(TAG, "skip malformed model at " + i + ": " + ae.getMessage());
                    } catch (Exception e) {
                        Log.w(TAG, "skip model parse error at " + i, e);
                    }
                }
            }
            int tp = root.optInt("total", 0);
            if (root.has("pagination") && !root.isNull("pagination")) {
                try { JSONObject pg = root.getJSONObject("pagination"); tp = pg.optInt("total", tp); } catch (Exception ignore) {}
            }
            total = Math.max(total, tp);
            // If we already have models, we can break after trying A2 + legacy; but continue to cover all archs to be safe
            // For efficiency, if we got models and arch was 2 (A2), also try legacy without arch to cover A1
            // We loop through all archAttempts anyway (A2, null, 1, custom) — cheap, only 4 requests max
        }
        if (merged.isEmpty()) {
            // No models found for any architecture — return empty but don't throw
            return new ModelsPage(new ArrayList<>(), page, pageSize, 0);
        }
        return new ModelsPage(new ArrayList<>(merged.values()), page, pageSize, Math.max(total, merged.size()));
    }

    // -- GET single model --

    public ModelDto getModel(String modelId) throws ApiException {
        String url = Tone3000Config.API_BASE + "/models/" + enc(modelId);
        JSONObject o = getJson(url);
        return ModelDto.fromJson(o);
    }

    // -- low-level authenticated GET json with refresh-on-401 --

    private JSONObject getJson(String url) throws ApiException {
        return getJsonWithRetry(url, true);
    }

    private JSONObject getJsonWithRetry(String url, boolean allowRefresh) throws ApiException {
        String token;
        try {
            token = Tone3000AuthManager.get(appCtx).getValidAccessToken(appCtx);
        } catch (ApiException ae) {
            // propagate auth_required
            throw ae;
        }
        Request req = new Request.Builder().url(url).get()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (resp.code() == 401 && allowRefresh) {
                Log.d(TAG, "401 on " + url + " -> refreshing token once");
                try {
                    Tone3000AuthManager.get(appCtx).refresh(appCtx);
                } catch (ApiException re) {
                    throw new ApiException(401, "auth_required", "Session expired — please re-authenticate: " + re.getMessage(), re);
                }
                return getJsonWithRetry(url, false);
            }
            if (!resp.isSuccessful()) {
                String ec = extractError(body, resp.code());
                String msg = "HTTP " + resp.code() + " on " + url + " : " + body;
                if (resp.code() == 404) throw new ApiException(404, ec, "Not found: " + url);
                if (resp.code() == 403) throw new ApiException(403, ec, "Forbidden: " + url);
                if (resp.code() == 429) throw new ApiException(429, ec, "Rate limited — try again shortly");
                if (resp.code() >= 500) throw new ApiException(resp.code(), ec, "Server error");
                throw new ApiException(resp.code(), ec, msg);
            }
            if (body == null || body.trim().isEmpty()) throw new ApiException(0, "empty_body", "Empty response from " + url);
            try {
                return new JSONObject(body);
            } catch (Exception e) {
                // Some list endpoints return { data: [...] } wrapped; try to parse as array root defensively
                throw new ApiException(0, "invalid_json", "Invalid JSON from " + url + ": " + e.getMessage(), e);
            }
        } catch (ApiException ae) { throw ae; }
        catch (IOException e) { throw new ApiException(0, "network_error", "Network error: " + e.getMessage(), e); }
    }

    private String extractError(String body, int code) {
        if (body == null || body.isEmpty()) return String.valueOf(code);
        try {
            JSONObject o = new JSONObject(body);
            String e = o.optString("error", null);
            if (e != null && !e.isEmpty()) return e;
            String m = o.optString("message", null);
            if (m != null && !m.isEmpty()) return m;
        } catch (Exception ignore) {}
        return String.valueOf(code);
    }

    private String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }
}
