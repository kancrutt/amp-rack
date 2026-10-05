package com.kancrutt.gigginggear.tone3000;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.TokenResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OAuth 2.0 Authorization Code + PKCE — token storage, refresh, state/verifier lifecycle.
 *
 * Security:
 * - code_verifier / state generated with SecureRandom
 * - code_challenge = BASE64URL(SHA256(verifier)) S256
 * - verifier/state persisted in EncryptedSharedPreferences until callback
 * - state verified before accepting callback
 * - access_token / refresh_token stored encrypted, never logged
 * - auto-refresh before expiry (60s skew)
 */
public final class Tone3000AuthManager {
    private static final String TAG = "Tone3000Auth";
    private static final String PREF_NAME = "tone3000_secure";
    private static final String KEY_ACCESS = "t3k_access_token";
    private static final String KEY_REFRESH = "t3k_refresh_token";
    private static final String KEY_EXPIRES_AT = "t3k_expires_at_ms";
    private static final String KEY_VERIFIER = "t3k_code_verifier";
    private static final String KEY_STATE = "t3k_state";
    private static final long REFRESH_SKEW_MS = 60_000L;

    private static Tone3000AuthManager instance;
    private final SharedPreferences prefs;
    private final OkHttpClient http;

    private Tone3000AuthManager(Context ctx) {
        SharedPreferences p;
        try {
            MasterKey mk = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(
                    ctx, PREF_NAME, mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e);
            p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = p;
        this.http = new OkHttpClient.Builder().build();
    }

    public static synchronized Tone3000AuthManager get(Context ctx) {
        if (instance == null) instance = new Tone3000AuthManager(ctx.getApplicationContext());
        return instance;
    }

    // --- PKCE + state generation ---

    public static final class AuthStart {
        public final String verifier;
        public final String challenge;
        public final String state;
        public final String authorizeUrl;
        AuthStart(String v, String c, String s, String url) { verifier=v; challenge=c; state=s; authorizeUrl=url; }
    }

    public AuthStart startAuthorization(Context ctx, String prompt, @Nullable String toneId) throws ApiException {
        return startAuthorization(ctx, prompt, toneId, null);
    }

    public AuthStart startAuthorization(Context ctx, String prompt, @Nullable String toneId,
                                         @Nullable String gears) throws ApiException {
        String clientId = Tone3000Config.getClientId(ctx);
        if (clientId == null || clientId.isEmpty()) throw new ApiException(0, "missing_client_id", "TONE3000_CLIENT_ID not configured — set BuildConfig or manifest meta-data");

        String verifier = generateVerifier();
        String challenge = challengeFromVerifier(verifier);
        String state = generateState();

        // persist until callback
        prefs.edit().putString(KEY_VERIFIER, verifier).putString(KEY_STATE, state).apply();

        Uri.Builder b = Uri.parse(Tone3000Config.AUTHORIZE_URL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", Tone3000Config.REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("state", state);
        if (prompt != null) b.appendQueryParameter("prompt", prompt);
        if (toneId != null) b.appendQueryParameter("tone_id", toneId);
        if (gears != null && !gears.trim().isEmpty()) b.appendQueryParameter("gears", gears);
        // UX hints appropriate to Giggin Gear (NAM/IR amp/cab, preview, A2 support)
        // Only set when prompt is select/load to avoid noise on bare auth
        if ("select_tone".equals(prompt) || "load_tone".equals(prompt)) {
            b.appendQueryParameter("format", "nam");
            // Support NAM A2 (and A1 via fallback) — without architecture param, A2 tones are hidden
            // Pass architecture=2 so A2 models are considered compatible; A1 fallback handled client-side
            b.appendQueryParameter("architecture", "2");
            // menubar helps user navigate/close the in-app browser sheet
            b.appendQueryParameter("menubar", "true");
            b.appendQueryParameter("preview", "true");
        }

        String url = b.build().toString();
        Log.d(TAG, "startAuthorization prompt=" + prompt + " state=" + state.substring(0, 8) + "…");
        return new AuthStart(verifier, challenge, state, url);
    }

    /** Convenience: prompt=select_tone (spec §1). */
    public AuthStart startSelectFlow(Context ctx) throws ApiException {
        return startAuthorization(ctx, "select_tone", null);
    }

    /** Select-flow variant scoped to a target gear family, e.g. "cab". */
    public AuthStart startSelectFlow(Context ctx, @Nullable String gears) throws ApiException {
        return startAuthorization(ctx, "select_tone", null, gears);
    }

    // --- Callback validation ---

    public static final class CallbackResult {
        public final String code;
        public final String toneId; // may be null
        public final boolean canceled;
        public final String error;
        CallbackResult(String code, String toneId, boolean canceled, String error) {
            this.code = code; this.toneId = toneId; this.canceled = canceled; this.error = error;
        }
    }

    /**
     * Verify state, handle canceled/error, return code+tone_id.
     * Caller must then exchange code for tokens.
     * state is checked FIRST per spec §7.
     */
    public CallbackResult handleCallback(Uri data) throws ApiException {
        if (data == null) throw new ApiException(0, "malformed_callback", "callback uri is null");
        String state = data.getQueryParameter("state");
        String expected = prefs.getString(KEY_STATE, null);
        // state must be present and match
        if (state == null || expected == null || !state.equals(expected)) {
            throw new ApiException(0, "state_mismatch", "OAuth state mismatch — possible CSRF");
        }
        String err = data.getQueryParameter("error");
        if (err != null && !err.isEmpty()) {
            throw new ApiException(0, err, "OAuth error: " + err);
        }
        String canceledStr = data.getQueryParameter("canceled");
        boolean canceled = "true".equalsIgnoreCase(canceledStr);
        String code = data.getQueryParameter("code");
        String toneId = data.getQueryParameter("tone_id");
        // Handle alternate param names defensively
        if (code == null) code = data.getQueryParameter("authorization_code");
        if (toneId == null) toneId = data.getQueryParameter("toneId");
        if (canceled && (code == null || code.isEmpty())) {
            // user closed before signing in — no code to exchange, treat as canceled without error
            return new CallbackResult(null, null, true, null);
        }
        if (canceled) {
            return new CallbackResult(code, null, true, null);
        }
        if (code == null || code.isEmpty()) {
            throw new ApiException(0, "missing_code", "authorization code missing in callback");
        }
        return new CallbackResult(code, toneId, false, null);
    }

    // --- Token exchange + refresh ---

    public TokenResponse exchangeCode(Context ctx, String code) throws ApiException {
        String verifier = prefs.getString(KEY_VERIFIER, null);
        if (verifier == null || verifier.isEmpty()) throw new ApiException(0, "missing_verifier", "code_verifier not found — restart auth");
        String clientId = Tone3000Config.getClientId(ctx);
        FormBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("code_verifier", verifier)
                .add("redirect_uri", Tone3000Config.REDIRECT_URI)
                .add("client_id", clientId)
                .build();
        TokenResponse tr = postToken(body);
        persistTokens(tr);
        // one-time verifier/state consumed
        prefs.edit().remove(KEY_VERIFIER).remove(KEY_STATE).apply();
        return tr;
    }

    public synchronized String getValidAccessToken(Context ctx) throws ApiException {
        String at = prefs.getString(KEY_ACCESS, null);
        long expAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        if (at == null || at.isEmpty()) throw new ApiException(401, "auth_required", "Not authenticated");
        long now = System.currentTimeMillis();
        if (expAt == 0 || now + REFRESH_SKEW_MS < expAt) return at;
        // expired / about to expire -> refresh
        TokenResponse tr = refresh(ctx);
        return tr.accessToken;
    }

    public synchronized TokenResponse refresh(Context ctx) throws ApiException {
        String rt = prefs.getString(KEY_REFRESH, null);
        String clientId = Tone3000Config.getClientId(ctx);
        if (rt == null || rt.isEmpty()) throw new ApiException(401, "missing_refresh_token", "No refresh token — re-authenticate");
        FormBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", rt)
                .add("client_id", clientId)
                .build();
        try {
            TokenResponse tr = postToken(body);
            persistTokens(tr);
            return tr;
        } catch (ApiException e) {
            // refresh failed (invalid_grant) -> clear and require re-auth
            if (e.httpCode == 400) {
                clearTokens();
            }
            throw e;
        }
    }

    private TokenResponse postToken(FormBody body) throws ApiException {
        Request req = new Request.Builder().url(Tone3000Config.TOKEN_URL).post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String b = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                String ec = extractErrorCode(b);
                throw new ApiException(resp.code(), ec, "token request failed: HTTP " + resp.code() + " " + b);
            }
            JSONObject o = new JSONObject(b);
            return TokenResponse.fromJson(o);
        } catch (ApiException ae) { throw ae; }
        catch (IOException e) { throw new ApiException(0, "network_error", "token network error", e); }
        catch (Exception e) { throw new ApiException(0, "parse_error", "token parse error: " + e.getMessage(), e); }
    }

    private void persistTokens(TokenResponse tr) {
        long expAt = System.currentTimeMillis() + tr.expiresIn * 1000L;
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(KEY_ACCESS, tr.accessToken);
        if (tr.refreshToken != null) ed.putString(KEY_REFRESH, tr.refreshToken);
        ed.putLong(KEY_EXPIRES_AT, expAt);
        ed.apply();
    }

    public void clearTokens() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).remove(KEY_EXPIRES_AT).apply();
    }

    public boolean isAuthenticated() {
        String at = prefs.getString(KEY_ACCESS, null);
        return at != null && !at.isEmpty();
    }

    public void clearAuthState() {
        prefs.edit().remove(KEY_VERIFIER).remove(KEY_STATE).apply();
    }

    // --- helpers ---

    private String generateVerifier() {
        byte[] r = new byte[32];
        new SecureRandom().nextBytes(r);
        return base64UrlNoPad(r);
    }
    private String generateState() {
        byte[] r = new byte[16];
        new SecureRandom().nextBytes(r);
        return base64UrlNoPad(r);
    }
    private String challengeFromVerifier(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return base64UrlNoPad(h);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private String base64UrlNoPad(byte[] b) {
        return Base64.encodeToString(b, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }
    private String extractErrorCode(String body) {
        try {
            JSONObject o = new JSONObject(body);
            String e = o.optString("error", null);
            if (e != null && !e.isEmpty()) return e;
            String ed = o.optString("error_description", null);
            if (ed != null) return ed;
        } catch (Exception ignore) {}
        return "token_error";
    }
}
