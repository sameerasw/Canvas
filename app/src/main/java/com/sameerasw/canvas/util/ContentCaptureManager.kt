package com.sameerasw.canvas.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

/**
 * Manages content capture functionality for notes.
 * Allows users to capture screenshots while taking notes.
 * Available only when the app is running in floating window mode.
 */
object ContentCaptureManager {
    private const val TAG = "ContentCaptureManager"

    /**
     * Launches the system content capture activity.
     * Only available when the app holds the Notes role and is running in floating window.
     *
     * Result codes:
     * - CAPTURE_CONTENT_FOR_NOTE_SUCCESS: Capture succeeded, URI available in result data
     * - CAPTURE_CONTENT_FOR_NOTE_FAILED: Capture failed
     * - CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED: User cancelled the operation
     * - CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED: Not in floating window mode
     * - CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN: Blocked by device policy
     */
    fun launchContentCapture(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.w(TAG, "Content capture requires Android 14 (API 34)+")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE)
            launcher.launch(intent)
            Log.d(TAG, "Content capture intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching content capture", e)
        }
    }

    /**
     * Handles content capture result and extracts the URI.
     * The URI points to the captured image that can be pasted into the note.
     */
    fun handleCaptureResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (Uri) -> Unit = {},
        onFailed: () -> Unit = {},
        onUserCanceled: () -> Unit = {},
        onWindowModeUnsupported: () -> Unit = {},
        onBlockedByAdmin: () -> Unit = {}
    ) {
        when (resultCode) {
            Intent.CAPTURE_CONTENT_FOR_NOTE_SUCCESS -> {
                val uri = data?.data
                if (uri != null) {
                    Log.d(TAG, "Content capture successful: $uri")
                    onSuccess(uri)
                } else {
                    Log.w(TAG, "Content capture succeeded but no URI returned")
                    onFailed()
                }
            }

            Intent.CAPTURE_CONTENT_FOR_NOTE_FAILED -> {
                Log.w(TAG, "Content capture failed")
                onFailed()
            }

            Intent.CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED -> {
                Log.d(TAG, "Content capture cancelled by user")
                onUserCanceled()
            }

            Intent.CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED -> {
                Log.w(TAG, "Content capture not available - app not in floating window mode")
                onWindowModeUnsupported()
            }

            Intent.CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN -> {
                Log.w(TAG, "Content capture blocked by device administrator")
                onBlockedByAdmin()
            }

            else -> {
                Log.w(TAG, "Unknown content capture result code: $resultCode")
                onFailed()
            }
        }
    }

    /**
     * Checks if screen capture is disabled by device policy.
     * Should be called before showing the capture button to the user.
     */
    fun isScreenCaptureDisabled(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        return try {
            val devicePolicyManager = activity.getSystemService(Activity.DEVICE_POLICY_SERVICE)
                    as android.app.admin.DevicePolicyManager
            // Pass null to check for system-wide restriction, not per admin
            devicePolicyManager.getScreenCaptureDisabled(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking screen capture restriction", e)
            false
        }
    }
}

