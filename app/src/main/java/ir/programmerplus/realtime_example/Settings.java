package ir.programmerplus.realtime_example;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;

public class Settings {

    private static final String KEY_CLOCK_MODE = "clock_mode";
    private static final String KEY_SHOW_DATE = "show_date";
    private static final String KEY_SHOW_SECONDS = "show_seconds";
    private static final String KEY_SHOW_MILLIS = "show_millis";
    private static final String KEY_USE_24H = "use_24h";
    private static final String KEY_TIME_FORMAT = "time_format";
    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_THEME = "theme";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_TIME_COLOR = "time_color";
    private static final String KEY_FONT = "font";
    private static final String KEY_SHOW_UTC = "show_utc";
    private static final String KEY_SHOW_SYNC_INFO = "show_sync_info";

    public static final int CLOCK_MODE_DIGITAL = 0;
    public static final int CLOCK_MODE_ANALOG = 1;
    public static final int CLOCK_MODE_BOTH = 2;

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final String FONT_DEFAULT = "";
    public static final String FONT_MONOSPACE = "monospace";
    public static final String FONT_SERIF = "serif";
    public static final String FONT_SANS_SERIF = "sans-serif";

    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getClockMode() {
        return prefs.getInt(KEY_CLOCK_MODE, CLOCK_MODE_DIGITAL);
    }

    public void setClockMode(int mode) {
        prefs.edit().putInt(KEY_CLOCK_MODE, mode).apply();
    }

    public boolean getShowDate() {
        return prefs.getBoolean(KEY_SHOW_DATE, true);
    }

    public void setShowDate(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_DATE, show).apply();
    }

    public boolean getShowSeconds() {
        return prefs.getBoolean(KEY_SHOW_SECONDS, true);
    }

    public void setShowSeconds(boolean show) {
        int current = getTimeFormat();
        int mapped = current;

        if (current == 0) {
            mapped = getTimeFormatFromFlags(show, getShowMillis(), getUse24h());
            SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_SHOW_SECONDS, show);
            if (mapped != 0) {
                editor.putInt(KEY_TIME_FORMAT, mapped);
            }
            editor.apply();
            return;
        }

        if (show) {
            if (current == 5 || current == 6) {
                mapped = getUse24h() ? 3 : 4;
            }
        } else {
            if (current == 1 || current == 3) {
                mapped = 5;
            } else if (current == 2 || current == 4) {
                mapped = 6;
            }
        }

        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_SHOW_SECONDS, show);
        if (mapped != current) {
            editor.putInt(KEY_TIME_FORMAT, mapped);
        }
        editor.apply();
    }

    public boolean getShowMillis() {
        return prefs.getBoolean(KEY_SHOW_MILLIS, true);
    }

    public void setShowMillis(boolean show) {
        int current = getTimeFormat();
        int mapped = current;

        if (current == 0) {
            mapped = getTimeFormatFromFlags(getShowSeconds(), show, getUse24h());
            SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_SHOW_MILLIS, show);
            if (mapped != 0) {
                editor.putInt(KEY_TIME_FORMAT, mapped);
            }
            editor.apply();
            return;
        }

        if (show) {
            if (current == 3 || current == 5) {
                mapped = 1;
            } else if (current == 4 || current == 6) {
                mapped = 2;
            }
        } else {
            if (current == 1) {
                mapped = 3;
            } else if (current == 2) {
                mapped = 4;
            }
        }

        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_SHOW_MILLIS, show);
        if (mapped != current) {
            editor.putInt(KEY_TIME_FORMAT, mapped);
        }
        editor.apply();
    }

    public boolean getUse24h() {
        return prefs.getBoolean(KEY_USE_24H, true);
    }

    public void setUse24h(boolean use24h) {
        int current = getTimeFormat();
        int mapped = current;

        if (current == 0) {
            mapped = getTimeFormatFromFlags(getShowSeconds(), getShowMillis(), use24h);
            SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_USE_24H, use24h);
            if (mapped != 0) {
                editor.putInt(KEY_TIME_FORMAT, mapped);
            }
            editor.apply();
            return;
        }

        if (use24h) {
            if (current == 2) mapped = 1;
            else if (current == 4) mapped = 3;
            else if (current == 6) mapped = 5;
        } else {
            if (current == 1) mapped = 2;
            else if (current == 3) mapped = 4;
            else if (current == 5) mapped = 6;
        }

        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_USE_24H, use24h);
        if (mapped != current) {
            editor.putInt(KEY_TIME_FORMAT, mapped);
        }
        editor.apply();
    }

    public boolean getShowSyncInfo() {
        return prefs.getBoolean(KEY_SHOW_SYNC_INFO, false);
    }

    public void setShowSyncInfo(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_SYNC_INFO, show).apply();
    }

    public int getTimeFormat() {
        return prefs.getInt(KEY_TIME_FORMAT, -1);
    }

    public void setTimeFormat(int format) {
        prefs.edit().putInt(KEY_TIME_FORMAT, format).apply();
    }

    public void setTimeFormatFromPreset(int which) {
        SharedPreferences.Editor editor = prefs.edit();
        switch (which) {
            case 0: // ISO 8601
                editor.putBoolean(KEY_USE_24H, true);
                editor.putBoolean(KEY_SHOW_SECONDS, true);
                editor.putBoolean(KEY_SHOW_MILLIS, true);
                break;
            case 1: // HH:mm:ss.SSS
                editor.putBoolean(KEY_USE_24H, true);
                editor.putBoolean(KEY_SHOW_SECONDS, true);
                editor.putBoolean(KEY_SHOW_MILLIS, true);
                break;
            case 2: // hh:mm:ss.SSS a
                editor.putBoolean(KEY_USE_24H, false);
                editor.putBoolean(KEY_SHOW_SECONDS, true);
                editor.putBoolean(KEY_SHOW_MILLIS, true);
                break;
            case 3: // HH:mm:ss
                editor.putBoolean(KEY_USE_24H, true);
                editor.putBoolean(KEY_SHOW_SECONDS, true);
                editor.putBoolean(KEY_SHOW_MILLIS, false);
                break;
            case 4: // hh:mm:ss a
                editor.putBoolean(KEY_USE_24H, false);
                editor.putBoolean(KEY_SHOW_SECONDS, true);
                editor.putBoolean(KEY_SHOW_MILLIS, false);
                break;
            case 5: // HH:mm
                editor.putBoolean(KEY_USE_24H, true);
                editor.putBoolean(KEY_SHOW_SECONDS, false);
                editor.putBoolean(KEY_SHOW_MILLIS, false);
                break;
            case 6: // hh:mm a
                editor.putBoolean(KEY_USE_24H, false);
                editor.putBoolean(KEY_SHOW_SECONDS, false);
                editor.putBoolean(KEY_SHOW_MILLIS, false);
                break;
        }
        editor.putInt(KEY_TIME_FORMAT, which);
        editor.apply();
    }

    private int getTimeFormatFromFlags(boolean showSeconds, boolean showMillis, boolean use24h) {
        if (!showSeconds) {
            return use24h ? 5 : 6;
        }
        if (showMillis) {
            return use24h ? 1 : 2;
        }
        return use24h ? 3 : 4;
    }

    public int getDateFormat() {
        return prefs.getInt(KEY_DATE_FORMAT, 0);
    }

    public void setDateFormat(int format) {
        prefs.edit().putInt(KEY_DATE_FORMAT, format).apply();
    }

    public int getTheme() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setTheme(int theme) {
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    public int getAccentColor(Context context) {
        int stored = prefs.getInt(KEY_ACCENT_COLOR, -1);
        if (stored == -1) {
            return MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, 0xFF6750A4);
        }
        return stored;
    }

    public int getAccentColorRaw() {
        return prefs.getInt(KEY_ACCENT_COLOR, -1);
    }

    public void setAccentColor(int color) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }

    public void resetAccentColor() {
        prefs.edit().remove(KEY_ACCENT_COLOR).apply();
    }

    public int getTimeColor(Context context) {
        int stored = prefs.getInt(KEY_TIME_COLOR, -1);
        if (stored == -1) {
            return MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, 0xFF6750A4);
        }
        return stored;
    }

    public int getTimeColorRaw() {
        return prefs.getInt(KEY_TIME_COLOR, -1);
    }

    public void setTimeColor(int color) {
        prefs.edit().putInt(KEY_TIME_COLOR, color).apply();
    }

    public void resetTimeColor() {
        prefs.edit().remove(KEY_TIME_COLOR).apply();
    }

    public String getFont() {
        return prefs.getString(KEY_FONT, FONT_DEFAULT);
    }

    public void setFont(String font) {
        prefs.edit().putString(KEY_FONT, font).apply();
    }

    public boolean getShowUtc() {
        return prefs.getBoolean(KEY_SHOW_UTC, false);
    }

    public void setShowUtc(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_UTC, show).apply();
    }

    public String buildTimePattern() {
        switch (getTimeFormat()) {
            case 0:
                return getShowUtc() ? "HH:mm:ss.SSS 'UTC'" : "HH:mm:ss.SSSXXX";
            case 1:
                return "HH:mm:ss.SSS";
            case 2:
                return "hh:mm:ss.SSS a";
            case 3:
                return "HH:mm:ss";
            case 4:
                return "hh:mm:ss a";
            case 5:
                return "HH:mm";
            case 6:
                return "hh:mm a";
            default:
                boolean showSec = getShowSeconds();
                boolean showMs = showSec && getShowMillis();
                boolean use24 = getUse24h();

                StringBuilder sb = new StringBuilder();
                if (use24) {
                    sb.append("HH");
                } else {
                    sb.append("hh");
                }
                sb.append(":mm");
                if (showSec) {
                    sb.append(":ss");
                    if (showMs) {
                        sb.append(".SSS");
                    }
                }
                if (!use24) {
                    sb.append(" a");
                }
                return sb.toString();
        }
    }

    public String buildDateTimePattern() {
        if (getTimeFormat() == 0) {
            return getShowUtc() ? "yyyy-MM-dd'T'HH:mm:ss.SSS 'UTC'" : "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
        }
        String timePat = buildTimePattern();
        if (getShowDate()) {
            return getDatePattern() + "  " + timePat;
        }
        return timePat;
    }

    public String getDatePattern() {
        switch (getDateFormat()) {
            case 1: return "dd/MM/yyyy";
            case 2: return "MM/dd/yyyy";
            case 3: return "dd MMMM yyyy";
            case 4: return "EEEE, dd MMMM yyyy";
            case 5: return "EEE, dd MMM";
            default: return "yyyy-MM-dd";
        }
    }

    public static String getTimeFormatLabel(int idx) {
        if (idx == -1) return "Custom";
        switch (idx) {
            case 1: return "HH:mm:ss.SSS";
            case 2: return "hh:mm:ss.SSS a";
            case 3: return "HH:mm:ss";
            case 4: return "hh:mm:ss a";
            case 5: return "HH:mm";
            case 6: return "hh:mm a";
            default: return "yyyy-MM-dd'T'HH:mm:ss.SSS UTC";
        }
    }

    public static void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }
}
