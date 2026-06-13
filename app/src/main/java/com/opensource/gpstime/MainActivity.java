package com.opensource.gpstime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.color.DynamicColors;
import com.opensource.gpstime.realtime.RealTime;
import com.opensource.gpstime.databinding.ActivityMainBinding;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private Settings settings;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private boolean updatesActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        settings = new Settings(this);
        Settings.applyTheme(settings.getTheme());
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnClearCache.setOnClickListener(v ->
                RealTime.clearCachedInfo());

        requestLocationPermission();
        applyClockMode();
        applyFont();
        applyUiColors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFont();
        applyUiColors();
        startClockUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClockUpdates();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private boolean isIsoPortrait() {
        return settings.getTimeFormat() == 0 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private boolean isIsoLandscape() {
        return settings.getTimeFormat() == 0 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static final String ISO_LANDSCAPE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private void startClockUpdates() {
        if (updatesActive) return;
        updatesActive = true;

        String timePat;
        if (settings.getTimeFormat() == 0) {
            timePat = isIsoPortrait() ? "HH:mm:ss.SSS" : ISO_LANDSCAPE_PATTERN;
        } else {
            timePat = settings.buildTimePattern();
        }

        final SimpleDateFormat timeFormat = new SimpleDateFormat(timePat, Locale.ENGLISH);
        final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat(
                settings.getDatePattern(), Locale.ENGLISH);
        final SimpleDateFormat timeOnlyFormat = new SimpleDateFormat(timePat, Locale.ENGLISH);
        if (settings.getShowUtc()) {
            TimeZone utc = TimeZone.getTimeZone("UTC");
            timeFormat.setTimeZone(utc);
            dateOnlyFormat.setTimeZone(utc);
            timeOnlyFormat.setTimeZone(utc);
        }

        final Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                try {
                    updateDisplay(timeFormat, dateOnlyFormat, timeOnlyFormat);
                } catch (Exception e) {
                    Log.e(TAG, "Error in updateDisplay", e);
                }
                if (updatesActive) {
                    boolean ip = isIsoPortrait();
                    if (settings.getTimeFormat() == 0) {
                        timeFormat.applyPattern(ip ? "HH:mm:ss.SSS" : ISO_LANDSCAPE_PATTERN);
                        timeOnlyFormat.applyPattern(ip ? "HH:mm:ss.SSS" : ISO_LANDSCAPE_PATTERN);
                    } else {
                        timeFormat.applyPattern(settings.buildTimePattern());
                        timeOnlyFormat.applyPattern(settings.buildTimePattern());
                    }
                    dateOnlyFormat.applyPattern(settings.getDatePattern());
                    long interval = computeUpdateInterval();
                    updateHandler.postDelayed(this, interval);
                }
            }
        };
        updateHandler.post(updateTask);
    }

    private long computeUpdateInterval() {
        boolean showSec = settings.getShowSeconds();
        boolean showMs = showSec && settings.getShowMillis();
        return showMs ? getDeviceRefreshIntervalMillis() : 1000L;
    }

    private long getDeviceRefreshIntervalMillis() {
        float refreshRate = 60f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (getDisplay() != null) {
                refreshRate = getDisplay().getRefreshRate();
            }
        } else {
            refreshRate = getWindowManager().getDefaultDisplay().getRefreshRate();
        }

        if (refreshRate <= 0) {
            refreshRate = 60f;
        }
        return Math.max(8L, Math.round(1000f / refreshRate));
    }

    private int getContrastingTextColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private int adjustAlpha(int color, float alpha) {
        int alphaInt = Math.round(Color.alpha(color) * alpha);
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }

    private void stopClockUpdates() {
        updatesActive = false;
        updateHandler.removeCallbacksAndMessages(null);
    }

    @SuppressLint("SetTextI18n")
    private void updateDisplay(SimpleDateFormat timeFormat, SimpleDateFormat dateOnlyFormat,
                               SimpleDateFormat timeOnlyFormat) {
        int mode = settings.getClockMode();
        boolean showDate = settings.getShowDate();
        boolean showSec = settings.getShowSeconds();

        if (!RealTime.isInitialized()) {
            String wait = getString(R.string.waiting_for_gps);
            switch (mode) {
                case Settings.CLOCK_MODE_DIGITAL:
                    setClockModeVisible(Settings.CLOCK_MODE_DIGITAL);
                    binding.txtTime.setText("--:--:--");
                    binding.txtStatus.setVisibility(View.VISIBLE);
                    binding.txtStatus.setText(wait);
                    binding.txtDate.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    binding.txtDate.setText("");
                    break;
                case Settings.CLOCK_MODE_ANALOG:
                    setClockModeVisible(Settings.CLOCK_MODE_ANALOG);
                    binding.txtAnalogTime.setText("");
                    break;
                case Settings.CLOCK_MODE_BOTH:
                    setClockModeVisible(Settings.CLOCK_MODE_BOTH);
                    binding.txtBothTime.setText("--:--:--");
                    binding.txtBothStatus.setVisibility(View.VISIBLE);
                    binding.txtBothStatus.setText(wait);
                    binding.txtBothDate.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    binding.txtBothDate.setText("");
                    break;
            }
            updateSyncInfo();
            return;
        }

        boolean isoLandscape = isIsoLandscape();

        Date now = RealTime.now();
        String timeStr = timeFormat.format(now);
        String dateStr = showDate ? dateOnlyFormat.format(now) : "";
        String shortTime = timeOnlyFormat.format(now);

        switch (mode) {
            case Settings.CLOCK_MODE_DIGITAL: {
                setClockModeVisible(Settings.CLOCK_MODE_DIGITAL);
                binding.txtTime.setText(timeStr);
                binding.txtStatus.setVisibility(View.GONE);
                binding.txtDate.setVisibility((showDate && !isoLandscape) ? View.VISIBLE : View.GONE);
                binding.txtDate.setText(dateStr);
                break;
            }
            case Settings.CLOCK_MODE_ANALOG: {
                setClockModeVisible(Settings.CLOCK_MODE_ANALOG);
                binding.analogClock.setTime(now);
                binding.txtAnalogTime.setText(shortTime);
                break;
            }
            case Settings.CLOCK_MODE_BOTH: {
                setClockModeVisible(Settings.CLOCK_MODE_BOTH);
                binding.analogClockBoth.setTime(now);
                binding.txtBothTime.setText(timeStr);
                binding.txtBothStatus.setVisibility(View.GONE);
                binding.txtBothDate.setVisibility((showDate && !isoLandscape) ? View.VISIBLE : View.GONE);
                binding.txtBothDate.setText(dateStr);
                break;
            }
        }

        if (isIsoPortrait()) {
            SimpleDateFormat tzFmt = new SimpleDateFormat(settings.getShowUtc() ? "z" : "XXX", Locale.ENGLISH);
            if (settings.getShowUtc()) tzFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            String tzStr = tzFmt.format(now);
            switch (mode) {
                case Settings.CLOCK_MODE_DIGITAL:
                    binding.txtStatus.setVisibility(View.VISIBLE);
                    binding.txtStatus.setText(tzStr);
                    break;
                case Settings.CLOCK_MODE_BOTH:
                    binding.txtBothStatus.setVisibility(View.VISIBLE);
                    binding.txtBothStatus.setText(tzStr);
                    break;
            }
        }

        updateSyncInfo();

        int appColor = settings.getAccentColor(MainActivity.this);
        switch (mode) {
            case Settings.CLOCK_MODE_DIGITAL:
                binding.txtTime.setTextColor(appColor);
                binding.txtStatus.setTextColor(appColor);
                binding.txtDate.setTextColor(appColor);
                break;
            case Settings.CLOCK_MODE_ANALOG:
                binding.analogClock.setAccentColor(appColor);
                binding.analogClock.setRimColor(appColor);
                binding.txtAnalogTime.setTextColor(appColor);
                break;
            case Settings.CLOCK_MODE_BOTH:
                binding.analogClockBoth.setAccentColor(appColor);
                binding.analogClockBoth.setRimColor(appColor);
                binding.txtBothTime.setTextColor(appColor);
                binding.txtBothStatus.setTextColor(appColor);
                binding.txtBothDate.setTextColor(appColor);
                break;
        }
    }

    private void setClockModeVisible(int mode) {
        binding.analogContainer.setVisibility(View.GONE);
        binding.bothContainer.setVisibility(View.GONE);
        binding.digitalContainer.setVisibility(View.GONE);

        switch (mode) {
            case Settings.CLOCK_MODE_DIGITAL:
                binding.digitalContainer.setVisibility(View.VISIBLE);
                break;
            case Settings.CLOCK_MODE_ANALOG:
                binding.analogContainer.setVisibility(View.VISIBLE);
                break;
            case Settings.CLOCK_MODE_BOTH:
                binding.bothContainer.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void applyClockMode() {
        setClockModeVisible(settings.getClockMode());
    }

    private void applyUiColors() {
        int accentColor = settings.getAccentColor(this);
        int contrast = getContrastingTextColor(accentColor);

        binding.getRoot().setBackgroundColor(adjustAlpha(accentColor, 0.05f));
        binding.txtSyncInfo.setTextColor(accentColor);

        binding.cardTime.setCardBackgroundColor(adjustAlpha(accentColor, 0.08f));
        binding.cardBoth.setCardBackgroundColor(adjustAlpha(accentColor, 0.08f));
        binding.btnSettings.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        binding.btnSettings.setTextColor(contrast);
        binding.btnClearCache.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(accentColor, 0.20f)));
        binding.btnClearCache.setTextColor(contrast);

        binding.analogClock.setFaceColor(adjustAlpha(accentColor, 0.12f));
        binding.analogClockBoth.setFaceColor(adjustAlpha(accentColor, 0.12f));
        binding.analogClock.setAccentColor(accentColor);
        binding.analogClockBoth.setAccentColor(accentColor);
    }

    private void updateSyncInfo() {
        boolean showMore = settings.getShowSyncInfo();

        if (!showMore) {
            binding.txtSyncInfo.setVisibility(View.GONE);
            return;
        }

        binding.txtSyncInfo.setVisibility(View.VISIBLE);

        if (!RealTime.isInitialized()) {
            binding.txtSyncInfo.setText(String.format(Locale.ENGLISH, "Syncing... (RTT: — ms)"));
            return;
        }

        long rttMs = RealTime.getLastGpsRoundTripTimeMs();
        if (rttMs >= 0) {
            binding.txtSyncInfo.setText(String.format(Locale.ENGLISH, "RTT: %d ms", rttMs));
        } else {
            binding.txtSyncInfo.setText(String.format(Locale.ENGLISH, "RTT: — ms"));
        }
    }

    private void applyFont() {
        String font = settings.getFont();
        Typeface tf = getTypefaceForFont(font);

        binding.txtTime.setTypeface(tf);
        binding.txtDate.setTypeface(null);
        binding.txtBothTime.setTypeface(tf);
        binding.txtBothDate.setTypeface(null);
        binding.txtAnalogTime.setTypeface(tf);
    }

    private Typeface getTypefaceForFont(String font) {
        if (font == null || font.isEmpty()) return null;
        return Typeface.create(font, Typeface.NORMAL);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 0);
    }
}
