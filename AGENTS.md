# Projektregeln

## Einstieg und Dokumentation

- Vor Änderungen [Übergabe.md](Übergabe.md) lesen; dort stehen Architektur, betriebliche Entscheidungen, Daten-/Mailbesonderheiten und offene Aufgaben. Live-Stand bei GitHub und Geräten anschließend prüfen.
- README als kurzen Einstieg halten. Anleitungen und Prüfprotokoll liegen unter `docs/`. Relative Links nach Verschiebungen mit `python3 tools/check_docs.py` prüfen.
- Übergabe bei neuen Architekturentscheidungen, Betriebsbesonderheiten und geändertem Release-/Prüfstand mitpflegen.

## Versionshistorie und Veröffentlichung

- Auf Wunsch des Betreibers CHANGELOG.md bei jeder funktionalen Änderung, Fehlerkorrektur und Veröffentlichung aktuell halten.
- Geplante oder noch nicht geprüfte Funktionen unter einem ausdrücklich unveröffentlichten Abschnitt führen; erst bei tatsächlicher Veröffentlichung als veröffentlicht kennzeichnen.
- Beim Release Versionsname und versionCode in app/build.gradle, README.md, CHANGELOG.md, docs/VALIDIERUNG.md und Übergabe.md sowie APK-Dateiname und downloads/SHA256SUMS konsistent aktualisieren. versionCode für jedes veröffentlichte Update erhöhen.
- Nur tatsächlich ausgeführte Prüfungen dokumentieren. Lokale Vorschauen von auf GitHub veröffentlichten APKs unterscheiden.
- Private Schlüsseldateien (.jks/.keystore), Passwörter und Tokens niemals auf GitHub hochladen. Bestehenden Release-Schlüssel für Updates verwenden.
- Neue APKs als GitHub-Release-Assets veröffentlichen. Historische APKs unter downloads/ und insbesondere die 0.5.0-Testdatei nicht ohne Prüfung von Tests und bestehenden Links entfernen.
