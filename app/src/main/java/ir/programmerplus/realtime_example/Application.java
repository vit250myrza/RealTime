package ir.programmerplus.realtime_example;


import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.multidex.MultiDexApplication;
import ir.programmerplus.realtime.RealTime;

public class Application extends MultiDexApplication {

    private static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        RealTime.builder(this)
                .withGpsProvider()
                .setLoggingEnabled(BuildConfig.DEBUG)
                .setSyncBackoffDelay(5, TimeUnit.MINUTES)
                .build(date -> Log.d(TAG, "RealTime is initialized, current dateTime: " + date));
    }
}
