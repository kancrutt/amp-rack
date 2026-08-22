package com.kancrutt.gigginggear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class MyPresets extends Fragment {
    MainActivity mainActivity;
    Spinner quickSpinner = null ;
    String TAG = getClass().getSimpleName();
    RecyclerView recyclerView;
    public MyPresetsAdapter myPresetsAdapter ;
    LocalPresetManager localPresetManager;
    ProgressBar progressBar = null;
    LinearLayout quickHeader = null ;
    PopupMenu sortMenu = null;
    boolean shared = false;
    boolean quick = false ;
    boolean factory = false ;
    public ToggleButton favToggle;

    public MyPresets () {

    }

    MyPresets (ProgressBar _progressBar) {
        progressBar = _progressBar;
    }

    MyPresets (boolean _shared) {
        shared = _shared;
    }
    MyPresets (boolean _shared, boolean _quick) {
        shared = _shared;
        quick = _quick;
    }
    MyPresets (boolean _shared, boolean _quick, boolean _factory) {
        shared = _shared;
        quick = _quick;
        factory = _factory;
    }

    void load () {
        if (myPresetsAdapter != null) {
            ArrayList<Map> mapList = new ArrayList<>();
            if (factory) {
                loadFactoryPresets(mapList);
            } else if (localPresetManager != null) {
                java.util.List<LocalPresetManager.Preset> list = localPresetManager.loadAllPresets();
                for (LocalPresetManager.Preset p : list) {
                    try {
                        org.json.JSONObject jo = LocalPresetManager.presetToJson(p);
                        HashMap<String, Object> map = new Gson().fromJson(jo.toString(), HashMap.class);
                        mapList.add(map);
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting preset for adapter", e);
                    }
                }
            }
            myPresetsAdapter.presets = mapList;
            myPresetsAdapter.allPresets = new ArrayList<>(mapList);
            myPresetsAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
        if (myPresetsAdapter != null) {
            myPresetsAdapter.quick = quick;
        }
    }

    /** Show a friendly message instead of a silent blank list. */
    void updateEmptyState () {
        View root = getView();
        if (root == null) return;
        View empty = root.findViewById(R.id.presets_empty_state);
        if (empty != null) {
            boolean isEmpty = myPresetsAdapter == null || myPresetsAdapter.getItemCount() == 0;
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Load pre-bundled stage rigs from assets/factory_presets/*.json
     * (schema-v1 JSON, same shape as LocalPresetManager output).
     */
    void loadFactoryPresets (ArrayList<Map> mapList) {
        int loaded = 0;
        try {
            String[] files = mainActivity.getAssets().list("factory_presets");
            if (files == null) {
                Log.w(TAG, "loadFactoryPresets: no factory_presets dir in assets");
                MainActivity.toast("No factory presets bundled");
                return;
            }
            java.util.Arrays.sort(files);
            for (String f : files) {
                if (!f.endsWith(".json"))
                    continue;
                // Per-file isolation: one bad asset must not blank the whole tab
                try {
                    org.json.JSONObject jo = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "factory_presets/" + f);
                    if (jo == null) {
                        Log.e(TAG, "loadFactoryPresets: cannot parse " + f);
                        continue;
                    }
                    HashMap<String, Object> map = new Gson().fromJson(jo.toString(), HashMap.class);
                    mapList.add(map);
                    loaded++;
                } catch (Exception e) {
                    Log.e(TAG, "loadFactoryPresets: failed on " + f, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadFactoryPresets: ", e);
        }
        if (loaded == 0) {
            Log.w(TAG, "loadFactoryPresets: 0 factory presets loaded");
            MainActivity.toast("No factory presets could be loaded");
        } else {
            Log.i(TAG, "loadFactoryPresets: loaded " + loaded + " factory presets");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_presets, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mainActivity == null)
            mainActivity = (MainActivity) getActivity();
        localPresetManager = new LocalPresetManager(mainActivity);

        recyclerView = (RecyclerView) ((LinearLayout) view).getChildAt(2);
        Button loadMore = view.findViewById(R.id.load_more);
        ProgressBar loadProgress = view.findViewById(R.id.load_progress);
        loadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadProgress.setVisibility(View.VISIBLE);
                loadMore.setVisibility(View.INVISIBLE);
                load();

            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == myPresetsAdapter.presets.size() - 1) {
//                    loadMore.setVisibility(View.VISIBLE);
//                    loadProgress.setVisibility(View.INVISIBLE);
//                    loadProgress.setVisibility(View.VISIBLE);
//                    loadMore.setVisibility(View.INVISIBLE);
                    if (favToggle == null || ! favToggle.isChecked())
                        load();
                }
            }
        });
                quickHeader = (LinearLayout) ((LinearLayout) view).getChildAt(0);
        myPresetsAdapter = new MyPresetsAdapter();
        myPresetsAdapter.setMainActivity(mainActivity);
        myPresetsAdapter.loadProgress = loadProgress;
        recyclerView.setAdapter(myPresetsAdapter);
        myPresetsAdapter.onListChanged = this::updateEmptyState;
        if (mainActivity.rack != null) {
            myPresetsAdapter.quickPatchProgress = mainActivity.rack.quickPatchProgress;
        }
        load();

        LinearLayout layout = (LinearLayout) view ;
        LinearLayout lx = (LinearLayout) ((LinearLayout) view).getChildAt(1);
        if (!shared && !factory) {
            lx.setVisibility(View.GONE);
        } else {
            EditText editText = (EditText) lx.getChildAt(0);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    Log.d(TAG, "afterTextChanged: " + editable.toString());
                    myPresetsAdapter.updateList(editable.toString());
                }
            });
        }

        if (shared && !factory) {
            ToggleButton toggleButton = (ToggleButton) lx.getChildAt(1);
            favToggle = toggleButton;
            toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    myPresetsAdapter.showOnlyFavorites(b);
                    if (b)
                        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
                    else
                        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
                }
            });

            File f = new File(mainActivity.favPresetsDir);
            File[] list = f.listFiles();
            for (File file: list) {
                JSONObject jsonObject = ConnectGuitar.loadJSONFromFile(mainActivity, Uri.fromFile(file));
                if (jsonObject == null) {
                    Log.e(TAG, "onViewCreated: " + file.getAbsolutePath() + " is null");
                    continue;
                }

                myPresetsAdapter.addPreset(MainActivity.JSONtoMap(jsonObject));
            }
        }

        Button sortByButton = (Button) lx.getChildAt(2);
        sortMenu = new PopupMenu(mainActivity, sortByButton);
        sortMenu.getMenuInflater().inflate(R.menu.sort_by, sortMenu.getMenu());

        /*
        switch (((MainActivity) getActivity()).defaultSharedPreferences.getString("orderBy", "timestamp")) {
            default:
            case "timestamp":
                sortMenu.getMenu().findItem(2).setChecked(true);
                break ;
            case "likes":
                sortMenu.getMenu().findItem(1).setChecked(true);
                break ;
        }

         */

        sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                SharedPreferences sharedPreferences = ((MainActivity) getActivity()).defaultSharedPreferences;
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    default:
                    case R.id.sort_by_time:
                        myPresetsAdapter.sortBy = "timestamp";
                        sharedPreferences.edit().putString("orderBy", "timestamp").apply();
                        break ;
                    case R.id.sort_by_likes:
                        myPresetsAdapter.sortBy = "likes";
                        sharedPreferences.edit().putString("orderBy", "likes").apply();
                        break ;
                }

                myPresetsAdapter.removeAll();
                load();
                return true;
            }
        });

        if (quick) {
            List<String> labels = new ArrayList<String>();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            quickHeader.setVisibility(View.VISIBLE);
            quickSpinner = (Spinner) quickHeader.getChildAt(1);
            labels.add("Factory Presets");
            Set<String> vals = sharedPreferences.getStringSet("collections", null) ;
            Log.d(TAG, "onViewCreated: vals: " + vals);
            if (vals != null) {
                Log.d(TAG, "onViewCreated: adding collections " + vals);
                for (String v: vals) {
                    labels.add(v);
                }
            }

            labels.add ("Load from file") ;
            labels.add ("More presets online") ;

            Log.d(TAG, "onViewCreated: labels: " + labels);

            ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, labels);
            quickSpinner.setAdapter(categoriesDataAdapter);
            quickSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == parent.getAdapter().getCount() - 1) {
                        String url = "https://amprack.acoustixaudio.org/view.php?type=Presets";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                        return;
                    } else if (position == parent.getAdapter().getCount() - 2) {
                        Intent intent_upload = new Intent();
                        intent_upload.setType("*/*");
                        intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                        intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getActivity().startActivityForResult(intent_upload,100);
                        return ;
                    }

                    myPresetsAdapter.removeAll();
                    if (position == 0) {
                        load();
                        return;
                    }

                    Set<String> vals = sharedPreferences.getStringSet("collections", null) ;
                    Log.d(TAG, "onItemSelected: " + vals);
                    String name = (String) vals.toArray() [position - 1];
                    String s = sharedPreferences.getString(name, null);
                    if (s == null) {
                        MainActivity.toast("Cannot load collection " + name);
                        return;
                    }

                    Log.d(TAG, "onItemSelected: loading collection " + name + "\n" + s);

                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        Iterator<String> keys = jsonObject.keys();

                        while(keys.hasNext()) {
                            String key = keys.next();
                            JSONObject preset = jsonObject.getJSONObject(key);
                            Log.d(TAG, "onCreate: key " + key);
                            HashMap<String, Object> yourHashMap = new Gson().fromJson(preset.toString(), HashMap.class);

                            myPresetsAdapter.addPreset(yourHashMap);
                        }
                    } catch (JSONException e) {
                        MainActivity.toast("Cannot load collection " + name + "\n"+e.getLocalizedMessage());
                        Log.e(TAG, "onItemClick: ", e);
                        return;
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if (mainActivity.tabletMode) {
                LinearLayout horiz = new LinearLayout(mainActivity);
                horiz.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout parent = (LinearLayout) recyclerView.getParent();
                parent.removeView(recyclerView);
                parent.addView(horiz);
                horiz.addView(recyclerView);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                recyclerView.setLayoutParams(layoutParams);

                LinearLayout ll = new LinearLayout(mainActivity);
                ll.setLayoutParams(layoutParams);

                ImageView up = new ImageView(mainActivity),
                        down = new ImageView(mainActivity);
//                up.setText("Patch up ⬆");
//                down.setText("Patch down ⬇");

                up.setImageDrawable(getResources().getDrawable(R.drawable.baseline_arrow_drop_up_24));
                down.setImageDrawable(getResources().getDrawable(R.drawable.baseline_arrow_drop_down_24));

                TextView t1 = new TextView(mainActivity),
                         t2 = new TextView(mainActivity);
                t1.setText("Patch");
                t2.setText(R.string.app_version);
                t1.setTextSize(48);
                t1.setTextSize(40);
                t1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                t2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    t1.setTypeface(getResources().getFont(R.font.gruppo));
                    t2.setTypeface(getResources().getFont(R.font.start));
                }

                ll.setOrientation(LinearLayout.VERTICAL);
                ll.addView(t1);
                ll.addView(up);
                ll.addView(down);
                ll.addView(t2);

                up.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainActivity.rack.patchMove(true);
                    }
                });

                down.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainActivity.rack.patchMove(false);
                    }
                });

                horiz.addView(ll);
            }
        }

        if (!shared && !factory) {
            sortByButton.setVisibility(View.GONE);
        }

        sortByButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortMenu.show();
            }
        });

        if (progressBar != null)
            myPresetsAdapter.setProgressBar(progressBar);
        load();
    }
}