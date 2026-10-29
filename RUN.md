# Laufzeitpaket starten

Dieses entpackte Paket braucht ein vorhandenes **JDK 21**, kein Maven und für lokale Demos kein Internet. Das JDK wird nicht mitgeliefert. In cmd.exe ins entpackte Verzeichnis wechseln.

**Terminal 1:**

```bat
scripts\broker.cmd start
```

Auf BROKER_READY warten; Terminal bleibt offen.

**Terminal 2:**

```bat
scripts\lab.cmd queue consume --worker alice --count 10
```

Auf READY warten.

**Terminal 3:**

```bat
scripts\lab.cmd queue produce --count 10
```

Zehn COMMITs im Worker prüfen. Broker mit Strg+C oder aus Terminal 3 mit `scripts\broker.cmd stop` geordnet beenden. Vor erneutem Start oder Reset das Prozessende abwarten.

Unter Linux/macOS `./scripts/broker.sh` beziehungsweise `./scripts/lab.sh` verwenden. Die Skripte funktionieren auch bei Leerzeichen und Aufrufen über einen absoluten Skriptpfad aus einem anderen Verzeichnis.

Diagnose: `scripts\doctor.cmd runtime`. Weitere Demos: [Szenarien](docs/scenarios.md). Das Paket enthält Demo-JARs, keine Übungsquellen; zum Implementieren der Aufgaben das vollständige Quellrepository verwenden. Lokale öffentliche Demozugangsdaten: lab / lab-local-only, ausschließlich Loopback. Remote braucht ein eigenes TLS-Profil mit individuellen Zugängen, siehe [Remote-Anleitung](docs/remote-broker.md).

Daten entstehen unter runtime/broker; Idempotenzvertiefung zusätzlich runtime/ledger. Reset nur nach Ende aller Worker und Broker: `scripts\broker.cmd reset --confirm`. Der H2-Ledger bleibt dabei erhalten. Niemals einen gemeinsam genutzten Remote-Broker als einzelne Übung zurücksetzen.
