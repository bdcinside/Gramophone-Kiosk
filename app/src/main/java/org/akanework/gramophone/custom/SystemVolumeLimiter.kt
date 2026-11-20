/*
 * Gramophone Kiosk: best-effort system media volume clamp on the playback service.
 * Does not create a calibrated hearing protection limit. Remote Bluetooth gain
 * and volumes outside this process cannot be reliably constrained by an app.
 * GPL-3.0-or-later, matching the upstream project.
 */
package org.akanework.gramophone.custom

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.AudioDeviceInfo
import android.media.AudioDeviceCallback
import android.media.AudioManager
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.preference.PreferenceManager

class SystemVolumeLimiter(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private val settingsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = clampNow()
    }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in LIMIT_KEYS) handler.post { clampNow() }
    }
    private val poll = object : Runnable {
        override fun run() {
            if (closed) return
            clampNow()
            handler.postDelayed(this, 450L) // backup for OEMs that don't notify observers
        }
    }
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = clampNow()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = clampNow()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        appContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, settingsObserver
        )
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        handler.post(poll)
    }

    /** Actual AudioTrack output type is supplied by Gramophone's existing routing callback. */
    fun setActualRoute(deviceType: Int?) {
        activeType = deviceType
        clampNow()
    }

    private fun clampNow() {
        if (closed) return
        try {
            clamp(appContext, audioManager)
        } catch (e: SecurityException) {
            Log.w(TAG, "MIUI/Android denied setStreamVolume", e)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to apply volume limit", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        handler.removeCallbacks(poll)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        appContext.contentResolver.unregisterContentObserver(settingsObserver)
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        activeType = null
    }

    companion object {
        private const val TAG = "KioskVolumeLimiter"
        private val LIMIT_KEYS = setOf(
            SystemVolumeSettings.KEY_BT_SYSTEM_ENABLED,
            SystemVolumeSettings.KEY_BT_SYSTEM_PERCENT,
            SystemVolumeSettings.KEY_SPEAKER_ENABLED,
            SystemVolumeSettings.KEY_SPEAKER_PERCENT
        )
        @Volatile private var activeType: Int? = null

        private fun route(context: Context, manager: AudioManager): SystemVolumePolicy.Route {
            val type = activeType
            if (type != null) {
                if (BluetoothVolumeSettings.isBluetoothRoute(type)) return SystemVolumePolicy.Route.BLUETOOTH
                if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) return SystemVolumePolicy.Route.SPEAKER
                // Wired/USB/HDMI outputs intentionally have no default cap.
                return SystemVolumePolicy.Route.OTHER
            }
            // Before AudioTrack routing is known, Android's BT state is only a heuristic.
            @Suppress("DEPRECATION")
            val bluetoothOn = try { manager.isBluetoothA2dpOn } catch (_: SecurityException) { false }
            if (bluetoothOn) return SystemVolumePolicy.Route.BLUETOOTH
            return SystemVolumePolicy.Route.SPEAKER
        }

        private fun allowed(context: Context, manager: AudioManager): Int? {
            val percent = SystemVolumeSettings.limitPercent(context, route(context, manager))
                ?: return null
            val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                manager.getStreamMinVolume(AudioManager.STREAM_MUSIC) else 0
            return SystemVolumePolicy.allowedIndex(max, min, percent)
        }

        private fun clamp(context: Context, manager: AudioManager) {
            val allowed = allowed(context, manager) ?: return
            val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (current > allowed) {
                // Does not guarantee zero transient overshoot from external BT volume changes.
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, allowed, 0)
            }
        }

        /**
         * For the app foreground, consume hardware volume-UP instead of letting the
         * Android system exceed the chosen media stream cap for even one key press.
         * Other sources (BT remote, system UI) are clamped by the service observer.
         */
        fun handleVolumeUp(context: Context, event: KeyEvent): Boolean {
            if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val allowed = allowed(context, manager) ?: return false
            if (event.action == KeyEvent.ACTION_DOWN) {
                val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val next = (current + 1).coerceAtMost(allowed)
                if (next > current) {
                    try {
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, next, AudioManager.FLAG_SHOW_UI)
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "Couldn't adjust volume", e)
                    }
                }
            }
            return true // also consume KEY_UP, preventing double-adjustment
        }
    }
}
