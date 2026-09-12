package com.kancrutt.gigginggear.tone3000.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Defensive DTO for GET /tones/{id} (and list items).
 * Every field validated: exists, not null, correct type before conversion.
 * Null is preserved where semantically meaningful (published_at, description).
 */
public final class ToneDto {
    public final long id;
    public final long userId;
    public final String userUsername; // from embedded user
    public final String userAvatar;   // may be null
    public final String createdAt;
    public final String updatedAt;
    public final String publishedAt; // nullable
    public final String title;
    public final String description; // nullable
    public final String gear; // e.g. "amp", "amp-cab"
    public final List<String> images; // nullable
    public final boolean isPublic; // nullable in API -> default false
    public final String format; // "nam", "ir", etc
    public final int modelsCount;
    public final int a1ModelsCount;
    public final int a2ModelsCount;
    public final int customModelsCount;
    public final int downloadsCount;
    public final int favoritesCount;
    public final String url;

    private ToneDto(long id, long userId, String userUsername, String userAvatar,
                    String createdAt, String updatedAt, String publishedAt,
                    String title, String description, String gear, List<String> images,
                    boolean isPublic, String format, int modelsCount, int a1, int a2, int custom,
                    int downloadsCount, int favoritesCount, String url) {
        this.id = id; this.userId = userId; this.userUsername = userUsername; this.userAvatar = userAvatar;
        this.createdAt = createdAt; this.updatedAt = updatedAt; this.publishedAt = publishedAt;
        this.title = title; this.description = description; this.gear = gear; this.images = images;
        this.isPublic = isPublic; this.format = format; this.modelsCount = modelsCount;
        this.a1ModelsCount = a1; this.a2ModelsCount = a2; this.customModelsCount = custom;
        this.downloadsCount = downloadsCount; this.favoritesCount = favoritesCount; this.url = url;
    }

    public static ToneDto fromJson(JSONObject o) throws ApiException {
        if (o == null) throw new ApiException(0, "null_tone", "tone json is null");
        long id = optLong(o, "id", -1);
        if (id < 0) throw new ApiException(0, "missing_id", "tone.id missing");
        long uid = optLong(o, "user_id", 0);
        String username = null, avatar = null;
        JSONObject user = o.optJSONObject("user");
        if (user != null) {
            username = optString(user, "username");
            avatar = optString(user, "avatar");
            if (username == null) username = optString(user, "name");
        }
        String title = optString(o, "title");
        if (title == null) title = "Untitled Tone";
        String gear = optString(o, "gear");
        if (gear == null) gear = "amp";
        String format = optString(o, "format");
        if (format == null) format = "nam";
        String desc = optStringNullable(o, "description");
        String pub = optStringNullable(o, "published_at");
        String ca = optString(o, "created_at");
        String ua = optString(o, "updated_at");
        String url = optString(o, "url");
        boolean isPub = o.optBoolean("is_public", false);
        // images: string[] | null
        List<String> imgs = null;
        if (o.has("images") && !o.isNull("images")) {
            try {
                JSONArray arr = o.getJSONArray("images");
                imgs = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    Object v = arr.opt(i);
                    if (v instanceof String) imgs.add((String) v);
                }
            } catch (Exception ignore) {}
        }
        int mc = optInt(o, "models_count", 0);
        int a1 = optInt(o, "a1_models_count", 0);
        int a2 = optInt(o, "a2_models_count", 0);
        int cc = optInt(o, "custom_models_count", 0);
        int dc = optInt(o, "downloads_count", 0);
        int fc = optInt(o, "favorites_count", 0);
        return new ToneDto(id, uid, username, avatar, ca, ua, pub, title, desc, gear, imgs, isPub, format, mc, a1, a2, cc, dc, fc, url);
    }

    // Helpers — never throw type_error, never assume string
    private static String optString(JSONObject o, String k) {
        if (!o.has(k) || o.isNull(k)) return null;
        try { Object v = o.get(k); if (v instanceof String) return (String) v; if (v instanceof Number) return String.valueOf(v); return null; } catch (Exception e) { return null; }
    }
    private static String optStringNullable(JSONObject o, String k) {
        if (!o.has(k) || o.isNull(k)) return null;
        return optString(o, k);
    }
    private static long optLong(JSONObject o, String k, long fallback) {
        if (!o.has(k) || o.isNull(k)) return fallback;
        try { Object v = o.get(k); if (v instanceof Number) return ((Number)v).longValue(); if (v instanceof String) return Long.parseLong(((String)v).trim()); return fallback; } catch (Exception e) { return fallback; }
    }
    private static int optInt(JSONObject o, String k, int fallback) {
        if (!o.has(k) || o.isNull(k)) return fallback;
        try { Object v = o.get(k); if (v instanceof Number) return ((Number)v).intValue(); if (v instanceof String) return Integer.parseInt(((String)v).trim()); return fallback; } catch (Exception e) { return fallback; }
    }

    public String primaryImage() {
        if (images != null && !images.isEmpty()) return images.get(0);
        return null;
    }
}
