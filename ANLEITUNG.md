# Gramophone-Kiosk v4 – Bluetooth-Lautstärkebegrenzung und ReplayGain

**Für:** `bdcinside/Gramophone-Kiosk`, Branch `beta`, auf Basis des zuletzt geteilten Quellcodes und der bisherigen Kiosk-Anpassungen.

## Was eingebaut wurde

Im bereits vorhandenen PIN-geschützten Administratormenü erscheint ein neuer Punkt **„🔊 Bluetooth-Lautstärke …“**. Nach fünf Berührungen auf „Gramophone“ und PIN-Eingabe findest du dort:

- **Bluetooth-Lautstärkebegrenzung aktivieren/deaktivieren.** Ab Werk ist sie deaktiviert, bis du sie ausdrücklich einschaltest.
- **„Maximaler App-Pegel“** als Schieberegler von 5 bis 100 Prozent in 5-Prozent-Schritten. Vorgabewert: 40 Prozent. Die Einstellung wird gespeichert.
- **„Leise Alben angleichen (ReplayGain-Metadaten)“**, separat ein-/ausschaltbar. Ab Werk deaktiviert.

Der Schutz sitzt im Audio-Ausgabepfad der App (`PostAmpAudioOutputProvider`), nicht nur in einer Anzeige. Er wird anhand des vom Player tatsächlich genutzten Bluetooth-Audiogeräts aktiviert und beim Wechsel zurück zu Lautsprecher/Kabel abgeschaltet. Solange die Route noch unbekannt ist, begrenzt der Player vorsichtshalber ebenfalls. Änderungen werden während der Wiedergabe übernommen. Das vorhandene Wiedergabe-, Emoji-, Album- und Admin-Verhalten bleibt unverändert.

### Was bedeuten die Prozent?

40 % bedeutet **höchstens 0,4 als app-interner Audio-Signalverstärkungsfaktor** im betreffenden Audio-Ausgabepfad, *nicht* 40 % der Hardwarelautstärke, des wahrgenommenen Schalls oder eines Schalldruckgrenzwerts. Die Lautstärketasten regeln weiterhin Android/den Bluetooth-Kopfhörer. Ein Gerät mit eigener Verstärkung oder absoluten Bluetooth-Lautstärkewerten kann bei hohem Hardwarepegel trotzdem zu laut sein. Diese Funktion ist **kein zertifizierter Kinder-Gehörschutz**. Für Kinder bitte die tatsächliche Lautstärke am verwendeten Kopfhörer prüfen und nach Möglichkeit ein Gerät mit echter Hardwarebegrenzung wählen.

## Was passiert bei leisen Alben?

- **Lauter machen:** Die System-/Kopfhörerlautstärke lässt sich weiter erhöhen. Der maximale, von Gramophone selbst ausgegebene Audiofaktor bleibt auf den eingestellten Wert begrenzt. Falls das nicht ausreicht, muss ein Erwachsener die Grenze über die PIN ändern.
- **ReplayGain einschalten:** Wenn die Musikdateien passende `REPLAYGAIN_ALBUM_GAIN`- bzw. unterstützte ReplayGain-Metadaten enthalten, gleicht Gramophone die Lautheit **zwischen Alben** an, ohne die innere Dynamik der einzelnen Alben unnötig zu verändern. Der bestehende ReplayGain-Prozessor von Gramophone wird verwendet; es gibt keine zweite eigenständige Klangverarbeitung.
- **Kein ReplayGain-Tag:** Es findet **keine automatische Messung oder Anpassung** statt. Für wirklich flächendeckende Normalisierung müsste man einmalig die Lautheit der Kinderlieddateien analysieren und ReplayGain-Tags schreiben (die Audiodaten selbst können unverändert bleiben) oder eine spätere Echtzeit-Normalisierung implementieren.

## Installation mit GitHub Desktop

1. Das ZIP `Gramophone-Kiosk-Bluetooth-Lautstaerke-v4.zip` herunterladen und entpacken.
2. In **GitHub Desktop** das Repository `Gramophone-Kiosk` und den Branch `beta` auswählen.
3. **Repository → Show in Explorer** öffnen.
4. Den enthaltenen Ordner **`app`** in das Repository-Hauptverzeichnis kopieren. Die Unterordner **zusammenführen** und die vier betroffenen Kotlin-Dateien **ersetzen**, die beiden neuen Kotlin-Dateien hinzufügen. **Nicht** den gesamten bestehenden `app`-Ordner löschen.
5. In GitHub Desktop die geänderten Dateien ansehen. Es sollten **genau 6 Kotlin-Dateien** unter `app/src/main/java/...` sein:
   - **Neu:** `custom/BluetoothVolumeSettings.kt`
   - **Neu:** `custom/BluetoothVolumeDialog.kt`
   - **Geändert:** `ui/fragments/ViewPagerFragment.kt`
   - **Geändert:** `logic/GramophonePlaybackService.kt`
   - **Geändert:** `logic/utils/PostAmpAudioOutputProvider.kt`
   - **Geändert:** `logic/utils/exoplayer/GramophoneRenderFactory.kt`
6. Commit-Nachricht z. B. `Bluetooth-Lautstaerkebegrenzung und ReplayGain-Menue` → **Commit to beta** → **Push origin**.
7. GitHub → **Actions** → `Build custom Gramophone APK` → **Run workflow**, Branch `beta`. Der bestehende Workflow muss weiterhin `:app:assembleKiosk` verwenden. **Keine** Anpassung an `build.gradle.kts` nötig.
8. Nach grünem Build `Gramophone-Kiosk-APK` herunterladen und auf dem Testgerät installieren. Wegen der GitHub-Debug-Testsignierung kann ein Update über die installierte App eine Signaturfehlermeldung geben. Vor einer Deinstallation Einstellungen und PIN berücksichtigen.

## Testplan nach dem Build

1. Prüfen, ob Gramophone startet, normale Alben abspielt und Emoji-Modus/PIN noch funktionieren.
2. Bei **nicht verbundenem Bluetooth** die Einstellung auf „Ein, 40 %“ setzen. Musik über den Gerätelautsprecher abspielen: keine zusätzliche Absenkung nach Erkennung der Lautsprecherroute.
3. Bluetooth-Kopfhörer verbinden. Musik vorsichtig zunächst mit **niedriger Hardwarelautstärke** starten: mit aktivem Limit sollte das Audio-Signal deutlich leiser sein als mit ausgeschaltetem Limit.
4. Limit z. B. von 40 % auf 20 % ändern: es wird während der Wiedergabe sofort leiser. 20 % → 40 % sollte wieder lauter werden, nie abrupt mit hoher Hardwarelautstärke testen.
5. Bluetooth trennen und erneut verbinden. Prüfen, ob sich die Grenze jeweils nur am Bluetooth-Ausgang bemerkbar macht.
6. ReplayGain einschalten und zwei Alben vergleichen, **von denen bekannt ist, dass sie ReplayGain-Tags besitzen**. Ohne Tags ist keine Änderung zu erwarten.
7. Nach vollständigem App-Neustart prüfen, ob alle Einstellungen gespeichert sind.

## Technische Validierung

- Die neuen Kotlin-Dateien wurden lokal mit einem Kotlin-Compiler und Android-Test-Stubs auf Syntax geprüft.
- Die Pegel-/Routenlogik hat **12 gezielte Plausibilitätstests** bestanden (inklusive Wechsel Bluetooth → Lautsprecher, Standardwert, Grenzwerte und Android-Versionen).
- **Ein vollständiger Android-/Media3-Gradle-Build sowie ein Test an einem echten Kopfhörer konnten hier nicht ausgeführt werden.** Erst ein erfolgreicher GitHub-Build bestätigt die Integration; anschließend muss die tatsächliche Pegelbegrenzung am Gerät geprüft werden.

## Hinweis zu unterschiedlichen Quellcode-Versionen

Das Paket enthält gezielt nur sechs Dateien und basiert auf der zuvor hochgeladenen `Gramophone-Kiosk-beta.zip` plus den von uns vorbereiteten bisherigen Anpassungen. Wenn du seitdem **weitere Änderungen gerade an diesen sechs Dateien** vorgenommen hast, die nicht aus unseren Paketen stammen, bitte vor dem Ersetzen die Änderungen in GitHub Desktop vergleichen, damit nichts überschrieben wird.
