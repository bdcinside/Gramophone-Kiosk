/*
 * Gramophone Kiosk: conservative step-based Android media volume limits.
 * GPL-3.0-or-later, matching the upstream project.
 */
package org.akanework.gramophone.custom

import kotlin.math.floor

/** Percentages refer to the Android STREAM_MUSIC volume *steps*, not dB SPL. */
object SystemVolumePolicy {
    enum class Route { BLUETOOTH, SPEAKER, OTHER }

    fun allowedIndex(maxIndex: Int, minIndex: Int, percent: Int): Int =
        floor(maxIndex.coerceAtLeast(0) * percent.coerceIn(5, 100) / 100.0)
            .toInt().coerceIn(minIndex.coerceAtLeast(0), maxIndex.coerceAtLeast(minIndex.coerceAtLeast(0)))

    fun limitForRoute(route: Route, btLimit: Int?, speakerLimit: Int?): Int? =
        when (route) {
            Route.BLUETOOTH -> btLimit
            Route.SPEAKER -> speakerLimit
            Route.OTHER -> null
        }

    fun clampedValue(current: Int, allowed: Int): Int = current.coerceAtMost(allowed)
}
