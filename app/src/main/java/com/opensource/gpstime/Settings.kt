package com.opensource.gpstime

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors

class Settings(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var clockMode: Int
        get() = prefs.getInt(KEY_CLOCK_MODE, CLOCK_MODE_DIGITAL)
        set(mode) = prefs.edit().putInt(KEY_CLOCK_MODE, mode).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, true)
        set(show) = prefs.edit().putBoolean(KEY_SHOW_DATE, show).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, true)
        set(show) {
            val current = timeFormat
            var mapped = current

            if (current == 0) {
                mapped = getTimeFormatFromFlags(show, showMillis, use24h)
                val editor = prefs.edit().putBoolean(KEY_SHOW_SECONDS, show)
                if (mapped != 0) {
                    editor.putInt(KEY_TIME_FORMAT, mapped)
                }
                editor.apply()
                return
            }

            if (show) {
                if (current == 5 || current == 6) {
                    mapped = if (use24h) 3 else 4
                }
            } else {
                if (current == 1 || current == 3) {
                    mapped = 5
                } else if (current == 2 || current == 4) {
                    mapped = 6
                }
            }

            val editor = prefs.edit().putBoolean(KEY_SHOW_SECONDS, show)
            if (mapped != current) {
                editor.putInt(KEY_TIME_FORMAT, mapped)
            }
            editor.apply()
        }

    var showMillis: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MILLIS, true)
        set(show) {
            val current = timeFormat
            var mapped = current

            if (current == 0) {
                mapped = getTimeFormatFromFlags(showSeconds, show, use24h)
                val editor = prefs.edit().putBoolean(KEY_SHOW_MILLIS, show)
                if (mapped != 0) {
                    editor.putInt(KEY_TIME_FORMAT, mapped)
                }
                editor.apply()
                return
            }

            if (show) {
                if (current == 3 || current == 5) {
                    mapped = 1
                } else if (current == 4 || current == 6) {
                    mapped = 2
                }
            } else {
                if (current == 1) {
                    mapped = 3
                } else if (current == 2) {
                    mapped = 4
                }
            }

            val editor = prefs.edit().putBoolean(KEY_SHOW_MILLIS, show)
            if (mapped != current) {
                editor.putInt(KEY_TIME_FORMAT, mapped)
            }
            editor.apply()
        }

    var use24h: Boolean
        get() = prefs.getBoolean(KEY_USE_24H, true)
        set(use24h) {
            val current = timeFormat
            var mapped = current

            if (current == 0) {
                mapped = getTimeFormatFromFlags(showSeconds, showMillis, use24h)
                val editor = prefs.edit().putBoolean(KEY_USE_24H, use24h)
                if (mapped != 0) {
                    editor.putInt(KEY_TIME_FORMAT, mapped)
                }
                editor.apply()
                return
            }

            if (use24h) {
                if (current == 2) mapped = 1
                else if (current == 4) mapped = 3
                else if (current == 6) mapped = 5
            } else {
                if (current == 1) mapped = 2
                else if (current == 3) mapped = 4
                else if (current == 5) mapped = 6
            }

            val editor = prefs.edit().putBoolean(KEY_USE_24H, use24h)
            if (mapped != current) {
                editor.putInt(KEY_TIME_FORMAT, mapped)
            }
            editor.apply()
        }

    var showSyncInfo: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYNC_INFO, false)
        set(show) = prefs.edit().putBoolean(KEY_SHOW_SYNC_INFO, show).apply()

    var timeFormat: Int
        get() = prefs.getInt(KEY_TIME_FORMAT, -1)
        set(format) = prefs.edit().putInt(KEY_TIME_FORMAT, format).apply()

    var dateFormat: Int
        get() = prefs.getInt(KEY_DATE_FORMAT, 0)
        set(format) = prefs.edit().putInt(KEY_DATE_FORMAT, format).apply()

    var theme: Int
        get() = prefs.getInt(KEY_THEME, THEME_SYSTEM)
        set(theme) = prefs.edit().putInt(KEY_THEME, theme).apply()

    var showUtc: Boolean
        get() = prefs.getBoolean(KEY_SHOW_UTC, false)
        set(show) = prefs.edit().putBoolean(KEY_SHOW_UTC, show).apply()

    var font: String
        get() = prefs.getString(KEY_FONT, FONT_DEFAULT) ?: FONT_DEFAULT
        set(font) = prefs.edit().putString(KEY_FONT, font).apply()

    val accentColorRaw: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, -1)

    val datePattern: String
        get() = when (dateFormat) {
            1 -> "dd/MM/yyyy"
            2 -> "MM/dd/yyyy"
            3 -> "dd MMMM yyyy"
            4 -> "EEEE, dd MMMM yyyy"
            5 -> "EEE, dd MMM"
            else -> "yyyy-MM-dd"
        }

    fun getAccentColor(context: Context): Int {
        val stored = prefs.getInt(KEY_ACCENT_COLOR, -1)
        return if (stored == -1) {
            MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, 0xFF6750A4.toInt())
        } else {
            stored
        }
    }

    fun setAccentColor(color: Int) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply()
    }

    fun resetAccentColor() {
        prefs.edit().remove(KEY_ACCENT_COLOR).apply()
    }

    fun setTimeFormatFromPreset(which: Int) {
        val editor = prefs.edit()
        when (which) {
            0 -> {
                editor.putBoolean(KEY_USE_24H, true)
                editor.putBoolean(KEY_SHOW_SECONDS, true)
                editor.putBoolean(KEY_SHOW_MILLIS, true)
            }
            1 -> {
                editor.putBoolean(KEY_USE_24H, true)
                editor.putBoolean(KEY_SHOW_SECONDS, true)
                editor.putBoolean(KEY_SHOW_MILLIS, true)
            }
            2 -> {
                editor.putBoolean(KEY_USE_24H, false)
                editor.putBoolean(KEY_SHOW_SECONDS, true)
                editor.putBoolean(KEY_SHOW_MILLIS, true)
            }
            3 -> {
                editor.putBoolean(KEY_USE_24H, true)
                editor.putBoolean(KEY_SHOW_SECONDS, true)
                editor.putBoolean(KEY_SHOW_MILLIS, false)
            }
            4 -> {
                editor.putBoolean(KEY_USE_24H, false)
                editor.putBoolean(KEY_SHOW_SECONDS, true)
                editor.putBoolean(KEY_SHOW_MILLIS, false)
            }
            5 -> {
                editor.putBoolean(KEY_USE_24H, true)
                editor.putBoolean(KEY_SHOW_SECONDS, false)
                editor.putBoolean(KEY_SHOW_MILLIS, false)
            }
            6 -> {
                editor.putBoolean(KEY_USE_24H, false)
                editor.putBoolean(KEY_SHOW_SECONDS, false)
                editor.putBoolean(KEY_SHOW_MILLIS, false)
            }
        }
        editor.putInt(KEY_TIME_FORMAT, which)
        editor.apply()
    }

    private fun getTimeFormatFromFlags(showSeconds: Boolean, showMillis: Boolean, use24h: Boolean): Int {
        return if (!showSeconds) {
            if (use24h) 5 else 6
        } else if (showMillis) {
            if (use24h) 1 else 2
        } else {
            if (use24h) 3 else 4
        }
    }

    fun buildTimePattern(): String {
        return when (timeFormat) {
            0 -> if (showUtc) "HH:mm:ss.SSS 'UTC'" else "HH:mm:ss.SSSXXX"
            1 -> "HH:mm:ss.SSS"
            2 -> "hh:mm:ss.SSS a"
            3 -> "HH:mm:ss"
            4 -> "hh:mm:ss a"
            5 -> "HH:mm"
            6 -> "hh:mm a"
            else -> {
                val showSec = showSeconds
                val showMs = showSec && showMillis
                val use24 = use24h
                val sb = StringBuilder()
                sb.append(if (use24) "HH" else "hh")
                sb.append(":mm")
                if (showSec) {
                    sb.append(":ss")
                    if (showMs) {
                        sb.append(".SSS")
                    }
                }
                if (!use24) {
                    sb.append(" a")
                }
                sb.toString()
            }
        }
    }

    fun buildDateTimePattern(): String {
        return if (timeFormat == 0) {
            if (showUtc) "yyyy-MM-dd'T'HH:mm:ss.SSS 'UTC'" else "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        } else {
            val timePat = buildTimePattern()
            if (showDate) {
                "$datePattern  $timePat"
            } else {
                timePat
            }
        }
    }

    companion object {
        const val CLOCK_MODE_DIGITAL = 0
        const val CLOCK_MODE_ANALOG = 1
        const val CLOCK_MODE_BOTH = 2

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        const val FONT_DEFAULT = ""
        const val FONT_MONOSPACE = "monospace"
        const val FONT_SERIF = "serif"
        const val FONT_SANS_SERIF = "sans-serif"

        private const val KEY_CLOCK_MODE = "clock_mode"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_SHOW_SECONDS = "show_seconds"
        private const val KEY_SHOW_MILLIS = "show_millis"
        private const val KEY_USE_24H = "use_24h"
        private const val KEY_TIME_FORMAT = "time_format"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_THEME = "theme"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT = "font"
        private const val KEY_SHOW_UTC = "show_utc"
        private const val KEY_SHOW_SYNC_INFO = "show_sync_info"

        fun getTimeFormatLabel(idx: Int): String {
            return when (idx) {
                -1 -> "Custom"
                1 -> "HH:mm:ss.SSS"
                2 -> "hh:mm:ss.SSS a"
                3 -> "HH:mm:ss"
                4 -> "hh:mm:ss a"
                5 -> "HH:mm"
                6 -> "hh:mm a"
                else -> "yyyy-MM-dd'T'HH:mm:ss.SSS UTC"
            }
        }

        fun applyTheme(themeMode: Int) {
            when (themeMode) {
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }
            }
        }
    }
}
