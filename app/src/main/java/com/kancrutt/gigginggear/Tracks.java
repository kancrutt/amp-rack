package com.kancrutt.gigginggear;

import static com.kancrutt.gigginggear.MainActivity.context;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.material.navigation.NavigationBarItemView;
import com.google.android.material.slider.Slider;

import java.io.File;

public class Tracks extends Fragment {
    MainActivity mainActivity = null;
    TracksAdapter tracksAdapter ;
    RecyclerView recyclerView ;
    ToggleButton playPause ;
    boolean themeInit = false;
    String TAG = getClass().getSimpleName();
    SurfaceView surfaceView;
    String filesDir ;
    ConstraintLayout constraintLayout ;
    ExoPlayer player ;
    static int requestCode = 1001 ;
    LinearLayout playerWindow ;
    boolean isDrums = false ;
    BitmapDrawable play, pause, reset ;

    public Tracks () {
        tracksAdapter = new TracksAdapter();
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.tracksAdapter.mainActivity = mainActivity;
    }

    public Tracks (MainActivity activity) {
        mainActivity = activity;
        tracksAdapter = new TracksAdapter();
        tracksAdapter.mainActivity = mainActivity;
    }
    public Tracks (MainActivity activity, boolean _isDrums) {
        mainActivity = activity ;
        tracksAdapter = new TracksAdapter();
        tracksAdapter.mainActivity = mainActivity;
        isDrums = _isDrums;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.tracks, null);
        return constraintLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        constraintLayout = (ConstraintLayout) view;
        recyclerView = (RecyclerView) view.findViewById(R.id.tracks_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        recyclerView.setAdapter(tracksAdapter);
        surfaceView = view.findViewById(R.id.tracks_video);
//        load (mainActivity.dir);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                player.setVideoSurface(surfaceView.getHolder().getSurface());

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        LinearLayout shareTip = view.findViewById(R.id.share_tip);
        FrameLayout frameLayout = view.findViewById(R.id.vframe);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 1) {
                    shareTip.setVisibility(View.GONE);
                    recyclerView.setOnScrollListener(null);
                }
            }
        });

        player = new ExoPlayer.Builder(context).build();
        player.setVideoScalingMode (C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        tracksAdapter.player = player;

        playPause = view.findViewById(R.id.tracks_play);
        tracksAdapter.tracks = this;
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPause.setChecked(false);
            }
        });
        playPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getContext() == null) {
                    Log.d("context is null", "onCheckedChanged() called with: compoundButton = [" + compoundButton + "], b = [" + b + "]");
                    return;
                }

                if (player == null) {
                    MainActivity.alert("Inconsistent app state", "Media Player is not initialized.");
                    return;
                }

                if (!b) {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_modern_play_btn));
                    player.pause();
                    frameLayout.setVisibility(View.GONE);
                } else {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_modern_pause_btn));
                    player.play();
                    try {
                        if (player.getCurrentMediaItem() != null && player.getCurrentMediaItem().localConfiguration.uri.toString().endsWith(".mp4")) {
                            frameLayout.setVisibility(View.VISIBLE);
                        }
                    } catch (NullPointerException e) {
                        Log.e(TAG, "onCheckedChanged: ", e);
                        return;
                    }
                }

//                player.setSkipSilenceEnabled(b);
            }
        });


        player.addListener(new Player.Listener() {
            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                Player.Listener.super.onVideoSizeChanged(videoSize);
                Log.d(TAG, String.format ("[video size]: %d x %d", videoSize.width, videoSize.height));
                surfaceView.getHolder().setFixedSize(videoSize.width, videoSize.height);
            }
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                Log.d(TAG, "onPlayerStateChanged: " + playbackState);
                if (playbackState == Player.EVENT_PLAYBACK_STATE_CHANGED)
                    playPause.setChecked(false);

            }
        });

        Slider slider = view.findViewById(R.id.tracks_volume);
        slider.setValue(player.getVolume());
        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (fromUser) {
                    player.setVolume(value);
                }
            }
        });

        Slider bpm = view.findViewById(R.id.tracks_bpm);
        bpm.setValueFrom(40f);
        bpm.setValueTo(240f);
        bpm.setStepSize(1f);
        bpm.setValue(120f);
        bpm.setLabelFormatter(value -> String.valueOf((int) value));
        bpm.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                Log.d(TAG, "onValueChange BPM: " + (int) value);
                // Map 40-240 BPM relative to 120 baseline for playback speed (0.33x to 2.0x)
                float speed = value / 120f;
                PlaybackParameters param = new PlaybackParameters(speed);
                player.setPlaybackParameters(param);
            }
        });

        android.widget.ImageButton resetBPM = view.findViewById(R.id.tracks_reset_bpm);
        resetBPM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bpm.setValue(120f);
            }
        });

        CheckBox loop = view.findViewById(R.id.loop);
        loop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                }
                else {
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                }
            }
        });

        /*

        CheckBox gapless = mainActivity.findViewById(R.id.gapless);
        gapless.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                player.setSkipSilenceEnabled(isChecked);
                player.setPauseAtEndOfMediaItems(!isChecked);
            }
        });

         */

//        boolean dynamicsProcessingEnabled = mainActivity.defaultSharedPreferences.getBoolean("tracks_fx", true);
        /* commenting out because causes crashes on some devices

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && dynamicsProcessingEnabled) {
            // Cue CSI Miami music
            DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    false,
                    0,
                    true,
                    1,
                    false,
                    0,
                    true
            );

            DynamicsProcessing.Config config = null;
            config = builder.build();
            for (int i = 0 ; i < 2; i ++) {
                DynamicsProcessing.Mbc mbc = config.getMbcByChannelIndex(i);
                DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);
                mbcBand.setAttackTime(50);
                mbcBand.setReleaseTime(100);
                mbcBand.setRatio(4.0f);
                mbcBand.setThreshold(-50.0f);
                mbcBand.setKneeWidth(6);
                mbcBand.setNoiseGateThreshold(-70f);
                mbcBand.setExpanderRatio(6);
                mbcBand.setPreGain(0);
                mbcBand.setPostGain(0);
                DynamicsProcessing.Limiter limiter = config.getLimiterByChannelIndex(i);
                limiter.setAttackTime(25);
                limiter.setRatio(2);
                limiter.setThreshold(0);
                limiter.setReleaseTime(50);
            }

            if (player.getAudioSessionId() != C.AUDIO_SESSION_ID_UNSET) {
                DynamicsProcessing dynamicsProcessing = new DynamicsProcessing(0, player.getAudioSessionId(), config);
                player.setAuxEffectInfo(new AuxEffectInfo(dynamicsProcessing.getId(), 1));
            }
        }

         */

        Button loadFile = view.findViewById(R.id.load_file);
        loadFile.setPadding(10,10,10,10);

        ToggleButton skipSilence = view.findViewById(R.id.skip_silence);
        skipSilence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tracksAdapter.player.setSkipSilenceEnabled(isChecked);
            }
        });

        playerWindow = view.findViewById(R.id.tracks_player);
        // Skeuomorphic skin engine disabled for this screen: the design system
        // is dark flat/vector. Controls are styled in tracks.xml directly.

        loadFile.setOnClickListener(new View.OnClickListener() {
            boolean finalD = isDrums;
            @Override
            public void onClick(View v) {
                Intent intent_upload = new Intent();
                intent_upload.setType("audio/*");
                intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                if (finalD)
//                    getActivity().startActivityForResult(intent_upload,1001);
//                else
                getActivity().startActivityForResult(intent_upload,requestCode);
                requestCode ++ ;
            }
        });

        if (! isDrums)
            addDrumsLooper();

        // Guitar Stompbox Looper controls (REC / PLAY / STOP / CLEAR)
        ToggleButton recToggle = view.findViewById(R.id.tracks_rec);
        TextView recStatus = view.findViewById(R.id.tracks_rec_status);
        View playBtn = view.findViewById(R.id.looper_play_btn);
        View stopBtn = view.findViewById(R.id.looper_stop_btn);
        View clearBtn = view.findViewById(R.id.looper_clear_btn);

        if (recToggle != null && mainActivity != null && mainActivity.record != null) {
            recToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mainActivity.record.isChecked() != isChecked) {
                    mainActivity.record.setChecked(isChecked);
                }
                if (recStatus != null) {
                    recStatus.setText(isChecked ? "● RECORDING GUITAR LOOP…" : "Tap REC to loop guitar playing");
                    recStatus.setTextColor(isChecked ? 0xFFFF3B30 : 0xFF8F94A0);
                }
                buttonView.postDelayed(() -> {
                    boolean actual = mainActivity.recording;
                    if (recToggle.isChecked() != actual) {
                        recToggle.setChecked(actual);
                        if (recStatus != null) {
                            recStatus.setText(actual ? "● RECORDING GUITAR LOOP…" : "Tap REC to loop guitar playing");
                            recStatus.setTextColor(actual ? 0xFFFF3B30 : 0xFF8F94A0);
                        }
                    } else if (!actual && recStatus != null) {
                        if (mainActivity.lastRecordedFileName != null) {
                            recStatus.setText("Loop saved: " + new File(mainActivity.lastRecordedFileName).getName());
                            // Auto-register saved guitar loop to player adapter for instant playback
                            try {
                                tracksAdapter.add(mainActivity.lastRecordedFileName);
                            } catch (Exception ignore) {}
                        }
                    }
                }, 400);
            });
        }

        if (playBtn != null) {
            playBtn.setOnClickListener(v -> {
                if (mainActivity != null && mainActivity.lastRecordedFileName != null) {
                    File f = new File(mainActivity.lastRecordedFileName);
                    if (f.exists()) {
                        try {
                            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(f));
                            player.setMediaItem(mediaItem);
                            player.setRepeatMode(Player.REPEAT_MODE_ONE); // Stompbox seamless loop mode
                            player.prepare();
                            player.play();
                            if (playPause != null) {
                                playPause.setChecked(true);
                            }
                            if (recStatus != null) {
                                recStatus.setText("▶ Playing Guitar Loop (" + f.getName() + ")");
                                recStatus.setTextColor(0xFF00E676);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "play loop failed", e);
                        }
                    } else {
                        MainActivity.toast("No recorded loop found. Tap REC first!");
                    }
                } else {
                    MainActivity.toast("No recorded loop found. Tap REC first!");
                }
            });
        }

        if (stopBtn != null) {
            stopBtn.setOnClickListener(v -> {
                if (player != null) {
                    player.pause();
                }
                if (playPause != null) {
                    playPause.setChecked(false);
                }
                if (recStatus != null) {
                    recStatus.setText("Loop stopped");
                    recStatus.setTextColor(0xFF8F94A0);
                }
            });
        }

        if (clearBtn != null) {
            clearBtn.setOnClickListener(v -> {
                if (player != null) {
                    player.stop();
                    player.clearMediaItems();
                }
                if (playPause != null) {
                    playPause.setChecked(false);
                }
                if (mainActivity != null && mainActivity.lastRecordedFileName != null) {
                    try {
                        File f = new File(mainActivity.lastRecordedFileName);
                        if (f.exists()) f.delete();
                    } catch (Exception ignore) {}
                    mainActivity.lastRecordedFileName = null;
                }
                if (recStatus != null) {
                    recStatus.setText("Loop cleared. Ready for new recording.");
                    recStatus.setTextColor(0xFF8F94A0);
                }
                MainActivity.toast("Guitar loop cleared");
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Keep REC visual in sync with actual engine state when returning to this tab
        View v = getView() != null ? getView() : constraintLayout;
        if (v == null || mainActivity == null || mainActivity.record == null) return;
        ToggleButton recToggle = v.findViewById(R.id.tracks_rec);
        TextView recStatus = v.findViewById(R.id.tracks_rec_status);
        if (recToggle != null && recToggle.isChecked() != mainActivity.recording) {
            recToggle.setChecked(mainActivity.recording);
        }
        if (recStatus != null) {
            boolean active = mainActivity.recording;
            recStatus.setText(active ? "Recording…" : "Capture your playing");
            recStatus.setTextColor(active ? 0xFFFF3B30 : 0xFF8F94A0);
        }
    }

    public void addDrumsLooper () {
        if (isDrums)
            return;

        if (mainActivity == null) {
            Log.e(TAG, "addDrumsLooper: main activity is null!");
            return;
        }

        if (mainActivity.tabletMode) {
            if (mainActivity.drums == null) {
                Log.e(TAG, "addDrumsLooper: drums is null!");
                return;
            }

            mainActivity.drums.isDrums = true ;
            mainActivity.drums.onCreateView(mainActivity.getLayoutInflater(), null, null);
            mainActivity.drums.onViewCreated(mainActivity.drums.constraintLayout, null);

            ConstraintLayout td = constraintLayout.findViewById(R.id.tracks_drums);

            td.addView(mainActivity.drums.constraintLayout);
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mainActivity.drums.constraintLayout);
            LinearLayout ll = constraintLayout.findViewById(R.id.tracks_ll);
            ll.setLayoutParams(layoutParams);
            mainActivity.drums.constraintLayout.setLayoutParams(layoutParams);

            constraintSet.connect(R.id.tracks_ll, ConstraintSet.RIGHT,R.id.tracks_master,ConstraintSet.RIGHT,0);
            constraintSet.applyTo(mainActivity.drums.constraintLayout);
        }
    }

    public void load (String [] files) {
        filesDir = "assets://drums";
        for (int i = 0 ; i < files.length; i ++) {
//            File file = new File("assets:///drums/" + files [i]);
            tracksAdapter.add("asset:///drums/" + files [i]);
        }
    }

    public void load (File dir) {
        Log.d(TAG, "load: loading folder " + dir.getAbsolutePath());
        filesDir = dir.getPath() ;
        File [] files = dir.listFiles();
        Log.d(TAG, "load: " + files.length + " files found");
        for (int i = 0 ; i < files.length; i ++) {
            Log.d(TAG, "load: adding file " + files[i].getAbsolutePath());
            tracksAdapter.add(files [i].getAbsolutePath());
        }
    }

    public void load (Uri filename) {
        tracksAdapter.add(filename.toString());
    }
}