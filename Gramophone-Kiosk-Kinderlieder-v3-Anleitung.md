# Gramophone-Kiosk – eigenes Kinderlieder-Wörterbuch v3

**Basis:** der funktionierende Kiosk-Fork auf `beta`. Dieses Paket ersetzt ausschließlich die Emoji-Erkennung. PIN, Albumsteuerung, Größen, Farben, Gradle und GitHub Actions bleiben unverändert.

## 1. Ergebnis aus deinem Export

- **770** Dateititel aus `\\192.168.2.100\Daten\audio\Kinderlieder` ausgewertet.
- Mit v2 enthalten **510 von 770** Titeln mindestens eine sichtbare Emoji-Ersetzung.
- Mit v3 enthalten **675 von 770** Titeln mindestens eine sichtbare Emoji-Ersetzung (**+165 Titel**).
- **165** zuvor vollständig unveränderte Titel erhalten nun erstmals eine Emoji-Ersetzung.
- **95** Titel bleiben komplett unverändert, darunter viele Fremdsprachen, Eigennamen oder beschädigte/abgeschnittene Dateinamen.
- Das ergänzte Lexikon umfasst nun **8.459 generierte Wortformen** (v2: 7.762) plus das separate, bisherige Standardwörterbuch. Das sind Formen, keine 8.459 unterschiedlichen Bedeutungen.

Wichtig: Gemessen wurde an den exportierten **Dateinamen**, nicht an ID3-Tags. Wenn Gramophone andere Titel anzeigt als der Dateiname, kann die tatsächliche Quote abweichen. In einigen Namen sind Sonderzeichen bereits beschädigt (`�`) oder Titel abgeschnitten; das lässt sich durch ein Wörterbuch nicht zuverlässig beheben.

## 2. Beispielergebnisse

| Ausgangstitel | Neue Anzeige (ohne Tracknummer) |
|---|---|
| Die Biene Maja | Die 🐝 |
| Der kleine Drache Kokosnuss | Der 🤏 🐉 🥥 |
| Giraffenaffensong | 🦒🐒🎵 |
| Traumzauberbaum | 🌳✨ |
| Die Schneckenpost | Die 🐌📮 |
| Der Pfannkuchenschreck | 🥞😱 |
| Der Kuckuck und der Esel | Der 🐦 und der 🫏 |
| Hänsel und Gretel | 👦 👧 |
| Pippi Langstrumpf | 👧 🧦 |
| Jahresuhr | 🕰️📅 |
| Sonnenlied | ☀️🎵 |

Begriffe wie `Mausoleum` und `Eisen` bleiben unverändert; wir vermeiden absichtliche Wortfetzen-Ersetzung. Unbekannte Namen und Texte bleiben lesbar.

## 3. Installieren (GitHub Desktop, kein Programmieren)

1. Das ZIP `Gramophone-Kiosk-Emoji-Kinderlieder-v3.zip` herunterladen und in einen beliebigen Ordner **entpacken**.
2. **GitHub Desktop** öffnen → `Gramophone-Kiosk` auswählen → Branch **`beta`** prüfen. Bei Bedarf zuerst **Fetch origin / Pull origin**.
3. Menü **Repository → Show in Explorer** öffnen.
4. **Nur den Ordner `app` aus dem ZIP** in deinen lokalen Repository-Hauptordner kopieren, Unterordner **zusammenführen** und nach Aufforderung `EmojiDisplay.kt` und `EmojiLexicon.kt` **ersetzen**. Die beiden Dateien müssen schließlich liegen unter:
   - `app/src/main/java/org/akanework/gramophone/custom/EmojiDisplay.kt`
   - `app/src/main/java/org/akanework/gramophone/custom/EmojiLexicon.kt`
5. In GitHub Desktop unter **Changes** sollten nur die beiden Kotlin-Dateien erscheinen; wenn v2 bereits eingespielt war, ist gegebenenfalls **nur `EmojiLexicon.kt` geändert**, denn `EmojiDisplay.kt` ist gegenüber v2 identisch.
6. **Commit to beta** mit dem Text `Emoji: Kinderlieder-Wortschatz v3`, danach **Push origin**.
7. GitHub Actions öffnen: https://github.com/bdcinside/Gramophone-Kiosk/actions → **Build custom Gramophone APK** → **Run workflow** (Branch `beta`).
8. Prüfen, dass der Schritt `:app:assembleKiosk` verwendet, das Artefakt `Gramophone-Kiosk-APK` herunterladen und die APK auf dem Testgerät installieren.
9. Nur bei erfolgreicher Installation die Funktionen testen. Bei Android-Signaturkonflikt **nicht sofort deinstallieren**, weil PIN und Einstellungen sonst verlorengehen können. Der GitHub-Build könnte weiterhin einen neu erzeugten Debug-Signierschlüssel nutzen; langfristig ist ein fester Signierschlüssel sinnvoll.

**NICHT ändern:** `.github/workflows/build-custom-gramophone.yml`, `app/build.gradle.kts`, `HiddenAdminGate.kt` sowie die Album- und Player-Dateien.

## 4. Prüfen und nachbearbeiten

- `Gramophone-Kinderlieder-Titelvergleich-v3.csv` enthält alle 770 Originaldateinamen und die simulierten Anzeigen von v2 und v3. In Excel in der Regel automatisch als UTF-8 mit Semikolontrennung lesbar.
- `Gramophone-Kinderlieder-Noch-ohne-Emoji.txt` listet die verbliebenen 95 unveränderten Titel.
- `Gramophone-Kinderlieder-Neue-Wortregeln.csv` gibt alle speziell ergänzten Wort-/Phrasenregeln zur Kontrolle aus.
- Wenn ein Emoji nicht passt, nenne mir Titel und gewünschtes Emoji. Dann ändern wir gezielt die Übersetzung. Bei mehrdeutigen Begriffen (etwa `Star` als Vogel oder englisch für Stern) braucht es ggf. einen Kontextvorbehalt.

## 5. Technischer Stand

- Die beiden Kotlin-Quelldateien wurden außerhalb von Android mit dem Kotlin-Compiler und minimalen Context-Stubs kompiliert.
- **25/25 frühere Regressionstests** und **37/37 zusätzliche Kinderlieder-Tests** bestanden.
- Alle **770 Dateititel** wurden mit demselben Kotlin-Konverter in v2 und v3 verglichen.
- **Kein vollständiger Android-/GitHub-Actions-Build in dieser Umgebung**: Die finale Installation muss auf deinem Gerät erprobt werden.
- Kein Netzwerk, keine KI-Abhängigkeit, kein Schreibzugriff auf die Musikdateien. Es wird weiterhin nur der sichtbare Text ersetzt.
