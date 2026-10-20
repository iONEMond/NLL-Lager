# Entwicklung und Veröffentlichung

[Projektstartseite](../README.md) · [Agentenübergabe](../Übergabe.md)

## Toolchain und Struktur

Java-Quellcode, Android Views ohne Compose oder WebView. Sprachziel Java 17; JDK 21 ist auf dem Entwicklungs-Mac eingerichtet. Gradle 8.13, Android Gradle Plugin 8.9.2, compileSdk/targetSdk 35, minSdk 26. Abhängigkeiten stehen in [app/build.gradle](../app/build.gradle).

| Pfad | Inhalt |
| --- | --- |
| `app/src/main/` | App-Code, Manifest und Ressourcen |
| `app/src/test/` | Lokale JUnit-/Robolectric-Tests |
| `app/src/androidTest/` | Geräte-Instrumentierung; StoreTest wird auch lokal eingebunden |
| `docs/` | Bedienung, Einrichtung, Technik und Prüfprotokoll |
| `vorlagen/` | XLSX-Importvorlagen mit Beispieldaten |
| `downloads/` | Historische APKs und Prüfsummen; neue APKs als Release-Assets |
| `tools/` | Vorlagenerzeugung und optionale Linux-Cloud-Werkzeuge |

## Lokal bauen und prüfen

Android Studio öffnen oder `JAVA_HOME` und `ANDROID_HOME` für die eigene Installation setzen. Alternativ kann eine nicht eingecheckte `local.properties` den SDK-Pfad enthalten.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease :app:lintRelease
python3 tools/check_docs.py
```

Der Release-Build aus der Kommandozeile ist ohne Signierkonfiguration **unsigniert**. Die Datei `app/build/outputs/apk/release/app-release-unsigned.apk` ist kein auslieferbares Update. Debug nutzt Paket `de.nll.lager.preview`, Release `de.nll.lager`.

124 vorhandene lokale Tests nutzen Robolectric mit Android 11. Ergebnisse: `app/build/test-results/testDebugUnitTest/` und `app/build/reports/`. Ein erfolgreicher Testlauf ersetzt keinen Honeywell-Test. Das Prüfprotokoll beschreibt die jeweilige Reichweite.

`./gradlew :app:connectedDebugAndroidTest` nur gegen ein ausdrücklich separates Testgerät ausführen: Die Tests löschen App-Testdaten. Keine produktiven Daten oder Emulatoren dafür verwenden.

## Optionale Cloud-Werkzeuge

`tools/setup-cloud.sh`, `tools/check-cloud.sh` und `tools/emulator-cloud.sh` stammen aus der Linux-Umgebung unter `/workspace/android-tools`. Sie sind keine macOS-Installationsanleitung und nicht für einen gewöhnlichen GitHub-Runner vorkonfiguriert. Das Setup lädt Toolchains, prüft deren Prüfsummen und enthält eine automatische SDK-Lizenzannahme; vor Verwendung prüfen, ob diese autorisiert ist. Der Emulator benötigt ein vorhandenes Android-30-Systemimage. Lokal den Gradle-Wrapper bevorzugen.

`tools/create_templates.py` erzeugt die beiden Importvorlagen; die Vorlagen sind bereits eingecheckt. Nicht unnötig neu erzeugen oder Beispielzeilen mit Produktivdaten ersetzen.

## Release-Ablauf

1. Tatsächlichen GitHub-Release- und Branchstand lesen. Installierten Entwicklungsständen Rechnung tragen: aktuell ist 0.7.0 mit Code 13 veröffentlicht; 0.7.1-dev mit Code 14 ist der unveröffentlichte Entwicklungsstand. Für den späteren stabilen Release einen höheren Code wählen.
2. Version in `app/build.gradle`, README, CHANGELOG und Prüfprotokoll konsistent halten. Solange keine signierte APK veröffentlicht ist, ausdrücklich „vorbereitet/nicht veröffentlicht“ schreiben.
3. Tests, Release-Build und Release-Lint ausführen; Metadaten prüfen. Geräteprüfungen nur nach tatsächlicher Durchführung bestätigen.
4. Über Android Studio **Generate Signed App Bundle or APK → APK** mit dem bestehenden Release-Schlüssel signieren. Der Betreiber trägt nötige Passwörter im lokalen Dialog ein. Nicht im Chat erfragen oder auslesen. Aktuelle ausdrückliche Betreiberpräferenz: „Remember passwords“ aktiviert lassen.
5. Signierte APK mit `apksigner verify --print-certs` prüfen. Paket `de.nll.lager`, erwartete Version und höherer versionCode mit `aapt dump badging` prüfen. SHA-256 berechnen. Signaturfingerabdruck siehe Übergabe.
6. Code, Dokumentation und `downloads/SHA256SUMS` abgleichen. Stabile Veröffentlichung mit Tag `vX.Y.Z` und genau einem Asset `NLL-Lager-X.Y.Z-release.apk` sowie `SHA256SUMS` erstellen. Keine unsignierten oder Vorschau-APKs als Haupt-Release hochladen.
7. Release-Asset anonym erneut lesen: URL, Größe, Version und GitHub-`digest` müssen zur APK passen. Der Updater verlangt das Digest-Feld. Erst dann README/CHANGELOG als veröffentlicht kennzeichnen.

Vollständige Updatebedingungen stehen in [UPDATES.md](UPDATES.md). Schlüssel niemals ersetzen, um ein fehlendes Passwort zu umgehen. 0.6.1 wurde nach erneuter lokaler Passworteingabe signiert, geprüft und als stabiler Release veröffentlicht.

## Dokumentationspflege

README ist der kurze Einstieg. Bedienregeln gehören nach `docs/BEDIENUNG.md`, technische Übergabepunkte nach `Übergabe.md`, ausgeführte Prüfungen nach `docs/VALIDIERUNG.md`. CHANGELOG beschreibt Änderungen und Veröffentlichungsstatus. `python3 tools/check_docs.py` (Python 3.9 oder neuer) prüft relative Markdown-Dateilinks gegen vorhandene Dateien; externe URLs und Überschriftenanker werden nicht geprüft.
