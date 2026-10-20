# Bedienung und Dateiformate

[Zur Projektstartseite](../README.md) · [Versionshistorie](../CHANGELOG.md)

Die Anleitung umfasst den veröffentlichten Stand 0.6.1 sowie ausdrücklich gekennzeichnete Ergänzungen für 0.7.0-dev (einschließlich des Zwischenstands 0.6.2-dev). 0.7.0 ist noch nicht als Release verfügbar.

## Start und Buchung

Unter **Einstellungen → Über die App (About)** stehen die tatsächlich installierte Version und Buildnummer, Ausgabe (Haupt-App/Vorschau), Paketname, Android-/API-Version und Datenbankversion sowie Hinweise zu Datenhaltung, Sicherung, Mail, Updates und Projektanleitungen. Ab 0.6.1.

1. Scanner als Tastatureingabe mit Enter konfigurieren. Das jeweilige Codefeld muss fokussiert sein.
2. Schloss oben rechts öffnen. Beim ersten Mal eigenes Passwort mit mindestens 8 Zeichen samt Wiederholung festlegen. Ab 0.6.2-dev ist alternativ eine vierstellige PIN erlaubt; bestehende Passwörter bleiben gültig.
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

Ab 0.6.1 zusätzlich Firmenformat: **Title, Code, Typ, Bemerkung, BST Menge**. Nur Title und Code werden benötigt: Code → Scancode und Materialnummer, Title → Beschreibung, Art → Material, Einheit → Stk. Typ, Bemerkung und Bestellmenge bleiben unberücksichtigt. Eine optionale Lager-Spalte ist möglich. Keine Übernahme einer Bestellmenge als Bestand oder Bewegung. Gilt für lokalen und Mailimport.

Materialspalten der bisherigen Vorlage: `Scancode`, `Mat Nummer`, `Beschreibung`, `Art`, `Einheit`; zusätzlich optional `Lager`. Art ist Material oder Kältemittel. Personenspalten: `Personencode`, `Name`; zusätzlich optional `Lagernummer`.

Die vorhandenen Vorlagen ohne Lager-Spalte bleiben kompatibel. Fehlende Zuordnungen anschließend im geschützten Menü pflegen. Bis 0.6.1 brechen bereits vorhandene Codes den Dateiimport ab. Ab 0.6.2-dev wird bei vorhandenen Codes nach **Vorhandene aktualisieren**, **Vorhandene überspringen** oder **Abbrechen** gefragt. Bei beiden Übernahmeoptionen werden neue Einträge ergänzt; nicht in der Datei enthaltene Einträge bleiben erhalten. Nicht mitgelieferte Zusatzspalten bleiben beim Aktualisieren unverändert. Mehrfachcodes innerhalb derselben Datei sind weiterhin ungültig. Die Datenbankübernahme erfolgt als Transaktion.

Nur echte `.xlsx`, erste Tabelle, erste nichtleere Zeile als Überschrift; Spalten dürfen umgeordnet werden. Maximal 10.000 Einträge, 20 MB, 200 Zeichen pro Feld. Formeln zuvor als Werte speichern. `.xls`, `.xlsm` und Passwortschutz werden nicht unterstützt.

**Codes und Lagernummern vor der Eingabe in Excel als Text formatieren**, damit führende Nullen erhalten bleiben.

Mailbefehle: **NLL Material**, **NLL Personen**, **NLL Export**. Kein Auftragsschlüssel erforderlich; nur die hinterlegte Firmenabsenderadresse wird zugelassen. Genau eine XLSX-Datei pro Import, Signaturbilder werden ignoriert.

## Ergänzungen ab 0.6.2-dev

- Material: Nur **Code** (auch Scancode/Barcode) und **Beschreibung** (auch Title) sind Pflichtspalten. Mat Nummer, Art, Einheit und Lager dürfen fehlen. Bei neuen Einträgen gelten Nummer = Code, Art = Material, Einheit = Stk, Lager leer; leere Materialnummer/Art/Einheit verwenden dieselben Standardwerte. Explizite Art weiterhin Material oder Kältemittel. Bei Personen bleiben Code und Name Pflicht; Beschreibung/Title ist auch als Name zulässig, Lagernummer optional.
- Personen-/Materialliste: Eintrag antippen → **Person löschen** bzw. **Material löschen** → bestätigen. Historische Bewegungen und Archiv werden nicht gelöscht. Gelöschte Einträge können durch einen späteren Import wieder angelegt werden.
- Import, Datei-/Mail-Export und manuelle Synchronisierung zeigen einen Fortschrittsbalken. Er zeigt je Arbeitsschritt die Datensatzanzahl, wenn bekannt, sonst eine laufende Aktivität. Erst die abschließende Erfolgsmeldung bestätigt die Übernahme bzw. Archivierung.
- Bei verbundener Gmail-Anbindung werden Einrichtungsoptionen ausgeblendet. **Verbindung bearbeiten** zeigt sie bei Bedarf wieder; fehlende Verbindung erfordert erneut Google-Freigabe.

## Weitere Funktionen für 0.7.0-dev

**Stammdaten bearbeiten:** Unter Material-/Personenliste den Eintrag antippen. Code und alle fachlichen Felder sind änderbar. Ein bereits belegter Code wird abgelehnt. Alte Buchungen behalten Code, Beschreibung, Person und Lager vom damaligen Buchungszeitpunkt.

**Buchungen korrigieren:** Einstellungen → Bewegungen & Archiv → betreffende Buchung → Stornieren/korrigieren. Einen Grund angeben. „Nur stornieren“ erfasst eine Gegenbuchung; „Stornieren & neu erfassen“ öffnet danach einen vorbelegten Buchungsentwurf. Dieser muss separat gespeichert werden. Fehlt inzwischen Artikel oder Person im Katalog, bleibt das Storno erfasst; Stammdaten vor der Neubuchung pflegen.

Das Original wird weder geändert noch gelöscht. Die Gegenbuchung hat dieselbe positive Menge, umgekehrte Lager und die Bewegungsart **Storno**. Ihre Beschreibung nennt Original-ID und Grund. Auch nach Export/Archivierung des Originals ist die Gegenbuchung eine neue offene Bewegung für den nächsten Bericht. Bei noch offenen Originalen erscheinen Original und Storno gemeinsam im Export. Materialwirtschaft muss diese Stornozeilen entsprechend berücksichtigen. Ein Original kann genau einmal storniert werden; ein Storno selbst nicht erneut. Für eine Fehlkorrektur eine passende neue normale Buchung erfassen.

**Doppelscan-Schutz:** Eine mit der letzten nicht stornierten Normalbuchung identische Erfassung innerhalb zehn Sekunden fragt „Trotzdem buchen“. Prüfkriterien: Person, Code, Menge, Von-/Nach-Lager und Bewegungsart. Abbrechen verändert keine Daten. Bewusst wiederholte Buchungen sind weiterhin möglich. Das ist eine Prüfung schneller Wiederholungen, keine allgemeine Dublettenbereinigung.

**Importvorschau:** Vor Übernahme werden die Anzahlen neuer, geänderter und unveränderter Einträge angezeigt. „Änderungen ansehen“ zeigt die gelieferten Änderungen je Feld mit Alt-/Neuwert, 20 Einträge je Seite. Nicht gelieferte Zusatzspalten werden erhalten. Ändern sich die Geschäftsdaten zwischen Vorschau und Übernahme, wird erneut eine Vorschau/Bestätigung verlangt.

**Sicherungsstatus:** Startseite, Einstellungen und Sicherungsmenü zeigen den letzten erfolgreichen Sicherungszeitpunkt und ob danach Geschäftsdaten geändert wurden. Automatische Sicherungen dürfen verzögert sein. Der Hinweis bezieht sich auf den letzten fertig geprüften Snapshot; er ist keine laufende Kontrolle, ob eine Datei außerhalb der App später gelöscht oder beschädigt wurde. Nach Auswahl eines anderen Sicherungsordners ist zunächst eine neue Sicherung nötig.

## Export und Daten

Export, Import, Stammdatenverwaltung, Lageroptionen, Personen und Archiv sind ausschließlich in den passwortgeschützten Einstellungen verfügbar. Beim Export zwischen **Excel (.xlsx)**, **CSV** und **Per Mail senden & archivieren** wählen. Der Mailweg benötigt eine eingerichtete Gmail-Verbindung und archiviert nach bestätigtem Versand genau die Bewegungen des Berichts; Fehler oder unklarer Versand lassen sie offen. Nach erfolgreichem Schreiben und Schließen der Datei werden genau die exportierten offenen Bewegungen archiviert. Bei Abbruch oder Fehler bleiben sie offen. Archivierte Bewegungen können erneut exportiert werden.

Ab 0.6.1: XLSX-Blatt Warenbewegungen mit genau acht Spalten: **Code, Beschreibung, Menge, Bewegungsart, Mitarbeiter, Datum, Lagerort (Von -> Nach), Buchung erfolgt?**. Die letzte Spalte ist immer leer für Materialwirtschaft. Code ist der gespeicherte Scancode, Mitarbeiter der Name. Mengen sind numerisch, Datum enthält Datum/Uhrzeit als Excel-Datumswert, Lagerort verbindet Von und Nach mit „ -> “. Filter und fixierte Kopfzeile bleiben erhalten. CSV und Mailberichte verwenden dieselben Spalten. Neue Buchungen verlangen Entnahme/Rückgabe/Umlagerung; bei älteren Buchungen bleibt die Bewegungsart leer.

CSV: UTF-8 mit BOM, Semikolon, Dezimalkomma, vollständig zitierte und gegen Excel-Formelausführung geschützte Textfelder. In Excel Codes als Text importieren.

In der veröffentlichten 0.5.0 bleiben SQLite-Daten lokal und die App hat keine Internetberechtigung. Version 0.6.0 ergänzt Internetzugriff für Gmail und übermittelt bei aktivierter Einrichtung Berichte und Rückmeldungen an das konfigurierte Firmenpostfach. Eine Deinstallation oder „App-Daten löschen“ entfernt auch das Archiv. Berichte außerhalb der App sichern. Ein Prozessabbruch zwischen Dateischreiben und Archivierung kann offene Bewegungen zurücklassen. Vor erneutem Export prüfen, ob der Bericht bereits vorliegt; im neuen Acht-Spalten-Format werden keine Bewegungs-IDs ausgegeben. Personenanmeldung per Code dient der Zuordnung. Das Einstellungspasswort schützt die Bedienfunktionen der App.

Materiallager ist eine Stammdatenzuordnung. Es gibt weiterhin keine Bestandsberechnung, Werkzeuge oder Nachbestellungen. Kältemittel verwenden eine Menge mit Einheit; getrennte Flaschen-/kg-Erfassung ist noch nicht umgesetzt.

## Vollständige Sicherung (ab 0.6.1)

Unter **Schloss → Sicherung & Wiederherstellung** einmal einen lokalen Dokumentenordner auswählen. Danach **Jetzt sichern** wählen und den Erfolgsstatus prüfen. Weitere Sicherungen werden nach Änderungen und beim App-Start automatisch eingeplant, sobald Android die Hintergrundarbeit zulässt. Sieben geprüfte Stände bleiben erhalten. [Einrichtung und Wiederherstellung](SICHERUNG.md). Die Sicherung enthält Material, Personen, Lager, offene Bewegungen und Archiv; keine Passwörter oder Google-Zugriffsdaten. Nicht in älteren Releases 0.5.0/0.6.0 enthalten.

## App-Updates (ab 0.6.1)

**Schloss → App aktualisieren → Nach Updates suchen** prüft den neuesten stabilen GitHub-Release ohne Anmeldung. Nach Download werden Prüfsumme, APK-Signatur, Paketname und höhere Versionsnummer geprüft. **Sichern & Update installieren** erstellt zuerst eine vollständige Sicherung und öffnet danach die Android-Installationsbestätigung. Ein eingerichteter Sicherungsordner ist Pflicht. Die Vorschau-App installiert keine Updates der Haupt-App. [Details und Releaseanforderungen](UPDATES.md). Einmalig muss eine Version mit dieser Funktion wie bisher installiert werden; die veröffentlichte 0.6.0 enthält den Updater noch nicht.


## Ab 0.7.1: Abbrechen und über Mail-App teilen (unveröffentlicht)

Laufende Importe, Exporte und manuelle Mailvorgänge bieten Abbrechen. Bitte den bestätigten Abschluss abwarten; beim endgültigen Speichern wird die Schaltfläche gesperrt. Importänderungen werden bei rechtzeitigem Abbruch vollständig zurückgerollt. Ein abgebrochener Dateiexport archiviert nichts; eine unvollständige Datei ggf. am Speicherziel löschen. Bereits abgeschlossene Mailaufträge oder übertragene Mails lassen sich nicht zurückrufen. Manuelle Mailvorgänge enden nach spätestens zwei Minuten mit einer Abbruchanforderung; Netzwerk-/Dateisystemtreiber können bis zur Rückkehr ihres laufenden Aufrufs benötigen.

Unter Bericht exportieren → Excel über Mail-App teilen eine eingerichtete Mail-App im privaten Profil wählen und dort selbst senden. Empfänger und Betreff sind vorbelegt, XLSX ist angehängt. Gmail-API-Einrichtung unserer App ist hierfür nicht erforderlich. Automatische Mailimporte benötigen weiterhin die Gmail-Anbindung. Teilen bestätigt keinen Versand und archiviert nichts.

Danach unter Bericht exportieren den offenen Bericht abschließen: Versand bestätigt · archivieren übernimmt nur die enthaltenen Bewegungen. Erneut teilen verwendet dieselbe Datei. Über Weitere Optionen kann man später entscheiden oder Bewegungen offen lassen. Auch nach Neustart bleibt die Entscheidung offen. Vor einem Ersatzversand bei unklarem Gmail-Direktversand erst Firmenpostfach und Gesendet prüfen, um doppelte Übernahme zu vermeiden.
