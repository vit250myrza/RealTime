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
import com.google.android.material.color.DynamicColors
import com.opensource.gpstime.databinding.ActivityMainBinding
import com.opensource.gpstime.realtime.RealTime
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
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

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

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
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
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

        val timePat: String
        if (settings.timeFormat == 0) {
            timePat = if (isIsoPortrait()) "HH:mm:ss.SSS" else ISO_LANDSCAPE_PATTERN
        } else {
            timePat = settings.buildTimePattern()
        }

        val timeFormat = SimpleDateFormat(timePat, Locale.ENGLISH)
        val dateOnlyFormat = SimpleDateFormat(settings.datePattern, Locale.ENGLISH)
        val timeOnlyFormat = SimpleDateFormat(timePat, Locale.ENGLISH)
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
                        timeOnlyFormat.applyPattern(if (ip) "HH:mm:ss.SSS" else ISO_LANDSCAPE_PATTERN)
                    } else {
                        timeFormat.applyPattern(settings.buildTimePattern())
                        timeOnlyFormat.applyPattern(settings.buildTimePattern())
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

    private fun getContrastingTextColor(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val alphaInt = Math.round(Color.alpha(color) * alpha)
        return (color and 0x00FFFFFF) or (alphaInt shl 24)
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
        val showSec = settings.showSeconds

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

        val appColor = settings.getAccentColor(this)
        when (mode) {
            Settings.CLOCK_MODE_DIGITAL -> {
                binding.txtTime.setTextColor(appColor)
                binding.txtStatus.setTextColor(appColor)
                binding.txtDate.setTextColor(appColor)
            }
            Settings.CLOCK_MODE_ANALOG -> {
                binding.analogClock.setAccentColor(appColor)
                binding.analogClock.setRimColor(appColor)
                binding.txtAnalogTime.setTextColor(appColor)
            }
            Settings.CLOCK_MODE_BOTH -> {
                binding.analogClockBoth.setAccentColor(appColor)
                binding.analogClockBoth.setRimColor(appColor)
                binding.txtBothTime.setTextColor(appColor)
                binding.txtBothStatus.setTextColor(appColor)
                binding.txtBothDate.setTextColor(appColor)
            }
        }
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
        val contrast = getContrastingTextColor(accentColor)

        binding.root.setBackgroundColor(adjustAlpha(accentColor, 0.05f))
        binding.txtSyncInfo.setTextColor(accentColor)

        binding.cardTime.setCardBackgroundColor(adjustAlpha(accentColor, 0.08f))
        binding.cardBoth.setCardBackgroundColor(adjustAlpha(accentColor, 0.08f))
        binding.btnSettings.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.btnSettings.setTextColor(contrast)
        binding.btnClearCache.backgroundTintList = ColorStateList.valueOf(adjustAlpha(accentColor, 0.20f))
        binding.btnClearCache.setTextColor(contrast)

        binding.analogClock.setFaceColor(adjustAlpha(accentColor, 0.12f))
        binding.analogClockBoth.setFaceColor(adjustAlpha(accentColor, 0.12f))
        binding.analogClock.setAccentColor(accentColor)
        binding.analogClockBoth.setAccentColor(accentColor)
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

        binding.txtTime.typeface = tf
        binding.txtDate.typeface = null
        binding.txtBothTime.typeface = tf
        binding.txtBothDate.typeface = null
        binding.txtAnalogTime.typeface = tf
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
