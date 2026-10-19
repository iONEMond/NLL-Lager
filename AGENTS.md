# Projektregeln

## Versionshistorie und Veröffentlichung

- Auf Wunsch des Betreibers CHANGELOG.md bei jeder funktionalen Änderung, Fehlerkorrektur und Veröffentlichung aktuell halten.
- Geplante oder noch nicht geprüfte Funktionen unter einem ausdrücklich unveröffentlichten Abschnitt führen; erst bei tatsächlicher Veröffentlichung als veröffentlicht kennzeichnen.
- Beim Release Versionsname und versionCode in app/build.gradle, README.md, CHANGELOG.md, VALIDIERUNG.md sowie APK-Dateiname und downloads/SHA256SUMS konsistent aktualisieren. versionCode für jedes veröffentlichte Update erhöhen.
- Nur tatsächlich ausgeführte Prüfungen dokumentieren. Lokale Vorschauen von auf GitHub veröffentlichten APKs unterscheiden.
- Private Schlüsseldateien (.jks/.keystore), Passwörter und Tokens niemals auf GitHub hochladen. Bestehenden Release-Schlüssel für Updates verwenden.
