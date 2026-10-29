# Aufgaben für Trainees

Ihr baut die Auftragsverarbeitung eines kleinen Onlineshops. Zahlungen, Versand und E-Mail sind ausschließlich simuliert. Zuerst lokal arbeiten; `scripts\broker.cmd start` bleibt in Terminal 1 offen. Nach `BROKER_READY` weitere Terminals nutzen. Übungsgerüste separat mit `mvnw.cmd -Ptraining clean verify` bauen. `scripts\exercises.cmd A produce --count 10` ist ein Beispiel für den Einstiegspunkt. Der Startcode ist kompilierbar, meldet aber bis zur Implementierung die offene Übungsstelle. Offene Stellen liegen ausschließlich unter `exercises/src/main/java/de/jumpstart/*Exercise.java`.

Die ausführbaren Demos dienen als beobachtbare Referenz, die [Musterlösungen](solutions.md) liegen getrennt. Nutzt pro Aufgabe einen neuen Namespace und gebt ihn bei allen beteiligten Clients identisch an. Die fertigen Demo- und Lösungsziele sind gleich: keine Demo-Reste mit einer neuen Aufgabe vermischen. `--count` am Consumer zählt Zustellungen, inklusive Rollbacks. Bei zwei Workern zunächst `--continuous` nutzen, nach vollständiger ID-Menge stoppen und die Queue prüfen.

## Aufgabe A – Bestellungen abarbeiten

Ihr implementiert zehn Bestellungen für orders.incoming im eigenen Namespace (S02). Sendet zuerst ohne Worker. Startet anschließend einen, dann für einen frischen Lauf zwei Worker mit kleiner Bearbeitungszeit.

**Zeit:** 35 Minuten. **Voraussetzungen:** Klassen, Schleifen, Exceptions; Konzepte Producer, Consumer und Queue.

**Startcode:** [AExercise.java](../exercises/src/main/java/de/jumpstart/AExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 3 zuerst:**

```bat
scripts\exercises.cmd A produce --count 10 --namespace task-a
```

**Terminal 2 danach:**

```bat
scripts\exercises.cmd A consume --worker alice --count 10 --namespace task-a
```

**Zweiter Lauf, Terminal 2 und 3, je eigener Aufruf:**

```bat
scripts\exercises.cmd A consume --worker alice --continuous --sleep 150 --namespace task-a2
scripts\exercises.cmd A consume --worker bob --continuous --sleep 150 --namespace task-a2
```

**Terminal 4 nach READY beider:**

```bat
scripts\exercises.cmd A produce --count 10 --namespace task-a2
```

**Erfolgskriterien:** Alle zehn fachlichen IDs sind im fehlerfreien Lauf genau einmal als COMMIT sichtbar; beide Worker beteiligen sich. Am Ende s02 browse im jeweiligen Namespace: visible=0. Die Lastverteilung muss nicht gleich sein.

**Hinweis 1:** Legt die Destination mit session.createQueue an. Ein TextMessage enthält JSON, ein Producer sendet persistent. Öffnet je Worker eigene Verbindung und Session.

**Hinweis 2:** Schreibt eine begrenzte receive(timeout)-Schleife, simuliert die Arbeit vor session.commit() und protokolliert mit Messages.log. Nutzt für den zweiten Lauf Dauerbetrieb statt vorgegebener Quote je Worker.

**Typische Fehler:** Falscher Namespace, Consumer zählt nur einen Teil und wartet auf eine starre Hälfte, fehlender Commit.

**Reflexionsfragen:** Was passiert ohne Consumer? Warum bekommen zwei Worker nicht beide alle Aufträge? Was ist bei einem Absturz nach Receive möglich?

## Aufgabe B – Zwei unabhängige Interessenten

Publiziert OrderCreated auf ein Topic (S03). Rechnungs- und Versandservice brauchen getrennte aktive Subscriptions. Ein dritter Interessent wird erst später angelegt.

**Zeit:** 30 Minuten. **Voraussetzungen:** A; Unterschied Auftrag/Event und Bereitschaft.

**Startcode:** [BExercise.java](../exercises/src/main/java/de/jumpstart/BExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 2:**

```bat
scripts\exercises.cmd B subscribe --worker billing --subscription billing --count 3 --namespace task-b
```

**Terminal 3:**

```bat
scripts\exercises.cmd B subscribe --worker shipping --subscription shipping --count 3 --namespace task-b
```

**Terminal 4 nach READY beider:**

```bat
scripts\exercises.cmd B produce --count 3 --namespace task-b
```

**Terminal 2 nach Ende:**

```bat
scripts\exercises.cmd B subscribe --worker late --subscription late --count 2 --namespace task-b
```

**Terminal 4 nach READY von late:**

```bat
scripts\exercises.cmd B produce --count 2 --namespace task-b
```

**Erfolgskriterien:** Beide zuerst aktiven Services erhalten dieselben drei IDs vollständig. Late erhält nur die zwei zukünftigen IDs.

**Hinweis 1:** createTopic und createConsumer verwenden; jede Verbindung erzeugt eine eigene nicht dauerhafte Subscription.

**Hinweis 2:** Consumer anlegen, connection.start() ausführen und READY ausgeben, bevor der Producer sendet. Der Subscription-Name im CLI ist hier ein Lernlabel, keine durable Identität.

**Typische Fehler:** Publish vor Bereitschaft, zwei Services an derselben Queue, Gleichsetzen von Topic mit gespeicherter Historie.

**Reflexionsfragen:** Warum erfüllt eine Queue mit zwei Consumern diese Anforderung nicht? Welche Events fehlen dem spät gestarteten Service?

## Aufgabe C – Der Versandservice ist offline

Registriert zuerst eine durable shipping-Subscription an S04, trennt sie, sendet drei Events und verbindet sie erneut. Wiederholt den Lauf mit geordnetem Brokerneustart.

**Zeit:** 25 Minuten. **Voraussetzungen:** B; persistente Events und stabile Identität.

**Startcode:** [CExercise.java](../exercises/src/main/java/de/jumpstart/CExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 2:**

```bat
scripts\exercises.cmd C register --subscription shipping --namespace task-c
```

**Terminal 3 nach Ende von register:**

```bat
scripts\exercises.cmd C produce --count 3 --namespace task-c
```

**Terminal 2 danach:**

```bat
scripts\exercises.cmd C subscribe --subscription shipping --count 3 --namespace task-c
```

**Wiederholung: Terminal 3 nach erneutem register und produce:**

```bat
scripts\broker.cmd stop
```

**Terminal 1 nach Brokerende:**

```bat
scripts\broker.cmd start
```

**Erfolgskriterien:** Jeweils drei wartende IDs kommen an; beim Neustart denselben Datenpfad und persistente Events verwenden. Abschließend erneut denselben subscribe-Aufruf ausführen.

**Hinweis 1:** Client-ID vor dem Start setzen; createDurableConsumer mit einem stabilen Subscription-Namen nutzen. register darf den Consumer danach schließen.

**Hinweis 2:** Identität besteht aus Client-ID und Subscription-Name. Verwendet Namespace und Szenario konsistent in beiden; zum Beispiel task-c.s04.client-shipping und task-c.s04.shipping.

**Typische Fehler:** Client-ID nach Verbindungsstart setzen, Namen wechseln, mit zwei aktiven ungeteilten Durable Consumern derselben Identität verbinden.

**Reflexionsfragen:** Was ändert ein neuer Name oder eine neue Client-ID? Warum sind persistente Message und durable Subscription verschiedene Eigenschaften?

## Aufgabe D – Fehler und Gift-Nachrichten

Ein Worker scheitert bei order-2, dessen priorityClass im Test invalid ist. Verwendet die transacted Session und insgesamt drei Versuche mit 500 ms Abstand (BrokerRuntime). Gesunde Aufträge bleiben verarbeitbar. Untersucht die DLQ und sendet bewusst eine reparierte Version.

**Zeit:** 45 Minuten. **Voraussetzungen:** A–C; Transaktionen, Rollback und Versuchszähler.

**Startcode:** [DExercise.java](../exercises/src/main/java/de/jumpstart/DExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 3:**

```bat
scripts\exercises.cmd D produce --count 3 --namespace task-d
```

**Terminal 2:**

```bat
scripts\exercises.cmd D consume --count 5 --namespace task-d
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd s06 browse --target dlq --namespace task-d
scripts\exercises.cmd D replay --count 1 --namespace task-d
```

**Terminal 2 danach:**

```bat
scripts\exercises.cmd D consume --count 1 --namespace task-d
```

**Erfolgskriterien:** Zwei gesunde COMMITs, drei Versuche für die fehlerhafte ID und anschließend genau ein passender DLQ-Eintrag. Reparierter Replay behält die fachliche ID, bekommt einen neuen technischen Send und lässt sich committen.

**Hinweis 1:** Bei invalid rollback statt commit. count zählt Zustellungen: drei ursprüngliche Nachrichten plus zwei zusätzliche Retries ergeben fünf Receives.

**Hinweis 2:** DLQ ist DLQ::DLQ.<namespace>.s06.orders.incoming. Read und reparierter Send lassen sich in derselben JMS-Transaktion verbinden. Payload UND Property korrigieren.

**Typische Fehler:** Nach Rollback trotzdem committen, drei Retries statt drei Gesamtversuche konfigurieren, Fehler durch endloses Wiederholen verdecken.

**Reflexionsfragen:** Was unterscheidet technischen Fehler, fachlich ungültigen Auftrag und endlose Retry-Schleife? Warum muss Replay bewusst geschehen?

## Aufgabe E – Dieselbe Bestellung zweimal

Erzeugt dieselbe fachliche ID zweimal (S09). Speichert den lokalen Buchungseffekt und die Idempotenz-ID atomar in H2. Löst gezielt den Crash nach DB-Commit und vor JMS-Commit aus. Startet den Worker neu.

**Zeit:** 60 Minuten, Vertiefung nach D. **Voraussetzungen:** D; JDBC-Transaktion und Unique Constraint. H2-Dependency im exercises-Modul ergänzen, Version aus dem Parent-POM übernehmen.

**Startcode:** [EExercise.java](../exercises/src/main/java/de/jumpstart/EExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 3:**

```bat
scripts\exercises.cmd E produce --count 2 --id same-order --namespace task-e
```

**Terminal 2:**

```bat
scripts\exercises.cmd E consume --count 1 --crash --namespace task-e
```

**Terminal 2 nach Exitcode 17:**

```bat
scripts\exercises.cmd E consume --count 2 --namespace task-e --timeout 10000
```

**Erfolgskriterien:** Mehrere technische Zustellungen, aber genau ein lokaler Ledger-Eintrag; beide Nachrichten haben dieselbe Fach-ID und verschiedene JMSMessageIDs. Nach Neustart den JMS-Commit für bereits bekannte IDs nachholen.

**Hinweis 1:** Ein INSERT in eine Tabelle mit business_id als Primärschlüssel enthält zugleich order_id und amount_cents als simulierten Effekt. DB-Autocommit ausschalten.

**Hinweis 2:** JDBC-URL mit WRITE_DELAY=0 und DB-Commit vor dem Crash. Unique-Verletzung SQLState 23505 behandeln und DB-Rollback ausführen. Erst danach JMS committen. Andere SQL-Fehler nicht als Duplikat verschlucken.

**Typische Fehler:** ID und Effekt separat committen, nur ein Java-Set verwenden, nach Runtime.halt ungeflushte H2-Schreibpuffer verlieren.

**Reflexionsfragen:** Warum überlebt ein Java-Set keinen Neustart? Wo liegt die Lücke bei zwei nicht atomaren DB-Operationen? Warum folgt daraus keine Sicherheit für echte E-Mails oder Zahlungen?

## Aufgabe F – Nur relevante Ereignisse

Publiziert sechs Events auf S07. Odd Orders sind EU, gerade Orders US; jede dritte ist OrderCancelled, die übrigen OrderCreated. Sagt die IDs beider Subscriber vor dem Start voraus.

**Zeit:** 30 Minuten. **Voraussetzungen:** B; Message-Properties und Selektoren.

**Startcode:** [FExercise.java](../exercises/src/main/java/de/jumpstart/FExercise.java). Die dortige TODO-Stelle mit direkten Jakarta-Messaging-Aufrufen vervollständigen. Konfiguration, JSON und Ausgabe aus common dürfen genutzt werden.

**Terminal 2:**

```bat
scripts\exercises.cmd F subscribe --worker eu --selector "region = 'EU'" --count 3 --namespace task-f
```

**Terminal 3:**

```bat
scripts\exercises.cmd F subscribe --worker cancellations --selector "eventType = 'OrderCancelled'" --count 2 --namespace task-f
```

**Terminal 4 nach READY beider:**

```bat
scripts\exercises.cmd F produce --count 6 --namespace task-f
```

**Erfolgskriterien:** EU erhält fachliche IDs der Orders 1,3,5; Stornierungen die der Orders 3,6. Vergleicht Mengen, nicht die Logreihenfolge.

**Hinweis 1:** setStringProperty auf der Message setzen; JSON und Properties müssen dieselben fachlichen Werte ausdrücken.

**Hinweis 2:** createConsumer(topic, selector) verwenden. Selektorwerte werden als Zeichenketten in einfachen Anführungszeichen geschrieben; der gesamte Ausdruck steht im cmd-Aufruf in doppelten Anführungszeichen.

**Typische Fehler:** Filter liest nur JSON, falsche Groß-/Kleinschreibung oder quotes, Publish vor READY.

**Reflexionsfragen:** Was ist der Unterschied zwischen Payload und Properties? Warum kann dieselbe Order bei beiden Subscriptions erscheinen?
