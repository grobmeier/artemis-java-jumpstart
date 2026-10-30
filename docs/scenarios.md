# Executable scenarios

The three-station pilot uses S01/S02, S03 and S05/S06. S04, S07 and S08–S16 are extensions. All commands below run in **cmd.exe from the lab root**. On Linux/macOS replace `scripts\lab.cmd` with `./scripts/lab.sh` and `scripts\broker.cmd` with `./scripts/broker.sh`.

First start `scripts\broker.cmd start` in **Terminal 1** and wait for `BROKER_READY`. Keep it open in the foreground throughout the other terminal steps. When a client must be `READY`, wait for that signal before publishing.

Consumer `--count` counts deliveries, including rolled-back attempts. `--timeout` is milliseconds per receive (default 5000); an incomplete bounded run exits with 2. `--continuous` explicitly keeps receiving. Use continuous mode when worker allocation is unknown, stop after the combined complete ID set, then browse. `--sleep` simulates processing time in milliseconds. Producers create fresh business IDs per invocation; `--id` deliberately fixes an ID.

Each scenario has separate `<namespace>.sNN.orders.incoming` or `.orders.events` destinations; default namespace is `training`. For repeats choose a fresh `--namespace` in **every** participating command, or drain known work and remove owned durable subscriptions. Empty queues without consumers can be removed using `scripts\lab.cmd sNN cleanup --confirm` with optional `--target incoming|outgoing|dlq|expiry`; replace placeholders before running. Topic cleanup uses the same subscription identity. Full broker reset is only for a stopped, exclusively owned local lab.


## S01 — Simple queue

**Purpose:** Ten persistent commands wait without a consumer.

**Topology:** Producer → durable anycast queue → worker. Destinations: `training.s01.*`.

**Terminal 3 first:**

```bat
scripts\lab.cmd s01 produce --count 10
scripts\lab.cmd s01 browse
```

**Terminal 2 next:**

```bat
scripts\lab.cmd s01 consume --worker alice --count 10
```

**Terminal 3 finally:**

```bat
scripts\lab.cmd s01 browse
```

**Expected evidence:** Before processing: visible=10. Afterwards: ten COMMITs and visible=0.

**Controlled experiment:** Publish without a worker and let the producer exit. The work remains.

**Cleanup:** Consume the known remaining count.


## S02 — Competing consumers

**Purpose:** Two workers share work instead of receiving independent copies.

**Topology:** Producer → one queue → Alice / Bob. Destinations: `training.s02.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s02 consume --worker alice --continuous --sleep 150
```

**Terminal 3:**

```bat
scripts\lab.cmd s02 consume --worker bob --continuous --sleep 150
```

**Terminal 4, after both workers are READY:**

```bat
scripts\lab.cmd s02 produce --count 10
```

**Expected evidence:** Ten business IDs in total; both workers participate. No exact half or alternating allocation is guaranteed.

**Controlled experiment:** Stop one worker while it waits in receive. The other can take further work. A crash after receive may cause redelivery (Station 3).

**Cleanup:** After the complete set of ten COMMITs, stop both workers; s02 browse should show visible=0.


## S03 — Non-durable topic subscriptions

**Purpose:** Active independent interests receive copies.

**Topology:** Topic → billing subscription + shipping subscription. Destinations: `training.s03.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s03 subscribe --subscription billing --worker billing --count 3
```

**Terminal 3:**

```bat
scripts\lab.cmd s03 subscribe --subscription shipping --worker shipping --count 3
```

**Terminal 4, after both services are READY:**

```bat
scripts\lab.cmd s03 produce --count 3
```

**Terminal 2, after the first run ends:**

```bat
scripts\lab.cmd s03 subscribe --subscription late --worker late --count 2
```

**Terminal 4, after late is READY:**

```bat
scripts\lab.cmd s03 produce --count 2
```

**Expected evidence:** Billing and shipping each receive the complete first set of three IDs; late receives only the two future IDs.

**Controlled experiment:** Publish before creating late: those events do not appear there. A bounded missing-count receive exits with 2. The CLI label is not a durable identity.

**Cleanup:** Close the consumers; non-durable subscriptions disappear on disconnection.


## S04 — Durable topic subscription

**Purpose:** Register first, go offline, then reconnect using the same identity.

**Topology:** Topic → durable shipping subscription. Destinations: `training.s04.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s04 register --subscription shipping
```

**Terminal 3, after register exits:**

```bat
scripts\lab.cmd s04 produce --count 3
```

**Terminal 2 next:**

```bat
scripts\lab.cmd s04 subscribe --subscription shipping --count 3
```

**Terminal 2, second run:**

```bat
scripts\lab.cmd s04 register --subscription shipping
```

**Terminal 3 next:**

```bat
scripts\lab.cmd s04 produce --count 3
scripts\broker.cmd stop
```

**Terminal 1, after the broker exits:**

```bat
scripts\broker.cmd start
```

**Terminal 2, after BROKER_READY:**

```bat
scripts\lab.cmd s04 subscribe --subscription shipping --count 3
```

**Expected evidence:** Three offline events arrive each time, including after graceful restart. Keep client ID training.s04.client-shipping and subscription training.s04.shipping unchanged.

**Controlled experiment:** Connect with --subscription other: this new identity does not receive shipping history and times out.

**Cleanup:** Run scripts\lab.cmd s04 unsubscribe --subscription shipping after its consumer exits. Remove an accidental other identity separately.


## S05 — Acknowledgement and redelivery

**Purpose:** Receive alone does not establish successful business processing.

**Topology:** Queue → transacted worker → rollback or crash. Destinations: `training.s05.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s05 produce --count 1 --id ack-demo
```

**Terminal 2:**

```bat
scripts\lab.cmd s05 consume --count 1 --rollback
```

**Terminal 2 next:**

```bat
scripts\lab.cmd s05 consume --count 1 --timeout 10000
```

**Expected evidence:** The same business ID returns, with JMSRedelivered=true and attempt=2 in the controlled run.

**Controlled experiment:** Use --crash instead of --rollback in the first worker: intentional exit 17, before JMS commit. Broker detection can take seconds; recover with --timeout 10000.

**Cleanup:** After successful commit the queue is empty. Read AUTO_ACKNOWLEDGE and session-wide CLIENT_ACKNOWLEDGE in concepts.md.


## S06 — Retry and dead letters

**Purpose:** The second of three orders is intentionally invalid.

**Topology:** Input queue → maximum three attempts → DLQ → repaired replay. Destinations: `training.s06.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s06 produce --count 3
```

**Terminal 2:**

```bat
scripts\lab.cmd s06 consume --count 5
```

**Terminal 3 next:**

```bat
scripts\lab.cmd s06 browse --target dlq
scripts\lab.cmd s06 replay --count 1
```

**Terminal 2 next:**

```bat
scripts\lab.cmd s06 consume --count 1
```

**Expected evidence:** Two healthy COMMITs, three ROLLBACKs for order-2, 500 ms redelivery delay, then one DLQ entry. Replay repairs priorityClass in payload AND properties while retaining the business ID.

**Controlled experiment:** Without repair the same invalid information would fail again. Three total attempts mean two additional retries.

**Cleanup:** Both s06 browse and s06 browse --target dlq should show visible=0. Inspect only your own failed work.


## S07 — Selectors and business routing

**Purpose:** Message properties determine each subscription view.

**Topology:** Topic → EU filter + cancellation filter. Destinations: `training.s07.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s07 subscribe --worker eu --selector "region = 'EU'" --count 3
```

**Terminal 3:**

```bat
scripts\lab.cmd s07 subscribe --worker cancellations --selector "eventType = 'OrderCancelled'" --count 2
```

**Terminal 4, after both are READY:**

```bat
scripts\lab.cmd s07 produce --count 6
```

**Expected evidence:** EU receives orders 1,3,5; cancellations receive 3,6. Business IDs end in the corresponding -N-1 suffix.

**Controlled experiment:** Filter a missing property: no matches and bounded timeout. A JSON field alone cannot satisfy a selector.

**Cleanup:** Non-durable subscriptions end with their connections.


## S08 — Local messaging transaction

**Purpose:** Receive and send are atomic against the same broker.

**Topology:** Queue A → one transacted session → queue B. Destinations: `training.s08.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s08 produce --count 1
```

**Terminal 2:**

```bat
scripts\lab.cmd s08 transfer --count 1 --rollback
```

**Terminal 3 next:**

```bat
scripts\lab.cmd s08 browse --target outgoing
```

**Terminal 2 next:**

```bat
scripts\lab.cmd s08 transfer --count 1
```

**Terminal 3 next:**

```bat
scripts\lab.cmd s08 consume --target outgoing --count 1
scripts\lab.cmd s08 browse
```

**Expected evidence:** Rollback produces no visible output. Commit confirms input and publishes output; input is empty at the end.

**Controlled experiment:** Only roll back once for this comparison. Repeating rollback can eventually send the input to the DLQ.

**Cleanup:** Drain both queues deliberately. This is neither a DB nor a distributed transaction.


## S09 — Idempotent processing

**Purpose:** Two technical sends share one business ID.

**Topology:** Queue → H2 transaction → JMS commit. Destinations: `training.s09.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s09 produce --count 2 --id ledger-demo
```

**Terminal 2:**

```bat
scripts\lab.cmd s09 consume --count 1 --crash
```

**Terminal 2 after exit code 17:**

```bat
scripts\lab.cmd s09 consume --count 2 --timeout 10000
```

**Terminal 3 next:**

```bat
scripts\lab.cmd ledger show
```

**Expected evidence:** Crash after DB commit, before JMS commit. Recovery reports two DUPLICATE observations with different JMSMessageIDs and exactly one ledger row in a fresh namespace.

**Controlled experiment:** After the experiment, stop workers and reset the ledger, then resend: the effect can happen again. Durable IDs matter.

**Cleanup:** Use scripts\lab.cmd ledger reset --confirm in your own namespace with workers stopped; first verify the queue is empty.


## S10 — Request/reply

**Purpose:** JMSReplyTo and checked correlation associate a reply with its request.

**Topology:** Request queue → responder → temporary reply queue. Destinations: `training.s10.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s10 reply --count 1
```

**Terminal 3, after READY:**

```bat
scripts\lab.cmd s10 request --timeout 3000
```

**Expected evidence:** Reply JMSCorrelationID equals the request JMSMessageID. Closing the creating connection removes the temporary reply queue.

**Controlled experiment:** Run s10 request --timeout 500 without a responder: exit 2. The committed request remains, but its temporary reply destination is no longer alive.

**Cleanup:** Deliberately consume leftover requests using s10 consume --count <REMAINING>. Do not start a late responder for orphaned requests.


## S11 — Time to live and expiry

**Purpose:** Stale work expires independently of processing failures.

**Topology:** Queue without consumers → TTL → expiry queue. Destinations: `training.s11.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s11 produce --count 1 --ttl 500
```

**Terminal 3 after about one second:**

```bat
scripts\lab.cmd s11 browse
scripts\lab.cmd s11 browse --target expiry
```

**Expected evidence:** Input empty, expiry contains the business ID. The scan interval is 100 ms; allow OS scheduling tolerance.

**Controlled experiment:** Start a worker before TTL expires: it can consume the valid message, leaving no expiry entry.

**Cleanup:** Use a fresh namespace for repeats; consume the owned expiry entry with s11 consume --target expiry --count 1.


## S12 — Delayed delivery

**Purpose:** Delivery delay affects first availability.

**Topology:** Producer → scheduled availability → queue consumer. Destinations: `training.s12.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s12 consume --count 1 --timeout 10000
```

**Terminal 3, after READY:**

```bat
scripts\lab.cmd s12 produce --count 1 --delay 2000
```

**Expected evidence:** Delivery no earlier than approximately two seconds after send, with scheduling tolerance. Code uses MessageProducer.setDeliveryDelay.

**Controlled experiment:** Use --timeout 500, then reconnect with a longer timeout. The early timeout does not discard the waiting message.

**Cleanup:** One successful commit leaves the queue empty.


## S13 — Message groups and order

**Purpose:** Observe three sequential events per order.

**Topology:** Queue with JMSXGroupID per order → two workers. Destinations: `training.s13.*`.

**Terminal 2:**

```bat
scripts\lab.cmd s13 consume --worker alice --continuous --sleep 100
```

**Terminal 3:**

```bat
scripts\lab.cmd s13 consume --worker bob --continuous --sleep 100
```

**Terminal 4, after both are READY:**

```bat
scripts\lab.cmd s13 produce --count 4 --events 3
```

**Expected evidence:** Twelve COMMITs; sequence=1,2,3 for each orderId at the same worker in the controlled run. Do not compare total log ordering across JVMs.

**Controlled experiment:** Stop a worker: group ownership may change and uncommitted work can return. This does not guarantee global order or exactly-once processing.

**Cleanup:** After the complete ID set, stop workers and browse s13. Repeat in a fresh namespace.


## S14 — Shared durable subscription

**Purpose:** Distinguish a shared subscription from an independent copy.

**Topology:** Topic → team (Alice/Bob share) + audit (separate copy). Destinations: `training.s14.*`.

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

**Terminal 5, after all are READY:**

```bat
scripts\lab.cmd s14 produce --count 6
```

**Expected evidence:** Team processes six IDs in total; audit receives the same complete six. The shared queue is a topic subscription.

**Controlled experiment:** Give Alice and Bob different subscription names: this creates two independent copies instead of shared work.

**Cleanup:** Stop workers, then s14 unsubscribe --subscription team and s14 unsubscribe --subscription audit. Remove accidental names separately.


## S15 — Persistence and restart

**Purpose:** Confirmed persistent sends survive graceful restart.

**Topology:** Persistent queue → graceful broker restart → worker. Destinations: `training.s15.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s15 produce --count 3
scripts\broker.cmd stop
```

**Terminal 1 after the broker exits:**

```bat
scripts\broker.cmd start
```

**Terminal 2 after BROKER_READY:**

```bat
scripts\lab.cmd s15 consume --count 3
```

**Expected evidence:** The same three business IDs arrive. Preserve runtime/broker and its NIO journal.

**Controlled experiment:** A different --dataDir refers to a different dataset; its empty state does not prove loss in the original journal.

**Cleanup:** Queue empty after successful consumption. This is not a power-loss or HA test.


## S16 — Backlog and slow consumers

**Purpose:** Observe backlog under a small bounded load.

**Topology:** Bounded producer → queue → slow worker and later helper. Destinations: `training.s16.*`.

**Terminal 3:**

```bat
scripts\lab.cmd s16 produce --count 20
scripts\lab.cmd s16 browse
```

**Terminal 2:**

```bat
scripts\lab.cmd s16 consume --worker slow --continuous --sleep 500
```

**Terminal 4 later:**

```bat
scripts\lab.cmd s16 consume --worker helper --continuous --sleep 100
```

**Expected evidence:** Initially 20 waiting IDs, then falling backlog. Broker prints depth, consumers and delivering every two seconds. A helper can increase drain rate.

**Controlled experiment:** Stop/restart the helper and compare rate. Do not launch unbounded producers or fill the disk.

**Cleanup:** After 20 total COMMITs stop workers; s16 browse should show visible=0. Paging starts at 1 MiB/address, global 64 MiB, consumerWindowSize=0; prefetch would affect allocation.
