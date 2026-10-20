# Übergabe an Nachfolge-Agenten

Stand: 20.10.2026. Zuerst [AGENTS.md](AGENTS.md), anschließend diese Datei und den aktuellen [Changelog](CHANGELOG.md) lesen. Versionsangaben und Live-Dienste vor nächsten Aktionen erneut prüfen; diese Datei ist eine Übergabe, kein Ersatz für den aktuellen Repository-/Gerätestand.

## Ziel und Zusammenarbeit

NLL Lager ersetzt die bisherige Lager-PowerApp als native Android-App für einen einzelnen Honeywell EDA52-0. Zuerst wurden Gestaltung und Warenbewegungen übernommen. Der Betreiber kommuniziert auf Deutsch und erwartet praktische Umsetzung, laufend gepflegte Versionshistorie und Abgleich von Mac-Projekt und GitHub. Keine unnötigen erneuten Bestätigungen; vorhandene Autorisierungen beachten. Echte Mailversendungen und Eingriffe in produktive Daten brauchen einen konkreten Auftrag.

Der Scanner nutzt Intune für das Arbeitsprofil. Die App wird im privaten Profil betrieben und soll nicht mit dem Arbeitsprofil kommunizieren. Gmail dient als Transport, weil ein direkter Weg ins Firmensystem fehlt. Listen werden per Mail geliefert; Excel-Berichte übernimmt der Betreiber manuell in die Firmendaten. Keine automatische Anbindung an das Firmen-ERP implementieren oder behaupten. Keine Gmail-App erforderlich.

## Aktueller Versions- und Prüfstand

| Version | Bedeutung |
| --- | --- |
| 0.5.0, Code 6 | Signierter Release; Betreiber meldete am 20.10.2026 erfolgreichen Gerätetest. Keine einzelnen Prüfschritte genannt. |
| 0.6.0, Code 8 | Neuester veröffentlichter GitHub-Release `v0.6.0`. Gmail und weitere Bedienabläufe. Noch keine umfassende Honeywell-Bestätigung. |
| 0.6.1-dev, Code 9 | Früherer lokaler Entwicklungsstand, keine stabile Veröffentlichung. |
| 0.6.1, Code 10 | Aktuell vorbereiteter Quellstand, **nicht signiert und nicht veröffentlicht**. Enthält Firmenimport, neuen Export, Bewegungsart, Sicherung, Updater und About. |

Die Rückmeldung „0.5 funktioniert“ gilt nur für 0.5.0. Nicht die veröffentlichte 0.6.0 mit der tatsächlich vom Betreiber installierten Version verwechseln. Die Signierung von 0.6.0 erfolgte nach damaliger lokaler Passworteingabe des Betreibers, nicht ohne Passwort. Für 0.6.1 verlangt Android Studio beide Passwörter erneut. Der Betreiber hat aktuell keinen Zugriff auf Android Studio; Veröffentlichung bis zur Signierung offen lassen.

Letzter funktionaler Prüfstand: 97 lokale Tests, Release-Build und Release-Lint erfolgreich, einschließlich nachträglicher About-Ergänzung. Keine visuelle Geräteprüfung des About-Menüs. Detaillierter Verlauf und historische Testzahlen: [VALIDIERUNG.md](docs/VALIDIERUNG.md). Dokumentationsumstrukturierung ist kein weiterer Geräte- oder Funktionstest.

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
- Kein Standardpasswort. Einrichtung mindestens acht Zeichen, gesalzener PBKDF2-HMAC-SHA256-Hash mit 120.000 Iterationen. Freigabe nur im RAM; Hintergrundwechsel/Neustart sperrt. Bereits autorisierte Dateidialoge/Schreibvorgänge haben begrenzte Ausnahmen; Prozessverlust autorisiert nichts erneut.
- Schutz auch direkt vor schreibenden Dialog- und Callback-Aktionen prüfen. `isAdminScreen()` und `requireAdmin()` bei neuen Menüpunkten beachten.
- Unbekannter Scan im Normalbetrieb: Nach App-Gerätepasswort darf genau dieser Artikel mit bereits gescanntem Code und passender Artikelart angelegt werden. Anschließend direkt buchen und wieder sperren. Abbruch sperrt ebenfalls.
- Materiallager liefert den Standard für „Von Lager“. „Nach Lager“ ist Dropdown und beginnt bei jeder neuen Buchung mit der Lagernummer des aktuell angemeldeten Mitarbeiters.
- Ab 0.6.1 Pflichtauswahl Entnahme/Rückgabe/Umlagerung. Historische Bewegungen behalten eine leere Bewegungsart; keine nachträgliche Vermutung.
- Personen-Code dient der Zuordnung, nicht einer individuellen starken Authentisierung.
- Keine Bestandsberechnung, Werkzeugverwaltung, Nachbestellungen, Warenkorb oder getrennte Flaschen-/kg-Erfassung vorhanden.

## Datenbank und Migrationen

SQLite `nll-lager.db`, aktuell Version **6**. Geschäftsdaten bleiben im privaten App-Speicher; keine direkt gemeinsam genutzte Arbeitsdatenbank. Bewegungen enthalten historische Snapshots von Artikel, Mitarbeiter und Lagern; Stammdatenupdates verändern diese nicht.

| DB-Version | Ergänzung |
| --- | --- |
| 2 | Materiallager und Mitarbeiter-Lagernummer |
| 3 | Mailaufträge und Zustandsverwaltung |
| 4 | Dauerhafte automatische Archivierung nach bestätigtem Mailversand |
| 5 | `movement_kind`, Altdaten leer |
| 6 | `app_settings` für Lageroptionen in derselben Transaktion wie Geschäftsdaten |

Alte Lagereinstellungen aus Preferences bleiben als Fallback lesbar. Passwörter und Gmail-Konfiguration bleiben außerhalb der Geschäftstabellen. Migrationen müssen alte Stammdaten, offene Buchungen und Archive erhalten. Keine Deinstallation, kein Datenbankreset als Problemlösung. Mengen werden exakt als Dezimalwerte behandelt, deutsche Kommaeingabe wird unterstützt. Codes und Lagernummern als Text erhalten, insbesondere führende Nullen.

## Import und Berichte

[Bedienung und Dateiformate](docs/BEDIENUNG.md) sowie `ExcelImport`/`ExcelExport` sind maßgeblich.

- Firmenliste: `Title` → Beschreibung, `Code` → Scan- und Materialnummer; Art Material, Einheit Stk. `Typ`, `Bemerkung`, `BST Menge` ignorieren. „BST Menge“ meint Bestellung, keine Bestands- oder Bewegungsübernahme. Optionale Lager-Spalte. Bisher nur Foto der Originaldatei erhalten; Tests verwenden nachgebildete XLSX-Dateien.
- Alte Material-/Personenvorlagen bleiben kompatibel. Echte XLSX, erste Tabelle, Header in erster nichtleerer Zeile. Kein XLS/XLSM, Passwortschutz oder Formeln. Maximal 20 MB/10.000 Einträge/200 Zeichen je Feld.
- Lokaler Dateiimport lehnt bereits vorhandene Codes ab; Transaktion komplett oder gar nicht. Mailimport ergänzt/aktualisiert und behält nicht enthaltene Einträge. Fehlende Lager-Spalte erhält beim Mailupdate die bisherige Zuordnung; explizit leere Lager-Zelle leert sie.
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

[Anleitung](docs/SICHERUNG.md). SAF-Dokumentenordner mit persistierter Lese-/Schreibfreigabe. Versioniertes logisches JSON `NLL-Lager-Sicherung`, Formatversion 1, SHA-256 über Nutzdaten. Keine rohe SQLite-Kopie. Enthält Materialien, Personen, Lageroptionen und offene/archivierte Bewegungen mit IDs und Snapshots. Maximal 64 MB/100.000 Zeilen je Geschäftstabelle.

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

1. Nach erneutem Betreiberzugriff 0.6.1 mit vorhandenem Schlüssel signieren, Signatur/Metadaten/Prüfsummen prüfen, stabilen Release veröffentlichen und Download anonym prüfen. Keine Fertigmeldung vor erfolgreichem Asset-Upload.
2. Auf Honeywell Update von 0.5.0 mit Datenerhalt, Scanner-Eingabe, Passwortschutz und unbekannte Artikel testen.
3. Echte Firmen-XLSX statt Foto prüfen; neues Berichtslayout und Pflichtbewegungsart mit Betreiber bestätigen.
4. Sicherungsordner, Wiederherstellung, Android-Installationsfreigabe und späteres In-App-Update auf echter Hardware testen.
5. Personen-/Aktualisierungsimport, Bericht mit echten Bewegungen, beide Archivierungswege, Neustart, Offlinebetrieb, Hintergrundabruf und unklaren Versand testen.
6. OAuth-Teststatus/Dauerbetrieb klären. Keine Zusicherung unbegrenzter Gmail-Freigabe oder garantierter Joblaufzeiten.
7. Weiterhin keine automatische Bestandsberechnung und kein Zugriff auf das Intune-Arbeitsprofil. Kein Lizenztext wurde vom Betreiber festgelegt; öffentliche Sichtbarkeit nicht als zusätzliche Lizenzzusage auslegen.

Neue Entscheidungen, Prüfungen und Releasezustände in den zuständigen Dokumenten und dieser Übergabe nachführen. Historische Prüfprotokolle nicht nachträglich zu erfolgreichen Hardwaretests umschreiben.
