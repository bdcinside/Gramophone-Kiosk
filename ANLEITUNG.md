# Gramophone-Kiosk: automatische Versionsnummern

## Ergebnis
Beim Build in GitHub Actions wird die GitHub-Laufnummer `GITHUB_RUN_NUMBER` automatisch gelesen.
Beispiel bei Lauf 42:
- Android `versionCode`: 10042 (muss bei Updates steigen)
- Android `versionName`: `1.1.2-kiosk.42` (sichtbare Versionsbezeichnung)

Die Werte steigen mit jedem neuen Workflow-Lauf, auch wenn ein Lauf scheitert. Außerhalb von GitHub Actions bleibt das Originalverhalten von Gramophone bestehen.

## Installation auf GitHub
1. In GitHub Desktop deinen Fork `Gramophone-Kiosk` auf Branch `beta` öffnen.
2. **Repository > Show in Explorer** wählen.
3. Den `app`-Ordner aus diesem Paket in das Repository kopieren und **app/build.gradle.kts** ersetzen. Nur diese Datei wird geändert; den Workflow nicht anpassen.
4. In GitHub Desktop **Commit to beta**, dann **Push origin** klicken.
5. GitHub Actions: den bestehenden Workflow **Build custom Gramophone APK** starten.
6. In den APK-Details erscheint nun eine Versionsnummer wie `1.1.2-kiosk.42`.

## Wichtig: Updates ohne Deinstallation
Ein steigender versionCode reicht NICHT aus. Android verlangt außerdem denselben Paketnamen und dieselbe Signatur wie bei der bereits installierten APK.
Der bisherige GitHub-Workflow benutzt `signingConfigs.getByName("debug")`. GitHub-Runner sind kurzlebig: ohne dauerhaft bereitgestellten Signierschlüssel kann jeder Lauf einen neuen Debug-Schlüssel erzeugen. Dann lässt sich die APK nicht über die vorige Installation aktualisieren.

Für echte automatische Updates sollte als nächstes ein *stabiler, privat aufbewahrter Signierschlüssel* über GitHub Actions Secrets eingebunden werden. Bestehende Installationen mit anderem Schlüssel müssen einmalig ersetzt werden (vorher Einstellungen sichern, da eine Deinstallation App-Daten entfernt). Private Keystore-Dateien und Kennwörter niemals ins Repository committen.

## Hinweis
Dieses Update basiert auf deiner zuletzt bereitgestellten vollständigen `app/build.gradle.kts` mit `create("kiosk")`. Es wurde auf Textstruktur geprüft, nicht vollständig mit Gradle kompiliert.
