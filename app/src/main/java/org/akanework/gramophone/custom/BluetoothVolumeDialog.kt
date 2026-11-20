/*
 * Gramophone Kiosk: PIN-protected limits for Android media steps + app attenuation.
 * GPL-3.0-or-later, matching the upstream project.
 */
package org.akanework.gramophone.custom

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BluetoothVolumeDialog {
    fun show(context: Context) {
        fun dp(v: Int) = (v * context.resources.displayMetrics.density + .5f).toInt()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
        }
        fun toggle(label: String, initial: Boolean): CheckBox = CheckBox(context).apply {
            text = label
            isChecked = initial
            root.addView(this)
        }
        fun slider(label: String, initial: Int, enabled: CheckBox): SeekBar {
            val description = TextView(context).apply {
                textSize = 15f
                root.addView(this)
            }
            val bar = SeekBar(context).apply {
                max = 19 // 5%,10%,...,100% of Android volume steps
                progress = initial.coerceIn(5, 100) / 5 - 1
                isEnabled = enabled.isChecked
                root.addView(this)
            }
            fun updateLabel() {
                description.text = "$label: ${(bar.progress + 1) * 5} %"
            }
            updateLabel()
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) = updateLabel()
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
            enabled.setOnCheckedChangeListener { _, checked -> bar.isEnabled = checked }
            return bar
        }

        val btSystem = toggle(
            "Bluetooth: Android-Lautstärke begrenzen",
            SystemVolumeSettings.btEnabled(context)
        )
        val btSteps = slider("Max. Bluetooth-Medienlautstärke (Stufen)",
            SystemVolumeSettings.btPercent(context), btSystem)
        val speaker = toggle(
            "Gerätelautsprecher: Android-Lautstärke begrenzen",
            SystemVolumeSettings.speakerEnabled(context)
        )
        val speakerSteps = slider("Max. Lautsprecher-Medienlautstärke (Stufen)",
            SystemVolumeSettings.speakerPercent(context), speaker)
        root.addView(TextView(context).apply {
            setPadding(0, dp(12), 0, dp(4))
            text = "Zusätzliche Signalbegrenzung (nur Bluetooth)"
            textSize = 16f
        })
        val appGuard = toggle("App-Pegel zusätzlich absenken",
            BluetoothVolumeSettings.isEnabled(context))
        val appGain = slider("Max. App-Signalfaktor",
            BluetoothVolumeSettings.getLimitPercent(context), appGuard)
        val normalize = toggle("Album-Lautstärkeangleichung (ReplayGain-Tags)",
            BluetoothVolumeSettings.normalizeAlbums(context))
        root.addView(TextView(context).apply {
            setPadding(0, dp(12), 0, 0)
            textSize = 12f
            text = "Medienlautstärke wird in Android-Stufen begrenzt. Lauter-Tasten " +
                "bleiben bis zur Grenze nutzbar; externe Bluetooth-Regler können kurz " +
                "übersteuern oder ihre eigene Verstärkung verändern. Kein kalibrierter " +
                "Gehörschutz. Für Kinder möglichst Kopfhörer mit Hardware-Limit nutzen. " +
                "ReplayGain benötigt passende Metadaten."
        })

        MaterialAlertDialogBuilder(context)
            .setTitle("Wiedergabelautstärke")
            .setView(ScrollView(context).apply { addView(root) })
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern") { _, _ ->
                SystemVolumeSettings.save(context,
                    btSystem.isChecked, (btSteps.progress + 1) * 5,
                    speaker.isChecked, (speakerSteps.progress + 1) * 5)
                BluetoothVolumeSettings.save(context,
                    appGuard.isChecked, (appGain.progress + 1) * 5,
                    normalize.isChecked)
            }
            .show()
    }
}
