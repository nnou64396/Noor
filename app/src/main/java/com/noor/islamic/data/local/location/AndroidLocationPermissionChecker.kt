package com.noor.islamic.data.local.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.noor.islamic.domain.repository.LocationPermissionChecker

/**
 * Checks coarse/fine location permission. A single coarse grant is enough.
 * `shouldShowRationale()` is only meaningful when an [Activity] context is
 * available (it degrades to false for application contexts).
 */
class AndroidLocationPermissionChecker(
    private val context: Context,
) : LocationPermissionChecker {

    override fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun shouldShowRationale(): Boolean {
        val activity = context as? Activity ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}