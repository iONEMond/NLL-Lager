# NLL Lager

Android-App für die Erfassung von Material- und Kältemittelbewegungen auf einem Honeywell EDA52-0. Buchungen funktionieren offline; Listen und Excel-Berichte können optional über Gmail ausgetauscht werden.

## Projektstand

| Stand | Status |
| --- | --- |
| **0.5.0** | Vom Betreiber auf dem Gerät erfolgreich getestet |
| **0.6.0 · Build 8** | Vorheriger Release; ergänzt Gmail |
| **0.6.1 · Build 10** | **Neuester veröffentlichter Release** |
| **0.7.0 · Build 13** | Release vorbereitet, noch nicht veröffentlicht |

[Release 0.6.1](https://github.com/iONEMond/NLL-Lager/releases/tag/v0.6.1) ergänzt Firmenlisten-Import, das neue Exportformat, Bewegungsarten, vollständige Sicherung/Wiederherstellung, GitHub-Updates und ein About-Menü. Lokale Prüfungen: 97 Tests, Release-Build und Release-Lint erfolgreich. Tests der neuen Abläufe auf dem Honeywell stehen aus. Details: [Versionshistorie](CHANGELOG.md) und [Prüfprotokoll](docs/VALIDIERUNG.md).

**In Entwicklung für 0.7.0:** Alle Änderungen aus 0.6.2-dev sowie vollständige Stammdatenbearbeitung, nachvollziehbare Stornos und neue Buchung, Doppelscan-Schutz, detaillierte Importvorschau und sichtbarer Sicherungsstatus. 124 lokale Tests sowie Debug-/Release-Build und Lint erfolgreich. Noch nicht in der unten angebotenen APK enthalten.

## Installation

**[Signierte APK 0.6.1 herunterladen](https://github.com/iONEMond/NLL-Lager/releases/download/v0.6.1/NLL-Lager-0.6.1-release.apk)** · [Releasehinweise](https://github.com/iONEMond/NLL-Lager/releases/tag/v0.6.1) · [SHA-256-Prüfsummen](downloads/SHA256SUMS)

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
