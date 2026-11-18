// BEISPIELMODUL: Vor dem Einbau Package-Zeile an die App anpassen.
// Dies ist ein eigenständiger PIN- und 5-Tipp-Controller; die UI-Verdrahtung
// mit dem bestehenden Titel und dem vorhandenen Menü erfolgt separat.
package org.akanework.gramophone.custom

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.SystemClock
import android.text.InputFilter
import android.text.InputType
import android.util.Base64
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Versteckter Admin-Einstieg: 5 Tipps binnen 3 Sekunden, PIN beim Erstzugriff setzen. */
class HiddenAdminGate(
    private val activity: Activity,
    private val onAuthorized: () -> Unit,
) {
    private val prefs = activity.getSharedPreferences("gramophone_custom_admin", Context.MODE_PRIVATE)
    private var tapCount = 0
    private var firstTapAt = 0L
    private var failures = 0
    private var lockedUntil = 0L

    fun onTitleTap() {
        val now = SystemClock.elapsedRealtime()
        if (firstTapAt == 0L || now - firstTapAt > 3000L) {
            firstTapAt = now
            tapCount = 0
        }
        tapCount++
        if (tapCount < 5) return
        tapCount = 0
        firstTapAt = 0L
        if (now < lockedUntil) {
            toast("Zu viele Versuche. Bitte später erneut versuchen.")
            return
        }
        if (prefs.contains("pin_hash") && prefs.contains("pin_salt")) {
            askForExistingPin()
        } else {
            askForNewPin()
        }
    }

    private fun askForNewPin() {
        pinDialog("Admin-PIN erstmalig festlegen", "Weiter") { pin ->
            if (!valid(pin)) {
                toast("Bitte 4 bis 8 Ziffern verwenden")
                return@pinDialog
            }
            pinDialog("PIN zur Bestätigung wiederholen", "Speichern") { confirmation ->
                if (pin != confirmation) {
                    toast("PINs stimmen nicht überein")
                    return@pinDialog
                }
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = deriveHash(pin, salt)
                prefs.edit()
                    .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                    .apply()
                toast("Admin-PIN gespeichert")
                onAuthorized()
            }
        }
    }

    private fun askForExistingPin() {
        pinDialog("Admin-PIN eingeben", "Entsperren") { pin ->
            val salt = Base64.decode(prefs.getString("pin_salt", ""), Base64.NO_WRAP)
            val expected = Base64.decode(prefs.getString("pin_hash", ""), Base64.NO_WRAP)
            if (MessageDigest.isEqual(deriveHash(pin, salt), expected)) {
                failures = 0
                onAuthorized()
            } else {
                failures++
                if (failures >= 5) {
                    lockedUntil = SystemClock.elapsedRealtime() + 30_000L
                    failures = 0
                    toast("PIN falsch. Zugang 30 Sekunden gesperrt")
                } else {
                    toast("PIN falsch")
                }
            }
        }
    }

    private fun pinDialog(title: String, confirmText: String, onPin: (String) -> Unit) {
        val editText = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(8))
            isSingleLine = true
            hint = "4–8 Ziffern"
        }
        val margin = (24 * activity.resources.displayMetrics.density).toInt()
        val container = FrameLayout(activity).apply {
            setPadding(margin, 0, margin, 0)
            addView(editText)
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton(confirmText) { _, _ -> onPin(editText.text.toString()) }
            .show()
    }

    private fun valid(pin: String) = pin.length in 4..8 && pin.all(Char::isDigit)

    private fun deriveHash(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, 150_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun toast(message: String) =
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
}
