# Trainee exercises

Build an order-processing simulation. Payments, shipping and emails are simulated. Work locally first. Keep `scripts\broker.cmd start` open in Terminal 1; wait for `BROKER_READY`, then use separate terminals from the lab root. Build the editable scaffolds with `mvnw.cmd -Ptraining clean verify`. Unfinished entry points compile but report exit 2 until implemented. Only `exercises/src/main/java/de/jumpstart/*Exercise.java` intentionally contains TODOs.

The executable demos provide observable references. [Worked solutions](solutions.md) live in a separate module. Use a new namespace per task and the identical value in all cooperating clients. Demos, exercises and solutions share scenario destinations: do not mix old work with a new run. Consumer `--count` includes rolled-back deliveries. With competing workers, begin with `--continuous`, stop after the combined complete ID set and browse the queue.

The pilot uses A, B and the failure branch of D. C, E and F are extensions. First predict in your own notes, run, introduce the controlled change, explain evidence and adapt your code. AI assistance is optional and subject to company policy. Check suggestions against code and logs. Never supply credentials, confidential code or company data to an external tool. Which suggestion did you accept or reject, and what evidence informed that decision?

Commands below are for cmd.exe. On Linux/macOS use `./mvnw` and `./scripts/exercises.sh`, `./scripts/lab.sh`, `./scripts/broker.sh`.


## Exercise A — Processing orders

Implement ten orders for orders.incoming (S02). Publish without workers, then repeat with one worker and two workers using a small processing delay.

**Time and prerequisites:** 35 minutes; classes, loops, exceptions; producer, consumer and queue concepts.

**Starting code:** [AExercise.java](../exercises/src/main/java/de/jumpstart/AExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 3 first:**

```bat
scripts\exercises.cmd A produce --count 10 --namespace task-a
```

**Terminal 2 next:**

```bat
scripts\exercises.cmd A consume --worker alice --count 10 --namespace task-a
```

**Second run, Terminal 2 and 3, one command per terminal:**

```bat
scripts\exercises.cmd A consume --worker alice --continuous --sleep 150 --namespace task-a2
scripts\exercises.cmd A consume --worker bob --continuous --sleep 150 --namespace task-a2
```

**Terminal 4 after both are READY:**

```bat
scripts\exercises.cmd A produce --count 10 --namespace task-a2
```

**Evidence of success:** All ten business IDs appear once as COMMIT in the controlled failure-free run, both workers participate, and s02 browse shows visible=0. An equal split is not required.

**Hint 1:** Use session.createQueue and persistent TextMessage sends. Each worker owns its connection and session.

**Hint 2:** Use bounded receive(timeout), do work before session.commit() and log using Messages.log. For two workers, use continuous mode rather than prescribing a per-worker quota.

**Common mistakes:** Wrong namespace, rigid half-batch count, missing commit.

**Reflection:** What waits without a consumer? Why do two workers not each get every command? What can happen after receive but before commit?


## Exercise B — Independent interests

Publish OrderCreated on a topic (S03). Billing and shipping need independent active subscriptions. Create a third interest later.

**Time and prerequisites:** 30 minutes; exercise A, command/event distinction and readiness.

**Starting code:** [BExercise.java](../exercises/src/main/java/de/jumpstart/BExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 2:**

```bat
scripts\exercises.cmd B subscribe --worker billing --subscription billing --count 3 --namespace task-b
```

**Terminal 3:**

```bat
scripts\exercises.cmd B subscribe --worker shipping --subscription shipping --count 3 --namespace task-b
```

**Terminal 4 after both are READY:**

```bat
scripts\exercises.cmd B produce --count 3 --namespace task-b
```

**Terminal 2 after exit:**

```bat
scripts\exercises.cmd B subscribe --worker late --subscription late --count 2 --namespace task-b
```

**Terminal 4 after late is READY:**

```bat
scripts\exercises.cmd B produce --count 2 --namespace task-b
```

**Evidence of success:** Both initially active services receive the complete same three-ID set. Late receives only the two future IDs.

**Hint 1:** Use createTopic and createConsumer. Each connection creates its own non-durable subscription.

**Hint 2:** Create consumers, start connections and print READY before publishing. The CLI subscription name is a learning label here, not a durable identity.

**Common mistakes:** Publishing before readiness, sharing one queue, confusing a topic with retained history.

**Reflection:** Why does a queue with two consumers fail this requirement? Which events does the late service miss?


## Exercise C — Shipping is offline

Register a durable shipping subscription (S04), disconnect, send three events, then reconnect. Repeat across graceful broker restart.

**Time and prerequisites:** 25 minutes; exercise B, persistent events and stable identity.

**Starting code:** [CExercise.java](../exercises/src/main/java/de/jumpstart/CExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 2:**

```bat
scripts\exercises.cmd C register --subscription shipping --namespace task-c
```

**Terminal 3 after exit of register:**

```bat
scripts\exercises.cmd C produce --count 3 --namespace task-c
```

**Terminal 2 next:**

```bat
scripts\exercises.cmd C subscribe --subscription shipping --count 3 --namespace task-c
```

**Repeat: Terminal 3 after register and produce again:**

```bat
scripts\broker.cmd stop
```

**Terminal 1 after the broker exits:**

```bat
scripts\broker.cmd start
```

**Evidence of success:** Three waiting IDs arrive each time. Preserve the same data path and persistent events on restart, then repeat the subscribe command.

**Hint 1:** Set the client ID before starting the connection. Use createDurableConsumer with a stable name; register may close the consumer afterwards.

**Hint 2:** Identity combines client ID and subscription name. Use namespace/scenario consistently, for example task-c.s04.client-shipping and task-c.s04.shipping.

**Common mistakes:** Setting client ID after connection start, changing names, two active unshared durable consumers with one identity.

**Reflection:** What changes with a new name/client ID? How do persistent messages differ from durable subscriptions?


## Exercise D — Failure and poison messages

Order-2 has priorityClass=invalid. Roll it back using a transacted session. BrokerRuntime allows three total attempts at 500 ms intervals. Inspect the DLQ and deliberately send a repaired version.

**Time and prerequisites:** 45 minutes; queue transactions, rollback and delivery counts. The pilot implements the failure branch first; complete replay afterwards if time is short.

**Starting code:** [DExercise.java](../exercises/src/main/java/de/jumpstart/DExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 3:**

```bat
scripts\exercises.cmd D produce --count 3 --namespace task-d
```

**Terminal 2:**

```bat
scripts\exercises.cmd D consume --count 5 --namespace task-d
```

**Terminal 3 next:**

```bat
scripts\lab.cmd s06 browse --target dlq --namespace task-d
scripts\exercises.cmd D replay --count 1 --namespace task-d
```

**Terminal 2 next:**

```bat
scripts\exercises.cmd D consume --count 1 --namespace task-d
```

**Evidence of success:** Two healthy COMMITs, three attempts for the invalid ID and one matching DLQ entry. Replay keeps the business ID but creates a new technical send; repaired work commits successfully.

**Hint 1:** Rollback invalid work instead of committing. count counts deliveries: three original messages plus two retries means five receives.

**Hint 2:** Local DLQ FQQN: DLQ::DLQ.<namespace>.s06.orders.incoming. Receive and repaired send can share one JMS transaction. Repair payload AND property.

**Common mistakes:** Committing after rollback, treating three attempts as three retries, hiding invalid data in endless retries.

**Reflection:** How do transient faults and invalid business data differ? Why should replay be deliberate?


## Exercise E — The same order twice

Send one business ID twice (S09). Store ID and local ledger effect atomically in H2. Crash after DB commit but before JMS commit, then restart.

**Time and prerequisites:** 60 minutes, extension; JDBC transactions and unique constraints. Add H2 to exercises/pom.xml using the parent-managed version.

**Starting code:** [EExercise.java](../exercises/src/main/java/de/jumpstart/EExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 3:**

```bat
scripts\exercises.cmd E produce --count 2 --id same-order --namespace task-e
```

**Terminal 2:**

```bat
scripts\exercises.cmd E consume --count 1 --crash --namespace task-e
```

**Terminal 2 after exit code 17:**

```bat
scripts\exercises.cmd E consume --count 2 --namespace task-e --timeout 10000
```

**Evidence of success:** Multiple technical deliveries, exactly one local ledger row. The two sends share the business ID and have different JMSMessageIDs. Complete JMS commit for already-known IDs after restart.

**Hint 1:** Use one INSERT containing business_id primary key, order_id and amount_cents as the simulated effect. Disable DB autocommit.

**Hint 2:** Use WRITE_DELAY=0 and DB commit before the crash. Handle unique violation SQLState 23505 with DB rollback, then commit JMS. Do not swallow other SQL errors as duplicates.

**Common mistakes:** Separate ID/effect commits, an in-memory Set, losing buffered H2 writes on Runtime.halt.

**Reflection:** Why does a Set not survive restart? Where is the gap between non-atomic DB operations? Why does this not protect real payments/emails automatically?


## Exercise F — Only relevant events

Publish six events on S07. Odd orders are EU, even orders US; every third event is OrderCancelled, others OrderCreated. Predict both subscriber ID sets first.

**Time and prerequisites:** 30 minutes; exercise B, message properties and selectors.

**Starting code:** [FExercise.java](../exercises/src/main/java/de/jumpstart/FExercise.java). Complete its TODO with direct Jakarta Messaging calls. You may use common for configuration, JSON and logging.

**Terminal 2:**

```bat
scripts\exercises.cmd F subscribe --worker eu --selector "region = 'EU'" --count 3 --namespace task-f
```

**Terminal 3:**

```bat
scripts\exercises.cmd F subscribe --worker cancellations --selector "eventType = 'OrderCancelled'" --count 2 --namespace task-f
```

**Terminal 4 after both are READY:**

```bat
scripts\exercises.cmd F produce --count 6 --namespace task-f
```

**Evidence of success:** EU receives orders 1,3,5; cancellations receive 3,6. Compare sets, not cross-process log order.

**Hint 1:** Set message properties with setStringProperty. JSON and properties must express the same business values.

**Hint 2:** Use createConsumer(topic, selector). Selector strings use single quotes; surround the entire expression with double quotes in cmd.exe.

**Common mistakes:** Filtering only JSON, wrong case/quotes, publishing before READY.

**Reflection:** What is the difference between payload and properties? Why can one order appear in both independent subscriptions?
