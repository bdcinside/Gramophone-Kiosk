# Gramophone-Kiosk – Emoji-Wortschatz v2

**Ausgangsbasis:** Die bisherige funktionierende Gramophone-Kiosk-Variante mit PIN-Zugang, Album-Logik und erfolgreichem `assembleKiosk`-Build. Die neue Erweiterung betrifft ausschließlich die Emoji-Erkennung. Die Gradle- und GitHub-Actions-Dateien werden **nicht** verändert.

## 1. Was ist neu?

- Das bisherige Emoji-Wörterbuch bleibt erhalten und hat Vorrang bei bekannten Begriffen.
- Ein neues Lexikon enthält rund **7.700 zusätzliche erkannte Schreibweisen und Wortformen** (aus Synonymen, ausgewählten Flexionsregeln und Formen; **nicht** 7.700 unterschiedliche Bedeutungen). Es gibt über 500 Themen-/Synonymgruppen.
- Pluralformen, Beugungen von Adjektiven (z. B. „roten“, „bunten“) und viele konjugierte Verben und Partizipien (z. B. „tanzt“, „hüpfenden“) werden erkannt.
- Deutsche Zusammensetzungen werden zerlegt, **wenn alle Bestandteile sicher im Wörterbuch vorkommen** (maximal vier Teile, einschließlich typischer Fugenlaute wie `s`, `n`, `en`, `es`, `e`, `er`). Vorhandene komplette Begriffe haben Vorrang vor Zerlegung.
- Einige häufige Formulierungen wie „Gute Nacht“ und „Happy Birthday“ haben eigene Emoji-Regeln.
- Unbekannte Wörter und Eigennamen bleiben unverändert. „Mausoleum“ darf nicht versehentlich durch „Maus“ verfälscht werden.
- Titel, Tags, Suchschlüssel, Sortierung und Dateien werden weiterhin **nicht** verändert. Ausschließlich die sichtbare Anzeige wird ersetzt.
- Die Erkennung erfolgt **offline**; kein Netzwerkzugriff, keine KI, keine neue Bibliothek.
- Ein kleiner, auf 512 Einträge begrenzter Cache reduziert wiederholte Berechnungen beim Scrollen.

## 2. Installation in deinen Fork

Repository: https://github.com/bdcinside/Gramophone-Kiosk/tree/beta

1. **GitHub Desktop** öffnen und dein lokales Repository `Gramophone-Kiosk` auswählen.
2. Prüfen, dass unter `Current Branch` wirklich **`beta`** steht. Falls du noch nicht synchronisiert hast, zuerst **Fetch origin / Pull origin** durchführen.
3. **Repository → Show in Explorer** anklicken. Es öffnet sich der lokale Projektordner (mit den Ordnern `app` und `.github`).
4. ZIP `Gramophone-Kiosk-Emoji-Wortschatz-v2.zip` entpacken.
5. **Nur den enthaltenen Ordner `app`** in den Projektordner hineinkopieren und die bestehenden Unterordner *zusammenführen*. **Keine kompletten vorhandenen Ordner löschen.**
6. Bei der einen gleichnamigen Datei `EmojiDisplay.kt` die **Datei ersetzen**. Die zweite Datei `EmojiLexicon.kt` wird neu hinzugefügt.
7. GitHub Desktop öffnen und kontrollieren, dass unter `Changes` **genau zwei Kotlin-Dateien** erscheinen:
   - `app/src/main/java/org/akanework/gramophone/custom/EmojiDisplay.kt` (geändert)
   - `app/src/main/java/org/akanework/gramophone/custom/EmojiLexicon.kt` (neu)
8. Unten bei `Summary` eingeben: `Emoji: großer Wortschatz, Wortformen und Komposita`.
9. Auf **Commit to beta**, danach oben auf **Push origin** klicken.
10. Unter https://github.com/bdcinside/Gramophone-Kiosk/actions deinen Workflow **Build custom Gramophone APK** auswählen, `Run workflow` auf Branch **beta** starten.
11. Der Compile-Schritt soll `:app:assembleKiosk` verwenden. Das Artefakt sollte **`Gramophone-Kiosk-APK`** heißen.
12. APK aus dem Artefakt-ZIP entpacken und auf dem Testgerät installieren. Zunächst die bisher funktionierende App/deren Daten **nicht** löschen.

### Wichtige Einschränkung beim Aktualisieren

Der aktuelle Test-Workflow verwendet möglicherweise einen **Debug-Signierschlüssel** auch für die Kiosk-Variante. Wenn bei jeder GitHub-Ausführung ein anderer Schlüssel erzeugt wird, verweigert Android die Installation über die vorhandene APK trotz gleicher Paketkennung. **Bei einer Signaturfehlermeldung nicht sofort die App deinstallieren:** Sonst gehen unter Umständen PIN und Einstellungen verloren. Für langfristige Updates sollte ein fester, sicher verwahrter Signierschlüssel in GitHub Actions eingerichtet werden.

## 3. Was du auf dem Gerät testen solltest

1. Ein einzelner bekannter Begriff: `Katze` → 🐱.
2. Neue Synonyme: `Meerschweinchen`, `Feuerwehrwagen`, `Waldfee`.
3. Beugungen: `die bunten Blumen`, `die hüpfenden Hasen`, `ein rotes Auto`.
4. Zusammensetzungen: `Katzenlied` → 🐱 🎵, `Blaubeerkuchen` → 🫐 🍰, `Waldspaziergang` → 🌲 🚶, `Kinderlieder` → 🧒 🎵.
5. Präzision: `Mausoleum` bleibt „Mausoleum“, `Maus` wird 🐭. Das Wort `Eisen` bleibt ebenfalls unverändert und darf **nicht** als „Eis“ gelesen werden.
6. PIN-Zugang: Fünfmal auf „Gramophone“ tippen und den Emoji-Modus ausschalten. Dann müssen wieder **alle Originaltitel** erscheinen. Erneut einschalten: Emojis erscheinen wieder.
7. Die zuvor funktionierenden Album-Cover-Klicks, roten Schaltflächen und großen Bedienelemente kontrollieren.

## 4. Eigenes Vokabular später erweitern

Öffne `app/src/main/java/org/akanework/gramophone/custom/EmojiLexicon.kt`. Am Ende stehen fünf gut lesbare Wortlisten:

- `NOUNS`: Nomen, Synonyme, häufige Pluralformen; auch Basis für zusammengesetzte Wörter.
- `ADJECTIVES`: Eigenschaftswörter; passende Endungen wie `-e`, `-en`, `-er`, `-es`, `-em` werden erzeugt.
- `VERBS`: Infinitive; viele reguläre Formen werden erzeugt.
- `IRREGULAR`: Unregelmäßige Verbformen und Ausnahmen.
- `PHRASES`: Feste Wortgruppen.

**Format: `Emoji|wort synonym variante`.** Zum Beispiel:

    🐱|kater mieze stubentiger
    🐸|frosch frösche froschkönig

Einfach eine neue Zeile innerhalb der richtigen Liste einfügen. Keine Anführungszeichen, Kommata oder Kotlin-Syntax in den Tabellen ergänzen. Danach committen und neu bauen. Schon vorhandene Wörter im alten `EmojiDisplay.kt` bleiben vorrangig; für eine Änderung einer **bestehenden** Übersetzung muss diese ursprüngliche Liste editiert werden.

### Grenzen

Wörter können mehrdeutig sein („Bank“ als Sitzbank oder Geldinstitut, „Schloss“ als Gebäude oder Schließmechanismus). Das Lexikon benutzt absichtlich **kein** Sprachmodell und versucht keine riskante Interpretation aus dem Satzkontext. Zusammensetzungen werden nur ersetzt, wenn sie vollständig und plausibel in bekannte Teile auflösbar sind. Es kann deshalb weiterhin Wörter geben, für die Text stehen bleibt. Das ist bei einer Kleinkind-App besser als irreführende Bilder.

## 5. Technische Überprüfung

Die beiden Kotlin-Dateien wurden gemeinsam mit minimalen Android-Context-Teststubs per Kotlin-Compiler erfolgreich kompiliert. 25 lokale Beispiel- und Negativtests waren erfolgreich. **Ein vollständiger Android-Gradle-Build und Gerätetest konnten hier nicht ausgeführt werden**; dafür ist dein funktionierender GitHub-Actions-Workflow vorgesehen. Die Zahl von rund 7.700 bezieht sich auf die neu generierte Lexikon-Lookup-Tabelle einschließlich Varianten, nicht auf verschiedene Grundbegriffe.
