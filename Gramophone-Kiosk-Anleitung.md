# Gramophone-Kiosk – vollständige Einbau- und Installationsanleitung

**Ausgangsbasis:** GitHub-Fork `bdcinside/Gramophone-Kiosk`, Branch `beta`, hochgeladenes ZIP `Gramophone-Kiosk-beta.zip` vom 18. November 2026.  
**Ergebnis:** Ein vorgefertigtes **Quellcode-Änderungspaket** für GitHub. Die APK ist noch **nicht** in dieser Umgebung gebaut oder auf einem Android-Gerät getestet worden.

## Was eingebaut ist

| Funktion | Verhalten |
|---|---|
| Verstecktes Menü | Sichtbares Drei-Punkte-Menü auf der Hauptseite wird verborgen; die Suchschaltfläche bleibt. |
| Admin-Zugang | Auf die Überschrift **Gramophone** fünfmal innerhalb von drei Sekunden tippen. |
| Erster Admin-Zugang | Eine 4- bis 8-stellige Zahlen-PIN festlegen und bestätigen. |
| Weitere Admin-Zugriffe | PIN eingeben; nach fünf falschen Versuchen 30 Sekunden Sperre (in derselben App-Sitzung). |
| Emoji-Modus | Standardmäßig **eingeschaltet**. Im PIN-Menü über „🧸 Kleinkindmodus (Emoji)“ ein- oder ausschalten. |
| Text-Emojis | 590 deutsche Begriffe/Wortformen (Tiere, Fahrzeuge, Natur, Essen, Gefühle usw.), ersetzt als **ganze Wörter** durch Standard-Unicode-Emojis. Unbekannte Wörter bleiben stehen. |
| Wo Emojis erscheinen | Musikbibliothek (Album-/Song-/Künstler-/Playlist-/Genre-Karten), Albumüberschriften, Mini-Player, Vollbild-Player und Bearbeitungs-/Titellisten. |
| Was unangetastet bleibt | Echte Musikmetadaten, Dateinamen, Sortierregeln, Suche, Wiedergabelisten und PIN-Menütexte. |
| Album-Klick ohne laufende Musik | Gesamtes Album in Disc-/Titelreihenfolge ab Titel 1 starten und Albumansicht öffnen. |
| Album-Klick während Musik spielt | Nur Albumansicht öffnen; Queue und laufende Musik nicht verändern. |
| Albumdetail | Abspielen, Mischen und Sortieren mit 96 × 96 dp statt 48 × 48 dp; rotes Wiedergabedreieck `#E53935`. |
| APK | GitHub Actions erzeugt eine Debug-APK (eigenständige App-ID, damit die offizielle App nicht überschrieben werden muss). |

**Beispiele:** `Der kleine Elefant` → `Der kleine 🐘`, `Die Feuerwehr kommt` → `Die 🚒 kommt`, `Schlaf, mein Bär` → `😴, mein 🐻`. Emojis sind von der installierten Android-Emoji-Schrift abhängig und können je nach Android-Version anders aussehen.

## A. Änderungspaket in deinen Fork übernehmen (ohne Programmierung)

### Vorbereitung

Du brauchst einen Windows-PC, dein GitHub-Konto und [GitHub Desktop](https://desktop.github.com/) (kostenlos). Der Browser allein entpackt ein ZIP **nicht** automatisch in ein GitHub-Repository; verwende deshalb die nachfolgenden Schritte.

1. **GitHub Desktop installieren und anmelden.** GitHub Desktop starten, dann über `File` → `Options` → `Accounts` (oder beim ersten Start) bei GitHub anmelden.
2. **Fork klonen.** `File` → `Clone repository…` → Register `URL` wählen und `https://github.com/bdcinside/Gramophone-Kiosk.git` eingeben. Lokalen Speicherort auswählen (z. B. `C:\Users\DEINNAME\Documents\GitHub\Gramophone-Kiosk`) und `Clone` klicken.
3. **Wichtig: Branch `beta` wählen.** Oben in GitHub Desktop auf `Current Branch` klicken und **beta** auswählen. Falls `main` aktiv ist, erst wechseln. Nicht mit „Create branch“ eine neue Branch anlegen.
4. **Repository-Ordner öffnen.** GitHub Desktop: `Repository` → `Show in Explorer`. Der Explorer öffnet den Ordner, in dem Dateien wie `gradlew`, `app` und `.github` liegen.
5. **Änderungspaket entpacken.** `Gramophone-Kiosk-Aenderungen.zip` herunterladen, Rechtsklick → `Alle extrahieren…`. Öffne den entpackten Ordner. Dort liegen die Ordner **`.github`** und **`app`** sowie diese Anleitung.
6. **Dateien hineinkopieren.** Markiere die Ordner `.github` und `app` im entpackten Änderungspaket, kopiere sie (`Strg+C`) und füge sie im **Repository-Stammordner** aus Schritt 4 ein (`Strg+V`). Unterordner zusammenführen und bei Rückfragen `Dateien im Ziel ersetzen` auswählen. **Nicht** die gesamten Ordner im Ziel löschen, nur kopieren/zusammenführen!
7. **Änderungen prüfen.** Zurück in GitHub Desktop, linke Spalte `Changes`. Dort sollten u. a. `EmojiDisplay.kt`, `AlbumAdapter.kt`, `ViewPagerFragment.kt` und `build-custom-gramophone.yml` erscheinen. Es dürfen keine riesigen Mengen unzusammenhängender Dateien gelöscht sein.
8. **Committen.** Links unten ins Feld `Summary` eingeben: `Kiosk: PIN-Menü, Albumstart, große Tasten, Emojis`. Button `Commit to beta` klicken.
9. **Zu GitHub hochladen.** Oben auf `Push origin` klicken. Erst nach diesem Schritt sind die geänderten Dateien in deinem Online-Fork. Unter [deinem Fork auf der Branch beta](https://github.com/bdcinside/Gramophone-Kiosk/tree/beta) zur Kontrolle nachsehen.

**Sicherheit:** Das Paket ändert nur deinen Fork und deinen lokalen Ordner, nie das Originalprojekt. Es benötigt keinen geheimen GitHub-Token in irgendeiner Quellcodedatei.

## B. APK mit GitHub Actions bauen

1. Öffne [GitHub Actions deines Forks](https://github.com/bdcinside/Gramophone-Kiosk/actions).
2. Wenn GitHub eine Schaltfläche `I understand my workflows, go ahead and enable them` anzeigt, diese bestätigen.
3. Links im Bereich `Workflows` auf **Build custom Gramophone APK** klicken. Wenn nicht sichtbar, prüfe, ob Schritt A8/A9 wirklich auf GitHub hochgeladen wurde. Manche GitHub-Oberflächen zeigen manuelle Workflows erst in der Standard-Branch an; in diesem Fall den Workflow unter `.github/workflows/build-custom-gramophone.yml` öffnen oder die Branch-Einstellung prüfen.
4. `Run workflow` klicken und die Branch **beta** auswählen. Anschließend den grünen Button `Run workflow` betätigen.
5. Den neuen Workflow-Lauf anklicken und die Schritte verfolgen. Nur **grün/erfolgreich** bedeutet, dass das Build abgeschlossen wurde. Bei rot den ersten roten Schritt öffnen und die Fehlermeldung sichern (gerne hier als Text oder Screenshot senden).
6. Nach erfolgreichem Lauf auf der Seite des Laufs im Abschnitt **Artifacts** das Artefakt **Gramophone-Custom-Debug-APK** anklicken.
7. Die heruntergeladene ZIP-Datei entpacken. Darin liegt die erzeugte Android-Datei mit Endung **`.apk`** (möglicherweise `app-debug.apk`). Dies ist eine signierte **Debug-APK**, keine Google-Play-Release-APK.

Der Build-Workflow nutzt JDK 21, Android SDK, Gradle sowie die erforderlichen Git-Submodule. Die originale `Android CI` aus dem Upstream wird in diesem Fork übersprungen, da sie nicht vorhandene Release-Signaturschlüssel voraussetzt. Entscheidend ist der **eigene Workflow**.

## C. Auf dem Xperia (Android 14) installieren

1. Die APK-Datei auf das Xperia übertragen – beispielsweise über USB oder Download im Browser.
2. Auf dem Smartphone in der Dateien-App auf die APK tippen.
3. Android fragt gegebenenfalls, ob diese Quelle Apps installieren darf. Für die verwendete Dateien-App/den Browser unter `Einstellungen` → `Apps` → `Spezieller App-Zugriff` → `Unbekannte Apps installieren` temporär erlauben (Menünamen können leicht abweichen).
4. `Installieren` bestätigen, Gramophone öffnen und die **Berechtigung für Musik/Audiodateien** erteilen. Abwarten, bis die lokale Musikbibliothek eingelesen ist.
5. Die neue Variante hat eine eigene App-ID (bei Debug-Build voraussichtlich **`org.akanework.gramophone.kiosk.debug`**). Sie kann daher parallel zur offiziellen App installiert sein. Falls zwei gleichnamige Icons erscheinen, in Android über `App-Info` oder im Kiosk-Launcher anhand der Paketkennung die neue Version auswählen.
6. Sobald du geprüft hast, dass die neue Version funktioniert, kannst du sie als Musik-App in deinem Kiosk-Launcher wählen. **Erst danach** Kiosk-Restriktionen verschärfen.

## D. Funktionsprüfung (bitte in dieser Reihenfolge)

1. **Erststart:** Bibliothek erscheint, der Emoji-Modus ist an. Der Hauptbildschirm zeigt das Suchsymbol, aber kein sichtbares Drei-Punkte-Menü.
2. **PIN festlegen:** Fünfmal zügig auf `Gramophone` tippen. Eine PIN mit vier bis acht Ziffern eingeben und bestätigen. Danach erscheint das Administratormenü.
3. **PIN erneut:** Menü schließen, wieder fünfmal tippen. Nun wird der PIN abgefragt. Falschen PIN einmal prüfen: kein Zugriff.
4. **Emoji-Schalter:** Nach PIN-Eingabe den Menüpunkt `🧸 Kleinkindmodus (Emoji)` umschalten. Die App wird zur Aktualisierung neu aufgebaut; Musik sollte dabei weiterhin verfügbar bleiben. Nochmal öffnen und zurückschalten.
5. **Albumstart:** Musikwiedergabe vollständig anhalten bzw. inaktiv lassen. Ein Albumcover antippen. Albumansicht öffnet sich, Titel 1 startet. Bei einem Album mit mehreren Tracks prüfen, ob Track 2 automatisch folgt.
6. **Album wechseln bei Wiedergabe:** Während ein Titel läuft, ein anderes Albumcover antippen. Es öffnet sich nur die andere Albumansicht; der bereits laufende Titel spielt ununterbrochen weiter.
7. **Große Bedienelemente:** In einer Albumansicht die drei großen Schaltflächen prüfen: Wiedergabe (rotes Dreieck), Mischen, Sortieren. Sie sollten etwa doppelt so breit und hoch wie zuvor und zuverlässig zu treffen sein.
8. **Kiosk-Test:** Kiosk-Launcher starten und die neue Gramophone-Variante auswählen. PIN-Zugang und Musikwiedergabe anschließend erneut prüfen.

## E. Wenn etwas nicht klappt

- **Der GitHub-Desktop-Bereich `Changes` ist leer:** Prüfe, ob du ins richtige lokale Repository kopiert hast und in GitHub Desktop tatsächlich `beta` aktiv ist.
- **In GitHub fehlen die Änderungen:** Nach dem Commit noch auf `Push origin` klicken.
- **GitHub Actions zeigt keinen Workflow:** Überprüfe `.github/workflows/build-custom-gramophone.yml` auf Branch `beta` und die Freigabe von Workflows im Repository. GitHub kann für die manuelle Ausführung eine Workflow-Datei auf der Standardbranch voraussetzen.
- **Build rot:** GitHub Actions → Workflow-Lauf → erster roter Schritt → komplette Fehlermeldung kopieren. Das Paket wurde **nicht auf einem vollständigen Android-SDK/Gradle-Setup kompiliert**, daher sind Buildanpassungen möglich.
- **Nach dem Öffnen keine Musik sichtbar:** Android-Musikberechtigung erlauben und prüfen, ob die Dateien lokal auf dem Gerät liegen.
- **5 Tipps reagieren nicht:** Direkt auf den Schriftzug bzw. dessen Titelbereich oben tippen, nicht auf die Suchlupe. Falls das auf deinem Gerät nicht zuverlässig funktioniert, muss die Touch-Fläche im Code angepasst werden.
- **PIN vergessen:** Es gibt noch keinen sicheren PIN-Wiederherstellungsdialog. Durch Löschen der **App-Daten der neuen Kiosk-App** wird auch die PIN zurückgesetzt; dabei gehen aber App-Einstellungen und gespeicherte App-Daten verloren. Musikdateien im normalen Gerätespeicher bleiben davon unberührt.
- **Ein Wort bleibt Text:** Es fehlt im Wörterbuch, ist anders geschrieben oder bezeichnet etwas Mehrdeutiges. Das ist normal. Die Einträge stehen im Quellcode in `app/src/main/java/org/akanework/gramophone/custom/EmojiDisplay.kt` und können später erweitert werden.
- **Vollständige Kindersicherung:** Die PIN schützt das App-interne Administratormenü, ersetzt aber **keine** Android-Gerätesperre und keinen korrekt eingerichteten Device-Owner-Kiosk.

## F. Dateien, in denen wir etwas geändert haben

- `.github/workflows/build-custom-gramophone.yml` – manueller APK-Build mit eigener Paketkennung
- `.github/workflows/android.yml` – ursprünglichen Release-Build im Fork überspringen
- `app/src/main/java/org/akanework/gramophone/custom/EmojiDisplay.kt` – Wörterbuch und Umschalter
- `app/src/main/java/org/akanework/gramophone/custom/HiddenAdminGate.kt` – PIN-Dialog, 5-Tipp-Erkennung (bereits im Fork-ZIP enthalten, vorsorglich mitgeliefert)
- `app/src/main/java/org/akanework/gramophone/custom/AlbumTapPolicy.kt` – Album-Klickregel (bereits im Fork-ZIP enthalten, vorsorglich mitgeliefert)
- `app/src/main/java/org/akanework/gramophone/ui/fragments/ViewPagerFragment.kt` – verstecktes Menü und Emoji-Schalter
- `app/src/main/java/org/akanework/gramophone/ui/adapters/AlbumAdapter.kt` – Albumstart abhängig von laufender Wiedergabe
- `app/src/main/java/org/akanework/gramophone/ui/adapters/BaseDecorAdapter.kt` – größere Bedienelemente nur in Albumansichten
- `app/src/main/res/layout/general_decor_album.xml` – 96-dp-Tasten
- `app/src/main/res/values/colors.xml` – Rot für Wiedergabedreieck
- `app/src/main/res/menu/home_menu.xml` – Suchsymbol sichtbar halten
- `app/src/main/java/org/akanework/gramophone/ui/adapters/BaseAdapter.kt` – sichtbare Textumwandlung für Bibliothekskarten
- `app/src/main/java/org/akanework/gramophone/ui/fragments/GeneralSubFragment.kt` – Emoji-Albumüberschrift
- `app/src/main/java/org/akanework/gramophone/ui/components/PreviewBottomSheet.kt` – Emoji-Mini-Player
- `app/src/main/java/org/akanework/gramophone/ui/components/FullBottomSheet.kt` – Emoji-Vollbildplayer
- `app/src/main/java/org/akanework/gramophone/ui/components/EditSongAdapter.kt` – Emoji-Liedliste beim Bearbeiten

---

**Stand der Verifikation:** ZIP-Inhalt mit dem Nutzer-Quellcode verglichen; XML und GitHub-Actions-YAML geparst; die reine Kotlin-Wörterbuchlogik in einem isolierten Test kompiliert und mit Beispielen geprüft. **Keine vollständige Android-APK gebaut und noch kein Gerätetest durchgeführt.**
