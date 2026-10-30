# Trainer guide: Launch Training Labs pilot

## Prepare before the session

Provide JDK 21, the public repository link and the verified runtime ZIP. On actual company computers check paths, `doctor build`, `mvnw.cmd -Ptraining clean verify` and the extracted ZIP in a directory with spaces. Prepare four cmd.exe terminals per learner. For offline source builds prepare and test the wrapper distribution and Maven dependency/plugin cache; runtime-only demonstrations need no Maven/network. Have company IT configure proxies/certificates; do not change execution policy or bypass TLS checks.

Use the English revision and platform evidence recorded in [acceptance.md](acceptance.md). Public station pages and worksheets contain deliberately authored teaching material. Java source, complete solutions and this facilitation guide are available in the public training repository. Maintain the learning website separately; do not copy the repository into the website build.

## Three stations: 3 hours 45 minutes

| Segment | Minutes |
|---|---:|
| Environment buffer | 20 |
| Story, concepts and prediction method | 15 |
| Station 1: queues, S01/S02 and exercise A | 45 |
| Station 2: independent subscribers, S03 and exercise B | 45 |
| Break | 15 |
| Station 3: acknowledgement/retry/DLQ, S05/S06 and D failure branch | 60 |
| Debrief, changed-condition demonstrations and cleanup | 25 |
| Total | 225 |

Each station follows **Predict → Run → Break → Explain → Adapt**. Learners record predictions and business-ID evidence locally before hints/AI explanations. Give hint 1 first and hint 2 later. The website's nine self-checks support understanding; they do not assess Java submissions. A checked completion item is self-assessment, not verified practical completion.

Keep C/S04 durable subscriptions, E/S09 idempotency and F/S07 selectors optional. Do not fit all sixteen scenarios into this pilot. In Station 3 implement rollback/commit first; finish all D replay code as follow-up if time runs short.

## Two trainees

Start with a separate local broker and a fresh namespace on each machine. Driver operates/types; navigator predicts, checks terminal readiness and reconciles IDs. Swap every 15–20 minutes and at least once per station. Each trainee must operate and explain a changed condition.

Ask each learner to predict one change before running it: remove a worker, start a subscriber late, or roll back instead of committing. Discuss what evidence confirms or contradicts the prediction. This is formative feedback, not AI-authorship detection or automated personnel assessment.

AI is optional under company policy. Ask: **Which suggestion did you accept or reject, and what evidence informed that decision?** Do not request private prompts/chat histories, credentials, confidential code or company data. The supplied hints must support completing the core path without AI.

## Facilitation and evidence

| Station | Evidence | Common misconception | Changed condition |
|---|---|---|---|
| 1 / A | visible=10 before worker, all IDs committed, visible=0; competing union complete/disjoint and both participate | Every worker gets a full copy or an exact half | Stop workers, send three, browse backlog; change batch/delay in A |
| 2 / B | Billing and shipping each receive the complete first three-ID set; late only the future two | A topic stores arbitrary history; CLI label creates durability | Publish before a new non-durable consumer; bounded missing-count timeout exits 2; add third interest |
| 3 / D | Same ID redelivered after rollback/crash, metadata; two healthy commits and three poison attempts; one DLQ entry; repaired replay and both queues empty | Receive equals business success; three attempts means three retries; replay gives exactly-once | Implement invalid rollback, repair both payload/property; explain why retry cannot repair invalid data |

Reference terminal sequences: [scenarios](scenarios.md). Editable entry points and two staged hints per task: [exercises](exercises.md). Compare business-ID sets and queue states; exact split and cross-process log order are not assessment criteria. Use short bounded receive timeouts; expected exit 17 is the crash demonstration, exit 2 signals incomplete bounded work.

Explain AUTO_ACKNOWLEDGE timing and session-wide CLIENT_ACKNOWLEDGE before drawing conclusions about business success. At-least-once permits duplicates. S09 protects one atomic local H2 effect; it does not provide end-to-end exactly-once for payments or emails.

## Repeat and clean up

Choose a fresh namespace in every participating client. Stop workers; deliberately consume the known remaining work and remove only owned durable identities. Empty owned queues may be removed with `cleanup --confirm`. Do not delete someone else's messages/subscriptions.

For a completely fresh **local** broker, stop all workers, run `scripts\broker.cmd stop`, wait for process exit, then `scripts\broker.cmd reset --confirm`. The reset removes only the marked broker data area and preserves H2. Reset a local ledger separately with `scripts\lab.cmd ledger reset --confirm --namespace NAME` while its workers are stopped. Investigate marker/path errors instead of bypassing guards. Extract a fresh ZIP if moving a previously used runtime changed its canonical path.

The optional [remote profile](remote-broker.md) needs actual company-PC TCP/TLS/authentication/authorization preflight. A website loading over HTTPS does not establish broker connectivity. Use the local broker/runtime if company network access is blocked. No remote deployment is part of this pilot.

For later shared remote work, authorize a group explicitly: A/S02 needs one shared queue; B/S03 one topic with independent subscriptions. A namespace argument alone grants no rights. C needs stable identities; two unshared consumers cannot use one active identity. E uses a local ledger per machine and is not a distributed idempotency system.

## Pilot feedback and launch gate

Record aggregate facilitation observations without collecting personal activity logs: setup blockers, actual station duration, unclear words/commands and whether each learner explained a changed condition. Ask what should be shortened or expanded. Update teaching content from these observations; do not claim measured learning improvement from an unrun pilot.

Before production publication, review the local/staging learning-page preview, exact English reference revision, Windows/Linux CI, public-bundle boundary and worksheet/print output with the owner. Real two-trainee pilot feedback remains a manual follow-up.
