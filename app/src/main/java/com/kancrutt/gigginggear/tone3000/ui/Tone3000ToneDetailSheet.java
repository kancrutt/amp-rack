package com.kancrutt.gigginggear.tone3000.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kancrutt.gigginggear.MainActivity;
import com.kancrutt.gigginggear.R;
import com.kancrutt.gigginggear.tone3000.Tone3000DownloadManager;
import com.kancrutt.gigginggear.tone3000.Tone3000Repository;
import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.ModelDto;
import com.kancrutt.gigginggear.tone3000.model.ToneDto;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Detail panel for a TONE3000 tone: image, title, gear, format, creator,
 * model selector (actual models from API), description, plus persistent
 * "Browse TONE3000" entry point.
 *
 * Handles: tone fetch, model list, per-model download with progress/cancel,
 * format routing (NAM/IR), and signal-chain insertion via MainActivity.
 */
public class Tone3000ToneDetailSheet extends BottomSheetDialogFragment {
    private static final String ARG_TONE_ID = "tone_id";
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private String toneId;
    private ToneDto tone;
    private Tone3000Repository repo;

    public static Tone3000ToneDetailSheet newInstance(String toneId) {
        Tone3000ToneDetailSheet f = new Tone3000ToneDetailSheet();
        Bundle b = new Bundle(); b.putString(ARG_TONE_ID, toneId); f.setArguments(b);
        return f;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog d = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        d.setOnShowListener(dlg -> {
            FrameLayout bs = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                BottomSheetBehavior<FrameLayout> beh = BottomSheetBehavior.from(bs);
                beh.setState(BottomSheetBehavior.STATE_EXPANDED);
                beh.setSkipCollapsed(true);
            }
        });
        return d;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_tone3000_detail, container, false);
        toneId = getArguments() != null ? getArguments().getString(ARG_TONE_ID) : null;
        repo = new Tone3000Repository(requireContext());

        ImageButton close = v.findViewById(R.id.t3k_detail_close);
        if (close != null) close.setOnClickListener(x -> dismiss());

        ProgressBar loading = v.findViewById(R.id.t3k_detail_loading);
        TextView error = v.findViewById(R.id.t3k_detail_error);
        LinearLayout content = v.findViewById(R.id.t3k_detail_content);
        LinearLayout modelsContainer = v.findViewById(R.id.t3k_models_container);
        Button browse = v.findViewById(R.id.t3k_detail_browse_btn);

        if (browse != null) browse.setOnClickListener(x -> {
            dismiss();
            // re-launch select flow
            Tone3000EntrySheet.newInstance().show(getParentFragmentManager(), "t3k_entry");
        });

        if (toneId == null || toneId.isEmpty()) {
            if (loading != null) loading.setVisibility(View.GONE);
            if (error != null) { error.setVisibility(View.VISIBLE); error.setText("Missing tone_id"); }
            return v;
        }

        if (loading != null) loading.setVisibility(View.VISIBLE);

        bg.execute(() -> {
            try {
                ToneDto t = repo.fetchTone(toneId);
                Tone3000Repository.ToneWithModels twm = null;
                // also fetch models eagerly for detail
                try { twm = repo.fetchToneWithModels(toneId); t = twm.tone; } catch (Exception ignore) {}
                ToneDto finalT = t;
                Tone3000Repository.ToneWithModels finalTwm = twm;
                main.post(() -> {
                    if (!isAdded()) return;
                    bindTone(v, finalT, finalTwm, loading, error, content, modelsContainer);
                });
            } catch (ApiException e) {
                main.post(() -> {
                    if (!isAdded()) return;
                    if (loading != null) loading.setVisibility(View.GONE);
                    if (error != null) {
                        error.setVisibility(View.VISIBLE);
                        error.setText(mapError(e));
                    }
                    if (e.isAuthRequired()) {
                        Toast.makeText(requireContext(), "Please re-authenticate with TONE3000", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        return v;
    }

    private void bindTone(View root, ToneDto t, Tone3000Repository.ToneWithModels twm,
                          ProgressBar loading, TextView error, LinearLayout content, LinearLayout modelsContainer) {
        this.tone = t;
        if (loading != null) loading.setVisibility(View.GONE);
        if (error != null) error.setVisibility(View.GONE);
        if (content != null) content.setVisibility(View.VISIBLE);

        TextView title = root.findViewById(R.id.t3k_detail_title);
        TextView creator = root.findViewById(R.id.t3k_detail_creator);
        TextView gear = root.findViewById(R.id.t3k_detail_gear);
        TextView format = root.findViewById(R.id.t3k_detail_format);
        TextView desc = root.findViewById(R.id.t3k_detail_desc);
        ImageView image = root.findViewById(R.id.t3k_detail_image);
        TextView t3kMark = root.findViewById(R.id.t3k_detail_mark);

        if (title != null) title.setText(t.title);
        if (creator != null) creator.setText(t.userUsername != null ? "@" + t.userUsername : "TONE3000");
        if (gear != null) gear.setText(friendlyGear(t.gear));
        if (format != null) format.setText(t.format != null ? t.format.toUpperCase() : "NAM");
        if (desc != null) desc.setText(t.description != null ? t.description : "");
        if (t3kMark != null) { t3kMark.setText("TONE3000"); t3kMark.setTextColor(Color.parseColor("#FFFFFF")); }

        // Load image via simple async (no extra lib) — defensively handle null + terminated executor
        if (image != null && t.primaryImage() != null) {
            String url = t.primaryImage();
            try {
                if (bg.isShutdown() || bg.isTerminated()) {
                    // fragment already destroyed — skip image load
                    return;
                }
                bg.execute(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(url).openStream();
                        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(in);
                        in.close();
                        main.post(() -> { try { image.setImageBitmap(bm); } catch (Exception ignore) {} });
                    } catch (Exception ignore) {}
                });
            } catch (java.util.concurrent.RejectedExecutionException ignore) {
                // pool terminated during dismiss — ignore
            }
        }

        // Models list — single binder for both paths so every row always shows
        // arch chip + size chip (A2 vs A2 Lite) + full uploader name.
        if (modelsContainer != null) {
            if (twm != null && twm.models != null && !twm.models.data.isEmpty()) {
                bindModelRows(modelsContainer, twm.models.data, t);
            } else {
                // trigger async fetch if not pre-fetched
                View empty = LayoutInflater.from(requireContext()).inflate(R.layout.item_tone3000_model_row, modelsContainer, false);
                TextView mn = empty.findViewById(R.id.t3k_model_name);
                if (mn != null) mn.setText("Loading models…");
                modelsContainer.addView(empty);
                bg.execute(() -> {
                    try {
                        Tone3000Repository.ToneWithModels fres = repo.fetchToneWithModels(t.id + "");
                        main.post(() -> {
                            if (!isAdded()) return;
                            bindModelRows(modelsContainer, fres.models != null ? fres.models.data : null, t);
                        });
                    } catch (ApiException e) {
                        main.post(() -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), mapError(e), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        }
    }

    /** One row per model: name (2 lines — keeps uploader speaker/mic details
     *  visible) + arch chip (A1/A2/Custom/IR) + size chip (Standard/Lite/…). */
    private void bindModelRows(LinearLayout modelsContainer, java.util.List<ModelDto> models, ToneDto t) {
        modelsContainer.removeAllViews();
        if (models == null || models.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("No models available for this tone.");
            emptyTv.setTextColor(Color.parseColor("#A1A1AA"));
            emptyTv.setPadding(16,16,16,16);
            modelsContainer.addView(emptyTv);
            return;
        }
        for (ModelDto m : models) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_tone3000_model_row, modelsContainer, false);
            TextView mn = row.findViewById(R.id.t3k_model_name);
            TextView ma = row.findViewById(R.id.t3k_model_arch);
            TextView ms = row.findViewById(R.id.t3k_model_size);
            Button dl = row.findViewById(R.id.t3k_model_download_btn);
            ProgressBar pb = row.findViewById(R.id.t3k_model_progress);
            if (mn != null) mn.setText(m.name);
            if (ma != null) ma.setText(archLabel(m, t));
            if (ms != null) ms.setText(sizeLabel(m.size));
            if (dl != null) {
                dl.setOnClickListener(x -> downloadAndLoad(m, t, dl, pb));
            }
            modelsContainer.addView(row);
        }
    }

    /** Arch chip: "A1" / "A2" / "CUSTOM", falls back to format ("NAM"/"IR"). */
    private static String archLabel(ModelDto m, ToneDto t) {
        if (m != null && m.architectureVersion != null) {
            String a = m.architectureVersion.trim().toLowerCase();
            if (a.equals("1")) return "A1";
            if (a.equals("2")) return "A2";
            if (!a.isEmpty()) return a.toUpperCase();
        }
        return t != null && t.format != null ? t.format.toUpperCase() : "MODEL";
    }

    /** Size chip, capitalized: Standard / Lite / Feather / Nano / Custom. */
    private static String sizeLabel(String size) {
        if (size == null) return "Standard";
        switch (size.trim().toLowerCase()) {
            case "lite": return "Lite";
            case "feather": return "Feather";
            case "nano": return "Nano";
            case "custom": return "Custom";
            default: return "Standard";
        }
    }

    /** Friendly gear: "amp" = amp-only, "amp-cab" = include cab (API enum). */
    private static String friendlyGear(String gear) {
        if (gear == null) return "AMP";
        switch (gear.trim().toLowerCase()) {
            case "amp": return "AMP · AMP ONLY";
            case "amp-cab": return "AMP + CAB · INCLUDE CAB";
            case "cab": return "CAB";
            case "pedal": return "PEDAL";
            case "outboard": return "OUTBOARD";
            case "space": return "SPACE";
            case "experimental": return "EXPERIMENTAL";
            default: return gear.toUpperCase();
        }
    }

    private void downloadAndLoad(ModelDto model, ToneDto toneDto, Button btn, ProgressBar pb) {
        if (btn != null) { btn.setEnabled(false); btn.setText("Downloading…"); }
        if (pb != null) { pb.setVisibility(View.VISIBLE); pb.setIndeterminate(false); pb.setProgress(0); }
        bg.execute(() -> {
            try {
                Tone3000DownloadManager dlm = new Tone3000DownloadManager(requireContext());
                Tone3000DownloadManager.DownloadResult res = dlm.downloadModel(model, toneDto.title, new Tone3000DownloadManager.ProgressListener() {
                    @Override public void onProgress(long r, long total) {
                        if (pb != null && total > 0) main.post(() -> {
                            pb.setMax(100);
                            pb.setProgress((int)(r * 100 / total));
                        });
                    }
                    @Override public boolean isCancelled() { return false; }
                });
                File f = res.file;
                main.post(() -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (btn != null) { btn.setEnabled(true); btn.setText("Downloaded"); }
                    // Route to NAM/IR loader
                    try {
                        MainActivity ma = (MainActivity) requireActivity();
                        boolean ok = ma.loadTone3000Model(f, model, toneDto);
                        if (ok) {
                            Toast.makeText(requireContext(), "Loaded: " + model.name, Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            Toast.makeText(requireContext(), "Unsupported format: " + toneDto.format, Toast.LENGTH_LONG).show();
                            if (btn != null) btn.setText("Retry");
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        if (btn != null) { btn.setEnabled(true); btn.setText("Download"); }
                    }
                });
            } catch (ApiException e) {
                main.post(() -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (btn != null) { btn.setEnabled(true); btn.setText("Retry"); }
                    Toast.makeText(requireContext(), mapError(e), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String mapError(ApiException e) {
        if (e.httpCode == 401) return "Not authenticated — please Browse TONE3000 again";
        if (e.httpCode == 403) return "Access denied";
        if (e.httpCode == 404) return "Tone or model not found";
        if (e.httpCode == 429) return "Rate limited (100/min) — try again shortly";
        if (e.httpCode >= 500) return "TONE3000 server error — try again later";
        if ("network_error".equals(e.errorCode)) return "Offline or network error";
        if ("invalid_json".equals(e.errorCode)) return "Unexpected response from TONE3000";
        return e.getMessage() != null ? e.getMessage() : "TONE3000 error";
    }

    @Override public void onDestroy() { super.onDestroy(); bg.shutdownNow(); }
}
