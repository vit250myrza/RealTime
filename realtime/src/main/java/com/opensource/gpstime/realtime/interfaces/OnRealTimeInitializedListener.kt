package com.opensource.gpstime.realtime.interfaces

import java.util.Date

fun interface OnRealTimeInitializedListener {
    fun onInitialized(date: Date)
}
