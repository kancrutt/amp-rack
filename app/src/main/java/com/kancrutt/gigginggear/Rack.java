package com.kancrutt.gigginggear;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;

import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.WindowInsets;
import android.view.RoundedCorner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Rack extends Fragment {
    MainActivity mainActivity ;
    int currentPresetNumber = 1 ;
    int currentSceneIndex = 0 ;
    LinearProgressIndicator quickPatchProgress ;
    Dialog youtubePlayer ;
    String TAG = getClass().getSimpleName();
    PopupMenu optionsMenu ;
    ToggleButton toggleVideo, videoRecord ;
    LinearLayout videoPreview ;
    TextureView videoTexture ;
    LinearLayout mixer ;
    Button patchUp, patchDown ;
    JSONObject jsonObject = new JSONObject();
    ToggleButton swapCamera ;
    LinearLayout rackMaster ;
    boolean mixerInit = false ;
    boolean autoHideMixer = true ;
    /*
    Rack () {
        mainActivity = (MainActivity) getActivity();
    }

    Rack (MainActivity activity) {
        mainActivity = activity ;
    }

     */

    public void printPlugins () {
        Log.d(TAG, "printPlugins: " + jsonObject.toString());
    }

    public void writeJSON () {
        File file = new File(mainActivity.dir, "plugins.json");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            stream.write(jsonObject.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        setRetainInstance(true);
        return inflater.inflate(R.layout.rack,null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        if (mainActivity.pluginDialog != null) {
            // we did this already
            return ;
        }

        quickPatchProgress = mainActivity.findViewById(R.id.patch_loading);

        mainActivity.onOff = view.findViewById(R.id.onoff);
        if (mainActivity.onOff != null) {
            mainActivity.onOff.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(mainActivity.onOff, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                    return false;
                }
            });
        }

        rackMaster = view.findViewById(R.id.rack_master);
        /*
        mainActivity.onOff.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                return mainActivity.hotkeys(keyCode, event);
            }
        });

         */

        mainActivity.onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainActivity.toggleEffect(!b);
            }
        });

        patchDown = mainActivity.findViewById(R.id.patch_down);
        mainActivity.pluginDialog = mainActivity.createPluginDialog();
        if (mainActivity.useTheme)
            mainActivity.linearLayoutPluginDialog.post(new Runnable() {
                @Override
                public void run() {
                    

                }
            });

        toggleVideo = mainActivity.findViewById(R.id.video_button);
        if (mainActivity.useTheme) {
            

        }

        videoRecord = mainActivity.findViewById(R.id.toggle_video);
        videoRecord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });
        videoPreview = mainActivity.findViewById(R.id.video_preview);
        swapCamera = mainActivity.findViewById(R.id.flip_camera);
        videoTexture = mainActivity.findViewById(R.id.video_texture);

        swapCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (videoRecord.isChecked()) {
                    mainActivity.toast ("Cannot flip camera while recording") ;
                    return;
                }

                toggleVideo.setChecked(false);
                toggleVideo.setChecked(true);
            }
        });

        videoRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mainActivity.record.isChecked()) {
                    buttonView.setChecked(false);
                    MainActivity.toast("Cannot record audio and video at the same time");
                    return;
                }

                if (isChecked) {
                    // Camera features pruned per UI_UX_SPEC §7.1
                } else {
                    if (toggleVideo.isChecked())
                        toggleVideo.setChecked(false);
                }
            }
        });

        toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Camera features pruned per UI_UX_SPEC §7.1
                buttonView.setChecked(false);
            }
        });

        mainActivity.tuner = view.findViewById(R.id.patch_label);
        Switch tunerSwitch = view.findViewById(R.id.tuner_switch);
        if (tunerSwitch != null) {
            tunerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mainActivity.tuner instanceof TextView) {
                    ((TextView) mainActivity.tuner).setText("Please wait ...");
                }
                mainActivity.tunerEnabled = isChecked;
                if (!mainActivity.tunerEnabled) {
                    AudioEngine.setTunerEnabled(false);
                    if (mainActivity.tuner instanceof TextView) {
                        ((TextView) mainActivity.tuner).setText("Tuner");
                    }
                } else {
                    if (mainActivity.onOff != null && !mainActivity.onOff.isChecked()) {
                        mainActivity.toast("Starting audio engine");
                        if (patchDown != null) patchDown.performClick();
                    }
                    AudioEngine.setTunerEnabled(true);
                }
            });
        }

        if (mainActivity.pluginDialogAdapter == null) {
            mainActivity.pluginDialogAdapter = new PluginDialogAdapter();
            mainActivity.pluginDialogAdapter.setMainActivity(getContext(), mainActivity);
        }

        mainActivity.triggerRecordToggle = view.findViewById(R.id.record_trigger);
        mainActivity.record = view.findViewById(R.id.record_button);
        if (mainActivity.record != null) {
            mainActivity.record.setOnLongClickListener(v -> {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            });
        }
        if (mainActivity.triggerRecordToggle != null) {
            mainActivity.triggerRecordToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mainActivity.outputMeter != null) {
                    mainActivity.outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                }
                if (mainActivity.record != null) {
                    mainActivity.record.setEnabled(!isChecked);
                    mainActivity.record.setText(isChecked ? "Tri" : "Rec");
                }
                mainActivity.triggerRecord = isChecked;
                if (!isChecked && mainActivity.recording) {
                    AudioEngine.toggleRecording(false);
                    mainActivity.recording = false;
                }
            });
        }

        mainActivity.record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (videoRecord.isChecked()) {
                    compoundButton.setChecked(false);
                    MainActivity.toast("Cannot record audio and video at the same time");
                    return;
                }

                if (!mainActivity.onOff.isChecked()) {
//                        mainActivity.record.setChecked(!b);
                    if (b) {
                        compoundButton.setChecked(false);
                        MainActivity.toast("Turn on the app to start recording");
                    } else {
                        AudioEngine.toggleRecording(b);
                        mainActivity.recording = b ;
                        mainActivity.showMediaPlayerDialog();
                    }

                    return;
                }

                if (b) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!mainActivity.isStoragePermissionGranted()) {
    //                        requestReadStoragePermission();
                            mainActivity.requestWriteStoragePermission();

                            /*
                            if (!isStoragePermissionGranted()) {
                                Toast.makeText(getApplicationContext(),
                                        "Permission denied. Recording features are disabled.",
                                        Toast.LENGTH_LONG)
                                        .show();
                                return ;
                            }
                            */
                        } else {
    //                        AudioEngine.setRecordingActive(b);
                            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
                            Date date = new Date();
                            mainActivity.lastRecordedFileName = formatter.format(date);
                            mainActivity.lastRecordedFileName = mainActivity.dir.getAbsolutePath() + "/" + mainActivity.lastRecordedFileName ;
                            AudioEngine.setFileName(mainActivity.lastRecordedFileName);
                            switch (mainActivity.exportFormat) {
                                case "0":
                                default:
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".wav" ;
                                    break ;
                                case "1":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".ogg" ;
                                    break ;
                                case "2":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".mp3" ;
                                    break ;
                            }

                            AudioEngine.toggleRecording(b);
                            mainActivity.recording = b ;
                        }
                    } else {
                        MainActivity.alert("Feature not supported", "Your device is too old to support this feature. Sorry.");
                        return ;
                    }
                } else {
//                    mainActivity.lastRecordedFileName = AudioEngine.getRecordingFileName();
                    AudioEngine.toggleRecording(b);
                    mainActivity.recording = b ;
                    if (!mainActivity.triggerRecord)
                        mainActivity.showMediaPlayerDialog();
                }
            }
        });

        SignalChainCanvasView signalChainCanvas = view.findViewById(R.id.signalChainCanvas);
        mainActivity.signalChainCanvas = signalChainCanvas;
        mainActivity.dataAdapter = new DataAdapter();
        mainActivity.dataAdapter.mainActivity = mainActivity;
        RecyclerView hiddenRv = new RecyclerView(getContext());
        hiddenRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        hiddenRv.setVisibility(View.GONE);
        hiddenRv.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        mainActivity.recyclerView = hiddenRv;
        mainActivity.recyclerView.setAdapter(mainActivity.dataAdapter);
        // Canvas card by id: on phone the canvas sits inside scroll parents,
        // so getParent() is no longer the card FrameLayout.
        ((FrameLayout) view.findViewById(R.id.signal_chain_card)).addView(hiddenRv);

        if (signalChainCanvas != null) {
            signalChainCanvas.setOnNodeSelectedListener(node -> {
                if (node != null && node.id >= 0 && mainActivity.dataAdapter != null) {
                    String pluginName = AudioEngine.getActivePluginName(node.id);
                    mainActivity.populateInlineParameterEditor(node.id, pluginName != null ? pluginName : "");
                }
            });
            signalChainCanvas.setOnDeleteNodeListener(node -> {
                if (node != null && node.id >= 0) {
                    try {
                        AudioEngine.deletePlugin(node.id);
                        signalChainCanvas.postDelayed(() -> {
                            signalChainCanvas.loadFromActivePlugins();
                            if (mainActivity.dataAdapter != null) mainActivity.dataAdapter.notifyDataSetChanged();
                        }, 80);
                    } catch (Exception e) {
                        Log.e(TAG, "deletePlugin failed for index " + node.id, e);
                    }
                }
            });
            signalChainCanvas.setOnParallelDropListener(slot -> {
                if (mainActivity != null) mainActivity.wrapBlockInParallel(slot);
            });
            signalChainCanvas.setOnWireDeleteListener(edge -> {
                if (edge == null) return;
                AlertDialog.Builder wireBuilder = new AlertDialog.Builder(getContext());
                wireBuilder
                        .setTitle("Remove connection?")
                        .setMessage("Delete this connection from the signal chain?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            signalChainCanvas.deleteEdge(edge);
                            if (mainActivity.dataAdapter != null) mainActivity.dataAdapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("Cancel", null);
                AlertDialog wireDialog = wireBuilder.create();
                wireDialog.show();
                MainActivity.colorDialogButtons(wireDialog);
            });
//            view.postDelayed(() -> signalChainCanvas.loadFromActivePlugins(), 600);
// Chain refresh happens after loadActivePreset() completes in MainActivity
        }

        TextView cpuMeter = view.findViewById(R.id.cpu_meter_text);
        if (cpuMeter != null) {
            Handler cpuHandler = new Handler(Looper.getMainLooper());
            // Real app-process CPU usage (includes native audio threads).
            // /proc/stat is SELinux-blocked on modern Android (always threw ->
            // permanent "CPU --" placeholder); getElapsedCpuTime works everywhere.
            final long[] prevCpu = new long[2]; // [cpuMs, wallMs]
            Runnable cpuRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        long cpuMs = android.os.Process.getElapsedCpuTime();
                        long wallMs = android.os.SystemClock.elapsedRealtime();
                        if (prevCpu[1] > 0 && wallMs > prevCpu[1]) {
                            long dCpu = cpuMs - prevCpu[0];
                            long dWall = wallMs - prevCpu[1];
                            int pct = (int) Math.round(100.0 * dCpu / dWall);
                            cpuMeter.setText("CPU " + pct + "%");
                            // CPU text width just changed -> re-check top-bar fit
                            applyTopBarCollapse();
                        }
                        prevCpu[0] = cpuMs;
                        prevCpu[1] = wallMs;
                    } catch (Exception e) {
                        cpuMeter.setText("");
                    }
                    cpuHandler.postDelayed(this, 2000);
                }
            };
            cpuHandler.post(cpuRunnable);
        }

        // Real sample-rate readout (replaces the old hardcoded fake "BPM" label)
        TextView srText = view.findViewById(R.id.top_sr_text);
        if (srText != null) {
            try {
                int sr = AudioEngine.getSampleRate();
                if (sr > 0) srText.setText((sr / 1000f) + " kHz");
                else srText.setText("");
            } catch (Exception e) {
                Log.w(TAG, "sample rate read failed", e);
            }
        }

        // BPM chip: REAL tempo state — drives delay/mod time controls on change
        TextView bpmChip = view.findViewById(R.id.top_bpm_text);
        if (bpmChip != null) {
            bpmChip.setText(mainActivity.getBpmDisplay());
            bpmChip.setOnClickListener(v -> mainActivity.showBpmDialog());
        }

        // ---- Top bar wiring (B5): master volume pill slider + preset nav / quick save ----
        LinearLayout knobContainer = view.findViewById(R.id.top_volume_knob_container);
        TextView topVolVal = view.findViewById(R.id.top_volume_val);
        if (knobContainer != null) {
            knobContainer.removeAllViews();
            float density = getResources().getDisplayMetrics().density;
            // Pill SeekBar (matches lastworks build): rounded track #2E323B,
            // white RoundedProgressDrawable progress, transparent thumb.
            android.widget.SeekBar volSeek = new android.widget.SeekBar(getContext());
            android.graphics.drawable.GradientDrawable track = new android.graphics.drawable.GradientDrawable();
            track.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            float cornerPx = 7f * density;
            track.setCornerRadius(cornerPx);
            track.setColor(android.graphics.Color.parseColor("#2E323B"));
            MainActivity.RoundedProgressDrawable progress = new MainActivity.RoundedProgressDrawable(
                    android.graphics.Color.WHITE, cornerPx);
            android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{track, progress});
            layer.setId(0, android.R.id.background);
            layer.setId(1, android.R.id.progress);
            volSeek.setProgressDrawable(layer);
            volSeek.setThumb(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            volSeek.setPadding(0, 0, 0, 0);
            volSeek.setMax(10000);
            volSeek.setProgress(7936);
            LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(
                    (int) (80 * density), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            vsLp.gravity = android.view.Gravity.CENTER_VERTICAL;
            knobContainer.addView(volSeek, vsLp);
            if (topVolVal != null) {
                topVolVal.setText("80%");
            }
            volSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    try {
                        float percent = progress * 100f / 10000f;
                        AudioEngine.setOutputVolume(percent / 100f);
                        Log.d(TAG, "MASTER_VOLUME slider=" + (int) percent + "% gain=" + (percent / 100f));
                        if (topVolVal != null) {
                            topVolVal.setText((int) percent + "%");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "setOutputVolume failed", e);
                    }
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
            });

            // Tap slider or value text to input value manually (0 .. 100%)
            View.OnClickListener manualVolClick = v -> {
                if (mainActivity != null) {
                    mainActivity.showManualValueInputDialog("Master Volume",
                            volSeek.getProgress() * 100f / 10000f, 0f, 100f, "%", newVal -> {
                        volSeek.setProgress(Math.round(newVal * 100f));
                        if (topVolVal != null) {
                            topVolVal.setText((int) newVal + "%");
                        }
                        try { AudioEngine.setOutputVolume(newVal / 100f); } catch (Exception ignored) {}
                    });
                }
            };
            knobContainer.setOnClickListener(manualVolClick);
            if (topVolVal != null) {
                topVolVal.setOnClickListener(manualVolClick);
            }
        }

        TextView topPresetDisplay = view.findViewById(R.id.top_preset_display);
        // persist=false: pre-bind paint only — must not clobber the stored
        // last-used label (read back after binding / after chain restore).
        updateTopBarPresetDisplay(1, "CLEAN", false);
        ImageButton presetPrevBtn = view.findViewById(R.id.preset_prev);
        ImageButton presetNextBtn = view.findViewById(R.id.preset_next);
        if (presetPrevBtn != null) {
            presetPrevBtn.setOnClickListener(v -> onPresetNavPrev());
        }
        if (presetNextBtn != null) {
            presetNextBtn.setOnClickListener(v -> onPresetNavNext());
        }
        if (topPresetDisplay != null) {
            topPresetDisplay.setOnClickListener(v -> showPresetPicker());
            topPresetDisplay.setOnLongClickListener(v -> {
                showRenamePresetDialog();
                return true;
            });
        }

        // Responsive top bar: measure REAL laid-out widths, collapse in defined
        // order (logo -> CPU -> sample-rate) so Scene + preset name always fit.
        // Replaces the old fixed-dp budgets tuned for one screen width.
        // One-way policy: elements only collapse (restore happens on view recreate).
        final View headerBarV = view.findViewById(R.id.routing_header_bar);
        topBarForCollapse = headerBarV;
        if (headerBarV != null) {
            headerBarV.post(() -> applyTopBarCollapse());
        }
        ImageView quickSaveBtn = view.findViewById(R.id.top_quick_save);
        if (quickSaveBtn != null) {
            quickSaveBtn.setOnClickListener(v -> showQuickSaveDialog());
        }

        com.google.android.material.button.MaterialButton sceneBtn = view.findViewById(R.id.top_scene_dropdown);
        SharedPreferences scenePrefs = getContext().getSharedPreferences("giggingear_scenes", android.content.Context.MODE_PRIVATE);
        String[] defaultSceneNames = getContext().getResources().getStringArray(R.array.scene_slots);
        final java.util.ArrayList<String> sceneNames = new java.util.ArrayList<>();
        for (int i = 0; i < defaultSceneNames.length; i++) {
            sceneNames.add(scenePrefs.getString("scene_name_" + i, defaultSceneNames[i]));
        }
        if (sceneBtn != null) {
            sceneBtn.setText(sceneNames.get(0));
            sceneBtn.setOnClickListener(v -> {
                AlertDialog.Builder sceneBuilder = new AlertDialog.Builder(getContext());
                sceneBuilder
                        .setTitle("Scene Options (" + sceneNames.get(currentSceneIndex) + ")")
                        .setSingleChoiceItems(sceneNames.toArray(new CharSequence[0]), currentSceneIndex, (d, which) -> {
                            currentSceneIndex = which;
                            sceneBtn.setText(sceneNames.get(which));
                            String json = scenePrefs.getString("scene_" + which, null);
                            if (json != null && !json.isEmpty() && mainActivity != null) {
                                mainActivity.loadPreset(json);
                            }
                            d.dismiss();
                        })
                        .setPositiveButton("Save To Current Scene", (d, w) -> {
                            saveCurrentChainToScene();
                        })
                        .setNeutralButton("Rename", (d, w) -> {
                                int which = currentSceneIndex;
                                AlertDialog.Builder rb = new AlertDialog.Builder(getContext());
                                rb.setTitle("Rename " + sceneNames.get(which));
                                final EditText input = new EditText(getContext());
                                input.setSingleLine(true);
                                input.setText(sceneNames.get(which));
                                input.setSelectAllOnFocus(true);
                                rb.setView(input);
                                rb.setNegativeButton("Cancel", null);
                                rb.setPositiveButton("Rename", (rd, rw) -> {
                                    String nm = input.getText().toString().trim();
                                    if (!nm.isEmpty()) {
                                        sceneNames.set(which, nm);
                                        scenePrefs.edit().putString("scene_name_" + which, nm).apply();
                                        sceneBtn.setText(nm);
                                    }
                                });
                                AlertDialog rd = rb.create();
                                rd.show();
                                MainActivity.colorDialogButtons(rd);
                            })
                        .setNegativeButton("Close", null);
                AlertDialog sceneDialog = sceneBuilder.create();
                sceneDialog.show();
                MainActivity.colorDialogButtons(sceneDialog);
            });
            sceneBtn.setOnLongClickListener(v -> {
                saveCurrentChainToScene();
                return true;
            });
        }

        // ---- Safe-area / rounded-corner insets (B4) ----
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            int top = Math.max(bars.top, cutout.top);
            int left = Math.max(bars.left, cutout.left);
            int right = Math.max(bars.right, cutout.right);
            if (Build.VERSION.SDK_INT >= 31) {
                WindowInsets rootInsets = v.getRootWindowInsets();
                if (rootInsets != null) {
                    RoundedCorner rcTL = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
                    RoundedCorner rcTR = rootInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT);
                    if (rcTL != null) left = Math.max(left, rcTL.getRadius());
                    if (rcTR != null) right = Math.max(right, rcTR.getRadius());
                }
            }
            v.setPadding(left, top, right, v.getPaddingBottom());
            return insets;
        });

        com.google.android.material.button.MaterialButton fabAddBlock = view.findViewById(R.id.fab_add_block);
        if (fabAddBlock != null) {
            fabAddBlock.setOnClickListener(v -> {
                PluginPickerFragment.newInstance().show(getParentFragmentManager(), "plugin_picker");
            });
        }

        File extDir = getContext().getExternalFilesDir(null);
        if (extDir == null) extDir = getContext().getFilesDir();
        String path = extDir.getAbsolutePath();

        AudioEngine.setExternalStoragePath(path);
        File defaultDir = new File (path + "/AmpRack/") ;
        if (!defaultDir.exists()) {
            Log.d(TAG, "making directory " + path + "/AmpRack/");
            try {
                if (!defaultDir.mkdir())
                    Log.wtf (TAG, "Unable to create directory!");
            }  catch (Exception e) {
                Log.w(TAG, "UNable to create directory: " + e.getMessage());
            }
        }
        AudioEngine.setDefaultStreamValues(getContext());
        if (mainActivity.lazyLoad == false)
            mainActivity.loadPlugins();
        mainActivity.loadActivePreset();

        SignalChainCanvasView finalCanvas = signalChainCanvas;
        final View finalRootView = view;
        view.postDelayed(() -> {
            try {
                if (finalCanvas != null) {
                    finalCanvas.loadFromActivePlugins();
                    centerCanvasHomeView(finalRootView, finalCanvas);
                }
                if (AudioEngine.getActivePlugins() > 0) {
                    String name0 = null;
                    try { name0 = AudioEngine.getActivePluginName(0); } catch (Exception ignore) {}
                    mainActivity.populateInlineParameterEditor(0, name0 != null ? name0 : "");
                }
            } catch (Exception e) {
                Log.e(TAG, "postLoad chain refresh failed", e);
            }
        }, 600);

        int libraries = 0 ;
        if (mainActivity.lazyLoad == false)
            libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "blacklist.json");
        boolean enableBlacklisted = mainActivity.defaultSharedPreferences.getBoolean("enableBlacklisted", false);

        // run this only once
        if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad == false) {
            for (int i = 0; i < libraries; i++) {
                for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                    JSONObject object = new JSONObject();
                    String name = AudioEngine.getPluginName(i, plugin);
                    int uniqueID = AudioEngine.getPluginUniqueID(i, plugin);

                    // Skip blacklisted plugins in plugin dialog
                    if (blacklist != null && blacklist.has(name) && !enableBlacklisted) {
                        Log.d(TAG, String.format("[lv2] blacklisted: %s", name));
                        continue;
                    }

                    int finalI = i;
                    int finalPlugin = plugin;

                    try {
                        object.put("name", name);
                        object.put("id", uniqueID);
                        object.put("plugin", finalPlugin);
                        object.put("library", mainActivity.sharedLibraries[i]);
                        jsonObject.put(String.valueOf((finalI * 100) + finalPlugin), object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.d("Plugin: ", finalI * 100 + finalPlugin + ": " + name + "=>" + uniqueID);
                    mainActivity.pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name, uniqueID, name);
                }
            }
        } else if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad) {
            JSONObject plugins = mainActivity.availablePluginsLV2 ;
            Iterator<String> keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.optString("name", "");
                        if (blacklist != null && blacklist.has(name) && ! enableBlacklisted) {
                            Log.d(TAG, String.format ("[lv2] blacklisted: %s", name));
                            continue;
                        }
                        String id = object.optString("id", "0");
                        String desc = object.optString("desc", "");
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id), desc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            plugins = mainActivity.availablePlugins ;
            keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.optString("name", "");
                        String desc = object.optString("desc", "");
                        String id = object.optString("id", "0");
                        if (blacklist != null && blacklist.has(name) && ! enableBlacklisted) {
                            Log.d(TAG, String.format ("[ladspa] blacklisted: %s", name));
                            continue;
                        }
//                        Log.d(TAG, "[LV2 plugin]: " + name + ": " + id);
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id), desc);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        MainActivity.totalPlugins = mainActivity.pluginDialogAdapter.plugins.size();
        mainActivity.hashCommands.add (this, "printPlugins");
        mainActivity.hashCommands.add (this, "writeJSON");

        MaterialButton optionsBtn = view.findViewById(R.id.menu_button);
        optionsMenu = new PopupMenu(mainActivity, optionsBtn);
        optionsMenu.getMenuInflater().inflate(R.menu.options_menu, optionsMenu.getMenu());

        MenuItem settings = optionsMenu.getMenu().findItem(R.id.settings_menu_item);
        MenuItem logout = optionsMenu.getMenu().findItem(R.id.logout_menu_item);
        MenuItem debug = optionsMenu.getMenu().findItem(R.id.debug_menu_item);
        MenuItem getPro = optionsMenu.getMenu().findItem(R.id.get_pro);
        if (mainActivity.defaultSharedPreferences.getBoolean("pro", false)) {
            getPro.setVisible(false);
            TextView textView = view.findViewById(R.id.app_main_title);
            textView.setText("Pro");
            (view.findViewById(R.id.pro_label)).setVisibility(View.VISIBLE);
        }

        MenuItem connectGuitar = optionsMenu.getMenu().getItem(4);
        connectGuitar.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getActivity(), ConnectGuitar.class);
                startActivity(intent);

                return true;
            }
        });

        MenuItem bug_item = optionsMenu.getMenu().getItem(5);
        MenuItem featureItem = optionsMenu.getMenu().getItem(6);
        MenuItem.OnMenuItemClickListener menuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                LayoutInflater inflater = getLayoutInflater();

                ConstraintLayout linearLayout = (ConstraintLayout) inflater.inflate(R.layout.bug_report, null) ;
                builder.setView(linearLayout) ;

                EditText title = linearLayout.findViewById(R.id.bug_title);
                EditText desc = linearLayout.findViewById(R.id.bug_description);
                EditText email = linearLayout.findViewById(R.id.bug_email);
                CheckBox notify = linearLayout.findViewById(R.id.bug_notify);

                AlertDialog alertDialog = builder.create();
                MaterialButton submit = linearLayout.findViewById(R.id.bug_submit);
                submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        saveBugReport(alertDialog, title.getText().toString(), desc.getText().toString(),
                                email.getText().toString(), notify.isChecked());
                    }
                });

                alertDialog.show();
                MainActivity.colorDialogButtons(alertDialog);
                return true;
            }
        } ;

        bug_item.setOnMenuItemClickListener(menuItemClickListener) ;
        featureItem.setOnMenuItemClickListener(menuItemClickListener) ;

        MenuItem howToUse = optionsMenu.getMenu().getItem(7);
        howToUse.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String url = "https://amprack.acoustixaudio.org";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

                return false;
            }
        });

        MenuItem saveCollection = optionsMenu.getMenu().getItem(8);
        saveCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mainActivity.saveCollection();
                return false;
            }
        });

        MenuItem loadCollection = optionsMenu.getMenu().getItem(9);
        loadCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent_upload = new Intent();
//                intent_upload.setType("application/json");
                intent_upload.setType("*/*");
                intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().startActivityForResult(intent_upload,100);
                return true;
            }
        });

        MenuItem tuneLatency = optionsMenu.getMenu().getItem(10);
        tuneLatency.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
//                MainActivity.toast(AudioEngine.tuneLatency());
                latencyDialog();
                return false;
            }
        });

        MenuItem sync_item = optionsMenu.getMenu().findItem(R.id.menu_sync);
        sync_item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                syncDialog();
                return false;
            }
        });

        MenuItem nam = optionsMenu.getMenu().findItem(R.id.nam);
        nam.setVisible(false);
        nam.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                startActivity(new Intent(mainActivity, NAMDownloader.class));
                return false;
            }
        });

        MenuItem ytMenu = optionsMenu.getMenu().findItem(R.id.youtube);
        ytMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                youtubePlayer.show();
//                ((WebView) youtubePlayer.findViewById(R.id.webview)).loadUrl("https://youtube.com");
                return false;
            }
        });

        MenuItem clear_midi = optionsMenu.getMenu().findItem(R.id.clear_midi);
        clear_midi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.midiControls.clear();
                Toast.makeText(mainActivity, "MIDI Controls cleared", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        mainActivity.midiDisplay = mainActivity.findViewById(R.id.midi_display);
        MenuItem bt_menu = optionsMenu.getMenu().findItem(R.id.bt_menu);
        bt_menu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.scanBLE();
                return false;
            }
        });

        MenuItem midiSelect = optionsMenu.getMenu().findItem(R.id.menu_select_midi);
        midiSelect.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                mainActivity.selectMidiDevice();
                return false;
            }
        });

        MenuItem downloadPC = optionsMenu.getMenu().findItem(R.id.download_pc);
        downloadPC.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                String url = "https://amprack.in";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return false;
            }
        });

        downloadPC.setVisible(MainActivity.proVersion);

        MenuItem exit_item = optionsMenu.getMenu().findItem(R.id.menu_exit);
        exit_item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mainActivity.finish();
                return true;
            }
        });

        debug.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
//                mainActivity.printDebugLog();
                AudioEngine.clearActiveQueue();
                mainActivity.dataAdapter.reset();
                mainActivity.lastPresetLoadedUID = null ;
                mainActivity.lastPresetLoadedPath = null ;
                mainActivity.patchName.setText("");
                mainActivity.patchDesc.setText("");
                return true;
            }
        });

        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return false;
            }
        });

        logout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("App running in 100% offline kiosk mode.")
                        .setTitle("Giggin' Gear Kiosk");

                builder.setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(mainActivity.getApplicationContext(),
                                "Running in offline mode.",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                MainActivity.colorDialogButtons(dialog);

                return false;
            }
        });



        optionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsMenu.show();
            }
        });

        TextView midi_map = mainActivity.findViewById(R.id.midi_port_mappings);
        if (midi_map != null) {
            midi_map.setOnClickListener(view1 -> mainActivity.midiMappingsDialog());
        }

        TextView midiDevice = mainActivity.findViewById(R.id.midi_port_button);
        if (midiDevice != null) {
            midiDevice.setOnClickListener(view1 -> mainActivity.selectMidiDevice());
        }

        View midiBt = mainActivity.findViewById(R.id.midi_bt_btn);
        if (midiBt != null) {
            midiBt.setOnClickListener(view1 -> mainActivity.scanBLE());
        }

        ImageView logoBtn = view.findViewById(R.id.logo_img);
        if (logoBtn != null) {
            logoBtn.setLongClickable(true);
            logoBtn.setOnLongClickListener(view1 -> {
                HashCommands commands = new HashCommands(mainActivity.context);
                commands.mainActivity = mainActivity;
                commands.show();
                return true;
            });
            logoBtn.setOnClickListener(v -> MainActivity.setAudioDevice());
        }

        mixer = mainActivity.findViewById(R.id.mixer);
        mainActivity.toggleMixer = mainActivity.findViewById(R.id.mixer_toggle);
        if (mainActivity.toggleMixer != null) {
            mainActivity.toggleMixer.setOnLongClickListener(v -> {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            });
            mainActivity.toggleMixer.setOnCheckedChangeListener((compoundButton, b) -> {
                AudioEngine.toggleMixer(!b);
                    
                if (mixer != null) mixer.setVisibility(GONE);
            });
            mainActivity.toggleMixer.setChecked(mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", false));
        }

        mainActivity.hidePanel = mainActivity.findViewById(R.id.hide_panel);
        if (mainActivity.hidePanel != null) {
            mainActivity.hidePanel.setOnClickListener(v -> {
                if (mainActivity.toggleMixer != null) {
                    mainActivity.toggleMixer.setChecked(!mainActivity.toggleMixer.isChecked());
                }
            });
        }

        mainActivity.inputVolume = mainActivity.findViewById(R.id.mixer_input_slider);
        mainActivity.outputVolume = mainActivity.findViewById(R.id.mixer_output_slider);
        mainActivity.inputMeter = mainActivity.findViewById(R.id.meter_input);
        mainActivity.outputMeter = mainActivity.findViewById(R.id.meter_output);

        if (mainActivity.inputMeter != null) mainActivity.inputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        if (mainActivity.outputMeter != null) mainActivity.outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

        if (mainActivity.inputVolume != null) {
            mainActivity.inputVolume.addOnChangeListener((slider, value, fromUser) -> AudioEngine.setInputVolume(value));
            mainActivity.inputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f));
        }
        if (mainActivity.outputVolume != null) {
            mainActivity.outputVolume.addOnChangeListener((slider, value, fromUser) -> AudioEngine.setOutputVolume(value));
            mainActivity.outputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f));
        }

        ToggleButton toggleButton = mainActivity.findViewById(R.id.onofftoggle);
        if (toggleButton != null) {
            toggleButton.setOnLongClickListener(v -> {
                mainActivity.setMidiControl(toggleButton, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            });
        }

        patchUp = mainActivity.findViewById(R.id.patch_up);
        if (patchUp != null) {
            patchUp.setOnLongClickListener(v -> {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            });
        }

        TextView patchName = mainActivity.findViewById(R.id.patch_name);
        mainActivity.patchName = patchName;
        TextView patchDesc = mainActivity.findViewById(R.id.patch_desc);
        mainActivity.patchDesc = patchDesc;
        mainActivity.patchNo = null; // replaced by per-selection top_preset_display update
        // skip syncTopBarPresetInfo here; updateTopBarPresetDisplay() handles it on node selection
        // Startup re-sync (clean-install "004 Brit" bug): the (1, "CLEAN") update at
        // view creation ran BEFORE patchName was bound, so syncTopBarPresetInfo()
        // early-returned on null patchName and the XML placeholder stuck forever.
        // Re-fire now that the binding exists via the stored last-used label
        // (falls back to "1. CLEAN" when prefs are absent). Idempotent: the chain
        // restore path paints the same stored label.
        if (mainActivity != null) mainActivity.restoreTopBarLabel();

        if (patchDesc != null) {
            patchDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (patchName != null) patchName.performClick();
                }
            });
        }

        LinearLayout patchMaster = mainActivity.findViewById(R.id.patch_master);
        patchMaster.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (patchName.getText().equals("Tap to load"))
                patchName.performClick();
                return false;
            }
        });

        patchName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (! patchName.getText().equals("Tap to load"))
//                    return false;

                if (! mainActivity.running) {
                    if (patchName.getText().equals("Tap to load") && mainActivity.dataAdapter.totalItems == 0) {
                        MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                            @Override
                            void run() {
                                patchDown.performClick();
                            }
                        };
                    } else if (patchName.getText().equals("Tap to load")){
                        patchName.setText("Custom");
                    }

                    if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                        mainActivity.onOff.setChecked(true);

                    else if (mainActivity.useTheme && ! toggleButton.isChecked())
                        toggleButton.setChecked(true);

                } else {
                    if (patchName.getText().equals("Tap to load"))
                        patchDown.performClick();
                }
                return false;
            }
        });
        patchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(true);
                        }
                    };

                } else {
                    patchMove(true);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);

            }
        });

        patchDown.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mainActivity.setMidiControl(v, -1, -1, MIDIControl.Type.TOGGLE, MIDIControl.Scope.GLOBAL);
                return false;
            }
        });
        patchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(false);
                        }
                    };
                } else {
                    patchMove(false);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);
            }
        });

        TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label),
                inLabel = mainActivity.findViewById(R.id.mixer_input_label),
                patchLabel = mainActivity.findViewById(R.id.patch_label),
                inRotaryDisplay = mainActivity.findViewById(R.id.rotary_input_display),
                inRotaryLabel = mainActivity.findViewById(R.id.rotary_input_label),
                outRotaryDisplay = mainActivity.findViewById(R.id.rotary_output_display),
                outRotaryLabel = mainActivity.findViewById(R.id.rotary_output_label),
                outLabel = mainActivity.findViewById(R.id.mixer_output_label);


        if (true) {
            inLabel.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(mainActivity.inputVolume, -1, -1, MIDIControl.Type.SLIDER, MIDIControl.Scope.GLOBAL);
                    return false;
                }
            });

            outLabel.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(mainActivity.outputVolume, -1, -1, MIDIControl.Type.SLIDER, MIDIControl.Scope.GLOBAL);
                    return false;
                }
            });
        }

        if (mainActivity.useTheme) {
            
            
            
            mainActivity.toggleMixer.setCompoundDrawables(null,null,null,null);

            
            
            
            
            

            
            
            
            

            boolean mixerIsVisible  = false ;
            if (mixer.getVisibility() == View.VISIBLE)
                mixerIsVisible = true ;

            

            ImageView wallpaper = mainActivity.findViewById(R.id.wallpaper);
            // Disabled legacy wallpaper and header texturing to allow clean pedalboard canvas!
            // mainActivity.skinEngine.wallpaper(wallpaper);
            // mainActivity.skinEngine.header(mainActivity.findViewById(R.id.master_button_box));

            
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "onCheckedChanged() called with: buttonView = [" + buttonView + "], isChecked = [" + isChecked + "]");
//                    mainActivity.toggleEffect(!isChecked);
                    mainActivity.onOff.setChecked(isChecked);
                    
                }
            });

            toggleButton.setVisibility(View.VISIBLE);
            mainActivity.onOff.setVisibility(GONE);

            
            optionsBtn.setCompoundDrawables(null, null, null, null);
            

            
            
            

            if (mixer.getVisibility() == View.VISIBLE) {
                mixerInit = true ;
//                

                mixer.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: theming mixer");
                        
                        

                    }
                });
            }

            if (true) {
                LinearLayout rotaryRack = mainActivity.findViewById(R.id.rotary_rack);
                rotaryRack.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn = mainActivity.findViewById(R.id.rotary_input_volume);
                mainActivity.rotarySeekbarOut = mainActivity.findViewById(R.id.rotary_output_volume);
                mainActivity.displayIn = mainActivity.findViewById(R.id.rotary_input_display);
                mainActivity.displayOut = mainActivity.findViewById(R.id.rotary_output_display);
                

                
                mainActivity.rotarySeekbarIn.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f) * 100);
                mainActivity.rotarySeekbarOut.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)*100);
                mainActivity.displayIn.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f)* 100)));
                mainActivity.displayOut.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)* 100)));

                mainActivity.rotarySeekbarIn.setVisibility(View.VISIBLE);
                mainActivity.rotarySeekbarOut.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn.setOnValueChangedListener(new RotarySeekbar.OnValueChangedListener() {
                    @Override
                    public void onValueChanged(RotarySeekbar sourceSeekbar, float value) {
                        mainActivity.displayIn.setText(String.valueOf((int) value));
                        mainActivity.inputVolume.setValue(value/100);

                    }
                });

                mainActivity.rotarySeekbarIn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarIn.getValue() ;
                        mainActivity.displayIn.setText(String.valueOf((int) value));
                        mainActivity.inputVolume.setValue(value/100);
                        return false;
                    }
                });

                mainActivity.rotarySeekbarOut.setOnValueChangedListener(new RotarySeekbar.OnValueChangedListener() {
                    @Override
                    public void onValueChanged(RotarySeekbar sourceSeekbar, float value) {
                        mainActivity.displayOut.setText(String.valueOf((int) value));
                        mainActivity.outputVolume.setValue(value/100);
                    }
                });

                mainActivity.rotarySeekbarOut.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarOut.getValue() ;
                        mainActivity.displayOut.setText(String.valueOf((int) value));
                        mainActivity.outputVolume.setValue(value/100);
                        return false;
                    }
                });


                inRotaryLabel.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mainActivity.setMidiControl(mainActivity.rotarySeekbarIn, -1, -1, MIDIControl.Type.KNOB, MIDIControl.Scope.GLOBAL);
                        return false;
                    }
                });

                outRotaryLabel.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mainActivity.setMidiControl(mainActivity.rotarySeekbarOut, -1, -1, MIDIControl.Type.KNOB, MIDIControl.Scope.GLOBAL);
                        return false;
                    }
                });

                mainActivity.inputVolume.setVisibility(GONE);
                mainActivity.outputVolume.setVisibility(GONE);
            }
        }

//        mainActivity.audioEncoder = new AudioEncoder(mainActivity);
//        mainActivity.camera2 = new Camera2 (mainActivity);
        mainActivity.sampleRateLabel = mainActivity.findViewById(R.id.sample_rate_display);
        mainActivity.srLayout = mainActivity.findViewById(R.id.sr_layout);
        mainActivity.latencyWarnLogo = mainActivity.findViewById(R.id.lowLatencyWarning);
        mainActivity.latencyWarnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.lowLatencyDialog();
            }
        });

        if (mainActivity.tabletMode) {
            Log.w(TAG, "onViewCreated: tablet mode activated, but 50/50 split drawer is disabled. Using full screen pedalboard canvas.");
            if (mainActivity.fab != null) mainActivity.fab.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, String.format ("[display dimensions]: %d x %d {%f}", mainActivity.deviceWidth, mainActivity.deviceHeight, (float) (1.0 * mainActivity.deviceWidth/mainActivity.deviceHeight)));
        }

        if (mainActivity.experimentalBuild) {
//            TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label);
            mixerLabel.setText("Beta " + String.valueOf(BuildConfig.VERSION_CODE));
//            mixerLabel.setTextColor(getResources().getColor(R.color.dark_red));
//            mixerLabel.setBackgroundColor(getResources().getColor(R.color.wheat));
        }

        youtubePlayer = new Dialog(mainActivity);
        youtubePlayer.setContentView(mainActivity.getLayoutInflater().inflate(R.layout.youtube, null));

        WebView yt = youtubePlayer.findViewById(R.id.webview);
        yt.getSettings().setJavaScriptEnabled(true);
        yt.getSettings().setDomStorageEnabled(true);
        yt.setWebViewClient(new WebViewClient());

        ((Button)(youtubePlayer.findViewById(R.id.patch_up))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patchUp.performClick();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.reload))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WebView) youtubePlayer.findViewById(R.id.webview)).reload();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.prev))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WebView) youtubePlayer.findViewById(R.id.webview)).goBack();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.next))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WebView) youtubePlayer.findViewById(R.id.webview)).goForward();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.close))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                youtubePlayer.hide();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.power))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.onOff.performClick();
            }
        });

        ((Button)(youtubePlayer.findViewById(R.id.patch_down))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patchDown.performClick();
            }
        });

        yt.loadUrl("https://youtube.com");
    }

    public void saveBugReport (AlertDialog dialog, String title, String description, String email, boolean notify) {
        dialog.dismiss();
        Toast.makeText(mainActivity, "Bug report saved locally in offline mode.", Toast.LENGTH_SHORT).show();
    }

    public void patchMove (boolean up) {
        if (mainActivity.patchNo == null) return; // retired by top-bar preset display
        String text = String.valueOf(mainActivity.patchNo.getText()) ;
        if (text == "-")
            text = "0" ;
        int p = Integer.valueOf(text);
        if (up)
            p ++ ;
        else
            p -- ;

        if (p >= mainActivity.quickPatch.myPresetsAdapter.allPresets.size() || p < 0)
            return ;

        if (mainActivity.quickPatch.myPresetsAdapter.allPresets.isEmpty()) {
            MainActivity.alert("Restart the app to load patches", "Patches are not loaded. Restart the app to load patches.");
            return ;
        }

        mainActivity.loadPreset(mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p));
        mainActivity.patchNo.setText(String.valueOf(p));
        mainActivity.patchName.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("name"));
        mainActivity.patchDesc.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("desc"));

    }

    public void latencyDialog () {
        LinearLayout linearLayout = (LinearLayout) mainActivity.getLayoutInflater().inflate(R.layout.latency_tuner, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Latency Tuner")
                .setView(linearLayout)
                .setPositiveButton("Close", null);

        Button latency  = linearLayout.findViewById(R.id.latency);
        latency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

//                AudioEngine.setBufferSizeFactor(0.5f);
                AudioEngine.latencyTuner();
                latency.setText(
                        String.format( "%.0f ms",
                                AudioEngine.getLatency(true) +
                                AudioEngine.getLatency(false)
                        )
                );
            }
        });

        if (mainActivity.running)
            latency.performClick();
        else
            Toast.makeText(mainActivity, "Turn on the audio engine to use Latency Tuner", Toast.LENGTH_SHORT).show();

        Button ato = linearLayout.findViewById(R.id.autofix);
        ato.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.tuneLatency();
                latency.performClick();
            }
        });

        Button minimize = linearLayout.findViewById(R.id.minimize);
        minimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.minimizeLatency();
                latency.performClick();
            }
        });

        Button glitch = linearLayout.findViewById(R.id.glitch);
        glitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    Toast.makeText(mainActivity, "Audio Engine not running", Toast.LENGTH_SHORT).show();
                    return;
                }

                AudioEngine.fixGlitches();
                latency.performClick();
            }
        });

        android.os.Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mainActivity.running)
                    latency.performClick();
                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "onDismiss: closing handler");
                handler.removeCallbacks(r);
            }
        });

        builder.show();
    }

    void syncDialog () {
        LinearLayout linearLayout = (LinearLayout) mainActivity.getLayoutInflater().inflate(R.layout.sync_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Sync with PC / Smart Pedal")
                .setView(linearLayout)
                .setPositiveButton("Close", null);

        Button download = linearLayout.findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://amprack.in"));
                startActivity(browserIntent);

            }
        });
        Button btn = linearLayout.findViewById(R.id.sync);
        EditText editText = linearLayout.findViewById(R.id.ip);
        AlertDialog dialog = builder.create();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity.presets.fragmentStateAdapter == null) {
                    MainActivity.alert("Presets not loaded", "Load presets first by switching to the presets Tab.");
                    return;
                }

                String text = editText.getText().toString();
                if (text.isEmpty())
                    return;

//                String host = text.split(":")[0];
//                int port = Integer.parseInt(text.split(":")[1]);

                JSONObject collection = new JSONObject();
                for (int i = 0 ; i < mainActivity.presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.size() ; i ++) {
                    try {
                        JSONObject jsonObject = new JSONObject(mainActivity.presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.get(i)) ;
                        collection.put(String.valueOf(i), jsonObject);
                    } catch (JSONException e) {
                        Log.e(TAG, "[sync]: ", e);
                    }
                }

                Log.d(TAG, "[sync]: got presets: " + collection.toString());
                SyncTask syncTask = new SyncTask();
                String  data [] = {
                        text.split (":") [0],
                        text.split (":") [1],
                        collection.toString() + "}"
                };

                try {
                    String result = syncTask.execute(data).get();
                    Log.d(TAG, "[sync]: " + result);

                    if (result == null || result.isEmpty()) {
                        Log.w(TAG, "[sync]: no presets received from server");
                        Toast.makeText(mainActivity, "No presets received from server", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    JSONObject j = new JSONObject (result);
//                    j.put("uid", FirebaseAuth.getInstance().getUid());
                    Log.d(TAG, "[sync json]: " + j.toString());
                    Iterator<String> keys = j.keys();

//                    while(keys.hasNext()) {
//                        String key = keys.next();
//                        JSONObject jo = j.getJSONObject(key);
//                        Log.d(TAG, "[preset]: " + jo.toString());
//                    }

                    Log.d(TAG, "[sync]: end presets processing");
                } catch (ExecutionException | InterruptedException | JSONException e) {
//                    throw new RuntimeException(e);
                    Log.e(TAG, "[sync]: ", e);
                    Toast.makeText(mainActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
        MainActivity.colorDialogButtons(dialog);
    }

    public String syncData(String host, int port, String toSend){
        Socket socket ;
        PrintWriter out ;
        BufferedReader in ;
        String result = "";

        try{
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(),
                    true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + host);
            return result;
        } catch  (IOException e) {
            System.out.println("No I/O");
            return result;
        }

        out.println(toSend);

        // Receive text from server
        try{
            String line = in.readLine();
            Log.d(TAG, "syncData: Text received: " + line);
            result = line ;
        } catch (IOException e){
            Log.d(TAG, "syncData: Read failed");
            return result;
        }

        return result;
    }

    private class SyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params)
        {
            String host = params[0];
            int port = Integer.parseInt(params[1]);
            String toSend = params [2];
            return syncData(host, port, toSend);
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            // do something with the result
        }
    }

    private void updateTopBarPresetDisplay(int presetNumber, String presetName) {
        updateTopBarPresetDisplay(presetNumber, presetName, true);
    }

    private void updateTopBarPresetDisplay(int presetNumber, String presetName, boolean persist) {
        if (presetNumber > 0) currentPresetNumber = presetNumber;
        String label = presetName != null ? presetName : "";
        if (label.matches("^\\d+\\s+.*")) {
            label = label.replaceFirst("^\\d+\\s+", "");
        }
        if (mainActivity != null && mainActivity.patchName != null && presetName != null) {
            mainActivity.patchName.setText(presetName);
        }
        if (mainActivity != null) {
            mainActivity.syncTopBarPresetInfo(currentPresetNumber);
        } else {
            TextView display = getView() != null ? getView().findViewById(R.id.top_preset_display) : null;
            if (display != null) {
                // Format: "1. PRESET NAME" with orange prefix
                String num = String.valueOf(currentPresetNumber);
                SpannableStringBuilder sb = new SpannableStringBuilder(num + ". " + label.toUpperCase(java.util.Locale.US));
                // Color the number and dot orange
                int orange = android.graphics.Color.parseColor("#FF8A00");
                sb.setSpan(new ForegroundColorSpan(orange), 0, num.length() + 1, 
                           android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                display.setText(sb);
            }
        }
        if (persist) {
            try {
                android.content.SharedPreferences.Editor ed = mainActivity.getPreferences(0).edit();
                ed.putString("topPresetName", label);
                ed.putInt("topPresetNumber", currentPresetNumber);
                ed.apply();
            } catch (Exception e) {
                Log.w(TAG, "persist top preset label failed", e);
            }
        }
    }

    private int getPresetCount() {
        MainActivity ma = mainActivity;
        if (ma == null || ma.quickPatch == null || ma.quickPatch.myPresetsAdapter == null) return 128;
        return ma.quickPatch.myPresetsAdapter.allPresets.size();
    }

    @SuppressWarnings("unchecked")
    private void selectPresetByIndex(int index) {
        MainActivity ma = mainActivity;
        if (ma == null || ma.quickPatch == null || ma.quickPatch.myPresetsAdapter == null) return;
        ArrayList<Map> all = ma.quickPatch.myPresetsAdapter.allPresets;
        if (index < 0 || index >= all.size()) return;
        Object o = all.get(index);
        if (!(o instanceof Map)) {
            Log.w(TAG, "selectPresetByIndex: non-Map preset at " + index + ", skipping");
            MainActivity.toast("Cannot load preset: bad format");
            return;
        }
        Map preset = (Map) o;
        String name = String.valueOf(preset.getOrDefault("name", "PRESET"));
        updateTopBarPresetDisplay(currentPresetNumber, name);
        try {
            ma.loadPreset(preset);
        } catch (Exception e) {
            Log.w(TAG, "selectPresetByIndex load failed", e);
        }
    }

    /** Prev/next preset navigation (lastworks behavior). Number is 1-based; index = number-1. */
    private void onPresetNavPrev() {
        int target = currentPresetNumber - 2;
        int count = getPresetCount();
        Log.d(TAG, "PRESET_NAV prev requested: current=" + currentPresetNumber + " target=" + target + " count=" + count);
        if (target < 0) {
            MainActivity.toast("Already at first preset");
            return;
        }
        currentPresetNumber = target + 1;
        selectPresetByIndex(target);
    }

    private void onPresetNavNext() {
        int target = currentPresetNumber;
        int count = getPresetCount();
        Log.d(TAG, "PRESET_NAV next requested: current=" + currentPresetNumber + " target=" + target + " count=" + count);
        if (target >= count) {
            MainActivity.toast("Already at last preset");
            return;
        }
        currentPresetNumber = target + 1;
        selectPresetByIndex(target);
    }

    /** Floating preset picker (lastworks behavior): tap preset display to open. */
    private void showPresetPicker() {
        MainActivity ma = mainActivity;
        if (ma == null || ma.quickPatch == null || ma.quickPatch.myPresetsAdapter == null) return;
        java.util.List<Map> all = ma.quickPatch.myPresetsAdapter.allPresets;
        if (all == null || all.isEmpty()) {
            MainActivity.toast("No presets loaded");
            return;
        }
        android.graphics.drawable.GradientDrawable containerBg = new android.graphics.drawable.GradientDrawable();
        containerBg.setColor(android.graphics.Color.parseColor("#272B33"));
        containerBg.setCornerRadius(dp(10));
        android.graphics.drawable.GradientDrawable rowBg = new android.graphics.drawable.GradientDrawable();
        rowBg.setColor(android.graphics.Color.parseColor("#14FFFFFF"));
        rowBg.setCornerRadius(dp(10));
        final androidx.appcompat.app.AlertDialog[] dialogRef = new androidx.appcompat.app.AlertDialog[1];
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        container.setPadding(pad, pad, pad, pad);
        int current = Math.max(1, currentPresetNumber);
        for (int i = 0; i < all.size(); i++) {
            Map preset = all.get(i);
            if (!(preset instanceof Map)) {
                Log.w(TAG, "showPresetPicker: skipping non-Map preset at " + i);
                continue;
            }
            String name = preset.get("name") != null ? preset.get("name").toString() : "Unnamed";
            String origin = preset.get("origin") != null ? preset.get("origin").toString() : "";
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int rp = dp(10);
            row.setPadding(rp, rp, rp, rp);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(6));
            row.setLayoutParams(rowLp);
            boolean selected = (i + 1) == current;
            row.setBackground(selected ? rowBg : containerBg);
            TextView num = new TextView(getContext());
            num.setText(String.format(java.util.Locale.US, "%d.", i + 1));
            num.setTextColor(android.graphics.Color.parseColor("#FFAA00"));
            num.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            num.setTextSize(13f);
            row.addView(num);
            TextView label = new TextView(getContext());
            label.setText(name);
            label.setTextColor(android.graphics.Color.WHITE);
            label.setTextSize(13f);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            labelLp.setMargins(dp(10), 0, dp(6), 0);
            label.setLayoutParams(labelLp);
            row.addView(label);
            if (!origin.isEmpty()) {
                TextView tag = new TextView(getContext());
                tag.setText("FACTORY".equals(origin.toUpperCase(java.util.Locale.US)) ? "FACTORY" : "MY");
                tag.setTextColor("FACTORY".equals(origin.toUpperCase(java.util.Locale.US))
                        ? android.graphics.Color.parseColor("#52B788")
                        : android.graphics.Color.parseColor("#8F94A0"));
                tag.setTextSize(10f);
                row.addView(tag);
            }
            final int index = i;
            row.setOnClickListener(v -> {
                currentPresetNumber = index + 1;
                selectPresetByIndex(index);
                if (dialogRef[0] != null) dialogRef[0].dismiss();
            });
            container.addView(row);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(getContext());
        scroll.addView(container);
        androidx.appcompat.app.AlertDialog.Builder builder = MainActivity.dialogBuilder(getContext());
        builder.setTitle("Select Preset");
        builder.setView(scroll);
        builder.setNegativeButton("Cancel", null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
        MainActivity.colorDialogButtons(dialogRef[0]);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    void onPresetLoadedFromList(int position, String fPresetName) {
        if (mainActivity == null) return;
        currentPresetNumber = position + 1;
        String name = fPresetName != null ? fPresetName : "";
        if (mainActivity.patchName != null) mainActivity.patchName.setText(name);
        mainActivity.syncTopBarPresetInfo(currentPresetNumber);
        updateTopBarPresetDisplay(currentPresetNumber, name);
    }

    private View topBarForCollapse;

    /** Phone-class scrollable canvas: default "home" view = the series row
     *  (IN -&gt; OUT) centered in the viewport. Runs once per Board creation
     *  (tag on the scroller); later chain rebuilds preserve the user's scroll
     *  position instead of yanking it back. Tablet no-op: the bool is false
     *  in values-sw600dp and layout-sw600dp has no canvas_vscroll. */
    private void centerCanvasHomeView(final View rootView, final SignalChainCanvasView canvas) {
        boolean phoneScroll;
        try {
            phoneScroll = getResources().getBoolean(R.bool.canvas_phone_scroll);
        } catch (Exception e) {
            return;
        }
        if (!phoneScroll || rootView == null || canvas == null) return;
        final View vscroll = rootView.findViewById(R.id.canvas_vscroll);
        if (vscroll == null || vscroll.getTag() != null) return;
        vscroll.post(new Runnable() {
            int tries = 0;
            @Override public void run() {
                try {
                    int ch = canvas.getHeight();
                    int vh = vscroll.getHeight();
                    float needH = 0f;
                    try { needH = canvas.getPhoneContentHPx(); } catch (Exception ignore) {}
                    if ((vh <= 0 || ch <= 0) && tries < 20) {
                        tries++;
                        vscroll.postDelayed(this, 100);
                        return;
                    }
                    // Content computed TALL (branches exist) but the view isn't
                    // laid out that tall yet -> wait for the layout pass instead
                    // of tagging "centered" while stuck at the top (S23 bug).
                    if (needH > vh && ch <= vh && tries < 20) {
                        tries++;
                        vscroll.postDelayed(this, 100);
                        return;
                    }
                    if (ch > vh && vh > 0) {
                        ((androidx.core.widget.NestedScrollView) vscroll).scrollTo(0, (ch - vh) / 2);
                    }
                    android.util.Log.d("CanvasHome", "centered ch=" + ch + " vh=" + vh + " needH=" + needH);
                    vscroll.setTag("centered");
                } catch (Exception ignore) {}
            }
        });
    }

    /**
     * Collapse top-bar elements in defined priority order based on REAL measured
     * widths (not fixed-dp budgets). Priority: preset name + scene always visible;
     * collapse order logo -> CPU -> sample-rate. One-way: no auto-restore.
     */
    private void applyTopBarCollapse() {
        try {
            if (!isAdded() || getContext() == null) return; // fragment detached (logcat IllegalStateException)
            View headerBar = topBarForCollapse;
            if (headerBar == null) return;
            ImageView logo = headerBar.findViewById(R.id.top_logo);
            LinearLayout left = headerBar.findViewById(R.id.top_left_zone);
            LinearLayout right = headerBar.findViewById(R.id.top_right_zone);
            TextView presetDisplay = headerBar.findViewById(R.id.top_preset_display);
            TextView cpuText = headerBar.findViewById(R.id.cpu_meter_text);
            TextView srText = headerBar.findViewById(R.id.top_sr_text);
            if (logo == null || left == null || right == null || presetDisplay == null) return;

            int barW = headerBar.getWidth() - headerBar.getPaddingStart() - headerBar.getPaddingEnd();
            if (barW <= 0) return;

            float density = getResources().getDisplayMetrics().density;
            float screenWidthDp = barW / density;

            // On phones (< 600dp) hide the center logo immediately so Scene, BPM,
            // SampleRate & CPU have 100% full width and never get clipped.
            if (screenWidthDp < 600f) {
                logo.setVisibility(View.GONE);
            }

            int leftFixed = Math.round(58 * density); // knob 36dp + floppy 16dp + margins
            int rightW = right.getWidth();
            int freeForName = barW - leftFixed - rightW;

            if (logo.getVisibility() == View.VISIBLE && logo.getWidth() > 0)
                freeForName -= logo.getWidth() + Math.round(8 * density);

            int minNameW = Math.round(55 * density);

            if (freeForName < minNameW) {
                if (logo.getVisibility() == View.VISIBLE) {
                    logo.setVisibility(View.GONE);
                    freeForName += (logo.getWidth() > 0 ? logo.getWidth() : Math.round(48 * density));
                }
                if (freeForName < minNameW && cpuText != null && cpuText.getVisibility() == View.VISIBLE) {
                    cpuText.setVisibility(View.GONE);
                    freeForName += (cpuText.getWidth() > 0 ? cpuText.getWidth() : Math.round(30 * density));
                }
                if (freeForName < minNameW && srText != null && srText.getVisibility() == View.VISIBLE) {
                    srText.setVisibility(View.GONE);
                }
                headerBar.requestLayout();
            }
        } catch (Exception e) {
            Log.w(TAG, "applyTopBarCollapse failed", e);
        }
    }

    private void showRenamePresetDialog() {
        if (mainActivity == null || mainActivity.patchName == null) return;
        final String current = mainActivity.patchName.getText().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename Preset");
        final EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        builder.setView(input);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Rename", (d, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                MainActivity.toast("Preset name cannot be empty.");
                return;
            }
            mainActivity.patchName.setText(name);
            updateTopBarPresetDisplay(currentPresetNumber, name);
            LocalPresetManager lpm = new LocalPresetManager(getContext());
            for (LocalPresetManager.Preset p : lpm.loadAllPresets()) {
                if (p.name != null && p.name.equals(current)) {
                    p.name = name;
                    lpm.savePreset(p);
                    break;
                }
            }
            MainActivity.toast("Preset renamed to " + name);
        });
        AlertDialog d = builder.create();
        d.show();
        MainActivity.colorDialogButtons(d);
    }

    private void showQuickSaveDialog() {
        if (mainActivity == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Save Preset");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        layout.setPadding(pad, pad, pad, 0);

        final EditText nameInput = new EditText(getContext());
        nameInput.setSingleLine(true);
        nameInput.setHint("Preset name");
        nameInput.setTextColor(android.graphics.Color.parseColor("#E6E6E6"));
        nameInput.setHintTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        nameInput.setText(mainActivity.patchName != null ? mainActivity.patchName.getText().toString() : "");

        final EditText descInput = new EditText(getContext());
        descInput.setSingleLine(true);
        descInput.setHint("Description (optional)");
        descInput.setTextColor(android.graphics.Color.parseColor("#E6E6E6"));
        descInput.setHintTextColor(android.graphics.Color.parseColor("#A1A1AA"));

        layout.addView(nameInput);
        layout.addView(descInput, new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Save", (d, which) -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                MainActivity.toast("Name of preset is required.");
                return;
            }
            String desc = descInput.getText().toString().trim();
            LocalPresetManager lpm = new LocalPresetManager(getContext());
            LocalPresetManager.Preset p = buildPresetFromChain(name, desc);
            // Dedup by name (lastworks behavior): keep the first id + earliest
            // createdAt, delete any other presets with the same name.
            String firstId = null;
            long firstAt = 0;
            try {
                for (LocalPresetManager.Preset existing : lpm.loadAllPresets()) {
                    if (existing.name == null || !existing.name.equals(name)) continue;
                    if (firstId == null) {
                        firstId = existing.id;
                        firstAt = existing.createdAt;
                    } else if (existing.id != null && !existing.id.equals(firstId)) {
                        lpm.deletePreset(existing.id);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "save preset dedup failed", e);
            }
            if (firstId != null) {
                p.id = firstId;
                if (firstAt != 0) p.createdAt = firstAt;
            }
            boolean ok = lpm.savePreset(p);
            if (ok) {
                if (mainActivity != null) {
                    mainActivity.refreshPresetLists();
                    int idx = mainActivity.findQuickPresetIndexById(p.id);
                    if (idx >= 0) currentPresetNumber = idx + 1;
                    if (mainActivity.patchName != null) mainActivity.patchName.setText(name);
                }
                updateTopBarPresetDisplay(currentPresetNumber, name);
                // Space saver: also snapshot the current chain into the selected scene slot
                saveCurrentChainToScene();
                MainActivity.toast("Preset saved.");
            } else {
                MainActivity.toast("Failed to save preset.");
            }
        });
        AlertDialog d = builder.create();
        d.show();
        MainActivity.colorDialogButtons(d);
    }

    private void saveCurrentChainToScene() {
        if (mainActivity == null || getContext() == null) return;
        try {
            SharedPreferences scenePrefs = getContext().getSharedPreferences("giggingear_scenes", android.content.Context.MODE_PRIVATE);
            String[] defaultSceneNames = getContext().getResources().getStringArray(R.array.scene_slots);
            java.util.List<String> names = new java.util.ArrayList<>();
            for (int i = 0; i < defaultSceneNames.length; i++) {
                names.add(scenePrefs.getString("scene_name_" + i, defaultSceneNames[i]));
            }
            java.util.Map chainMap = mainActivity.presetToMap();
            if (chainMap == null) return;
            org.json.JSONObject json = new org.json.JSONObject();
            for (Object k : chainMap.keySet()) {
                Object p = chainMap.get(k);
                json.put((String) k, new org.json.JSONObject((java.util.Map) p).toString());
            }
            int scene = currentSceneIndex;
            if (scene < 0 || scene >= names.size()) scene = 0;
            scenePrefs.edit().putString("scene_" + scene, json.toString()).apply();
            MainActivity.toast("Saved to " + names.get(scene));
        } catch (Exception e) {
            Log.w(TAG, "saveCurrentChainToScene failed", e);
        }
    }

    private LocalPresetManager.Preset buildPresetFromChain(String name, String desc) {
        LocalPresetManager.Preset p = new LocalPresetManager.Preset();
        p.name = name;
        p.description = desc;
        p.author = "GigginGear Stage";
        p.favorite = false;
        p.masterInputVolume = 1.0f;
        p.masterOutputVolume = 1.0f;
        java.util.List<LocalPresetManager.PluginItem> plugins = new java.util.ArrayList<>();
        int total = AudioEngine.getActivePlugins();
        for (int i = 0; i < total; i++) {
            LocalPresetManager.PluginItem item = new LocalPresetManager.PluginItem();
            item.name = AudioEngine.getActivePluginName(i);
            item.uniqueId = AudioEngine.getActivePluginID(i);
            item.enabled = AudioEngine.getActivePluginEnabled(i);
            float[] values = AudioEngine.getActivePluginValues(i);
            java.util.List<LocalPresetManager.ControlItem> controls = new java.util.ArrayList<>();
            for (int k = 0; k < values.length; k++) {
                LocalPresetManager.ControlItem ci = new LocalPresetManager.ControlItem();
                ci.name = AudioEngine.getControlName(i, k);
                ci.value = values[k];
                controls.add(ci);
            }
            item.controls = controls;
            plugins.add(item);
        }
        p.plugins = plugins;

        // Persist real-time graph wiring and canvas node positions
        try {
            int[] graphEdges = AudioEngine.getGraphEdges();
            if (graphEdges != null && graphEdges.length > 0) {
                p.graph = graphEdges;
            }
        } catch (Exception ignored) {}

        if (mainActivity != null && mainActivity.signalChainCanvas != null) {
            for (SignalChainCanvasView.Node n : mainActivity.signalChainCanvas.getNodes()) {
                if (n != null) {
                    p.nodePositions.put(String.valueOf(n.id), new float[]{n.x, n.y});
                }
            }
        }

        return p;
    }


}
