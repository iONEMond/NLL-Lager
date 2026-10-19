# Versionshistorie

Neueste veröffentlichte APK: **0.5.0**. Geplante Funktionen sind ausdrücklich als unveröffentlicht markiert. Die Vorschauen 0.2.0 bis 0.4.0 wurden lokal getestet; ihr Funktionsstand wurde mit 0.4.1 auf GitHub übernommen.

## 0.6.0 – in Entwicklung, nicht veröffentlicht

Lokaler Build: 0.6.0-dev, versionCode 7. Die Mail-Anbindung ist implementiert und lokal mit 57 Tests sowie Build und Lint geprüft; echte Google-Freigabe, Gmail-Abruf und ein leerer Excel-Testexport sind im separaten Emulator geprüft. [Einrichtung und Betriebsablauf](GMAIL-EINRICHTUNG.md). Kein Release-APK-Upload für diesen Entwicklungsstand.

Google-Einrichtung am 19.10.2026: Gmail API, Release-OAuth-Client, Scanner-Testnutzer und Lese-/Sendebereiche konfiguriert. Signierte 0.6.0-dev-Test-APK lokal erstellt und Signatur geprüft. Echte Google-Kontofreigabe und Gmail-Synchronisierung im separaten Emulator erfolgreich. Leerer Excel-Testexport am 19.10.2026 mit Auftragsstatus „Versendet“ bestätigt (0 offene Bewegungen). Empfang des Excel-Berichts vom Betreiber bestätigt. Erster echter Importauftrag wegen zusätzlicher Signaturbilder abgelehnt; Fehlerantwort versendet. Erneuter Material-Mailimport mit Firmensignatur erfolgreich: 1 ergänzt, 0 aktualisiert; Rückmeldung mit Status „Versendet“. Honeywell-Test weiterhin offen.

Korrektur für Firmensignaturen: Zusätzliche Bilder werden beim Mailimport ignoriert und nicht heruntergeladen. Genau eine XLSX-Datei bleibt erforderlich; weitere Nicht-Bild-Dateien werden abgelehnt. Drei zusätzliche Tests für verschachtelte Signaturbilder, Mehrfach-/Fremdanhänge und fehlende Excel-Datei. Korrigierte Test-APK mit bestehendem Release-Schlüssel signiert und Signatur geprüft; erneuter echter Materialimport mit Firmensignatur erfolgreich (1 ergänzt, 0 aktualisiert), Rückmeldung versendet.

Vereinfachte Mailbefehle auf Betreiberwunsch: `NLL Material`, `NLL Personen`, `NLL Export`, ohne Auftragsschlüssel. Ausschließlich die hinterlegte Firmenabsenderadresse wird zugelassen. Groß-/Kleinschreibung und äußere Leerzeichen sind egal, Antwortpräfixe und andere Zusätze bleiben ungültig. Vorhandene Kontofreigabe und Abrufbeginn bleiben erhalten. 57 Tests, Build und Lint erfolgreich; signierter Testbuild mit dieser Änderung erstellt und mit bestehendem Zertifikat geprüft; echter Mailtest mit kurzen Betreffzeilen noch ausstehend.

Feature-Update für genau einen Scanner im privaten Android-Profil, ohne Kommunikation mit dem Arbeitsprofil.

- Eigenes Gmail-Postfach direkt in der Lager-App anbinden; Gmail-App und eigener Vermittlungsserver sind nicht erforderlich. Google-API-/OAuth-Einrichtung ist erforderlich.
- Material- und Personenlisten aus Excel-Mailanhängen prüfen und importieren; vorhandene Datensätze gezielt aktualisieren und neue ergänzen.
- Mailkommando für Excel-Export an ein fest hinterlegtes Firmenpostfach. Die Übernahme in Firmensysteme erfolgt manuell durch den Betreiber.
- Importergebnis und Fehler zurückmelden, doppelte Verarbeitung von Aufträgen verhindern.
- Synchronisierung beim App-Start, manuell und regelmäßig im Hintergrund; Offline-Nutzung bleibt möglich, sofortige Ausführung ist nicht garantiert.
- Verbindung, Empfänger und Synchronisierung im passwortgeschützten Menü verwalten.
- Berichtversand und Abschluss/Archivierung der Bewegungen getrennt behandeln; separate Bestätigung am Scanner nach Übernahme ins Firmenpostfach.

## 0.5.0 – 19.10.2026

- Erste veröffentlichte Test-APK mit dauerhaftem Release-Schlüssel; versionCode 6, Paket `de.nll.lager`, Anzeigename NLL Lager.
- Funktionsstand unverändert gegenüber 0.4.1. Release-Build, Release-Lint und APK-Signatur erfolgreich geprüft; Honeywell-Hardwaretest steht aus.
- Separate Installation zur Vorschau (`de.nll.lager.preview`), keine automatische Datenübernahme. Frühere Installationen mit gleichem Paket und anderem Schlüssel sind nicht direkt aktualisierbar.
- [Signierte APK](downloads/NLL-Lager-0.5.0-release.apk), [Prüfsummen](downloads/SHA256SUMS).

## 0.4.1 – 19.10.2026

- Klickbarer Zurück-Pfeil fest oben links; bestehende Bestätigung beim Verlassen einer Buchung bleibt erhalten.
- Gesammelten Vorschau-Funktionsstand einschließlich geschützter Einstellungen, Lagerzuordnung und Excel-Export auf GitHub veröffentlicht.
- versionCode 5, Paket `de.nll.lager.preview`; 40 lokale Tests bestanden, Build und Lint erfolgreich.
- [Vorschau-APK](downloads/NLL-Lager-0.4.1-vorschau.apk).

## 0.4.0 – lokale Vorschau, 19.10.2026

- Lagerzuordnung beim Anlegen und Bearbeiten von Materialien; als Von Lager vorausgewählt.
- Nach Lager als Dropdown, bei jeder Buchung mit Lagernummer des angemeldeten Mitarbeiters vorbelegt.
- Optionale Importspalten Lager und Lagernummer; bestehende Vorlagen weiterhin verwendbar.
- Excel-Export (.xlsx) zusätzlich zu CSV, einschließlich Archivexport. Archivierung nach erfolgreichem Schreiben und Schließen.
- Datenbankmigration ergänzt Lagerfelder und erhält bestehende Daten; 40 lokale Tests bestanden.

## 0.3.0 – lokale Vorschau, 19.10.2026

- Von Lager als Dropdown; auswählbare Lager im Einstellungsmenü pflegen.
- Passwortgeschützte Einstellungen mit festem Schlosssymbol oben rechts; Freigabe nur im Arbeitsspeicher.
- Im normalen Betrieb ausschließlich Personenanmeldung, Artikelwahl und Material-/Kältemittelbewegungen. Verwaltung, Import, Export und Archiv geschützt.
- 32 lokale Tests bestanden.

## 0.2.0 – lokale Vorschau, 19.10.2026

- Oberfläche an PowerApp-Vorlage angenähert: helle Flächen, blaue Aktionen und getrennte Artikelbereiche für Material und Kältemittel.
- Artikelsuche nach Beschreibung, Materialnummer und Code; Auswahl per Antippen oder Scanner mit Enter.
- Separates Vorschau-Paket `de.nll.lager.preview`; 24 lokale Tests bestanden.

## 0.1.0 – Ausgangsversion

- Offline-Erfassung von Warenbewegungen mit Personen- und Zeitzuordnung, lokalen Stammdaten, Excel-Import, CSV-Export und Archiv.
- [Historische Debug-APK](downloads/NLL-Lager-0.1.0-debug.apk).
