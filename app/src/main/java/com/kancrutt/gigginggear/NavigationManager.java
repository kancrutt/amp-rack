package com.kancrutt.gigginggear;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Manages 4-tab stage navigation (Rig View, Presets, Tuner Overlay, Settings).
 * Extracted from MainActivity to decompose God Activity.
 */
public class NavigationManager {
    public enum Tab {
        RIG_VIEW,
        PRESETS,
        TUNER,
        SETTINGS
    }

    private final FragmentManager fragmentManager;
    private final int containerId;

    private Fragment rigViewFragment;
    private Fragment presetsFragment;
    private Fragment settingsFragment;
    private Tab currentTab = Tab.RIG_VIEW;

    public NavigationManager(@NonNull FragmentManager fragmentManager, int containerId) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }

    public void initFragments(Fragment rigView, Fragment presets, Fragment settings) {
        this.rigViewFragment = rigView;
        this.presetsFragment = presets;
        this.settingsFragment = settings;

        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (!rigViewFragment.isAdded()) ft.add(containerId, rigViewFragment, "RIG_VIEW");
        if (!presetsFragment.isAdded()) ft.add(containerId, presetsFragment, "PRESETS").hide(presetsFragment);
        if (!settingsFragment.isAdded()) ft.add(containerId, settingsFragment, "SETTINGS").hide(settingsFragment);
        ft.commitAllowingStateLoss();
    }

    public void selectTab(Tab tab) {
        if (tab == Tab.TUNER) {
            // Tuner is an overlay dialog/sheet, handled separately by host activity
            return;
        }

        if (currentTab == tab) return;

        FragmentTransaction ft = fragmentManager.beginTransaction();
        hideCurrentTab(ft);

        switch (tab) {
            case RIG_VIEW:
                if (rigViewFragment != null) ft.show(rigViewFragment);
                break;
            case PRESETS:
                if (presetsFragment != null) ft.show(presetsFragment);
                break;
            case SETTINGS:
                if (settingsFragment != null) ft.show(settingsFragment);
                break;
        }
        ft.commitAllowingStateLoss();
        currentTab = tab;
    }

    private void hideCurrentTab(FragmentTransaction ft) {
        switch (currentTab) {
            case RIG_VIEW:
                if (rigViewFragment != null) ft.hide(rigViewFragment);
                break;
            case PRESETS:
                if (presetsFragment != null) ft.hide(presetsFragment);
                break;
            case SETTINGS:
                if (settingsFragment != null) ft.hide(settingsFragment);
                break;
        }
    }

    public Tab getCurrentTab() {
        return currentTab;
    }
}
