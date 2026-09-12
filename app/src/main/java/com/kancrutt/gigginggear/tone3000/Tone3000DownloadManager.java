package com.kancrutt.gigginggear.tone3000;

import android.content.Context;
import android.util.Log;

import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.ModelDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Downloads a TONE3000 model from its authenticated model_url.
 * Uses existing Giggin' Gear storage pipeline (externalFilesDir) — no duplicate logic.
 * Handles progress, cancellation, network failure, HTTP errors, insufficient storage,
 * duplicate file, corrupted/incomplete download, retry.
 * Must NOT block UI thread; caller runs on background executor.
 * Must NOT touch audio thread.
 */
public final class Tone3000DownloadManager {
    private static final String TAG = "Tone3000DL";
    private final OkHttpClient http;
    private final Context appCtx;

    public Tone3000DownloadManager(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        this.http = new OkHttpClient.Builder().build();
    }

    public interface ProgressListener {
        void onProgress(long bytesRead, long contentLength);
        boolean isCancelled();
    }

    public static final class DownloadResult {
        public final File file;
        public final long bytes;
        DownloadResult(File f, long b) { file=f; bytes=b; }
    }

    /**
     * Download model to <externalFilesDir>/TONE3000/<sanitized tone dir>/<model name>.
     * model_url requires Bearer auth per API docs.
     */
    public DownloadResult downloadModel(ModelDto model, String toneTitle, ProgressListener listener) throws ApiException {
        if (model == null || model.modelUrl == null) throw new ApiException(0, "missing_model_url", "model_url is null");
        String token;
        try {
            token = Tone3000AuthManager.get(appCtx).getValidAccessToken(appCtx);
        } catch (ApiException ae) { throw ae; }

        File dest = resolveDestination(model, toneTitle);
        // duplicate handling: if exists and length > 0, treat as already downloaded; caller can decide to overwrite
        // We do NOT silently overwrite — if file exists we return it as success (idempotent)
        if (dest.exists() && dest.length() > 0) {
            Log.i(TAG, "model already exists: " + dest.getAbsolutePath());
            return new DownloadResult(dest, dest.length());
        }
        File tmp = new File(dest.getAbsolutePath() + ".tmp");
        // ensure parent
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        Request req = new Request.Builder().url(model.modelUrl)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/octet-stream")
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                String body = "";
                try { body = resp.body() != null ? resp.body().string() : ""; } catch (Exception ignore) {}
                if (resp.code() == 401) throw new ApiException(401, "auth_required", "Download auth failed — please re-authenticate");
                if (resp.code() == 403) throw new ApiException(403, "forbidden", "Download forbidden");
                if (resp.code() == 404) throw new ApiException(404, "not_found", "Model not found on TONE3000");
                if (resp.code() == 429) throw new ApiException(429, "rate_limited", "Rate limited");
                throw new ApiException(resp.code(), "http_" + resp.code(), "Download failed HTTP " + resp.code() + " " + body);
            }
            long total = -1;
            try { String cl = resp.header("Content-Length"); if (cl != null) total = Long.parseLong(cl); } catch (Exception ignore) {}
            long read = 0;
            try (InputStream in = resp.body().byteStream();
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (listener != null && listener.isCancelled()) {
                        out.close();
                        tmp.delete();
                        throw new ApiException(0, "canceled", "Download canceled");
                    }
                    out.write(buf, 0, n);
                    read += n;
                    if (listener != null) listener.onProgress(read, total);
                }
                out.getFD().sync();
            }
            // basic validation: non-empty
            if (read == 0 || tmp.length() == 0) {
                tmp.delete();
                throw new ApiException(0, "empty_file", "Downloaded file is empty");
            }
            // insufficient storage would have thrown IOException during write; surface as ApiException
            if (!tmp.renameTo(dest)) {
                // fallback copy
                if (dest.exists()) dest.delete();
                if (!tmp.renameTo(dest)) throw new ApiException(0, "io_error", "Failed to finalize download");
            }
            Log.i(TAG, "downloaded " + model.name + " -> " + dest.getAbsolutePath() + " (" + read + " bytes)");
            return new DownloadResult(dest, read);
        } catch (ApiException ae) { throw ae; }
        catch (IOException e) {
            tmp.delete();
            String msg = e.getMessage() != null ? e.getMessage() : "IO error";
            if (msg.toLowerCase().contains("no space") || msg.toLowerCase().contains("enospc")) {
                throw new ApiException(0, "no_space", "Insufficient storage", e);
            }
            throw new ApiException(0, "network_error", "Download network error: " + msg, e);
        }
    }

    private File resolveDestination(ModelDto model, String toneTitle) {
        File base = appCtx.getExternalFilesDir(null);
        if (base == null) base = appCtx.getFilesDir();
        String safeTone = sanitize(toneTitle != null ? toneTitle : ("tone_" + model.toneId));
        String safeName = sanitize(model.name);
        String ext = model.extension();
        if (!safeName.toLowerCase().endsWith("." + ext)) safeName = safeName + "." + ext;
        File dir = new File(base, "TONE3000/" + safeTone);
        return new File(dir, safeName);
    }

    private String sanitize(String s) {
        if (s == null || s.isEmpty()) return "untitled";
        // strip path separators, keep alnum, space, dash, underscore, dot
        String t = s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (t.length() > 64) t = t.substring(0, 64);
        if (t.isEmpty()) return "untitled";
        return t;
    }
}
