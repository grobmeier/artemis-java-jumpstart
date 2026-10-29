# Artemis Java Jumpstart

[![Java 21: Windows und Linux](https://github.com/grobmeier/artemis-java-jumpstart/actions/workflows/verify.yml/badge.svg)](https://github.com/grobmeier/artemis-java-jumpstart/actions/workflows/verify.yml)

Ein lokales Java-21-Trainingslab für Bestellverarbeitung mit Apache Artemis und Jakarta Messaging. Kein Docker, kein Application Server, keine echten Zahlungen oder Kundendaten. Der Broker läuft sichtbar in einer eigenen JVM; die Clients verbinden sich über TCP.

## Repository für Trainees

Das Repository ist privat: [grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart). Nach Annahme der persönlichen GitHub-Einladung klonen:

```bat
git clone https://github.com/grobmeier/artemis-java-jumpstart.git
cd artemis-java-jumpstart
```

Ohne Git kann der Quellcode nach Anmeldung über **Code → Download ZIP** geladen werden. [Zugriff, Arbeitszweige und GitHub Actions](docs/github-training.md) erklärt den Ablauf für Trainer und Trainees.

## Quickstart unter Windows (cmd.exe)

Zuerst im Repository bauen (initial mit Internetzugriff):

```bat
.\mvnw.cmd clean verify
.\scripts\doctor.cmd build
```

**Terminal 1:** Broker im Vordergrund starten. Auf `BROKER_READY` warten; das Terminal bleibt offen.

```bat
.\scripts\broker.cmd start
```

**Terminal 2:** Worker starten. Auf `READY` warten.

```bat
.\scripts\lab.cmd queue consume --worker alice --count 10
```

**Terminal 3:** Zehn persistente Bestellaufträge senden.

```bat
.\scripts\lab.cmd queue produce --count 10
```

Der Worker meldet zehn `COMMIT`s und beendet sich. Der Broker bleibt aktiv. Strg+C in Terminal 1 stoppt ihn geordnet; alternativ `scripts\broker.cmd stop` aus einem weiteren Terminal. Bei `TIMEOUT` endet ein begrenzter Consumer mit Exitcode 2, damit fehlende Nachrichten in Prüfungen auffallen. Für manuelle Experimente `--continuous` verwenden und mit Strg+C beenden.

Unter Linux/macOS dieselben Befehle mit `./mvnw` und `./scripts/*.sh` verwenden. JDK 21 muss in `JAVA_HOME` oder `PATH` stehen. Java 21 ist die geprüfte Baseline; neuere JDKs sind keine automatisch geprüfte Plattform.

## Lernweg

[Windows einrichten](docs/windows-setup.md) → [Grundbegriffe](docs/concepts.md) → [S01–S07](docs/scenarios.md) → [Aufgaben A–F](docs/exercises.md). S08–S16 sind zusätzliche Vertiefungen. Der [Trainerleitfaden](docs/trainer-guide.md) trennt Halbtag und Vertiefung. [Musterlösungen](docs/solutions.md) stehen getrennt von den Aufgaben.

## Laufzeitpaket ohne Maven

`clean verify` erzeugt `distribution/target/artemis-java-jumpstart-runtime.zip`. In ein beliebiges Verzeichnis entpacken, auch mit Leerzeichen. Die Kurzanleitung im Paket heißt `RUN.md`. Es enthält `lib/broker.jar`, `lib/lab.jar`, Skripte und Dokumentation. Ab dann benötigen lokale Demos nur ein JDK 21, weder Maven noch Internet. Das JDK wird nicht mitgeliefert. Übungen und Lösungen werden aus den Quellen separat mit `-Ptraining` gebaut.

## Konfiguration und Daten

Reihenfolge: **CLI > LAB_-Umgebungsvariablen > Properties-Datei > Defaults**. `--config` bzw. `LAB_CONFIG` wählt die Datei. Technische Schlüssel wie `dataDir` werden zu `LAB_DATA_DIR`. Standard: Loopback `127.0.0.1:61616`, Namespace `training`, öffentliche lokale Demozugangsdaten `lab` / `lab-local-only`. Diese sind ausschließlich für den lokalen Rechner bestimmt.

`--namespace team1` trennt Zielnamen, Client-IDs und Subscriptions. Innerhalb einer Übung müssen alle zusammenarbeitenden Prozesse denselben Namespace nutzen. Namespace ist lokal keine Zugriffskontrolle. [Remote](docs/remote-broker.md) verwendet TLS und individuelle berechtigte Nutzer ohne Demo-Fallback. Passwörter über geschützte lokale Konfiguration oder Umgebungsvariablen setzen; keine Geheimnisse in CLI, Git oder Logs.

Brokerdateien: `runtime/broker/` (NIO-Journal, Bindings, Paging, große Nachrichten). H2 für S09: `runtime/ledger/<namespace>.mv.db`. Nur bei gestopptem Broker: `scripts\broker.cmd reset --confirm`. Eigentumsmarker, kanonische Pfade und Prozesssperre begrenzen den Reset auf den eigenen Datenbereich. Das Ledger bleibt erhalten; separat mit `lab ledger reset --confirm` bei gestoppten Workern zurücksetzen. Für gezielte Übungen Queues leeren oder die jeweilige Subscription abmelden, siehe Szenarien.

## Prüfung und Hilfe

`doctor build` prüft Build-Voraussetzungen; `doctor runtime` prüft Artefakte, TCP und authentifizierte Brokerverbindung. [Fehlerhilfe](docs/troubleshooting.md), [Versionsquellen](docs/versions.md) und [tatsächlicher Abnahmestatus](docs/acceptance.md) beschreiben Grenzen und Ergebnisse. Ein vorbereiteter CI-Workflow ist kein bestandener Windows-Test.

## Automatische Prüfung mit GitHub Actions

Nach jedem Push und Pull Request baut GitHub das Lab mit JDK 21 auf **Windows und Linux** und führt die vollständige Testsuite einschließlich der Musterlösungen aus. Beide Prüfungen sollen grün sein, bevor Änderungen übernommen werden. Fertige Laufzeitpakete und Testberichte stehen im jeweiligen [Actions-Lauf](https://github.com/grobmeier/artemis-java-jumpstart/actions) unter **Artifacts** bereit. Die Pakete laufen anschließend lokal; für Übungen wird keine GitHub-Verbindung benötigt.

Apache License 2.0 für eigenen Code und Texte. Maven Wrapper: Apache Maven, ebenfalls Apache License 2.0. Das ZIP enthält Drittbibliotheken mit eigenen Lizenzhinweisen in den JARs; siehe NOTICE.
