# Entwicklung 0.6.0-dev – noch nicht veröffentlicht

Stand: 19.10.2026. Gmail-API-Anbindung, Auftragsverarbeitung, transaktionaler Mailimport, dauerhafte Versandwarteschlange und separat bestätigter Exportabschluss lokal umgesetzt. Echte Google-Anmeldung, Gmail-Abruf und der Versand eines leeren Excel-Testexports sind im separaten Emulator geprüft. 64 lokale Tests bestanden (davon 19 Mail-Verarbeitungstests und 2 zusätzliche UI-Tests). Debug-Build und Android-Lint erfolgreich; Lint ohne Fehler. Datenbankmigration aus Version 1 und 2, Import-Rollback, doppelte Aufträge, Export-Snapshot, getrennte Archivierung, unklarer Versandstatus, Empfängerprüfung und geschütztes Mail-Menü geprüft. Entwicklungsversion im bestehenden API-37-Emulator installiert und gestartet; keine AndroidRuntime-Startfehler im geprüften Logauszug. Zusätzlich 0.6.0-dev-Release-Test-APK (Paket de.nll.lager, versionCode 7) über Android Studio signiert; Paket/Version mit aapt und Signatur mit apksigner erfolgreich verifiziert. Zertifikat unverändert gegenüber 0.5.0. Google-Kontoanmeldung, OAuth-Freigabe und echter Gmail-Abruf mit bestätigtem Status „Synchronisierung abgeschlossen“ im separaten Emulator NLL Gmail Test geprüft (19.10.2026). Leerer Excel-Testexport mit 0 offenen Bewegungen am 19.10.2026 um 22:25 ausgelöst; Auftragsstatus „Versendet“ in der App bestätigt. Empfang im Firmenpostfach vom Betreiber bestätigt. Erster Importauftrag wegen Signaturbildern abgelehnt; Fehlerantwort mit Status „Versendet“ bestätigt. Erneuter Material-Mailimport mit Firmensignatur erfolgreich (1 ergänzt, 0 aktualisiert), Rückmeldung versendet. Honeywell-Hintergrundtest noch offen.

# Validierung – NLL Lager 0.5.0

Stand: 19.10.2026. Versionsfreigabe ohne Änderungen an der App-Logik gegenüber 0.4.1.

- Release-Build und `:app:lintRelease` erfolgreich.
- Signierte APK mit apksigner erfolgreich verifiziert (v2); Zertifikat SHA-256: `1d70da7786708b6dcd04319235fc76ca4d411f12474a63bc98852fc9b5fa25c0`.
- Zielversion 0.5.0 (versionCode 6), Paket `de.nll.lager`, Mindest-API 26, Ziel-/Compile-API 35.

## Funktionsprüfung des unveränderten Stands 0.4.1

Stand: 19.10.2026, lokaler Apple-Silicon-Mac.

- APK-Build erfolgreich. Paket de.nll.lager.preview, Version 0.4.1 (5), Mindest-API 26, Ziel-/Compile-API 35.
- 40 lokale Tests bestanden: 3 AdminSettingsTest, 5 LocationsExportTest, 5 RulesTest, 10 StoreTest, 17 WorkflowTest. Robolectric/Android 11.
- Geprüft: Passwortprüfung und Speicherung ohne Klartext, gesperrte Verwaltungsaktionen, Rückkehr aus dem Hintergrund, Lagerlisten, Lagerzuordnung/Vorauswahl je Buchung, Datenbankmigration mit Erhalt alter Bewegungen, XLSX-Import mit alten und neuen Spalten, führende Nullen, Dezimalmengen, CSV und XLSX, Export-Snapshot, Archivexport und Schreibfehler ohne Archivierung.
- XLSX zusätzlich unabhängig mit openpyxl geöffnet: Textcodes, numerische Menge, Datumswert und Berichtsspalten geprüft.
- Android-Lint ohne Fehler. Verbleibende Warnungen im lokal generierten Bericht.
- Vorschau als Update im lokalen ARM-Emulator API 37 installiert und gestartet. Oberfläche, Personen-/Materialimport und Anmeldung visuell geprüft. Der Zurück-Pfeil nutzt die vorhandenen Navigationsziele und die Bestätigung beim Verlassen einer Buchung.

Kein Honeywell-Hardwaretest. Vollständige Geräte-Instrumentierung wurde für diesen Stand nicht ausgeführt. 40 bestandene lokale Tests sind kein Nachweis für Scannerverhalten auf dem EDA52-0.

Die herunterladbare APK ist eine mit lokalem Entwicklungsschlüssel signierte Vorschau. Die ältere 0.1.0 hat ein anderes Paket und bleibt separat installierbar. Prüfsummen: downloads/SHA256SUMS.

Signaturbild-Korrektur: Drei zusätzliche Tests prüfen Import mit verschachtelten Bildanhängen ohne Bilddownload, unveränderte Ablehnung zusätzlicher Excel-/Fremddateien und Ablehnung reiner Bildmails. Korrigierte Test-APK erneut mit bestehendem Release-Schlüssel signiert und mit apksigner geprüft. Erneuter echter Materialimport mit Firmensignatur am 19.10.2026 erfolgreich: 1 ergänzt, 0 aktualisiert; Rückmeldung mit Status „Versendet“.

Vereinfachte Betreffzeilen ohne Schlüssel: 64 Tests, Debug-Build und Lint erfolgreich. Tests decken alle drei Befehle, Groß-/Kleinschreibung, zusätzliche Wörter, Antwortpräfixe, unveränderte Absenderprüfung und Erhalt bestehender Kontofreigabe/Abrufgrenze ab. Signierter Build mit kurzen Betreffzeilen erstellt und mit apksigner geprüft (bestehendes Release-Zertifikat). APK enthält die neuen Betreffzeilen; Kopien im Mac-Projektordner aktualisiert. Echter Mailtest mit kurzen Betreffzeilen noch ausstehend.

Neue Abläufe: 64 Tests, Debug-Build und Lint erfolgreich. Vier zusätzliche Mailtests prüfen automatische Snapshot-Archivierung, unklaren Versand ohne Archivierung, Abschluss nach Neustart ohne Wiederholung und Migration 3→4 mit bestehenden Aufträgen. Drei UI-Tests prüfen Passwortschutz einschließlich falschem Passwort und vorbelegtem Code, Rückkehr zur gesperrten Buchung, KM-Vorbelegung/Abbruch sowie die Mailoption im Berichtmenü. Aktualisierte APK mit bestehendem Release-Schlüssel signiert, mit apksigner geprüft und als Update im separaten Gmail-Testemulator installiert. Echte Mail-/Geräteprüfung dieser beiden neuen Abläufe noch offen.
