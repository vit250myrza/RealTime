package com.opensource.gpstime

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.opensource.gpstime.databinding.ActivitySettingsBinding
import com.opensource.gpstime.realtime.RealTime
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: Settings
    private var initializing = true
    private val syncUpdateHandler = Handler(Looper.getMainLooper())
    private var syncUpdateActive = false
    private var defaultChipTextColors: Array<ColorStateList?>? = null
    private var defaultChipBackgroundColors: Array<ColorStateList?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        settings = Settings(this)
        Settings.applyTheme(settings.theme)
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadSettings()
        initializing = false
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        startSyncInfoUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopSyncInfoUpdates()
    }

    private fun loadSettings() {
        binding.chipDigital.isChecked = settings.clockMode == Settings.CLOCK_MODE_DIGITAL
        binding.chipAnalog.isChecked = settings.clockMode == Settings.CLOCK_MODE_ANALOG
        binding.chipBoth.isChecked = settings.clockMode == Settings.CLOCK_MODE_BOTH

        binding.swShowDate.isChecked = settings.showDate
        binding.swShowSeconds.isChecked = settings.showSeconds
        binding.swShowMillis.isChecked = settings.showMillis
        binding.sw24h.isChecked = settings.use24h
        binding.swUtc.isChecked = settings.showUtc
        binding.swShowSyncInfo.isChecked = settings.showSyncInfo

        binding.btnTimeFormat.text = Settings.getTimeFormatLabel(settings.timeFormat)
        binding.btnDateFormat.text = getDateLabel(settings.dateFormat)

        val theme = settings.theme
        binding.chipThemeSystem.isChecked = theme == Settings.THEME_SYSTEM
        binding.chipThemeLight.isChecked = theme == Settings.THEME_LIGHT
        binding.chipThemeDark.isChecked = theme == Settings.THEME_DARK

        binding.btnAccentColor.text = getAccentLabel(settings.accentColorRaw)
        binding.btnFont.text = getFontLabel(settings.font)

        saveDefaultChipColors()
        applySwitchColors()
        applyAccentColors()
    }

    private fun applyAccentColors() {
        val accentColor = settings.getAccentColor(this)
        val contrast = getContrastingTextColor(accentColor)

        binding.toolbar.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.toolbar.setTitleTextColor(contrast)
        binding.toolbar.navigationIcon?.setTint(contrast)

        binding.btnTimeFormat.setTextColor(accentColor)
        binding.btnDateFormat.setTextColor(accentColor)
        binding.btnAccentColor.setTextColor(accentColor)
        binding.btnFont.setTextColor(accentColor)

        if (settings.accentColorRaw == -1) {
            resetChipColorsToTheme()
        } else {
            val chipBg = createChipBackgroundColorStateList(accentColor)
            val chipText = createChipTextColorStateList(accentColor, contrast)

            binding.chipDigital.chipBackgroundColor = chipBg
            binding.chipDigital.setTextColor(chipText)
            binding.chipAnalog.chipBackgroundColor = chipBg
            binding.chipAnalog.setTextColor(chipText)
            binding.chipBoth.chipBackgroundColor = chipBg
            binding.chipBoth.setTextColor(chipText)
            binding.chipThemeSystem.chipBackgroundColor = chipBg
            binding.chipThemeSystem.setTextColor(chipText)
            binding.chipThemeLight.chipBackgroundColor = chipBg
            binding.chipThemeLight.setTextColor(chipText)
            binding.chipThemeDark.chipBackgroundColor = chipBg
            binding.chipThemeDark.setTextColor(chipText)
        }
    }

    private fun saveDefaultChipColors() {
        defaultChipTextColors = arrayOf(
            binding.chipDigital.textColors,
            binding.chipAnalog.textColors,
            binding.chipBoth.textColors,
            binding.chipThemeSystem.textColors,
            binding.chipThemeLight.textColors,
            binding.chipThemeDark.textColors,
        )
        defaultChipBackgroundColors = arrayOf(
            binding.chipDigital.chipBackgroundColor,
            binding.chipAnalog.chipBackgroundColor,
            binding.chipBoth.chipBackgroundColor,
            binding.chipThemeSystem.chipBackgroundColor,
            binding.chipThemeLight.chipBackgroundColor,
            binding.chipThemeDark.chipBackgroundColor,
        )
    }

    private fun resetChipColorsToTheme() {
        binding.chipDigital.chipBackgroundColor = defaultChipBackgroundColors?.get(0)
        binding.chipDigital.setTextColor(defaultChipTextColors?.get(0))
        binding.chipAnalog.chipBackgroundColor = defaultChipBackgroundColors?.get(1)
        binding.chipAnalog.setTextColor(defaultChipTextColors?.get(1))
        binding.chipBoth.chipBackgroundColor = defaultChipBackgroundColors?.get(2)
        binding.chipBoth.setTextColor(defaultChipTextColors?.get(2))
        binding.chipThemeSystem.chipBackgroundColor = defaultChipBackgroundColors?.get(3)
        binding.chipThemeSystem.setTextColor(defaultChipTextColors?.get(3))
        binding.chipThemeLight.chipBackgroundColor = defaultChipBackgroundColors?.get(4)
        binding.chipThemeLight.setTextColor(defaultChipTextColors?.get(4))
        binding.chipThemeDark.chipBackgroundColor = defaultChipBackgroundColors?.get(5)
        binding.chipThemeDark.setTextColor(defaultChipTextColors?.get(5))
    }

    private fun createChipBackgroundColorStateList(accentColor: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                adjustAlpha(accentColor, 0.08f)
            )
        )
    }

    private fun createChipTextColorStateList(accentColor: Int, contrast: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                contrast,
                accentColor
            )
        )
    }

    private fun getContrastingTextColor(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val alphaInt = Math.round(Color.alpha(color) * alpha)
        return (color and 0x00FFFFFF) or (alphaInt shl 24)
    }

    private fun applySwitchColors() {
        if (settings.accentColorRaw == -1) {
            binding.swShowDate.thumbTintList = null
            binding.swShowDate.trackTintList = null
            binding.swShowSeconds.thumbTintList = null
            binding.swShowSeconds.trackTintList = null
            binding.swShowMillis.thumbTintList = null
            binding.swShowMillis.trackTintList = null
            binding.sw24h.thumbTintList = null
            binding.sw24h.trackTintList = null
            binding.swUtc.thumbTintList = null
            binding.swUtc.trackTintList = null
            binding.swShowSyncInfo.thumbTintList = null
            binding.swShowSyncInfo.trackTintList = null
            return
        }
        val accentColor = settings.getAccentColor(this)
        val accentColorTransparent = (accentColor and 0x00FFFFFF) or 0x33000000
        val thumbTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                0xFFE0E0E0.toInt()
            )
        )
        val trackTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColorTransparent,
                0x33000000
            )
        )
        binding.swShowDate.thumbTintList = thumbTint
        binding.swShowDate.trackTintList = trackTint
        binding.swShowSeconds.thumbTintList = thumbTint
        binding.swShowSeconds.trackTintList = trackTint
        binding.swShowMillis.thumbTintList = thumbTint
        binding.swShowMillis.trackTintList = trackTint
        binding.sw24h.thumbTintList = thumbTint
        binding.sw24h.trackTintList = trackTint
        binding.swUtc.thumbTintList = thumbTint
        binding.swUtc.trackTintList = trackTint
        binding.swShowSyncInfo.thumbTintList = thumbTint
        binding.swShowSyncInfo.trackTintList = trackTint
    }

    private fun setupListeners() {
        val clockModeRoot = binding.chipClockMode.parent as ViewGroup
        binding.chipDigital.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot)
                settings.clockMode = Settings.CLOCK_MODE_DIGITAL
            }
        }
        binding.chipAnalog.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot)
                settings.clockMode = Settings.CLOCK_MODE_ANALOG
            }
        }
        binding.chipBoth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(clockModeRoot)
                settings.clockMode = Settings.CLOCK_MODE_BOTH
            }
        }

        binding.swShowDate.setOnCheckedChangeListener { _, isChecked -> settings.showDate = isChecked }
        binding.swShowSeconds.setOnCheckedChangeListener { _, isChecked ->
            settings.showSeconds = isChecked
            if (!isChecked) binding.swShowMillis.isChecked = false
            binding.btnTimeFormat.text = Settings.getTimeFormatLabel(settings.timeFormat)
        }
        binding.swShowMillis.setOnCheckedChangeListener { _, isChecked ->
            settings.showMillis = isChecked
            if (isChecked) binding.swShowSeconds.isChecked = true
            binding.btnTimeFormat.text = Settings.getTimeFormatLabel(settings.timeFormat)
        }
        binding.sw24h.setOnCheckedChangeListener { _, isChecked ->
            settings.use24h = isChecked
            binding.btnTimeFormat.text = Settings.getTimeFormatLabel(settings.timeFormat)
        }
        binding.swUtc.setOnCheckedChangeListener { _, isChecked -> settings.showUtc = isChecked }
        binding.swShowSyncInfo.setOnCheckedChangeListener { _, isChecked ->
            settings.showSyncInfo = isChecked
            updateSyncInfo()
        }

        binding.btnTimeFormat.setOnClickListener { showTimeFormatPicker() }
        binding.btnDateFormat.setOnClickListener { showDateFormatPicker() }

        val themeRoot = binding.chipTheme.parent as ViewGroup
        binding.chipThemeSystem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot)
                settings.theme = Settings.THEME_SYSTEM
                recreate()
            }
        }
        binding.chipThemeLight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot)
                settings.theme = Settings.THEME_LIGHT
                recreate()
            }
        }
        binding.chipThemeDark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot)
                settings.theme = Settings.THEME_DARK
                recreate()
            }
        }

        binding.btnAccentColor.setOnClickListener { showColorDialog() }
        binding.btnFont.setOnClickListener { showFontPickerDialog() }
    }

    private fun showColorDialog() {
        val current = settings.accentColorRaw
        var checked = 0
        for (i in COLOR_VALUES.indices) {
            if (current == COLOR_VALUES[i]) {
                checked = i
                break
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_color)
            .setSingleChoiceItems(COLOR_NAMES, checked) { dialog, which ->
                if (which == COLOR_NAMES.size - 1) {
                    dialog.dismiss()
                    showCustomHexDialog()
                } else {
                    if (COLOR_VALUES[which] == -1) {
                        settings.resetAccentColor()
                    } else {
                        settings.setAccentColor(COLOR_VALUES[which])
                    }
                    binding.btnAccentColor.text = COLOR_NAMES[which]
                    applySwitchColors()
                    applyAccentColors()
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun showCustomHexDialog() {
        val input = EditText(this)
        input.setText("#FF")
        input.setSelection(input.text.length)
        input.hint = "#AARRGGBB or #RRGGBB"
        val pad = (24 * resources.displayMetrics.density).toInt()
        input.setPadding(pad, pad / 3, pad, pad / 3)

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom app color")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                var hex = input.text.toString().trim()
                try {
                    if (!hex.startsWith("#")) hex = "#$hex"
                    val color = Color.parseColor(hex)
                    settings.setAccentColor(color)
                    binding.btnAccentColor.text = String.format("#%08X", color)
                    applySwitchColors()
                    applyAccentColors()
                } catch (e: IllegalArgumentException) {
                    binding.btnAccentColor.setText(R.string.accent_color)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontPickerDialog() {
        val current = settings.font
        var checked = 0
        for (i in FONT_VALUES.indices) {
            if (FONT_VALUES[i] == current) {
                checked = i
                break
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.font)
            .setSingleChoiceItems(FONT_NAMES, checked) { dialog, which ->
                settings.font = FONT_VALUES[which]
                binding.btnFont.text = FONT_NAMES[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showTimeFormatPicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.time_format)
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.format_iso),
                    getString(R.string.format_24h_ms),
                    getString(R.string.format_12h_ms),
                    getString(R.string.format_24h),
                    getString(R.string.format_12h),
                    getString(R.string.format_24h_ns),
                    getString(R.string.format_12h_ns)
                ),
                settings.timeFormat
            ) { dialog, which ->
                settings.setTimeFormatFromPreset(which)
                binding.btnTimeFormat.text = Settings.getTimeFormatLabel(which)
                dialog.dismiss()
            }
            .show()
    }

    private fun showDateFormatPicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.date_format)
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.date_standard),
                    getString(R.string.date_eu),
                    getString(R.string.date_us),
                    getString(R.string.date_long),
                    getString(R.string.date_full),
                    getString(R.string.date_short)
                ),
                settings.dateFormat
            ) { dialog, which ->
                settings.dateFormat = which
                binding.btnDateFormat.text = getDateLabel(which)
                dialog.dismiss()
            }
            .show()
    }

    private fun getAccentLabel(raw: Int): String {
        if (raw == -1) return COLOR_NAMES[0]
        for (i in 1 until COLOR_VALUES.size) {
            if (raw == COLOR_VALUES[i]) return COLOR_NAMES[i]
        }
        return String.format("#%08X", raw)
    }

    private fun getFontLabel(font: String): String {
        for (i in FONT_VALUES.indices) {
            if (FONT_VALUES[i] == font) return FONT_NAMES[i]
        }
        return if (font.isEmpty()) "Default" else font
    }

    private fun getDateLabel(idx: Int): String {
        return when (idx) {
            1 -> getString(R.string.date_eu)
            2 -> getString(R.string.date_us)
            3 -> getString(R.string.date_long)
            4 -> getString(R.string.date_full)
            5 -> getString(R.string.date_short)
            else -> getString(R.string.date_standard)
        }
    }

    private fun startSyncInfoUpdates() {
        if (syncUpdateActive) return
        syncUpdateActive = true

        val syncUpdateTask = object : Runnable {
            override fun run() {
                if (syncUpdateActive) {
                    updateSyncInfo()
                    syncUpdateHandler.postDelayed(this, 1000)
                }
            }
        }
        syncUpdateHandler.post(syncUpdateTask)
    }

    private fun stopSyncInfoUpdates() {
        syncUpdateActive = false
        syncUpdateHandler.removeCallbacksAndMessages(null)
    }

    private fun updateSyncInfo() {
        val showMore = settings.showSyncInfo

        if (!showMore) {
            binding.txtGpsSyncInfo.visibility = View.GONE
            return
        }

        binding.txtGpsSyncInfo.visibility = View.VISIBLE

        if (!RealTime.isInitialized()) {
            binding.txtGpsSyncInfo.text = String.format(Locale.ENGLISH, "RTT: \u2014 ms")
            return
        }

        val rttMs = RealTime.getLastGpsRoundTripTimeMs()
        if (rttMs >= 0) {
            binding.txtGpsSyncInfo.text = String.format(Locale.ENGLISH, "RTT: %d ms", rttMs)
        } else {
            binding.txtGpsSyncInfo.text = String.format(Locale.ENGLISH, "RTT: \u2014 ms")
        }
    }

    companion object {
        private val COLOR_VALUES = intArrayOf(
            -1,
            0xFF6750A4.toInt(),
            0xFF1976D2.toInt(),
            0xFF388E3C.toInt(),
            0xFFD32F2F.toInt(),
            0xFFF57C00.toInt(),
            0xFF00796B.toInt(),
            0xFFC2185B.toInt(),
            0xFF303F9F.toInt(),
        )

        private val COLOR_NAMES = arrayOf(
            "System default (Material You)",
            "Purple", "Blue",
            "Green", "Red",
            "Orange", "Teal",
            "Pink", "Indigo",
            "Custom\u2026"
        )

        private val FONT_NAMES = arrayOf(
            "Default",
            "Monospace",
            "Serif",
            "Sans-serif"
        )

        private val FONT_VALUES = arrayOf(
            Settings.FONT_DEFAULT,
            Settings.FONT_MONOSPACE,
            Settings.FONT_SERIF,
            Settings.FONT_SANS_SERIF
        )
    }
}
