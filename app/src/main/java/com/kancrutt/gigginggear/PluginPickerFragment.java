package com.kancrutt.gigginggear;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;

public class PluginPickerFragment extends BottomSheetDialogFragment {

    private MainActivity mainActivity;
    private RecyclerView recyclerView;
    private EditText searchEditText;

    public static PluginPickerFragment newInstance() {
        return new PluginPickerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_plugin_picker, container, false);
        if (getActivity() instanceof MainActivity) {
            mainActivity = (MainActivity) getActivity();
        }

        ImageButton closeBtn = view.findViewById(R.id.picker_close_btn);
        closeBtn.setOnClickListener(v -> dismiss());

        searchEditText = view.findViewById(R.id.picker_search);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mainActivity != null && mainActivity.pluginDialogAdapter != null) {
                    mainActivity.pluginDialogAdapter.search(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ChipGroup chipGroup = view.findViewById(R.id.picker_category_chips);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (mainActivity == null || mainActivity.pluginDialogAdapter == null) return;
            if (checkedId == R.id.chip_cat_all) {
                mainActivity.pluginDialogAdapter.filterByCategory("All");
            } else if (checkedId == R.id.chip_cat_amps) {
                mainActivity.pluginDialogAdapter.filterByCategory("AMP/CAB");
            } else if (checkedId == R.id.chip_cat_overdrive) {
                mainActivity.pluginDialogAdapter.filterByCategory("OVERDRIVE");
            } else if (checkedId == R.id.chip_cat_distortion) {
                mainActivity.pluginDialogAdapter.filterByCategory("DISTORTION");
            } else if (checkedId == R.id.chip_cat_modulation) {
                mainActivity.pluginDialogAdapter.filterByCategory("MODULATION");
            } else if (checkedId == R.id.chip_cat_delay) {
                mainActivity.pluginDialogAdapter.filterByCategory("DELAY");
            } else if (checkedId == R.id.chip_cat_reverb) {
                mainActivity.pluginDialogAdapter.filterByCategory("REVERB");
            } else if (checkedId == R.id.chip_cat_utility) {
                mainActivity.pluginDialogAdapter.filterByCategory("UTILITY");
            }
        });

        recyclerView = view.findViewById(R.id.picker_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (mainActivity != null && mainActivity.pluginDialogAdapter != null) {
            recyclerView.setAdapter(mainActivity.pluginDialogAdapter);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // WRAP_CONTENT height lets BottomSheetBehavior size the sheet to its
            // content, capped at its max height (90% of screen). MATCH_PARENT here
            // made the sheet overflow in landscape (short viewport) before content
            // was visible — only the title bar showed.
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // Force the sheet fully expanded (never collapsed to peek height, which
        // shows only the title bar) and pin it expanded so landscape can't leave
        // it half-open.
        if (getDialog() != null) {
            View sheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
        // Cap the plugin list height so the sheet never overflows, and the list
        // is always internally scrollable. Reserve room for handle+header+search
        // (~210dp); leave the list at least 120dp tall.
        View view = getView();
        if (view != null) {
            RecyclerView rv = view.findViewById(R.id.picker_recycler_view);
            if (rv != null) {
                int screenH = getResources().getDisplayMetrics().heightPixels;
                float density = getResources().getDisplayMetrics().density;
                int reserved = Math.round(210 * density);
                int listH = Math.max(Math.round(120 * density),
                        Math.round(0.78f * screenH) - reserved);
                ViewGroup.LayoutParams lp = rv.getLayoutParams();
                lp.height = listH;
                rv.setLayoutParams(lp);
            }
        }
    }
}
