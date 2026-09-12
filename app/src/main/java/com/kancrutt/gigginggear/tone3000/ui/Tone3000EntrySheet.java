package com.kancrutt.gigginggear.tone3000.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kancrutt.gigginggear.R;
import com.kancrutt.gigginggear.tone3000.Tone3000AuthManager;
import com.kancrutt.gigginggear.tone3000.Tone3000Config;
import com.kancrutt.gigginggear.tone3000.model.ApiException;

/**
 * Compact partnership/entry surface shown before authentication.
 * Shows Giggin' Gear + TONE3000 branding, description, and Browse button.
 * Auth itself is delegated to TONE3000 Select flow via Custom Tabs.
 */
public class Tone3000EntrySheet extends BottomSheetDialogFragment {

    public static Tone3000EntrySheet newInstance() { return new Tone3000EntrySheet(); }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog d = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        d.setOnShowListener(dlg -> {
            FrameLayout bs = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) {
                BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(bs);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
                b.setSkipCollapsed(true);
            }
        });
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_tone3000_entry, container, false);

        ImageButton close = v.findViewById(R.id.t3k_entry_close);
        if (close != null) close.setOnClickListener(x -> dismiss());

        Button browse = v.findViewById(R.id.t3k_browse_btn);
        TextView subtitle = v.findViewById(R.id.t3k_entry_subtitle);
        TextView legal = v.findViewById(R.id.t3k_entry_legal);

        if (subtitle != null) {
            subtitle.setText("Giggin' Gear integrates with TONE3000 to give you access to a massive library of Neural Amp Modeler captures and IRs created by the TONE3000 community.");
        }
        if (legal != null) {
            legal.setText("You will be redirected to tone3000.com to browse and select a tone. TONE3000 handles sign-in and catalog browsing.");
            legal.setTextColor(Color.parseColor("#8B8B8F"));
        }

        if (browse != null) {
            browse.setOnClickListener(x -> {
                if (!Tone3000Config.isConfigured(requireContext())) {
                    Toast.makeText(requireContext(), "TONE3000 not configured — missing client_id. Contact support.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Tone3000AuthManager.AuthStart start = Tone3000AuthManager.get(requireContext()).startSelectFlow(requireContext());
                    CustomTabsIntent cti = new CustomTabsIntent.Builder().setShowTitle(true).build();
                    cti.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    cti.launchUrl(requireContext(), Uri.parse(start.authorizeUrl));
                    // keep sheet open briefly; dismiss after launch so callback can still be handled
                    dismiss();
                } catch (ApiException e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to start TONE3000: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        View learnMore = v.findViewById(R.id.t3k_learn_more);
        if (learnMore != null) {
            learnMore.setOnClickListener(x -> {
                try {
                    CustomTabsIntent cti = new CustomTabsIntent.Builder().build();
                    cti.launchUrl(requireContext(), Uri.parse("https://www.tone3000.com/api"));
                } catch (Exception ignore) {}
            });
        }

        return v;
    }
}
