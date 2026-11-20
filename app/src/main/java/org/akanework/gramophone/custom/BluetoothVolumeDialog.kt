/*
 * Gramophone Kiosk: PIN-protected Bluetooth audio settings dialog.
 * GPL-3.0-or-later, matching the upstream Gramophone project.
 */
package org.akanework.gramophone.custom

import android.content.Context
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BluetoothVolumeDialog {
    fun show(context: Context) {
        fun dp(v: Int) = (v * context.resources.displayMetrics.density + .5f).toInt()
        val enabled = CheckBox(context).apply {
            text = "Bluetooth-Lautstärkebegrenzung aktivieren"
            isChecked = BluetoothVolumeSettings.isEnabled(context)
        }
        val chosenPercent = BluetoothVolumeSettings.getLimitPercent(context)
        val value = TextView(context).apply {
            text = "Maximaler App-Pegel: $chosenPercent %"
            textSize = 16f
        }
        val seek = SeekBar(context).apply {
            max = 19  // 5%, 10%, ... 100%
            progress = chosenPercent / 5 - 1
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                value.text = "Maximaler App-Pegel: ${(progress + 1) * 5} %"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        seek.isEnabled = enabled.isChecked
        enabled.setOnCheckedChangeListener { _, isChecked -> seek.isEnabled = isChecked }

        val normalize = CheckBox(context).apply {
            text = "Leise Alben angleichen (ReplayGain-Metadaten)"
            isChecked = BluetoothVolumeSettings.normalizeAlbums(context)
        }
        val notice = TextView(context).apply {
            text = "Gilt nur für die Musikwiedergabe dieser App über Bluetooth. " +
                "Lautstärketasten bleiben nutzbar, erhöhen den App-Pegel aber nicht über " +
                "die eingestellte Grenze. ReplayGain wirkt nur, wenn passende Lautheitsdaten " +
                "in den Musikdateien gespeichert sind. Keine Garantie für eine gehörsichere " +
                "Lautstärke: Kopfhörer regeln ihre tatsächliche Lautstärke auch selbst."
            textSize = 13f
        }
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(enabled)
            addView(value)
            addView(seek)
            addView(normalize)
            addView(notice)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle("Bluetooth-Lautstärke")
            .setView(layout)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern") { _, _ ->
                BluetoothVolumeSettings.save(
                    context, enabled.isChecked, (seek.progress + 1) * 5, normalize.isChecked
                )
            }
            .show()
    }
}
