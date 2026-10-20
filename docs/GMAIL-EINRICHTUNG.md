# Gmail-Anbindung – Release 0.6.0

Release 0.6.0 (versionCode 8) für den ersten Honeywell-Gerätetest. Google-Login, Gmail-Abruf, Versand eines leeren Excel-Testexports und Material-Mailimport mit Firmensignatur sind im Emulator geprüft. Hardware-/Hintergrundtest und echte Prüfung der zuletzt ergänzten Abläufe stehen aus. Die ältere APK 0.5.0 enthält keine Mailfunktionen.

## Einrichtungsstand am 19.10.2026

Gmail API aktiviert; Google-Auth-App NLL Lager eingerichtet. Android-OAuth-Client für das Release-Paket `de.nll.lager` mit dem unten dokumentierten Release-Zertifikat erstellt. Das separate Scanner-Gmail-Konto ist als Testnutzer gespeichert; angeforderte Bereiche `gmail.readonly` und `gmail.send` sind konfiguriert. Status weiterhin Test. Für das Vorschau-Paket ist noch kein eigener OAuth-Client angelegt. Im separaten Emulator NLL Gmail Test sind Google-Kontoanmeldung, Kontofreigabe und ein echter Gmail-Synchronisierungslauf erfolgreich bestätigt (19.10.2026). Ein leerer Excel-Testexport (0 offene Bewegungen) wurde am 19.10.2026 um 22:25 mit Auftragsstatus „Versendet“ bestätigt. Empfang des Excel-Berichts im Firmenpostfach vom Betreiber bestätigt. Material-Mailimport mit Firmensignatur erfolgreich (1 ergänzt, 0 aktualisiert), Rückmeldung versendet. Die 0.6.0-dev-Test-APK wurde mit dem Release-Schlüssel gebaut; Paket, Version (7) und APK-v2-Signatur sind geprüft. Sie liegt lokal im Mac-Projektordner als NLL-Lager-0.6.0-dev-release.apk. Dieser Entwicklungsbuild wurde durch den signierten Release 0.6.0 (versionCode 8) ersetzt. Noch ausstehend sind Personenimport, Aktualisierung vorhandener Stammdaten im echten Mailbetrieb und Geräte-/Hintergrundtests.

## Voraussetzungen

- Ein separates Gmail-Konto für genau diesen Scanner. Keine Gmail-App erforderlich.
- Das Konto im privaten Android-Profil verfügbar machen; die Google-Anmeldung und Kontofreigabe übernimmt der Betreiber selbst. Google Play-Dienste sind erforderlich.
- Die Firmenadresse, von der Aufträge kommen und an die Antworten gehen. Keine Passwörter oder Tokens ins Repository oder in den Chat eintragen.
- Ein Google-Cloud-Projekt mit aktivierter **Gmail API** und eingerichteter Google Auth Platform. Für den ersten Test die Scanner-Gmail-Adresse als Testnutzer eintragen.
- Android-OAuth-Client mit passendem Paketnamen und SHA-1 des Signierzertifikats erstellen. Die Zuordnung erfolgt durch Google Play-Dienste; es muss kein Client-Secret in die APK eingebaut werden.

| Build | Paket | SHA-1-Zertifikatsfingerabdruck |
|---|---|---|
| Release mit vorhandenem NLL-Schlüssel | `de.nll.lager` | `2F:5C:DD:F8:C5:9B:93:33:AF:0D:A3:19:C6:97:00:6C:16:6E:A9:CC` |
| Lokale Vorschau auf diesem Mac | `de.nll.lager.preview` | `7F:14:3B:ED:8E:BB:54:F6:2C:D1:70:17:7C:5A:F1:CF:6A:1D:6C:92` |

Angeforderte Berechtigungen: `https://www.googleapis.com/auth/gmail.readonly` (Nachrichten/Anhänge lesen) und `https://www.googleapis.com/auth/gmail.send` (Ergebnisse/Excel senden). Die App löscht oder verändert keine Mails und speichert selbst keine OAuth-Tokens. Beide Berechtigungen müssen erteilt werden.

Google stuft Gmail-Lesezugriff als eingeschränkten Scope ein. Für kleine, bekannte Nutzerkreise können Ausnahmen von der Verifizierung gelten; das ist keine generelle Zusicherung für jedes Projekt. Im Google-Testmodus verfallen Nutzerfreigaben nach sieben Tagen. Den dauerhaften Veröffentlichungs-/Freigabestatus vor dem unbeaufsichtigten Betrieb klären.

Quellen: [Android-Autorisierung](https://developer.android.com/identity/authorization), [Gmail-Scopes](https://developers.google.com/workspace/gmail/api/auth/scopes), [Testnutzer und Ablauf](https://support.google.com/cloud/answer/15549945), [Verifizierungsausnahmen](https://support.google.com/cloud/answer/13464323).

## Ergänzungen für 0.6.2-dev (noch nicht veröffentlicht)

Bei verbundener Anbindung sind Einrichtungsoptionen standardmäßig ausgeblendet; „Verbindung bearbeiten“ zeigt sie wieder. Manueller Sync und Mailversand zeigen Fortschrittsbalken mit aktuellem Arbeitsschritt. Im Materialimport reichen Code und Beschreibung/Title; neue Einträge erhalten Nummer = Code, Art Material und Einheit Stk. Bei Aktualisierung bleiben fehlende Zusatzspalten erhalten. Mailimport aktualisiert vorhandene Codes wie bisher ohne lokale Rückfrage; die Auswahl Aktualisieren/Überspringen betrifft den Dateiimport.

## Bedienung

1. Schloss entsperren → **Mail & Synchronisierung** → **Postfach & Empfänger einrichten**.
2. Scanner-Gmail-Adresse und feste Firmenadresse eintragen; automatischen Abruf nach Bedarf aktivieren.
3. **Mit Google verbinden** wählen und Zugriff auf Lesen und Senden freigeben. Das bestätigte Konto muss der konfigurierten Gmail-Adresse entsprechen.
4. **Jetzt synchronisieren** prüft wartende Aufträge. **Export jetzt per Mail senden** erzeugt einen lokalen Exportauftrag an dieselbe Firmenadresse.
5. Nur neue Mails nach Einrichtung bzw. Adresswechsel und im Gmail-Posteingang werden berücksichtigt. Keine automatische Verarbeitung alter Mailbestände.

Die Betreffzeilen sind ohne Auftragsschlüssel verwendbar:

```text
NLL Material
NLL Personen
NLL Export
```

Groß-/Kleinschreibung und äußere Leerzeichen sind egal. Zusätze wie `Re:` werden nicht akzeptiert. Nur die hinterlegte Firmenabsenderadresse im From-Header wird zugelassen. Auf Wunsch des Betreibers gibt es keinen zusätzlichen Auftragsschlüssel mehr; eine gefälschte Absenderadresse wird damit nicht kryptografisch ausgeschlossen. Antworten gehen ausschließlich an die konfigurierte Firmenadresse, niemals an Reply-To oder zusätzliche Empfänger. Unbekannte Mails lösen keine Antwort aus. Die bisherigen Betreffzeilen mit Schlüssel gelten nicht mehr. Vorhandene Kontofreigabe, Abrufbeginn und bereits verarbeitete Aufträge bleiben beim Update erhalten.

## Import und Export

Ab 0.6.1-dev verwenden Excel-Mailberichte dasselbe neue Acht-Spalten-Layout wie der Dateiexport: Code, Beschreibung, Menge, Bewegungsart, Mitarbeiter, Datum, Lagerort (Von -> Nach), Buchung erfolgt?. Die letzte Spalte bleibt leer. Bereits wartende Mailberichte behalten ihren beim Erstellen gespeicherten Inhalt; sie werden nicht nachträglich umformatiert.

Ab Entwicklungsstand 0.6.1-dev wird auch die Firmenliste mit Title und Code erkannt. Code dient als Scan-/Materialnummer, Title als Beschreibung; Art Material und Einheit Stk. Typ, Bemerkung und BST Menge (Bestellmenge) werden ignoriert. Bei fehlender Lager-Spalte bleibt eine vorhandene Lagerzuordnung im Mailimport erhalten. Diese Ergänzung ist noch nicht in Release 0.6.0 enthalten.

- Beim Import genau einen `.xlsx`-Anhang senden. Zusätzliche Bilder mit MIME-Typ `image/*` (z. B. Firmensignatur) werden ignoriert und nicht heruntergeladen. Andere zusätzliche Dateien und mehrere Excel-Dateien werden abgelehnt. Die bisherigen Material-/Personenvorlagen bleiben kompatibel. Maximal 20 MB pro Anhang und 10.000 Datensätze; Formeln und doppelte Codes innerhalb der Datei werden abgelehnt.
- Neue Codes werden angelegt, vorhandene Codes aktualisiert. Nicht in der Datei enthaltene Einträge bleiben bestehen. Fehlt die optionale Lagerspalte, bleibt eine bestehende Zuordnung erhalten; eine vorhandene, leere Lagerspalte leert sie bewusst.
- Der Import erfolgt mit Auftragsprotokoll in einer Datenbanktransaktion. Ein Fehler lässt den bisherigen Katalog erhalten. Frühere Buchungen behalten ihre gespeicherten Artikel-/Personendaten.
- Der Mailbericht enthält die zum Erstellungszeitpunkt offenen Bewegungen. Spätere Buchungen werden nicht in diesen Bericht oder dessen Abschluss einbezogen. Maximal 12 MB fertige XLSX-Datei für Mailversand.
- Versand bedeutet nicht Zustellung: Mailkommandos und **Export jetzt per Mail senden** im Mail-Menü werden nach tatsächlicher Übernahme unter **Übernahme bestätigen & archivieren** abgeschlossen. Unter **Bericht exportieren → Per Mail senden & archivieren** wird dagegen auf ausdrückliche Bestätigung bereits nach erfolgreichem Gmail-Versand archiviert. Fehler/unklarer Versand lassen Bewegungen offen; spätere Buchungen bleiben außerhalb des Berichts. Der automatische Abschluss ist dauerhaft gespeichert und wird nach einem Neustart ohne erneuten Versand nachgeholt, wenn der Versand bereits bestätigt war. Der lokale Dateiexport behält sein bisheriges Verhalten (Archivierung nach erfolgreichem Dateischreiben).
- Das Gerät speichert Auftragskennungen dauerhaft. Wiederholter Abruf derselben Mail importiert nicht erneut. Eine neu gesendete Mail ist ein neuer Auftrag.
- Wird ein Versand unterbrochen oder ist sein Ergebnis unklar, erfolgt keine automatische Wiederholung oder Archivierung. Zuerst Firmenpostfach und Gmail-Gesendet prüfen; dann im geschützten Menü **Ist angekommen** oder **Erneut senden** wählen. Eine bewusste Wiederholung kann eine zweite Mail erzeugen, verwendet aber denselben Bericht.
- Protokoll und vorbereitete Antworten einschließlich Exportdateien liegen im privaten App-Speicher; sie bleiben über Neustarts erhalten. Aktuell erfolgt keine automatische Bereinigung. Eine Deinstallation/App-Datenlöschung entfernt diesen Stand ebenso wie die Lagerdaten.

## Hintergrundbetrieb und offene Prüfung

JobScheduler fragt bei erlaubtem Netzwerk frühestens ungefähr alle 15 Minuten ab; Android, Energiesparen, Profilpause und fehlendes Netz können die Ausführung verzögern. Bei vollständig gestoppter App muss sie wieder geöffnet werden. Kein Echtzeitversprechen. **Automatischen Mailzugriff deaktivieren** beendet weitere App-Abrufe; eine bereits bei Google erteilte Freigabe kann separat im Google-Konto widerrufen werden.

Noch erforderlich: Personenimport und Aktualisierung vorhandener Einträge per echter Mail, Export mit Bewegungen und separater Abschluss am Gerät sowie Hintergrund-/Neustarttest auf dem Honeywell. Google-/Mailanbieter können Versand oder Anhänge zusätzlich filtern. Diese Punkte bleiben für den ersten Gerätetest ausdrücklich offen.

## Fehlersuche: „Hintergrundverarbeitung läuft“ in 0.7.0

Die Anzeige bedeutet eine aktive Verarbeitungssperre zum Zeitpunkt des Bildschirmaufbaus. In 0.7.0 kann sie nach abgeschlossenem Hintergrundlauf veraltet stehen bleiben: Menü verlassen und neu öffnen. Falls die manuellen Aktionen weiterhin „Bitte kurz warten“ melden, ist die Sperre noch aktiv. Die Meldung allein belegt keinen Google-Verbindungsfehler. App nicht deinstallieren oder Daten löschen. Stopp erzwingen beendet den Prozess und die laufende Sperre; automatischer Abruf kann beim nächsten Start erneut beginnen.

Unveröffentlichte Korrektur 0.7.1-dev: Hintergrundstatus aktualisiert sich selbst, aktueller Arbeitsschritt wird angezeigt, registrierte Mailläufe lassen sich abbrechen und haben ein Gesamtzeitlimit von zwei Minuten. Sperre erst nach Workerabschluss freigeben; bei unterbrochener Mailübertragung unklaren Status prüfen, keine automatische Wiederholung.

Neue Exporte fehlen unter Letzte Aufträge: Das deutet auf fehlende Auftragserstellung vor dem Versand hin. Ab 0.7.2 umgehen ausdrücklich am Scanner ausgelöste Exporte die Posteingangs-Zeitgrenze; danach wird die gespeicherte Kennung geprüft. Vorbereitungsfehler erscheinen mit konkreter Ursache im Status. Eingehende Mails werden weiterhin nur ab der eingerichteten Zeitgrenze verarbeitet. Kein Zurücksetzen der Kontoverbindung oder App-Daten auf Verdacht erforderlich.
