package com.kancrutt.gigginggear;

import androidx.appcompat.app.AlertDialog;
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
    /** Factory/library panels are read-only: hide the delete affordance there. */
    boolean allowDelete = true ;
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
        String name = preset.get("name") != null ? preset.get("name").toString() : "";
        for (Map p : presets) {
            if (name.equals(p.get("name") != null ? p.get("name").toString() : "")) {
                return;
            }
        }
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

    /** Remove one entry from allPresets by stable id (fallback: name). Index-based
     *  removal is wrong here because presets/allPresets diverge under search filter. */
    void removeFromAllById (String id, String name) {
        if (allPresets == null || allPresets.isEmpty()) return;
        for (int i = 0; i < allPresets.size(); i++) {
            Map m = allPresets.get(i);
            if (m == null) continue;
            Object mid = m.get("id");
            Object mname = m.get("name");
            boolean match = false;
            if (id != null && mid != null && id.equals(mid.toString())) match = true;
            else if (name != null && mname != null && name.equals(mname.toString()) && (id == null || mid == null)) match = true;
            if (match) {
                allPresets.remove(i);
                return;
            }
        }
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
     * Derive indicator pills from a schema-v1 preset's plugins using the SAME
     * category engine as the signal-chain canvas (CategoryHelper) — so a preset
     * with a NAM/AIDA block shows AMP, a Convolution/IR block shows CAB, etc.
     * Order-stable, deduped, color-coded per category. The preset's own free-form
     * "tags" (user keywords) are appended as neutral pills.
     */
    ArrayList<Object[]> tagSpecsFor (Map preset) {
        // {label, color} pairs
        java.util.LinkedHashMap<String, Integer> specs = new java.util.LinkedHashMap<>();
        Object pluginsObj = preset.get("plugins");
        if (pluginsObj instanceof java.util.List) {
            for (Object o : (java.util.List<?>) pluginsObj) {
                if (!(o instanceof Map))
                    continue;
                String name = value((Map) o, "name", "library");
                if (name == null)
                    continue;
                CategoryHelper.Category cat = CategoryHelper.getCategory(name);
                if (cat == CategoryHelper.Category.UNKNOWN || cat == CategoryHelper.Category.UTILITY)
                    continue;
                specs.putIfAbsent(cat.code, cat.color);
            }
        }
        Object tagsObj = preset.get("tags");
        if (tagsObj instanceof java.util.List) {
            for (Object t : (java.util.List<?>) tagsObj) {
                if (t != null && !t.toString().trim().isEmpty())
                    specs.putIfAbsent(t.toString().trim().toUpperCase(Locale.ROOT),
                            CategoryHelper.COLOR_DEFAULT);
            }
        }
        if (specs.isEmpty())
            specs.put("RIG", CategoryHelper.COLOR_DEFAULT);
        ArrayList<Object[]> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : specs.entrySet())
            out.add(new Object[]{e.getKey(), e.getValue()});
        return out;
    }

    void addTagPill (LinearLayout tagsRow, String text, int color) {
        TextView pill = new TextView(context);
        pill.setText(text);
        pill.setTextSize(10);
        pill.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        pill.setTextColor(color);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setStroke(1, (color & 0x00FFFFFF) | 0x66000000);
        bg.setCornerRadius(20 * context.getResources().getDisplayMetrics().density);
        pill.setBackground(bg);
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
        for (Object[] spec : tagSpecsFor(preset))
            addTagPill(holder.tags, (String) spec[0], (Integer) spec[1]);

        String meta = metaText(preset);
        holder.meta.setText(meta);
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isSchemaV1 = preset.containsKey("plugins");
        boolean isLocal = "local_user".equals(value(preset, "uid"));

        if (quick) {
            holder.delete.setVisibility(View.GONE);
            holder.favorite.setVisibility(View.GONE);
        } else if (isSchemaV1) {
            holder.delete.setVisibility(allowDelete ? View.VISIBLE : View.GONE);
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

            // Single-writer path: refreshes label (orange prefix), nav cursor AND
            // the persisted cold-start label. Falls back to label-only sync.
            try {
                if (mainActivity.rack != null) mainActivity.rack.onPresetLoadedFromList(position, fPresetName);
                else mainActivity.syncTopBarPresetInfo(position + 1);
            } catch (Exception e) {
                try { mainActivity.syncTopBarPresetInfo(position + 1); } catch (Exception ignore) {}
            }
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
            // The delete target is the map bound to THIS row at bind time — never
            // holder.getAdapterPosition(): stale holders report NO_POSITION (-1) at
            // tap time (proven on device: pos=-1/size=1 = total silence), and a
            // position re-query can address the wrong row after a list reload.
            // Row content always equals the bound map, so identity is exact.
            final Map<String, Object> clickTarget = preset;
            if (clickTarget == null) {
                Log.e(TAG, "delete: unbound row, refusing");
                MainActivity.toast("Cannot delete: row not ready, try again");
                return;
            }
            final String targetName = value(clickTarget, "name", "Untitled");
            // ID-only addressing: display names are not unique, so falling back
            // to the name here deleted (or failed) the wrong file. A row without
            // an id cannot be deleted honestly — abort loudly instead.
            Object rawId = clickTarget.get("id");
            final String clickId = (rawId != null && !rawId.toString().isEmpty())
                    ? rawId.toString() : null;

            AlertDialog.Builder builder = MainActivity.dialogBuilder(mainActivity);
            builder.setTitle("Delete this preset?");
            builder.setMessage("Delete \"" + targetName + "\"? This cannot be undone.");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                if (clickId == null) {
                    Log.e(TAG, "delete: preset row has no id, refusing (name=" + targetName + ")");
                    MainActivity.toast("Cannot delete \"" + targetName + "\": missing id");
                    return;
                }
                if (localPresetManager == null) {
                    Log.e(TAG, "delete: localPresetManager null, refusing (id=" + clickId + ")");
                    MainActivity.toast("Cannot delete \"" + targetName + "\": storage unavailable");
                    return;
                }
                boolean fileDeleted = false;
                try {
                    fileDeleted = localPresetManager.deletePreset(clickId);
                } catch (Exception e) {
                    Log.e(TAG, "delete failed", e);
                }
                if (!fileDeleted) {
                    // Never fake success: the row stays (refresh below reloads it)
                    // and the user gets a failure toast instead of a lying "Deleted".
                    Log.e(TAG, "deletePreset failed for id=" + clickId + " (name=" + targetName + ")");
                    MainActivity.toast("Delete failed — preset kept");
                    try {
                        if (mainActivity != null) mainActivity.refreshPresetLists();
                    } catch (Exception e) {
                        Log.w(TAG, "post-delete refresh failed", e);
                    }
                    return;
                }
                // Remove the visible row by identity (the bound map): no position
                // logic anywhere — holder positions are untrustworthy by design now.
                int visiblePos = presets != null ? presets.indexOf(clickTarget) : -1;
                if (visiblePos >= 0) {
                    presets.remove(visiblePos);
                }
                // allPresets may be ordered/filtered differently: match by id/name,
                // never by visible index (that deleted the wrong row under search).
                removeFromAllById(clickId, targetName);
                notifyDataSetChanged();
                if (onListChanged != null) onListChanged.run();
                // Reload every list from disk so quick-patch nav, picker and the
                // other panel drop the deleted preset immediately (no tab-switch
                // needed to see the update).
                try {
                    if (mainActivity != null) mainActivity.refreshPresetLists();
                } catch (Exception e) {
                    Log.w(TAG, "post-delete refresh failed", e);
                }
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
