# Sicherung und Wiederherstellung

Ab Release 0.6.1 (versionCode 10) verfügbar; nicht in den älteren Releases 0.5.0/0.6.0 enthalten.

## Einrichten

1. Schloss öffnen und **Sicherung & Wiederherstellung** aufrufen.
2. **Sicherungsordner auswählen** und einen Ordner im gemeinsam genutzten Gerätespeicher wählen, beispielsweise Dokumente/NLL-Lager. Lese-/Schreibzugriff erlauben.
3. **Jetzt sichern** wählen. Erst „Sicherung erstellt und geprüft“ bzw. ein erfolgreicher Sicherungsstatus bestätigt die fertige Datei.

Die Arbeitsdatenbank bleibt geschützt innerhalb der App. Die Sicherungsdateien liegen im ausgewählten Ordner und können unabhängig von der App aufbewahrt werden. Ein anderer Speicheranbieter ist über die Android-Auswahl ebenfalls möglich; dessen Verfügbarkeit bestimmt dann die Erreichbarkeit der Sicherungen.

## Automatische Sicherungen

Nach Buchungen, Stammdaten-/Lageränderungen, Importen und Archivierungen sowie beim App-Start wird eine Sicherung eingeplant. Android kann Hintergrundarbeit verzögern. Unveränderte Daten erzeugen keine weitere automatische Kopie; manuelles Sichern erzwingt einen Stand. Fehlender Ordnerzugriff oder Speicherplatz wird im Sicherungsmenü angezeigt. Bis zur erfolgreichen Sicherung können letzte Änderungen nur in der App vorhanden sein.

Dateien heißen NLL-Sicherung-Datum-Uhrzeit-Kennung.nllbackup. Erst schreiben, schließen, vollständig zurücklesen und die Prüfsumme prüfen, danach den Namen abschließen. Unfertige .partial-Dateien zählen nicht als Sicherung. Die sieben neuesten geprüften Stände bleiben erhalten; Bereinigung erfolgt erst nach erfolgreicher neuer Sicherung und betrifft nur gültige NLL-Sicherungsdateien. Andere Dateien werden nicht gelöscht. Schlägt die Bereinigung fehl, bleiben gegebenenfalls mehr als sieben Stände liegen.

## Inhalt

- Vollständiger Material- und Personenkatalog einschließlich Codes und Lagerzuordnung.
- Lageroptionen in ihrer gespeicherten Reihenfolge.
- Offene und archivierte Bewegungen einschließlich IDs, Zeitpunkt, historischen Artikel-/Mitarbeiterangaben, Menge, Bewegungsart und Archivzuordnung.

Kein Excel-Bericht, sondern eine versionierte JSON-Sicherungsdatei mit SHA-256-Prüfsumme. Sie ist nicht verschlüsselt; die Prüfsumme erkennt Beschädigungen und ist kein Schutz gegen absichtliche Manipulation. Keine App-Passwörter, Google-Tokens, Kontofreigaben oder Mailwarteschlangen. Nur selbst erzeugte, vertrauenswürdige Sicherungen verwenden. Begrenzung: 64 MB und 100.000 Datensätze je Geschäftstabelle.

## Wiederherstellen

1. Nach einer Neuinstallation zunächst ein App-Passwort festlegen und das geschützte Menü öffnen.
2. Einen beschreibbaren Sicherungsordner auswählen. Dort muss vor jedem Ersetzen eine geprüfte Sicherheitskopie des aktuellen Stands gespeichert werden können.
3. **Sicherung wiederherstellen** wählen und eine fertige .nllbackup-Datei öffnen.
4. Die angezeigten Anzahlen prüfen. **Ersetzen & wiederherstellen** ersetzt die aktuellen Geschäftsdaten einschließlich Archiv vollständig; es findet keine Zusammenführung statt. Abbrechen verändert die Daten nicht.
5. Nach Erfolg erneut anmelden; Gmail bei Bedarf erneut verbinden.

Dateistruktur, Version, Prüfsumme, Schlüsselfelder und Werte werden vor der Übernahme geprüft. Der Ersatz von Geschäftsdaten und Lageroptionen erfolgt in einer Datenbanktransaktion. Scheitert der Ersatz, bleiben die bisherigen Geschäftsdaten erhalten. Nach Auswahl eines Sicherungsstands kann Gmail auch bei einem fehlgeschlagenen Ersatz bereits getrennt sein und muss erneut verbunden werden.

Die Wiederherstellung löscht alte lokale Mailaufträge und trennt die Gmail-Verbindung. Nach erneutem Verbinden werden nur Mails nach dem Wiederherstellungszeitpunkt verarbeitet. So werden alte Import-/Exportaufträge nicht erneut ausgeführt oder historische Exportgrenzen auf neue Daten angewendet. Alte Mailberichte können weiterhin im Postfach liegen; vor erneutem Versand prüfen.

Nach Deinstallation ist die Ordnerfreigabe erneut nötig. App-Updates erhalten die Daten normalerweise ohne Wiederherstellung. Gegen Geräteverlust oder Defekt eine zusätzliche Kopie außerhalb des Geräts aufbewahren. Der echte Ordner-/Neuinstallationstest auf dem Honeywell steht noch aus.
