# App-Updates über GitHub

Ab Release 0.6.1 (versionCode 10) verfügbar; ältere Releases 0.5.0/0.6.0 enthalten den Updater noch nicht. Die erste Version mit Updater muss einmal manuell auf dem Gerät installiert werden. Danach können neuere Versionen über den Button installiert werden.

## Bedienung

1. Schloss entsperren → **App aktualisieren**.
2. **Nach Updates suchen** zeigt einen neueren stabilen Release und seine Hinweise an. Die Abfrage erfolgt nur auf Knopfdruck.
3. **Update herunterladen** bestätigen. Die APK wird im privaten App-Speicher geladen und geprüft.
4. Unter **Sicherung & Wiederherstellung** muss ein erreichbarer, beschreibbarer Sicherungsordner eingerichtet sein.
5. **Sichern & Update installieren** wählen. Falls Android die Installationsfreigabe noch nicht erteilt hat, über **Android-Einstellungen** öffnen und dort selbst freigeben. Nach Rückkehr das Schloss erneut öffnen und die Installation erneut starten.
6. Nach erneuter APK-Prüfung und erfolgreicher vollständiger Sicherung öffnet sich der Android-Installationsdialog. Installation dort bestätigen. Keine Deinstallation durchführen.

Fehlt die Sicherung, läuft eine Mailverarbeitung, ist die APK ungültig oder sperrt die Geräteverwaltung Installationen, startet die App keine Installation bzw. zeigt eine Fehlermeldung. Ein App-Update erhält grundsätzlich die vorhandenen App-Daten; die zusätzliche Sicherung ist für eine mögliche Wiederherstellung vorgesehen. Es gibt keine unbeaufsichtigte Installation. Die Vorschau mit Paket de.nll.lager.preview installiert keine Updates der Haupt-App.

## Releasequelle und Veröffentlichung

Festes öffentliches Repository: iONEMond/NLL-Lager. API: https://api.github.com/repos/iONEMond/NLL-Lager/releases/latest. Keine eingebetteten Zugangsdaten. Nur stabile, veröffentlichte Releases; keine Entwürfe oder Pre-Releases. GitHub-Abfragelimits können eine spätere Wiederholung erfordern.

Für jeden zukünftigen Release:

- Tag vX.Y.Z und Version X.Y.Z; drei numerische Bestandteile.
- Genau ein Asset NLL-Lager-X.Y.Z-release.apk und eine von GitHub gelieferte SHA-256-Angabe im digest-Feld des Assets.
- APK-Paket de.nll.lager, Versionsname passend zum Release und versionCode höher als bei der installierten Version. Auch gegenüber bereits installierten Entwicklungsständen erhöhen.
- Derselbe bestehende Release-Schlüssel. Ein Schlüsselwechsel benötigt eine gesonderte Anpassung des Updaters.
- APK-Version, Signatur, Prüfsummen und Versionshistorie vor Veröffentlichung prüfen. Der Updater lädt die APK aus dem Release-Asset, nicht aus dem Repository-Dateibaum.

Die App prüft Downloadlänge, SHA-256 und mit apksig die vollständige APK-Signatur. Das Zertifikat muss den fest hinterlegten Release-Fingerabdruck tragen; auch die installierte Haupt-App muss diesen Schlüssel verwenden. Paketname, Versionsname, höherer versionCode und minimale Android-Version werden vor Installation geprüft. Zusätzlich prüft Android die Installation selbst.

HTTPS-Verbindungen und maximal fünf Weiterleitungen sind auf feste GitHub-/Release-Asset-Hosts beschränkt. APK-Limit 64 MB, Metadatenlimit 1 MB. Unvollständige Downloads werden entfernt. Der Installer erhält nur temporären Lesezugriff auf die betreffende APK; Geschäftsdaten werden nicht freigegeben. Alte Update-Dateien werden beim App-Start nach 24 Stunden bereinigt.

Die Freigabe über Android und die abschließende Installation müssen noch auf dem Honeywell getestet werden. Gerätevorgaben können die Installation aus einer App heraus verbieten.
