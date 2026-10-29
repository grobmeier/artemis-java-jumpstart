# Getrennte Musterlösungen

Erst die Aufgabe selbst bearbeiten. Die Lösungen stehen im eigenständigen `solutions`-Modul mit direkten Jakarta-Messaging-Aufrufen und ohne Abhängigkeit auf `lab`. Mit `mvnw.cmd -Ptraining clean verify` bauen; Broker separat starten. `scripts\solutions.cmd` hat dieselben Argumente wie die Übungen. Pro Aufgabe frischen Namespace nutzen.

| Aufgabe | Einstieg | Aktionen | Szenario |
|---|---|---|---|
| A | [ASolution](../solutions/src/main/java/de/jumpstart/ASolution.java) | produce, consume | s02 |
| B | [BSolution](../solutions/src/main/java/de/jumpstart/BSolution.java) | produce, subscribe | s03 |
| C | [CSolution](../solutions/src/main/java/de/jumpstart/CSolution.java) | register, produce, subscribe, unsubscribe | s04 |
| D | [DSolution](../solutions/src/main/java/de/jumpstart/DSolution.java) | produce, consume, replay | s06 |
| E | [ESolution](../solutions/src/main/java/de/jumpstart/ESolution.java) und [SolutionLedger](../solutions/src/main/java/de/jumpstart/SolutionLedger.java) | produce, consume; --crash | s09 |
| F | [FSolution](../solutions/src/main/java/de/jumpstart/FSolution.java) | produce, subscribe; --selector | s07 |

Die vollständigen Terminalfolgen stehen in exercises.md. Dort `exercises.cmd` durch `solutions.cmd` ersetzen. Beispiele:

```bat
scripts\solutions.cmd A produce --count 10 --namespace solution-a
scripts\solutions.cmd A consume --count 10 --namespace solution-a
```

Für Topics Subscriber **vor** Publish starten und auf READY warten; die obigen Queue-Aufrufe sind ausdrücklich keine Topic-Startreihenfolge. Bei D sind es bei drei erzeugten Nachrichten fünf Zustellungen (zwei gesunde plus drei fehlerhafte Versuche). Für E: erst zwei gleiche fachliche IDs produzieren, dann Consumer mit `--count 1 --crash` starten (Exitcode 17), anschließend `--count 2` ohne Crash. `scripts\lab.cmd ledger show --namespace <NAME>` zeigt den einen Effekt. `WRITE_DELAY=0` macht den H2-Commit vor dem harten Prozessabbruch unmittelbar persistent.

A verwendet je Worker eigene Ressourcen und committet nach Bearbeitung. B erstellt getrennte nicht dauerhafte Subscriber. C setzt stabile Client-ID vor Start und hält die durable Subscription nach Trennung. D rollt ungültige Arbeit zurück und repariert beim Replay Payload/Property gemeinsam. E verarbeitet Unique-Verletzung gezielt und committet JMS nach erfolgreichem oder schon bekanntem lokalen Effekt. F setzt die Properties passend zum JSON und filtert mit der Messaging-Selektorsprache. Das sind Muster, keine Exactly-once-End-to-end-Garantie.

Aufräumen wie im jeweiligen Szenario: Worker beenden, bekannte Restmengen kontrolliert konsumieren, C-Subscription gezielt abmelden, E-Ledger nur bei gestoppten Workern im eigenen Namespace zurücksetzen.
