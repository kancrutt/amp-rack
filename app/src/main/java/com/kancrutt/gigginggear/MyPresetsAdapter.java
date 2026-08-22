package com.kancrutt.gigginggear;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyPresetsAdapter extends RecyclerView.Adapter<MyPresetsAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList<Map> presets = new ArrayList<>();
    /** Invoked after list mutations (e.g. delete) so the host can refresh empty-state UI. */
    public Runnable onListChanged;
    MainActivity mainActivity = null;
    LocalPresetManager localPresetManager;
    ProgressBar progressBar , loadProgress;
    LinearProgressIndicator quickPatchProgress ;
    boolean quick = false ;
    MyPresetsAdapter myPresetsAdapter ;
    String sortBy = "timestamp";
    String uid = null;
    Map<String, Object> favoritePresets = null;
    ArrayList <Map> allPresets = new ArrayList<>() ;

    void removeAll () {
        int num = allPresets.size();
        allPresets.clear();
        presets.clear();
        notifyItemRangeRemoved(0, num);
    }

    void addPreset (Map preset) {
        if (preset == null)
            return;

        presets.add(preset);
        allPresets.add(preset);
        notifyDataSetChanged();
    }

    void removePreset (Map preset) {
        int i = 0 ;
        for (Map p:
             presets) {
            if (p.get("name").toString().equals(preset.get("name").toString())) {
                break ;
            } else {
                i++ ;
            }
        }

        if (i >= presets.size())
            return;

        presets.remove(i);
        allPresets.remove(i);
        notifyItemRemoved(i);
    }

    void setMainActivity (MainActivity _mainActivity) {
        mainActivity = _mainActivity;
    }

    void setProgressBar (ProgressBar progressBar1) {
        progressBar = progressBar1 ;
    }

    String value (Map preset, String... keys) {
        for (String key: keys) {
            Object o = preset.get(key);
            if (o != null)
                return o.toString();
        }
        return null;
    }

    String presetKey (Map preset) {
        String name = value(preset, "name");
        String path = value(preset, "path");
        return path != null ? path : (name != null ? name : "preset");
    }

    /**
     * Derive tag pills ([NAM][CAB][DELAY]) from a schema-v1 preset's plugins,
     * falling back to the preset's own "tags" list.
     */
    ArrayList<String> tagsFor (Map preset) {
        ArrayList<String> tags = new ArrayList<>();
        Object tagsObj = preset.get("tags");
        if (tagsObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) tagsObj;
            for (Object t : list) {
                if (t != null && !t.toString().trim().isEmpty())
                    tags.add(t.toString().trim().toUpperCase(Locale.ROOT));
            }
            if (!tags.isEmpty())
                return tags;
        }

        Object pluginsObj = preset.get("plugins");
        if (pluginsObj instanceof java.util.List) {
            for (Object o : (java.util.List<?>) pluginsObj) {
                if (!(o instanceof Map))
                    continue;
                Map<?, ?> p = (Map<?, ?>) o;
                String name = value((Map) p, "name", "library");
                if (name == null)
                    continue;
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("nam") || lower.contains("neural") || hasNamFile(p)) {
                    if (!tags.contains("NAM")) tags.add("NAM");
                } else if (lower.contains("cab") || lower.contains("impulse") || lower.contains("ir ")) {
                    if (!tags.contains("CAB")) tags.add("CAB");
                } else if (lower.contains("delay") || lower.contains("reverb") || lower.contains("echo")) {
                    if (!tags.contains("DELAY")) tags.add("DELAY");
                }
            }
        }

        if (tags.isEmpty())
            tags.add("RIG");

        return tags;
    }

    boolean hasNamFile (Map plugin) {
        Object fpObj = plugin.get("file_ports");
        if (!(fpObj instanceof java.util.List))
            return false;
        for (Object o : (java.util.List<?>) fpObj) {
            if (!(o instanceof Map))
                continue;
            Object rel = ((Map<?, ?>) o).get("relative_path");
            Object fn = ((Map<?, ?>) o).get("file_name");
            if (rel != null && rel.toString().toLowerCase(Locale.ROOT).endsWith(".nam"))
                return true;
            if (fn != null && fn.toString().toLowerCase(Locale.ROOT).endsWith(".nam"))
                return true;
        }
        return false;
    }

    void addTagPill (LinearLayout tagsRow, String text) {
        TextView pill = new TextView(context);
        pill.setText(text);
        pill.setTextSize(10);
        pill.setTextColor(0xFFE8A33D);
        pill.setBackgroundResource(R.drawable.gg_pill_bg);
        pill.setPadding(12, 4, 12, 4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(8);
        pill.setLayoutParams(lp);
        tagsRow.addView(pill);
    }

    String metaText (Map preset) {
        StringBuilder sb = new StringBuilder();
        Object created = preset.get("created_at");
        if (created instanceof Number) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                sb.append(sdf.format(new Date(((Number) created).longValue())));
            } catch (Exception ignored) {
            }
        }
        Object author = preset.get("author");
        if (author != null && !author.toString().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(author.toString());
        }
        return sb.toString();
    }

    @NonNull
    @Override
    public MyPresetsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        localPresetManager = new LocalPresetManager(parent.getContext());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_preset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPresetsAdapter.ViewHolder holder, int position) {
        uid = "local_user";
        myPresetsAdapter = this ;

        Map <String, Object> preset = presets.get(position);
        if (preset == null)
            return;

        String presetName = value(preset, "name");
        String presetDesc = value(preset, "desc", "description");
        if (presetName == null) presetName = "Untitled";
        final String fPresetName = presetName;
        final String fPresetDesc = presetDesc;

        holder.title.setText(presetName);
        holder.desc.setText(presetDesc != null ? presetDesc : "");
        holder.desc.setVisibility(presetDesc != null && !presetDesc.trim().isEmpty() ? View.VISIBLE : View.GONE);

        holder.tags.removeAllViews();
        ArrayList<String> tags = tagsFor(preset);
        for (String t : tags)
            addTagPill(holder.tags, t);

        String meta = metaText(preset);
        holder.meta.setText(meta);
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isSchemaV1 = preset.containsKey("plugins");
        boolean isLocal = "local_user".equals(value(preset, "uid"));

        if (quick) {
            holder.delete.setVisibility(View.GONE);
            holder.favorite.setVisibility(View.GONE);
        } else if (isSchemaV1) {
            holder.delete.setVisibility(View.VISIBLE);
            holder.favorite.setVisibility(View.GONE);
        } else {
            holder.delete.setVisibility(isLocal ? View.VISIBLE : View.GONE);
            holder.favorite.setVisibility(isLocal ? View.GONE : View.VISIBLE);
        }

        View.OnClickListener loadListener = v -> {
            mainActivity.loadPreset(preset);
            Log.d(TAG, "onClick: loading preset " + preset);
            mainActivity.patchName.setText(fPresetName);
            mainActivity.patchDesc.setText(fPresetDesc != null ? fPresetDesc : "");

            if (preset.containsKey("uid") && preset.containsKey("path")) {
                mainActivity.lastPresetLoadedPath = ((String) preset.get("path")).split("/")[1];
                mainActivity.lastPresetLoadedUID = (String) preset.get("uid");
            }

            // patchNo was retired by the top-bar preset display redesign
            // (set to null in Rack) — guard to avoid NPE on every card tap.
            if (mainActivity.patchNo != null)
                mainActivity.patchNo.setText(quick ? String.valueOf(position) : "-");

            mainActivity.syncTopBarPresetInfo(position + 1);
            MainActivity.toast("Loaded preset " + fPresetName);
        };
        holder.quickLoad.setOnClickListener(loadListener);
        // Whole row is the tap target (list interaction model)
        holder.root.setOnClickListener(loadListener);

        holder.favorite.setOnCheckedChangeListener(null);
        String key = presetKey(preset);
        if (favoritePresets != null && favoritePresets.containsKey(key)) {
            holder.favorite.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
            holder.favorite.setChecked(true);
        } else {
            holder.favorite.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            holder.favorite.setChecked(false);
        }

        holder.favorite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (favoritePresets == null) {
                        favoritePresets = new HashMap<String, Object>();
                    }
                    favoritePresets.put(key, preset.get("name"));
                    mainActivity.addFavoritePreset(preset);
                } else {
                    compoundButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
                    favoritePresets.remove(key);
                    mainActivity.removeFavoritePreset(preset);
                }
            }
        });

        holder.delete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos < 0 || pos >= presets.size()) return;
            Map<String, Object> target = presets.get(pos);
            String targetName = value(target, "name", "Untitled");

            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle("Delete this preset?");
            builder.setMessage("Delete \"" + targetName + "\"? This cannot be undone.");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos < 0 || currentPos >= presets.size()) return;
                Map<String, Object> itemToDelete = presets.get(currentPos);
                if (itemToDelete != null) {
                    String idStr = null;
                    if (itemToDelete.containsKey("id") && itemToDelete.get("id") != null) {
                        idStr = itemToDelete.get("id").toString();
                    } else if (itemToDelete.containsKey("name") && itemToDelete.get("name") != null) {
                        idStr = itemToDelete.get("name").toString();
                    }
                    if (idStr != null && localPresetManager != null) {
                        try {
                            localPresetManager.deletePreset(idStr);
                        } catch (Exception e) {
                            Log.e(TAG, "delete failed", e);
                        }
                    }
                }
                presets.remove(currentPos);
                if (currentPos < allPresets.size()) allPresets.remove(currentPos);
                notifyItemRemoved(currentPos);
                if (onListChanged != null) onListChanged.run();
                MainActivity.toast("Deleted " + targetName);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView title, desc, meta;
        LinearLayout tags;
        MaterialButton quickLoad, delete;
        ToggleButton favorite;

        public ViewHolder(View view) {
            super(view);
            root = (LinearLayout) view;
            title = view.findViewById(R.id.preset_title);
            desc = view.findViewById(R.id.preset_desc);
            tags = view.findViewById(R.id.preset_tags);
            meta = view.findViewById(R.id.preset_meta);
            quickLoad = view.findViewById(R.id.preset_quick_load);
            delete = view.findViewById(R.id.preset_delete);
            favorite = view.findViewById(R.id.preset_favorite);
        }

        public LinearLayout getLinearLayout() {
            return root ;
        }
    }

    public void updateList(String string){
        Log.d(TAG, "updateList: " +
            String.format ("<%s> presets: %s\nall presets:%s", string, presets.toString(), allPresets.toString()));

        presets.clear();
        if(string.length() == 0) {
            for (Map m: allPresets)
                presets.add(m);
        } else {
            for (Map m : allPresets) {
                Object name = m.get("name");
                if (name != null && name.toString().toLowerCase().contains(string)) {
                    presets.add(m);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void resetList () {
        presets = allPresets ;
        notifyDataSetChanged();
    }

    public void showOnlyFavorites (boolean show) {
        Log.d(TAG, "showOnlyFavorites: " + show);
        presets.clear();
        if(show == false) {
            Log.d(TAG, "showOnlyFavorites: adding all presets");
            for (Map m: allPresets)
                presets.add(m);
        } else {
            for (Map m : allPresets) {
                if (favoritePresets != null) {
                    if (favoritePresets.containsKey(presetKey(m))) {
                        Log.d(TAG, "showOnlyFavorites: adding preset " + m.get("name"));
                        presets.add(m);
                    }
                }
            }
        }

        notifyDataSetChanged();
    }

    boolean contains (Map preset) {
        for (Map p:
             allPresets) {
            if (preset.get("name").equals(p.get("name")) && preset.get("uid").equals(p.get("uid")))
                return true;
        }

        return false;
    }
}
