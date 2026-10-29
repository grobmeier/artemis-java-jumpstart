# Acceptance evidence

## English Launch Training Labs revision

English CLI, diagnostic strings, exercise comments and matching assertions passed the local Java 21 process/TCP suite on 29 October 2026: **18 tests, zero failures/errors/skips** (103 seconds, macOS, Zulu 21.0.1). Final documentation packaging and Windows/Linux validation of the English pilot revision are recorded below once executed. Do not treat a pending platform as passed.

## Previous verified baseline

The pre-migration baseline was d0cf55fb88c7f5529c7ed7a822df2ca7c35ba517. [GitHub Actions run 40939707952](https://github.com/grobmeier/artemis-java-jumpstart/actions/runs/40939707952) established Windows/Linux success before English migration. It is historical evidence, not verification of a newer English head.

## Reproduce

Run `mvnw.cmd -B -Ptraining clean verify` on Windows or `./mvnw -B -Ptraining clean verify` on Linux/macOS with JDK 21. Tests use actual packaged JARs in separate JVMs, local TCP/TLS sockets and temporary namespaces. Reports: distribution/target/failsafe-reports; child logs: distribution/target/process-logs; runtime: distribution/target/artemis-java-jumpstart-runtime.zip. GitHub artifacts are private and retained seven days.

The suite checks queue ID sets and competing-worker participation; independent/late subscribers; offline durable delivery/restart; rollback/crash redelivery; three poison attempts, DLQ and repaired replay; selectors; atomic receive/send; H2 crash recovery; correlated temporary request/reply; TTL/delay; message groups; shared durable fan-out; bounded backlog; TLS/authentication/authorization; wrapper launchers in paths with spaces; precedence; reset ownership/lock/symlink/junction guards and preservation of foreign files. Scaffolds may be completed; independent reference solutions are executed in the training profile.

Windows tests invoke real cmd launchers, including explicit UTF-8 output and captured Java version detection. Linux and macOS tests do not substitute for Windows execution. Generated local test certificates do not establish company-PC connectivity to a remote trainer broker. No real remote server was changed.

## Manual pilot follow-up

The public learning-page implementation needs browser, print, worksheet and privacy-boundary verification in the website repository. A real two-trainee pilot and any optional company-network remote preflight remain manual follow-ups. No production/remote deployment is implied by these checks.
