package com.kancrutt.gigginggear;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    String TAG = "DataAdapter";
    Context context = null;
    int primaryColor = Color.parseColor("#FFFFFF");
    public int totalItems = 0;
    ArrayList<Integer> plugins = new ArrayList<>();
    public ArrayList<ViewHolder> holders = new ArrayList<>();
    HashMap<Integer, Integer> selectedModels = new HashMap<>();
    HashMap<Integer, String> audioFiles = new HashMap<>();
    HashMap<Integer, String> audioFilesR = new HashMap<>();
    MainActivity mainActivity;
    private int selectedPosition = 0;

    public void setSelectedPosition(int pos) {
        int prev = selectedPosition;
        selectedPosition = pos;
        notifyItemChanged(prev);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static final int VIEW_TYPE_COMPACT = 0;
    public static final int VIEW_TYPE_WIDE = 1;

    @Override
    public int getItemViewType(int position) {
        String name = AudioEngine.getActivePluginName(position);
        if (name != null) {
            String n = name.toLowerCase();
            if (n.contains("timeline") || n.contains("bigsky") || n.contains("echotron") ||
                n.contains("reverbtron") || n.contains("multiband") || n.contains("stereo")) {
                return VIEW_TYPE_WIDE;
            }
        }
        return VIEW_TYPE_COMPACT;
    }

    @NonNull
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        int layoutRes = (viewType == VIEW_TYPE_WIDE) ? R.layout.view_pedal_card_wide : R.layout.view_pedal_card_compact;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holders.add(holder);

        String pluginName = AudioEngine.getActivePluginName(position);
        if (pluginName == null) {
            Log.e(TAG, "onBindViewHolder: plugin name returned null", null);
            return;
        }

        if (holder.root != null) {
            boolean isAmp = isAmpBlock(pluginName);
            boolean isSelected = (position == selectedPosition);
            if (isSelected) {
                holder.root.setBackgroundResource(R.drawable.card_selected_highlight);
            } else {
                holder.root.setBackgroundResource(isAmp ? R.drawable.rounded_corners_amp_3d : R.drawable.rounded_corners_pedal_3d);
            }

            if (holder.pluginName != null) {
                holder.pluginName.setText(PluginNaming.displayName(pluginName));
                holder.pluginName.setTextColor(Color.parseColor("#FFFFFF"));
            }

            if (holder.subtitleView != null) {
                holder.subtitleView.setText(isAmp ? "Neural Amp (NAM)" : "Neural FX (NAM)");
                holder.subtitleView.setTextColor(Color.parseColor("#8F94A0"));
            }

            if (holder.categoryBadge != null) {
                holder.categoryBadge.setText("A" + (position + 1));
                holder.categoryBadge.setTextColor(getCategoryActiveColor(pluginName));
            }

            View accentBar = holder.root.findViewById(R.id.category_accent_bar);
            if (accentBar != null) {
                accentBar.setBackgroundColor(getCategoryActiveColor(pluginName));
            }

            if (holder.pedalIcon != null) {
                holder.pedalIcon.setImageResource(getCategoryIconRes(pluginName));
                holder.pedalIcon.setActivated(AudioEngine.getActivePluginEnabled(position));
            }

            if (holder.pedalLed != null) {
                boolean active = AudioEngine.getActivePluginEnabled(position);
                holder.pedalLed.setBackgroundResource(active ? R.drawable.led_glowing_active : R.drawable.led_glowing_bypass);
            }

            android.view.GestureDetector gestureDetector = new android.view.GestureDetector(context, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        String currentName = AudioEngine.getActivePluginName(adapterPosition);
                        mainActivity.showPluginDetailDialog(adapterPosition, currentName);
                    }
                    return true;
                }

                @Override
                public boolean onDoubleTap(android.view.MotionEvent e) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        String currentName = AudioEngine.getActivePluginName(adapterPosition);
                        mainActivity.showPluginDetailDialog(adapterPosition, currentName);
                    }
                    return true;
                }
            });

            View.OnTouchListener touchListener = (v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            };

            holder.root.setOnTouchListener(touchListener);
            holder.itemView.setOnTouchListener(touchListener);
            if (holder.pluginName != null) holder.pluginName.setOnTouchListener(touchListener);
            if (holder.subtitleView != null) holder.subtitleView.setOnTouchListener(touchListener);
            if (holder.categoryBadge != null) holder.categoryBadge.setOnTouchListener(touchListener);

            if (holder.deleteButton != null) {
                holder.deleteButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos >= 0 && pos < plugins.size()) {
                        Log.d(TAG, "deleteButton: deleting slot " + pos);
                        // Route through the central delete path so T3K meta shifts +
                        // canvas + badges re-sync (inline editor and canvas FAB must
                        // share the same path, otherwise the T3K badge "follows" the
                        // stale slot index and lands on the wrong block).
                        try { mainActivity.deletePluginAndSyncTone3000(pos); }
                        catch (Exception e) {
                            Log.e(TAG, "deletePluginAndSyncTone3000 failed, direct fallback", e);
                            AudioEngine.deletePlugin(pos);
                        }
                        deleteItem(pos);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        int active = AudioEngine.getActivePlugins();
        if (active <= 0) return 0;
        return Math.min(plugins.size(), active);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout root;
        public TextView pluginName;
        public TextView subtitleView;
        public TextView categoryBadge;
        public View pedalLed;
        public ImageView pedalIcon;
        public MaterialButton deleteButton;
        public ArrayList<com.google.android.material.slider.Slider> sliders = new ArrayList<>();
        public String audioFile = null;
        public String audioFileR = null;
        public float irBlend = 0.5f;
        public boolean hasFileSpinner = false;
        public android.widget.Spinner modelSpinner;
        public HashMap<Integer, android.widget.Spinner> atomSpinners = new HashMap<>();

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.pedal_card_root);
            if (root == null && itemView instanceof LinearLayout) {
                root = (LinearLayout) itemView;
            }

            pluginName = itemView.findViewById(R.id.pedal_title);
            subtitleView = itemView.findViewById(R.id.pedal_subtitle);
            categoryBadge = itemView.findViewById(R.id.pedal_category_badge);
            pedalLed = itemView.findViewById(R.id.pedal_led);
            pedalIcon = itemView.findViewById(R.id.pedal_icon);
            deleteButton = itemView.findViewById(R.id.plugin_delete);
        }

        public TextView getTextView() {
            return pluginName;
        }
    }

    void setColor(int color) {
        primaryColor = color;
    }

    int getCategoryIconRes(String pluginName) {
        return CategoryHelper.getSelectorRes(pluginName);
    }

    boolean isAmpBlock(String pluginName) {
        return CategoryHelper.getCategory(pluginName) == CategoryHelper.Category.AMP;
    }

    int getCategoryActiveColor(String pluginName) {
        return CategoryHelper.getColor(pluginName);
    }

    int getCategoryInactiveColor(String pluginName) {
        return Color.parseColor("#3A3D45");
    }

    void addItem(int pluginID, int index) {
        plugins.add(pluginID);
        notifyItemInserted(index);
    }

    /** Insert at an arbitrary slot (mirrors Engine.insertRoutingBlock shift). */
    void insertItem(int pluginID, int index) {
        int at = Math.max(0, Math.min(index, plugins.size()));
        plugins.add(at, pluginID);
        notifyItemInserted(at);
    }

    void setSelectedModel(int plugin, int model) {
        selectedModels.put(plugin, model);
    }

    void setAudioFiles(int plugin, String filename) {
        audioFiles.put(plugin, filename);
    }

    void setAudioFilesR(int plugin, String filename) {
        audioFilesR.put(plugin, filename);
    }

    void reset() {
        plugins.clear();
        holders.clear();
        notifyDataSetChanged();
    }

    void deleteAll() {
        for (int i = 0; i < plugins.size(); i++) {
            deleteItem(i);
        }
    }

    void deleteItem(int index) {
        if (index >= plugins.size()) {
            Log.w(TAG, "deleteItem: index >= plugins size", null);
            return;
        }

        plugins.remove(index);
        notifyItemRemoved(index);
    }

    void moveItemUp(int index) {
        if (index <= 0 || index >= plugins.size()) return;
        int p = plugins.remove(index);
        plugins.add(index - 1, p);
        notifyItemMoved(index, index - 1);
    }

    void moveItemDown(int index) {
        if (index < 0 || index >= plugins.size() - 1) return;
        int p = plugins.remove(index);
        plugins.add(index + 1, p);
        notifyItemMoved(index, index + 1);
    }
}
