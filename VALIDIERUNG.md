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
