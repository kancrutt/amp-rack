package com.kancrutt.gigginggear;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SkinEngine {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName() ;
    String theme = "Adwaita" ;
    int nativeTheme = R.style.Theme_AmpRack;
    String themeDir = "themes/Adwaita/";
    HashMap <String, HashMap <String, String>> config = new HashMap<>();
    JSONObject jsonConfig ;
    Skinner skinner ;
    public boolean custom = false ;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    Display display ;
    Uri themeUri ;
    HashMap<String, Uri> themeFiles = new HashMap<>();
    HashMap<String, Bitmap> bitmaps = new HashMap<>();
    int screenWidth = 1800, screenHeight = 2400 ;
    float scaleFactor = 1 ;

    Paint paint ;

    static void setColorScheme (MainActivity mainActivity, String colorScheme) {
        switch (colorScheme) {
            case "Theme.Light":
                mainActivity.setTheme (R.style.Theme_Light);
                break;

            case "Theme.AmpRack":
            default:
                mainActivity.setTheme (R.style.Theme_AmpRack);
                break;
        }
    }

    void setTheme (String _theme) {
        theme = _theme ;
        if (theme.startsWith("content://") || theme.startsWith("/"))
            custom = true ;
        Log.d(TAG, "setTheme() called with: _theme = [" + _theme + "]\ncustom = " + custom);
        if (! custom) {
            themeDir = theme;
            themeDir = String.format("themes/%s/", theme);
            jsonConfig = ConnectGuitar.loadJSONFromAssetFile(mainActivity, themeDir + "theme.json");
            if (jsonConfig == null) {
                MainActivity.toast("Theme loading failed: " + _theme);
                setTheme("TubeAmp");
                return;
            }
        }
        else {
            if (theme.startsWith("/"))
                themeUri = Uri.fromFile(new File(theme));
            else
                themeUri = Uri.parse(theme);
            themeDir= "" ;
            getUriFileList(themeUri);
            Uri _json = themeFiles.get("theme.json");
            jsonConfig = ConnectGuitar.loadJSONFromFile(mainActivity, _json);
        }
        Log.d(TAG, "setTheme: " + jsonConfig.toString());
        load ();
    }

    SkinEngine (MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        skinner = new Skinner(mainActivity);
        skinner.skinEngine = this ;
        skinner.init();
        paint = new Paint();
        setTheme("Adwaita"); // sane default
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        display = mainActivity.getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth() ;
        if (_mainActivity.tabletMode)
            screenWidth = screenWidth / 2;
        screenHeight = display.getHeight();
        scaleFactor = screenWidth / 1120f ;

        Log.d(TAG, "SkinEngine: " + displayMetrics);
    }

    void load () {
        JSONObject hConfig ;
        Iterator<String> keys = jsonConfig.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                if (jsonConfig.get(key) instanceof JSONObject) {
                    JSONObject object = jsonConfig.getJSONObject(key);
                    Iterator<String> keys1 = object.keys();
                    HashMap <String, String> hashMap = new HashMap();
                    while (keys1.hasNext()) {
                        String key2 = keys1.next();
                        hashMap.put(key2, object.getString(key2));
                    }

                    config.put(key, hashMap);
                }
            } catch (JSONException e) {
                Log.e(TAG, "load: " + key, e);
            }
        }

        Log.d(TAG, config.toString());
    }

    void wallpaper (ImageView wall) {
        String wallpaper = PreferenceManager.getDefaultSharedPreferences(mainActivity).getString("background", null);
        if (wallpaper == null)
            wallpaper = config.get("wallpaper").get("bg") ;

        Log.d(TAG, "wallpaper: " + wallpaper);

        if (wallpaper.startsWith("#")) {
            wall.setImageBitmap(null);
            wall.setBackgroundColor(Color.parseColor(wallpaper));
        } else {
            String customBg = mainActivity.defaultSharedPreferences.getString("background", null);
            Bitmap bg = null ;
            if (!wallpaper.startsWith("content://"))
                bg = skinner.getBitmapFromAssets( skinner.displayMetrics.widthPixels, -1, themeDir + wallpaper);
            if (customBg == null)
                wall.setImageBitmap(bg);
            else {
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(mainActivity.getContentResolver(), Uri.parse(customBg));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                if (bitmap != null) {
                    wall.setCropToPadding(true);
                    wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    wall.setImageBitmap(bitmap);
                }
                else
                    wall.setImageBitmap(bg);
            }
        }
    }

    void header (LinearLayout layout) {
        if (layout == null || config == null || config.get("header") == null) return;
        try {
            String hBg = config.get("header").get("bg");
            String textColor = config.get("button") != null ? config.get("button").get("text-color") : "#FFFFFF";
            TextView appTitle = mainActivity.findViewById(R.id.app_main_title);
            if (appTitle != null && textColor != null) {
                appTitle.setTextColor(Color.parseColor(textColor));
            }

            ToggleButton record = mainActivity.findViewById(R.id.record_button);
            if (record != null && textColor != null) {
                record.setTextColor(Color.parseColor(textColor));
            }

            if (hBg != null && skinner != null && skinner.displayMetrics != null) {
                Bitmap headerBg = skinner.getBitmapFromAssets(skinner.displayMetrics.widthPixels, -1, themeDir + hBg);
                if (headerBg != null) {
                    layout.setBackground(new Drawable() {
                        @Override
                        public void draw(@NonNull Canvas canvas) {
                            setBounds(0, 0, headerBg.getWidth(), headerBg.getHeight());
                            canvas.drawBitmap(headerBg, 0, 0, paint);
                        }

                        @Override public void setAlpha(int i) {}
                        @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {}
                        @Override public int getOpacity() { return 0; }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "header: skin error", e);
        }
    }

    enum Resize {
        Width,
        Height,
        None
    }

    void fab (ExtendedFloatingActionButton _button, Resize resize, float factor) {
        if (_button == null || config == null || config.get("icons") == null) return;
        try {
            _button.hide();
            String addIcon = config.get("icons").get("add");
            if (addIcon != null && skinner != null) {
                Bitmap bitmap = skinner.getBitmapFromAssets(-1, 0, themeDir + addIcon);
                if (bitmap != null) {
                    BitmapDrawable drawable = new BitmapDrawable(bitmap);
                    _button.setIcon(drawable);
                }
            }
            button (_button, resize, factor);
            _button.show();
        } catch (Exception e) {
            Log.e(TAG, "fab: skin error", e);
        }
    }

    void button (Button button, Resize resize, float factor) {
        if (button == null || config == null || config.get("button") == null) return;
        try {
            view (button, "button", "bg", Resize.Width, 1);
            String txtColor = config.get("button").get("text-color");
            if (txtColor != null) {
                button.setTextColor(Color.parseColor(txtColor));
            }
            button.setPadding(20,10,20,10);
        } catch (Exception e) {
            Log.e(TAG, "button: skin error", e);
        }
    }

    void view (View _view, String category, String name, Resize resize, float factor) {
        if (_view == null || config == null || config.get(category) == null) return;
        try {
            String filename = config.get(category).get(name) ;
            if (filename == null) return;
            _view.setBackground(new Drawable() {
                @Override
                public void draw(@NonNull Canvas canvas) {
                    int w = _view.getWidth(), h = _view.getHeight() ;
                    if (w <= 0 || h <= 0) return;
                    Bitmap b = null;
                    if (resize == Resize.Width)
                        b = skinner.getBitmapFromAssets((int) ((float) w * factor), -1, themeDir + filename);
                    else if (resize == Resize.Height)
                        b = skinner.getBitmapFromAssets(-1 , (int) ((float) h * factor), themeDir + filename);
                    else
                        b = skinner.getBitmapFromAssets(w , h, themeDir + filename);
                    if (b == null || b.isRecycled()) return;
                    setBounds(0, 0, w, h);
                    canvas.drawBitmap(b, (w - b.getWidth()) / 2, (h - b.getHeight()) / 2, paint);
                }

                @Override public void setAlpha(int i) {}
                @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {}
                @Override public int getOpacity() { return 0; }
            });
        } catch (Exception e) {
            Log.e(TAG, "view: skin error", e);
        }
    }

    BitmapDrawable bitmapDrawable (String category, String name) {
        if (config == null || config.get(category) == null || config.get(category).get(name) == null) return null;
        try {
            Bitmap b = skinner.getBitmapFromAssets(0 , 0, themeDir + config.get(category).get(name));
            if (b != null) return new BitmapDrawable(b);
        } catch (Exception e) {
            Log.e(TAG, "bitmapDrawable: skin error", e);
        }
        return null;
    }

    void drawableLeft (Button _view, String category, String name, Resize resize, float factor) {
        if (_view == null) return;
        _view.setCompoundDrawables(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                if (_view == null) return;
                int w = _view.getWidth(), h = _view.getHeight() ;
                if (w <= 0 || h <= 0) return;
                if (config == null || config.get(category) == null || config.get(category).get(name) == null) return;
                Bitmap b = null;
                try {
                    if (resize == Resize.Width)
                        b = skinner.getBitmapFromAssets((int) ((float) w * factor), -1, themeDir + config.get(category).get(name));
                    else if (resize == Resize.Height)
                        b = skinner.getBitmapFromAssets(-1 , (int) ((float) h * factor), themeDir + config.get(category).get(name));
                    else
                        b = skinner.getBitmapFromAssets(0 , 0, themeDir + config.get(category).get(name));
                } catch (Exception e) {
                    return;
                }
                if (b == null || b.isRecycled()) return;
                setBounds(0, 0, w, h);
                if (resize == Resize.None) {
                    setBounds(0, 0, b.getWidth(), b.getHeight());
                    canvas.drawBitmap(b, 0, 0, paint);
                }
                else
                    canvas.drawBitmap(b, (w - b.getWidth()) / 2, 0, paint);
            }

            @Override
            public void setAlpha(int i) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        }, null, null, null);
    }

    void toggle (ToggleButton toggleButton, boolean state) {
        String on = config.get("toggle").get("on") ;
        if (! state) {
            on = config.get("toggle").get("off");
        }

        String finalOn = on;

        Drawable drawable = new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
//                Log.d(TAG, "toggle: draw() called with: canvas = [" + canvas + "]");
                final int w = toggleButton.getWidth(), h = toggleButton.getHeight() ;
                Bitmap b ;
                b = skinner.getBitmapFromAssets(w, -1, themeDir + finalOn);
//                setBounds(0, 0, w, h);
                canvas.drawBitmap(b, (w - b.getWidth()) / 2, (h - b.getHeight()) / 2, paint);
            }

            @Override
            public void setAlpha(int i) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        } ;

//        toggleButton.setButtonDrawable(null);
//        Bitmap b = skinner.getBitmapFromAssets(toggleButton.getWidth() , -1, themeDir + finalOn);
//        BitmapDrawable bitmapDrawable = new BitmapDrawable(b);
        toggleButton.setButtonDrawable(drawable);
    }

    void toggleWithKey (ToggleButton toggleButton, String key, String onState, String offState, boolean state) {
        if (toggleButton == null || config == null || config.get(key) == null || config.get(key).get(onState) == null)
            return;
        try {
            String on = config.get(key).get(onState) ;
            if (! state) {
                on = config.get(key).get(offState);
            }

            String finalOn = on;
            toggleButton.setCompoundDrawables(null,null,null,null);

            Drawable drawable = new Drawable() {
                @Override
                public void draw(@NonNull Canvas canvas) {
                    final int w = toggleButton.getWidth(), h = toggleButton.getHeight() ;
                    if (w <= 0 || h <= 0 || finalOn == null) return;
                    Bitmap b = skinner.getBitmapFromAssets(w , -1, themeDir + finalOn);
                    if (b != null && !b.isRecycled())
                        canvas.drawBitmap(b, (w - b.getWidth()) / 2, (h - b.getHeight()) / 2, paint);
                }

                @Override public void setAlpha(int i) {}
                @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {}
                @Override public int getOpacity() { return 0; }
            } ;

            toggleButton.setButtonDrawable(drawable);
        } catch (Exception e) {
            Log.e(TAG, "toggleWithKey: skin error", e);
        }
    }

    void setLogo (ImageView imageView) {
        if (imageView == null || config == null || config.get("header") == null || config.get("header").get("logo") == null) return;
        try {
            Bitmap b = skinner.getBitmapFromAssets(0, 0, themeDir + config.get("header").get("logo"));
            if (b != null)
                imageView.setImageBitmap(b);
        } catch (Exception e) {
            Log.e(TAG, "setLogo: skin error", e);
        }
    }

    void slider (Slider slider) {
        if (slider == null || config == null || config.get("slider") == null) return;
        try {
            String activeColorStr = config.get("slider").get("active");
            String baseColorStr = config.get("slider").get("base");
            if (activeColorStr != null && baseColorStr != null) {
                ColorStateList active = ColorStateList.valueOf(Color.parseColor(activeColorStr));
                ColorStateList base = ColorStateList.valueOf(Color.parseColor(baseColorStr));
                slider.setTrackInactiveTintList(base);
                slider.setTrackActiveTintList(active);
            }

            String thumbFile = config.get("slider").get("thumb");
            if (thumbFile != null && skinner != null) {
                Bitmap bg = skinner.getBitmapFromAssets(-1, 0, themeDir + thumbFile);
                if (bg != null) {
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(bg);
                    slider.setCustomThumbDrawable(bitmapDrawable);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "slider: skin error", e);
        }
    }

    void setNativeTheme () {
//        mainActivity.setTheme(nativeTheme);
        boolean dark = mainActivity.defaultSharedPreferences.getBoolean("dark_mode", true) ;
        String mTheme = dark ? "Theme.AmpRack" : "Theme.Light" ;
        Log.d(TAG, "setNativeTheme: " + mTheme);
        setColorScheme(mainActivity, mTheme);
    }

    void cardText (TextView textView) {
        if (textView == null || config == null || config.get("card") == null) return;
        String c_ = config.get("card").get("text-color") ;
        if (c_ == null && config.get("button") != null)
            c_ = config.get("button").get("text-color");
        if (c_ == null)
            return ;

        try {
            int color = Color.parseColor(c_);
            textView.setTextColor(color);
        } catch (Exception ignored) {}
    }

    void card (View layout) {
        int w = layout.getWidth(), h = layout.getHeight() ;
        /*
        Log.d(TAG, "card() called with: layout = [" + layout + "]" +
                String.format("\n%d x %d", w, h));

         */

        Bitmap topLeft = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top-left"));
        Bitmap topRight = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top-right"));
        Bitmap bottomLeft = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom-left"));
        Bitmap bottomRight = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom-right"));
        Bitmap top = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top"));
        Bitmap bottom = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom"));
        Bitmap left = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("left"));
        Bitmap right = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("right"));

        String bg = config.get("card").get("bg") ;
        int alpha = Integer.valueOf(config.get("card").get("alpha"));
        Bitmap bgBitmap = null ;
        if (bg.startsWith("#") == false) {
            bgBitmap = skinner.getBitmapFromAssets(0, -1, themeDir + bg);
        }

        Bitmap finalBgBitmap = bgBitmap;
        BitmapDrawable bitmapDrawable = new BitmapDrawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, w, h);
                if (bg.startsWith("#"))
                    canvas.drawColor(Color.parseColor(bg));
                else {
                    int j = 0, i = 0 ;
                    boolean drawing = true ;
                    while (drawing) {
                        /*
                        Log.d(TAG, "draw: " + String.format(
                                "i: %d\tj: %d\twidth: %d\theight: %d",
                                i, j, canvas.getWidth(), canvas.getHeight()
                        ));

                         */
                        canvas.drawBitmap(finalBgBitmap, i, j, paint);
                        i += finalBgBitmap.getWidth() ;
                        if (i > canvas.getWidth()) {
                            i = 0 ;
                            j += finalBgBitmap.getHeight();

                            if (j > canvas.getHeight()) {
                                drawing = false;
                                break ;
                            }
                        }
                    }
                }

//                Log.d(TAG, "draw: " + String.format("topright %d x %d", topRight.getWidth(), topRight.getHeight()));

                int painted = 0 ;
                int tWidth = top.getWidth();
                for (painted = 0 ; painted + tWidth < w + tWidth ; painted = painted + tWidth) {
                    canvas.drawBitmap(top, painted, 0, paint);
                }

                tWidth = bottom.getWidth();
                int bottomHeight = h - bottom.getHeight();
                for (painted = 0 ; painted + tWidth < w + tWidth ; painted = painted + tWidth) {
                    canvas.drawBitmap(bottom, painted, bottomHeight, paint);
                }

                int tHeight = left.getHeight() ;
                for (painted = 0 ; painted + tHeight < h + tHeight; painted = painted + tHeight) {
                    canvas.drawBitmap(left, 0, painted, paint);
                }

                tHeight = right.getHeight() ;
                bottomHeight = w - right.getWidth() ;
                for (painted = 0 ; painted + tHeight < h + tHeight; painted = painted + tHeight) {
                    canvas.drawBitmap(right, bottomHeight, painted, paint);
                }

                canvas.drawBitmap(topLeft, 0, 0, paint);
                canvas.drawBitmap(topRight, layout.getWidth() - topRight.getWidth() , 0, paint);
                canvas.drawBitmap(bottomLeft, 0, h - bottomLeft.getHeight(), paint);
                canvas.drawBitmap(bottomRight, w - bottomRight.getWidth(), h - bottomRight.getHeight(), paint);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        } ;

        bitmapDrawable.setAlpha(alpha);
        layout.setBackground(bitmapDrawable);
        layout.getBackground().setAlpha(alpha);

        /*
        if (config.get("card").get("padding") != null) {
            int padding = Integer.valueOf(config.get("card").get("padding"));
            layout.setPadding(padding, padding, padding, padding);
        }

         */
    }

    boolean hasKnob () {
        return jsonConfig.has("knobs");
    }

    void knob (SeekBar seekBar, int knobSize, int min, int max, int value) {
        BitmapDrawable drawable = bitmapDrawable("knobs", String.valueOf(knobSize));
        RotateDrawable rotateDrawable = new RotateDrawable();
        rotateDrawable.setDrawable(drawable);
        rotateDrawable.setFromDegrees(min);
        rotateDrawable.setToDegrees(max);
        rotateDrawable.setLevel(value);
        seekBar.setBackground(drawable);
    }

    void rotate (View view, int knobSize, float angle) {
        Bitmap bitmap = skinner.getBitmapFromAssets(0, 0,
                themeDir + config.get("knobs").get(String.valueOf(knobSize)));
        view.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                canvas.drawBitmap(bitmap, 0, 0, paint);
                canvas.rotate(angle);
                canvas.restore();
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        });
    }

    void rotary (RotarySeekbar seekBar, int knobSize, float min, float max, float value) {
        seekBar.mNeedleColor = Color.parseColor(config.get("knobs").get("thumbColor"));
        seekBar.mKnobColor = Color.parseColor(config.get("knobs").get("thumbColor"));
        // Tactile 3D body (metallic bezel + dark cap + gloss + shadow) is drawn
        // on canvas; the old flat bitmap background is no longer used here.
        seekBar.setShowKnob(true);
        seekBar.setBackground(null);
        seekBar.setMaxValue(max);
        seekBar.setMinValue(min);
        seekBar.setValue(value);
        seekBar.setRotation((float) value/max);
        /*
        Log.d(TAG, "rotary: " + String.format(
                "[%f %f]: %f (%f)", min, max, value, seekBar.valueToRotation()
        ));

         */
    }

    void getUriFileList (Uri uri) {
        DocumentFile root = null ;
        if (uri.getScheme().equals("file"))
            root = DocumentFile.fromFile(new File(uri.getPath()));
        else
            root = DocumentFile.fromTreeUri(mainActivity, uri);
        DocumentFile [] files = root.listFiles() ;
        for (DocumentFile file: files) {
            /*
            Log.d(TAG, "getUriFileList: " + String.format(
                    "\t%s:\t%s",
                    file.getName(),
                    file.getUri()
            ));

             */
            themeFiles.put(file.getName(), file.getUri());
            bitmaps.put(file.getName(), skinner.getBitmap(file.getUri()));
            if (file.isDirectory()) {
                DocumentFile [] _files = file.listFiles() ;
                for (DocumentFile _file: _files) {
                    /*
                    Log.d(TAG, "getUriFileList: " + String.format(
                            "\t%s:\t%s",
                            _file.getName(),
                            _file.getUri()
                    ));

                     */
                    themeFiles.put(file.getName() + "/" + _file.getName(), _file.getUri());
                    bitmaps.put(file.getName() + "/" + _file.getName(), skinner.getBitmap(_file.getUri()));

                }
            }
        }
    }

    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public static String unzip(InputStream zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        String dirname = null ;
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(zipFilePath);
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            Log.d("SkinEngine", String.format ("[unzip] %s: %s", destDirectory, entry.getName()));
            String filePath = destDirectory + File.separator + entry.getName();
            if (dirname == null)
                dirname = entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
//                Log.d("Skin Engine", String.format ("%s: %s", destDirectory, entry.getName()));
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
        return dirname;
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
