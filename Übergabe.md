# Übergabe an Nachfolge-Agenten

Stand: 20.10.2026. Zuerst [AGENTS.md](AGENTS.md), anschließend diese Datei und den aktuellen [Changelog](CHANGELOG.md) lesen. Versionsangaben und Live-Dienste vor nächsten Aktionen erneut prüfen; diese Datei ist eine Übergabe, kein Ersatz für den aktuellen Repository-/Gerätestand.

## Ziel und Zusammenarbeit

NLL Lager ersetzt die bisherige Lager-PowerApp als native Android-App für einen einzelnen Honeywell EDA52-0. Zuerst wurden Gestaltung und Warenbewegungen übernommen. Der Betreiber kommuniziert auf Deutsch und erwartet praktische Umsetzung, laufend gepflegte Versionshistorie und Abgleich von Mac-Projekt und GitHub. Keine unnötigen erneuten Bestätigungen; vorhandene Autorisierungen beachten. Echte Mailversendungen und Eingriffe in produktive Daten brauchen einen konkreten Auftrag.

Der Scanner nutzt Intune für das Arbeitsprofil. Die App wird im privaten Profil betrieben und soll nicht mit dem Arbeitsprofil kommunizieren. Gmail dient als Transport, weil ein direkter Weg ins Firmensystem fehlt. Listen werden per Mail geliefert; Excel-Berichte übernimmt der Betreiber manuell in die Firmendaten. Keine automatische Anbindung an das Firmen-ERP implementieren oder behaupten. Keine Gmail-App erforderlich.

## Aktueller Versions- und Prüfstand

| Version | Bedeutung |
| --- | --- |
| 0.5.0, Code 6 | Signierter Release; Betreiber meldete am 20.10.2026 erfolgreichen Gerätetest. Keine einzelnen Prüfschritte genannt. |
| 0.6.0, Code 8 | Vorheriger GitHub-Release `v0.6.0`. Gmail und weitere Bedienabläufe. Noch keine umfassende Honeywell-Bestätigung. |
| 0.6.1-dev, Code 9 | Früherer lokaler Entwicklungsstand, keine stabile Veröffentlichung. |
| 0.6.1, Code 10 | Vorheriger Release, **signiert, geprüft und als v0.6.1 veröffentlicht**. Enthält Firmenimport, neuen Export, Bewegungsart, Sicherung, Updater und About. |
| 0.6.2-dev, Code 11 | Unveröffentlichter Zwischenstand, jetzt in 0.7.0 übernommen. |
| 0.7.0-dev, Code 12 | Früherer lokaler Entwicklungsstand. |
| 0.7.1-dev, Code 14 | Unveröffentlichte Weiterentwicklung: Abbruch und zusätzliche Android-Teilen-Option. |
| 0.7.0, Code 13 | Zurückgezogen: Release und Download wegen Mailproblemen gelöscht; Quelltag bleibt erhalten. |
| 0.7.1, Code 15 | Neuester stabiler Release, signiert und öffentlich geprüft. |

Die Rückmeldung „0.5 funktioniert“ gilt nur für 0.5.0. Nicht die veröffentlichte 0.6.0 mit der tatsächlich vom Betreiber installierten Version verwechseln. Die Signierung von 0.6.0 erfolgte nach damaliger lokaler Passworteingabe des Betreibers, nicht ohne Passwort. Am 20.10.2026 hat der Betreiber die Passwörter erneut direkt in Android Studio eingegeben. 0.6.1 wurde damit signiert und mit dem bestehenden Zertifikat geprüft. Veröffentlichung als stabiler Release v0.6.1 über Safari abgeschlossen; anonyme API-/Download- und Signaturprüfung erfolgreich. Passwörter weiterhin unbekannt und nicht gespeichert.

Letzter funktionaler Prüfstand: 124 lokale Tests, Debug-/Release-Build und beide Lint-Prüfungen erfolgreich; Releaseprüfung für 0.7.0 (13) erneut erfolgreich. Keine visuelle Geräteprüfung des About-Menüs. Detaillierter Verlauf und historische Testzahlen: [VALIDIERUNG.md](docs/VALIDIERUNG.md). Dokumentationsumstrukturierung ist kein weiterer Geräte- oder Funktionstest.

## Arbeitsverzeichnis und GitHub

- GitHub: `https://github.com/iONEMond/NLL-Lager`, Standardbranch `main`. Seit 20.10.2026 öffentlich; anonyme Releaseabfrage wurde geprüft.
- Mac-Projekt: `/Users/etzel/chatCodex/Android/NLL Lager/android-app`.
- Übergeordneter Mac-Ordner enthält lokale APK-Kopien, Vorlagen und eine private Schlüsselkopie. Diese Dateien nicht pauschal veröffentlichen oder löschen.
- Bei Erstellung dieser Übergabe ist der lokale Quellordner **kein Git-Checkout**. Nicht blind `git status/push` oder ein neu initialisiertes Repository voraussetzen. Zunächst tatsächlichen Zustand prüfen.
- Bisheriger Abgleich per GitHub-Connector: Branchkopf lesen → `create_tree` mit bisherigem Tree als Basis → `create_commit` mit bisherigem Parent → `update_ref` auf `main` mit `expected_sha`, ohne Force. Bei abweichendem Kopf neu lesen und Änderungen abgleichen. Niemals fremde Änderungen überschreiben.
- Remote-Dateipfade beginnen direkt mit `app/`, `docs/` usw.; es gibt dort keinen zusätzlichen Ordner `android-app/`.
- Neue Release-APKs liegen in GitHub Releases, nicht im Git-Dateibaum. `downloads/` bewahrt alte, bereits veröffentlichte Artefakte; die 0.5.0-APK ist zusätzlich Testdatei.
- `.gitignore` schließt Schlüssel, Builds, IDE-Daten und neue APK-/AAB-Dateien aus. Lokale alte Vorschau-Notizen liegen unter `docs/archiv/` und sind bewusst nicht Teil des öffentlichen Repositories.

## Architektur und Einstiegspunkte

Java, native Android Views; kein Compose, kein Kotlin, keine zusätzliche Serverkomponente. Eine Activity mit programmatischer Oberfläche. [Entwicklung und Befehle](docs/ENTWICKLUNG.md).

| Bereich | Dateien unter `app/src/main/java/de/nll/lager/` |
| --- | --- |
| Oberfläche, Navigation, Passwortdialoge, Dateiauswahl, Google-Autorisierung | `MainActivity.java` |
| SQLite-Schema, Migrationen, Stammdaten und Buchungen | `Store.java` |
| Validierung, exakte Mengen und CSV-Schutz | `Rules.java` |
| Passwortprüfung, Lageroptionen | `AdminSettings.java` |
| XLSX einlesen/schreiben | `ExcelImport.java`, `ExcelExport.java` |
| Gmail-HTTP, Protokoll, Konfiguration | `GmailClient.java`, `MailProtocol.java`, `MailConfig.java` |
| Mailzustände, Snapshots, Versand und Wiederanlauf | `MailStore.java`, `MailSync.java`, `MailJobService.java` |
| Logische Sicherung, SAF-Dateien, Hintergrundarbeit | `BackupData.java`, `Backup.java`, `BackupJobService.java` |
| Releaseabfrage, Download, Signaturprüfung, APK-Zugriff | `AppUpdate.java`, `UpdateFileProvider.java` |

Gradle 8.13, AGP 8.9.2, Java-Sprachziel 17, JDK 21 lokal, API 26–35. `com.google.android.gms:play-services-auth:22.0.0`; kryptografische APK-Prüfung mit `com.android.tools.build:apksig:8.9.2`. Abhängigkeiten nicht ohne Anlass aktualisieren. Paket Haupt-App `de.nll.lager`, Debug `de.nll.lager.preview`, Debug-Anzeigename „NLL Lager · Vorschau“.

## Verbindliche Bedienregeln

- Festes Schloss oben rechts: geschlossen = normaler Betrieb; offen = Einstellungen freigegeben. Zurück als klickbarer Pfeil oben links.
- Normalbetrieb: Personenanmeldung und Material-/Kältemittelbewegungen. Stammdaten, Personenverwaltung, Listen, Import/Export, Archiv, Mail, Sicherung, Updates und About sind geschützt.
- Kein Standardpasswort. Ab 0.6.2-dev Einrichtung mit vierstelliger numerischer PIN oder mindestens acht Zeichen langem Passwort; vorhandene Passwörter bleiben gültig, gesalzener PBKDF2-HMAC-SHA256-Hash mit 120.000 Iterationen. Freigabe nur im RAM; Hintergrundwechsel/Neustart sperrt. Bereits autorisierte Dateidialoge/Schreibvorgänge haben begrenzte Ausnahmen; Prozessverlust autorisiert nichts erneut.
- Schutz auch direkt vor schreibenden Dialog- und Callback-Aktionen prüfen. `isAdminScreen()` und `requireAdmin()` bei neuen Menüpunkten beachten.
- Unbekannter Scan im Normalbetrieb: Nach App-Gerätepasswort darf genau dieser Artikel mit bereits gescanntem Code und passender Artikelart angelegt werden. Anschließend direkt buchen und wieder sperren. Abbruch sperrt ebenfalls.
- Materiallager liefert den Standard für „Von Lager“. „Nach Lager“ ist Dropdown und beginnt bei jeder neuen Buchung mit der Lagernummer des aktuell angemeldeten Mitarbeiters.
- Ab 0.6.1 Pflichtauswahl Entnahme/Rückgabe/Umlagerung. Historische Bewegungen behalten eine leere Bewegungsart; keine nachträgliche Vermutung.
- Personen-Code dient der Zuordnung, nicht einer individuellen starken Authentisierung.
- Keine Bestandsberechnung, Werkzeugverwaltung, Nachbestellungen, Warenkorb oder getrennte Flaschen-/kg-Erfassung vorhanden.

## Ergänzte Bedienabläufe in 0.6.2-dev

`WorkProgress` ist der callbackbasierte Fortschritt für Parser, Store, Excel-/CSV-Ausgabe und MailSync. `OperationProgress` zeigt einen horizontalen Balken samt Phase; unbekannte Dauer bleibt unbestimmt. Anzeige lebt nur in der Activity, Daten-/Mailtransaktionen dürfen unabhängig davon sauber fertig werden. Keine geschätzten globalen Prozentwerte als Beweis einer abgeschlossenen Übernahme verwenden. Bestehende Overloads ohne Callback bleiben für Hintergrundjobs und Tests verfügbar.

Sync-Einrichtung bei `config.ready()` zunächst eingeklappt, über Verbindung bearbeiten erweiterbar. Fehlende Verbindung zeigt wieder Setup/Google-Autorisierung. Personen/Material haben im Lagerzuordnungsdialog eine Löschaktion mit erneuter Adminprüfung und Schutz vor gleichzeitiger Mailverarbeitung. `Store.deleteEntry` löscht ausschließlich Stammdaten; historische Snapshots bleiben unverändert. Bei Löschung aktiver Person/Artikel entfernt die UI deren aktuelle Zuordnung.

## Ergänzte Funktionen in 0.7.0

- `Store.updatePerson/updateMaterial` bearbeitet auch Primärcodes. Kollisionen werden vom eindeutigen Schlüssel abgewiesen; frühere Buchungen behalten Snapshots. UI verwirft/aktualisiert betroffene aktive Auswahl, sperrt parallele Mailverarbeitung und prüft Freigabe erneut.
- `Store.cancelMovement` erzeugt eine neue offene Gegenbuchung, verändert das Original nicht. Menge bleibt positiv, Lager werden vertauscht, Bewegungsart ist Storno. `reversal_of` (eindeutiger Index) verweist auf Original; `cancellation_reason` und `cancelled_by` dokumentieren Grund/Akteur. Bei nicht angemeldeter Person Akteur „Administration“. Keine individuelle starke Authentisierung behaupten. Storno eines Stornos und zweites Storno desselben Originals sind gesperrt. Auch für archivierte/versandte Originale wird nur eine neue offene Gegenbuchung erzeugt; gespeicherte Mailberichte bleiben unverändert. Acht Exportspalten unverändert, neue Stornozeilen müssen von Materialwirtschaft berücksichtigt werden.
- „Stornieren & neu erfassen“ speichert zunächst das Storno, dann einen ungespeicherten Buchungsentwurf mit damaliger Menge/Lagern/Art für separate Bestätigung. Fehlende Stammdaten werden nicht automatisch wieder angelegt. Original und Storno dürfen niemals gelöscht werden, um einen falschen Korrekturversuch zu kaschieren.
- `Store.recentDuplicate/bookChecked` prüft letzte nicht stornierte Normalbuchung, identische Person/Code/Menge/Lager/Bewegungsart innerhalb zehn Sekunden. Bestätigung und Schreiben sind transaktional abgesichert. UI fängt Mehrfachklicks ab und bindet den bestätigten Artikel/Mitarbeiter als Snapshot. Kein globaler Dublettendetektor und keine zeitlose Einmalbuchung.
- `ImportPreview` liest konsistent und schreibfrei, liefert Anzahlen plus feldweise Unterschiede. Spaltenpräsenz aus ExcelImport erhalten! Detaildialog blättert mit 20 Zeilen. Datenrevision wird in der Vorschau festgehalten; `Store.importRows(...expectedRevision)` prüft sie innerhalb der Übernahmetransaktion. Bei Abweichung neue Vorschau und erneute ausdrückliche Übernahme, kein stilles Fortfahren. Bestehende Overloads bleiben für Tests/interne strikt definierte Aufrufe.
- Revisions-Trigger zählen INSERT/UPDATE/DELETE auf Stammdaten/Bewegungen und Lageroptionen (nur app_settings.key=locations). Schlüssel `database_id` und `data_revision` gehören nicht in die Geschäftssicherung. Snapshot nimmt Revision und Kennung innerhalb derselben DB-Transaktion auf; erfolgreiche Sicherung merkt sich genau diese Werte. Spätere Änderungen während Dateischreiben bleiben ungesichert; Rollbacks zählen nicht. `Backup.statusLine` ist ein Vergleich zum letzten erfolgreichen Snapshot, keine erneute Live-Dateiprüfung. Ordnerwechsel setzt alten Status zurück.

## Datenbank und Migrationen

SQLite `nll-lager.db`, aktuell Version **7**. Geschäftsdaten bleiben im privaten App-Speicher; keine direkt gemeinsam genutzte Arbeitsdatenbank. Bewegungen enthalten historische Snapshots von Artikel, Mitarbeiter und Lagern; Stammdatenupdates verändern diese nicht.

| DB-Version | Ergänzung |
| --- | --- |
| 2 | Materiallager und Mitarbeiter-Lagernummer |
| 3 | Mailaufträge und Zustandsverwaltung |
| 4 | Dauerhafte automatische Archivierung nach bestätigtem Mailversand |
| 5 | `movement_kind`, Altdaten leer |
| 6 | `app_settings` für Lageroptionen in derselben Transaktion wie Geschäftsdaten |
| 7 | Storno-Bezug/-Grund/-Akteur, eindeutiger Gegenbuchungsindex, lokale Datenbankkennung und transaktionale Revisions-Trigger |

Alte Lagereinstellungen aus Preferences bleiben als Fallback lesbar. Passwörter und Gmail-Konfiguration bleiben außerhalb der Geschäftstabellen. Migrationen müssen alte Stammdaten, offene Buchungen und Archive erhalten. Keine Deinstallation, kein Datenbankreset als Problemlösung. Mengen werden exakt als Dezimalwerte behandelt, deutsche Kommaeingabe wird unterstützt. Codes und Lagernummern als Text erhalten, insbesondere führende Nullen.

## Import und Berichte

[Bedienung und Dateiformate](docs/BEDIENUNG.md) sowie `ExcelImport`/`ExcelExport` sind maßgeblich.

- Firmenliste: `Title` → Beschreibung, `Code` → Scan- und Materialnummer; Art Material, Einheit Stk. `Typ`, `Bemerkung`, `BST Menge` ignorieren. „BST Menge“ meint Bestellung, keine Bestands- oder Bewegungsübernahme. Optionale Lager-Spalte. Bisher nur Foto der Originaldatei erhalten; Tests verwenden nachgebildete XLSX-Dateien.
- Alte Material-/Personenvorlagen bleiben kompatibel. Echte XLSX, erste Tabelle, Header in erster nichtleerer Zeile. Kein XLS/XLSM, Passwortschutz oder Formeln. Maximal 20 MB/10.000 Einträge/200 Zeichen je Feld.
- Ab 0.6.2-dev fragt der Dateiimport bei vorhandenen Codes nach Aktualisieren/Überspringen/Abbrechen; Transaktion komplett oder gar nicht. Neue Codes werden ergänzt, Mehrfachcodes innerhalb der Datei bleiben ungültig. Der Standard-Store-Aufruf bleibt intern strikt; die UI übergibt nach Auswahl ImportPolicy. Mailimport ergänzt/aktualisiert und behält nicht enthaltene Einträge. Ab 0.6.2-dev sind nur Materialcode und Beschreibung Pflicht; Nummer/Art/Einheit/Lager optional. Fehlende Materialnummer/Art/Einheit werden bei Neuanlage mit Code/Material/Stk vorbelegt; bei Aktualisierung bleiben nicht mitgelieferte Spalten erhalten. ExcelImport liefert dafür MappedRows mit Spaltenpräsenz – nicht unbedacht in eine normale ArrayList kopieren, sonst geht diese Information verloren. Fehlende Lager-Spalte erhält beim Mailupdate die bisherige Zuordnung; explizit leere Lager-Zelle leert sie.
- Export ab 0.6.1, genau diese Reihenfolge: **Code; Beschreibung; Menge; Bewegungsart; Mitarbeiter; Datum; Lagerort (Von -> Nach); Buchung erfolgt?** Letzte Spalte leer für Materialwirtschaft. Datum enthält Uhrzeit. Keine IDs/Einheiten zusätzlich einschieben.
- XLSX typisiert Mengen und Datum, Codes als Text; Filter und fixierte Kopfzeile. CSV UTF-8-BOM, Semikolon, Dezimalkomma, zitierte Textfelder mit Schutz gegen Excel-Formelausführung.
- Snapshot des Berichts festhalten. Nur nach erfolgreichem Dateischreiben und Schließen archivieren; Abbruch/Fehler lässt Bewegungen offen. Spätere Buchungen gehören nicht in den Abschluss. Archivexport bleibt möglich.

## Gmail: Besonderheiten unbedingt erhalten

[Einrichtung](docs/GMAIL-EINRICHTUNG.md). Der Betreiber hat ein dediziertes Scanner-Gmail-Konto und ein festes Firmenpostfach angegeben; Adressen werden in der App eingerichtet und müssen nicht in öffentlichen Beispielen stehen. Keine Passwörter oder Tokens sammeln. Google-Cloud-Projekt bei Einrichtung: `action-potential-512119-r1`. Gmail API aktiviert, externe Auth-App „NLL Lager“ im Teststatus, Scannerkonto als Testnutzer. Release-OAuth-Client passt zu Paket und SHA-1 unten. Für Vorschau ist kein eigener OAuth-Client eingerichtet.

Google-Play-Dienste verwalten Tokens; die App speichert sie nicht. Beide Scopes `gmail.readonly` und `gmail.send` erforderlich. Konto muss zur konfigurierten Adresse passen. Im Testmodus können Freigaben nach sieben Tagen ablaufen; Dauerbetrieb/OAuth-Status ist offen. Vor Änderungen aktuelle Google-Vorgaben prüfen.

- Betreff genau `NLL Material`, `NLL Personen`, `NLL Export`; Groß-/Kleinschreibung und äußere Leerzeichen egal, kein `Re:` oder Zusatz.
- Betreiber entschied ausdrücklich für zugelassene Firmenabsenderadresse **ohne zusätzlichen Auftragsschlüssel**. `From`-Prüfung ist keine kryptografische Authentisierung. Diese Entscheidung nicht stillschweigend ändern oder als stärker abgesichert beschreiben.
- Antworten ausschließlich an konfiguriertes Firmenpostfach, nicht Reply-To/zusätzliche Empfänger. Unbekannte Mails ignorieren.
- Nur Posteingang und nur Nachrichten nach Einrichtung/Adresswechsel. Begrenzter Abruf, dauerhaftes Deduplizieren über Mail-ID. Alte Aufträge nach Wiederherstellung nicht erneut ausführen.
- Genau eine XLSX-Datei; verschachtelte Signaturbilder ignorieren und nicht laden. Andere Anhänge/mehrere XLSX ablehnen. Kein lockerer Fallback bei unerwartetem MIME-Inhalt.
- Durable Outbox: fertige EML im privaten App-Speicher; Zustände u. a. pending/sending/sent/uncertain/archived. Bei unklarem Versand keine automatische Wiederholung und keine Archivierung. Betreiber prüft Postfach, bestätigt Empfang oder wiederholt bewusst denselben Bericht.
- Mailkommando und Versand im Mail-Menü: separate Übernahmebestätigung archiviert. **Bericht exportieren → Per Mail senden & archivieren**: nach bestätigter Gmail-Annahme automatisch exakt den Snapshot archivieren. Versand ist keine Zustellbestätigung. Wiederanlauf muss Archivierung nachholen können, ohne erneut zu senden.
- `MailSync.BUSY` schützt konkurrierende Mailverarbeitung; bei Wiederherstellung/Updatevorbereitung mit berücksichtigen.
- JobScheduler etwa alle 15 Minuten mit Netz, Android darf verzögern. Kein Echtzeitversprechen. Keine automatische Bereinigung alter Outbox-Dateien implementiert.

Echt geprüft im separaten Emulator: Google-Anmeldung, Kontofreigabe, Abruf, leerer Excel-Versand und Materialimport mit Firmensignatur. Empfang vom Betreiber bestätigt. Nicht alle weiteren Mailpfade sind echt geprüft.

## Sicherung und Wiederherstellung

[Anleitung](docs/SICHERUNG.md). SAF-Dokumentenordner mit persistierter Lese-/Schreibfreigabe. Versioniertes logisches JSON `NLL-Lager-Sicherung`, ab 0.7.0 Formatversion 2 mit 17 Bewegungsfeldern (vorher 14), SHA-256 über Nutzdaten. Format 1 bleibt lesbar: ursprünglichen Hash zuerst prüfen, danach Metadaten ergänzen. Format 2 erfordert neue App; nicht auf 0.6.1 zurückwechseln. Keine rohe SQLite-Kopie. Enthält Materialien, Personen, Lageroptionen und offene/archivierte Bewegungen mit IDs und Snapshots. Maximal 64 MB/100.000 Zeilen je Geschäftstabelle.

Keine Passwörter, Tokens, Kontofreigaben oder Mailwarteschlangen. Nicht verschlüsselt; Hash erkennt Beschädigung und ist kein Manipulationsschutz. Schreiben als `.partial`, schließen, zurücklesen/prüfen, erst dann `.nllbackup`. Nach erfolgreichem neuen Stand sieben gültige Stände behalten; Fremddateien nicht löschen. Android kann geplante Sicherungen verzögern, deshalb für kritische Schritte ausdrücklich synchron sichern.

Wiederherstellung ersetzt Geschäftsdaten samt Lageroptionen vollständig in einer Transaktion. **Vorher zwingend geprüfte Sicherheitskopie des aktuellen Stands**, auch wenn die aktuelle DB leer ist. Währenddessen keine Mailverarbeitung. Mailaufträge löschen, Gmail trennen und Abrufbeginn neu setzen, damit alte Aufträge nicht wieder ausgeführt werden. Bei Fehler können Geschäftsdaten zurückgerollt und Gmail trotzdem bereits getrennt sein. Danach erneute Anmeldung; Gmail/Ordnerfreigaben nach Neuinstallation neu einrichten. Sicherungen auf demselben Gerät schützen nicht gegen Geräteverlust.

## Signierung und Updates

Bestehenden Release-Schlüssel verwenden. Lokaler Originalpfad `/Users/etzel/.android/nll-lager-release.jks`, zusätzliche private Kopie im übergeordneten Projektordner; Alias `nll-lager-release`. Passwort unbekannt und absichtlich nicht gespeichert. Private Schlüssel weder ausgeben noch hochladen.

- Release-Zertifikat SHA-256: `1d70da7786708b6dcd04319235fc76ca4d411f12474a63bc98852fc9b5fa25c0`
- Release-Zertifikat SHA-1 für Google: `2F:5C:DD:F8:C5:9B:93:33:AF:0D:A3:19:C6:97:00:6C:16:6E:A9:CC`
- Diese Fingerabdrücke sind öffentliches Zertifikatsmaterial, keine privaten Schlüssel.

[Release-Ablauf](docs/ENTWICKLUNG.md) und [Updater-Vertrag](docs/UPDATES.md) beachten. Anonyme Abfrage `https://api.github.com/repos/iONEMond/NLL-Lager/releases/latest`. Nur stabile Tags `vX.Y.Z`, genau ein Asset `NLL-Lager-X.Y.Z-release.apk`, GitHub-`digest` mit SHA-256. Kein GitHub-Token in der App.

Vollständige APK-Signatur mit apksig gegen fest gepinntes Zertifikat prüfen; Hash allein genügt nicht. Zusätzlich Paket, Versionsname, höherer versionCode und Android-Kompatibilität prüfen. HTTPS/Weiterleitungen nur zu fest erlaubten GitHub-Hosts, Größen-/Zeitlimits. Download begrenzt 64 MB, Teil-Dateien bei Fehler entfernen. Nur die betreffende APK über schreibgeschützten ContentProvider freigeben.

Vor Installerstart erneut verifizieren und vollständige Sicherung durchführen. Android-Installationsfreigabe und Installationsbestätigung bleiben beim Nutzer. Gerät/Intune kann das verhindern. Vorschau darf Haupt-App nicht aktualisieren. Keine Downgrades, Deinstallation, stille Installation oder Schlüsselwechsel. 0.5/0.6.0 enthalten keinen Updater; erste updaterfähige Version einmal manuell installieren.

## Lokale Werkzeuge und Testgeräte

Bisheriger Mac: SDK `/Users/etzel/Library/Android/sdk`, JDK `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`. Nicht als portable Voraussetzungen hardcoden. Kein `gh` eingerichtet; Connector bzw. Browser für GitHub. Für UI-Aktionen vorhandene Computer-Use-Werkzeuge benutzen; nicht mit AppleScript oder adb-Tastatureingaben umgehen.

Zwei Emulatoren waren vorhanden: „Medium Phone API 37.0“ mit älteren Installationen und „NLL Gmail Test“ (`NLL_Gmail_Test`) für die korrekt signierte Haupt-App. Der separate Emulator vermeidet Signaturkonflikte; ältere Instanzen nicht deinstallieren/zurücksetzen. Seriennummern können sich ändern, vor adb-Aktionen Geräte neu identifizieren. Nicht annehmen, dass letzter Quellstand bereits auf Emulator oder Scanner installiert ist. Android-Studio-Passwortdialog dem Betreiber für direkte Eingabe überlassen; „Remember passwords“ bisher deaktiviert.

## Nächste Aufgaben und bekannte Grenzen

1. Veröffentlichung 0.7.0 abgeschlossen: https://github.com/iONEMond/NLL-Lager/releases/tag/v0.7.0. APK und SHA256SUMS hochgeladen; anonymer Download und Signatur geprüft. APK lokal unter downloads/NLL-Lager-0.7.0-release.apk und im übergeordneten Projektordner. SHA-256 1f85c71d34653bff723e1bcf26b1b9b7dccaf17063f1c5d1f9d71c0d972f35eb, Größe 2.438.752 Bytes. Nächster Schritt: Honeywell-Test. Neue Sicherungen verwenden Format 2, alte Format-1-Sicherungen bleiben lesbar; kein Downgrade auf 0.6.1 mit neuen Sicherungen.
2. Auf Honeywell Update von 0.5.0 mit Datenerhalt, Scanner-Eingabe, Passwortschutz und unbekannte Artikel testen.
3. Echte Firmen-XLSX statt Foto prüfen; neues Berichtslayout und Pflichtbewegungsart mit Betreiber bestätigen.
4. Sicherungsordner, Wiederherstellung, Android-Installationsfreigabe und späteres In-App-Update auf echter Hardware testen.
5. Personen-/Aktualisierungsimport, Bericht mit echten Bewegungen, beide Archivierungswege, Neustart, Offlinebetrieb, Hintergrundabruf und unklaren Versand testen.
6. OAuth-Teststatus/Dauerbetrieb klären. Keine Zusicherung unbegrenzter Gmail-Freigabe oder garantierter Joblaufzeiten.
7. Weiterhin keine automatische Bestandsberechnung und kein Zugriff auf das Intune-Arbeitsprofil. Kein Lizenztext wurde vom Betreiber festgelegt; öffentliche Sichtbarkeit nicht als zusätzliche Lizenzzusage auslegen.

Neue Entscheidungen, Prüfungen und Releasezustände in den zuständigen Dokumenten und dieser Übergabe nachführen. Historische Prüfprotokolle nicht nachträglich zu erfolgreichen Hardwaretests umschreiben.

## Weiterentwicklung 0.7.1-dev: Abbruch und externer Mailversand

Betreiber meldet in 0.7.0 dauerhaft offenen Fortschrittsdialog beim Direktversand auf dem Scanner. Ursache auf Hardware noch offen; nicht als behobenen Google-/Kontofehler ausgeben. Zusätzliches Teilen ausdrücklich gewünscht, bestehende Gmail-Funktionen bleiben erhalten.

OperationControl mit kooperativem Cancellation-Token, Worker-Interrupt, HTTP-disconnect-Callback und atomarer nicht mehr abbrechbarer Commit-Grenze. OperationProgress bietet Abbrechen; manuelle Mailvorgänge zusätzlich 120 Sekunden Gesamtzeitlimit. UI bleibt bis Workerabschluss gesperrt. Lokaler Import rollt vollständig zurück, Multi-Auftrag-Sync behält bereits abgeschlossene Aufträge. Ausgehende Übertragungsabbrüche konservativ uncertain, keine Wiederholung/Archivierung. Lokaler pending-Export bei Abbruch cancelled; BUSY durchgehend über Vorbereitung, Versand und Abbruchbereinigung halten. MailSync.runOwned nur bei gehaltener BUSY-Sperre verwenden.

SharedReport schreibt unveränderliche XLSX nach files/shared-reports, ReportFileProvider gibt nur passende Einzelnamen lesend frei. ACTION_SEND mit ClipData/EXTRA_STREAM, MIME und FLAG_GRANT_READ_URI_PERMISSION; keine Zugangsdaten oder Versandbestätigung der Mail-App. pending in separaten SharedPreferences shared-report persistiert Datei, DB-Identität, einzelne Bewegungs-IDs und SHA-256 der historischen Felder (Archivstatus ausgenommen). Erst ausdrückliche Bestätigung unter Bericht exportieren archiviert diese IDs nach vollständiger Inhaltsprüfung in einer Transaktion. Ein späterer Restore mit abweichenden IDs/Inhalten verhindert Archivierung. Spätere Buchungen bleiben offen. Vorhandene Mail-Ausgangszustände werden nicht in den externen Bericht übernommen; vor manuellem Ersatzversand unklare frühere Sendungen im Postfach prüfen.

Derzeit keine neue signierte Release-APK, kein weiterer Geräte-/Mailtest. Öffentlicher Release weiterhin v0.7.0.

Prüfstand 0.7.1-dev: 136 lokale Tests, Debug-/Release-Build und beide Lint-Prüfungen erfolgreich. Vorschau-APK lokal NLL-Lager-0.7.1-dev-vorschau.apk im übergeordneten Projektordner; eigene Vorschau-Daten, kein Update der Haupt-App.

Ergänzung nach Scanner-Rückmeldung: 0.6.1 funktionierte laut Betreiber, unter 0.7.0 dauerhaft „Hintergrundverarbeitung läuft“. Das ist eine BUSY-Anzeige, kein Nachweis für abgelehnte Google-Authentifizierung. Vergleich der veröffentlichten MailSync-Versionen: Google-Autorisierung/SHA-Zuordnung wurde nicht neu umgestellt; beide besaßen kein Gesamtzeitlimit. Die Hintergrundanzeige kam mit der Fortschrittsänderung hinzu und blieb nach Workerabschluss ohne erneutes Rendern sichtbar. Tatsächliche Ursache eines möglicherweise noch blockierten Workers auf Hardware bleibt offen.

Korrektur: MailSync.RunGuard registriert den aktiven Mail-Cancellation-Token, begrenzt automatische Läufe auf 120 Sekunden und entfernt Referenz/Timer beim Verlassen. BUSY wird weiterhin ausschließlich vom Besitzer nach Workerabschluss freigegeben; Abbruch setzt es niemals blind zurück. Mail-Menü zeigt Phase und bietet nur für registrierte Mailläufe Abbrechen (keinen Abbruch fremder Import-/Restore-Sperren). Handler aktualisiert Menü nur bei geänderten Werten, beendet Beobachtung beim Verlassen/Stop/Destroy. MailJobService.onStopJob bricht eigenen Token statt einen möglicherweise fremden Lauf ab; unterdrückt jobFinished nach Jobabbruch.

Aktueller Prüfstand einschließlich Hintergrundkorrektur: 140 lokale Tests ohne Fehler, Debug-/Release-Build und beide Lint-Prüfungen erfolgreich. Lokale Vorschau-APK erneut aktualisiert. Kein neuer Honeywell-/Google-Mailtest; öffentlicher Release unverändert v0.7.0.

Release-Kandidat 0.7.1, Code 15: Betreiber hat Übernahme auf main, Veröffentlichung und Entfernen des fehlerhaften Releases 0.7.0 ausdrücklich angefordert. Korrektur auf main übernommen. 140 Tests, Release-Build und Release-Lint erneut erfolgreich. Signierte 0.7.1 mit bestehendem Zertifikat geprüft, Größe 2.444.688 Bytes, SHA-256 44b52378981eb0acd013e16dc91f111014aa797bc4c7d903b26565a4d3cd7aca. Android Studio hatte Passwortfelder vorausgefüllt; Remember passwords war aktiviert und wurde vor dem Signieren deaktiviert. Passwörter nicht ausgelesen. Veröffentlichung und Rücknahme noch ausstehend.

0.7.1 (Code 15) als stabiler Release v0.7.1 veröffentlicht: https://github.com/iONEMond/NLL-Lager/releases/tag/v0.7.1. Anonyme Latest-API bestätigt v0.7.1, draft=false, prerelease=false; öffentlicher APK-Download und Signatur erneut geprüft. SHA256SUMS 657 Bytes, SHA-256 269e9e7bf1332404caf76ceb55ebc94a7b57726393199e5a7199f632fac50eb3. Release 0.7.0 nach ausdrücklicher Bestätigung gelöscht: API /releases/tags/v0.7.0 und alter APK-Download liefern anonym 404. Latest bleibt v0.7.1. Git-Tag und Quellcode-Historie erhalten; lokale alte APK nur als historisches Artefakt, nicht anbieten.

## 0.7.2-dev: lokale Exportaufträge

Betreiber meldet nach 0.7.1: Altauftrag wurde versendet, neue Direktversandanforderungen aus beiden Menüs fehlen unter Letzte Aufträge. Hardwarestatus/Fehlertext noch nicht vorliegend. Reproduziert: synthetischer lokaler Export wurde vom Eingangsmail-since-Filter still verworfen, wenn Gerätezeit unter gespeicherter Abrufgrenze liegt. Nicht als nachgewiesene Scannerursache ausgeben.

MailSync.prepareLocalExport erstellt die feste lokale Exportanforderung und ruft privaten vorbereitenden Pfad mit localExport=true auf. Nur dieser Pfad umgeht since; echte Mails bleiben vollständig gefiltert. Lokale IDs müssen local-UUID entsprechen; Dublette wird abgelehnt, nach Vorbereitung muss genau diese Kennung gespeichert sein. Bei lokalen Vorbereitungsfehlern keine Fehlerantwort in Outbox als Ersatz für Bericht; Exception geht zur UI. MainActivity zeigt konkrete Vorbereitungsursache in Mailstatus und notice, bereits gespeicherte Aufträge bleiben dauerhaft. BUSY-, Timeout-, Abbruch- und Archivierungsregeln unverändert. Code 16, 0.7.2-dev, noch keine neue signierte Veröffentlichung.

Prüfstand 0.7.2-dev: 145 lokale Tests ohne Fehler, Debug-/Release-Build und beide Lint-Prüfungen erfolgreich. Lokale Vorschau im übergeordneten Projektordner NLL-Lager-0.7.2-dev-vorschau.apk, eigene Daten und kein Haupt-App-Update.
