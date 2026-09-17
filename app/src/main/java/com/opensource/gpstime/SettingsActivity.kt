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
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
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
    private var defaultSwitchThumbTint: ColorStateList? = null
    private var defaultSwitchTrackTint: ColorStateList? = null

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

        saveDefaults()
        applySwitchColors()
        applyAccentColors()
    }

    private fun applyAccentColors() {
        val accentColor = settings.getAccentColor(this)
        val contrast = UiColorUtils.getContrastingTextColor(accentColor)

        binding.toolbar.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.toolbar.setTitleTextColor(contrast)
        binding.toolbar.navigationIcon?.setTint(contrast)

        window.statusBarColor = accentColor
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = (contrast == Color.BLACK)

        val surfaceColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface, Color.WHITE)
        val btnTextColor = UiColorUtils.ensureContrast(accentColor, surfaceColor, minContrast = 4.5)

        binding.btnTimeFormat.setTextColor(btnTextColor)
        binding.btnDateFormat.setTextColor(btnTextColor)
        binding.btnAccentColor.setTextColor(btnTextColor)
        binding.btnFont.setTextColor(btnTextColor)

        if (settings.accentColorRaw == -1) {
            resetChipColorsToTheme()
        } else {
            val chipBg = createChipBackgroundColorStateList(accentColor, surfaceColor)
            val chipText = createChipTextColorStateList(accentColor, contrast, surfaceColor)

            val chips = arrayOf(
                binding.chipDigital,
                binding.chipAnalog,
                binding.chipBoth,
                binding.chipThemeSystem,
                binding.chipThemeLight,
                binding.chipThemeDark
            )
            for (chip in chips) {
                chip.chipBackgroundColor = chipBg
                chip.setTextColor(chipText)
            }
        }
    }

    private fun saveDefaults() {
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
        defaultSwitchThumbTint = binding.swShowDate.thumbTintList
        defaultSwitchTrackTint = binding.swShowDate.trackTintList
    }

    private fun resetChipColorsToTheme() {
        val chips = arrayOf(
            binding.chipDigital,
            binding.chipAnalog,
            binding.chipBoth,
            binding.chipThemeSystem,
            binding.chipThemeLight,
            binding.chipThemeDark
        )
        for (i in chips.indices) {
            chips[i].chipBackgroundColor = defaultChipBackgroundColors?.get(i)
            chips[i].setTextColor(defaultChipTextColors?.get(i))
        }
    }

    private fun createChipBackgroundColorStateList(accentColor: Int, surfaceColor: Int): ColorStateList {
        val uncheckedBg = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.12f), surfaceColor)
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                uncheckedBg
            )
        )
    }

    private fun createChipTextColorStateList(accentColor: Int, contrast: Int, surfaceColor: Int): ColorStateList {
        val uncheckedBg = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.12f), surfaceColor)
        val uncheckedTextColor = UiColorUtils.ensureContrast(accentColor, uncheckedBg, minContrast = 4.5)
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                contrast,
                uncheckedTextColor
            )
        )
    }

    private fun applySwitchColors() {
        val switches = arrayOf(
            binding.swShowDate,
            binding.swShowSeconds,
            binding.swShowMillis,
            binding.sw24h,
            binding.swUtc,
            binding.swShowSyncInfo
        )

        if (settings.accentColorRaw == -1) {
            for (sw in switches) {
                sw.thumbTintList = defaultSwitchThumbTint
                sw.trackTintList = defaultSwitchTrackTint
            }
            return
        }

        val accentColor = settings.getAccentColor(this)
        val surfaceColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface, Color.WHITE)
        val isDark = ColorUtils.calculateLuminance(surfaceColor) < 0.5

        val checkedThumbColor = UiColorUtils.ensureContrast(accentColor, surfaceColor, minContrast = 3.0)
        val uncheckedThumbColor = if (isDark) 0xFF938F99.toInt() else 0xFF79747E.toInt()

        val checkedTrackColor = UiColorUtils.adjustAlpha(checkedThumbColor, 0.40f)
        val uncheckedTrackColor = if (isDark) 0x33FFFFFF else 0x26000000

        val thumbTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                checkedThumbColor,
                uncheckedThumbColor
            )
        )
        val trackTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                checkedTrackColor,
                uncheckedTrackColor
            )
        )

        for (sw in switches) {
            sw.thumbTintList = thumbTint
            sw.trackTintList = trackTint
        }
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
                Settings.applyTheme(Settings.THEME_SYSTEM)
                recreate()
            }
        }
        binding.chipThemeLight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot)
                settings.theme = Settings.THEME_LIGHT
                Settings.applyTheme(Settings.THEME_LIGHT)
                recreate()
            }
        }
        binding.chipThemeDark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !initializing) {
                TransitionManager.beginDelayedTransition(themeRoot)
                settings.theme = Settings.THEME_DARK
                Settings.applyTheme(Settings.THEME_DARK)
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
        val padHorizontal = (24 * resources.displayMetrics.density).toInt()
        val padVertical = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padHorizontal, padVertical, padHorizontal, padVertical)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_color)
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
                    binding.btnAccentColor.text = getAccentLabel(settings.accentColorRaw)
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
