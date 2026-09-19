package com.kancrutt.gigginggear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class AudioForegroundService extends Service {
    private static final String CHANNEL_ID = "audio_mic_fg";
    private static final int NOTIF_ID = 1001;
    private static final String TAG = "AudioFGService";

    @Override public void onCreate() {
        super.onCreate();
        // Bulletproof: ANY throwable here (including Errors like
        // NoSuchMethodError on old devices, or SecurityException when the
        // microphone permission state doesn't allow a mic-type FGS) used to
        // kill service creation and loop-crash on restart. Never crash here;
        // stop gracefully instead — MainActivity restarts the service when
        // the user returns (lifecycle is Activity-driven).
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Audio Engine", NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Keeps audio engine alive for microphone input");
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);
            }
            // A microphone-type FGS without RECORD_AUDIO throws
            // SecurityException at startForeground — bail out cleanly instead.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "RECORD_AUDIO not granted; not starting mic foreground service");
                stopSelf();
                return;
            }
            Notification n;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                n = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Giggin' Gear")
                    .setContentText("Audio engine active")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build();
            } else {
                // Pre-O fallback: 2-arg Builder (Context, String) does not exist.
                n = new Notification.Builder(this)
                    .setContentTitle("Giggin' Gear")
                    .setContentText("Audio engine active")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build();
            }

            startForeground(NOTIF_ID, n);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start foreground; stopping service instead of crashing", t);
            try { stopSelf(); } catch (Throwable ignore) {}
        }
    }
    // NOT_STICKY: MainActivity starts this service in onResume and stops it
    // in onPause. STICKY caused the system to auto-restart a crashing service
    // in a tight loop; with NOT_STICKY a failure stays dead until the user
    // returns and the Activity restarts it deliberately.
    @Override public int onStartCommand(Intent i, int flags, int startId){ return START_NOT_STICKY; }
    @Override public IBinder onBind(Intent i){ return null; }
    @Override public void onDestroy(){ super.onDestroy(); stopForeground(true); }
}
