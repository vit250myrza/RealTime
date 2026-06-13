package com.opensource.gpstime.realtime.utils

import android.content.Context
import android.content.pm.PackageManager

object RealTimeUtils {

    @Suppress("BooleanMethodIsAlwaysInverted")
    @JvmStatic
    fun manifestPermissionIsPresent(context: Context, permission: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions ?: return false
            permissions.any { it == permission }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            false
        }
    }
}
