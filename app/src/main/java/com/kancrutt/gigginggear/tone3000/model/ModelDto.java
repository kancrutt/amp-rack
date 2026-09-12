package com.kancrutt.gigginggear.tone3000.model;

import org.json.JSONObject;

/**
 * DTO for GET /models and GET /models/{id}.
 * model_url is the authenticated download source — fetch with Bearer token.
 */
public final class ModelDto {
    public final long id;
    public final long toneId;
    public final long userId;
    public final String name;
    public final String size; // "standard", "lite", "feather", etc
    public final String modelUrl; // requires Bearer auth to download
    public final String architectureVersion; // "1", "2", "custom", or null for IR
    public final String createdAt;
    public final String updatedAt;

    private ModelDto(long id, long toneId, long userId, String name, String size,
                     String modelUrl, String arch, String ca, String ua) {
        this.id = id; this.toneId = toneId; this.userId = userId;
        this.name = name; this.size = size; this.modelUrl = modelUrl;
        this.architectureVersion = arch; this.createdAt = ca; this.updatedAt = ua;
    }

    public static ModelDto fromJson(JSONObject o) throws ApiException {
        if (o == null) throw new ApiException(0, "null_model", "model json is null");
        long id = optLong(o, "id", -1);
        if (id < 0) throw new ApiException(0, "missing_model_id", "model.id missing");
        long tid = optLong(o, "tone_id", -1);
        long uid = optLong(o, "user_id", 0);
        String name = optString(o, "name");
        if (name == null || name.isEmpty()) name = "Model " + id;
        String size = optString(o, "size");
        if (size == null) size = "standard";
        String url = optString(o, "model_url");
        if (url == null) url = optString(o, "modelUrl"); // defensive alias
        if (url == null || url.isEmpty()) throw new ApiException(0, "missing_model_url", "model_url missing for model " + id);
        String arch = null;
        if (o.has("architecture_version") && !o.isNull("architecture_version")) {
            Object av = o.opt("architecture_version");
            if (av instanceof Number) arch = String.valueOf(((Number)av).intValue());
            else if (av instanceof String) arch = (String) av;
        }
        String ca = optString(o, "created_at");
        String ua = optString(o, "updated_at");
        return new ModelDto(id, tid, uid, name, size, url, arch, ca, ua);
    }

    private static String optString(JSONObject o, String k) {
        if (!o.has(k) || o.isNull(k)) return null;
        try { Object v = o.get(k); if (v instanceof String) return (String) v; if (v instanceof Number) return String.valueOf(v); return null; } catch (Exception e) { return null; }
    }
    private static long optLong(JSONObject o, String k, long fallback) {
        if (!o.has(k) || o.isNull(k)) return fallback;
        try { Object v = o.get(k); if (v instanceof Number) return ((Number)v).longValue(); if (v instanceof String) return Long.parseLong(((String)v).trim()); return fallback; } catch (Exception e) { return fallback; }
    }

    /** File extension hint from model_url or name. */
    public String extension() {
        String src = modelUrl != null ? modelUrl : name;
        int q = src.indexOf('?'); if (q >= 0) src = src.substring(0, q);
        int dot = src.lastIndexOf('.');
        if (dot >= 0 && dot < src.length() - 1) return src.substring(dot + 1).toLowerCase();
        // fallback by architecture: NAM models are .nam, IR are .wav
        return "nam";
    }

    public boolean isNam() { String e = extension(); return e.equals("nam") || e.equals("json"); }
    public boolean isIr() { String e = extension(); return e.equals("wav") || e.equals("ir"); }
}
