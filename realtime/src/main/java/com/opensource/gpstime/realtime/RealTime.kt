package com.opensource.gpstime.realtime

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.NonNull
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.opensource.gpstime.realtime.interfaces.EnhancedLocationListener
import com.opensource.gpstime.realtime.interfaces.OnRealTimeInitializedListener
import com.opensource.gpstime.realtime.utils.CacheUtils
import com.opensource.gpstime.realtime.utils.LogUtils
import com.opensource.gpstime.realtime.utils.RealTimeUtils
import java.util.Date
import java.util.concurrent.TimeUnit

class RealTime private constructor(ctx: Context) : LifecycleEventObserver {

    private val context: Context
    private val locationManager: LocationManager
    private var backoffDelay: Long = 0
    private var gpsProviderEnabled: Boolean = false
    private var initializedListener: OnRealTimeInitializedListener? = null
    private var resyncRunnable: Runnable? = null

    private val handler = Handler(Looper.getMainLooper())

    init {
        val protectedCtx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ctx.createDeviceProtectedStorageContext()
        } else {
            ctx
        }
        CacheUtils.initialize(protectedCtx)
        context = protectedCtx.applicationContext
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        initRealTimeStatusObservable()
        INITIALIZED.set(isInitialized())
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(@NonNull source: LifecycleOwner, @NonNull event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                LogUtils.i(TAG, "Application is in foreground. Lifecycle event: $event")
                if (isInitialized() && cachedTimeIsValid(backoffDelay)) {
                    LogUtils.v(TAG, "RealTime cached time is valid. No need to resynchronize RealTime at this time.")
                } else {
                    LogUtils.v(TAG, "RealTime cached time is NOT valid. Trying to resynchronize RealTime...")
                    build()
                }
            }
            Lifecycle.Event.ON_STOP -> {
                LogUtils.i(TAG, "Application is in background")
            }
            else -> {}
        }
    }

    fun withGpsProvider(): RealTime {
        if (!RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            throw IllegalStateException("You need to add location permissions to your manifest.")
        }
        gpsProviderEnabled = true
        return this
    }

    fun setLoggingEnabled(enabled: Boolean): RealTime {
        LogUtils.setLoggingEnabled(enabled)
        return this
    }

    fun setSyncBackoffDelay(backoffDelay: Long, @NonNull unit: TimeUnit): RealTime {
        this.backoffDelay = TimeUnit.MILLISECONDS.convert(backoffDelay, unit)
        return this
    }

    fun build(onInitializedListener: OnRealTimeInitializedListener?) {
        initializedListener = onInitializedListener
        if (isInitialized() && onInitializedListener != null) {
            onInitializedListener.onInitialized(now())
        }
        build()
    }

    fun build() {
        LogUtils.v(TAG, "Starting to build RealTime...")
        if (gpsProviderEnabled) {
            requestLocationUpdates()
        }
    }

    private fun cachedTimeIsValid(backoffDelay: Long): Boolean {
        if (backoffDelay < 0) return true
        if (backoffDelay == 0L) return false
        val cachedTime = CacheUtils.getCachedTime()
        val timeNow = now().time
        return (timeNow - cachedTime) < backoffDelay
    }

    private fun initRealTimeStatusObservable() {
        INITIALIZED.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                LogUtils.v(TAG, "RealTime ${if (INITIALIZED.get()) "is" else "is NOT"} initialized.")
                if (!INITIALIZED.get()) {
                    val cachedTime = CacheUtils.getCachedTime()
                    val cachedBootTime = CacheUtils.getCachedBootTime()
                    val cachedDeviceUptime = CacheUtils.getCachedDeviceUptime()
                    if (cachedTime == 0L || cachedBootTime == 0L || cachedDeviceUptime == 0L) {
                        LogUtils.d(TAG, "Cached data are unavailable. Try to reinitialize RealTime...")
                        build()
                    }
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                LogUtils.i(TAG, "Location provider is enabled.")
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1L, 0f, locationListener)
            LogUtils.d(TAG, "Requesting time from location provider...")
            scheduleNextResync()
        } else {
            LogUtils.w(TAG, "Location permission was not granted.")
        }
    }

    private fun scheduleNextResync() {
        resyncRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            LogUtils.d(TAG, "Triggering periodic resync after 5 minutes.")
            requestLocationUpdates()
        }
        resyncRunnable = runnable
        handler.postDelayed(runnable, RESYNC_INTERVAL_MS)
        LogUtils.d(TAG, "Scheduled next resync in 5 minutes.")
    }

    @SuppressLint("MissingPermission")
    private fun setTime(time: Long?) {
        if (time == null || time == 0L) return
        val deviceUptime = SystemClock.elapsedRealtime()
        val bootTime = time - deviceUptime
        CacheUtils.setCachedTime(time)
        CacheUtils.setCachedBootTime(bootTime)
        CacheUtils.setCachedDeviceUptime(deviceUptime)
        if (!INITIALIZED.get()) {
            INITIALIZED.set(true)
        }
        initializedListener?.onInitialized(Date(time))
    }

    private val locationListener = object : EnhancedLocationListener() {
        @SuppressLint("MissingPermission")
        override fun onLocationChanged(location: Location, gpsTime: Long) {
            LogUtils.i(TAG, "Time from location provider: " + Date(gpsTime))
            var adjustedTime = gpsTime
            var rttMs = -1L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val fixElapsedMs = location.elapsedRealtimeNanos / 1000000
                val delayMs = SystemClock.elapsedRealtime() - fixElapsedMs
                if (delayMs > 0 && delayMs < 10000) {
                    rttMs = delayMs * 2
                    adjustedTime = gpsTime + delayMs
                }
            }
            lastGpsRoundTripTimeMs = rttMs
            setTime(adjustedTime)
            locationManager.removeUpdates(this)
            scheduleNextResync()
        }

        override fun onProviderDisabled(provider: String) {
            LogUtils.v(TAG, "Location provider: $provider is disabled.")
        }

        override fun onProviderEnabled(provider: String) {
            LogUtils.v(TAG, "Location provider enabled.")
            if (!isInitialized()) {
                requestLocationUpdates()
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    companion object {
        private const val TAG = "RealTime"
        private const val RESYNC_INTERVAL_MS = 5 * 60 * 1000L

        val INITIALIZED = ObservableBoolean()

        private var lastGpsRoundTripTimeMs = -1L

        private var instance: RealTime? = null

        @JvmStatic
        fun builder(context: Context): RealTime {
            synchronized(RealTime::class.java) {
                if (instance == null) {
                    instance = RealTime(context)
                }
            }
            return instance!!
        }

        @JvmStatic
        fun isInitialized(): Boolean = isCachedTimeValid()

        @JvmStatic
        fun now(): Date {
            if (!isInitialized()) {
                throw IllegalStateException("You need to init RealTime at least once.")
            }
            val cachedTime = CacheUtils.getCachedTime()
            val cachedDeviceUptime = CacheUtils.getCachedDeviceUptime()
            val deviceUptime = SystemClock.elapsedRealtime()
            return Date(cachedTime + (deviceUptime - cachedDeviceUptime))
        }

        @JvmStatic
        fun getCachedTimeAgeMillis(): Long {
            val cachedTime = CacheUtils.getCachedTime()
            return if (cachedTime == 0L) -1 else Math.max(0, System.currentTimeMillis() - cachedTime)
        }

        @JvmStatic
        fun getCachedTimeAgeSinceBootMillis(): Long {
            val cachedDeviceUptime = CacheUtils.getCachedDeviceUptime()
            return if (cachedDeviceUptime == 0L) -1 else Math.max(0, SystemClock.elapsedRealtime() - cachedDeviceUptime)
        }

        @JvmStatic
        fun getLastGpsRoundTripTimeMs(): Long = lastGpsRoundTripTimeMs

        @JvmStatic
        fun clearCachedInfo() {
            CacheUtils.setCachedTime(0L)
            CacheUtils.setCachedBootTime(0L)
            CacheUtils.setCachedDeviceUptime(0L)
            LogUtils.d(TAG, "RealTime disk cache cleared.")
            INITIALIZED.set(false)
        }

        private fun isCachedTimeValid(): Boolean {
            val cachedBootTime = CacheUtils.getCachedBootTime()
            if (cachedBootTime == 0L) return false
            val bootTimeChanged = SystemClock.elapsedRealtime() < CacheUtils.getCachedDeviceUptime()
            return !bootTimeChanged
        }
    }
}
