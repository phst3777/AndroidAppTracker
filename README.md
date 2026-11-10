# Snus Tracker (Android, Kotlin + Jetpack Compose)

## App auf dem Nothing Phone 3a installieren

1. Android Studio öffnen (aktuelle Version) -> **Open** -> diesen Ordner `SnusTracker` wählen.
2. Warten, bis der Gradle-Sync fertig ist (erstes Mal dauert es ein paar Minuten, Internet nötig).
   Falls Android Studio ein Update für das Android-Gradle-Plugin vorschlägt: "Update" ist okay.
3. Handy vorbereiten:
   - Einstellungen -> Über das Telefon -> 7x auf die Build-Nummer tippen (schaltet Entwickleroptionen frei)
   - Einstellungen -> System -> Entwickleroptionen -> **USB-Debugging** einschalten
4. Handy per USB anschließen, die Debug-Abfrage am Handy bestätigen.
5. In Android Studio oben das Handy auswählen und auf **Run** (grünes Dreieck) klicken.

### Alternativ: APK-Datei
Build -> Build Bundle(s) / APK(s) -> Build APK(s). Die Datei liegt danach in
`app/build/outputs/apk/debug/app-debug.apk`. Aufs Handy kopieren und antippen
(Android fragt, ob die Installation aus dieser Quelle erlaubt sein soll).

## APK ohne Android Studio bauen (über GitHub)

1. Kostenlosen Account auf github.com anlegen, dann **New repository** (privat reicht).
2. Den Inhalt des Ordners `SnusTracker` hochladen (im Repository: Add file -> Upload files).
   Wichtig: auch der versteckte Ordner `.github` muss mit, sonst startet der Build nicht.
3. Reiter **Actions** öffnen. Der Build "APK bauen" läuft von selbst (ca. 5 Minuten).
   Falls nicht: links "APK bauen" -> "Run workflow".
4. Fertigen Lauf antippen, unten unter **Artifacts** `snus-tracker-apk` herunterladen
   (ZIP mit `app-debug.apk`), entpacken und antippen.
5. Android fragt, ob die Installation aus dieser Quelle erlaubt ist: einmal erlauben.

## Daten aus der Web-Version übernehmen
Web-Version -> Einstellungen -> "Sicherung kopieren". In der App -> Einstellungen ->
"Aus Zwischenablage einfügen".

## Aufbau
| Datei | Inhalt |
|---|---|
| `Model.kt` | Datenmodell, Zähl-Logik (+1, -1, Dosen-Zähler), Speichern als JSON, Zahlenformat |
| `TrackerViewModel.kt` | Hält den Zustand und speichert nach jeder Änderung |
| `Theme.kt` | Nothing-Farben (hell/dunkel nach Systemeinstellung), Doto-Schrift |
| `Components.kt` | Karte, Segment-Schalter, Knöpfe |
| `TodayScreen.kt` | Zähler, +1, Dosen-Anzeige, Dosen-Zähler |
| `HistoryScreen.kt` | Balkendiagramm (selbst gezeichnet mit Canvas) |
| `CostsScreen.kt` | Hochrechnung und Gesamtausgaben |
| `SettingsScreen.kt` | Dose/Preis, Sicherung, Löschen |
| `MainActivity.kt` | Einstieg und untere Tab-Leiste |

Die Schrift Doto steht unter der SIL Open Font License (Google Fonts).
