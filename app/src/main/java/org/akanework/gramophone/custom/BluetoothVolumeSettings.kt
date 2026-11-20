/*
 * Gramophone Kiosk: optional Bluetooth audio guard.
 * GPL-3.0-or-later, matching the upstream Gramophone project.
 */
package org.akanework.gramophone.custom

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.preference.PreferenceManager

/** Audio-output attenuation, not a calibrated hearing-safety dB limit. */
object BluetoothVolumeSettings {
    const val ADMIN_MENU_ID = 0x45A002
    const val KEY_ENABLED = "kiosk_bt_limit_enabled"
    const val KEY_MAX_PERCENT = "kiosk_bt_limit_percent"
    const val KEY_NORMALIZE_ALBUMS = "kiosk_bt_normalize_albums"

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getBoolean(KEY_ENABLED, false)

    fun getLimitPercent(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getInt(KEY_MAX_PERCENT, 40).coerceIn(5, 100)

    fun normalizeAlbums(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getBoolean(KEY_NORMALIZE_ALBUMS, false)

    fun save(context: Context, enabled: Boolean, percent: Int, normalize: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_MAX_PERCENT, percent.coerceIn(5, 100))
            .putBoolean(KEY_NORMALIZE_ALBUMS, normalize)
            .apply()
    }

    /** Match the *actual routed output*, not merely a paired or connected device. */
    fun isBluetoothRoute(type: Int?): Boolean {
        if (type == null) return false
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            type == AudioDeviceInfo.TYPE_HEARING_AID) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
             type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            type == AudioDeviceInfo.TYPE_BLE_BROADCAST) return true
        return false
    }

    /** Unknown route is conservatively capped until AudioTrack reports the destination. */
    fun shouldLimit(context: Context, routedDeviceType: Int?): Boolean =
        isEnabled(context) && (routedDeviceType == null || isBluetoothRoute(routedDeviceType))

    /** Absolute maximum app-level output multiplier (does not control headphone volume). */
    fun capGain(context: Context, routedDeviceType: Int?): Float =
        if (shouldLimit(context, routedDeviceType)) getLimitPercent(context) / 100f else 1f

    fun limitGain(requested: Float, maxAllowed: Float): Float =
        requested.coerceIn(0f, maxAllowed.coerceIn(0f, 1f))
}
