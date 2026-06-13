# RealTime (GPS-only fork)

[![platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16)
[![Build](https://github.com/vit250myrza/RealTime/actions/workflows/android.yml/badge.svg)](https://github.com/vit250myrza/RealTime/actions/workflows/android.yml)
[![Download APK](https://img.shields.io/badge/Download-APK-blue?logo=android)](https://github.com/vit250myrza/RealTime/actions/workflows/android.yml)

GPS-only fork of RealTime — a reliable time library for Android. Gets the current time from GPS satellites and maintains it across device clock changes until the next reboot. NTP and HTTP time server providers have been removed.

# Features
- Single time provider: **GPS satellites** via `LocationManager`
- Caches the GPS time and projects it forward using `SystemClock.elapsedRealtime()`, immune to user clock changes
- Compensates for GPS-to-app transport delay using `location.getElapsedRealtimeNanos()`
- Detects device reboot and re-initializes automatically
- Retries on location provider enabled/disabled changes

# How to add dependency

Step 1. Add the JitPack repository to your build.gradle file

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies {
    implementation 'com.opensource.gpstime:realtime:1.3.0'
}
```

# How to use

Add this to `onCreate` of your `Application` class:

```java
RealTime.builder(this)
      .withGpsProvider()
      .setLoggingEnabled(BuildConfig.DEBUG)
      .setSyncBackoffDelay(30, TimeUnit.SECONDS)
      .build(date -> Log.d(TAG, "RealTime is initialized, current dateTime: " + date));
```

Then everywhere you need reliable time:

```java
if (RealTime.isInitialized()) {
    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
    binding.txtDateTime.setText(isoFormat.format(RealTime.now()));
} else {
    binding.txtDateTime.setText("Waiting for GPS...");
}
```

# Notes
- Requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permission — add to your manifest and request at runtime.
- GPS time has a **transport delay** (200–800ms from satellite fix to app callback). This fork cancels it using `location.getElapsedRealtimeNanos()` so the cached time matches wall clock within ~1ms.
- The example app displays the time as an ISO 8601 timestamp with milliseconds, updated every 50ms.

# Methods

| method                                        | description                                                                                                                     |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| withGpsProvider()                             | Enables GPS provider (requires location permission in manifest).                                                                |
| setLoggingEnabled(boolean enabled)            | Enables or disables logcat logging.                                                                                             |
| setSyncBackoffDelay(long delay, TimeUnit unit)| Sets the minimum interval between re-sync attempts.                                                                             |
| build()                                       | Starts RealTime initialization using the enabled providers.                                                                     |
| build(OnRealTimeInitializedListener listener) | Starts initialization and notifies via `onInitialized(Date)` callback.                                                          |
| isInitialized()                               | Returns `true` if a reliable time has been cached.                                                                              |
| now()                                         | Returns the current reliable `Date`, projected forward from the last GPS fix.                                                   |
| clearCachedInfo()                             | Clears cached time so RealTime re-initializes on next access.                                                                   |
