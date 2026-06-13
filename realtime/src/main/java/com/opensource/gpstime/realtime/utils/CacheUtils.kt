package com.opensource.gpstime.realtime.utils

import android.content.Context
import android.content.SharedPreferences
import com.opensource.gpstime.realtime.BuildConfig

object CacheUtils {

    private const val SHARED_PREF_NAME = "RealTimePreference"

    private val KEY_CACHED_TIME = "${BuildConfig.LIBRARY_PACKAGE_NAME}.cached_time"
    private val KEY_CACHED_BOOT_TIME = "${BuildConfig.LIBRARY_PACKAGE_NAME}.cached_boot_time"
    private val KEY_CACHED_DEVICE_UPTIME = "${BuildConfig.LIBRARY_PACKAGE_NAME}.cached_device_uptime"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getCachedTime(): Long = sharedPreferences!!.getLong(KEY_CACHED_TIME, 0)

    fun getCachedDeviceUptime(): Long = sharedPreferences!!.getLong(KEY_CACHED_DEVICE_UPTIME, 0)

    fun getCachedBootTime(): Long = sharedPreferences!!.getLong(KEY_CACHED_BOOT_TIME, 0)

    fun setCachedTime(time: Long) {
        sharedPreferences!!.edit().putLong(KEY_CACHED_TIME, time).apply()
    }

    fun setCachedDeviceUptime(deviceUptime: Long) {
        sharedPreferences!!.edit().putLong(KEY_CACHED_DEVICE_UPTIME, deviceUptime).apply()
    }

    fun setCachedBootTime(bootTime: Long) {
        sharedPreferences!!.edit().putLong(KEY_CACHED_BOOT_TIME, bootTime).apply()
    }
}
