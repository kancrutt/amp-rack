package com.kancrutt.gigginggear;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * In-app host for the bottom-nav SETTING tab.
 * Mirrors SettingsActivity.HeaderFragment so the exact same header_preferences
 * XML and behaviors drive both entry points (functional preservation).
 * Sub-screens are resolved by MainActivity via OnPreferenceStartFragmentCallback,
 * reusing SettingsActivity's existing static preference fragments.
 */
public class SettingsHostFragment extends PreferenceFragmentCompat {

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
