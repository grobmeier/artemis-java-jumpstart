# Run the prebuilt lab

Extract the complete package before running scripts. Check for `lib/lab.jar` and `lib/broker.jar`. If you have `pom.xml` and `mvnw` instead, you downloaded source: follow README.md and build it first. Set JAVA_HOME to the supplied JDK 21 directory in every terminal, as shown in README.md. On Windows, use cmd.exe for the command blocks below.

This extracted package requires a supplied **JDK 21**. Local demonstrations need no Maven or internet. The JDK is not included. In each cmd.exe terminal, change to the extracted `artemis-java-jumpstart` directory.

**Terminal 1 — broker (foreground):**

```bat
.\scripts\broker.cmd start
```

Wait for `BROKER_READY`.

**Terminal 2 — worker:**

```bat
.\scripts\lab.cmd s01 consume --worker alice --count 10 --namespace first-run --timeout 300000
```

Wait for `READY`.

**Terminal 3 — producer:**

```bat
.\scripts\lab.cmd s01 produce --count 10 --namespace first-run
```

The worker waits up to five minutes per receive while you open the producer terminal. Check ten `COMMIT` records. Stop the broker using Ctrl+C or `.\scripts\broker.cmd stop` from Terminal 3. Wait for process exit before restart/reset. On Linux/macOS use `./scripts/broker.sh` and `./scripts/lab.sh`; scripts support paths with spaces and absolute invocation from another directory.

Diagnosis: `.\scripts\doctor.cmd runtime`. See [scenarios](docs/scenarios.md). This package contains demo JARs, not exercise source; implementing tasks requires the full private source repository and a build environment. Offline source builds also need the prepared wrapper and dependency cache. Public local demo credentials are `lab` / `lab-local-only`, loopback only. Remote use needs individual credentials and TLS; see [remote guide](docs/remote-broker.md).

Data is stored under `runtime/broker`; S09 also uses `runtime/ledger`. Stop all workers and the broker before `.\scripts\broker.cmd reset --confirm`. This preserves the H2 ledger. Never reset a shared remote broker for an individual exercise.
