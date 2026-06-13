package com.opensource.gpstime.realtime.utils

import android.util.Log

object LogUtils {

    private var loggingEnabled = true

    fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    fun v(tag: String, msg: String) {
        if (loggingEnabled) Log.v(tag, msg)
    }

    fun d(tag: String, msg: String) {
        if (loggingEnabled) Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (loggingEnabled) Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        if (loggingEnabled) Log.w(tag, msg)
    }

    fun w(tag: String, msg: String, t: Throwable) {
        if (loggingEnabled) Log.w(tag, msg, t)
    }

    fun e(tag: String, msg: String) {
        if (loggingEnabled) Log.e(tag, msg)
    }

    fun e(tag: String, msg: String, t: Throwable) {
        if (loggingEnabled) Log.e(tag, msg, t)
    }
}
