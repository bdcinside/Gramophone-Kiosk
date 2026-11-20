# Gramophone-Kiosk v5 – Lautstärkeobergrenzen für Bluetooth und Lautsprecher

**Zielgerät:** Xiaomi Mi Max 3, Android 10 / MIUI 12. Branch `beta` von `bdcinside/Gramophone-Kiosk`.

## Was sich ändert

Im bisherigen, PIN-geschützten Administratormenü heißt der Menüpunkt jetzt **🔊 Wiedergabelautstärke …**.

Dort gibt es fünf Bereiche:

1. **Bluetooth: Android-Lautstärke begrenzen** (An/Aus), mit eigenem Grenzwert in 5-%-Schritten.
2. **Gerätelautsprecher: Android-Lautstärke begrenzen** (An/Aus), mit eigenem Grenzwert in 5-%-Schritten.
3. **Zusätzliche Signalbegrenzung nur Bluetooth**, die bisherige App-Pegeldämpfung.
4. **ReplayGain**, weiterhin nur bei vorhandenen Metadaten.
5. Hinweis auf Grenzen der Sicherheit.

Die beiden *neuen* Systemlautstärke-Obergrenzen sind **ab Werk deaktiviert**. Nach dem Einschalten werden sie sofort gespeichert und angewendet, solange Gramophone seinen Wiedergabedienst betreibt. Die Taste "Lauter" ist im Vordergrund der App bis zum Limit nutzbar und wird darüber nicht weitergegeben. Änderungen über externe Bluetooth-Tasten oder die Android-Lautstärkeanzeige werden per Beobachter und zusätzlicher periodischer Prüfung auf den gewählten Wert zurückgestellt. Es kann dabei **kurzzeitige Überschreitungen** geben. Die separate App-Pegeldämpfung und ReplayGain bleiben verfügbar.

Die Prozentangaben stehen für **Anteile der Android-Lautstärkestufen** – nicht für dB(A), nicht für eine kalibrierte Kopfhörerleistung und nicht für die subjektiv empfundene Lautstärke.

**Keine echte geräteweite/physische Sperre:** Der Patch wirkt nicht auf andere Apps, nicht zuverlässig bei beendetem Gramophone-Dienst und nicht auf unabhängige Verstärker/Lautstärkeregler im Bluetooth-Headset. Android stellt einer normalen App hierfür keine garantierte, globale Obergrenze bereit. Für Gehörschutz bei Kleinkindern sind Kopfhörer/Lautsprecher mit fester Hardware-Lautstärkebegrenzung die verlässlichere Maßnahme. Das Ausschalten des Kioskmodus oder die Deinstallation kann die Begrenzung ebenfalls aufheben.

### Was ist bei einem leisen Album?

Die Lautstärketasten können **bis zur konfigurierten Obergrenze** erhöhen, danach nicht. ReplayGain kann Unterschiede zwischen Alben ausgleichen, wenn die Musikdateien die geeigneten ReplayGain-Metadaten haben. Ohne diese Daten wird die Lautheit nicht automatisch gemessen. Die Admin-PIN erlaubt, den Grenzwert später zu ändern.

## Installation mit GitHub Desktop

1. Die ZIP-Datei `Gramophone-Kiosk-Lautstaerke-Limits-v5.zip` herunterladen und entpacken.
2. In GitHub Desktop `bdcinside/Gramophone-Kiosk` und Branch **`beta`** auswählen.
3. **Repository → Show in Explorer** öffnen.
4. Aus dem ZIP den Ordner `app` in das **Hauptverzeichnis** deines Forks kopieren. Nur Dateien ersetzen/ergänzen, **nicht** den gesamten `app`-Ordner löschen.
5. In GitHub Desktop **10 geänderte bzw. neue Projektdateien** prüfen:
   - `app/src/main/AndroidManifest.xml` – Berechtigung `MODIFY_AUDIO_SETTINGS`
   - `app/src/main/java/org/akanework/gramophone/custom/BluetoothVolumeSettings.kt`
   - `app/src/main/java/org/akanework/gramophone/custom/BluetoothVolumeDialog.kt`
   - `app/src/main/java/org/akanework/gramophone/custom/SystemVolumePolicy.kt` (neu)
   - `app/src/main/java/org/akanework/gramophone/custom/SystemVolumeSettings.kt` (neu)
   - `app/src/main/java/org/akanework/gramophone/custom/SystemVolumeLimiter.kt` (neu)
   - `app/src/main/java/org/akanework/gramophone/logic/GramophonePlaybackService.kt`
   - `app/src/main/java/org/akanework/gramophone/logic/utils/PostAmpAudioOutputProvider.kt`
   - `app/src/main/java/org/akanework/gramophone/ui/MainActivity.kt`
   - `app/src/main/java/org/akanework/gramophone/ui/fragments/ViewPagerFragment.kt`
6. Commit-Nachricht `Kiosk v5: Lautstaerkeobergrenzen fuer Bluetooth und Lautsprecher` → **Commit to beta** → **Push origin**.
7. GitHub → Actions → vorhandenen Workflow **Build custom Gramophone APK** → **Run workflow** für Branch `beta`. Dieser muss weiterhin `./gradlew :app:assembleKiosk` ausführen. **Gradle-Datei und Workflow bitte nicht erneut ersetzen.**
8. Artefakt `Gramophone-Kiosk-APK` herunterladen, ZIP entpacken, APK auf Mi Max 3 installieren. Bei Signaturkonflikt nicht einfach die vorhandene Version löschen; zuerst Einstellungen/PIN und Signierkonzept sichern.

## Empfohlene Testreihenfolge

1. Das erste Mal mit **sehr niedriger Lautstärke**, ohne Kopfhörer, im Admin-Menü **Lautsprecher 20 % aktivieren** und Bluetooth-Obergrenze vorerst auslassen. Bei laufendem Album die Taste "Lauter" wiederholt drücken. Ab der Grenze darf es nicht mehr lauter werden.
2. Über einen vorhandenen Android-Lautstärkeregler den Wert erhöhen; der Wiedergabedienst muss ihn wieder nach unten korrigieren. Ein **kurzer Anstieg ist technisch möglich**.
3. Lautsprechergrenze auf 40 % ändern: Nochmals Tasten testen. Im Menü deaktivieren: Dann lässt sich über die alte Grenze hinaus lauter stellen.
4. Mit Bluetooth-Kopfhörer verbinden, **zuerst sehr leise stellen**; Bluetooth-Grenze 20 % aktivieren. Bei laufender Wiedergabe Tasten und am Kopfhörer vorhandene Lautstärkeregelung testen. Geräte mit eigener unabhängiger Verstärkung können trotz App-Limit laut werden.
5. Bluetooth trennen: Lautsprechergrenze muss unabhängig greifen. Bei angeschlossenem Kabel/USB-Ausgang gibt es derzeit keine eigene Grenze.
6. Nach vollständigem Neustart, neuem Album und Wiedergabe im Hintergrund testen. Außerdem PIN, Emoji-Modus, ReplayGain und Album-Klicks prüfen.
7. Gerät nicht als unüberwacht „gehörsicher“ betrachten: Test mit Kind erst nach Prüfung am tatsächlichen Lautsprecher/Kopfhörer.

## Technischer Umfang und Teststatus

- Verwendet `AudioManager.STREAM_MUSIC` mit `getStreamVolume`, `getStreamMaxVolume` und `setStreamVolume`.
- Nutzt bei Wiedergabe die bestehende AudioTrack-Routeninformation (Bluetooth vs. eingebauter Lautsprecher), beobachtet Systemlautstärke und prüft alle 450 ms nach.
- Im Gramophone-Hauptfenster wird `KEYCODE_VOLUME_UP` bis zur Obergrenze appseitig verarbeitet; Bluetooth-Remote-Tasten können weiterhin über Android direkt wirken.
- Beim Aktivieren der System-Obergrenze werden zusätzliche Player-internen Boost-Gewinne über 1.0 deaktiviert, um die Systemgrenze nicht zu unterlaufen.
- Die reine Grenzwertberechnung wurde separat mit Kotlin kompiliert und 13 Assertions geprüft.
- **Der vollständige Android-Build und Tests auf deinem Xiaomi konnten hier nicht ausgeführt werden.** Der erfolgreiche GitHub-Build und die praktischen Lautstärketests stehen noch aus.

### Bitte bei Buildfehlern

In GitHub Actions den fehlerhaften Build und Schritt öffnen und über `... → Download log archive` die ZIP hier hochladen. Bei einem Fehler in einer Kotlin-Datei reichen Fehlermeldungen plus 15 Zeilen Kontext. Keine weiteren Gradle-Korrekturen auf Verdacht.
