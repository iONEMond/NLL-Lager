# NLL Lager – Android 0.6.0

Offline-App für den Honeywell EDA52-0: Material- und Kältemittelbewegungen scannen und lokal speichern. Helle Oberfläche mit blauen Akzenten, große Schaltflächen und getrennte Artikelbereiche.

[Versionshistorie und geplante Updates](CHANGELOG.md)

**Release 0.6.0 – erster Gerätetest.** [Gmail-Einrichtung und Ablauf](GMAIL-EINRICHTUNG.md). Google-Anmeldung, Excel-Mailversand und Material-Mailimport mit Firmensignatur im Emulator geprüft. Honeywell-Hardwaretest und echte Prüfung der neuen Export-/Neuanlageabläufe stehen noch aus.

**Release 0.6.1 vorbereitet, noch nicht veröffentlicht (versionCode 10)** – Import der Firmen-Materialliste mit `Title` und `Code`, neues Ausgabeformat, Bewegungsart-Auswahl, vollständige Gerätesicherung und In-App-Updates von GitHub. Signierung mit dem bestehenden Schlüssel steht aus; Android Studio benötigt die Schlüsselpasswörter erneut. Noch nicht in der veröffentlichten APK 0.6.0 enthalten.

## Download

**[NLL Lager 0.6.0 – signierte APK](https://github.com/iONEMond/NLL-Lager/releases/download/v0.6.0/NLL-Lager-0.6.0-release.apk)** – auf GitHub „Download raw file“ wählen. [SHA-256-Prüfsummen](downloads/SHA256SUMS).

Release für den ersten Gerätetest, `versionCode 8`, Paket `de.nll.lager`, App-Name **NLL Lager**. Hardwaretest auf dem Honeywell steht noch aus. Weitere Updates verwenden denselben Schlüssel und einen höheren versionCode.

Die bisherige [Vorschau 0.4.1](downloads/NLL-Lager-0.4.1-vorschau.apk) verwendet `de.nll.lager.preview`. Die Release-App wird separat installiert; Vorschau-Daten werden nicht automatisch übernommen. Eine vorhandene Version 0.1.0 mit gleichem Paketnamen und anderem Schlüssel lässt sich nicht direkt aktualisieren; vor einem Wechsel Daten exportieren.

Der private Signierschlüssel und Passwörter bleiben lokal und gehören nicht ins Repository.

## Bedienung

1. Scanner als Tastatureingabe mit Enter konfigurieren. Das jeweilige Codefeld muss fokussiert sein.
2. Schloss oben rechts öffnen. Beim ersten Mal eigenes Passwort mit mindestens 8 Zeichen samt Wiederholung festlegen.
3. Unter **Lageroptionen verwalten** die allgemeinen Lager einrichten. Personen und Materialien manuell anlegen oder aus Excel importieren.
4. Bei Personen die **Lagernummer** pflegen; bei Materialien das **Lager** auswählen. Vorhandene Einträge zum Nachpflegen antippen.
5. Offenes Schloss zum Sperren antippen. Im normalen Betrieb sind nur Personenanmeldung, Artikelwahl und Material-/Kältemittelbewegungen verfügbar.
6. Mit persönlichem Code anmelden und Material oder Kältemittel wählen. Beschreibung, Materialnummer und Scancode sind durchsuchbar. Artikel antippen oder exakten Code mit Enter übernehmen.
7. Das Materiallager wird als **Von Lager** vorausgewählt. **Nach Lager** ist ebenfalls ein Dropdown und startet bei jeder neuen Buchung mit der Lagernummer der angemeldeten Person. Menge eingeben und die Buchung bestätigen.
8. Der Pfeil oben links führt zurück; bei offener Buchung erfolgt eine Bestätigung vor dem Verwerfen der Eingaben.

Das Schloss bleibt auch beim Scrollen erreichbar. Freigabe wird nicht dauerhaft gespeichert; Neustart und Verlassen der App sperren. Bereits autorisierte Dateidialoge bzw. laufende Schreibvorgänge dürfen abgeschlossen werden. Ein Prozessverlust erfordert erneute Freigabe.

Unbekannte Artikelcodes können im Material-/KM-Bereich nach Scannen und Bestätigen über **Anlegen** mit dem Gerätepasswort erfasst werden. Der Code und die Artikelart sind vorbelegt. Nach Speichern startet die Buchung; die Einstellungen sind wieder gesperrt.

## Import

[Materialvorlage](vorlagen/Material-Import.xlsx) und [Personenvorlage](vorlagen/Personen-Import.xlsx) enthalten **Beispieldaten**, die vor produktivem Import ersetzt werden müssen.

Ab 0.6.1-dev zusätzlich Firmenformat: **Title, Code, Typ, Bemerkung, BST Menge**. Nur Title und Code werden benötigt: Code → Scancode und Materialnummer, Title → Beschreibung, Art → Material, Einheit → Stk. Typ, Bemerkung und Bestellmenge bleiben unberücksichtigt. Eine optionale Lager-Spalte ist möglich. Keine Übernahme einer Bestellmenge als Bestand oder Bewegung. Gilt für lokalen und Mailimport.

Materialspalten der bisherigen Vorlage: `Scancode`, `Mat Nummer`, `Beschreibung`, `Art`, `Einheit`; zusätzlich optional `Lager`. Art ist Material oder Kältemittel. Personenspalten: `Personencode`, `Name`; zusätzlich optional `Lagernummer`.

Die vorhandenen Vorlagen ohne Lager-Spalte bleiben kompatibel. Fehlende Zuordnungen anschließend im geschützten Menü pflegen. Vorhandene Codes werden nicht überschrieben; doppelte Codes brechen den gesamten Import ab. Die Datenbankübernahme erfolgt als Transaktion.

Nur echte `.xlsx`, erste Tabelle, erste nichtleere Zeile als Überschrift; Spalten dürfen umgeordnet werden. Maximal 10.000 Einträge, 20 MB, 200 Zeichen pro Feld. Formeln zuvor als Werte speichern. `.xls`, `.xlsm` und Passwortschutz werden nicht unterstützt.

**Codes und Lagernummern vor der Eingabe in Excel als Text formatieren**, damit führende Nullen erhalten bleiben.

Mailbefehle: **NLL Material**, **NLL Personen**, **NLL Export**. Kein Auftragsschlüssel erforderlich; nur die hinterlegte Firmenabsenderadresse wird zugelassen. Genau eine XLSX-Datei pro Import, Signaturbilder werden ignoriert.

## Export und Daten

Export, Import, Stammdatenverwaltung, Lageroptionen, Personen und Archiv sind ausschließlich in den passwortgeschützten Einstellungen verfügbar. Beim Export zwischen **Excel (.xlsx)**, **CSV** und **Per Mail senden & archivieren** wählen. Der Mailweg benötigt eine eingerichtete Gmail-Verbindung und archiviert nach bestätigtem Versand genau die Bewegungen des Berichts; Fehler oder unklarer Versand lassen sie offen. Nach erfolgreichem Schreiben und Schließen der Datei werden genau die exportierten offenen Bewegungen archiviert. Bei Abbruch oder Fehler bleiben sie offen. Archivierte Bewegungen können erneut exportiert werden.

Ab 0.6.1-dev: XLSX-Blatt Warenbewegungen mit genau acht Spalten: **Code, Beschreibung, Menge, Bewegungsart, Mitarbeiter, Datum, Lagerort (Von -> Nach), Buchung erfolgt?**. Die letzte Spalte ist immer leer für Materialwirtschaft. Code ist der gespeicherte Scancode, Mitarbeiter der Name. Mengen sind numerisch, Datum enthält Datum/Uhrzeit als Excel-Datumswert, Lagerort verbindet Von und Nach mit „ -> “. Filter und fixierte Kopfzeile bleiben erhalten. CSV und Mailberichte verwenden dieselben Spalten. Neue Buchungen verlangen Entnahme/Rückgabe/Umlagerung; bei älteren Buchungen bleibt die Bewegungsart leer.

CSV: UTF-8 mit BOM, Semikolon, Dezimalkomma, vollständig zitierte und gegen Excel-Formelausführung geschützte Textfelder. In Excel Codes als Text importieren.

In der veröffentlichten 0.5.0 bleiben SQLite-Daten lokal und die App hat keine Internetberechtigung. Version 0.6.0 ergänzt Internetzugriff für Gmail und übermittelt bei aktivierter Einrichtung Berichte und Rückmeldungen an das konfigurierte Firmenpostfach. Eine Deinstallation oder „App-Daten löschen“ entfernt auch das Archiv. Berichte außerhalb der App sichern. Ein Prozessabbruch zwischen Dateischreiben und Archivierung kann offene Bewegungen zurücklassen; Bewegungs-IDs helfen beim Erkennen doppelter Exporte. Personenanmeldung per Code dient der Zuordnung. Das Einstellungspasswort schützt die Bedienfunktionen der App.

Materiallager ist eine Stammdatenzuordnung. Es gibt weiterhin keine Bestandsberechnung, Werkzeuge oder Nachbestellungen. Kältemittel verwenden eine Menge mit Einheit; getrennte Flaschen-/kg-Erfassung ist noch nicht umgesetzt.

## Vollständige Sicherung (ab 0.6.1-dev)

Unter **Schloss → Sicherung & Wiederherstellung** einmal einen lokalen Dokumentenordner auswählen. Danach **Jetzt sichern** wählen und den Erfolgsstatus prüfen. Weitere Sicherungen werden nach Änderungen und beim App-Start automatisch eingeplant, sobald Android die Hintergrundarbeit zulässt. Sieben geprüfte Stände bleiben erhalten. [Einrichtung und Wiederherstellung](SICHERUNG.md). Die Sicherung enthält Material, Personen, Lager, offene Bewegungen und Archiv; keine Passwörter oder Google-Zugriffsdaten. Noch nicht im veröffentlichten Release 0.6.0 enthalten.

## App-Updates (ab 0.6.1-dev)

**Schloss → App aktualisieren → Nach Updates suchen** prüft den neuesten stabilen GitHub-Release ohne Anmeldung. Nach Download werden Prüfsumme, APK-Signatur, Paketname und höhere Versionsnummer geprüft. **Sichern & Update installieren** erstellt zuerst eine vollständige Sicherung und öffnet danach die Android-Installationsbestätigung. Ein eingerichteter Sicherungsordner ist Pflicht. Die Vorschau-App installiert keine Updates der Haupt-App. [Details und Releaseanforderungen](UPDATES.md). Einmalig muss eine Version mit dieser Funktion wie bisher installiert werden; die veröffentlichte 0.6.0 enthält den Updater noch nicht.

## Entwicklung

Java 17 oder 21, Android SDK Platform 35 und Build Tools 35.0.0. Gradle 8.13 / AGP 8.9.2. Mindestens Android 8.0 (API 26), Ziel-API 35.

```sh
export JAVA_HOME=/pfad/zum/jdk-21
export ANDROID_HOME=/pfad/zum/android-sdk
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Datenbankversion 6 ergänzt transaktional gesicherte Lageroptionen; Migration von Version 1/2/3/4/5 erhält bestehende Bewegungen und Stammdaten. Lokale Tests nutzen Robolectric mit Android 11. Stand der Prüfung: [VALIDIERUNG.md](VALIDIERUNG.md).

Geräte-Instrumentierung (`:app:connectedDebugAndroidTest`) löscht App-Testdaten und darf nur auf einer separaten Testinstallation ausgeführt werden. Cloud-Setup bleibt unter `tools/setup-cloud.sh` und `tools/check-cloud.sh` verfügbar.
