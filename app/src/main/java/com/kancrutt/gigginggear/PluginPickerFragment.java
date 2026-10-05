package com.kancrutt.gigginggear;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import com.kancrutt.gigginggear.tone3000.Tone3000AuthManager;
import com.kancrutt.gigginggear.tone3000.Tone3000Config;
import com.kancrutt.gigginggear.tone3000.ui.Tone3000EntrySheet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class PluginPickerFragment extends BottomSheetDialogFragment {

    private static final String TAG = "PluginPickerFragment";
    private MainActivity mainActivity;
    private LinearLayout catContainer;
    private LinearLayout itemsContainer;
    private CategoryHelper.Category activeCat = CategoryHelper.Category.AMP;

    public static PluginPickerFragment newInstance() {
        return new PluginPickerFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setHideable(false);
                behavior.setDraggable(false);
                // Prevent drag-to-dismiss while still allowing inner ScrollView to scroll
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override public void onStateChanged(@NonNull View bottomSheetView, int newState) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    }
                    @Override public void onSlide(@NonNull View bottomSheetView, float slideOffset) {}
                });
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.setLayoutParams(layoutParams);
                }
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_plugin_picker, container, false);
        if (getActivity() instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
        }

        ImageButton closeBtn = view.findViewById(R.id.picker_close_btn);
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dismiss());

        View bottomClose = view.findViewById(R.id.picker_bottom_close_btn);
        if (bottomClose != null) bottomClose.setOnClickListener(v -> dismiss());

        catContainer = view.findViewById(R.id.picker_cat_container);
        itemsContainer = view.findViewById(R.id.picker_items_container);

        setupCategories();

        // The picker layout ships a search box; without this watcher it is
        // inert (typed queries changed nothing — the reported "search nggak
        // jalan"). Non-empty query searches every category; empty restores
        // the active category. Mirrors MainActivity's picker dialog.
        EditText pickerSearch = view.findViewById(R.id.picker_search_input);
        ImageButton pickerClear = view.findViewById(R.id.picker_search_clear);
        if (pickerSearch != null && itemsContainer != null) {
            if (pickerClear != null) {
                pickerClear.setOnClickListener(v -> pickerSearch.setText(""));
            }
            pickerSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim().toLowerCase();
                    if (pickerClear != null) {
                        pickerClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (query.isEmpty()) {
                        loadItemsForCategory(activeCat);
                    } else {
                        loadItemsMatchingQuery(query);
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        return view;
    }

    private void setupCategories() {
        if (catContainer == null) return;
        catContainer.removeAllViews();

        // TONE3000 entry — full-logo context at top of category column (spec §5/6)
        View t3kView = getLayoutInflater().inflate(R.layout.item_model_category_row, catContainer, false);
        ImageView t3kIcon = t3kView.findViewById(R.id.item_cat_icon);
        TextView t3kTitle = t3kView.findViewById(R.id.item_cat_title);
        t3kTitle.setText("TONE3000");
        t3kTitle.setTextColor(Color.parseColor("#FFFFFF"));
        if (t3kIcon != null) {
            t3kIcon.setImageResource(R.drawable.ic_cat_amp);
            t3kIcon.setColorFilter(Color.parseColor("#FFFFFF"), android.graphics.PorterDuff.Mode.SRC_IN);
        }
        t3kView.setBackgroundResource(R.drawable.glass_card_bg);
        t3kView.setOnClickListener(v -> {
            if (getActivity() == null) return;
            if (!Tone3000Config.isConfigured(getActivity())) {
                Toast.makeText(getActivity(), "TONE3000 not configured — missing client_id", Toast.LENGTH_LONG).show();
                return;
            }
            dismiss();
            try {
                Tone3000EntrySheet sheet = Tone3000EntrySheet.newInstance();
                sheet.show(getParentFragmentManager(), "t3k_entry");
            } catch (Exception e) {
                try {
                    Tone3000AuthManager.AuthStart s = Tone3000AuthManager.get(getActivity()).startSelectFlow(getActivity());
                    CustomTabsIntent cti = new CustomTabsIntent.Builder().setShowTitle(true).build();
                    cti.launchUrl(getActivity(), Uri.parse(s.authorizeUrl));
                } catch (Exception ex) { Toast.makeText(getActivity(), "TONE3000 failed: " + ex.getMessage(), Toast.LENGTH_LONG).show(); }
            }
        });
        catContainer.addView(t3kView);
        // thin divider
        View div = new View(getContext());
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(1 * getResources().getDisplayMetrics().density)));
        div.setBackgroundColor(Color.parseColor("#1E232A"));
        catContainer.addView(div);

        final CategoryHelper.Category[] categories = {
                CategoryHelper.Category.AMP,
                CategoryHelper.Category.CABINET,
                CategoryHelper.Category.DISTORTION,
                CategoryHelper.Category.DYNAMICS,
                CategoryHelper.Category.MODULATION,
                CategoryHelper.Category.DELAY,
                CategoryHelper.Category.REVERB,
                CategoryHelper.Category.EQ,
                CategoryHelper.Category.PITCH,
                CategoryHelper.Category.WAH,
                CategoryHelper.Category.UTILITY,
                CategoryHelper.Category.GATE
        };

        final String[] catLabels = {
                "Amp / Head", "Cabinet", "Overdrive / Dist", "Compressor",
                "Modulation", "Delay", "Reverb", "EQ", "Pitch / Synth",
                "Wah", "Utility", "Noise Gate"
        };

        for (int i = 0; i < categories.length; i++) {
            final int catIdx = i;
            final CategoryHelper.Category cat = categories[i];
            View catView = getLayoutInflater().inflate(R.layout.item_model_category_row, catContainer, false);
            ImageView icon = catView.findViewById(R.id.item_cat_icon);
            TextView title = catView.findViewById(R.id.item_cat_title);

            title.setText(catLabels[i]);
            int catColor = CategoryHelper.getColor(cat.name());
            icon.setImageResource(CategoryHelper.getIconRes(cat.name()));
            icon.setColorFilter(catColor, android.graphics.PorterDuff.Mode.SRC_IN);

            boolean isInitial = (i == 0);
            catView.setBackgroundResource(isInitial ? R.drawable.glass_category_selected_bg : R.drawable.glass_card_bg);

            catView.setOnClickListener(v -> {
                for (int c = 0; c < catContainer.getChildCount(); c++) {
                    catContainer.getChildAt(c).setBackgroundResource(R.drawable.glass_card_bg);
                }
                catView.setBackgroundResource(R.drawable.glass_category_selected_bg);
                activeCat = cat;
                loadItemsForCategory(cat);
            });

            catContainer.addView(catView);
        }

        // Initially load first category (AMP)
        loadItemsForCategory(categories[0]);
    }

    private void loadItemsForCategory(CategoryHelper.Category cat) {
        if (itemsContainer == null || mainActivity == null) return;
        itemsContainer.removeAllViews();

        if (cat == CategoryHelper.Category.SPLIT || cat == CategoryHelper.Category.MERGE) {
            // Routing utilities
            addRoutingRow("SPLIT (Parallel Branch)", "Split signal into 2 parallel paths", R.drawable.ic_routing_split, true);
            addRoutingRow("MERGE (Sum to Mono)", "Combine parallel branches into 1 path", R.drawable.ic_routing_merge, false);
            return;
        }

        // Load blacklist
        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "blacklist.json");
        boolean enableBlacklisted = mainActivity.defaultSharedPreferences.getBoolean("enableBlacklisted", false);

        // Scan available LADSPA & LV2 plugins
        JSONObject[] sources = {mainActivity.availablePlugins, mainActivity.availablePluginsLV2};
        ArrayList<String> loadedPluginNames = new ArrayList<>();

        for (JSONObject source : sources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject obj = source.optJSONObject(k);
                if (obj == null) continue;
                String pName = obj.optString("name", "");
                if (pName.isEmpty() || loadedPluginNames.contains(pName)) continue;
                if (!MainActivity.isGuitarPlugin(pName)) continue;
                // MIDI-only synths can never sound in a guitar chain (no audio
                // input to feed them) — hide instead of offering a crash/toast.
                if (MainActivity.isUnsupportedSynth(pName)) continue;

                // Blacklist check
                String pNameKey = pName != null ? pName.toLowerCase().trim() : "";
                if (blacklist != null && blacklist.has(pNameKey) && !enableBlacklisted) {
                    Log.d(TAG, String.format ("[plugin picker] blacklisted: %s", pName));
                    continue;
                }

                CategoryHelper.Category pCat = CategoryHelper.getCategory(pName);
                if (pCat == cat) {
                    loadedPluginNames.add(pName);
                    String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                    if (desc.length() > 40) desc = desc.substring(0, 37) + "…";

                    View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, itemsContainer, false);
                    TextView t = row.findViewById(R.id.item_model_title);
                    TextView b = row.findViewById(R.id.item_model_brand);
                    ImageView thumb = row.findViewById(R.id.item_model_thumb);
                    ImageView star = row.findViewById(R.id.item_model_star);
                    star.setVisibility(View.GONE);

                    t.setText(PluginNaming.displayName(pName));
                    b.setText(desc);
                    thumb.setImageResource(CategoryHelper.getIconRes(pName));
                    thumb.setColorFilter(CategoryHelper.getColor(pName), android.graphics.PorterDuff.Mode.SRC_IN);

                    row.setOnClickListener(v -> {
                        mainActivity.addPluginByName(pName);
                        dismiss();
                    });
                    itemsContainer.addView(row);
                }
            }
        }

        // Fallback for file-based amps if plugin scan is empty
        if (loadedPluginNames.isEmpty() && (cat == CategoryHelper.Category.AMP || cat == CategoryHelper.Category.CABINET)) {
            String[] sampleAmps = {"Neural Amp Modeler NG", "Convolution Reverb"};
            for (String sa : sampleAmps) {
                View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, itemsContainer, false);
                TextView t = row.findViewById(R.id.item_model_title);
                TextView b = row.findViewById(R.id.item_model_brand);
                ImageView thumb = row.findViewById(R.id.item_model_thumb);
                row.findViewById(R.id.item_model_star).setVisibility(View.GONE);

                t.setText(sa);
                b.setText(cat.name() + " Loader");
                thumb.setImageResource(CategoryHelper.getIconRes(cat.name()));
                thumb.setColorFilter(CategoryHelper.getColor(cat.name()), android.graphics.PorterDuff.Mode.SRC_IN);

                row.setOnClickListener(v -> {
                    mainActivity.addPluginByName(sa);
                    dismiss();
                });
                itemsContainer.addView(row);
            }
        }
    }

    /** Global search across every category (routing rows + all LADSPA/LV2
     *  plugins). Same blacklist/synth filters as the browse path so a searched
     *  result is always addable. */
    private void loadItemsMatchingQuery(String query) {
        if (itemsContainer == null || mainActivity == null) return;
        itemsContainer.removeAllViews();
        String q = query.toLowerCase();
        int hits = 0;

        if ("split (parallel branch)".contains(q) || "split signal into 2 parallel paths".contains(q)) {
            addRoutingRow("SPLIT (Parallel Branch)", "Split signal into 2 parallel paths", R.drawable.ic_routing_split, true);
            hits++;
        }
        if ("merge (sum to mono)".contains(q) || "combine parallel branches into 1 path".contains(q)) {
            addRoutingRow("MERGE (Sum to Mono)", "Combine parallel branches into 1 path", R.drawable.ic_routing_merge, false);
            hits++;
        }

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "blacklist.json");
        boolean enableBlacklisted = mainActivity.defaultSharedPreferences.getBoolean("enableBlacklisted", false);

        JSONObject[] sources = {mainActivity.availablePlugins, mainActivity.availablePluginsLV2};
        ArrayList<String> seen = new ArrayList<>();
        for (JSONObject source : sources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                JSONObject obj = source.optJSONObject(keys.next());
                if (obj == null) continue;
                final String pName = obj.optString("name", "");
                if (pName.isEmpty() || seen.contains(pName)) continue;
                if (!MainActivity.isGuitarPlugin(pName)) continue;
                if (MainActivity.isUnsupportedSynth(pName)) continue;
                String pNameKey = pName.toLowerCase().trim();
                if (blacklist != null && blacklist.has(pNameKey) && !enableBlacklisted) continue;
                String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                if (!(pName.toLowerCase().contains(q) || desc.toLowerCase().contains(q))) continue;
                seen.add(pName);
                String shortDesc = desc.length() > 40 ? desc.substring(0, 37) + "…" : desc;

                View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, itemsContainer, false);
                TextView t = row.findViewById(R.id.item_model_title);
                TextView b = row.findViewById(R.id.item_model_brand);
                ImageView thumb = row.findViewById(R.id.item_model_thumb);
                row.findViewById(R.id.item_model_star).setVisibility(View.GONE);
                t.setText(PluginNaming.displayName(pName));
                b.setText(shortDesc);
                thumb.setImageResource(CategoryHelper.getIconRes(pName));
                thumb.setColorFilter(CategoryHelper.getColor(pName), android.graphics.PorterDuff.Mode.SRC_IN);
                row.setOnClickListener(v -> {
                    mainActivity.addPluginByName(pName);
                    dismiss();
                });
                itemsContainer.addView(row);
                hits++;
            }
        }

        if (hits == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("No matches for \"" + query + "\"");
            empty.setTextColor(Color.parseColor("#A1A1AA"));
            empty.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            int pad = Math.round(12 * getResources().getDisplayMetrics().density);
            empty.setPadding(pad, Math.round(16 * getResources().getDisplayMetrics().density), pad, Math.round(16 * getResources().getDisplayMetrics().density));
            itemsContainer.addView(empty);
        }
    }

    private void addRoutingRow(String title, String desc, int iconRes, boolean isSplit) {
        View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, itemsContainer, false);
        TextView t = row.findViewById(R.id.item_model_title);
        TextView b = row.findViewById(R.id.item_model_brand);
        ImageView thumb = row.findViewById(R.id.item_model_thumb);
        row.findViewById(R.id.item_model_star).setVisibility(View.GONE);

        t.setText(title);
        b.setText(desc);
        thumb.setImageResource(iconRes);
        thumb.setColorFilter(Color.parseColor("#FFFFFF"), android.graphics.PorterDuff.Mode.SRC_IN);

        row.setOnClickListener(v -> {
            if (mainActivity != null) mainActivity.addRoutingBlock(isSplit);
            dismiss();
        });
        itemsContainer.addView(row);
    }
}
