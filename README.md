# Artemis Java Jumpstart

A local Java 21 lab about order processing with Apache Artemis and Jakarta Messaging. The broker runs visibly in a separate JVM; clients connect over TCP. Payments, shipping and emails are simulated.

## Training repository

This repository is **private**: [grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart). Repository access requires an invitation from your trainer. After accepting your personal invitation:

```bat
git clone https://github.com/grobmeier/artemis-java-jumpstart.git
cd artemis-java-jumpstart
git switch -c feature/YOUR_GITHUB_NAME-exercise-a
.\mvnw.cmd -Ptraining clean verify
```

Replace `YOUR_GITHUB_NAME`. Without Git, sign in and choose **Code → Download ZIP**. See [access, personal branches and GitHub Actions](docs/github-training.md). Invited collaborators can also read the sample solutions and other branches.

## First local run

Use JDK 21. Open three independent cmd.exe terminals in the repository root. The broker remains in the foreground; do not paste all terminal blocks into one terminal.

**Terminal 1 — broker:**

```bat
.\scripts\broker.cmd start
```

Wait for `BROKER_READY`.

**Terminal 2 — worker:**

```bat
.\scripts\lab.cmd s01 consume --worker alice --count 10 --namespace first-run
```

Wait for `READY`.

**Terminal 3 — producer:**

```bat
.\scripts\lab.cmd s01 produce --count 10 --namespace first-run
```

The worker reports ten `COMMIT` records and exits. The broker stays active. Stop it with Ctrl+C in Terminal 1 or `.\scripts\broker.cmd stop` in another terminal; wait for process exit before restart/reset.

On Linux/macOS use `./mvnw` and `./scripts/*.sh`. Set `JAVA_HOME` to a JDK or put it on `PATH`. Java 21 is the tested baseline; newer JDKs are not automatically verified platforms. A bounded consumer exits with **2** if the requested delivery count is not reached before its receive timeout. Use `--continuous` for manual competing-worker experiments and stop with Ctrl+C. An intentional `--crash` exits with **17**.

## Learning path

The Launch Training Labs pilot has three stations: **Queues and competing consumers** (S01/S02, exercise A), **Events and independent subscribers** (S03, exercise B), and **Acknowledgement, retry and dead letters** (S05/S06, exercise D). See the [pilot trainer guide](docs/trainer-guide.md).

[Windows setup](docs/windows-setup.md) → [concepts](docs/concepts.md) → [executable scenarios](docs/scenarios.md) → [exercises A–F](docs/exercises.md). S04/C, S07/F and S08–S16 are extensions. [Worked solutions](docs/solutions.md) are separate from the editable scaffolds. Predict, run, break, explain and adapt; a green reference build does not grade a trainee's solution.

## Runtime package and offline use

`clean verify` creates `distribution/target/artemis-java-jumpstart-runtime.zip`. Extract it, including into a path with spaces, and read **RUN.md**. It contains `lib/broker.jar`, `lib/lab.jar`, scripts, documentation and licences. Local demonstrations then need only the supplied JDK 21: no Maven, internet, Docker or administrator rights. The JDK is not included. Editing exercises requires the full source and a build environment; build exercises and solutions with `-Ptraining`. An offline source build additionally needs a prepared Maven Wrapper distribution, dependencies and plugin cache.

## Configuration and owned data

Precedence: **CLI > LAB_ environment variables > properties file > defaults**. `--config` or `LAB_CONFIG` selects the file. Camel-case keys such as `dataDir` map to `LAB_DATA_DIR`. Defaults: loopback `127.0.0.1:61616`, namespace `training`, public local demo credentials `lab` / `lab-local-only`. These are for local use only.

Use the same `--namespace team1` in every cooperating client. Choose a fresh namespace for each repeat. A local namespace separates names, not access rights. The optional [remote profile](docs/remote-broker.md) requires TLS and individual authorized users without demo fallback. Keep real passwords in protected local configuration or environment variables, never CLI arguments, Git or logs.

Broker files live under `runtime/broker/` (NIO journal, bindings, paging, large messages). S09 uses `runtime/ledger/<namespace>.mv.db`. Stop all workers and the broker before `.\scripts\broker.cmd reset --confirm`. Ownership markers, canonical path checks and a process lock confine deletion to the owned broker area. The ledger is preserved; reset it separately with `lab ledger reset --confirm` while its workers are stopped. Prefer targeted queue cleanup or subscription removal for individual experiments. Never reset a shared remote broker as trainee cleanup.

## Verification

```bat
.\mvnw.cmd -B -Ptraining clean verify
```

This starts real child JVMs and TCP/TLS brokers, exercises the reference scenarios and independent solutions, and packages the runtime. Trainee scaffolds may be completed; tests do not require TODOs to remain open. [Acceptance evidence](docs/acceptance.md), [versions and sources](docs/versions.md), [troubleshooting](docs/troubleshooting.md), [dependency licences](licenses/DEPENDENCIES.md).
