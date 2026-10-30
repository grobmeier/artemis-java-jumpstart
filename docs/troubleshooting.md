# Troubleshooting

| Observation | Check/action |
|---|---|
| Source ZIP scripts say build artifact is missing | Extract the whole source, including .mvn; set JAVA_HOME, run scripts/doctor build, then mvnw -Ptraining clean verify. A GitHub source ZIP is not the prebuilt runtime ZIP. |
| JAVA_HOME missing even though java works | Set JAVA_HOME to the JDK root in each terminal. The Windows Maven Wrapper requires it. Use cmd.exe set commands or PowerShell $env: syntax as documented; do not mix shells. |
| Worker exits while opening the producer terminal | Use the documented --timeout 300000 (five minutes per receive), or restart the worker. Confirm READY before sending. Queue backlog survives; non-durable topic subscribers must be restarted together before a new batch. |
| Build cannot replace/delete a JAR | Stop all clients and your broker before rebuilding; Windows can lock JARs used by running Java processes. Rebuild, restart the broker and repeat with fresh namespaces. |
| Java missing/wrong version | where java or command -v java, java -version, JAVA_HOME; supply JDK 21 |
| Build artifact missing | mvnw.cmd clean verify from source; add -Ptraining for exercises/solutions; inspect lib in runtime ZIP |
| Wrapper cannot download Maven | Company access, bootstrap cache, HTTPS/Java truststore; then Maven settings.xml for artifacts |
| Port occupied | Find your own broker; use the same alternate port in every process if another service owns it; do not blindly kill unrelated processes |
| Broker port unreachable | Start broker separately, wait BROKER_READY, check host/port/firewall |
| Authentication fails | Correct profile, individual user and private password; never paste secrets into logs |
| Remote TLS fails | Check truststore, chain, expiry, PKCS12 password and SAN matching DNS; retain verification |
| Consumer TIMEOUT | READY before publish? Same namespace/scenario? Correct count? New non-durable subscriptions have no prior history |
| Durable subscriber empty | Same client ID AND subscription name as registration; register before offline send |
| Message redelivered | Previous commit missing, rollback or crash; compare business ID and redelivery metadata |
| S06 takes longer | Three attempts for order-2 plus two healthy deliveries means five receives |
| More than one ledger row | Fresh namespace, same business ID, same H2 path, WRITE_DELAY=0 and one atomic INSERT/commit |
| H2 file locked | One local S09 worker per namespace/ledger; stop the other worker, do not delete a live DB |
| Reset refused | Broker fully stopped? Owned marker? Under runtime? Symlink/junction? Investigate, keep guards |
| Cleanup refused | Queue must be empty without active consumers; process known remaining work and stop workers |
| Uneven allocation | No fixed quota: wait both READY, use enough messages and small delay; consumer window affects pre-allocation |
| Send times out | Check broker/disk. Outcome may be uncertain; preserve business ID when retrying |

`scripts/doctor build` works in a fresh source checkout before compilation and checks the JDK/wrapper, not Maven repository connectivity. `doctor runtime` checks JARs, TCP and authenticated connection, not every destination permission. Broker start intentionally blocks. count must be positive; continuous mode is explicit. Unexpected errors produce a concise English diagnostic rather than a provider stack trace that might contain credentials.

Incomplete bounded consumption exits with 2. Do not assume each competing worker receives count=5: use --continuous and reconcile the combined set. Intentional crash exits with 17. Broker detection after disconnection can take seconds; recovery uses a bounded 10000 ms timeout.

Targeted empty queue removal: `scripts\lab.cmd s01 cleanup --confirm`, optionally --target outgoing, dlq or expiry, using the right namespace/scenario. Remove durable topic resources using unsubscribe with the same identity. Reset whole owned local broker data only after graceful stop; H2 is separate.

Moving an already-used runtime deliberately invalidates its canonical ownership marker. Extract a fresh ZIP for a new training run. Do not edit the marker to bypass checks; the trainer decides any controlled data migration. Unused packages may be moved freely.
