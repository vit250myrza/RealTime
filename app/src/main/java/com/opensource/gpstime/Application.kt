package com.opensource.gpstime

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.opensource.gpstime.realtime.RealTime
import java.util.concurrent.TimeUnit

class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        RealTime.builder(this)
            .withGpsProvider()
            .setLoggingEnabled(BuildConfig.DEBUG)
            .setSyncBackoffDelay(5, TimeUnit.MINUTES)
            .build { date -> Log.d(TAG, "RealTime is initialized, current dateTime: $date") }
    }

    companion object {
        private const val TAG = "Application"
    }
}
