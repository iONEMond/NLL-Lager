# Validierung – NLL Lager 0.1.0

Stand: 19.10.2026, aktuelle Cloud-Instanz.

- APK erfolgreich gebaut: `app/build/outputs/apk/debug/app-debug.apk`.
- Paket `de.nll.lager`, Android ab API 26, Ziel-API 35.
- APK-Signatur v2 erfolgreich geprüft; signierte Entwicklungsversion.
- 22 lokale Tests: 5 RulesTest, 10 StoreTest, 7 WorkflowTest. Keine Fehler, keine übersprungenen Tests. Android-Framework für API 30 über Robolectric.
- Geprüft: Personenanmeldung, Codeänderung vor Buchung, Dezimalkomma bei deutschem und englischem System, SQLite-Persistenz, exakte Mengen, XLSX-Vorlagen/Shared Strings/Spaltenzuordnung, vollständiges Rollback bei Importfehlern, CSV-Ausgabe, erfolgreiche Archivierung nach Dateischreiben, Exportabbruch und Schreibfehler ohne Archivierung, Export-Snapshot und Archiv erneut exportieren.
- Android-Lint: keine Fehler. Verbleibende Warnungen stehen im generierten Lint-Bericht.
- Gradle-Wrapper 8.13 mit offizieller SHA-256-Prüfsumme gebootstrapped. Build und Setup-Skript wiederholt ausgeführt.

Die Ergebnisse liegen unter `app/build/test-results/testDebugUnitTest/` und `app/build/reports/lint-results-debug.html`.

## Noch nicht auf Hardware geprüft

Der Honeywell EDA52-0 wurde hier nicht angeschlossen. Vollständige Emulator-Instrumentierung wurde nicht ausgeführt: Die Cloud-Maschine hat kein `/dev/kvm`, Softwareemulation erreichte keinen verlässlichen Start; Android 11 zeigte einen Kernel-Panic des virtuellen Dateisystems vor App-Start. Das ist keine erfolgreiche APK-Laufzeitprüfung auf einem Gerät.

Die 13 bereitgestellten Geräte-Tests nur auf einem separaten Testgerät ausführen; sie löschen dessen App-Datenbank. Scannerprofil auf Tastaturausgabe mit Enter einstellen und Buchung/Export mit Testdaten am Honeywell prüfen.

Die Cloud-Felder `install_script` und `start_skill` sind als Entwurf gespeichert. Veröffentlichung und Wiederherstellung in einer neuen Cloud-Aufgabe wurden nicht geprüft.

SHA-256 der APK:
`2ada137d35aae064b1151a75c795c45aeaaa7c440d044d3175625f5df54a6a91`
