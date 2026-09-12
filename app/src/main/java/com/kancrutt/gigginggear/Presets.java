package com.kancrutt.gigginggear;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Presets extends Fragment {
    View view ;
    ViewPager2 viewPager;
    LinearLayout loginNotice ;
    TabLayout tabLayout ;
    PresetAdapter fragmentStateAdapter ;
    private static final String TAG = "Presets";
    MainActivity mainActivity;
    LocalPresetManager localPresetManager;
    ProgressBar progressPreset ;
    LinearLayout progressLayout ;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: view created");
        view = inflater.inflate(R.layout.fragment_presets,null) ;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        localPresetManager = new LocalPresetManager(getContext());

        fragmentStateAdapter = new PresetAdapter(this);
        ConstraintLayout constraintLayout = (ConstraintLayout) view  ;
        loginNotice = view.findViewById(R.id.login_notice);
        tabLayout = view.findViewById(R.id.my_preset_tab_layout);
        progressLayout = view.findViewById(R.id.preset_progress_bar);
        progressPreset = (ProgressBar) progressLayout.getChildAt(0);
        viewPager = view.findViewById(R.id.presets_pager);

        if (loginNotice != null) {
            loginNotice.setVisibility(View.GONE);
        }
        if (tabLayout != null) {
            if (mainActivity.tabletMode) {
                tabLayout.setVisibility(View.GONE);
            } else {
                tabLayout.setVisibility(View.VISIBLE);
            }
        }

        viewPager.setSaveEnabled(false);
        fragmentStateAdapter.createFragment(0);
        if (! mainActivity.tabletMode)
            fragmentStateAdapter.createFragment(1);
        else {
            // Tablet split view: LEFT = Factory/Library presets (read-only reference),
            // RIGHT (viewPager pos 0) = My Presets (local). Using factory=true stops the
            // left panel from re-loading the same local list (the "double list" bug) and
            // keeps save/delete/organize operating on a single source of truth.
            fragmentStateAdapter.libraryPresets = new MyPresets(true, false, true);
            fragmentStateAdapter.libraryPresets.mainActivity = mainActivity ;
            fragmentStateAdapter.libraryPresets.progressBar = progressPreset;

            LinearLayout ll = (LinearLayout) fragmentStateAdapter.libraryPresets.onCreateView(mainActivity.getLayoutInflater(), null, null);
            fragmentStateAdapter.libraryPresets.onViewCreated(ll, null);

            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);
            ll.setLayoutParams(layoutParams);
            viewPager.setLayoutParams(layoutParams);

            ConstraintLayout root = mainActivity.findViewById(R.id.presets_constraint);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.presets_library_layout, ConstraintSet.LEFT,R.id.presets_pager,ConstraintSet.RIGHT,0);
            constraintSet.connect(R.id.presets_pager, ConstraintSet.TOP,R.id.my_preset_tab_layout,ConstraintSet.BOTTOM,0);

            constraintSet.applyTo(constraintLayout);

            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);

            LinearLayout libLayout = view.findViewById(R.id.presets_library_layout);
            // onViewCreated can run again (rotation, tab revisit): clear first so the
            // factory panel is never appended a second time (double-list recurrence).
            libLayout.removeAllViews();
            libLayout.addView(ll);
        }

        viewPager.setAdapter(fragmentStateAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (! mainActivity.tabletMode)
                    viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        LinearLayout layout = view.findViewById(R.id.new_preset_layout);
        ExtendedFloatingActionButton presetFab = view.findViewById(R.id.fab_preset);
        presetFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: showing preset");
                Map preset ;
                preset = mainActivity.presetToMap();

                AlertDialog.Builder builder = MainActivity.dialogBuilder(getActivity());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.save_preset_dialog, null);
                builder.setView(linearLayout).setCancelable(false);

                LinearLayout linearLayout1 = (LinearLayout) linearLayout.getChildAt(3);
                MaterialButton save = (MaterialButton) linearLayout1.getChildAt(1);
                ProgressBar progressBar = (ProgressBar) linearLayout.getChildAt(4);
                AlertDialog dialog = builder.create() ;

                EditText name = (EditText) linearLayout.getChildAt(1);
                EditText desc = (EditText) linearLayout.getChildAt(2);

                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (name.getText().toString().length() == 0) {
                            mainActivity.toast("Name of preset is required.");
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);
                        // Snapshot the LIVE rig (engine + canvas), not an empty shell.
                        LocalPresetManager.Preset p = mainActivity.buildPresetFromChain(
                                name.getText().toString(), desc.getText().toString());
                        // Overwrite-by-name so re-saving keeps one file per name.
                        try {
                            String keepId = null;
                            long keepCreated = 0;
                            for (LocalPresetManager.Preset existing : localPresetManager.loadAllPresets()) {
                                if (existing.name != null && existing.name.equals(p.name)) {
                                    if (keepId == null) {
                                        keepId = existing.id;
                                        keepCreated = existing.createdAt;
                                    } else if (existing.id != null && !existing.id.equals(keepId)) {
                                        try { localPresetManager.deletePreset(existing.id); } catch (Exception ignore) {}
                                    }
                                }
                            }
                            if (keepId != null) {
                                p.id = keepId;
                                if (keepCreated != 0) p.createdAt = keepCreated;
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "save preset dedup failed", e);
                        }
                        localPresetManager.savePreset(p);

                        mainActivity.patchName.setText(name.getText());
                        mainActivity.patchDesc.setText(desc.getText());
                        dialog.dismiss();
                        mainActivity.refreshPresetLists();
                    }
                });

                MaterialButton cancel = (MaterialButton) linearLayout1.getChildAt(0);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss ();
                    }
                });

                dialog.show();

            }
        });

        if (mainActivity.useTheme) {
        }
    }

    public class PresetAdapter extends FragmentStateAdapter {
        public ArrayList<Fragment> arrayList = new ArrayList<>();
        public MyPresets libraryPresets ;
        public MyPresets myPresets;
        private Presets presetsFragment;

        public MyPresets getMyPresets() {
            return myPresets;
        }

        public PresetAdapter(@NonNull Presets fragmentActivity) {
            super(fragmentActivity);
            this.presetsFragment = fragmentActivity;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                myPresets = new MyPresets(progressPreset);
                arrayList.add(myPresets);
                return myPresets;
            } else {// if (position == 1) {
                libraryPresets = new MyPresets(true, false, true);
                libraryPresets.progressBar = progressPreset;
                arrayList.add(libraryPresets);
                return libraryPresets;
            }

//            Log.e(TAG, "createFragment: unknown position " + position, null);
//            return null ;
        }

        @Override
        public int getItemCount() {
            if (presetsFragment != null && presetsFragment.mainActivity != null && presetsFragment.mainActivity.tabletMode) {
                return 1;
            }
            return 2;
        }


    }

    public static class PresetFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.my_presets, container, false);
        }
    }
}
