# Tatsächliche Abnahme

Stand: **29. Oktober 2026**. Das Repository [grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart) ist **privat**, auf `main` veröffentlicht und mit GitHub Actions geprüft. Ein Trainer-Server wurde nicht verändert.

## Bestätigte Plattformabnahme mit GitHub Actions

Erfolgreicher Lauf: [40938410361](https://github.com/grobmeier/artemis-java-jumpstart/actions/runs/40938410361), getesteter Commit **`e541d1c181154734ac3fcf86d3d532c1c9ee9277`**. Beide Jobs haben dieselbe vollständige Suite mit aktiviertem Trainingsprofil ausgeführt:

| Plattform | JDK | Ausgeführter Befehl | Ergebnis |
|---|---|---|---|
| Windows Server 2025, `windows-latest` | Temurin 21.0.12.1 | `call .\mvnw.cmd -B -Ptraining clean verify` | **18 Tests bestanden, 0 Fehler, 0 übersprungen** |
| Ubuntu 26.04.1, `ubuntu-latest` | Temurin 21.0.13 | `./mvnw -B -Ptraining clean verify` | **18 Tests bestanden, 0 Fehler, 0 übersprungen** |

Die Testbericht-XMLs wurden aus den heruntergeladenen GitHub-Artefakten gegengeprüft. Beide Artefakte enthalten das Laufzeit-ZIP mit Broker-/Client-JARs, Launchern, RUN.md und Lizenzen. Private Konfigurationen, Nutzerdaten, Schlüsseldateien und runtime-Daten fehlen wie vorgesehen. Unter Windows wurden auch die cmd-Launcher aus einem entpackten Pfad mit Leerzeichen, Exitcodes und Junction-Schutz tatsächlich geprüft.

Der reale Windows-Lauf fand zunächst eine abweichende Konsolenkodierung und eine fehlerhafte Erfassung von `java -version` im Batchskript. Die Korrekturen sind im oben genannten erfolgreichen Commit enthalten: eigene Java-Ausgabe ist UTF-8; der Launcher liest die Versionsausgabe über eine eigene kurzlebige Datei ein. Die Konsole verwendet während des Java-Aufrufs UTF-8 und erhält beim normalen Prozessende ihre vorherige Codepage zurück. Fehlgeschlagene Vorläufe sind keine bestandene Abnahme.

Der aktuelle Status neuerer Commits ist im [Actions-Verlauf](https://github.com/grobmeier/artemis-java-jumpstart/actions/workflows/verify.yml) sichtbar. Die folgende Dokumentationspflege kann deshalb einen neueren Commit haben als der oben festgehaltene erste vollständige Plattformnachweis.

## Lokale Prüfungen vor der Veröffentlichung

Lokale Plattform: **macOS 26.5.2 / Apple Silicon**, Azul **JDK 21.0.1**, ohne Preview. Maven Wrapper **3.3.4**, Maven **3.9.16**.

Alle lokalen Maven-Befehle liefen aus dem Repository-Hauptverzeichnis mit `JAVA_HOME` auf das genannte JDK und echten TCP-Sockets. Die lokale macOS-Prüfung ist von den tatsächlich ausgeführten GitHub-Plattformläufen oben getrennt dokumentiert.

| Befehl | Ergebnis auf macOS |
|---|---|
| `./mvnw -B clean verify` | **Bestanden**, Abschluss 19:42 MEZ: 17 Verhaltenstests bestanden; ein separat aktivierter Lösungstest erwartungsgemäß übersprungen |
| `./mvnw -B -Ptraining clean verify` | **Bestanden**, Abschluss 19:44 MEZ: **18 Tests, 0 Fehler, 0 übersprungen**; Übungen und Lösungen gebaut und geprüft |
| `sh -n scripts/*.sh` (je Datei) | Shellsyntax bestanden |
| Lokale Markdown-Verweisprüfung | Keine fehlenden lokalen Verweisziele im Quellrepository |
| ZIP-/JAR-Inhaltsprüfung | Broker-/Client-JAR, Launcher, Konfiguration, RUN.md und Lizenzen vorhanden; keine runtime-Daten, privaten Konfigurationen, Nutzerdateien oder Schlüsseldateien; keine javax.jms-API-Klassen |

Berichte stehen in `distribution/target/failsafe-reports/`, zusätzliche Kindprozessausgaben in `distribution/target/process-logs/` und das ZIP unter `distribution/target/artemis-java-jumpstart-runtime.zip`. GitHub Actions stellt diese Dateien als private Download-Artefakte mit sieben Tagen Aufbewahrung bereit; siehe [Anleitung](github-training.md).

## Nachgewiesenes Verhalten

| Bereich | Nachweis |
|---|---|
| S01 / S02 | Queue-Sends ohne Consumer bleiben verfügbar; zwei bereite Worker erhalten zusammen die vollständige ID-Menge ohne Duplikate; keine feste Quote vorausgesetzt |
| S03 | Zwei aktive unabhängige Subscriptions erhalten die vollständige Menge; später Subscriber erhält ausschließlich neue IDs |
| S04 / S15 | Zuerst registrierte durable Subscription erhält Offline-Events; persistente Topic- und Queue-Nachrichten überleben geordneten Brokerprozess-Neustart |
| S05 | Rollback und harter Clientabbruch vor JMS-Commit führen begrenzt zur Wiederzustellung mit gleicher fachlicher ID und Delivery-Metadaten |
| S06 | Drei Gesamtversuche, zwei zusätzliche Retries, begrenzter Abstand; gesunde Nachrichten laufen weiter; DLQ und bewusst reparierter Replay erhalten die fachliche ID |
| S07 | EU- und Stornierungsselektoren liefern die vorhergesagten ID-Mengen |
| S08 | Receive/Send-Rollback erzeugt keinen Ausgang; Commit bestätigt Eingang und macht Ausgang sichtbar |
| S09 | Zwei technische Sends derselben Fach-ID; harter Crash nach synchronem H2-Commit, vor JMS-Commit; nach Neustart genau ein lokaler Ledger-Effekt und JMS-Abschluss beider Zustellungen |
| S10 | Temporäre Reply-Queue, überprüfte Correlation-ID und begrenzter Timeout ohne Antwortdienst |
| S11 / S12 | Ablauf zur Expiry-Queue ohne Consumer; Delivery Delay mit Unter-/Obergrenze und Schedulingtoleranz |
| S13 | Vier Orders mit je drei Events: gleiche Workerzuordnung und Reihenfolge 1,2,3 je Gruppe im kontrollierten Smokecheck |
| S14 | Zwei Worker teilen eine Shared Durable Subscription; unabhängige zweite Subscription erhält eine vollständige eigene Kopie |
| S16 | Begrenzter Rückstau von 20 IDs, langsamer Worker und zweiter Helfer; vollständige Menge wird abgebaut |
| Aufgaben A–F | Sechs Gerüste werden im Trainingsprofil kompiliert und gepackt; sechs eigenständige Lösungen werden ausgeführt, einschließlich Durable, DLQ/Replay, Selektoren und H2-Crash-Wiederanlauf. Trainee-Implementierungen dürfen die offenen Stellen vervollständigen |
| Remote-Profil | Lokal erzeugte Testzertifikate, echtes TLS, individuelle synthetische Nutzer, eigene/fremde Namespace-Rechte, bewusste Gruppenfreigabe, eigene/fremde DLQ, temporäres Request/Reply; falscher Zertifikatshostname wird abgewiesen |
| Launcher / Konfiguration | Entpacktes Paket im Pfad mit Leerzeichen, Aufruf außerhalb des Lab-Arbeitsverzeichnisses; Exitcodes, fehlendes JDK, fehlende Artefakte, belegter Port, falsches Passwort, CLI-vor-Umgebung-vor-Datei, Build-/Runtime-Doctor |
| Aufräumen | Reset bei laufendem Broker verweigert; fremder Datenpfad, falscher Marker und Symlink-/Junction-Ziel verweigert; fremde Datei erhalten; leere eigene Queue gezielt entfernbar, aktive Consumer verhindern Cleanup |

Die Entwicklungstests fanden zunächst Producer-Credit-Warten und eine H2-Schreibpufferlücke beim harten Prozessabbruch. Die aktuelle Konfiguration benutzt synchrone persistente Sends mit Antwortfrist, PAGE und deaktiviertem Producer-Credit-Window sowie H2 `WRITE_DELAY=0`. Der abschließende Gesamtlauf enthält diese Korrekturen; fehlgeschlagene Vorläufe gelten nicht als Abnahme.

## Prüfprinzip

Die Verhaltenstests verwenden gepackte JARs in **getrennten JVMs**, echten TCP-Transport, temporäre eigene Datenpfade und Namespaces sowie einen konfliktfrei ausgewählten Port. Kein Docker und keine Testcontainers. Readiness-Signale und begrenztes Polling koordinieren asynchrone Schritte; Kindprozesse werden auch bei Fehlern beendet. Der Pakettest startet über die jeweiligen Plattformlauncher. Der Windows-Zweig verwendet cmd.exe und einen Directory-Junction-Test; beides wurde im bestätigten Windows-Job tatsächlich ausgeführt.

## Verbleibende Prüfung außerhalb des Repositories

| Plattform / Umgebung | Status |
|---|---|
| Firmenrechner → Trainer-Server | **Ausstehend**: reale Portfreigabe, DNS, TLS-Kette, individuelle Zugänge und Berechtigungen vor dem Training prüfen |

Der CI-Workflow führt für Windows und Linux dieselbe vollständige Suite mit `-Ptraining` aus. Die bestätigten Jobresultate und heruntergeladenen Berichte sind der Plattformnachweis. Physischer Stromausfall, HA, XA, produktive Dienste und verteilte Idempotenz sind kein Gegenstand der Abnahme. Das vorgebaute Paket benötigt lokal JDK 21 und kein Internet; ein Offline-Quellbuild benötigt zusätzlich vorbereiteten Wrapper-/Maven-Cache. Die paketinterne RUN.md ist der Einstieg ohne Maven. Quellcodeverweise in der vollständigen Dokumentation beziehen sich auf das separate Quellrepository.
