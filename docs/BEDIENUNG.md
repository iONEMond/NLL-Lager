# Bedienung und Dateiformate

[Zur Projektstartseite](../README.md) · [Versionshistorie](../CHANGELOG.md)

Die folgenden Anleitungen beschreiben den vorbereiteten Stand 0.6.1. Veröffentlicht ist 0.6.0; neue Funktionen sind jeweils gekennzeichnet.

## Start und Buchung

Unter **Einstellungen → Über die App (About)** stehen die tatsächlich installierte Version und Buildnummer, Ausgabe (Haupt-App/Vorschau), Paketname, Android-/API-Version und Datenbankversion sowie Hinweise zu Datenhaltung, Sicherung, Mail, Updates und Projektanleitungen. Ab 0.6.1, noch nicht veröffentlicht.

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

[Materialvorlage](../vorlagen/Material-Import.xlsx) und [Personenvorlage](../vorlagen/Personen-Import.xlsx) enthalten **Beispieldaten**, die vor produktivem Import ersetzt werden müssen.

Ab 0.6.1 (noch unveröffentlicht) zusätzlich Firmenformat: **Title, Code, Typ, Bemerkung, BST Menge**. Nur Title und Code werden benötigt: Code → Scancode und Materialnummer, Title → Beschreibung, Art → Material, Einheit → Stk. Typ, Bemerkung und Bestellmenge bleiben unberücksichtigt. Eine optionale Lager-Spalte ist möglich. Keine Übernahme einer Bestellmenge als Bestand oder Bewegung. Gilt für lokalen und Mailimport.

Materialspalten der bisherigen Vorlage: `Scancode`, `Mat Nummer`, `Beschreibung`, `Art`, `Einheit`; zusätzlich optional `Lager`. Art ist Material oder Kältemittel. Personenspalten: `Personencode`, `Name`; zusätzlich optional `Lagernummer`.

Die vorhandenen Vorlagen ohne Lager-Spalte bleiben kompatibel. Fehlende Zuordnungen anschließend im geschützten Menü pflegen. Vorhandene Codes werden nicht überschrieben; doppelte Codes brechen den gesamten Import ab. Die Datenbankübernahme erfolgt als Transaktion.

Nur echte `.xlsx`, erste Tabelle, erste nichtleere Zeile als Überschrift; Spalten dürfen umgeordnet werden. Maximal 10.000 Einträge, 20 MB, 200 Zeichen pro Feld. Formeln zuvor als Werte speichern. `.xls`, `.xlsm` und Passwortschutz werden nicht unterstützt.

**Codes und Lagernummern vor der Eingabe in Excel als Text formatieren**, damit führende Nullen erhalten bleiben.

Mailbefehle: **NLL Material**, **NLL Personen**, **NLL Export**. Kein Auftragsschlüssel erforderlich; nur die hinterlegte Firmenabsenderadresse wird zugelassen. Genau eine XLSX-Datei pro Import, Signaturbilder werden ignoriert.

## Export und Daten

Export, Import, Stammdatenverwaltung, Lageroptionen, Personen und Archiv sind ausschließlich in den passwortgeschützten Einstellungen verfügbar. Beim Export zwischen **Excel (.xlsx)**, **CSV** und **Per Mail senden & archivieren** wählen. Der Mailweg benötigt eine eingerichtete Gmail-Verbindung und archiviert nach bestätigtem Versand genau die Bewegungen des Berichts; Fehler oder unklarer Versand lassen sie offen. Nach erfolgreichem Schreiben und Schließen der Datei werden genau die exportierten offenen Bewegungen archiviert. Bei Abbruch oder Fehler bleiben sie offen. Archivierte Bewegungen können erneut exportiert werden.

Ab 0.6.1 (noch unveröffentlicht): XLSX-Blatt Warenbewegungen mit genau acht Spalten: **Code, Beschreibung, Menge, Bewegungsart, Mitarbeiter, Datum, Lagerort (Von -> Nach), Buchung erfolgt?**. Die letzte Spalte ist immer leer für Materialwirtschaft. Code ist der gespeicherte Scancode, Mitarbeiter der Name. Mengen sind numerisch, Datum enthält Datum/Uhrzeit als Excel-Datumswert, Lagerort verbindet Von und Nach mit „ -> “. Filter und fixierte Kopfzeile bleiben erhalten. CSV und Mailberichte verwenden dieselben Spalten. Neue Buchungen verlangen Entnahme/Rückgabe/Umlagerung; bei älteren Buchungen bleibt die Bewegungsart leer.

CSV: UTF-8 mit BOM, Semikolon, Dezimalkomma, vollständig zitierte und gegen Excel-Formelausführung geschützte Textfelder. In Excel Codes als Text importieren.

In der veröffentlichten 0.5.0 bleiben SQLite-Daten lokal und die App hat keine Internetberechtigung. Version 0.6.0 ergänzt Internetzugriff für Gmail und übermittelt bei aktivierter Einrichtung Berichte und Rückmeldungen an das konfigurierte Firmenpostfach. Eine Deinstallation oder „App-Daten löschen“ entfernt auch das Archiv. Berichte außerhalb der App sichern. Ein Prozessabbruch zwischen Dateischreiben und Archivierung kann offene Bewegungen zurücklassen. Vor erneutem Export prüfen, ob der Bericht bereits vorliegt; im neuen Acht-Spalten-Format werden keine Bewegungs-IDs ausgegeben. Personenanmeldung per Code dient der Zuordnung. Das Einstellungspasswort schützt die Bedienfunktionen der App.

Materiallager ist eine Stammdatenzuordnung. Es gibt weiterhin keine Bestandsberechnung, Werkzeuge oder Nachbestellungen. Kältemittel verwenden eine Menge mit Einheit; getrennte Flaschen-/kg-Erfassung ist noch nicht umgesetzt.

## Vollständige Sicherung (ab 0.6.1, noch unveröffentlicht)

Unter **Schloss → Sicherung & Wiederherstellung** einmal einen lokalen Dokumentenordner auswählen. Danach **Jetzt sichern** wählen und den Erfolgsstatus prüfen. Weitere Sicherungen werden nach Änderungen und beim App-Start automatisch eingeplant, sobald Android die Hintergrundarbeit zulässt. Sieben geprüfte Stände bleiben erhalten. [Einrichtung und Wiederherstellung](SICHERUNG.md). Die Sicherung enthält Material, Personen, Lager, offene Bewegungen und Archiv; keine Passwörter oder Google-Zugriffsdaten. Noch nicht im veröffentlichten Release 0.6.0 enthalten.

## App-Updates (ab 0.6.1, noch unveröffentlicht)

**Schloss → App aktualisieren → Nach Updates suchen** prüft den neuesten stabilen GitHub-Release ohne Anmeldung. Nach Download werden Prüfsumme, APK-Signatur, Paketname und höhere Versionsnummer geprüft. **Sichern & Update installieren** erstellt zuerst eine vollständige Sicherung und öffnet danach die Android-Installationsbestätigung. Ein eingerichteter Sicherungsordner ist Pflicht. Die Vorschau-App installiert keine Updates der Haupt-App. [Details und Releaseanforderungen](UPDATES.md). Einmalig muss eine Version mit dieser Funktion wie bisher installiert werden; die veröffentlichte 0.6.0 enthält den Updater noch nicht.

