# Trainerleitfaden

## Vorbereitung

JDK 21 bereitstellen; auf echten Firmenrechnern Pfad, `doctor build`, `clean verify -Ptraining` und Laufzeit-ZIP in einem Pfad mit Leerzeichen vorab prüfen. Drei bis fünf cmd-Terminals vorbereiten. Paket und eventuell Maven-Cache für Offline-Ausweichplan verteilen. Proxy und Zertifikate durch die zuständige IT konfigurieren lassen; keine Ausführungsrichtlinien ändern. Remote-Probe vor dem Training separat mit TCP, TLS und Authentifizierung durchführen. Quellen, Buildresultate und den ehrlichen Plattformstatus aus acceptance.md lesen.

## Kompakter Halbtag: 3 Stunden 50 Minuten

| Abschnitt | Minuten |
|---|---:|
| Vorab geprüfte Umgebung starten, Lernziele | 15 |
| Begriffe und geführte S01-Demo | 25 |
| Aufgabe A plus Vergleich mit zwei Workern | 35 |
| Aufgabe B und spätes Abonnieren | 30 |
| Pause | 15 |
| Aufgabe C plus Neustart | 25 |
| Geführte Fehlerdemo aus D / S05 und S06 | 30 |
| Reflexion, Vergleich der Beobachtungen | 35 |
| Ausblick und Aufräumen | 20 |

Nicht alle 16 Szenarien in den Halbtag pressen. D hier führen, noch nicht vollständig selbst implementieren lassen.

## Vertiefung: weitere 3 Stunden 30 Minuten

D selbst implementieren (45), E mit Crash/Recovery (60), Pause (15), F (30), ausgewählte S08/S10–S16 je Interesse (40), Auswertung und Cleanup (20 Minuten). Bei weniger JDBC-Erfahrung E gemeinsam lesen und den lokalen Effekt statt zusätzlicher Infrastruktur bearbeiten.

## Zwei Trainees

Driver schreibt, Navigator sagt die erwartete Beobachtung voraus und prüft IDs. Rollen alle 15–20 Minuten tauschen. Zunächst jeweils eigenen lokalen Broker und Namespace verwenden. Optional später am zentralen Broker bewusst einen vom Trainer autorisierten Gruppenbereich wählen: A/S02 und Message Groups brauchen für gemeinsame Arbeit dieselbe Queue; B/S03 braucht dieselbe Topic-Address, aber unabhängige Subscriptions. C verwendet stabile Identitäten; zwei ungeteilte Durable Consumer derselben Identität dürfen nicht gleichzeitig laufen. E nutzt pro Rechner ein lokales H2-Ledger und ist damit kein gemeinsames verteiltes Idempotenzsystem.

Getrennte persönliche Übungen behalten isolierte autorisierte Namespaces. `--namespace` allein verschafft keine Berechtigung. Gruppenfreigabe im Trainer-Nutzerprofil muss ausdrücklich gesetzt sein. Keine fremden Subscriptions oder Nachrichten löschen.

## Demo-Reihenfolge und Wiederholung

S01: ohne Consumer senden → Queue prüfen → konsumieren. S02: beide READY abwarten. S03: beide aktiven Subscriptions und späten Subscriber zeigen. S04: registrieren → offline senden → wieder verbinden → separat mit Neustart. S05: Rollback und Crash unterscheiden. S06: gesunde Nachrichten plus drei Versuche, DLQ lesen und reparieren. S07: Vorhersage der Mengen vor dem Publish. Dann erst Vertiefungen.

Zwischen Läufen frischen Namespace wählen oder Ziele kontrolliert leeren und Durable Subscriptions abmelden. Für einen vollständig neuen lokalen Durchgang alle Worker stoppen, Broker stoppen und auf Prozessende warten, dann `scripts\broker.cmd reset --confirm`. Ledger bei gestoppten Workern separat pro Namespace mit `scripts\lab.cmd ledger reset --confirm` zurücksetzen. Der Reset entfernt ausschließlich den markierten Broker-Datenbereich, keine fremden Dateien und kein Ledger. Marker-/Pfadfehler zuerst untersuchen; den Schutz nicht umgehen.

Bei blockiertem Netzwerk das vorgebaute lokale ZIP mit JDK nutzen. Fehlende Remote-Erreichbarkeit ist kein Anlass, Unternehmensproxy oder Firewall zu umgehen. Bei Buildproblemen vorbereiteten Wrapper-/Maven-Cache oder Laufzeitpaket verwenden; den Unterschied zwischen Offline-Run und Offline-Quellbuild erklären.

## Bewertung und Unterstützung je Aufgabe

Mengen fachlicher IDs, beobachtbare Zustände und begründete Erklärungen bewerten. Identische Logreihenfolge und exakte Lastverteilung sind keine Kriterien. Die zwei gestuften Hinweise stehen bei jeder Aufgabe in exercises.md; erst Hinweis 1, später Hinweis 2 anbieten.

### A – Bestellungen abarbeiten

Zeit: 35 Minuten. Voraussetzungen: Klassen, Schleifen, Exceptions; Konzepte Producer, Consumer und Queue.

Typische Fehler: Falscher Namespace, Consumer zählt nur einen Teil und wartet auf eine starre Hälfte, fehlender Commit.

Erwartete Antworten: Ohne Worker wartet Arbeit im Broker. Consumer an derselben Queue teilen Zustellungen. Receive allein ist noch keine Bestätigung.

Hinweis 1: Legt die Destination mit session.createQueue an. Ein TextMessage enthält JSON, ein Producer sendet persistent. Öffnet je Worker eigene Verbindung und Session.

Hinweis 2: Schreibt eine begrenzte receive(timeout)-Schleife, simuliert die Arbeit vor session.commit() und protokolliert mit Messages.log. Nutzt für den zweiten Lauf Dauerbetrieb statt vorgegebener Quote je Worker.

Mögliche Anschlussfrage: Was würde ein größerer Consumer Window an der Verteilung ändern?

### B – Zwei unabhängige Interessenten

Zeit: 30 Minuten. Voraussetzungen: A; Unterschied Auftrag/Event und Bereitschaft.

Typische Fehler: Publish vor Bereitschaft, zwei Services an derselben Queue, Gleichsetzen von Topic mit gespeicherter Historie.

Erwartete Antworten: Jeder unabhängige Interessent hat eine eigene Subscription-Queue. Ein später nicht dauerhaft angelegter Subscriber kennt die Vergangenheit nicht.

Hinweis 1: createTopic und createConsumer verwenden; jede Verbindung erzeugt eine eigene nicht dauerhafte Subscription.

Hinweis 2: Consumer anlegen, connection.start() ausführen und READY ausgeben, bevor der Producer sendet. Der Subscription-Name im CLI ist hier ein Lernlabel, keine durable Identität.

Mögliche Anschlussfrage: Wie wird ein Interessent offline erreichbar?

### C – Der Versandservice ist offline

Zeit: 25 Minuten. Voraussetzungen: B; persistente Events und stabile Identität.

Typische Fehler: Client-ID nach Verbindungsstart setzen, Namen wechseln, mit zwei aktiven ungeteilten Durable Consumern derselben Identität verbinden.

Erwartete Antworten: Client-ID und Subscription-Name sind die ungeteilte durable Identität. Eine neue Identität erzeugt eine andere Subscription. Dauerhaftigkeit der Subscription und Persistenz der Nachricht sind getrennt.

Hinweis 1: Client-ID vor dem Start setzen; createDurableConsumer mit einem stabilen Subscription-Namen nutzen. register darf den Consumer danach schließen.

Hinweis 2: Identität besteht aus Client-ID und Subscription-Name. Verwendet Namespace und Szenario konsistent in beiden; zum Beispiel task-c.s04.client-shipping und task-c.s04.shipping.

Mögliche Anschlussfrage: Welche Identität bleibt bei Deployment oder Rechnerwechsel stabil?

### D – Fehler und Gift-Nachrichten

Zeit: 45 Minuten. Voraussetzungen: A–C; Transaktionen, Rollback und Versuchszähler.

Typische Fehler: Nach Rollback trotzdem committen, drei Retries statt drei Gesamtversuche konfigurieren, Fehler durch endloses Wiederholen verdecken.

Erwartete Antworten: Drei Gesamtversuche bedeuten zwei Retries. Technische Fehler können vorübergehend sein; fachliche Ungültigkeit braucht Korrektur. DLQ erhält die Nachricht zur Analyse statt endlos zu kreisen.

Hinweis 1: Bei invalid rollback statt commit. count zählt Zustellungen: drei ursprüngliche Nachrichten plus zwei zusätzliche Retries ergeben fünf Receives.

Hinweis 2: DLQ ist DLQ::DLQ.<namespace>.s06.orders.incoming. Read und reparierter Send lassen sich in derselben JMS-Transaktion verbinden. Payload UND Property korrigieren.

Mögliche Anschlussfrage: Welche Informationen braucht ein sicheres Replay-Werkzeug?

### E – Dieselbe Bestellung zweimal

Zeit: 60 Minuten, Vertiefung nach D. Voraussetzungen: D; JDBC-Transaktion und Unique Constraint. H2-Dependency im exercises-Modul ergänzen, Version aus dem Parent-POM übernehmen.

Typische Fehler: ID und Effekt separat committen, nur ein Java-Set verwenden, nach Runtime.halt ungeflushte H2-Schreibpuffer verlieren.

Erwartete Antworten: Primärschlüssel und Effekt müssen gemeinsam committen. Die DB überlebt den Prozessneustart, ein Set nicht. DB und JMS committen getrennt; Duplikate schließen die Lücke für diesen lokalen Effekt.

Hinweis 1: Ein INSERT in eine Tabelle mit business_id als Primärschlüssel enthält zugleich order_id und amount_cents als simulierten Effekt. DB-Autocommit ausschalten.

Hinweis 2: JDBC-URL mit WRITE_DELAY=0 und DB-Commit vor dem Crash. Unique-Verletzung SQLState 23505 behandeln und DB-Rollback ausführen. Erst danach JMS committen. Andere SQL-Fehler nicht als Duplikat verschlucken.

Mögliche Anschlussfrage: Was müsste für eine externe Zahlung zusätzlich idempotent sein?

### F – Nur relevante Ereignisse

Zeit: 30 Minuten. Voraussetzungen: B; Message-Properties und Selektoren.

Typische Fehler: Filter liest nur JSON, falsche Groß-/Kleinschreibung oder quotes, Publish vor READY.

Erwartete Antworten: Properties werden gefiltert; JSON bleibt Nutzlast. Order 3 erfüllt beide Bedingungen und gehört deshalb in beide erwarteten Mengen.

Hinweis 1: setStringProperty auf der Message setzen; JSON und Properties müssen dieselben fachlichen Werte ausdrücken.

Hinweis 2: createConsumer(topic, selector) verwenden. Selektorwerte werden als Zeichenketten in einfachen Anführungszeichen geschrieben; der gesamte Ausdruck steht im cmd-Aufruf in doppelten Anführungszeichen.

Mögliche Anschlussfrage: Was passiert, wenn Payload und Property widersprüchlich sind?
