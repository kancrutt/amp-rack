package com.kancrutt.gigginggear;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "Settings";
    public AudioManager audioManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);

            Preference purchase = findPreference("purchase_pro");
            if (purchase != null) {
                purchase.setVisible(false);
            }

            Preference privacy = findPreference("privacy_policy");
            if (privacy != null) {
                privacy.setOnPreferenceClickListener(preference -> {
                    String url = "https://amprack.acoustixaudio.org/privacy.php";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return false;
                });
            }

            Preference cool = findPreference("cool");
            if (cool != null) {
                cool.setOnPreferenceClickListener(preference -> {
                    MainActivity.about();
                    return false;
                });
            }
        }
    }

    public static class MessagesFragment extends PreferenceFragmentCompat {
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        int defaultInputDevice = 0 ;
        int defaultOutputDevice = 0 ;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.messages_preferences, rootKey);
            ListPreference listPreference = findPreference("input");
            ListPreference listPreferenceOutput = findPreference("output");

            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> entryValues = new ArrayList<>();
            // Host-agnostic: works when embedded in MainActivity's settings tab too
            AudioManager audioManager;
            if (getActivity() instanceof SettingsActivity) {
                audioManager = ((SettingsActivity) getActivity()).audioManager;
            } else if (getActivity() != null) {
                audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            } else {
                audioManager = null;
            }
            if (audioManager == null) {
                MainActivity.alert("Cannot connect to Audio Manager", "Cannot connect to audio manager. Please restart the app");
                return;
            }

            audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
            audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

            entries.add("Default");
            entryValues.add("-1");

            for (int i = 0 ; i < audioDevicesInput.length ; i ++) {
                String name = MainActivity.typeToString(audioDevicesInput[i].getType());
                int deviceID = audioDevicesInput[i].getId();
//                name = (String) audioDevicesInput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesInput[i].getProductName() + ")");
//                entryValues.add(String.valueOf(audioDevicesInput[i]));
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    AudioEngine.setRecordingDeviceId(new Integer(newValue.toString()));
                    return true;
                }
            });

            entries.clear();
            entryValues.clear();

            entries.add("Default");
            entryValues.add("-1");

            for (int i = 0 ; i < audioDevicesOutput.length ; i ++) {
                String name = MainActivity.typeToString(audioDevicesOutput[i].getType());
                int deviceID = audioDevicesOutput[i].getId();
//                name = (String) audioDevicesOutput[i].getProductName();
                entries.add(name + " (" + (String) audioDevicesOutput[i].getProductName() + ")");
                entryValues.add(String.valueOf(deviceID));
                Log.d(TITLE_TAG, "onCreatePreferences: " + String.format ("%s: %s", deviceID, name));
            }

            listPreferenceOutput.setEntries(entries.toArray(new CharSequence[entries.size()]));
            listPreferenceOutput.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

            listPreferenceOutput.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.d(getClass().getSimpleName(), "onPreferenceChange: [playbackDeviceID] " + newValue.toString());
//                    AudioEngine.setPlaybackDeviceId(Integer.parseInt(newValue.toString()));
                    return true;
                }
            });
        }
    }

    public static class SyncFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey);
            /*
            if (MainActivity.proVersion == false) {
                ListPreference export = findPreference("export_format");
                export.setSummary("More export formats are available in the Pro Version");
                export.setShouldDisableView(true);
                export.setEnabled(false);
            }

             */
        }
    }

    public static class ThemeFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.theme_settings, rootKey);
            SwitchPreference darkMode = findPreference("dark_mode");
            if (darkMode != null) {
                darkMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String scheme = Boolean.TRUE.equals(newValue) ? "Theme.AmpRack" : "Theme.Light";
                        PreferenceManager.getDefaultSharedPreferences(getContext())
                                .edit().putString("color_scheme", scheme).apply();
                        return true;
                    }
                });
            }
        }
    }

    public static class ProfileFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.account_pref, rootKey);
        }
    }

    public static class VersionFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.version_info, rootKey);
            Preference bNum = findPreference("build_number");
            if (bNum != null) {
                bNum.setSummary(R.string.build_id);
                bNum.setOnPreferenceClickListener(preference -> {
                    HashCommands commands = new HashCommands(getContext());
                    commands.show();
                    return false;
                });
            }

            Preference bName = findPreference("build_name");
            if (bName != null) {
                bName.setSummary(BuildConfig.VERSION_NAME);
                bName.setOnPreferenceClickListener(preference -> {
                    MainActivity.about();
                    return false;
                });
            }

            Preference vCode = findPreference("version_code");
            if (vCode != null) {
                vCode.setSummary(BuildConfig.VERSION_CODE + "-" + BuildConfig.BUILD_TYPE);
            }

            Preference pCount = findPreference("plugins");
            if (pCount != null) {
                pCount.setSummary(String.valueOf(MainActivity.totalPlugins));
            }

            Preference enc = findPreference("encoders_supported");
            if (enc != null) {
                enc.setSummary("WAV 16/24/32-Bit Float");
            }

            Preference nameInfo = findPreference("name_info");
            if (nameInfo != null) {
                nameInfo.setTitle("Giggin' Gear Stage");
            }
        }
    }

    public static class TracksFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.tracks_settings, rootKey);
        }
    }

    public static class CameraSettings extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.camera_settings, rootKey);

        }
    }
}