# Separate worked solutions

Attempt the task first. The private solutions module uses direct Jakarta Messaging calls without depending on lab. Build with mvnw.cmd -Ptraining clean verify; start the broker separately. scripts\solutions.cmd takes the same arguments as exercises. Use a fresh namespace per task.

| Exercise | Entry | Actions | Scenario |
|---|---|---|---|
| A | [ASolution](../solutions/src/main/java/de/jumpstart/ASolution.java) | produce, consume | s02 |
| B | [BSolution](../solutions/src/main/java/de/jumpstart/BSolution.java) | produce, subscribe | s03 |
| C | [CSolution](../solutions/src/main/java/de/jumpstart/CSolution.java) | register, produce, subscribe, unsubscribe | s04 |
| D | [DSolution](../solutions/src/main/java/de/jumpstart/DSolution.java) | produce, consume, replay | s06 |
| E | [ESolution](../solutions/src/main/java/de/jumpstart/ESolution.java), [SolutionLedger](../solutions/src/main/java/de/jumpstart/SolutionLedger.java) | produce, consume; --crash | s09 |
| F | [FSolution](../solutions/src/main/java/de/jumpstart/FSolution.java) | produce, subscribe; --selector | s07 |

Use the separate-terminal sequences in [exercises.md](exercises.md), replacing exercises.cmd with solutions.cmd. A queue example:

```bat
scripts\solutions.cmd A produce --count 10 --namespace solution-a
scripts\solutions.cmd A consume --count 10 --namespace solution-a
```

For topics start subscribers **before** publishing and wait for READY; the queue example is not a topic startup sequence. D needs five deliveries for three produced messages. E produces two equal business IDs, crashes with --count 1 --crash (exit 17), then recovers with --count 2 --timeout 10000. `scripts\lab.cmd ledger show --namespace NAME` reports one effect. WRITE_DELAY=0 persists the DB commit before the hard crash.

A owns per-worker resources and commits after work. B creates separate non-durable subscribers. C sets stable client ID before connection start and retains durable interest offline. D rolls back invalid work and repairs payload/property together. E specifically handles unique violations and commits JMS after a successful or already-known local effect. F sets properties consistently with JSON and uses messaging selectors. These are learning patterns, not an end-to-end exactly-once guarantee.

Stop workers, deliberately drain known work, unsubscribe C's identity and reset E's owned local ledger only with its workers stopped. Do not publish this module or its complete worked solutions on the public learning site.
