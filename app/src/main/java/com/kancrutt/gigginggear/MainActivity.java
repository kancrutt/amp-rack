package com.kancrutt.gigginggear;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.lang.Math.abs;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Gravity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.net.MacAddress;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import com.kancrutt.gigginggear.metadata.ParameterMetadata;
import com.kancrutt.gigginggear.metadata.ParameterFormatter;
import android.util.ArraySet;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SubMenu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.kancrutt.gigginggear.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.kancrutt.gigginggear.tone3000.Tone3000AuthManager;
import com.kancrutt.gigginggear.tone3000.Tone3000Config;
import com.kancrutt.gigginggear.tone3000.model.ApiException;
import com.kancrutt.gigginggear.tone3000.model.ModelDto;
import com.kancrutt.gigginggear.tone3000.model.ToneDto;
import com.kancrutt.gigginggear.tone3000.ui.Tone3000EntrySheet;
import com.kancrutt.gigginggear.tone3000.ui.Tone3000ToneDetailSheet;
import com.kancrutt.gigginggear.util.LabelFormatter;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    MidiOutputPort midiOutputPort;
    MyReceiver midiReciever;
    private CompanionDeviceManager deviceManager;

    class MyReceiver extends MidiReceiver {
        MainActivity mainActivity ;
        MidiFramer midiFramer ;
        MyReceiver (MainActivity m) {
            mainActivity = m ;
            midiFramer = new MidiFramer(this);
        }

        public void logMidiMessage(byte[] data, int offset, int count) {
            String text = "Received: ";
            for (int i = 0; i < count; i++) {
                text += String.format("0x%02X, ", data[offset + i]);
            }
//            Log.i(TAG, text);
        }

        public void onSend(byte[] data, int offset,
                           int count, long timestamp) throws IOException {
            ShortMessage shortMessage = new ShortMessage(data);
            byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
            int channel = (byte) (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
//            Log.i(TAG, "onSend: recieved message on channel " + channel);
            switch (command) {
                case MidiConstants.STATUS_NOTE_OFF:
//                    noteOff(channel, data[1], data[2]);
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi note off] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    break;
                case MidiConstants.STATUS_NOTE_ON:
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi note on] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
//                    noteOn(channel, data[1], data[2]);
                    break;
                case MidiConstants.STATUS_PITCH_BEND:
                    int bend = (data[2] << 7) + data[1];
//                    pitchBend(channel, bend);
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi pitch bend] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    break;
                case MidiConstants.STATUS_PROGRAM_CHANGE:
//                    mProgram = data[1];
//                    mFreeVoices.clear();
//                    Log.d(TAG, "onSend: program change");
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
                    logMidiMessage(data, offset, count);
                    break;
                case MidiConstants.STATUS_CONTROL_CHANGE:
//                    Log.d(TAG, "onSend: control change " );
//                    Log.d(TAG, String.format ("[midi control change] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
                    logMidiMessage(data, offset, count);
                    break;
                default:
//                    Log.i(TAG, "onSend: command not understood, instructions unclear ...!");
                    logMidiMessage(data, offset, count);
                    break;
            }
        }
    }

    private static final String TAG = "Amp Rack MainActivity";

    private static final String CHANNEL_ID = "default";
    Surface surface_ = null;
    Spinner pluginDialogSortBy ;
    public static TextView sampleRateLabel ;
    public static ImageView latencyWarnLogo ;
    public static LinearLayout srLayout; ;
    public boolean headphoneWarning = true;
    public boolean experimentalBuild = false;
    static Context context;
    ArrayList <MIDIControl> midiControls ;
    MidiManager midiManager ;
    MidiDevice midiDevice ;
    String midiLastConnectedDevice = null ;
    int midiLastConnectedPort = -1 ;
    static FileOutputStream fileOutputStream = null ;
    static DataOutputStream dataOutputStream = null ;
    static MainActivity mainActivity;
    private ParameterMetadata parameterMetadata;
    BluetoothDevice deviceToPair  ;
    boolean videoRecording = false ;
    static boolean tabletMode = false ;
    Dialog midiAddDialog = null ;
    ToggleButton triggerMidiButton = null ;
    ConstraintLayout midiLayout = null ;
    EditText channelEdit = null, controlEdit = null ;

    public static String price = "$2";
    MediaPlayerDialog mediaPlayerDialog = null;
    private OrientationEventListener orientationEventListener;
    public String favPresetsDir = null ;

    static class AVBuffer {
        float [] bytes ;
        int size ;
    }
    public static LinkedBlockingQueue <AVBuffer> avBuffer = new LinkedBlockingQueue<>();
    public static LinkedBlockingQueue <OnEngineStartListener> engineStartListeners = new LinkedBlockingQueue<>();

    static int avEncoderIndex = 0 ;
    public static long presentationTimeUs = 0;
    int totalBytesRead = 0;

    String lastPresetLoadedPath = null ;
    String lastPresetLoadedUID = null ;

    ExtendedFloatingActionButton fab ;
    Button hidePanel;
    TextView midiDisplay ;
    SwitchMaterial onOff = null ;
    String exportFormat = "1";
    TextView patchName, patchNo, patchDesc ;
    ToggleButton triggerRecordToggle ;
    int deviceWidth;
    int deviceHeight;
    long totalMemory = 0;
    static boolean lowMemoryMode = false;
    static boolean darkMode = false ;
    boolean safeMode = false;
    static boolean introShown = false ;
    ToggleButton record;
    boolean triggerRecord = false ;
    public Handler handler;
    public boolean tunerEnabled = false;
    public boolean tunerMuted = false;
    public View [] tunerViews = null;

    enum RequestCode {
        TRACK_AUDIO_FILE (1001);

        RequestCode(int i) {
        }
    }  ;
    boolean triggerRecordedSomething = false ;
    boolean recording = false ;
    PopupMenu addPluginMenu;
    RecyclerView recyclerView;
    SignalChainCanvasView signalChainCanvas;
    DataAdapter dataAdapter;

    private final Map<String, float[]> savedNodePositions = new HashMap<>();

    public void storeNodePositions(Map<String, float[]> positions) {
        savedNodePositions.clear();
        if (positions != null) savedNodePositions.putAll(positions);
    }

    public Map<String, float[]> getSavedNodePositions() {
        return new HashMap<>(savedNodePositions);
    }
    AlertDialog pluginDialog;
    ImageView pluginDialogWallpaper;
    AudioManager audioManager;
    TextView tuner ;
    boolean running = false ;
    // Engine auto-starts (no power button by design): attempted once per process.
    boolean autoStartDone = false ;
    long bootFinish = 0 ;
    static boolean showIntro = false ;
    static public JSONObject pluginCategories, pluginCreators;
    public Spinner pluginDialogCategorySpinner;
    AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput;
    int defaultInputDevice = 0;
    int defaultOutputDevice = 0;
    RecyclerView.LayoutManager layoutManager;
    ConstraintLayout linearLayoutPluginDialog;
    boolean lazyLoad = true;
    static String[] sharedLibraries;
    static String[] sharedLibrariesLV2;
    PluginDialogAdapter pluginDialogAdapter;
    SharedPreferences defaultSharedPreferences = null;
    Notification notification;
    JSONObject knobsLayout;
    public static boolean proVersion = false;
    File dir;
    static File localThemeDir ;
    HashCommands hashCommands;
    JSONObject rdf;
    JSONObject ampModels;
    Slider inputVolume, outputVolume;
    SeekBar seekBarIn, seekBarOut;
    RotarySeekbar rotarySeekbarIn, rotarySeekbarOut;
    TextView displayIn, displayOut;

    ToggleButton toggleMixer;
    static int totalPlugins = 0;
    static boolean useTheme = true;
    String theme = "TubeAmp";
    String customTheme = null ;

    int primaryColor = com.google.android.material.R.color.design_default_color_primary;
    private static final int AUDIO_EFFECT_REQUEST = 0;
    private static final int READ_STORAGE_REQUEST = 1;
    private static final int WRITE_STORAGE_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 3;
    private static final int PERMISSION_REQUEST_CODE_BLUETOOTH = 3;
    final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501; // Any value

    // Firebase
    public Rack rack;
    public Tracks tracks, drums;
    public Presets presets;
    public SettingsHostFragment settingsHost;
    int nag = 0 ;
//    public MyPresets quickPatch;
    public QuickPatch quickPatch ;
    PopupMenu optionsMenu;
    String lastRecordedFileName;
    NotificationManagerCompat notificationManager;
    static VerticalMeterView inputMeter;
    static VerticalMeterView outputMeter;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
//        System.loadLibrary("opusenc");
    }

    private ActivityMainBinding binding;
    MediaPlayer mediaPlayer;
    JSONObject availablePlugins, availablePluginsLV2;
    ArrayList<String> lv2Plugins = new ArrayList<String>();
    public AudioEngineManager audioEngineManager;
    public LocalPresetManager localPresetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        /*
        savedInstanceState = null ;
        super.onCreate(null);

         */


        context = this;
        mainActivity = this;
        audioEngineManager = new AudioEngineManager(this);
        localPresetManager = new LocalPresetManager(this);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        midiLastConnectedDevice = defaultSharedPreferences.getString("last_midi", null);
        midiLastConnectedPort = defaultSharedPreferences.getInt("last_midi_port", -1);

        midiManager = (MidiManager)context.getSystemService(Context.MIDI_SERVICE);
        String mac = defaultSharedPreferences.getString("last_bt", null);
        if (mac != null) {
            Log.i(TAG, "onCreate: trying to connect to previously connected bluetooth device " + mac);
            deviceToPair = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
            if (deviceToPair != null) {
                Log.i(TAG, "onCreate: bluetooth device connected !");
                midiManager.openBluetoothDevice(deviceToPair, new MidiManager.OnDeviceOpenedListener() {
                    @Override
                    public void onDeviceOpened(MidiDevice device) {
                        Log.i(TAG, "onDeviceOpened: bluetooth device " + mac);
                    }
                }, null);
            }
        }

        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        lowMemoryMode = memoryInfo.lowMemory ;

        handler = new Handler(Looper.getMainLooper()) ;

        if (savedInstanceState != null) {
            // to remove duplicate fragments
            List<Fragment> al = getSupportFragmentManager().getFragments();
            if (al == null) {
                // code that handles no existing fragments
                Log.d(TAG, "onCreate: no existing fragments");
//                return;
            } else {
                for (Fragment frag : al) {
                    // To save any of the fragments, add this check.
                    // A tag can be added as a third parameter to the fragment when you commit it
                    if (frag == null) {
                        continue;
                    }

                    switch (frag.getTag()) {
                        case "qp":
                            quickPatch = (QuickPatch) frag;
                            break;
                        case "rack":
                            rack = (Rack) frag;
                            break;
                        case "presets":
                            presets = (Presets) frag;
                            break;
                        case "tracks":
                            tracks = (Tracks) frag;

                            if (tracks.mainActivity == null)
                                tracks.setMainActivity(this);
                            break;
                        case "drums":
                            drums = (Tracks) frag;
                            break;
                        default:
                    }
                    Log.d(TAG, "onCreate: " + frag);
//                getSupportFragmentManager().beginTransaction().remove(frag).commit();
                }
            }
        }

        Log.d(TAG, "onCreate: Welcome! " + getApplicationInfo().toString());

        File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) downloadsDir = getFilesDir();
        // Global crash catcher — writes stacktrace to file so Hery can share it without adb
        Thread.UncaughtExceptionHandler prevHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                Log.e(TAG, "CRASH on " + t.getName(), e);
                File logFile = new File(getExternalFilesDir(null), "crash_last.txt");
                java.io.FileWriter fw = new java.io.FileWriter(logFile, false);
                fw.write(sw.toString());
                fw.close();
            } catch (Exception ignore) {}
            if (prevHandler != null) prevHandler.uncaughtException(t, e);
        });

        favPresetsDir = downloadsDir.getPath() + "/" + "favoritePresets";
        File f = new File(favPresetsDir) ;
        if (! f.exists())
            f.mkdirs();

        hashCommands = new HashCommands(this);
        hashCommands.setMainActivity(this);
        hashCommands.add (this, "AudioRecordTest");
        hashCommands.add (this, "pluginsCrashTest");
        hashCommands.add (this, "resetMIDI");
        hashCommands.add (this, "printMidi");
        hashCommands.add(this, "saveActivePreset");
        hashCommands.add(this, "printActivePreset");
        hashCommands.add(this, "testLV2");
        hashCommands.add(this, "printDebugLog");
        hashCommands.add(this, "printActiveChain");
        hashCommands.add(this, "printPluginsAll");
        hashCommands.add(this, "drummer");
        hashCommands.add(this, "featured");
        hashCommands.add(this, "resetOnboard");
        hashCommands.add(this, "setAudioDevice");
        hashCommands.add (this, "cameraTest");
        hashCommands.add (this, "getLatency");

        headphoneWarning = defaultSharedPreferences.getBoolean("headphone-warning", true);

        midiControls = new ArrayList<>();
        midiReciever = new MyReceiver(this);
        MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
        Log.i(TAG, "onCreate: [midi] last connected device " + midiLastConnectedDevice);
        Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));
        boolean found = false ;
        for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
            Log.d(TAG, String.format ("[midi device] %s: %s",
                    midiDeviceInfo.getId(), midiDeviceInfo.toString()));
            Log.d(TAG, String.format ("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
            int outputPort = -1 ;
            for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfo.getPorts()) {
                Log.d(TAG, String.format ("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                    outputPort = portInfo.getPortNumber() ;
                    Log.d(TAG, String.format ("[midi port] output port: %d", outputPort));
                    if (midiLastConnectedDevice == null && midiLastConnectedPort == -1) {
//                        found = true;
                        break;
                    }

                    String mName = midiDeviceInfo.getProperties().getString("name", midiDeviceInfo.toString()) ;
                    Log.i(TAG, "onCreate: [midi] compare: " + String.format("%s -> %s [%b]", midiLastConnectedDevice, mName, midiLastConnectedDevice.equals(mName)));
                    if (midiLastConnectedDevice != null && midiLastConnectedDevice.equals(mName)) {
                        if (midiLastConnectedPort == -1)
                            midiLastConnectedPort = 0;

                        outputPort = midiLastConnectedPort ;
                        found = true;
                        Log.i(TAG, "onCreate: [midi] found last connected device " + midiLastConnectedDevice);
                        midiConnect(midiDeviceInfo, outputPort);
                        break ;
                    }
                }

                if (found) {
                    Log.i(TAG, "onCreate: [midi] found: break");
                    break;
                }
            }

//            if (!found && midiDevice == null && midiDeviceInfo != null) {
            if (midiLastConnectedDevice == null) {
                midiConnect(midiDeviceInfo, outputPort);
            }

        }

        Log.d(TAG, "onCreate: " + String.format("" +
                "%d: %d", BuildConfig.VERSION_CODE, defaultSharedPreferences.getInt("currentVersion", 0)));
//        if (BuildConfig.VERSION_CODE > defaultSharedPreferences.getInt("currentVersion", 0)) {
        // only show onboard when first installed
        showIntro = false;

        Intent intentMain = getIntent();
        int showOn = intentMain.getIntExtra("onboard", 0);

        Log.d(TAG, "onCreate: showOn: " + showOn);

        theme = defaultSharedPreferences.getString("theme", "TubeAmp");
//        customTheme = defaultSharedPreferences.getString("custom_theme", null);
        String themeOnboarded = intentMain.getStringExtra("theme");
        if (themeOnboarded != null)
            theme = themeOnboarded;
        if (showIntro && !introShown && showOn == 0 && ! lowMemoryMode) {
            Intent intent = new Intent(this, Onboard.class);
            startActivity(intent);
        }

        String[] configVersion = BuildConfig.VERSION_NAME.split("-");

        if (configVersion.length > 1 && configVersion[1].equals("experimental")) {
            experimentalBuild = true ;
        }

        Log.d(TAG, "onCreate: loading theme " + theme);
        if (theme.equals("Material") || lowMemoryMode) {
            useTheme = false;
        } else {
            /*
            if (customTheme != null) {
                theme = customTheme;
            }

             */
        }

        int nightModeFlags =
                getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                darkMode = true;
                break;
            default:
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                darkMode = false;
                break;
        }

        pluginCategories = MainActivity.loadJSONFromAsset("plugins.json");
        pluginCreators = MainActivity.loadJSONFromAsset("creator.json");
        availablePlugins = ConnectGuitar.loadJSONFromAssetFile(this, "all_plugins.json");
        availablePluginsLV2 = ConnectGuitar.loadJSONFromAssetFile(this, "lv2_plugins.json");
        ampModels = ConnectGuitar.loadJSONFromAssetFile(this, "amps.json");
         knobsLayout = ConnectGuitar.loadJSONFromAssetFile(this, "knobs.json");
         parameterMetadata = ParameterMetadata.loadFromAsset(this);

        Iterator<String> keys = availablePluginsLV2.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                String _p = availablePluginsLV2.getJSONObject(key).getString("name");
                Log.d(TAG, "onCreate: found LV2 plugin " + key + ": " + _p);
                lv2Plugins.add(_p);
            } catch (JSONException e) {
                Log.e(TAG, "onCreate: no name in plugin " + key, e);
            }
        }

        Log.d(TAG, "onCreate: [LV2 plugins]: " + availablePluginsLV2.toString());

        Log.d(TAG, "onCreate: bootstart: " + defaultSharedPreferences.getLong("bootStartz", 1L) + " bootFinish: " + defaultSharedPreferences.getLong("bootFinish", 1L));
        if (defaultSharedPreferences.getLong("bootFinish", 1L) < defaultSharedPreferences.getLong("bootStart", 0L)) {
            safeMode = true;
            Log.d(TAG, "onCreate: turned on safe mode");
        }

        defaultSharedPreferences.edit().putLong("bootStart", System.currentTimeMillis()).commit();

        // Must run AFTER installSplashScreen() so the theme has been swapped from
        // Theme.MySplash (Theme.SplashScreen, not AppCompat) to Theme.AmpRack.
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        notificationManager = NotificationManagerCompat.from(this);
        lazyLoad = defaultSharedPreferences.getBoolean("lazyLoad", true);
        Log.d(TAG, "onCreate: lazyLoad set to " + lazyLoad);
        try {
            proVersion = defaultSharedPreferences.getBoolean("pro", false);
        } catch (ClassCastException e) {
            Log.e(TAG, "onCreate: incorrect preference found!", e);
            proVersion = false;
            defaultSharedPreferences.edit().putBoolean("pro", false).apply();
        }
        Log.d(TAG, "onCreate: purchased proVersion: " + proVersion);

        boolean forceAds = defaultSharedPreferences.getString("forceads", "off") != "off";
//        Log.d(TAG, "onCreate: forceads is " + defaultSharedPreferences.getString("forceads", "off"));
        proVersion = true;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

//        AudioEngine.showProgress(null);
//        AudioEngine.showProgress(context);

         AudioEngine.create();

         AudioEngine.setLibraryPath(getApplicationInfo().nativeLibraryDir);
         AudioEngine.setLazyLoad(lazyLoad);
         try {
             JSONObject metadataJson = loadJSONFromAsset("parameter_metadata.json");
             if (metadataJson != null) {
                 AudioEngine.setParameterMetadata(metadataJson.toString());
                 Log.i(TAG, "parameter metadata installed into native engine");
             } else {
                 Log.w(TAG, "parameter metadata missing; native engine will use source defaults");
             }
         } catch (Throwable t) {
             Log.e(TAG, "parameter metadata install failed; continuing with fallback", t);
         }
         // load included plugins
        loadPlugins();

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: initializing new fragments");

            rack = new Rack();
            tracks = new Tracks(this);
            drums = new Tracks(this, true);
            drums.isDrums = true ;
            presets = new Presets();
            quickPatch = new QuickPatch();
            settingsHost = new SettingsHostFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, quickPatch, "qp")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, rack, "rack")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, presets, "presets")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, tracks, "tracks")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, drums, "drums")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, settingsHost, "settings_host")
                    .commit();
        } else {
            Log.d(TAG, "onCreate: restoring fragments from FragmentManager");
            rack = (Rack) getSupportFragmentManager().findFragmentByTag("rack");
            if (rack == null) rack = new Rack();

            tracks = (Tracks) getSupportFragmentManager().findFragmentByTag("tracks");
            if (tracks == null) tracks = new Tracks(this);

            drums = (Tracks) getSupportFragmentManager().findFragmentByTag("drums");
            if (drums == null) {
                drums = new Tracks(this, true);
                drums.isDrums = true;
            }

            presets = (Presets) getSupportFragmentManager().findFragmentByTag("presets");
            if (presets == null) presets = new Presets();

            quickPatch = (QuickPatch) getSupportFragmentManager().findFragmentByTag("qp");
            if (quickPatch == null) quickPatch = new QuickPatch();

            settingsHost = (SettingsHostFragment) getSupportFragmentManager().findFragmentByTag("settings_host");
            if (settingsHost == null) settingsHost = new SettingsHostFragment();

            // If a settings sub-screen was on the back stack, drop back to base state
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                try { getSupportFragmentManager().popBackStackImmediate(); } catch (Exception ignored) {}
            }
        }

//        LoadFragment(presets);
//        LoadFragment(rack);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        /* we do this async after getting billing client result
        if (! proVersion) {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });

            AdView mAdView = findViewById(R.id.adViewBanner);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

         */

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioDevicesInput = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        totalMemory = memInfo.totalMem;
        Log.d(TAG, "onCreate: total memory available: " + totalMemory);

        int color = 0;

        if (! lowMemoryMode) {
            color = getDominantColor(BitmapFactory.decodeResource(getResources(), R.drawable.bg));
        } else {
            color = getResources().getColor(primaryColor);
        }
//        getWindow().setStatusBarColor(color);
        color = adjustAlpha(color, .5f);
        primaryColor = color;

        NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener
                = new NavigationBarView.OnItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Reselection guard: re-tapping the active tab must not re-commit
                // show/hide transactions (same-ms duplicate trigger source).
                try {
                    if (item.getItemId() == R.id.page_rack && rack != null && rack.isAdded() && !rack.isHidden()
                            && (settingsHost == null || settingsHost.isHidden())) return true;
                } catch (Exception ignored) {}
                // Drop any open settings sub-screen before switching tabs
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    try { getSupportFragmentManager().popBackStackImmediate(); } catch (Exception ignored) {}
                }
                if (item.getItemId() != R.id.page_tuner) {
                    FrameLayout tunerOverlay = findViewById(R.id.tuner_overlay_container);
                    if (tunerOverlay != null && tunerOverlay.getVisibility() == View.VISIBLE) {
                        closeTunerOverlay(tunerOverlay);
                    }
                }
                Fragment fragment = null;
                Class<Rack> fragmentClass = null;
                switch (item.getItemId()) {
                    case R.id.page_rack:
                        fragment = rack;
                        androidx.fragment.app.FragmentTransaction ftRack = getSupportFragmentManager().beginTransaction();
                        if (rack != null) ftRack.show(rack);
                        if (quickPatch != null) ftRack.hide(quickPatch);
                        if (presets != null) ftRack.hide(presets);
                        if (tracks != null) ftRack.hide(tracks);
                        if (drums != null) ftRack.hide(drums);
                        if (settingsHost != null) ftRack.hide(settingsHost);
                        ftRack.commitNow();
                        return true;
                         /*
                        getSupportFragmentManager()
                            .beginTransaction()
                            .hide (presets)
                            .show (rack)
                            .commit();
                        return true ;

                         */

                    case R.id.page_preset:
                        fragment = presets;
                        androidx.fragment.app.FragmentTransaction ftPreset = getSupportFragmentManager().beginTransaction();
                        if (presets != null) ftPreset.show(presets);
                        if (rack != null) ftPreset.hide(rack);
                        if (tracks != null) ftPreset.hide(tracks);
                        if (quickPatch != null) ftPreset.hide(quickPatch);
                        if (drums != null) ftPreset.hide(drums);
                        if (settingsHost != null) ftPreset.hide(settingsHost);
                        ftPreset.commitNow();
                        return true;

                    case R.id.page_tuner:
                        androidx.fragment.app.FragmentTransaction ftTuner = getSupportFragmentManager().beginTransaction();
                        if (rack != null) ftTuner.show(rack);
                        if (presets != null) ftTuner.hide(presets);
                        if (quickPatch != null) ftTuner.hide(quickPatch);
                        if (tracks != null) ftTuner.hide(tracks);
                        if (drums != null) ftTuner.hide(drums);
                        if (settingsHost != null) ftTuner.hide(settingsHost);
                        ftTuner.commitNow();
                        showTunerOverlay();
                        return true;
                    case R.id.page_drums:
                        androidx.fragment.app.FragmentTransaction ftDrums = getSupportFragmentManager().beginTransaction();
                        if (drums != null) ftDrums.show(drums);
                        if (rack != null) ftDrums.hide(rack);
                        if (presets != null) ftDrums.hide(presets);
                        if (quickPatch != null) ftDrums.hide(quickPatch);
                        if (tracks != null) ftDrums.hide(tracks);
                        if (settingsHost != null) ftDrums.hide(settingsHost);
                        ftDrums.commitNow();
                        return true;
                    case R.id.page_settings:
                        androidx.fragment.app.FragmentTransaction ftSettings = getSupportFragmentManager().beginTransaction();
                        if (settingsHost != null) ftSettings.show(settingsHost);
                        if (rack != null) ftSettings.hide(rack);
                        if (presets != null) ftSettings.hide(presets);
                        if (quickPatch != null) ftSettings.hide(quickPatch);
                        if (tracks != null) ftSettings.hide(tracks);
                        if (drums != null) ftSettings.hide(drums);
                        ftSettings.commitNow();
                        return true;
                        /*
                        if (presets.loginNotice == null)
                            return LoadFragment(fragment);

                        getSupportFragmentManager()
                                .beginTransaction()
                                .hide (rack)
                                .show (presets)
                                .commit();
                        return true ;

                         */

                }
//                return LoadFragment(fragment);
                return false;
            }
        };

        NavigationBarView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (rack != null) ft.show(rack);
        if (quickPatch != null) ft.hide(quickPatch);
        if (presets != null) ft.hide(presets);
        if (tracks != null) ft.hide(tracks);
        if (drums != null) ft.hide(drums);
        if (settingsHost != null) ft.hide(settingsHost);
        ft.commit();

        bottomNavigationView.setSelectedItemId(R.id.page_rack);

        // notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    getResources().getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);
        }
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Effects Processor is running")
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MIN);


        notification = builder.build();
        dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dir == null) dir = context.getFilesDir();
        localThemeDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (localThemeDir == null) localThemeDir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        tracks.load(dir);
//        tracks.load(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES));
        Set<String> _tracksCustom = defaultSharedPreferences.getStringSet("tracks", null) ;
        if (_tracksCustom != null) {
            for (String _d:
                 _tracksCustom) {
                tracks.load(Uri.parse(_d));
            }
        }

        try {
            drums.load(getAssets().list("drums"));
        } catch (IOException e) {
            MainActivity.toast("Cannot load drum loops: " + e.getMessage());
            e.printStackTrace();
        }

        Set<String> _drumsCustom = defaultSharedPreferences.getStringSet("drums", null) ;
        if (_drumsCustom != null) {
            for (String _d:
                    _drumsCustom) {
                drums.load(Uri.parse(_d));
            }
        }

        applyWallpaper(context, getWindow(), getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
        // Kiosk mode: pro features active

        deviceWidth = getWindowManager().getDefaultDisplay().getWidth();
        deviceHeight = getWindowManager().getDefaultDisplay().getHeight();

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float smallestWidthDp = Math.min(dm.widthPixels, dm.heightPixels) / dm.density;
        tabletMode = defaultSharedPreferences.getBoolean("tablet_mode", smallestWidthDp >= 600f);
        if (tabletMode && (float) (1.0f * deviceWidth / deviceHeight) > 0.7f)
            tabletMode = true ;
        else
            tabletMode = false ;

        Log.d(TAG, "onCreate: Loading JSON");
        rdf = loadJSONFromAsset("plugins_info.json");

        bootFinish = System.currentTimeMillis();
        defaultSharedPreferences.edit().putLong("bootFinish", bootFinish).commit();
        if (safeMode) {
            toast("Safe mode was enabled because the app did not load successfully. Some features may be disabled.");
        }

        Log.d(TAG, "onCreate: boot complete, we are now live bootFinish: [" + bootFinish + "]");
        if (showIntro && ! introShown && showOn == 0) {
            finishActivity(0);
        }

//        Log.d(TAG, "onCreate: mixer state: " + );

//        Log.d(TAG, String.format ("[orientation]: %s", getWindowManager().getDefaultDisplay().getRotation()));


        if (experimentalBuild)
            alert("Experimental beta version", "For testing only. Please report issues on Github or via the app's feedback bug reporting system.");

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            proVersion = true ;
        }

        // TONE3000: handle OAuth callback if launched via deep-link (survives recreation)
        handleTone3000Callback(getIntent());

//        register_bt_callback();
//        proVersion = false;
//        defaultSharedPreferences.edit().putBoolean("pro", false).apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTone3000Callback(intent);
    }

    // ---- TONE3000 callback handling (deep-link) ----
    private void handleTone3000Callback(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        android.net.Uri data = intent.getData();
        if (data == null) return;
        String scheme = data.getScheme();
        String host = data.getHost();
        // Accept both gigginggear:// and com.kancrutt.gigginggear://
        boolean isT3k = data.toString().contains("tone3000/callback") || ("gigginggear".equals(scheme) && "tone3000".equals(host)) || ("com.kancrutt.gigginggear".equals(scheme) && "tone3000".equals(host));
        if (!isT3k) return;
        Log.i(TAG, "TONE3000 callback: " + data);
        try {
            Tone3000AuthManager auth = Tone3000AuthManager.get(this);
            Tone3000AuthManager.CallbackResult cb = auth.handleCallback(data);
            if (cb.canceled) {
                if (cb.code != null) {
                    // user signed in but canceled tone selection — still exchange to get token for future browsing
                    exchangeTone3000Code(cb.code, null);
                } else {
                    toast("TONE3000 selection canceled");
                }
                return;
            }
            if (cb.error != null) {
                toast("TONE3000 error: " + cb.error);
                return;
            }
            exchangeTone3000Code(cb.code, cb.toneId);
        } catch (ApiException e) {
            Log.e(TAG, "TONE3000 callback failed", e);
            if ("state_mismatch".equals(e.errorCode)) toast("TONE3000 security check failed — please try again");
            else if ("missing_code".equals(e.errorCode)) toast("TONE3000 callback missing code");
            else toast("TONE3000 error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "TONE3000 callback exception", e);
            toast("TONE3000 callback failed: " + e.getMessage());
        }
    }

    private void exchangeTone3000Code(String code, @Nullable String toneId) {
        // show quick progress
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Connecting to TONE3000…");
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
        new Thread(() -> {
            try {
                Tone3000AuthManager.get(this).exchangeCode(this, code);
                runOnUiThread(() -> {
                    try { pd.dismiss(); } catch (Exception ignore) {}
                    if (toneId != null && !toneId.isEmpty()) {
                        // Fetch tone + show detail sheet (model selector)
                        Tone3000ToneDetailSheet sheet = Tone3000ToneDetailSheet.newInstance(toneId);
                        sheet.show(getSupportFragmentManager(), "t3k_detail");
                    } else {
                        // No tone selected (canceled or bare auth) — stay authenticated, let user Browse again
                        toast("TONE3000 connected — tap + to browse tones");
                    }
                });
            } catch (ApiException e) {
                runOnUiThread(() -> {
                    try { pd.dismiss(); } catch (Exception ignore) {}
                    Log.e(TAG, "TONE3000 token exchange failed", e);
                    if (e.httpCode == 400) toast("TONE3000 auth expired — please try again");
                    else toast("TONE3000 auth failed: " + e.getMessage());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    try { pd.dismiss(); } catch (Exception ignore) {}
                    toast("TONE3000 auth failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Load a downloaded TONE3000 model file into the signal chain.
     * Routes NAM -> existing NAM loader, IR -> existing IR/Cab loader.
     * Creates/updates effect block, persists TONE3000 origin metadata.
     * @return true if routed successfully, false if format unsupported
     */
    public boolean loadTone3000Model(File modelFile, ModelDto model, ToneDto tone) {
        if (modelFile == null || !modelFile.exists()) {
            toast("Model file missing");
            return false;
        }
        String fmt = tone.format != null ? tone.format.toLowerCase() : "nam";
        boolean isNam = fmt.contains("nam") || fmt.contains("aida") || model.isNam();
        boolean isIr = fmt.contains("ir") || fmt.contains("wav") || model.isIr();
        // Determine target plugin: for NAM use active NAM block or auto-add; for IR use cab/IR
        try {
            // Strategy: if user has a selected block (inline editor), use that slot; else add new
            int targetSlot = -1;
            if (dataAdapter != null) {
                int sel = dataAdapter.getSelectedPosition();
                int total = AudioEngine.getActivePlugins();
                if (sel >= 0 && sel < total) {
                    // Verify selected plugin can load the requested format
                    boolean canUse = false;
                    if (isNam && canLoadNamModel(sel)) canUse = true;
                    if (isIr && canLoadIrModel(sel)) canUse = true;
                    if (canUse) targetSlot = sel;
                }
            }
            if (targetSlot < 0) {
                // Add appropriate loader block — A2 directly to RT-Neural (A1-only NAM crashes on A2 JSON)
                boolean isA2Direct = model != null && "2".equals(model.architectureVersion);
                // NOTE: loader names must match the bundled registries EXACTLY
                // (all_plugins.json / lv2_plugins.json). "RT-Neural Generic" and
                // "Convolution Reverb" do NOT exist there — the rt-neural .so is
                // registered as "AIDA-X".
                // NG (nam.so) is the only architecturally-plausible A2 loader;
                // it clean-fails via the port guard until its atom-port load
                // path is wired, then AIDA-X/NAM are tried.
                String loaderName = isIr ? "Simple IR Loader" : (isA2Direct ? "Neural Amp Modeler NG" : "Neural Amp Modeler");
                // addPluginByName never throws and silently ignores unknown names,
                // so a slot is only valid when the chain actually grew. Never fall
                // through to a stale slot — that would write the model into an
                // unrelated block.
                if (isIr) {
                    String[] irCandidates = {"Simple IR Loader", "Impulse Loader", "Neural Amp Modeler"};
                    for (String cand : irCandidates) {
                        try {
                            int before = AudioEngine.getActivePlugins();
                            addPluginByName(cand);
                            if (AudioEngine.getActivePlugins() > before) {
                                targetSlot = AudioEngine.getActivePlugins() - 1;
                                break;
                            }
                        } catch (Exception ignore) {}
                    }
                } else {
                    int before = AudioEngine.getActivePlugins();
                    addPluginByName(loaderName);
                    if (AudioEngine.getActivePlugins() > before)
                        targetSlot = AudioEngine.getActivePlugins() - 1;
                }
            }
            if (targetSlot < 0 || targetSlot >= AudioEngine.getActivePlugins()) {
                toast("No slot available for model");
                return false;
            }
            if (isNam && !canLoadNamModel(targetSlot)) {
                // loader added but not recognized — still try to set filename (native will error clearly)
                Log.w(TAG, "target slot not NAM-capable but trying anyway: " + targetSlot);
            }
            if (isIr && isNam) {
                // ambiguous — treat as NAM per spec (determine from actual API metadata, not assumption)
                Log.d(TAG, "format ambiguous nam/ir, using nam path for " + tone.title);
                isIr = false;
            }
            // Validate file exists and is non-empty before handing to native
            if (modelFile.length() == 0) { toast("Downloaded file is empty"); return false; }
            String destPath = modelFile.getAbsolutePath();
            // Log architecture for debugging A2
            try { Log.i(TAG, "TONE3000 load: tone=" + tone.title + " arch=" + model.architectureVersion + " file=" + destPath + " size=" + modelFile.length()); } catch (Exception ignore) {}
            // Quick inspect first bytes of .nam to detect A2 vs A1 (A2 has "architecture":"A2" or snapshot)
            if (model.architectureVersion != null && model.architectureVersion.equals("2")) {
                try {
                    String head = new String(java.nio.file.Files.readAllBytes(modelFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    if (head.length() > 800) head = head.substring(0, 800);
                    Log.d(TAG, "TONE3000 A2 head: " + head.replace("\n"," "));
                } catch (Exception ignore) {}
            }
            // Use existing storage pipeline: file already in externalFilesDir/TONE3000 — hand directly to native
            String err = AudioEngine.setPluginFilename(destPath, targetSlot);
            if (err != null) {
                Log.w(TAG, "TONE3000 setPluginFilename failed (first try " + AudioEngine.getActivePluginName(targetSlot) + "): " + err + " arch=" + model.architectureVersion);
                // For NAM A2, try fallback loaders: RT-Neural Generic often supports A2 when NAM doesn't
                boolean isA2 = "2".equals(model.architectureVersion);
                if (isNam && isA2) {
                    String[] fallbackLoaders = new String[]{"Neural Amp Modeler NG", "AIDA-X", "Neural Amp Modeler"};
                    for (String fb : fallbackLoaders) {
                        try {
                            String curName = AudioEngine.getActivePluginName(targetSlot);
                            if (fb.equals(curName)) continue;
                            Log.i(TAG, "TONE3000 A2 fallback: trying loader " + fb);
                            // Trial-at-end strategy: append the fallback AFTER the
                            // current chain and only touch targetSlot on success.
                            // (Delete-then-add per candidate eats innocent blocks
                            // when an add fails, because slots shift down.)
                            int before = AudioEngine.getActivePlugins();
                            addPluginByName(fb);
                            if (AudioEngine.getActivePlugins() != before + 1) {
                                Log.w(TAG, "TONE3000 A2 fallback: add failed for " + fb + ", trying next");
                                continue;
                            }
                            int trial = AudioEngine.getActivePlugins() - 1;
                            if (!canLoadNamModel(trial)) {
                                safeDeletePlugin(trial);
                                continue;
                            }
                            String err2 = AudioEngine.setPluginFilename(destPath, trial);
                            if (err2 == null) {
                                // Success: drop the old slot (trial is last, so it
                                // shifts down to trial-1 == new last), then move it
                                // into place to keep visual mapping correct.
                                if (!safeDeletePlugin(targetSlot)) {
                                    safeDeletePlugin(trial);
                                    continue;
                                }
                                int last = AudioEngine.getActivePlugins() - 1;
                                try {
                                    AudioEngine.movePlugin(last, targetSlot);
                                } catch (Throwable t) {
                                    Log.w(TAG, "movePlugin failed after fallback add", t);
                                }
                                Log.i(TAG, "TONE3000 A2 fallback succeeded with " + fb + " at slot " + targetSlot);
                                err = null;
                                break;
                            } else {
                                Log.w(TAG, "TONE3000 fallback " + fb + " failed: " + err2);
                                safeDeletePlugin(trial);
                            }
                        } catch (Exception fe) { Log.w(TAG, "fallback " + fb + " exception", fe); }
                    }
                }
                if (err != null) {
                    // Unsupported format handling
                    if (err.toLowerCase().contains("unsupported") || err.toLowerCase().contains("format") || err.toLowerCase().contains("nam")) {
                        // For A2, show specific guidance
                        if ("2".equals(model.architectureVersion)) {
                            toast("NAM A2 not supported by this loader — try updating the app or use A1 model");
                        } else {
                            toast("Unsupported format: " + fmt);
                        }
                        // clean up slot if we just added it
                        try { AudioEngine.deletePlugin(targetSlot); shiftT3kMetaOnDelete(targetSlot); } catch (Exception ignore) {}
                        if (signalChainCanvas != null) {
                            signalChainCanvas.clearT3kBadges();
                            signalChainCanvas.post(() -> {
                                signalChainCanvas.loadFromActivePlugins(true);
                                signalChainCanvas.postDelayed(() -> signalChainCanvas.reapplyT3kBadgesFromMeta(t3kBlockMeta), 120);
                            });
                        }
                        return false;
                    }
                    toast("Model load failed: " + err);
                    return false;
                }
            }
            // Persist TONE3000 origin metadata into the preset autosave path
            // Attach to the active plugin's logical preset item for next save
            try {
                // We stash metadata in SharedPreferences keyed by slot for preset serializer to pick up
                // Simpler: write a sidecar json next to the model file and teach serializer to read it
                File metaFile = new File(modelFile.getParentFile(), modelFile.getName() + ".t3k.json");
                JSONObject meta = new JSONObject();
                meta.put("tone3000ToneId", String.valueOf(tone.id));
                meta.put("tone3000ModelId", String.valueOf(model.id));
                meta.put("tone3000Creator", tone.userUsername != null ? tone.userUsername : "");
                meta.put("tone3000ToneTitle", tone.title);
                meta.put("tone3000Format", tone.format);
                meta.put("tone3000GearType", tone.gear);
                meta.put("tone3000SourceUrl", model.modelUrl);
                try (FileOutputStream fos = new FileOutputStream(metaFile)) { fos.write(meta.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                // Also keep in-memory map for this session so adapter can show T3K badge
                t3kBlockMeta.put(targetSlot, meta);
                t3kModelPath.put(targetSlot, destPath);
            } catch (Exception e) { Log.w(TAG, "failed to persist t3k meta", e); }

            // Update signal chain visual — mark as T3K origin
            final int badgeSlot = targetSlot;
            if (signalChainCanvas != null) {
                signalChainCanvas.post(() -> {
                    signalChainCanvas.loadFromActivePlugins();
                    signalChainCanvas.postDelayed(() -> {
                        try { signalChainCanvas.setT3kBadge(badgeSlot, true); } catch (Exception ignore) {}
                        signalChainCanvas.invalidate();
                    }, 120);
                });
            }
            if (dataAdapter != null) dataAdapter.notifyDataSetChanged();
            // ENGINE VERIFICATION — read back from native to confirm the model file is really
            // connected to the plugin (not just a UI state). This proves the source engine loaded it.
            try {
                String loaded = AudioEngine.getPluginLoadedFileName(targetSlot);
                boolean engineConfirmed = loaded != null && !loaded.isEmpty() && (loaded.equals(destPath) || loaded.endsWith(modelFile.getName()));
                boolean active = AudioEngine.getActivePluginEnabled(targetSlot);
                if (engineConfirmed) {
                    Log.i(TAG, "TONE3000 ENGINE VERIFIED: slot=" + targetSlot + " file=" + loaded + " active=" + active
                            + " arch=" + model.architectureVersion + " format=" + fmt);
                    toast("Loaded TONE3000: " + tone.title + " → " + model.name + (active ? "" : " (bypassed)"));
                } else {
                    // Engine did not actually attach the file — surface it instead of faking success
                    Log.e(TAG, "TONE3000 ENGINE VERIFY FAILED: expected=" + destPath + " got=" + loaded);
                    toast("Model attached but engine read-back failed — check log");
                }
            } catch (Throwable ve) {
                Log.w(TAG, "engine verification unavailable, assuming loaded", ve);
                toast("Loaded TONE3000: " + tone.title + " → " + model.name);
            }
            // Persist preset snapshot will include metadata on next save (see captureWithT3k)
            return true;
        } catch (Exception e) {
            Log.e(TAG, "loadTone3000Model failed", e);
            toast("Failed to load model: " + e.getMessage());
            return false;
        }
    }

    // In-memory T3K block metadata for current session (slot -> json)
    private final java.util.Map<Integer, JSONObject> t3kBlockMeta = new java.util.HashMap<>();
    // model path per slot for preset persistence
    private final java.util.Map<Integer, String> t3kModelPath = new java.util.HashMap<>();
    // Pending T3K from preset load (populated in loadSchemaV1Preset, consumed in loadPreset post)
    private final java.util.Map<Integer, JSONObject> pendingT3kLoad = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> pendingT3kFile = new java.util.HashMap<>();

    @Nullable
    JSONObject getTone3000MetaForSlot(int slot) {
        JSONObject v = t3kBlockMeta.get(slot);
        if (v != null) return v;
        // fallback: try to read sidecar .t3k.json next to model file if present
        String p = t3kModelPath.get(slot);
        if (p != null) {
            try {
                java.io.File meta = new java.io.File(new java.io.File(p).getParentFile(), new java.io.File(p).getName() + ".t3k.json");
                if (meta.exists()) {
                    String s = new String(java.nio.file.Files.readAllBytes(meta.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    return new JSONObject(s);
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    @Nullable
    String getTone3000ModelPathForSlot(int slot) { return t3kModelPath.get(slot); }

    /** Called when preset load finds a missing TONE3000 file. */
    void handleMissingTone3000Model(LocalPresetManager.PluginItem item, int slot) {
        String toneId = item.tone3000ToneId;
        String title = item.tone3000ToneTitle != null ? item.tone3000ToneTitle : (item.name != null ? item.name : "TONE3000 model");
        new AlertDialog.Builder(this)
            .setTitle("TONE3000 model missing")
            .setMessage("TONE3000 model is not downloaded on this device.\n\n" + title + (item.tone3000Creator != null ? " — @" + item.tone3000Creator : "") + "\n\nDownload again or choose a replacement.")
            .setPositiveButton("Download again", (d, w) -> {
                if (toneId != null && !toneId.isEmpty()) {
                    // Re-auth if needed, then open detail sheet to re-download
                    Tone3000ToneDetailSheet sheet = Tone3000ToneDetailSheet.newInstance(toneId);
                    sheet.show(getSupportFragmentManager(), "t3k_detail_rereload");
                } else {
                    // No tone id — just open browse
                    Tone3000EntrySheet.newInstance().show(getSupportFragmentManager(), "t3k_entry");
                }
            })
            .setNeutralButton("Choose replacement", (d, w) -> {
                Tone3000EntrySheet.newInstance().show(getSupportFragmentManager(), "t3k_entry");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    boolean isTone3000Slot(int slot) {
        return getTone3000MetaForSlot(slot) != null;
    }
    java.util.Set<Integer> getTone3000SlotIds() { return new java.util.HashSet<>(t3kBlockMeta.keySet()); }

    /** Delete plugin and keep T3K maps in sync (shifts indices after deletion). */
    /** Shift T3K metadata maps down by one for every slot > deletedSlot, and
     *  remove the entry for the deleted slot. Called after AudioEngine.deletePlugin. */
    private void shiftT3kMetaOnDelete(int deletedSlot) {
        java.util.Map<Integer, JSONObject> newMeta = new java.util.HashMap<>();
        java.util.Map<Integer, String> newPath = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, JSONObject> e : t3kBlockMeta.entrySet()) {
            int k = e.getKey();
            if (k == deletedSlot) continue;
            int nk = k > deletedSlot ? k - 1 : k;
            newMeta.put(nk, e.getValue());
        }
        for (java.util.Map.Entry<Integer, String> e : t3kModelPath.entrySet()) {
            int k = e.getKey();
            if (k == deletedSlot) continue;
            int nk = k > deletedSlot ? k - 1 : k;
            newPath.put(nk, e.getValue());
        }
        t3kBlockMeta.clear(); t3kBlockMeta.putAll(newMeta);
        t3kModelPath.clear(); t3kModelPath.putAll(newPath);
    }

    /** Shift T3K metadata maps after a movePlugin(from, to) call.
     *  movePlugin removes the plugin at `from` and re-inserts it at `to`, shifting
     *  every slot in [min, max] (exclusive of `from`) by one. */
    private void shiftT3kMetaOnMove(int from, int to) {
        if (from == to || from < 0 || to < 0) return;
        int lo = Math.min(from, to), hi = Math.max(from, to);
        java.util.Map<Integer, JSONObject> newMeta = new java.util.HashMap<>();
        java.util.Map<Integer, String> newPath = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, JSONObject> e : t3kBlockMeta.entrySet()) {
            int k = e.getKey();
            if (k == from) { newMeta.put(to, e.getValue()); continue; }
            if (k >= lo && k <= hi) k += (to > from ? -1 : 1);
            newMeta.put(k, e.getValue());
        }
        for (java.util.Map.Entry<Integer, String> e : t3kModelPath.entrySet()) {
            int k = e.getKey();
            if (k == from) { newPath.put(to, e.getValue()); continue; }
            if (k >= lo && k <= hi) k += (to > from ? -1 : 1);
            newPath.put(k, e.getValue());
        }
        t3kBlockMeta.clear(); t3kBlockMeta.putAll(newMeta);
        t3kModelPath.clear(); t3kModelPath.putAll(newPath);
    }

    /** Delete a plugin slot and keep T3K metadata + canvas badges in sync.
     *  Centralized delete entry point — ALL delete paths MUST go through here so
     *  t3kBlockMeta stays aligned with engine slots and canvas badges re-render. */
    void deletePluginAndSyncTone3000(int slot) {
        int total = AudioEngine.getActivePlugins();
        if (slot < 0 || slot >= total) {
            Log.w(TAG, "deletePluginAndSyncTone3000: stale slot " + slot + " of " + total + ", skipping");
            return;
        }
        try { AudioEngine.deletePlugin(slot); } catch (Exception e) { Log.e(TAG, "deletePlugin failed", e); return; }
        shiftT3kMetaOnDelete(slot);
        if (signalChainCanvas != null) {
            signalChainCanvas.clearT3kBadges();
            signalChainCanvas.post(() -> {
                // force=true: ensure slotToPluginId map is rebuilt — the single-flight
                // dedup would skip this if another caller triggered the same engine state,
                // leaving stale nodes + stale badge map on screen.
                signalChainCanvas.loadFromActivePlugins(true);
                signalChainCanvas.postDelayed(() -> {
                    signalChainCanvas.reapplyT3kBadgesFromMeta(t3kBlockMeta);
                }, 120);
            });
        }
    }

    void showMediaPlayerDialog() {
        if (lastRecordedFileName == null) {
            Log.e(TAG, "showMediaPlayerDialog: no last recorded audio");
            return;
        }

        if (mediaPlayerDialog == null)
            mediaPlayerDialog = new MediaPlayerDialog(this, mediaPlayer);
        mediaPlayerDialog.dialog.show();
        if (mediaPlayerDialog.dialog != null)
            return;
        Log.d(TAG, "showMediaPlayerDialog: " + lastRecordedFileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.media_player_dialog, null);
        SurfaceView surface = constraintLayout.findViewById(R.id.video_player_dialog);

        LinearLayout surfaceLayout = constraintLayout.findViewById(R.id.surface_ll);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (lastRecordedFileName.endsWith(".mp4")) {
                    int w = mp.getVideoWidth(), h = mp.getVideoHeight();
                    surfaceLayout.setVisibility(VISIBLE);
                    surface.getHolder().setFixedSize(w, h);
                    Log.d(TAG, "onPrepared: ends with mp4");
                } else {
                    surfaceLayout.setVisibility(GONE);
                }
            }
        });

        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mediaPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mediaPlayer.setDisplay(null);
                mediaPlayer.stop();
            }
        });

        ToggleButton toggleButton = constraintLayout.findViewById(R.id.media_play);
        surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButton.setChecked(false);
            }
        });
        Button openFolder = constraintLayout.findViewById(R.id.open_folder);
        openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(dir.toString());
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(uri, "*/*");
                startActivity(intent);

            }
        });
        TextView textView = constraintLayout.findViewById(R.id.media_filename);
        File file = new File(lastRecordedFileName);
        tracks.tracksAdapter.add(lastRecordedFileName);
        textView.setText(file.getName());
        toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Uri uri = Uri.parse(lastRecordedFileName);
                    try {
                        if (lastRecordedFileName.endsWith(".mp4")) {
                            surfaceLayout.setVisibility(VISIBLE);
                            Log.d(TAG, "onCheckedChanged: ends with mp4");
                        } else {
                            surfaceLayout.setVisibility(GONE);
                            Log.d(TAG, "onCheckedChanged: no end");
                        }

                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(getApplicationContext(), uri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("Cannot load media file: " + e.getMessage());
                        return;
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        MainActivity.toast(e.getMessage());
                        return;
                    }

                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_pause_24);
                    mediaPlayer.start();
                } else {
                    mediaPlayer.pause();
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                    surface.setVisibility(GONE);
                }
            }
        });

        SeekBar seekBar = constraintLayout.findViewById(R.id.media_seekbar);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                seekBar.setProgress(0);
                surface.setVisibility(GONE);
            }
        });

        Button share = constraintLayout.findViewById(R.id.share_file);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareFile(file);
                // this is pretty awesome!
                // update 24-6-2022 doesnt work :(
                /*
                MediaScannerConnection.scanFile(context,
                        new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                                intentShareFile.setType("audio/*");
                                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);

                                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                                        "Sharing Audio File...");
                                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing Audio File...");

                                intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));
                            }
                        });

                 */
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar _seekBar) {
                mediaPlayer.seekTo(mediaPlayer.getDuration() * (_seekBar.getProgress() / 100));

            }
        });

        // Disabling this because this caused a crash when stopping recording ... I think
        // very hard to debug ... VERY !!!
        /*
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    seekBar.setProgress(100 * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                    Log.d(TAG, "run: " + mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                }
            }
        }, 0, 1000);

         */

        builder.setView(constraintLayout)
                .setPositiveButton("Close", null);

        AlertDialog dialog = builder.create();

        Button deleteFile = constraintLayout.findViewById(R.id.delete_file);
        deleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure you want to delete this file?")
                        .setTitle("Delete " + lastRecordedFileName)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface _dialog, int id) {
                                file.delete();
                                if (file.exists()) {
                                    toast("File could not be deleted");
                                } else {
                                    toast("File deleted");
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null);
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mediaPlayer.stop();
//                timer.cancel();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mediaPlayer.stop();
//                timer.cancel();
            }
        });
        dialog.show();

    }

    /**
     * A native method that is implemented by the 'amprack' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    void setupRack() {
        onOff = findViewById(R.id.onoff);
        if (onOff != null) {
            onOff.setOnCheckedChangeListener((compoundButton, b) -> toggleEffect(!b));
        }

        pluginDialog = createPluginDialog();

        if (linearLayoutPluginDialog != null && linearLayoutPluginDialog.getChildCount() > 2) {
            RecyclerView recyclerView1 = (RecyclerView) linearLayoutPluginDialog.getChildAt(2);
            if (recyclerView1 != null) {
                recyclerView1.setLayoutManager(new LinearLayoutManager(this));
                pluginDialogAdapter = new PluginDialogAdapter();
                pluginDialogAdapter.setMainActivity(context, this);
                recyclerView1.setAdapter(pluginDialogAdapter);
            }
        }

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) {
            addPluginMenu = new PopupMenu(context, fab);
        }

        record = findViewById(R.id.record_button);
        if (record != null) {
            record.setOnCheckedChangeListener((compoundButton, b) -> {
                Log.d(TAG, "onCheckedChanged: record pressed");
                if (onOff != null && onOff.isChecked()) {
                    Log.d(TAG, "onCheckedChanged: onOff is checked");
                    toast("Cannot start or stop recording while playing");
                    record.setChecked(!b);
                    return;
                }

                if (b) {
                    if (!isStoragePermissionGranted()) {
                        requestWriteStoragePermission();
                    } else {
                        AudioEngine.setRecordingActive(b);
                    }
                }
            });
        }

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(this, "assets/blacklist.json");
        Log.d(TAG, String.format ("blacklist: %s", blacklist != null ? blacklist.toString() : "empty"));

        int libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");
        if (addPluginMenu != null) {
            for (int i = 0; i < libraries; i++) {
                SubMenu subMenu = addPluginMenu.getMenu().addSubMenu(AudioEngine.getLibraryName(i));
                for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                    String name = AudioEngine.getPluginName(i, plugin);
                    MenuItem menuItem = subMenu.add(name);
                    int finalI = i;
                    int finalPlugin = plugin;
                    int pluginID = finalI * 100 + finalPlugin ;
                    if (blacklist != null && blacklist.has(String.valueOf(pluginID)))
                        continue;

                    if (pluginDialogAdapter != null)
                        pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name);

                    menuItem.setOnMenuItemClickListener(item -> {
                        int ret = AudioEngine.addPlugin(finalI, finalPlugin);
                        if (dataAdapter != null) dataAdapter.addItem(finalI * 100 + finalPlugin, ret);
                        return false;
                    });
                }
            }
        }

        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPluginPickerDialog();
                }
            });
        }

        MaterialButton settings = findViewById(R.id.settings);
        if (settings != null) {
            settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    // Get the layout inflater
                    LayoutInflater inflater = getLayoutInflater();

                    // Inflate and set the layout for the dialog
                    // Pass null as the parent view because its going in the dialog layout
                    LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.audio_devices_selector, null);
                    builder.setView(linearLayout)
                            // Add action buttons
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // sign in the user ...
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });

                    int i = 0;
    //                HashMap<CharSequence, Integer> inputs = new HashMap<>();
    //                HashMap <CharSequence, Integer> outputs = new HashMap<>();
                    ArrayList<String> input_s = new ArrayList<>();
                    ArrayList<String> output_s = new ArrayList<>();

                    for (i = 0; i < audioDevicesInput.length; i++) {
                        String name = typeToString(audioDevicesInput[i].getType());
    //                    inputs.put(name, audioDevicesInput [i].getId()) ;
                        input_s.add(name);
                    }

                    for (i = 0; i < audioDevicesOutput.length; i++) {
                        String name = typeToString(audioDevicesOutput[i].getType());
    //                    outputs.put(name, audioDevicesOutput [i].getId()) ;
                        output_s.add(name);
                    }

                    ArrayAdapter input_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, input_s);
                    input_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    ArrayAdapter output_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, output_s);
                    output_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    Spinner in = (Spinner) linearLayout.getChildAt(1);
                    Spinner out = (Spinner) linearLayout.getChildAt(3);
                    in.setAdapter(input_a);
                    out.setAdapter(output_a);

                    in.setSelection(defaultInputDevice);
                    out.setSelection(defaultOutputDevice);

                    in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setRecordingDeviceId(audioDevicesInput[i].getId());
                        defaultInputDevice = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                out.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setPlaybackDeviceId(audioDevicesOutput[i].getId());
                        defaultInputDevice = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                builder.show();
            }
        });
        } // Closing brace for if (settings != null) {

        // RecyclerView initialization is handled inside Rack.java onViewCreated()
        dataAdapter = new DataAdapter();
        dataAdapter.mainActivity = this;

        // add sample item to recylcer view here
        dataAdapter.setColor(primaryColor);
        /*
        dataAdapter.addItem(0, 1);
        dataAdapter.addItem(1, 2);
        dataAdapter.notifyDataSetChanged();

         */

        /*
        FloatingActionButton debugButton = findViewById(R.id.debug);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioEngine.debugInfo();
            }
        });

         */


    }

    @Override
    protected void onStart() {
        super.onStart();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "onStart: " + String.format(
                "bootFinish: %d", bootFinish
        ));

    }

    @Override
    protected void onResume() {
        super.onResume();
        applyWallpaper(context, getWindow(), getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
//        recreate();
        Log.d(TAG, "lifecycle: resumed");
//        AudioEngine.create(); // originally was here
//        loadPlugins();

        // our app got rejected because we use MANAGE STORAGE permission.
        // so we switch to the new API
//        File dir = Environment.getExternalStorageDirectory();
        dir = context.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC);

        if (dir == null || !dir.mkdirs()) {
            Log.e(TAG, "Directory not created: " + dir.toString());
        } else {
            Log.d(TAG, "onResume: default directory set as " + dir.toString());
        }

        String path = dir.getAbsolutePath();

        AudioEngine.setExternalStoragePath(path);
        File defaultDir = dir;
        if (!defaultDir.exists()) {
            try {
                if (!defaultDir.mkdir())
                    Log.wtf(TAG, "Unable to create directory!");
            } catch (Exception e) {
                Log.w(TAG, "UNable to create directory: " + e.getMessage());
            }
        }

//        loadActivePreset();
        // when savedInstanceState is not null
        // the problem with this is that when we start at first,
        // this will be empty since firestore returns data async
        /*
        if (quickPatch.myPresetsAdapter.allPresets.isEmpty() && savedState != null) {
            Log.d(TAG, "onViewCreated: loading quick patch manually: " + savedState);
            quickPatch.load();
        }

         */

        // Auto-start the audio engine: no power button by design. The grant
        // callback (onRequestPermissionsResult) starts it when permission
        // arrives later; deny leaves the engine off for this session.
        if (!autoStartDone) {
            autoStartDone = true;
            try {
                if (!running) {
                    if (isRecordPermissionGranted()) {
                        Log.d(TAG, "onResume: auto-starting audio engine");
                        toggleEffect(false);
                    } else {
                        Log.d(TAG, "onResume: requesting record permission for auto-start");
                        requestRecordPermission();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "onResume: auto-start failed", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "lifecycle: paused");
        saveActivePreset();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "lifecycle: stopped");
        saveActivePreset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "lifecycle: destroyed");
        stopEffect();
        saveActivePreset();
        AudioEngine.delete();
    }

    public void toggleEffect(boolean isPlaying) {
        if (isPlaying) {
            /*
            if (record.isChecked()) {
                lastRecordedFileName = AudioEngine.getRecordingFileName();
                showMediaPlayerDialog();
            }

             */

            if (record.isChecked())
                record.setChecked(false);
            if (rack.toggleVideo.isChecked())
                rack.toggleVideo.setChecked(false);

            stopEffect();
            notificationManager.cancelAll();
            running = false ;
            mainActivity.sampleRateLabel.setText(null);
            srLayout.setVisibility(GONE);
            mainActivity.latencyWarnLogo.setVisibility(GONE);
        } else {
            if (! isHeadphonesPlugged() && headphoneWarning) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("No headphones or audio interface detected: you may hear feedback if you run the app on device speakers. Do you wish to continue?")
                        .setPositiveButton("Start audio", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                headphoneWarning = false;
                                toggleEffect(isPlaying);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onOff.setChecked(false);
                            }
                        })
                        .setNeutralButton("Do not show again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position of the selected item.
                                defaultSharedPreferences.edit().putBoolean("headphone-warning", false).apply();
                                headphoneWarning = false;
                                toggleEffect(isPlaying);
                            }
                        })
                        .setTitle("Feedback Noise Warning") ;
                builder.create().show();
                return;
            }
            // apply settings
            applyPreferencesDevices();
            applyPreferencesExport();
            startEffect();
            notificationManager.notify(0, notification);
        }
    }

    private void startEffect() {
        Log.d(TAG, "Attempting to start");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!isRecordPermissionGranted()) {
            requestRecordPermission();
            return;
        }

        running = AudioEngine.setEffectOn(true);
    }

    private void stopEffect() {
        if (! running) return;
        Log.d(TAG, "Playing, attempting to stop, state: " + running);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AudioEngine.setEffectOn(false);
        if (bootFinish > 0 && !AudioEngine.wasLowLatency() && defaultSharedPreferences.getBoolean("warnLowLatency", true)) {
            Log.d(TAG, "stopEffect() called: Low Latency Warning");
            toast(getResources().getString(R.string.lowLatencyWarning));
        }

        inputMeter.setProgress(0);
        outputMeter.setProgress(0);
        if(triggerRecordedSomething) {
            showMediaPlayerDialog();
            triggerRecordToggle.setChecked(false);
            triggerRecordedSomething = false;
//            outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        }

        if (record.isChecked())
            record.setChecked(false);

        nag ++ ;
    }

    private boolean isRecordPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    boolean isStoragePermissionGranted() {
        return true; /*

                (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) ;/* &&
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_GRANTED) ; */

    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                READ_STORAGE_REQUEST);
    }

    void requestWriteStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_STORAGE_REQUEST);

        /*
        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);

         */

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
        if (resultCode == RESULT_OK && requestCode == APP_STORAGE_ACCESS_REQUEST_CODE) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted. Now resume your workflow.
            } else {
                Toast.makeText(getApplicationContext(),
                                "Storage permission denied. Recording and playing features won't work",
                                Toast.LENGTH_LONG)
                        .show();

            }

        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (resultCode == RESULT_OK && requestCode == 100) {
            Uri selectedImage = data.getData();
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
            Log.d(TAG, "onActivityResult: " + selectedImage.getPath());

            String contents = null ;
            try
            {
                InputStream in =  mainActivity.getContentResolver().openInputStream(selectedImage) ;
                contents = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));

//                contents = IOUtils.toString(in, StandardCharsets.UTF_8);
                System.out.println(contents);
            } catch (IOException e) {
                toast(e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
                return;
            }

            JSONObject jsonObject = null ;
            try {
                jsonObject = new JSONObject(contents);
            } catch (JSONException e) {
                alert("Cannot load file", e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
            }

            if (jsonObject != null) {
                Log.d(TAG, "onActivityResult: adding collection json " + contents);
                String name = selectedImage.getLastPathSegment();
                if (name.contains(":"))
                    name = name.substring(name.lastIndexOf(":") + 1);
                try {
                    sharedPreferences.edit().putString(name, jsonObject.toString(4)).commit();
                } catch (JSONException e) {
                    MainActivity.alert("Cannot load collection", e.getMessage());
                    Log.e(TAG, "onActivityResult: ", e);
                }
                Set<String> _vals = sharedPreferences.getStringSet("collections", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(name);
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                sharedPreferences.edit().putStringSet("collections", vals).commit();
                /*
                MainActivity.alert("Loaded collection", String.format(
                        "%s was loaded successfully. Restart the app and select the collection from the quick presets drop down menu."
                ));

                 */

                List<String> labels = new ArrayList<String>();
                labels.add("Factory Presets");
                if (vals != null) {
                    Log.d(TAG, "onViewCreated: adding collections " + vals);
                    for (String v: vals) {
                        labels.add(v);
                    }

                    labels.add ("Load from file") ;
                    labels.add ("More presets online") ;
                }

                ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, labels);
                quickPatch.quickSpinner.setAdapter(categoriesDataAdapter);
                MainActivity.alert("Preset Collection Imported", "You can load presets from the collection from Quick Presets -> Collections");
            }

            Log.d(TAG, "onActivityResult: setting wallpaper: " + selectedImage.toString());
        }

        if (resultCode == RESULT_OK && (requestCode == 1001 || requestCode == 1002)) {
            Uri selectedImage = data.getData();
            Log.d(TAG, "onActivityResult: trying to add " + selectedImage.toString());
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;

            if (requestCode == 1001) {
                Set<String> _vals = sharedPreferences.getStringSet("tracks", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(selectedImage.toString());
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                tracks.load(selectedImage);
                sharedPreferences.edit().putStringSet("tracks", vals).commit();
            }
            else {
                Set<String> _vals = sharedPreferences.getStringSet("drums", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(selectedImage.toString());
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                drums.load(selectedImage);
                sharedPreferences.edit().putStringSet("drums", vals).commit();
            }
        }

        if (resultCode == RESULT_OK && requestCode > 4999 && requestCode < 5020) {
            /* todo:
            1. Check if file is audio OR better plugin name
            2. If it is, do below
            3. If it is not, send filename to NAM plugin
            4. maybe a switch here, if NAM do this, if Looper do that

             */

            int plugin = requestCode - 5000 ;
            Uri returnUri = data.getData();

            if (returnUri != null) {
                String mimeType = getContentResolver().getType(returnUri);
                Log.d(TAG, String.format ("[mimetype]: %s", mimeType));
                if (!mimeType.startsWith("audio")) {
                    DocumentFile file = DocumentFile.fromSingleUri(mainActivity, returnUri);
                    Log.d(TAG, String.format ("ayyo filename: %s", file.getName()));
                    String path = file.getName();
//                    String path = returnUri.getPath();
                    if (path == null)
                        return;
                    String ext = path.substring(path.toString().lastIndexOf('.')+1) ;
                    Log.d(TAG, "onActivityResult: got mime type " + mimeType + ": " + path + " (" + ext + ")");
                    String dir = context.getExternalFilesDir(
                            Environment.DIRECTORY_DOWNLOADS) + "/" + AudioEngine.getActivePluginName(plugin);
                    // Legacy spinner holder no longer exists (hidden RecyclerView has no
                    // attached holders) — load straight into the engine instead.
                    switch (ext.toLowerCase()) {
                        case "nam":
                        default: // aye
                            Log.d(TAG, String.format("setFileName: %s", path));
                            try {
                                String s = getFileContent(returnUri);
                                String basename = path.substring(path.lastIndexOf(":") + 1);
                                basename = basename.substring(basename.lastIndexOf("/") + 1);
                                Log.d(TAG, String.format("[basename]: %s", basename));
                                String dest = dir + "/" + basename;
                                File fDir = new File (dir) ;
                                if (! fDir.exists())
                                    if (!fDir.mkdirs())
                                        alert("Cannot create directory", "Error loading model: " + dir);

                                writeFile(dest, s);
                                // Native setPluginFilename dereferences lv2Descriptor
                                // without null-check -> SIGSEGV on non-file-port plugins.
                                if (!canLoadNamModel(plugin)) {
                                    toast("Only AMP/CAB blocks can load .nam models");
                                    return;
                                }
                                // Correct engine API: Plugin::setFileName() connects port 99
                                // (size) / port 100 (path). Arg order: filename FIRST.
                                String err = AudioEngine.setPluginFilename(dest, plugin);
                                if (err != null) {
                                    toast("Model load failed: " + err);
                                    Log.w(TAG, "[load model] SAF pick FAILED -> plugin " + plugin + " err: " + err);
                                } else {
                                    Log.i(TAG, "[load model] SAF pick -> plugin " + plugin + " file " + dest);
                                    toast("Loaded model: " + basename);
                                    if (signalChainCanvas != null) signalChainCanvas.invalidate();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "model load failed", e);
                                toast("Failed to load model: " + e.getMessage());
                            }
                            return;
                        case "zip":
                            try {
                                unzipNAMModel(dir, returnUri);
                                File zdir = new File(dir);
                                File[] models = zdir.listFiles((d, n) -> n.toLowerCase().endsWith(".nam"));
                                boolean canZ = canLoadNamModel(plugin);
                                if (models != null && models.length > 0 && canZ) {
                                    java.util.Arrays.sort(models);
                                    String picked = models[models.length - 1].getAbsolutePath();
                                    String err2 = AudioEngine.setPluginFilename(picked, plugin);
                                    if (err2 != null) {
                                        toast("Model load failed: " + err2);
                                    } else {
                                        toast("Loaded model: " + models[models.length - 1].getName());
                                    }
                                } else {
                                    toast("No .nam file found inside zip");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "zip model load failed", e);
                                toast("Failed to unpack model");
                            }
                            return;
                    }
                }
            }

            AudioDecoder audioDecoder = new AudioDecoder(this);
            MediaCodecList supported = new MediaCodecList(MediaCodecList.ALL_CODECS);
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

//                if (!codecInfo.isEncoder()) {
//                    continue;
//                }

                String[] types = codecInfo.getSupportedTypes();
                String feature = "decoder" ;
                if (codecInfo.isEncoder())
                    feature = "encoder" ;
                if (codecInfo.isHardwareAccelerated())
                    feature += " hwaccel";
                String typ = "" ;
                for (String s: types)
                    typ += s + " ";
                Log.d(TAG, String.format ("found supported codec: %s [%s]", typ, feature));
            }

            try {
                int samplerate = AudioEngine.getSampleRate() ;
                if (samplerate < 44100 /*aaaaaaaarghhh*/)
                    samplerate = 48000 ;

                Uri audioFile = data.getData();
                getContentResolver().takePersistableUriPermission(audioFile, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;

                Log.d(TAG, String.format ("[audiofile]: %s", audioFile));
                float [] samples = audioDecoder.decode(audioFile, null, samplerate);
                if (samples == null)
                    return;
                AudioEngine.setPluginBuffer(samples, plugin);
                Log.d(TAG, String.format ("[decoder]: %d", samples.length));
                DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
                if (holder != null) {
                    holder.audioFile = audioFile.toString();
                } else if (dataAdapter != null) {
                    dataAdapter.setAudioFiles(plugin, audioFile.toString());
                }
            } catch (IOException e) {
                toast(e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
            }
        }

        if (resultCode == RESULT_OK && requestCode > 5049 && requestCode < 5070) {
            int plugin = requestCode - 5050 ;
            Uri returnUri = data.getData();
            if (returnUri != null) {
                try {
                    AudioDecoder audioDecoder = new AudioDecoder(this);
                    int samplerate = AudioEngine.getSampleRate() ;
                    if (samplerate < 44100)
                        samplerate = 48000 ;
                    getContentResolver().takePersistableUriPermission(returnUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
                    Log.d(TAG, String.format ("[audiofile R]: %s", returnUri));
                    float [] samplesR = audioDecoder.decode(returnUri, null, samplerate);
                    if (samplesR == null)
                        return;
                    DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
                    if (holder != null) holder.audioFileR = returnUri.toString();
                    if (holder != null && holder.audioFile != null) {
                        float [] samplesL = audioDecoder.decode(Uri.parse(holder.audioFile), null, samplerate);
                        if (samplesL != null) {
                            AudioEngine.setPluginBufferStereo(samplesL, samplesR, plugin);
                            Log.d(TAG, String.format ("[decoder stereo]: L=%d R=%d", samplesL.length, samplesR.length));
                        }
                    } else {
                        AudioEngine.setPluginBuffer(samplesR, plugin);
                    }
                } catch (IOException e) {
                    toast(e.getMessage());
                    Log.e(TAG, "onActivityResult: ", e);
                }
            }
        }

        if (resultCode == RESULT_OK && requestCode > 9999 && requestCode < 100000) {
            Uri returnUri = data.getData();
            int parse = requestCode - 10000 ;
            int plugin = parse / 100 ;
            int control = parse - (plugin * 100) ;
            DocumentFile file = DocumentFile.fromSingleUri(mainActivity, returnUri);
            Log.d(TAG, String.format ("ayyo filename: %s [%s]", file.getName(), file.getUri()));
            Log.d(TAG, String.format ("[load atom]: %d %d %d", requestCode, plugin, control));
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
            if (returnUri != null) {
                String dir = context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + AudioEngine.getActivePluginName(plugin);

//                String basename = file.getName() ; //returnUri.getLastPathSegment();
//                basename = basename.substring(basename.lastIndexOf(":") + 1);
//                Log.d(TAG, String.format("[basename]: %s", basename));
                String dest = dir + "/" + file.getName();
                File fDir = new File (dir) ;
                if (! fDir.exists()) {
                    if (!fDir.mkdirs()) {
                        alert("Cannot create directory", "Error loading model: " + dir);
                        return;
                    }
                }

                String path = file.getName();
                String ext = path.substring(path.toString().lastIndexOf('.')+1) ;

                Log.d(TAG, String.format ("extension: %s", ext));
                if (ext.equalsIgnoreCase("zip")) {
                    unzipNAMModel(dir, returnUri);
                    Spinner spinner = dataAdapter.holders.get(plugin).atomSpinners.get(control) ;
                    if (spinner == null) {
                        Log.d(TAG, "onActivityResult: spinner is null! why???");
                    } else
                        setSpinnerFromDir(dataAdapter.holders.get(plugin).atomSpinners.get(control), dir, null);
                    return;
                }

                try {
//                    copy (new File(file.getUri().getPath()), new File(dest));
                    copyFile(returnUri, Uri.parse("file://" + dest));
                } catch (IOException e) {
                    alert("Error loading file", e.getMessage());
                    Log.e(TAG, "onActivityResult: ", e);
                    return;
                }

                Log.d(TAG, String.format ("[copy file]: %s -> %s", returnUri.getPath(), dest));
                Log.d(TAG, String.format ("[load atom]: got filename %s", dest));
                int selection = setSpinnerFromDir(dataAdapter.holders.get(plugin).atomSpinners.get(control), dir, file.getName());
                dataAdapter.holders.get(plugin).atomSpinners.get(control).setSelection(selection);
                AudioEngine.setAtomPort(plugin, control, dest);
            }

            return;
        }

        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            Log.i(TAG, "onActivityResult: bluetooth device found");
            if (resultCode == Activity.RESULT_OK && data != null) {
                ScanResult scanResult = null ;
                if (data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) instanceof ScanResult) {
                    scanResult = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                    deviceToPair = scanResult.getDevice();
                } else if (data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) instanceof BluetoothDevice) {
                    deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) ;
                } else {
                    Toast.makeText(context, "Unknown device type selected", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (deviceToPair != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        Log.i(TAG, "onActivityResult: no bluetooth permission");
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                PERMISSION_REQUEST_CODE_BLUETOOTH);
                        Toast.makeText(mainActivity, "Missing Bluetooth permission", Toast.LENGTH_SHORT).show();
                        return;
                    }

//                    deviceToPair.createBond();
//                    deviceToPair.connectGatt(mainActivity, true, new BluetoothGattCallback() {
//                        @Override
//                        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
//                            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
//                        }
//                    });
                    Log.i(TAG, "onActivityResult: bluetooth device connected");
                    // ... Continue interacting with the paired device.
                    defaultSharedPreferences.edit().putString("last_bt", deviceToPair.getAddress()).apply();
                    midiManager.openBluetoothDevice(deviceToPair, new MidiManager.OnDeviceOpenedListener() {
                        @Override
                        public void onDeviceOpened(MidiDevice device) {
                            midiDevice = device ;
                            midiLastConnectedDevice = device.getInfo().getProperties().getString("name", device.getInfo().toString());
                            MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
                            Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));
                            for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
                                Log.d(TAG, String.format("[midi device] %s: %s",
                                        midiDeviceInfo.getId(), midiDeviceInfo.toString()));
                                Log.d(TAG, String.format("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
                                int outputPort = -1;
                                for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfo.getPorts()) {
                                    Log.d(TAG, String.format("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                                        outputPort = portInfo.getPortNumber();
                                        Log.d(TAG, String.format("[midi port] output port: %d", outputPort));
                                        break;
                                    }
                                }

                                midiOutputPort = midiDevice.openOutputPort(outputPort);
                                midiOutputPort.connect(midiReciever);
                                defaultSharedPreferences.edit().putString("last_midi", midiLastConnectedDevice).commit();
                                defaultSharedPreferences.edit().putInt("last_midi_port", outputPort).commit();
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((findViewById(R.id.bt_icon))).setVisibility(VISIBLE);
                                        ((TextView)mainActivity.findViewById(R.id.midi_name)).setText(midiLastConnectedDevice);
                                    }
                                });
                            }
                        }
                    }, null);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (AUDIO_EFFECT_REQUEST != requestCode && requestCode != READ_STORAGE_REQUEST && requestCode != WRITE_STORAGE_REQUEST &&
                PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (requestCode == READ_STORAGE_REQUEST || requestCode == WRITE_STORAGE_REQUEST) {
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                                "Storage permission denied. Recording and playing features won't work",
                                Toast.LENGTH_LONG)
                        .show();
            } else {
                if (dir == null || !dir.mkdirs()) {
                    Log.e(TAG, "Directory not created: " + dir.toString());
                }

            }
        }

        if (AUDIO_EFFECT_REQUEST == requestCode) {
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                // User denied the permission, without this we cannot record audio
                // Show a toast and update the status accordingly
                Toast.makeText(getApplicationContext(),
                                "Permission denied.",
                                Toast.LENGTH_LONG)
                        .show();
            } else {
                // Permission was granted, start live effect
                toggleEffect(false);
            }
        }

        if (PERMISSION_REQUEST_CODE_CAMERA == requestCode &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mainActivity.rack.toggleVideo.setChecked(false);
//            mainActivity.rack.toggleVideo.setChecked(true);
        }

        if (PERMISSION_REQUEST_CODE_BLUETOOTH == requestCode &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                scanBLE();
            }
        }
    }

    public static int getDominantColor(Bitmap bitmap) {
        // haha!
        return bitmap.getPixel(0, 0);
    }

    @ColorInt
    public static int adjustAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    static String typeToString(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "auxiliary line-level connectors";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth device supporting the A2DP profile";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth device typically used for telephony";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "built-in earphone speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "built-in microphone";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "built-in speaker";
            case AudioDeviceInfo.TYPE_BUS:
                return "BUS";
            case AudioDeviceInfo.TYPE_DOCK:
                return "DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "FM";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "FM tuner";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "HDMI audio return channel";
            case AudioDeviceInfo.TYPE_IP:
                return "IP";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "line analog";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "line digital";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "telephony";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TV tuner";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB accessory";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB device";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wired headphones";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "wired headset";
            default:
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "unknown";
        }
    }

    AlertDialog createPluginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        linearLayoutPluginDialog = (ConstraintLayout) inflater.inflate(R.layout.load_plugin_dialog, null);
        /*
        if (useTheme) {
        }

         */

        EditText editText = (EditText) linearLayoutPluginDialog.findViewById(R.id.pl_search);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                pluginDialogAdapter.search(editable.toString());
            }
        });

        Button clear = linearLayoutPluginDialog.findViewById(R.id.search_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText("");
            }
        });

        ToggleButton toggleButton = (ToggleButton) linearLayoutPluginDialog.findViewById(R.id.pl_favs);
        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                pluginDialogAdapter.showOnlyFavorites(b);
                if (b)
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
                else
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            }
        });

        Iterator<String> keys = MainActivity.pluginCategories.keys();
        List<String> categories = new ArrayList<String>();

        while (keys.hasNext()) {
            String key = keys.next();
            Log.d(TAG, "pluginCategory: key " + key);
            categories.add(key);
        }

        ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, categories);

        List <String> creators = new ArrayList<>();
        keys = pluginCreators.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Log.d(TAG, "pluginCreator: key " + key);
            creators.add(key);
        }

        ArrayAdapter<String> creatorDataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, creators);

        String [] sortBy = {
                "Category",
                "Creator"
        } ;

        ArrayAdapter<String> sortByAdaptor = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, sortBy);

        pluginDialogSortBy = (Spinner) linearLayoutPluginDialog.findViewById(R.id.sort_by);
        pluginDialogSortBy.setAdapter(sortByAdaptor);

        pluginDialogSortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        pluginDialogCategorySpinner.setAdapter(categoriesDataAdapter);
                        break;
                    case 1:
                        pluginDialogCategorySpinner.setAdapter(creatorDataAdapter);
                        break ;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Drop down layout style - list view with radio button
        sortByAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoriesDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        creatorDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pluginDialogCategorySpinner = (Spinner) linearLayoutPluginDialog.findViewById(R.id.plugin_types);
        // attaching data adapter to spinner
        pluginDialogCategorySpinner.setAdapter(categoriesDataAdapter);
        pluginDialogCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (view == null)
                    return;
                String category = ((TextView) view).getText().toString();
                Log.d(TAG, "onItemSelected: selected category " + category);
                pluginDialogAdapter.filterByCategory(category);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pluginDialogWallpaper = linearLayoutPluginDialog.findViewById(R.id.pl_wallpaper);
        Button closeButton = linearLayoutPluginDialog.findViewById(R.id.pl_close);

        if (! tabletMode) {
            builder.setView(linearLayoutPluginDialog);
        } else {
            builder.setView(linearLayoutPluginDialog); // Use dialog for both modes now that 50/50 split is disabled
        }

        AlertDialog pluginDialog = builder.create();
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pluginDialog.hide();
            }
        });
        // Add action buttons
        /*
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                }) ;
            */
        return pluginDialog;
    }

    public void addPluginToRack(int pluginID) {
        int library = pluginID / 100;
        int plug = pluginID - (library * 100);
        Log.d(TAG, "addPluginToRack: loading from " + sharedLibraries.length + " LADSPA and " + sharedLibrariesLV2.length + " LV2 plugins");
        Log.d(TAG, "Adding plugin: " + library + ": " + plug);
        int ret = -1;
        try {
            if (lazyLoad == false)
                ret = AudioEngine.addPlugin(library, plug);
            else {
                // query the plugin and replace the following with
                // if plugin is LADSPA
                if (library > 149 /* because we have 149 lADSPA libraries */) {
                    ret = AudioEngine.addPluginLazyLV2(sharedLibrariesLV2[library - sharedLibraries.length], plug);
                } else
                    ret = AudioEngine.addPluginLazy(sharedLibraries[library], plug);
            }
        } catch (Throwable t) {
            Log.e(TAG, "addPluginToRack: AudioEngine.addPlugin threw", t);
            Toast.makeText(context, "Error adding plugin (see logcat)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ret < 0) {
            Log.w(TAG, "addPluginToRack: native addPlugin returned " + ret);
        }

        dataAdapter.addItem(pluginID, ret >= 0 ? ret : dataAdapter.getItemCount());

        try {
            if (signalChainCanvas != null) {
                signalChainCanvas.post(() -> signalChainCanvas.loadFromActivePlugins());
            }
        } catch (Exception e) {
            Log.w(TAG, "addPluginToRack: canvas refresh skipped", e);
        }

        Toast.makeText(context, "Added plugin to rack", Toast.LENGTH_LONG).show();

//        Snackbar.make(recyclerView, "Added plugin to rack", Snackbar.LENGTH_LONG)
//                .show();
    }

    /**
     * Insert a SPLIT or MERGE routing block at the end of the chain.
     *
     * These are structured routing utilities (not audio effects): a SPLIT forks the
     * signal into two parallel branches, a MERGE recombines them into one. This is
     * the PRIMARY way to build parallel routing — far less error-prone on a
     * touchscreen than dragging a wire between two small port dots. The port-drag
     * wire system remains available as a secondary/advanced interaction.
     *
     * Data model: both map onto the existing GraphSolver DAG as ordinary nodes with
     * fixed port counts (SPLIT = 1-in/2-out, MERGE = 2-in/1-out). No solver changes.
     */
    public void addRoutingBlock(boolean isSplit) {
        try {
            int n = AudioEngine.addRoutingBlock(isSplit);
            if (n <= 0) {
                toast("Could not add routing block");
                return;
            }
            if (dataAdapter != null) {
                // Routing blocks have no plugin library ID; use a sentinel so the
                // rack list stays index-aligned with the engine's activePlugins.
                dataAdapter.addItem(isSplit ? ROUTING_ID_SPLIT : ROUTING_ID_MERGE, n - 1);
            }
            if (signalChainCanvas != null) {
                signalChainCanvas.post(() -> signalChainCanvas.loadFromActivePlugins());
            }
            toast(isSplit ? "SPLIT added — chain branches into two paths"
                          : "MERGE added — branches recombine here");
        } catch (Throwable t) {
            Log.e(TAG, "addRoutingBlock failed", t);
            toast("Error adding routing block");
        }
    }

    static final int ROUTING_ID_SPLIT = 900100;
    static final int ROUTING_ID_MERGE = 900200;

    void loadPlugins() {
        if (AudioEngine.getTotalPlugins() != 0)
            return;
//        String[] tapPlugins = context.getResources().getStringArray(R.array.tap_plugins);
        sharedLibraries = context.getResources().getStringArray(R.array.ladspa_plugins);
        sharedLibrariesLV2 = context.getResources().getStringArray(R.array.lv2_plugins);
        AudioEngine.setMainActivityClassName("com/kancrutt/gigginggear/MainActivity");

//        sharedLibraries.add (sharedLibrariesLV2);

        if (lazyLoad == false) {
            for (String s : sharedLibraries) {
                AudioEngine.loadLibrary(/*"lib" + */s);
            }

            AudioEngine.loadPlugins();
        }


        /*
        File dir = new File (getApplicationInfo().nativeLibraryDir) ;
        File[] soFiles = dir.listFiles();
        Log.d(TAG, "loadPlugins: found "+ soFiles.length + " plugins in directory "+ getApplicationInfo().nativeLibraryDir);

        for (File s: soFiles) {
            Log.d(TAG, "loadPlugins: loading " + s.toString());
            AudioEngine.loadLibrary(s.toString().split(".")[0]);
        }

         */
    }

    String presetToString() {
        JSONObject midiJSON = midiPluginControlsAsJSON();

        int totalPresets = AudioEngine.getActivePlugins();
        JSONObject preset = new JSONObject();

        for (int i = 0; i < totalPresets; i++) {
            JSONObject jo = new JSONObject();
            String vals = "";

            float[] values = AudioEngine.getActivePluginValues(i);
            for (int k = 0; k < values.length; k++) {
                vals += values[k];
                if (k < values.length - 1) {
                    vals += ";";
                }
            }

            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            int spinnerValue = -1 ;
            String audioFile = null ;
            String audioFileR = null ;
            float irBlend = 0.5f ;
            if (holder == null) {
                Log.w(TAG, "presetToString: holder is null" );
            } else {
                audioFile = holder.audioFile;
                audioFileR = holder.audioFileR;
                irBlend = holder.irBlend;
                if (holder.hasFileSpinner) {
                    spinnerValue = holder.modelSpinner.getSelectedItemPosition();
                } else {
                    Log.d(TAG, "presetToString: preset has holder, but no file spinner!");
                }
            }

            try {
                jo.put("name", AudioEngine.getActivePluginName(i));
                if (midiJSON.has(String.valueOf(i))) {
                    jo.put("midi", midiJSON.getJSONArray(String.valueOf(i)));
                }

                jo.put("controls", vals);
                if (spinnerValue > -1)
                    jo.put("selectedModel", spinnerValue);
                if (audioFile != null)
                    jo.put("audioFile", audioFile);
                if (audioFileR != null)
                    jo.put("audioFileR", audioFileR);
                jo.put("irBlend", irBlend);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                preset.put(String.valueOf(i), jo.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            preset.put("routingMode", AudioEngine.getRoutingMode());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            int[] graphEdges = AudioEngine.getGraphEdges();
            if (graphEdges != null && graphEdges.length > 0) {
                preset.put("graph", graphEdges);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return preset.toString();
    }

    Map presetToMap() {
        int totalPresets = AudioEngine.getActivePlugins();
        Map<String, Object> preset = new HashMap<>();

        for (int i = 0; i < totalPresets; i++) {
            Map<String, String> jo = new HashMap<>();
            String vals = "";

            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            int spinnerValue = -1 ;
            String audioFile = null ;
            String audioFileR = null ;
            float irBlend = 0.5f ;

            if (holder == null) {
                Log.w(TAG, "presetToString: holder is null" );
            } else {
                audioFile = holder.audioFile;
                audioFileR = holder.audioFileR;
                irBlend = holder.irBlend;
                if (holder.hasFileSpinner) {
                    spinnerValue = holder.modelSpinner.getSelectedItemPosition();
                } else {
                    Log.d(TAG, "presetToString: preset has holder, but no file spinner!");
                }
            }

            float[] values = AudioEngine.getActivePluginValues(i);
            for (int k = 0; k < values.length; k++) {
                vals += values[k];
                if (k < values.length - 1) {
                    vals += ";";
                }
            }

            jo.put("name", AudioEngine.getActivePluginName(i));
            jo.put("controls", vals);
            jo.put("midi", mainActivity.midiControlsForDB(i));
            if (spinnerValue > -1)
                jo.put("selectedModel", String.valueOf(spinnerValue));
            if (audioFile != null)
                jo.put ("audioFile", audioFile);
            if (audioFileR != null)
                jo.put ("audioFileR", audioFileR);
            jo.put ("irBlend", String.valueOf(irBlend));

            preset.put(String.valueOf(i), jo);
        }

        try {
            int[] graphEdges = AudioEngine.getGraphEdges();
            if (graphEdges != null && graphEdges.length > 0) {
                java.util.List<Integer> gList = new ArrayList<>();
                for (int g : graphEdges) gList.add(g);
                preset.put("graph", gList); // flat list; reader accepts JSONArray/List
            }
        } catch (Exception ignored) {}

        if (signalChainCanvas != null) {
            Map<String, Object> posMap = new HashMap<>();
            for (SignalChainCanvasView.Node n : signalChainCanvas.getNodes()) {
                if (n != null) {
                    java.util.List<Float> xy = new ArrayList<>();
                    xy.add(n.x);
                    xy.add(n.y);
                    posMap.put(String.valueOf(n.id), xy);
                }
            }
            if (!posMap.isEmpty()) {
                preset.put("node_positions", (Map) posMap);
            }
        }

        return preset;
    }

    /**
     * Canonical snapshot of the live rig (engine + canvas) as a schema-v1 Preset.
     * Single source of truth for ALL save paths (top-bar quick save, Presets FAB):
     * plugins with control values, engine-read-back model file ports, TONE3000
     * origin meta, graph edges and canvas node positions. Per-plugin isolation:
     * one bad native read must not blank the whole save.
     */
    LocalPresetManager.Preset buildPresetFromChain(String name, String desc) {
        LocalPresetManager.Preset p = new LocalPresetManager.Preset();
        p.name = name;
        p.description = desc != null ? desc : "";
        p.author = "GigginGear Stage";
        p.favorite = false;
        p.masterInputVolume = 1.0f;
        p.masterOutputVolume = 1.0f;
        java.util.List<LocalPresetManager.PluginItem> plugins = new java.util.ArrayList<>();
        int total = 0;
        try {
            total = AudioEngine.getActivePlugins();
        } catch (Exception e) {
            Log.w(TAG, "buildPresetFromChain: getActivePlugins failed", e);
        }
        for (int i = 0; i < total; i++) {
            try {
                LocalPresetManager.PluginItem item = new LocalPresetManager.PluginItem();
                item.name = AudioEngine.getActivePluginName(i);
                try { item.uniqueId = AudioEngine.getActivePluginID(i); } catch (Exception ignore) { item.uniqueId = 0; }
                try { item.enabled = AudioEngine.getActivePluginEnabled(i); } catch (Exception ignore) { item.enabled = true; }
                float[] values = null;
                try { values = AudioEngine.getActivePluginValues(i); } catch (Exception ignore) {}
                java.util.List<LocalPresetManager.ControlItem> controls = new java.util.ArrayList<>();
                if (values != null) {
                    for (int k = 0; k < values.length; k++) {
                        LocalPresetManager.ControlItem ci = new LocalPresetManager.ControlItem();
                        try { ci.name = AudioEngine.getControlName(i, k); } catch (Exception ignore) { ci.name = "Control " + k; }
                        if (ci.name == null) ci.name = "Control " + k;
                        ci.value = values[k];
                        controls.add(ci);
                    }
                }
                item.controls = controls;
                // Model file actually connected in the engine (NAM/IR) — read back
                // natively so a swapped model survives save/load without UI holders
                // (holders are null for off-screen plugins).
                java.util.List<LocalPresetManager.FilePortItem> fps = new java.util.ArrayList<>();
                try {
                    String loadedFile = AudioEngine.getPluginLoadedFileName(i);
                    if (loadedFile != null && !loadedFile.isEmpty()) {
                        LocalPresetManager.FilePortItem fpi = new LocalPresetManager.FilePortItem();
                        fpi.portIndex = 0;
                        fpi.portName = "Model File";
                        try { fpi.fileName = new java.io.File(loadedFile).getName(); }
                        catch (Exception ignore) { fpi.fileName = loadedFile; }
                        fpi.relativePath = loadedFile;
                        fps.add(fpi);
                    }
                } catch (Exception ignore) {}
                // Fallback: TONE3000 session path when engine read-back is empty.
                // STRICTLY gated on the slot having T3K origin meta this session —
                // an unmapped slot (fresh/foreign preset) must stay empty or the
                // stale path of an unrelated model leaks into this preset.
                if (fps.isEmpty() && getTone3000MetaForSlot(i) != null) {
                    try {
                        String modelPath = getTone3000ModelPathForSlot(i);
                        if (modelPath != null && !modelPath.isEmpty()) {
                            LocalPresetManager.FilePortItem fpi = new LocalPresetManager.FilePortItem();
                            fpi.portIndex = 0;
                            fpi.portName = "Model File";
                            try { fpi.fileName = new java.io.File(modelPath).getName(); }
                            catch (Exception ignore) { fpi.fileName = modelPath; }
                            fpi.relativePath = modelPath;
                            fps.add(fpi);
                        }
                    } catch (Exception ignore) {}
                }
                item.filePorts = fps;
                // TONE3000 origin metadata so badges + missing-file check restore.
                try {
                    JSONObject meta = getTone3000MetaForSlot(i);
                    if (meta != null) {
                        item.tone3000ToneId = meta.optString("tone3000ToneId", null);
                        item.tone3000ModelId = meta.optString("tone3000ModelId", null);
                        item.tone3000Creator = meta.optString("tone3000Creator", null);
                        item.tone3000ToneTitle = meta.optString("tone3000ToneTitle", null);
                        item.tone3000Format = meta.optString("tone3000Format", null);
                        item.tone3000GearType = meta.optString("tone3000GearType", null);
                        item.tone3000SourceUrl = meta.optString("tone3000SourceUrl", null);
                        if (item.tone3000ToneId != null && item.tone3000ToneId.isEmpty()) item.tone3000ToneId = null;
                        if (item.tone3000ModelId != null && item.tone3000ModelId.isEmpty()) item.tone3000ModelId = null;
                        if (item.tone3000Creator != null && item.tone3000Creator.isEmpty()) item.tone3000Creator = null;
                        if (item.tone3000ToneTitle != null && item.tone3000ToneTitle.isEmpty()) item.tone3000ToneTitle = null;
                        if (item.tone3000Format != null && item.tone3000Format.isEmpty()) item.tone3000Format = null;
                        if (item.tone3000GearType != null && item.tone3000GearType.isEmpty()) item.tone3000GearType = null;
                        if (item.tone3000SourceUrl != null && item.tone3000SourceUrl.isEmpty()) item.tone3000SourceUrl = null;
                    }
                } catch (Exception ignore) {}
                plugins.add(item);
            } catch (Exception e) {
                Log.w(TAG, "buildPresetFromChain: skipping plugin " + i, e);
            }
        }
        p.plugins = plugins;

        try {
            int[] graphEdges = AudioEngine.getGraphEdges();
            if (graphEdges != null && graphEdges.length > 0) {
                p.graph = graphEdges;
            }
        } catch (Exception ignored) {}

        try {
            if (signalChainCanvas != null) {
                for (SignalChainCanvasView.Node n : signalChainCanvas.getNodes()) {
                    if (n != null) {
                        p.nodePositions.put(String.valueOf(n.id), new float[]{n.x, n.y});
                    }
                }
            }
        } catch (Exception ignored) {}

        return p;
    }

    /** Reload every visible preset list after a save/delete so top-bar nav,
     *  picker, quick patch and the Presets tab all see the same data. */
    void refreshPresetLists() {
        try {
            if (quickPatch != null && quickPatch.myPresetsAdapter != null) {
                quickPatch.load();
            }
        } catch (Exception e) {
            Log.w(TAG, "refreshPresetLists: quickPatch reload failed", e);
        }
        try {
            if (presets != null && presets.fragmentStateAdapter != null
                    && presets.fragmentStateAdapter.getMyPresets() != null) {
                presets.fragmentStateAdapter.getMyPresets().load();
            }
        } catch (Exception e) {
            Log.w(TAG, "refreshPresetLists: presets tab reload failed", e);
        }
    }

    /** Index of a local preset id inside the quick-patch list (-1 if absent). */
    int findQuickPresetIndexById(String id) {
        try {
            if (id != null && quickPatch != null && quickPatch.myPresetsAdapter != null
                    && quickPatch.myPresetsAdapter.allPresets != null) {
                java.util.List list = quickPatch.myPresetsAdapter.allPresets;
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (o instanceof java.util.Map) {
                        Object mid = ((java.util.Map<?, ?>) o).get("id");
                        if (id.equals(mid != null ? mid.toString() : null)) return i;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "findQuickPresetIndexById failed", e);
        }
        return -1;
    }


    /*  The problem with this is that we can only get viewholders which are visible on screen.
        See #19 https://github.com/djshaji/amp-rack/issues/19
     */
    String _presetToString() throws JSONException {
        JSONObject preset = new JSONObject();
        if (dataAdapter == null)
            return null;

        for (int i = 0; i < dataAdapter.getItemCount(); i++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i + " of " + dataAdapter.getItemCount(), null);
                continue;
            }

            JSONObject jo = new JSONObject();
            String vals = "";

            for (int k = 0; k < holder.sliders.size(); k++) {
                vals += holder.sliders.get(k).getValue();
                if (k < holder.sliders.size() - 1) {
                    vals += ";";
                }
            }

            try {
                jo.put("name", holder.pluginName.getText());
                jo.put("controls", vals);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            preset.put(String.valueOf(i), jo.toString());
        }

        return preset.toString();
    }

    Map _presetToMap() throws JSONException {
        Map<String, Object> preset = new HashMap<>();
        if (dataAdapter == null)
            return null;

        for (int i = 0; i < dataAdapter.getItemCount(); i++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i, null);
                continue;
            }

            Map<String, String> jo = new HashMap<>();
            String vals = "";

            for (int k = 0; k < holder.sliders.size(); k++) {
                vals += holder.sliders.get(k).getValue();
                if (k < holder.sliders.size() - 1) {
                    vals += ";";
                }
            }

            jo.put("name", (String) holder.pluginName.getText());
            jo.put("controls", vals);

            preset.put(String.valueOf(i), jo);
        }

        return preset;
    }


    public void printActivePreset() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset;
        preset = presetToString();

        if (preset == null)
            return;
        Log.d(TAG, "printActivePreset: " + preset.toString());
    }

    public void saveActivePreset() {
        saveGlobalMidi();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset = null;
        // Canonical snapshot (engine-read-back model files, T3K meta, graph,
        // node positions). The legacy presetToString() read model files off
        // RecyclerView holders — null on the hidden list — so cold start
        // restored the right plugins with the WRONG (empty) models.
        try {
            LocalPresetManager.Preset snap = buildPresetFromChain("Active Preset", "");
            if (snap != null) preset = LocalPresetManager.presetToJson(snap).toString();
        } catch (Exception e) {
            Log.w(TAG, "saveActivePreset: canonical snapshot failed, legacy fallback", e);
            preset = null;
        }
        if (preset == null) preset = presetToString();
        if (preset == null)
            Log.d(TAG, "saveActivePreset: preset is null");
        else {
            sharedPreferences.edit().putString("activePreset", preset).apply();
            Log.d(TAG, "saveActivePreset: saved " + preset);
        }

        defaultSharedPreferences.edit().putFloat("inputVolume", inputVolume.getValue()).apply();
        defaultSharedPreferences.edit().putFloat("outputVolume", outputVolume.getValue()).apply();
        defaultSharedPreferences.edit().putBoolean("toggleMixer", toggleMixer.isChecked()).apply();

        Log.d(TAG, "saveActivePreset: toggle mixer: " + !toggleMixer.isChecked());
        Log.d(TAG, "saveActivePreset: Saved preset: " + preset);
    }

    void loadActivePreset() {
        if (safeMode) {
            Log.d(TAG, "loadActivePreset: skipping loading preset");
            return;
        }
        
        loadGlobalMidi();
        
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset = sharedPreferences.getString("activePreset", null);
        AudioEngine.toggleMixer(sharedPreferences.getBoolean("toggleMixer", true));
        Log.i(TAG, "loadActivePreset: loading preset:\n" + preset);

        if (preset != null) {
            Log.d(TAG, "loadActivePreset: " + preset);
            // saveActivePreset now stores a schema-v1 snapshot ("plugins").
            // Route it through the Map loader (model files, T3K meta,
            // positions); legacy strings fall through to the String loader.
            boolean routed = false;
            try {
                java.util.Map<?, ?> snapMap =
                        new com.google.gson.Gson().fromJson(preset, java.util.HashMap.class);
                if (snapMap != null && snapMap.containsKey("plugins")) {
                    loadPreset((Map) snapMap);
                    routed = true;
                }
            } catch (Exception e) {
                Log.w(TAG, "loadActivePreset: snapshot parse failed, legacy load", e);
            }
            if (!routed) loadPreset(preset);
        } else {
            Log.d(TAG, "loadActivePreset: active preset is null or empty");
        }
        ensureDefaultChainHasAmp();
    }

    public void ensureDefaultChainHasAmp() {
        if (dataAdapter == null) return;
        if (dataAdapter.getItemCount() == 0 && AudioEngine.getActivePlugins() == 0) {
            Log.d(TAG, "ensureDefaultChainHasAmp: chain is empty, adding default Amp block");
            addPluginByName("Neural Amp Modeler");
        }
    }

    public void showPluginDetailDialog(int position, String pluginName) {
        populateInlineParameterEditor(position, pluginName);
    }

    public static final int PARAM_EDITOR_CLEAR = -1;

    public void populateInlineParameterEditor(int position, String pluginName) {
        if (position == PARAM_EDITOR_CLEAR) {
            runOnUiThread(() -> clearInlineParameterEditor());
            return;
        }
        if (position < 0 && position != -100 && position != -200) return;
        if (dataAdapter != null && position >= 0) {
            dataAdapter.setSelectedPosition(position);
        }
        runOnUiThread(() -> {
            TextView title = findViewById(R.id.detail_plugin_title);
            TextView subtitle = findViewById(R.id.detail_plugin_subtitle);
            TextView badge = findViewById(R.id.detail_badge_pill);
            MaterialButton enabledBtn = findViewById(R.id.detail_enabled_btn);
            TextView modelName = findViewById(R.id.detail_model_name);
            MaterialButton browseBtn = findViewById(R.id.detail_model_browse_btn);
            // Use ConstraintLayout params to allow conditional centering or side‑by‑side layout
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams btnLp = new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
            // Default: center horizontally and vertically within the parent
            btnLp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            btnLp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            btnLp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            btnLp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            browseBtn.setLayoutParams(btnLp);
            ImageView gearRender = findViewById(R.id.detail_gear_render);
            com.google.android.material.switchmaterial.SwitchMaterial enabledSwitch = findViewById(R.id.detail_enabled_switch);
            TextView enabledLabelView = findViewById(R.id.detail_enabled_label);
            View ledView = findViewById(R.id.detail_led);
            LinearLayout levelContainer = findViewById(R.id.container_level_knobs);
            LinearLayout toneContainer = findViewById(R.id.container_tone_knobs);

            if (position == -100 || position == -200) {
                // Global physical I/O anchors. They do not have bypass switches or models.
                if (title != null) title.setText(position == -100 ? "INPUT" : "OUTPUT");
                if (subtitle != null) subtitle.setText("GLOBAL STAGE");
                if (badge != null) badge.setVisibility(View.GONE);
                if (enabledSwitch != null) enabledSwitch.setVisibility(View.GONE);
                if (enabledLabelView != null) enabledLabelView.setVisibility(View.GONE);
                if (ledView != null) ledView.setVisibility(View.GONE);
                
                if (modelName != null && modelName.getParent() instanceof View) {
                    ((View) modelName.getParent()).setVisibility(View.GONE);
                }
                if (browseBtn != null) browseBtn.setVisibility(View.GONE);
                if (gearRender != null) gearRender.setVisibility(View.GONE);

                TextView gearInfoType = findViewById(R.id.detail_gear_info_type);
                TextView gearInfoDesc = findViewById(R.id.detail_gear_info_desc);
                if (gearInfoType != null) {
                    gearInfoType.setText(position == -100 ? "PHYSICAL INPUT" : "PHYSICAL OUTPUT");
                    gearInfoType.setTextColor(CategoryHelper.getColor(position == -100 ? "input" : "output"));
                }
                if (gearInfoDesc != null) {
                    gearInfoDesc.setText(position == -100 ? "Adjust the global incoming trim level before the signal hits the first block in your chain." : "Adjust the global output level sent to the hardware stream.");
                }

                if (levelContainer != null) levelContainer.removeAllViews();
                if (toneContainer != null) toneContainer.removeAllViews();
                
                // Build the global trim knob
                float density = getResources().getDisplayMetrics().density;
                LinearLayout knobCell = new LinearLayout(this);
                knobCell.setOrientation(LinearLayout.VERTICAL);
                knobCell.setGravity(Gravity.CENTER);
                int marginPx = Math.round(16 * density);
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cellLp.setMargins(marginPx, 0, marginPx, 0);
                knobCell.setLayoutParams(cellLp);
                
                android.graphics.Typeface jakartaFont = null;
                try {
                    jakartaFont = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.plus_jakarta_sans);
                } catch (Throwable ignored) {}
                
                float labelSizePx = 11.5f * density;
                TextView labelView = new TextView(this);
                labelView.setText(position == -100 ? "INPUT TRIM" : "MAIN OUT");
                labelView.setTextColor(Color.parseColor("#8F94A0"));
                labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, labelSizePx);
                labelView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                labelView.setGravity(Gravity.CENTER);
                if (jakartaFont != null) labelView.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                
                int knobDimPx = Math.round(52 * density); // knob_size_large
                RotarySeekbar rKnob = new RotarySeekbar(this);
                rKnob.setShowKnob(true);
                rKnob.setMinValue(0f);
                rKnob.setMaxValue(position == -100 ? 200f : 100f);
                // Currently AudioEngine doesn't have an input/output getter, we track state or initialize to 100
                float initGain;
                try {
                    initGain = (position == -100 ? AudioEngine.getInputVolume() : AudioEngine.getOutputVolume()) * 100f;
                } catch (Exception ignored) { initGain = 100f; }
                rKnob.setValue(initGain);
                rKnob.mKnobColor = CategoryHelper.getColor(position == -100 ? "input" : "output");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(knobDimPx, knobDimPx);
                lp.gravity = Gravity.CENTER;
                rKnob.setLayoutParams(lp);
                
                float valSizePx = 12.5f * density;
                TextView valView = new TextView(this);
                valView.setText(String.format(java.util.Locale.US, "%.0f%%", initGain));
                valView.setTextColor(Color.parseColor("#FFFFFF"));
                valView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, valSizePx);
                valView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                valView.setPadding(Math.round(4 * density), Math.round(2 * density), Math.round(4 * density), Math.round(2 * density));
                valView.setClickable(true);
                valView.setFocusable(true);
                if (jakartaFont != null) valView.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);

                rKnob.setOnValueChangedListener((source, value) -> {
                    if (position == -100) {
                        try { AudioEngine.setInputVolume(value / 100f); } catch (Exception ignored) {}
                    } else {
                        try { AudioEngine.setOutputVolume(value / 100f); } catch (Exception ignored) {}
                    }
                    valView.setText(String.format(java.util.Locale.US, "%.0f%%", value));
                });

                View.OnClickListener manualInputClick = v -> {
                    showManualValueInputDialog(position == -100 ? "Input Trim" : "Main Out", rKnob.getValue(), 0f, position == -100 ? 200f : 100f, "%", newVal -> {
                        rKnob.setValue(newVal);
                        if (position == -100) {
                            try { AudioEngine.setInputVolume(newVal / 100f); } catch (Exception ignored) {}
                        } else {
                            try { AudioEngine.setOutputVolume(newVal / 100f); } catch (Exception ignored) {}
                        }
                        valView.setText(String.format(java.util.Locale.US, "%.0f%%", newVal));
                    });
                };
                valView.setOnClickListener(manualInputClick);
                labelView.setOnClickListener(manualInputClick);
                rKnob.setOnKnobClickListener(k -> manualInputClick.onClick(k));

                knobCell.addView(labelView);
                knobCell.addView(rKnob);
                knobCell.addView(valView);
                
                if (levelContainer != null) levelContainer.addView(knobCell);
                if (toneContainer != null) addEmptyPanelHint(toneContainer, "GLOBAL CONTROL");
                
                return;
            }

            if (title != null) title.setText(pluginName != null ? PluginNaming.displayName(pluginName) : "Plugin Parameters");

            boolean isAmp = dataAdapter != null && dataAdapter.isAmpBlock(pluginName);
            boolean isCab = false;
            if (pluginName != null) {
                String low = pluginName.toLowerCase();
                if (low.contains("cabinet") || low.contains("cab") || low.contains("ir") || low.contains("impulse")) {
                    isCab = true;
                }
            }

            if (subtitle != null) {
                if (isCab) {
                    subtitle.setText("CAB · SPEAKER CABINET 4x12");
                } else if (isAmp) {
                    // Only real model loaders may claim NAM; anything else is
                    // just an amp (C* AmpVTS etc. have no file ports).
                    subtitle.setText(canLoadNamModel(position) ? "AMP · NEURAL AMP (NAM)" : "AMP");
                } else {
                    subtitle.setText("EFFECT · STOMPBOX");
                }
            }
            if (gearRender != null) {
                gearRender.setImageResource(CategoryHelper.getIconRes(pluginName));
                gearRender.setColorFilter(CategoryHelper.getColor(pluginName), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            TextView gearInfoType = findViewById(R.id.detail_gear_info_type);
            TextView gearInfoDesc = findViewById(R.id.detail_gear_info_desc);
            if (gearInfoType != null) {
                gearInfoType.setText(pluginName != null && !pluginName.isEmpty()
                        ? PluginNaming.displayName(pluginName) : "EFFECT");
                gearInfoType.setTextColor(CategoryHelper.getColor(pluginName));
            }
            if (gearInfoDesc != null) {
                String fullDesc = getPluginFullDescription(pluginName);
                if (fullDesc != null && !fullDesc.isEmpty()) {
                    gearInfoDesc.setText(fullDesc);
                } else {
                    gearInfoDesc.setText(pluginName != null ? pluginName : "Standard Model");
                }
            }
            if (badge != null) {
                badge.setText("A" + (position + 1));
                badge.setTextColor(CategoryHelper.getColor(pluginName));
            }

            if (modelName != null) try {
                if (modelName.getParent() instanceof View) {
                    ((View) modelName.getParent()).setVisibility(View.VISIBLE);
                }
                // TONE3000: show tone title + T3K mark if origin is T3K
                JSONObject t3kMeta = getTone3000MetaForSlot(position);
                if (t3kMeta != null) {
                    String tTitle = t3kMeta.optString("tone3000ToneTitle", null);
                    String creator = t3kMeta.optString("tone3000Creator", null);
                    String gear = t3kMeta.optString("tone3000GearType", null);
                    String fmt = t3kMeta.optString("tone3000Format", null);
                    // Use T3K mark per spec §16 compact mark in block, but detail shows full context
                    String display = (tTitle != null && !tTitle.isEmpty() ? tTitle : pluginName);
                    if (creator != null && !creator.isEmpty()) display += "  ·  @" + creator;
                    modelName.setText(display);
                    if (badge != null) { badge.setText("T3K"); badge.setTextColor(Color.parseColor("#FF8A00")); }
                    if (subtitle != null) {
                        String sub = "TONE3000";
                        if (gear != null) sub += " · " + gear.toUpperCase();
                        if (fmt != null) sub += " · " + fmt.toUpperCase();
                        subtitle.setText(sub);
                        subtitle.setTextColor(Color.parseColor("#FF8A00"));
                    }
                    if (gearInfoType != null) gearInfoType.setText(tTitle != null ? tTitle : pluginName);
                    if (gearInfoDesc != null) {
                        String desc = t3kMeta.optString("tone3000ToneTitle", "");
                        if (creator != null) desc += (desc.isEmpty() ? "" : " — ") + "@" + creator;
                        gearInfoDesc.setText(desc);
                    }
                } else {
                    modelName.setText(pluginName != null && !pluginName.isEmpty() ? PluginNaming.displayName(pluginName) : "Standard Model");
                }
            } catch (Exception e) { android.util.Log.w(TAG, "T3K detail bind failed", e); try { modelName.setText(pluginName != null ? PluginNaming.displayName(pluginName) : "Standard Model"); } catch (Exception ignore) {} }

            // "Browse Models" opens the GENERIC replace-plugin browser
            // (showModelPickerDialog): pre-selected to this block's category but
            // free to pick any category — every block needs it, including FX.
            // ONLY the TONE3000-specific entries are AMP/CAB-gated: the injected
            // "Browse TONE3000 →" link below, and the T3K row inside the dialog
            // (loadModelsIntoContainer shows it only under AMP/CAB tabs).
            final boolean allowT3k = isAmp || isCab || isTone3000Slot(position);
            if (browseBtn != null) try {
                browseBtn.setVisibility(View.VISIBLE);
                JSONObject t3kMeta2 = getTone3000MetaForSlot(position);
                if (t3kMeta2 != null) {
                    browseBtn.setText("TONE3000 Details");
                    browseBtn.setOnClickListener(v -> {
                        String tid = t3kMeta2.optString("tone3000ToneId", null);
                        if (tid != null && !tid.isEmpty()) {
                            try { Tone3000ToneDetailSheet sheet = Tone3000ToneDetailSheet.newInstance(tid); sheet.show(getSupportFragmentManager(), "t3k_detail_tap"); } catch (Exception e) { showModelPickerDialog(position); }
                        } else {
                            showModelPickerDialog(position);
                        }
                    });
                } else {
                    browseBtn.setText("Browse");
                    browseBtn.setOnClickListener(v -> showModelPickerDialog(position));
                }
                if (t3kMeta2 != null) {
                    browseBtn.setText("Tone3K");
                    browseBtn.setOnClickListener(v -> {
                        String tid = t3kMeta2.optString("tone3000ToneId", null);
                        if (tid != null && !tid.isEmpty()) {
                            try { Tone3000ToneDetailSheet sheet = Tone3000ToneDetailSheet.newInstance(tid); sheet.show(getSupportFragmentManager(), "t3k_detail_tap"); } catch (Exception e) { showModelPickerDialog(position); }
                        } else {
                            showModelPickerDialog(position);
                        }
                    });
                }
            } catch (Exception e) { android.util.Log.w(TAG, "browseBtn bind failed", e); }
            // Secondary "Browse TONE3000 →" link: AMP/CAB non-T3K blocks only.
            // Always scrub a stale link first (detail panel views are recycled).
            try {
            if (browseBtn != null && browseBtn.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) browseBtn.getParent();
                android.view.View stale = parent.findViewWithTag("t3k_browse_link");
                if (stale != null) parent.removeView(stale);
                if (allowT3k && getTone3000MetaForSlot(position) == null) {
                    TextView t3kLink = new TextView(this);
                    t3kLink.setTag("t3k_browse_link");
                    t3kLink.setId(View.generateViewId());
                    t3kLink.setText("Tone3K");
                    t3kLink.setTextColor(Color.parseColor("#FF8A00"));
                    t3kLink.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f);
                    t3kLink.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    t3kLink.setPadding(Math.round(8 * getResources().getDisplayMetrics().density), Math.round(6 * getResources().getDisplayMetrics().density), Math.round(8 * getResources().getDisplayMetrics().density), Math.round(6 * getResources().getDisplayMetrics().density));
                    t3kLink.setClickable(true);
                    t3kLink.setFocusable(true);
                    t3kLink.setOnClickListener(v -> {
                        try { Tone3000EntrySheet sheet = Tone3000EntrySheet.newInstance(); sheet.show(getSupportFragmentManager(), "t3k_entry"); } catch (Exception e) { android.util.Log.w(TAG, "t3k entry launch failed", e); }
                    });
                    // Set constraints: align start to end of browseBtn, end to parent, vertically centered
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams linkLp = new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    linkLp.startToEnd = browseBtn.getId();
                    linkLp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    linkLp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    linkLp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    t3kLink.setLayoutParams(linkLp);
                    parent.addView(t3kLink);
                    // Adjust browseBtn to be left of the t3kLink when the link is present
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams btnLpAdj = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) browseBtn.getLayoutParams();
                    btnLpAdj.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    btnLpAdj.endToStart = t3kLink.getId();
                    btnLpAdj.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    btnLpAdj.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    btnLpAdj.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    browseBtn.setLayoutParams(btnLpAdj);
                }
            } } catch (Exception e) { android.util.Log.w(TAG, "t3k link injection failed", e); }

            if (gearRender != null) {
                gearRender.setVisibility(View.VISIBLE);
                gearRender.setImageResource(CategoryHelper.getIconRes(pluginName));
                gearRender.setColorFilter(CategoryHelper.getColor(pluginName), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            boolean isActive = AudioEngine.getActivePluginEnabled(position);

            final int colorActiveGreen = Color.parseColor("#00E676");
            final int colorInactiveGrey = Color.parseColor("#8F94A0");
            final ColorStateList thumbTintOn = ColorStateList.valueOf(colorActiveGreen);
            final ColorStateList thumbTintOff = ColorStateList.valueOf(Color.parseColor("#6A6E78"));
            final ColorStateList trackTintOn = ColorStateList.valueOf(Color.parseColor("#2A3D2E"));
            final ColorStateList trackTintOff = ColorStateList.valueOf(Color.parseColor("#25272C"));

            if (enabledSwitch != null) {
                enabledSwitch.setOnCheckedChangeListener(null);
                enabledSwitch.setChecked(isActive);
                enabledSwitch.setThumbTintList(isActive ? thumbTintOn : thumbTintOff);
                enabledSwitch.setTrackTintList(isActive ? trackTintOn : trackTintOff);

                if (enabledLabelView != null) {
                    enabledLabelView.setText(isActive ? "ON" : "OFF");
                    enabledLabelView.setTextColor(isActive ? colorActiveGreen : colorInactiveGrey);
                }
                if (ledView != null) {
                    ledView.setBackgroundResource(isActive ? R.drawable.led_glowing_active : R.drawable.led_glowing_bypass);
                }
                enabledSwitch.setOnCheckedChangeListener((btn, checked) -> {
                    AudioEngine.togglePlugin(position, checked);
                    enabledSwitch.setThumbTintList(checked ? thumbTintOn : thumbTintOff);
                    enabledSwitch.setTrackTintList(checked ? trackTintOn : trackTintOff);
                    if (enabledLabelView != null) {
                        enabledLabelView.setText(checked ? "ON" : "OFF");
                        enabledLabelView.setTextColor(checked ? colorActiveGreen : colorInactiveGrey);
                    }
                    if (ledView != null) {
                        ledView.setBackgroundResource(checked ? R.drawable.led_glowing_active : R.drawable.led_glowing_bypass);
                    }
                    if (dataAdapter != null) dataAdapter.notifyItemChanged(position);
                    if (signalChainCanvas != null) signalChainCanvas.invalidate();
                });
            }
            if (enabledBtn != null) {
                enabledBtn.setText(isActive ? "ENABLED" : "BYPASSED");
                enabledBtn.setTextColor(isActive ? Color.parseColor("#00E676") : Color.parseColor("#888888"));
                enabledBtn.setStrokeColor(ColorStateList.valueOf(isActive ? Color.parseColor("#00E676") : Color.parseColor("#888888")));
                enabledBtn.setOnClickListener(v -> {
                    boolean newActive = !AudioEngine.getActivePluginEnabled(position);
                    AudioEngine.togglePlugin(position, newActive);
                    if (enabledSwitch != null) {
                        enabledSwitch.setOnCheckedChangeListener(null);
                        enabledSwitch.setChecked(newActive);
                        enabledSwitch.setOnCheckedChangeListener((btn, checked) -> {
                            AudioEngine.togglePlugin(position, checked);
                            if (enabledLabelView != null) {
                                enabledLabelView.setText(checked ? "ON" : "OFF");
                                enabledLabelView.setTextColor(checked ? Color.parseColor("#00E676") : Color.parseColor("#888888"));
                            }
                        });
                    }
                    if (enabledLabelView != null) {
                        enabledLabelView.setText(newActive ? "ON" : "OFF");
                        enabledLabelView.setTextColor(newActive ? Color.parseColor("#00E676") : Color.parseColor("#888888"));
                    }
                    enabledBtn.setText(newActive ? "ENABLED" : "BYPASSED");
                    enabledBtn.setTextColor(newActive ? Color.parseColor("#00E676") : Color.parseColor("#888888"));
                    enabledBtn.setStrokeColor(ColorStateList.valueOf(newActive ? Color.parseColor("#00E676") : Color.parseColor("#888888")));
                    if (dataAdapter != null) dataAdapter.notifyItemChanged(position);
                });
            }

            // Populate dynamic parameter panels with 3D Rotary Hero Knobs & readable dB readouts
            LinearLayout levelPanel = findViewById(R.id.panel_level);
            LinearLayout tonePanel = findViewById(R.id.panel_tone);
            LinearLayout rightCol = findViewById(R.id.parameter_right_column);
            if (rightCol != null) {
                rightCol.setVisibility(View.VISIBLE);
            }

            if (levelContainer != null && toneContainer != null) {
                levelContainer.removeAllViews();
                toneContainer.removeAllViews();

                ArrayList<LinearLayout> levelCells = new ArrayList<>();
                ArrayList<LinearLayout> toneCells = new ArrayList<>();

                 float density = getResources().getDisplayMetrics().density;
                 int numControls = AudioEngine.getPluginControls(position);
                 int activePluginId = AudioEngine.getActivePluginID(position);
                int knobDimPx = Math.round(52 * density); // Standard level knob size
                float labelSizePx = 10f * density;
                float valSizePx = 11f * density;

                android.graphics.Typeface jakartaFont = null;
                try {
                    jakartaFont = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.plus_jakarta_sans);
                } catch (Throwable ignored) {}

                for (int i = 0; i < numControls; i++) {
                    String rawName = AudioEngine.getControlName(position, i);
                    // Zero-Trust label: main name for UI, enum hint for helper/picker.
                    // LADSPA embeds enums in the port name ("Modulation depth
                    // (0=none, 1=AM, 2=RM)") — the parens never reach the UI.
                    com.kancrutt.gigginggear.util.LabelFormatter.ControlLabel parsed =
                            com.kancrutt.gigginggear.util.LabelFormatter.parseControlLabel(rawName);
                     String cName = parsed.getMain();
                     if (cName == null || cName.isEmpty()) continue;
                     final String cHint = parsed.getHint();
                     ParameterMetadata.PortMeta metadata = null;
                     int nativePortIndex = AudioEngine.getControlPortIndex(position, i);
                      if (parameterMetadata != null && nativePortIndex >= 0) {
                          String activeUri = "";
                          try { activeUri = AudioEngine.getActivePluginUri(position); } catch (Throwable ignored) {}
                          metadata = (activeUri != null && !activeUri.isEmpty())
                                  ? parameterMetadata.getLv2Port(activeUri, rawName)
                                  : parameterMetadata.getLadspaPort(String.valueOf(activePluginId), nativePortIndex);
                      }
                     if (metadata == null) {
                         Log.d(TAG, "parameter metadata missing plugin=" + activePluginId + " port=" + nativePortIndex + " name=" + cName);
                     }

                    float val = AudioEngine.getActivePluginValue(position, i);
                    float minVal = 0f;
                    float maxVal = 1f;
                    int ctrlType = 0; // 0 FLOAT, 1 INT, 2 TOGGLE, 3 ATOM, 4 ENUM
                    boolean isLog = false;
                    boolean isIntegerParam = false; // discrete integer steps (native isInteger flag / INT type)
                     String nativeUnit = null;
                    try {
                        float[] bounds = AudioEngine.getPluginControlValues(position, i);
                        if (bounds != null && bounds.length >= 3 && bounds[2] > bounds[1]) {
                            minVal = bounds[1];
                            maxVal = bounds[2];
                            if (bounds.length > 3) ctrlType = (int) bounds[3];
                            if (bounds.length > 4) isLog = bounds[4] > 0.5f;
                            if (bounds.length > 6) isIntegerParam = bounds[6] > 0.5f;
                        }
                    } catch (Throwable ignored) {}
                    try { nativeUnit = AudioEngine.getControlUnit(position, i); } catch (Throwable ignored) {}
                    try { int t = AudioEngine.getControlType(position, i); ctrlType = t; } catch (Throwable ignored) {}
                    try { isLog = AudioEngine.getControlIsLogarithmic(position, i); } catch (Throwable ignored) {}
                    if (ctrlType == 1) isIntegerParam = true; // INT type => discrete integer steps
                    // LADSPA enum-in-name: "(0=none, 1=AM, 2=RM)" + INT range 0..N
                    // renders as a proper picker row, never as slider text.
                    String[] hintEnumLabels = null;
                    if (isIntegerParam && cHint != null) {
                        try { hintEnumLabels = com.kancrutt.gigginggear.util.LabelFormatter.parseEnumHint(cHint, minVal, maxVal); } catch (Throwable ignored) {}
                    }
                     final String[] fHintEnumLabels = hintEnumLabels;
                     final int TYPE_TOGGLE = 2;
                     final int TYPE_ENUM = 4;

                     // Metadata wins when non-empty; otherwise fall back to native.
                     // An empty metadata unit must never shadow native "pc".
                     // Unitless stays "" — a 0..1 range alone never implies %.
                     String unit = ParameterFormatter.resolveUnit(
                             metadata != null ? metadata.unit : null, nativeUnit);
                     // Native bounds/current value are the DSP contract. Build
                     // an effective formatter profile from them even when the
                     // optional generated profile has no matching LV2 entry.
                     ParameterMetadata.ControlType effectiveControl = metadata != null
                             ? metadata.controlType
                             : (ctrlType == TYPE_TOGGLE ? ParameterMetadata.ControlType.TOGGLE
                             : ctrlType == TYPE_ENUM ? ParameterMetadata.ControlType.ENUM_PICKER
                             : isIntegerParam ? ParameterMetadata.ControlType.INTEGER_STEPPER
                             : isLog ? ParameterMetadata.ControlType.LOG_KNOB
                             : ParameterMetadata.ControlType.KNOB);
                     ParameterMetadata.PluginType effectivePlugin = metadata != null
                             ? metadata.pluginType
                             : (effectiveControl == ParameterMetadata.ControlType.TOGGLE
                             ? ParameterMetadata.PluginType.BOOLEAN
                             : effectiveControl == ParameterMetadata.ControlType.ENUM_PICKER
                             ? ParameterMetadata.PluginType.ENUM : isIntegerParam
                             ? ParameterMetadata.PluginType.INT : ParameterMetadata.PluginType.FLOAT);
                     final ParameterMetadata.PortMeta parameterMeta = new ParameterMetadata.PortMeta(
                             cName, "port_" + nativePortIndex, nativePortIndex, val,
                             minVal, maxVal, unit == null ? "" : unit, effectiveControl,
                             effectivePlugin, isLog, isIntegerParam,
                             ctrlType == TYPE_TOGGLE,
                             metadata == null ? null : metadata.enumValues,
                             metadata == null ? null : metadata.enumLabels);
                    // Boolean / Toggle: show On/Off, never 0-1% or -100% (fixes Aida Enabled 0-1)
                     if (ctrlType == TYPE_TOGGLE || (metadata != null && metadata.controlType == ParameterMetadata.ControlType.TOGGLE)) {
                        // Render as Switch row — proportional & safe padding, aligned
                        LinearLayout toggleRow = new LinearLayout(this);
                        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
                        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
                        toggleRow.setPadding(Math.round(12 * density), Math.round(12 * density), Math.round(12 * density), Math.round(12 * density));
                        toggleRow.setBackgroundResource(R.drawable.glass_card_bg);
                        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        toggleLp.setMargins(0, Math.round(4 * density), 0, Math.round(4 * density));
                        toggleRow.setLayoutParams(toggleLp);
                        toggleRow.setMinimumHeight(Math.round(48 * density));

                        TextView tLabel = new TextView(this);
                        tLabel.setText(cName);
                        tLabel.setTextColor(Color.parseColor("#E6E6E6"));
                        tLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 11f * density);
                        if (jakartaFont != null) tLabel.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        tLabel.setMaxLines(2);
                        tLabel.setMinLines(1);
                        tLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        tLabel.setSingleLine(false);
                        tLabel.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        tLp.setMarginEnd(Math.round(12 * density));
                        tLp.gravity = Gravity.CENTER_VERTICAL;
                        tLabel.setLayoutParams(tLp);

                        com.google.android.material.switchmaterial.SwitchMaterial sw = new com.google.android.material.switchmaterial.SwitchMaterial(this);
                        sw.setChecked(val > 0.5f);
                        sw.setThumbTintList(ColorStateList.valueOf(val > 0.5f ? Color.parseColor("#00E676") : Color.parseColor("#6A6E78")));
                        sw.setTrackTintList(ColorStateList.valueOf(val > 0.5f ? Color.parseColor("#2A3D2E") : Color.parseColor("#25272C")));
                        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        swLp.gravity = Gravity.CENTER_VERTICAL;
                        sw.setLayoutParams(swLp);
                        TextView status = new TextView(this);
                        status.setText(val > 0.5f ? "ON" : "OFF");
                        status.setTextColor(val > 0.5f ? Color.parseColor("#00E676") : Color.parseColor("#8F94A0"));
                        status.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 10f * density);
                        if (jakartaFont != null) status.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                        status.setMinWidth(Math.round(36 * density));
                        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(Math.round(48 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
                        statusLp.setMarginStart(Math.round(8 * density));
                        statusLp.gravity = Gravity.CENTER_VERTICAL;
                        status.setLayoutParams(statusLp);

                        int finalI2 = i;
                        sw.setOnCheckedChangeListener((btn, checked) -> {
                            AudioEngine.setPluginControl(position, finalI2, checked ? 1f : 0f);
                            status.setText(checked ? "ON" : "OFF");
                            status.setTextColor(checked ? Color.parseColor("#00E676") : Color.parseColor("#8F94A0"));
                            sw.setThumbTintList(ColorStateList.valueOf(checked ? Color.parseColor("#00E676") : Color.parseColor("#6A6E78")));
                            sw.setTrackTintList(ColorStateList.valueOf(checked ? Color.parseColor("#2A3D2E") : Color.parseColor("#25272C")));
                        });
                        toggleRow.addView(tLabel);
                        toggleRow.addView(sw);
                        toggleRow.addView(status);
                        toneCells.add(toggleRow);
                        continue;
                    }
                    // Enumeration: render as a discrete picker row backed by native scalePoint labels.
                    // LADSPA has no scale-points, so an INT control carrying an enum hint
                    // ("0=none, 1=AM, 2=RM") takes the same path with hint-derived labels.
                    if (ctrlType == TYPE_ENUM || (metadata != null && metadata.controlType == ParameterMetadata.ControlType.ENUM_PICKER) || fHintEnumLabels != null) {
                        unit = ""; // enumeration has no unit
                        String[] enumLabels = metadata != null && metadata.enumLabels != null ? metadata.enumLabels : fHintEnumLabels;
                        float[] enumValues = metadata != null ? metadata.enumValues : null;
                        try {
                            String[] nativeLabels = AudioEngine.getControlEnumLabels(position, i);
                            if (nativeLabels != null && nativeLabels.length > 0) enumLabels = nativeLabels;
                        } catch (Throwable ignored) {}
                        final boolean enumHasLabels = enumLabels != null && enumLabels.length > 0;

                        LinearLayout enumRow = new LinearLayout(this);
                        enumRow.setOrientation(LinearLayout.HORIZONTAL);
                        enumRow.setGravity(Gravity.CENTER_VERTICAL);
                        enumRow.setPadding(Math.round(12 * density), Math.round(12 * density), Math.round(12 * density), Math.round(12 * density));
                        enumRow.setBackgroundResource(R.drawable.glass_card_bg);
                        LinearLayout.LayoutParams eRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        eRowLp.setMargins(0, Math.round(4 * density), 0, Math.round(4 * density));
                        enumRow.setLayoutParams(eRowLp);
                        enumRow.setMinimumHeight(Math.round(48 * density));

                        TextView enumName = new TextView(this);
                        enumName.setText(cName);
                        enumName.setTextColor(Color.parseColor("#E6E6E6"));
                        enumName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 11f * density);
                        if (jakartaFont != null) enumName.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        LinearLayout.LayoutParams eNameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.44f);
                        eNameLp.setMarginEnd(Math.round(12 * density));
                        eNameLp.gravity = Gravity.CENTER_VERTICAL;
                        enumName.setLayoutParams(eNameLp);
                        enumName.setMaxLines(2);
                        enumName.setMinLines(1);
                        enumName.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        enumName.setSingleLine(false);
                        enumName.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                        TextView enumValue = new TextView(this);
                        // Labeled enums may start above zero (LADSPA 1-based, LV2
                        // scale-points): index labels by (value - min), never raw.
                        int labelIdx0 = enumValues != null ? nearestEnumIndex(val, enumValues) : (int) Math.round(val - minVal);
                        enumValue.setText(enumHasLabels && labelIdx0 >= 0 && labelIdx0 < enumLabels.length ? enumLabels[labelIdx0] : String.valueOf((int) val));
                        enumValue.setTextColor(Color.parseColor("#A1A1AA"));
                        enumValue.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 10.5f * density);
                        if (jakartaFont != null) enumValue.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        enumValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                        enumValue.setMaxLines(2);
                        enumValue.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        LinearLayout.LayoutParams eValLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.40f);
                        eValLp.setMarginEnd(Math.round(12 * density));
                        eValLp.gravity = Gravity.CENTER_VERTICAL;
                        enumValue.setLayoutParams(eValLp);

                        ImageView enumMore = new ImageView(this);
                        enumMore.setImageResource(R.drawable.ic_more_vert);
                        enumMore.setColorFilter(Color.parseColor("#71717A"), android.graphics.PorterDuff.Mode.SRC_IN);
                        LinearLayout.LayoutParams eMoreLp = new LinearLayout.LayoutParams(Math.round(20 * density), Math.round(20 * density));
                        eMoreLp.setMargins(Math.round(4 * density), 0, Math.round(4 * density), 0);
                        eMoreLp.gravity = Gravity.CENTER_VERTICAL;
                        enumMore.setLayoutParams(eMoreLp);

                        int finalIEnum = i;
                        String[] labelsFinal = enumLabels;
                        float[] valuesFinal = enumValues;
                        final boolean hasLabelsFinal = enumHasLabels;
                        // Map native scale-point label index back to native integer value.
                        // If we have labels, options are 0..labels.length-1 (snap current to range).
                        final float fEnumMinVal = minVal;
                        final float fEnumMaxVal = maxVal;
                        int optionCount = hasLabelsFinal ? labelsFinal.length : Math.max(1, (int) (fEnumMaxVal - fEnumMinVal + 1));
                        // Clamp initial selection into valid range
                        int initialIdx;
                        if (hasLabelsFinal) {
                            initialIdx = valuesFinal != null ? nearestEnumIndex(val, valuesFinal) : Math.max(0, Math.min(labelsFinal.length - 1, (int) Math.round(val - fEnumMinVal)));
                        } else {
                            initialIdx = Math.max(0, Math.min(optionCount - 1, (int) Math.round(val - fEnumMinVal)));
                        }
                        enumValue.setText(hasLabelsFinal ? labelsFinal[initialIdx] : String.valueOf(initialIdx));

                        View.OnClickListener enumClick = v -> {
                            if (hasLabelsFinal) {
                                com.google.android.material.dialog.MaterialAlertDialogBuilder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
                                b.setTitle(cName);
                                 int cur = valuesFinal != null ? nearestEnumIndex(AudioEngine.getActivePluginValue(position, finalIEnum), valuesFinal) : Math.max(0, Math.min(labelsFinal.length - 1, (int) Math.round(AudioEngine.getActivePluginValue(position, finalIEnum) - fEnumMinVal)));
                                 b.setSingleChoiceItems(labelsFinal, cur, (dialog, which) -> {
                                     float newVal = valuesFinal != null ? valuesFinal[which] : fEnumMinVal + (float) which;
                                    AudioEngine.setPluginControl(position, finalIEnum, newVal);
                                    enumValue.setText(labelsFinal[which]);
                                    dialog.dismiss();
                                });
                                b.setNegativeButton("CANCEL", null);
                                b.show();
                            } else {
                                // No labels — cycle integer steps via +/- in dialog
                                String[] opts = new String[optionCount];
                                for (int k = 0; k < optionCount; k++) opts[k] = String.valueOf(k);
                                com.google.android.material.dialog.MaterialAlertDialogBuilder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
                                b.setTitle(cName);
                                final int finalOptionCount = optionCount;
                                int cur = Math.max(0, Math.min(finalOptionCount - 1, (int) Math.round(AudioEngine.getActivePluginValue(position, finalIEnum) - fEnumMinVal)));
                                b.setSingleChoiceItems(opts, cur, (dialog, which) -> {
                                    float newVal = fEnumMinVal + (float) which;
                                    AudioEngine.setPluginControl(position, finalIEnum, newVal);
                                    enumValue.setText(opts[which]);
                                    dialog.dismiss();
                                });
                                b.setNegativeButton("CANCEL", null);
                                b.show();
                            }
                        };
                        enumRow.setOnClickListener(enumClick);
                        enumName.setOnClickListener(enumClick);
                        enumValue.setOnClickListener(enumClick);
                        enumMore.setOnClickListener(enumClick);

                        enumRow.addView(enumName);
                        enumRow.addView(enumValue);
                        enumRow.addView(enumMore);
                        toneCells.add(enumRow);
                        continue;
                    }

                    boolean isLevelControl = metadata != null && ("dB".equals(metadata.unit) || "coef".equals(metadata.unit));
                    // Override: if log freq, not level knob even if contains gain? Keep level for true gain
                    final int pluginColor = CategoryHelper.getColor(pluginName);
                    final boolean fIsLog = isLog;
                    final boolean fIsInt = isIntegerParam;

                    if (isLevelControl) {
                        // Level controls use the same horizontal slider row as
                        // tone controls (lastworks build: all controls are sliders)
                        LinearLayout sliderRowLvl = new LinearLayout(this);
                        sliderRowLvl.setOrientation(LinearLayout.HORIZONTAL);
                        sliderRowLvl.setGravity(Gravity.CENTER_VERTICAL);
                        sliderRowLvl.setPadding(Math.round(12 * density), Math.round(12 * density), Math.round(12 * density), Math.round(12 * density));
                        sliderRowLvl.setBackgroundResource(R.drawable.glass_card_bg);
                        LinearLayout.LayoutParams rowLpLvl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rowLpLvl.setMargins(0, Math.round(4 * density), 0, Math.round(4 * density));
                        sliderRowLvl.setLayoutParams(rowLpLvl);
                        sliderRowLvl.setMinimumHeight(Math.round(48 * density));

                        TextView labelView = new TextView(this);
                        labelView.setText(cName.toUpperCase());
                        if (cHint != null && !cHint.isEmpty()) {
                            labelView.setContentDescription(cName + " (" + cHint + ")");
                        }
                        labelView.setTextColor(Color.parseColor("#E6E6E6"));
                        labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 11f * density);
                        labelView.setMaxLines(2);
                        labelView.setMinLines(1);
                        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        if (jakartaFont != null) labelView.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.44f);
                        nameLp.setMarginEnd(Math.round(12 * density));
                        nameLp.gravity = Gravity.CENTER_VERTICAL;
                        labelView.setLayoutParams(nameLp);

                        final int progressMaxLvl = 10000;
                        SeekBar seekBarLvl = new SeekBar(this);
                        float trackRadiusPxLvl = 7 * density;
                        android.graphics.drawable.GradientDrawable bgShapeLvl = new android.graphics.drawable.GradientDrawable();
                        bgShapeLvl.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        bgShapeLvl.setCornerRadius(trackRadiusPxLvl);
                        bgShapeLvl.setColor(Color.parseColor("#2E323B"));
                        RoundedProgressDrawable progressFillLvl = new RoundedProgressDrawable(pluginColor, trackRadiusPxLvl);
                        android.graphics.drawable.LayerDrawable trackDrawableLvl = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bgShapeLvl, progressFillLvl});
                        trackDrawableLvl.setId(0, android.R.id.background);
                        trackDrawableLvl.setId(1, android.R.id.progress);
                        seekBarLvl.setProgressDrawable(trackDrawableLvl);
                        seekBarLvl.setThumb(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                        seekBarLvl.setPadding(0, 0, 0, 0);
                        seekBarLvl.setMax(progressMaxLvl);
                        float clampedValLvl = Math.max(minVal, Math.min(val, maxVal));
                        float valueRangeLvl = maxVal > minVal ? maxVal - minVal : 1f;
                        if (fIsLog) {
                            float p01 = logValueToProgress(clampedValLvl, minVal, maxVal);
                            seekBarLvl.setProgress(Math.round(p01 * progressMaxLvl));
                        } else {
                            seekBarLvl.setProgress(Math.round((clampedValLvl - minVal) / valueRangeLvl * progressMaxLvl));
                        }
                        LinearLayout.LayoutParams sliderLpLvl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        sliderLpLvl.setMarginStart(Math.round(4 * density));
                        seekBarLvl.setLayoutParams(sliderLpLvl);

                        TextView valView = new TextView(this);
                        valView.setText(ParameterFormatter.rawToDisplay(val, parameterMeta).text);
                        valView.setTextColor(Color.parseColor("#E6E6E6"));
                        valView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, valSizePx);
                        if (jakartaFont != null) valView.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        valView.setPadding(Math.round(4 * density), Math.round(2 * density), Math.round(4 * density), Math.round(2 * density));
                        valView.setMinWidth(Math.round(52 * density));
                        valView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

                        int finalILvl = i;
                        String unitFinalLvl = unit;
                        final float fMinLvl = minVal;
                        final float fMaxLvl = maxVal;
                        final String fCNameLvl = cName;
                        final String fHintLvl = cHint;

                        seekBarLvl.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                                if (!fromUser) return;
                                float value;
                                if (fIsLog) {
                                    value = logProgressToValue(progress / (float) progressMaxLvl, fMinLvl, fMaxLvl);
                                } else {
                                    value = fMinLvl + (progress / (float) progressMaxLvl) * (fMaxLvl - fMinLvl);
                                }
                                if (fIsInt) {
                                    value = Math.round(value);
                                }
                                value = Math.max(fMinLvl, Math.min(fMaxLvl, value));
                                AudioEngine.setPluginControl(position, finalILvl, value);
                                valView.setText(ParameterFormatter.rawToDisplay(value, parameterMeta).text);
                            }
                            @Override public void onStartTrackingTouch(SeekBar sb) {}
                            @Override public void onStopTrackingTouch(SeekBar sb) {}
                        });

                        View.OnClickListener manualInputClickLvl = v -> {
                            float curValLvl;
                            if (fIsLog) {
                                curValLvl = logProgressToValue(seekBarLvl.getProgress() / (float) progressMaxLvl, fMinLvl, fMaxLvl);
                                curValLvl = Math.max(fMinLvl, Math.min(fMaxLvl, curValLvl));
                            } else {
                                curValLvl = fMinLvl + (seekBarLvl.getProgress() / (float) progressMaxLvl) * (fMaxLvl - fMinLvl);
                            }
                            showManualValueInputDialog(fCNameLvl, curValLvl, fMinLvl, fMaxLvl, unitFinalLvl, fHintLvl, newVal -> {
                                if (fIsInt) newVal = Math.round(newVal);
                                newVal = Math.max(fMinLvl, Math.min(fMaxLvl, newVal));
                                AudioEngine.setPluginControl(position, finalILvl, newVal);
                                if (fIsLog) {
                                    seekBarLvl.setProgress(Math.round(logValueToProgress(newVal, fMinLvl, fMaxLvl) * progressMaxLvl));
                                } else {
                                    seekBarLvl.setProgress(Math.round((newVal - fMinLvl) / (fMaxLvl - fMinLvl) * progressMaxLvl));
                                }
                                valView.setText(ParameterFormatter.rawToDisplay(newVal, parameterMeta).text);
                            });
                        };
                        valView.setOnClickListener(manualInputClickLvl);
                        labelView.setOnClickListener(manualInputClickLvl);

                        sliderRowLvl.addView(labelView);
                        sliderRowLvl.addView(seekBarLvl);
                        sliderRowLvl.addView(valView);
                        levelCells.add(sliderRowLvl);
                    } else {
                        // Tone / FX controls: proportional layout — safe padding, no mepet/cut, aligned with slider
                        LinearLayout sliderRow = new LinearLayout(this);
                        sliderRow.setOrientation(LinearLayout.HORIZONTAL);
                        sliderRow.setGravity(Gravity.CENTER_VERTICAL);
                        sliderRow.setPadding(Math.round(12 * density), Math.round(12 * density), Math.round(12 * density), Math.round(12 * density));
                        sliderRow.setBackgroundResource(R.drawable.glass_card_bg);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rowLp.setMargins(0, Math.round(4 * density), 0, Math.round(4 * density));
                        sliderRow.setLayoutParams(rowLp);
                        sliderRow.setMinimumHeight(Math.round(48 * density));

                        TextView nameLabel = new TextView(this);
                        nameLabel.setText(cName);
                        if (cHint != null && !cHint.isEmpty()) {
                            // Enum detail lives here (tooltip) + manual dialog message —
                            // never baked into the visible label text.
                            nameLabel.setContentDescription(cName + " (" + cHint + ")");
                        }
                        nameLabel.setTextColor(Color.parseColor("#E6E6E6"));
                        nameLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 11f * density);
                        if (jakartaFont != null) nameLabel.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.44f);
                        nameLp.setMarginEnd(Math.round(12 * density));
                        nameLp.gravity = Gravity.CENTER_VERTICAL;
                        nameLabel.setLayoutParams(nameLp);
                        nameLabel.setMaxLines(2);
                        nameLabel.setMinLines(1);
                        nameLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        nameLabel.setSingleLine(false);
                        nameLabel.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                        final int progressMax = 10000;
                        SeekBar seekBar = new SeekBar(this);
                        // Track: rounded bg layer + rounded progress fill. The progress fill widens
                        // with the level but keeps rounded corners on both ends (no sharp clip edge).
                        float trackRadiusPx = 7 * density;
                        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
                        bgShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        bgShape.setCornerRadius(trackRadiusPx);
                        bgShape.setColor(Color.parseColor("#2E323B"));
                        RoundedProgressDrawable progressFill = new RoundedProgressDrawable(pluginColor, trackRadiusPx);
                        android.graphics.drawable.LayerDrawable trackDrawable = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bgShape, progressFill});
                        trackDrawable.setId(0, android.R.id.background);
                        trackDrawable.setId(1, android.R.id.progress);
                        seekBar.setProgressDrawable(trackDrawable);
                        seekBar.setThumb(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                        seekBar.setPadding(0, 0, 0, 0);
                        seekBar.setMax(progressMax);
                        float clampedVal = Math.max(minVal, Math.min(val, maxVal));
                        float valueRange = maxVal > minVal ? maxVal - minVal : 1f;
                        // Logarithmic: project linear progress <-> value via log domain
                        if (fIsLog) {
                            float p01 = logValueToProgress(clampedVal, minVal, maxVal);
                            seekBar.setProgress(Math.round(p01 * progressMax));
                        } else {
                            seekBar.setProgress(Math.round((clampedVal - minVal) / valueRange * progressMax));
                        }
                        LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        sliderLp.setMarginStart(Math.round(4 * density));
                        sliderLp.setMarginEnd(Math.round(8 * density));
                        sliderLp.gravity = Gravity.CENTER_VERTICAL;
                        seekBar.setLayoutParams(sliderLp);

                        TextView valReadout = new TextView(this);
                                valReadout.setText(ParameterFormatter.rawToDisplay(clampedVal, parameterMeta).text);
                        valReadout.setTextColor(Color.parseColor("#E6E6E6"));
                        valReadout.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 10.5f * density);
                        valReadout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                        if (jakartaFont != null) valReadout.setTypeface(jakartaFont, android.graphics.Typeface.BOLD);
                        valReadout.setSingleLine(true);
                        valReadout.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(Math.round(54 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
                        valLp.gravity = Gravity.CENTER_VERTICAL;
                        valReadout.setLayoutParams(valLp);

                        ImageView moreBtn = new ImageView(this);
                        moreBtn.setImageResource(R.drawable.ic_more_vert);
                        moreBtn.setColorFilter(Color.parseColor("#71717A"), android.graphics.PorterDuff.Mode.SRC_IN);
                        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(Math.round(20 * density), Math.round(20 * density));
                        moreLp.setMargins(Math.round(4 * density), 0, Math.round(4 * density), 0);
                        moreLp.gravity = Gravity.CENTER_VERTICAL;
                        moreBtn.setLayoutParams(moreLp);

                        int finalI = i;
                        String unitFinal = unit;
                        final float fMin = minVal;
                        final float fMax = maxVal;
                        final String fCName = cName;
                        final String fHint = cHint;
                        final float fValueRange = valueRange;
                        final boolean fbIsLog = fIsLog;
                        final boolean fbIsInt = isIntegerParam;
                        final int tracePluginId = activePluginId;
                        final int tracePort = nativePortIndex;
                        final int[] lastAcceptedProgress = {seekBar.getProgress()};
                        final float[] lastAcceptedRaw = {clampedVal};
                        final boolean[] updatingControl = {false};

                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                                if (!fromUser || updatingControl[0]) return;
                                float value = Float.NaN;
                                try {
                                    if (progress < 0 || progress > progressMax) {
                                        throw new IllegalArgumentException("progress outside bounds: " + progress);
                                    }
                                    if (!(Float.isFinite(fMin) && Float.isFinite(fMax)) || fMax <= fMin) {
                                        throw new IllegalArgumentException("invalid range " + fMin + ".." + fMax);
                                    }
                                    float p01 = progress / (float) progressMax;
                                    value = fbIsLog ? logProgressToValue(p01, fMin, fMax)
                                            : fMin + p01 * (fMax - fMin);
                                    if (!Float.isFinite(value)) throw new IllegalArgumentException("non-finite converted value");
                                    if (fbIsInt) value = Math.round(value);
                                    value = Math.max(fMin, Math.min(fMax, value));
                                    Log.i(TAG, "PARAM_TRACE ui_edit plugin=" + position + " pluginId=" + tracePluginId
                                            + " control=" + finalI + " port=" + tracePort + " name=" + fCName
                                            + " progress=" + progress + "/" + progressMax + " raw=" + value
                                            + " min=" + fMin + " max=" + fMax + " unit=" + unitFinal
                                            + " metadataNull=" + (parameterMeta == null)
                                            + " enumValues=" + java.util.Arrays.toString(parameterMeta == null ? null : parameterMeta.enumValues)
                                            + " enumLabels=" + java.util.Arrays.toString(parameterMeta == null ? null : parameterMeta.enumLabels));
                                    AudioEngine.setPluginControl(position, finalI, value);
                                    float accepted = AudioEngine.getActivePluginValue(position, finalI);
                                    if (!Float.isFinite(accepted)) throw new IllegalStateException("native returned non-finite value");
                                    int acceptedProgress = fbIsLog
                                            ? Math.round(logValueToProgress(accepted, fMin, fMax) * progressMax)
                                            : Math.round((accepted - fMin) / (fMax - fMin) * progressMax);
                                    acceptedProgress = Math.max(0, Math.min(progressMax, acceptedProgress));
                                    lastAcceptedRaw[0] = accepted;
                                    lastAcceptedProgress[0] = acceptedProgress;
                                    valReadout.setText(ParameterFormatter.rawToDisplay(accepted, parameterMeta).text);
                                    updatingControl[0] = true;
                                    sb.setProgress(acceptedProgress);
                                    updatingControl[0] = false;
                                    Log.i(TAG, "PARAM_TRACE native_readback plugin=" + position + " control=" + finalI
                                            + " raw=" + accepted + " display=" + valReadout.getText()
                                            + " unit=" + unitFinal);
                                } catch (Throwable t) {
                                    updatingControl[0] = true;
                                    sb.setProgress(lastAcceptedProgress[0]);
                                    updatingControl[0] = false;
                                    try { valReadout.setText(ParameterFormatter.rawToDisplay(lastAcceptedRaw[0], parameterMeta).text); } catch (Throwable ignored) { }
                                    String context = "PARAM_TRACE slider_exception plugin=" + position + " pluginId=" + tracePluginId
                                            + " control=" + finalI + " port=" + tracePort + " name=" + fCName
                                            + " progress=" + progress + "/" + progressMax + " raw=" + value
                                            + " min=" + fMin + " max=" + fMax + " unit=" + unitFinal
                                            + " metadataNull=" + (parameterMeta == null)
                                            + " enumValues=" + java.util.Arrays.toString(parameterMeta == null ? null : parameterMeta.enumValues)
                                            + " enumLabels=" + java.util.Arrays.toString(parameterMeta == null ? null : parameterMeta.enumLabels);
                                    Log.e(TAG, context, t);
                                }
                            }
                            @Override public void onStartTrackingTouch(SeekBar sb) {}
                            @Override public void onStopTrackingTouch(SeekBar sb) {}
                        });

                        View.OnClickListener manualInputClick = v -> {
                            float p01cur = seekBar.getProgress() / (float) progressMax;
                            float currentVal = fbIsLog ? logProgressToValue(p01cur, fMin, fMax)
                                                      : fMin + p01cur * fValueRange;
                            showManualValueInputDialog(fCName, currentVal, fMin, fMax, unitFinal, fHint, newVal -> {
                                float rawVal = newVal;
                                if (fbIsInt) rawVal = Math.round(rawVal);
                                rawVal = Math.max(fMin, Math.min(fMax, rawVal));
                                AudioEngine.setPluginControl(position, finalI, rawVal);
                                int newProgress;
                                if (fbIsLog) {
                                    newProgress = Math.round(logValueToProgress(rawVal, fMin, fMax) * progressMax);
                                } else {
                                    newProgress = Math.round((rawVal - fMin) / fValueRange * progressMax);
                                }
                                seekBar.setProgress(Math.max(0, Math.min(progressMax, newProgress)));
                                    valReadout.setText(ParameterFormatter.rawToDisplay(rawVal, parameterMeta).text);
                            });
                        };
                        valReadout.setOnClickListener(manualInputClick);
                        nameLabel.setOnClickListener(manualInputClick);
                        moreBtn.setOnClickListener(manualInputClick);

                        sliderRow.addView(nameLabel);
                        sliderRow.addView(seekBar);
                        sliderRow.addView(valReadout);
                        sliderRow.addView(moreBtn);
                        toneCells.add(sliderRow);
                    }
                }

                // Populate LEVEL container
                for (LinearLayout cell : levelCells) {
                    levelContainer.addView(cell);
                }

                // Populate CONTROLS container as a vertical list of sleek slider rows
                toneContainer.setOrientation(LinearLayout.VERTICAL);
                toneContainer.setGravity(Gravity.NO_GRAVITY);
                for (LinearLayout cell : toneCells) {
                    toneContainer.addView(cell);
                }

                // Dynamically adjust panel visibility
                boolean hasLevel = levelCells.size() > 0;
                boolean hasTone = toneCells.size() > 0;

                if (levelPanel != null) {
                    levelPanel.setVisibility(hasLevel ? View.VISIBLE : View.GONE);
                }

                if (!hasLevel && !hasTone) {
                    addEmptyPanelHint(toneContainer, "MODEL-CAPTURED / NO CONTROLS");
                }
            }
        });
    }

    private void clearInlineParameterEditor() {
        TextView title = findViewById(R.id.detail_plugin_title);
        TextView subtitle = findViewById(R.id.detail_plugin_subtitle);
        TextView badge = findViewById(R.id.detail_badge_pill);
        TextView modelName = findViewById(R.id.detail_model_name);
        MaterialButton browseBtn = findViewById(R.id.detail_model_browse_btn);
        ImageView gearRender = findViewById(R.id.detail_gear_render);
        com.google.android.material.switchmaterial.SwitchMaterial enabledSwitch = findViewById(R.id.detail_enabled_switch);
        TextView enabledLabelView = findViewById(R.id.detail_enabled_label);
        View ledView = findViewById(R.id.detail_led);
        LinearLayout levelContainer = findViewById(R.id.container_level_knobs);
        LinearLayout toneContainer = findViewById(R.id.container_tone_knobs);
        TextView gearInfoType = findViewById(R.id.detail_gear_info_type);
        TextView gearInfoDesc = findViewById(R.id.detail_gear_info_desc);

        if (title != null) title.setText("No Plugin Selected");
        if (subtitle != null) subtitle.setText("Select a block from the canvas");
        if (badge != null) badge.setVisibility(View.GONE);
        if (modelName != null) modelName.setText("");
        if (browseBtn != null) browseBtn.setVisibility(View.GONE);
        if (gearRender != null) gearRender.setImageResource(0);
        if (enabledSwitch != null) enabledSwitch.setVisibility(View.GONE);
        if (enabledLabelView != null) enabledLabelView.setVisibility(View.GONE);
        if (ledView != null) ledView.setVisibility(View.GONE);
        if (gearInfoType != null) gearInfoType.setText("");
        if (gearInfoDesc != null) gearInfoDesc.setText("");
        if (levelContainer != null) levelContainer.removeAllViews();
        if (toneContainer != null) toneContainer.removeAllViews();
        addEmptyPanelHint(toneContainer, "SELECT A BLOCK");
        if (dataAdapter != null) dataAdapter.setSelectedPosition(-1);
    }

    /**
     * Center-justify a knob row inside its HorizontalScrollView when the content
     * is narrower than the viewport; switch to START gravity when it overflows,
     * so scrolling can reach the first (leftmost) knob instead of clipping it.
     */
    private void applyScrollGravity(final LinearLayout knobContainer) {
        if (knobContainer == null || !(knobContainer.getParent() instanceof android.view.ViewGroup)) return;
        final android.view.ViewGroup hsv = (android.view.ViewGroup) knobContainer.getParent();
        hsv.post(() -> {
            int childW = knobContainer.getWidth();
            int parentW = hsv.getWidth();
            if (childW <= 0 || parentW <= 0) return;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) knobContainer.getLayoutParams();
            if (!(lp instanceof FrameLayout.LayoutParams)) return;
            int g = (childW <= parentW) ? Gravity.CENTER : Gravity.START;
            if (((FrameLayout.LayoutParams) lp).gravity != g) {
                ((FrameLayout.LayoutParams) lp).gravity = g;
                knobContainer.setLayoutParams(lp);
            }
        });
    }

    private void addEmptyPanelHint(LinearLayout container, String message) {
        TextView hint = new TextView(this);
        hint.setText(message);
        hint.setTextColor(Color.parseColor("#555A66"));
        hint.setTextSize(10);
        hint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(8, 12, 8, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        container.addView(hint, lp);
    }

    public void updateRoutingUI(int mode) {
        // No routing buttons anymore - routing is implicit from node wiring
        // This method kept for preset loading compatibility only
    }

    int getCategoryIconRes(String pluginName) {
        return CategoryHelper.getSelectorRes(pluginName);
    }

    int getGearRenderRes(String pluginName) {
        return CategoryHelper.getIconRes(pluginName);
    }

    static String prettyControlName(String cName) {
        // Single choke point: enum hints ("(0=none, 1=AM)") are stripped here,
        // so EVERY caller (toggles, enums, knobs, sliders, MIDI) gets the clean
        // main label. Handles camelCase/snake_case/concatenated words globally.
        return com.kancrutt.gigginggear.util.LabelFormatter.parseControlLabel(cName).getMain();
    }

    /**
     * Progress fill for the plugin-control SeekBar. Unlike ClipDrawable (which cuts the
     * leading edge with a straight vertical line), this draws a rounded rect whose width
     * tracks the progress level, so both ends of the filled segment stay smoothly rounded.
     */
    static class RoundedProgressDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float radiusPx;
        RoundedProgressDrawable(int color, float radiusPx) {
            this.radiusPx = radiusPx;
            paint.setColor(color);
        }
        @Override public void draw(android.graphics.Canvas canvas) {
            android.graphics.RectF b = new android.graphics.RectF(getBounds());
            float frac = Math.max(0f, Math.min(1f, getLevel() / 10000f));
            float w = b.width() * frac;
            if (w <= 0f) return;
            canvas.drawRoundRect(b.left, b.top, b.left + w, b.bottom, radiusPx, radiusPx, paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
    // ---- Logarithmic scaling: Hz / ms with portProperty logarithmic — Slider feels natural
    static float logProgressToValue(float p01, float min, float max) {
        if (min <= 0 || max <= min) return min + p01 * (max - min);
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        return (float) Math.exp(logMin + p01 * (logMax - logMin));
    }
    static float logValueToProgress(float val, float min, float max) {
        if (min <= 0 || max <= min || val <= 0) return (val - min) / (max - min);
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        double logVal = Math.log(Math.max(min, val));
        return (float) ((logVal - logMin) / (logMax - logMin));
    }

    public void showManualValueInputDialog(String controlName, float currentVal, float minVal, float maxVal, String unit, OnManualValueListener listener) {
        showManualValueInputDialog(controlName, currentVal, minVal, maxVal, unit, null, listener);
    }

    public void showManualValueInputDialog(String controlName, float currentVal, float minVal, float maxVal, String unit, String hint, OnManualValueListener listener) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        if (hint != null && !hint.isEmpty()) builder.setMessage(hint);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);
        
        TextView hintView = new TextView(this);
        // Percent rule (same as ParameterFormatter): scale 0..1 -> 0..100
        // ONLY when the final unit is "%". Unitless 0..1 stays raw.
        final boolean pctScaled = ParameterFormatter.isPercentScaled(unit, minVal, maxVal);
        final String unitLabel = (unit != null) ? unit : "";
        float dMin = pctScaled ? minVal * 100f : minVal;
        float dMax = pctScaled ? maxVal * 100f : maxVal;
        float dCur = pctScaled ? currentVal * 100f : currentVal;
        String rangeStr = "Min: " + ParameterFormatter.formatNumber(dMin)
                + "   Max: " + ParameterFormatter.formatNumber(dMax)
                + (unitLabel.isEmpty() ? "" : " " + unitLabel);
        hintView.setText(rangeStr);
        hintView.setTextColor(Color.parseColor("#8F94A0"));
        hintView.setTextSize(12);
        hintView.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        container.addView(hintView);
        
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER 
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL 
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        // Display value uses the main formatter's numbering so the dialog
        // matches knob/slider readouts (e.g. raw 0.71 -> "71" for %).
        input.setText(ParameterFormatter.formatNumber(dCur));
        input.setTextColor(Color.WHITE);
        input.setTextSize(18);
        input.setSelectAllOnFocus(true);
        container.addView(input);
        
        builder.setTitle("Set " + controlName)
               .setView(container)
               .setPositiveButton("SET", (dialog, which) -> {
                    String text = input.getText().toString().trim().replace(',', '.');
                    try {
                        float enteredDisplay = Float.parseFloat(text);
                        float clampedDisplay = Math.max(dMin, Math.min(dMax, enteredDisplay));
                        float rawVal = pctScaled ? clampedDisplay / 100f : clampedDisplay;
                        rawVal = Math.max(minVal, Math.min(maxVal, rawVal));
                        if (listener != null) listener.onValueSet(rawVal);
                    } catch (Exception e) {
                        toast("Invalid number format");
                    }
                })
               .setNegativeButton("CANCEL", null);
               
         androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
         dialog.show();
         input.setTextColor(Color.WHITE);
         input.setHintTextColor(Color.parseColor("#8F94A0"));
         input.requestFocus();
    }

    interface OnManualValueListener {
        void onValueSet(float value);
    }

    private static int nearestEnumIndex(float value, float[] values) {
        if (values == null || values.length == 0) return 0;
        int best = 0;
        float distance = Math.abs(value - values[0]);
        for (int i = 1; i < values.length; i++) {
            float next = Math.abs(value - values[i]);
            if (next < distance) {
                distance = next;
                best = i;
            }
        }
        return best;
    }


    void loadPreset(Map map) {
        if (map == null) {
            MainActivity.toast("Cannot load preset: preset is null");
            return;
        }

        // schema-v1 presets (LocalPresetManager / factory rigs) carry a "plugins" array.
        if (map.containsKey("plugins")) {
            loadSchemaV1Preset(map);
            return;
        }

        // legacy map form: {name, desc, uid, path, controls:{...}} or scene map
        JSONObject jsonObject = new JSONObject(map);
        String controls;
        try {
            if (jsonObject.has("controls")) {
                controls = (String) jsonObject.get("controls").toString();
                loadPreset(controls.toString());
            } else {
                // Scene map directly contains keys "0", "1", ... and metadata
                loadPreset(jsonObject.toString());
            }
        } catch (JSONException e) {
            MainActivity.toast("Cannot load preset: " + e.getMessage());
            e.printStackTrace();
        }


    }

    /**
     * Load a schema-v1 preset (Map produced by Gson from LocalPresetManager.presetToJson).
     * Converts the "plugins" array into the legacy serialized JSON string expected by
     * {@link #loadPreset(String)}.
     */
    void loadSchemaV1Preset(Map map) {
        JSONObject legacy = new JSONObject();
        try {
            Object pluginsObj = map.get("plugins");
            if (!(pluginsObj instanceof java.util.List)) {
                MainActivity.toast("Cannot load preset: no plugins");
                return;
            }

            java.util.List<?> plugins = (java.util.List<?>) pluginsObj;
            int plugin = 0;
            for (Object o : plugins) {
                if (!(o instanceof Map))
                    continue;
                Map<?, ?> p = (Map<?, ?>) o;

                JSONObject jo = new JSONObject();
                Object nameObj = p.get("name");
                if (nameObj == null) {
                    Log.e(TAG, "loadSchemaV1Preset: plugin without name, skipping");
                    continue;
                }
                jo.put("name", nameObj.toString());
                // Bypassed blocks still load (models, knobs, files) — the bypass
                // itself is re-applied by loadPreset(String) after the build.
                // Dropping them here deleted bypassed blocks on every restore.
                jo.put("enabled", !Boolean.FALSE.equals(p.get("enabled")));

                StringBuilder controls = new StringBuilder();
                Object controlsObj = p.get("controls");
                if (controlsObj instanceof java.util.List) {
                    java.util.List<?> ctrl = (java.util.List<?>) controlsObj;
                    for (int i = 0; i < ctrl.size(); i++) {
                        Object c = ctrl.get(i);
                        if (c instanceof Map) {
                            Object value = ((Map<?, ?>) c).get("value");
                            if (i > 0) controls.append(";");
                            controls.append(value != null ? value.toString() : "0.0");
                        }
                    }
                }
                jo.put("controls", controls.toString());

                Object fpObj = p.get("file_ports");
                if (fpObj instanceof java.util.List) {
                    java.util.List<?> fp = (java.util.List<?>) fpObj;
                    if (!fp.isEmpty() && fp.get(0) instanceof Map) {
                        Object rel = ((Map<?, ?>) fp.get(0)).get("relative_path");
                        if (rel != null && !rel.toString().isEmpty())
                            jo.put("audioFile", rel.toString());
                    }
                }
                // Preserve TONE3000 origin metadata for missing-file handling & badge restore
                for (String k : new String[]{"tone3000ToneId","tone3000ModelId","tone3000Creator","tone3000ToneTitle","tone3000Format","tone3000GearType","tone3000SourceUrl"}) {
                    Object v = p.get(k);
                    if (v != null && !v.toString().isEmpty()) {
                        try { jo.put(k, v.toString()); } catch (Exception ignore) {}
                    }
                }

                legacy.put(String.valueOf(plugin), jo.toString());
                // Keep pending T3K meta for post-load badge + missing check
                try {
                    JSONObject t3k = new JSONObject();
                    boolean hasT3k = false;
                    for (String k : new String[]{"tone3000ToneId","tone3000ModelId","tone3000Creator","tone3000ToneTitle","tone3000Format","tone3000GearType","tone3000SourceUrl"}) {
                        Object v = p.get(k);
                        if (v != null && !v.toString().isEmpty()) { t3k.put(k, v.toString()); hasT3k = true; }
                    }
                    if (hasT3k) pendingT3kLoad.put(plugin, t3k);
                    // also keep file path for existence check
                    Object fp0 = p.get("file_ports");
                    if (fp0 instanceof java.util.List) {
                        java.util.List<?> fpl = (java.util.List<?>) fp0;
                        if (!fpl.isEmpty() && fpl.get(0) instanceof Map) {
                            Object rp = ((Map<?,?>)fpl.get(0)).get("relative_path");
                            if (rp != null) pendingT3kFile.put(plugin, rp.toString());
                        }
                    } else if (fpObj instanceof java.util.List) {
                        // already handled above — also keep for pending check
                        java.util.List<?> fpl2 = (java.util.List<?>) fpObj;
                        if (!fpl2.isEmpty() && fpl2.get(0) instanceof Map) {
                            Object rp = ((Map<?,?>)fpl2.get(0)).get("relative_path");
                            if (rp != null) pendingT3kFile.put(plugin, rp.toString());
                        }
                    }
                } catch (Exception ignore) {}
                plugin++;
            }

            // Carry over graph edges & node positions if present in map
            Object gSrc = map.containsKey("graph") ? map.get("graph") : map.get("graph_meta");
            if (gSrc instanceof Map && ((Map<?, ?>) gSrc).containsKey("graph")) {
                gSrc = ((Map<?, ?>) gSrc).get("graph");
            }
            if (gSrc instanceof java.util.List) {
                legacy.put("graph", new JSONArray((java.util.List<?>) gSrc));
            }
            if (map.containsKey("node_positions") && map.get("node_positions") instanceof Map) {
                legacy.put("node_positions", new JSONObject((Map<?, ?>) map.get("node_positions")));
            }
        } catch (JSONException e) {
            MainActivity.toast("Cannot load preset: " + e.getMessage());
            Log.e(TAG, "loadSchemaV1Preset: ", e);
            return;
        }

        Log.d(TAG, "loadSchemaV1Preset: " + legacy.toString());
        loadPreset(legacy.toString());
    }

    void loadPreset(String preset) {
        // Bypass is released in finally: any exception escaping the body used
        // to wedge bypass stuck true = whole chain silent until the next load.
        AudioEngine.bypass(true);
        try {
            loadPresetInner(preset);
        } finally {
            AudioEngine.bypass(false);
        }
    }

    private void loadPresetInner(String preset) {
        // forgot to add this. that this was forgotten was very difficult to guess
        AudioEngine.bypass(true);
        AudioEngine.clearActiveQueue();
        resetPluginMIDI();
        // TONE3000 session state is PER-PRESET: stale meta/paths from the
        // previously loaded preset must never survive a load — otherwise the
        // save fallback injects the old T3K model into EVERY following preset
        // whose amp block has no engine-read-back file.
        t3kBlockMeta.clear();
        t3kModelPath.clear();

        Log.d(TAG, "loadPreset: " + preset);
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(preset);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadPreset: Unable to load preset\n" + preset, e);
            AudioEngine.bypass(false);
            return;
        }

        int items = dataAdapter.totalItems;
        if (items > 0) {
            Log.d(TAG, "loadPreset: already loaded something, deleting ...");
//            dataAdapter.deleteAll();
            dataAdapter.reset();
        }

        if (jsonObject.has("routingMode")) {
            int rMode = jsonObject.optInt("routingMode", 0);
            AudioEngine.setRoutingMode(rMode);
            updateRoutingUI(rMode);
        }

        final List<Integer> savedGraphEdges = new ArrayList<>();
        // Engine slots of blocks saved as bypassed (re-applied after build).
        final List<Integer> disabledSlots = new ArrayList<>();
        if (jsonObject.has("graph")) {
            JSONArray graphArr = jsonObject.optJSONArray("graph");
            if (graphArr != null && graphArr.length() % 2 == 0) {
                int half = graphArr.length() / 2;
                int[] from = new int[half];
                int[] to = new int[half];
                boolean ok = true;
                for (int i = 0; i < half; i++) {
                    try {
                        from[i] = graphArr.getInt(i * 2);
                        to[i] = graphArr.getInt(i * 2 + 1);
                        savedGraphEdges.add(from[i]);
                        savedGraphEdges.add(to[i]);
                    } catch (JSONException e) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    AudioEngine.setGraph(from, to);
                    Log.d(TAG, "loadPreset: restored graph edges (" + half + ")");
                }
            }
        }

        final Map<String, float[]> customNodePos = new HashMap<>();
        if (jsonObject.has("node_positions")) {
            JSONObject np = jsonObject.optJSONObject("node_positions");
            if (np != null) {
                Iterator<String> keys = np.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONArray arr = np.optJSONArray(k);
                    if (arr != null && arr.length() >= 2) {
                        customNodePos.put(k, new float[]{(float) arr.optDouble(0, 0), (float) arr.optDouble(1, 0)});
                    }
                }
            }
        }

        int plugin = 0;
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            // Issue 1 Fix: Skip non-plugin metadata keys (e.g. routingMode, name, schema_version)
            if (!key.matches("\\d+")) {
                continue;
            }

            JSONObject jo;
            try {
                Log.d(TAG, "loadPreset: trying preset " + key + ": " + jsonObject.getString(key));
                jo = new JSONObject(jsonObject.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse key: " + key, e);
                continue;
            }

            String name, controls;
            int spinnerValue = -1 ;
            String audioFile = null ;
            String audioFileR = null ;
            float irBlend = 0.5f ;
            boolean enabled = true ;
            try {
                name = jo.getString("name");
                controls = jo.getString("controls");

                if (jo.has("midi") && ! jo.getString("midi").isEmpty()) {
                    JSONArray jsonArray ;
                    String _s = jo.getString("midi");
                    if (! _s.contains(";"))
                        jsonArray = jo.getJSONArray("midi");
                    else {
                        jsonArray = new JSONArray();
                        String [] v = _s.split("\\|");
                        for (String s : v) {
                            if (s.isEmpty())
                                continue;

                            String[] vv = s.split(";");
                            JSONObject _j = new JSONObject();
                            _j.put("plugin", vv[0]);
                            _j.put("pluginControl", vv[1]);
                            _j.put("channel", vv[2]);
                            _j.put("control", vv[3]);
                            jsonArray.put(_j);
                        }

                        Log.i(TAG, "loadPreset: firestore midi controls for " + plugin + " " + jsonArray.toString());
                    }

                    for (int d = 0 ; d < jsonArray.length(); d ++) {
                        JSONObject jControl = jsonArray.getJSONObject(d);
                        MIDIControl midiControl = new MIDIControl();
                        midiControl.plugin = plugin ;
                        midiControl.pluginControl = jControl.getInt("pluginControl");
                        midiControl.scope = MIDIControl.Scope.PLUGIN ;
                        midiControl.channel = jControl.getInt("channel");
                        midiControl.control = jControl.getInt("control");
                        midiControls.add(midiControl);
                        Log.i(TAG, "loadPreset: added midi control " + midiControl.get());
                    }
                }

                if (jo.has("selectedModel"))
                    spinnerValue = jo.getInt("selectedModel");
                if (jo.has("audioFile"))
                    audioFile = jo.getString("audioFile");
                if (jo.has("audioFileR"))
                    audioFileR = jo.getString("audioFileR");
                if (jo.has("irBlend"))
                    irBlend = (float) jo.getDouble("irBlend");
                // schema-v1 snapshots carry per-block bypass (default: enabled)
                enabled = jo.optBoolean("enabled", true);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse name or controls for key: " + key, e);
                continue;
            }

            int ret = -1;
            if (!lazyLoad)
                AudioEngine.addPluginByName(name);
            else
                addPluginByName(name);
            Log.d(TAG, "loadPreset: Loaded plugin: " + name);
            String[] control = controls.split(";");


            // Holders populate asynchronously after layout; during bulk load they
            // are legitimately empty and sliders bind on the adapter refresh
            // at the end of loadPreset. Never touch the adapter here: this
            // method can run off the UI thread (preset callbacks), where
            // notifyDataSetChanged() throws CalledFromWrongThreadException.
            DataAdapter.ViewHolder holder = null;
            try {
                if (plugin >= 0 && plugin < dataAdapter.holders.size()) {
                    holder = dataAdapter.holders.get(plugin);
                }
            } catch (Exception e) {
                Log.w(TAG, "loadPreset: holder lookup failed for " + plugin, e);
            }
            if (holder == null) {
                Log.d(TAG, "loadPreset: no holder yet for " + plugin + " (binds on adapter refresh)");
            }


            // Schema-v1 presets may carry an empty controls string: "".split(";")
            // yields [""], whose parse throws. Skip quietly and keep defaults.
            if (controls == null || controls.trim().isEmpty()) {
                Log.d(TAG, "loadPreset: no saved controls, keeping plugin defaults");
            } else {
            Log.d(TAG, "loadPreset: loading " + control.length + " controls from " + controls);
            for (int i = 0; i < control.length; i++) {
                String token = control[i] == null ? "" : control[i].trim();
                if (token.isEmpty()) continue;
                Log.d(TAG, "loadPreset: " + i + ": " + token);
                //                holder.sliders.get(i).setValue(Integer.parseInt(control [i]));
                float savedValue = -6906;// aaaaargh
                try {
                    savedValue = Float.parseFloat(token);
                } catch (Exception e) {
                    Log.e(TAG, "loadPreset: cannot load saved float value for " + token, e);
                    continue;
                }

                AudioEngine.setPresetValue(plugin, i, savedValue);
                // forgot this too
                AudioEngine.setPluginControl(plugin, i, savedValue);
            }
            } // end non-empty controls

            int pos = dataAdapter.getItemCount();

            if (spinnerValue > -1 )
                dataAdapter.setSelectedModel(plugin, spinnerValue);
            if (audioFile != null)
                dataAdapter.setAudioFiles(plugin, audioFile);
            if (audioFileR != null)
                dataAdapter.setAudioFilesR(plugin, audioFileR);
            if (!enabled) disabledSlots.add(plugin);

            plugin++;

//            holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(dataAdapter.getItemCount() - 1);
//            if (holder.hasFileSpinner) {
//                holder.modelSpinner.setSelection(spinnerValue);
//            }
        }

        AudioEngine.bypass(false);

        // Re-apply per-block bypass from schema-v1 snapshots (slots == load
        // order; fresh adds default to enabled). Legacy snapshots carry no
        // "enabled" key so this loop is a no-op for them.
        for (int s : disabledSlots) {
            try {
                if (s >= 0 && s < AudioEngine.getActivePlugins()) AudioEngine.togglePlugin(s, false);
            } catch (Exception e) {
                Log.w(TAG, "loadPreset: bypass restore failed for slot " + s, e);
            }
        }

        // Refresh visual chain and select the first active block in the editor
        if (signalChainCanvas != null) {
            if (!customNodePos.isEmpty()) {
                signalChainCanvas.setPendingCustomPositions(customNodePos);
            }
            signalChainCanvas.postDelayed(() -> {
                signalChainCanvas.loadFromActivePlugins();
                if (!savedGraphEdges.isEmpty()) {
                    int half = savedGraphEdges.size() / 2;
                    int[] from = new int[half];
                    int[] to = new int[half];
                    for (int i = 0; i < half; i++) {
                        from[i] = savedGraphEdges.get(i * 2);
                        to[i] = savedGraphEdges.get(i * 2 + 1);
                    }
                    signalChainCanvas.setGraphEdges(from, to);
                }
                if (!customNodePos.isEmpty()) {
                    signalChainCanvas.setCustomNodePositions(customNodePos);
                    signalChainCanvas.setPendingCustomPositions(customNodePos);
                }
                int activeCount = AudioEngine.getActivePlugins();
                if (activeCount > 0) {
                    populateInlineParameterEditor(0, AudioEngine.getActivePluginName(0));
                }
            }, 300);
        }
        if (dataAdapter != null) {
            dataAdapter.notifyDataSetChanged();
        }

        // TONE3000: restore badges from preset + check missing files (post, after canvas rebuild)
        if (!pendingT3kLoad.isEmpty() || !pendingT3kFile.isEmpty()) {
            final java.util.Map<Integer, JSONObject> copyMeta = new java.util.HashMap<>(pendingT3kLoad);
            final java.util.Map<Integer, String> copyFile = new java.util.HashMap<>(pendingT3kFile);
            pendingT3kLoad.clear();
            pendingT3kFile.clear();
            // merge into live maps for future saves
            for (java.util.Map.Entry<Integer, JSONObject> e : copyMeta.entrySet()) {
                t3kBlockMeta.put(e.getKey(), e.getValue());
                // file path for that slot
                String fp = copyFile.get(e.getKey());
                if (fp != null) t3kModelPath.put(e.getKey(), fp);
            }
            // also for entries that only have file path but no meta (legacy)
            for (java.util.Map.Entry<Integer, String> e : copyFile.entrySet()) {
                if (!t3kModelPath.containsKey(e.getKey())) t3kModelPath.put(e.getKey(), e.getValue());
            }
            // after canvas rebuild, apply badges + missing check (600ms to ensure loadFromActivePlugins done)
            if (signalChainCanvas != null) {
                signalChainCanvas.postDelayed(() -> {
                    // Use CURRENT t3kBlockMeta (may have been shifted by a delete that
                    // ran between load and this re-apply) instead of the snapshot copyMeta.
                    // Gate on engine truth: stale meta must never badge a block
                    // that cannot hold a model (the badge-on-non-NAM bug).
                    for (Integer slot : t3kBlockMeta.keySet()) {
                        try {
                            if (slot != null && canLoadNamModel(slot))
                                signalChainCanvas.setT3kBadge(slot, true);
                            else
                                Log.w(TAG, "skipping T3K badge on non-loader slot " + slot);
                        } catch (Exception ignore) {}
                    }
                    // missing file check: if T3K toneId present but local file absent, prompt
                    // Use current t3kBlockMeta (same keys as t3kModelPath after any shift).
                    for (java.util.Map.Entry<Integer, JSONObject> e : t3kBlockMeta.entrySet()) {
                        int slot = e.getKey();
                        String path = t3kModelPath.get(slot);
                        boolean missing = true;
                        if (path != null) {
                            try { missing = !new java.io.File(path).exists(); } catch (Exception ignore) { missing = true; }
                            // also try sidecar model file via t3kModelPath
                            if (missing) {
                                String alt = t3kModelPath.get(slot);
                                if (alt != null) try { missing = !new java.io.File(alt).exists(); } catch (Exception ignore) {}
                            }
                        }
                        if (missing) {
                            try {
                                JSONObject meta = e.getValue();
                                LocalPresetManager.PluginItem dummy = new LocalPresetManager.PluginItem();
                                dummy.tone3000ToneId = meta.optString("tone3000ToneId", null);
                                dummy.tone3000ToneTitle = meta.optString("tone3000ToneTitle", null);
                                dummy.tone3000Creator = meta.optString("tone3000Creator", null);
                                dummy.name = dummy.tone3000ToneTitle != null ? dummy.tone3000ToneTitle : "TONE3000";
                                handleMissingTone3000Model(dummy, slot);
                                break; // show one at a time
                            } catch (Exception ignore) {}
                        }
                    }
                }, 650);
            }
        }
    }

    void syncTopBarPresetInfo() {
        syncTopBarPresetInfo(-1);
    }

    void syncTopBarPresetInfo(int explicitNumber) {
        if (patchName == null) return;
        TextView tn = findViewById(R.id.top_preset_display);
        if (tn != null) {
            int num = explicitNumber > 0 ? explicitNumber : 1;
            if (explicitNumber <= 0 && dataAdapter != null) {
                int sel = dataAdapter.getSelectedPosition();
                if (sel >= 0) num = sel + 1;
            }
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            String numStr = String.valueOf(num) + ". ";
            int start = sb.length();
            sb.append(numStr);
            sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    start, start + numStr.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FFAA00")),
                    start, start + numStr.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            String pText = patchName.getText() != null ? patchName.getText().toString() : "CLEAN";
            // Strip any legacy prefix like "2 GIGGIN' GEAR" if present to avoid duplicate numbers
            if (pText.matches("^\\d+\\s+.*")) {
                pText = pText.replaceFirst("^\\d+\\s+", "");
            }
            sb.append(pText);
            tn.setText(sb);
        }
    }

    // ---- BPM: real app-level tempo state driving time-based effect controls ----
    public static final String PREF_BPM = "giggingear_bpm";
    public static final String PREF_BPM_DIV = "giggingear_bpm_div";
    private float bpmValue = 120f;
    private float bpmDivision = 1f; // multiplier of quarter note (0.5=8th, 0.75=dotted 8th, 2=half)

    public float getBpm() { return bpmValue; }

    public void setBpm(float bpm) {
        bpmValue = bpm;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putFloat(PREF_BPM, bpm).apply();
        syncTimeBlocksToBpm(bpm);
        TextView chip = findViewById(R.id.top_bpm_text);
        if (chip != null) chip.setText((int) bpm + " BPM");
    }

    public String getBpmDisplay() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        bpmValue = sp.getFloat(PREF_BPM, 120f);
        bpmDivision = sp.getFloat(PREF_BPM_DIV, 1f);
        return (int) bpmValue + " BPM";
    }

    /** Push new tempo into every delay-time control of active blocks (real engine values). */
    public void syncTimeBlocksToBpm(float bpm) {
        try {
            int n = AudioEngine.getActivePlugins();
            float quarterSec = 60f / Math.max(30f, Math.min(300f, bpm));
            int hits = 0;
            for (int p = 0; p < n; p++) {
                int controls;
                try { controls = AudioEngine.getPluginControls(p); } catch (Exception e) { continue; }
                String blockName;
                try { blockName = AudioEngine.getActivePluginName(p); } catch (Exception e) { blockName = "?"; }
                for (int c = 0; c < controls; c++) {
                    String unit;
                    try { unit = AudioEngine.getControlUnit(p, c); } catch (Exception e) { continue; }
                    // Tempo sync only targets ports whose authoritative metadata
                    // declares seconds or milliseconds. Names are not semantic data.
                    if (!("s".equals(unit) || "ms".equals(unit))) continue;
                    float val = quarterSec * bpmDivision;
                    float[] bounds = AudioEngine.getPluginControlValues(p, c);
                    if (bounds == null || bounds.length < 3) continue;
                    val = "ms".equals(unit) ? val * 1000f : val;
                    val = Math.max(bounds[1], Math.min(bounds[2], val));
                    try {
                        AudioEngine.setPluginControl(p, c, val);
                        hits++;
                        Log.i(TAG, "syncTimeBlocksToBpm: " + blockName + " control=" + c + " -> " + val + unit);
                    } catch (Exception e) {
                        Log.w(TAG, "bpm set failed control=" + c, e);
                    }
                }
            }
            if (hits > 0) {
                toast("Tempo synced to " + hits + " delay control(s)");
            } else {
                Log.d(TAG, "syncTimeBlocksToBpm: no delay blocks to sync");
            }
        } catch (Exception e) {
            Log.w(TAG, "syncTimeBlocksToBpm failed", e);
        }
    }

    /** BPM dialog: slider + tap-tempo + note-division selector for delay sync. */
    public void showBpmDialog() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final float[] currentBpm = {sp.getFloat(PREF_BPM, 120f)};
        final long[] taps = {-1};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        final com.google.android.material.slider.Slider slider =
                new com.google.android.material.slider.Slider(this);
        slider.setValueFrom(40);
        slider.setValueTo(240);
        slider.setStepSize(1);
        slider.setValue(currentBpm[0]);
        slider.setTrackActiveTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        slider.setTrackInactiveTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#444444")));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        slider.setTrackHeight(16);
        slider.setThumbRadius(16);
        box.addView(slider);

        TextView readout = new TextView(this);
        readout.setText(String.valueOf((int) currentBpm[0]));
        readout.setTextSize(34);
        readout.setGravity(android.view.Gravity.CENTER);
        readout.setTextColor(Color.parseColor("#FFFFFF"));
        box.addView(readout);

        slider.addOnChangeListener((s, value, fromUser) -> {
            currentBpm[0] = value;
            readout.setText(String.valueOf((int) value));
        });

        android.widget.Button tapBtn = new android.widget.Button(this);
        tapBtn.setText("TAP TEMPO");
        tapBtn.setOnClickListener(v -> {
            long now = android.os.SystemClock.elapsedRealtime();
            if (taps[0] > 0 && now - taps[0] < 2500) {
                long interval = now - taps[0];
                float tapped = 60000f / interval;
                tapped = Math.max(40f, Math.min(240f, tapped));
                currentBpm[0] = Math.round(tapped);
                slider.setValue(currentBpm[0]);
            }
            taps[0] = now;
        });
        box.addView(tapBtn);

        String[] divisions = {"Half", "Quarter", "Dotted 8th", "Eighth", "16th"};
        float[] mults = {2f, 1f, 0.75f, 0.5f, 0.25f};
        int preselect = 1;
        for (int i = 0; i < mults.length; i++)
            if (Math.abs(mults[i] - bpmDivision) < 0.01f) { preselect = i; break; }
        android.widget.Spinner divSpin = new android.widget.Spinner(this);
        divSpin.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, divisions));
        divSpin.setSelection(preselect);
        android.widget.TextView divLabel = new android.widget.TextView(this);
        divLabel.setText("Delay sync division");
        divLabel.setTextSize(12);
        divLabel.setTextColor(Color.parseColor("#8F94A0"));
        box.addView(divLabel);
        box.addView(divSpin);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tempo")
                .setView(box)
                .setPositiveButton("Apply", (d, w) -> {
                    float bpm = currentBpm[0];
                    float mult = mults[divSpin.getSelectedItemPosition()];
                    sp.edit().putFloat(PREF_BPM, bpm).putFloat(PREF_BPM_DIV, mult).apply();
                    bpmDivision = mult;
                    TextView chip = findViewById(R.id.top_bpm_text);
                    if (chip != null) chip.setText((int) bpm + " BPM");
                    syncTimeBlocksToBpm(bpm);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Lookup full description from all_plugins.json or lv2_plugins.json */
    public String getPluginFullDescription(String pluginName) {
        if (pluginName == null) return null;
        try {
            if (availablePlugins != null) {
                Iterator<String> keys = availablePlugins.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject o = availablePlugins.optJSONObject(k);
                    if (o != null && pluginName.equalsIgnoreCase(o.optString("name"))) {
                        String d = o.optString("desc", o.optString("description", null));
                        if (d != null && !d.trim().isEmpty()) return d;
                    }
                }
            }
            if (availablePluginsLV2 != null) {
                Iterator<String> keys = availablePluginsLV2.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject o = availablePluginsLV2.optJSONObject(k);
                    if (o != null && pluginName.equalsIgnoreCase(o.optString("name"))) {
                        String d = o.optString("desc", o.optString("description", null));
                        if (d != null && !d.trim().isEmpty()) return d;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getPluginFullDescription failed for " + pluginName, e);
        }
        return null;
    }

    /** Find the atom/file-port control index of a block (for setAtomPort model loading). */
    private int resolveAtomControl(int plugin) {
        try {
            int controls = AudioEngine.getPluginControls(plugin);
            // Heuristic by control name: NAM/LV2 file ports are named Model/File/nam
            for (int c = 0; c < controls; c++) {
                String cn;
                try { cn = AudioEngine.getControlName(plugin, c); } catch (Exception e) { continue; }
                if (cn == null) continue;
                String s = cn.toLowerCase();
                if (s.contains("file") || s.contains("model") || s.equals("nam") || s.contains("config")) {
                    Log.i(TAG, "resolveAtomControl: plugin " + plugin + " -> control " + c + " (" + cn + ")");
                    return c;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "resolveAtomControl failed for " + plugin, e);
        }
        return 0; // fallback: single-file-port plugins typically use index 0
    }

    /** Apply an already-on-disk model file to the given active block via its atom port. */
    /** Engine truth: can a model file actually attach to this slot? Name/category
     *  heuristics used to claim every AMP block was NAM-capable (wrong subtitle,
     *  badge on non-NAM blocks, doomed setPluginFilename calls). */
    private boolean canLoadNamModel(int plugin) {
        try {
            return AudioEngine.canLoadModelFile(plugin);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean canLoadIrModel(int plugin) {
        try {
            if (plugin < 0 || plugin >= AudioEngine.getActivePlugins()) return false;
            String n = AudioEngine.getActivePluginName(plugin);
            if (n == null) return false;
            String ln = n.toLowerCase();
            CategoryHelper.Category c = CategoryHelper.getCategory(n);
            return ln.contains("ir") || ln.contains("convolution") || ln.contains("cab")
                    || ln.contains("reverb") || c == CategoryHelper.Category.CABINET;
        } catch (Throwable t) {
            return false;
        }
    }

    private void applyLocalModelToPlugin(int plugin, File srcFile) {
        try {
            String dirName = AudioEngine.getActivePluginName(plugin);
            if (dirName == null) dirName = "Model" + plugin;
            File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), dirName);
            if (!dir.exists() && !dir.mkdirs()) {
                toast("Cannot create model directory");
                return;
            }
            File dest = new File(dir, srcFile.getName());
            if (!srcFile.getAbsolutePath().equals(dest.getAbsolutePath())) {
                java.nio.file.Files.copy(srcFile.toPath(), dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // Correct engine API: file-path ports (Plugin::setFileName)
            if (!canLoadNamModel(plugin)) {
                toast("Only AMP/CAB blocks can load .nam models");
                return;
            }
            String err3 = AudioEngine.setPluginFilename(dest.getAbsolutePath(), plugin);
            if (err3 != null) {
                toast("Model load failed: " + err3);
                Log.w(TAG, "[load model] local FAILED -> plugin " + plugin + " err: " + err3);
            } else {
                Log.i(TAG, "[load model] local -> plugin " + plugin + " file " + dest.getAbsolutePath());
                toast("Model loaded: " + srcFile.getName());
                if (signalChainCanvas != null) signalChainCanvas.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "applyLocalModelToPlugin failed", e);
            toast("Failed to load model: " + e.getMessage());
        }
    }

    /**
     * Modern Two-Column Model Browser Dialog:
     * - Left column: categories with icons and count/name
     * - Right column: model cards with thumbnail, brand/name, and star favorites
     */
    public void showModelPickerDialog(int position) {
        String pluginName;
        try { pluginName = AudioEngine.getActivePluginName(position); }
        catch (Exception e) { pluginName = null; }
        if (pluginName == null) pluginName = "Plugin " + position;

        CategoryHelper.Category currentCat = CategoryHelper.getCategory(pluginName);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_model_browser)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ImageButton closeX = dialog.findViewById(R.id.dialog_model_close_x);
        com.google.android.material.button.MaterialButton closeBtn = dialog.findViewById(R.id.dialog_model_close_btn);
        LinearLayout catContainer = dialog.findViewById(R.id.category_container);
        LinearLayout modelsContainer = dialog.findViewById(R.id.models_container);

        if (closeX != null) closeX.setOnClickListener(v -> dialog.dismiss());
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Category List Definitions — must cover every CategoryHelper family,
        // otherwise classified plugins become unreachable in the browser.
        final CategoryHelper.Category[] categories = {
                CategoryHelper.Category.AMP,
                CategoryHelper.Category.CABINET,
                CategoryHelper.Category.DISTORTION,
                CategoryHelper.Category.DYNAMICS,
                CategoryHelper.Category.MODULATION,
                CategoryHelper.Category.DELAY,
                CategoryHelper.Category.REVERB,
                CategoryHelper.Category.EQ,
                CategoryHelper.Category.PITCH,
                CategoryHelper.Category.WAH,
                CategoryHelper.Category.UTILITY,
                CategoryHelper.Category.GATE
        };

        final String[] catLabels = {
                "Amp / Head", "Cabinet", "Overdrive / Dist", "Compressor",
                "Modulation", "Delay", "Reverb", "EQ", "Pitch / Synth",
                "Wah", "Volume / Utility", "Noise Gate"
        };

        final Runnable[] populateModelsForCat = new Runnable[1];
        // Active category holder — drives the global search fallback (empty query = this cat).
        final CategoryHelper.Category[] activeModelCat = {currentCat};

        if (catContainer != null) {
            catContainer.removeAllViews();
            for (int i = 0; i < categories.length; i++) {
                final int catIdx = i;
                final CategoryHelper.Category cat = categories[i];
                View catView = getLayoutInflater().inflate(R.layout.item_model_category_row, catContainer, false);
                ImageView icon = catView.findViewById(R.id.item_cat_icon);
                TextView title = catView.findViewById(R.id.item_cat_title);

                title.setText(catLabels[i]);
                int catColor = CategoryHelper.getColor(cat.name());
                icon.setImageResource(CategoryHelper.getIconRes(cat.name()));
                icon.setColorFilter(catColor, android.graphics.PorterDuff.Mode.SRC_IN);

                boolean isSelected = (cat == currentCat);
                catView.setBackgroundResource(isSelected ? R.drawable.glass_category_selected_bg : R.drawable.glass_card_bg);

                catView.setOnClickListener(v -> {
                    for (int c = 0; c < catContainer.getChildCount(); c++) {
                        catContainer.getChildAt(c).setBackgroundResource(R.drawable.glass_card_bg);
                    }
                    catView.setBackgroundResource(R.drawable.glass_category_selected_bg);
                    activeModelCat[0] = cat;
                    EditText si = dialog.findViewById(R.id.model_search_input);
                    if (si != null && si.getText().length() > 0) {
                        si.setText(""); // watcher repopulates with the new category
                    } else {
                        loadModelsIntoContainer(modelsContainer, cat, position, dialog);
                    }
                });

                catContainer.addView(catView);
            }
        }

        // Initially populate for the active block's category
        if (modelsContainer != null) {
            loadModelsIntoContainer(modelsContainer, currentCat, position, dialog);
        }

        // Wire GLOBAL search: non-empty query searches every category,
        // empty query falls back to the active category.
        EditText searchInput = dialog.findViewById(R.id.model_search_input);
        ImageButton searchClear = dialog.findViewById(R.id.model_search_clear);
        if (searchInput != null && modelsContainer != null) {
            if (searchClear != null) {
                searchClear.setOnClickListener(v -> searchInput.setText(""));
            }
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim().toLowerCase();
                    if (searchClear != null) {
                        searchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (query.isEmpty()) {
                        loadModelsIntoContainer(modelsContainer, activeModelCat[0], position, dialog);
                    } else {
                        loadModelsMatchingQuery(modelsContainer, query, position, dialog, categories);
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    public void showPluginPickerDialog() {
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_plugin_picker)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ImageButton closeX = dialog.findViewById(R.id.picker_close_btn);
        com.google.android.material.button.MaterialButton closeBtn = dialog.findViewById(R.id.picker_bottom_close_btn);
        LinearLayout catContainer = dialog.findViewById(R.id.picker_cat_container);
        LinearLayout itemsContainer = dialog.findViewById(R.id.picker_items_container);
        LinearLayout t3kContainer = dialog.findViewById(R.id.t3k_entry_container);

        if (closeX != null) closeX.setOnClickListener(v -> dialog.dismiss());
        if (closeBtn != null) closeBtn.setOnClickListener(v -> dialog.dismiss());

        // TONE3000 entry point — glass-styled, full-logo context, below ADD header (spec §5/6/25)
        if (t3kContainer != null) try {
            t3kContainer.setVisibility(View.VISIBLE);
            t3kContainer.removeAllViews();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(Math.round(10 * getResources().getDisplayMetrics().density), Math.round(10 * getResources().getDisplayMetrics().density), Math.round(8 * getResources().getDisplayMetrics().density), Math.round(10 * getResources().getDisplayMetrics().density));
            row.setBackgroundResource(R.drawable.glass_card_bg);
            row.setClickable(true);
            row.setFocusable(true);
            try {
                android.util.TypedValue tv = new android.util.TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
                if (tv.resourceId != 0) row.setForeground(getDrawable(tv.resourceId));
            } catch (Exception ignore) {}

            // Left: TONE3000 text-logo block + description
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView logo = new TextView(this);
            logo.setText("TONE3000");
            logo.setTextColor(Color.parseColor("#FF8A00"));
            logo.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            logo.setLetterSpacing(0.08f);
            TextView sub = new TextView(this);
            sub.setText("Browse TONE3000  •  NAM captures & IRs");
            sub.setTextColor(Color.parseColor("#E6E6E6"));
            sub.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f);
            sub.setPadding(0, Math.round(2 * getResources().getDisplayMetrics().density), 0, 0);
            TextView hint = new TextView(this);
            hint.setText("Giggin' Gear integrates with TONE3000 — massive community library");
            hint.setTextColor(Color.parseColor("#A1A1AA"));
            hint.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f);
            hint.setPadding(0, Math.round(2 * getResources().getDisplayMetrics().density), 0, 0);
            left.addView(logo); left.addView(sub); left.addView(hint);

            com.google.android.material.button.MaterialButton browse = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
            browse.setText("Browse");
            browse.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f);
            browse.setCornerRadius(Math.round(8 * getResources().getDisplayMetrics().density));
            browse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8A00")));
            browse.setTextColor(Color.WHITE);
            browse.setPadding(Math.round(14 * getResources().getDisplayMetrics().density), 0, Math.round(14 * getResources().getDisplayMetrics().density), 0);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Math.round(36 * getResources().getDisplayMetrics().density));
            bp.gravity = Gravity.CENTER_HORIZONTAL;
            bp.setMarginStart(0);
            browse.setLayoutParams(bp);

            View.OnClickListener goT3k = v -> {
                dialog.dismiss();
                // If not authenticated, show partnership sheet first; else go straight to Select
                if (!Tone3000Config.isConfigured(this)) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("TONE3000 not configured")
                        .setMessage("Missing TONE3000_CLIENT_ID.\n\nAdd it via BuildConfig (gradle -P TONE3000_CLIENT_ID=xxx) or AndroidManifest meta-data com.kancrutt.gigginggear.TONE3000_CLIENT_ID.\n\nGet your publishable key at tone3000.com/settings → API Keys.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }
                // Show partnership/entry sheet (spec §6) — it handles PKCE + Custom Tabs
                try {
                    Tone3000EntrySheet sheet = Tone3000EntrySheet.newInstance();
                    sheet.show(getSupportFragmentManager(), "t3k_entry");
                } catch (Exception e) {
                    // fallback: direct Custom Tabs launch
                    try {
                        Tone3000AuthManager.AuthStart s = Tone3000AuthManager.get(this).startSelectFlow(this);
                        androidx.browser.customtabs.CustomTabsIntent cti = new androidx.browser.customtabs.CustomTabsIntent.Builder().setShowTitle(true).build();
                        cti.launchUrl(this, android.net.Uri.parse(s.authorizeUrl));
                    } catch (Exception ex) { toast("TONE3000 failed: " + ex.getMessage()); }
                }
            };
            row.setOnClickListener(goT3k);
            browse.setOnClickListener(goT3k);
            row.addView(left);
            row.addView(browse);
            t3kContainer.addView(row);

            // Divider hint
            View divider = new View(this);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(1 * getResources().getDisplayMetrics().density));
            dp.setMargins(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, 0);
            divider.setLayoutParams(dp);
            divider.setBackgroundColor(Color.parseColor("#1E232A"));
        } catch (Exception e) { android.util.Log.w("GigginGear", "TONE3000 entry injection failed", e); }

        final CategoryHelper.Category[] categories = {
                CategoryHelper.Category.AMP,
                CategoryHelper.Category.CABINET,
                CategoryHelper.Category.DISTORTION,
                CategoryHelper.Category.DYNAMICS,
                CategoryHelper.Category.MODULATION,
                CategoryHelper.Category.DELAY,
                CategoryHelper.Category.REVERB,
                CategoryHelper.Category.EQ,
                CategoryHelper.Category.PITCH,
                CategoryHelper.Category.WAH,
                CategoryHelper.Category.UTILITY,
                CategoryHelper.Category.GATE,
                CategoryHelper.Category.SPLIT
        };

        final String[] catLabels = {
                "Amp / Head", "Cabinet", "Overdrive / Dist", "Compressor",
                "Modulation", "Delay", "Reverb", "EQ", "Pitch / Synth",
                "Wah", "Volume / Utility", "Noise Gate", "Routing / Utility"
        };
        // Active category holder — drives the global search fallback (empty query = this cat).
        final CategoryHelper.Category[] activePickerCat = {categories[0]};

        if (catContainer != null) {
            catContainer.removeAllViews();
            for (int i = 0; i < categories.length; i++) {
                final CategoryHelper.Category cat = categories[i];
                View catView = getLayoutInflater().inflate(R.layout.item_model_category_row, catContainer, false);
                ImageView icon = catView.findViewById(R.id.item_cat_icon);
                TextView title = catView.findViewById(R.id.item_cat_title);

                title.setText(catLabels[i]);
                int catColor = CategoryHelper.getColor(cat.name());
                icon.setImageResource(CategoryHelper.getIconRes(cat.name()));
                icon.setColorFilter(catColor, android.graphics.PorterDuff.Mode.SRC_IN);

                boolean isInitial = (i == 0);
                catView.setBackgroundResource(isInitial ? R.drawable.glass_category_selected_bg : R.drawable.glass_card_bg);

                catView.setOnClickListener(v -> {
                    for (int c = 0; c < catContainer.getChildCount(); c++) {
                        catContainer.getChildAt(c).setBackgroundResource(R.drawable.glass_card_bg);
                    }
                    catView.setBackgroundResource(R.drawable.glass_category_selected_bg);
                    activePickerCat[0] = cat;
                    EditText si = dialog.findViewById(R.id.picker_search_input);
                    if (si != null && si.getText().length() > 0) {
                        si.setText(""); // watcher repopulates with the new category
                    } else {
                        loadPluginsIntoContainer(itemsContainer, cat, dialog);
                    }
                });

                catContainer.addView(catView);
            }
        }

        if (itemsContainer != null) {
            loadPluginsIntoContainer(itemsContainer, categories[0], dialog);
        }

        // Wire GLOBAL search for add plugin dialog: non-empty query searches
        // every category, empty query falls back to the active category.
        EditText pickerSearch = dialog.findViewById(R.id.picker_search_input);
        ImageButton pickerClear = dialog.findViewById(R.id.picker_search_clear);
        if (pickerSearch != null && itemsContainer != null) {
            if (pickerClear != null) {
                pickerClear.setOnClickListener(v -> pickerSearch.setText(""));
            }
            pickerSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim().toLowerCase();
                    if (pickerClear != null) {
                        pickerClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (query.isEmpty()) {
                        loadPluginsIntoContainer(itemsContainer, activePickerCat[0], dialog);
                    } else {
                        loadPluginsMatchingQuery(itemsContainer, query, dialog, categories);
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void loadPluginsIntoContainer(LinearLayout container, CategoryHelper.Category cat, androidx.appcompat.app.AlertDialog dialog) {
        if (container == null) return;
        container.removeAllViews();

        if (cat == CategoryHelper.Category.SPLIT || cat == CategoryHelper.Category.MERGE) {
            addPluginPickerRow(container, "SPLIT (Parallel Branch)", "Split signal into 2 parallel paths", R.drawable.ic_routing_split, dialog, v -> {
                addRoutingBlock(true);
                dialog.dismiss();
            });
            addPluginPickerRow(container, "MERGE (Sum to Mono)", "Combine parallel branches into 1 path", R.drawable.ic_routing_merge, dialog, v -> {
                addRoutingBlock(false);
                dialog.dismiss();
            });
            return;
        }

        JSONObject[] sources = {availablePlugins, availablePluginsLV2};
        ArrayList<String> loadedPluginNames = new ArrayList<>();

        for (JSONObject source : sources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject obj = source.optJSONObject(k);
                if (obj == null) continue;
                String pName = obj.optString("name", "");
                if (pName.isEmpty() || loadedPluginNames.contains(pName)) continue;

                CategoryHelper.Category pCat = CategoryHelper.getCategory(pName);
                if (pCat == cat) {
                    loadedPluginNames.add(pName);
                    String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                    if (desc.length() > 40) desc = desc.substring(0, 37) + "\u2026";

                    final String pluginName = pName;
                    addPluginPickerRow(container, pName, desc, CategoryHelper.getIconRes(pName), dialog, v -> {
                        addPluginByName(pluginName);
                        dialog.dismiss();
                    });
                }
            }
        }

        if (loadedPluginNames.isEmpty() && (cat == CategoryHelper.Category.AMP || cat == CategoryHelper.Category.CABINET)) {
            String[] sampleAmps = {"Neural Amp Modeler", "Convolution Reverb", "AIDA-X"};
            for (String sa : sampleAmps) {
                final String pluginName = sa;
                addPluginPickerRow(container, sa, cat.name() + " Loader", CategoryHelper.getIconRes(cat.name()), dialog, v -> {
                    addPluginByName(pluginName);
                    dialog.dismiss();
                });
            }
        }
    }

    /** Global plugin search: matches across every category plus routing blocks. */
    private void loadPluginsMatchingQuery(LinearLayout container, String query,
                                          androidx.appcompat.app.AlertDialog dialog,
                                          CategoryHelper.Category[] categories) {
        if (container == null) return;
        container.removeAllViews();
        String q = query.toLowerCase();
        int hits = 0;

        // Routing blocks on match
        if ("split (parallel branch)".contains(q) || "split signal into 2 parallel paths".contains(q)) {
            addPluginPickerRow(container, "SPLIT (Parallel Branch)", "Split signal into 2 parallel paths", R.drawable.ic_routing_split, dialog, v -> {
                addRoutingBlock(true);
                dialog.dismiss();
            });
            hits++;
        }
        if ("merge (sum to mono)".contains(q) || "combine parallel branches into 1 path".contains(q)) {
            addPluginPickerRow(container, "MERGE (Sum to Mono)", "Combine parallel branches into 1 path", R.drawable.ic_routing_merge, dialog, v -> {
                addRoutingBlock(false);
                dialog.dismiss();
            });
            hits++;
        }

        // Every plugin across all categories
        JSONObject[] sources = {availablePlugins, availablePluginsLV2};
        ArrayList<String> seen = new ArrayList<>();
        for (JSONObject source : sources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                JSONObject obj = source.optJSONObject(keys.next());
                if (obj == null) continue;
                final String pName = obj.optString("name", "");
                if (pName.isEmpty() || seen.contains(pName)) continue;
                String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                if (!(pName.toLowerCase().contains(q) || desc.toLowerCase().contains(q))) continue;
                seen.add(pName);
                String shortDesc = desc.length() > 40 ? desc.substring(0, 37) + "…" : desc;
                addPluginPickerRow(container, pName, shortDesc, CategoryHelper.getIconRes(pName), dialog, v -> {
                    addPluginByName(pName);
                    dialog.dismiss();
                });
                hits++;
            }
        }

        if (hits == 0) {
            TextView empty = new TextView(this);
            empty.setText("No matches for \"" + query + "\"");
            empty.setTextColor(Color.parseColor("#A1A1AA"));
            empty.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            empty.setPadding(Math.round(12 * getResources().getDisplayMetrics().density),
                    Math.round(16 * getResources().getDisplayMetrics().density),
                    Math.round(12 * getResources().getDisplayMetrics().density),
                    Math.round(16 * getResources().getDisplayMetrics().density));
            container.addView(empty);
        }
    }

    private void addPluginPickerRow(LinearLayout container, String title, String desc, int iconRes, androidx.appcompat.app.AlertDialog dialog, View.OnClickListener onClick) {
        View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
        TextView t = row.findViewById(R.id.item_model_title);
        TextView b = row.findViewById(R.id.item_model_brand);
        ImageView thumb = row.findViewById(R.id.item_model_thumb);
        row.findViewById(R.id.item_model_star).setVisibility(View.GONE);

        t.setText(title);
        b.setText(desc);
        thumb.setImageResource(iconRes);
        thumb.setColorFilter(CategoryHelper.getColor(title), android.graphics.PorterDuff.Mode.SRC_IN);

        row.setOnClickListener(onClick);
        container.addView(row);
    }

    private void loadModelsIntoContainer(LinearLayout container, CategoryHelper.Category cat, int position, androidx.appcompat.app.AlertDialog dialog) {
        if (container == null) return;
        container.removeAllViews();

        boolean isFileBased = (cat == CategoryHelper.Category.AMP || cat == CategoryHelper.Category.CABINET);

        if (isFileBased) {
            // Load local files + Storage Option
            File baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            ArrayList<File> filesList = new ArrayList<>();
            if (baseDir != null) {
                File[] dirs = baseDir.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File d : dirs) {
                        File[] fs = d.listFiles((dd, n) -> n.toLowerCase().endsWith(".nam") || n.toLowerCase().endsWith(".wav"));
                        if (fs != null) {
                            for (File f : fs) filesList.add(f);
                        }
                    }
                }
            }

            // Factory name-only rows deleted: they changed no sound (no file,
            // no DSP call). Real sources below: local .nam/.wav, TONE3000, Storage.

            // 2. Add local downloaded user models if present
            if (!filesList.isEmpty()) {
                for (File f : filesList) {
                    View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
                    TextView t = row.findViewById(R.id.item_model_title);
                    TextView b = row.findViewById(R.id.item_model_brand);
                    ImageView thumb = row.findViewById(R.id.item_model_thumb);

                    t.setText(f.getName());
                    b.setText("Local Storage • " + cat.name());
                    thumb.setImageResource(CategoryHelper.getIconRes(cat.name()));
                    thumb.setColorFilter(CategoryHelper.getColor(cat.name()), android.graphics.PorterDuff.Mode.SRC_IN);

                    row.setOnClickListener(v -> {
                        applyLocalModelToPlugin(position, f);
                        TextView mn = findViewById(R.id.detail_model_name);
                        if (mn != null) mn.setText(f.getName());
                        dialog.dismiss();
                    });
                    container.addView(row);
                }
            }

            // Real built-in AMP/CABINET plugins (swap the block — actual sound
            // change). This replaces the deleted name-only factory rows, so
            // Browse always offers non-NAM amp/cab choices that really work.
            {
                JSONObject[] pluginSources = {availablePlugins, availablePluginsLV2};
                ArrayList<String> pluginNames = new ArrayList<>();
                for (JSONObject source : pluginSources) {
                    if (source == null) continue;
                    Iterator<String> keys = source.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        JSONObject obj = source.optJSONObject(k);
                        if (obj == null) continue;
                        String pName = obj.optString("name", "");
                        if (pName.isEmpty() || pluginNames.contains(pName)) continue;
                        if (CategoryHelper.getCategory(pName) != cat) continue;
                        pluginNames.add(pName);
                        final String pick = pName;
                        View prow = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
                        ((TextView) prow.findViewById(R.id.item_model_title)).setText(pName);
                        String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                        if (desc.length() > 40) desc = desc.substring(0, 37) + "…";
                        ((TextView) prow.findViewById(R.id.item_model_brand)).setText(desc);
                        ImageView pthumb = prow.findViewById(R.id.item_model_thumb);
                        pthumb.setImageResource(CategoryHelper.getIconRes(pName));
                        pthumb.setColorFilter(CategoryHelper.getColor(pName), android.graphics.PorterDuff.Mode.SRC_IN);
                        prow.findViewById(R.id.item_model_star).setVisibility(View.GONE);
                        prow.setOnClickListener(v -> {
                            swapActivePlugin(position, pick);
                            dialog.dismiss();
                        });
                        container.addView(prow);
                    }
                }
            }

            // TONE3000 browse row — persistent per spec §18
            View t3kRow = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
            TextView t3kt = t3kRow.findViewById(R.id.item_model_title);
            TextView t3kb = t3kRow.findViewById(R.id.item_model_brand);
            ImageView t3kThumb = t3kRow.findViewById(R.id.item_model_thumb);
            t3kRow.findViewById(R.id.item_model_star).setVisibility(View.GONE);
            t3kt.setText("Browse TONE3000");
            t3kt.setTextColor(Color.parseColor("#FF8A00"));
            t3kb.setText("NAM captures & IRs — massive community library");
            t3kThumb.setImageResource(R.drawable.ic_cat_amp);
            t3kThumb.setColorFilter(Color.parseColor("#FF8A00"), android.graphics.PorterDuff.Mode.SRC_IN);
            t3kRow.setBackgroundResource(R.drawable.glass_card_bg);
            t3kRow.setOnClickListener(v -> {
                dialog.dismiss();
                if (!Tone3000Config.isConfigured(this)) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("TONE3000 not configured")
                        .setMessage("Missing TONE3000_CLIENT_ID.\n\nAdd it via BuildConfig or manifest meta-data.")
                        .setPositiveButton("OK", null).show();
                    return;
                }
                Tone3000EntrySheet sheet = Tone3000EntrySheet.newInstance();
                sheet.show(getSupportFragmentManager(), "t3k_entry");
            });
            container.addView(t3kRow);

            // Storage picker row
            View storageRow = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
            TextView st = storageRow.findViewById(R.id.item_model_title);
            TextView sb = storageRow.findViewById(R.id.item_model_brand);
            ImageView sthumb = storageRow.findViewById(R.id.item_model_thumb);
            storageRow.findViewById(R.id.item_model_star).setVisibility(View.GONE);

            st.setText("Load from Storage…");
            sb.setText("Select custom .nam / .wav file");
            sthumb.setImageResource(R.drawable.ic_baseline_save_24);
            sthumb.setColorFilter(Color.parseColor("#FF8A00"), android.graphics.PorterDuff.Mode.SRC_IN);

            storageRow.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent_upload = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.setType("*/*");
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent_upload, 5000 + position);
            });
            container.addView(storageRow);

        } else {
            // Built-in LADSPA/LV2 plugins
            JSONObject[] pluginSources = {availablePlugins, availablePluginsLV2};
            ArrayList<String> pluginNames = new ArrayList<>();
            for (JSONObject source : pluginSources) {
                if (source == null) continue;
                Iterator<String> keys = source.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject obj = source.optJSONObject(k);
                    if (obj == null) continue;
                    String pName = obj.optString("name", "");
                    if (pName.isEmpty()) continue;
                    CategoryHelper.Category pCat = CategoryHelper.getCategory(pName);
                    if (pCat == cat && !pluginNames.contains(pName)) {
                        pluginNames.add(pName);
                        View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
                        TextView t = row.findViewById(R.id.item_model_title);
                        TextView b = row.findViewById(R.id.item_model_brand);
                        ImageView thumb = row.findViewById(R.id.item_model_thumb);

                        t.setText(pName);
                        String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                        if (desc.length() > 40) desc = desc.substring(0, 37) + "…";
                        b.setText(desc);
                        thumb.setImageResource(CategoryHelper.getIconRes(pName));
                        thumb.setColorFilter(CategoryHelper.getColor(pName), android.graphics.PorterDuff.Mode.SRC_IN);

                        row.setOnClickListener(v -> {
                            swapActivePlugin(position, pName);
                            dialog.dismiss();
                        });
                        container.addView(row);
                    }
                }
            }
        }
    }

    /** Global model search: matches across every category (factories, local files, all plugins). */
    private void loadModelsMatchingQuery(LinearLayout container, String query, int position,
                                         androidx.appcompat.app.AlertDialog dialog,
                                         CategoryHelper.Category[] categories) {
        if (container == null) return;
        container.removeAllViews();
        String q = query.toLowerCase();
        int hits = 0;

        // (Factory name-only rows deleted: they changed no sound.)

        // Local downloaded files (.nam / .wav)
        try {
            File baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (baseDir != null) {
                File[] dirs = baseDir.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File d : dirs) {
                        File[] fs = d.listFiles((dd, n) -> n.toLowerCase().endsWith(".nam") || n.toLowerCase().endsWith(".wav"));
                        if (fs == null) continue;
                        for (File f : fs) {
                            if (!f.getName().toLowerCase().contains(q)) continue;
                            View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
                            ((TextView) row.findViewById(R.id.item_model_title)).setText(f.getName());
                            ((TextView) row.findViewById(R.id.item_model_brand)).setText("Local Storage");
                            ImageView thumb = row.findViewById(R.id.item_model_thumb);
                            thumb.setImageResource(CategoryHelper.getIconRes(CategoryHelper.Category.AMP.name()));
                            row.setOnClickListener(v -> {
                                applyLocalModelToPlugin(position, f);
                                TextView mn = findViewById(R.id.detail_model_name);
                                if (mn != null) mn.setText(f.getName());
                                dialog.dismiss();
                            });
                            container.addView(row);
                            hits++;
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // Every built-in plugin across all categories
        JSONObject[] pluginSources = {availablePlugins, availablePluginsLV2};
        ArrayList<String> seen = new ArrayList<>();
        for (JSONObject source : pluginSources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                JSONObject obj = source.optJSONObject(keys.next());
                if (obj == null) continue;
                final String pName = obj.optString("name", "");
                if (pName.isEmpty() || seen.contains(pName)) continue;
                String desc = obj.optString("desc", obj.optString("description", "Built-in Effect"));
                if (!(pName.toLowerCase().contains(q) || desc.toLowerCase().contains(q))) continue;
                seen.add(pName);
                View row = getLayoutInflater().inflate(R.layout.item_model_browser_row, container, false);
                ((TextView) row.findViewById(R.id.item_model_title)).setText(pName);
                String shortDesc = desc.length() > 40 ? desc.substring(0, 37) + "…" : desc;
                ((TextView) row.findViewById(R.id.item_model_brand)).setText(shortDesc);
                ImageView thumb = row.findViewById(R.id.item_model_thumb);
                thumb.setImageResource(CategoryHelper.getIconRes(pName));
                thumb.setColorFilter(CategoryHelper.getColor(pName), android.graphics.PorterDuff.Mode.SRC_IN);
                row.setOnClickListener(v -> {
                    swapActivePlugin(position, pName);
                    dialog.dismiss();
                });
                container.addView(row);
                hits++;
            }
        }

        if (hits == 0) {
            TextView empty = new TextView(this);
            empty.setText("No matches for \"" + query + "\"");
            empty.setTextColor(Color.parseColor("#A1A1AA"));
            empty.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            empty.setPadding(Math.round(12 * getResources().getDisplayMetrics().density),
                    Math.round(16 * getResources().getDisplayMetrics().density),
                    Math.round(12 * getResources().getDisplayMetrics().density),
                    Math.round(16 * getResources().getDisplayMetrics().density));
            container.addView(empty);
        }
    }

    /** True when a plugin name resolves in the bundled LADSPA/LV2 inventories (no DSP calls). */
    private boolean isPluginKnown(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) return false;
        JSONObject[] sources = {availablePlugins, availablePluginsLV2};
        for (JSONObject source : sources) {
            if (source == null) continue;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                JSONObject obj = source.optJSONObject(keys.next());
                if (obj == null) continue;
                if (pluginName.equals(obj.optString("name", ""))) return true;
            }
        }
        return false;
    }

    /** Delete guarded by a live slot-range check — a stale index aborts native (SIGABRT, uncatchable). */
    private boolean safeDeletePlugin(int slot) {
        try {
            int total = AudioEngine.getActivePlugins();
            if (slot < 0 || slot >= total) {
                Log.w(TAG, "safeDeletePlugin: stale slot " + slot + " of " + total + ", skipping");
                return false;
            }
            AudioEngine.deletePlugin(slot);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "safeDeletePlugin failed", t);
            return false;
        }
    }

    /** Swap the active plugin at a given slot with another plugin and refresh UI. */
    public void swapActivePlugin(int position, String newPluginName) {
        // Guard: never destroy the current plugin for a name the engine can't resolve.
        if (!isPluginKnown(newPluginName)) {
            Log.e(TAG, "swapActivePlugin: unknown plugin name " + newPluginName);
            toast("Couldn't find " + newPluginName);
            return;
        }
        try {
            // Guard: stale position aborts native — bail before touching the chain.
            int total = AudioEngine.getActivePlugins();
            if (position < 0 || position >= total) {
                Log.e(TAG, "swapActivePlugin: stale position " + position + " of " + total);
                toast("Couldn't find " + newPluginName);
                return;
            }
            // Save state of enabled before delete
            boolean wasEnabled = AudioEngine.getActivePluginEnabled(position);
            // Trial-at-end: append the replacement FIRST and only delete the
            // old slot on success. Delete-first destroys the old block when
            // the add fails, and the stale addedPos math below can then move
            // an innocent block into place (wrong order / wrong sound).
            int before = AudioEngine.getActivePlugins();
            addPluginByName(newPluginName);
            if (AudioEngine.getActivePlugins() != before + 1) {
                Log.e(TAG, "swapActivePlugin: add failed for " + newPluginName + ", old block kept");
                toast("Couldn't load " + newPluginName);
                return;
            }
            int addedPos = AudioEngine.getActivePlugins() - 1;
            // Keep T3K meta aligned with engine slots: the delete shifts everything
            // > position down, the move below shifts it back. Without these
            // shifts the badge/meta for adjacent T3K blocks drifts by one slot each
            // swap — eventually the badge lands on an unrelated block (the
            // "badge pindah ke block sebelah" bug on swap + load).
            AudioEngine.deletePlugin(position);
            shiftT3kMetaOnDelete(position);
            // The delete shifted the trial (was last) down one slot.
            int trial = addedPos - 1;
            if (trial >= 0 && trial != position && position >= 0 && position <= trial) {
                AudioEngine.movePlugin(trial, position);
                shiftT3kMetaOnMove(trial, position);
            }
            try { AudioEngine.togglePlugin(position, wasEnabled); } catch (Exception ignore) {}

            // Refresh signal chain canvas and force re-select the current node so its icon & color update
            if (signalChainCanvas != null) {
                signalChainCanvas.clearT3kBadges();
                signalChainCanvas.loadFromActivePlugins(true);
                signalChainCanvas.post(() -> signalChainCanvas.reapplyT3kBadgesFromMeta(t3kBlockMeta));
                signalChainCanvas.selectNode(position);
                signalChainCanvas.invalidate();
            }

            // Re-populate inline parameter editor with the newly loaded plugin at the correct slot
            populateInlineParameterEditor(position, newPluginName);
            toast("Loaded: " + PluginNaming.displayName(newPluginName));
        } catch (Exception e) {
            Log.e(TAG, "swapActivePlugin failed", e);
            toast("Error loading " + newPluginName);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(androidx.preference.PreferenceFragmentCompat caller, androidx.preference.Preference pref) {        // Mirror SettingsActivity.onPreferenceStartFragment: open sub-screens
        // in fragment_container using the same static preference fragments.
        final Bundle args = pref.getExtras();
        final androidx.fragment.app.Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    private boolean LoadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitNow();
            return true;
        }
        return false;
    }

    public static void toast(String text) {
        Toast.makeText(context,
                        text,
                        Toast.LENGTH_LONG)
                .show();

    }

    /**
     * Header-scoped dialog builder: AppCompat + Material3 glass overlay so every
     * alert dialog uses the dark theme with white/light-grey text AND light
     * button text. Must stay AppCompat — the old android.app.AlertDialog ignored
     * the M3 button styling and rendered Cancel/Delete/etc. in black.
     */
    public static androidx.appcompat.app.AlertDialog.Builder dialogBuilder(Context ctx) {
        return new androidx.appcompat.app.AlertDialog.Builder(ctx, R.style.ThemeOverlay_Giggin_Dialog);
    }

    public static void alert(String title, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text)
                .setTitle(title)
                .setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void heartPlugin(String name) {
        Log.d(TAG, "heartPlugin: " + name);
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            set = new HashSet<>();
        }

        set.add(name);
        sharedPreferences.edit().putStringSet("favoritePresets", set).apply();
    }

    public void unheartPlugin(String name) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return;
        }

        set.remove(name);
        sharedPreferences.edit().putStringSet("favoritePresets", set).apply();
    }

    boolean isPluginHearted(String plugin) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return false;
        }

        return set.contains(plugin);
    }

    Set getHeartedPlugins() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);

        return set;
    }

    void applyPreferencesDevices() {
        // Audio Devices
        String input = defaultSharedPreferences.getString("input", "-1");
        String output = defaultSharedPreferences.getString("output", "-1");
        Log.d(TAG, "applyPreferences: [devices] " + String.format("input: %s, output: %s", input, output));

        Log.d(TAG, "applyPreferencesDevices: " + String.format(
                "[preferences] playback device: %s, recording device: %s",
                output, input
        ));
        AudioEngine.setRecordingDeviceId(new Integer(input));
        AudioEngine.setPlaybackDeviceId(new Integer(output));

        AudioEngine.setLowLatency(defaultSharedPreferences.getBoolean("latency", true));
        int sampleRate = 48000;
        try {
            sampleRate = Integer.valueOf(defaultSharedPreferences.getString("sample_rate", "48000"));
        } catch (ClassCastException e) {
            Log.e(TAG, "applyPreferencesDevices: cannot get default sample rate from preference: " + defaultSharedPreferences.getString("sample_rate", null), e);
        }
        AudioEngine.setSampleRate(sampleRate);
    }

    void applyPreferencesExport() {
        exportFormat = defaultSharedPreferences.getString("export_format", "1");
        AudioEngine.setExportFormat(Integer.parseInt(exportFormat));
        Integer bitRate = Integer.valueOf(defaultSharedPreferences.getString("opus_bitrate", "64"));
        Log.d(TAG, "applyPreferencesExport: setting bitrate " + bitRate * 1000);
        AudioEngine.setOpusBitRate(bitRate * 1000);
//        if (proVersion == false) {
//            AudioEngine.setExportFormat(0);
//        }
    }

    public static void printDebugLog() {
        AudioEngine.debugInfo();

    }

    public static void printActiveChain () {
        AudioEngine.printActiveChain();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Bitmap scaleBackground(Bitmap originalImage, int width, int height) {
        Bitmap background = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

        float originalWidth = originalImage.getWidth();
        float originalHeight = originalImage.getHeight();

        Canvas canvas = new Canvas(background);

//        float scale = width / originalWidth;
        float scale = height / originalHeight;

//        float xTranslation = 0.0f;
//        float yTranslation = (height - originalHeight * scale) / 2.0f;
        float xTranslation = (width - originalWidth * scale) / 2.0f;
        float yTranslation = 0.0f;

        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);
        return background;

    }

    public static void applyWallpaper(Context _context, Window window, Resources resources, ImageView imageView, int width, int height) {
        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_mode", true) ;
        String mTheme = darkMode ? "Theme.AmpRack" : "Theme.Light" ;
        Log.d(TAG, "applyWallpaper: setting native theme " + mTheme);

        if (lowMemoryMode) {
            imageView.setBackgroundColor(_context.getResources().getColor(R.color.black));
            return;
        }

        String wallpaper = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", null);
        Log.d(TAG, "applyWallpaper: wallpaper: " + wallpaper);

        if (useTheme) {
            // Disabled legacy skin wallpaper to allow clean pedalboard canvas!
            imageView.setBackgroundColor(Color.parseColor("#121214"));
            return ;
        }

        Bitmap bitmap = null;
        if (wallpaper != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(_context.getContentResolver(), Uri.parse(wallpaper));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);
    }

    public static void applyWallpaper1(Context _context, Window window, Resources resources, ImageView imageView, int width, int height) {
        if (useTheme) {
            imageView.setBackgroundColor(Color.parseColor("#121214"));
            return ;
        }

        String resIdString = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", "700032");
//        String resIdString = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", "a1");
        Bitmap bitmap = null;
        switch (resIdString) {
            default:
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(_context.getContentResolver(), Uri.parse(resIdString));
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

                bitmap = scaleBackground(bitmap, width, height);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "Space":
                if (lowMemoryMode)
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg1);
                else
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                break;
            case "Water":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.water);
                break;
            case "Fire":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.fire);
                break;
            case "Sky":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.sky);
                break;
            case "Earth":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_earth);
                break;
            case "a1":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a1);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "700032":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "a2":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a2);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "a3":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a3);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "a4":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a4);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "2.9 Beta":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                context.setTheme(R.style.Theme_AmpRack);
                break;
        }

        if (bitmap == null) {
            Log.e(TAG, "applyWallpaper: No suitable bg from settings", null);
            return;
        }

        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);
        Paint paint = new Paint();
        PorterDuffColorFilter filter = new PorterDuffColorFilter(0xffaaaaaa, PorterDuff.Mode.MULTIPLY);
        imageView.getDrawable().setColorFilter(filter);
        paint.setColorFilter(filter);
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 10, 10);

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(croppedBitmap, 0, 0, paint);
        int color = (int) (getDominantColor(croppedBitmap));

        Log.d(TAG, "applyWallpaper: dominant color " + color + ": " + Color.parseColor("#6c6f6e"));
        window.setStatusBarColor(color);
    }

    static public JSONObject loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: unable to parse json", ex);
            return null;
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: cannot parse json", e);
        }

        return jsonObject;
    }

    public static void shareFile(File file) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri contentUri = null;
        try {
            contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, "shareFile: ", illegalArgumentException);
            return;
        }

        intentShareFile.setType("audio/*");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, contentUri);

        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                "Sharing Audio File...");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.app_name) + " recorded audio ...");

        intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));

    }

    public void runCommand(String command) {
        switch (command) {
            case "saveactivepreset":
                saveActivePreset();
                break;
            default:
                alert(this.getClass().getSimpleName(), "Command not supported: " + command);
                break;
        }
    }

    public void addPluginByName(String pluginName) {
//        Log.d(TAG, "addPluginByName: " + pluginName);
//        Log.d(TAG, "addPluginByName: " + availablePlugins.toString());
        JSONObject plugins = availablePlugins;
        if (plugins == null) { Log.e(TAG, "availablePlugins is null"); addPluginByNameLV2(pluginName); return; }
        Iterator<String> keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (plugins.get(key) instanceof JSONObject) {
//                    Log.d(TAG, "onCreate: key " + key);
                    JSONObject object = plugins.getJSONObject(key);
                    // do something with jsonObject here
                    String name = object.getString("name");
                    String id = object.getString("id");
                    int plugin = object.getInt("plugin");
                    String lib = object.getString("library");

//                    Log.d(TAG, String.format ("addPluginByName: comparing %s and %s", pluginName, name));

                    if (pluginName.equals(name)) {
                        Log.d(TAG, "addPluginByName: found plugin " + name);
                        // Native drops unloadable plugins silently: only touch
                        // UI state when the chain actually grew, or UI/engine
                        // slots desync (stale indices, blank editors).
                        int before = AudioEngine.getActivePlugins();
                        AudioEngine.addPluginLazy(lib, plugin);
                        if (AudioEngine.getActivePlugins() <= before) {
                            Log.e(TAG, "addPluginByName: engine refused " + name);
                            toast("Couldn't load " + name);
                            return;
                        }
                        if (dataAdapter != null) {
                            int pluginID = 0;
                            try { pluginID = Integer.parseInt(key); } catch (Exception ignored) {}
                            dataAdapter.addItem(pluginID, dataAdapter.getItemCount());
                        }
                        if (signalChainCanvas != null) {
                            signalChainCanvas.post(() -> signalChainCanvas.loadFromActivePlugins());
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // First-chance LADSPA miss is the normal path for LV2-only names.
        Log.d(TAG, "addPluginByName: not a LADSPA name (" + pluginName + ") ... trying LV2 plugins ...");
        addPluginByNameLV2(pluginName);
    }

    public void addPluginByNameLV2(String pluginName) {
//        Log.d(TAG, "addPluginByName: " + pluginName);
        if (availablePluginsLV2 != null) Log.d(TAG, "addPluginByName: " + availablePluginsLV2.toString());
        JSONObject plugins = availablePluginsLV2;
        if (plugins == null) { Log.e(TAG, "availablePluginsLV2 is null for " + pluginName); return; }
        Iterator<String> keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (plugins.get(key) instanceof JSONObject) {
//                    Log.d(TAG, "onCreate: key " + key);
                    JSONObject object = plugins.getJSONObject(key);
                    // do something with jsonObject here
                    String name = object.getString("name");
                    String id = object.getString("id");
                    int plugin = object.getInt("index");
                    String lib = object.getString("library");

//                    Log.d(TAG, String.format ("addPluginByName: comparing %s and %s", pluginName, name));

                    if (pluginName.equals(name)) {
                        Log.d(TAG, "addPluginByNameLV2: found plugin " + name);
                        int before = AudioEngine.getActivePlugins();
                        AudioEngine.addPluginLazyLV2(lib, plugin);
                        if (AudioEngine.getActivePlugins() <= before) {
                            Log.e(TAG, "addPluginByNameLV2: engine refused " + name);
                            toast("Couldn't load " + name);
                            return;
                        }
                        if (dataAdapter != null) {
                            int pluginID = 0;
                            try { pluginID = Integer.parseInt(key); } catch (Exception ignored) {}
                            dataAdapter.addItem(pluginID, dataAdapter.getItemCount());
                        }
                        if (signalChainCanvas != null) {
                            signalChainCanvas.post(() -> signalChainCanvas.loadFromActivePlugins());
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.e(TAG, "[LV2] addPluginByName: unable to find plugin name " + pluginName);
    }

    static public String readAssetFile(Context context, String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: unable to parse json", ex);
            return null;
        }

        return json ;
    }

    public void testLV2 () {
        Log.d(TAG, "testLV2: ");
//        Log.d(TAG, "testLV2: " + getLV2Info("eql"));
//        AudioEngine.testLV2();
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        for (AudioDeviceInfo audioDeviceInfo: audioDevicesInput)
            Log.d(TAG, String.format ("[%s] %s: %s", audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
        for (AudioDeviceInfo audioDeviceInfo: audioDevicesOutput)
            Log.d(TAG, String.format ("[%s] %s: %s", audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
//        Log.d(TAG, "testLV2: " + getLV2Info("mda-Limiter.so", "http://drobilla.net/plugins/mda/Limiter"));
        Log.d(TAG, String.format ("[orientation]: %d", mainActivity.getResources().getConfiguration().orientation));

    }

    public static String getLV2Info (String libraryName, String plugin) {
        String pluginName ;
        if (plugin.indexOf("#") != -1)
            pluginName = plugin.split("#")[1];
        else {
            String [] p = plugin.split("/");
            pluginName = p [p.length -1];
        }

        Context ctx = context;
        String rel = "lv2/" + libraryName + "/" + pluginName + ".json";
        Log.d(TAG, "getLV2Info: " + rel);

        JSONObject jsonObject = null;
        if (ctx != null) {
            try {
                jsonObject = ConnectGuitar.loadJSONFromAssetFile(ctx, rel);
            } catch (Throwable t) {
                Log.e(TAG, "getLV2Info: exception for " + rel, t);
                jsonObject = null;
            }
            if (jsonObject == null) {
                try {
                    String[] assets = ctx.getAssets().list("lv2/" + libraryName);
                    if (assets != null) {
                        for (String a : assets) {
                            if (a.endsWith(".json")) {
                                try {
                                    JSONObject fallback = ConnectGuitar.loadJSONFromAssetFile(ctx, "lv2/" + libraryName + "/" + a);
                                    if (fallback != null) {
                                        Log.w(TAG, "getLV2Info: fell back to lv2/" + libraryName + "/" + a);
                                        jsonObject = fallback;
                                        break;
                                    }
                                } catch (Throwable t2) {
                                    Log.e(TAG, "getLV2Info: fallback read failed " + a, t2);
                                }
                            }
                        }
                    }
                } catch (Throwable t3) {
                    Log.e(TAG, "getLV2Info: cannot list lv2/" + libraryName, t3);
                }
            }
        }

        if (jsonObject == null) {
            Log.w(TAG, "getLV2Info: no manifest for " + rel + ", returning {}");
            return "{}";
        }
        return jsonObject.toString();
    }

    public static boolean isLibraryLV2 (String libraryName) {
        return Arrays.asList(sharedLibrariesLV2).contains(libraryName) ;
    }

    public boolean isPluginLV2 (String pluginName) {
//        Log.d(TAG, "isPluginLV2: " + pluginName + " in " + lv2Plugins.toString() + " = " + lv2Plugins.contains(pluginName));
        return lv2Plugins.contains(pluginName);
    }

    static ArrayList <Float> tunerBuffer = new ArrayList<>();
    static Pitch pitch = new Pitch(AudioEngine.getSampleRate());
    static void setMixerMeter (float inputValue, float outputValue) {
        inputMeter.setProgress((int) (inputValue * 100));
        outputMeter.setProgress((int) (outputValue * 100));
    }

    static int audioIn = 0, audioOut = 0 ;
    public void showTunerOverlay () {
        FrameLayout overlay = findViewById(R.id.tuner_overlay_container);
        if (overlay != null && overlay.getVisibility() == View.VISIBLE) {
            closeTunerOverlay(overlay);
            return;
        }

        View tunerLayout = getLayoutInflater().inflate(R.layout.dialog_tuner, null);
        overlay.removeAllViews();
        overlay.addView(tunerLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setVisibility(View.VISIBLE);

        tunerEnabled = true;
        AudioEngine.setTunerEnabled(true);
        tunerViews = new View[5];
        tunerViews[0] = tunerLayout.findViewById(R.id.tuner_note);
        tunerViews[1] = tunerLayout.findViewById(R.id.tuner_freq);
        tunerViews[2] = tunerLayout.findViewById(R.id.tuner_cents);
        tunerViews[3] = tunerLayout.findViewById(R.id.tuner_indicator);
        tunerViews[4] = tunerLayout.findViewById(R.id.tuner_mute);

        View mute = tunerViews[4];
        mute.setOnClickListener(v -> {
            tunerMuted = !tunerMuted;
            AudioEngine.setTunerEnabled(!tunerMuted);
            ((MaterialButton) mute).setText(tunerMuted ? "MUTE AUDIO: ON" : "MUTE AUDIO: OFF");
            ((MaterialButton) mute).setStrokeColor(ColorStateList.valueOf(tunerMuted ? 0xFF3B30 : 0x00E676));
        });

        ImageButton closeBtn = tunerLayout.findViewById(R.id.tuner_close);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> closeTunerOverlay(overlay));
        }
    }

    private void closeTunerOverlay (FrameLayout overlay) {
        overlay.removeAllViews();
        overlay.setVisibility(View.GONE);
        tunerEnabled = false;
        AudioEngine.setTunerEnabled(false);
        tunerViews = null;
        if (tuner != null && tuner instanceof TextView) {
            ((TextView) tuner).setText("Tuner");
        }
    }

    static void setTuner (float [] data, int size) {
        if (! mainActivity.tunerEnabled)
            return ;

        double freq = pitch.computePitchFrequency(data);
        String note = " - " ;
        double cents = 0 ;
        double diff = 0 ;
        float targetFrequency = 0;
        for (int i = 0 ; i < pitch.notes.length ; i ++) {
            targetFrequency = Float.valueOf(pitch.notes [i][1]) ;
            cents = 1200 * Math.log(freq / targetFrequency) / Math.log(2);
            if (abs (cents) < 50) {
                note = pitch.notes [i][0];
                diff = freq - targetFrequency ;
                break ;
            }
        }

//        Log.d(TAG, String.format(
//                "%f %s %f",
//                data [0], note, freq
//        )) ;
        String finalNote = note;
        double finalCents = cents;
        float finalTargetFrequency = targetFrequency;
        double finalDiff = diff;
        mainActivity.handler.post(() -> {
//            Log.d(TAG, "setTuner: " + String.format("%s %f [%f] %f", finalNote,freq, finalTargetFrequency, finalCents));
            // write your code here
            if (mainActivity.tunerViews != null) {
                TextView noteView = (TextView) mainActivity.tunerViews[0];
                TextView freqView = (TextView) mainActivity.tunerViews[1];
                TextView centsView = (TextView) mainActivity.tunerViews[2];
                View indicator = mainActivity.tunerViews[3];
                if (noteView != null) noteView.setText(finalNote);
                if (freqView != null) freqView.setText(String.format("%.2f Hz", freq));
                boolean inTune = abs(finalCents) <= 3;
                if (centsView != null) {
                    centsView.setText(String.format("%+.0f cents", finalCents));
                    centsView.setTextColor(ColorStateList.valueOf(inTune ? 0x00E676 : 0xFF3B30));
                    centsView.setTextColor(inTune ? 0x00E676 : 0xFF3B30);
                }
                if (indicator != null) {
                    float clamp = (float) Math.max(-50, Math.min(50, finalCents));
                    float frac = (clamp + 50f) / 100f;
                    // Travel proportional to density so the sweep matches the bar
                    // width on every DPI (was fixed ±120 raw px).
                    float d = mainActivity.getResources().getDisplayMetrics().density;
                    indicator.setTranslationX((frac - 0.5f) * 2f * 120f * d);
                    indicator.setBackgroundColor(inTune ? 0xFF00E676 : 0xFFFFAA00);
                }
            }
            if (abs (finalDiff) < 1){
                mainActivity.tuner.setText("✔ " + finalNote);
            } else if (finalDiff < 0) {
                mainActivity.tuner.setText("⬆ " +  + Math.round(finalDiff) + "  " + finalNote );
            } else {
                mainActivity.tuner.setText("⬇ " + Math.round(finalDiff) + "  " + finalNote );
            }
        });

        tunerBuffer.clear();
    }

    static void setMixerMeterSwitch (float inputValue, boolean isInput) {
        if (inputValue < 0.001)
            return;
//        Log.d(TAG, "setMixerMeterSwitch() called with: inputValue = [" + inputValue + "], isInput = [" + isInput + "]");
        if (isInput) {
            mainActivity.handler.post(() -> {
                        if (inputValue > 1f)
                            inputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        else
                            inputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                        inputMeter.setProgress((int) (inputValue * 100));
                    });
            /*
            if (tunerBuffer.size() < 32) {
                Log.d(TAG, tunerBuffer.size() + ": setMixerMeter: " + inputValue);
                tunerBuffer.add(inputValue);
            } else {
                double freq = pitch.computePitchFrequency(tunerBuffer);
                String note = " - " ;
                double diff = 0 ;
                for (int i = 0 ; i < pitch.notes.length ; i ++) {
                    double _diff = Float.valueOf(pitch.notes [i][1]) - freq ;
                    if (abs (_diff) < 31) {
                        diff = _diff;
                        note = pitch.notes [i][0];
                        break ;
                    }
                }

                Log.d(TAG, String.format(
                        "%s %f %f",
                        note, freq, diff
                        )) ;
                String finalNote = note;
                double finalDiff = diff;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // write your code here
                        mainActivity.tuner.setText(finalNote);
                        if (finalDiff < 0) {
                            mainActivity.tuner.setCompoundDrawables(mainActivity.getResources().getDrawable(R.drawable.ic_baseline_keyboard_arrow_up_24), null,null,null);
                        } else {
                            mainActivity.tuner.setCompoundDrawables(null,null,null, mainActivity.getResources().getDrawable(R.drawable.ic_baseline_keyboard_arrow_down_24));
                        }
                    }
                });

                tunerBuffer.clear();
            }
             */
        }
        else {
            mainActivity.handler.post(() -> {
                outputMeter.setProgress((int) (inputValue * 100));
//            Log.d(TAG, "setMixerMeterSwitch: " + inputValue);
                if (mainActivity.triggerRecord) {
                    if (mainActivity.recording) {
                        if (inputValue < 0.1) {
//                        mainActivity.record.setChecked(false);
                            AudioEngine.toggleRecording(false);
                            mainActivity.recording = false;
                            mainActivity.triggerRecord = false;
                            outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        }
                    } else {
                        if (inputValue > 0.3) {
                            outputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
//                        mainActivity.record.setChecked(true);
                            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
                            Date date = new Date();
                            mainActivity.lastRecordedFileName = formatter.format(date);
                            mainActivity.lastRecordedFileName = mainActivity.dir.getAbsolutePath() + "/" + mainActivity.lastRecordedFileName;
                            AudioEngine.setFileName(mainActivity.lastRecordedFileName);
                            switch (mainActivity.exportFormat) {
                                case "0":
                                default:
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".wav";
                                    break;
                                case "1":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".ogg";
                                    break;
                                case "2":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".mp3";
                                    break;
                            }

                            mainActivity.triggerRecordedSomething = true;
                            mainActivity.recording = true;
                            Log.d(TAG, "setMixerMeterSwitch: triggering recording");
                            AudioEngine.toggleRecording(true);
//                        mainActivity.triggerRecordToggle.setChecked(false);
                        }
                    }
                } else {
                    if (inputValue > 1f)
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    else
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                }
            });

        }
    }

    static void setMixerMeterInput (float inputValue) {
//        Log.d(TAG, "setMixerMeterInput() called with: inputValue = [" + inputValue + "]");
        inputMeter.setProgress((int) (inputValue * 100));
    }
    static void setMixerMeterOutput (float outputValue) {
//        Log.d(TAG, "setMixerMeterOutput() called with: outputValue = [" + outputValue + "]");
        outputMeter.setProgress((int) (outputValue * 100));
    }

    /* the following supposibly adds hardware keyboard shortcut support a.k.a volume keys

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (onOff != null) {
            if (onOff.isChecked() == false)
                return super.onKeyLongPress(keyCode, event);
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                rack.patchDown.performClick();
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                rack.patchUp.performClick();
        }

        return super.onKeyLongPress(keyCode, event);
    }
     */

    public static void drummer () {
        Intent intent = new Intent(context, DrumMachineActivity.class);
        mainActivity.startActivity(intent);
     }

     public static void resetOnboard () {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("currentVersion").apply();
     }

     public static void featured () {
         Intent intent = new Intent(context, FeaturedVideos.class);
         mainActivity.startActivity(intent);
     }

     /*
    public boolean hotkeys (int keyCode, KeyEvent event) {
        Log.d(TAG, "hotkeys() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        switch (keyCode) {
            case KeyEvent.KEYCODE_SPACE:
                onOff.setChecked(!onOff.isChecked());
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return hotkeys(keyCode, event);
    }

      */
    public static void printPluginsAll () {
        Log.d(TAG, "printPlugins: " + String.format(
                "Shared libraries: %d", sharedLibrariesLV2.length
        ));

        for (int i = 0 ; i < sharedLibrariesLV2.length ; i ++) {
            Log.d(TAG, String.format("printPluginsAll [%d]: %s", sharedLibraries.length + i, sharedLibrariesLV2 [i]));
        }
    }

    public void saveCollection () {
        ArrayList selectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        List<CharSequence> strings = new ArrayList<>();
        if (presets.fragmentStateAdapter.myPresets.myPresetsAdapter == null) {
            alert("Load presets first", "To save preset collection to file, switch to the Presets tab from the bottom navigation menu");
            return;
        }
        for (int i = 0 ; i < presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.size() ; i ++) {
            strings.add(presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.get(i).get("name").toString());
        }

        builder.setTitle("Select presets for collection")
                .setMultiChoiceItems((strings.toArray(new
                                CharSequence[strings.size()])), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    if (which < selectedItems.size())
                                        selectedItems.remove(which);
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the selectedItems results somewhere
                        // or return them to the component that opened the dialog
                        Log.d(TAG, "onClick: " + selectedItems.toString());
                        JSONObject collection = new JSONObject();
                        for (int i = 0 ; i < presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.size() ; i ++) {
                            if (selectedItems.contains(i)) {
                                try {
                                    JSONObject jsonObject = new JSONObject(presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.get(i)) ;
                                    collection.put(String.valueOf(i), jsonObject);
                                } catch (JSONException e) {
                                    Log.e(TAG, "onClick: ", e);
                                }
                            }
                        }

                        AlertDialog.Builder _Builder1 = new AlertDialog.Builder(mainActivity);
                        _Builder1.setTitle("Enter file name for preset collection");
                        LinearLayout linearLayout = new LinearLayout(context);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        TextView textView = new TextView(context);
                        textView.setText("File will be saved to " + context.getExternalFilesDir(
                                Environment.DIRECTORY_DOWNLOADS));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                        layoutParams.setMargins(10,10,10,10);
                        linearLayout.setLayoutParams(layoutParams);
                        linearLayout.setPadding(50,50,50,50);
                        linearLayout.addView(textView);

                        EditText editText = new EditText(context);
                        linearLayout.addView(editText);
                        _Builder1.setView(linearLayout).
                                setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String filename = String.valueOf(editText.getText());

                                        if (filename != null) {
                                            File file = new File(context.getExternalFilesDir(
                                                    Environment.DIRECTORY_DOWNLOADS) + "/" + filename +".txt");
                                            FileOutputStream stream = null;
                                            try {
                                                stream = new FileOutputStream(file);
                                            } catch (FileNotFoundException e) {
                                                alert("Cannot write file", e.getMessage());
                                                throw new RuntimeException(e);
                                            }

                                            if (stream != null) {
                                                try {
                                                    String data = collection.toString(4);
                                                    stream.write(data.getBytes());
                                                    Log.d(TAG, "onClick: " + data);
                                                    stream.close();
                                                } catch (IOException e) {
                                                    alert ("Cannot write file", e.getMessage());
                                                    Log.e(TAG, "onClick: ", e);
                                                } catch (JSONException e) {
                                                    alert ("Cannot write file", e.getMessage());
                                                    Log.e(TAG, "onClick: ", e);
                                                }

                                                alert("Wrote file succesfully",
                                                        String.format(
                                                                "Collection saved to file:\n%s",
                                                                file.getName()
                                                        ));
                                            }

                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", null);
                        Log.d(TAG, "onClick: show dilog");
                        _Builder1.create().show();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        Dialog dialog = builder.create() ;
        dialog.show();
    }

    public void AudioRecordTest () {
        Log.d(TAG, "AudioRecordTest: ");
        int min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord aRecord = null;

        aRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 48000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, 1024*4);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Audio Record Test");
        builder.setPositiveButton("Close", null);

        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView textView = new TextView(getApplicationContext());
        textView.setText("Toggle record");
        ToggleButton toggleButton = new ToggleButton(getApplicationContext());
        linearLayout.addView(textView);
        linearLayout.addView(toggleButton);
        builder.setView(linearLayout);
        int maxJitter = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack track = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxJitter, AudioTrack.MODE_STREAM);

        Dialog dialog = builder.create();
        Pitch p = new Pitch(48000);
        AudioRecord finalARecord = aRecord;
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    finalARecord.startRecording();
                    float[] lin = new float[1024*4];
                    while (finalARecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int num = finalARecord.read(lin, 0, 1024 *4, AudioRecord.READ_NON_BLOCKING);
                        double pitchFrequency = p.computePitchFrequency (lin);
                        Log.d(TAG, "onCheckedChanged: " + lin [0] + " " + pitchFrequency);
//                        break ;

                    }
                } else {
                    finalARecord.stop();
                }
            }
        } ;

        toggleButton.setOnCheckedChangeListener(listener);
        dialog.show();
    }

    // ---- restored from git HEAD (methods lost during cleanup) ----

    public boolean isHeadphonesPlugged(){
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for(AudioDeviceInfo deviceInfo : audioDevices){
            if(Arrays.stream(safeAudioDevices).anyMatch(x -> x == deviceInfo.getType())){
                return true;
            }
        }

        return false;
    }


    public String getFileContent(Uri uri) {
        InputStreamReader inputStreamReader = null;
        try {
            if (!uri.toString().startsWith("/"))
                inputStreamReader = new InputStreamReader(getContentResolver().openInputStream(uri));
            else {
                inputStreamReader = new InputStreamReader(new FileInputStream(new File(String.valueOf(uri))));
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "getFileContent: ", e);
//            throw new RuntimeException(e);
            return null;
        }

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder sb = new StringBuilder();
        String s;
        while (true) {
            try {
                if (!((s = bufferedReader.readLine()) != null)) break;
            } catch (IOException e) {
                Log.e(TAG, "getFileContent: ", e);
                throw new RuntimeException(e);
            }

            sb.append(s);
        }
        String fileContent = sb.toString();
        return fileContent;
    }


    public void unzipNAMModel (String dir, Uri uri) {
        InputStream inputStream = null;
        String basename = null ;
        try {
            inputStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            MainActivity.alert("Cannot load model", e.getMessage());
            Log.e(TAG, "onActivityResult: ", e);
        }
        try {
            basename = unzip(inputStream, dir);
        } catch (IOException e) {
            MainActivity.alert("Cannot unzip model", e.getMessage());
            Log.e(TAG, "onActivityResult: ", e);
        }

        toast("Successfully extracted Model " + basename);
    }

    private static String unzip(InputStream zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        String dirname = null ;
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(zipFilePath);
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            Log.d(TAG, String.format("[unzip] %s: %s", destDirectory, entry.getName()));
            String filePath = destDirectory + File.separator + entry.getName();
            if (dirname == null)
                dirname = entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
        return dirname;
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[1024];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }


    public static void writeFile (String path, String text) {
        try {
            File file = new File(path);

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "writeFile: ", e);
            toast(e.getMessage());
        }
    }


    private void copyFile(Uri pathFrom, Uri pathTo) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(pathFrom)) {
            if(in == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(pathTo)) {
                if(out == null) return;
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }


    void setMidiControl (View view, int plugin, int control, MIDIControl.Type type, MIDIControl.Scope scope) {
        Log.i(TAG, String.format(
                "[set midi control] plugin: %d, control: %d",
                plugin, control
        ));

        String channel = "";
        String data1 = "";

        MIDIControl old = null;
        
        for (MIDIControl midiControl: midiControls) {
            Log.d(TAG, String.format ("[midi plugin check] %d: %d",
                    midiControl.view.getId(), view.getId()
                    ));

            if (midiControl.view.getId() == view.getId()) {
                old = midiControl ;
                data1 = String.valueOf(midiControl.control);
                channel = String.valueOf(midiControl.channel);
                break ;
            }
        }

        MIDIControl midiControl = new MIDIControl() ;
        midiControl.plugin = plugin ;
        midiControl.scope = scope ;
        midiControl.type = type;
//        if (scope == MIDIControl.Scope.GLOBAL) {
//            midiControl.type = MIDIControl.Type.TOGGLE;
//        }
//        midiControl.control = control ;
        midiControl.pluginControl = control ;
        midiControl.view = view ;

        midiLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.midi_add_control, null);
        ((EditText) midiLayout.findViewById(R.id.channel)).setText(channel);
        ((EditText) midiLayout.findViewById(R.id.control)).setText(data1);

        triggerMidiButton = midiLayout.findViewById(R.id.midi_trigger);

        channelEdit = midiLayout.findViewById(R.id.channel) ;
        controlEdit = midiLayout.findViewById(R.id.control) ;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(midiLayout);
        if (scope == MIDIControl.Scope.PLUGIN)
            builder.setTitle("Set MIDI Control for " + AudioEngine.getActivePluginName(plugin) + " " + AudioEngine.getControlName(plugin, control));
        else
            builder.setTitle("Set MIDI Control");

        MIDIControl finalOld = old;
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (finalOld != null)
                    midiControls.remove(finalOld);
                String channel = ((EditText) midiLayout.findViewById(R.id.channel)).getText().toString();
                String program = ((EditText) midiLayout.findViewById(R.id.control)).getText().toString();

                int ch = -1 ;
                int pr =  -1 ;

                try {
                    pr = Integer.parseInt(program);
                    ch = Integer.parseInt(channel);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "onDismiss: ", e);
                    alert("Failed to set MIDI control", e.getMessage());
                    return;
                }

                midiControl.channel = ch ;
                midiControl.control = pr ;
                midiControls.add(midiControl);

                Toast.makeText(MainActivity.this, "MIDI control set successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "[midi controls]: " + midiControls.toString());
                if (scope == MIDIControl.Scope.GLOBAL) {
                    saveGlobalMidi();
                } else {
                    saveActivePreset();
                }
            }
        }) ;

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create() ;

        midiAddDialog = dialog ;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.i(TAG, "onDismiss: midi add dialog set to null");
                midiAddDialog = null ;
            }
        });

        dialog.show();
    }


    void processMIDIMessage (int channel, int data1, int data2) {
//        Log.d(TAG, "processMIDIMessage: process message " +
//                String.format("%d %d %d", channel, data1, data2));
        String msg = String.format(
                "%d %d %d",
                channel, data1, data2
        );

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                midiDisplay.setText(msg);

                if (midiAddDialog != null && triggerMidiButton != null && triggerMidiButton.isChecked()) {
                    EditText ed1 = ((EditText) midiAddDialog.findViewById(R.id.channel));
                    if (ed1 != null)
                        ed1.setText(String.valueOf(channel));
                    EditText ed2 = ((EditText) midiAddDialog.findViewById(R.id.control));
                    if (ed2 != null)
                        ed2.setText(String.valueOf(data1));
                }
            }
        });

        for (MIDIControl midiControl: midiControls) {
//            Log.i(TAG, "processMIDIMessage: probe midiControl " + midiControl);

            if (midiControl.channel == channel && data1 == midiControl.control) {
                midiControl.process(data2);
                break ;
            }
        }
    }


    void loadGlobalMidi () {
        Log.i(TAG, "loadGlobalMidi: loading settings");
        String s = defaultSharedPreferences.getString("global_midi", null);
        if (s == null) {
            Log.i(TAG, "loadGlobalMidi: no saved settings");
            return;
        } else {
            Log.i(TAG, "loadGlobalMidi: saved settings found " + s);
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(s);
            for (int i = 0 ; i < jsonArray.length(); i ++) {
                JSONObject j = jsonArray.getJSONObject(i);
                MIDIControl midiControl = new MIDIControl();
                midiControl.scope = MIDIControl.Scope.GLOBAL;
                String idString = j.getString("view");
                if (idString.isEmpty()) {
                    Log.w(TAG, "loadGlobalMidi: invalid settings, skipping " + j);
                    continue;
                }
                midiControl.view = findViewById(stringToId(idString));
                switch (j.getString ("type")) {
                    case "KNOB":
                        midiControl.type = MIDIControl.Type.KNOB ;
                        break ;
                    case "SLIDER":
                        midiControl.type = MIDIControl.Type.SLIDER ;
                        break ;
                    case "TOGGLE":
                    default:
                        midiControl.type = MIDIControl.Type.TOGGLE ;
                        break ;
                }

                midiControl.scope = MIDIControl.Scope.GLOBAL;
                midiControl.channel = j.getInt("channel");
                midiControl.control = j.getInt("control");
                midiControls.add(midiControl);
                Log.i(TAG, "loadGlobalMidi: added control " + midiControl);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }


    void saveGlobalMidi () {
        JSONArray jsonArray = new JSONArray();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.PLUGIN || midiControl.view == null)
                continue;

            try {
                JSONObject jsonObject = midiControl.get() ;
                Log.i(TAG, "saveGlobalMidi: " + jsonObject.toString());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(TAG, "saveGlobalMidi: " + jsonArray.toString());
        defaultSharedPreferences.edit().putString("global_midi", jsonArray.toString()).apply();
    }


    void resetPluginMIDI () {
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL)
                continue;

            midiControls.remove(midiControl);
        }
    }


    String midiControlsForDB (int plugin) {
        StringBuilder s = new StringBuilder();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL|| midiControl.plugin != plugin)
                continue;

            s.append(midiControl.getForDB()).append("|");
        }

        return s.toString();
    }


    JSONObject midiPluginControlsAsJSON () {
        JSONObject jsonObject = new JSONObject();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL)
                continue;

            try {
                if (! jsonObject.has(String.valueOf(midiControl.plugin)))
                    jsonObject.put(String.valueOf(midiControl.plugin), new JSONArray());
                jsonObject.getJSONArray(String.valueOf(midiControl.plugin)).put(midiControl.get());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(TAG, "midiPluginControlsAsJSON: " + jsonObject);
        return jsonObject;
    }


    void scanBLE () {
        if (Build.VERSION.SDK_INT < 33) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth MIDI only supported on Android 13 and above.");
            builder.setMessage("To use a Bluetooth MIDI Controller with Amp Rack, install MIDI BLE Connect from the Play store, connect to your BLE MIDI Controller and then restart Amp Rack");
            builder.setPositiveButton("Install MIDI BLE Connect", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String url = "https://play.google.com/store/apps/details?id=com.mobileer.example.midibtlepairing";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });

            builder.show();
            return;
        }

        deviceManager =
                (CompanionDeviceManager) getSystemService(
                        Context.COMPANION_DEVICE_SERVICE
                );

        // To skip filtering based on name and supported feature flags,
        // do not include calls to setNamePattern() and addServiceUuid(),
        // respectively. This example uses Bluetooth.
        BluetoothDeviceFilter deviceFilter =
                new BluetoothDeviceFilter.Builder()
                        .setNamePattern(Pattern.compile("My device"))
                        .addServiceUuid(
                                new ParcelUuid(new UUID(0x123abcL, -1L)), null
                        )
                        .build();

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of device names is presented to the user as
        // pairing options.
        AssociationRequest pairingRequest = new AssociationRequest.Builder()
//                .addDeviceFilter(deviceFilter)
//                .setSingleDevice(true)
                .build();

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest, executor, new CompanionDeviceManager.Callback() {
            // Called when a device is found. Launch the IntentSender so the user can
            // select the device they want to pair with.
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                try {
                    startIntentSenderForResult(
                            chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                    );
                } catch (IntentSender.SendIntentException e) {
                    Log.e("MainActivity", "Failed to send intent");
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            public void onAssociationCreated(AssociationInfo associationInfo) {
                // AssociationInfo object is created and get association id and the
                // macAddress.
                int associationId = associationInfo.getId();
                MacAddress macAddress = associationInfo.getDeviceMacAddress();
            }

            @Override
            public void onFailure(CharSequence errorMessage) {
                // Handle the failure.
            }
        });
    }


    void midiConnect (MidiDeviceInfo midiDeviceInfo, int outputPort) {
        int finalOutputPort = outputPort;
        Log.d(TAG, String.format ("[midi] opening device: %s", midiDeviceInfo.toString()));
        try {
            midiManager.openDevice(midiDeviceInfo, device -> {
                midiDevice = device;
                Log.d(TAG, String.format("[midi] device opened: opening port..."));
                midiOutputPort = device.openOutputPort(finalOutputPort);
                Log.d(TAG, String.format("[midi] port opened: port %d", midiOutputPort.getPortNumber()));
                midiOutputPort.connect(midiReciever);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (midiDevice == null)
                            return;

                        Log.i(TAG, "[bt] run: connected device " + midiDevice.getInfo().getType() + " >" + midiDeviceInfo.getProperties().getString("name", ""));
                        ((TextView) findViewById(R.id.midi_name)).setText(midiDeviceInfo.getProperties().getString("name", ""));
                        if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_USB || midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
                            (findViewById(R.id.midi_icon)).setVisibility(VISIBLE);
                            (findViewById(R.id.midi_icon)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    selectMidiDevice();
                                }
                            });
                        } else if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                            (findViewById(R.id.bt_icon)).setVisibility(VISIBLE);
                            (findViewById(R.id.bt_icon)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    detachBLE();
                                }
                            });
                        }
                    }
                });
            }, null);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
//                    Toast.makeText(mainActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public static abstract class OnEngineStartListener {
        abstract void run () ;

        OnEngineStartListener () {
            engineStartListeners.add(this);
        }
    }


    private static final int SELECT_DEVICE_REQUEST_CODE = 100000;


    // ==== additional restored from HEAD ====
    public int [] safeAudioDevices = {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_AUX_LINE,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_IP,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET
    } ;


    public static void setAudioDevice () {
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        audioDevicesInput = mainActivity.audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = mainActivity.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        String current_in = mainActivity.defaultSharedPreferences.getString("input", "");
        String current_out = mainActivity.defaultSharedPreferences.getString("output", "");

        if (current_out == "")
            current_out = "0" ;
        if (current_in == "")
            current_in = "0";

        Log.d(TAG, String.format ("%s: %s", current_in, current_out));
        int cin = 0, cout = 0, counter = 0 ; ;

        for (AudioDeviceInfo audioDeviceInfo: audioDevicesInput) {
            Log.d(TAG, String.format("<%s> [%s] %s: %s", current_in, audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
            if (Integer.parseInt(current_in) == audioDeviceInfo.getId())
                cin = counter + 1;
            counter ++ ;
        }

        counter = 0 ;
        for (AudioDeviceInfo audioDeviceInfo: audioDevicesOutput) {
            Log.d(TAG, String.format("%b <%s> [%s] %s: %s",
                    Integer.valueOf(current_out) == Integer.valueOf(audioDeviceInfo.getId()),
                    current_out, audioDeviceInfo.getType(),
                    audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
            if (Integer.parseInt(current_out) == audioDeviceInfo.getId())
                cout = counter + 1;
            counter ++ ;
        }
        Log.d(TAG, String.format ("[defaults] %d: %d", cin, cout));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Get the layout inflater
        LayoutInflater inflater = mainActivity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.audio_devices_selector, null);
        builder.setView(linearLayout)
                // Add action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        /*
                        try {
                            String in = ((EditText) linearLayout.findViewById(R.id.custom_input_device)).getText().toString() ;
                            if (!in.isEmpty()) {
                                int dev = Integer.parseInt(in) ;
                                AudioEngine.setRecordingDeviceId(dev);
                                Log.d(TAG, String.format ("set record dev: %d", dev));
                                mainActivity.defaultSharedPreferences.edit().putString("input", String.valueOf(dev)).apply();
                            }
                            in = ((EditText) linearLayout.findViewById(R.id.custom_output_device)).getText().toString() ;
                            if (!in.isEmpty()) {
                                int dev = Integer.parseInt(in) ;
                                mainActivity.defaultSharedPreferences.edit().putString("output", String.valueOf(dev)).apply();
                                Log.d(TAG, String.format ("set playback dev: %d", dev));
                                AudioEngine.setPlaybackDeviceId(dev);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "onClick: ", e);
                        }

                         */
                    }
                });
                /*
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

                 */

        int i = 0;
//                HashMap<CharSequence, Integer> inputs = new HashMap<>();
//                HashMap <CharSequence, Integer> outputs = new HashMap<>();
        ArrayList<String> input_s = new ArrayList<>();
        ArrayList<String> output_s = new ArrayList<>();

        input_s.add ("Default");
        output_s.add ("Default");

        for (i = 0; i < audioDevicesInput.length; i++) {
            AudioDeviceInfo deviceInfo = audioDevicesInput[i] ;
            String name = String.format("[%d] %s (%s)", deviceInfo.getId(), deviceInfo.getProductName(), typeToString(deviceInfo.getType()));
//                    inputs.put(name, audioDevicesInput [i].getId()) ;
            input_s.add(name);
        }

        for (i = 0; i < audioDevicesOutput.length; i++) {
            AudioDeviceInfo deviceInfo = audioDevicesOutput[i] ;
            String name = String.format("[%d] %s (%s)", deviceInfo.getId(), deviceInfo.getProductName(), typeToString(deviceInfo.getType()));
//                    outputs.put(name, audioDevicesOutput [i].getId()) ;
            output_s.add(name);
        }

        ArrayAdapter input_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, input_s);
        input_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter output_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, output_s);
        output_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner in = (Spinner) linearLayout.findViewById(R.id.input_devices);
        Spinner out = (Spinner) linearLayout.findViewById(R.id.output_devices);
        in.setAdapter(input_a);
        out.setAdapter(output_a);

        in.setSelection(cin);
        out.setSelection(cout);

        in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    mainActivity.defaultSharedPreferences.edit().putString("input", "-1").apply();
                    return ;
                }

                i-- ;
                mainActivity.defaultSharedPreferences.edit().putString("input", String.valueOf(audioDevicesInput[i].getId())).apply();
//                AudioEngine.setRecordingDeviceId(audioDevicesInput[i].getId());
                mainActivity.defaultInputDevice = i;
                Log.d(TAG, String.format ("set recording device: %d", audioDevicesInput[i].getId()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        out.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    mainActivity.defaultSharedPreferences.edit().putString("output", "-1").apply();
                    return ;
                }

                i-- ;
                AudioEngine.setPlaybackDeviceId(audioDevicesOutput[i].getId());
                mainActivity.defaultSharedPreferences.edit().putString("output", String.valueOf(audioDevicesOutput[i].getId())).apply();
                mainActivity.defaultInputDevice = i;
                Log.d(TAG, String.format ("set playback device: %d", audioDevicesOutput[i].getId()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        builder.show();
    }


    public static void about () {
        Intent intent = new Intent(mainActivity, About.class);
        mainActivity.startActivity(intent);

    }


    public static void lowLatencyDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Latency Warning")
                .setMessage(R.string.low_latency_warning)
                .setNegativeButton("Close", null)
                .setIcon(R.drawable.baseline_warning_24);
        builder.create().show();
    }


    public void addFavoritePreset (Map preset) {
        JSONObject jsonObject = new JSONObject(preset);
        File f = new File(new StringJoiner ("/").add(favPresetsDir).add (preset.get("name").toString()).toString());
        if (! f.exists())
            writeFile(f.getPath(), jsonObject.toString());
    }


    public void removeFavoritePreset (Map preset) {
        File f = new File(new StringJoiner ("/").add(favPresetsDir).add (preset.get("name").toString()).toString());
        if (! f.delete()) {
            Toast.makeText(context, "Cannot remove preset " + preset.get("name"), Toast.LENGTH_SHORT).show();
        }
    }


    public static Map JSONtoMap (JSONObject j) {
        try {
            Log.d(TAG, String.format ("json: %s", j.toString(4)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

//        Log.d(TAG, String.format ("%s: %s"));
        HashMap <String,Object> map = new HashMap();
        try {
            String [] params = {
                    "name",
                    "uid",
                    "desc",
                    "path",
                    "public",
                    "timestamp",
                    "likes"
            } ;

            for (String s: params)
                if (j.has(s))
                    map.put(s, j.get(s));

            HashMap <String, HashMap> hashMap = new HashMap();
            map.put("controls", hashMap);

            // todo: do each control individually
            JSONObject controls = j.getJSONObject ("controls");
            Iterator<String> keys = controls.keys();
            if (! keys.hasNext()) {
                Log.e(TAG, "JSONtoMap: no controls found! error! error!");
                return map;
            }
            String next = keys.next();
            while (keys.hasNext()) {
                JSONObject settings = controls.getJSONObject(next);
                HashMap <String, Object> set = new HashMap<>();

                set.put("name", settings.get("name"));
                set.put ("controls", ((String) settings.get("controls")).split (";"));
                hashMap.put(next, set);

                next = keys.next();
            }

        } catch (JSONException e) {
            Toast.makeText(mainActivity, "Cannot load preset " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "JSONtoMap: ", e);
            return null ;
        }

        return map;
    }


    int stringToId (String s) {
        return getResources().getIdentifier(s, "id", getPackageName());
    }


    Executor executor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };


    void selectMidiDevice () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.midi_select, null);
        builder.setView(linearLayout);

        MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
        Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));

        ArrayList <String> devices = new ArrayList<>();
        for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
            Log.d(TAG, String.format("[midi device] %s: %s",
                    midiDeviceInfo.getId(), midiDeviceInfo.toString()));
            devices.add(midiDeviceInfo.getProperties().getString("name", midiDeviceInfo.toString()));
            Log.d(TAG, String.format("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
        }

        Spinner dSpinner = linearLayout.findViewById(R.id.select_midi_device);
        Spinner pSpinner = linearLayout.findViewById(R.id.select_midi_port);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, devices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dSpinner.setAdapter(adapter);

        int x = 0;
        if (midiLastConnectedDevice != null) {
            for (String d : devices) {
                if (d.equals(midiLastConnectedDevice)) {
                    dSpinner.setSelection(x);
                    break;
                }

                x++;
            }
        }

        dSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> ports = new ArrayList<>();
                for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfos[position].getPorts()) {
                    Log.d(TAG, String.format ("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                        int outputPort = portInfo.getPortNumber();
                        Log.d(TAG, String.format ("[midi port] output port: %d", outputPort));
                        ports.add(String.valueOf(outputPort));
                        break ;
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, ports);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pSpinner.setAdapter(adapter);

                if (midiLastConnectedPort != -1) {
                    int x = 0 ;
                    for (MidiDeviceInfo.PortInfo p: midiDeviceInfos [position].getPorts()) {
                        if (midiLastConnectedPort == p.getPortNumber()) {
                            pSpinner.setSelection(x);
                            break;
                        }

                        x++;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        builder.setTitle("Select MIDI Device / Port");
        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int btn) {
                if (midiDevice != null) {
                    try {
                        midiDevice.close();
                    } catch (IOException e) {
                        Log.e(TAG, "onClick: ", e);
                    }
                }

                int which = dSpinner.getSelectedItemPosition();

                Log.i(TAG, "onClick: select device " + which );
                if (which == -1) {
                    Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                midiManager.openDevice(midiDeviceInfos [which], device -> {
                    midiDevice = device ;
                    Log.d(TAG, String.format ("[midi] device opened: opening port..."));

                    midiOutputPort = device.openOutputPort(pSpinner.getSelectedItemPosition());
                    midiLastConnectedDevice = midiDeviceInfos [which].getProperties().getString("name", midiDeviceInfos [which].toString());
                    midiLastConnectedPort = pSpinner.getSelectedItemPosition() ;
                    Log.i(TAG, "onClick: [midi] save midi device " + midiLastConnectedDevice + " " + midiLastConnectedPort);
                    defaultSharedPreferences.edit().putString("last_midi", midiLastConnectedDevice).commit();
                    defaultSharedPreferences.edit().putInt("last_midi_port", midiLastConnectedPort).commit();

                    Log.d(TAG, String.format ("[midi] port opened: port %d", midiOutputPort.getPortNumber()));
                    midiOutputPort.connect(midiReciever);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "[bt] run: connected device " + midiDevice.getInfo().getType());
                            ((TextView) findViewById(R.id.midi_name)).setText(midiDeviceInfos [which].getProperties().getString("name", ""));
                            if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_USB || midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
                                (findViewById(R.id.midi_icon)).setVisibility(VISIBLE);
                                (findViewById(R.id.midi_icon)).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        selectMidiDevice();
                                    }
                                });
                            }
                            else if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                                (findViewById(R.id.bt_icon)).setVisibility(VISIBLE);
                                (findViewById(R.id.bt_icon)).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        detachBLE();
                                    }
                                });
                            }
                        }
                    });
                }, null);

                Toast.makeText(MainActivity.this, "MIDI Device connected", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }


    void detachBLE () {
        Log.i(TAG, "detachBLE: ");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Disconnect and forget Bluetooth device?");
        builder.setPositiveButton("Disconnect and forget", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                defaultSharedPreferences.edit ().remove("last_bt").commit();
                if (deviceToPair != null) {
                    deviceToPair = null ;
                    if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                        try {
                            midiDevice.close();
                            ((TextView) findViewById(R.id.midi_name)).setText("");
                        } catch (IOException e) {
                            Log.e(TAG, "onClick: ", e);
                        }
                    }
                }

                Toast.makeText(mainActivity, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                (findViewById(R.id.bt_icon)).setVisibility(GONE);
            }
        });

        builder.show();
    }


    void midiMappingsDialog () {
        ArrayList <Integer> selectedItems = new ArrayList();  // Where we track the selected items
        ArrayList <String> mappings = new ArrayList<>();
        for (MIDIControl control: midiControls) {
            if (control.plugin != -1) {
                mappings.add(String.format(
                        "[%d %d] %s %s",
                        control.channel, control.control,
                        AudioEngine.getActivePluginName(control.plugin),
                        AudioEngine.getControlName(control.plugin, control.pluginControl)
                ));
            } else {
                mappings.add(String.format(
                        "[%d %d] %s",
                        control.channel, control.control,
                        control.getID()
                ));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title.
        builder.setTitle("MIDI Mappings")
                // Specify the list array, the items to be selected by default (null for
                // none), and the listener through which to receive callbacks when items
                // are selected.
                .setMultiChoiceItems((CharSequence[]) mappings.toArray(new CharSequence [mappings.size()]), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checks the item, add it to the selected
                                    // items.
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // If the item is already in the array, remove it.
                                    selectedItems.remove(which);
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User taps OK, so save the selectedItems results
                        // somewhere or return them to the component that opens the
                        // dialog.
                        for (int i: selectedItems) {
                            Log.i(TAG, "onClick: [midi control] remove " + midiControls.get(i));
                            midiControls.remove(i);
                            Toast.makeText(MainActivity.this, "Selected mappings removed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton("Close", null)
                .setNegativeButton("Remove all", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        midiControls.clear();
                        Toast.makeText(MainActivity.this, "MIDI Mappings cleared", Toast.LENGTH_SHORT).show();
                    }
                });

        builder.show();
    }


    public static int setSpinnerFromDir (Spinner spinner, String dir, String toSelect) {
        if (spinner == null) {
            return 0 ;
        }

        int selection = 0 ;
        ArrayList <String> models = new ArrayList<>();
        DocumentFile root = DocumentFile.fromFile(new File(dir));
        DocumentFile [] files = root.listFiles() ;
        int counter = 0 ;
        for (DocumentFile file: files) {
            Log.d(TAG, String.format ("%s: %s", file.getName(), file.getUri()));
            models.add(file.getName());
            if (toSelect != null && file.getName().equals(toSelect))
                selection = counter ;
            counter ++ ;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return selection;
    }

    public static void pushToVideo(float[] buffer, int len) {
        // JNI callback from native audio engine for meter/visualizer data
        // Stub implementation - no-op for now
    }

    public static void setSampleRateDisplay(int rate, boolean precision) {
        // JNI callback from native audio engine for sample rate display
        // Stub implementation - no-op for now
    }

}
