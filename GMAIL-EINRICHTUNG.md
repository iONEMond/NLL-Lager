# Gmail-Anbindung – Entwicklungsstand 0.6.0

Noch keine freigegebene Version. Der lokale Entwicklungsbuild heißt 0.6.0-dev (versionCode 7). Ein echter Google-Login und Mailaustausch müssen nach Einrichtung des Postfachs geprüft werden. Die veröffentlichte APK 0.5.0 enthält diese Funktionen nicht.

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

## Bedienung

1. Schloss entsperren → **Mail & Synchronisierung** → **Postfach & Empfänger einrichten**.
2. Scanner-Gmail-Adresse und feste Firmenadresse eintragen; automatischen Abruf nach Bedarf aktivieren.
3. **Mit Google verbinden** wählen und Zugriff auf Lesen und Senden freigeben. Das bestätigte Konto muss der konfigurierten Gmail-Adresse entsprechen.
4. **Jetzt synchronisieren** prüft wartende Aufträge. **Export jetzt per Mail senden** erzeugt einen lokalen Exportauftrag an dieselbe Firmenadresse.
5. Nur neue Mails nach Einrichtung bzw. Adresswechsel und im Gmail-Posteingang werden berücksichtigt. Keine automatische Verarbeitung alter Mailbestände.

Die vollständigen Betreffzeilen stehen einschließlich eines zufälligen Auftragsschlüssels ausschließlich im geschützten Menü:

```text
NLL AUFTRAGSSCHLUESSEL IMPORT MATERIAL
NLL AUFTRAGSSCHLUESSEL IMPORT PERSONEN
NLL AUFTRAGSSCHLUESSEL EXPORT
```

`AUFTRAGSSCHLUESSEL` ist ein Platzhalter. Den echten Wert aus dem Menü übernehmen, vertraulich behandeln und nicht in GitHub-Issues veröffentlichen. Es werden nur exakt passende Befehle mit gültigem Schlüssel und der hinterlegten Firmenabsenderadresse verarbeitet. Antworten gehen ausschließlich an die konfigurierte Firmenadresse, niemals an Reply-To oder zusätzliche Empfänger. Unbekannte Mails lösen keine Antwort aus.

## Import und Export

- Beim Import genau einen `.xlsx`-Anhang senden. Die bisherigen Material-/Personenvorlagen bleiben kompatibel. Maximal 20 MB pro Anhang und 10.000 Datensätze; Formeln und doppelte Codes innerhalb der Datei werden abgelehnt.
- Neue Codes werden angelegt, vorhandene Codes aktualisiert. Nicht in der Datei enthaltene Einträge bleiben bestehen. Fehlt die optionale Lagerspalte, bleibt eine bestehende Zuordnung erhalten; eine vorhandene, leere Lagerspalte leert sie bewusst.
- Der Import erfolgt mit Auftragsprotokoll in einer Datenbanktransaktion. Ein Fehler lässt den bisherigen Katalog erhalten. Frühere Buchungen behalten ihre gespeicherten Artikel-/Personendaten.
- Der Mailbericht enthält die zum Erstellungszeitpunkt offenen Bewegungen. Spätere Buchungen werden nicht in diesen Bericht oder dessen Abschluss einbezogen. Maximal 12 MB fertige XLSX-Datei für Mailversand.
- Versand bedeutet nicht Zustellung: Erst nach tatsächlicher Übernahme unter **Übernahme bestätigen & archivieren** abschließen. Der lokale Dateiexport behält sein bisheriges Verhalten (Archivierung nach erfolgreichem Dateischreiben).
- Das Gerät speichert Auftragskennungen dauerhaft. Wiederholter Abruf derselben Mail importiert nicht erneut. Eine neu gesendete Mail ist ein neuer Auftrag.
- Wird ein Versand unterbrochen oder ist sein Ergebnis unklar, erfolgt keine automatische Wiederholung oder Archivierung. Zuerst Firmenpostfach und Gmail-Gesendet prüfen; dann im geschützten Menü **Ist angekommen** oder **Erneut senden** wählen. Eine bewusste Wiederholung kann eine zweite Mail erzeugen, verwendet aber denselben Bericht.
- Protokoll und vorbereitete Antworten einschließlich Exportdateien liegen im privaten App-Speicher; sie bleiben über Neustarts erhalten. Aktuell erfolgt keine automatische Bereinigung. Eine Deinstallation/App-Datenlöschung entfernt diesen Stand ebenso wie die Lagerdaten.

## Hintergrundbetrieb und offene Prüfung

JobScheduler fragt bei erlaubtem Netzwerk frühestens ungefähr alle 15 Minuten ab; Android, Energiesparen, Profilpause und fehlendes Netz können die Ausführung verzögern. Bei vollständig gestoppter App muss sie wieder geöffnet werden. Kein Echtzeitversprechen. **Automatischen Mailzugriff deaktivieren** beendet weitere App-Abrufe; eine bereits bei Google erteilte Freigabe kann separat im Google-Konto widerrufen werden.

Noch erforderlich: echter OAuth-Login, Testimport vom Firmenpostfach, Empfang des XLSX-Exports, separater Abschluss am Gerät sowie Hintergrund-/Neustarttest auf dem Honeywell. Google-/Mailanbieter können Versand oder Anhänge zusätzlich filtern. Ohne diese Prüfungen keine Veröffentlichung als fertige 0.6.0.
