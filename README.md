# NLL Lager

Android-App für die Erfassung von Material- und Kältemittelbewegungen auf einem Honeywell EDA52-0. Buchungen funktionieren offline; Listen und Excel-Berichte können optional über Gmail ausgetauscht werden.

## Projektstand

| Stand | Status |
| --- | --- |
| **0.5.0** | Vom Betreiber auf dem Gerät erfolgreich getestet |
| **0.6.0 · Build 8** | Vorheriger Release; ergänzt Gmail |
| **0.6.1 · Build 10** | Vorheriger Release |
| **0.7.0 · Build 13** | Wird wegen Mailproblemen zurückgezogen; nicht installieren |
| **0.7.1 · Build 15** | **Neuester veröffentlichter Release** |

[Release 0.7.1](https://github.com/iONEMond/NLL-Lager/releases/tag/v0.7.1) ergänzt Abbruch und Zeitlimit für Mailvorgänge, aktuelle Hintergrundanzeige und Excel über die Android-Mail-App teilen. 140 lokale Tests, Release-Build und Lint erfolgreich; öffentlicher Download und Signatur geprüft. Geräte-/Mailtest der Korrektur steht aus. 0.7.0 wegen gemeldeter Mailprobleme nicht weiter installieren. Details: [Versionshistorie](CHANGELOG.md) und [Prüfprotokoll](docs/VALIDIERUNG.md).

## Installation

**[Signierte APK 0.7.1 herunterladen](https://github.com/iONEMond/NLL-Lager/releases/download/v0.7.1/NLL-Lager-0.7.1-release.apk)** · [Releasehinweise](https://github.com/iONEMond/NLL-Lager/releases/tag/v0.7.1) · [SHA-256-Prüfsummen](downloads/SHA256SUMS)

Die Haupt-App verwendet `de.nll.lager`. Ein Update von der signierten 0.5.0 erhält die App-Daten; die App dazu nicht deinstallieren. Die Vorschau verwendet `de.nll.lager.preview` mit eigenen Daten. Alte Installationen mit einem anderen Signierschlüssel lassen sich nicht direkt aktualisieren.

Der Updater ist ab 0.6.1 enthalten. Von 0.5.0 oder 0.6.0 zunächst diese Version einmal manuell installieren; anschließend sind spätere Updates über die App möglich. Unter „Sicherung & Wiederherstellung“ einen Sicherungsordner einrichten.

## Funktionen

- Personenanmeldung per Code, Artikelsuche und Scannen mit Enter.
- Material- und Kältemittelbewegungen mit Menge, Von-/Nach-Lager und Mitarbeiterzuordnung.
- Passwortgeschützte Einstellungen für Stammdaten, Import, Export und Archiv.
- Excel-/CSV-Berichte, optionale Gmail-Anbindung für Listen und Exporte.
- Ab 0.6.1: Sicherungsordner, Wiederherstellung und geprüfte GitHub-Updates.

Die App erfasst Bewegungen; sie berechnet keine Lagerbestände. Sie arbeitet im privaten Scanner-Profil ohne Verbindung zum Intune-Arbeitsprofil.

## Dokumentation

| Thema | Einstieg |
| --- | --- |
| Bedienung, Import und Export | [Bedienungsanleitung](docs/BEDIENUNG.md) |
| Gmail und Mailbefehle | [Gmail einrichten](docs/GMAIL-EINRICHTUNG.md) |
| Daten sichern und wiederherstellen | [Sicherung](docs/SICHERUNG.md) |
| App-Updates und Releaseanforderungen | [Updates](docs/UPDATES.md) |
| Bauen, testen und veröffentlichen | [Entwicklung](docs/ENTWICKLUNG.md) |
| Architektur, Besonderheiten und offene Aufgaben | [Übergabe für Nachfolge-Agenten](Übergabe.md) |
| Prüfungen und Gerätetests | [Validierung](docs/VALIDIERUNG.md) |

Importvorlagen mit Beispieldaten: [Material](vorlagen/Material-Import.xlsx) · [Personen](vorlagen/Personen-Import.xlsx). Beispieldaten vor dem Einsatz ersetzen.

## Entwicklung starten

Voraussetzungen: JDK 17 oder 21, Android SDK Platform 35 und Build Tools 35.0.0. Gradle wird über den mitgelieferten Wrapper gestartet.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`. Vor Änderungen [AGENTS.md](AGENTS.md) und [Übergabe.md](Übergabe.md) lesen. Signierschlüssel, Passwörter und Tokens gehören nicht ins Repository.
