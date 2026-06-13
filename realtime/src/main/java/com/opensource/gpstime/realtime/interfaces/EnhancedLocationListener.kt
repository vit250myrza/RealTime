package com.opensource.gpstime.realtime.interfaces

import android.location.Location
import android.location.LocationListener

abstract class EnhancedLocationListener : LocationListener {

    abstract fun onLocationChanged(location: Location, timeInMs: Long)

    override fun onLocationChanged(location: Location) {
        var gpsTime = location.time
        if (gpsTime > 0 && gpsTime < 1673000000000L)
            gpsTime += 619315200000L
        location.time = gpsTime
        onLocationChanged(location, gpsTime)
    }
}
