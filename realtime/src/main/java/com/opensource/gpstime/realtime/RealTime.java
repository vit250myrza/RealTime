package com.opensource.gpstime.realtime;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.opensource.gpstime.realtime.interfaces.EnhancedLocationListener;
import com.opensource.gpstime.realtime.interfaces.OnRealTimeInitializedListener;
import com.opensource.gpstime.realtime.utils.CacheUtils;
import com.opensource.gpstime.realtime.utils.LogUtils;
import com.opensource.gpstime.realtime.utils.RealTimeUtils;

/**
 * Using RealTime class, you only need to initialize current reliable time once using GPS provider
 * and use the reliable current time until next boot of device.
 * <p/>
 * Author: Homayoon Ahmadi
 * <br/>
 * Email: homayoon.ahmadi8@gmail.com
 */
public class RealTime implements LifecycleEventObserver {

    private static final String TAG = RealTime.class.getSimpleName();
    private static final long RESYNC_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    private long backoffDelay;
    private boolean gpsProviderEnabled = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Context context;
    private final LocationManager locationManager;
    private OnRealTimeInitializedListener initializedListener;

    private static final ObservableBoolean INITIALIZED = new ObservableBoolean();

    private static long lastGpsRoundTripTimeMs = -1;

    private static RealTime instance;
    private Runnable resyncRunnable;


    /**
     * The constructor will initialize all needed classes
     *
     * @param context application context
     */
    private RealTime(Context context) {
        context = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? context.createDeviceProtectedStorageContext() : context;

        CacheUtils.initialize(context);

        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        initRealTimeStatusObservable();

        INITIALIZED.set(isInitialized());

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_START, ON_RESUME -> {
                LogUtils.i(TAG, "Application is in foreground. Lifecycle event: " + event);

                if (isInitialized() && cachedTimeIsValid(backoffDelay)) {
                    LogUtils.v(TAG, "RealTime cached time is valid. No need to resynchronize RealTime at this time.");
                } else {
                    LogUtils.v(TAG, "RealTime cached time is NOT valid. Trying to resynchronize RealTime...");
                    build();
                }
            }
            case ON_STOP -> LogUtils.i(TAG, "Application is in background");
        }
    }

    /**
     * This function will initialize a singleton instance of the RealTime class
     *
     * @param context application context
     * @return RealTime instance
     */
    public static RealTime builder(Context context) {

        synchronized (RealTime.class) {
            if (instance == null) {
                instance = new RealTime(context);
            }
        }

        return instance;
    }

    /**
     * This method will set an observable on RealTime initialize state
     * if class is not initialized or cached data are cleared, we try to
     * reinitialize the RealTime again.
     */
    private void initRealTimeStatusObservable() {

        INITIALIZED.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                LogUtils.v(TAG, "RealTime " + (INITIALIZED.get() ? "is" : "is NOT") + " initialized.");

                if (!INITIALIZED.get()) {
                    long cachedTime = CacheUtils.getCachedTime();
                    long cachedBootTime = CacheUtils.getCachedBootTime();
                    long cachedDeviceUptime = CacheUtils.getCachedDeviceUptime();

                    if (cachedTime == 0 || cachedBootTime == 0 || cachedDeviceUptime == 0) {
                        LogUtils.d(TAG, "Cached data are unavailable. Try to reinitialize RealTime...");
                        build();
                    }
                }
            }
        });
    }

    /**
     * This method enables GPS provider. Using this function, you can get current time from GPS satellites.
     * <p>
     * You have to add either of {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} permissions in your manifest
     * and make sure to get permissions in runtime from the user.
     *
     * @return RealTime instance
     */
    public RealTime withGpsProvider() {
        // check if we have at least one of location permissions in the manifest
        if (!RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                !RealTimeUtils.manifestPermissionIsPresent(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            throw new IllegalStateException("You need to add location permissions to your manifest.");
        }

        gpsProviderEnabled = true;
        return this;
    }

    /**
     * This function enables and disables logging information to logcat
     *
     * @param enabled sets weather logs must be enabled or not
     * @return RealTime instance
     */
    public RealTime setLoggingEnabled(boolean enabled) {
        LogUtils.setLoggingEnabled(enabled);

        return this;
    }

    /**
     * Sets the backoff delay for re-syncing RealTime with time providers.
     *
     * @param backoffDelay the duration of the backoff delay
     * @param unit         the unit of time for the backoff delay
     * @return the RealTime instance with the updated backoff delay
     * @throws NullPointerException if the unit parameter is null
     */
    public RealTime setSyncBackoffDelay(long backoffDelay, @NonNull java.util.concurrent.TimeUnit unit) {
        this.backoffDelay = java.util.concurrent.TimeUnit.MILLISECONDS.convert(backoffDelay, unit);

        return this;
    }

    /**
     * This function will set onInitializeListener and build the RealTime and starts
     * to sync with requested providers
     *
     * @param onInitializedListener listener to get notified when initialized
     */
    public void build(OnRealTimeInitializedListener onInitializedListener) {
        this.initializedListener = onInitializedListener;

        if (isInitialized() && onInitializedListener != null) {
            onInitializedListener.onInitialized(now());
        }

        build();
    }

    /**
     * This function will build the RealTime class and starts to sync time using
     * requested providers.
     */
    public void build() {

        LogUtils.v(TAG, "Starting to build RealTime...");

        if (gpsProviderEnabled) {
            requestLocationUpdates();
        }
    }

    private boolean cachedTimeIsValid(long backoffDelay) {
        if (backoffDelay < 0) return true;
        if (backoffDelay == 0) return false;

        long cachedTime = CacheUtils.getCachedTime();
        long timeNow = now().getTime();

        return (timeNow - cachedTime) < backoffDelay;
    }

    /**
     * This method will show that RealTime class is initialized with a reliable time or not
     *
     * @return is RealTime initialized or not
     */
    public static boolean isInitialized() {
        return isCachedTimeValid();
    }

    /**
     * This function returns current reliable datetime if the class was initialized before
     *
     * @return current reliable date object
     * @throws IllegalStateException if the class is not initialized yet
     */
    public static Date now() throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("You need to init RealTime at least once.");
        }

        long cachedTime = CacheUtils.getCachedTime();
        long cachedDeviceUptime = CacheUtils.getCachedDeviceUptime();
        long deviceUptime = SystemClock.elapsedRealtime();
        long now = cachedTime + (deviceUptime - cachedDeviceUptime);

        return new Date(now);
    }

    public static long getCachedTimeAgeMillis() {
        long cachedTime = CacheUtils.getCachedTime();
        if (cachedTime == 0) {
            return -1;
        }
        return Math.max(0, System.currentTimeMillis() - cachedTime);
    }

    public static long getCachedTimeAgeSinceBootMillis() {
        long cachedDeviceUptime = CacheUtils.getCachedDeviceUptime();
        if (cachedDeviceUptime == 0) {
            return -1;
        }
        return Math.max(0, SystemClock.elapsedRealtime() - cachedDeviceUptime);
    }

    public static long getLastGpsRoundTripTimeMs() {
        return lastGpsRoundTripTimeMs;
    }

    /**
     * In this function we request location updates if we have location permission and gps provider
     * is enabled by user.
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LogUtils.i(TAG, "Location provider is enabled.");
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, locationListener);
            LogUtils.d(TAG, "Requesting time from location provider...");
            
            // Schedule the next resync in 5 minutes
            scheduleNextResync();
        } else {
            LogUtils.w(TAG, "Location permission was not granted.");
        }
    }
    
    /**
     * Schedule the next GPS resync for 5 minutes from now
     */
    private void scheduleNextResync() {
        // Cancel any previously scheduled resync
        if (resyncRunnable != null) {
            handler.removeCallbacks(resyncRunnable);
        }
        
        // Create and schedule the resync runnable
        resyncRunnable = () -> {
            LogUtils.d(TAG, "Triggering periodic resync after 5 minutes.");
            requestLocationUpdates();
        };
        
        handler.postDelayed(resyncRunnable, RESYNC_INTERVAL_MS);
        LogUtils.d(TAG, "Scheduled next resync in 5 minutes.");
    }

    /**
     * This function will check if we have a valid cached time
     *
     * @return true if cache is valid, false otherwise
     */
    private static boolean isCachedTimeValid() {
        long cachedBootTime = CacheUtils.getCachedBootTime();
        if (cachedBootTime == 0) {
            return false;
        }

        // has boot time changed (simple check)
        boolean bootTimeChanged = SystemClock.elapsedRealtime() < CacheUtils.getCachedDeviceUptime();
        return !bootTimeChanged;
    }


    /**
     * This function clears all cached data. We use this function to clear expired datetime
     * after reboot and try to reinitialize the RealTime.
     */
    public static void clearCachedInfo() {
        CacheUtils.setCachedTime(0L);
        CacheUtils.setCachedBootTime(0);
        CacheUtils.setCachedDeviceUptime(0);

        LogUtils.d(TAG, "RealTime disk cache cleared.");

        INITIALIZED.set(false);
    }

    /**
     * This function will set time and cache needed data to preferences.
     *
     * @param time reliable current time
     */
    @SuppressLint("MissingPermission")
    private void setTime(Long time) {
        if (time == null || time == 0) return;

        long deviceUptime = SystemClock.elapsedRealtime();
        long bootTime = time - deviceUptime;

        // write data to cache
        CacheUtils.setCachedTime(time);
        CacheUtils.setCachedBootTime(bootTime);
        CacheUtils.setCachedDeviceUptime(deviceUptime);

        if (!INITIALIZED.get()) {
            INITIALIZED.set(true);
        }

        // populate results
        if (initializedListener != null)
            initializedListener.onInitialized(new Date(time));
    }

    /**
     * Here we implement a location listener to get date from location provider
     */
    private final LocationListener locationListener = new EnhancedLocationListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLocationChanged(@NonNull Location location, long gpsTime) {
            LogUtils.i(TAG, "Time from location provider: " + new Date(gpsTime));

            // Project GPS fix time forward using elapsedRealtime to cancel transport delay
            long adjustedTime = gpsTime;
            long rttMs = -1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                long fixElapsedMs = location.getElapsedRealtimeNanos() / 1000000;
                long delayMs = SystemClock.elapsedRealtime() - fixElapsedMs;
                if (delayMs > 0 && delayMs < 10000) {
                    rttMs = delayMs * 2;
                    adjustedTime = gpsTime + delayMs;
                }
            }
            lastGpsRoundTripTimeMs = rttMs;
            setTime(adjustedTime);

            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
            
            // Schedule next resync
            scheduleNextResync();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            LogUtils.v(TAG, "Location provider: " + provider + " is disabled.");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            LogUtils.v(TAG, "Location provider enabled.");

            if (locationManager != null && !isInitialized()) {
                requestLocationUpdates();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // we need to implement this empty method to prevent crash on API 21 (Android 5)
        }
    };

}
