# Reiseplaner (Android, Kotlin)

Reise-Manager-App: Reisen anlegen, Orte mit Koordinaten sammeln, abhaken, bearbeiten.
Alles wird lokal in einer Room-Datenbank gespeichert (bleibt nach App-Neustart erhalten).

## Funktionen
- **Reisen** mit Name, Reiseziel, Start/Ende (Datumsauswahl), Notizen; Status („in 12 Tagen“, „Unterwegs“, „Beendet“) und Fortschrittsbalken
- **Orte pro Reise** mit Name, Koordinaten, Notiz und Kategorie (Sehenswürdigkeit, Essen, Unterkunft, …)
- **Abhaken**, Filter (Alle / Offen / Erledigt), erledigte Orte rutschen nach unten
- **Bearbeiten/Löschen** mit „Rückgängig“-Snackbar
- **Aktuellen Standort verwenden** (GPS, Berechtigung wird bei Bedarf abgefragt)
- **Koordinaten-Eingabe** als `48.7758, 9.1829`, `48,7758 9,1829` oder Google-Maps-Link mit `@lat,lon`
- **Karte öffnen** (installierte Karten-App per `geo:`-Intent, sonst Browser), Koordinaten per Tipp kopieren
- **Reise teilen** als Text mit Kartenlinks
- Hell-/Dunkelmodus automatisch

## Bauen
1. Android Studio (Ladybug oder neuer) öffnen → **Open** → Ordner `Reiseplaner` wählen.
2. Gradle-Sync abwarten (lädt Gradle 8.9 und die Abhängigkeiten automatisch; JDK 17 ist in Android Studio enthalten).
3. Handy per USB anschließen (Entwickleroptionen + USB-Debugging) oder einen Emulator starten → **Run ▶**.

### APK erzeugen
**Build → Build Bundle(s) / APK(s) → Build APK(s)**. Die Debug-APK liegt danach unter
`app/build/outputs/apk/debug/app-debug.apk` und kann auf dem Handy installiert werden.
Für eine signierte Release-APK: **Build → Generate Signed Bundle / APK**.

## Technik
Kotlin 2.0, Jetpack Compose (Material 3), Navigation Compose, Room (KSP), Google Play Services Location.
minSdk 26 (Android 8.0), targetSdk 35.
