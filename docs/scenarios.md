# Ausführbare Szenarien

S01–S07 bilden den Pflichtlernweg. S08–S16 sind Vertiefungen. Alle Befehle sind für **cmd.exe** aus dem Lab-Verzeichnis angegeben. Unter Linux/macOS `scripts\lab.cmd` durch `./scripts/lab.sh` und `scripts\broker.cmd` durch `./scripts/broker.sh` ersetzen. Broker zuerst in **Terminal 1** mit `scripts\broker.cmd start` starten und auf `BROKER_READY` warten. Er bleibt während der unten genannten Terminalaufrufe aktiv. Wo `READY` gefordert ist, erst danach senden.

`--count` bei Consumern zählt **Zustellungen**, einschließlich zurückgerollter Versuche. Fehlende Nachrichten bis `--timeout` (Millisekunden pro Empfang, Standard 5000) ergeben Exitcode 2. `--continuous` ist der ausdrückliche Dauerbetrieb. Bei Worker-Verteilung darf die individuelle Menge unbekannt sein; dann Dauerbetrieb verwenden und erst nach vollständiger Gesamtmenge stoppen. `--sleep` simuliert Bearbeitungszeit in Millisekunden. Producer erzeugen pro Aufruf neue fachliche IDs; `--id` setzt absichtlich eine feste ID.

Jedes Szenario hat eigene Ziele `<namespace>.sNN.orders.incoming` oder `.orders.events`; Standardnamespace `training`. Für jeden Wiederholungslauf einen frischen Namespace in **allen** Befehlen wählen, oder gezielt leeren. Bei konkurrierenden Clients alle im selben Szenario und Namespace starten. Aufräumen bedeutet: Worker beenden, restliche Queue-Nachrichten kontrolliert konsumieren, Durable Subscriptions abmelden. Leere Queue-Ressourcen ohne Consumer können zusätzlich gezielt mit `scripts\lab.cmd sNN cleanup --confirm [--target incoming|outgoing|dlq|expiry]` entfernt werden; Platzhalter vorher ersetzen. Topic-Ressourcen über ihre Subscription-Identität abmelden. Der vollständige Broker-Reset ist nur für ein allein genutztes lokales Lab mit gestopptem Broker geeignet; nie als Cleanup eines geteilten Remote-Brokers.

## S01 – Einfache Queue

**Zweck:** Zehn persistente Aufträge warten auch ohne Consumer.

**Topologie:** Producer → dauerhafte Anycast-Queue → Worker. Eigener Zielbereich `training.s01.*`.

**Terminal 3 zuerst:**

```bat
scripts\lab.cmd s01 produce --count 10
scripts\lab.cmd s01 browse
```

**Terminal 2 danach:**

```bat
scripts\lab.cmd s01 consume --worker alice --count 10
```

**Terminal 3 zuletzt:**

```bat
scripts\lab.cmd s01 browse
```

**Erwartung:** Vorher visible=10, danach zehn COMMITs und visible=0.

**Failure-Experiment:** Ohne Consumer senden und Producer beenden; die Aufträge bleiben.

**Aufräumen:** Queue mit der bekannten Restmenge konsumieren.

## S02 – Competing Consumers

**Zweck:** Zwei Worker teilen Arbeit statt Kopien zu erhalten.

**Topologie:** Producer → eine Queue → Alice / Bob. Eigener Zielbereich `training.s02.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s02 consume --worker alice --continuous --sleep 150
```

**Terminal 3:**

```bat
scripts\lab.cmd s02 consume --worker bob --continuous --sleep 150
```

**Terminal 4, nach READY beider Worker:**

```bat
scripts\lab.cmd s02 produce --count 10
```

**Erwartung:** Zehn fachliche IDs insgesamt; beide Worker beteiligen sich. Keine exakt hälftige oder abwechselnde Verteilung voraussetzen.

**Failure-Experiment:** Einen Worker während receive stoppen; der andere kann weitere Arbeit übernehmen. Abbruch nach Receive kann Wiederzustellung erzeugen.

**Aufräumen:** Nach zehn COMMITs beide Worker stoppen; s02 browse muss visible=0 zeigen.

## S03 – Nicht dauerhafte Topic-Subscriptions

**Zweck:** Aktive unabhängige Interessenten erhalten Kopien.

**Topologie:** Topic → Rechnung-Subscription + Versand-Subscription. Eigener Zielbereich `training.s03.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s03 subscribe --subscription billing --worker billing --count 3
```

**Terminal 3:**

```bat
scripts\lab.cmd s03 subscribe --subscription shipping --worker shipping --count 3
```

**Terminal 4, nach READY beider Services:**

```bat
scripts\lab.cmd s03 produce --count 3
```

**Terminal 2, nach Ende des ersten Laufs:**

```bat
scripts\lab.cmd s03 subscribe --subscription late --worker late --count 2
```

**Terminal 4, nach READY von late:**

```bat
scripts\lab.cmd s03 produce --count 2
```

**Erwartung:** Rechnung und Versand erhalten je drei gleiche IDs; late nur die zwei zukünftigen.

**Failure-Experiment:** Vor Anlegen des late-Subscribers weitere Events senden: diese erscheinen bei ihm nicht.

**Aufräumen:** Nicht dauerhafte Subscriptions verschwinden beim Trennen; keine dauerhafte Historie.

## S04 – Durable Topic-Subscription

**Zweck:** Registrieren, offline sein, später dieselbe Identität nutzen.

**Topologie:** Topic → dauerhafte shipping-Subscription. Eigener Zielbereich `training.s04.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s04 register --subscription shipping
```

**Terminal 3, nach Ende von register:**

```bat
scripts\lab.cmd s04 produce --count 3
```

**Terminal 2 danach:**

```bat
scripts\lab.cmd s04 subscribe --subscription shipping --count 3
```

**Terminal 2, zweiter Lauf:**

```bat
scripts\lab.cmd s04 register --subscription shipping
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd s04 produce --count 3
scripts\broker.cmd stop
```

**Terminal 1, nach Brokerende:**

```bat
scripts\broker.cmd start
```

**Terminal 2, nach BROKER_READY:**

```bat
scripts\lab.cmd s04 subscribe --subscription shipping --count 3
```

**Erwartung:** Je drei Offline-Events kommen an, im zweiten Lauf auch nach geordnetem Neustart. Client-ID training.s04.client-shipping und Subscription training.s04.shipping bleiben identisch.

**Failure-Experiment:** Mit --subscription other verbinden: neue Identität erhält nicht die wartenden Events von shipping; begrenzter Empfang endet mit Timeout.

**Aufräumen:** scripts\lab.cmd s04 unsubscribe --subscription shipping; eventuell versehentlich angelegtes other ebenfalls abmelden, nachdem dessen Consumer beendet ist.

## S05 – Acknowledgement und Wiederzustellung

**Zweck:** Receive allein bestätigt keinen Fachabschluss.

**Topologie:** Queue → transacted Worker → Rollback oder Crash. Eigener Zielbereich `training.s05.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s05 produce --count 1 --id ack-demo
```

**Terminal 2:**

```bat
scripts\lab.cmd s05 consume --count 1 --rollback
```

**Terminal 2 danach:**

```bat
scripts\lab.cmd s05 consume --count 1 --timeout 10000
```

**Erwartung:** Dieselbe fachliche ID wird erneut empfangen, typischerweise mit JMSRedelivered=true und attempt=2.

**Failure-Experiment:** Statt --rollback im ersten Worker --crash verwenden (erwarteter Exitcode 17); die Brokererkennung kann einige Sekunden benötigen.

**Aufräumen:** Nach erfolgreichem Commit ist die Queue leer; Grundlagen zu AUTO_ACKNOWLEDGE und sessionweitem CLIENT_ACKNOWLEDGE in concepts.md lesen.

## S06 – Retry und Dead Letter

**Zweck:** Die zweite von drei Orders ist absichtlich ungültig.

**Topologie:** Eingangsqueue → maximal drei Versuche → DLQ → reparierter Replay. Eigener Zielbereich `training.s06.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s06 produce --count 3
```

**Terminal 2:**

```bat
scripts\lab.cmd s06 consume --count 5
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd s06 browse --target dlq
scripts\lab.cmd s06 replay --count 1
```

**Terminal 2 danach:**

```bat
scripts\lab.cmd s06 consume --count 1
```

**Erwartung:** Zwei gesunde COMMITs, drei ROLLBACKs für order-2, 500 ms Verzögerung zwischen Versuchen; danach eine DLQ-Nachricht. Replay repariert priorityClass und behält die ID.

**Failure-Experiment:** Ohne Reparatur würde dieselbe ungültige Fachinformation wieder scheitern. Drei Versuche sind zwei zusätzliche Retries, nicht drei Retries.

**Aufräumen:** s06 browse und s06 browse --target dlq müssen visible=0 zeigen; DLQ nur gezielt lesen, nicht fremde Fehler löschen.

## S07 – Selektoren und fachliches Routing

**Zweck:** Eigenschaften steuern die jeweilige Sicht.

**Topologie:** Topic → EU-Filter + Stornierungsfilter. Eigener Zielbereich `training.s07.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s07 subscribe --worker eu --selector "region = 'EU'" --count 3
```

**Terminal 3:**

```bat
scripts\lab.cmd s07 subscribe --worker cancellations --selector "eventType = 'OrderCancelled'" --count 2
```

**Terminal 4, nach READY beider:**

```bat
scripts\lab.cmd s07 produce --count 6
```

**Erwartung:** Vorhersage: EU sind Order 1, 3, 5; Stornierungen sind 3 und 6. Die fachlichen IDs enden entsprechend auf -N-1.

**Failure-Experiment:** Selektor auf nicht vorhandene Property setzen: kein Treffer, Timeout; JSON-Feld allein wäre nicht ausreichend.

**Aufräumen:** Nicht dauerhafte Subscriptions enden mit ihren Verbindungen.

## S08 – Lokale Messaging-Transaktion

**Zweck:** Receive und Send gelten atomar gegenüber demselben Broker.

**Topologie:** Queue A → eine transacted Session → Queue B. Eigener Zielbereich `training.s08.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s08 produce --count 1
```

**Terminal 2:**

```bat
scripts\lab.cmd s08 transfer --count 1 --rollback
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd s08 browse --target outgoing
```

**Terminal 2 danach:**

```bat
scripts\lab.cmd s08 transfer --count 1
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd s08 consume --target outgoing --count 1
scripts\lab.cmd s08 browse
```

**Erwartung:** Rollback lässt keinen Ausgang sichtbar werden. Commit bestätigt Eingang und erzeugt Ausgang; am Ende Eingang leer.

**Failure-Experiment:** --rollback mehrfach ausführen: nach dem Versuchslimit kann der Eingang zur DLQ gehen. Für den Vergleich genau einmal zurückrollen.

**Aufräumen:** Beide Queues kontrolliert leeren; das ist keine DB- oder verteilte Transaktion.

## S09 – Idempotente Verarbeitung

**Zweck:** Zwei technische Sends teilen eine fachliche ID.

**Topologie:** Queue → H2-Transaktion → JMS-Commit. Eigener Zielbereich `training.s09.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s09 produce --count 2 --id ledger-demo
```

**Terminal 2:**

```bat
scripts\lab.cmd s09 consume --count 1 --crash
```

**Terminal 2 nach Exitcode 17:**

```bat
scripts\lab.cmd s09 consume --count 2 --timeout 10000
```

**Terminal 3 danach:**

```bat
scripts\lab.cmd ledger show
```

**Erwartung:** Crash erfolgt nach DB-Commit, vor JMS-Commit. Nach Neustart zwei DUPLICATE-Beobachtungen mit verschiedenen JMSMessageIDs und genau ein Ledger-Eintrag (bei frischem Namespace).

**Failure-Experiment:** Ledger erst nach dem Test bei gestoppten Workern zurücksetzen und erneut senden: der Effekt wäre wieder möglich. Das zeigt die Bedeutung dauerhafter IDs.

**Aufräumen:** scripts\lab.cmd ledger reset --confirm nur im eigenen Namespace bei gestoppten Workern; Queue vorher leer prüfen.

## S10 – Request Reply

**Zweck:** JMSReplyTo und geprüfte Correlation ordnen die Antwort zu.

**Topologie:** Request-Queue → Antwortdienst → temporäre Reply-Queue. Eigener Zielbereich `training.s10.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s10 reply --count 1
```

**Terminal 3, nach READY:**

```bat
scripts\lab.cmd s10 request --timeout 3000
```

**Erwartung:** Antwort besitzt JMSCorrelationID gleich JMSMessageID des Requests. Temporäre Queue wird bei Ende der erzeugenden Verbindung entfernt.

**Failure-Experiment:** Ohne Antwortdienst s10 request --timeout 500 aufrufen: Exitcode 2. Bereits gesendeter Request bleibt; spätere Antwort findet kein lebendes temporäres Ziel.

**Aufräumen:** Nach Timeout verbliebene Requests mit s10 consume --count <REST> bewusst konsumieren; keinen verspäteten Antwortdienst auf verwaiste Requests loslassen.

## S11 – Time to Live und Expiry

**Zweck:** Veraltete Arbeit endet unabhängig von Verarbeitungsfehlern.

**Topologie:** Queue ohne Consumer → TTL → Expiry-Queue. Eigener Zielbereich `training.s11.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s11 produce --count 1 --ttl 500
```

**Terminal 3 nach etwa einer Sekunde:**

```bat
scripts\lab.cmd s11 browse
scripts\lab.cmd s11 browse --target expiry
```

**Erwartung:** Eingang leer, Expiry enthält die fachliche ID. Scanintervall 100 ms; Betriebssystem und Scheduling brauchen Toleranz.

**Failure-Experiment:** Einen Worker vor Ablauf starten: er kann die noch gültige Nachricht konsumieren, dann entsteht kein Expiry-Eintrag.

**Aufräumen:** Für Wiederholung frischen Namespace nutzen; abgelaufene Nachrichten gezielt mit s11 consume --target expiry --count 1 entfernen.

## S12 – Verzögerte Zustellung

**Zweck:** Delivery-Delay betrifft die erste Zustellung.

**Topologie:** Producer → geplante Verfügbarkeit → Queue-Consumer. Eigener Zielbereich `training.s12.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s12 consume --count 1 --timeout 10000
```

**Terminal 3, nach READY:**

```bat
scripts\lab.cmd s12 produce --count 1 --delay 2000
```

**Erwartung:** Zustellung frühestens ungefähr zwei Sekunden nach dem Send, mit Schedulingtoleranz. MessageProducer.setDeliveryDelay wird im Code sichtbar verwendet.

**Failure-Experiment:** Worker mit --timeout 500 starten und danach mit längerer Frist erneut verbinden: früher Timeout verwirft die wartende Nachricht nicht.

**Aufräumen:** Nach einem Commit ist die Queue leer.

## S13 – Message Groups und Reihenfolge

**Zweck:** Je Order drei aufeinanderfolgende Events beobachten.

**Topologie:** Queue mit JMSXGroupID pro Order → zwei Worker. Eigener Zielbereich `training.s13.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s13 consume --worker alice --continuous --sleep 100
```

**Terminal 3:**

```bat
scripts\lab.cmd s13 consume --worker bob --continuous --sleep 100
```

**Terminal 4, nach READY beider:**

```bat
scripts\lab.cmd s13 produce --count 4 --events 3
```

**Erwartung:** Zwölf COMMITs, sequence=1,2,3 pro orderId bei demselben Worker im kontrollierten Lauf. Nicht die Gesamtlogreihenfolge verschiedener JVMs vergleichen.

**Failure-Experiment:** Einen Worker beenden; Gruppenzuordnung kann wechseln und unbestätigte Arbeit erneut erscheinen. Keine globale Reihenfolge oder Exactly-once-Garantie.

**Aufräumen:** Nach vollständiger Menge Worker stoppen, s13 browse prüfen. Smokecheck mit frischem Namespace wiederholen.

## S14 – Shared Durable Subscription

**Zweck:** Geteilte Subscription und unabhängige Kopie unterscheiden.

**Topologie:** Topic → team (Alice/Bob teilen) + audit (eigene Kopie). Eigener Zielbereich `training.s14.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s14 subscribe --subscription team --worker alice --continuous --sleep 100
```

**Terminal 3:**

```bat
scripts\lab.cmd s14 subscribe --subscription team --worker bob --continuous --sleep 100
```

**Terminal 4:**

```bat
scripts\lab.cmd s14 subscribe --subscription audit --worker audit --count 6
```

**Terminal 5, nach READY aller:**

```bat
scripts\lab.cmd s14 produce --count 6
```

**Erwartung:** Team verarbeitet sechs IDs insgesamt, Audit dieselben sechs vollständig. Anders als S02 hängt die geteilte Queue hier als Subscription an einem Topic.

**Failure-Experiment:** Andere Subscription-Namen für Alice und Bob wählen: dann entstehen zwei eigene Kopien statt geteilter Arbeit.

**Aufräumen:** Worker stoppen; s14 unsubscribe --subscription team und s14 unsubscribe --subscription audit. Versehentliche andere Namen ebenfalls gezielt abmelden.

## S15 – Persistenz und Neustart

**Zweck:** Bestätigte persistente Sends überleben den Neustart.

**Topologie:** persistente Queue → geordneter Brokerneustart → Worker. Eigener Zielbereich `training.s15.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s15 produce --count 3
scripts\broker.cmd stop
```

**Terminal 1 nach Brokerende:**

```bat
scripts\broker.cmd start
```

**Terminal 2 nach BROKER_READY:**

```bat
scripts\lab.cmd s15 consume --count 3
```

**Erwartung:** Dieselben drei fachlichen IDs kommen an. Derselbe runtime/broker-Pfad mit NIO-Journal bleibt erhalten.

**Failure-Experiment:** Vor Neustart einen anderen --dataDir verwenden: das ist ein anderer Datenbestand und kein Beleg für Datenverlust im ursprünglichen Journal.

**Aufräumen:** Queue nach erfolgreichem Empfang leer; kein Stromausfalltest und keine Aussage zu HA.

## S16 – Rückstau und langsame Consumer

**Zweck:** Sichtbarer Backlog bei kleiner begrenzter Last.

**Topologie:** begrenzter Producer → Queue → langsamer und später zweiter Worker. Eigener Zielbereich `training.s16.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s16 produce --count 20
scripts\lab.cmd s16 browse
```

**Terminal 2:**

```bat
scripts\lab.cmd s16 consume --worker slow --continuous --sleep 500
```

**Terminal 4 später:**

```bat
scripts\lab.cmd s16 consume --worker helper --continuous --sleep 100
```

**Erwartung:** Anfangs 20 wartende IDs, dann sinkender Rückstau. Brokerterminal zeigt depth, consumers und delivering alle zwei Sekunden. Zweiter Worker erhöht mögliche Abnahmerate.

**Failure-Experiment:** Hilfsworker stoppen, wieder starten und Rate vergleichen. Nicht unbegrenzt neue Producer starten oder die Platte füllen.

**Aufräumen:** Nach 20 Gesamt-COMMITs Worker stoppen; s16 browse visible=0. Paging ab 1 MiB/Address, global 64 MiB, consumerWindowSize=0; Prefetch beeinflusst sonst die Verteilung.
