/*
 * Gramophone Kiosk: separate PIN-protected Bluetooth and speaker media volume limits.
 * GPL-3.0-or-later, matching the upstream project.
 */
package org.akanework.gramophone.custom

import android.content.Context
import androidx.preference.PreferenceManager

object SystemVolumeSettings {
    const val KEY_BT_SYSTEM_ENABLED = "kiosk_system_bt_limit_enabled"
    const val KEY_BT_SYSTEM_PERCENT = "kiosk_system_bt_limit_percent"
    const val KEY_SPEAKER_ENABLED = "kiosk_system_speaker_limit_enabled"
    const val KEY_SPEAKER_PERCENT = "kiosk_system_speaker_limit_percent"

    private fun prefs(ctx: Context) =
        PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)

    fun btEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_BT_SYSTEM_ENABLED, false)
    fun btPercent(ctx: Context) = prefs(ctx).getInt(KEY_BT_SYSTEM_PERCENT, 40).coerceIn(5, 100)
    fun speakerEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_SPEAKER_ENABLED, false)
    fun speakerPercent(ctx: Context) = prefs(ctx).getInt(KEY_SPEAKER_PERCENT, 40).coerceIn(5, 100)

    fun limitPercent(ctx: Context, route: SystemVolumePolicy.Route): Int? =
        SystemVolumePolicy.limitForRoute(
            route,
            btPercent(ctx).takeIf { btEnabled(ctx) },
            speakerPercent(ctx).takeIf { speakerEnabled(ctx) }
        )

    fun save(ctx: Context, bt: Boolean, btPct: Int, speaker: Boolean, speakerPct: Int) {
        prefs(ctx).edit()
            .putBoolean(KEY_BT_SYSTEM_ENABLED, bt)
            .putInt(KEY_BT_SYSTEM_PERCENT, btPct.coerceIn(5, 100))
            .putBoolean(KEY_SPEAKER_ENABLED, speaker)
            .putInt(KEY_SPEAKER_PERCENT, speakerPct.coerceIn(5, 100))
            .apply()
    }
}
