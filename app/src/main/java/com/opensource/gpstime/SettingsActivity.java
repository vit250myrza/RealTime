package com.opensource.gpstime;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import com.opensource.gpstime.realtime.RealTime;
import com.opensource.gpstime.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private Settings settings;
    private boolean initializing = true;
    private final Handler syncUpdateHandler = new Handler(Looper.getMainLooper());
    private boolean syncUpdateActive = false;

    private static final int[] COLOR_VALUES = {
            -1,
            0xFF6750A4, 0xFF1976D2,
            0xFF388E3C, 0xFFD32F2F,
            0xFFF57C00, 0xFF00796B,
            0xFFC2185B, 0xFF303F9F,
    };

    private static final String[] COLOR_NAMES = {
            "System default (Material You)",
            "Purple", "Blue",
            "Green", "Red",
            "Orange", "Teal",
            "Pink", "Indigo",
            "Custom\u2026"
    };

    private static final String[] FONT_NAMES = {
            "Default",
            "Monospace",
            "Serif",
            "Sans-serif"
    };

    private static final String[] FONT_VALUES = {
            Settings.FONT_DEFAULT,
            Settings.FONT_MONOSPACE,
            Settings.FONT_SERIF,
            Settings.FONT_SANS_SERIF
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        settings = new Settings(this);
        Settings.applyTheme(settings.getTheme());
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadSettings();
        initializing = false;
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSyncInfoUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSyncInfoUpdates();
    }

    private void loadSettings() {
        binding.chipDigital.setChecked(settings.getClockMode() == Settings.CLOCK_MODE_DIGITAL);
        binding.chipAnalog.setChecked(settings.getClockMode() == Settings.CLOCK_MODE_ANALOG);
        binding.chipBoth.setChecked(settings.getClockMode() == Settings.CLOCK_MODE_BOTH);

        binding.swShowDate.setChecked(settings.getShowDate());
        binding.swShowSeconds.setChecked(settings.getShowSeconds());
        binding.swShowMillis.setChecked(settings.getShowMillis());
        binding.sw24h.setChecked(settings.getUse24h());
        binding.swUtc.setChecked(settings.getShowUtc());
        binding.swShowSyncInfo.setChecked(settings.getShowSyncInfo());

        binding.btnTimeFormat.setText(Settings.getTimeFormatLabel(settings.getTimeFormat()));
        binding.btnDateFormat.setText(getDateLabel(settings.getDateFormat()));

        int theme = settings.getTheme();
        binding.chipThemeSystem.setChecked(theme == Settings.THEME_SYSTEM);
        binding.chipThemeLight.setChecked(theme == Settings.THEME_LIGHT);
        binding.chipThemeDark.setChecked(theme == Settings.THEME_DARK);

        binding.btnAccentColor.setText(getAccentLabel(settings.getAccentColorRaw()));
        binding.btnFont.setText(getFontLabel(settings.getFont()));

        applySwitchColors();
        applyAccentColors();
    }

    private void applyAccentColors() {
        int accentColor = settings.getAccentColor(this);
        int contrast = getContrastingTextColor(accentColor);

        binding.toolbar.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        binding.toolbar.setTitleTextColor(contrast);
        if (binding.toolbar.getNavigationIcon() != null) {
            binding.toolbar.getNavigationIcon().setTint(contrast);
        }

        binding.btnTimeFormat.setTextColor(accentColor);
        binding.btnDateFormat.setTextColor(accentColor);
        binding.btnAccentColor.setTextColor(accentColor);
        binding.btnFont.setTextColor(accentColor);

        ColorStateList chipBg = createChipBackgroundColorStateList(accentColor);
        ColorStateList chipText = createChipTextColorStateList(accentColor, contrast);

        binding.chipDigital.setChipBackgroundColor(chipBg);
        binding.chipDigital.setTextColor(chipText);
        binding.chipAnalog.setChipBackgroundColor(chipBg);
        binding.chipAnalog.setTextColor(chipText);
        binding.chipBoth.setChipBackgroundColor(chipBg);
        binding.chipBoth.setTextColor(chipText);
        binding.chipThemeSystem.setChipBackgroundColor(chipBg);
        binding.chipThemeSystem.setTextColor(chipText);
        binding.chipThemeLight.setChipBackgroundColor(chipBg);
        binding.chipThemeLight.setTextColor(chipText);
        binding.chipThemeDark.setChipBackgroundColor(chipBg);
        binding.chipThemeDark.setTextColor(chipText);
    }

    private ColorStateList createChipBackgroundColorStateList(int accentColor) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        accentColor,
                        adjustAlpha(accentColor, 0.08f)
                }
        );
    }

    private ColorStateList createChipTextColorStateList(int accentColor, int contrast) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        contrast,
                        accentColor
                }
        );
    }

    private int getContrastingTextColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private int adjustAlpha(int color, float alpha) {
        int alphaInt = Math.round(Color.alpha(color) * alpha);
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }

    private void applySwitchColors() {
        int accentColor = settings.getAccentColor(this);
        int accentColorTransparent = (accentColor & 0x00FFFFFF) | 0x33000000;
        android.content.res.ColorStateList thumbTint = new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        accentColor,
                        0xFFE0E0E0
                }
        );
        android.content.res.ColorStateList trackTint = new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        accentColorTransparent,
                        0x33000000
                }
        );
        binding.swShowDate.setThumbTintList(thumbTint);
        binding.swShowDate.setTrackTintList(trackTint);
        binding.swShowSeconds.setThumbTintList(thumbTint);
        binding.swShowSeconds.setTrackTintList(trackTint);
        binding.swShowMillis.setThumbTintList(thumbTint);
        binding.swShowMillis.setTrackTintList(trackTint);
        binding.sw24h.setThumbTintList(thumbTint);
        binding.sw24h.setTrackTintList(trackTint);
        binding.swUtc.setThumbTintList(thumbTint);
        binding.swUtc.setTrackTintList(trackTint);
        binding.swShowSyncInfo.setThumbTintList(thumbTint);
        binding.swShowSyncInfo.setTrackTintList(trackTint);
    }

    private void setupListeners() {
        ViewGroup clockModeRoot = (ViewGroup) binding.chipClockMode.getParent();
        binding.chipDigital.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot);
                settings.setClockMode(Settings.CLOCK_MODE_DIGITAL);
            }
        });
        binding.chipAnalog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot);
                settings.setClockMode(Settings.CLOCK_MODE_ANALOG);
            }
        });
        binding.chipBoth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot);
                settings.setClockMode(Settings.CLOCK_MODE_BOTH);
            }
        });

        binding.swShowDate.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setShowDate(isChecked));
        binding.swShowSeconds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setShowSeconds(isChecked);
            if (!isChecked) binding.swShowMillis.setChecked(false);
            binding.btnTimeFormat.setText(Settings.getTimeFormatLabel(settings.getTimeFormat()));
        });
        binding.swShowMillis.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setShowMillis(isChecked);
            if (isChecked) binding.swShowSeconds.setChecked(true);
            binding.btnTimeFormat.setText(Settings.getTimeFormatLabel(settings.getTimeFormat()));
        });
        binding.sw24h.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setUse24h(isChecked);
            binding.btnTimeFormat.setText(Settings.getTimeFormatLabel(settings.getTimeFormat()));
        });
        binding.swUtc.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setShowUtc(isChecked));
        binding.swShowSyncInfo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setShowSyncInfo(isChecked);
            updateSyncInfo();
        });

        binding.btnTimeFormat.setOnClickListener(v -> showTimeFormatPicker());
        binding.btnDateFormat.setOnClickListener(v -> showDateFormatPicker());

        ViewGroup themeRoot = (ViewGroup) binding.chipTheme.getParent();
        binding.chipThemeSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot);
                settings.setTheme(Settings.THEME_SYSTEM);
                recreate();
            }
        });
        binding.chipThemeLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot);
                settings.setTheme(Settings.THEME_LIGHT);
                recreate();
            }
        });
        binding.chipThemeDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot);
                settings.setTheme(Settings.THEME_DARK);
                recreate();
            }
        });

        binding.btnAccentColor.setOnClickListener(v -> showColorDialog());
        binding.btnFont.setOnClickListener(v -> showFontPickerDialog());
    }

    private void showColorDialog() {
        int current = settings.getAccentColorRaw();
        int checked = 0;
        for (int i = 0; i < COLOR_VALUES.length; i++) {
            if (current == COLOR_VALUES[i]) { checked = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_color)
                .setSingleChoiceItems(COLOR_NAMES, checked, (dialog, which) -> {
                    if (which == COLOR_NAMES.length - 1) {
                        dialog.dismiss();
                        showCustomHexDialog();
                    } else {
                        if (COLOR_VALUES[which] == -1) {
                            settings.resetAccentColor();
                        } else {
                            settings.setAccentColor(COLOR_VALUES[which]);
                        }
                        binding.btnAccentColor.setText(COLOR_NAMES[which]);
                        applySwitchColors();
                        applyAccentColors();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showCustomHexDialog() {
        EditText input = new EditText(this);
        input.setText("#FF");
        input.setSelection(input.getText().length());
        input.setHint("#AARRGGBB or #RRGGBB");
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 3, pad, pad / 3);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Custom app color")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String hex = input.getText().toString().trim();
                    try {
                        if (!hex.startsWith("#")) hex = "#" + hex;
                        int color = Color.parseColor(hex);
                        settings.setAccentColor(color);
                        binding.btnAccentColor.setText(String.format("#%08X", color));
                        applySwitchColors();
                        applyAccentColors();
                    } catch (IllegalArgumentException e) {
                        binding.btnAccentColor.setText(R.string.accent_color);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFontPickerDialog() {
        String current = settings.getFont();
        int checked = 0;
        for (int i = 0; i < FONT_VALUES.length; i++) {
            if (FONT_VALUES[i].equals(current)) { checked = i; break; }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.font)
                .setSingleChoiceItems(FONT_NAMES, checked, (dialog, which) -> {
                    settings.setFont(FONT_VALUES[which]);
                    binding.btnFont.setText(FONT_NAMES[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private void showTimeFormatPicker() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.time_format)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.format_iso),
                        getString(R.string.format_24h_ms),
                        getString(R.string.format_12h_ms),
                        getString(R.string.format_24h),
                        getString(R.string.format_12h),
                        getString(R.string.format_24h_ns),
                        getString(R.string.format_12h_ns)
                }, settings.getTimeFormat(), (dialog, which) -> {
                    settings.setTimeFormatFromPreset(which);
                    binding.btnTimeFormat.setText(Settings.getTimeFormatLabel(which));
                    dialog.dismiss();
                })
                .show();
    }

    private void showDateFormatPicker() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.date_format)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.date_standard),
                        getString(R.string.date_eu),
                        getString(R.string.date_us),
                        getString(R.string.date_long),
                        getString(R.string.date_full),
                        getString(R.string.date_short)
                }, settings.getDateFormat(), (dialog, which) -> {
                    settings.setDateFormat(which);
                    binding.btnDateFormat.setText(getDateLabel(which));
                    dialog.dismiss();
                })
                .show();
    }

    private String getAccentLabel(int raw) {
        if (raw == -1) return COLOR_NAMES[0];
        for (int i = 1; i < COLOR_VALUES.length; i++) {
            if (raw == COLOR_VALUES[i]) return COLOR_NAMES[i];
        }
        return String.format("#%08X", raw);
    }

    private String getFontLabel(String font) {
        for (int i = 0; i < FONT_VALUES.length; i++) {
            if (FONT_VALUES[i].equals(font)) return FONT_NAMES[i];
        }
        return font.isEmpty() ? "Default" : font;
    }

    private String getDateLabel(int idx) {
        switch (idx) {
            case 1: return getString(R.string.date_eu);
            case 2: return getString(R.string.date_us);
            case 3: return getString(R.string.date_long);
            case 4: return getString(R.string.date_full);
            case 5: return getString(R.string.date_short);
            default: return getString(R.string.date_standard);
        }
    }

    private void startSyncInfoUpdates() {
        if (syncUpdateActive) return;
        syncUpdateActive = true;

        final Runnable syncUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (syncUpdateActive) {
                    updateSyncInfo();
                    syncUpdateHandler.postDelayed(this, 1000);
                }
            }
        };
        syncUpdateHandler.post(syncUpdateTask);
    }

    private void stopSyncInfoUpdates() {
        syncUpdateActive = false;
        syncUpdateHandler.removeCallbacksAndMessages(null);
    }

    private void updateSyncInfo() {
        boolean showMore = settings.getShowSyncInfo();

        if (!showMore) {
            binding.txtGpsSyncInfo.setVisibility(android.view.View.GONE);
            return;
        }

        binding.txtGpsSyncInfo.setVisibility(android.view.View.VISIBLE);

        if (!RealTime.isInitialized()) {
            binding.txtGpsSyncInfo.setText(String.format(Locale.ENGLISH, "RTT: \u2014 ms"));
            return;
        }

        long rttMs = RealTime.getLastGpsRoundTripTimeMs();
        if (rttMs >= 0) {
            binding.txtGpsSyncInfo.setText(String.format(Locale.ENGLISH, "RTT: %d ms", rttMs));
        } else {
            binding.txtGpsSyncInfo.setText(String.format(Locale.ENGLISH, "RTT: — ms"));
        }
    }
}
