package com.kancrutt.gigginggear.tone3000;

import android.content.Context;

import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.ModelDto;
import com.kancrutt.gigginggear.tone3000.model.ToneDto;

/**
 * Integration boundary: UI -> Repository -> AuthManager/ApiClient -> HTTP.
 * Single place to compose tone+models fetch + expose typed results.
 * Do NOT put HTTP calls in Composables/UI.
 */
public final class Tone3000Repository {
    private final Tone3000ApiClient api;

    public Tone3000Repository(Context ctx) { this.api = new Tone3000ApiClient(ctx); }

    public ToneDto fetchTone(String toneId) throws ApiException { return api.getTone(toneId); }

    public Tone3000ApiClient.ModelsPage fetchModels(String toneId, int page, int pageSize) throws ApiException {
        return api.listModels(toneId, page, pageSize);
    }

    public ModelDto fetchModel(String modelId) throws ApiException { return api.getModel(modelId); }

    /** Convenience: tone + first page of models together. */
    public static final class ToneWithModels {
        public final ToneDto tone;
        public final Tone3000ApiClient.ModelsPage models;
        ToneWithModels(ToneDto t, Tone3000ApiClient.ModelsPage m) { tone=t; models=m; }
    }

    public ToneWithModels fetchToneWithModels(String toneId) throws ApiException {
        ToneDto t = api.getTone(toneId);
        // 300 = API max: capture-rich tones (amp-only + cab variants x sizes)
        // must list EVERYTHING — a small page silently hides variants.
        Tone3000ApiClient.ModelsPage mp = api.listModels(toneId, 1, 300);
        return new ToneWithModels(t, mp);
    }
}
