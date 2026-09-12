package com.kancrutt.gigginggear.tone3000.model;

import org.json.JSONObject;

/**
 * Defensive DTO for POST /oauth/token response.
 * All fields are validated — never assume type.
 */
public final class TokenResponse {
    public final String accessToken;
    public final String refreshToken;
    public final long expiresIn; // seconds
    public final String tokenType; // "bearer"
    public final String scope;

    private TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType, String scope) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.scope = scope;
    }

    public static TokenResponse fromJson(JSONObject o) throws ApiException {
        if (o == null) throw new ApiException(0, "null_token_json", "Token response is null");
        String at = optString(o, "access_token");
        String rt = optString(o, "refresh_token");
        if (at == null || at.isEmpty()) throw new ApiException(0, "missing_access_token", "access_token missing in token response");
        // refresh_token may be absent on some flows; allow null but log
        long exp = optLong(o, "expires_in", 3600L);
        String tt = optString(o, "token_type");
        if (tt == null) tt = "bearer";
        String sc = optString(o, "scope");
        return new TokenResponse(at, rt, exp, tt, sc);
    }

    private static String optString(JSONObject o, String k) {
        if (!o.has(k) || o.isNull(k)) return null;
        try {
            Object v = o.get(k);
            if (v instanceof String) return (String) v;
            // number -> string coercion where sensible
            if (v instanceof Number) return String.valueOf(v);
            return null;
        } catch (Exception e) { return null; }
    }
    private static long optLong(JSONObject o, String k, long fallback) {
        if (!o.has(k) || o.isNull(k)) return fallback;
        try {
            Object v = o.get(k);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v instanceof String) return Long.parseLong(((String) v).trim());
            return fallback;
        } catch (Exception e) { return fallback; }
    }

    @Override public String toString() { return "TokenResponse{expiresIn=" + expiresIn + ", tokenType=" + tokenType + "}"; }
}
