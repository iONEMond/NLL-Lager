# Funktionspaket 0.7.0-dev – 20.10.2026

Version 0.7.0-dev, versionCode 12, Datenbank 7, Sicherungsformat 2 (alte Format-1-Dateien weiter lesbar). 124 lokale Tests ohne Fehler/übersprungene Tests. assembleDebug, lintDebug, assembleRelease und lintRelease erfolgreich. Sechzehn zusätzliche Tests gegenüber 0.6.2-dev prüfen Stammdatenänderung inklusive Codes und Konflikten mit erhaltenen Snapshots, Storno eines archivierten Originals mit offener Gegenbuchung, doppelte/fehlgeschlagene Stornos und Rollback, Doppelscanprüfung nach Neustart und explizite Freigabe, Neueingabe nach Storno, Sicherungs-Roundtrip mit Storno und Ablehnung ungültiger Bezüge, alte Format-1-Dateien samt Prüfsumme, konsistente/schreibfreie Importvorschau und Ablehnung veralteter Vorschau, persistente transaktionale Datenrevision, echtes Schema-6-Migrationsfixture sowie UI-Abläufe für Stammdaten, Mehrfachklicks, Wiederholungswarnung, Stornogrund und separat gespeicherte Korrektur. Sicherungsstatus auch bei Änderungen nach dem gesicherten Snapshot sowie Rollback geprüft.

Debug-Vorschau lokal separat gebaut. Keine neue Hardwareinstallation, visuelle Prüfung oder echte Mailversendung durchgeführt. Release-Build ist unsigniert; keine neue Veröffentlichung. Stabiler Download bleibt 0.6.1.

# Bedienungs- und Importänderungen 0.6.2-dev – 20.10.2026

Version 0.6.2-dev, versionCode 11, Datenbank weiterhin Version 6. Alle sechs beauftragten Punkte der Reihe nach implementiert und gezielt geprüft. Abschließend 108 lokale Tests, keine Fehler oder übersprungenen Tests; assembleDebug, lintDebug, assembleRelease und lintRelease erfolgreich. Elf zusätzliche Tests decken optionale Materialfelder/Spaltenpräsenz, Erhalt bei Updates, vierstellige PIN mit führenden Nullen, Fortschrittsbalken/Datensatzanzahlen, geklappte Sync-Einrichtung, Löschbestätigung samt erneuter Freigabeprüfung, Erhalt offener/archivierter Buchungen, Importkonflikt-Auswahl, Aktualisieren/Überspringen und Transaktionsrollback ab. Bestehende Mail-, Backup-, Signatur- und Exportfehler-Tests weiterhin erfolgreich. Relative Dokumentationslinks geprüft.

Keine neue Geräteinstallation, visuelle Honeywell-Prüfung oder echte Mailversendung ausgeführt. Fortschritt und Mailtransport in lokalen Tests geprüft; Original-Firmen-XLSX weiterhin nicht vorhanden. Debug-Vorschau unter separatem Paket de.nll.lager.preview lokal gebaut. Release-Ausgabe unsigniert; noch kein 0.6.2-Release oder neues OTA-Asset veröffentlicht. Stabile APK bleibt 0.6.1.

# Veröffentlichung 0.6.1 – 20.10.2026

Stabiler GitHub-Release v0.6.1 über Safari veröffentlicht (kein Entwurf/Pre-Release), genau eine APK sowie SHA256SUMS. Anonyme HTTPS-Abfrage von releases/latest liefert v0.6.1. APK erneut ohne Zugangsdaten heruntergeladen: 2.423.836 Bytes, SHA-256 a6d6adafeb98fa023a5b0ad9e1a24989b5f85d613aabcee89bb7d4f26cd85b31; identisch mit lokal signierter APK und GitHub-Digest. SHA256SUMS-Digest ebenfalls abgeglichen. Heruntergeladene APK mit apksigner erneut erfolgreich gegen vorhandenes Release-Zertifikat geprüft. Keine neue Geräteinstallation oder Mailprüfung durchgeführt. [Release](https://github.com/iONEMond/NLL-Lager/releases/tag/v0.6.1). Frühere Abschnitte sind historische Prüfstände.

# Signierte APK 0.6.1 – 20.10.2026

Android Studio meldet erfolgreichen signierten Release-Build nach lokaler Passworteingabe des Betreibers. apksigner verify --print-certs erfolgreich; bestehendes Release-Zertifikat SHA-256 1d70da7786708b6dcd04319235fc76ca4d411f12474a63bc98852fc9b5fa25c0. aapt: de.nll.lager, versionName 0.6.1, versionCode 10, minSdk 26, targetSdk 35. APK 2.423.836 Bytes; SHA-256 a6d6adafeb98fa023a5b0ad9e1a24989b5f85d613aabcee89bb7d4f26cd85b31. Lokale Kopien im Projektordner und downloads/; keine erneuten Geräte- oder Mailtests. Release-Upload und externe Downloadprüfung noch offen.

# Repository-Pflege – 20.10.2026

Dokumentation nach docs/ gegliedert, README gekürzt, Übergabe und Entwicklungsanleitung ergänzt. 11 Markdown-Dokumente mit 46 relativen Dateilinks geprüft; ein absichtlich ungültiger Link wurde im Prüftest korrekt abgelehnt und die temporäre Testdatei anschließend entfernt. About-Text auf den neuen Anleitungsordner angepasst. assembleRelease und lintRelease danach erfolgreich; fachliche Logik unverändert, bestehende 97 Funktionstests aus dem letzten Lauf nicht erneut ausgeführt. Keine weitere Geräteprüfung und keine signierte Veröffentlichung.

# Release-Vorbereitung 0.6.1 – 20.10.2026

About-Menü nachträglich ergänzt: Version/Build aus PackageInfo, Ausgabe, Android-/API- und Datenbankversion sowie Betriebs-/Projekthinweise. Auch dieser Bildschirm ist als Admin-Bildschirm geschützt und verwendet die bestehende Zurück-/Sperrlogik. Nach der Ergänzung erneut alle 97 vorhandenen Tests, assembleRelease und lintRelease erfolgreich. Noch keine visuelle Geräteprüfung dieses Menüs; Release weiterhin unsigniert.

Versionsname 0.6.1, versionCode 10, Paket de.nll.lager. Erneut 97 Tests ohne Fehler oder übersprungene Tests, assembleRelease und lintRelease erfolgreich. Metadaten der erstellten app-release-unsigned.apk mit aapt geprüft. APK ist unsigniert und nicht als installierbares Update veröffentlicht. Bestehender Schlüssel vorhanden; Android Studio benötigt die nicht gespeicherten Schlüsselpasswörter erneut. Signaturprüfung und Veröffentlichung folgen erst nach Signierung. Honeywell-Tests der neuen Funktionen weiterhin offen. downloads/SHA256SUMS bleibt bis zum signierten Release unverändert.

# Geräterückmeldung zu 0.5.0 – 20.10.2026

Betreiber meldet: „Hab die 0.5 getestet funktioniert wunderbar“. Erfolgreicher Gerätetest von Version 0.5.0 laut Betreiber; keine einzelnen Testfälle oder Logs angegeben. Diese Bestätigung gilt für 0.5.0 und bestätigt nicht die später ergänzten Funktionen in 0.6.0 oder 0.6.1-dev. Historische Abschnitte mit damals ausstehenden Hardwaretests bleiben als damaliger Prüfstand bestehen.

# In-App-Updates – 0.6.1-dev

20.10.2026: 97 Tests, Debug-Build und Lint erfolgreich. 13 neue Update-Tests prüfen stabile Metadaten, numerische Versionsvergleiche, fehlende Prüfsummen, doppelte Assets, Fremd-URLs, HTTPS-/Redirect-Hostgrenzen, Redirect-Schleifen, API-Limits und tokenfreie Anfragen. Echte vorhandene Release-APK 0.5.0 wird kryptografisch mit apksig geprüft; manipulierte APK trotz angepasster Dateiprüfsumme, falsches Zertifikat, falscher Paketname, Downgrade und unpassende Release-Version werden abgelehnt. Simulierter Download mit echtem APK-Inhalt und erfolgreicher Sicherung vor Installationsvorbereitung geprüft, außerdem Teil-Downloadbereinigung, fehlende Sicherung und laufende Mailverarbeitung. APK-Provider ist nur lesbar und gibt keine Datenbankpfade frei. Zwei UI-Tests prüfen Menüschutz, Vorschau-Sperre und erforderlichen Sicherungsordner.

Live-Leseprüfung ohne Zugangsdaten: Repository private=false; /releases/latest liefert v0.6.0, NLL-Lager-0.6.0-release.apk (2.243.408 Bytes) und SHA-256 db7bc06100d5e87169c9b62d4ed9c87624e12176636eb9bb2f5e38bfcb0c90a8. Netzwerktransport in den App-Tests simuliert; echter Download und Android-Installationsdialog auf dem Honeywell noch offen. Noch kein neuer Release und kein automatisches Update durchgeführt.

# Vollständige Sicherung – 0.6.1-dev

20.10.2026: 82 Tests, Debug-Build und Lint erfolgreich. Elf neue Sicherungstests prüfen vollständigen Roundtrip mit offenen/archivierten Buchungen, Lagerzuordnung und Bewegungsart; Prüfsumme, Formatversion und doppelte Codes; Dateianbieter mit echtem Datei-I/O, sieben Stände, unveränderte Fremddateien; Schreib-/Umbenennungsfehler, beschädigtes Rücklesen und gelöschte letzte Sicherung; Sicherheitskopie vor Wiederherstellung, Gmail-Trennung und blockierte Wiederherstellung bei laufender Mailverarbeitung; Transaktionsrollback einschließlich Lageroptionen; Migration 5→6 sowie Wiederherstellung nach simulierter Löschung der App-Daten bei erhaltenem Dokument. Ein zusätzlicher UI-Test prüft Menü-/Ergebnisschutz und Abbruch ohne Datenänderung. Der Test-Dateianbieter simuliert Androids Dokumentenzugriff; echte Ordnerfreigabe und Neuinstallation auf dem Honeywell stehen noch aus. Entwicklungsbuild, noch kein neuer Release.

# Ausgabeformat und Bewegungsart – 0.6.1-dev

20.10.2026: Acht-Spalten-Export für XLSX, CSV, Mail und Archiv. 70 Tests, Debug-Build und Lint erfolgreich. Zusätzliche Tests prüfen genaue Kopfzeilen/Reihenfolge, Textcode mit führenden Nullen, numerische Menge, Excel-Datum, gespeicherte Bewegungsart, Mitarbeitername, kombinierten Lagerort, leere letzte Spalte und Filterbereich A:H. UI-Test prüft Pflichtauswahl und gespeicherte Entnahme. Migration 4→5 erhält Archiv und lässt historische Bewegungsarten leer. Noch nicht veröffentlicht; installierte Release-APK bleibt 0.6.0.

# Importergänzung 0.6.1-dev – 20.10.2026

Firmenlayout aus dem Foto mit Title, Code, Typ, Bemerkung und BST Menge nachgebildet. Drei zusätzliche XLSX-Tests prüfen numerische Codes, führende Nullen als Text, umgeordnete Spalten, optionale Lagerzuordnung, keine Buchungen durch Bestellmengen sowie Ablehnung doppelter Codes, fehlender Pflichtwerte und doppelter Überschriften. 67 Tests, Debug-Build und Lint erfolgreich. Originaldatei noch nicht geprüft; bisher nur Foto vorhanden. Noch kein signierter Build oder Release dieser Ergänzung.

# Releaseprüfung 0.6.0

19.10.2026: Release für den ersten Gerätetest. Version 0.6.0, versionCode 8, Paket `de.nll.lager`. 64 lokale Tests, Debug-Build und Release-Lint erfolgreich. Release-APK über Android Studio mit dem vorhandenen Schlüssel signiert; Paket/Version mit aapt und Signatur mit apksigner geprüft. Zertifikat unverändert gegenüber 0.5.0. APK und SHA-256-Prüfsumme im Projektordner; Veröffentlichung als GitHub-Release v0.6.0. Honeywell-Hardwaretest noch offen. Die folgenden Aufzeichnungen beschreiben die Prüfungen der Entwicklungsstände, deren Code in diesem Release enthalten ist.

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

## Releaseprüfung 0.7.0 (Code 13), 20.10.2026

124 lokale Tests ohne Fehler; Release-Build und Release-Lint erneut erfolgreich. Signierte APK: de.nll.lager, Version 0.7.0, Code 13, Mindest-API 26. apksigner erfolgreich mit bestehendem Release-Zertifikat. Größe 2.438.752 Bytes, SHA-256 1f85c71d34653bff723e1bcf26b1b9b7dccaf17063f1c5d1f9d71c0d972f35eb. Passwörter durch Betreiber direkt eingegeben, nicht gespeichert. Veröffentlichung noch ausstehend. Kein neuer Hardware- oder Mailtest.
