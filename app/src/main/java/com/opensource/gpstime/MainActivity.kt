package com.opensource.gpstime

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.opensource.gpstime.databinding.ActivityMainBinding
import com.opensource.gpstime.realtime.RealTime
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updatesActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        settings = Settings(this)
        Settings.applyTheme(settings.theme)
        super.onCreate(savedInstanceState)

        enableFullscreenMode()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnClearCache.setOnClickListener {
            RealTime.clearCachedInfo()
        }

        requestLocationPermission()
        applyClockMode()
        applyFont()
        applyUiColors()
    }

    override fun onResume() {
        super.onResume()
        applyClockMode()
        applyFont()
        applyUiColors()
        startClockUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopClockUpdates()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun enableFullscreenMode() {
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun isIsoPortrait(): Boolean {
        return settings.timeFormat == 0 && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun isIsoLandscape(): Boolean {
        return settings.timeFormat == 0 && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun startClockUpdates() {
        if (updatesActive) return
        updatesActive = true

        val timePat: String = if (settings.timeFormat == 0) {
            if (isIsoPortrait()) "HH:mm:ss.SSS" else ISO_LANDSCAPE_PATTERN
        } else {
            settings.buildTimePattern()
        }
        val timeOnlyPat: String = if (settings.timeFormat == 0) "HH:mm:ss.SSS" else settings.buildTimePattern()

        val timeFormat = SimpleDateFormat(timePat, Locale.ENGLISH)
        val dateOnlyFormat = SimpleDateFormat(settings.datePattern, Locale.ENGLISH)
        val timeOnlyFormat = SimpleDateFormat(timeOnlyPat, Locale.ENGLISH)
        if (settings.showUtc) {
            val utc = TimeZone.getTimeZone("UTC")
            timeFormat.timeZone = utc
            dateOnlyFormat.timeZone = utc
            timeOnlyFormat.timeZone = utc
        }

        val updateTask = object : Runnable {
            override fun run() {
                try {
                    updateDisplay(timeFormat, dateOnlyFormat, timeOnlyFormat)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in updateDisplay", e)
                }
                if (updatesActive) {
                    val ip = isIsoPortrait()
                    if (settings.timeFormat == 0) {
                        timeFormat.applyPattern(if (ip) "HH:mm:ss.SSS" else ISO_LANDSCAPE_PATTERN)
                        timeOnlyFormat.applyPattern("HH:mm:ss.SSS")
                    } else {
                        val pat = settings.buildTimePattern()
                        timeFormat.applyPattern(pat)
                        timeOnlyFormat.applyPattern(pat)
                    }
                    dateOnlyFormat.applyPattern(settings.datePattern)
                    val interval = computeUpdateInterval()
                    updateHandler.postDelayed(this, interval)
                }
            }
        }
        updateHandler.post(updateTask)
    }

    private fun computeUpdateInterval(): Long {
        val showSec = settings.showSeconds
        val showMs = showSec && settings.showMillis
        return if (showMs) getDeviceRefreshIntervalMillis() else 1000L
    }

    private fun getDeviceRefreshIntervalMillis(): Long {
        var refreshRate = 60f
        refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
        if (refreshRate <= 0) {
            refreshRate = 60f
        }
        return Math.max(8L, Math.round(1000f / refreshRate).toLong())
    }

    private fun stopClockUpdates() {
        updatesActive = false
        updateHandler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplay(
        timeFormat: SimpleDateFormat,
        dateOnlyFormat: SimpleDateFormat,
        timeOnlyFormat: SimpleDateFormat
    ) {
        val mode = settings.clockMode
        val showDate = settings.showDate

        if (!RealTime.isInitialized()) {
            val wait = getString(R.string.waiting_for_gps)
            when (mode) {
                Settings.CLOCK_MODE_DIGITAL -> {
                    setClockModeVisible(Settings.CLOCK_MODE_DIGITAL)
                    binding.txtTime.text = "--:--:--"
                    binding.txtStatus.visibility = View.VISIBLE
                    binding.txtStatus.text = wait
                    binding.txtDate.visibility = if (showDate) View.VISIBLE else View.GONE
                    binding.txtDate.text = ""
                }
                Settings.CLOCK_MODE_ANALOG -> {
                    setClockModeVisible(Settings.CLOCK_MODE_ANALOG)
                    binding.txtAnalogTime.text = ""
                }
                Settings.CLOCK_MODE_BOTH -> {
                    setClockModeVisible(Settings.CLOCK_MODE_BOTH)
                    binding.txtBothTime.text = "--:--:--"
                    binding.txtBothStatus.visibility = View.VISIBLE
                    binding.txtBothStatus.text = wait
                    binding.txtBothDate.visibility = if (showDate) View.VISIBLE else View.GONE
                    binding.txtBothDate.text = ""
                }
            }
            updateSyncInfo()
            return
        }

        val isoLandscape = isIsoLandscape()

        val now = RealTime.now()
        val timeStr = timeFormat.format(now)
        val dateStr = if (showDate) dateOnlyFormat.format(now) else ""
        val shortTime = timeOnlyFormat.format(now)

        when (mode) {
            Settings.CLOCK_MODE_DIGITAL -> {
                setClockModeVisible(Settings.CLOCK_MODE_DIGITAL)
                binding.txtTime.text = timeStr
                binding.txtStatus.visibility = View.GONE
                binding.txtDate.visibility = if (showDate && !isoLandscape) View.VISIBLE else View.GONE
                binding.txtDate.text = dateStr
            }
            Settings.CLOCK_MODE_ANALOG -> {
                setClockModeVisible(Settings.CLOCK_MODE_ANALOG)
                binding.analogClock.setTime(now)
                binding.txtAnalogTime.text = shortTime
            }
            Settings.CLOCK_MODE_BOTH -> {
                setClockModeVisible(Settings.CLOCK_MODE_BOTH)
                binding.analogClockBoth.setTime(now)
                binding.txtBothTime.text = timeStr
                binding.txtBothStatus.visibility = View.GONE
                binding.txtBothDate.visibility = if (showDate && !isoLandscape) View.VISIBLE else View.GONE
                binding.txtBothDate.text = dateStr
            }
        }

        if (isIsoPortrait()) {
            val tzFmt = SimpleDateFormat(if (settings.showUtc) "z" else "XXX", Locale.ENGLISH)
            if (settings.showUtc) tzFmt.timeZone = TimeZone.getTimeZone("UTC")
            val tzStr = tzFmt.format(now)
            when (mode) {
                Settings.CLOCK_MODE_DIGITAL -> {
                    binding.txtStatus.visibility = View.VISIBLE
                    binding.txtStatus.text = tzStr
                }
                Settings.CLOCK_MODE_BOTH -> {
                    binding.txtBothStatus.visibility = View.VISIBLE
                    binding.txtBothStatus.text = tzStr
                }
            }
        }

        updateSyncInfo()
    }

    private fun setClockModeVisible(mode: Int) {
        binding.analogContainer.visibility = View.GONE
        binding.bothContainer.visibility = View.GONE
        binding.digitalContainer.visibility = View.GONE

        when (mode) {
            Settings.CLOCK_MODE_DIGITAL -> binding.digitalContainer.visibility = View.VISIBLE
            Settings.CLOCK_MODE_ANALOG -> binding.analogContainer.visibility = View.VISIBLE
            Settings.CLOCK_MODE_BOTH -> binding.bothContainer.visibility = View.VISIBLE
        }
    }

    private fun applyClockMode() {
        setClockModeVisible(settings.clockMode)
    }

    private fun applyUiColors() {
        val accentColor = settings.getAccentColor(this)
        val baseWindowBg = MaterialColors.getColor(binding.root, R.attr.clockBackgroundColor, Color.WHITE)
        val rootBg = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.05f), baseWindowBg)
        binding.root.setBackgroundColor(rootBg)

        val baseCardBg = MaterialColors.getColor(binding.cardTime, R.attr.clockCardBackgroundColor, Color.LTGRAY)
        val cardBg = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.08f), baseCardBg)
        binding.cardTime.setCardBackgroundColor(cardBg)
        binding.cardBoth.setCardBackgroundColor(cardBg)

        val contrastOnAccent = UiColorUtils.getContrastingTextColor(accentColor)
        binding.btnSettings.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.btnSettings.setTextColor(contrastOnAccent)

        val clearCacheBg = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.20f), rootBg)
        val clearCacheTextColor = UiColorUtils.ensureContrast(accentColor, clearCacheBg, minContrast = 4.5)
        binding.btnClearCache.backgroundTintList = ColorStateList.valueOf(UiColorUtils.adjustAlpha(accentColor, 0.20f))
        binding.btnClearCache.setTextColor(clearCacheTextColor)

        val syncInfoColor = UiColorUtils.ensureContrast(accentColor, rootBg, minContrast = 4.5)
        binding.txtSyncInfo.setTextColor(syncInfoColor)

        val primaryTextColor = UiColorUtils.ensureContrast(accentColor, cardBg, minContrast = 4.5)
        val secondaryTextColor = UiColorUtils.ensureContrast(UiColorUtils.adjustAlpha(accentColor, 0.80f), cardBg, minContrast = 3.5)

        binding.txtTime.setTextColor(primaryTextColor)
        binding.txtStatus.setTextColor(secondaryTextColor)
        binding.txtDate.setTextColor(secondaryTextColor)

        binding.txtBothTime.setTextColor(primaryTextColor)
        binding.txtBothStatus.setTextColor(secondaryTextColor)
        binding.txtBothDate.setTextColor(secondaryTextColor)

        val analogTimeColor = UiColorUtils.ensureContrast(accentColor, rootBg, minContrast = 4.5)
        binding.txtAnalogTime.setTextColor(analogTimeColor)

        val dialColor = ColorUtils.compositeColors(UiColorUtils.adjustAlpha(accentColor, 0.12f), baseCardBg)
        val clockAccent = UiColorUtils.ensureContrast(accentColor, dialColor, minContrast = 4.5)
        val clockRim = UiColorUtils.ensureContrast(accentColor, rootBg, minContrast = 3.0)
        val clockTick = UiColorUtils.ensureContrast(MaterialColors.getColor(binding.root, R.attr.clockTextColor, Color.DKGRAY), dialColor, minContrast = 3.5)

        binding.analogClock.setFaceColor(dialColor)
        binding.analogClock.setAccentColor(clockAccent)
        binding.analogClock.setRimColor(clockRim)
        binding.analogClock.setTickColor(clockTick)

        binding.analogClockBoth.setFaceColor(dialColor)
        binding.analogClockBoth.setAccentColor(clockAccent)
        binding.analogClockBoth.setRimColor(clockRim)
        binding.analogClockBoth.setTickColor(clockTick)
    }

    private fun updateSyncInfo() {
        val showMore = settings.showSyncInfo

        if (!showMore) {
            binding.txtSyncInfo.visibility = View.GONE
            return
        }

        binding.txtSyncInfo.visibility = View.VISIBLE

        if (!RealTime.isInitialized()) {
            binding.txtSyncInfo.text = String.format(Locale.ENGLISH, "Syncing... (RTT: \u2014 ms)")
            return
        }

        val rttMs = RealTime.getLastGpsRoundTripTimeMs()
        if (rttMs >= 0) {
            binding.txtSyncInfo.text = String.format(Locale.ENGLISH, "RTT: %d ms", rttMs)
        } else {
            binding.txtSyncInfo.text = String.format(Locale.ENGLISH, "RTT: \u2014 ms")
        }
    }

    private fun applyFont() {
        val font = settings.font
        val tf = getTypefaceForFont(font)

        binding.txtTime.setTypeface(tf, Typeface.BOLD)
        binding.txtDate.setTypeface(null, Typeface.NORMAL)
        binding.txtBothTime.setTypeface(tf, Typeface.BOLD)
        binding.txtBothDate.setTypeface(null, Typeface.NORMAL)
        binding.txtAnalogTime.setTypeface(tf, Typeface.NORMAL)
    }

    private fun getTypefaceForFont(font: String): Typeface? {
        if (font.isEmpty()) return null
        return Typeface.create(font, Typeface.NORMAL)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ), 0)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val ISO_LANDSCAPE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    }
}
