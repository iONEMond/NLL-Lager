# NLL Lager – Android 0.1.0

Offline-App für den Honeywell EDA52-0 mit Scanner im Tastaturmodus. Native Android-Oberfläche mit großen Schaltflächen, lokale SQLite-Datenbank, keine Serververbindung und keine Internetberechtigung.

## Funktionsumfang

- Personen mit bestehendem Scancode anmelden; Personen manuell anlegen oder aus Excel importieren.
- Material und Kältemittel per bestehendem Code erkennen; Stammdaten manuell anlegen oder aus Excel importieren.
- Materialnummer, Beschreibung, Scancode, Art, Einheit, Von Lager, Nach Lager, positive Dezimalmenge, Datum/Zeit mit Zeitzonenoffset sowie Person speichern. Buchungsdaten enthalten unveränderliche Kopien der Stammdaten.
- Große Eingabefelder, Mengen bis drei Nachkommastellen, Buchungsbestätigung gegen versehentliche Eingaben.
- Alle offenen Bewegungen als CSV über den Android-Dateidialog speichern. **Erst nach erfolgreichem Schreiben und Schließen** der Datei werden genau diese Bewegungen archiviert. Während des Dateidialogs neu hinzugekommene Bewegungen werden nicht mitarchiviert.
- Archiv ansehen und erneut exportieren. Keine automatische Löschung der Datenbank, Personen oder Materialien.

## APK herunterladen

**[NLL Lager 0.1.0 – Test-APK herunterladen](downloads/NLL-Lager-0.1.0-debug.apk)** (auf GitHub anschließend „Download raw file“ wählen). [SHA-256-Prüfsumme](downloads/SHA256SUMS).

## Auf dem Honeywell installieren und starten

1. `app-debug.apk` aus dem Build auf das Gerät kopieren und installieren; ggf. Installation aus dieser Quelle erlauben. Es handelt sich um eine **mit Entwicklungsschlüssel signierte Testversion**, nicht um eine Play-Store-Veröffentlichung.
2. In den Honeywell-Scan-Einstellungen für die App Tastaturausgabe aktivieren; Abschlusszeichen **Enter** verwenden. Keine zusätzliche Scanner-SDK-App oder Kameraberechtigung erforderlich.
3. NLL Lager öffnen. Unter **Personen verwalten → Aus Excel importieren** die Personenliste einlesen oder eine Person anlegen.
4. Persönlichen Code in das fokussierte Feld scannen und anmelden.
5. Unter **Material verwalten → Aus Excel importieren** Materialliste einlesen.
6. Im Buchungsbildschirm Materialcode scannen. Von/Nach Lager und Menge eingeben, **Bewegung speichern** drücken und Zusammenfassung bestätigen.
7. **Bericht exportieren** drücken, Speicherort und Dateinamen wählen. Erst auf die Erfolgsmeldung warten; Abbruch oder Schreibfehler archivieren nichts.

Beim Scannen muss das jeweilige Codefeld fokussiert sein. Ein Scan mit Enter übernimmt den Code; alternativ den Button drücken. In Mengenfeldern wird Eingabe als Menge interpretiert, deshalb vor dem nächsten Scan das Codefeld antippen. Führende Nullen und Groß-/Kleinschreibung bleiben bei Codes erhalten.

## Excel-Import

Vorlagen befinden sich in [vorlagen/](vorlagen/). **Beispieldatensätze vor dem echten Import ersetzen oder löschen.**

Materialien, erste Zeile:

| Scancode | Mat Nummer | Beschreibung | Art | Einheit |
| --- | --- | --- | --- | --- |
| 0001234567890 | MAT-001 | Ventil | Material | Stk |
| 0001234567891 | KM-032 | R32 | Kältemittel | kg |

Personen, erste Zeile:

| Personencode | Name |
| --- | --- |
| 0007 | Erika Muster |

- Echte `.xlsx`-Dateien, erste Tabelle, erste nichtleere Zeile enthält die Überschriften. Spalten dürfen umgeordnet werden. `.xls`, `.xlsm` und Passwortschutz sind nicht unterstützt; als `.xlsx` speichern.
- **Code-Spalten in Excel als Text formatieren, bevor Codes eingegeben werden.** Bereits durch Excel entfernte Nullen oder gerundete lange Nummern kann die App nicht wiederherstellen.
- Alle aufgeführten Felder sind erforderlich. Art = `Material` oder `Kältemittel`, Einheit frei (z. B. `Stk`, `kg`, `m`). Maximale Feldlänge 200 Zeichen, maximal 10.000 Datensätze und 20 MB Dateigröße.
- Formeln vorher durch ihre Werte ersetzen. Doppelte Codes innerhalb der Datei oder bereits vorhandene Codes brechen den gesamten Import ab. Keine stillen Überschreibungen; Datenbankimport ist eine Transaktion.
- Manuelle Erfassung bleibt ohne Excel möglich. Bestehende Stammdaten können in dieser ersten Version noch nicht bearbeitet werden.

## Berichte und Datensicherung

CSV: UTF-8 mit BOM, Semikolon, vollständig zitierte Felder, Dezimalkomma für deutsches Excel, ISO-Zeit mit lokalem Offset; schützt Textfelder gegen Excel-Formelausführung. In Excel bei Bedarf über **Daten → Aus Text/CSV** öffnen und Code-Spalten als Text importieren. Ein Doppelklick kann Codes durch Excel in Zahlen umwandeln.

Die Datenbank liegt privat auf dem Gerät. Eine Deinstallation oder „App-Daten löschen“ entfernt auch das interne Archiv. CSV-Berichte daher außerhalb der App aufbewahren und regelmäßig auf einen PC sichern. Die Personenanmeldung ist eine Zuordnung per Code, keine fälschungssichere Authentifizierung. Material- und Personenstammdaten bleiben beim Berichtsexport erhalten. Ein unterbrochener Prozess nach erfolgreichem Dateischreiben, aber vor der Archivierung lässt Bewegungen vorsichtshalber offen; Bewegungs-IDs erlauben das Erkennen möglicher doppelter Exporte.

## Entwicklung und Build

Java 21 (oder 17), Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.13 / Android Gradle Plugin 8.9.2. Mindestversion Android 8.0 (API 26), Ziel-API 35; vorgesehenes Gerät Honeywell EDA52 mit Android 11. Der Hardwaretest auf dem EDA52 steht aus.

```sh
export ANDROID_HOME=/pfad/zum/android-sdk
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Mit angeschlossenem Android-Gerät oder laufendem Emulator:
./gradlew :app:connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Die lokalen Tests verwenden Robolectric mit Android 11 für SQLite, Excel-Import und die Activity-Bedienung samt Export. Sie benötigen keinen vollständigen Emulator und nutzen isolierte Testdaten. Geräte-Instrumentierung (`connectedDebugAndroidTest`) dagegen löscht die Datenbank der verbundenen Testinstallation: **ausschließlich mit einem separaten Testgerät, niemals mit produktiven Buchungsdaten ausführen**.

Ein Hardwaretest auf dem Honeywell steht separat aus. In dieser Cloud-Maschine fehlt `/dev/kvm`; die versuchte vollständige Softwareemulation startete nicht zuverlässig (virtuelles Dateisystem / Kernel-Panic vor App-Start). Sie ist deshalb kein Bestandteil der erforderlichen Build- und lokalen Testprüfung. `tools/emulator-cloud.sh` bleibt für eine geeignete Testumgebung optional verfügbar.

Cloud-spezifische Toolchain-Einrichtung: `bash tools/setup-cloud.sh`; danach `bash tools/check-cloud.sh`. Caches, SDK und Entwicklungsschlüssel liegen außerhalb des Checkouts unter `/workspace/android-tools`. Bestehenden Checkout verwenden, keinen zusätzlichen Git-Worktree anlegen.
